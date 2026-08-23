import java.io.*;
import java.util.*;
import java.util.jar.*;

public class TestPLI {
    static class V extends ClassLoader {
        V(ClassLoader p) { super(p); }
        @Override protected Class<?> loadClass(String n, boolean r) throws ClassNotFoundException {
            synchronized (getClassLoadingLock(n)) {
                Class<?> c = findLoadedClass(n);
                if (c == null) { c = super.loadClass(n, r); }
                if (r) resolveClass(c);
                return c;
            }
        }
    }
    public static void main(String[] args) throws Exception {
        String jarPath = args[0];
        byte[] data;
        try (JarFile jar = new JarFile(jarPath)) {
            data = readAll(jar.getInputStream(jar.getJarEntry("com/andrei1058/bedwars/libs/sidebar/v1_20_R3/PlayerListImpl.class")));
        }
        V v = new V(TestPLI.class.getClassLoader());
        Class<?> c = v.defineClass("com.andrei1058.bedwars.libs.sidebar.v1_20_R3.PlayerListImpl", data, 0, data.length);
        try {
            Class.forName("com.andrei1058.bedwars.libs.sidebar.v1_20_R3.PlayerListImpl", true, v);
            System.out.println("OK - verified + initialized");
        } catch (Throwable t) {
            t.printStackTrace(System.out);
        }
        // check if IChatMutableComponent is on the parent classpath
        try {
            Class<?> icmc = Class.forName("net.minecraft.network.chat.IChatMutableComponent", false, TestPLI.class.getClassLoader());
            System.out.println("IChatMutableComponent resolvable: " + icmc.getName() + " interface=" + icmc.isInterface());
        } catch (Throwable t) {
            System.out.println("IChatMutableComponent NOT resolvable: " + t);
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
