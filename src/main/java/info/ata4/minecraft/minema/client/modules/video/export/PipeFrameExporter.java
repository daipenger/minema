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

import info.ata4.minecraft.minema.CaptureSession;
import info.ata4.minecraft.minema.Minema;
import info.ata4.minecraft.minema.client.config.MinemaConfig;
import info.ata4.minecraft.minema.client.config.enums.MotionBlur;
import info.ata4.minecraft.minema.client.modules.modifiers.TimerModifier;
import info.ata4.minecraft.minema.client.util.CaptureTime;
import info.ata4.minecraft.minema.client.util.MinemaException;
import net.minecraft.client.resources.I18n;
import net.minecraft.util.Util;
import org.apache.commons.lang3.StringUtils;

import java.io.File;
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

/**
 *
 * @author Nico Bergemann <barracuda415 at yahoo.de>
 */
public class PipeFrameExporter extends FrameExporter {

	private Process proc;
	private WritableByteChannel pipe;
	private boolean isColor;

	public PipeFrameExporter(boolean isColor) {
		this.isColor = isColor;
	}

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
		String ffmpeg = this.findFFMPEG(cfg.videoEncoderPath.get());

		// Add human readable error messages
		if (!new File(ffmpeg).isFile()) {
			throw new MinemaException(I18n.format("minema.error.ffmpeg_not_exists", ffmpeg));
		}

		String params = this.isColor && cfg.useAlpha.get() ? cfg.videoEncoderParamsAlpha.get() : cfg.videoEncoderParams.get();
		params = params.replace("%WIDTH%", String.valueOf(width));
		params = params.replace("%HEIGHT%", String.valueOf(height));
		params = params.replace("%FPS%", String.valueOf(cfg.getFrameRate()));
		params = params.replace("%NAME%", movieName);
		String defvf = "vflip";
		if (cfg.motionBlurLevel.get() != MotionBlur.DISABLE && !params.contains("%DEFVF%"))
			throw new MinemaException(I18n.format("minema.error.require_defvf"));
		for (int i = 0; i < cfg.motionBlurLevel.get().getExp(cfg.frameRate.get()); i++)
			defvf += ",tblend=all_mode=average,framestep=2";
		params = params.replace("%DEFVF%", defvf);

		List<String> cmds = new ArrayList<>();
		cmds.add(ffmpeg);
		cmds.addAll(Arrays.asList(StringUtils.split(params, ' ')));

		// build encoder process and redirect output
		ProcessBuilder pb = new ProcessBuilder(cmds);
		pb.directory(path.toFile());
		pb.redirectErrorStream(true);

		if (cfg.enableEncoderLogging.get()) {
			pb.redirectOutput(path.resolve(movieName.concat(".log")).toFile());
		} else {
			// Apparently not redirecting the output to a file can lead to a
			// crash of the game, but like not the one with crash log and stuff,
			// and not even the native one that yields one of those hs_err_PID.log
			// files, but like total unlogged crash...
			//
			// So yeah, I guess I'll just redirect it to a dummy file
			cfg.dummyLog.delete();
			pb.redirectOutput(cfg.dummyLog);
		}

		try {
			proc = pb.start();
		} catch (Exception e) {
			throw new MinemaException(I18n.format("minema.error.ffmpeg_error", path.toFile().getAbsolutePath(), ffmpeg), e);
		}

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

	/**
	 * People usually are not bright enough, even though everything is stated
	 * in the tutorial, they still manage to specify either wrong path to ffmpeg, or
	 * they specify the path to the folder...
	 *
	 * This little method should simplify their lives!
	 */
	private String findFFMPEG(String path) {
		File file = new File(path);
		boolean isWin = Util.getOSType() == Util.EnumOS.WINDOWS;

		if (file.isDirectory()) {
			String subpath = isWin ? "ffmpeg.exe" : "ffmpeg";
			File bin = new File(file, subpath);

			if (bin.isFile()) {
				return bin.getAbsolutePath();
			}

			bin = new File(file, "bin" + (isWin ? "\\" : "/") + subpath);

			if (bin.isFile()) {
				return bin.getAbsolutePath();
			}
		} else if (isWin && !file.exists()) {
			File exe = new File(path + ".exe");

			if (exe.exists()) {
				return exe.getAbsolutePath();
			}
		}

		return path;
	}

	@Override
	public void destroy() throws Exception {
		super.destroy();

		try {
			if (pipe != null && pipe.isOpen()) {
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
