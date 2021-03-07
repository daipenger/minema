/*
 ** 2014 July 28
 **
 ** The author disclaims copyright to this source code.  In place of
 ** a legal notice, here is a blessing:
 **    May you do good and not evil.
 **    May you find forgiveness for yourself and forgive others.
 **    May you share freely, never taking more than you give.
 */
package info.ata4.minecraft.minema.client.config;

import info.ata4.minecraft.minema.util.config.ConfigBoolean;
import info.ata4.minecraft.minema.util.config.ConfigDouble;
import info.ata4.minecraft.minema.util.config.ConfigEnum;
import info.ata4.minecraft.minema.util.config.ConfigInteger;
import info.ata4.minecraft.minema.util.config.ConfigString;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.I18n;
import net.minecraftforge.common.config.ConfigCategory;
import net.minecraftforge.common.config.ConfigElement;
import net.minecraftforge.common.config.Configuration;
import net.minecraftforge.fml.client.config.IConfigElement;
import org.apache.commons.lang3.text.WordUtils;
import org.lwjgl.opengl.Display;

import java.io.File;
import java.util.Arrays;
import java.util.List;

/**
 *
 * @author Nico Bergemann <barracuda415 at yahoo.de>
 */
public class MinemaConfig {

	private static final int MAX_TEXTURE_SIZE = Minecraft.getGLMaximumTextureSize();

	private final Configuration configForge;

	private final ConfigCategory ENCODING_CATEGORY;
	private final ConfigCategory CAPTURING_CATEGORY;
	private final ConfigCategory ENGINE_CATEGORY;

	public final File dummyLog;

	public static final String LANG_KEY = "minema.config";

	public final ConfigBoolean useVideoEncoder = new ConfigBoolean(true);
	public final ConfigString videoEncoderPath = new ConfigString("ffmpeg");
	public final ConfigString videoEncoderParams = new ConfigString(
			"-f rawvideo -pix_fmt bgr24 -s %WIDTH%x%HEIGHT% -r %FPS% -i - -vf vflip -c:v libx264 -preset ultrafast -tune zerolatency -qp 18 -pix_fmt yuv420p %NAME%.mp4");
	public final ConfigString videoEncoderParamsAlpha = new ConfigString(
			"-f rawvideo -pix_fmt rgb32 -s %WIDTH%x%HEIGHT% -r %FPS% -i - -vf vflip -c:v libx264 -preset ultrafast -tune zerolatency -qp 18 -pix_fmt yuv420p %NAME%_rgb.mp4 -vf vflip,alphaextract,format=yuv420p %NAME%_alpha.mp4");
	public final ConfigEnum<SnapResolution> snapResolution = new ConfigEnum<>(SnapResolution.MOD2);
	public final ConfigBoolean enableEncoderLogging = new ConfigBoolean(true);

	public final ConfigInteger frameWidth = new ConfigInteger(0, 0, MAX_TEXTURE_SIZE);
	public final ConfigInteger frameHeight = new ConfigInteger(0, 0, MAX_TEXTURE_SIZE);
	public final ConfigDouble frameRate = new ConfigDouble(60.0, 1.0, 240.0);
	public final ConfigInteger frameLimit = new ConfigInteger(-1, -1);
	public final ConfigString capturePath = new ConfigString("movies");
	public final ConfigBoolean showOverlay = new ConfigBoolean(false);
	public final ConfigBoolean captureDepth = new ConfigBoolean(false);
	public final ConfigDouble captureDepthDistance = new ConfigDouble(0.0, 0.0, 256.0);
	public final ConfigBoolean recordGui = new ConfigBoolean(true);
	public final ConfigBoolean aaFastRenderFix = new ConfigBoolean(false);
	public final ConfigBoolean useAlpha = new ConfigBoolean(false);
	public final ConfigInteger heldFrames = new ConfigInteger(1, 1, 100);
	public final ConfigBoolean useAlphaScreenshot = new ConfigBoolean(false);
	public final ConfigBoolean exportAECamera = new ConfigBoolean(false);

	public final ConfigDouble engineSpeed = new ConfigDouble(1.0, 0.01, 100.0);
	public final ConfigBoolean syncEngine = new ConfigBoolean(true);
	public final ConfigBoolean preloadChunks = new ConfigBoolean(true);
	public final ConfigBoolean forcePreloadChunks = new ConfigBoolean(false);
	public final ConfigInteger networkDelay = new ConfigInteger(20, 0, Integer.MAX_VALUE);

