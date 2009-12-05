package com.vuphone.tictactoe;

import javax.microedition.khronos.egl.EGL10;
import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;
import javax.microedition.khronos.opengles.GL11;

import android.content.Context;
import android.content.res.Resources;
import android.graphics.BitmapFactory;
import android.opengl.GLSurfaceView;
import android.opengl.GLSurfaceView.Renderer;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnTouchListener;

import com.vuphone.tictactoe.graphics.GLHelpers;
import com.vuphone.tictactoe.graphics.GLManager;
import com.vuphone.tictactoe.graphics.GLModel;
import com.vuphone.tictactoe.model.Board;
import com.vuphone.tictactoe.model.geometry.Point;
import com.vuphone.tictactoe.model.geometry.Rect;

public class BoardGLView extends GLSurfaceView implements OnTouchListener, Renderer {

	public int backingWidth;
	public int backingHeight;
	public Board board;
	private float boardTileAnimationFractions[][] = new float[3][3];
	
	// OpenGL Models
	private GLModel xPiece;
	private GLModel oPiece;
	private GLModel grid;
	private GLModel table;
	private GLModel plane;
	private GLModel sign_plane;
	private GLModel star;
	private GLModel pieceShadow;
	private GLModel pieceHighlight;
	
	private float touchAge = 0;
	private Point touchLocation = null;
	private float animateFraction = 0;
	
	private float myTurnAnimationFraction = 0;
	private float gameOverAnimationFraction = 0;
	
	private BoardGLViewDelegate delegate;
	
	public BoardGLView(Context context, BoardGLViewDelegate d) {
		super(context);
		this.setOnTouchListener(this);
		this.setRenderer(this);
		delegate = d;
	}

	public void onSurfaceChanged(GL10 gl, int width, int height) {
		backingWidth = width;
		backingHeight = height;
	}

	public void onSurfaceReleased(GL10 gl)
	{	
	}

	public void onSurfaceCreated(GL10 gl, EGLConfig config) {
		GLManager m = GLManager.getInstance();

		// setup our environment variables
		m.gl = (GL11) gl;
	    gl.glDepthMask(true);
	    gl.glFrontFace(GL11.GL_CW);
	    gl.glShadeModel(GL11.GL_SMOOTH);
	    
		delegate.paintSurfaceGLReady();

		// load 3d models
		oPiece = new GLModel("o.model", "oTexture");
		xPiece = new GLModel("x.model", "xTexture");
		table = new GLModel("table.model", "graniteTexture");
		grid = new GLModel("grid.model", "gridTexture");
		plane = new GLModel("plane.model", "backgroundTexture");
		star = new GLModel("plane.model", "starTexture");
		sign_plane = new GLModel("sign_plane.model", "itsyourturnTexture");
		pieceShadow = new GLModel("box_highlight.model", "shadowTexture");
		pieceHighlight = new GLModel("highlight.model", "touchTexture");
		
		// load textures
		Resources res = m.glContext.getResources();
		m.loadTextureWithName("gridTexture", BitmapFactory.decodeResource(res, R.drawable.grid));
		m.loadTextureWithName("graniteTexture", BitmapFactory.decodeResource(res, R.drawable.wood));
		m.loadTextureWithName("oTexture", BitmapFactory.decodeResource(res, R.drawable.o));
		m.loadTextureWithName("xTexture", BitmapFactory.decodeResource(res, R.drawable.x));
		m.loadTextureWithName("backgroundTexture", BitmapFactory.decodeResource(res, R.drawable.background));
		m.loadTextureWithName("starTexture", BitmapFactory.decodeResource(res, R.drawable.star));
		m.loadTextureWithName("itsyourturnTexture", BitmapFactory.decodeResource(res, R.drawable.itsyourturn));
		m.loadTextureWithName("shadowTexture", BitmapFactory.decodeResource(res, R.drawable.shadow));
		m.loadTextureWithName("touchTexture", BitmapFactory.decodeResource(res, R.drawable.touch));
		m.loadTextureWithName("girlTexture", BitmapFactory.decodeResource(res, R.drawable.winnergirl));
		m.loadTextureWithName("winPlaneTexture", BitmapFactory.decodeResource(res, R.drawable.winner));
		m.loadTextureWithName("losePlaneTexture", BitmapFactory.decodeResource(res, R.drawable.loser));
		m.loadTextureWithName("tiePlaneTexture", BitmapFactory.decodeResource(res, R.drawable.tie));
		m.loadTextureWithName("swipeTexture", BitmapFactory.decodeResource(res, R.drawable.swipe));
	}
	
