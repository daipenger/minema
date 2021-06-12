package info.ata4.minecraft.minema;

import info.ata4.minecraft.minema.client.engine.FixedTimer;
import info.ata4.minecraft.minema.client.modules.modifiers.TimerModifier;
import info.ata4.minecraft.minema.client.modules.tracker.BaseTracker;
import info.ata4.minecraft.minema.client.util.MinemaException;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import java.io.File;

/**
 * Minema API which can be used through reflection or by adding the
 * mod to dependency list
 */
@SideOnly(Side.CLIENT)
public class MinemaAPI {

	/**
	 * Get Minema's version
	 */
	public static String getVersion() {
		return Minema.VERSION;
	}

	/**
	 * Is Minema currently recording
	 */
	public static boolean isRecording() {
		return CaptureSession.singleton.isEnabled();
	}

	/**
	 * Get file to the capture folder
	 */
	public static File getCapturePath() {
		return new File(Minema.instance.getConfig().capturePath.get());
	}

	/**
	 * Set engine speed
	 *
	 * This could be used for keyframing engine speed for timelapses
	 * and other stuff
	 */
	public static void setEngineSpeed(float speed) {
		FixedTimer timer = TimerModifier.getTimer();

		if (timer != null) {
			timer.setSpeed(speed);
		}
	}

	/**
	 * Toggle Minema's recording
	 */
	public static boolean toggleRecording(boolean start) throws Exception {
		if (start == isRecording()) {
			return false;
		}

		if (start) {
			CaptureSession.singleton.start();
		} else {
			CaptureSession.singleton.stopCapture();
		}

		return true;
	}

	public static String getMessage(Exception e) {
		if (e.getCause() instanceof MinemaException) {
			 return e.getCause().getMessage();
		}

		return e.getMessage();
	}
	
	/**
	 * Track coordinate information using current ModelViewMatrix
	 * @param name The name of tracker, must not be empty
	 */
	public static void doTrack(String name) {
		if (name == null || name.isEmpty())
			return;
		BaseTracker.doTrack(name);
	}

}