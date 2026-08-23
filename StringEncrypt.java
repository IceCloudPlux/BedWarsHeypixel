import org.objectweb.asm.*;
import org.objectweb.asm.tree.*;

import java.io.*;
import java.util.*;
import java.util.jar.*;

/**
 * String-encryption post-processor for the license-checker class.
 *
 * Finds the class in the input jar whose constant pool contains the license
 * URL marker ("VEhITE8="), then replaces EVERY string literal in that class
 * with:  int[] {encrypted chars} + zz(int[]) -> String   (runtime decrypt).
 *
 * This removes the base64 fragments and the "User-Agent"/"GET" HTTP strings
 * from the constant pool so a decompiler cannot grep the URL or identify the
 * class. Class/method names are already obfuscated by ProGuard; we add the
 * runtime decrypt method zz(int[]) with an encrypted body-free string table.
 *
 * Frames are recomputed (COMPUTE_FRAMES) using the same ASM-hierarchy resolver
 * as FixFrames, so the output is directly JVM-verifiable.
 *
 * Usage: java StringEncrypt <in.jar> <out.jar>
 */
public class StringEncrypt {

    // ---- shared key formula (must match zz()) ----
    static int key(int i) { return (i * 7 + 0x5D) & 0xFF; }
    static int encChar(int c, int i) { return (c ^ key(i)) & 0xFFFF; }

    // ---- same hierarchy resolver as FixFrames ----
    static final class Hierarchy {
        private static final Map<String, List<String>> superCache = new HashMap<>();
        static List<String> directSupers(String name) {
            return superCache.computeIfAbsent(name, n -> {
                List<String> result = new ArrayList<>();
                try {
                    InputStream is = StringEncrypt.class.getClassLoader().getResourceAsStream(n + ".class");
                    if (is != null) {
                        ClassReader cr = new ClassReader(is);
                        if (cr.getSuperName() != null) result.add(cr.getSuperName());
                        result.addAll(Arrays.asList(cr.getInterfaces()));
                    }
                } catch (Throwable ignored) {}
                return result;
            });
        }
        static String commonSuper(String t1, String t2) {
            if (t1.equals(t2)) return t1;
            Set<String> seen1 = new HashSet<>();
            ArrayDeque<String> queue = new ArrayDeque<>();
            queue.add(t1);
            while (!queue.isEmpty()) {
                String cur = queue.poll();
                if (seen1.add(cur)) for (String s : directSupers(cur)) queue.add(s);
            }
            Set<String> seen2 = new HashSet<>();
            queue.clear(); queue.add(t2);
            while (!queue.isEmpty()) {
                String cur = queue.poll();
                if (seen1.contains(cur)) return cur;
                if (seen2.add(cur)) for (String s : directSupers(cur)) queue.add(s);
            }
            return "java/lang/Object";
        }
    }

