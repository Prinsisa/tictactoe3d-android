package com.vuphone.tictactoe.graphics;

import java.util.HashMap;

import javax.microedition.khronos.opengles.GL11;
import javax.microedition.khronos.opengles.GL11ExtensionPack;

import android.content.Context;
import android.graphics.Bitmap;
import android.util.Log;

public class GLManager {
    
	private static GLManager instance = null;

	public boolean framebuffersSupported = true;
	public boolean pointSpriteSupported = false;
	
	public GL11 gl;
	public Context glContext;
    
	public HashMap<String, GLTexture> textures = new HashMap<String, GLTexture>();
	
	protected GLManager() {
	}

	public static GLManager getInstance() {
		if (instance == null) {
			instance = new GLManager();
		}
		return instance;
	}
	
	public void runEnvironmentTests()
	{
		GL11ExtensionPack g = (GL11ExtensionPack)gl;
		int [] r = new int [1];
		try {
			g.glGetFramebufferAttachmentParameterivOES(GL11ExtensionPack.GL_FRAMEBUFFER_OES, GL11ExtensionPack.GL_COLOR_ATTACHMENT0_OES, GL11.GL_TEXTURE_2D, r, 0);
			Log.d("Layers Graphics", "Framebuffers are supported");
			
		} catch (UnsupportedOperationException e) {
			framebuffersSupported = false;
			Log.d("Layers Graphics", "Framebuffers are NOT supported");
		}
	}
	
	public void loadTextureWithName(String name, Bitmap image)
	{
		textures.put(name, new GLTexture(image));
	}
	
	public GLTexture textureForName(String name) 
	{
		return textures.get(name);
	}
	
	public int textureIDForName(String name) 
	{
		return textures.get(name).texture;
	}

}
