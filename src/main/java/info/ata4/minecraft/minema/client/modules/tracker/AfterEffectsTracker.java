package info.ata4.minecraft.minema.client.modules.tracker;

import java.io.File;
import java.io.FileOutputStream;
import java.io.PrintStream;
import java.nio.DoubleBuffer;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map.Entry;

import javax.vecmath.Matrix4d;
import javax.vecmath.Vector3d;
import javax.vecmath.Vector4d;
import org.lwjgl.BufferUtils;
import org.lwjgl.opengl.GL11;

import info.ata4.minecraft.minema.CaptureSession;
import info.ata4.minecraft.minema.Minema;
import info.ata4.minecraft.minema.client.config.MinemaConfig;
import info.ata4.minecraft.minema.client.event.MinemaEventbus;
import info.ata4.minecraft.minema.client.modules.modifiers.TimerModifier;
import net.minecraft.client.Minecraft;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraftforge.client.event.EntityViewRenderEvent;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

public class AfterEffectsTracker extends BaseTracker {
	
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
	
	private Matrix4d camera = new Matrix4d();
	private int frame = -1;
	private boolean tracking = false;
	
	private HashMap<String, LinkedHashMap<Integer, Double>> zoomMap;
	private HashMap<String, LinkedHashMap<Integer, Vector3d>> orientationMap;
	private HashMap<String, LinkedHashMap<Integer, Vector3d>> positionMap;
	
	private double frameRate;
	private int width;
	private int height;
	
	@Override
	protected void doEnable() throws Exception {		
		MinemaConfig cfg = Minema.instance.getConfig();
		frameRate = cfg.getFrameRate();
		width = cfg.getFrameWidth();
		height = cfg.getFrameHeight();
		
		zoomMap = new HashMap<>();
		orientationMap = new HashMap<>();
		positionMap = new HashMap<>();
		
		frame = -1;
		tracking = false;
		
		MinemaEventbus.endRenderBUS.registerListener((e) -> onRenderEnd());
		MinemaEventbus.cameraBUS.registerListener((e) -> afterCamera());
	}

	@Override
	protected boolean checkEnable() {
		return Minema.instance.getConfig().exportAECamera.get() && !Minema.instance.getConfig().vr.get();
	}

	@Override
	protected void doDisable() throws Exception {
		for (String tracker : positionMap.keySet()) {
			String filename;
			if (tracker == null)
				filename = CaptureSession.singleton.getFilename() + ".keyframes.txt";
			else
				filename = CaptureSession.singleton.getFilename() + ".keyframes." + tracker + ".txt";
			
			LinkedHashMap<Integer, Double> zoom = zoomMap.get(tracker);
			LinkedHashMap<Integer, Vector3d> orientation = orientationMap.get(tracker);
			LinkedHashMap<Integer, Vector3d> position = positionMap.get(tracker);
			
			PrintStream fw = new PrintStream(new FileOutputStream(new File(CaptureSession.singleton.getCaptureDir().toFile(), filename)), true);
			fw.println("Adobe After Effects 8.0 Keyframe Data");
			fw.println(String.format("\tUnits Per Second\t%.2f", frameRate));
			fw.println(String.format("\tSource Width\t%d", width));
			fw.println(String.format("\tSource Height\t%d", height));
			fw.println("\tSource Pixel Aspect Ratio\t1");
			fw.println("\tComp Pixel Aspect Ratio\t1");
			
			if (zoom != null) {
				fw.println("Camera Options\tZoom");
				fw.println("\tFrame");
				for (Entry<Integer, Double> entry : zoom.entrySet())
					fw.println(String.format("\t%d\t%.3f", entry.getKey(), entry.getValue()));
			}
			
			fw.println("Transform\tOrientation");
			fw.println("\tFrame");
			for (Entry<Integer, Vector3d> entry : orientation.entrySet())
				fw.println(String.format("\t%d\t%.3f\t%.3f\t%.3f", entry.getKey(), entry.getValue().x, entry.getValue().y, entry.getValue().z));
			
			fw.println("Transform\tPosition");
			fw.println("\tFrame");
			for (Entry<Integer, Vector3d> entry : position.entrySet())
				fw.println(String.format("\t%d\t%.3f\t%.3f\t%.3f", entry.getKey(), entry.getValue().x, entry.getValue().y, entry.getValue().z));
			
			fw.println("End of Keyframe Data");
			fw.close();
		}

		zoomMap = null;
		orientationMap = null;
		positionMap = null;
	}
	
	@Override
	public void track(String name) {
		if (!this.isEnabled() || !TimerModifier.canRecord() || !tracking)
			return;
		
		updateMVP();
		
		boolean isCamera = name == null;
		
		if (isCamera)
			camera.set(modelview);

		if (positionMap.get(name) == null)
			positionMap.put(name, new LinkedHashMap<>());
		LinkedHashMap<Integer, Vector3d> position = positionMap.get(name);
		
		if (orientationMap.get(name) == null)
			orientationMap.put(name, new LinkedHashMap<>());
		LinkedHashMap<Integer, Vector3d> orientation = orientationMap.get(name);

		if (isCamera) {
			if (zoomMap.get(name) == null)
				zoomMap.put(name, new LinkedHashMap<>());
			zoomMap.get(name).put(frame, height / 2.0 * projection.m11);
		}
		
		Matrix4d mat = modelview;
		if (!isCamera) {
			mat.invert();
			mat.mul(camera);
		}
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
		
		position.put(frame, new Vector3d(mat.m03, mat.m13, mat.m23));
		
		mat.m03 = mat.m13 = mat.m23 = 0;
		trans.rotY(Math.PI);
		mat.mul(trans);
		mat.transpose();
		
		double Rx;
		double Ry;
		double Rz;
		boolean inv = false;
		
		if (!isCamera) {
			Matrix4d matInvSy = new Matrix4d(
					1, 0, 0, 0,
					0, -1, 0, 0,
					0, 0, 1, 0,
					0, 0, 0, 1
					);
			Vector4d matX = new Vector4d();
			mat.getColumn(0, matX);
			Vector4d matY = new Vector4d();
			mat.getColumn(1, matY);
			Vector4d matZ = new Vector4d();
			mat.getColumn(2, matZ);
			Vector3d vx = new Vector3d(matX.x, matX.y, matX.z);
			Vector3d vy = new Vector3d(matY.x, matY.y, matY.z);
			Vector3d vz = new Vector3d(matZ.x, matZ.y, matZ.z);
			
			Vector3d crossY = new Vector3d();
			Vector3d originalY = new Vector3d();
			originalY.normalize(vy);
			crossY.cross(vz, vx);
			crossY.normalize();
			if (crossY.dot(originalY) < 0) {
				inv = true;
				mat.mul(matInvSy);
			}
		}
		
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
		
		if (isCamera)
			orientation.put(frame, new Vector3d(-Rx, -Ry, 180 - Rz));
		else
			orientation.put(frame, new Vector3d(
					inv ? (Rx - 180) : (180 - Rx), 
					Ry, 
					inv ? 180 - Rz : Rz));
	}
	
	private void afterCamera() {
		frame++;
		tracking = true;
		track(null);
	}
	
	private void onRenderEnd() {
		tracking = false;
	}

}
