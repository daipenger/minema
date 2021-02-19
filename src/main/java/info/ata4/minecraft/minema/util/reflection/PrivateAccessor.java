package info.ata4.minecraft.minema.util.reflection;

import java.lang.reflect.Field;
import java.util.Optional;
import net.minecraft.client.Minecraft;
import net.minecraft.util.Timer;

public final class PrivateAccessor {

	// These classes can already be loaded or are already loaded by the JVM at
	// this point (Minecraft core classes)
	private static Field Minecraft_timer = getAccessibleField(Minecraft.class, "field_71428_T", "timer");
	private static Field Timer_ticksPerSecond = getAccessibleField(Timer.class, "field_74282_a", "ticksPerSecond");

	// These classes might not be able to be loaded by the JVM at this point
	// (Mod classes of which the corresponding mod is not yet loaded)
	private static Optional<Field> Shaders_frameTimeCounter;
	private static void lateLoadFrameTimeCounterField() {
		if (Shaders_frameTimeCounter == null) {
			Shaders_frameTimeCounter = Optional.ofNullable(getAccessibleField("net.optifine.shaders.Shaders", "frameTimeCounter"));
		}
	}


	public static Timer getMinecraftTimer(Minecraft mc) {
		if (Minecraft_timer != null) {
			try {
				return (Timer) Minecraft_timer.get(mc);
			} catch (IllegalArgumentException | IllegalAccessException e) {
			}
		}

		throw new IllegalStateException("Cannot get timer");
	}

	public static void setMinecraftTimer(Minecraft mc, Timer timer) {
		if (Minecraft_timer != null) {
			try {
				Minecraft_timer.set(mc, timer);
				return;
			} catch (IllegalArgumentException | IllegalAccessException e) {
			}
		}

		throw new IllegalStateException("Cannot set timer");
	}

	public static float getTimerTicksPerSecond(Timer timer) {
		if (Timer_ticksPerSecond != null) {
			try {
				return (float) Timer_ticksPerSecond.get(timer);
			} catch (IllegalArgumentException | IllegalAccessException e) {
			}
		}

		// Minecraft default
		return 20;
	}

	public static float getFrameTimeCounter() {
		lateLoadFrameTimeCounterField();

		if (Shaders_frameTimeCounter.isPresent()) {
			try {
				// this field is static, just using null as the object
				return Shaders_frameTimeCounter.get().getFloat(null);
			} catch (IllegalArgumentException | IllegalAccessException e) {
			}
		}

		// just a default
		return 0;
	}

	public static void setFrameTimeCounter(float frameTimerCounter) {
		lateLoadFrameTimeCounterField();

		if (Shaders_frameTimeCounter.isPresent()) {
			try {
				// this field is static, just using null as the object
				Shaders_frameTimeCounter.get().setFloat(null, frameTimerCounter);
			} catch (IllegalArgumentException | IllegalAccessException e) {
			}
		}
	}

	/*
	 * Utility methods
	 */

	private static Field getAccessibleField(Class<?> clazz, String... names) {
		for (String name : names) {
			try {
				Field field = clazz.getDeclaredField(name);
				field.setAccessible(true);
				return field;
			} catch (NoSuchFieldException | SecurityException e) {
			}
		}

		return null;
	}

	private static Field getAccessibleField(String clazz, String... names) {
		try {
			return getAccessibleField(Class.forName(clazz), names);
		} catch (ClassNotFoundException e) {
			return null;
		}
	}

}
