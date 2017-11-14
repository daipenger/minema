package info.ata4.minecraft.minema.client.modules.video.export;

import java.nio.ByteBuffer;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import info.ata4.minecraft.minema.CaptureSession;

public abstract class FrameExporter {

	protected static final Logger L = LogManager.getLogger();

	protected String movieName;
	protected int width;
	protected int height;
	protected ExecutorService exportService;
	protected Future<?> exportFuture;

	public FrameExporter() {
		exportService = Executors.newSingleThreadExecutor();
	}

	public void enable(String movieName, int width, int height) throws Exception {
		this.movieName = movieName;
		this.width = width;
		this.height = height;
	}

	public void destroy() throws Exception {
		exportService.shutdown();

		try {
			if (!exportService.awaitTermination(3, TimeUnit.SECONDS)) {
				L.warn("Frame export service termination timeout");
				exportService.shutdownNow();
			}
		} catch (InterruptedException ex) {
			handleWarning(ex, "Frame export service termination interrupted");
		}
	}

	public final void waitForLastExport() throws Exception {
		// wait for the previous task to complete before sending the next one
		try {
			if (exportFuture != null) {
				exportFuture.get();
			}
		} catch (InterruptedException ex) {
			// catch uncritical interruption exception
			handleWarning(ex, "Frame export task interrupted");
		}
	}

	public final void exportFrame(ByteBuffer buffer) throws Exception {
		// export frame in the background so that the next frame can be
		// rendered in the meantime
		exportFuture = exportService.submit(() -> {
			try {
				doExportFrame(buffer);
			} catch (Exception ex) {
				throw new RuntimeException("Can't export frame " + CaptureSession.singleton.getTime().getNumFrames(),
						ex);
			}
		});
	}

	protected abstract void doExportFrame(ByteBuffer buffer) throws Exception;

	protected void handleWarning(Throwable t, String message, Object... args) {
		L.warn(String.format(message, args), t);
	}

}
