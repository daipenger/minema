package info.ata4.minecraft.minema.client.modules.video.vr;

import org.lwjgl.opengl.GL13;

public enum CubeFace {

	FRONT(GL13.GL_TEXTURE_CUBE_MAP_NEGATIVE_Z, 0, 0),
	BACK(GL13.GL_TEXTURE_CUBE_MAP_POSITIVE_Z, 0, 180),
	LEFT(GL13.GL_TEXTURE_CUBE_MAP_POSITIVE_X, 0, -90),
	RIGHT(GL13.GL_TEXTURE_CUBE_MAP_NEGATIVE_X, 0, 90),
	TOP(GL13.GL_TEXTURE_CUBE_MAP_NEGATIVE_Y, -90, 180),
	BOTTOM(GL13.GL_TEXTURE_CUBE_MAP_POSITIVE_Y, 90, 180);
	
	public final int target;
	public final double rotateX;
	public final double rotateY;
	
	private CubeFace(int target, double rx, double ry) {
		this.target = target;
		this.rotateX = rx;
		this.rotateY = ry;
	}
	
}
