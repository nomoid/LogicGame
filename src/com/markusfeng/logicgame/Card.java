package com.markusfeng.logicgame;

/**
 * Card operations as public static methods
 * 
 * @author Markus Feng
 */
public final class Card {
	
	public static final int HEARTS = 0;
	public static final int DIAMONDS = 13;
	public static final int SPADES = 26;
	public static final int CLUBS = 39;
	
	private Card(){
		
	}
	
	public static String shortString(int val){
		return String.valueOf(getShortSuit(val)) + getShortNumber(val);
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
			throw new IllegalArgumentException("Invalid number");
		}
		if(getSuit(val) == "Jokers"){
			switch(number){
				case 15: return "Black";
				case 14: return "Red";
				default: throw new IllegalArgumentException("Invalid number");
			}
		}
		else{
			switch(number){
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
			throw new IllegalArgumentException("Invalid number");
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
			case 14: return 'R';
			case 15: return 'B';
			default: throw new IllegalArgumentException("Invalid number");
		}
	}
	
	public static String getColor(int val){
		String s = getSuit(val);
		if (s.equals("Spades")) {
			return "Black";
		} else if (s.equals("Hearts")) {
			return "Red";
		} else if (s.equals("Clubs")) {
			return "Black";
		} else if (s.equals("Diamonds")) {
			return "Red";
		} else if (s.equals("Jokers")) {
			return getNumberString(val);
		} else if (s.equals("None")) {
			return "None";
		} else {
			throw new IllegalArgumentException("Invalid suit");
		}
	}
	
	public static char getShortSuit(int val){
		String s = getSuit(val);
		if (s.equals("Spades")) {
			return 'S';
		} else if (s.equals("Hearts")) {
			return 'H';
		} else if (s.equals("Clubs")) {
			return 'C';
		} else if (s.equals("Diamonds")) {
			return 'D';
		} else if (s.equals("Jokers")) {
			return 'X';
		} else if (s.equals("None")) {
			return 'N';
		} else {
			throw new IllegalArgumentException("Invalid suit");
		}
	}
	
	public static int getNumber(int val){
		if(val > 54 || val < 0){
			throw new IllegalArgumentException("Invalid number");
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
			throw new IllegalArgumentException("Invalid suit");
		}
		switch(val){
			case 54: return "Jokers";
			case 53: return "Jokers";
			case 0: return "None";
			default: switch((val - 1) / 13){
				case 0: return "Hearts";
				case 1: return "Diamonds";
				case 2: return "Spades";
				case 3: return "Clubs";
				default: throw new IllegalArgumentException("Invalid suit");
			}
		}
	};
}
