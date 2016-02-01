package com.markusfeng.logicgame.multiplayer;

import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

public final class Commands{

	private Commands(){
		//Do nothing
	}
	
	public static Command make(Command command, Map<String, String> add){
		Map<String, String> data = new HashMap<String, String>(command.getArguments());
		data.putAll(add);
		return make(command.getName(), data);
	}

	public static Command make(String name){
		Map<String, String> empty = Collections.emptyMap();
		return make(name, empty);
	}

	public static Command make(String name, String... argumentData){
		if(argumentData.length % 2 != 0){
			throw new IllegalArgumentException("Illegal command arguments!");
		}
		Map<String, String> data = new LinkedHashMap<String, String>();
		for(int i = 0; i < argumentData.length; i += 2){
			data.put(argumentData[i], argumentData[i + 1]);
		}
		return make(name, data);
	}

	public static Command make(String name, Map<String, String> data){
		return new Generic(name, data);
	}

	private static final boolean ALLOW_EMPTY_NAME = false;

	protected static class Generic implements Command{

		protected Map<String, String> args;
		protected String name;

		protected Generic(String name, Map<String, String> args){
			if(!ALLOW_EMPTY_NAME && (name == null || name.length() == 0)){
				throw new IllegalArgumentException("Name cannot be empty");
			}
			this.name = name;
			this.args = new LinkedHashMap<String, String>(args);
		}

		@Override
		public String getName(){
			return name;
		}

		@Override
		public Map<String, String> getArguments(){
			return Collections.unmodifiableMap(args);
		}

		@Override
		public String toString(){
			return Parser.commandToString(this);
		}

		@Override
		public int hashCode(){
			return getName().hashCode() ^ getArguments().hashCode();
		}

		@Override
		public boolean equals(Object o){
			if(o instanceof Command){
				Command c = (Command) o;
				if(getName().equals(c.getName()) &&
						getArguments().equals(c.getArguments())){
					return true;
				}
			}
			return false;
		}
	}


	public static Command parseCommand(String in){
		/*
		 * Command format
		 * "[" name (";" argument "=" value)* (";")? "]"
		 *
		 *
		 */

		return Parser.parseCommand(in, "[", "]", ";", "=", "\\");
	}


	public static String fromArray(int[] ia){
		StringBuilder sb = new StringBuilder();
		boolean first = true;
		for(int i : ia){
			if(first){
				first = false;
			}
			else{
				sb.append("/");
			}
			sb.append(i);
		}
		return sb.toString();
	}

	public static String fromArray(long[] ia){
		StringBuilder sb = new StringBuilder();
		boolean first = true;
		for(long i : ia){
			if(first){
				first = false;
			}
			else{
				sb.append("/");
			}
			sb.append(i);
		}
		return sb.toString();
	}

	public static String fromIterable(Iterable<?> iter){
		StringBuilder sb = new StringBuilder();
		boolean first = true;
		for(Object obj : iter){
			if(first){
				first = false;
			}
			else{
				sb.append("/");
			}
			sb.append(obj);
		}
		return sb.toString();
	}

	public static int[] toIntArray(String s) throws NumberFormatException{
		String[] arr = s.split("/");
		int[] ia = new int[arr.length];
		for(int i = 0; i < arr.length; i++){
			ia[i] = Integer.parseInt(arr[i]);
		}
		return ia;
	}

	public static long[] toLongArray(String s) throws NumberFormatException{
		String[] arr = s.split("/");
		long[] ia = new long[arr.length];
		for(int i = 0; i < arr.length; i++){
			ia[i] = Long.parseLong(arr[i]);
		}
		return ia;
	}

	protected static final class Parser{

		private Parser(){
			//Do nothing
		}

		protected static Command parseCommand(String in, String init, String fin,
				String entrySeparator, String keyValueSeparator, String escape){
			if(!(in.startsWith(init) && in.endsWith(fin))){
				throw new IllegalArgumentException("Illegal command.");
			}
			Map<String, String> hm = new LinkedHashMap<String, String>();
			if(in.length() == 0){
				return make("");
			}
			String name = null;
			in = in.substring(0, in.length() - fin.length());
			in = in.substring(init.length());
			//String[] sa = s.split("(?<!\\\\)\n");
			String[] sa = in.split("(?<=([^\\\\])(\\\\\\\\){0," + in.length()/2 + "});");
			for(String sn : sa){
				if(sn.length() == 0){
					continue;
				}
				if(name == null){
					name = sn;
					continue;
				}
				//String[] sna = sn.split("(?<!\\\\)=");
				String[] sna = sn.split("(?<=([^\\\\])(\\\\\\\\){0," + sn.length()/2 + "})=");
				String object;
				if(sna.length > 1){
					object = commandUnescape(sna[1]);
				}
				else{
					object = "";
				}
				String meta = commandUnescape(sna[0]);
				hm.put(meta, object);
			}
			return make(name, hm);
		}

		private static String commandUnescape(String in){
			in = in.replaceAll("(?<=(\\\\\\\\){0," + in.length()/2 + "})\\\\;", ";");
			in = in.replaceAll("(?<=(\\\\\\\\){0," + in.length()/2 + "})\\\\=", "=");
			in = in.replace("\\\\", "\\");
			return in;
		}

		public static String commandToString(Command command){
			String s = "[";
			s += command.getName();
			for(Map.Entry<String, String> entry : command.getArguments().entrySet()){
				s += ";" + commandEscape(entry.getKey().toString()) + "=" + commandEscape(entry.getValue().toString());
			}
			s += "]";
			return s;
		}

		private static String commandEscape(String in){
			in = in.replace("\\", "\\\\");
			in = in.replace("=", "\\=");
			in = in.replace(";", "\\;");
			return in;
		}
	}
}
