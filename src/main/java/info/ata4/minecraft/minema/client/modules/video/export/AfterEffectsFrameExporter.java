package info.ata4.minecraft.minema.client.modules.video.export;

import java.io.File;
import java.io.FileOutputStream;
import java.io.PrintStream;
import java.nio.DoubleBuffer;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;

import javax.vecmath.Matrix4d;
import javax.vecmath.Vector3d;
import javax.vecmath.Vector4d;
import org.lwjgl.BufferUtils;
import org.lwjgl.opengl.GL11;

import info.ata4.minecraft.minema.CaptureSession;
import info.ata4.minecraft.minema.Minema;
import info.ata4.minecraft.minema.client.config.MinemaConfig;
import info.ata4.minecraft.minema.client.event.MinemaEventbus;
import info.ata4.minecraft.minema.client.modules.CaptureModule;
import info.ata4.minecraft.minema.client.modules.modifiers.TimerModifier;
import net.minecraft.client.Minecraft;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraftforge.client.event.EntityViewRenderEvent;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

public class AfterEffectsFrameExporter extends CaptureModule {
	
	private static final DoubleBuffer buffer = BufferUtils.createDoubleBuffer(16);
	private static final double[] floats = new double[16];
	private static final Matrix4d modelview = new Matrix4d();
	private static final Matrix4d projection = new Matrix4d();

	private static void updateMVP() {
		buffer.clear();
		GL11.glGetDouble(GL11.GL_MODELVIEW_MATRIX, buffer);
		buffer.get(floats);
		modelview.set(floats);
		modelview.transpose();
		buffer.clear();
		GL11.glGetDouble(GL11.GL_PROJECTION_MATRIX, buffer);
		buffer.get(floats);
		projection.set(floats);
		projection.transpose();
	}

	private String filename = null;
	
	private PrintStream fw;
	private ArrayList<Double> zoom;
	private ArrayList<Vector3d> orientation;
	private ArrayList<Vector3d> position;
	
	private int height;
	
	@Override
	protected void doEnable() throws Exception {
		MinemaEventbus.cameraBUS.registerListener(e -> this.afterCamera());
		
		MinemaConfig cfg = Minema.instance.getConfig();
		String filename = this.filename == null || this.filename.isEmpty() ? new SimpleDateFormat("yyyy-MM-dd_HH.mm.ss").format(new Date()) : this.filename;
		filename += ".keyframes.txt";
		fw = new PrintStream(new FileOutputStream(new File(CaptureSession.singleton.getCaptureDir().toFile(), filename)), true, "ASCII");
		fw.println("Adobe After Effects 8.0 Keyframe Data");
		fw.println(String.format("\tUnits Per Second\t%.2f", cfg.frameRate.get().doubleValue()));
		fw.println(String.format("\tSource Width\t%d", cfg.getFrameWidth()));
		fw.println(String.format("\tSource Height\t%d", height = cfg.getFrameHeight()));
		fw.println("\tSource Pixel Aspect Ratio\t1");
		fw.println("\tComp Pixel Aspect Ratio\t1");
		zoom = new ArrayList<>();
		orientation = new ArrayList<>();
		position = new ArrayList<>();
	}

	@Override
	protected boolean checkEnable() {
		return Minema.instance.getConfig().exportAECamera.get();
	}

	@Override
	protected void doDisable() throws Exception {
		fw.println("Camera Options\tZoom");
		fw.println("\tFrame");
		for (int i = 0; i < zoom.size(); i++)
			fw.println(String.format("\t%d\t%.3f", i, zoom.get(i)));
		
		fw.println("Transform\tOrientation");
		fw.println("\tFrame");
		for (int i = 0; i < orientation.size(); i++)
			fw.println(String.format("\t%d\t%.3f\t%.3f\t%.3f", i, orientation.get(i).x, orientation.get(i).y, orientation.get(i).z));
		
		fw.println("Transform\tPosition");
		fw.println("\tFrame");
		for (int i = 0; i < position.size(); i++)
			fw.println(String.format("\t%d\t%.3f\t%.3f\t%.3f", i, position.get(i).x, position.get(i).y, position.get(i).z));
		
		fw.println("End of Keyframe Data");
		fw.close();
		fw = null;
		zoom = null;
		orientation = null;
		position = null;
		filename = null;
	}
	
	public void afterCamera() {
		if (!this.isEnabled() || !TimerModifier.canRecord())
			return;
		
		updateMVP();
		
		zoom.add(height / 2.0 * projection.m11);
		
		Matrix4d mat = modelview;
		Matrix4d trans = new Matrix4d();
		
		Minecraft mc = Minecraft.getMinecraft();
		
		Entity entity = mc.getRenderViewEntity();

		double x = entity.lastTickPosX + (entity.posX - entity.lastTickPosX) * mc.getRenderPartialTicks();
		double y = entity.lastTickPosY + (entity.posY - entity.lastTickPosY) * mc.getRenderPartialTicks();
		double z = entity.lastTickPosZ + (entity.posZ - entity.lastTickPosZ) * mc.getRenderPartialTicks();

		trans.setIdentity();
		trans.setTranslation(new Vector3d(-x, -y, -z));
		mat.mul(trans);
		mat.invert();
		
		position.add(new Vector3d(mat.m03, mat.m13, mat.m23));
		
		mat.m03 = mat.m13 = mat.m23 = 0;
		trans.rotY(Math.PI);
		mat.mul(trans);
		mat.transpose();
		
		double Rx;
		double Ry;
		double Rz;
		
		Matrix4d rot = new Matrix4d();
		Vector4d test = new Vector4d(1, 0, 0, 1);
		Vector4d result = new Vector4d();
		mat.transform(test, result);
		if (Math.abs(result.y) > 1E-7 || Math.abs(result.x) > 1E-7) {
			double radian;
			radian = Math.atan2(result.y, result.x);
			Rz = Math.toDegrees(radian);
			rot.rotZ(-radian);
			rot.mul(mat);
			mat.set(rot);
			mat.transform(test, result);
			
			radian = Math.atan2(-result.z, result.x);
			Ry = Math.toDegrees(radian);
			rot.rotY(-radian);
			rot.mul(mat);
			mat.set(rot);
			test.x = 0;
			test.y = 1;
			mat.transform(test, result);
			
			radian = Math.atan2(result.z, result.y);
			Rx = Math.toDegrees(radian);
		} else {
			if (result.length() > 1E-7) {
				Rz = 0;
				double radianX;
				double sign = -Math.signum(result.z);
				Ry = sign * 90f;
				test.x = 0;
				test.z = sign;
				mat.transform(test, result);
				radianX = sign * -Math.atan2(result.y, result.x);
				Rx = Math.toDegrees(radianX);
			} else {
				Rx = Ry = Rz = 0;
			}
		}
		
		orientation.add(new Vector3d(-Rx, -Ry, 180 - Rz));
	}

	public void setName(String filename) {
		this.filename = filename;
	}

}
