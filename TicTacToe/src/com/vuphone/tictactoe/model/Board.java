package com.vuphone.tictactoe.model;

import java.net.Socket;

import android.util.Log;

import com.vuphone.tictactoe.BoardGLView;
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
	private static BoardGLView paintView_ = null;

	private int squares[][] = { { 0, 0, 0 }, { 0, 0, 0 }, { 0, 0, 0 } };
	private int winSquares[][] = { { 0, 0, 0 }, { 0, 0, 0 }, { 0, 0, 0 } };

	protected Board() {

	}

	public static Board getInstance() {
		if (instance_ == null)
			instance_ = new Board();
		return instance_;
	}

	public boolean winInSquare(int x, int y) {
		return (winSquares[x][y] == 1);
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

		} else if (!isNetworkedGame()) {
			squares[x][y] = currentTurn_;
			setWhosTurn(currentTurn_ == 1 ? 2 : 1);
		}

		setWinner(findWinner());
		checkForFullBoard();

		paintView_.animatePieceDrop(x, y);
	}

	public void setValueByOpponent(int x, int y) {
		squares[x][y] = currentTurn_;
		setWhosTurn(PLAYER_ME);

		setWinner(findWinner());
		checkForFullBoard();

		paintView_.animatePieceDrop(x, y);
	}

	public void setWinner(int player) {
		WINNER = player;

		if (player != 0)
			endGame();
	}

	public void checkForFullBoard() {
		int count = 0;
		for (int x = 0; x < 3; ++x)
			for (int y = 0; y < 3; ++y)
				if (squares[x][y] == 0)
					++count;

		// is the board full
		if (count == 0)
			endGame();
	}

	public int findWinner() {

		// look for chains of three across
		for (int y = 0; y < 3; y++) {
			if ((squares[0][y] == squares[1][y])
					&& (squares[0][y] == squares[2][y]) && (squares[0][y] != 0)) {
				winSquares[0][y] = 1;
				winSquares[1][y] = 1;
				winSquares[2][y] = 1;
				return squares[0][y];
			}
		}
		// look for chains of three down
		for (int x = 0; x < 3; x++) {
			if ((squares[x][0] == squares[x][1])
					&& (squares[x][0] == squares[x][2]) && (squares[x][0] != 0)) {
				winSquares[x][0] = 1;
				winSquares[x][1] = 1;
				winSquares[x][2] = 1;
				return squares[x][0];
			}
		}
		// check the diagonals
		if ((squares[0][0] == squares[1][1])
				&& (squares[1][1] == squares[2][2]) && (squares[1][1] != 0)) {
			winSquares[0][0] = winSquares[1][1] = winSquares[2][2] = 1;
			return squares[1][1];
		}
		if ((squares[2][0] == squares[1][1])
				&& (squares[1][1] == squares[0][2]) && (squares[1][1] != 0)) {
			winSquares[2][0] = winSquares[1][1] = winSquares[0][2] = 1;
			return squares[1][1];
		}

		return 0;
	}

	public int getWhosTurn() {
		return currentTurn_;
	}

	public int getMyPlayerID() {
		if (this.isNetworkedGame())
			return PLAYER_ME;
		else if (this.WINNER != 0)
			return this.getWinner();
		else
			return PLAYER_ME;
	}

	public void setWhosTurn(int turn) {
		currentTurn_ = turn;
		synchronized (currentTurn_) {
			currentTurn_.notifyAll();
		}
	}

	/**
	 * Alias for !isGameInProgress
	 * 
	 * @return
	 */
	public boolean isGameOver() {
		return !inProgress_;
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

		if (isNetworkedGame())
			listenOnSocket();
	}

	private void clearBoard() {
		for (int x = 0; x < 3; ++x)
			for (int y = 0; y < 3; ++y)
				squares[x][y] = 0;
		for (int x = 0; x < 3; ++x)
			for (int y = 0; y < 3; ++y)
				winSquares[x][y] = 0;
	}

	public Socket getOpponentSocket() {
		return sock_;
	}

	public void setOpponentSocket(Socket s) {
		if(sock_ != null){
			try {
				sock_.close();
			} catch (Exception e) {
			}
		}
		
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

		if (isNetworkedGame()) {
			GameServer gs = GameServer.getInstance();
			gs.sendCmd(sock_, gs.cmdGameOver);
		}
	}

	private void listenOnSocket() {
		final GameServer gameServer = GameServer.getInstance();

		// spawn a thread to listen for network activity
		new Thread(new Runnable() {
			public void run() {
				while (Board.getInstance().isGameInProgress()) {
					String cmd = gameServer.readCmdFromSocket(sock_, 5);
					if ((sock_ == null) || !sock_.isConnected()
							|| (cmd != null && cmd.equals("-1"))) {
						Log.d("mad", "Socket connection was lost mid-game!");
						gameServer.parseInGameCmd(gameServer.cmdPlayerExited);
					} else if (cmd != null) {
						gameServer.parseInGameCmd(cmd);
					}

				}
			}
		}).start();
	}

	public void prematureEndGame() {
		if (isNetworkedGame()) {
			GameServer gs = GameServer.getInstance();
			gs.sendCmd(sock_, gs.cmdPlayerExited);
		}
		endGame();
	}

	public BoardGLView paintView() {
		return paintView_;
	}

	public void setPaintView(BoardGLView paintView) {
		paintView_ = paintView;
	}
}
