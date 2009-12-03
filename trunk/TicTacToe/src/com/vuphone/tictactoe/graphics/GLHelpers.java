package com.vuphone.tictactoe.graphics;

import java.io.ByteArrayOutputStream;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;

import javax.microedition.khronos.opengles.GL11;

import android.graphics.Bitmap;
import android.graphics.Color;

import com.vuphone.tictactoe.model.geometry.Point;
import com.vuphone.tictactoe.model.geometry.Rect;
import com.vuphone.tictactoe.model.geometry.Size;

public class GLHelpers {
	
	private static float unitVerticies[] = {
        -1.0f, -1.5f,
        1.0f, -1.5f,
        -1.0f,  1.5f,
        1.0f,  1.5f,
    };

    private static float unitTexcoords[] = {
        0, 0,
        1, 0,
        0, 1,
        1, 1,
    };
    
	public static ByteBuffer unitVerticiesBuffer = createBufferWrapper(unitVerticies);
    public static ByteBuffer unitTexcoordsBuffer = createBufferWrapper(unitTexcoords);
    private static FloatBuffer vertexBuffer = (ByteBuffer.allocateDirect(500 * 4)).asFloatBuffer();
    
	public static ByteBuffer createBufferWrapper(float verticies[]) 
	{
		ByteBuffer vbb = ByteBuffer.allocateDirect(verticies.length * 4);
		vbb.order(ByteOrder.nativeOrder());
		FloatBuffer mVertexBuffer = vbb.asFloatBuffer();
		mVertexBuffer.put(verticies);
		mVertexBuffer.position(0);
		return vbb;
	}

	public static ByteBuffer createBufferWrapper(int[] verticies)
	{
		ByteBuffer vbb = ByteBuffer.allocateDirect(verticies.length * 4);
		vbb.order(ByteOrder.nativeOrder());
		IntBuffer mVertexBuffer = vbb.asIntBuffer();
		mVertexBuffer.put(verticies);
		mVertexBuffer.position(0);
		return vbb;
	}
	
	public static ByteBuffer createBufferWrapperByte(int[] verticies)
	{
		ByteBuffer vbb = ByteBuffer.allocateDirect(verticies.length * 4);
		vbb.order(ByteOrder.nativeOrder());;
		for (int ii = 0; ii < verticies.length; ii++)
			vbb.put((byte)verticies[ii]);
		vbb.position(0);
		return vbb;
	}
	
	public static void CrossProduct(float x1, float y1, float z1, float x2, float y2, float z2, float res[]) 
	{ 
	    res[0] = y1*z2 - y2*z1; 
	    res[1] = x2*z1 - x1*z2; 
	    res[2] = x1*y2 - x2*y1; 
	} 
	
	public static void gluLookAt(float eyeX, float eyeY, float eyeZ, float lookAtX, float lookAtY, float lookAtZ, float upX, float upY, float upZ) 
	{ 
	    // i am not using here proper implementation for vectors. 
	    // if you want, you can replace the arrays with your own 
	    // vector types 
	    float f[] = {0,0,0}; 

	    // calculating the viewing vector 
	    f[0] = lookAtX - eyeX; 
	    f[1] = lookAtY - eyeY; 
	    f[2] = lookAtZ - eyeZ; 

	    float fMag = (float) Math.sqrt(f[0]*f[0] + f[1]*f[1] + f[2]*f[2]); 
	    float upMag = (float) Math.sqrt(upX*upX + upY*upY + upZ*upZ); 

	    // normalizing the viewing vector 
	    if( fMag != 0) 
	    { 
	    f[0] = f[0]/fMag; 
	    f[1] = f[1]/fMag; 
	    f[2] = f[2]/fMag; 
	    } 

	    // normalising the up vector. no need for this here if you have your 
	    // up vector already normalised, which is mostly the case. 
	    if( upMag != 0 ) 
	    { 
	    upX = upX/upMag; 
	    upY = upY/upMag; 
	    upZ = upZ/upMag; 
	    } 

	    float s[] = {0,0,0};
	    float u[] = {0,0,0};
	    
	    CrossProduct(f[0], f[1], f[2], upX, upY, upZ, s); 
	    CrossProduct(s[0], s[1], s[2], f[0], f[1], f[2], u); 

	    float M[]= 
	    { 
	    s[0], u[0], -f[0], 0, 
	    s[1], u[1], -f[1], 0, 
	    s[2], u[2], -f[2], 0, 
	    0, 0, 0, 1 
	    }; 

	    GLManager.getInstance().gl.glMultMatrixf(M,0); 
	    GLManager.getInstance().gl.glTranslatef (-eyeX, -eyeY, -eyeZ); 
	}
	
