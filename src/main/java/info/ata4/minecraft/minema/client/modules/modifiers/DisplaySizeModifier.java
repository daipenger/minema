package info.ata4.minecraft.minema.client.modules.modifiers;

import org.lwjgl.opengl.Display;

import info.ata4.minecraft.minema.Minema;
import info.ata4.minecraft.minema.client.config.MinemaConfig;
import info.ata4.minecraft.minema.client.modules.CaptureModule;
import net.minecraft.client.renderer.OpenGlHelper;
import net.minecraft.client.shader.Framebuffer;

public class DisplaySizeModifier extends CaptureModule {

	private int originalWidth;
	private int originalHeight;

	@Override
	protected void doEnable() {
		MinemaConfig cfg = Minema.instance.getConfig();
		originalWidth = Display.getWidth();
		originalHeight = Display.getHeight();

		resize(cfg.getFrameWidth(), cfg.getFrameHeight());

		// render framebuffer texture in original size
		if (OpenGlHelper.isFramebufferEnabled()) {
			setFramebufferTextureSize(originalWidth, originalHeight);
		}
	}

	@Override
	protected boolean checkEnable() {
		return Minema.instance.getConfig().useFrameSize();
	}

	@Override
	protected void doDisable() {
		resize(originalWidth, originalHeight);
	}

	public void resize(int width, int height) {
		MC.resize(width, height);
	}

	public void setFramebufferTextureSize(int width, int height) {
		Framebuffer fb = MC.getFramebuffer();
		fb.framebufferTextureWidth = width;
		fb.framebufferTextureHeight = height;
	}

}
