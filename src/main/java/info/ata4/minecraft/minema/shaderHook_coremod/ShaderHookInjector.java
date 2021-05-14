package info.ata4.minecraft.minema.shaderHook_coremod;

import net.minecraft.launchwrapper.IClassTransformer;

import org.apache.logging.log4j.LogManager;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.Label;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.FieldInsnNode;
import org.objectweb.asm.tree.FrameNode;
import org.objectweb.asm.tree.InsnList;
import org.objectweb.asm.tree.InsnNode;
import org.objectweb.asm.tree.JumpInsnNode;
import org.objectweb.asm.tree.LabelNode;
import org.objectweb.asm.tree.LdcInsnNode;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.MethodNode;
import org.objectweb.asm.tree.TypeInsnNode;
import org.objectweb.asm.tree.VarInsnNode;

import info.ata4.minecraft.minema.client.modules.SyncModule;

import java.util.Iterator;
import java.util.ListIterator;

public final class ShaderHookInjector implements IClassTransformer {

	// All obfuscated/deobfuscated mappings can be found in the .gradle
	// directory (usually inside user directory) in
	// .gradle\caches\minecraft\de\oceanlabs\mcp\mcp_snapshot\XXXXXXXX\X.XX\srgs\mcp-notch.srg:
	// MCP Mappings for all classes, methods and fields
	// Do not use methods.csv etc. because those are the Forge mappings (which
	// is only relevant for runtime reflection)

	private static final String entityRenderer = "net.minecraft.client.renderer.EntityRenderer";
	private static final String minecraftServer = "net.minecraft.server.MinecraftServer";
	private static final String screenshotHelper = "net.minecraft.util.ScreenShotHelper";
	private static final String glStateManager = "net.minecraft.client.renderer.GlStateManager";
	private static final String entityTrackerEntry = "net.minecraft.entity.EntityTrackerEntry";
	private static final String netHandlerPlayClient = "net.minecraft.client.network.NetHandlerPlayClient";
	
	@Override
	public byte[] transform(final String obfuscated, final String deobfuscated, final byte[] bytes) {
		// "Deobfuscated" is always passed as a deobfuscated argument, but the
		// "obfuscated" argument may be deobfuscated or obfuscated
		if (entityRenderer.equals(deobfuscated) || minecraftServer.equals(deobfuscated)
				|| screenshotHelper.equals(deobfuscated)
				|| glStateManager.equals(deobfuscated)
				|| entityTrackerEntry.equals(deobfuscated)
				|| netHandlerPlayClient.equals(deobfuscated)) {

			final ClassReader classReader = new ClassReader(bytes);
			final ClassNode classNode = new ClassNode();
			classReader.accept(classNode, 0);

			boolean isInAlreadyDeobfuscatedState = obfuscated.equals(deobfuscated);

			if (entityRenderer.equals(deobfuscated)) {
				this.transformEntityRenderer(classNode, isInAlreadyDeobfuscatedState);
			} else if (minecraftServer.equals(deobfuscated)) {
				this.transformMinecraftServer(classNode, isInAlreadyDeobfuscatedState);
			} else if (screenshotHelper.equals(deobfuscated)) {
				this.transformScreenshotHelper(classNode, isInAlreadyDeobfuscatedState);
			} else if (glStateManager.equals(deobfuscated)) {
				this.transformGlStateManager(classNode, isInAlreadyDeobfuscatedState);
			} else if (entityTrackerEntry.equals(deobfuscated)) {
				this.transformEntityTrackerEntry(classNode, isInAlreadyDeobfuscatedState);
			} else if (netHandlerPlayClient.equals(deobfuscated)) {
				this.transformNetHandlerPlayClient(classNode, isInAlreadyDeobfuscatedState);
			}

			final ClassWriter classWriter = new ClassWriter(ClassWriter.COMPUTE_MAXS);
			classNode.accept(classWriter);
			return classWriter.toByteArray();

		}

		return bytes;
	}

