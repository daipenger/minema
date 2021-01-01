package info.ata4.minecraft.minema.client.modules;

import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;

import info.ata4.minecraft.minema.Minema;
import info.ata4.minecraft.minema.client.config.MinemaConfig;
import info.ata4.minecraft.minema.util.reflection.PrivateAccessor;
import net.minecraft.server.MinecraftServer;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.common.FMLCommonHandler;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent.ClientTickEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent.Phase;

// A better synchronize module insteads of ShaderSync & TickSynchronizer (Maybe)
public class SyncModule extends CaptureModule {

	private static SyncModule instance = null;
	
	private static float frameTime;
	private static float frameTimeStep;
	
	// Called by ASM from EntityRenderer
	public static void doFrameTimeSync() {
		if (instance != null && instance.isEnabled()) {
			frameTime += frameTimeStep;
			frameTime %= 3600.0F;
			PrivateAccessor.setFrameTimeCounter(frameTime);
		}
	}
	
	private static int syncTicks = 0; // 0 or 1
	private static Lock lock = new ReentrantLock();
	private static Condition condServer = lock.newCondition();
	private static Condition condClient = lock.newCondition();
	
	// Called by ASM from MinecraftServer
	public static long doServerTickSync(long currentTime) {
		try {
			if (instance != null && instance.isEnabled()) {
				MinecraftServer server = FMLCommonHandler.instance().getMinecraftServerInstance();
				lock.lock();
				while (instance != null && instance.isEnabled()) {
					for (; syncTicks > 0; syncTicks--) {
						server.tick();
					}
					condClient.signalAll();
					condServer.await();
				}
				lock.unlock();
				return MinecraftServer.getCurrentTimeMillis();
			}
		} catch (Exception ex) {
			L.error("Server tick sync failed: {}", ex.getMessage());
			L.catching(Level.DEBUG, ex);
		}
		return currentTime;
	}
	
	@SubscribeEvent
	public void onClientTick(ClientTickEvent evt) {
		if (!isEnabled() || evt.phase != Phase.START) {
			return;
		}
		try {
			syncTicks++;
			condServer.signalAll();
			condClient.await();
		} catch (Exception ex) {
			L.error("Client tick sync failed: {}", ex.getMessage());
			L.catching(Level.DEBUG, ex);
		}
	}
	
	@Override
	protected void doEnable() throws Exception {
		MinemaConfig cfg = Minema.instance.getConfig();
		float fps = cfg.frameRate.get().floatValue();
		float speed = cfg.engineSpeed.get().floatValue();
		frameTime = PrivateAccessor.getFrameTimeCounter();
		frameTimeStep = speed / fps;
		
		instance = this;
		lock.lock();
		MinecraftForge.EVENT_BUS.register(this);
	}

	@Override
	protected boolean checkEnable() {
		return Minema.instance.getConfig().syncEngine.get() & MC.isSingleplayer();
	}

	@Override
	protected void doDisable() throws Exception {
		instance = null;
		MinecraftForge.EVENT_BUS.unregister(this);
		condServer.signalAll();
		lock.unlock();
	}
	
}
