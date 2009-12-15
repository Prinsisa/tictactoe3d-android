package com.vuphone.tictactoe;

import java.net.Socket;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Service;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.res.Configuration;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Vibrator;
import android.telephony.TelephonyManager;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.animation.AlphaAnimation;
import android.view.animation.Animation;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.vuphone.tictactoe.model.Board;

public class LobbyActivity extends Activity implements OnClickListener {

	private static final int MENU_ABOUT = 4;
	private static final int MENU_BABES = 3;
	private static final int MENU_SCAN = 2;
	private static final int MENU_SETTINGS = 1;
	private static Context context_ = null;
	private static LobbyActivity instance_ = null;
	private static Boolean animateBtnFindPlayers_ = false;
	private static final int animateBtnFindPlayersDelay = 2000;

	public static Button btnStart_ = null;
	public static Button btnFindPlayers_ = null;
	static final Handler uiThreadCallback = new Handler();
	final GameServer gameServer = GameServer.getInstance(); 

	/**
	 * Called when the activity is first created and again after the apps goes
	 * offscreen and resumes
	 */

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		Log.d("mad","   super.onCreate()");
		
		setContentView(R.layout.main);

		context_ = getBaseContext();
		instance_ = this;
		btnStart_ = ((Button) findViewById(R.id.btnSendRequest));
		btnFindPlayers_ = ((Button) findViewById(R.id.btnPeers));

		btnStart_.setOnClickListener(this);
		btnFindPlayers_.setOnClickListener(this);
		btnFindPlayers_.setText("Find local peers");
		((Button) findViewById(R.id.btnSinglePlayer)).setOnClickListener(this);

		gameServer.wifiManager = (WifiManager) getSystemService(Context.WIFI_SERVICE); 
		
		// Display the IP address
		TelephonyManager t = ((TelephonyManager) getSystemService(Context.TELEPHONY_SERVICE));
		if (t != null && t.getLine1Number() != null)
			GameServer.nameOfPlayer = t.getLine1Number();
		else
			GameServer.nameOfPlayer = "TicTacToe Player";