	private void transformEntityRenderer(ClassNode classNode, boolean isInAlreadyDeobfuscatedState) {
		final String method = isInAlreadyDeobfuscatedState ? "renderWorld" : "b";

		for (final MethodNode m : classNode.methods) {
			if (method.equals(m.name) && "(FJ)V".equals(m.desc)) {
				// after the GLStateManager.enableDepth call:
				// that is right after Optifine patches the source code to
				// call shadersmod/client/Shaders#beginRender which includes
				// the initialization of frameTimeCounter

				String calledClass = isInAlreadyDeobfuscatedState ? "net/minecraft/client/renderer/GlStateManager" : "bus";
				String calledMethod = isInAlreadyDeobfuscatedState ? "enableDepth" : "k";

				// find it (insert and insertBefore do not work because
				// nodes build the actual recursive data structure and the
				// location has to be an actual member of the data)

				ListIterator<AbstractInsnNode> iterator = m.instructions.iterator();

				while (iterator.hasNext()) {
					AbstractInsnNode currentNode = iterator.next();
					if (doesMatchStaticCall(currentNode, calledClass, calledMethod, "()V")) {
						iterator.add(new MethodInsnNode(Opcodes.INVOKESTATIC,
//								"info/ata4/minecraft/minema/client/modules/ShaderSync",
//								"setFrameTimeCounter", "()V", false));
								"info/ata4/minecraft/minema/client/modules/SyncModule",
								"doFrameTimeSync", "()V", false));
						break;
					}
				}
			} else if (m.name.equals("a") && m.desc.equals("(IFJ)V")) {
				ListIterator<AbstractInsnNode> iterator = m.instructions.iterator();

				while (iterator.hasNext()) {
					AbstractInsnNode currentNode = iterator.next();

					if (currentNode.getOpcode() == Opcodes.LDC) {
						LdcInsnNode ldc = (LdcInsnNode) currentNode;

						if ("hand".equals(ldc.cst)) {
							currentNode = iterator.next();
							iterator.add(new MethodInsnNode(Opcodes.INVOKESTATIC, "info/ata4/minecraft/minema/CaptureSession", "ASMmidRender", "()V", false));

							break;
						}
					}
				}
				
				iterator = m.instructions.iterator();
				while (iterator.hasNext()) {
					AbstractInsnNode currentNode = iterator.next();
					
					if (currentNode instanceof MethodInsnNode) {
						MethodInsnNode mnode = (MethodInsnNode) currentNode;
						if (mnode.name.equals("a") && mnode.desc.equals("(FI)V") && mnode.owner.equals("buq")) {
							iterator.add(new MethodInsnNode(Opcodes.INVOKESTATIC, "info/ata4/minecraft/minema/CaptureSession", "ASMAfterCamera", "()V", false));
							break;
						}
					}
				}
			}
		}
	}

	private void transformMinecraftServer(ClassNode classNode, boolean isInAlreadyDeobfuscatedState) {
		
		for (MethodNode method : classNode.methods) {
			if (method.name.equals("run")) {
//				int i = 0;
				Iterator<AbstractInsnNode> nodes = method.instructions.iterator();
				AbstractInsnNode target = null;
				
				boolean isDeobf = false;
				AbstractInsnNode last = null;
				while (nodes.hasNext()) {
					AbstractInsnNode node = nodes.next();

					if (node instanceof VarInsnNode) {
						VarInsnNode var = (VarInsnNode) node;

//						if (var.getOpcode() == Opcodes.LSTORE && var.var == 1) {
//							if (i == 1) {
//								target = var;
//							}
//
//							i += 1;
//						}
						
//					    INVOKESTATIC net/minecraft/server/MinecraftServer.getCurrentTimeMillis ()J
//					    LSTORE 3
						if (var.getOpcode() == Opcodes.LSTORE && var.var == 3) {
							if (last instanceof MethodInsnNode) {
								MethodInsnNode m = (MethodInsnNode) last;
								if (m.getOpcode() == Opcodes.INVOKESTATIC) {
									if (m.name.equals("getCurrentTimeMillis")) {
										target = last;
										isDeobf = true;
										break;
									} else if (m.name.equals("aw")) {
										target = last;
										isDeobf = false;
										break;
									}
								}
							}
						}
					}
					last = node;
				}

				if (target != null) {
					InsnList list = new InsnList();

//					list.add(new VarInsnNode(Opcodes.LLOAD, 1));
//					list.add(new MethodInsnNode(Opcodes.INVOKESTATIC, "info/ata4/minecraft/minema/client/modules/ShaderSync", "correctServerTick", "(J)J", false));
//					list.add(new VarInsnNode(Opcodes.LSTORE, 1));
//
//					method.instructions.insert(target, list);
					
//				    ALOAD 0
//				    ALOAD 0
//				    GETFIELD info/ata4/minecraft/minema/shaderHook_coremod/ShaderHookInjector.currentTime : J
//				    INVOKESTATIC info/ata4/minecraft/minema/client/modules/SyncModule.doServerTickSync (J)J
//				    PUTFIELD info/ata4/minecraft/minema/shaderHook_coremod/ShaderHookInjector.currentTime : J
					String fieldCurrentTime = isDeobf ? "currentTime" : "ab";
					list.add(new VarInsnNode(Opcodes.ALOAD, 0));
					list.add(new VarInsnNode(Opcodes.ALOAD, 0));
					list.add(new FieldInsnNode(Opcodes.GETFIELD, "net/minecraft/server/MinecraftServer", fieldCurrentTime, "J"));
					list.add(new MethodInsnNode(Opcodes.INVOKESTATIC, "info/ata4/minecraft/minema/client/modules/SyncModule", "doServerTickSync", "(J)J", false));
					list.add(new FieldInsnNode(Opcodes.PUTFIELD, "net/minecraft/server/MinecraftServer", fieldCurrentTime, "J"));
					method.instructions.insertBefore(target, list);
				}
				
			}
		}
	}

