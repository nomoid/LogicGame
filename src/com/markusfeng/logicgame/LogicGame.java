package com.markusfeng.logicgame;

import java.io.Closeable;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.newdawn.slick.AppGameContainer;
import org.newdawn.slick.BasicGame;
import org.newdawn.slick.Color;
import org.newdawn.slick.GameContainer;
import org.newdawn.slick.Graphics;
import org.newdawn.slick.Image;
import org.newdawn.slick.Input;
import org.newdawn.slick.SlickException;
import org.newdawn.slick.SpriteSheet;

import com.markusfeng.Shared.Version;
import com.markusfeng.logicgame.multiplayer.LogicGameProcessor;

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
@Version(value = "build-1")
public class LogicGame extends BasicGame{
	
	static final int DEFAULT_PORT = 59132;
	
	static final int DEFAULT_WIDTH = 1280;
	static final int DEFAULT_HEIGHT = 800;
	
	int playerNumber = 0;
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
	
	//TODO Warning -> not closed yet
	Set<Closeable> closeables;
	
	String consoleLine = "";
	
	SpriteSheet sheet; 

	public static void main(String[] args) throws SlickException{
		//Creates the game container
		AppGameContainer app = new AppGameContainer(new LogicGame());
		app.setDisplayMode(DEFAULT_WIDTH, DEFAULT_HEIGHT, false);
		app.setTargetFrameRate(60);
		app.setAlwaysRender(true);
		app.start();
	}
	
	public LogicGame() {
		super("Logic");
	}
	
	@Override
	public void init(GameContainer gc) throws SlickException {
		closeables = new HashSet<Closeable>();
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
			faceUp[transpose(i)] = true;
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
		//g.clear();
		g.drawString("Console: " + consoleLine, 10, 30);
		g.drawString("Version: " + getVersion(), 10, 50);
		//Try 4 player rendering first
		//Render counterclockwise from bottom
		for(int i = 0; i < players; i++){
			//Transform in degrees per player
			float transform = -90;
			for(int j = 0; j < cardsPerPlayer; j++){
				//Current index in the cards and faceDown arrays
				int currentIndex = transpose(i * cardsPerPlayer + j);
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
				if(processor == null){
					faceUp[i] = !faceUp[i];
				}
				else{
					processor.invokeMethod("flip", Collections.singletonMap("index", String.valueOf(i)));
				}
			}
		}
		
	}
	
	protected LogicGameProcessor processor;
	
	@Override
	public void keyPressed(int key, char c){
		if(key == Input.KEY_DELETE || key == Input.KEY_BACK){
			if(consoleLine.length() > 0){
				consoleLine = consoleLine.substring(0, consoleLine.length() - 1);
			}
		}
		else if(key == Input.KEY_ENTER){
			processConsole(consoleLine);
			consoleLine = "";
		}
		else{
			consoleLine = consoleLine + c;
		}
	}
	
	public void processConsole(String command){
		try{
			System.out.println("Running command: " + command);
			String[] args = command.split(" ");
			if(args.length == 0){
				return;
			}
			String name = args[0];
			if(name.equalsIgnoreCase("/server")){
				int port = DEFAULT_PORT;
				if(args.length >= 2){
					//throws NumberFormatException
					port = Integer.parseInt(args[1]);
				}
				startServer(port);
			}
			else if(name.equalsIgnoreCase("/client")){
				String host = "localhost";
				int port = DEFAULT_PORT;
				if(args.length >= 2){
					host = args[1];
				}
				if(args.length >= 3){
					//throws NumberFormatException
					port = Integer.parseInt(args[2]);
				}
				startClient(host, port);
			}
			else{
				//TODO implement chat?
				System.out.println("Invalid command: " + command);
			}
		}
		catch(Exception e){
			System.out.println("Command occured with exception " + e.getMessage());
		}
	}
	
	protected void startServer(int port) throws IOException{
		if(processor != null){
			return;
		}
		System.out.println("Server started");
		processor = LogicGameProcessor.startServer(this, port, closeables);
	}
	
	protected void startClient(String host, int port) throws IOException{
		if(processor != null){
			return;
		}
		System.out.println("Client started");
		processor = LogicGameProcessor.startClient(this, host, port, closeables);
	}
	
	public Image getCardFromSheet(int card){
		//Gets the given card from the sprite sheet
		return sheet.getSprite((card - 1) % 13, (card - 1) / 13);
	}
	
	public Image getBackFromSheet(int back){
		//Get the given card back from the sprite sheet
		return getCardFromSheet(back + 55);
	}
	
	public int[] getCards(){
		return cards;
	}
	
	public boolean[] getFaceUp(){
		return faceUp;
	}

	public void setCardDataRecieved(int[] array, int playerNumber) {
		this.playerNumber = playerNumber;
		//System.arraycopy(array, playerNumber * cardsPerPlayer, cards, 0, cards.length - playerNumber * cardsPerPlayer);
		//System.arraycopy(array, 0, cards, cards.length - playerNumber * cardsPerPlayer, playerNumber * cardsPerPlayer);
		System.arraycopy(array, 0, cards, 0, cards.length);
		for(int i = 0; i < rects.length; i++){
			//Resets the collision rectangles
			rects[i] = null;
		}
		for(int i = 0; i < faceUp.length; i++){
			faceUp[i] = false;
		}
		for(int i = 0; i < cardsPerPlayer; i++){
			//Sets the cards of this player to be face up
			faceUp[transpose(i)] = true;
		}
	}

	public String flip(int index) {
		faceUp[index] = !faceUp[index];
		return "complete";
	}
	
	public int transpose(int index){
		return (index + playerNumber * cardsPerPlayer) % (players * cardsPerPlayer);
	}
	
	public int untranspose(int index){
		return (index + players * cardsPerPlayer - playerNumber * cardsPerPlayer) % (players * cardsPerPlayer);
	}
	
	public String getVersion(){
		Version[] versions = LogicGame.class.getAnnotationsByType(Version.class);
		if(versions.length != 1){
			throw new UnsupportedOperationException("No valid version found");
		}
		return versions[0].value();
	}

	public boolean compatibleVersion(String version){
		return getVersion().equals(version);
	}
}
