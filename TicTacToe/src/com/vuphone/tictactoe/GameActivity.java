package com.vuphone.tictactoe;

import android.app.Activity;
import android.os.Bundle;
import android.util.Log;
import android.view.Window;
import android.view.WindowManager;

import com.vuphone.tictactoe.graphics.GLManager;
import com.vuphone.tictactoe.model.Board;

public class GameActivity extends Activity implements BoardGLViewDelegate {
	private BoardGLView paintView;
	private Board gameBoard;
	
	/** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        // remove our title bar
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);
        
        // create the actual openGL view and setup our custom renderer
        paintView = new BoardGLView(this, this);
        setContentView(paintView);

        // get the OpenGL context from the surface and save it
		GLManager.getInstance().glContext = paintView.getContext();
    }
    

	public void paintSurfaceGLReady() {
		// determine what we can do with our OpenGL Environment
		GLManager.getInstance().runEnvironmentTests();
		
		// load the game model
		gameBoard = new Board();
		
		// give the view a handle to the model
		paintView.board = gameBoard;
		paintView.requestRender();
	}

	public void paintSurfaceSquareTouched(int x, int y) {
		// TODO Auto-generated method stub
		Log.d("Touch", "User touched square " + x + "," + y );
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


}