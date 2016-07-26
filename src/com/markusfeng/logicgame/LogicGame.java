package com.markusfeng.logicgame;

import java.awt.EventQueue;
import java.awt.Font;
import java.io.Closeable;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Logger;

import javax.swing.JFrame;
import javax.swing.WindowConstants;

import org.newdawn.slick.AppGameContainer;
import org.newdawn.slick.Color;
import org.newdawn.slick.GameContainer;
import org.newdawn.slick.Graphics;
import org.newdawn.slick.Image;
import org.newdawn.slick.Input;
import org.newdawn.slick.SlickException;
import org.newdawn.slick.SpriteSheet;
import org.newdawn.slick.TrueTypeFont;

import com.markusfeng.Shared.Commands;
import com.markusfeng.Shared.Version;
import com.markusfeng.modules.logging.AbstractConsoleFrame;
import com.markusfeng.modules.logging.CustomLevel;

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
 * "2 player mode"
 *   Each turn:
 *     Active player player picks a card of its conjugate (partner)
 *     Active player looks at the card
 *     Active player picks a card of the enemy or the enemy's conjugate
 *       If card correct, flip
 *       Otherwise, active player chooses a card that he/she owns and flips
 *
 * Feedback:
 *   Add instructions screen
 *     Turn order (anti-clockwise)
 *   Add chat
 *   Make player limiter
 *
 * @author Markus Feng
 */
@Version(value = "0.0.0.4")
public class LogicGame implements Screen{

	//Default server port (random number)
	private static final int DEFAULT_PORT = 59132;

	private static final int DEFAULT_WIDTH = 1280;
	private static final int DEFAULT_HEIGHT = 800;

	//Actions
	private static final int ACTION_PASSING = 0;
	private static final int ACTION_PASS_RECEIVING = 1;
	private static final int ACTION_GUESSING = 2;
	private static final int ACTION_REVEALING = 3;
	private static final int ACTION_CLAIMING = 4;

	//The messages to display when the /help command is invoked
	private static final String[] HELP_MESSAGE = {"The following are the console commands",
			"/help: displays this message", "/server [port]: starts the server (on the specified port)",
			"/client [host] [port]: starts the client (on the given host and the specified port)",
			"/server2 [port]: starts the server on two player mode (on the specified port)",
			"/restart: restarts the server", "(no slash): chat to all players"};

	//The current action, as specified by Actions
	private int currentAction = 0;
	//The number of the player who has the current turn
	private int currentTurn = 0;
	//The number of the current player
	private int playerNumber = 0;

	//Is currently in two player mode
	private boolean twoPlayerMode;

	private final int players = 4;
	private final int cardsPerPlayer = 6;

	//Array for storing all of the values of cards
	//No security model -> give everyone all information about cards
	private final int[] cards = new int[players * cardsPerPlayer];
	//Index matches with cards
	private final boolean[] faceUp = new boolean[cards.length];
	private final CollisionRect[] rects = new CollisionRect[cards.length];

	//Spades for inner cards
	private final int[] hearts = new int[players * cardsPerPlayer / 2];
	//Hearts for inner cards
	private final int[] spades = new int[players * cardsPerPlayer / 2];
	//An array that is always face up for inner cards
	private final boolean[] alwaysFaceUp = new boolean[players * cardsPerPlayer / 2];

	//Is currently picking a card
	private boolean cardPicking = false;
	//Center cards for picking
	private final CollisionRect[] centerRects = new CollisionRect[cardsPerPlayer * players / 2];

	//If the current player's cards are being revealed
	//Toggled by the reveal button
	private boolean revealing;
	//The reveal button collision box
	private CollisionRect reveal;
	//The claim button collision box
	private CollisionRect claim;
	//Index of currently picked card
	private int currentPicking = 0;

	//Index of a received card to temporarily reveal
	private int receiveIndex = 0;
	//Index of last guess
	private int guessIndex = -1;

	private int cardWidth = 90;
	private int cardHeight = 120;
	//Number of "spacings" to put on either side of the cards,
	//if one spacing is the distance between two cards
	private int sidePadding = 3;

