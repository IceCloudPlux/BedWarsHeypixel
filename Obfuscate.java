import org.objectweb.asm.*;

import java.io.*;
import java.nio.file.*;
import java.util.*;
import java.util.jar.*;
import java.util.zip.*;

/**
 * ASM-based obfuscator for the BedWars plugin.
 * Strips debug attributes (SourceFile, LineNumberTable, LocalVariableTable,
 * LocalVariableTypeTable, SourceDebugExtension) from every class file,
 * producing smaller, harder-to-decompile classes without breaking bytecode.
 *
 * Usage: java -cp asm-9.7.jar Obfuscate <in.jar> <out.jar>
 */
public class Obfuscate {

    private static int classesProcessed = 0;
    private static int classesStripped = 0;
    private static int classesFailed = 0;

    public static void main(String[] args) throws Exception {
        if (args.length < 2) {
            System.err.println("Usage: java Obfuscate <in.jar> <out.jar>");
            System.exit(1);
        }
        Path in = Paths.get(args[0]);
        Path out = Paths.get(args[1]);

        try (JarInputStream jis = new JarInputStream(Files.newInputStream(in));
             JarOutputStream jos = new JarOutputStream(Files.newOutputStream(out))) {

            // Preserve the original manifest (JarInputStream reads it separately)
            Manifest manifest = jis.getManifest();
            if (manifest != null) {
                jos.putNextEntry(new JarEntry("META-INF/MANIFEST.MF"));
                manifest.write(jos);
                jos.closeEntry();
            }

            byte[] buffer = new byte[65536];
            JarEntry entry;
            while ((entry = jis.getNextJarEntry()) != null) {
                ByteArrayOutputStream bos = new ByteArrayOutputStream();
                int read;
                while ((read = jis.read(buffer)) != -1) {
                    bos.write(buffer, 0, read);
                }
                byte[] data = bos.toByteArray();

                byte[] outData;
                if (entry.getName().endsWith(".class") && !entry.getName().startsWith("META-INF/")) {
                    outData = obfuscateClass(data, entry.getName());
                } else {
                    outData = data;
                }

                JarEntry newEntry = new JarEntry(entry.getName());
                newEntry.setTime(entry.getTime());
                jos.putNextEntry(newEntry);
                jos.write(outData);
                jos.closeEntry();
            }
        }

        System.out.println("Processed " + classesProcessed + " classes, stripped debug info from "
                + classesStripped + ", failed " + classesFailed);
    }

    private static byte[] obfuscateClass(byte[] classBytes, String name) {
        classesProcessed++;
        try {
            ClassReader cr = new ClassReader(classBytes);
            ClassWriter cw = new ClassWriter(cr, 0);

            boolean[] changed = {false};
            cr.accept(new DebugStrippingVisitor(cw, changed), 0);

            byte[] out = cw.toByteArray();
            // Validate: re-parse the result to make sure it is structurally valid
            new ClassReader(out);
            if (changed[0]) {
                classesStripped++;
                return out;
            }
            return classBytes;
        } catch (Exception e) {
            classesFailed++;
            System.err.println("ERROR processing " + name + ": " + e);
            return classBytes;
        }
    }

    /** Visitor that removes debug information. */
    private static class DebugStrippingVisitor extends ClassVisitor {
        private final boolean[] changed;

        DebugStrippingVisitor(ClassVisitor cv, boolean[] changed) {
            super(Opcodes.ASM9, cv);
            this.changed = changed;
        }

        @Override
        public void visitSource(String source, String debug) {
            changed[0] = true; // SourceFile / SourceDebugExtension removed
            // skip -> attribute dropped
        }

        @Override
        public MethodVisitor visitMethod(int access, String name, String descriptor,
                                         String signature, String[] exceptions) {
            MethodVisitor mv = super.visitMethod(access, name, descriptor, signature, exceptions);
            return new DebugStrippingMethodVisitor(mv, changed);
        }
    }

    /** Method visitor that drops line numbers and local variable tables. */
    private static class DebugStrippingMethodVisitor extends MethodVisitor {
        private final boolean[] changed;

        DebugStrippingMethodVisitor(MethodVisitor mv, boolean[] changed) {
            super(Opcodes.ASM9, mv);
            this.changed = changed;
        }

        @Override
        public void visitLineNumber(int line, Label start) {
            changed[0] = true; // LineNumberTable removed
            // skip
        }

        @Override
        public void visitLocalVariable(String name, String descriptor, String signature,
                                       Label start, Label end, int index) {
            changed[0] = true; // LocalVariableTable removed
            // skip
        }

        @Override
        public void visitFrame(int type, int nLocal, Object[] local, int nStack, Object[] stack) {
            // Frames must be preserved (we do not recompute)
            super.visitFrame(type, nLocal, local, nStack, stack);
        }
    }
}