    public static void main(String[] args) throws Exception {
        String in = args[0], out = args[1];
        // classes that get their string literals encrypted:
        //  1. the license-checker class (found via the URL marker)
        //  2. the main entry class BedWars (its plaintext license log messages
        //     reveal exactly where the license check happens and where to patch)
        Set<String> targets = new LinkedHashSet<>(Arrays.asList("com/andrei1058/bedwars/BedWars.class"));
        // every jar entry (class or resource) -> bytes; classes get rewritten
        Map<String, byte[]> classes = new LinkedHashMap<>();

        try (JarFile jar = new JarFile(in)) {
            Enumeration<JarEntry> en = jar.entries();
            while (en.hasMoreElements()) {
                JarEntry e = en.nextElement();
                byte[] data = readAll(jar.getInputStream(e));
                classes.put(e.getName(), data);
                if (e.getName().endsWith(".class") && !e.getName().startsWith("META-INF/")
                        && containsMarker(data)) targets.add(e.getName());
            }
        }

        int totalEnc = 0, totalFields = 0;
        for (String target : targets) {
            byte[] bytes = classes.get(target);
            if (bytes == null) {
                System.out.println("SKIP (not in jar): " + target);
                continue;
            }
            System.out.println("Encrypting: " + target);
            ClassNode cn = new ClassNode(Opcodes.ASM9);
            new ClassReader(bytes).accept(cn, 0);

            String decryptName = "zz";
            while (methodNameExists(cn, decryptName)) decryptName += "x";

            int encCount = 0;
            for (MethodNode mn : cn.methods) {
                if (mn.instructions == null) continue;
                for (AbstractInsnNode ins : mn.instructions.toArray()) {
                    if (ins instanceof LdcInsnNode) {
                        Object c = ((LdcInsnNode) ins).cst;
                        if (c instanceof String && !((String) c).isEmpty()) {
                            String s = (String) c;
                            InsnList seq = buildEncrypted(s);
                            seq.add(new MethodInsnNode(Opcodes.INVOKESTATIC, cn.name, decryptName,
                                    "([I)Ljava/lang/String;", false));
                            mn.instructions.insertBefore(ins, seq);
                            mn.instructions.remove(ins);
                            encCount++;
                        }
                    }
                }
            }
            // strip unreferenced static-final String ConstantValue fields
            // (javac inlines such constants into LDC at the use site, leaving the
            // field dead but its value still visible in the constant pool)
            int fieldRemoved = 0;
            for (Iterator<FieldNode> it = cn.fields.iterator(); it.hasNext(); ) {
                FieldNode fn = it.next();
                if (fn.value instanceof String && !fieldIsRead(cn, fn)) {
                    it.remove();
                    fieldRemoved++;
                }
            }

            cn.methods.add(makeDecryptMethod(decryptName, cn.name));
            totalEnc += encCount;
            totalFields += fieldRemoved;

            ClassWriter cw = new ClassWriter(ClassWriter.COMPUTE_FRAMES) {
                @Override protected String getCommonSuperClass(String t1, String t2) {
                    return Hierarchy.commonSuper(t1, t2);
                }
            };
            cn.accept(cw);
            classes.put(target, cw.toByteArray());
            System.out.println("  encrypted strings: " + encCount + ", removed dead String fields: " + fieldRemoved);
        }
        System.out.println("TOTAL encrypted strings: " + totalEnc + ", removed dead String fields: " + totalFields);

        // write jar
        try (JarOutputStream jos = new JarOutputStream(new FileOutputStream(out))) {
            for (Map.Entry<String, byte[]> e : classes.entrySet()) {
                JarEntry ne = new JarEntry(e.getKey());
                ne.setTime(System.currentTimeMillis());
                jos.putNextEntry(ne);
                jos.write(e.getValue());
                jos.closeEntry();
            }
        }
        System.out.println("Wrote " + out + "  (" + classes.size() + " classes)");
    }

    /** Whether the class already declares a method with the given name. */
    private static boolean methodNameExists(ClassNode cn, String name) {
        for (MethodNode mn : cn.methods) {
            if (mn.name.equals(name)) return true;
        }
        return false;
    }

    /** Whether any instruction in the class reads the given field (getstatic/getfield). */
    private static boolean fieldIsRead(ClassNode cn, FieldNode target) {
        for (MethodNode mn : cn.methods) {
            if (mn.instructions == null) continue;
            for (AbstractInsnNode ins : mn.instructions) {
                if (ins instanceof FieldInsnNode) {
                    FieldInsnNode fin = (FieldInsnNode) ins;
                    if (fin.name.equals(target.name) && fin.desc.equals(target.desc)) return true;
                }
            }
        }
        return false;
    }

    /** Build instructions that push an encrypted int[] for the given string. */
    private static InsnList buildEncrypted(String s) {
        int[] enc = new int[s.length()];
        for (int i = 0; i < s.length(); i++) enc[i] = encChar(s.charAt(i), i);
        InsnList seq = new InsnList();
        // array length
        pushInt(seq, enc.length);
        seq.add(new IntInsnNode(Opcodes.NEWARRAY, Opcodes.T_INT));
        for (int i = 0; i < enc.length; i++) {
            seq.add(new InsnNode(Opcodes.DUP));
            pushInt(seq, i);
            pushInt(seq, enc[i]);
            seq.add(new InsnNode(Opcodes.IASTORE));
        }
        return seq;
    }

