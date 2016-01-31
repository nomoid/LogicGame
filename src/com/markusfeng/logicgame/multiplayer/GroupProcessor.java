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

import com.markusfeng.SocketRelay.A.SocketHandler;
import com.markusfeng.SocketRelay.C.SocketProcessorAbstract;

public abstract class GroupProcessor extends SocketProcessorAbstract<String>{
	
	protected Random random;
	protected boolean isServer;
	protected Map<Long, SocketHandler<String>> ids;
	protected long id;
	
	public GroupProcessor(boolean isServer){
		this.isServer = isServer;
		random = new Random();
		ids = new HashMap<Long, SocketHandler<String>>();
		id = random.nextLong();
	}
	
	@Override
	public void attachHandler(SocketHandler<String> handler){
		super.attachHandler(handler);
		if(isServer){
			long rand = random.nextLong();
			while(ids.containsKey(rand)){
				rand = random.nextLong();
			}
			Map<String, String> map = handlerAdded(rand, handler);
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
	}
	
	@Override
	public void removeHandler(SocketHandler<String> handler){
		super.removeHandler(handler);
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

	@Override
	public void attachHandlers(Collection<SocketHandler<String>> handlers){
		for(SocketHandler<String> handler : handlers){
			attachHandler(handler);
		}
	}

	@Override
	public void removeHandlers(Collection<SocketHandler<String>> handlers){
		super.removeHandlers(handlers);
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

	@Override
	public void removeAllHandlers(){
		removeHandlers(getHandlers());
	}
	
	public void output(Command out, boolean blocking) throws IOException{
		if(!out.getArguments().containsKey("id")){
			out = Commands.make(out, Collections.singletonMap("id", String.valueOf(id)));
		}
		output(out.toString(), blocking);
	}

	public void outputToHandler(SocketHandler<String> handler, Command out, boolean blocking) throws IOException{
		if(!out.getArguments().containsKey("id")){
			out = Commands.make(out, Collections.singletonMap("id", String.valueOf(id)));
		}
		outputToHandler(handler, out.toString(), blocking);
	}

	@Override
	public void input(String in) {
		Command command = Commands.parseCommand(in);
		if(isServer){
			String recipient = command.getArguments().get("recipient");
			if(recipient != null){
				try {
					outputToHandler(ids.get(recipient), command, false);
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
			else if(!command.getArguments().containsKey("serveronly")){
				for(Entry<Long, SocketHandler<String>> handler : ids.entrySet()){
					String id = command.getArguments().get("id");
					if(id == null || handler.getKey() != Long.parseLong(id)){
						try {
							outputToHandler(handler.getValue(), command, false);
						} catch (IOException e) {
							// TODO Auto-generated catch block
							e.printStackTrace();
						}
					}
				}
			}
		}
		else{
			if(command.getName().equalsIgnoreCase("initialize")){
				id = Long.parseLong(command.getArguments().get("targetid"));
			}
		}
		process(command);
	}
	
	public Map<Long, SocketHandler<String>> getHandlersWithIDs(){
		return Collections.unmodifiableMap(ids);
	}
	
	protected abstract Map<String, String> handlerAdded(long id, SocketHandler<String> handler);
	protected abstract void handlerRemoved(long id, SocketHandler<String> handler);
	protected abstract void process(Command command);

}
