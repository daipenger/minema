package info.ata4.minecraft.minema.client.modules.video;

import static org.lwjgl.opengl.ARBBufferObject.glBindBufferARB;
import static org.lwjgl.opengl.ARBBufferObject.glMapBufferARB;
import static org.lwjgl.opengl.ARBBufferObject.glUnmapBufferARB;
import static org.lwjgl.opengl.GL11.GL_PACK_ALIGNMENT;
import static org.lwjgl.opengl.GL11.GL_TEXTURE_2D;
import static org.lwjgl.opengl.GL11.GL_UNPACK_ALIGNMENT;
import static org.lwjgl.opengl.GL11.GL_UNSIGNED_BYTE;
import static org.lwjgl.opengl.GL11.glGetTexImage;
import static org.lwjgl.opengl.GL11.glPixelStorei;
import static org.lwjgl.opengl.GL11.glReadPixels;
import static org.lwjgl.opengl.GL12.GL_BGR;

import org.lwjgl.opengl.Util;

import net.minecraft.client.shader.Framebuffer;

public class ColorbufferReader extends CommonReader {

	public ColorbufferReader(int width, int height, boolean isPBO, boolean isFBO) {
		super(width, height, 3, GL_UNSIGNED_BYTE, GL_BGR, isPBO, isFBO);
	}

	@Override
	public boolean readPixels() {
		// set alignment flags
		glPixelStorei(GL_PACK_ALIGNMENT, 1);
		glPixelStorei(GL_UNPACK_ALIGNMENT, 1);

		if (isPBO) {
			glBindBufferARB(PBO_TARGET, frontName);

			if (isFBO) {
				Framebuffer fb = MC.getFramebuffer();
				fb.bindFramebufferTexture();
				glGetTexImage(GL_TEXTURE_2D, 0, FORMAT, TYPE, 0);
				fb.unbindFramebufferTexture();
			} else {
				glReadPixels(0, 0, width, height, FORMAT, TYPE, 0);
			}

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
			if (isFBO) {
				Framebuffer fb = MC.getFramebuffer();
				fb.bindFramebufferTexture();
				glGetTexImage(GL_TEXTURE_2D, 0, FORMAT, TYPE, buffer);
				fb.unbindFramebufferTexture();
			} else {
				glReadPixels(0, 0, width, height, FORMAT, TYPE, buffer);
			}
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
