package com.markusfeng.logicgame;

import org.newdawn.slick.GameContainer;
import org.newdawn.slick.Graphics;
import org.newdawn.slick.SlickException;

public interface Screen{
	void update(GameContainer gc, int delta) throws SlickException;

	void render(GameContainer gc, Graphics g) throws SlickException;

	void init(GameContainer gc) throws SlickException;

	void mouseClicked(int button, int x, int y, int buttonCount);

	void keyPressed(int key, char c);

	ScreenManager getParent();

	String getName();
}
