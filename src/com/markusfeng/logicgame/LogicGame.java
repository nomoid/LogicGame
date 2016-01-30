package com.markusfeng.logicgame;

import org.newdawn.slick.AppGameContainer;
import org.newdawn.slick.BasicGame;
import org.newdawn.slick.GameContainer;
import org.newdawn.slick.Graphics;
import org.newdawn.slick.SlickException;

public class LogicGame extends BasicGame{

	public static void main(String[] args) throws SlickException{
		AppGameContainer app = new AppGameContainer(new LogicGame());
		app.setDisplayMode(800, 600, false);
		app.setTargetFrameRate(60);
		app.start();
	}
	
	public LogicGame() {
		super("Logic");
	}

	@Override
	public void render(GameContainer arg0, Graphics arg1) throws SlickException {
		
	}

	@Override
	public void init(GameContainer container) throws SlickException {
		
	}

	@Override
	public void update(GameContainer container, int delta) throws SlickException {
		
	}
}
