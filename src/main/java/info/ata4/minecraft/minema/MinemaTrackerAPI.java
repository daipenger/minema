package info.ata4.minecraft.minema;

import info.ata4.minecraft.minema.client.modules.tracker.BaseTracker;

public class MinemaTrackerAPI {
	
	/**
	 * Track coordinate information using the current ModelViewMatrix
	 * @param name The name of tracker, must not empty
	 */
	public static void doTrack(String name) {
		if (name == null || name.isEmpty())
			return;
		BaseTracker.doTrack(name);
	}

}
