package com.vuphone.tictactoe;

import android.app.Activity;
import android.app.Service;
import android.content.Context;
import android.content.pm.ActivityInfo;
import android.os.Bundle;
import android.os.Handler;
import android.os.Vibrator;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Toast;

import com.vuphone.tictactoe.graphics.GLManager;
import com.vuphone.tictactoe.model.Board;

public class GameActivity extends Activity implements BoardGLViewDelegate {
	private BoardGLView paintView;
	private Board gameBoard = null;

	private static Context context_ = null;
	static final Handler uiThreadCallback = new Handler();
	
	/**
	 * Called when the activity is first created and again after the apps goes
	 * offscreen and resumes
	 */

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		// Prevent the screen from changing orientation when keyboard opens
		setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);

		// load the game model
		gameBoard = Board.getInstance();
		
		context_ = getBaseContext();
	
		// remove our title bar
		requestWindowFeature(Window.FEATURE_NO_TITLE);
		getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
				WindowManager.LayoutParams.FLAG_FULLSCREEN);

		// create the actual openGL view and setup our custom renderer
		paintView = new BoardGLView(this, this);
		setContentView(paintView);
		gameBoard.setPaintView(paintView);
		
		// get the OpenGL context from the surface and save it
		GLManager.getInstance().glContext = paintView.getContext();
		
		if(Settings.getInstance().getBoolean(Settings.KEEP_SCREEN_ON, true))
			paintView.setKeepScreenOn(true);
		else
			paintView.setKeepScreenOn(false);
	}

	public void paintSurfaceGLReady() {
		// determine what we can do with our OpenGL Environment
		GLManager.getInstance().runEnvironmentTests();

		// give the view a handle to the model
		paintView.board = gameBoard;
		paintView.requestRender();
	}

	public void paintSurfaceSquareTouched(int x, int y) 
	{
		if (Settings.getInstance().getBoolean(Settings.VIBRATE, true)){
			Vibrator vibrator = (Vibrator) getApplication().getSystemService(
					Service.VIBRATOR_SERVICE);
			vibrator.vibrate(80);
		}
		gameBoard.setValueInSquare(x, y);
	}

	public void paintSurfaceGameClose()
	{
		super.finish();
	}
	
	public static void echo(String msg) {
		Toast.makeText(context_, msg, Toast.LENGTH_LONG).show();
	}
	
	@Override
	protected void onResume() {
		// Ideally a game should implement onResume() and onPause()
		// to take appropriate action when the activity looses focus
		super.onResume();
		paintView.onResume();
	}

	@Override
	protected void onPause() {
		// Ideally a game should implement onResume() and onPause()
		// to take appropriate action when the activity looses focus
		super.onPause();
		paintView.onPause();

		Board.getInstance().setOpponentSocket(null);
		Board.getInstance().prematureEndGame();
		
		super.finish();
	}

	@Override
	protected void onStop() {
		super.onStop();
	}

}