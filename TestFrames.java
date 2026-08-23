import org.objectweb.asm.*;

import java.io.*;
import java.util.jar.*;

public class TestFrames {
    public static void main(String[] args) throws Exception {
        String jarPath = args[0];
        String entry = args[1]; // e.g. a/hr.class
        byte[] data;
        try (JarFile jar = new JarFile(jarPath)) {
            data = readFully(jar.getInputStream(jar.getJarEntry(entry)));
        }
        System.out.println("input size=" + data.length);

        ClassReader cr = new ClassReader(data);
        System.out.println("version=" + cr.readShort(6) + " (raw)");

        ClassWriter cw = new ClassWriter(cr, ClassWriter.COMPUTE_FRAMES);
        cr.accept(cw, 0);
        byte[] out = cw.toByteArray();
        System.out.println("output size=" + out.length);

        // check for StackMapTable via javap-like scan: re-parse with visitor counting frames
        final int[] frameCount = {0};
        ClassReader cr2 = new ClassReader(out);
        cr2.accept(new ClassVisitor(Opcodes.ASM9) {
            @Override public MethodVisitor visitMethod(int a, String n, String d, String s, String[] e) {
                return new MethodVisitor(Opcodes.ASM9) {
                    @Override public void visitFrame(int t, int nl, Object[] l, int ns, Object[] st) {
                        frameCount[0]++;
                    }
                };
            }
        }, 0);
        System.out.println("frame visit callbacks after rewrite = " + frameCount[0]);
    }

    private static byte[] readFully(InputStream is) throws IOException {
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        byte[] buf = new byte[8192];
        int n;
        while ((n = is.read(buf)) != -1) bos.write(buf, 0, n);
        return bos.toByteArray();
    }
}
