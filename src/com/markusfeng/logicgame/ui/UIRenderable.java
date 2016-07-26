package com.markusfeng.logicgame.ui;

import org.newdawn.slick.GameContainer;
import org.newdawn.slick.Graphics;
import org.newdawn.slick.SlickException;

public interface UIRenderable{
	void render(GameContainer gc, Graphics g) throws SlickException;

	boolean isVisible();
}
