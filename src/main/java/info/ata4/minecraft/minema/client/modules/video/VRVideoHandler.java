package info.ata4.minecraft.minema.client.modules.video;

import java.io.File;
import java.io.InputStream;
import java.nio.DoubleBuffer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashSet;

import org.lwjgl.BufferUtils;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL12;
import org.lwjgl.opengl.GL13;
import org.lwjgl.opengl.GL20;
import org.lwjgl.opengl.GLContext;
import info.ata4.minecraft.minema.CaptureSession;
import info.ata4.minecraft.minema.Minema;
import info.ata4.minecraft.minema.client.config.MinemaConfig;
import info.ata4.minecraft.minema.client.event.EndRenderEvent;
import info.ata4.minecraft.minema.client.event.MidRenderEvent;
import info.ata4.minecraft.minema.client.event.MinemaEventbus;
import info.ata4.minecraft.minema.client.modules.CaptureModule;
import info.ata4.minecraft.minema.client.modules.modifiers.TimerModifier;
import info.ata4.minecraft.minema.client.modules.video.export.FrameExporter;
import info.ata4.minecraft.minema.client.modules.video.export.ImageFrameExporter;
import info.ata4.minecraft.minema.client.modules.video.export.PipeFrameExporter;
import info.ata4.minecraft.minema.client.modules.video.vr.CubeFace;
import info.ata4.minecraft.minema.client.modules.video.vr.Mp4SphericalInjector;
import info.ata4.minecraft.minema.client.util.MinemaException;
import info.ata4.minecraft.minema.util.reflection.PrivateAccessor;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.OpenGlHelper;
import net.minecraft.client.resources.I18n;
import net.minecraft.client.shader.Framebuffer;
import net.minecraft.util.text.ChatType;
import net.minecraft.util.text.TextComponentTranslation;
import net.minecraftforge.client.event.EntityViewRenderEvent.FOVModifier;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.common.eventhandler.EventPriority;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

public class VRVideoHandler extends CaptureModule {
	
	private static float SCALEFOV = 112.619865f; // atan(1.5)
	
	private static int program = -1;
	
	static {
		program = GL20.glCreateProgram();
		int vert = GL20.glCreateShader(GL20.GL_VERTEX_SHADER);
		try (InputStream in = VRVideoHandler.class.getResourceAsStream("/assets/minema/vr/vr.vert")) {
			byte[] b = new byte[in.available()];
			in.read(b);
			String code = new String(b);
			GL20.glShaderSource(vert, code);
			GL20.glCompileShader(vert);
			if (GL20.glGetShaderi(vert, GL20.GL_COMPILE_STATUS) == 0)
				throw new Exception();
			GL20.glAttachShader(program, vert);
		} catch (Exception e) {
			GL20.glDeleteShader(vert);
			GL20.glDeleteProgram(program);
			program = -1;
		}
		if (program != -1) {
			int frag = GL20.glCreateShader(GL20.GL_FRAGMENT_SHADER);
			try (InputStream in = VRVideoHandler.class.getResourceAsStream("/assets/minema/vr/vr.frag")) {
				byte[] b = new byte[in.available()];
				in.read(b);
				String code = new String(b);
				GL20.glShaderSource(frag, code);
				GL20.glCompileShader(frag);
				if (GL20.glGetShaderi(vert, GL20.GL_COMPILE_STATUS) == 0)
					throw new Exception();
				GL20.glAttachShader(program, frag);
			} catch (Exception e) {
				GL20.glDeleteShader(vert);
				GL20.glDeleteShader(frag);
				GL20.glDeleteProgram(program);
				program = -1;
			}
			if (program != -1) {
				GL20.glLinkProgram(program);
				GL20.glDetachShader(program, vert);
				GL20.glDetachShader(program, frag);
				GL20.glDeleteShader(vert);
				GL20.glDeleteShader(frag);
				GL20.glValidateProgram(program);
				if (GL20.glGetProgrami(program, GL20.GL_VALIDATE_STATUS) == 0) {
					GL20.glDeleteProgram(program);
					program = -1;
				} else {
					GL20.glUseProgram(program);
					GL20.glUniform1i(GL20.glGetUniformLocation(program, "tex"), 0);
					GL20.glUseProgram(0);
				}
			}
		}
	}
	
//	public static float getZNear(float origin) {
//		return instance != null && instance.isEnabled() ? 0.0005f : origin;
//	}

	private ColorbufferReader colorReader;
	private FrameExporter colorExport;

	private String colorName;
	private int startWidth;
	private int startHeight;
	private boolean recordGui;
	
