package info.ata4.minecraft.minema;

import net.minecraft.client.Minecraft;
import net.minecraft.util.text.TextComponentString;
import net.minecraft.util.text.TextFormatting;

public class Utils {

	public static void print(final String msg, final TextFormatting format) {
		final TextComponentString text = new TextComponentString(msg == null ? "null" : msg);
		text.getStyle().setColor(format);
		Minecraft.getMinecraft().player.sendMessage(text);
	}

	public static void printError(Throwable throwable) {
		print(throwable.getClass().getName(), TextFormatting.RED);
		print(throwable.getMessage(), TextFormatting.RED);
		Throwable cause = throwable.getCause();
		if (cause != null) {
			print("Cause:", TextFormatting.RED);
			print(cause.getClass().getName(), TextFormatting.RED);
			print(cause.getMessage(), TextFormatting.RED);
		}
		throwable.printStackTrace();
		print("See log for full stacktrace", TextFormatting.RED);
	}

}
