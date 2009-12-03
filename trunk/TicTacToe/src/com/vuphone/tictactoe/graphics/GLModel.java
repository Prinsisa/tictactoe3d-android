package com.vuphone.tictactoe.graphics;

import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;

import javax.microedition.khronos.opengles.GL11;

import com.vuphone.tictactoe.crossplatform.ApplePlistFile;

public class GLModel {

	private String texture;
	private ByteBuffer vertexData;
	private ByteBuffer texcoordData;
	private ByteBuffer normalData;
	private int vertexCount;
	
	public float x = 0;
	public float y = 0;
	public float z = 0;
	public float yaw = 0;
	public float pitch = 0;
	public float roll = 0;
	
	public GLModel(String fileName, String textureName)
	{
		texture = textureName;
		InputStream s = null;
		
		try {
			s = GLManager.getInstance().glContext.getAssets().open(fileName);
			ApplePlistFile f = new ApplePlistFile(s);
			
			vertexData = (ByteBuffer)f.objectForKey("vertices");
			texcoordData = (ByteBuffer)f.objectForKey("texcoords");
			normalData = (ByteBuffer)f.objectForKey("normals");
			vertexCount = ((Integer)f.objectForKey("vertexCount")).intValue();
			
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
	}

	public void draw()
	{
	    float mesh_diffuse[] = {0.75f, 0.5f, 0.26f, 1.0f};
	    float mesh_specular[] = {1, 1, 1, 1};
	    float mesh_shininess[] = {25.0f};
	    
	    GL11 gl = GLManager.getInstance().gl;
	    gl.glMaterialfv(GL11.GL_FRONT, GL11.GL_DIFFUSE, mesh_diffuse, 0);
	    gl.glMaterialfv(GL11.GL_FRONT, GL11.GL_SPECULAR, mesh_specular, 0);
	    gl.glMaterialfv(GL11.GL_FRONT, GL11.GL_SHININESS, mesh_shininess, 0);
	   
	    gl.glEnable(GL11.GL_TEXTURE_2D);
	    gl.glBindTexture(GL11.GL_TEXTURE_2D, GLManager.getInstance().textureIDForName(texture));
	    
	    gl.glPushMatrix();
	    gl.glTranslatef(x, y, z);
	    gl.glRotatef(yaw, 0, 1, 0);
	    gl.glRotatef(pitch, 0, 0, 1);
	    gl.glRotatef(roll, 1, 0, 0);
	    
	    gl.glNormalPointer(GL11.GL_FLOAT, 0, normalData);
	    gl.glEnableClientState(GL11.GL_NORMAL_ARRAY);
	    gl.glVertexPointer(3, GL11.GL_FLOAT, 0, vertexData);
	    gl.glEnableClientState(GL11.GL_VERTEX_ARRAY);
	    gl.glTexCoordPointer(2, GL11.GL_FLOAT, 0, texcoordData);
	    gl.glEnableClientState(GL11.GL_TEXTURE_COORD_ARRAY);
	    gl.glDrawArrays(GL11.GL_TRIANGLES, 0, vertexCount);
	    gl.glPopMatrix();
	}

}
