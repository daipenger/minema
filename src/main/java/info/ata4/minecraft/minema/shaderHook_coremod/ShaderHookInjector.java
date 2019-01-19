package info.ata4.minecraft.minema.shaderHook_coremod;

import java.util.ListIterator;

import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.LdcInsnNode;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.MethodNode;

import net.minecraft.launchwrapper.IClassTransformer;

public final class ShaderHookInjector implements IClassTransformer {

	// All obfuscated/deobfuscated mappings can be found in the .gradle
	// directory (usually inside user directory) in
	// .gradle\caches\minecraft\de\oceanlabs\mcp\mcp_snapshot\XXXXXXXX\X.XX\srgs\mcp-notch.srg:
	// MCP Mappings for all classes, methods and fields
	// Do not use methods.csv etc. because those are the Forge mappings (which
	// is only relevant for runtime reflection)

	private static final String deobfuscatedClass = "net.minecraft.client.renderer.EntityRenderer";
	// private static final String obfuscatedClass = "buq";

	private static final String deobfuscatedMethod = "renderWorld";
	private static final String obfuscatedMethod = "b";

	@Override
	public byte[] transform(final String obfuscated, final String deobfuscated, final byte[] bytes) {
		// "Deobfuscated" is always passed as a deobfuscated argument, but the
		// "obfuscated" argument may be deobfuscated or obfuscated

		if (deobfuscatedClass.equals(deobfuscated)) {

			final ClassReader classReader = new ClassReader(bytes);
			final ClassNode classNode = new ClassNode();
			classReader.accept(classNode, 0);

			boolean isInAlreadyDeobfuscatedState = obfuscated.equals(deobfuscated);

			final String method = isInAlreadyDeobfuscatedState ? deobfuscatedMethod : obfuscatedMethod;

			for (final MethodNode m : classNode.methods) {

				if (method.equals(m.name) && "(FJ)V".equals(m.desc)) {

					// after the GLStateManager.enableDepth call:
					// that is right after Optifine patches the source code to
					// call shadersmod/client/Shaders#beginRender which includes
					// the initialization of frameTimeCounter

					String calledClass = isInAlreadyDeobfuscatedState ? "net/minecraft/client/renderer/GlStateManager"
							: "bus";
					String calledMethod = isInAlreadyDeobfuscatedState ? "enableDepth" : "k";

					// find it (insert and insertBefore do not work because
					// nodes build the actual recursive data structure and the
					// location has to be an actual member of the data)

					ListIterator<AbstractInsnNode> iterator = m.instructions.iterator();
					while (iterator.hasNext()) {
						AbstractInsnNode currentNode = iterator.next();
						if (doesMatchStaticCall(currentNode, calledClass, calledMethod, "()V")) {
							iterator.add(new MethodInsnNode(Opcodes.INVOKESTATIC,
									"info/ata4/minecraft/minema/client/modules/ShaderSync",
									"setFrameTimeCounter", "()V", false));
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
								iterator.add(new MethodInsnNode(Opcodes.INVOKESTATIC,
										"info/ata4/minecraft/minema/CaptureSession", "ASMmidRender", "()V", false));

								break;
							}
						}
					}

				}

			}

			final ClassWriter classWriter = new ClassWriter(ClassWriter.COMPUTE_MAXS);
			classNode.accept(classWriter);
			return classWriter.toByteArray();

		}

		if ("net.minecraft.entity.Entity".equals(deobfuscated)) {

			final ClassReader classReader = new ClassReader(bytes);
			final ClassNode classNode = new ClassNode();
			classReader.accept(classNode, 0);

			for (final MethodNode m : classNode.methods) {

				if ("c".equals(m.name) && "(FF)V".equals(m.desc)) {

					int i = 0;
					ListIterator<AbstractInsnNode> iterator = m.instructions.iterator();
					while (iterator.hasNext()) {
						AbstractInsnNode currentNode = iterator.next();
						if (i != 37) {
							i++;
							continue;
						}
						if (currentNode.getOpcode() != Opcodes.PUTFIELD) {
							System.out.println("NOT PUTFIELD!" + currentNode.getOpcode());
						}
						AbstractInsnNode a1 = iterator.next();
						System.out.println(a1.getOpcode());
						iterator.remove();
						AbstractInsnNode a2 = iterator.next();
						System.out.println(a2.getOpcode());
						iterator.remove();
						AbstractInsnNode a3 = iterator.next();
						System.out.println(a3.getOpcode());
						iterator.remove();
						AbstractInsnNode a4 = iterator.next();
						System.out.println(a4.getOpcode());
						iterator.remove();
						AbstractInsnNode a5 = iterator.next();
						System.out.println(a5.getOpcode());
						iterator.remove();
						AbstractInsnNode a6 = iterator.next();
						System.out.println(a6.getOpcode());
						iterator.remove();
						AbstractInsnNode a7 = iterator.next();
						System.out.println(a7.getOpcode());
						iterator.remove();
						break;
					}

					break;

				}

			}

			final ClassWriter classWriter = new ClassWriter(ClassWriter.COMPUTE_MAXS);
			classNode.accept(classWriter);
			return classWriter.toByteArray();

		}

		return bytes;
	}

	private boolean doesMatchStaticCall(AbstractInsnNode node, String calledClass, String calledMethod,
			String signature) {
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
