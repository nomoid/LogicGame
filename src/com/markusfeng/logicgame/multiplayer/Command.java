package com.markusfeng.logicgame.multiplayer;

import java.util.Map;

public interface Command{
	String getName();
	Map<String, String> getArguments();
}
