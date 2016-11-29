import java.util.ArrayList;
import java.util.Random;
import java.util.Scanner;

import processing.core.PApplet;
import processing.core.PFont;
import processing.core.PImage;

public class Minesweeper extends PApplet{
	public static final int MIN_GRID_DIMENSION = 5;
	public static final int MAX_GRID_DIMENSION = 10;
	
	public static final int MIN_DIFFICULTY = 1;
	public static final int MAX_DIFFICULTY = 3;
	
	public static final double DIFFICULTY_BEGINNER = 6.0;
	public static final double DIFFICULTY_INTERMEDIATE = 7.0;
	public static final double DIFFICULTY_EXPERT = 8.0;
	
	public static int[][] mineField;
	public static char[][] unknownTiles; 
	public static boolean[][] revealedPositions;
	
	public static int gridWidth;
	public static int gridHeight;

	public static final int GUR = 50; //Grid unit ratio
	
	PImage img;
	
	public static void main(String[] args) {
		Scanner scnr = new Scanner(System.in);
		Random rdm = new Random(321); //Seeded for testing!!!
		
		System.out.println("Welcome to janky minesweeper, scrub.\n");
		gridWidth = promptUserForNumber(scnr, "grid width between " + MIN_GRID_DIMENSION + 
				" and " + MAX_GRID_DIMENSION, MIN_GRID_DIMENSION, MAX_GRID_DIMENSION);
		gridHeight = promptUserForNumber(scnr, "grid height between " + MIN_GRID_DIMENSION + 
				" and " + MAX_GRID_DIMENSION, MIN_GRID_DIMENSION, MAX_GRID_DIMENSION);
		
		mineField = new int[gridWidth][gridHeight];
		unknownTiles = new char [gridWidth][gridHeight];
		for (int i = 0; i < unknownTiles.length; i++) {
			for (int j = 0; j < unknownTiles[i].length; j++) {
				unknownTiles[i][j] = '~';
			} 
		} //Initialize the unknown mines to have a wave character
		revealedPositions = new boolean [gridWidth][gridHeight];
		for (int i = 0; i < revealedPositions.length; i++) {
			for (int j = 0; j < revealedPositions[i].length; j++) {
				revealedPositions[i][j] = false;
			}
		} //Initialize revealed positions false to start since nothing has been revealed
		
		int gameDifficulty = promptUserForNumber(scnr, "game difficulty - (1)Beginner, (2)"
				+ "Intermediate, or (3)Expert ", MIN_DIFFICULTY, MAX_DIFFICULTY);
		System.out.println("");
		
		PApplet.main("Minesweeper");
		
		//Initial position decision and populating of the mineField (should only run once)
//		displayMineField(mineField, unknownTiles, revealedPositions);
		System.out.println("");
		int yPos = promptUserForNumber(scnr, "row to check", 1, gridHeight);
		int xPos = promptUserForNumber(scnr, "column to check", 1, gridWidth);
		seedMineField(rdm, gameDifficulty, xPos, yPos);
		checkFieldPosition(xPos, yPos);
		//Seed/populate, then perform check on the specified start position
		System.out.println("");
		
		//Print out initial state of the mine field for the player
//		displayMineField(mineField, unknownTiles, revealedPositions);
		System.out.println("");
		
		int userActionChoice = 0;
		while (!revealedMine()) {
			System.out.println("Check a tile (1), or flag/unflag a tile (2)?");
			userActionChoice = promptUserForNumber(scnr, "your action for this turn", 1, 2);
			System.out.println("");
			switch (userActionChoice) {
			case 1: //Check the tile specified
				xPos = promptUserForNumber(scnr, "row to check", 1, gridHeight);
				yPos = promptUserForNumber(scnr, "column to check", 1, gridWidth);
				if (unknownTiles[xPos -1][yPos - 1] == '?') {
					System.out.println("This tile has been flagged. Choose another"
							+ " tile to check, or unflag this tile.");
					continue;
				} //If a flagged tile is attempted to be revealed, don't let the user check it
				checkFieldPosition(xPos, yPos);
				break;
			case 2: //Flag the tile specified
				xPos = promptUserForNumber(scnr, "row to flag/unflag", 1, gridHeight);
				yPos = promptUserForNumber(scnr, "column to flag/unflag", 1, gridWidth);
				flagSquare(xPos, yPos);
				break;
			default:
				System.out.println("ERROR: Try again.");
				continue;
			}
			
			//Print out the mine field after each successful user action
			System.out.println("");
//			displayMineField(mineField, unknownTiles, revealedPositions);
			
			if (winCondition()) {
				System.out.println("\nCongratulations! You win!");
				break;
			} //If the player reveals all non-mine tiles, they win (losing message will not trigger)
		}
		
		//If loop ends and a mine was revealed, the losing message plays
		if (revealedMine()) {
			System.out.println("\n< Game Over >");
		}
	}
	
