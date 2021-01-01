/*
 ** 2012 January 3
 **
 ** The author disclaims copyright to this source code.  In place of
 ** a legal notice, here is a blessing:
 **    May you do good and not evil.
 **    May you find forgiveness for yourself and forgive others.
 **    May you share freely, never taking more than you give.
 */
package info.ata4.minecraft.minema.client.engine;

import info.ata4.minecraft.minema.Minema;
//import info.ata4.minecraft.minema.client.modules.ShaderSync;
import net.minecraft.util.Timer;

/**
 * Extension of Minecraft's default timer for fixed framerate rendering.
 *
 * @author Nico Bergemann <barracuda415 at yahoo.de> / Shader part: daipenger
 */
public class FixedTimer extends Timer {

	private final float ticksPerSecond;
	private final float framesPerSecond;
	private float timerSpeed;

	private int held;
	private int frames;
	private boolean canRecord;

	public FixedTimer(float tps, float fps, float speed) {
		super(tps);
		ticksPerSecond = tps;
		framesPerSecond = fps;
		timerSpeed = speed;

		held = Math.max(1, Minema.instance.getConfig().heldFrames.get());
	}

	public boolean canRecord() {
		return canRecord;
	}

	@Override
	public void updateTimer() {
		canRecord = false;
		frames += 1;

		if (frames >= held) {
//			if (held > 1) {
//				ShaderSync.freeze(false);
//			}

			frames = 0;
			canRecord = true;
		} else {
//			if (held > 1) {
//				ShaderSync.freeze(true);
//			}

			elapsedTicks = 0;
			return;
		}

		elapsedPartialTicks += timerSpeed * (ticksPerSecond / framesPerSecond);
		elapsedTicks = (int) elapsedPartialTicks;
		elapsedPartialTicks -= elapsedTicks;
		renderPartialTicks = elapsedPartialTicks;
	}

	public void setSpeed(float speed) {
		this.timerSpeed = speed;
	}

}
