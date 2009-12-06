package com.vuphone.tictactoe;

import android.app.Activity;
import android.content.pm.ActivityInfo;
import android.os.Bundle;
import android.view.Window;
import android.view.WindowManager;

import com.vuphone.tictactoe.graphics.GLManager;
import com.vuphone.tictactoe.model.Board;

public class GameActivity extends Activity implements BoardGLViewDelegate {
	private BoardGLView paintView;
	private Board gameBoard = null;

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

		// remove our title bar
		requestWindowFeature(Window.FEATURE_NO_TITLE);
		getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
				WindowManager.LayoutParams.FLAG_FULLSCREEN);

		// create the actual openGL view and setup our custom renderer
		paintView = new BoardGLView(this, this);
		setContentView(paintView);

		// get the OpenGL context from the surface and save it
		GLManager.getInstance().glContext = paintView.getContext();
	}

	public void paintSurfaceGLReady() {
		// determine what we can do with our OpenGL Environment
		GLManager.getInstance().runEnvironmentTests();

		// give the view a handle to the model
		paintView.board = gameBoard;
		paintView.requestRender();
	}

	public void paintSurfaceSquareTouched(int x, int y) {

		int before = gameBoard.valueInSquare(x, y);
		gameBoard.setValueInSquare(x, y);

		// get the value from the square and tell the paint surface to drop a
		// tile
		int after = gameBoard.valueInSquare(x, y);
		if (after != before) {
			paintView.animatePieceDrop(x, y);
		}
		gameBoard.setValueInSquare(x, y);
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

		super.finish();
	}

	@Override
	protected void onStop() {
		super.onStop();
		Board.getInstance().endGame();
	}

}