	/**
	 * The following method sets Processing's display canvas to 50 pixels
	 * per grid unit on the X and Y axis. The update method.
	 */
	public void settings() { 
		size(GUR * gridWidth, GUR * gridHeight); 
	}
	
	public void setup() {
		size(GUR * gridWidth, GUR * gridHeight); 
		PFont f = createFont("Arial", 12, true);
		textFont(f, 12);
		img = loadImage("unnamed.png");
	}
	
	/**
	 * The following method draws minesweeper grid spaces at the specified
	 * coordinates x and y with Processing. Note this is designed to 
	 * correspond to the index values of a 2D array.
	 * 
	 * @param x
	 *            X coordinate at the top left of the rectangle to be drawn.
	 * @param y
	 *            Y coordinate at the top left of the rectangle to be drawn.
	 */
	public void drawGridSpace(int x, int y) {
		//Draw outer shade
		stroke(175);
		fill(150);
		rect(x * GUR, y * GUR, GUR, GUR);
		
		//Draw inner shade
		stroke(125);
		fill(125);
		rect(x * GUR + 5, y * GUR + 5, 40, 40);
	}
	
	/**
	 * This method draws minesweeper grid spaces with number values
	 * at spaces indicated by x and y coordinates, along with a 
	 * value that has a color associated with it's print.
	 * 
	 * @param x
	 *            X coordinate at the top left of the rectangle to be drawn.
	 * @param y
	 *            Y coordinate at the top left of the rectangle to be drawn.
	 * @param value
	 * 			  The value of the surrounding number of mines.
	 */
	public void drawGridValue(int x, int y, int value) {
		//Draw BG shade for tile
		stroke(175);
		fill(125);
		rect(x * GUR, y * GUR, GUR, GUR);
		
		int xDisplace = 23;
		int yDisplace = 30;
		
		//For each #, draw in the # with specific color in center
		switch (value) {
		case 0:
			//No fill for zero, just empty 
			break;
		case 1:
			fill(0, 0, 175);
			text("1", x * GUR + xDisplace, y * GUR + yDisplace);
			break;
		case 2:
			fill(0, 225, 0);
			text("2", x * GUR + xDisplace, y * GUR + yDisplace);
			break;
		case 3:
			fill(225, 0, 0);
			text("3", x * GUR + xDisplace, y * GUR + yDisplace);
			break;
		case 4:
			fill(0, 0, 225);
			text("4", x * GUR + xDisplace, y * GUR + yDisplace);
			break;
		case 5:
			fill(225, 55, 55);
			text("5", x * GUR + xDisplace, y * GUR + yDisplace);
			break;
		case 6:
			fill(0, 100, 225);
			text("6", x * GUR + xDisplace, y * GUR + yDisplace);
			break;
		case 7:
			fill(225, 25, 150);
			text("7", x * GUR + xDisplace, y * GUR + yDisplace);
			break;
		case 8:
			fill(225, 150, 25);
			text("8", x * GUR + xDisplace, y * GUR + yDisplace);
			break;
		}
	}
	
	public void drawMine(int x, int y) {
		int xDisplace = 3;
		int yDisplace = 3;
		//Draws the mine image from "unnamed.png", the mine image file
		image(img, x * GUR + xDisplace, y * GUR + yDisplace, GUR, GUR);	
	}
		
