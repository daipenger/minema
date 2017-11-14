package info.ata4.minecraft.minema.client.modules;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import net.minecraft.client.Minecraft;

public abstract class CaptureModule {

	protected static final Minecraft MC = Minecraft.getMinecraft();
	protected static final Logger L = LogManager.getLogger();
	private boolean enabled;

	public String getName() {
		return getClass().getSimpleName();
	}

	public synchronized final boolean isEnabled() {
		return enabled;
	}

	/**
	 * Enables this module if the current configuration says so
	 * 
	 * @throws Exception
	 */
	public synchronized final void enable() throws Exception {
		if (enabled) {
			return;
		}

		if (checkEnable()) {

			enabled = true;
			L.info("Enabling " + getName());
			try {
				doEnable();
			} catch (Exception e) {
				throw new Exception("Cannot enable module", e);
			}

		}
	}

	/**
	 * Disables this module if it was active. Even though it might throw an
	 * exception this module must recover into a state that makes it reusable for
	 * enabling again as if it was freshly instantiated.
	 * 
	 * @throws Exception
	 */
	public synchronized final void disable() throws Exception {
		if (!enabled) {
			return;
		}

		L.info("Disabling " + getName());
		try {
			doDisable();
		} catch (Exception e) {
			throw new Exception("Cannot disable module", e);
		}

		enabled = false;
	}

	protected abstract void doEnable() throws Exception;

	/**
	 * @return True if this module should be enabled given the current configuration
	 */
	protected abstract boolean checkEnable();

	protected abstract void doDisable() throws Exception;
}