	private void transformScreenshotHelper(ClassNode classNode, boolean isInAlreadyDeobfuscatedState) {
		for (MethodNode method : classNode.methods) {
			if (!method.desc.contains("BufferedImage")) {
				continue;
			}

			boolean passedBufferImage = false;
			AbstractInsnNode target = null;
			Iterator<AbstractInsnNode> it = method.instructions.iterator();

			while (it.hasNext())
			{
				AbstractInsnNode node = it.next();

				if (passedBufferImage && node.getOpcode() == Opcodes.ICONST_1) {
					target = node;

					break;
				} else if (node.getOpcode() == Opcodes.NEW && ((TypeInsnNode) node).desc.endsWith("BufferedImage"))  {
					passedBufferImage = true;

					continue;
				}
			}

			if (target != null)
			{
				AbstractInsnNode node = new MethodInsnNode(Opcodes.INVOKESTATIC, "info/ata4/minecraft/minema/client/util/ScreenshotHelper", "getType", "()I", false);

				method.instructions.insert(target, node);
				method.instructions.remove(target);
			}
		}
	}

	private void transformGlStateManager(ClassNode classNode, boolean isInAlreadyDeobfuscatedState) {

		String blendFunc = isInAlreadyDeobfuscatedState ? "blendFunc" : "a";
		String tryBlendFuncSeparate = isInAlreadyDeobfuscatedState ? "tryBlendFuncSeparate" : "a";

		// test blendFunc tho may need to alter more
		for (MethodNode method : classNode.methods) {

			/**
			 * {@link net.minecraft.client.renderer.GlStateManager#blendFunc(int, int)}
			 */
			if((method.name.equals(blendFunc)) && method.desc.equals("(II)V")) {

				AbstractInsnNode invokeNode = null;
				Iterator<AbstractInsnNode> it = method.instructions.iterator();

				while (it.hasNext())
				{
					AbstractInsnNode node = it.next();
					if (node.getOpcode() == Opcodes.INVOKESTATIC) {
						invokeNode = node;
					}
				}

				if(invokeNode != null) {
					AbstractInsnNode methodInvoke = new MethodInsnNode(Opcodes.INVOKESTATIC,
							"info/ata4/minecraft/minema/client/util/ScreenshotHelper", "replaceBlendFunc",
							"(II)V", false);
					InsnList list = new InsnList();

					list.add(new VarInsnNode(Opcodes.ILOAD, 0));
					list.add(new VarInsnNode(Opcodes.ILOAD, 1));
					list.add(methodInvoke);

					method.instructions.insert(invokeNode, list);
				}
			}

			/**
			 * {@link net.minecraft.client.renderer.GlStateManager#tryBlendFuncSeparate(int, int, int, int)}
			 */
			if(method.name.equals(tryBlendFuncSeparate) && method.desc.equals("(IIII)V")) {

				AbstractInsnNode invokeNode = null;
				Iterator<AbstractInsnNode> it = method.instructions.iterator();

				while (it.hasNext())
				{
					AbstractInsnNode node = it.next();
					if (node.getOpcode() == Opcodes.INVOKESTATIC) {
						invokeNode = node;
					}
				}

				if(invokeNode != null) {
					AbstractInsnNode methodInvoke = new MethodInsnNode(Opcodes.INVOKESTATIC,
							"info/ata4/minecraft/minema/client/util/ScreenshotHelper", "tryBlendFuncSeparate",
							"(IIII)V", false);
					InsnList list = new InsnList();

					list.add(new VarInsnNode(Opcodes.ILOAD, 0));
					list.add(new VarInsnNode(Opcodes.ILOAD, 1));
					list.add(new VarInsnNode(Opcodes.ILOAD, 2));
					list.add(new VarInsnNode(Opcodes.ILOAD, 3));
					list.add(methodInvoke);

					method.instructions.insert(invokeNode, list);
				}
			}
		}
	}