	public void onDrawFrame(GL10 gl) {
		
		float smoothed = (float)Math.sin(animateFraction * Math.PI / 2);
		float camera[] = {0,40,0};
		float w = 1f + (1 - smoothed) * 6;
		float h = 1.5f + (1 - smoothed) * 9;
		
		gl.glViewport(0, 0, backingWidth, backingHeight);
		gl.glMatrixMode(GL11.GL_MODELVIEW);
	    gl.glLoadIdentity();
	    gl.glMatrixMode(GL11.GL_PROJECTION);
		gl.glLoadIdentity();
		gl.glOrthof(-0.33f, 0.33f, -0.5f, 0.5f, -1, 1);
		
		if (animateFraction < 1)
		animateFraction += 0.015;
		
	    // clear the canvas
	    float color = Math.min(smoothed * 2, 1);
	    gl.glColor4f(color,color,color,1);
	    gl.glClearColor(color,color,color, 1.0f);
	    gl.glClear(GL11.GL_COLOR_BUFFER_BIT | GL11.GL_DEPTH_BUFFER_BIT);
	    gl.glDisable(GL11.GL_DEPTH_TEST);
	    
	    // draw our attractive background
	    plane.draw();
	    
	    // draw a couple stars on top in an additive blend mode
	    gl.glEnable(GL11.GL_BLEND);
	    gl.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_DST_ALPHA);
	    star.pitch = star.pitch + 0.2f;
	    star.draw();
	    star.pitch = -(star.pitch - 40);
	    star.draw();
	    star.pitch = -star.pitch + 40;