	//TODO Warning -> not closed yet
	private Set<Closeable> closeables;

	private String consoleLine = "";
	private String tempDisplay = "";
	private String winLoseDisplay = "";

	//Last few lines on chat
	private LinkedList<String> chatHistory;
	private final int historySize = 3;

	private SpriteSheet sheet;
	//private TrueTypeFont defaultFont;

	//Processor for multiplayer
	protected LogicGameProcessor processor;

	private ScreenManager manager;

	private boolean initializationComplete = false;

	public static final String NAME = "LogicGame";

	@Override
	public String getName(){
		return NAME;
	}

	public LogicGame(ScreenManager manager){
		this.manager = manager;
	}

	@Override
	public void init(GameContainer gc) throws SlickException{
		initializationComplete = false;
		//Creates the set of Closeables to be cleaned up
		closeables = new HashSet<Closeable>();
		//Creates the chat history linked list
		chatHistory = new LinkedList<String>();
		//Generates the cards (shuffling them), then deals the cards out
		dealCards(generateCards());
		//Lengthy initialization tasks (e.g. loading resources) are performed in the update method
		initializationComplete = true;
	}

	//Returns a list of cards to be used in the Logic game
	private List<Integer> generateCards(){
		List<Integer> cardList = new ArrayList<Integer>();
		//Add necessary logic cards
		//Ace to Queen of Hearts and Spades (by default)
		for(int i = 0; i < cardsPerPlayer * players / 2; i++){
			cardList.add(Card.HEARTS + i + 1);
			hearts[i] = Card.HEARTS + i + 1;
			cardList.add(Card.SPADES + i + 1);
			spades[i] = Card.SPADES + i + 1;
			alwaysFaceUp[i] = true;
		}
		//Shuffle cards
		Collections.shuffle(cardList);
		return cardList;
	}

	//Deal the cards out to the card array
	private void dealCards(List<Integer> cardList){
		sortDealtCards(cardList);
		//Add cards to card array
		int i = 0;
		for(int n : cardList){
			cards[i] = n;
			i++;
		}
	}

	//Sort the dealt cards in ascending order
	//Ties are broken randomly
	private void sortDealtCards(List<Integer> cards){
		List<Integer> sorted = new ArrayList<Integer>();
		for(int i = 0; i < players; i++){
			List<Integer> sub = new ArrayList<Integer>(cards.subList(i * cardsPerPlayer, (i + 1) * cardsPerPlayer));
			sub.sort(new Comparator<Integer>(){

				@Override
				public int compare(Integer x, Integer y){
					return Integer.compare(Card.getNumber(x), Card.getNumber(y));
				}

			});
			sorted.addAll(sub);
		}
		cards.clear();
		cards.addAll(sorted);
	}

