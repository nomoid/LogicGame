package com.markusfeng.logicgame;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import org.newdawn.slick.AppGameContainer;
import org.newdawn.slick.BasicGame;
import org.newdawn.slick.Color;
import org.newdawn.slick.GameContainer;
import org.newdawn.slick.Graphics;
import org.newdawn.slick.Image;
import org.newdawn.slick.SlickException;
import org.newdawn.slick.SpriteSheet;

/**
 * The engine driving the logic game
 * 
 * Turn structure:
 *   Active player's partner chooses a card that he/she owns -> send INDEX to all (secure: send VALUE to active)
 *   Active player looks at that card
 *   Active player picks a card of enemy -> send INDEX to all
 *   Active player declares a card -> send VALUE to all
 *     If card correct, flip (secure: send VALUE to all)
 *     Otherwise, active player chooses a card that he/she owns and flips -> send INDEX to all (secure: SEND VALUE to all)
 *   Move on to next player as active player
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
	boolean[] faceUp = new boolean[cards.length];
	CollisionRect[] rects = new CollisionRect[cards.length];
	
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
		//Generates the cards (shuffling them), then deals the cards out
		dealCards(generateCards());
	}
	
	//Returns a list of cards to be used in the Logic game
	List<Integer> generateCards(){
		List<Integer> cardList = new ArrayList<Integer>();
		//Add necessary logic cards
		//Ace to Queen of Hearts and Spades (by default)
		for(int i = 0; i < cardsPerPlayer * players / 2; i++){
			cardList.add(Card.HEARTS + i + 1);
			cardList.add(Card.SPADES + i + 1);
		}
		for(int i = 0; i < cardsPerPlayer; i++){
			//Sets the cards of this player to be face up
			faceUp[i] = true;
		}
		//Shuffle cards
		Collections.shuffle(cardList);
		//cardList.forEach(x -> System.out.println(x + ": " + Card.longString(x)));
		for(int x : cardList){
			System.out.println(x + ": " + Card.longString(x));
		}
		return cardList;
	}
	
	void dealCards(List<Integer> cardList){
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
			sub.sort(new Comparator<Integer>(){

				@Override
				public int compare(Integer x, Integer y) {
					return Integer.compare(Card.getNumber(x), Card.getNumber(y));
				}
				
			});
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
		//Save performance by only setting the collision rectangle if it doesn't already exist
		if(rects[currentIndex] == null){
			//Makes a collision rectangle for the card to check for clicks
			CollisionRect rect = new CollisionRect(x, y, cardWidth, cardHeight);
			if((int)(Math.round(transform / 90)) % 2 == 0){
				//Adds the collision rectangle to the array
				rects[currentIndex] = rect;
			}
			else{
				//If the card is rotated, make sure to rotate the collision rectangle
				rects[currentIndex] = rect.rotatedCopy();
			}
		}
		int currentCard = cards[currentIndex];
		//Rotates the rendering system to make cards rotated
		g.rotate(x + cardWidth/2, y + cardHeight/2, transform);
		//Renders face down or face up card based on whether the face down variable is set to true
		if(!faceUp[currentIndex]){
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
	
	@Override
	public void mouseClicked(int button, int x, int y, int buttonCount) {
		//Go through the card collision rectangles
		for(int i = 0; i < rects.length; i++){
			//If the collision rectangle collides with the clicked point
			if(rects[i].collidesWithPoint(x, y)){
				//Changes the card to be face up and face down, or vice versa
				faceUp[i] = !faceUp[i];
			}
		}
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
