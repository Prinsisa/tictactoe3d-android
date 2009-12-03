package com.vuphone.tictactoe.model.geometry;

import android.graphics.RectF;

public class Rect {
	public Point origin;
	public Size size;
	
	public Rect(){
		origin = new Point();
		size = new Size();
	}
	
	public Rect(float x, float y, float w, float h){
		origin = new Point(x,y);
		size = new Size(w,h);
	}

	public void union(float x, float y, float w, float h) {
		float extentX = Math.max(origin.x + size.width, x + w);
		float extentY = Math.max(origin.y + size.height, y + h);
		origin.x = Math.min(origin.x, x);
		origin.y = Math.min(origin.y, y);
		size.width = extentX - origin.x;
		size.height = extentY - origin.x;
	}

	public RectF toRectF() {
		return new RectF(origin.x, origin.y, origin.x + size.width, origin.y + size.height);
	}
}
