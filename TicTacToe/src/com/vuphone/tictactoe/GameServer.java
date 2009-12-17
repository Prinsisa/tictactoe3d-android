package com.vuphone.tictactoe;

import java.io.IOException;
import java.io.InputStream;
import java.net.BindException;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Properties;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import android.util.Log;

import com.vuphone.tictactoe.model.Board;
import com.vuphone.tictactoe.network.NetworkManager;

/**
 * 
 * @author Adam Albright, Ben Gotow
 * 
 */
public class GameServer extends Thread {

	private static final int PING_TIMEOUT_MS = 1200;

	private static int SUBNET_COUNT = 4;

	public static final int PEER_THREAD_COUNT = 5;

	private static GameServer instance_ = null;

	public static final int RESPONSE_NONE = 3;
	public static final int RESPONSE_DENY = 2;
	public static final int RESPONSE_ACCEPT = 1;
	public static final int RESPONSE_GAME_IN_PROGRESS = 4;

	private boolean STOP_PING_LAN = false;
	private boolean SHUTDOWN = false;
	private ServerSocket s_ = null;

	private String listening_ip_ = null;
	private int PORT = 1234;

	private final String cmdGameRequest = "<cmd>REQUEST-NEW-GAME</cmd>";
	private final String cmdAcceptGame = "<cmd>ACCEPT-GAME-REQUEST</cmd>";
	private final String cmdDenyGame = "<cmd>DENY-GAME-REQUEST</cmd>";
	public final String cmdCancelGame = "<cmd>CANCEL-GAME-REQUEST</cmd>";
	private final String cmdGameInProgress = "<cmd>GAME-IN-PROGRESS</cmd>";
	public final String cmdPlayerExited = "<cmd>PLAYER-EXITED-GAME</cmd>";
	private final String cmdBoardUpdate = "<cmd><boardupdate/>";
	public final String cmdGameOver = "<cmd>GAME-OVER</cmd>";
	private final String cmdPing = "<cmd>PING</cmd>";
	private final String cmdHello = "<cmd><hello/>";

	public ArrayList<Properties> helloList = new ArrayList<Properties>();

	public AtomicInteger peerThreadsComplete = new AtomicInteger(0);

	private NetworkManager networkManager_ = NetworkManager.getInstance();

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