	public static int RoundToPowerOfTwo(int x) {
		int result = 1;
	    
	    if (x <= 1)
	        return 2;
	    else
	        // need to account for the fact that numbers already powers of 2, (32, 64, 128, etc...)
	        // will return a power of 2 one larger than necessary.
	        x = x - 1;
	        
	    while (x > 0){
	        x = x >> 1;
	        result = result << 1;
	    }
	    return result;
	}

	public static Bitmap ConvertGLDataToBitmap(byte [] bytes, int w, int h)
	{	
        int colors[]=new int[w * h]; 
        
        // convert the OpenGL integer data (which is in RGBA) to the android color format, which uses
        // a single integer to store all four color components (since int's are 32 bit)
        for(int i=0; i < h; i++) 
        {   
             for(int j=0; j < w; j++) 
             {  
                  int r=bytes[(i*w+j) * 4];
                  int g=bytes[(i*w+j) * 4 + 1];
                  int b=bytes[(i*w+j) * 4 + 2];
                  int a=bytes[(i*w+j) * 4 + 3];
                  colors[(h-i-1)*w+j] = (a << 24) | (r << 16) | (g << 8) | b;
             } 
        }

        return Bitmap.createBitmap(colors, w, h, Bitmap.Config.ARGB_8888);   
	}
	
	public static Bitmap DumpFramebufferRegionToBitmap(boolean useStaticMemory, Rect region) 
	{
		GL11 gl = GLManager.getInstance().gl;
		int w = (int)region.size.width;
		int h = (int)region.size.height;
		byte[] bytes=new byte[w * h * 4]; 
        ByteBuffer bb=ByteBuffer.wrap(bytes); 
        bb.position(0); 
  
        
        gl.glReadPixels((int)region.origin.x, (int)region.origin.y, w, h, GL11.GL_RGBA, GL11.GL_UNSIGNED_BYTE, bb); 
        return ConvertGLDataToBitmap(bytes, w, h);
	}

	public static ByteArrayOutputStream DumpFramebufferRegionToPNG(Rect rect) {
		ByteArrayOutputStream s = new ByteArrayOutputStream();
		Bitmap b = DumpFramebufferRegionToBitmap(true, rect);
		b.compress(Bitmap.CompressFormat.PNG, 100, s);
		
		return s;
	}

	public static ByteBuffer DumpFramebufferRegionToData(Rect region) {
		GL11 gl = GLManager.getInstance().gl;
		int w = (int)region.size.width;
		int h = (int)region.size.height;
		byte[] bytes=new byte[w * h * 4]; 
        ByteBuffer bb=ByteBuffer.wrap(bytes); 
        bb.position(0); 
        
        gl.glReadPixels((int)region.origin.x, (int)region.origin.y, w, h, GL11.GL_RGBA, GL11.GL_UNSIGNED_BYTE, bb); 
        return bb;
	}