	private void transformEntityTrackerEntry(ClassNode classNode, boolean isInAlreadyDeobfuscatedState) {
		String m = isInAlreadyDeobfuscatedState ? "updatePlayerList" : "a";
		String c = isInAlreadyDeobfuscatedState ? "net/minecraft/entity/EntityTrackerEntry" : "os";
		String f = isInAlreadyDeobfuscatedState ? "updateFrequency" : "g";
		for (MethodNode method : classNode.methods) {
			if (method.desc.equals("(Ljava/util/List;)V") && method.name.equals(m)) {
				AbstractInsnNode target = null;
				Iterator<AbstractInsnNode> it = method.instructions.iterator();

				while (it.hasNext())
				{
					AbstractInsnNode node = it.next();
					if (node.getOpcode() == Opcodes.GETFIELD) {
						FieldInsnNode fnode = (FieldInsnNode) node;
						if (fnode.owner.equals(c) && fnode.name.equals(f)) {
							target = node;
							break;
						}
					}
				}
				
				if (target != null)
				{
					AbstractInsnNode node = new MethodInsnNode(Opcodes.INVOKESTATIC, "info/ata4/minecraft/minema/client/modules/SyncModule", "getUpdateFrequency", "(I)I", false);
					method.instructions.insert(target, node);
					return;
				}
			}
		}
	}

	private void transformNetHandlerPlayClient(ClassNode classNode, boolean isInAlreadyDeobfuscatedState) {
		String m1 = isInAlreadyDeobfuscatedState ? "handleEntityMovement(Lnet/minecraft/network/play/server/SPacketEntity;)V" : "a(Ljj;)V";
		String m2 = isInAlreadyDeobfuscatedState ? "handleEntityTeleport(Lnet/minecraft/network/play/server/SPacketEntityTeleport;)V" : "a(Lkt;)V";
		for (MethodNode method : classNode.methods) {
			String m = method.name + method.desc;
			if (m.equals(m1) || m.equals(m2)) {
				AbstractInsnNode target = null;
				Iterator<AbstractInsnNode> it = method.instructions.iterator();

				while (it.hasNext())
				{
					AbstractInsnNode node = it.next();
					if (node.getOpcode() == Opcodes.ICONST_3) {
						target = node;
						break;
					}
				}
				
				if (target != null)
				{
					AbstractInsnNode node = new MethodInsnNode(Opcodes.INVOKESTATIC, "info/ata4/minecraft/minema/client/modules/SyncModule", "getUpdateFrequency", "(I)I", false);
					method.instructions.insert(target, node);
				}
			}
		}
	}

	private boolean doesMatchStaticCall(AbstractInsnNode node, String calledClass, String calledMethod, String signature) {
		if (node.getOpcode() == Opcodes.INVOKESTATIC) {
			MethodInsnNode methodCall = (MethodInsnNode) node;
			if (methodCall.owner.equals(calledClass) && methodCall.name.equals(calledMethod)
					&& methodCall.desc.equals(signature)) {
				return true;
			}
		}

		return false;
	}

}
