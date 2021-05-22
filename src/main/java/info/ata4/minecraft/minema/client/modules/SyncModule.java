package info.ata4.minecraft.minema.client.modules;

import java.io.IOException;
import java.util.Queue;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.FutureTask;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import info.ata4.minecraft.minema.Minema;
import info.ata4.minecraft.minema.client.config.MinemaConfig;
import info.ata4.minecraft.minema.client.modules.modifiers.TimerModifier;
import info.ata4.minecraft.minema.util.reflection.PrivateAccessor;
import net.minecraft.network.INetHandler;
import net.minecraft.network.Packet;
import net.minecraft.network.PacketBuffer;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.Util;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.common.FMLCommonHandler;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent.ClientTickEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent.Phase;

// A better synchronize module insteads of ShaderSync & TickSynchronizer (Maybe)
public class SyncModule extends CaptureModule {

	private static final Logger NetworkLogger = LogManager.getLogger("MinemaNetworkSync");
	
	private static SyncModule instance = null;
	private static Queue<FutureTask<?>>  queue = null;
	
	private static float frameTime;
	private static float frameTimeStep;
//	private static long networkDelay;
	
	// Called by ASM from Minecraft
	public static int minTicks(int ten, int elapsedTicks) {
		return instance != null && instance.isEnabled() ? elapsedTicks : Math.min(ten, elapsedTicks);
	}
	
	// Called by ASM from EntityRenderer
	public static void doFrameTimeSync() {
		if (instance != null && instance.isEnabled()) {
			if (TimerModifier.isFirstFrame()) {
				frameTime += frameTimeStep;
				frameTime %= 3600.0;
			}
			PrivateAccessor.setFrameTimeCounter(frameTime);
		}
	}

	// Called by ASM from EntityTrackerEntry & NetHandlerPlayClient
	public static int getUpdateFrequency(int origin) {
		return instance != null && instance.isEnabled() && origin == 3 ? 1 : origin;
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
					if (PacketMinemaSync.lock != null)
						try {PacketMinemaSync.lock.await();} catch (InterruptedException e1) {}
					for (; syncTicks > 0; syncTicks--) {
						server.tick();
					}
					server.getPlayerList().getPlayers().get(0).connection.sendPacket(new PacketMinemaSync());
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
	
	public static void wakeServerTick() {
		if (instance == null || !instance.isEnabled())
			return;
		try {
			syncTicks += 1;
			condServer.signalAll();
			condClient.await();
		} catch (Exception ex) {
			L.error("Client tick sync failed: {}", ex.getMessage());
			L.catching(Level.DEBUG, ex);
		}
		
		// Wait for network message
//		try {
//			Thread.sleep(networkDelay);
//		} catch (InterruptedException e) {
//		}
	}
	
	@SubscribeEvent
	public void onClientTick(ClientTickEvent e) {
		if (e.phase == Phase.START) {
			MC.player.connection.sendPacket(new PacketMinemaSync());
			wakeServerTick();
			if (queue  == null)
				queue = PrivateAccessor.getScheduledTasks(MC);
			try {PacketMinemaSync.lock.await();} catch (InterruptedException e1) {}
			
			while (!queue.isEmpty())
				Util.runTask(queue.poll(), NetworkLogger); // Don't postpone network events to the next frame
		}
	}
	
	@Override
	protected void doEnable() throws Exception {
		MinemaConfig cfg = Minema.instance.getConfig();
		float fps = (float) cfg.getFrameRate();
		float speed = cfg.engineSpeed.get().floatValue();
		
		frameTime = PrivateAccessor.getFrameTimeCounter();
		frameTimeStep = speed / fps;
//		networkDelay = cfg.networkDelay.get();

		PacketMinemaSync.lock = null;
		MinecraftForge.EVENT_BUS.register(this);
		instance = this;
		syncTicks = 0;
		lock.lock();
	}

	@Override
	protected boolean checkEnable() {
		return Minema.instance.getConfig().syncEngine.get() & MC.isSingleplayer();
	}

	@Override
	protected void doDisable() throws Exception {
		MinecraftForge.EVENT_BUS.unregister(this);
		instance = null;
		condServer.signalAll();
		lock.unlock();
	}
	
	public static class PacketMinemaSync implements Packet<INetHandler> {

		public static CountDownLatch lock;
		
		public PacketMinemaSync() {
			lock = new CountDownLatch(1);
		}
		
		@Override
		public void readPacketData(PacketBuffer buf) throws IOException {}

		@Override
		public void writePacketData(PacketBuffer buf) throws IOException {}

		@Override
		public void processPacket(INetHandler handler) {
			lock.countDown();
		}

	}
	
}
