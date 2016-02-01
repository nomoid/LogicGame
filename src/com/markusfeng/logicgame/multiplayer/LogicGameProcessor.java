package com.markusfeng.logicgame.multiplayer;

import java.io.Closeable;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

import com.markusfeng.SocketRelay.A.SocketClient;
import com.markusfeng.SocketRelay.A.SocketHandler;
import com.markusfeng.SocketRelay.A.SocketServer;
import com.markusfeng.SocketRelay.B.SocketProcessorGenerator;
import com.markusfeng.SocketRelay.C.SocketHelper;
import com.markusfeng.logicgame.LogicGame;

public class LogicGameProcessor extends RemoteMethodGroupProcessor
		implements SocketProcessorGenerator<LogicGameProcessor>{
	
	protected LogicGame game;
	protected ArrayList<Long> players;
	
	public static LogicGameProcessor startServer(LogicGame game, int port, Set<Closeable> closeables)
			throws IOException{
		LogicGameProcessor mp = new LogicGameProcessor(game, true);
		SocketServer<SocketHandler<String>> server =
				SocketHelper.getStringServer(port, mp);
		closeables.add(server);
		server.open();
		return mp;
	}
	
	public static LogicGameProcessor startClient(LogicGame game, String host, int port, Set<Closeable> closeables)
			throws IOException{
		LogicGameProcessor mp = new LogicGameProcessor(game, false);
		SocketClient<SocketHandler<String>> client =
				SocketHelper.getStringClient(host, port, mp);
		closeables.add(client);
		client.open();
		return mp;
	}

	public LogicGameProcessor(LogicGame game, boolean isServer) {
		super(isServer);
		players = new ArrayList<Long>();
		this.game = game;
		if(isServer){
			players.add(getID());
		}
		addMethod("flip", new RemoteMethod(){

			@Override
			public String apply(Map<String, String> parameters) {
				int index = Integer.parseInt(parameters.get("index"));
				LogicGame game = LogicGameProcessor.this.game;
				return game.flip(index);
			}
			
		});
	}

	@Override
	protected Map<String, String> handlerAdded(Future<Long> addedID, SocketHandler<String> handler) {
		//Send the cards in the logic game to all clients
		HashMap<String, String> data = new HashMap<String, String>();
		if(isServer){
			data.put("carddata", Commands.fromArray(game.getCards()));
			synchronized(players){
				data.put("playernumber", String.valueOf(players.size()));
				try {
					players.add(addedID.get());
				} catch (InterruptedException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				} catch (ExecutionException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
		}
		return data;
	}

	@Override
	protected void handlerRemoved(long removedID, SocketHandler<String> handler) {
		// TODO Auto-generated method stub
		
	}

	@Override
	protected void process(Command command) {
		if(command.getName().equalsIgnoreCase("initialize")){
			int[] array = Commands.toIntArray(command.getArguments().get("carddata"));
			int playerNumber = Integer.parseInt(command.getArguments().get("playernumber"));
			game.setCardDataRecieved(array, playerNumber);
		}
	}
	
	@Override
	public LogicGameProcessor get() {
		return this;
	}

}
