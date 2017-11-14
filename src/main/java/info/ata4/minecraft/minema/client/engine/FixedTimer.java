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

import info.ata4.minecraft.minema.util.reflection.PrivateAccessor;
import net.minecraft.util.Timer;

/**
 * Extension of Minecraft's default timer for fixed framerate rendering.
 *
 * @author Nico Bergemann <barracuda415 at yahoo.de> / Shader part: daipenger
 */
public class FixedTimer extends Timer {

	private final float ticksPerSecond;
	private final float framesPerSecond;
	private final float timerSpeed;

	/*
	 * Timespan between frames is 1/framesPerSecond (same as frequency and
	 * period in physics) -> the shader mod just measures the time between
	 * frames, in this context it is a constant time
	 */
	private final float frameTimeCounter_step;
	private float fixedFrameTimeCounter;

	public FixedTimer(float tps, float fps, float speed) {
		super(tps);
		ticksPerSecond = tps;
		framesPerSecond = fps;
		timerSpeed = speed;
		fixedFrameTimeCounter = PrivateAccessor.getFrameTimeCounter();
		frameTimeCounter_step = speed / fps;
	}

	@Override
	public void updateTimer() {
		elapsedPartialTicks += timerSpeed * (ticksPerSecond / framesPerSecond);
		elapsedTicks = (int) elapsedPartialTicks;
		elapsedPartialTicks -= elapsedTicks;
		renderPartialTicks = elapsedPartialTicks;
		// Shader mod analog code
		fixedFrameTimeCounter += frameTimeCounter_step;
		fixedFrameTimeCounter %= 3600.0F;
	}

	public void setFrameTimeCounter() {
		PrivateAccessor.setFrameTimeCounter(fixedFrameTimeCounter);
	}

}
