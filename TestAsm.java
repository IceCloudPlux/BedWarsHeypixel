import org.objectweb.asm.*;
import java.io.*;

public class TestAsm {
    public static void main(String[] args) throws Exception {
        // Load the class file from the directory
        File f = new File(args[0]);
        byte[] data = readFully(new FileInputStream(f));
        System.out.println("input size=" + data.length);

        ClassReader cr = new ClassReader(data);
        ClassWriter cw = new ClassWriter(cr, ClassWriter.COMPUTE_FRAMES);
        cr.accept(cw, 0);
        byte[] out = cw.toByteArray();
        System.out.println("output size=" + out.length);

        final int[] frameCount = {0};
        ClassReader cr2 = new ClassReader(out);
        cr2.accept(new ClassVisitor(Opcodes.ASM9) {
            @Override public MethodVisitor visitMethod(int a, String n, String d, String s, String[] e) {
                System.out.println("  method " + n + d);
                return new MethodVisitor(Opcodes.ASM9) {
                    @Override public void visitFrame(int t, int nl, Object[] l, int ns, Object[] st) {
                        frameCount[0]++;
                    }
                };
            }
        }, 0);
        System.out.println("frame callbacks after rewrite = " + frameCount[0]);
    }

    private static byte[] readFully(InputStream is) throws IOException {
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        byte[] buf = new byte[8192];
        int n;
        while ((n = is.read(buf)) != -1) bos.write(buf, 0, n);
        return bos.toByteArray();
    }
}
