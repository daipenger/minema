package info.ata4.minecraft.minema.client.event;

import info.ata4.minecraft.minema.CaptureSession;

public abstract class CaptureEvent {

	public final CaptureSession session;

	public CaptureEvent(CaptureSession session) {
		this.session = session;
	}

}
