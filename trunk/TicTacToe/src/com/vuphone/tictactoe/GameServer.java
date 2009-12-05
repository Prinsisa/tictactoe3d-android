package com.vuphone.tictactoe;

import java.io.IOException;
import java.io.InputStream;
import java.net.BindException;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import android.util.Log;

import com.vuphone.tictactoe.model.Board;

/**
 * 
 * @author Adam Albright, Ben Gotow
 * 
 */
public class GameServer extends Thread {

	private static GameServer instance_ = null;

	public static final int RESPONSE_NONE = 3;
	public static final int RESPONSE_DENY = 2;
	public static final int RESPONSE_ACCEPT = 1;
	public static final int RESPONSE_GAME_IN_PROGRESS = 4;

	private boolean SHUTDOWN = false;
	private ServerSocket s_ = null;

	private String listening_ip_ = null;
	private int PORT = 1234;

	private final String cmdGameRequest = "<cmd>REQUEST-NEW-GAME</cmd>";
	private final String cmdAcceptGame = "<cmd>ACCEPT-GAME-REQUEST</cmd>";
	private final String cmdDenyGame = "<cmd>DENY-GAME-REQUEST</cmd>";
	private final String cmdGameInProgress = "<cmd>GAME-IN-PROGRESS</cmd>";
	private final String cmdPlayerExited = "<cmd>PLAYER-EXITED-GAME</cmd>";
	private final String cmdBoardUpdate = "<cmd><boardupdate/>";

	public GameServer() {
		super("GameServer");
	}

	@Override
	public void run() {
		try {
			s_ = new ServerSocket(PORT);
			s_.setSoTimeout(2000);
		} catch (BindException e) {
			Log.d("mad", " %%%Port is already in use%%%");
			return;
		} catch (Exception e) {
		}

		Log.d("mad", "[*] Listening for game requests on port " + PORT + "...");

		// lets find our IP address
		try {
			Socket socket = new Socket("www.google.com", 80);
			socket.setSoTimeout(2500);
			listening_ip_ = socket.getLocalAddress().toString().substring(1);
			socket.close();
		} catch (Exception e) {
			try {
				listening_ip_ = InetAddress.getLocalHost().getHostAddress();
			} catch (UnknownHostException e1) {
				listening_ip_ = "No internet connection";
			}
		}

		Log.d("mad", "IP Addr: " + listening_ip_);

		/*
		 * Accept TCP socket connections
		 */
		while (!SHUTDOWN) {
			try {

				final Socket sock = s_.accept();

				// Spit out next data
				Log.d("mad", "Incoming game request from "
						+ sock.getRemoteSocketAddress());

				if (Board.getInstance().isGameInProgress()) {
					sendCmd(sock, cmdGameInProgress);
					sock.close();
				} else {

					LobbyActivity.uiThreadCallback.post(new Runnable() {
						public void run() {
							LobbyActivity.getInstance().incomingGameRequestCB(
									sock);
						}
					});
				}
			} catch (Exception e) {
				// We want accept to throw exceptions after timeout to keep from
				// blocking indefinitely
			}
		}

		Log.d("mad", "[*] Data server shutting down!");

		try {
			s_.close();
			s_ = null;
		} catch (IOException e) {
		}
	}

	public void die() {
		SHUTDOWN = true;
	}

	public String buildBoardUpdateCmd(int x, int y) {
		return cmdBoardUpdate + x + "," + y + "</cmd>";
	}

	public void sendGameRequest(final String remoteAddr, final int remotePort) {

		// spawn a new thread to send game request
		// use guiHandler to callback about response

		new Thread(new Runnable() {
			public void run() {
				final Socket sock = sendRequest(remoteAddr, remotePort);
				LobbyActivity.uiThreadCallback.post(new Runnable() {
					public void run() {
						LobbyActivity.getInstance().deliveredRequestCB(
								(sock == null) ? false : true);

						if (sock == null)
							return;

						new Thread(new Runnable() {

							public void run() {
								final int result = waitOnGameRequest(sock);
								LobbyActivity.uiThreadCallback
										.post(new Runnable() {
											public void run() {
												LobbyActivity.getInstance()
														.requestResponseCB(
																result, sock);
											}
										});
							}
						}).start();
					}
				});
			}
		}).start();
	}

