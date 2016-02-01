package com.markusfeng.logicgame.multiplayer;

import java.io.IOException;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Random;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.Future;

import com.markusfeng.SocketRelay.A.SocketHandler;
import com.markusfeng.SocketRelay.C.SocketProcessorAbstract;

public abstract class GroupProcessor extends SocketProcessorAbstract<String>{
	
	protected Object assignmentLock = new Object();
	protected Random random;
	protected boolean isServer;
	protected Map<Long, SocketHandler<String>> ids;
	protected long serverID;
	protected volatile long id;
	protected volatile boolean assigned;
	public static final Runnable EMPTY_RUNNABLE = new Runnable(){

		@Override
		public void run() {
			//Do nothing
		}
		
	};
	
	public GroupProcessor(boolean isServer){
		this.isServer = isServer;
		random = new Random();
		
		if(isServer){
			id = random.nextLong();
			assigned = true;
		}
		ids = new HashMap<Long, SocketHandler<String>>();
	}
	
	@Override
	public void attachHandler(SocketHandler<String> handler){
		super.attachHandler(handler);
		if(isServer){
			long randTmp = random.nextLong();
			//Disallow 0 as id or repeated ids
			while(randTmp == 0 || ids.containsKey(randTmp)){
				randTmp = random.nextLong();
			}
			final long rand = randTmp;
			Map<String, String> map = handlerAdded(tpe.submit(new Callable<Long>(){

				@Override
				public Long call() throws Exception {
					return rand;
				}
				
			}), handler);
			map.put("targetid", String.valueOf(rand));
			ids.put(rand, handler);
			Command gameOver = Commands.make("initialize", map);
			try{
				outputToHandler(handler, gameOver, false);
			}
			catch(IOException e){
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
		else{
			ids.put(0L, handler);
			handlerAdded(tpe.submit(new Callable<Long>(){
				
				@Override
				public Long call(){
					waitForID();
					return getServerID();
				}
				
			}), handler);
		}
	}
	
	@Override
	public void removeHandler(SocketHandler<String> handler){
		super.removeHandler(handler);
		if(isServer){
			if(handler == null){
				return;
			}
			boolean targetFound = false;
			long targetID = 0;
			for(Map.Entry<Long, SocketHandler<String>> entry : ids.entrySet()){
				if(entry.getValue().equals(handler)){
					targetID = entry.getKey();
					targetFound = true;
					break;
				}
			}
			if(targetFound){
				ids.remove(targetID);
				handlerRemoved(targetID, handler);
			}
		}
		else{
			handlerRemoved(getServerID(), handler);
		}
	}

	@Override
	public void attachHandlers(Collection<SocketHandler<String>> handlers){
		for(SocketHandler<String> handler : handlers){
			attachHandler(handler);
		}
	}

	@Override
	public void removeHandlers(Collection<SocketHandler<String>> handlers){
		super.removeHandlers(handlers);
		if(isServer){
			Set<Long> toBeRemoved = new HashSet<Long>();
			for(Map.Entry<Long, SocketHandler<String>> entry : ids.entrySet()){
				if(handlers.contains(entry.getValue())){
					toBeRemoved.add(entry.getKey());
				}
			}
			for(long l : toBeRemoved){
				SocketHandler<String> handler = ids.remove(l);
				handlerRemoved(l, handler);
			}
		}
		else{
			for(SocketHandler<String> handler : handlers){
				handlerRemoved(getServerID(), handler);
			}
		}
	}

	@Override
	public void removeAllHandlers(){
		removeHandlers(getHandlers());
	}
	
	public void output(Command out, boolean blocking) throws IOException{
		if(!out.getArguments().containsKey("id")){
			out = Commands.make(out, Collections.singletonMap("id", String.valueOf(id)));
		}
		else{
			out = Commands.make(out, Collections.singletonMap("serverid", String.valueOf(id)));
		}
		output(out.toString(), blocking);
	}

	public void outputToHandler(SocketHandler<String> handler, Command out, boolean blocking) throws IOException{
		if(!out.getArguments().containsKey("id")){
			out = Commands.make(out, Collections.singletonMap("id", String.valueOf(id)));
		}
		else{
			out = Commands.make(out, Collections.singletonMap("serverid", String.valueOf(id)));
		}
		outputToHandler(handler, out.toString(), blocking);
	}

	/*
	 * Commands:
	 * 
	 * initialize: sent by server to client upon connection
	 *   targetid (id): assigns the client's id to targetid
	 * 
	 * Arguments:
	 * 
	 * serverid (id): the id of the server, if it's not the original sender
	 * id (id): the id of the original sender
	 * recipients (recipients): sends message only to recipients with the ids specified, separated by /
	 * serveronly (no args): sends message to server only (superseded by recipients)
	 * 
	 * Permissions:
	 * recipients (server): allows the client to use the recipient argument
	 * serveronly (server): allows the client to use the serveronly argument
	 * 
	 */
	@Override
	public void input(String in) {
		Command command = Commands.parseCommand(in);
		if(systemProcess(command)){
			process(command);
		}
	}
	
	protected boolean systemProcess(Command command){
		if(isServer){
			long sender;
			try{
				sender = Long.parseLong(command.getArguments().get("id"));
			}
			catch(NumberFormatException e){
				sender = 0;
			}
			//Argument handling  
			String recipients = command.getArguments().get("recipients");
			if(permission(sender, "recipients") && recipients != null){
				String[] recipientArray = recipients.split("/");
				boolean forward = false;
				for(String recipient : recipientArray){
					if(recipient.equals(String.valueOf(id))){
						forward = true;
					}
					else{
						try {
							outputToHandler(ids.get(Long.parseLong(recipient)), command, false);
						} catch (IOException e) {
							// TODO Auto-generated catch block
							e.printStackTrace();
						} catch (NumberFormatException e){
							// TODO Auto-generated catch block
							e.printStackTrace();
						}
					}
				}
				if(!forward){
					return false;
				}
			}
			else if(!permission(sender, "serveronly") || !command.getArguments().containsKey("serveronly")){
				for(Entry<Long, SocketHandler<String>> handler : ids.entrySet()){
					String id = command.getArguments().get("id");
					try {
						if(id == null || handler.getKey() != Long.parseLong(id)){
							outputToHandler(handler.getValue(), command, false);
						}
					} catch (IOException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					} catch (NumberFormatException e){
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
				}
			}
			return true;
		}
		else{
			if(command.getName().equalsIgnoreCase("initialize")){
				try {
					serverID = Long.parseLong(command.getArguments().get("id"));
					ids.put(serverID, ids.remove(0));
				} catch (NumberFormatException e){
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
				try {
					id = Long.parseLong(command.getArguments().get("targetid"));
					synchronized(assignmentLock){
						assigned = true;
						assignmentLock.notifyAll();
					}
				} catch (NumberFormatException e){
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
		}
		return true;
	}
	
	public Map<Long, SocketHandler<String>> getHandlersWithIDs(){
		return Collections.unmodifiableMap(ids);
	}
	
	public long getID(){
		return id;
	}
	
	public long getServerID(){
		return serverID;
	}
	
	public boolean idAssigned(){
		synchronized(assignmentLock){
			return assigned;
		}
	}
	
	protected Object getAssignmentLock(){
		return assignmentLock;
	}
	
	public boolean isServer(){
		return isServer;
	}
	
	protected void waitForID(){
		synchronized(assignmentLock){
			while(!idAssigned()){
				try {
					assignmentLock.wait();
				} catch (InterruptedException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
		}
	}
	
	protected abstract Map<String, String> handlerAdded(Future<Long> addedID, SocketHandler<String> handler);
	protected abstract void handlerRemoved(long removedID, SocketHandler<String> handler);
	protected abstract void process(Command command);
	protected abstract boolean permission(long requester, String permission);
	
	@Override
	public void close(){
		super.close();
		synchronized(assignmentLock){
			assignmentLock.notifyAll();
		}
	}

}
