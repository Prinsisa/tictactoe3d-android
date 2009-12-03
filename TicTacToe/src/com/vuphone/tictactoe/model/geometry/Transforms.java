package com.vuphone.tictactoe.model.geometry;

public class Transforms {

	public float zoom;
	public float x;
	public float y;

	public static Transforms MakeUnitTransforms()
	{
	    return new Transforms(0, 0, 1);
	}
	
	public static Transforms ConcatTransforms(Transforms t1, Transforms t2) 
	{
	    return new Transforms(t1.x + t2.x / t1.zoom,t1.y + t2.y / t1.zoom, t1.zoom * t2.zoom);
	}

	public static boolean IsUnitTransform(Transforms t) 
	{
		return ((t.x == 0) && (t.y == 0) && (t.zoom == 1));
	}

	public Transforms(float x1, float y1, float zoom1)
	{
	    x = x1;
	    y = y1;
	    zoom = zoom1;
	}
	
	public void applyTransforms(Transforms newT)
	{
	    x *= newT.x;
	    y *= newT.y;
	    zoom *= newT.zoom;
	}
}