	private Socket sendRequest(String remoteAddr, int remotePort) {
		// write the object
		Socket sock = getSocket(remoteAddr, remotePort);
		if (sock == null) {
			Log.d("mad", "Couldn't open a socket!");
			return null;
		}

		Log.d("mad", "Sending request to " + sock.getRemoteSocketAddress());

		if (sendCmd(sock, cmdGameRequest))
			return sock;
		else
			return null;
	}

	public void responseToRequest(Socket sock, boolean iaccept) {
		try {
			if (iaccept)
				sendCmd(sock, cmdAcceptGame);
			else {
				sendCmd(sock, cmdDenyGame);
				sock.close();
			}
		} catch (IOException e) {

		}
	}

	private int waitOnGameRequest(Socket sock) {
		String s = readCmdFromSocket(sock, 8);
		if (s != null)
			Log.d("mad", "Got a packet! " + s);

		if (s == null)
			return RESPONSE_NONE;
		else if (s.equals(cmdAcceptGame))
			return RESPONSE_ACCEPT;
		else if (s.equals(cmdDenyGame))
			return RESPONSE_DENY;
		else if (s.equals(cmdGameInProgress))
			return RESPONSE_GAME_IN_PROGRESS;
		else
			return RESPONSE_NONE;
	}

	public String readCmdFromSocket(Socket sock, int timeoutSEC) {
		if(sock == null)
			return null;
		
		InputStream in;

		try {
			sock.setSoTimeout(2000);
			in = sock.getInputStream();
		} catch (IOException e1) {
			e1.printStackTrace();
			return null;
		}

		StringBuilder xml = new StringBuilder();
		Pattern regex = Pattern.compile("(<cmd>.+</cmd>)", Pattern.DOTALL);
		Matcher regexMatcher = null;
		int c = 0;

		// Waits until a packet is received between START & END flags
		while (in != null && sock.isConnected()) {
			try {
				c = in.read();
				if (c == -1) {
					in.close();
					in = null;
				}
			} catch (Exception e) {
				// Exception is thrown once a second
				--timeoutSEC;
				if (timeoutSEC == 0)
					return null;
				else
					continue;
			}

			xml.append((char) c);

			if (((char) c) == '>') {
				regexMatcher = regex.matcher(xml);
				if (regexMatcher.find())
					return regexMatcher.group(1);
			}
		}

		return null;
	}

	private Socket getSocket(String remoteAddr, int remotePort) {
		Socket sock = null;

		Log.d("mad", "Opening a socket to " + remoteAddr + ":" + remotePort);

		try {
			sock = new Socket(remoteAddr, remotePort);

			if (!sock.isConnected())
				return null;
		} catch (Exception e) {
			return null;
		}

		return sock;
	}

	public static GameServer getInstance() {
		if (instance_ == null) {
			instance_ = new GameServer();
			instance_.start();
		}

		return instance_;
	}

	public String getMyIP() {
		int count = 10;
		while ((--count) > 0 && listening_ip_ == null) {
			try {
				Thread.sleep(500);
			} catch (Exception e) {
			}
		}

		return listening_ip_;
	}

	public boolean sendCmd(Socket sock, String cmd) {
		try {
			sock.getOutputStream().write(cmd.getBytes());
			return true;
		} catch (Exception e) {
			System.err.println("Error! Connection to opponent lost!");
			e.printStackTrace();
			return false;
		}
	}

	public void sendPlayerExited(Socket sock) {
		sendCmd(sock, cmdPlayerExited);
	}

	public void parseInGameCmd(String s) {
		Log.d("mad", "InGameCmd received! " + s);

	}
}
