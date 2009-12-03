package com.vuphone.tictactoe.graphics;

import java.io.ByteArrayOutputStream;
import java.nio.ByteBuffer;
import java.nio.IntBuffer;

import javax.microedition.khronos.opengles.GL10;
import javax.microedition.khronos.opengles.GL11;
import javax.microedition.khronos.opengles.GL11ExtensionPack;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.opengl.GLUtils;
import android.util.Log;

import com.vuphone.tictactoe.model.geometry.Rect;
import com.vuphone.tictactoe.model.geometry.Size;


public class GLTexture {

	public static int compositingFramebuffer = -1;
	public static int originalActiveFramebuffer = -1;
	private static float bindVerticies[] = {
	    0.0f, 0.0f,
	    1.0f, 0.0f,
	    0.0f, 1.0f,
	    1.0f, 1.0f,
	};

	
    public int texture;
    public Size size;

    public GLTexture(int color, Size s)
    {
    	GL11 gl = GLManager.getInstance().gl;
    	int[] t = new int[1];
		gl.glGenTextures(1, t, 0);
		
		// setup our properties
		texture = t[0];
		size = s;
		
		// create a new bitmap with the desired color
		Bitmap bmp = Bitmap.createBitmap((int)s.width, (int)s.height, Bitmap.Config.ARGB_8888);
		Canvas c = new Canvas(bmp);
		c.drawARGB(Color.alpha(color), Color.red(color), Color.green(color), Color.blue(color));

		// bind the bitmap to the opengl space
		gl.glBindTexture(GL10.GL_TEXTURE_2D, texture);
		gl.glTexParameterx(GL10.GL_TEXTURE_2D, GL10.GL_TEXTURE_MIN_FILTER,
				GL10.GL_LINEAR);
		gl.glTexParameterx(GL10.GL_TEXTURE_2D, GL10.GL_TEXTURE_MAG_FILTER,
				GL10.GL_LINEAR);
		GLUtils.texImage2D(GL10.GL_TEXTURE_2D, 0, bmp, 0);
		bmp.recycle();
    }
    
    public GLTexture(Bitmap img)
    {
    	GL11 gl = GLManager.getInstance().gl;
    	int[] t = new int[1];
		gl.glGenTextures(1, t, 0);
		
		// setup our properties
		texture = t[0];
		size = new Size(img.getWidth(), img.getHeight());
		
		Log.d("Layers Graphics","Texture given #" + texture);
		
		// bind the bitmap to the opengl space
		gl.glBindTexture(GL10.GL_TEXTURE_2D, texture);
		gl.glTexParameterx(GL10.GL_TEXTURE_2D, GL10.GL_TEXTURE_MIN_FILTER,
				GL10.GL_LINEAR);
		gl.glTexParameterx(GL10.GL_TEXTURE_2D, GL10.GL_TEXTURE_MAG_FILTER,
				GL10.GL_LINEAR);
		GLUtils.texImage2D(GL10.GL_TEXTURE_2D, 0, img, 0);
    }
    
    public GLTexture(Bitmap img, Size s)
    {
    	GL11 gl = GLManager.getInstance().gl;
    	int[] t = new int[1];
		gl.glGenTextures(1, t, 0);
		
		// setup our properties
		texture = t[0];
		size = s;
		
		// create a new bitmap with the desired color
		Bitmap bmp = Bitmap.createScaledBitmap(img, (int)s.width, (int)s.height, true);
		// bind the bitmap to the opengl space
		gl.glBindTexture(GL10.GL_TEXTURE_2D, texture);
		gl.glTexParameterx(GL10.GL_TEXTURE_2D, GL10.GL_TEXTURE_MIN_FILTER,
				GL10.GL_LINEAR);
		gl.glTexParameterx(GL10.GL_TEXTURE_2D, GL10.GL_TEXTURE_MAG_FILTER,
				GL10.GL_LINEAR);
		GLUtils.texImage2D(GL10.GL_TEXTURE_2D, 0, bmp, 0);
		bmp.recycle();
    }
    
