package com.markusfeng.logicgame.multiplayer;

import java.io.Closeable;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
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
		this.game = game;
		addMethod("flip", new RemoteMethod(){

			@Override
			public String apply(Map<String, String> parameters) {
				int index = Integer.parseInt(parameters.get("index"));
				boolean[] faceUp = LogicGameProcessor.this.game.getFaceUp();
				faceUp[index] = !faceUp[index];
				return "complete";
			}
			
		});
	}

	@Override
	protected Map<String, String> handlerAdded(Future<Long> addedID, SocketHandler<String> handler) {
		//Send the cards in the logic game to all clients
		HashMap<String, String> data = new HashMap<String, String>();
		data.put("carddata", Commands.fromArray(game.getCards()));
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
			System.arraycopy(array, 0, game.getCards(), 0, game.getCards().length);
		}
	}
	
	@Override
	public LogicGameProcessor get() {
		return this;
	}

}
