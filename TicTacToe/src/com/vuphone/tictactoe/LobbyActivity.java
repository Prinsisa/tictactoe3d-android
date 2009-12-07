package com.vuphone.tictactoe;

import java.net.Socket;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.res.Configuration;
import android.os.Bundle;
import android.os.Handler;
import android.text.Layout;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.vuphone.tictactoe.model.Board;

public class LobbyActivity extends Activity implements OnClickListener {

	private static Context context_ = null;
	private static LobbyActivity instance_ = null;
	public static Button btnStart_ = null;
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

		btnStart_ = (Button) findViewById(R.id.btnSendRequest);
		btnStart_.setOnClickListener(this);
		((Button) findViewById(R.id.btnSinglePlayer)).setOnClickListener(this);

		// Get IP addr
		String ip = GameServer.getInstance().getMyIP();
		((TextView) findViewById(R.id.lblMyIP)).setText("My IP: " + ip);
	}

	/**
	 * Start or SinglePlayer button is clicked
	 */
	public void onClick(View v) {

		String btn = ((Button) v).getText().toString();

		if (btn.equals("Single Player Mode")) {
			Board.getInstance().startNewGame(1);
			Intent i = new Intent(this, GameActivity.class);
			startActivity(i);
			return;
		}

		btnStart_.setClickable(false);

		TextView ip = (TextView) findViewById(R.id.server);

		try {
			String addy = ip.getText().toString();

			//Check for a valid IP
			Pattern regex = Pattern
					.compile(
							"^(?:(?:25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)\\.){3}(?:25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)$",
							Pattern.MULTILINE);
			Matcher regexMatcher = regex.matcher(addy);

			if (regexMatcher.matches()){
				GameServer.getInstance().sendGameRequest(addy);
				return;
			}
			
		} catch (Exception e) {
		}
		
		echo("Invalid opponent information!");
		btnStart_.setClickable(true);

	}
	
	public void onConfigurationChanged (Configuration newConfig)
	{
		View l = this.findViewById(R.id.container);
		
		super.onConfigurationChanged(newConfig);
		if (newConfig.orientation == Configuration.ORIENTATION_LANDSCAPE){
			l.setBackgroundResource(R.drawable.splashlandscape);
		} else {
			l.setBackgroundResource(R.drawable.splash);
		}
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
						GameServer.getInstance().responseToRequest(sock, true);
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

	@Override
	public void onResume() {
		super.onResume();
		btnStart_.setClickable(true);
		GameServer.getInstance().updateIPAddress();
	}
}