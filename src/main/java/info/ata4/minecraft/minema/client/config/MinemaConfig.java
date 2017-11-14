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

import java.util.Arrays;
import java.util.List;

import org.apache.commons.lang3.text.WordUtils;
import org.lwjgl.opengl.Display;

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

	public static final String LANG_KEY = "minema.config";

	public final ConfigBoolean useVideoEncoder = new ConfigBoolean(true);
	public final ConfigString videoEncoderPath = new ConfigString("ffmpeg");
	public final ConfigString videoEncoderParams = new ConfigString(
			"-f rawvideo -pix_fmt bgr24 -s %WIDTH%x%HEIGHT% -r %FPS% -i - -vf vflip -c:v libx264 -preset ultrafast -tune zerolatency -qp 18 -pix_fmt yuv420p %NAME%.mp4");
	public final ConfigEnum<SnapResolution> snapResolution = new ConfigEnum<>(SnapResolution.MOD2);

	public final ConfigInteger frameWidth = new ConfigInteger(0, 0, MAX_TEXTURE_SIZE);
	public final ConfigInteger frameHeight = new ConfigInteger(0, 0, MAX_TEXTURE_SIZE);
	public final ConfigDouble frameRate = new ConfigDouble(60.0, 1.0, 240.0);
	public final ConfigInteger frameLimit = new ConfigInteger(-1, -1);
	public final ConfigString capturePath = new ConfigString("movies");
	public final ConfigBoolean showOverlay = new ConfigBoolean(false);
	public final ConfigBoolean captureDepth = new ConfigBoolean(false);

	public final ConfigDouble engineSpeed = new ConfigDouble(1.0, 0.01, 100.0);
	public final ConfigBoolean syncEngine = new ConfigBoolean(true);
	public final ConfigBoolean preloadChunks = new ConfigBoolean(true);
	public final ConfigBoolean forcePreloadChunks = new ConfigBoolean(false);

	public MinemaConfig(Configuration cfg) {
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
		snapResolution.link(cfg, ENCODING_CATEGORY, "snapResolution", LANG_KEY);

		frameWidth.link(cfg, CAPTURING_CATEGORY, "frameWidth", LANG_KEY);
		frameHeight.link(cfg, CAPTURING_CATEGORY, "frameHeight", LANG_KEY);
		frameRate.link(cfg, CAPTURING_CATEGORY, "frameRate", LANG_KEY);
		frameLimit.link(cfg, CAPTURING_CATEGORY, "frameLimit", LANG_KEY);
		capturePath.link(cfg, CAPTURING_CATEGORY, "capturePath", LANG_KEY);
		showOverlay.link(cfg, CAPTURING_CATEGORY, "showOverlay", LANG_KEY);
		captureDepth.link(cfg, CAPTURING_CATEGORY, "captureDepth", LANG_KEY);

		engineSpeed.link(cfg, ENGINE_CATEGORY, "engineSpeed", LANG_KEY);
		syncEngine.link(cfg, ENGINE_CATEGORY, "syncEngine", LANG_KEY);
		preloadChunks.link(cfg, ENGINE_CATEGORY, "preloadChunks", LANG_KEY);
		forcePreloadChunks.link(cfg, ENGINE_CATEGORY, "forcePreloadChunks", LANG_KEY);
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
