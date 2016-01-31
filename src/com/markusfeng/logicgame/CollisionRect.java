package com.markusfeng.logicgame;

/**
 * Immutable type of four ints representing a rectangular box
 * @author Markus Feng
 */
public class CollisionRect {
	
	protected final int x;
	protected final int y;
	protected final int width;
	protected final int height;
	
	public CollisionRect(int x, int y, int width, int height){
		this.x = x;
		this.y = y;
		this.width = width;
		this.height = height;
	}
	
	//Returns a rotated copy of the rectangle (width and height swapped, changed x and y)
	public CollisionRect rotatedCopy(){
		return new CollisionRect(x + (width - height) / 2, y + (height - width) / 2, height, width);
	}
	
	public boolean collidesWithPoint(int x, int y){
		//Checks each boundary to see if the point is within
		return x > this.x && x < this.x + width && y > this.y && y < this.y + height;
	}
	
	public int getX(){
		return x;
	}
	
	public int getY(){
		return y;
	}
	
	public int getWidth(){
		return width;
	}
	
	public int getHeight(){
		return height;
	}
}
