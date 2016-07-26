package com.markusfeng.logicgame.ui;

import org.newdawn.slick.Color;
import org.newdawn.slick.GameContainer;
import org.newdawn.slick.Graphics;

import com.markusfeng.logicgame.CollisionRect;
import com.markusfeng.logicgame.Helper;

public class UIButton extends CollisionRect implements UIElement{

	public static final Color DEFAULT_BACKGROUND = Color.white;
	public static final Color DEFAULT_FOREGROUND = Color.black;

	protected Color background;
	protected Color foreground;
	protected String text;
	protected boolean visible;

	public UIButton(String text, int x, int y, int width, int height){
		this(text, x, y, width, height, DEFAULT_FOREGROUND, DEFAULT_BACKGROUND);
	}

	public UIButton(String text, int x, int y, int width, int height, Color background, Color foreground){
		super(x, y, width, height);
		this.background = background;
		this.foreground = foreground;
		this.text = text;
	}

	@Override
	public void render(GameContainer gc, Graphics g){
		Helper.renderButton(g, text, x, y, width, height, background, foreground);
	}

	@Override
	public void update(GameContainer g, int delta){
		// Do nothing
	}

	public Color getBackground(){
		return background;
	}

	public Color getForecround(){
		return foreground;
	}

	public String getText(){
		return text;
	}

	@Override
	public boolean isVisible(){
		return visible;
	}
}
