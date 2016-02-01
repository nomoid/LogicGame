package com.markusfeng.logicgame.multiplayer;

import java.io.IOException;
import java.util.AbstractMap;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import com.markusfeng.SocketRelay.Compatibility.Function;

public abstract class RemoteMethodGroupProcessor extends GroupProcessor{
	
	protected Map<String, RemoteMethod> methods;
	protected Map<Long, CompletableFuture<Map<Long, CompletableFuture<String>>>> invocations;
	protected Map<Long, Map<Long, CompletableFuture<String>>> invocationReturns;

	public RemoteMethodGroupProcessor(boolean isServer) {
		super(isServer);
		methods = new HashMap<String, RemoteMethod>();
		invocations = new HashMap<Long, CompletableFuture<Map<Long, CompletableFuture<String>>>>();
		invocationReturns = new HashMap<Long, Map<Long, CompletableFuture<String>>>();
	}

	/*
	 * Commands (in addition to the ones in GroupProcessor):
	 * 
	 * invoke: sent when a method invocation is warranted (use recipients to control invocation targets)
	 *   methodname (string): the name of the method to be invoked
	 *   multithread (marker): a separate thread should be created for the invocation
	 *   invokeid (id): the id of the invocation (to match with the invoke return)
	 *   
	 * invokedata: sent by a server to a client when the client invokes the messages
	 *   targets (id...): the invocation targets of the invocation //maybe encrypt with proxy later? [array split with /]
	 *   
	 * invokereturn: sent by the system when an invocation is completed (returns all original arguments)
	 *   callerid (id): the id of the original method caller
	 *   return (string): the return value of the method invocation
	 *   accessdenied (marker): if the access is denied
	 *   exceptionthrown (marker): if an exception was thrown in the invocation
	 * 
	 * Arguments:
	 * 
	 * Permissions:
	 * invoke (any): allows the client to invoke any method
	 * invoke.* (any): allows the client to invoke a specific method
	 * invokereturn (any): allows the client to return the invocation
	 * invokedata (any): allows the client to send back invocation data
	 * 
	 */
	@Override
	public boolean systemProcess(Command command) {
		long sender = 0;
		try{
			sender = Long.parseLong(command.getArguments().get("id"));
		}
		catch(NumberFormatException e){
			e.printStackTrace();
		}
		if(isServer && !command.getName().equalsIgnoreCase("invokereturn")){
			if(permission(sender, "invoke") && command.getName().equalsIgnoreCase("invoke")){
				//Return the invocation data
				Map<String, String> map = new HashMap<String, String>(command.getArguments());
				map.put("targets", getInvocationTargetString(command));
				if(sender == id){
					invokeData(Commands.make("invokedata", map));
				}
				else{
					try {
						outputToHandler(ids.get(sender), Commands.make("invokedata", map), false);
					} catch (IOException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
				}
			}
			//Redirect as necessary
			if(!super.systemProcess(command)){
				return false;
			}
		}
		if(permission(sender, "invoke") && command.getName().equalsIgnoreCase("invoke")){
			if(isServer && sender == id){
				return true;
			}
			String method = command.getArguments().get("methodname");
			boolean multiThread = command.getArguments().containsKey("multithread");
			if(permission(sender, "invoke." + method) && methods.containsKey(method)){
				final Command commandC = command;
				final long senderC = sender;
				if(multiThread){
					tpe.execute(new Runnable(){
						public void run(){
							Command c = applyInvoke(commandC);
							if(c != null){
								try {
									outputToHandler(ids.get(senderC), c, false);
								} catch (IOException e) {
									// TODO Auto-generated catch block
									e.printStackTrace();
								}
							}
						}
					});
				}
				else{
					Command c = applyInvoke(command);
					if(c != null){
						try {
							outputToHandler(ids.get(sender), c, false);
						} catch (IOException e) {
							// TODO Auto-generated catch block
							e.printStackTrace();
						}
					}
				}
			}
			else{
				Map<String, String> data = new HashMap<String, String>(command.getArguments());
				data.put("callerid", command.getArguments().get("id"));
				data.put("id", String.valueOf(id));
				data.put("accessdenied", "");
				try {
					outputToHandler(ids.get(sender), Commands.make("invokereturn", data), false);
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
		}
		else if(permission(sender, "invokedata") && command.getName().equalsIgnoreCase("invokedata")){
			invokeData(command);
		}
		else if(permission(sender, "invokereturn") && command.getName().equalsIgnoreCase("invokereturn")){
			try{
				long callerID = Long.parseLong(command.getArguments().get("callerid"));
				if(callerID == id){
					long invokeID = Long.parseLong(command.getArguments().get("invokeid"));
					CompletableFuture<String> future = invocationReturns.get(invokeID).get(sender);
					if(command.getArguments().get("accessdenied") != null){
						future.completeExceptionally(new SecurityException("Illegal remove invocation"));
					}
					else if(command.getArguments().get("exceptionthrown") != null){
						future.completeExceptionally(new CompletionException(command.getArguments()
								.get("exceptionthrown"), null));
					}
					else{
						future.complete(command.getArguments().get("return"));
					}
				}
				else if(isServer){
					//Redirect
					try {
						outputToHandler(ids.get(callerID), command, false);
					} catch (IOException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
				}
			} catch(NullPointerException e){
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch(NumberFormatException e){
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
		if(isServer){
			return true;
		}
		else{
			return super.systemProcess(command);
		}
	}

	private String getInvocationTargetString(Command command){
		Set<Long> invocationTargets = new HashSet<Long>();
		String recipients = command.getArguments().get("recipients");
		if(recipients == null){
			boolean fail = false;
			invocationTargets.add(id);
			invocationTargets.addAll(ids.keySet());
			try{
				invocationTargets.add(Long.parseLong(command.getArguments().get("id")));
			}
			catch(NumberFormatException e){
				e.printStackTrace();
				fail = true;
			}
			StringBuilder sb = new StringBuilder();
			boolean first = true;
			for(long l : invocationTargets){
				if(first){
					first = false;
				}
				else{
					sb.append("/");
				}
				sb.append(l);
			}
			if(fail){
				if(invocationTargets.size() != 0){
					sb.append("/");
				}
				sb.append(command.getArguments().get("id"));
			}
			return sb.toString();
		}
		else{
			return recipients.length() == 0 ? command.getArguments().get("id") : 
				recipients + "/" + command.getArguments().get("id");
		}
	}
	
	void invokeData(Command command){
		Map<Long, CompletableFuture<String>> returnMap = new HashMap<Long, CompletableFuture<String>>();
		String targets = command.getArguments().get("targets");
		String[] targetArray = targets.split("/"); 
		for(String target : targetArray){
			returnMap.put(Long.parseLong(target), new CompletableFuture<String>());
		}
		try{
			long invokeID = Long.parseLong(command.getArguments().get("invokeid"));
			invocationReturns.put(invokeID, returnMap);
			CompletableFuture<Map<Long, CompletableFuture<String>>> future = 
					invocations.get(invokeID);
			future.complete(returnMap);
		}
		catch(NumberFormatException e){
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	public Command applyInvoke(Command command){
		String method = command.getArguments().get("methodname");
		long sender = 0;
		try{
			sender = Long.parseLong(command.getArguments().get("id"));
		}
		catch(NumberFormatException e){
			e.printStackTrace();
		}
		RemoteMethod remote = methods.get(method);
		String returnValue;
		try{
			returnValue = remote.apply(command.getArguments());
		}
		catch(Exception e){
			Map<String, String> data = new HashMap<String, String>(command.getArguments());
			data.put("callerid", String.valueOf(sender));
			data.put("id", String.valueOf(id));
			data.put("exceptionthrown", e.toString());
			return Commands.make("invokereturn", data);
		}
		Map<String, String> data = new HashMap<String, String>(command.getArguments());
		data.put("callerid", String.valueOf(sender));
		data.put("id", String.valueOf(id));
		if(returnValue != null){
			data.put("return", returnValue);
		}
		if(!(remote instanceof VoidRemoteMethod)){
			return Commands.make("invokereturn", data);
		}
		return null;
	}
	
	/*
	 * Makes a CompletableFuture of Long -> CompletableFuture
	 * As the server returns the "got invocation call", the list of longs that will be invoked comes back
	 * This makes a mapping that is completed; The mapping is put into invocationReturns
	 * As each invocation comes back, each CompletableFuture is completed individually
	 */
	public Future<Map<Long, CompletableFuture<String>>> invokeMethod(String name, Map<String, String> args){
		waitForID();
		final Map<String, String> data = new HashMap<String, String>(args);
		final CompletableFuture<Map<Long, CompletableFuture<String>>> future = 
				new CompletableFuture<Map<Long, CompletableFuture<String>>>();
		tpe.execute(new Runnable(){
			public void run(){
				try {
					//Wait until invoke data comes back
					future.get();
					//Local copy of invocation
					data.put("id", String.valueOf(id));
					Command c = applyInvoke(Commands.make("invoke", data));
					if(c != null){
						systemProcess(c);
					}
				} catch (InterruptedException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				} catch (ExecutionException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
		});
		long invokeID = random.nextLong();
		while(invocations.containsKey(invokeID)){
			invokeID = random.nextLong();
		}
		data.put("methodname", name);
		data.put("invokeid", String.valueOf(invokeID));
		invocations.put(invokeID, future);
		if(isServer){
			//Send data to everyone
			data.put("id", String.valueOf(id));
			systemProcess(Commands.make("invoke", data));
		}
		else{
			//Send data to server
			try {
				outputToHandler(ids.get(serverID), Commands.make("invoke", data), false);
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
		return future;
	}
	
	
	@Override
	protected boolean permission(long requester, String permission) {
		//By default, all permissions are granted
		return true;
	}
	
	public static <T, S, R> CompletableFuture<Map<S, R>> runAsynchronously(Map<S, CompletableFuture<T>> map, final Function<Map.Entry<S, T>, R> function){
		final CompletableFuture<Map<S, R>> future = new CompletableFuture<Map<S, R>>();
		final ExecutorService exec = Executors.newCachedThreadPool();
		final Set<Callable<Map.Entry<S, R>>> callableSet = new HashSet<Callable<Map.Entry<S, R>>>();
		for(final Map.Entry<S, CompletableFuture<T>> entry : map.entrySet()){
			callableSet.add(new Callable<Map.Entry<S, R>>(){

				@Override
				public Map.Entry<S, R> call() throws Exception {
					T value = entry.getValue().get();
					return new AbstractMap.SimpleImmutableEntry<S, R>(entry.getKey(), 
							function.apply(new AbstractMap.SimpleImmutableEntry<S, T>(entry.getKey(), value)));
				}
				
			});
		}
		exec.execute(new Runnable(){

			@Override
			public void run() {
				try {
					Map<S, R> map = new HashMap<S, R>();
					List<Future<Map.Entry<S, R>>> list = exec.invokeAll(callableSet);
					for(Future<Map.Entry<S, R>> f : list){
						try{
							Map.Entry<S, R> entry = f.get();
							map.put(entry.getKey(), entry.getValue());
						}
						catch (ExecutionException e) {
							// TODO Auto-generated catch block
							e.printStackTrace();
						}
					}
					future.complete(map);
				} catch (InterruptedException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
			
		});
		return future;
	}
}
