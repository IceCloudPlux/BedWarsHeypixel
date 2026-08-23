import org.objectweb.asm.*;
import org.objectweb.asm.tree.*;

import java.io.*;
import java.util.*;
import java.util.jar.*;

/**
 * Scan a jar and locate classes containing sensitive strings (URL fragments,
 * base64 blobs), then dump their obfuscated name and member structure.
 *
 * Usage: java FindClass <jar>
 */
public class FindClass {
    public static void main(String[] args) throws Exception {
        String jarPath = args[0];
        try (JarFile jar = new JarFile(jarPath)) {
            Enumeration<JarEntry> en = jar.entries();
            while (en.hasMoreElements()) {
                JarEntry e = en.nextElement();
                if (!e.getName().endsWith(".class") || e.getName().startsWith("META-INF/")) continue;
                byte[] data = readAll(jar.getInputStream(e));
                try {
                    ClassNode cn = new ClassNode(Opcodes.ASM9);
                    new ClassReader(data).accept(cn, 0);
                    Set<String> strs = new TreeSet<>();
                    for (FieldNode fn : cn.fields) {
                        if (fn.value instanceof String) strs.add((String) fn.value);
                    }
                    for (MethodNode mn : cn.methods) {
                        collectStrings(mn, strs);
                    }
                    // sensitive marker: base64 fragments "VEhITE8" / "eWxsMSI" or URL parts
                    boolean hit = false;
                    StringBuilder urlParts = new StringBuilder();
                    for (String s : strs) {
                        if (s.length() >= 4) {
                            // base64 alphabet only
                            if (s.matches("^[A-Za-z0-9+/=]{4,}$")) { hit = true; }
                        }
                        if (s.contains("raw.githubusercontent") || s.contains("github")
                                || s.contains("licenses") || s.contains("http")) {
                            hit = true; urlParts.append(" [" + s + "]");
                        }
                    }
                    if (hit) {
                        System.out.println(">>> " + cn.name + "  members=" + cn.methods.size()
                                + "  (strings=" + strs.size() + ")" + urlParts);
                        System.out.println("    FIELDS: " + cn.fields.stream()
                                .map(f -> f.name + ":" + f.desc).reduce((a,b)->a+", "+b).orElse("none"));
                        System.out.println("    METHODS: " + cn.methods.stream()
                                .map(m -> m.name + m.desc).reduce((a,b)->a+", "+b).orElse("none"));
                        for (String s : strs) {
                            if (s.length() >= 6) System.out.println("      STR: " + s);
                        }
                        System.out.println();
                    }
                } catch (Throwable t) {
                    // skip broken class
                }
            }
        }
    }

    private static void collectStrings(MethodNode mn, Set<String> out) {
        if (mn.instructions == null) return;
        for (AbstractInsnNode in : mn.instructions) {
            if (in instanceof LdcInsnNode) {
                Object c = ((LdcInsnNode) in).cst;
                if (c instanceof String) out.add((String) c);
            }
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
