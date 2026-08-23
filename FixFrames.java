import org.objectweb.asm.*;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassWriter;

import java.io.*;
import java.util.*;
import java.util.jar.*;

/**
 * Post-processor for ProGuard output (which used -dontpreverify and therefore
 * produced classes without StackMapTable frames). Recomputes StackMapTable
 * frames with ASM COMPUTE_FRAMES so the classes pass JVM verification.
 *
 * Run with classpath: fixframes;obfuscated.jar;<all spigot/libs jars>;asm.jar
 * so that the class hierarchy (common superclasses) can be resolved.
 *
 * Usage: java FixFrames <proguard-out.jar> <final.jar>
 */
public class FixFrames {

    public static void main(String[] args) throws Exception {
        String in = args[0], out = args[1];
        int fixed = 0, failed = 0, kept = 0, total = 0;
        List<String> failures = new ArrayList<>();

        try (JarInputStream jis = new JarInputStream(new FileInputStream(in));
             JarOutputStream jos = new JarOutputStream(new FileOutputStream(out))) {

            Manifest mf = jis.getManifest();
            if (mf != null) {
                jos.putNextEntry(new JarEntry("META-INF/MANIFEST.MF"));
                mf.write(jos);
                jos.closeEntry();
            }

            byte[] buf = new byte[65536];
            JarEntry e;
            while ((e = jis.getNextJarEntry()) != null) {
                ByteArrayOutputStream bos = new ByteArrayOutputStream();
                int n;
                while ((n = jis.read(buf)) != -1) bos.write(buf, 0, n);
                byte[] data = bos.toByteArray();

                byte[] outData = data;
                if (e.getName().endsWith(".class") && !e.getName().startsWith("META-INF/versions/")) {
                    total++;
                    byte[] fixedBytes = fixFrames(data, e.getName());
                    if (fixedBytes != data) fixed++;
                    else { failed++; failures.add(e.getName()); }
                    outData = fixedBytes;
                } else {
                    kept++;
                }

                JarEntry ne = new JarEntry(e.getName());
                ne.setTime(e.getTime());
                jos.putNextEntry(ne);
                jos.write(outData);
                jos.closeEntry();
            }
        }
        System.out.println("FixFrames: classes=" + total + " recomputed=" + fixed
                + " UNFIXED=" + failed + " non-class kept=" + kept);
        for (String f : failures) System.out.println("  UNFIXED: " + f);
    }

    private static byte[] fixFrames(byte[] data, String name) {
        try {
            ClassReader cr = new ClassReader(data);
            // IMPORTANT: do NOT use `new ClassWriter(cr, COMPUTE_FRAMES)`.
            // Passing the reader enables ASM's constant-pool fast path, which
            // copies unmodified method Code attributes verbatim and silently
            // SKIPS frame computation (observed: output byte-identical to input,
            // zero frames). A fresh writer always recomputes frames.
            ClassWriter cw = new ClassWriter(ClassWriter.COMPUTE_FRAMES) {
                @Override
                protected String getCommonSuperClass(String type1, String type2) {
                    return Hierarchy.commonSuper(type1, type2);
                }
            };
            // ProGuard normalizes interface-method calls to InterfaceMethodref.
            // The HotSpot verifier requires invokevirtual to reference a
            // Methodref; the original bytecode used Methodref (itf=false).
            // Re-emit every INVOKEVIRTUAL as Methodref to pass verification.
            ClassVisitor cv = new ClassVisitor(Opcodes.ASM9, cw) {
                @Override
                public MethodVisitor visitMethod(int access, String mname, String mdesc,
                                                 String msig, String[] mex) {
                    MethodVisitor mv = super.visitMethod(access, mname, mdesc, msig, mex);
                    return new MethodVisitor(Opcodes.ASM9, mv) {
                        @Override
                        public void visitMethodInsn(int opcode, String owner, String m,
                                                    String d, boolean itf) {
                            if (opcode == Opcodes.INVOKEVIRTUAL && itf) {
                                itf = false; // write Methodref, not InterfaceMethodref
                            }
                            super.visitMethodInsn(opcode, owner, m, d, itf);
                        }
                    };
                }
            };
            cr.accept(cv, 0);
            byte[] out = cw.toByteArray();
            new ClassReader(out); // structural sanity check
            return out;
        } catch (Throwable t) {
            System.err.println("FixFrames FAILED for " + name + ": " + t);
            return data;
        }
    }

    /**
     * Resolves the class hierarchy by reading class files with ASM directly,
     * never via Class.forName. This avoids the JVM triggering verification of
     * obfuscated classes (which lack frames until we fix them), which would
     * otherwise throw VerifyError during getCommonSuperClass and cause the
     * fix to be silently skipped.
     */
    static final class Hierarchy {
        private static final Map<String, List<String>> superCache = new HashMap<>();

        /** Direct superclass + implemented interfaces of {@code name} (empty for java/lang/Object). */
        static List<String> directSupers(String name) {
            return superCache.computeIfAbsent(name, n -> {
                List<String> result = new ArrayList<>();
                try {
                    InputStream is = FixFrames.class.getClassLoader().getResourceAsStream(n + ".class");
                    if (is != null) {
                        ClassReader cr = new ClassReader(is);
                        if (cr.getSuperName() != null) result.add(cr.getSuperName());
                        result.addAll(Arrays.asList(cr.getInterfaces()));
                    }
                } catch (Throwable ignored) {
                    // class not on classpath (e.g. NMS) -> treated as Object
                }
                return result;
            });
        }

        /** Least common supertype of two type names (class or interface). */
        static String commonSuper(String t1, String t2) {
            if (t1.equals(t2)) return t1;

            // collect all transitive supertypes of t1
            Set<String> seen1 = new HashSet<>();
            ArrayDeque<String> queue = new ArrayDeque<>();
            queue.add(t1);
            while (!queue.isEmpty()) {
                String cur = queue.poll();
                if (seen1.add(cur)) {
                    for (String s : directSupers(cur)) queue.add(s);
                }
            }

            // BFS from t2; first supertype that t1 also has is the LCS
            Set<String> seen2 = new HashSet<>();
            queue.clear();
            queue.add(t2);
            while (!queue.isEmpty()) {
                String cur = queue.poll();
                if (seen1.contains(cur)) return cur;
                if (seen2.add(cur)) {
                    for (String s : directSupers(cur)) queue.add(s);
                }
            }
            return "java/lang/Object";
        }
    }
}