	public static void CompositeSmudgeBetweenPoints(Point p, Point q, GLTexture brushTexture, GLTexture targetTexture, float size, float pressure, Size destinationTextureSize) {
		
		GL11 gl = GLManager.getInstance().gl;
	    gl.glEnable(GL11.GL_BLEND);
	    gl.glEnable(GL11.GL_TEXTURE_2D);

	    GLTexture stampTexture = CompositeSmudgeCreateStamp(p, brushTexture, targetTexture, size, destinationTextureSize);
	    CompositeSmudgeBetweenPointsWithStamp(p, q, stampTexture, targetTexture, size, pressure, destinationTextureSize);

	}

	public static GLTexture CompositeSmudgeCreateStamp(Point p, GLTexture brushTexture, GLTexture targetTexture, float size, Size destinationTextureSize)
	{
	    int sizeRounded = RoundToPowerOfTwo((int)size);
	    GLTexture stampTexture = new GLTexture(Color.TRANSPARENT, new Size(sizeRounded,sizeRounded));
	    GL11 gl = GLManager.getInstance().gl;
	    
	    stampTexture.clearRenderbufferAndDrawFullViewport(true);
	    
	    gl.glDisable(GL11.GL_BLEND);
	    gl.glEnable(GL11.GL_TEXTURE_2D);

	    float ratio = (sizeRounded/size);
	    float ox = (float) ((p.x-size/2.0) * ratio);
	    float oy = (float) (((destinationTextureSize.height-p.y)-size/2.0) * ratio);

	    float tex[] = {
	        0, 1,
	        1, 1,
	        0, 0,
	        1, 0,
	    };

	    float brushVer[] = {
	        0, 0,
	        sizeRounded, 0,
	        0, sizeRounded,
	        sizeRounded, sizeRounded,
	    };

	    float stampVer[] = {
	        -ox, -oy,
	        -ox+destinationTextureSize.width*ratio, -oy,
	        -ox, -oy+destinationTextureSize.height*ratio,
	        -ox+destinationTextureSize.width*ratio, -oy+destinationTextureSize.height*ratio,
	    };
	    
	    gl.glColor4f(1, 1, 1, 1);
	    gl.glVertexPointer(2, GL11.GL_FLOAT, 0, createBufferWrapper(stampVer));
	    gl.glEnableClientState(GL11.GL_VERTEX_ARRAY);
	    gl.glTexCoordPointer(2, GL11.GL_FLOAT, 0, createBufferWrapper(tex));
	    gl.glEnableClientState(GL11.GL_TEXTURE_COORD_ARRAY);
	    targetTexture.draw();
	    gl.glEnable(GL11.GL_BLEND);
	    gl.glBlendFunc(GL11.GL_ZERO, GL11.GL_SRC_ALPHA);
	    gl.glVertexPointer(2, GL11.GL_FLOAT, 0, createBufferWrapper(brushVer));
	    gl.glEnableClientState(GL11.GL_VERTEX_ARRAY);
	    gl.glTexCoordPointer(2, GL11.GL_FLOAT, 0, createBufferWrapper(unitTexcoords));
	    gl.glEnableClientState(GL11.GL_TEXTURE_COORD_ARRAY);
	    brushTexture.draw();
	    stampTexture.fillFromViewport();
	        
	    return stampTexture;
	}
	
