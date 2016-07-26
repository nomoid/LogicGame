package com.markusfeng.logicgame.ui;

public interface UICollisionElement extends UIElement{
	boolean collidesWithPoint(int x, int y);

	void collisionAction(int x, int y);
}
