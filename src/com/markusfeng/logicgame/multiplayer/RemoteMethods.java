package com.markusfeng.logicgame.multiplayer;

import java.util.Collections;
import java.util.Map;

import com.markusfeng.SocketRelay.Compatibility.Consumer;

public final class RemoteMethods {
	
	private RemoteMethods(){
		
	}
	
	public static VoidRemoteMethod fromConsumer(final Consumer<Map<String, String>> consumer){
		return new VoidRemoteMethod(){

			@Override
			public String apply(Map<String, String> socket) {
				consumer.accept(socket);
				return null;
			}
			
		};
	}
	
	public static Command serverOnly(Command command){
		return Commands.make(command, Collections.singletonMap("serveronly", ""));
	}
	
	public static Command recipients(Command command, long... recipients){
		StringBuilder sb = new StringBuilder();
		boolean first = true;
		for(long l : recipients){
			if(first){
				first = false;
			}
			else{
				sb.append("/");
			}
			sb.append(l);
		}
		return Commands.make(command, Collections.singletonMap("recipients", sb.toString()));
	}
}
