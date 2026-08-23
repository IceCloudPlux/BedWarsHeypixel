import org.objectweb.asm.*;

import java.io.*;
import java.lang.reflect.*;
import java.util.*;
import java.util.jar.*;

/**
 * Locates the (renamed) LicenseChecker class in the obfuscated jar by scanning
 * for the class whose bytecode contains the encrypted fragment "VEhITE8=",
 * then invokes its renamed private decrypt method to prove URL decryption
 * still works after strong obfuscation.
 */
public class TestObfuscated {
    public static void main(String[] args) throws Exception {
        String jarPath = args[0];
        byte[] classBytes = null;
        String className = null;

        try (JarFile jar = new JarFile(jarPath)) {
            Enumeration<JarEntry> en = jar.entries();
            while (en.hasMoreElements()) {
                JarEntry e = en.nextElement();
                String n = e.getName();
                if (!n.endsWith(".class") || n.startsWith("META-INF/versions/")) continue;
                byte[] bytes = readFully(jar.getInputStream(e));
                if (contains(bytes, "VEhITE8=".getBytes("UTF-8"))) {
                    classBytes = bytes;
                    className = n.substring(0, n.length() - 6).replace('/', '.');
                    break;
                }
            }
        }
        if (classBytes == null) { System.out.println("FAIL: LicenseChecker-equivalent not found"); System.exit(1); }
        System.out.println("Found encrypted-string class (renamed): " + className);

        // Find the method that loads the encrypted fragment (the decrypt method)
        final String[] decryptMethod = new String[1];
        ClassReader cr = new ClassReader(classBytes);
        cr.accept(new ClassVisitor(Opcodes.ASM9) {
            @Override public MethodVisitor visitMethod(int a, String name, String desc, String s, String[] e) {
                return new MethodVisitor(Opcodes.ASM9) {
                    @Override public void visitLdcInsn(Object value) {
                        if ("VEhITE8=".equals(value) && decryptMethod[0] == null) {
                            decryptMethod[0] = name;
                        }
                    }
                };
            }
        }, 0);
        if (decryptMethod[0] == null) { System.out.println("FAIL: decrypt method not found"); System.exit(1); }
        System.out.println("Renamed decrypt method: " + decryptMethod[0]);

        // Load the class and invoke the decrypt method via reflection
        byte[] finalBytes = classBytes;
        String finalClassName = className;
        ClassLoader cl = new ClassLoader() {
            @Override protected Class<?> findClass(String name) throws ClassNotFoundException {
                if (name.equals(finalClassName)) return defineClass(name, finalBytes, 0, finalBytes.length);
                return super.findClass(name);
            }
        };
        Class<?> cls = cl.loadClass(finalClassName);
        Method m = cls.getDeclaredMethod(decryptMethod[0]);
        m.setAccessible(true);
        String url = (String) m.invoke(null);
        System.out.println("Decrypted URL: " + url);

        String expected = "https://raw.githubusercontent.com/IceCloudPlux/IceCloudPlux-Website/main/_data/licenses.txt";
        if (url.equals(expected)) {
            System.out.println("VERIFY: URL decryption works after strong obfuscation.");
        } else {
            System.out.println("FAIL: URL mismatch!");
            System.exit(1);
        }
    }

    private static boolean contains(byte[] data, byte[] needle) {
        outer:
        for (int i = 0; i <= data.length - needle.length; i++) {
            for (int j = 0; j < needle.length; j++) {
                if (data[i + j] != needle[j]) continue outer;
            }
            return true;
        }
        return false;
    }

    private static byte[] readFully(InputStream is) throws IOException {
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        byte[] buf = new byte[8192];
        int n;
        while ((n = is.read(buf)) != -1) bos.write(buf, 0, n);
        return bos.toByteArray();
    }
}
