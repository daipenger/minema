package info.ata4.minecraft.minema.client.modules.video.vr;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.RandomAccessFile;
import java.util.Arrays;

public class Mp4SphericalInjector {

	private static final byte[] SPHERICAL;
	
	static {
		byte[] b;
		try (InputStream in = Mp4SphericalInjector.class.getResourceAsStream("/assets/minema/vr/spherical.dat")) {
			b = new byte[in.available()];
			in.read(b);
		} catch (IOException e) {
			b = new byte[0];
		}
		SPHERICAL = b;
	}
	
	private static long readUIntBE(RandomAccessFile fp) throws IOException {
		byte[] b = new byte[4];
		fp.read(b);
		return (b[0] & 0xFFL) << 24 | (b[1] & 0xFFL) << 16 | (b[2] & 0xFFL) << 8 | (b[3] & 0xFFL);
	}
	
	private static void writeUIntBE(RandomAccessFile fp, long value) throws IOException {
		fp.writeByte((int) value >> 24);
		fp.writeByte((int) value >> 16);
		fp.writeByte((int) value >> 8);
		fp.writeByte((int) value);
	}
	
	private static String readBoxTag(RandomAccessFile fp) throws IOException {
		byte[] b = new byte[4];
		fp.read(b);
		return new String(b);
	}
	
	private static long findBox(RandomAccessFile fp, String box) throws IOException, InjectFailedException {
		while (fp.getFilePointer() < fp.length()) {
			long size = readUIntBE(fp);
			String tag = readBoxTag(fp);
			if (tag.equals(box))
				return fp.getFilePointer() - 8;
			fp.seek(fp.getFilePointer() + size - 8);
		}
		throw new InjectFailedException("No such Box: " + box);
	}
	
	private static void insertData(RandomAccessFile fp, byte[] bytes) throws IOException {
		byte[] flipA = Arrays.copyOf(bytes, bytes.length);
		byte[] flipB = new byte[flipA.length];
		int writeBytes = bytes.length;
		while (writeBytes > 0) {
			int readBytes = fp.read(flipB);
			if (readBytes > 0)
				fp.seek(fp.getFilePointer() - readBytes);
			fp.write(flipA, 0, writeBytes);
			byte[] temp = flipA;
			flipA = flipB;
			flipB = temp;
			writeBytes = readBytes;
		}
	}
	
	public static void inject(File in) throws InjectFailedException {
		try (RandomAccessFile fp = new RandomAccessFile(in, "rw")) {
			long moov = findBox(fp, "moov");
			long trak = findBox(fp, "trak");
			fp.seek(trak);
			long size = readUIntBE(fp);
			long insertPos = trak + size;
			
			fp.seek(moov);
			size = readUIntBE(fp);
			size += SPHERICAL.length;
			fp.seek(moov);
			writeUIntBE(fp, size);
			
			fp.seek(trak);
			size = readUIntBE(fp);
			size += SPHERICAL.length;
			fp.seek(trak);
			writeUIntBE(fp, size);
			
			fp.seek(insertPos);
			insertData(fp, SPHERICAL);
		} catch (FileNotFoundException e) {
			throw new InjectFailedException("File Not Found.", e);
		} catch (IOException e) {
			throw new InjectFailedException("I/O Error.", e);
		}
	}
	
	public static class InjectFailedException extends Exception {
		
		public InjectFailedException(String msg) {
			super(msg);
		}

		public InjectFailedException(String msg, Throwable cause) {
			super(msg, cause);
		}
		
	}

}
