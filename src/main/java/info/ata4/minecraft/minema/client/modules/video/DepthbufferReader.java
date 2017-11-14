package info.ata4.minecraft.minema.client.modules.video;

import static org.lwjgl.opengl.ARBBufferObject.glBindBufferARB;
import static org.lwjgl.opengl.ARBBufferObject.glMapBufferARB;
import static org.lwjgl.opengl.ARBBufferObject.glUnmapBufferARB;
import static org.lwjgl.opengl.GL11.GL_DEPTH_COMPONENT;
import static org.lwjgl.opengl.GL11.GL_FLOAT;
import static org.lwjgl.opengl.GL11.GL_PACK_ALIGNMENT;
import static org.lwjgl.opengl.GL11.GL_UNPACK_ALIGNMENT;
import static org.lwjgl.opengl.GL11.glPixelStorei;
import static org.lwjgl.opengl.GL11.glReadPixels;

import org.lwjgl.opengl.Util;

public class DepthbufferReader extends CommonReader {

	public DepthbufferReader(int width, int height, boolean isPBO, boolean isFBO) {
		super(width, height, 4, GL_FLOAT, GL_DEPTH_COMPONENT, isPBO, isFBO);
	}

	@Override
	public boolean readPixels() {
		// set alignment flags
		glPixelStorei(GL_PACK_ALIGNMENT, 1);
		glPixelStorei(GL_UNPACK_ALIGNMENT, 1);

		// Cannot read Minecraft's framebuffer even if it is active, as the depth buffer
		// is not a texture
		if (isPBO) {
			glBindBufferARB(PBO_TARGET, frontName);

			glReadPixels(0, 0, width, height, FORMAT, TYPE, 0);

			// copy back-buffer
			glBindBufferARB(PBO_TARGET, backName);
			buffer = glMapBufferARB(PBO_TARGET, PBO_ACCESS, bufferSize, buffer);
			glUnmapBufferARB(PBO_TARGET);
			glBindBufferARB(PBO_TARGET, 0);

			// If mapping threw an error -> crash immediately please
			Util.checkGLError();

			// swap PBOs
			int swapName = frontName;
			frontName = backName;
			backName = swapName;
		} else {
			glReadPixels(0, 0, width, height, FORMAT, TYPE, buffer);
		}

		buffer.rewind();

		// first frame is empty in PBO mode, don't export it
		if (isPBO & firstFrame) {
			firstFrame = false;
			return false;
		}

		return true;
	}

}