	    // look from camera XYZ, look at the origin, positive Y up vector
	    gl.glLoadIdentity();
	    gl.glFrustumf(-w, w, -h, h, 35.0f, 65.0f);
		GLHelpers.gluLookAt(camera[0], camera[1], camera[2], 0, 0, -3.5f, 0, 1, 0);
	    gl.glEnable(GL11.GL_DEPTH_TEST);
    	gl.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);

	    // draw the table down some
    	gl.glTranslatef(0, -14f, 3f);
		gl.glTranslatef(0, 0, (-60 * gameOverAnimationFraction));
	    // draw the table
	    grid.draw();
	    table.draw();

	    for (int x = 0; x < 3; x ++){
	    	for (int y = 0; y < 3; y++){
	    		float fraction = boardTileAnimationFractions[x][y];
	    		float yOffset = (1-fraction) * 10;

	    		if (fraction < 1)
	    			boardTileAnimationFractions[x][y] += 0.1;

    			gl.glColor4f(1, 1, 1, fraction);
	    		gl.glTranslatef((x-1) * 11f, 0, (y-1) * 11f);
	    		if (board.valueInSquare(x, y) != 0){
	    			gl.glEnable(GL11.GL_BLEND);
	    		    pieceShadow.draw();
	    		    gl.glDisable(GL11.GL_BLEND);
	    		}

    			gl.glTranslatef(0, yOffset, 0);
	    		if (board.valueInSquare(x, y) == 1)
	    			xPiece.draw();
	    		
	    		if (board.valueInSquare(x, y) == 2)
	    		    oPiece.draw();

    			gl.glTranslatef(0, -yOffset, 0);
	    		gl.glTranslatef(-(x-1) * 11f, 0, -(y-1) * 11f);
	    		
	    	}
	    }

		gl.glTranslatef(0, 0, (60 * gameOverAnimationFraction));
		gl.glTranslatef(0, 14, -3f);
		
		// draw 2D things like the signs and hot girls
		gl.glViewport(0, 0, backingWidth, backingHeight);
		gl.glMatrixMode(GL11.GL_MODELVIEW);
	    gl.glLoadIdentity();
	    gl.glMatrixMode(GL11.GL_PROJECTION);
		gl.glLoadIdentity();
		gl.glOrthof(-0.33f, 0.33f, -0.5f, 0.5f, -1, 1);
		gl.glColor4f(1, 1, 1, 1);
		
	    if ((myTurnAnimationFraction < 1) && (board.isMyTurn()) && (!board.isGameOver()))
    		myTurnAnimationFraction = (float) Math.min(myTurnAnimationFraction + 0.002 + (1-myTurnAnimationFraction) * 0.05, 1);
    	else if ((myTurnAnimationFraction > 0) && ((!board.isMyTurn()) || (board.isGameOver())))
    		myTurnAnimationFraction -= 0.1;
	    
	    gl.glEnable(GL11.GL_BLEND);
    	gl.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
	    sign_plane.setTexture("itsyourturnTexture");
    	gl.glPushMatrix();
    	gl.glTranslatef(0, 1-myTurnAnimationFraction, 0);
    	sign_plane.draw();
    	gl.glPopMatrix();
	    
	    // draw in the game over objects, if the game is over
	    if (board.isGameOver()){
	    	if (gameOverAnimationFraction < 1)
	    		gameOverAnimationFraction = (float) Math.min(gameOverAnimationFraction + 0.01 + (1-gameOverAnimationFraction) * 0.10, 1);
	    		
	    	gl.glPushMatrix();
	    	gl.glTranslatef((1-gameOverAnimationFraction) * 1.0f + 0.10f, -0.31f, 0);
	    	gl.glScalef(0.75f, 0.75f, 1);
	    	
	    	// draw the winner message
	    	if (board.getWinner() == board.getMyPlayerID())
	    		sign_plane.setTexture("winPlaneTexture");
	    	else if (board.getWinner() == 0)
	    		sign_plane.setTexture("tiePlaneTexture");
	    	else
	    		sign_plane.setTexture("losePlaneTexture");
	    	
	    	sign_plane.draw();
	    	gl.glPopMatrix();
	    	
	    	// draw the chick
	    	gl.glPushMatrix();
	    	gl.glScalef(0.6f, 2.4f, 1.0f);
	    	gl.glTranslatef((1-gameOverAnimationFraction) * -1.0f - 0.26f, -0.31f, 1);
	    	sign_plane.setTexture("girlTexture");
	    	sign_plane.draw();
	    	gl.glPopMatrix();
	    	
	    	// draw the swipe that goes across the screen
	    	gl.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE);
			gl.glPushMatrix();
	    	gl.glTranslatef(-0.5f + gameOverAnimationFraction * 2f, 0, 0);
	    	gl.glRotatef(45, 0, 0, 1);
	    	gl.glScalef(20f, 0.5f, 1.0f);
	    	sign_plane.setTexture("swipeTexture");
	    	sign_plane.draw();
	    	gl.glPopMatrix();
	    }
	    gl.glDisable(GL11.GL_BLEND);
	    
		// draw the touch overlay, if we have one
		if (touchLocation != null){
			gl.glViewport(0, 0, backingWidth, backingHeight);

			gl.glMatrixMode(GL11.GL_MODELVIEW);
		    gl.glLoadIdentity();
			
		    gl.glMatrixMode(GL11.GL_PROJECTION);
			gl.glLoadIdentity();
		    gl.glOrthof(0,backingWidth,0,backingHeight, -1, 1);
			
			gl.glDisable(GL11.GL_DEPTH_TEST);
			gl.glTranslatef(touchLocation.x, backingHeight-touchLocation.y, 0);
			gl.glEnable(GL11.GL_BLEND);
			gl.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE);
			gl.glColor4f(1, 1, 1, 1-touchAge);
			pieceHighlight.draw();
		    gl.glDisable(GL11.GL_BLEND);
			gl.glTranslatef(-touchLocation.x, -(backingHeight-touchLocation.y), 0);
			gl.glColor4f(1, 1, 1, 1);
			
			touchAge = touchAge + 0.12f;

			if (touchAge >= 1)
				touchLocation = null;
			
		}
	}

	public boolean onTouch(View v, MotionEvent event) 
	{
		final MotionEvent e = MotionEvent.obtain(event);
		if (e.getAction() == MotionEvent.ACTION_DOWN)
			Log.d("Touch","Top Level Got Began");
		
		this.queueEvent(new Runnable() {
			public void run() {
				if (e.getAction() == MotionEvent.ACTION_DOWN){
					touchBegan(e);
				}else if (e.getAction() == MotionEvent.ACTION_MOVE){
					//touchMoved(e);
				} else {
					touchEnded(e);
				}
			}
		});

		requestRender();
		return true;
	}

	public void touchBegan(MotionEvent event) {
		Point p = new Point(event.getX(), event.getY());	
		Rect boardRect = new Rect(backingWidth / 20, backingHeight / 2.6f, backingWidth - (backingWidth / 20) * 2, backingHeight - (backingHeight / 20 + backingHeight / 2.6f));
	
		// create touchLocation
		touchLocation = p;
		touchAge = 0;
		
		// figure out which 9th of the board the point is in
		float sectionWidth = boardRect.size.width / 3;
		float sectionHeight = boardRect.size.height / 3;
		
		for (int x = 0; x < 3; x++){
			for (int y = 0; y < 3; y++){
				float originX = boardRect.origin.x + x * sectionWidth;
				float originY = boardRect.origin.y + y * sectionHeight;
				if ((p.x > originX) && (p.x < originX + sectionWidth) && 
					(p.y > originY) && (p.y < originY + sectionHeight)){
					delegate.paintSurfaceSquareTouched(x,y);
					return;
				}
			}
		}
	}

	public void touchEnded(MotionEvent event) {
	}

	public int[] getOptimalOpenGLConfiguration()
	{
		int[] r = { EGL10.EGL_DEPTH_SIZE, 8, EGL10.EGL_NONE };
		return r;
	}

	public void animatePieceDrop(int x, int y) 
	{
		boardTileAnimationFractions[x][y] = 0;
	}
}