	public void drawFlag(int x, int y) {
		int xDisplace = 23;
		int yDisplace = 30;
		
		fill(255, 0, 0); 
		text("?", x * GUR + xDisplace, y * GUR + yDisplace);
	}

	
	/**
	 * This method uses Processing to draw the minefield to 
	 * the screen along with other parts of the game. 
	 * It then prints out the mine field to the user using the 3 2D arrays 
	 * (mineField, unknownTiles, revealedPositions).
	 * 
	 * @param mineField
	 *            The 2D array that holds the values corresponding to mine location.
	 * @param unknownTiles
	 *            The 2D array that holds char values that are printed when a tile has not yet
	 *            been reveal and can hold flags or waves.
	 * @param revealedPositions
	 * 			  The 2D array that holds boolean values that reflect whether or not
	 * 			  the user has revealed the contents of that tile.
	 */
	//Removed to make way for a better display with Processing
	public void draw() { 
		background(175);
		for (int i = 0; i < mineField.length; i++) {
			for (int j = 0; j < mineField[i].length; j++) {
				if (revealedPositions[i][j]) { //If the position has been revealed
					if (mineField[i][j] == 0) {
						drawGridValue(i, j, 0);
					} //If no mine, print a space for improved readability
					else if (mineField[i][j] == 9) {
						drawMine(i, j); 
					} //If mine, print an X to show the player they have revealed a mine
					else { 
						drawGridValue(i, j, mineField[i][j]);
					} //Else, print the number assigned to that index
				}
				else { //If the position has not been revealed
					drawGridSpace(i, j);
					if (unknownTiles[i][j] == '?') {
						drawFlag(i, j);
					} //Print a flag if one is there
				}
			}
		}
		return;
	}
	
	/**
	 * This method determines how many mines should be placed based on the gameDifficulty,
	 * its corresponding quotient value, and the size of the mineField.
	 * 
	 * @param mineField
	 *            The 2D array that holds the values corresponding to mine location.
	 * @param gameDifficulty
	 *            The user-selected game difficulty 1-3.
	 * @return The number of mines that the game ought to be initialized to.
	 */

	public static int determineInitialMines(int gameDifficulty) {
		int minMines = 0;
		
		//Determines how many mines will be placed as a function of selected size and difficulty
		switch (gameDifficulty) { 
		case 1:
			minMines = (int)(mineField.length * mineField[0].length / DIFFICULTY_BEGINNER);
			break;
		case 2:
			minMines = (int)(mineField.length * mineField[0].length / DIFFICULTY_INTERMEDIATE);
			break;
		case 3:
			minMines = (int)(mineField.length * mineField[0].length / DIFFICULTY_EXPERT);
			break;
		default:
			System.out.println("Error: Difficulty not found.");
			break; //Should not have to run if program calls correctly
		}
		return minMines;
	}
	