    public GLTexture(IntBuffer b, Size s)
    {
    	GL11 gl = GLManager.getInstance().gl;
    	int[] t = new int[1];
		gl.glGenTextures(1, t, 0);
		
		// setup our properties
		texture = t[0];
		size = s;

		Bitmap bmp = Bitmap.createBitmap(b.array(), (int)s.width, (int)s.height, Bitmap.Config.ARGB_8888);
		// bind the bitmap to the opengl space
		gl.glBindTexture(GL10.GL_TEXTURE_2D, texture);
		gl.glTexParameterx(GL10.GL_TEXTURE_2D, GL10.GL_TEXTURE_MIN_FILTER,
				GL10.GL_LINEAR);
		gl.glTexParameterx(GL10.GL_TEXTURE_2D, GL10.GL_TEXTURE_MAG_FILTER,
				GL10.GL_LINEAR);
		GLUtils.texImage2D(GL10.GL_TEXTURE_2D, 0, bmp, 0);
		bmp.recycle();
    }

    // Drawing Texture

    public void draw()
    {
    	GL11 gl = GLManager.getInstance().gl;
    	gl.glBindTexture(GL10.GL_TEXTURE_2D, texture);
    	gl.glDrawArrays(GL10.GL_TRIANGLE_STRIP, 0, 4);
    }
    
    public void draw(int textureFilter)
    {
    	GL11 gl = GLManager.getInstance().gl;
    	gl.glBindTexture(GL10.GL_TEXTURE_2D, texture);
        gl.glTexParameterx(GL10.GL_TEXTURE_2D, GL10.GL_TEXTURE_MIN_FILTER, textureFilter);
        gl.glTexParameterx(GL10.GL_TEXTURE_2D, GL10.GL_TEXTURE_MAG_FILTER, textureFilter);
        gl.glDrawArrays(GL10.GL_TRIANGLE_STRIP, 0, 4);
    }
    
    // Compositing Into Texture

    public void clearRenderbufferAndDrawFullViewport(boolean shouldDrawTexture)
    {
    	GL11 gl = GLManager.getInstance().gl;
    	GL11ExtensionPack glp = (GL11ExtensionPack)gl;
    	
    	// reset our viewport
    	gl.glMatrixMode(GL11.GL_PROJECTION);
        gl.glLoadIdentity();
        gl.glViewport(0, 0, (int)size.width, (int)size.height);
        gl.glMatrixMode(GL11.GL_MODELVIEW);
        gl.glLoadIdentity();
        gl.glOrthof(0, size.width, 0, size.height, -1, 1);
        
        bindVerticies[2] = size.width;
        bindVerticies[5] = size.height;
        bindVerticies[6] = size.width;
        bindVerticies[7] = size.height;
        
        // Sets up pointers and enables states needed for using vertex arrays and textures
        gl.glVertexPointer(2, GL11.GL_FLOAT, 0, GLHelpers.createBufferWrapper(bindVerticies));
        gl.glEnableClientState(GL11.GL_VERTEX_ARRAY);
        gl.glTexCoordPointer(2, GL11.GL_FLOAT, 0, GLHelpers.unitTexcoordsBuffer);
        gl.glEnableClientState(GL11.GL_TEXTURE_COORD_ARRAY);
        gl.glEnable(GL11.GL_TEXTURE_2D);
        gl.glEnable(GL11.GL_BLEND);
 
        // clear everything off the screen
        gl.glClearColor(0,0,0,0);
        gl.glClear(GL11.GL_COLOR_BUFFER_BIT);
    
        
        // draw ourselves in!
        if (shouldDrawTexture) draw();
    }
    
