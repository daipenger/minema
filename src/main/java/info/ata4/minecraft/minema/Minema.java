package info.ata4.minecraft.minema;

import info.ata4.minecraft.minema.client.gui.GuiCaptureConfiguration;
import info.ata4.minecraft.minema.client.util.ScreenshotHelper;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.renderer.RenderGlobal;
import net.minecraft.client.renderer.culling.ClippingHelper;
import net.minecraftforge.client.event.RenderWorldLastEvent;
import net.minecraftforge.client.event.ScreenshotEvent;
import net.minecraftforge.fml.common.FMLCommonHandler;
import net.minecraftforge.fml.common.Loader;
import net.minecraftforge.fml.common.ModContainer;
import net.minecraftforge.fml.common.eventhandler.EventPriority;
import net.minecraftforge.fml.common.gameevent.TickEvent;
import net.minecraftforge.fml.relauncher.ReflectionHelper;
import org.lwjgl.input.Keyboard;

import info.ata4.minecraft.minema.client.config.MinemaConfig;
import net.minecraft.client.settings.KeyBinding;
import net.minecraftforge.client.ClientCommandHandler;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.common.config.Configuration;
import net.minecraftforge.fml.client.event.ConfigChangedEvent;
import net.minecraftforge.fml.client.registry.ClientRegistry;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.Mod.EventHandler;
import net.minecraftforge.fml.common.Mod.Instance;
import net.minecraftforge.fml.common.event.FMLInitializationEvent;
import net.minecraftforge.fml.common.event.FMLPreInitializationEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.InputEvent.KeyInputEvent;

import java.io.File;
import java.lang.reflect.Field;

/**
 * Most of the files in this repo do have the old copyright notice about
 * Barracuda even though I have touched most of it, in some cases substantially.
 * Few classes do not contain the notice, these are the ones that I have written
 * completely myself or some of the class with substantial changes.
 * 
 * @author Gregosteros (minecraftforum) / daipenger (github)
 */
@Mod(modid = Minema.MODID, name = Minema.NAME, clientSideOnly = true, acceptedMinecraftVersions = Minema.MCVERSION, version = Minema.VERSION, guiFactory = "info.ata4.minecraft.minema.client.config.MinemaConfigGuiFactory")
public class Minema {

	public static final String NAME = "Minema";
	public static final String MODID = "minema";
	public static final String VERSION = "3.5.4 TickSync";
	public static final String MCVERSION = "1.12.2";

	private static final String category = "key.categories.minema";
	private static final KeyBinding KEY_CAPTURE = new KeyBinding("key.minema.capture", Keyboard.KEY_F4, category);
	/* private static final KeyBinding KEY_FREEZE = new KeyBinding("key.minema.freeze", Keyboard.KEY_HOME, category); */

	@Instance(MODID)
	public static Minema instance;
	public static ModContainer container;

	private MinemaConfig config;

	@EventHandler
	public void onPreInit(FMLPreInitializationEvent e) {
		config = new MinemaConfig(e.getSuggestedConfigurationFile());
		container = Loader.instance().activeModContainer();
	}

	@EventHandler
	public void onInit(FMLInitializationEvent evt) {
		ClientCommandHandler.instance.registerCommand(new CommandMinema());
		ClientRegistry.registerKeyBinding(KEY_CAPTURE);
		/* ClientRegistry.registerKeyBinding(KEY_FREEZE); */
		MinecraftForge.EVENT_BUS.register(this);
	}

	@SubscribeEvent
	public void onConfigChanged(ConfigChangedEvent e) {
		if (e.getModID().equals(MODID)) {
			if (config.getConfigForge().hasChanged()) {
				config.getConfigForge().save();
			}
		}
	}

	@SubscribeEvent
	public void onKeyInput(KeyInputEvent event) {
		if (KEY_CAPTURE.isPressed()) {
			if (GuiScreen.isShiftKeyDown() && !CaptureSession.singleton.isEnabled())
				Minecraft.getMinecraft().displayGuiScreen(new GuiCaptureConfiguration());
			else if (!CaptureSession.singleton.startCapture())
				CaptureSession.singleton.stopCapture();
		}

		/* if (KEY_FREEZE.isPressed()) {
			try {
				final String terrainFrustumMcp = "debugFixTerrainFrustum";
				final String terrainFrustumSrg = "field_175002_T";
				final String terrainFrustumNotch = "U";
				final String terrainClipMcp = "debugFixedClippingHelper";
				final String terrainClipSrg = "field_175001_U";
				final String terrainClipNotch = "V";

				RenderGlobal global = Minecraft.getMinecraft().renderGlobal;
				Object helper = ReflectionHelper.getPrivateValue(RenderGlobal.class, global, terrainClipMcp, terrainFrustumSrg, terrainFrustumNotch);

				if (helper == null) {
					ReflectionHelper.setPrivateValue(RenderGlobal.class, global, true, terrainFrustumMcp, terrainFrustumSrg, terrainFrustumNotch);
				} else {
					ReflectionHelper.setPrivateValue(RenderGlobal.class, global, null, terrainClipMcp, terrainClipSrg, terrainClipNotch);
				}
			} catch (Exception e) {
				e.printStackTrace();
			}
		} */
	}

	public MinemaConfig getConfig() {
		return config;
	}

}
