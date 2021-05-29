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

import org.apache.logging.log4j.LogManager;

import info.ata4.minecraft.minema.CaptureSession;
import info.ata4.minecraft.minema.Minema;
import info.ata4.minecraft.minema.client.modules.SyncModule;
import info.ata4.minecraft.minema.client.modules.SyncModule.PacketMinemaSync;
import info.ata4.minecraft.minema.client.modules.modifiers.TimerModifier;
import info.ata4.minecraft.minema.util.reflection.PrivateAccessor;
import net.minecraft.client.Minecraft;
//import info.ata4.minecraft.minema.client.modules.ShaderSync;
import net.minecraft.util.Timer;

/**
 * Extension of Minecraft's default timer for fixed framerate rendering.
 *
 * @author Nico Bergemann <barracuda415 at yahoo.de> / Shader part: daipenger
 */
public class FixedTimer extends Timer {

	private final double ticksPerSecond;
	private final double framesPerSecond;
	private double timerSpeed;

	private int held;
	private int frames;
	private boolean isFirstFrame;
	private boolean canOutput;
	private boolean canRecord;
	private boolean vr;
	private int vrCount;
	private int vrFace;
	
	private double ticks; // 0.9999999 = 1

	public FixedTimer(double tps, double fps, double speed) {
		super((float) tps);
		ticksPerSecond = tps;
		framesPerSecond = fps;
		timerSpeed = speed;

		held = Math.max(1, Minema.instance.getConfig().heldFrames.get());
		frames = 0;
		canOutput = true;
		canRecord = false;
		vr = Minema.instance.getConfig().vr.get();
		vrCount = 0;
		
		ticks = -1;
	}
	
	public boolean isFirstFrame() {
		return isFirstFrame;
	}

	public boolean canRecord() {
		return canRecord;
	} 
	
	public int getCubeFace() {
		return vrFace;
	}

	@Override
	public void updateTimer() {
		isFirstFrame = false;
		if (canOutput) { // last frame can output
			isFirstFrame = true;
			// First frame has a server tick
			if (ticks < 0) {
				ticks = 0;
				SyncModule.wakeServerTick();
				try {PacketMinemaSync.lock.await();} catch (InterruptedException e1) {}
			} else
				ticks += timerSpeed * (ticksPerSecond / framesPerSecond);
			elapsedTicks = (int) (float) ticks;
			ticks -= elapsedTicks;
			if (ticks < 1E-14)
				ticks = 0;
			renderPartialTicks = elapsedPartialTicks = (float) ticks;
			
//			SyncModule.wakeServerTick(elapsedTicks); // Before client handle network message.
		}
		
		canOutput = false;
		canRecord = false;
		frames += 1;
		vrCount %= 6;
		vrFace = vrCount;
		if (frames >= held) {
//			if (held > 1) {
//				ShaderSync.freeze(false);
//			}

			frames = 0;
			canRecord = true;
			canOutput = vr ? ++vrCount == 6 : true;
		} 
		if (frames > 1 && frames < held || vrFace > 0){
//			if (held > 1) {
//				ShaderSync.freeze(true);
//			}

			elapsedTicks = 0;
		}
        
	}

	public void setSpeed(double speed) {
		this.timerSpeed = speed;
	}

}
