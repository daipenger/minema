/*
 ** 2014 July 29
 **
 ** The author disclaims copyright to this source code.  In place of
 ** a legal notice, here is a blessing:
 **    May you do good and not evil.
 **    May you find forgiveness for yourself and forgive others.
 **    May you share freely, never taking more than you give.
 */
package info.ata4.minecraft.minema.client.modules.video.export;

import java.io.FilterOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.lang.reflect.Field;
import java.nio.ByteBuffer;
import java.nio.channels.Channels;
import java.nio.channels.WritableByteChannel;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.TimeUnit;

import org.apache.commons.lang3.StringUtils;

import info.ata4.minecraft.minema.CaptureSession;
import info.ata4.minecraft.minema.Minema;
import info.ata4.minecraft.minema.client.config.MinemaConfig;

/**
 *
 * @author Nico Bergemann <barracuda415 at yahoo.de>
 */
public class PipeFrameExporter extends FrameExporter {

	private Process proc;
	private WritableByteChannel pipe;

	@Override
	protected void doExportFrame(ByteBuffer buffer) throws Exception {
		if (pipe.isOpen()) {
			pipe.write(buffer);
			buffer.rewind();
		}
	}

	@Override
	public void enable(String movieName, int width, int height) throws Exception {
		super.enable(movieName, width, height);

		MinemaConfig cfg = Minema.instance.getConfig();
		Path path = CaptureSession.singleton.getCaptureDir();

		String params = cfg.videoEncoderParams.get();
		params = params.replace("%WIDTH%", String.valueOf(width));
		params = params.replace("%HEIGHT%", String.valueOf(height));
		params = params.replace("%FPS%", String.valueOf(cfg.frameRate.get()));
		params = params.replace("%NAME%", movieName);

		List<String> cmds = new ArrayList<>();
		cmds.add(cfg.videoEncoderPath.get());
		cmds.addAll(Arrays.asList(StringUtils.split(params, ' ')));

		// build encoder process and redirect output
		ProcessBuilder pb = new ProcessBuilder(cmds);
		pb.directory(path.toFile());
		pb.redirectErrorStream(true);
		pb.redirectOutput(path.resolve(movieName.concat(".log")).toFile());
		proc = pb.start();

		// Java wraps the process output stream into a BufferedOutputStream,
		// but its little buffer is just slowing everything down with the
		// huge
		// amount of data we're dealing here, so unwrap it with this little
		// hack.
		OutputStream os = proc.getOutputStream();
		if (os instanceof FilterOutputStream) {
			Field outField = FilterOutputStream.class.getDeclaredField("out");
			outField.setAccessible(true);
			os = (OutputStream) outField.get(os);
		}

		pipe = Channels.newChannel(os);
	}

	@Override
	public void destroy() throws Exception {
		super.destroy();

		try {
			if (pipe.isOpen()) {
				pipe.close();
			}
		} catch (IOException ex) {
			handleWarning(ex, "Pipe not closed properly");
		}

		try {
			if (proc != null) {
				proc.waitFor(1, TimeUnit.MINUTES);
				proc.destroy();
			}
		} catch (InterruptedException ex) {
			handleWarning(ex, "Pipe program termination interrupted");
		}
	}

}