	//Renders the game
	@Override
	public void render(GameContainer gc, Graphics g) throws SlickException{
		if(!initializationComplete){
			Helper.drawLoadingScreen(gc, g);
			return;
		}
		g.setBackground(Color.black);
		g.setColor(Color.white);
		g.setFont(Helper.getDefaultFont());
		g.drawString("Console: " + consoleLine, 10, 30);
		g.drawString("Version: " + getVersion(), 10, 50);
		//Renders the three lines of text providing game information
		Helper.renderString(g, getFirstLine(), gc.getWidth() / 2, gc.getHeight() / 4);
		Helper.renderString(g, getSecondLine(), gc.getWidth() / 2, gc.getHeight() / 4 + 20);
		Helper.renderString(g, getThirdLine(), gc.getWidth() / 2, gc.getHeight() / 4 + 40);
		//Renders the last few lines of chat history
		int n = 0;
		int tempSpacing = getSpacing(gc);
		for(String s : chatHistory){
			g.drawString(s, gc.getWidth() - tempSpacing * sidePadding - (gc.getWidth() - gc.getHeight()) / 2 + 10,
					10 + 20 * n);
			n++;
		}
		//Renders the "reveal your own cards" button
		String ownership = currentTurn >= 0 ? "Your Own" : "All";
		String showOrHide = revealing ? "Hide" : "Show";
		CollisionRect revealButton = Helper.renderButton(g, showOrHide + " " + ownership + " Cards", gc.getWidth() / 3,
				gc.getHeight() / 4, 200, 40, Color.green, Color.black);
		if(reveal == null){
			reveal = revealButton;
		}
		//Renders the "claim" button
		CollisionRect claimButton = Helper.renderButton(g, "Claim", gc.getWidth() * 2 / 3, gc.getHeight() / 4, 200, 40,
				Color.green, Color.black);
		if(claim == null){
			claim = claimButton;
		}
		//Try 4 player rendering first
		//Render counterclockwise from bottom
		for(int i = 0; i < players; i++){
			//Transform in degrees per player
			float transform = -90;
			for(int j = 0; j < cardsPerPlayer; j++){
				//Current index in the cards and faceDown arrays
				int currentIndex = transpose(i * cardsPerPlayer + j);
				//Calculate the spacing between each card
				int spacing = getSpacing(gc);
				int x;
				int y;
				int frameWidth = gc.getWidth();
				int frameHeight = gc.getHeight();
				//Calculate the x and y coordinates based on the position of the player
				//Currently only supports less than 4 players
				switch(i){
					case 0:
						x = spacing * (j + sidePadding) + cardWidth * j + (frameWidth - frameHeight) / 2;
						y = frameHeight - cardHeight / 4 - cardHeight;
						break;
					case 1:
						x = frameWidth - cardHeight / 4 - cardWidth;
						y = frameHeight - spacing * (j + sidePadding) - cardWidth * (j + 1);
						break;
					case 2:
						x = frameWidth - spacing * (j + sidePadding) - cardWidth * (j + 1)
								- (frameWidth - frameHeight) / 2;
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
				renderCard(gc, g, rects, cards, faceUp, currentIndex, x, y, transform * i);
			}
		}
		if(cardPicking){
			//Two rows
			for(int i = 0; i < 2; i++){
				for(int j = 0; j < cardsPerPlayer * players / 4; j++){
					//Current index in the cards and faceDown arrays
					int currentIndex = i * cardsPerPlayer + j;
					//Calculate the spacing between each card
					int spacing = getSpacing(gc);
					int x;
					int y;
					int frameWidth = gc.getWidth();
					int frameHeight = gc.getHeight();
					x = spacing * (j + sidePadding) + cardWidth * j + (frameWidth - frameHeight) / 2;
					if(i == 0){
						y = frameHeight / 3;
					}
					else{
						y = frameHeight * 2 / 3 - cardHeight;
					}
					renderCard(gc, g, centerRects, isPickingHearts() ? hearts : spades, alwaysFaceUp, currentIndex, x,
							y, 0);
				}
			}
		}
	}

	//Renders a card
	private void renderCard(GameContainer gc, Graphics g, CollisionRect[] rects, int[] cards, boolean[] faceUp,
			int currentIndex, int x, int y, float transform){
		//Save performance by only setting the collision rectangle if it doesn't already exist
		if(rects[currentIndex] == null){
			//Makes a collision rectangle for the card to check for clicks
			CollisionRect rect = new CollisionRect(x, y, cardWidth, cardHeight);
			if(Math.round(transform / 90) % 2 == 0){
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
		g.rotate(x + cardWidth / 2, y + cardHeight / 2, transform);
		//Only do it if the cards rendered are in the outer edge
		if(cards == this.cards){
			//If the card is currently being revealed
			if(currentAction == ACTION_PASS_RECEIVING && currentIndex == receiveIndex){
				g.drawImage(getBackFromSheet(1), x, y - 20);
			}
			//If the card is the last card guessed
			if(currentIndex == guessIndex){
				g.drawImage(getBackFromSheet(5), x, y - 20);
			}
		}
		//Renders face down or face up card based on whether the face down variable is set to true
		if(faceUp[currentIndex] || revealing && (isOwn(currentIndex) || currentTurn < 0)){
			g.drawImage(getCardFromSheet(currentCard), x, y);
		}
		else{
			g.drawImage(getBackFromSheet(Card.getColor(currentCard).equals("Red") ? 0 : 3), x, y);
		}
		//Resets the transform to the original
		g.resetTransform();
	}

	//Gets the spacing for the rendering
	private int getSpacing(GameContainer gc){
		return (gc.getHeight() - cardsPerPlayer * cardWidth) / (cardsPerPlayer + sidePadding * 2 - 1);
	}

	//Gets the first line of custom information
	//Displays the current player name
	private String getFirstLine(){
		return "You are " + getPlayerNameForNumber(playerNumber);
	}

	//Gets the second line of custom information
	//Displays the currently ongoing action
	private String getSecondLine(){
		String display = "";
		if(currentTurn < 0){
			return winLoseDisplay;
		}
		switch(currentAction){
			case ACTION_PASSING:
				if(twoPlayerMode){
					display += getPlayerNameForNumber(currentTurn) + " is choosing a card to pass.";
				}
				else{
					display += getPlayerNameForNumber(nextPartner(currentTurn)) + " is choosing a card to pass.";
				}
				break;
			case ACTION_PASS_RECEIVING:
				display += getPlayerNameForNumber(currentTurn) + " is recieving the pass.";
				break;
			case ACTION_GUESSING:
				display += getPlayerNameForNumber(currentTurn) + " is guessing a card.";
				break;
			case ACTION_REVEALING:
				display += getPlayerNameForNumber(currentTurn) + " is choosing a card to reveal.";
				break;
			case ACTION_CLAIMING:
				display += getPlayerNameForNumber(currentTurn) + " is claiming.";
				break;
		}
		return display;
	}

	//Gets the first line of custom information
	//Displays the temporary display (e.g. value of last card picked, game over)
	private String getThirdLine(){
		return tempDisplay;
	}

	//Gets the player name for the given player number
	private String getPlayerNameForNumber(int number){
		if(number < 0){
			//Returns "System"
			return "System";
		}
		else{
			//Returns the number plus one (e.g. player 0 is displayed as Player 1)
			return "Player " + (number + 1);
		}
	}

	//Updates the game's state
	@Override
	public void update(GameContainer gc, int delta) throws SlickException{

	}

	//Called when a mounse button is clicked
	@Override
	public void mouseClicked(int button, int x, int y, int buttonCount){
		//Go through the card collision rectangles
		for(int i = 0; i < rects.length; i++){
			//If the collision rectangle collides with the clicked point
			if(rects[i].collidesWithPoint(x, y)){
				cardClicked(i);
				return;
			}
		}
		if(cardPicking){
			//Check for collision with the center rectangles
			for(int i = 0; i < centerRects.length; i++){
				//If the collision rectangle collides with the clicked point
				if(centerRects[i].collidesWithPoint(x, y)){
					cardPicked(i);
					return;
				}
			}
		}
		//Check for collision of the "reveal your own cards" button
		if(reveal.collidesWithPoint(x, y)){
			revealSelf();
		}
		//Check for collision of the "claim" button
		if(claim.collidesWithPoint(x, y)){
			if(processor == null){
				claim(playerNumber);
			}
			else{
				processor.invokeMethod("claim", Collections.singletonMap("playernumber", String.valueOf(playerNumber)));
			}
		}
	}

	//Called when a card on the outer edge is clicked
	private void cardClicked(int index){
		switch(currentAction){
			case ACTION_PASSING:
				if(twoPlayerMode){
					//Current player picks card to pass
					//Only allow the current player to pass
					if(playerNumber != currentTurn){
						return;
					}
					//Only allow a player to pick his or her own cards
					if(!isPartner(index)){
						return;
					}
				}
				else{
					//Only allow the current player's partner to pass
					if(playerNumber != nextPartner(currentTurn)){
						return;
					}
					//Only allow a player to pick his or her own cards
					if(!isOwn(index)){
						return;
					}
				}
				//Only allow face down cards
				if(faceUp[index]){
					return;
				}
				if(processor != null){
					processor.invokeMethod("pass", Collections.singletonMap("index", String.valueOf(index)));
				}
				else{
					pass(index);
				}
				break;
			case ACTION_PASS_RECEIVING:
				//Only allow current player to receive
				if(playerNumber != currentTurn){
					return;
				}
				//Only allow the card at the received index to be chosen
				if(receiveIndex != index){
					return;
				}
				if(processor != null){
					processor.invokeMethod("received", Collections.<String, String> emptyMap());
				}
				else{
					received();
				}
				break;
			case ACTION_GUESSING:
				//Only allow the current player to guess
				if(playerNumber != currentTurn){
					return;
				}
				//Only allow a player to pick an opponent's cards
				if(!isOpponent(index)){
					return;
				}
				//Only allow face down cards
				if(faceUp[index]){
					return;
				}
				//Only allow picking when an outer card has not yet already been picked
				if(cardPicking){
					return;
				}
				//Sets the current picking index
				currentPicking = index;
				//Go into card picking mode (pick an inner card)
				cardPicking = true;
				break;
			case ACTION_REVEALING:
				//Only allow the current player to reveal
				if(playerNumber != currentTurn){
					return;
				}
				//Only allow a player to pick his or her own cards
				if(!isOwn(index)){
					return;
				}
				//Only allow face down cards
				if(faceUp[index]){
					return;
				}
				if(processor != null){
					processor.invokeMethod("reveal", Collections.singletonMap("index", String.valueOf(index)));
				}
				else{
					reveal(index);
				}
				break;
			case ACTION_CLAIMING:
				//Only allow the claiming player to guess
				if(playerNumber != currentTurn){
					return;
				}
				//Only allow face down cards
				if(faceUp[index]){
					return;
				}
				//Only allow picking when an outer card has not yet already been picked
				if(cardPicking){
					return;
				}
				//Sets the current picking index
				currentPicking = index;
				//Go into card picking mode (pick an inner card)
				cardPicking = true;
				break;
		}
	}

	//Called when an inner card is picked
	private void cardPicked(int index){
		//Get the card picked
		int pick = isPickingHearts() ? hearts[index] : spades[index];
		//Send the card picked
		if(processor != null){
			Map<String, String> args = new HashMap<String, String>();
			args.put("index", String.valueOf(currentPicking));
			args.put("pick", String.valueOf(pick));
			processor.invokeMethod("guess", args);
		}
		else{
			guess(currentPicking, pick);
		}
		//Stops card picking mode
		cardPicking = false;
	}

	//Returns true if the card currently picking is of the hearts suit
	private boolean isPickingHearts(){
		return Card.getSuit(cards[currentPicking]).equals("Hearts");
	}

	//Returns true if the card at index is the player's own
	private boolean isOwn(int index){
		return untranspose(index) / cardsPerPlayer == 0;
	}

	//Returns true if the card at index is the player's partner's
	private boolean isPartner(int index){
		int playerNum = untranspose(index) / cardsPerPlayer;
		return playerNum > 0 && playerNum % 2 == 0;
	}

	//Returns true if the card at index is the player's opponent's
	private boolean isOpponent(int index){
		int playerNum = untranspose(index) / cardsPerPlayer;
		return playerNum % 2 == 1;
	}

	//Returns the index of the player's next partner
	private int nextPartner(int player){
		return (player + 2) % players;
	}

	//Go to the next action that is valid
	private void goToNextAction(boolean correctGuess){
		//If the current action directly precedes the action to be validated
		//Used for checking if the pass receiving action is valid
		boolean rightBefore = true;
		boolean done = false;
		while(!done){
			switch(currentAction){
				case ACTION_PASSING:
					//Go to the pass receiving action
					currentAction = ACTION_PASS_RECEIVING;
					break;
				case ACTION_PASS_RECEIVING:
					//Go to the guessing action
					currentAction = ACTION_GUESSING;
					break;
				case ACTION_GUESSING:
					//On correct guess
					if(correctGuess){
						//Go to the passing action
						currentAction = ACTION_PASSING;
						//Increment the turn
						goToNextTurn();
					}
					else{
						//Go to the revealing action
						currentAction = ACTION_REVEALING;
					}
					break;
				case ACTION_REVEALING:
					//Go to the passing action
					currentAction = ACTION_PASSING;
					//Increment the turn
					goToNextTurn();
					break;
				case ACTION_CLAIMING:
					//Stay on the current action
					break;
			}
			//Checks if the action is valid, ending if it is
			done = isActionValid(rightBefore);
			rightBefore = false;
		}
	}

	void goToNextTurn(){
		if(twoPlayerMode){
			currentTurn = (currentTurn + 1) % 2;
		}
		else{
			currentTurn = (currentTurn + 1) % players;
		}
	}

	//Returns true an action is valid
	private boolean isActionValid(boolean rightBefore){
		switch(currentAction){
			case ACTION_PASSING:
				//Invalid if all of the current player's partner's cards are face up
				return !isAllFaceUp(nextPartner(currentTurn));
			case ACTION_PASS_RECEIVING:
				//Invalid if the previous action is not directly preceding
				return rightBefore;
			case ACTION_GUESSING:
				//Invalid if all of the opponent's cards are face up
				for(int i = 1; i < players; i += 2){
					if(!isAllFaceUp((currentTurn + i) % players)){
						return true;
					}
				}
				return false;
			case ACTION_REVEALING:
				//Invalid if all of the current player's cards are face up
				return !isAllFaceUp(currentTurn);
			case ACTION_CLAIMING:
				//Always valid
				return true;
		}
		return true;
	}

	//Returns true if all of a player's cards are face up
	private boolean isAllFaceUp(int player){
		for(int i = player * cardsPerPlayer; i < (player + 1) * cardsPerPlayer; i++){
			if(!faceUp[i]){
				return false;
			}
		}
		return true;
	}

	//Remote method
	//Called when the card data is received upon connection
	public void receiveCardData(int[] array, int playerNumber, boolean twoPlayerMode){
		int tempPlayers = twoPlayerMode ? 2 : players;
		//Limits the number of players that can join
		if(playerNumber >= tempPlayers){
			//Resets the processor
			processor.close();
			processor = null;
			return;
		}
		this.playerNumber = playerNumber;
		this.twoPlayerMode = twoPlayerMode;
		reset(array);
	}

	//Remote method
	//Resets the game state to the cards defined by the array
	public String reset(int[] array){
		//Copies the cards into the array
		System.arraycopy(array, 0, cards, 0, cards.length);
		for(int i = 0; i < rects.length; i++){
			//Resets the collision rectangles
			rects[i] = null;
		}
		for(int i = 0; i < faceUp.length; i++){
			//Turns all cards face down
			faceUp[i] = false;
		}
		//Turn off card picking mode
		cardPicking = false;
		//Resets the turn and the action
		currentTurn = 0;
		currentAction = ACTION_PASSING;
		//Resets the receive index and guess index
		receiveIndex = 0;
		guessIndex = -1;
		//Resets the displays
		tempDisplay = "";
		winLoseDisplay = "";
		//Resets revealing
		revealing = false;
		return "complete";
	}

	//Remote method
	//Flip a card at an index(unused remote method)
	public String flip(int index){
		faceUp[index] = !faceUp[index];
		return "complete";
	}

	private JFrame loggerFrame;
	private boolean loggerStarted;
	private Logger log = Logger.getLogger("logic");

	//Remote method
	//Receives a message sent by a player
	public String message(final int player, final String content){
		EventQueue.invokeLater(new Runnable(){

			@Override
			public void run(){
				if(!loggerStarted){
					loggerStarted = true;
					loggerFrame = new AbstractConsoleFrame(log){

						private static final long serialVersionUID = 5273594771840765904L;

						@Override
						protected void process(String s){
							if(processor == null){
								message(playerNumber, s);
							}
							else{
								Map<String, String> data = new HashMap<String, String>();
								data.put("playernumber", String.valueOf(playerNumber));
								data.put("content", s);
								processor.invokeMethod("message", data);
							}
						}

					};
					loggerFrame.setDefaultCloseOperation(WindowConstants.HIDE_ON_CLOSE);
					loggerFrame.setTitle("Chat");
					loggerFrame.setFocusableWindowState(false);
					loggerFrame.setVisible(true);
					loggerFrame.setFocusableWindowState(true);
				}
				pushMessage(getPlayerNameForNumber(player) + ": " + content, player >= 0);
			}
		});
		return "complete";
	}

	//Pushes the actual message to the logging system
	private void pushMessage(String message, boolean toHistory){
		log.log(CustomLevel.NOMESSAGE, message);
		if(toHistory){
			chatHistory.addLast(message);
			if(chatHistory.size() > historySize){
				chatHistory.remove();
			}
		}
	}

	//Remote method
	//Called after a card has been chosen to be passed
	public String pass(int index){
		//If the player is on the same team as the passer, display the card
		if(playerNumber % 2 == currentTurn % 2){
			faceUp[index] = true;
		}
		receiveIndex = index;
		goToNextAction(true);
		return "complete";
	}

	//Remote method
	//Called after a card has been declared received
	public String received(){
		//If the player is on the same team as the passer, hide the card
		if(playerNumber % 2 == currentTurn % 2){
			faceUp[receiveIndex] = false;
		}
		goToNextAction(true);
		return "complete";
	}

	//Remote method
	//Called after a card has been guessed, either in guessing or in claiming
	public String guess(int index, int pick){
		//Sets the last guessed index to the index of the guess
		guessIndex = index;
		//Sets the temporary display to the last card picked
		tempDisplay = "Last card picked: " + Card.longString(pick);
		if(cards[index] == pick){
			//Guess correctly, turn moves forward
			faceUp[index] = true;
			//If guessing, go to the next action
			if(currentAction == ACTION_GUESSING){
				goToNextAction(true);
			}
			//If claiming, check if all cards are revealed, and if they are, win the game
			else if(currentAction == ACTION_CLAIMING){
				boolean passing = true;
				for(boolean b : faceUp){
					if(!b){
						passing = false;
						break;
					}
				}
				if(passing){
					//Display winning message
					winLoseDisplay = getPlayerNameForNumber(currentTurn) + " wins!";
					//Prevents game progress
					currentTurn = -1;
					//Sets mode to non-revealing
					revealing = false;
				}
			}
		}
		else{
			//Guess incorrectly
			if(currentAction == ACTION_GUESSING){
				goToNextAction(false);
			}
			//If claiming, loses the game
			else if(currentAction == ACTION_CLAIMING){
				//Display losing message
				winLoseDisplay = getPlayerNameForNumber(currentTurn) + " loses!";
				//Prevents game progress
				currentTurn = -1;
				//Sets mode to non-revealing
				revealing = false;
			}
		}
		return "complete";
	}

	//Remote method
	//Called after a card has been chosen to be revealed
	public String reveal(int index){
		//Sets the card at the revealed index to be face up
		faceUp[index] = true;
		goToNextAction(true);
		return "complete";
	}

	//Remote method
	//Called when a player begins claiming
	public String claim(int player){
		currentTurn = player;
		//Reveal all of claiming player's cards
		for(int i = player * cardsPerPlayer; i < (player + 1) * cardsPerPlayer; i++){
			faceUp[i] = true;
		}
		currentAction = ACTION_CLAIMING;
		return "complete";
	}

	//Reveals all of your own cards
	public void revealSelf(){
		revealing = !revealing;
	}

	//Called when a key button is pressed
	//Used for the console
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

	//Processes a console command (string)
	public void processConsole(String command){
		//TODO create commands where one client can control multiple players
		try{
			String[] args = command.split(" ");
			if(args.length == 0){
				return;
			}
			String name = args[0];
			if(name.startsWith("/")){
				System.out.println("Running command: " + command);
				if(name.equalsIgnoreCase("/help")){
					for(String s : HELP_MESSAGE){
						message(-1, s);
					}
				}
				else if(name.equalsIgnoreCase("/server")){
					int port = DEFAULT_PORT;
					if(args.length >= 2){
						//throws NumberFormatException
						port = Integer.parseInt(args[1]);
					}
					twoPlayerMode = false;
					startServer(port);
				}
				else if(name.equalsIgnoreCase("/server2")){
					int port = DEFAULT_PORT;
					if(args.length >= 2){
						//throws NumberFormatException
						port = Integer.parseInt(args[1]);
					}
					twoPlayerMode = true;
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
				else if(name.equalsIgnoreCase("/restart")){
					//Generates the cards (shuffling them), then deals the cards out
					dealCards(generateCards());
					if(processor == null){
						reset(cards);
					}
					else{
						Map<String, String> data = new HashMap<String, String>();
						data.put("carddata", Commands.fromArray(getCards()));
						processor.invokeMethod("reset", data);
					}
				}
				else{
					System.out.println("Invalid command: " + command);
				}
			}
			else{
				if(processor == null){
					message(playerNumber, command);
				}
				else{
					Map<String, String> data = new HashMap<String, String>();
					data.put("playernumber", String.valueOf(playerNumber));
					data.put("content", command);
					processor.invokeMethod("message", data);
				}
			}
		}
		catch(Exception e){
			System.out.println("Command occured with exception " + e.getMessage());
		}
	}

	//Starts the server
	protected void startServer(int port) throws IOException{
		if(processor != null){
			return;
		}
		System.out.println("Server started");
		processor = LogicGameProcessor.startServer(this, port, closeables);
	}

	//Starts the client
	protected void startClient(String host, int port) throws IOException{
		if(processor != null){
			return;
		}
		System.out.println("Client started");
		processor = LogicGameProcessor.startClient(this, host, port, closeables);
	}

	//Gets an image of a card from the sprite sheet
	public Image getCardFromSheet(int card){
		//Gets the given card from the sprite sheet
		return sheet.getSprite((card - 1) % 13, (card - 1) / 13);
	}

	//Gets an image of a card back from the sprite sheet
	public Image getBackFromSheet(int back){
		//Get the given card back from the sprite sheet
		return getCardFromSheet(back + 55);
	}

	//Returns the array of outer cards
	public int[] getCards(){
		return cards;
	}

	//Returns which of the outer cards are face up
	public boolean[] getFaceUp(){
		return faceUp;
	}

	//Transposes a card from the real index to the index of the current player
	public int transpose(int index){
		return (index + playerNumber * cardsPerPlayer) % (players * cardsPerPlayer);
	}

	//Untransposes a card from the index of the current player to the real index
	public int untranspose(int index){
		return (index + players * cardsPerPlayer - playerNumber * cardsPerPlayer) % (players * cardsPerPlayer);
	}

	//Gets the current version of the game
	public static String getVersion(){
		Version[] versions = LogicGame.class.getAnnotationsByType(Version.class);
		if(versions.length != 1){
			throw new UnsupportedOperationException("No valid version found");
		}
		return versions[0].value();
	}

	//Returns if another version of the game is compatible with the current version
	public boolean compatibleVersion(String version){
		return getVersion().equals(version);
	}

	public boolean isTwoPlayerMode(){
		return twoPlayerMode;
	}

	@Override
	public ScreenManager getParent(){
		return manager;
	}

	public static void main(String[] args) throws SlickException{
		//Creates the app game container
		AppGameContainer app = new AppGameContainer(new ScreenManager());
		//Sets the height and the width of the app
		app.setDisplayMode(DEFAULT_WIDTH, DEFAULT_HEIGHT, false);
		//Sets to run at approx 60fps
		app.setTargetFrameRate(60);
		//Always render to allow for background updating
		app.setAlwaysRender(true);
		app.start();
	}

	public void initResources() throws SlickException{
		//Creates the card sheets
		sheet = new SpriteSheet(new Image("resources" + File.separator + "poker_120.png"), cardWidth, cardHeight);
		//Makes the font of the graphics system
		TrueTypeFont defaultFont = new TrueTypeFont(new Font("Calibri", Font.PLAIN, 20), true);
		Helper.setDefaultFont(defaultFont);
	}
}
