/*
 ** 2014 July 28
 **
 ** The author disclaims copyright to this source code.  In place of
 ** a legal notice, here is a blessing:
 **    May you do good and not evil.
 **    May you find forgiveness for yourself and forgive others.
 **    May you share freely, never taking more than you give.
 */
package info.ata4.minecraft.minema.client.modules.modifiers;

import info.ata4.minecraft.minema.Minema;
import info.ata4.minecraft.minema.client.config.MinemaConfig;
import info.ata4.minecraft.minema.client.engine.FixedTimer;
import info.ata4.minecraft.minema.client.modules.CaptureModule;
import info.ata4.minecraft.minema.util.reflection.PrivateAccessor;
import net.minecraft.util.Timer;

/**
 *
 * @author Nico Bergemann <barracuda415 at yahoo.de>
 */
public class TimerModifier extends CaptureModule {

	private static FixedTimer timer = null;
	private float defaultTps;

	@Override
	protected void doEnable() {
		MinemaConfig cfg = Minema.instance.getConfig();

		Timer defaultTimer = PrivateAccessor.getMinecraftTimer(MC);

		// check if it's modified already
		if (defaultTimer instanceof FixedTimer) {
			L.warn("Timer is already modified!");
			return;
		}

		// get default ticks per second if possible
		if (defaultTimer != null) {
			defaultTps = PrivateAccessor.getTimerTicksPerSecond(defaultTimer);
		}

		float fps = cfg.frameRate.get().floatValue();
		float speed = cfg.engineSpeed.get().floatValue();

		// set fixed delay timer
		timer = new FixedTimer(defaultTps, fps, speed);
		PrivateAccessor.setMinecraftTimer(MC, timer);
	}

	@Override
	protected boolean checkEnable() {
		return Minema.instance.getConfig().syncEngine.get() & MC.isSingleplayer();
	}

	@Override
	protected void doDisable() {
		// check if it's still modified
		if (!(PrivateAccessor.getMinecraftTimer(MC) instanceof FixedTimer)) {
			L.warn("Timer is already restored!");
			return;
		}

		// restore default timer
		timer = null;
		PrivateAccessor.setMinecraftTimer(MC, new Timer(defaultTps));
	}

	/**
	 * CALLED BY ASM INJECTED CODE! (COREMOD) DO NOT MODIFY METHOD SIGNATURE!
	 */
	public static void setFrameTimeCounter() {
		// This spot is right here because I can choose to only synchronize when
		// recording right here
		if (timer == null)
			return;
		timer.setFrameTimeCounter();
	}

}
