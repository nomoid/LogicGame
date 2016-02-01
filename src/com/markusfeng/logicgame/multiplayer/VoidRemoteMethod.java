package com.markusfeng.logicgame.multiplayer;

/**
 * A method that can be remotely invoked, with no return value.
 * Does not send back a completion notification to the sender.
 * If a completion notification is desired, use a RemoteMethod
 * that returns and empty String instead.
 * 
 * Can be created by using the RemoteMethods.fromConsumer function.
 * 
 * @author Markus Feng
 */
public interface VoidRemoteMethod extends RemoteMethod{

}