	static void CompositeSmudgeBetweenPointsWithStamp(Point p, Point q, GLTexture stampTexture, GLTexture targetTexture, float size, float pressure, Size destinationTextureSize)
	{
	    int sizeRounded = RoundToPowerOfTwo((int)size);
	    GL11 gl = GLManager.getInstance().gl;
	    
	    float pointVerticies[] = {
	        -sizeRounded/2, -sizeRounded/2, 
	        sizeRounded/2, -sizeRounded/2, 
	        -sizeRounded/2, sizeRounded/2, 
	        sizeRounded/2, sizeRounded/2, 
	    };
	    
	    float pointTexcoords[] = {
	        0, 1,
	        1, 1,
	        0, 0,
	        1, 0,
	    };
	    
	    ByteBuffer pointVerticiesBuffer = createBufferWrapper(pointVerticies);
	    ByteBuffer pointTexcoordsBuffer = createBufferWrapper(pointTexcoords);
	    
	    boolean setupForPartialDraw = false;
	    int vertexCount = 0;
	    vertexBuffer.clear();
	    
	    // Add points to the buffer so there are drawing points every X pixels
	    float dist = FastSqrt((q.x - p.x) * (q.x - p.x) + (q.y - p.y) * (q.y - p.y));
	    if (dist < 1.0f)
	        return;
	    
	    float preferredSeparation = (float)size / 30.0f;
	    int count = (int) Math.max(dist / Math.max(1, preferredSeparation), 1);
	    
	    targetTexture.clearRenderbufferAndDrawFullViewport(true);
	    gl.glColor4f(pressure,pressure,pressure,pressure);
	    gl.glBindTexture(GL11.GL_TEXTURE_2D, stampTexture.texture);
	    gl.glBlendFunc(GL11.GL_ONE, GL11.GL_ONE_MINUS_SRC_ALPHA);
	    
	    float subdx = (q.x - p.x) / (float)count;
	    float subdy = (q.y - p.y) / (float)count;
	    
	    for(int i = 1; i <= count ; ++i) {
	        float ix = (float) Math.floor(p.x + subdx * (float)i);
	        float iy = (float) Math.floor(p.y + subdy * (float)i);
	        
	        if ((ix >= 0) && (ix <= destinationTextureSize.width) && (iy >= 0) && (iy <= destinationTextureSize.height) && (size <= 64.0)){
	            vertexBuffer.put(ix);
	            vertexBuffer.put(iy);
	            vertexCount += 1;
	            
	        } else if ((ix >= -sizeRounded) && (ix <= destinationTextureSize.width + sizeRounded) && (iy >= -sizeRounded) && (iy <= destinationTextureSize.height + sizeRounded)){
	            // point is partially offscreen - we can't draw it with glDrawPoints. Use a slower method.
	            if (!setupForPartialDraw){
	            	gl.glVertexPointer(2, GL11.GL_FLOAT, 0, pointVerticiesBuffer);
	                gl.glEnableClientState(GL11.GL_VERTEX_ARRAY);
	                gl.glTexCoordPointer(2, GL11.GL_FLOAT, 0, pointTexcoordsBuffer);
	                gl.glEnableClientState(GL11.GL_TEXTURE_COORD_ARRAY);
	                setupForPartialDraw = true;
	            }
	            gl.glTranslatef(ix, iy, 0);
	            gl.glDrawArrays(GL11.GL_TRIANGLE_STRIP, 0, 4);
	            gl.glTranslatef(-ix, -iy, 0);
	        }
	    }
	        
	    if (vertexCount > 0){
	        
	    	gl.glEnableClientState(GL11.GL_VERTEX_ARRAY);
	    	gl.glEnable(GL11.GL_POINT_SPRITE_OES);
	    	gl.glTexEnvf(GL11.GL_POINT_SPRITE_OES, GL11.GL_COORD_REPLACE_OES, GL11.GL_TRUE);
	    	gl.glPointSize(size);
	    	gl.glVertexPointer(2, GL11.GL_FLOAT, 0, vertexBuffer);
	    	gl.glDrawArrays(GL11.GL_POINTS, 0, vertexCount);
	    }
	    targetTexture.fillFromViewport();
	    
	}

	private static float FastSqrt(float z) {
	    float xhalf = 0.5f*z;
	    int i = (int)z;
	    i = 0x5f3759df - (i >> 1);
	    z = (float)i;
	    z = z*(1.5f - xhalf*z*z);
	    return 1.0f/z;
	}

	public static void CompositeStrokeBetweenPoints(Point p, Point q, GLTexture brushTexture, GLTexture targetTexture, float size, Size destinationTextureSize)
	{
		GL11 gl = GLManager.getInstance().gl;
		boolean pointSpriteSupported = GLManager.getInstance().pointSpriteSupported;
		
		targetTexture.clearRenderbufferAndDrawFullViewport(true);

		gl.glClearColor(0,0,0, 0);
	    gl.glClear(GL11.GL_COLOR_BUFFER_BIT);
	    
	    gl.glEnable(GL11.GL_BLEND);
	    gl.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE);
	    