	/**
	 * This method populates mineField, initially with 0's and 9's (mines), then uses
	 * that intermediary array to re-populate it with numbers 0-8 in the initial 0-value
	 * indexes in order to form a complete underlying mineField array. Should only be 
	 * called one time in the main method.
	 * 
	 * @param mineField
	 *            The 2D array that will hold index values corresponding to the number
	 *            of mines adjacent/if the index is a mine.
	 * @param rdm
	 *            The Random object that will generate numbers to determine what is a mine.
	 * @param gameDifficulty
	 *            The user determined difficulty level 1-3.
	 * @param xPos
	 *            The user-selected initial xPos or column.
	 * @param yPos
	 *            The user-selected initial yPos or row.
	 */
	public static void seedMineField(Random rdm, 
			int gameDifficulty, int xPos, int yPos) {
		int minesLaid = 0;
		int minMines = determineInitialMines(gameDifficulty);
		int randomConstraint = 0;
		switch (gameDifficulty) { //Determines the contraint for rdm via difficulty
		case 1:
			randomConstraint = (int)DIFFICULTY_BEGINNER;
			break;
		case 2:
			randomConstraint = (int)DIFFICULTY_INTERMEDIATE;
			break;
		case 3:
			randomConstraint = (int)DIFFICULTY_EXPERT;
			break;
		default: 
			System.out.println("Error: Difficulty not found.");
			break; //Should not have to run if program calls correctly
		}
		
		while (minesLaid < minMines) { //Will stop if the mines laid have reached desired number
			for (int i = 0; i < mineField.length; i++) {
				for (int j = 0; j < mineField[i].length; j++) {
					if (i == xPos - 1 && j == yPos - 1) {
						mineField[i][j] = 0;
						continue; //If the tile is the 1st user selected tile, it can't be a mine
						//otherwise the player would instantly lose
					}
					if (rdm.nextInt(randomConstraint) > (randomConstraint - 2)) {
						//If the random object returns high enough, 
						//set the index to the mine value (9)
						mineField[i][j] = 9; 
						++minesLaid;
					}
					else {
						//Otherwise, set the index to have a value of 0 (for now)
						mineField[i][j] = 0;
					}
				}
			}
		} 
		
		//At this pint, mineField should be populated with a number of 0's and 9's
		//Now use 9's to determine the numbers (that aid the player) of their adjacent tiles
		int adjacentMines = 0;
		for (int i = 0; i < mineField.length; i++) {
			for (int j = 0; j < mineField[i].length; j++) {
				adjacentMines = 0; //Reset # of surrounding mines at each iteration
				if (mineField[i][j] == 9) {
					continue; //If a tile has a mine, skip over it - must not be modified
				}
				
				//Set default values for L R U D based on optimal scenario (non-border tile)
				int leftConstraint = i - 1, rightConstraint = i + 1;
				int upperConstraint = j - 1, lowerConstraint = j + 1;
				//Following conditionals adjust for border conditions ex. side or corner tile
				if (i - 1 < 0) {
					leftConstraint = i;
				}
				if (i + 1 >= mineField.length) {
					rightConstraint = i;
				}
				if (j - 1 < 0) {
					upperConstraint = j;
				}
				if (j + 1 >= mineField[i].length) {
					lowerConstraint = j;
				}
				
				for (int x = leftConstraint; x < rightConstraint + 1; x++) {
					for (int y = upperConstraint; y < lowerConstraint + 1; y++) {
						if (mineField[x][y] == 9) {
							++adjacentMines;
						}
					}
				}
				
				mineField[i][j] = adjacentMines; //Should set to 0-8, never 9 (mine) since
				//mineField[i][j] itself cannot be a (9) if the code reaches this point
			}
		}
		return;
	}
	
