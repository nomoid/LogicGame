package com.markusfeng.logicgame;

/**
 * Card operations as public static methods
 * 
 * @author Markus Feng
 */
public final class Card {
	
	private Card(){
		
	}
	
	public static String longString(int val){
		if(val == 0){
			return "None";
		}
		String suit = getSuit(val);
		String number = getNumberString(val);
		if(suit == "Jokers"){
			return number + " Joker";
		}
		else{
			return number + " of " + suit;
		}
	}
	
	public static String getNumberString(int val){
		int number = getNumber(val);
		if(number > 15 || number < 0){
			throw new IllegalStateException("Invalid number");
		}
		if(getSuit(val) == "Jokers"){
			switch(val){
				case 15: return "Red";
				case 14: return "Black";
				default: throw new IllegalStateException("Invalid number");
			}
		}
		else{
			switch(val){
				case 0: return "None";
				case 1: return "Ace";
				case 11: return "Jack";
				case 12: return "Queen";
				case 13: return "King";
				default: return String.valueOf(number);
			}
		}
	}
	
	public static char getShortNumber(int val){
		int num = getNumber(val);
		if(num < 0){
			throw new IllegalStateException("Invalid number");
		}
		if(num == 0){
			return 'N';
		}
		if(num == 1){
			return 'A';
		}
		if(num < 10){
			return String.valueOf(num).charAt(0);
		}
		switch(num){
			case 10: return 'T';
			case 11: return 'J';
			case 12: return 'Q';
			case 13: return 'K';
			case 14: return 'B';
			case 15: return 'R';
			default: throw new IllegalStateException("Invalid number");
		}
	}
	
	public static char getShortSuit(int val){
		switch(getSuit(val)){
			case "Spades": return 'S';
			case "Hearts": return 'H';
			case "Clubs": return 'C';
			case "Diamonds": return 'D';
			case "Jokers": return 'X';
			case "None": return 'N';
			default: return '_';
		}
	}
	
	public static int getNumber(int val){
		if(val > 54 || val < 0){
			throw new IllegalStateException("Invalid number");
		}
		switch(val){
			case 54: return 15;
			case 53: return 14;
			case 0: return 0;
			default: return ((val - 1) % 13) + 1;
		}
	}
	
	public static String getSuit(int val){
		if(val > 54 || val < 0){
			throw new IllegalStateException("Invalid suit");
		}
		switch(val){
			case 54: return "Jokers";
			case 53: return "Jokers";
			case 0: return "None";
			default: switch((val - 1) / 13){
				case 0: return "Diamonds";
				case 1: return "Clubs";
				case 2: return "Hearts";
				case 3: return "Spades";
				default: throw new IllegalStateException("Invalid suit");
			}
		}
	};
}
