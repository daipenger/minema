package info.ata4.minecraft.minema.client.event;

import info.ata4.minecraft.minema.CaptureSession;
import info.ata4.minecraft.minema.shaderHook_coremod.ShaderHookInjector;

/**
 * Is posted when the render pipeline is right before clearing the depth buffer
 * for rendering hand, GUI and other stuff
 * <p>
 * See {@link ShaderHookInjector#transform(String, String, byte[])} for details
 */
public class MidRenderEvent extends CaptureEvent {

	public MidRenderEvent(CaptureSession session) {
		super(session);
	}

}
