package info.ata4.minecraft.minema.client.util;

import info.ata4.minecraft.minema.CaptureSession;
import info.ata4.minecraft.minema.Minema;
import info.ata4.minecraft.minema.client.config.MinemaConfig;
import net.minecraft.client.renderer.GlStateManager;
import org.lwjgl.opengl.GL11;

import java.awt.image.BufferedImage;

public class ScreenshotHelper
{
	/**
	 * CALLED BY ASM INJECTED CODE! (COREMOD) DO NOT MODIFY METHOD SIGNATURE!
	 */
	public static int getType() {
		return Minema.instance.getConfig().useAlphaScreenshot.get() ? BufferedImage.TYPE_4BYTE_ABGR : BufferedImage.TYPE_INT_RGB;
	}

	/**
	 * CALLED BY ASM INJECTED CODE! (COREMOD) DO NOT MODIFY METHOD SIGNATURE!
	 */
	public static boolean canSubstitute() {
		if (Minema.instance != null) {
			MinemaConfig config = Minema.instance.getConfig();

			return config != null && (config.useAlphaScreenshot.get() || (CaptureSession.singleton.isEnabled() && config.useAlpha.get()));
		}

		return false;
	}

	/**
	 * Replace the call for normal blend functions
	 */
	public static void replaceBlendFunc(int srcFactor, int dstFactor) {
		if (canSubstitute() && srcFactor == GL11.GL_SRC_ALPHA && dstFactor == GL11.GL_ONE_MINUS_SRC_ALPHA) {
			magicBlendFunction();
		}
	}

	/**
	 * Replace the call for normal try blend functions
	 */
	public static void tryBlendFuncSeparate(int srcFactor, int dstFactor, int srcFactorAlpha, int dstFactorAlpha) {
		if (canSubstitute()) {
			if (srcFactor == GL11.GL_SRC_ALPHA && dstFactor == GL11.GL_ONE_MINUS_SRC_ALPHA && srcFactorAlpha == GL11.GL_ONE_MINUS_DST_ALPHA && dstFactorAlpha == GL11.GL_ONE) {

			} else if (srcFactor == GL11.GL_SRC_ALPHA && dstFactor == GL11.GL_ONE_MINUS_SRC_ALPHA) {
				magicBlendFunction();
			}
		}
	}

	public static void magicBlendFunction() {
		GlStateManager.tryBlendFuncSeparate(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA, GL11.GL_ONE_MINUS_DST_ALPHA, GL11.GL_ONE);
	}
}