	/**
	 * This method determines which tiles flip to "revealed" in the revealedPositions
	 * array, which displayMineField method will then print out in accordance to
	 * which tiles have been revealed and which tiles still are to be shown - is the core
	 * mechanism of gameplay.
	 * 
	 * Algorithm - 
	 * 1. Check if the tile in question (xPos, yPos) is not revealed - if it is already revealed,
	 * 	  print a message to the user and inform them the tile is already revealed (do nothing)
	 * 2. If the tile has *not* been revealed, then start by revealing that first tile
	 * 3. If the user's tile is revealed to be a mine - immediately exit the method, as the 
	 * 	  user has lost and no other tiles need to be revealed
	 * 4. If the user's tile is a non-9 and a non-0 number (1-8) then only that tile should
	 *	  be revealed - exit the method here
	 * 5. If the user's tile is *not* a mine and *not* a 1-8 number (that is, if the user's
	 *    tile is a zero), then we can proceed by the following method to reveal other 
	 *    non-zero adjacent tiles, similar to hugging a wall in a maze to navigate
	 *    
	 *    *Remember, each time you perform any action on one of revealedArray's incidices, you 
	 *     MUST check if that indice is in the game's boundarys else exception error
	 * 		(1) First, reveal the 8 adjacent tiles to the user's zero-tile
	 * 		(2) Start by the tile directly downwards from the user's choice; if no
	 * 			such tile exists (the tile to be revealed is on the bottom edge) or
	 * 			the tile is nonzero then proceed to (4)
	 * 		(2) If the tile below is zero, move the method's position to that position and
	 * 			reveal the 8 tiles that are around it.
	 * 		(3) Continue until the tile below the one most recently revealed is non-zero
	 * 		(4) From here, start with the left and check if that tile is zero; 
	 * 			if it is, move to that tile and reveal around it; 
	 * 			if not, check right and do the same
	 * 			if both left and right are zeroes, default to <left>
	 * 			if neither, move upwards and start over from (4) to obtain a movement
	 * 				if the program continues moving up all the way to the upper limit or
	 * 				a nonzero without moving left or right, end the method there
	 * 		(5) After making the movement choice above and revealing, we are now on a new
	 * 			tile whose lower tile has not been confirmed a zero/non-zero
	 * 
	 * @param mineField
	 *            The populated mineField with 0-8 and 9/mines filled in.
	 * @param revealedPositions
	 *            The 2D array that tracks which positions have been revealed by the player.
	 * @return The boolean indicating whether or not a mine has been revealed.
	 */
	public static void checkFieldPosition(int xPos, int yPos) {
		//Adjust user input to array indices
		xPos -= 1;
		yPos -= 1;
		
		//If the position has been revealed prior
		if (revealedPositions[xPos][yPos]) {
			System.out.println("Already revealed this tile - choose another.");
			return;
		}
		else { 
			//If the tile to be revealed is a mine (9) or a non-zero indicator #
			if (mineField[xPos][yPos] != 0) {
				//Reveal that position and exit the method
				revealedPositions[xPos][yPos] = true;
				return;
			}
			//But if the user's tile to check *is* zero, then:
			else {
				//Create 2 Integer ArrayLists to hold values (linked by indexes) corresponding to
				//the coordinates of the 0's that must be checked
				ArrayList<Integer> xToCheck = new ArrayList<Integer>();
				ArrayList<Integer> yToCheck = new ArrayList<Integer>();
				revealedPositions[xPos][yPos] = true;
				
				//Call method that reveals the 8 positions adjacent to the
				//initial x and y position, then adds any tiles revealed
				//that have value 0 in corresponding mineField indices to the
				//ArrayLists for further use
				revealAdjacentTiles( 
						xPos, yPos, xToCheck, yToCheck);
				xToCheck.add(xPos);
				yToCheck.add(yPos);
				//Below variable starts at -1 so that it will always cause the 2nd part
				//of the below loop's expression to evaluate true
				int lastIterationSize = -1;
				//While there are still 0's to check and the size of xToCheck
				//is not the lastIterationSize (size of xToCheck at last iteration)
				while (xToCheck.size() > 0 && xToCheck.size() 
						!= lastIterationSize) {
					for (int i = 0; i < xToCheck.size(); i++) {
						//Set lastIterationSize to be the size before the ArrayLists
						//are potentially modified
						lastIterationSize = xToCheck.size();
						
						//For each matching index, run the method
						//to reveal and check if new 0's are found in the reveal
						revealAdjacentTiles( 
								xToCheck.get(i), yToCheck.get(i), xToCheck, yToCheck);
						//If the ArrayLists got new values, the loop conditional will
						//see that the ArrayList size is not equal to what it was before
						//the iteration, and will continue - ends once no new zeros are 
						//found by the method.
					}
				}
			}
		}
	}
	
