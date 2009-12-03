package com.vuphone.tictactoe.model;

public class Board {

	public static int PLAYER_1 = 1;
	public static int PLAYER_2 = 2;
	
	private int squares[][] = {{0,1,1},{1,2,2},{0,2,0}};
	
	public Board () {
	
	}
	
	public int valueInSquare(int x, int y)
	{
		return squares[x][y];
	}
	
	public void setValueInSquare(int x, int y, int v)
	{
		squares[x][y] = v;
	}
}