	private int cubemap = -1;
	private boolean ssr = false;
	
	private final DoubleBuffer buffer = BufferUtils.createDoubleBuffer(16);
	private final HashSet<String> outputs = new HashSet<>();
	
	@Override
	protected void doEnable() throws Exception {
		MinemaConfig cfg = Minema.instance.getConfig();
		ssr = cfg.vrSSRSupport.get();
		
		cubemap = GL11.glGenTextures();
		GL11.glBindTexture(GL13.GL_TEXTURE_CUBE_MAP, cubemap);
		GL11.glTexParameteri(GL13.GL_TEXTURE_CUBE_MAP, GL11.GL_TEXTURE_MAG_FILTER, GL11.GL_LINEAR);
		GL11.glTexParameteri(GL13.GL_TEXTURE_CUBE_MAP, GL11.GL_TEXTURE_MIN_FILTER, GL11.GL_LINEAR);
		GL11.glTexParameteri(GL13.GL_TEXTURE_CUBE_MAP, GL11.GL_TEXTURE_WRAP_S, GL12.GL_CLAMP_TO_EDGE);
		GL11.glTexParameteri(GL13.GL_TEXTURE_CUBE_MAP, GL11.GL_TEXTURE_WRAP_T, GL12.GL_CLAMP_TO_EDGE);
		GL11.glTexParameteri(GL13.GL_TEXTURE_CUBE_MAP, GL12.GL_TEXTURE_WRAP_R, GL12.GL_CLAMP_TO_EDGE);
		GL11.glBindTexture(GL13.GL_TEXTURE_CUBE_MAP, 0);

		this.startWidth = MC.displayWidth;
		this.startHeight = MC.displayHeight;
		this.colorName = VideoHandler.customName == null || VideoHandler.customName.isEmpty() ? new SimpleDateFormat("yyyy-MM-dd_HH.mm.ss").format(new Date()) : VideoHandler.customName;
		this.recordGui = cfg.recordGui.get();

		boolean usePBO = GLContext.getCapabilities().GL_ARB_pixel_buffer_object;
		boolean useFBO = OpenGlHelper.isFramebufferEnabled();
		boolean usePipe = cfg.useVideoEncoder.get();
		
		CaptureSession.singleton.setFilename(this.colorName);
		VideoHandler.customName = null;
		colorReader = new ColorbufferReader(startWidth, startHeight, usePBO, useFBO, Minema.instance.getConfig().useAlpha.get());
		colorExport = usePipe ? new PipeFrameExporter(true) : new ImageFrameExporter();
		
		if (!Minema.instance.getConfig().useVideoEncoder.get()) {
			Path colorDir = CaptureSession.singleton.getCaptureDir().resolve(colorName);

			if (!Files.exists(colorDir)) {
				Files.createDirectory(colorDir);
			}
		}

		colorExport.enable(colorName, startWidth, startHeight);

		MinemaEventbus.midRenderBUS.registerListener((e) -> onRenderMid(e));
		MinemaEventbus.endRenderBUS.registerListener((e) -> onRenderEnd(e));
		MinemaEventbus.cameraBUS.registerListener(e -> this.afterCamera());
		MinecraftForge.EVENT_BUS.register(this);

		outputs.clear();

		if (!useFBO || program == -1)
			throw new MinemaException(I18n.format("minema.error.vr_not_support"));
		
		if (usePipe) {
			String paramStr = cfg.useAlpha.get() ? cfg.videoEncoderParamsAlpha.get() : cfg.videoEncoderParams.get();
			paramStr = paramStr.substring(paramStr.lastIndexOf(" -i ") + 4);
			String[] params = paramStr.split(" ");
			for (String param : params) {
				if (param.endsWith(".mp4")) {
					String filename = param.replace("%NAME%", colorName);
					if (Files.exists(CaptureSession.singleton.getCaptureDir().resolve(filename))) {
						outputs.clear();
						throw new MinemaException(I18n.format("minema.error.file_exists", filename));
					}
					outputs.add(filename);
				}
			}
		}
	}

	@Override
	protected void doDisable() throws Exception {
		MinecraftForge.EVENT_BUS.unregister(this);
		
		// Export Last Frame
		colorExport.waitForLastExport();
		if (colorReader.readLastFrame()) {
			colorExport.exportFrame(colorReader.buffer);
		}
		
		colorReader.destroy();
		colorExport.destroy();
		colorReader = null;
		colorExport = null;
		
		GL11.glDeleteTextures(cubemap);
		cubemap = -1;
		
		if (Minema.instance.getConfig().vrMetadata.get()) {
			File dir = CaptureSession.singleton.getCaptureDir().toFile();
			for (String output : outputs) {
				File in = new File(dir, output);
				if (in.exists()) {
					try {
						Mp4SphericalInjector.inject(in);
					} catch (Mp4SphericalInjector.InjectFailedException e) {
						MC.ingameGUI.addChatMessage(ChatType.CHAT, new TextComponentTranslation("minema.error.vr_broken", output));
					}
				}
			}
		}
	}

