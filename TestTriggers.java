import java.io.*;
import java.util.jar.*;

public class TestTriggers {
    static class L extends ClassLoader {
        L(ClassLoader p) { super(p); }
        Class<?> def(String n, byte[] b) { return defineClass(n, b, 0, b.length); }
        void res(Class<?> c) { resolveClass(c); }
    }
    public static void main(String[] args) throws Exception {
        String jarPath = args[0];
        byte[] data;
        try (JarFile jar = new JarFile(jarPath)) {
            data = readAll(jar.getInputStream(jar.getJarEntry("a/hr.class")));
        }

        System.out.println("--- trigger 1: defineClass + resolveClass ---");
        try {
            L loader = new L(TestTriggers.class.getClassLoader());
            Class<?> c = loader.def("a.hr", data);
            loader.res(c);
            System.out.println("  no VerifyError (resolveClass does NOT verify)");
        } catch (VerifyError e) {
            System.out.println("  VerifyError: " + e.getMessage());
        }

        System.out.println("--- trigger 2: Class.forName(name, false, loader) ---");
        try {
            L loader = new L(TestTriggers.class.getClassLoader());
            loader.def("a.hr", data);
            Class.forName("a.hr", false, loader);
            System.out.println("  no VerifyError (forName init=false does NOT verify)");
        } catch (VerifyError e) {
            System.out.println("  VerifyError: " + e.getMessage());
        }

        System.out.println("--- trigger 3: Class.forName(name, true, loader) ---");
        try {
            L loader = new L(TestTriggers.class.getClassLoader());
            loader.def("a.hr", data);
            Class.forName("a.hr", true, loader);
            System.out.println("  no VerifyError");
        } catch (VerifyError e) {
            System.out.println("  VerifyError: " + e.getMessage());
        }
    }
    private static byte[] readAll(InputStream is) throws IOException {
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        byte[] buf = new byte[8192];
        int n;
        while ((n = is.read(buf)) != -1) bos.write(buf, 0, n);
        return bos.toByteArray();
    }
}
