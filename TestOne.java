import org.objectweb.asm.*;
import java.io.*;
import java.util.*;
import java.util.jar.*;

public class TestOne {
    // Same hierarchy resolver as FixFrames
    static final class Hierarchy {
        private static final Map<String, List<String>> superCache = new HashMap<>();
        static List<String> directSupers(String name) {
            return superCache.computeIfAbsent(name, n -> {
                List<String> result = new ArrayList<>();
                try {
                    InputStream is = TestOne.class.getClassLoader().getResourceAsStream(n + ".class");
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
        String jarPath = args[0];
        byte[] data;
        try (JarFile jar = new JarFile(jarPath)) {
            data = readAll(jar.getInputStream(jar.getJarEntry("com/andrei1058/bedwars/libs/sidebar/v1_20_R3/PlayerListImpl.class")));
        }
        System.out.println("input size=" + data.length);

        // Approach 1: FixFrames logic (Hierarchy resolver)
        ClassReader cr = new ClassReader(data);
        ClassWriter cw = new ClassWriter(ClassWriter.COMPUTE_FRAMES) {
            @Override protected String getCommonSuperClass(String t1, String t2) {
                return Hierarchy.commonSuper(t1, t2);
            }
        };
        cr.accept(cw, 0);
        byte[] out = cw.toByteArray();
        System.out.println("FixFrames(Hierarchy) output size=" + out.length);
        new FileOutputStream("_scan_tmp3\\PLI_ff.class").write(out);

        // Approach 2: default ASM getCommonSuperClass (needs full classpath)
        ClassReader cr2 = new ClassReader(data);
        ClassWriter cw2 = new ClassWriter(ClassWriter.COMPUTE_FRAMES);
        cr2.accept(cw2, 0);
        byte[] out2 = cw2.toByteArray();
        System.out.println("default-ASM output size=" + out2.length);
        new FileOutputStream("_scan_tmp3\\PLI_def.class").write(out2);
    }

    private static byte[] readAll(InputStream is) throws IOException {
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        byte[] buf = new byte[8192];
        int n;
        while ((n = is.read(buf)) != -1) bos.write(buf, 0, n);
        return bos.toByteArray();
    }
}