	@Override
	protected boolean checkEnable() {
		return Minema.instance.getConfig().vr.get();
	}

	private boolean exportColor() throws Exception {
		CubeFace face = TimerModifier.getCubeFace();
		GL11.glBindTexture(GL13.GL_TEXTURE_CUBE_MAP, cubemap);
		if (TimerModifier.canRecord()) {
			Framebuffer fb = MC.getFramebuffer();
			fb.bindFramebufferTexture();
			if (ssr)
				GL11.glCopyTexImage2D(face.target, 0, GL11.GL_RGBA8, startHeight / 3 * 2, startHeight / 6, startHeight / 3 * 2, startHeight / 3 * 2, 0);
			else
				GL11.glCopyTexImage2D(face.target, 0, GL11.GL_RGBA8, startHeight / 2, 0, startHeight, startHeight, 0);
			fb.unbindFramebufferTexture();
		}
		int prog = GL11.glGetInteger(GL20.GL_CURRENT_PROGRAM);
		GL20.glUseProgram(program);
		boolean alpha = GL11.glIsEnabled(GL11.GL_ALPHA_TEST);
		boolean blend = GL11.glIsEnabled(GL11.GL_BLEND);
		boolean depth = GL11.glIsEnabled(GL11.GL_DEPTH_TEST);
		boolean fog = GL11.glIsEnabled(GL11.GL_FOG);
		GlStateManager.disableAlpha();
		GlStateManager.disableBlend();
		GlStateManager.disableDepth();
		GlStateManager.disableFog();
        GL11.glBegin(GL11.GL_TRIANGLE_STRIP);
        GL11.glTexCoord2f(0.0F, 0.0F);
        GL11.glVertex3f(-1.0F, -1.0F, 0.0F);
        GL11.glTexCoord2f(1.0F, 0.0F);
        GL11.glVertex3f(1.0F, -1.0F, 0.0F);
        GL11.glTexCoord2f(0.0F, 1.0F);
        GL11.glVertex3f(-1.0F, 1.0F, 0.0F);
        GL11.glTexCoord2f(1.0F, 1.0F);
        GL11.glVertex3f(1.0F, 1.0F, 0.0F);
        GL11.glEnd();
        if (alpha)
    		GlStateManager.enableAlpha();
        if (!blend)
    		GlStateManager.enableBlend();
        if (depth)
    		GlStateManager.enableDepth();
        if (fog)
    		GlStateManager.enableFog();
		GL11.glBindTexture(GL13.GL_TEXTURE_CUBE_MAP, 0);
		GL20.glUseProgram(prog);
		
		if (TimerModifier.canRecord() && face == CubeFace.BOTTOM) {
			colorExport.waitForLastExport();
			if (colorReader.readPixels()) {
				colorExport.exportFrame(colorReader.buffer);
			}
			return true;
		}
		return false;
	}
	
	@SubscribeEvent(priority = EventPriority.LOWEST)
	public void lockFOV(FOVModifier e) {
		e.setFOV(ssr ? SCALEFOV : 90);
	}
	
	private void afterCamera() {
		CubeFace face = TimerModifier.getCubeFace();
		int mode = GL11.glGetInteger(GL11.GL_MATRIX_MODE);
		GL11.glMatrixMode(GL11.GL_MODELVIEW);
		buffer.clear();
		GL11.glGetDouble(GL11.GL_MODELVIEW_MATRIX, buffer);
		buffer.rewind();
		GL11.glLoadIdentity();
		GL11.glRotated(face.rotateX, 1, 0, 0);
		GL11.glRotated(face.rotateY, 0, 1, 0);
		GL11.glMultMatrix(buffer);
		GL11.glMatrixMode(mode);
	}

	private void onRenderMid(MidRenderEvent e) throws Exception {
		checkDimensions();

		if (!recordGui && !PrivateAccessor.isShaderPackLoaded()) {
			if (exportColor())
				e.session.getTime().nextFrame();
		}
	}

	private void onRenderEnd(EndRenderEvent e) throws Exception {
		checkDimensions();

		if (recordGui || PrivateAccessor.isShaderPackLoaded()) {
			if (exportColor())
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
