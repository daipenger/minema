package info.ata4.minecraft.minema.util.reflection;

import net.optifine.shaders.Shaders;

public class ShadersHelper {

	private static final boolean haveShaders;
	
	static {
		boolean shaders = false;
		try {
			Class.forName("Config");
			Class.forName("net.optifine.shaders.Shaders");
			shaders = true;
		} catch (ClassNotFoundException | SecurityException | IllegalArgumentException e) {}
		haveShaders = shaders;
	}
	
	public static boolean usingShaders() {
		return haveShaders ? Shaders.shaderPackLoaded : false;
	}

}
