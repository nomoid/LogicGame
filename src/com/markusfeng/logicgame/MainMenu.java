package com.markusfeng.logicgame;

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.newdawn.slick.Color;
import org.newdawn.slick.GameContainer;
import org.newdawn.slick.Graphics;
import org.newdawn.slick.SlickException;

public class MainMenu implements Screen{

	public static final int DEFAULT_PORT = 56860;
	public static final String NAME = "MainMenu";

	private static final String SINGLE_PLAYER = "single-player";
	private static final String HOST_GAME = "host-game";
	private static final String JOIN_GAME = "join-game";
	private static final String SERVER_LABEL = "server-label";
	private static final String PORT_LABEL = "port-label";
	private static final String BACK = "back";

	private static final List<String> MAIN_VISIBLE = Arrays.asList(SINGLE_PLAYER, HOST_GAME, JOIN_GAME);
	private static final List<String> JOIN_VISIBLE = Arrays.asList(SERVER_LABEL, BACK);
	private static final List<String> HOST_VISIBLE = Arrays.asList(PORT_LABEL, BACK);

	private Set<String> visible = new HashSet<String>();
	private ScreenManager manager;
	private CollisionRect singlePlayer;
	private CollisionRect hostGame;
	private CollisionRect joinGame;
	private CollisionRect continueRect;
	private CollisionRect back;
	private String serverLabel = "Server IP (default port: " + DEFAULT_PORT + "): ";
	private String portLabel = "Port (default: " + DEFAULT_PORT + "): ";

	private boolean initializationComplete = false;
	private boolean initializationStarted = false;

	private MainMenuState mainMenuState;

	private int currentPort = -1;
	private String currentAddress = "";

	public MainMenu(ScreenManager manager){
		this.manager = manager;
		transitionState(MainMenuState.MAIN);
	}

	@Override
	public void update(GameContainer gc, int delta) throws SlickException{
		//Initialization
		if(!initializationStarted){
			initializationStarted = true;
			((LogicGame) manager.getScreen(LogicGame.NAME)).initResources();
			initializationComplete = true;
		}
		//Currently all game state changes are based on
		//events so the update method has no state changes
	}

	@Override
	public void render(GameContainer gc, Graphics g) throws SlickException{
		if(!initializationComplete){
			Helper.drawLoadingScreen(gc, g);
			return;
		}
		g.setFont(Helper.getDefaultFont());
		g.setColor(Color.white);
		Helper.renderString(g, "Logic Game", gc.getWidth() / 2, gc.getHeight() / 4);
		if(visible.contains(SINGLE_PLAYER)){
			CollisionRect singlePlayerButton = Helper.renderButton(g, "Single Player", gc.getWidth() / 2,
					gc.getHeight() / 2, 200, 40, Color.green, Color.black);
			if(singlePlayer == null){
				singlePlayer = singlePlayerButton;
			}
		}
		if(visible.contains(HOST_GAME)){
			CollisionRect hostGameButton = Helper.renderButton(g, "Host Multiplayer", gc.getWidth() / 2,
					gc.getHeight() / 2 + 60, 200, 40, Color.green, Color.black);
			if(hostGame == null){
				hostGame = hostGameButton;
			}
		}
		if(visible.contains(JOIN_GAME)){
			CollisionRect joinGameButton = Helper.renderButton(g, "Join Multiplayer", gc.getWidth() / 2,
					gc.getHeight() / 2 + 120, 200, 40, Color.green, Color.black);
			if(joinGame == null){
				joinGame = joinGameButton;
			}
		}
		if(visible.contains(PORT_LABEL)){
			Helper.renderString(g, "Server Port: " + stringifyPort(), gc.getWidth() / 2, gc.getHeight() / 2);
		}
		if(visible.contains(SERVER_LABEL)){
			Helper.renderString(g, "Server Address: " + currentAddress, gc.getWidth() / 2, gc.getHeight() / 2);
		}
		if(visible.contains(BACK)){
			CollisionRect backButton = Helper.renderButton(g, "Back", gc.getWidth() / 2, gc.getHeight() / 2 + 180, 200,
					40, Color.green, Color.black);
			if(back == null){
				back = backButton;
			}
		}
	}

	private String stringifyPort(){
		if(currentPort < 0){
			return "";
		}
		else{
			return String.valueOf(currentPort);
		}
	}

	@Override
	public void init(GameContainer gc) throws SlickException{
		// TODO Auto-generated method stub

	}

	protected void transitionState(MainMenuState state){
		visible.clear();
		if(state == MainMenuState.MAIN){
			visible.addAll(MAIN_VISIBLE);
		}
		if(state == MainMenuState.MULTI_HOST){
			visible.addAll(HOST_VISIBLE);
		}
		if(state == MainMenuState.MULTI_JOIN){
			visible.addAll(JOIN_VISIBLE);
		}
		mainMenuState = state;
	}

	@Override
	public void mouseClicked(int button, int x, int y, int buttonCount){
		//Do not handle mouse events before initialization is complete
		if(!initializationComplete){
			return;
		}
		if(visible.contains(SINGLE_PLAYER) && singlePlayer.collidesWithPoint(x, y)){
			manager.setScreen(manager.getScreen(LogicGame.NAME));
		}
		if(visible.contains(HOST_GAME) && hostGame.collidesWithPoint(x, y)){
			transitionState(MainMenuState.MULTI_HOST);
		}
		if(visible.contains(JOIN_GAME) && joinGame.collidesWithPoint(x, y)){
			transitionState(MainMenuState.MULTI_JOIN);
		}
		if(visible.contains(BACK) && back.collidesWithPoint(x, y)){
			transitionState(MainMenuState.MAIN);
		}
	}

	@Override
	public void keyPressed(int key, char c){
		//Do not handle key events before initialization is complete
		if(!initializationComplete){
			return;
		}
	}

	@Override
	public ScreenManager getParent(){
		return manager;
	}

	@Override
	public String getName(){
		return NAME;
	}

	private enum MainMenuState{
		MAIN, MULTI_JOIN, MULTI_HOST;
	}
}
