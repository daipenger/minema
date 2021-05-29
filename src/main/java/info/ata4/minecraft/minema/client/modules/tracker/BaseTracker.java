package info.ata4.minecraft.minema.client.modules.tracker;

import java.util.ArrayList;

import info.ata4.minecraft.minema.MinemaTrackerAPI;
import info.ata4.minecraft.minema.client.modules.CaptureModule;

public abstract class BaseTracker extends CaptureModule {
	
	private static ArrayList<BaseTracker> trackers = new ArrayList<>();

	public static void doTrack(String name) {
		for (BaseTracker tracker : trackers)
			if (tracker.isEnabled())
				tracker.doTrack(name, false);
	}
	
	public BaseTracker() {
		trackers.add(this);
	}
	
	public abstract void doTrack(String name, boolean isCamera);
	
}
