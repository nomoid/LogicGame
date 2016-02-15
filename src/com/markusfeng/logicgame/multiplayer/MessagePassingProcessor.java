package com.markusfeng.logicgame.multiplayer;

import java.io.Closeable;
import java.io.IOException;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

import com.markusfeng.SocketRelay.A.SocketClient;
import com.markusfeng.SocketRelay.A.SocketHandler;
import com.markusfeng.SocketRelay.A.SocketServer;
import com.markusfeng.SocketRelay.B.SocketProcessorGenerator;
import com.markusfeng.SocketRelay.C.SocketHelper;

public class MessagePassingProcessor extends GroupProcessor implements SocketProcessorGenerator<MessagePassingProcessor>{
	
	static Random random = new Random();
	static final int MIN_PORT = 10000;
	static final int MAX_PORT = 60000;
	
	static Set<Closeable> closeables = new HashSet<Closeable>();
	static boolean done = false;
	
	static int randomPort(){
		return random.nextInt(MAX_PORT - MIN_PORT) + MIN_PORT;
	}
	
	public static void main(String[] args){
		try{
			int port = randomPort();
			@SuppressWarnings("unused")
			final MessagePassingProcessor server = startServer(port);
			final MessagePassingProcessor client = startClient(new String[]{""}, port);
			final MessagePassingProcessor client2 = startClient(new String[]{""}, port);
			new Thread(new Runnable(){
				
				@Override
				public void run() {
					try {
						while(!done){
							client.waitForID();
							client.output(Commands.make("ping", Collections.singletonMap("value", String.valueOf(random.nextLong()))), false);
							Thread.sleep(1000);
						}
					} catch (Exception e){
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
				}
				
			}).start();
			new Thread(new Runnable(){
				
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
				
			}).start();
			Thread.sleep(10000);
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
	
	static MessagePassingProcessor startServer(int port){
		MessagePassingProcessor mp = new MessagePassingProcessor(true);
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
	
	static MessagePassingProcessor startClient(String[] field, int portZ){
		MessagePassingProcessor mp = new MessagePassingProcessor(false);
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

	public MessagePassingProcessor(boolean isServer) {
		super(isServer);
	}

	@Override
	protected Map<String, String> handlerAdded(final Future<Long> addedID, SocketHandler<String> handler) {
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
		return new HashMap<String, String>();
	}

	@Override
	protected void handlerRemoved(long addedID, SocketHandler<String> handler) {
		System.out.println(getID() + ": Handler removed with id: " + addedID);
	}
	
	@Override
	protected void process(Command command) {
		System.out.println(getID() + ": Command recieved: " + command.getName() + " with arguments:" + command.getArguments());
	}

	@Override
	public MessagePassingProcessor get() {
		return this;
	}

	@Override
	protected boolean permission(long requester, String permission) {
		return true;
	}
}