		/*
		 * Accept TCP socket connections
		 */
		while (!SHUTDOWN) {
			try {

				final Socket sock = s_.accept();

				// Spit out next data
				Log.d("mad", "Incoming game request from "
						+ sock.getRemoteSocketAddress());

				String cmd = readCmdFromSocket(sock, 1000);
				if (cmd == null)
					continue;

				if (cmd.equals(cmdPing)) {
					Log.d("mad", "You just got pinged!");
					sendCmd(sock, buildHelloCmd());
					sock.close();

				}
				// See if a game is in progress or a request is currently active
				else if (Board.getInstance().isGameInProgress()
						|| LobbyActivity.getInstance().activeRequestDialog != null) {
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

	public String buildHelloCmd() {
		return cmdHello
				+ Settings.getInstance().getString(Settings.DISPLAY_NAME, "")
				+ "</cmd>";
	}

	public String getRemoteIP(Socket sock) {
		String ip = sock.getRemoteSocketAddress().toString();
		return ip.substring(0, ip.indexOf('/'));
	}

	public void sendGameRequest(final String remoteAddr) {

		// spawn a new thread to send game request
		// use guiHandler to callback about response

		new Thread(new Runnable() {
			public void run() {
				final Socket sock = sendRequest(remoteAddr, PORT);
				LobbyActivity.uiThreadCallback.post(new Runnable() {
					public void run() {
						LobbyActivity.getInstance().deliveredRequestCB(sock);

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

	public void findPeers() {
		new Thread(new Runnable() {
			public void run() {
				pingTheLan();
			}
		}).start();
	}

	private void pingTheLan() {
		if (!networkManager_.isConnectionWorking())
			return;

		peerThreadsComplete.set(0);
		STOP_PING_LAN = false;

		// Just scan the 243 machines in the last octect for now
		final String myIP = networkManager_.getIpAddress();
		if (!networkManager_.isIpInternal(myIP))
			return;

		String digits[] = myIP.split("\\.");
		String baseIP = digits[0] + "." + digits[1] + ".";

		int thirdOctet = Integer.parseInt(digits[2]);
		int thirdOctetEven = (int) Math.floor(thirdOctet / 4.0) * 4;

		String netmask = networkManager_.getSubnetMask();
		if (netmask == null)
			return;

		Log.d("mad", " Scanning with subnet mask: " + netmask);

		if (netmask.startsWith("255.255.255")) {
			// Scans 255 IPs
			SUBNET_COUNT = 1;
			pingTheSubnet(baseIP + thirdOctet, myIP);

		} else if (netmask.startsWith("255.255.254")) {
			// Scans 511 IPs
			SUBNET_COUNT = 2;

			// 123.123.124.x
			pingTheSubnet(baseIP + (thirdOctetEven + 0), myIP);

			// 123.123.125.x
			pingTheSubnet(baseIP + (thirdOctetEven + 1), myIP);

		}

		else // if (netmask.startsWith("255.255.252")) {
		{
			// Scans 1023 IPs
			SUBNET_COUNT = 4;

			// 123.123.124.x
			pingTheSubnet(baseIP + (thirdOctetEven + 0), myIP);

			// 123.123.125.x
			pingTheSubnet(baseIP + (thirdOctetEven + 1), myIP);

			// 123.123.126.x
			pingTheSubnet(baseIP + (thirdOctetEven + 2), myIP);

			// 123.123.127.x
			pingTheSubnet(baseIP + (thirdOctetEven + 3), myIP);
		}
	}

	private void pingTheSubnet(final String baseIP, final String myIP) {
		// spawn 17 threads to ping the subnet
		for (int i = 0; i < PEER_THREAD_COUNT; ++i) {
			final int start = i * 255 / PEER_THREAD_COUNT;
			final int end = start + 255 / PEER_THREAD_COUNT;

			new Thread(new Runnable() {
				public void run() {
					for (int j = start; j < end; ++j) {
						if (STOP_PING_LAN == true)
							break;
						String node = baseIP + "." + j;
						if (!node.equals(myIP))
							pingMachine(node);
					}
					pingTheSubnetComplete();
				}
			}).start();
		}
	}

	private synchronized void pingTheSubnetComplete() {

		if (peerThreadsComplete.incrementAndGet() >= PEER_THREAD_COUNT
				* SUBNET_COUNT) {
			LobbyActivity.uiThreadCallback.post(new Runnable() {
				public void run() {
					LobbyActivity.getInstance().findPlayersFinished();
				}
			});
		}
	}

	private boolean pingMachine(String ip) {
		Log.d("mad", "Pinging: " + ip);
		Socket sock = new Socket();

		try {
			sock.connect(new InetSocketAddress(ip, PORT), PING_TIMEOUT_MS);
			// timeout
		} catch (Exception e) {
			return false;
		}

		sendCmd(sock, cmdPing);

		String cmd = readCmdFromSocket(sock, 2);
		try {
			sock.close();
		} catch (IOException e) {
		}

		if (cmd != null && cmd.substring(0, cmdHello.length()).equals(cmdHello)) {
			String name = cmd.replaceAll("<[^<>]+>", "");

			synchronized (helloList) {
				Properties p = new Properties();
				p.put("ip", ip);
				p.put("name", name);
				helloList.add(p);

				helloList.notifyAll();
			}

			LobbyActivity.uiThreadCallback.post(new Runnable() {
				public void run() {
					LobbyActivity.getInstance().findPlayersCountUpdated();
				}
			});

			Log.d("mad", "Found opponent: " + name);
			return true;
		}

		return false;
	}

	public void stopPingTheLan() {
		STOP_PING_LAN = true;
		LobbyActivity.getInstance().findPlayersFinished();
	}

	public synchronized String updateIPAddress() {
		listening_ip_ = networkManager_.getIpAddressFromTest();

		if (!networkManager_.isWifiEnabled())
			Log.d("mad", "  Wifi is disabled!");

		if (listening_ip_ == null && networkManager_.isWifiEnabled() == true) {
			if (networkManager_.getIpAddressWifi() != null) {
				Log.d("mad", " *Internet is goofy. Rebooting wifi...");

				networkManager_.reassociateWifi();
			}
		}

		if (listening_ip_ == null)
			listening_ip_ = "No internet connection";
		else
			Log.d("mad", "IP Addr: " + listening_ip_);

		return listening_ip_;
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

	public void cancelRequest(Socket sock) {
		try {
			sendCmd(sock, cmdCancelGame);
			sock.close();
		} catch (IOException e) {
		}
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
		String s = null;
		while (s == null && !sock.isClosed())
			s = readCmdFromSocket(sock, 1);

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
		if (sock == null)
			return null;

		InputStream in;

		try {
			sock.setSoTimeout(2000);
			in = sock.getInputStream();
		} catch (IOException e1) {
			e1.printStackTrace();
			return "-1";
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

	public boolean sendCmd(Socket sock, String cmd) {
		try {
			sock.getOutputStream().write(cmd.getBytes());
			return true;
		} catch (Exception e) {
			Log.d("mad", "Error! Connection to opponent lost!");
			try {
				sock.close();
			} catch (Exception e1) {
			}

			return false;
		}
	}

	public void parseInGameCmd(String s) {
		Log.d("mad", "InGameCmd received! " + s);
		Board board = Board.getInstance();

		int updateLen = cmdBoardUpdate.length();
		if (s.substring(0, updateLen).equals(cmdBoardUpdate)) {
			String v = s.substring(updateLen, updateLen + 3);
			int x = v.charAt(0) - 48;
			int y = v.charAt(2) - 48;
			board.setValueByOpponent(x, y);
		} else if (s.equals(cmdPlayerExited)) {
			if (board.isGameInProgress()) {
				GameActivity.uiThreadCallback.post(new Runnable() {
					public void run() {
						GameActivity.echo("Your opponent exited the game!");
					}
				});

				board.setWinner(board.getMyPlayerID());
				board.endGame();
			}
		}
	}
}
