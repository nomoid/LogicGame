package com.markusfeng.logicgame;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.newdawn.slick.BasicGame;
import org.newdawn.slick.GameContainer;
import org.newdawn.slick.Graphics;
import org.newdawn.slick.SlickException;

public class ScreenManager extends BasicGame{

	private Set<String> initialized = new HashSet<String>();
	private Map<String, Screen> screenMap = new HashMap<String, Screen>();
	private Screen currentScreen;
	private boolean rendered = false;

	public ScreenManager(){
		super("Logic");
		addScreen(new LogicGame(this));
		Screen mainMenu = new MainMenu(this);
		setScreen(mainMenu);
	}

	@Override
	public void render(GameContainer container, Graphics g) throws SlickException{
		currentScreen.render(container, g);
		rendered = true;
	}

	@Override
	public void init(GameContainer container) throws SlickException{
		if(!initialized.contains(currentScreen.getName())){
			initialized.add(currentScreen.getName());
			currentScreen.init(container);
		}
	}

	@Override
	public void update(GameContainer container, int delta) throws SlickException{
		if(!rendered){
			return;
		}
		if(!initialized.contains(currentScreen.getName())){
			initialized.add(currentScreen.getName());
			currentScreen.init(container);
		}
		currentScreen.update(container, delta);
	}

	@Override
	public void mouseClicked(int button, int x, int y, int buttonCount){
		currentScreen.mouseClicked(button, x, y, buttonCount);
	}

	@Override
	public void keyPressed(int key, char c){
		currentScreen.keyPressed(key, c);
	}

	public void addScreen(Screen screen){
		screenMap.put(screen.getName(), screen);
	}

	public void removeScreen(Screen screen){
		screenMap.remove(screen.getName());
	}

	public void setScreen(Screen screen){
		if(!screenMap.containsKey(screen.getName())){
			addScreen(screen);
		}
		currentScreen = screen;
		rendered = false;
	}

	public Screen getScreen(String name){
		return screenMap.get(name);
	}
}