		updateIP();
	}

	/**
	 * Start or SinglePlayer button is clicked
	 */
	public void onClick(View v) {

		Vibrator vibrator = (Vibrator) getApplication().getSystemService(
				Service.VIBRATOR_SERVICE);
		vibrator.vibrate(80);

		if (v.getId() == R.id.btnSinglePlayer) {
			Board.getInstance().startNewGame(1);
			Intent i = new Intent(this, GameActivity.class);
			startActivity(i);
			return;
		} else if (v.getId() == R.id.btnPeers) {
			if ((gameServer.helloList.size() == 0)
					&& (animateBtnFindPlayers_ == false)) {
				updatePeerList();
			} else {
				Intent i = new Intent(this, PeerListActivity.class);
				startActivityForResult(i, 69);
			}
			return;

		} else {
			btnStart_.setClickable(false);

			TextView ip = (TextView) findViewById(R.id.server);

			try {
				String addy = ip.getText().toString();

				// Check for a valid IP
				Pattern regex = Pattern
						.compile(
								"^(?:(?:25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)\\.){3}(?:25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)$",
								Pattern.MULTILINE);
				Matcher regexMatcher = regex.matcher(addy);

				if (regexMatcher.matches()) {
					gameServer.sendGameRequest(addy);
					return;
				}

			} catch (Exception e) {
			}

			echo("Invalid opponent information!");
			btnStart_.setClickable(true);
		}
	}

	public void updatePeerList() {
		if (!gameServer.isInternetEnabled()) {
			// todo
			// prompt to enable Internet
			echo("Can't search with no internet!");
			return;
		}

		btnFindPlayers_.setText("Finding peers...");

		// Spawn a thread for faster startup
		new Thread(new Runnable() {
			public void run() {

				// start actually pinging the lan
				gameServer.findPeers();

				// pulse the find players button while we're looking
				animateBtnFindPlayers_ = true;

				try {
					float alphaStart = .90f;
					float alphaEnd = 0.2f;

					while (animateBtnFindPlayers_) {
						btnFindPlayers_.clearAnimation();

						Animation animation = new AlphaAnimation(alphaStart,
								alphaEnd);
						animation.setDuration(animateBtnFindPlayersDelay);
						btnFindPlayers_.setAnimation(animation);
						Thread.sleep(animateBtnFindPlayersDelay);

						float t = alphaStart;
						alphaStart = alphaEnd;
						alphaEnd = t;
					}

					btnFindPlayers_.clearAnimation();
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
		}).start();
	}

	public void onConfigurationChanged(Configuration newConfig) {
		View l = this.findViewById(R.id.container);

		super.onConfigurationChanged(newConfig);
		if (newConfig.orientation == Configuration.ORIENTATION_LANDSCAPE) {
			l.setBackgroundResource(R.drawable.splashlandscape);
		} else {
			l.setBackgroundResource(R.drawable.splash);
		}
	}

	public void setViewIPAddress(String ip) {
		if (ip == null)
			return;

		((TextView) findViewById(R.id.lblMyIP)).setText("My IP: " + ip);
	}

	public synchronized void setViewPeerCount(int peers) {
		if (peers == 0)
			return;

		Button t = (Button) findViewById(R.id.btnPeers);
		t.setText("Finding peers (" + peers + ")");
	}

	public void deliveredRequestCB(boolean success) {
		if (success) {
			echo("Delivered request! Awaiting reply...");
		} else {
			echo("Unable to connect to the remote player");
			btnStart_.setClickable(true);
		}
	}

	public void requestResponseCB(int response, Socket sock) {
		try {
			if (response == GameServer.RESPONSE_ACCEPT) {
				echo("Opponent has accepted your challenge! Starting game...");

				Board.getInstance().setOpponentSocket(sock);
				Board.getInstance().startNewGame(1); // initiator is always 1

				Intent i = new Intent(this, GameActivity.class);
				startActivity(i);
				return;

			} else if (response == GameServer.RESPONSE_DENY) {
				echo("Opponent has denied your challenge! You win for now.");

			} else if (response == GameServer.RESPONSE_GAME_IN_PROGRESS) {
				echo("Opponent is currently in a game. Try again later.");

			} else {
				echo("Opponent didn't response to your request.");
			}

			btnStart_.setClickable(true);
			sock.close();
		} catch (Exception e) {
			e.printStackTrace();
		}

	}

	public void incomingGameRequestCB(final Socket sock) {
		String ip = sock.getRemoteSocketAddress().toString();
		ip = ip.substring(0, ip.indexOf('/'));

		String msg = "You've got an incoming request from " + ip
				+ ". Want to play?";

		final Intent act = new Intent(this, GameActivity.class);
		AlertDialog dialog = new AlertDialog.Builder(LobbyActivity.this)
				.create();
		dialog.setMessage(msg);
		dialog.setButton(DialogInterface.BUTTON_POSITIVE, "Yes",
				new DialogInterface.OnClickListener() {
					public void onClick(DialogInterface dialog, int i) {
						dialog.cancel();

						// todo
						// confirm that the request hasn't expired
						gameServer.responseToRequest(sock, true);
						Board.getInstance().setOpponentSocket(sock);
						Board.getInstance().startNewGame(2); // receiver is
						// always 2

						startActivity(act);
						return;
					}
				});

		dialog.setButton(DialogInterface.BUTTON_NEGATIVE, "No",
				new DialogInterface.OnClickListener() {
					public void onClick(DialogInterface dialog, int i) {
						dialog.cancel();
						gameServer.responseToRequest(sock, false);
					}
				});

		dialog.show();
	}

	public static LobbyActivity getInstance() {
		if (instance_ == null)
			System.err.println("Error! LobbyAct->getInst before onCreate");

		return instance_;
	}

	public static void echo(String msg) {
		Toast.makeText(context_, msg, Toast.LENGTH_LONG).show();
	}

	/**
	 * Called when PeerListActivity is closed
	 */
	@Override
	public void onActivityResult(int requestCode, int resultCode, Intent data) {
		super.onActivityResult(requestCode, resultCode, data);

		if (resultCode == RESULT_CANCELED || data == null)
			return;

		Bundle extras = data.getExtras();

		if (data.getExtras() != null) {
			String ip = extras.getString("ip");

			// Autofill the IP in the IP Box
			((EditText) findViewById(R.id.server)).setText(ip);
		}
	}

	@Override
	public void onResume() {
		super.onResume();
		Log.d("mad","   super.onResume()");
		
		btnStart_.setClickable(true);
	}

	@Override
	public void onPause() {
		super.onPause();
		Log.d("mad","   super.onPause()");
		
		gameServer.stopPingTheLan();

	}

	/**
	 * Sets up the Menu options for this activity
	 */
	public boolean onCreateOptionsMenu(Menu menu) {
		super.onCreateOptionsMenu(menu);
		int i = 0;

		menu.add(Menu.NONE, MENU_SETTINGS, i++, "Settings").setIcon(
				android.R.drawable.ic_menu_preferences);

		menu.add(Menu.NONE, MENU_SCAN, i++, "Re-scan").setIcon(
				android.R.drawable.ic_menu_edit);

		menu.add(Menu.NONE, MENU_BABES, i++, "More Babes!").setIcon(
				android.R.drawable.ic_menu_camera);

		menu.add(Menu.NONE, MENU_ABOUT, i++, "About").setIcon(
				android.R.drawable.ic_menu_info_details);

		return true;
	}

	/**
	 * Called when an Menu item is clicked
	 */
	@Override
	public boolean onMenuItemSelected(int featureId, MenuItem item) {
		super.onMenuItemSelected(featureId, item);

		switch (item.getItemId()) {

		case (MENU_ABOUT):
			echo("About");
			break;

		case (MENU_BABES):
			echo("Babes");
			break;

		case (MENU_SCAN):
			updatePeerList();
			break;

		case (MENU_SETTINGS):
			echo("Settings");
			break;

		}
		return true;
	}

	public void findPlayersFinished() {
		if (gameServer.helloList.size() == 0) {
			btnFindPlayers_.setText("Find local peers");
			echo("No opponents found...");
		} else
			btnFindPlayers_.setText(gameServer.helloList.size()
					+ " peers");

		animateBtnFindPlayers_ = false;
	}

	public void findPlayersCountUpdated() {
		setViewPeerCount(gameServer.helloList.size());
	}

	public void updateIP() {
		if (!gameServer.isInternetEnabled())
			setViewIPAddress("Checking your connection...");

		new Thread(new Runnable() {
			public void run() {
				final String ip = gameServer.updateIPAddress();
				uiThreadCallback.post(new Runnable() {
					public void run() {
						setViewIPAddress(ip);
					}
				});
			}
		}).start();
	}
}