	public MinemaConfig(File path) {
		Configuration cfg = new Configuration(path);

		this.dummyLog = new File(path.getParentFile(), "minema.log");
		this.configForge = cfg;

		ENCODING_CATEGORY = cfg.getCategory("encoding");
		CAPTURING_CATEGORY = cfg.getCategory("capturing");
		ENGINE_CATEGORY = cfg.getCategory("engine");

		for (ConfigCategory category : new ConfigCategory[] { ENCODING_CATEGORY, CAPTURING_CATEGORY,
				ENGINE_CATEGORY }) {
			String langKey = LANG_KEY + "." + category.getName();
			String comment = WordUtils.wrap(I18n.format(langKey + ".tooltip"), 128);
			category.setLanguageKey(langKey);
			category.setComment(comment);
		}

		useVideoEncoder.link(cfg, ENCODING_CATEGORY, "useVideoEncoder", LANG_KEY);
		videoEncoderPath.link(cfg, ENCODING_CATEGORY, "videoEncoderPath", LANG_KEY);
		videoEncoderParams.link(cfg, ENCODING_CATEGORY, "videoEncoderParams", LANG_KEY);
		videoEncoderParamsAlpha.link(cfg, ENCODING_CATEGORY, "videoEncoderParamsAlpha", LANG_KEY);
		snapResolution.link(cfg, ENCODING_CATEGORY, "snapResolution", LANG_KEY);
		enableEncoderLogging.link(cfg, ENCODING_CATEGORY, "enableEncoderLogging", LANG_KEY);

		frameWidth.link(cfg, CAPTURING_CATEGORY, "frameWidth", LANG_KEY);
		frameHeight.link(cfg, CAPTURING_CATEGORY, "frameHeight", LANG_KEY);
		frameRate.link(cfg, CAPTURING_CATEGORY, "frameRate", LANG_KEY);
		frameLimit.link(cfg, CAPTURING_CATEGORY, "frameLimit", LANG_KEY);
		capturePath.link(cfg, CAPTURING_CATEGORY, "capturePath", LANG_KEY);
		showOverlay.link(cfg, CAPTURING_CATEGORY, "showOverlay", LANG_KEY);
		captureDepth.link(cfg, CAPTURING_CATEGORY, "captureDepth", LANG_KEY);
		captureDepthDistance.link(cfg, CAPTURING_CATEGORY, "captureDepthDistance", LANG_KEY);
		recordGui.link(cfg, CAPTURING_CATEGORY, "recordGui", LANG_KEY);
		aaFastRenderFix.link(cfg, CAPTURING_CATEGORY, "aaFastRenderFix", LANG_KEY);
		useAlpha.link(cfg, CAPTURING_CATEGORY, "useAlpha", LANG_KEY);
		heldFrames.link(cfg, CAPTURING_CATEGORY, "heldFrames", LANG_KEY);
		useAlphaScreenshot.link(cfg, CAPTURING_CATEGORY, "useAlphaScreenshot", LANG_KEY);
		exportAECamera.link(cfg, CAPTURING_CATEGORY, "exportAECamera", LANG_KEY);

		engineSpeed.link(cfg, ENGINE_CATEGORY, "engineSpeed", LANG_KEY);
		syncEngine.link(cfg, ENGINE_CATEGORY, "syncEngine", LANG_KEY);
		preloadChunks.link(cfg, ENGINE_CATEGORY, "preloadChunks", LANG_KEY);
		forcePreloadChunks.link(cfg, ENGINE_CATEGORY, "forcePreloadChunks", LANG_KEY);
		networkDelay.link(cfg, ENGINE_CATEGORY, "networkDelay", LANG_KEY);
	}

	public Configuration getConfigForge() {
		return configForge;
	}

	public List<IConfigElement> getCategoryElements() {
		return Arrays.asList(new ConfigElement(ENCODING_CATEGORY), new ConfigElement(CAPTURING_CATEGORY),
				new ConfigElement(ENGINE_CATEGORY));
	}

	public int getFrameWidth() {
		int width = frameWidth.get();

		// use display width if not set
		if (width == 0) {
			width = Display.getWidth();
		}

		// snap to nearest
		if (useVideoEncoder.get()) {
			width = snapResolution.get().snap(width);
		}

		return width;
	}

	public int getFrameHeight() {
		int height = frameHeight.get();

		// use display height if not set
		if (height == 0) {
			height = Display.getHeight();
		}

		// snap to nearest
		if (useVideoEncoder.get()) {
			height = snapResolution.get().snap(height);
		}

		return height;
	}

	public boolean useFrameSize() {
		return getFrameWidth() != Display.getWidth() || getFrameHeight() != Display.getHeight();
	}

}
