import java.io.*;
import java.lang.reflect.Method;
import java.util.jar.*;

/**
 * Functional test: loads the obfuscated license class a/hr.class from the jar
 * (it only depends on JDK types, so it loads standalone), triggers JVM
 * verification via Class.forName(init=false), then invokes its real private
 * static zz(int[]) decrypt method with arrays produced by the same key formula
 * (i*7+0x5D) and confirms they round-trip back to the known original strings.
 */
public class TestLicVerify {

    static int key(int i) { return (i * 7 + 0x5D) & 0xFF; }
    static int encChar(int c, int i) { return (c ^ key(i)) & 0xFFFF; }

    static int[] encrypt(String s) {
        int[] enc = new int[s.length()];
        for (int i = 0; i < s.length(); i++) enc[i] = encChar(s.charAt(i), i);
        return enc;
    }

    static class L extends ClassLoader {
        private final byte[] bytes;
        L(ClassLoader p, byte[] bytes) { super(p); this.bytes = bytes; }
        @Override
        protected Class<?> findClass(String name) throws ClassNotFoundException {
            if (name.equals("a.hr")) return defineClass(name, bytes, 0, bytes.length);
            return super.findClass(name);
        }
    }

    public static void main(String[] args) throws Exception {
        String jarPath = args[0];
        byte[] data;
        try (JarFile jar = new JarFile(jarPath)) {
            data = readAll(jar.getInputStream(jar.getJarEntry("a/hr.class")));
        }
        L loader = new L(TestLicVerify.class.getClassLoader(), data);
        Class<?> c = Class.forName("a.hr", false, loader); // verification only
        System.out.println("Verified + loaded: " + c.getName());

        Method zz = null;
        for (Method m : c.getDeclaredMethods()) {
            if (m.getName().startsWith("zz") && m.getParameterCount() == 1
                    && m.getParameterTypes()[0] == int[].class) {
                zz = m;
            }
        }
        if (zz == null) throw new IllegalStateException("zz(int[]) not found");
        zz.setAccessible(true);
        System.out.println("decrypt method: " + zz.getName());

        String[] originals = {
            // the license URL is stored as 19 base64 fragments (segmented obfuscation),
            // each fragment encrypted as its own standalone string
            "VEhITE8=", "eWxsMSI=", "PWQtIz4=", "OSQzJCI=", "PSo7NzY=",
            "KzoxK3E=", "BQkLSS8=", "DgguAQI=", "ARAkGAE=", "A1QyGB4=",
            "we7t9+Y=", "2eX88aQ=", "x/Xy4/k=", "4/K4+vY=", "9/Cxwfo=",
            "xNHEisk=", "xc/Jwt8=", "1sCdx8s=", "zg==",
            "User-Agent", "GET", "UTF-8", "Accept", "Connection"
        };
        boolean allOk = true;
        for (String o : originals) {
            String dec = (String) zz.invoke(null, (Object) encrypt(o));
            boolean ok = o.equals(dec);
            allOk &= ok;
            System.out.println((ok ? "PASS" : "FAIL") + "  " + o + "  ->  " + dec);
        }
        System.out.println(allOk ? "ALL ROUND-TRIP OK" : "MISMATCH DETECTED");
        if (!allOk) System.exit(3);
    }

    private static byte[] readAll(InputStream is) throws IOException {
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        byte[] buf = new byte[8192];
        int n;
        while ((n = is.read(buf)) != -1) bos.write(buf, 0, n);
        return bos.toByteArray();
    }
}
