package com.vuphone.tictactoe;

import java.io.IOException;
import java.io.InputStream;
import java.net.BindException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.NetworkInterface;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.Properties;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import android.net.DhcpInfo;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.util.Log;

import com.vuphone.tictactoe.model.Board;

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
	private Boolean determining_ip = false;
	private final Object waitForIPLock = new Object();

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

	public static String nameOfPlayer;
	public ArrayList<Properties> helloList = new ArrayList<Properties>();

	public AtomicInteger peerThreadsComplete = new AtomicInteger(0);

	public WifiManager wifiManager = null;

	public GameServer() {
		super("GameServer");
	}

	@Override
	public void run() {
		getMyIP();
		Log.d("mad", "IP Addr: " + listening_ip_);

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

				} else if (Board.getInstance().isGameInProgress()) {
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
		return cmdHello + nameOfPlayer + "</cmd>";
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
		if (!isInternetEnabled())
			return;

		peerThreadsComplete.set(0);
		STOP_PING_LAN = false;

		// Just scan the 243 machines in the last octect for now
		final String myIP = getMyIP();

		String digits[] = myIP.split("\\.");
		String baseIP = digits[0] + "." + digits[1] + ".";

		int thirdOctet = Integer.parseInt(digits[2]);

		thirdOctet = (int) Math.floor(thirdOctet / 4.0) * 4;
		Log.d("mad", "Last digit = " + thirdOctet);

		SUBNET_COUNT = 4;

		// 123.123.124.x
		pingTheSubnet(baseIP + (thirdOctet + 0), myIP);

		// 123.123.125.x
		pingTheSubnet(baseIP + (thirdOctet + 1), myIP);

		// 123.123.126.x
		pingTheSubnet(baseIP + (thirdOctet + 2), myIP);

		// 123.123.127.x
		pingTheSubnet(baseIP + (thirdOctet + 3), myIP);
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
		synchronized (waitForIPLock) {
			determining_ip = true;
		}

		// lets find our IP address
		try {
			Socket socket = new Socket("www.google.com", 80);
			listening_ip_ = socket.getLocalAddress().toString().substring(1);
			socket.close();
		} catch (Exception e) {
			try {
				listening_ip_ = InetAddress.getLocalHost().getHostAddress();
			} catch (UnknownHostException e1) {
				listening_ip_ = "No internet connection";
			}
		}

		if (listening_ip_ == null || listening_ip_.equals("127.0.0.1"))
			listening_ip_ = "No internet connection";

		synchronized (waitForIPLock) {
			determining_ip = false;
			waitForIPLock.notifyAll();
		}

		if (!isInternetEnabled()) {

			if (wifiManager.isWifiEnabled() == true) {

				WifiInfo info = wifiManager.getConnectionInfo();
				DhcpInfo dhcp = wifiManager.getDhcpInfo();
				String ip, netmask = "";
				if (info != null) {
					int i = info.getIpAddress();

					if (i != 0 && (ip = wifiInt2String(i)) != null) {
						Log.d("mad", " *Internet is goofy. Rebooting wifi");

						wifiManager.reassociate();

						if (dhcp != null)
							netmask = wifiInt2String(dhcp.netmask);

						Log.d("mad", " WIFI IP:" + ip + " |Netmask:" + netmask);
					}
				}
			}
		}

		Log.d("mad", "   Iterator says our IP is: " + getLocalIpAddress());
		return listening_ip_;
	}

	private String wifiInt2String(int ip) {
		String str = null;
		byte[] byteaddr = new byte[] { (byte) (ip & 0xff),
				(byte) (ip >> 8 & 0xff), (byte) (ip >> 16 & 0xff),
				(byte) (ip >> 24 & 0xff) };
		try {
			str = InetAddress.getByAddress(byteaddr).getHostAddress();
		} catch (Exception e) {
			// TODO: handle exception
		}

		return str;
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
		while(s == null && !sock.isClosed())
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

	public String getMyIP() {
		if (listening_ip_ != null)
			return listening_ip_;

		try {
			synchronized (waitForIPLock) {
				if (!determining_ip)
					updateIPAddress();
				else
					waitForIPLock.wait(2000);
			}
		} catch (InterruptedException e) {
		}

		return listening_ip_;
	}

	public String getLocalIpAddress() {
		try {
			for (Enumeration<NetworkInterface> en = NetworkInterface
					.getNetworkInterfaces(); en.hasMoreElements();) {
				NetworkInterface intf = en.nextElement();
				for (Enumeration<InetAddress> enumIpAddr = intf
						.getInetAddresses(); enumIpAddr.hasMoreElements();) {
					InetAddress inetAddress = enumIpAddr.nextElement();
					if (!inetAddress.isLoopbackAddress()) {
						return inetAddress.getHostAddress().toString();
					}
				}
			}
		} catch (SocketException ex) {
			Log.e("mad", ex.toString());
		}
		return null;
	}

	public boolean isInternetEnabled() {
		if (listening_ip_ == null)
			return false;

		if (listening_ip_.charAt(0) == 'N')
			return false;

		return true;
	}

	public boolean sendCmd(Socket sock, String cmd) {
		try {
			sock.getOutputStream().write(cmd.getBytes());
			return true;
		} catch (Exception e) {
			Log.d("mad", "Error! Connection to opponent lost!");
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
			board.setWinner(board.getMyPlayerID());
			board.endGame();
		}
	}
}
