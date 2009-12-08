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
import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.os.Vibrator;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.vuphone.tictactoe.model.Board;

public class LobbyActivity extends Activity implements OnClickListener {

	private static Context context_ = null;
	private static LobbyActivity instance_ = null;
	private static Boolean animateBtnFindPlayers_ = false;
	private static int animateBtnFindPlayersBrightness_ = 0;
	private static int animateBtnFindPlayersDelta_ = 4;
	
	public static Button btnStart_ = null;
	public static Button btnFindPlayers_ = null;
	static final Handler uiThreadCallback = new Handler();

	/**
	 * Called when the activity is first created and again after the apps goes
	 * offscreen and resumes
	 */

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.main);

		context_ = getBaseContext();
		instance_ = this;
		btnStart_ = ((Button) findViewById(R.id.btnSendRequest));
		btnFindPlayers_ = ((Button) findViewById(R.id.btnPeers));
		
		btnStart_.setOnClickListener(this);
		btnFindPlayers_.setOnClickListener(this);
		btnFindPlayers_.setText("Find local peers");
		((Button) findViewById(R.id.btnSinglePlayer)).setOnClickListener(this);
		
		// Display the IP address
		setViewIPAddress(GameServer.getInstance().getMyIP());
	}

	/**
	 * Start or SinglePlayer button is clicked
	 */
	public void onClick(View v) {

		Vibrator vibrator = (Vibrator) getApplication().getSystemService(Service.VIBRATOR_SERVICE);
		vibrator.vibrate(80);

		if (v.getId() == R.id.btnSinglePlayer) {
			Board.getInstance().startNewGame(1);
			Intent i = new Intent(this, GameActivity.class);
			startActivity(i);
			return;
		} else if (v.getId() == R.id.btnPeers) {
				if ((GameServer.getInstance().helloList.size() == 0 ) && (animateBtnFindPlayers_ == false)){
					btnFindPlayers_.setText("Finding peers...");
					initializePeerList();
					
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
				Pattern regex = Pattern.compile(
						"^(?:(?:25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)\\.){3}(?:25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)$",
						Pattern.MULTILINE);
				Matcher regexMatcher = regex.matcher(addy);

				if (regexMatcher.matches()) {
					GameServer.getInstance().sendGameRequest(addy);
					return;
				}

			} catch (Exception e) {
			}

			echo("Invalid opponent information!");
			btnStart_.setClickable(true);
		}
	}

	public void initializePeerList() {

		// Spawn a thread for faster startup
		new Thread(new Runnable() {
			public void run() {
				// pulse the find players button while we're looking
				animateBtnFindPlayers_ = true;
				try {
				while (animateBtnFindPlayers_){
					
					uiThreadCallback.post(new Runnable() {
						public void run() {
							int c = animateBtnFindPlayersBrightness_;
							btnFindPlayers_.setTextColor(Color.rgb(c,c,c));
							c = c + animateBtnFindPlayersDelta_;
							
							if ((c > 200) || (c < 0))
								animateBtnFindPlayersDelta_ = -animateBtnFindPlayersDelta_;
							else
								animateBtnFindPlayersBrightness_ = c;
						}
					});
					
					Thread.sleep(40);
				}
				} catch (Exception e){
					e.printStackTrace();
				}
				
				// start actually pinging the lan
				GameServer.getInstance().pingTheLan();
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

		String msg = "You've got an incoming request from " + ip + ". Want to play?";

		final Intent act = new Intent(this, GameActivity.class);
		AlertDialog dialog = new AlertDialog.Builder(LobbyActivity.this).create();
		dialog.setMessage(msg);
		dialog.setButton(DialogInterface.BUTTON_POSITIVE, "Yes", new DialogInterface.OnClickListener() {
			public void onClick(DialogInterface dialog, int i) {
				dialog.cancel();
				GameServer.getInstance().responseToRequest(sock, true);
				Board.getInstance().setOpponentSocket(sock);
				Board.getInstance().startNewGame(2); // receiver is
				// always 2

				startActivity(act);
				return;
			}
		});

		dialog.setButton(DialogInterface.BUTTON_NEGATIVE, "No", new DialogInterface.OnClickListener() {
			public void onClick(DialogInterface dialog, int i) {
				dialog.cancel();
				GameServer.getInstance().responseToRequest(sock, false);
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
		Toast.makeText(context_, msg, Toast.LENGTH_SHORT).show();
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

		btnStart_.setClickable(true);
		setViewIPAddress(GameServer.getInstance().updateIPAddress());
	}

	public void findPlayersFinished() {
		btnFindPlayers_.setText("Find local peers");
		animateBtnFindPlayers_ = false;
	}

	public void findPlayersCountUpdated() {
		final GameServer gs = GameServer.getInstance();
		setViewPeerCount(gs.helloList.size());
	}

}