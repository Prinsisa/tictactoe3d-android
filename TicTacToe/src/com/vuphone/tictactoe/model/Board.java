package com.vuphone.tictactoe.model;

import java.net.Socket;

public class Board {

	public static int PLAYER_1 = 1;
	public static int PLAYER_2 = 2;
	private Integer currentTurn_ = 1;
	private static boolean inProgress_ = false;
	private static Board instance_ = null;
	private Socket sock_ = null;

	private int squares[][] = { { 0, 1, 1 }, { 1, 2, 2 }, { 0, 2, 0 } };

	protected Board() {

	}

	public static Board getInstance() {
		if (instance_ == null)
			instance_ = new Board();
		return instance_;
	}

	public int valueInSquare(int x, int y) {
		return squares[x][y];
	}

	public void setValueInSquare(int x, int y, int v) {
		squares[x][y] = v;
	}

	public int getWhosTurn() {
		return currentTurn_;
	}

	public void setWhosTurn(int turn) {
		currentTurn_ = turn;
		synchronized(currentTurn_){
			currentTurn_.notifyAll();
		}
	}

	public boolean isGameOver() {
		// Are all the spaces filled?
		// todo
		return false;
	}

	/**
	 * 
	 * @return playerID else 0
	 */
	public int getWinner() {
		// todo
		return 0;
	}

	public boolean isGameInProgress() {
		return inProgress_;
	}

	public void startNewGame() {
		clearBoard();
		inProgress_ = true;
		setWhosTurn(PLAYER_1);
	}

	private void clearBoard() {

	}
	
	public Socket getOpponentSocket(){
		return sock_;
	}
	
	public void setOpponentSocket(Socket s){
		sock_ = s;
	}
}
