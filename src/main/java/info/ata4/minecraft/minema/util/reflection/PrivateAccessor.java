package info.ata4.minecraft.minema.util.reflection;

import java.lang.reflect.Field;
import java.util.Optional;
import java.util.Queue;
import java.util.Set;
import java.util.concurrent.FutureTask;

import it.unimi.dsi.fastutil.longs.Long2ObjectMap;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ChunkProviderClient;
import net.minecraft.client.renderer.EntityRenderer;
import net.minecraft.client.renderer.RenderGlobal;
import net.minecraft.client.renderer.chunk.RenderChunk;
import net.minecraft.client.settings.GameSettings;
import net.minecraft.util.Timer;

public final class PrivateAccessor {

	// These classes can already be loaded or are already loaded by the JVM at
	// this point (Minecraft core classes)
	private static Field Minecraft_timer = getAccessibleField(Minecraft.class, "field_71428_T", "timer");
	private static Field Timer_ticksPerSecond = getAccessibleField(Timer.class, "field_74282_a", "ticksPerSecond");
	private static Field Minecraft_scheduledTasks = getAccessibleField(Minecraft.class, "field_152351_aB", "scheduledTasks");
	private static Field ChunkProviderClient_loadedChunks = getAccessibleField(ChunkProviderClient.class, "field_73236_b", "loadedChunks");
	private static Field EntityRenderer_frameCount = getAccessibleField(EntityRenderer.class, "field_175084_ae", "frameCount");
//	private static Field RenderGlobal_chunksToUpdate = getAccessibleField(RenderGlobal.class, "field_175009_l", "chunksToUpdate");
	
	// These classes might not be able to be loaded by the JVM at this point
	// (Mod classes of which the corresponding mod is not yet loaded)
	private static Optional<Field> Shaders_frameTimeCounter;
	private static Optional<Field> ofLazyChunkLoading;
	private static Optional<Field> ofChunkUpdates;
	private static void lateLoadOptifineField() {
		if (Shaders_frameTimeCounter == null) {
			Shaders_frameTimeCounter = Optional.ofNullable(getAccessibleField("net.optifine.shaders.Shaders", "frameTimeCounter"));
		}
		if (ofLazyChunkLoading == null) {
			ofLazyChunkLoading = Optional.ofNullable(getAccessibleField(GameSettings.class, "ofLazyChunkLoading"));
		}
		if (ofChunkUpdates == null) {
			ofChunkUpdates = Optional.ofNullable(getAccessibleField(GameSettings.class, "ofChunkUpdates"));
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
	
	public static Queue<FutureTask<?>> getScheduledTasks(Minecraft mc) {
		if (Minecraft_scheduledTasks != null) {
			try {
				return (Queue<FutureTask<?>>) Minecraft_scheduledTasks.get(mc);
			} catch (IllegalArgumentException | IllegalAccessException e) {
			}
		}

		throw new IllegalStateException("Cannot get client task queue");
	}

	public static Long2ObjectMap getLoadedChunks(ChunkProviderClient provider) {
		if (provider != null)
			if (ChunkProviderClient_loadedChunks != null) {
				try {
					return (Long2ObjectMap) ChunkProviderClient_loadedChunks.get(provider);
				} catch (IllegalArgumentException | IllegalAccessException e) {
				}
			}

		return null;
	}
	
	public static int getAndAddFrameCount(EntityRenderer renderer) {
		if (EntityRenderer_frameCount != null) {
			try {
				int rtn = EntityRenderer_frameCount.getInt(renderer);
				EntityRenderer_frameCount.setInt(renderer, rtn + 1);
				return rtn;
			} catch (IllegalArgumentException | IllegalAccessException e) {
			}
		}

		return 0;
	}

//	public static Set<RenderChunk> getChunksToUpdate(RenderGlobal renderglobal) {
//		if (RenderGlobal_chunksToUpdate != null) {
//			try {
//				return (Set) RenderGlobal_chunksToUpdate.get(renderglobal);
//			} catch (IllegalArgumentException | IllegalAccessException e) {
//			}
//		}
//		
//		return null;
//	}

	public static float getFrameTimeCounter() {
		lateLoadOptifineField();

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
		lateLoadOptifineField();

		if (Shaders_frameTimeCounter.isPresent()) {
			try {
				// this field is static, just using null as the object
				Shaders_frameTimeCounter.get().setFloat(null, frameTimerCounter);
			} catch (IllegalArgumentException | IllegalAccessException e) {
			}
		}
	}

	public static boolean isLazyChunkLoading() {
		lateLoadOptifineField();

		if (ofLazyChunkLoading.isPresent()) {
			try {
				return ofLazyChunkLoading.get().getBoolean(Minecraft.getMinecraft().gameSettings);
			} catch (IllegalArgumentException | IllegalAccessException e) {
			}
		}

		// just a default
		return false;
	}

	public static void setLazyChunkLoading(boolean value) {
		lateLoadOptifineField();

		if (ofLazyChunkLoading.isPresent()) {
			try {
				ofLazyChunkLoading.get().setBoolean(Minecraft.getMinecraft().gameSettings, value);
			} catch (IllegalArgumentException | IllegalAccessException e) {
			}
		}
	}

	public static int getChunkUpdates() {
		lateLoadOptifineField();

		if (ofChunkUpdates.isPresent()) {
			try {
				return ofChunkUpdates.get().getInt(Minecraft.getMinecraft().gameSettings);
			} catch (IllegalArgumentException | IllegalAccessException e) {
			}
		}

		// just a default
		return 0;
	}

	public static void setChunkUpdates(int updates) {
		lateLoadOptifineField();

		if (ofChunkUpdates.isPresent()) {
			try {
				ofChunkUpdates.get().setInt(Minecraft.getMinecraft().gameSettings, updates);
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
