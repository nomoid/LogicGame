package com.markusfeng.logicgame.ui;

import org.newdawn.slick.GameContainer;
import org.newdawn.slick.Graphics;
import org.newdawn.slick.SlickException;

import com.markusfeng.logicgame.CollisionRect;

public class UICollisionRect extends CollisionRect implements UICollisionElement{

	protected boolean visible = true;

	public UICollisionRect(int x, int y, int width, int height){
		super(x, y, width, height);
	}

	@Override
	public void collisionAction(int x, int y){
		// Do nothing
	}

	@Override
	public void render(GameContainer gc, Graphics g) throws SlickException{
		throw new SlickException("Cannot render UICollisionRect");
	}

	@Override
	public void update(GameContainer g, int delta) throws SlickException{
		// Do nothing
	}

	@Override
	public boolean isVisible(){
		return visible;
	}
}