    private static void pushInt(InsnList seq, int v) {
        if (v >= -1 && v <= 5) {
            seq.add(new InsnNode(Opcodes.ICONST_0 + v));
        } else if (v >= Byte.MIN_VALUE && v <= Byte.MAX_VALUE) {
            seq.add(new IntInsnNode(Opcodes.BIPUSH, v));
        } else if (v >= Short.MIN_VALUE && v <= Short.MAX_VALUE) {
            seq.add(new IntInsnNode(Opcodes.SIPUSH, v));
        } else {
            seq.add(new LdcInsnNode(v));
        }
    }

    /** private static String zz(int[] a) { ... XOR-decode ... } */
    private static MethodNode makeDecryptMethod(String name, String owner) {
        MethodNode m = new MethodNode(Opcodes.ACC_PRIVATE | Opcodes.ACC_STATIC, name, "([I)Ljava/lang/String;",
                null, null);
        InsnList il = m.instructions;
        // StringBuilder sb = new StringBuilder();
        il.add(new TypeInsnNode(Opcodes.NEW, "java/lang/StringBuilder"));
        il.add(new InsnNode(Opcodes.DUP));
        il.add(new MethodInsnNode(Opcodes.INVOKESPECIAL, "java/lang/StringBuilder", "<init>", "()V", false));
        il.add(new VarInsnNode(Opcodes.ASTORE, 1));
        // int i = 0;
        il.add(new InsnNode(Opcodes.ICONST_0));
        il.add(new VarInsnNode(Opcodes.ISTORE, 2));
        // loop head: while (i < a.length)
        LabelNode head = new LabelNode(), end = new LabelNode();
        il.add(head);
        il.add(new VarInsnNode(Opcodes.ILOAD, 2));
        il.add(new VarInsnNode(Opcodes.ALOAD, 0));
        il.add(new InsnNode(Opcodes.ARRAYLENGTH));
        il.add(new JumpInsnNode(Opcodes.IF_ICMPGE, end));
        // sb.append((char)(a[i] ^ key(i)));
        il.add(new VarInsnNode(Opcodes.ALOAD, 1));
        il.add(new VarInsnNode(Opcodes.ALOAD, 0));
        il.add(new VarInsnNode(Opcodes.ILOAD, 2));
        il.add(new InsnNode(Opcodes.IALOAD));
        il.add(new VarInsnNode(Opcodes.ILOAD, 2));
        pushInt(il, 7);
        il.add(new InsnNode(Opcodes.IMUL));
        pushInt(il, 0x5D);
        il.add(new InsnNode(Opcodes.IADD));
        il.add(new InsnNode(Opcodes.IXOR));
        il.add(new InsnNode(Opcodes.I2C));
        il.add(new MethodInsnNode(Opcodes.INVOKEVIRTUAL, "java/lang/StringBuilder", "append", "(C)Ljava/lang/StringBuilder;", false));
        il.add(new InsnNode(Opcodes.POP));
        // i++
        il.add(new IincInsnNode(2, 1));
        il.add(new JumpInsnNode(Opcodes.GOTO, head));
        // return sb.toString();
        il.add(end);
        il.add(new VarInsnNode(Opcodes.ALOAD, 1));
        il.add(new MethodInsnNode(Opcodes.INVOKEVIRTUAL, "java/lang/StringBuilder", "toString", "()Ljava/lang/String;", false));
        il.add(new InsnNode(Opcodes.ARETURN));
        m.maxLocals = 3;
        m.maxStack = 4;
        return m;
    }

    /** Check if class constant pool contains the license URL marker. */
    private static boolean containsMarker(byte[] data) {
        try {
            ClassNode cn = new ClassNode(Opcodes.ASM9);
            new ClassReader(data).accept(cn, 0);
            for (MethodNode mn : cn.methods) {
                if (mn.instructions == null) continue;
                for (AbstractInsnNode ins : mn.instructions) {
                    if (ins instanceof LdcInsnNode && "VEhITE8=".equals(((LdcInsnNode) ins).cst)) return true;
                }
            }
            for (FieldNode fn : cn.fields) {
                if (fn.value instanceof String && "VEhITE8=".equals(fn.value)) return true;
            }
        } catch (Throwable ignored) {}
        return false;
    }

    private static byte[] readAll(InputStream is) throws IOException {
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        byte[] buf = new byte[8192];
        int n;
        while ((n = is.read(buf)) != -1) bos.write(buf, 0, n);
        return bos.toByteArray();
    }
}
