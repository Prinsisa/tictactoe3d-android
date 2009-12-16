package com.vuphone.tictactoe;

import java.net.Socket;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
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
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.vuphone.tictactoe.model.Board;
import com.vuphone.tictactoe.network.NetworkManager;

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

	private static final int DIALOG_ABOUT = 0;
	private static final int DIALOG_MOREBABES = 1;

	final GameServer gameServer = GameServer.getInstance();
	final Settings settings_ = Settings.getInstance();
	final NetworkManager networkManager_ = NetworkManager.getInstance();
	public AlertDialog activeRequestDialog = null;

	/**
	 * Called when the activity is first created and again after the apps goes
	 * offscreen and resumes
	 */

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		Log.d("mad", "   super.onCreate()");

		setContentView(R.layout.main);

		context_ = getBaseContext();
		instance_ = this;
		settings_.loadPreferences(getPreferences(MODE_PRIVATE));
		networkManager_
				.setWifiManager((WifiManager) getSystemService(Context.WIFI_SERVICE));

		btnStart_ = ((Button) findViewById(R.id.btnSendRequest));
		btnFindPlayers_ = ((Button) findViewById(R.id.btnPeers));

		btnStart_.setOnClickListener(this);
		btnFindPlayers_.setOnClickListener(this);
		btnFindPlayers_.setText("Find local peers");
		((Button) findViewById(R.id.btnSinglePlayer)).setOnClickListener(this);

		// Display the IP address
		updateIP();

		if (settings_.getBoolean(Settings.FIRST_LAUNCH, true))
			initializeForFirstLaunch();

		// Keep the screen on while this activity is showing
		if (settings_.getBoolean(Settings.KEEP_SCREEN_ON, true))
			btnStart_.setKeepScreenOn(true);
		else
			btnStart_.setKeepScreenOn(false);
	}

	/**
	 * Used to setup the defaults the first time the user launches the program
	 * after install
	 */
	public void initializeForFirstLaunch() {
		Log.d("mad", " This is the first launch after installation!");
		settings_.putBoolean(Settings.FIRST_LAUNCH, false);

		settings_.putBoolean(Settings.PLAY_SOUNDS, true);
		settings_.putBoolean(Settings.VIBRATE, true);
		settings_.putBoolean(Settings.KEEP_SCREEN_ON, true);

		// Set the default DisplayName as Phone number
		String displayName = "TicTacToe Player";
		TelephonyManager t = ((TelephonyManager) getSystemService(Context.TELEPHONY_SERVICE));
		if (t != null && t.getLine1Number() != null)
			displayName = t.getLine1Number();

		settings_.putString(Settings.DISPLAY_NAME, displayName);
	}

	/**
	 * Start or SinglePlayer button is clicked
	 */
	public void onClick(View v) {

		if (Settings.getInstance().getBoolean(Settings.VIBRATE, true)){
			Vibrator vibrator = (Vibrator) getApplication().getSystemService(
					Service.VIBRATOR_SERVICE);
			vibrator.vibrate(80);
		}
		
		if (v.getId() == R.id.btnSinglePlayer) {
			Board.getInstance().startNewGame(1);
			Intent i = new Intent(this, GameActivity.class);
			startActivity(i);
			return;
		} else if (v.getId() == R.id.btnPeers) {
			if (gameServer.helloList.size() == 0
					&& animateBtnFindPlayers_ == false) {
				gameServer.helloList.clear();
				updatePeerList();
			} else if (gameServer.helloList.size() != 0) {
				Intent i = new Intent(this, PeerListActivity.class);
				startActivityForResult(i, 69);
			}
			return;

		} else {
			btnStart_.setClickable(false);

			TextView ip = (TextView) findViewById(R.id.server);

			try {
				String addy = ip.getText().toString();

				// Verify internet is working
				if (!networkManager_.isConnectionWorking()) {
					if (networkManager_.isWifiEnabled()) {

						AlertDialog dialog = new AlertDialog.Builder(
								LobbyActivity.this).create();
						dialog
								.setMessage("You are not currently connected to the internet");
						dialog.setButton(DialogInterface.BUTTON_POSITIVE, "OK",
								new DialogInterface.OnClickListener() {
									public void onClick(DialogInterface dialog,
											int i) {
										dialog.cancel();
									}
								});

						dialog.show();

					} else {
						AlertDialog dialog = new AlertDialog.Builder(
								LobbyActivity.this).create();
						dialog
								.setMessage("You are not currently connected to the internet. Would you like to enable your wireless connection?");
						dialog.setButton(DialogInterface.BUTTON_POSITIVE,
								"Yes", new DialogInterface.OnClickListener() {
									public void onClick(DialogInterface dialog,
											int i) {
										dialog.cancel();
										networkManager_.setWifiEnabled(true);
									}
								});

						dialog.setButton(DialogInterface.BUTTON_NEGATIVE, "No",
								new DialogInterface.OnClickListener() {
									public void onClick(DialogInterface dialog,
											int i) {
										dialog.cancel();
									}
								});

						dialog.show();
					}

					return;
				}

				// Check for a valid IP
				if (NetworkManager.getInstance().isIpValid(addy)) {
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
		String ip = networkManager_.getIpAddress();

		if (!networkManager_.isWifiEnabled()) {
			AlertDialog dialog = new AlertDialog.Builder(LobbyActivity.this)
					.create();
			dialog
					.setMessage("You must be connected to a wireless network to scan for local players. Want to enable it?");
			dialog.setButton(DialogInterface.BUTTON_POSITIVE, "Yes",
					new DialogInterface.OnClickListener() {
						public void onClick(DialogInterface dialog, int i) {
							dialog.cancel();
							networkManager_.setWifiEnabled(true);
						}
					});

			dialog.setButton(DialogInterface.BUTTON_NEGATIVE, "No",
					new DialogInterface.OnClickListener() {
						public void onClick(DialogInterface dialog, int i) {
							dialog.cancel();
						}
					});

			dialog.show();
			return;
		} else if (!networkManager_.isIpInternal(ip)) {
			AlertDialog dialog = new AlertDialog.Builder(LobbyActivity.this)
					.create();
			dialog
					.setMessage("You must be connected to a wireless network to scan for local players. Please ensure you have signal.");
			dialog.setButton(DialogInterface.BUTTON_POSITIVE, "OK",
					new DialogInterface.OnClickListener() {
						public void onClick(DialogInterface dialog, int i) {
							dialog.cancel();
							networkManager_.setWifiEnabled(true);
						}
					});

			dialog.show();
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

	@Override
	public void onConfigurationChanged(Configuration newConfig) {
		super.onConfigurationChanged(newConfig);

		View l = this.findViewById(R.id.container);

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

	public void deliveredRequestCB(final Socket sock) {
		if (sock != null) {
			AlertDialog dialog = new AlertDialog.Builder(LobbyActivity.this)
					.create();
			dialog.setMessage("Waiting for opponent to accept...");
			dialog.setButton(DialogInterface.BUTTON_POSITIVE, "Cancel",
					new DialogInterface.OnClickListener() {
						public void onClick(DialogInterface dialog, int i) {
							dialog.cancel();
							gameServer.cancelRequest(sock);
							return;
						}
					});

			dialog.show();
			activeRequestDialog = dialog;
		} else {
			echo("Unable to connect to the remote player");
			btnStart_.setClickable(true);
		}
	}

	public void requestResponseCB(int response, Socket sock) {
		try {
			if (activeRequestDialog != null) {
				activeRequestDialog.dismiss();
				activeRequestDialog = null;
			}

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
				// User canceled the request
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
						activeRequestDialog = null;
						dialog.cancel();

						// todo
						// confirm that the request hasn't expired
						gameServer.responseToRequest(sock, true);
						Board.getInstance().setOpponentSocket(sock);
						Board.getInstance().startNewGame(2); // receiver is
						// always 2

						startActivity(act);
					}
				});

		dialog.setButton(DialogInterface.BUTTON_NEGATIVE, "No",
				new DialogInterface.OnClickListener() {
					public void onClick(DialogInterface dialog, int i) {
						activeRequestDialog = null;
						dialog.cancel();
						gameServer.responseToRequest(sock, false);
					}
				});

		dialog.show();
		activeRequestDialog = dialog;

		// monitor the socket to see if the initiator cancels
		new Thread(new Runnable() {
			public void run() {
				String cmd = null;
				while (cmd == null && activeRequestDialog != null)
					cmd = gameServer.readCmdFromSocket(sock, 1);

				Log.d("mad", "Got a cmd: " + cmd);

				if (cmd != null && cmd.equals(gameServer.cmdCancelGame)) {
					uiThreadCallback.post(new Runnable() {

						public void run() {
							if (activeRequestDialog != null) {
								activeRequestDialog.dismiss();
								activeRequestDialog = null;
							}

						}
					});
				}
			}
		}).start();
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

			// Auto-send a request
			onClick(findViewById(R.id.btnSendRequest));
		}
	}

	@Override
	public void onResume() {
		super.onResume();
		Log.d("mad", "   super.onResume()");

		btnStart_.setClickable(true);
	}

	@Override
	public void onPause() {
		super.onPause();
		Log.d("mad", "   super.onPause()");

		if (animateBtnFindPlayers_)
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

	protected Dialog onCreateDialog(int id) {
		Dialog dialog;
		TextView text;
		ImageView image;

		switch (id) {
		case DIALOG_ABOUT:
			dialog = new Dialog(this);

			dialog.setContentView(R.layout.about_dialog);
			dialog.setTitle("About TicTacToe 3D");

			text = (TextView) dialog.findViewById(R.id.text);
			text
					.setText("'TicTacToe 3D - Hot Babe Edition' began as a class project by Adam Albright and Ben Gotow, "
							+ "students in Computer Engineering at Vanderbilt University.");
			image = (ImageView) dialog.findViewById(R.id.image);
			image.setImageResource(R.drawable.icon);
			break;

		case DIALOG_MOREBABES:
			dialog = new Dialog(this);

			dialog.setContentView(R.layout.about_dialog);
			dialog.setTitle("So you want more babes?");

			text = (TextView) dialog.findViewById(R.id.text);
			text
					.setText("Check out hotbabeapps.com for more Android apps featuring hot babes! We told you we could make "
							+ "these games entertaining...");
			image = (ImageView) dialog.findViewById(R.id.image);
			image.setImageResource(R.drawable.icon);
			break;
		default:
			dialog = null;
		}

		return dialog;
	}

	/**
	 * Called when an Menu item is clicked
	 */
	@Override
	public boolean onMenuItemSelected(int featureId, MenuItem item) {
		super.onMenuItemSelected(featureId, item);

		switch (item.getItemId()) {

		case (MENU_ABOUT):
			this.showDialog(DIALOG_ABOUT);
			break;

		case (MENU_BABES):
			this.showDialog(DIALOG_MOREBABES);
			break;

		case (MENU_SCAN):
			updatePeerList();
			break;

		case (MENU_SETTINGS):
			Intent i = new Intent(this, SettingsActivity.class);
			startActivity(i);

			break;
		}

		return true;
	}

	public void findPlayersFinished() {
		if (gameServer.helloList.size() == 0) {
			btnFindPlayers_.setText("Find local peers");
			echo("No opponents found...");
		} else
			btnFindPlayers_.setText(gameServer.helloList.size() + " peers");

		animateBtnFindPlayers_ = false;
	}

	public void findPlayersCountUpdated() {
		setViewPeerCount(gameServer.helloList.size());
	}

	public void updateIP() {
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

	@Override
	public void onDestroy() {
		super.onDestroy();
		Log.d("mad", "   super.onDestroy()");
	}

	@Override
	public void onRestart() {
		super.onRestart();
		Log.d("mad", "   super.onRestart()");
	}

	@Override
	public void onStop() {
		super.onStop();
		Log.d("mad", "   super.onStop()");
	}

	@Override
	public void onStart() {
		super.onStart();
		Log.d("mad", "   super.onStart()");
	}
}