    public void bindToFramebufferAndClearDep(boolean shouldClear)
    {
    	if (GLManager.getInstance().framebuffersSupported == false){
    		throw new UnsupportedOperationException();
    	}
    	
    	GL11 gl = GLManager.getInstance().gl;
    	GL11ExtensionPack glp = (GL11ExtensionPack)gl;
    	int[] r = new int[1];
    	
    	// make sure the compositing framebuffer exists
        if (compositingFramebuffer == 0){
        	glp.glGenFramebuffersOES(1, r, 0);
        	compositingFramebuffer = r[0];
        }
            
        // grab the current compositingFramebuffer so we can restore it later
        gl.glGetIntegerv(GL11ExtensionPack.GL_FRAMEBUFFER_BINDING_OES, r, 0);
        originalActiveFramebuffer = r[0];
        
        // make sure this is NOT the compositing framebuffer
        if (originalActiveFramebuffer == compositingFramebuffer){
            Log.e("LayersGraphics", "BeginCompositeIntoTexture called with compositingFramebuffer already bound!!");
            return;
        }
        
        // bind a compositing framebuffer and bind it to texture
        glp.glBindFramebufferOES(GL11ExtensionPack.GL_FRAMEBUFFER_OES, compositingFramebuffer);
        
        glp.glGetFramebufferAttachmentParameterivOES(GL11ExtensionPack.GL_FRAMEBUFFER_OES, GL11ExtensionPack.GL_COLOR_ATTACHMENT0_OES, GL11.GL_TEXTURE_2D, r, 0);
        if (r[0] != texture){
            glp.glFramebufferTexture2DOES(GL11ExtensionPack.GL_FRAMEBUFFER_OES, GL11ExtensionPack.GL_COLOR_ATTACHMENT0_OES, GL11.GL_TEXTURE_2D, texture, 0);
        }
        
        gl.glMatrixMode(GL11.GL_PROJECTION);
        gl.glLoadIdentity();
        gl.glViewport(0, 0, (int)size.width, (int)size.height);
        gl.glMatrixMode(GL11.GL_MODELVIEW);
        gl.glLoadIdentity();
        gl.glOrthof(0, size.width, 0, size.height, -1, 1);
        
        bindVerticies[2] = size.width;
        bindVerticies[5] = size.height;
        bindVerticies[6] = size.width;
        bindVerticies[7] = size.height;
        
        // Sets up pointers and enables states needed for using vertex arrays and textures
        gl.glVertexPointer(2, GL11.GL_FLOAT, 0, GLHelpers.createBufferWrapper(bindVerticies));
        gl.glEnableClientState(GL11.GL_VERTEX_ARRAY);
        gl.glTexCoordPointer(2, GL11.GL_FLOAT, 0, GLHelpers.unitTexcoordsBuffer);
        gl.glEnableClientState(GL11.GL_TEXTURE_COORD_ARRAY);
        gl.glEnable(GL11.GL_TEXTURE_2D);
        gl.glEnable(GL11.GL_BLEND);
        
        if (shouldClear){
            gl.glClearColor(0,0,0,0);
            gl.glClear(GL11.GL_COLOR_BUFFER_BIT);
        }
    }
    
	public void unbindDep()
	{
    	GL11ExtensionPack glp = (GL11ExtensionPack)GLManager.getInstance().gl;
    	glp.glBindFramebufferOES(GL11ExtensionPack.GL_FRAMEBUFFER_OES, originalActiveFramebuffer);
	}
	
	public void clear()
	{
		this.clearRenderbufferAndDrawFullViewport(false);
		this.fillFromViewport();
	}
	
	// Reading Texture Data
	
	public Bitmap newBitmap(boolean immediateUse)
	{
		clearRenderbufferAndDrawFullViewport(true);
		//bindToFramebufferAndClear(false);
		Bitmap b = GLHelpers.DumpFramebufferRegionToBitmap(immediateUse, new Rect(0,0,size.width,size.height));
		//unbind();
	    return b;
	}
	
	public ByteBuffer getData()
	{
		return getDataInRegion(new Rect(0,0,size.width,size.height));
	}
	
	public ByteBuffer getDataInRegion(Rect region)
	{
		clearRenderbufferAndDrawFullViewport(true);
		//bindToFramebufferAndClear(false);
		ByteBuffer data = GLHelpers.DumpFramebufferRegionToData(new Rect(0,0,size.width,size.height));
		//unbind();
		return data;
	}
	
	public ByteArrayOutputStream getPNGData()
	{
		clearRenderbufferAndDrawFullViewport(true);
		//bindToFramebufferAndClear(false);
		ByteArrayOutputStream data = GLHelpers.DumpFramebufferRegionToPNG(new Rect(0,0,size.width,size.height));
		//unbind();
		return data;
	}
	
	// Replacing Texture Data
	
	public void replaceTextureData(Rect region, ByteBuffer data, boolean dataIsFromPNG)
	{
	    // If the data we've been handed is a PNG representation of an image, it's pretty easy to draw it
	    // into the correct region. If that's not the case, we have to do a bit more work to load the image.
	   Bitmap b = null;
		if (dataIsFromPNG)
			b = BitmapFactory.decodeByteArray(data.array(), 0, data.limit());
		else
			b = GLHelpers.ConvertGLDataToBitmap(data.array(), (int)region.size.width, (int)region.size.height);
	   	
	    replaceTextureData(region, b);
	}
	
