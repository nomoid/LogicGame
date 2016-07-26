package com.markusfeng.logicgame;

import org.newdawn.slick.Color;
import org.newdawn.slick.GameContainer;
import org.newdawn.slick.Graphics;
import org.newdawn.slick.TrueTypeFont;

public final class Helper{

	private Helper(){

	}

	//Gets the width of a string in a graphics context
	public static int getTextWidth(Graphics g, String text){
		return g.getFont().getWidth(text);
	}

	//Gets the height of a string in a graphics context
	public static int getTextHeight(Graphics g, String text){
		return g.getFont().getHeight(text);
	}

	public static CollisionRect renderButton(Graphics g, String text, int x, int y, int width, int height,
			Color background, Color foreground){
		Color old = g.getColor();
		int realX = x - width / 2;
		int realY = y - height / 2;
		g.setColor(background);
		g.fillRect(realX, realY, width, height);
		CollisionRect button = new CollisionRect(realX, realY, width, height);
		g.setColor(foreground);
		//Draws the text into the center of the button
		g.drawString(text, realX + width / 2 - Helper.getTextWidth(g, text) / 2,
				realY + height / 2 - Helper.getTextHeight(g, text) / 2);
		g.setColor(old);
		return button;
	}

	//Renders a string, centered by width
	//Top for y, center for x
	public static void renderString(Graphics g, String text, int x, int y){
		g.drawString(text, x - getTextWidth(g, text) / 2, y);
	}

	public static void drawLoadingScreen(GameContainer gc, Graphics g){
		Color old = g.getColor();
		g.setColor(Color.white);
		Helper.renderString(g, "Logic Game", gc.getWidth() / 2, gc.getHeight() / 4);
		Helper.renderString(g, "Version: " + LogicGame.getVersion(), gc.getWidth() / 2, gc.getHeight() / 4 + 20);
		Helper.renderString(g, "Loading...", gc.getWidth() / 2, gc.getHeight() / 2);
		g.setColor(old);
		return;
	}

	private static TrueTypeFont defaultFont;

	public static TrueTypeFont getDefaultFont(){
		return defaultFont;
	}

	public static void setDefaultFont(TrueTypeFont font){
		defaultFont = font;
	}

}
