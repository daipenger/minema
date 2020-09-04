/*
 ** 2012 March 31
 **
 ** The author disclaims copyright to this source code.  In place of
 ** a legal notice, here is a blessing:
 **    May you do good and not evil.
 **    May you find forgiveness for yourself and forgive others.
 **    May you share freely, never taking more than you give.
 */
package info.ata4.minecraft.minema.client.modules;

import java.util.ArrayList;

import info.ata4.minecraft.minema.CaptureSession;
import info.ata4.minecraft.minema.Minema;
import info.ata4.minecraft.minema.client.event.EndRenderEvent;
import info.ata4.minecraft.minema.client.event.MinemaEventbus;
import info.ata4.minecraft.minema.client.util.CaptureTime;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.ScaledResolution;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraftforge.client.event.RenderGameOverlayEvent;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.client.GuiErrorBase;
import net.minecraftforge.fml.common.eventhandler.EventPriority;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

/**
 * Minema information screen overlay.
 *
 * @author Nico Bergemann <barracuda415 at yahoo.de>
 */
public class CaptureOverlay extends CaptureModule {

	@Override
	protected void doEnable() throws Exception {
		MinecraftForge.EVENT_BUS.register(this);
		MinemaEventbus.endRenderBUS.registerListener((e) -> onRenderEnd(e));
	}

	private void onRenderEnd(EndRenderEvent e) throws Exception {
		CaptureTime time = CaptureSession.singleton.getTime();
		ArrayList<String> info = new ArrayList<String>();

		String frame = String.valueOf(time.getNumFrames());
		info.add("Frame: " + frame);

		String fps = Minecraft.getDebugFPS() + " fps";
		info.add("Rate: " + fps);

		String avg = (int) time.getAverageFPS() + " fps";
		info.add("Avg.: " + avg);

		String delay = CaptureTime.getTimeUnit(time.getPreviousCaptureTime());
		info.add("Delay: " + delay);

		info.add("Time R: " + time.getRealTimeString());
		info.add("Time V: " + time.getVideoTimeString());

		Minecraft mc = Minecraft.getMinecraft();
		FontRenderer font = mc.fontRenderer;
		ScaledResolution resolution = new ScaledResolution(mc);

		int x = 10;
		int y = resolution.getScaledHeight() - 14 * (info.size() - 1) - font.FONT_HEIGHT - 10;
		int s = resolution.getScaleFactor();

		GlStateManager.pushMatrix();
		GlStateManager.scale(s, s, s);

		for (String string : info)
		{
			int w = font.getStringWidth(string);

			Gui.drawRect(x - 2, y - 2, x + w + 2, y + font.FONT_HEIGHT + 2, 0x88000000);
			font.drawStringWithShadow(string, x, y + 1, 0xffffff);

			y += 14;
		}

		GlStateManager.popMatrix();
	}

	@Override
	protected void doDisable() throws Exception {
		MinecraftForge.EVENT_BUS.unregister(this);
	}

	@Override
	protected boolean checkEnable() {
		return Minema.instance.getConfig().showOverlay.get();
	}

}