	    FloatBuffer oldColor = ByteBuffer.allocateDirect(16).asFloatBuffer();
	    gl.glGetFloatv(GL11.GL_CURRENT_COLOR, oldColor);
	    
	    gl.glEnable(GL11.GL_TEXTURE_2D);
	    gl.glBindTexture(GL11.GL_TEXTURE_2D, brushTexture.texture);
	    
	    // Add points to the buffer so there are drawing points every X pixels
	    int dist = (int) Math.ceil(Math.sqrt((q.x - p.x) * (q.x - p.x) + (q.y - p.y) * (q.y - p.y)));
	    float preferredSeparation = (float)size / 30.0f;
	    int count = (int) Math.max(dist / Math.max(0.1, preferredSeparation), 1);
	    
	    int vertexCount = 0;
	    vertexBuffer.clear();
	    
	    float dx = (q.x - p.x) / count;
	    float dy = (q.y - p.y) / count;
	    float radius = size/2.0f;
	    boolean setupForPartialDraw = false;
	    
	    float pointVerticies[] = {
	        -radius, radius, 
	        radius, radius, 
	        -radius, -radius, 
	        radius, -radius, 
	    };
	    ByteBuffer pointVerticiesBuffer = createBufferWrapper(pointVerticies);
	    
	    for(int i = 0; i < count; ++i) {
	        float ix = (p.x + dx * i);
	        float iy = (p.y + dy * i);
	        
	        if (ix - Math.floor(ix) < 0.25f) ix += 0.5;
	        if (iy - Math.floor(iy) < 0.25f) iy += 0.5;
	            
	        if (pointSpriteSupported && (ix >= 0) && (ix <= destinationTextureSize.width) && (iy >= 0) && (iy <= destinationTextureSize.height) && (size <= 64.0)){
	            vertexBuffer.put(ix);
	            vertexBuffer.put(iy);
	            vertexCount += 1;
	            
	        } else {
	            // point is partially offscreen - we can't draw it with glDrawPoints. Use a slower method.
	            if (!setupForPartialDraw){
	            	gl.glVertexPointer(2, GL11.GL_FLOAT, 0, pointVerticiesBuffer);
	                gl.glEnableClientState(GL11.GL_VERTEX_ARRAY);
	                gl.glTexCoordPointer(2, GL11.GL_FLOAT, 0, unitTexcoordsBuffer);
	                gl.glEnableClientState(GL11.GL_TEXTURE_COORD_ARRAY);
	                setupForPartialDraw = true;
	            }
	            gl.glTranslatef(ix, iy, 0);
	            gl.glDrawArrays(GL11.GL_TRIANGLE_STRIP, 0, 4);
	            gl.glTranslatef(-ix, -iy, 0);
	        }
	    }
	    
	    if (vertexCount > 0){
	    	gl.glEnableClientState(GL11.GL_VERTEX_ARRAY);
	    	gl.glEnable(GL11.GL_POINT_SPRITE_OES);
	    	gl.glTexEnvf(GL11.GL_POINT_SPRITE_OES, GL11.GL_COORD_REPLACE_OES, GL11.GL_TRUE);
	    	gl.glPointSize(size);
	        
	    	gl.glVertexPointer(2, GL11.GL_FLOAT, 0, vertexBuffer);
	    	gl.glDrawArrays(GL11.GL_POINTS, 0, vertexCount);
	    }
	       
	    gl.glColor4f( oldColor.get(0), oldColor.get(1), oldColor.get(2), oldColor.get(3));
	    
	    targetTexture.fillFromViewport();
	    gl.glClear(GL11.GL_COLOR_BUFFER_BIT);
	}



	
}
