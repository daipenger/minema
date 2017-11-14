/*
 ** 2014 July 28
 **
 ** The author disclaims copyright to this source code.  In place of
 ** a legal notice, here is a blessing:
 **    May you do good and not evil.
 **    May you find forgiveness for yourself and forgive others.
 **    May you share freely, never taking more than you give.
 */
package info.ata4.minecraft.minema;

import net.minecraft.command.CommandBase;
import net.minecraft.command.CommandException;
import net.minecraft.command.ICommandSender;
import net.minecraft.command.WrongUsageException;
import net.minecraft.server.MinecraftServer;

/**
 *
 * @author Nico Bergemann <barracuda415 at yahoo.de>
 */
public class CommandMinema extends CommandBase {

	@Override
	public String getName() {
		return "minema";
	}

	@Override
	public String getUsage(ICommandSender sender) {
		return "commands.minema.usage";
	}

	@Override
	public void execute(MinecraftServer server, ICommandSender sender, String[] args) throws CommandException {
		if (args.length == 0) {
			throw new WrongUsageException(getUsage(sender));
		}

		String cmd = args[0];

		switch (cmd) {
		case "enable":
			CaptureSession.singleton.startCapture();
			break;
		case "disable":
			CaptureSession.singleton.stopCapture();
			break;
		default:
			throw new WrongUsageException(getUsage(sender));
		}
	}

	@Override
	public int getRequiredPermissionLevel() {
		return 0;
	}

}
