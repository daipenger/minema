package info.ata4.minecraft.minema.client.modules.tracker;

import java.util.ArrayList;

import info.ata4.minecraft.minema.client.modules.CaptureModule;

public abstract class BaseTracker extends CaptureModule {
	
	private static ArrayList<BaseTracker> trackers = new ArrayList<>();

	public static void doTrack(String name) {
		for (BaseTracker tracker : trackers)
			if (tracker.isEnabled())
				tracker.track(name);
	}
	
	public BaseTracker() {
		trackers.add(this);
	}
	
	public abstract void track(String name);
	
}
