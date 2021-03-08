package info.ata4.minecraft.minema.client.modules.video;

import java.nio.ByteBuffer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.text.SimpleDateFormat;
import java.util.Date;

import net.minecraft.client.resources.I18n;
import org.lwjgl.opengl.GLContext;

import info.ata4.minecraft.minema.CaptureSession;
import info.ata4.minecraft.minema.Minema;
import info.ata4.minecraft.minema.client.config.MinemaConfig;
import info.ata4.minecraft.minema.client.event.EndRenderEvent;
import info.ata4.minecraft.minema.client.event.MidRenderEvent;
import info.ata4.minecraft.minema.client.event.MinemaEventbus;
import info.ata4.minecraft.minema.client.modules.CaptureModule;
import info.ata4.minecraft.minema.client.modules.video.export.AfterEffectsFrameExporter;
import info.ata4.minecraft.minema.client.modules.video.export.FrameExporter;
import info.ata4.minecraft.minema.client.modules.video.export.ImageFrameExporter;
import info.ata4.minecraft.minema.client.modules.video.export.PipeFrameExporter;
import info.ata4.minecraft.minema.util.reflection.ShadersHelper;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.OpenGlHelper;

public class VideoHandler extends CaptureModule {

	public static String customName;

	private ColorbufferReader colorReader;
	private FrameExporter colorExport;

	private DepthbufferReader depthReader;
	private FrameExporter depthExport;
	private ByteBuffer depthRemapping;
	private float depthDistance;

	private String colorName;
	private String depthName;
	private int startWidth;
	private int startHeight;
	private boolean recordGui;
	
	private AfterEffectsFrameExporter aecamera = new AfterEffectsFrameExporter();

	@Override
	protected void doEnable() throws Exception {
		MinemaConfig cfg = Minema.instance.getConfig();

		this.startWidth = MC.displayWidth;
		this.startHeight = MC.displayHeight;
		this.colorName = customName == null || customName.isEmpty() ? new SimpleDateFormat("yyyy-MM-dd_HH.mm.ss").format(new Date()) : customName;
		this.depthName = colorName.concat("depthBuffer");
		this.depthDistance = cfg.captureDepthDistance.get().floatValue();
		this.recordGui = cfg.recordGui.get();

		boolean usePBO = GLContext.getCapabilities().GL_ARB_pixel_buffer_object;
		boolean useFBO = OpenGlHelper.isFramebufferEnabled();
		boolean usePipe = cfg.useVideoEncoder.get();
		boolean recordDepth = cfg.captureDepth.get();

		customName = null;
		colorReader = new ColorbufferReader(startWidth, startHeight, usePBO, useFBO, Minema.instance.getConfig().useAlpha.get());
		colorExport = usePipe ? new PipeFrameExporter() : new ImageFrameExporter();

		if (recordDepth) {
			depthReader = new DepthbufferReader(startWidth, startHeight, usePBO, useFBO);
			depthExport = usePipe ? new PipeFrameExporter() : new ImageFrameExporter();
			depthRemapping = ByteBuffer.allocateDirect(startWidth * startHeight * 3);
			depthRemapping.rewind();
		}

		if (!Minema.instance.getConfig().useVideoEncoder.get()) {
			Path colorDir = CaptureSession.singleton.getCaptureDir().resolve(colorName);
			Path depthDir = CaptureSession.singleton.getCaptureDir().resolve(depthName);

			if (!Files.exists(colorDir)) {
				Files.createDirectory(colorDir);
			}
			if (recordDepth && !Files.exists(depthDir)) {
				Files.createDirectory(depthDir);
			}
		}

		colorExport.enable(colorName, startWidth, startHeight);
		if (depthExport != null)
			depthExport.enable(depthName, startWidth, startHeight);

		MinemaEventbus.midRenderBUS.registerListener((e) -> onRenderMid(e));
		MinemaEventbus.endRenderBUS.registerListener((e) -> onRenderEnd(e));
		
		aecamera.setName(this.colorName);
		aecamera.enable();
	}

	@Override
	protected void doDisable() throws Exception {
		aecamera.disable();
		
		// Export Last Frame
		colorExport.waitForLastExport();
		if (colorReader.readLastFrame()) {
			colorExport.exportFrame(colorReader.buffer);
		}
		
		colorReader.destroy();
		colorExport.destroy();
		colorReader = null;
		colorExport = null;

		if (depthReader == null)
			return;

		depthExport.waitForLastExport();
		if (depthReader.readLastFrame()) {
			ByteBuffer floats = depthReader.buffer;

			while (floats.hasRemaining()) {
				float f = floats.getFloat();
				byte b = (byte) (linearizeDepth(f) * 255);
				depthRemapping.put(b);
				depthRemapping.put(b);
				depthRemapping.put(b);
			}

			floats.rewind();
			depthRemapping.rewind();

			depthExport.exportFrame(depthRemapping);
		}
		depthReader.destroy();
		depthExport.destroy();
		depthExport = null;
		depthReader = null;
		depthRemapping = null;
	}

	@Override
	protected boolean checkEnable() {
		return true;
	}

	private void onRenderMid(MidRenderEvent e) throws Exception {
		checkDimensions();

		if (depthReader != null) {
			depthExport.waitForLastExport();
			if (depthReader.readPixels()) {
				ByteBuffer floats = depthReader.buffer;

				while (floats.hasRemaining()) {
					float f = floats.getFloat();
					byte b = (byte) (linearizeDepth(f) * 255);
					depthRemapping.put(b);
					depthRemapping.put(b);
					depthRemapping.put(b);
				}

				floats.rewind();
				depthRemapping.rewind();

				depthExport.exportFrame(depthRemapping);
			}
		}

		if (!recordGui && !ShadersHelper.usingShaders()) {
			exportColor();

			e.session.getTime().nextFrame();
		}
	}

	private void exportColor() throws Exception {
		colorExport.waitForLastExport();
		if (colorReader.readPixels()) {
			colorExport.exportFrame(colorReader.buffer);
		}
	}

	private float linearizeDepth(float z) {
		final float near = 0.05f;
		final float far = this.depthDistance > 0 ? this.depthDistance : Minecraft.getMinecraft().gameSettings.renderDistanceChunks << 4;
		return 0.1f / (far + near - (2 * z - 1) * (far - near));
	}

	private void onRenderEnd(EndRenderEvent e) throws Exception {
		checkDimensions();

		if (recordGui || ShadersHelper.usingShaders()) {
			exportColor();

			e.session.getTime().nextFrame();
		}

	}

	private void checkDimensions() {
		if (MC.displayWidth != startWidth || MC.displayHeight != startHeight) {
			throw new IllegalStateException(I18n.format("minema.error.size_change",
					MC.displayWidth, MC.displayHeight, startWidth, startHeight));
		}
	}

}
