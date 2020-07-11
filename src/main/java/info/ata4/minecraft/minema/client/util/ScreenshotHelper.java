package info.ata4.minecraft.minema.client.util;

import info.ata4.minecraft.minema.CaptureSession;
import info.ata4.minecraft.minema.Minema;
import info.ata4.minecraft.minema.client.config.MinemaConfig;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.OpenGlHelper;
import org.lwjgl.opengl.GL11;

import java.awt.image.BufferedImage;

public class ScreenshotHelper
{
	/**
	 * CALLED BY ASM INJECTED CODE! (COREMOD) DO NOT MODIFY METHOD SIGNATURE!
	 */
	public static int getType()
	{
		return Minema.instance.getConfig().useAlphaScreenshot.get() ? BufferedImage.TYPE_4BYTE_ABGR : BufferedImage.TYPE_INT_RGB;
	}

	public static boolean canSubstitute()
	{
		MinemaConfig config = Minema.instance.getConfig();

		return config.useAlphaScreenshot.get() || (CaptureSession.singleton.isEnabled() && config.useAlpha.get());
	}

	/**
	 * Replace the call for normal blend functions
	 */
	public static void replaceBlendFunc(int srcFactor, int dstFactor) {
		if(canSubstitute()) {
			if(srcFactor == GL11.GL_SRC_ALPHA && dstFactor == GL11.GL_ONE_MINUS_SRC_ALPHA) {
				magicBlendFunction();
			}
			else {
				GL11.glBlendFunc(srcFactor, dstFactor);
			}
		}
		else {
			GL11.glBlendFunc(srcFactor, dstFactor);
		}
	}

	public static void tryBlendFuncSeparate(int srcFactor, int dstFactor, int srcFactorAlpha, int dstFactorAlpha) {
		if(canSubstitute()) {
			if (srcFactor == GL11.GL_SRC_ALPHA && dstFactor == GL11.GL_ONE_MINUS_SRC_ALPHA
					&& srcFactorAlpha == GL11.GL_ONE_MINUS_DST_ALPHA && dstFactorAlpha == GL11.GL_ONE) {
				OpenGlHelper.glBlendFunc(srcFactor, dstFactor, srcFactorAlpha, dstFactorAlpha);
			}
			else if (srcFactor == GL11.GL_SRC_ALPHA && dstFactor == GL11.GL_ONE_MINUS_SRC_ALPHA) {
				magicBlendFunction();
			}
			else {
				OpenGlHelper.glBlendFunc(srcFactor, dstFactor, srcFactorAlpha, dstFactorAlpha);
			}
		}
		else {
			OpenGlHelper.glBlendFunc(srcFactor, dstFactor, srcFactorAlpha, dstFactorAlpha);
		}
	}

	public static void magicBlendFunction() {
		GlStateManager.tryBlendFuncSeparate(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA,
				GL11.GL_ONE_MINUS_DST_ALPHA, GL11.GL_ONE);
	}
}