	public void replaceTextureData(Rect region, Bitmap image)
	{
		// Here we go:
	    // 1) Determine a power-of-two region from region parameter
	    // 2) Get current texture data in power-of-two region out of OpenGL as a CGImage
	    // 3) Composite in the new portion (also CGImage)
	    // 4) Create a new temporary texture from the power-of-two region
	    // 5) Draw temporary texture into our main texture
	    // 6) Destroy temporary texture
	    
	    // Determine how we can fit the region into a texture. We want to create the smallest texture
	    // that will hold the modified region. This dramatically improves performance.
	    Rect textureRegion = region;
	    textureRegion.size.width = GLHelpers.RoundToPowerOfTwo((int)region.size.width);
	    textureRegion.size.height = GLHelpers.RoundToPowerOfTwo((int)region.size.height);
	    if (textureRegion.origin.x + textureRegion.size.width > size.width)
	        textureRegion.origin.x = size.width - textureRegion.size.width;
	    if (textureRegion.origin.y + textureRegion.size.height > size.height)
	        textureRegion.origin.y = size.height - textureRegion.size.height;
	    
	    //NSLog(@"replacing %f, %f", textureRegion.size.width, textureRegion.size.height);
	    
	    // Create an image of the current texture data in that region
	    clearRenderbufferAndDrawFullViewport(true);
		//bindToFramebufferAndClear(false);
	    Bitmap current = GLHelpers.DumpFramebufferRegionToBitmap(true, textureRegion); // use static memory
	    //unbind();
	    
	    // Create a canvas to perform the compositing in. needs to be zeroed!
	    // We'll pass the context data directly to openGL to create the new texture object later.
	    Canvas c = new Canvas(current);
	    	   
	    // We have to translate the region's origin too since we flip the damn thing upside down.
	    Rect flippedTextureRegion = textureRegion;
	    flippedTextureRegion.origin.y = -textureRegion.origin.y + size.height - textureRegion.size.height;
	    Rect flippedRegion = region;
	    flippedRegion.origin.y = -region.origin.y + size.height - region.size.height;

	    // We have some data representing part of the textureRegion we'd like to replace. However, the data
	    // is only for PART of the textureRegion, since we rounded up to a power of two. We need to get
	    // RELATIVE coordinates.
	    Rect relativeRegion = new Rect(flippedRegion.origin.x - flippedTextureRegion.origin.x, flippedRegion.origin.y - flippedTextureRegion.origin.y, flippedRegion.size.width, flippedRegion.size.height);
	    
	    // draw the new image data into the region that we're replacing
	    c.save();
	    c.clipRect(relativeRegion.toRectF());
	    c.drawColor(Color.TRANSPARENT);
	    c.drawBitmap(image, new android.graphics.Rect(0,0,image.getWidth(), image.getHeight()), relativeRegion.toRectF(), new Paint());
	    	    
	    // get the resulting image and create a texture from it
	    GLTexture resultTexture = new GLTexture(current);
	    GL11 gl = GLManager.getInstance().gl;
	   	    
	    // paste the result texture into the layer texture, using a glBlendMode that will
	    // completely replace the pixel data underneath
	    //bindToFramebufferAndClear(false);
	    float textureRegionVerticies[] = {
	        textureRegion.origin.x, textureRegion.origin.y,
	        textureRegion.origin.x + textureRegion.size.width, textureRegion.origin.y,
	        textureRegion.origin.x,  textureRegion.origin.y + textureRegion.size.height,
	        textureRegion.origin.x + textureRegion.size.width,  textureRegion.origin.y + textureRegion.size.height,
	    };
	    gl.glVertexPointer(2, GL11.GL_FLOAT, 0, GLHelpers.createBufferWrapper(textureRegionVerticies));
	    gl.glEnableClientState(GL11.GL_VERTEX_ARRAY);
	    gl.glEnable(GL11.GL_BLEND);
	    gl.glBlendFunc(GL11.GL_ONE, GL11.GL_ZERO);
	    resultTexture.draw();
	    
	    fillFromViewport();	    
	    //unbind();
	}

	public void fillFromViewport() {
		GL11 gl = GLManager.getInstance().gl;
	   	//First, chose a texture to bind the screenshot to...
	    gl.glBindTexture(GL11.GL_TEXTURE_2D, this.texture);

	    //Then... you simply use this command to bind it...
	    gl.glCopyTexImage2D(GL11.GL_TEXTURE_2D, 0, GL11.GL_RGBA, 0, 0, (int)size.width, (int)size.height, 0);

	}

}