	/**
	 * This method is built to be used in the checkMineField method, and reveals the 8 adjacent
	 * tiles to the tile in question, if such tiles are in bounds. Only to be called if xPos, yPos
	 * contains a 0 in mineField, meaning no mines will be revealed if the up-to 8 adjacent tiles
	 * are revealed.
	 * 
	 * @param mineField
	 *            The populated mineField with 0-8 and 9/mines filled in.
	 * @param revealedPositions
	 *            The 2D array that tracks which positions have been revealed by the player.
	 * @param xPos
	 * 			  Initial x position to check.
	 * @param yPos
	 * 			  Initial y position to check.
	 * @param xToCheck
	 * 			  ArrayList holding x coordinates of zero-valued indices in mineField
	 * 			  that the method reveals.
	 * @param yToCheck
	 * 			  ArrayList holding y coordinates of zero-valued indices in mineField
	 * 			  that the method reveals.
	 */
	public static void revealAdjacentTiles(int xPos, int yPos, 
			ArrayList<Integer> xToCheck, ArrayList<Integer> yToCheck) {
		//Moving cardinally counterclockwise starting at east, check if
		//that direction's adjacent tile is in bounds, and if it is, set it to be true
		boolean foundPair = false;
		if (xPos + 1 < revealedPositions.length) {//East of xPos, yPos
			revealedPositions[xPos + 1][yPos] = true;
			//If the adjacent value is zero - add it to the list of coordinates to check
			if (mineField[xPos + 1][yPos] == 0) {
				//Assume the pair coordinate in question doesn't exist in the 2 ArrayLists
				foundPair = false;
				for (int i = 0; i < xToCheck.size(); i++) {
					//If a pair of indexed values in xToCheck and yToCheck match the
					//positions in question, set foundPair to true
					if (xToCheck.get(i) == xPos + 1 && yToCheck.get(i) == yPos) {
						foundPair = true;
					}
				}
				//If no pair was found while parsing the ArrayLists, safely add
				//the determined new pair to their respective ArrayLists
				if (!foundPair) {
					xToCheck.add(xPos + 1);
					yToCheck.add(yPos);
				}
			}
		}
		if (xPos + 1 < revealedPositions.length && yPos - 1 >= 0) {//North-east
			revealedPositions[xPos + 1][yPos - 1] = true;
			if (mineField[xPos + 1][yPos - 1] == 0) {
				foundPair = false;
				for (int i = 0; i < xToCheck.size(); i++) {
					if (xToCheck.get(i) == xPos + 1 && yToCheck.get(i) == yPos - 1) {
						foundPair = true;
					}
				}
				if (!foundPair) {
					xToCheck.add(xPos + 1);
					yToCheck.add(yPos - 1);
				}
			}
		}
		if (yPos - 1 >= 0) {//North
			revealedPositions[xPos][yPos - 1] = true;
			if (mineField[xPos][yPos - 1] == 0) {
				foundPair = false;
				for (int i = 0; i < xToCheck.size(); i++) {
					if (xToCheck.get(i) == xPos && yToCheck.get(i) == yPos - 1) {
						foundPair = true;
					}
				}
				if (!foundPair) {
					xToCheck.add(xPos);
					yToCheck.add(yPos - 1);
				}
			}
		}
		if (xPos - 1 >= 0 && yPos - 1 >= 0) {//North-west
			revealedPositions[xPos - 1][yPos - 1] = true;
			if (mineField[xPos - 1][yPos - 1] == 0) {
				foundPair = false;
				for (int i = 0; i < xToCheck.size(); i++) {
					if (xToCheck.get(i) == xPos - 1 && yToCheck.get(i) == yPos - 1) {
						foundPair = true;
					}
				}
				if (!foundPair) {
					xToCheck.add(xPos - 1);
					yToCheck.add(yPos - 1);
				}
			}
		}
		if (xPos - 1 >= 0) {//West
			revealedPositions[xPos - 1][yPos] = true;
			if (mineField[xPos - 1][yPos] == 0) {
				if (mineField[xPos - 1][yPos] == 0) {
					foundPair = false;
					for (int i = 0; i < xToCheck.size(); i++) {
						if (xToCheck.get(i) == xPos - 1 && yToCheck.get(i) == yPos) {
							foundPair = true;
						}
					}
					if (!foundPair) {
						xToCheck.add(xPos - 1);
						yToCheck.add(yPos);
					}
				}
			}
		}
		if (xPos - 1 >= 0 && yPos + 1 < revealedPositions[0].length) {//South-west
			revealedPositions[xPos - 1][yPos + 1] = true;
			if (mineField[xPos - 1][yPos + 1] == 0) {
				if (mineField[xPos - 1][yPos + 1] == 0) {
					foundPair = false;
					for (int i = 0; i < xToCheck.size(); i++) {
						if (xToCheck.get(i) == xPos - 1 && yToCheck.get(i) == yPos + 1) {
							foundPair = true;
						}
					}
					if (!foundPair) {
						xToCheck.add(xPos - 1);
						yToCheck.add(yPos + 1);
					}
				}
			}
		}
		if (yPos + 1 < revealedPositions[0].length) {//South
			revealedPositions[xPos][yPos + 1] = true;
			if (mineField[xPos][yPos + 1] == 0) {
				foundPair = false;
				for (int i = 0; i < xToCheck.size(); i++) {
					if (xToCheck.get(i) == xPos && yToCheck.get(i) == yPos + 1) {
						foundPair = true;
					}
				}
				if (!foundPair) {
					xToCheck.add(xPos);
					yToCheck.add(yPos + 1);
				}
			}
		}
		if (xPos + 1 < revealedPositions.length && 
				yPos + 1 < revealedPositions[0].length) {//South-east
			revealedPositions[xPos + 1][yPos + 1] = true;
			if (mineField[xPos + 1][yPos + 1] == 0) {
				foundPair = false;
				for (int i = 0; i < xToCheck.size(); i++) {
					if (xToCheck.get(i) == xPos + 1 && yToCheck.get(i) == yPos + 1) {
						foundPair = true;
					}
				}
				if (!foundPair) {
					xToCheck.add(xPos + 1);
					yToCheck.add(yPos + 1);
				}
			}
		}
		return;
	}
	
