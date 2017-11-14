package info.ata4.minecraft.minema.client.event;

import java.util.ArrayList;

/**
 * EventBus mechanism for a global observer pattern
 * <p>
 * Use the static EventBus instances declared by this class!
 *
 * @param <X>
 *            Generic parameter specifying the type of event
 */
public class MinemaEventbus<X> {

	public static final MinemaEventbus<MidRenderEvent> midRenderBUS = new MinemaEventbus<>();
	public static final MinemaEventbus<EndRenderEvent> endRenderBUS = new MinemaEventbus<>();

	private final ArrayList<IEventListener<X>> listeners;

	private MinemaEventbus() {
		this.listeners = new ArrayList<>();
	}

	/**
	 * @param listener
	 *            A listener now getting all events thrown by anyone calling
	 *            {@link MinemaEventbus#throwEvent(Object)} on this EventBus
	 *            instance
	 */
	public void registerListener(final IEventListener<X> listener) {
		this.listeners.add(listener);
	}

	/**
	 * @param event
	 *            Throw in an event to be listened by all listeners of this EventBus
	 *            instance
	 * @throws Exception
	 */
	public void throwEvent(final X event) throws Exception {
		for (final IEventListener<X> listener : this.listeners) {
			listener.onEvent(event);
		}
	}

	/**
	 * Dereferences all registered listeners in all buses
	 */
	public static void reset() {
		midRenderBUS.listeners.clear();
		endRenderBUS.listeners.clear();
	}

}
