package com.markusfeng.logicgame.multiplayer;

import java.io.Closeable;
import java.io.IOException;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import com.markusfeng.SocketRelay.A.SocketClient;
import com.markusfeng.SocketRelay.A.SocketHandler;
import com.markusfeng.SocketRelay.A.SocketServer;
import com.markusfeng.SocketRelay.B.SocketProcessorGenerator;
import com.markusfeng.SocketRelay.C.SocketHelper;
import com.markusfeng.SocketRelay.Compatibility.Function;

public class RemoteMethodMessageProcessor extends RemoteMethodProcessor 
		implements SocketProcessorGenerator<RemoteMethodProcessor>{
	
	static final boolean VERBOSE = false;
	
	static Random random = new Random();
	static final int MIN_PORT = 10000;
	static final int MAX_PORT = 60000;
	
	static Set<Closeable> closeables = new HashSet<Closeable>();
	static boolean done = false;
	
	static int randomPort(){
		return random.nextInt(MAX_PORT - MIN_PORT) + MIN_PORT;
	}
	

	@SuppressWarnings("unused")
	public static void main(String[] args){
		try{
			int port = randomPort();
			final RemoteMethodMessageProcessor server = startServer(port);
			final RemoteMethodMessageProcessor client = startClient(new String[]{""}, port);
			final RemoteMethodMessageProcessor client2 = startClient(new String[]{""}, port);
			new Thread(new Runnable(){
				
				@Override
				public void run() {
					try {
						//client.waitForID();
						Thread.sleep(1000);
						Future<Map<Long, CompletableFuture<String>>> future = 
								server.invokeMethod("ping", Collections.singletonMap("pingmessage", "helloworld"));
						Map<Long, CompletableFuture<String>> map = future.get();
						System.out.println("invocations gotten: " + map);
						/*for(Map.Entry<Long, CompletableFuture<String>> entry : map.entrySet()){
							System.out.println("invocation returned: " + entry.getKey() + "," + entry.getValue().get());
						}*/
						CompletableFuture<Map<Long, Void>> completed = runAsynchronously(Executors.newCachedThreadPool(), map, new Function<Map.Entry<Long, String>, Void>(){

							@Override
							public Void apply(Map.Entry<Long, String> entry) {
								System.out.println("invocation returned: " + entry.getKey() + "," + entry.getValue());
								return null;
							}
							
						});
						completed.get();
						System.out.println("invocation complete");
					} catch (Exception e){
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
				}
				
			}).start();
			/*new Thread(new Runnable(){
				
				@Override
				public void run() {
					try {
						while(!done){
							Object lock = client2.getAssignmentLock();
							synchronized(lock){
								while(!client2.idAssigned()){
									lock.wait();
								}
							}
							client2.output(Commands.make("pong", Collections.singletonMap("value", String.valueOf(random.nextLong()))), false);
							Thread.sleep(2500);
						}
					} catch (Exception e){
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
				}
				
			}).start();*/
			Thread.sleep(5000);
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		finally{
			for(Closeable closeable : closeables){
				try{
					closeable.close();
				}
				catch(Exception e){
					e.printStackTrace();
				}
			}
			done = true;
		}
		try {
			Thread.sleep(1000);
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	static RemoteMethodMessageProcessor startServer(int port){
		RemoteMethodMessageProcessor mp = new RemoteMethodMessageProcessor(true);
		try{
			SocketServer<SocketHandler<String>> server =
					SocketHelper.getStringServer(port, mp);
			closeables.add(server);
			server.open();
		}
		catch(NumberFormatException e){
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		catch(IOException e){
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return mp;
	}
	
	static RemoteMethodMessageProcessor startClient(String[] field, int portZ){
		RemoteMethodMessageProcessor mp = new RemoteMethodMessageProcessor(false);
		try{
			//String[] field = mpm.getField().split(":");
			String host = field.length < 1 ? "localhost" : field[0];
			int port = field.length < 2 ? portZ : Integer.parseInt(field[1]);
			SocketClient<SocketHandler<String>> client =
					SocketHelper.getStringClient(host, port, mp);
			closeables.add(client);
			client.open();
		}
		catch(NumberFormatException e){
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		catch(IOException e){
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return mp;
	}

	public RemoteMethodMessageProcessor(boolean isServer) {
		super(isServer);
		addMethod("ping", new RemoteMethod(){

			@Override
			public String apply(Map<String, String> parameters) {
				long randTime = random.nextInt(2000) + 2000;
				System.out.println(getID() + ": ping recieved (" + randTime + "): " + parameters.get("pingmessage"));
				try {
					Thread.sleep(randTime);
				} catch (InterruptedException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
				System.out.println(getID() + ": ping back (" + randTime + "): " + parameters.get("pingmessage"));
				return parameters.get("pingmessage");
			}
			
		});
	}

	@Override
	protected Map<String, String> handlerAdded(final Future<Long> addedID, SocketHandler<String> handler) {
		if(VERBOSE){
			executor().execute(new Runnable(){
				
				@Override
				public void run(){
					try {
						long added = addedID.get();
						System.out.println(getID() + ": Handler added with id: " + added);
					} catch (InterruptedException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					} catch (ExecutionException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
				}
				
			});
		}
		return new HashMap<String, String>();
	}

	@Override
	protected void handlerRemoved(long addedID, SocketHandler<String> handler) {
		if(VERBOSE){
			System.out.println(getID() + ": Handler removed with id: " + addedID);
		}
	}
	
	@Override
	protected void process(Command command) {
		if(VERBOSE){
			System.out.println(getID() + ": Command recieved: " + command.getName() + " with arguments:" + command.getArguments());
		}
	}
	
	@Override
	public RemoteMethodProcessor get() {
		return this;
	}

}
