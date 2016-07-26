package com.markusfeng.logicgame.ui;

import java.util.ArrayList;
import java.util.List;

import org.newdawn.slick.GameContainer;
import org.newdawn.slick.Graphics;
import org.newdawn.slick.SlickException;

import com.markusfeng.logicgame.ScreenManager;

public class UIContainer extends UICollisionRect implements UIScreen{

	protected List<UIElement> elements = new ArrayList<UIElement>();
	protected ScreenManager parent;
	protected String name;
	protected UIDimension dimension;

	public UIContainer(ScreenManager parent, String name, int width, int height){
		super(0, 0, width, height);
		this.parent = parent;
		this.name = name;
	}

	public UIContainer(ScreenManager parent, String name, UIDimension dimension){
		super(0, 0, dimension.getWidth(), dimension.getHeight());
		this.parent = parent;
		this.name = name;
		this.dimension = dimension;
	}

	@Override
	public void update(GameContainer gc, int delta) throws SlickException{
		for(UIElement element : elements){
			if(element.isVisible()){
				element.update(gc, delta);
			}
		}
	}

	@Override
	public void render(GameContainer gc, Graphics g) throws SlickException{
		for(UIElement element : elements){
			if(element.isVisible()){
				element.render(gc, g);
			}
		}
	}

	@Override
	public void init(GameContainer gc) throws SlickException{
		// Do nothing
	}

	@Override
	public void mouseClicked(int button, int x, int y, int buttonCount){
		for(UIElement element : elements){
			if(element instanceof UICollisionElement){
				UICollisionElement collider = (UICollisionElement) element;
				if(collider.collidesWithPoint(x, y)){
					collider.collisionAction(x, y);
				}
			}
		}
	}

	@Override
	public void keyPressed(int key, char c){
		// TODO Auto-generated method stub

	}

	@Override
	public ScreenManager getParent(){
		return parent;
	}

	@Override
	public String getName(){
		return name;
	}

	@Override
	public int getWidth(){
		if(dimension == null){
			return super.getWidth();
		}
		else{
			return dimension.getWidth();
		}
	}

	@Override
	public int getHeight(){
		if(dimension == null){
			return super.getHeight();
		}
		else{
			return dimension.getHeight();
		}
	}
}