	/**
	 * This method determines whether or not a mine has been revealed by the user, which
	 * should end the game if true - should be called at the start of in the game loop.
	 * 
	 * @param mineField
	 *            The populated mineField with 0-8 and 9/mines filled in.
	 * @param revealedPositions
	 *            The 2D array that tracks which positions have been revealed by the player.
	 * @return The boolean indicating whether or not a mine has been revealed.
	 */
	public static boolean revealedMine() {
		for (int i = 0; i < mineField.length; i++) {
			for (int j = 0; j < mineField[i].length; j++) {
				if (mineField[i][j] == 9 && revealedPositions[i][j]) {
					return true; //Should a position be revealed that is a mine, return true
				}
			}
		}
		return false; //By default, assume no mines have been revealed
	}
	
	public static void flagSquare(int xPos, int yPos) {
		xPos -= 1;
		yPos -= 1;
		if (unknownTiles[xPos][yPos] == '~') {
			unknownTiles[xPos][yPos] = '?';
		} //If wave, toggle to question mark (flag)
		else { 
			unknownTiles[xPos][yPos] = '~';
		} //If not wave (is question mark), then toggle back to wave (unflag)
		return;
	}
	
	/**
	 * This method prompts users for an integer with a given promptWord and requires that
	 * its value be between lowerBound and upperBound
	 * 
	 * @param scnr
	 *            The instance of the Scanner reading System.in.
	 * @param promptWord
	 *            The word that will tell the user what the value will be used for.
	 * @param lowerBound
	 *            The minimum acceptable number (inclusive).
	 * @param upperBound
	 *            The maximum acceptable number (inclusive).
	 * @return The number entered by the user between lowerBound and upperBound (inclusive).
	 */
	public static int promptUserForNumber(Scanner scnr, String promptWord, 
			int lowerBound, int upperBound) {
		int userNumber = -1; //Initialized (-1) since no part of this program deals with negative
		do {
		System.out.print("Enter a " + promptWord + ": ");
		if (!scnr.hasNextInt()) {
			System.out.println("Expected an integer but found: " 
				+ scnr.nextLine());
			continue; //Run the loop again if the input is not an integer and show the user
		}
		userNumber = scnr.nextInt();
		scnr.nextLine();
		if (userNumber > upperBound || userNumber < lowerBound) {
			System.out.println("Expected a number between " + lowerBound + " and " 
					+ upperBound + " but found : " + userNumber);
			continue; //Run the loop again if input is not in bounds, and show the user why
		}
		} while (userNumber > upperBound || userNumber < lowerBound);
		return userNumber;
	}
	
	/**
	 * This method prompts users for an integer with a given promptWord and requires that
	 * its value be between lowerBound and upperBound
	 * 
	 * @param mineField
	 *            The 2D array that holds the values corresponding to mine location.
	 * @param revealedPositions
	 * 			  The 2D array that holds boolean values that reflect whether or not
	 * 			  the user has revealed the contents of that tile.
	 * @return Boolean value representing whether the user wins yet or not.
	 */
	public static boolean winCondition () {
		for (int i = 0; i < mineField.length; i++) {
			for (int j = 0; j < mineField[i].length; j++) {
				if (!revealedPositions[i][j] && (mineField[i][j] >= 0 && mineField[i][j] < 9)) {
					return false;
				} //If any index with value 0-8 (no mine) is not revealed, no win yet
			}
		}
		return true; //Otherwise user wins
	}
}
