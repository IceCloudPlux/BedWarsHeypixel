import java.io.*;
import java.util.*;
import java.util.jar.*;

/**
 * Verifies every class in the target jar by loading all of its classes into a
 * dedicated classloader (consistent loader identity) and forcing JVM bytecode
 * verification via class initialization (Class.forName(name, true, loader)).
 *
 * Empirical JDK 17 behavior: defineClass+resolveClass, Class.forName(init=false)
 * and loadClass() do NOT trigger bytecode verification (HotSpot verifies
 * lazily); only initialization or first method execution does. Initialization
 * of <clinit> happens AFTER verification, so:
 *   - VerifyError            -> real frame problem (must be 0)
 *   - ExceptionInInitializer -> class verified OK, but static init failed in
 *                               this plain JVM (no Bukkit server) - tolerable
 *   - NoClassDefFoundError   -> missing optional dependency - tolerable
 *
 * The target jar must NOT be on the -cp; the parent loader provides only the
 * library classpath (spigot/paper/...). All target classes are defined by the
 * VerifyLoader itself from the jar bytes.
 */
public class LoadAllClasses {

    static class VerifyLoader extends ClassLoader {
        private final Map<String, byte[]> bytesByName;

        VerifyLoader(ClassLoader parent, Map<String, byte[]> bytesByName) {
            super(parent);
            this.bytesByName = bytesByName;
        }

        @Override
        protected Class<?> loadClass(String name, boolean resolve) throws ClassNotFoundException {
            synchronized (getClassLoadingLock(name)) {
                Class<?> c = findLoadedClass(name);
                if (c == null) {
                    byte[] bytes = bytesByName.get(name);
                    if (bytes != null) {
                        c = defineClass(name, bytes, 0, bytes.length); // target class: self-define
                    } else {
                        return super.loadClass(name, resolve); // library: parent first
                    }
                }
                if (resolve) resolveClass(c);
                return c;
            }
        }
    }

    public static void main(String[] args) throws Exception {
        String jarPath = args[0];
        Map<String, byte[]> bytesByName = new HashMap<>();
        try (JarFile jar = new JarFile(jarPath)) {
            Enumeration<JarEntry> en = jar.entries();
            while (en.hasMoreElements()) {
                JarEntry e = en.nextElement();
                String n = e.getName();
                if (n.endsWith(".class") && !n.startsWith("META-INF/versions/")) {
                    String cn = n.substring(0, n.length() - 6).replace('/', '.');
                    bytesByName.put(cn, readAll(jar.getInputStream(e)));
                }
            }
        }

        VerifyLoader loader = new VerifyLoader(LoadAllClasses.class.getClassLoader(), bytesByName);
        int ok = 0, verifyErr = 0, initErr = 0, missingDep = 0, other = 0;
        List<String> verifyFails = new ArrayList<>();
        Map<String, Integer> missingCounts = new TreeMap<>();

        for (String cn : bytesByName.keySet()) {
            try {
                // init=true -> link -> VERIFY -> run <clinit>
                Class.forName(cn, true, loader);
                ok++;
            } catch (VerifyError e) {
                verifyErr++;
                verifyFails.add(cn + "  ->  " + e.getMessage());
            } catch (ExceptionInInitializerError e) {
                initErr++; // verified OK; <clinit> failed in plain JVM (no server)
            } catch (LinkageError e) {
                // NoClassDefFoundError and other linkage issues (optional deps)
                missingDep++;
                String m = e.getMessage();
                missingCounts.merge(m == null ? "?" : m, 1, Integer::sum);
            } catch (Throwable t) {
                other++;
                System.err.println("OTHER " + cn + ": " + t.getClass().getName() + ": " + t.getMessage());
            }
        }

        System.out.println("Total=" + bytesByName.size() + " verifiedOK=" + ok
                + " verifyErrors=" + verifyErr + " initErrors=" + initErr
                + " missingDependency=" + missingDep + " other=" + other);
        System.out.println("\n=== VERIFY ERRORS (must be 0) ===");
        for (String s : verifyFails) System.out.println("  " + s);
        System.out.println("\n=== Missing dependency counts (acceptable, optional) ===");
        for (Map.Entry<String, Integer> e : missingCounts.entrySet()) {
            System.out.println("  " + e.getKey() + " x" + e.getValue());
        }
        if (verifyErr > 0) System.exit(2);
    }

    private static byte[] readAll(InputStream is) throws IOException {
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        byte[] buf = new byte[8192];
        int n;
        while ((n = is.read(buf)) != -1) bos.write(buf, 0, n);
        return bos.toByteArray();
    }
}
