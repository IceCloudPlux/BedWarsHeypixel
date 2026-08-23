import org.objectweb.asm.*;
import java.io.*;

public class TestAsm3 {
    public static void main(String[] args) throws Exception {
        File f = new File(args[0]);
        byte[] data = readFully(new FileInputStream(f));
        System.out.println("input size=" + data.length);

        ClassReader cr = new ClassReader(data);
        final int[] totalInsns = {0};
        final int[] totalFrames = {0};
        ClassWriter cw = new ClassWriter(cr, ClassWriter.COMPUTE_FRAMES);
        ClassVisitor cv = new ClassVisitor(Opcodes.ASM9, cw) {
            @Override public MethodVisitor visitMethod(int access, String name, String desc, String sig, String[] ex) {
                final MethodVisitor mv = super.visitMethod(access, name, desc, sig, ex);
                return new MethodVisitor(Opcodes.ASM9, mv) {
                    @Override public void visitFrame(int t, int nl, Object[] l, int ns, Object[] st) {
                        totalFrames[0]++;
                        mv.visitFrame(t, nl, l, ns, st);
                    }
                    @Override public void visitInsn(int op) { totalInsns[0]++; mv.visitInsn(op); }
                    @Override public void visitIntInsn(int op, int o) { totalInsns[0]++; mv.visitIntInsn(op, o); }
                    @Override public void visitVarInsn(int op, int v) { totalInsns[0]++; mv.visitVarInsn(op, v); }
                    @Override public void visitTypeInsn(int op, String t) { totalInsns[0]++; mv.visitTypeInsn(op, t); }
                    @Override public void visitFieldInsn(int op, String o, String n2, String d2) { totalInsns[0]++; mv.visitFieldInsn(op, o, n2, d2); }
                    @Override public void visitMethodInsn(int op, String o, String n2, String d2, boolean itf) { totalInsns[0]++; mv.visitMethodInsn(op, o, n2, d2, itf); }
                    @Override public void visitJumpInsn(int op, Label l) { totalInsns[0]++; mv.visitJumpInsn(op, l); }
                };
            }
        };
        cr.accept(cv, 0);
        System.out.println("insns visited during write = " + totalInsns[0]);
        System.out.println("frames visited during write = " + totalFrames[0]);
        byte[] out = cw.toByteArray();
        System.out.println("output size=" + out.length);

        final int[] outFrames = {0};
        ClassReader cr2 = new ClassReader(out);
        cr2.accept(new ClassVisitor(Opcodes.ASM9) {
            @Override public MethodVisitor visitMethod(int a, String n, String d, String s, String[] e) {
                return new MethodVisitor(Opcodes.ASM9) {
                    @Override public void visitFrame(int t, int nl, Object[] l, int ns, Object[] st) {
                        outFrames[0]++;
                    }
                };
            }
        }, 0);
        System.out.println("frames in output = " + outFrames[0]);
    }

    private static byte[] readFully(InputStream is) throws IOException {
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        byte[] buf = new byte[8192];
        int n;
        while ((n = is.read(buf)) != -1) bos.write(buf, 0, n);
        return bos.toByteArray();
    }
}
