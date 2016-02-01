package com.markusfeng.logicgame.multiplayer;

import java.util.Map;

import com.markusfeng.SocketRelay.Compatibility.Function;

/**
 * A method that can be remotely invoked. 
 * The input of the function is the map of parameters, while the output
 * of the function is the return value.
 * 
 * @author Markus Feng
 */
public interface RemoteMethod extends Function<Map<String, String>, String>{
	
	@Override
	String apply(Map<String, String> parameters);
	
}
