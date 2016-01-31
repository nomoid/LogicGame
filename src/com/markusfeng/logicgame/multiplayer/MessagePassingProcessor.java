package com.markusfeng.logicgame.multiplayer;

import java.io.Closeable;
import java.io.IOException;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Random;
import java.util.Set;

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
			@SuppressWarnings("unused")
			final MessagePassingProcessor client2 = startClient(new String[]{""}, port);
			new Thread(new Runnable(){
				
				@Override
				public void run() {
					try {
						while(!done){
							client.output(Commands.make("ping", Collections.singletonMap("value", String.valueOf(random.nextLong()))), false);
							Thread.sleep(1000);
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
			closeables.add(mp);
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
			closeables.add(mp);
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
	protected Map<String, String> handlerAdded(long id, SocketHandler<String> handler) {
		System.out.println(id + ": Handler added with id: " + id);
		return new HashMap<String, String>();
	}

	@Override
	protected void handlerRemoved(long id, SocketHandler<String> handler) {
		System.out.println(id + ": Handler removed with id: " + id);
	}
	
	@Override
	protected void process(Command command) {
		System.out.println(id + ": Command recieved: " + command.getName() + " with arguments:" + command.getArguments());
	}

	@Override
	public MessagePassingProcessor get() {
		return this;
	}
}
