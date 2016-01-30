package com.markusfeng.logicgame;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.newdawn.slick.AppGameContainer;
import org.newdawn.slick.BasicGame;
import org.newdawn.slick.GameContainer;
import org.newdawn.slick.Graphics;
import org.newdawn.slick.Image;
import org.newdawn.slick.SlickException;
import org.newdawn.slick.SpriteSheet;
import org.newdawn.slick.Color;

/**
 * The engine driving the logic game
 * 
 * @author Markus Feng
 */
public class LogicGame extends BasicGame{
	
	
	static final int DEFAULT_WIDTH = 1280;
	static final int DEFAULT_HEIGHT = 800;
	
	int players = 4;
	int cardsPerPlayer = 6;
	//Array for storing all of the values of cards
	//No security model -> give everyone all information about cards
	int[] cards = new int[players * cardsPerPlayer];
	//Index matches with cards
	boolean[] faceDown = new boolean[players * cardsPerPlayer];
	
	int cardWidth = 90;
	int cardHeight = 120;
	//Number of "spacings" to put on either side of the cards,
	//if one spacing is the distance between two cards
	int sidePadding = 3;
	
	SpriteSheet sheet; 

	public static void main(String[] args) throws SlickException{
		//Creates the game container
		AppGameContainer app = new AppGameContainer(new LogicGame());
		app.setDisplayMode(DEFAULT_WIDTH, DEFAULT_HEIGHT, false);
		app.setTargetFrameRate(60);
		app.start();
	}
	
	public LogicGame() {
		super("Logic");
	}
	
	@Override
	public void init(GameContainer gc) throws SlickException {
		//Creates the card sheets
		sheet = new SpriteSheet(new Image("assets/poker_120.png"), cardWidth, cardHeight);
		//Deals the cards out
		dealCards();
	}
	
	void dealCards(){
		List<Integer> cardList = new ArrayList<Integer>();
		//Add necessary logic cards
		//Ace to Queen of Hearts and Spades (by default)
		for(int i = 0; i < cardsPerPlayer * players / 2; i++){
			cardList.add(Card.HEARTS + i + 1);
			cardList.add(Card.SPADES + i + 1);
			//Randomly sets the cards to face down
			if(Math.random() > 0.5){
				faceDown[i] = true;
			}
			if(Math.random() > 0.5){
				faceDown[i + cardsPerPlayer * players / 2] = true;
			}
		}
		//Shuffle cards
		Collections.shuffle(cardList);
		cardList.forEach(x -> System.out.println(x + ": " + Card.longString(x)));
		//Sort the dealt cards in ascending order
		//Ties are broken randomly
		sortDealtCards(cardList);
		//Add cards to card array
		int i = 0;
		for(int n : cardList){
			cards[i] = n;
			i++;
		}
	}
	
	void sortDealtCards(List<Integer> cards){
		List<Integer> sorted = new ArrayList<Integer>();
		for(int i = 0; i < players; i++){
			List<Integer> sub = new ArrayList<Integer>(cards.subList(i * cardsPerPlayer, (i + 1) * cardsPerPlayer));
			sub.sort((x, y) -> Integer.compare(Card.getNumber(x), Card.getNumber(y)));
			sorted.addAll(sub);
		}
		cards.clear();
		cards.addAll(sorted);
	}

	@Override
	public void render(GameContainer gc, Graphics g) throws SlickException {
		g.setBackground(new Color(0, 0, 0));
		g.clear();
		//Try 4 player rendering first
		//Render counterclockwise from bottom
		for(int i = 0; i < players; i++){
			//Transform in degrees per player
			float transform = -90;
			for(int j = 0; j < cardsPerPlayer; j++){
				//Current index in the cards and faceDown arrays
				int currentIndex = i * cardsPerPlayer + j;
				//Calculate the spacing between each card
				int spacing = (gc.getHeight() - (cardsPerPlayer) * cardWidth) / 
						(cardsPerPlayer + sidePadding * 2 - 1);
				int x;
				int y;
				int frameWidth = gc.getWidth();
				int frameHeight = gc.getHeight();
				//Calculate the x and y coordinates based on the position of the player
				//Currently only supports less than 4 players
				switch(i){
				case 0:
					x = spacing * (j + sidePadding) + cardWidth * j + (frameWidth - frameHeight) / 2;
					y = frameHeight - (cardHeight / 4) - (cardHeight);
					break;
				case 1:
					x = frameWidth - (cardHeight / 4) - (cardWidth);
					y = frameHeight - spacing * (j + sidePadding) - cardWidth * (j + 1);
					break;
				case 2:
					x = frameWidth - spacing * (j + sidePadding) - cardWidth * (j + 1) - (frameWidth - frameHeight) / 2;
					y = cardHeight / 4;
					break;
				case 3:
					x = cardHeight / 4;
					y = spacing * (j + sidePadding) + cardWidth * j;
					break;
				default:
					throw new IllegalStateException("Invalid player: " + i);	
				}
				//Renders the card with the given information
				renderCard(gc, g, currentIndex, x, y, transform * i);
			}
		}
	}
	
	void renderCard(GameContainer gc, Graphics g, int currentIndex, 
			int x, int y, float transform){
		int currentCard = cards[currentIndex];
		//Rotates the rendering system to make cards rotated
		g.rotate(x + cardWidth/2, y + cardHeight/2, transform);
		//Renders face down or face up card based on whether the face down variable is set to true
		if(faceDown[currentIndex]){
			g.drawImage(getBackFromSheet(Card.getColor(currentCard).equals("Red") ? 0 : 3), x, y);
		}
		else{
			g.drawImage(getCardFromSheet(currentCard), x, y);
		}
		//Resets the transform to the original
		g.resetTransform();
	}

	@Override
	public void update(GameContainer gc, int delta) throws SlickException {
		
	}
	
	public Image getCardFromSheet(int card){
		//Gets the given card from the sprite sheet
		return sheet.getSprite((card - 1) % 13, (card - 1) / 13);
	}
	
	public Image getBackFromSheet(int back){
		//Get the given card back from the sprite sheet
		return getCardFromSheet(back + 55);
	}
}
