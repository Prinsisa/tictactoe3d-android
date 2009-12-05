package com.vuphone.tictactoe.model;

import java.net.Socket;

import com.vuphone.tictactoe.GameServer;

public class Board {

	public static int PLAYER_1 = 1;
	public static int PLAYER_2 = 2;
	private int PLAYER_ME = 0;
	private int WINNER = 0;
	
	private Integer currentTurn_ = 1;
	private static boolean inProgress_ = false;
	private static Board instance_ = null;
	private Socket sock_ = null;
	private static GameServer gameServer = GameServer.getInstance();
	
	private int squares[][] = { { 0, 0, 0 }, { 0, 0, 0 }, { 0, 0, 0 } };

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

	public void setValueInSquare(int x, int y) {
		// is this piece already set?
		if (squares[x][y] != 0)
			return;

		if (isNetworkedGame() && currentTurn_ == PLAYER_ME) {
			squares[x][y] = currentTurn_;
			setWhosTurn(PLAYER_ME == 1 ? 2 : 1);

			// notify remote user of change
			String cmd = gameServer.buildBoardUpdateCmd(x, y);
			gameServer.sendCmd(sock_, cmd);
			
			if(isGameOver())
				endGame();			
		} else if (!isNetworkedGame()) {
			squares[x][y] = currentTurn_;
			setWhosTurn(currentTurn_ == 1 ? 2 : 1);
		}
	}

	public void setValueByOpponent(int x, int y){
		squares[x][y] = currentTurn_;
		setWhosTurn(PLAYER_ME);
		if(isGameOver())
			endGame();
	}
	
	public void setWinner(int player){
		WINNER = player;
	}
	
	public int getWhosTurn() {
		return currentTurn_;
	}

	public int getMyPlayerID() {
		return PLAYER_ME;
	}

	public void setWhosTurn(int turn) {
		currentTurn_ = turn;
		synchronized (currentTurn_) {
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
		return WINNER;
	}

	public boolean isGameInProgress() {
		return inProgress_;
	}

	public void startNewGame(int playerNum) {
		inProgress_ = true;
		WINNER = 0;
		clearBoard();
		PLAYER_ME = playerNum;
		setWhosTurn(PLAYER_1);

		listenOnSocket();
	}

	private void clearBoard() {
		for (int x = 0; x < 3; ++x)
			for (int y = 0; y < 3; ++y)
				squares[x][y] = 0;
	}

	public Socket getOpponentSocket() {
		return sock_;
	}

	public void setOpponentSocket(Socket s) {
		sock_ = s;
	}

	public boolean isNetworkedGame() {
		return (sock_ == null) ? false : true;
	}

	public boolean isMyTurn() {
		if (isNetworkedGame())
			return getMyPlayerID() == getWhosTurn();
		else
			return true;
	}

	public void endGame() {
		inProgress_ = false;
		GameServer gs = GameServer.getInstance();
		
		if (isGameOver()) {
			// game finished normally
			gs.sendCmd(sock_, gs.cmdGameOver);
		} else {
			// we exited prematurely
			gs.sendCmd(sock_, gs.cmdPlayerExited);
		}

		sock_ = null;
	}

	private void listenOnSocket() {
		final GameServer gameServer = GameServer.getInstance();

		// spawn a thread to listen for network activity
		new Thread(new Runnable() {
			public void run() {
				while (Board.getInstance().isGameInProgress()) {
					String cmd = gameServer.readCmdFromSocket(sock_, 5);
					if (cmd != null)
						gameServer.parseInGameCmd(cmd);
				}
			}
		}).start();
	}
}
