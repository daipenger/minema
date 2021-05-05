package info.ata4.minecraft.minema;

import info.ata4.minecraft.minema.client.config.MinemaConfig;
import info.ata4.minecraft.minema.client.event.AfterCameraEvent;
import info.ata4.minecraft.minema.client.event.EndRenderEvent;
import info.ata4.minecraft.minema.client.event.MidRenderEvent;
import info.ata4.minecraft.minema.client.event.MinemaEventbus;
import info.ata4.minecraft.minema.client.modules.CaptureModule;
import info.ata4.minecraft.minema.client.modules.CaptureNotification;
import info.ata4.minecraft.minema.client.modules.CaptureOverlay;
import info.ata4.minecraft.minema.client.modules.ChunkPreloader;
//import info.ata4.minecraft.minema.client.modules.ShaderSync;
import info.ata4.minecraft.minema.client.modules.SyncModule;
//import info.ata4.minecraft.minema.client.modules.TickSynchronizer;
import info.ata4.minecraft.minema.client.modules.modifiers.DisplaySizeModifier;
import info.ata4.minecraft.minema.client.modules.modifiers.GameSettingsModifier;
import info.ata4.minecraft.minema.client.modules.modifiers.TimerModifier;
import info.ata4.minecraft.minema.client.modules.video.VideoHandler;
import info.ata4.minecraft.minema.client.util.CaptureTime;
import info.ata4.minecraft.minema.client.util.MinemaException;
import info.ata4.minecraft.minema.util.reflection.ShadersHelper;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.I18n;
import net.minecraft.util.text.TextFormatting;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent.ClientTickEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent.Phase;
import net.minecraftforge.fml.common.gameevent.TickEvent.RenderTickEvent;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import org.apache.logging.log4j.LogManager;
import org.lwjgl.opengl.GLContext;

public class CaptureSession {

	public static final CaptureSession singleton = new CaptureSession();

	private final CaptureModule[] modules = new CaptureModule[] { new GameSettingsModifier(), new SyncModule(), /*new ShaderSync(),*/
			new TimerModifier(), /*new TickSynchronizer(),*/ new ChunkPreloader(), new DisplaySizeModifier(),
			new VideoHandler(), new CaptureOverlay(), new CaptureNotification() };

	private Path captureDir;
	private CaptureTime time;
	private int frameLimit;
	private boolean isEnabled;
	private boolean isRecording;

	private CaptureSession() {
	}

	public boolean isEnabled() {
		return isEnabled;
	}

	public boolean startCapture() {
		try {
			return start();
		} catch (MinemaException e) {
			Utils.printPrettyError(e);
			stopCapture();
		} catch (Exception e) {
			if (e.getCause() != null && e.getCause() instanceof MinemaException) {
				Utils.printPrettyError(e);
			} else {
				Utils.printError(e);
			}

			stopCapture();
		}

		return true;
	}

	public boolean start() throws Exception
	{
		if (isEnabled)
			return false;
		isEnabled = true;
		isRecording = false;

		try {
			Minecraft MC = Minecraft.getMinecraft();
			MinemaConfig cfg = Minema.instance.getConfig();

			frameLimit = cfg.getFrameLimit();
			captureDir = Paths.get(cfg.capturePath.get());

			if (!Files.exists(captureDir)) {
				try {
					Files.createDirectories(captureDir);
				} catch (SecurityException e) {
					throw new MinemaException(I18n.format("minema.error.capture_path_permissions", captureDir.toFile().getAbsolutePath()));
				}
			} else if (!Files.isDirectory(captureDir)) {
				throw new MinemaException(I18n.format("minema.error.capture_path_exists", captureDir.toFile().getAbsolutePath()));
			}

			if (cfg.syncEngine.get() & !MC.isSingleplayer()) {
				Utils.print(I18n.format("minema.error.warning").toUpperCase(), TextFormatting.RED);
				Utils.print(I18n.format("minema.error.tick_sync"), TextFormatting.RED);
			}

			if (cfg.preloadChunks.get() & !MC.isSingleplayer()) {
				Utils.print(I18n.format("minema.error.warning"), TextFormatting.YELLOW);
				Utils.print(I18n.format("minema.error.chunk_loading"), TextFormatting.YELLOW);
			}

			for (CaptureModule m : modules) {
				m.enable();
			}

			MinecraftForge.EVENT_BUS.register(this);

			time = new CaptureTime(cfg.getFrameRate());
		} catch (Exception e) {
			stopCapture();

			throw e;
		}

		return true;
	}

	public boolean stopCapture() {
		if (!isEnabled)
			return false;
		
		MinemaEventbus.reset();
		MinecraftForge.EVENT_BUS.unregister(this);

		for (CaptureModule m : modules) {
			if (m.isEnabled()) {
				try {
					m.disable();
				} catch (Exception e) {
					Utils.printError(e);
				}
			}
		}

		isEnabled = false;
		return true;
	}

	public Path getCaptureDir() {
		return captureDir;
	}

	public CaptureTime getTime() {
		return time;
	}

	private <X> void execFrameEvent(MinemaEventbus<X> bus, X event) {
		if (isEnabled) {

			if (frameLimit > 0 && time.getNumFrames() >= frameLimit) {
				stopCapture();
				return;
			}
			
			// Recording begin in next frame
			if (!this.isRecording)
				return;

			try {
				bus.throwEvent(event);
			} catch (Exception e) {
				Utils.printError(e);
				stopCapture();
			}

		}
	}
	
	// Recording begin in next frame
	@SubscribeEvent
	public void onRenderTick(RenderTickEvent e) {
		if (e.phase == Phase.END) {
			if (this.isRecording)
				execFrameEvent(MinemaEventbus.endRenderBUS, new EndRenderEvent(this));
			this.isRecording = true;
		}
	}

	/**
	 * Called by ASM hook
	 */
	public static void ASMmidRender() {
		singleton.execFrameEvent(MinemaEventbus.midRenderBUS, new MidRenderEvent(singleton));
	}

	/**
	 * Called by ASM hook
	 */
	public static void ASMAfterCamera() {
		singleton.execFrameEvent(MinemaEventbus.cameraBUS, new AfterCameraEvent(singleton));
	}
	
}
