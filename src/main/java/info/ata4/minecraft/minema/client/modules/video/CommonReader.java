/*
** 2016 June 03
**
** The author disclaims copyright to this source code. In place of
** a legal notice, here is a blessing:
**    May you do good and not evil.
**    May you find forgiveness for yourself and forgive others.
**    May you share freely, never taking more than you give.
 */
package info.ata4.minecraft.minema.client.modules.video;

import static org.lwjgl.opengl.ARBBufferObject.GL_READ_ONLY_ARB;
import static org.lwjgl.opengl.ARBBufferObject.GL_STREAM_READ_ARB;
import static org.lwjgl.opengl.ARBBufferObject.glBindBufferARB;
import static org.lwjgl.opengl.ARBBufferObject.glBufferDataARB;
import static org.lwjgl.opengl.ARBBufferObject.glDeleteBuffersARB;
import static org.lwjgl.opengl.ARBBufferObject.glGenBuffersARB;
import static org.lwjgl.opengl.ARBPixelBufferObject.GL_PIXEL_PACK_BUFFER_ARB;

import java.nio.ByteBuffer;

import net.minecraft.client.Minecraft;

/**
 *
 * @author Nico Bergemann <barracuda415 at yahoo.de>
 */
public abstract class CommonReader {

	protected static final Minecraft MC = Minecraft.getMinecraft();
	protected static final int PBO_TARGET = GL_PIXEL_PACK_BUFFER_ARB;
	protected static final int PBO_USAGE = GL_STREAM_READ_ARB;
	protected static final int PBO_ACCESS = GL_READ_ONLY_ARB;

	protected final int TYPE;
	protected final int FORMAT;
	protected final boolean isPBO;
	protected final boolean isFBO;

	public final int width;
	public final int height;

	/**
	 * Might become a native buffer upon the first frame, if PBO is used
	 */
	public ByteBuffer buffer;
	protected final int bufferSize;

	protected int frontName;
	protected int backName;
	protected boolean firstFrame;

	public CommonReader(int width, int height, int BPP, int TYPE, int FORMAT, boolean isPBO, boolean isFBO) {
		this.TYPE = TYPE;
		this.FORMAT = FORMAT;
		this.isPBO = isPBO;
		this.isFBO = isFBO;
		this.width = width;
		this.height = height;

		bufferSize = width * height * BPP;

		if (isPBO) {
			frontName = glGenBuffersARB();
			glBindBufferARB(PBO_TARGET, frontName);
			glBufferDataARB(PBO_TARGET, bufferSize, PBO_USAGE);

			backName = glGenBuffersARB();
			glBindBufferARB(PBO_TARGET, backName);
			glBufferDataARB(PBO_TARGET, bufferSize, PBO_USAGE);

			glBindBufferARB(PBO_TARGET, 0);

			firstFrame = true;
		} else {
			this.buffer = ByteBuffer.allocateDirect(bufferSize);
			buffer.rewind();
		}
	}

	public abstract boolean readPixels();

	public void destroy() {
		if (isPBO) {
			glDeleteBuffersARB(frontName);
			glDeleteBuffersARB(backName);
		}
	}

}
