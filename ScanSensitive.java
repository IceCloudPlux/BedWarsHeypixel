import org.objectweb.asm.*;
import org.objectweb.asm.tree.*;

import java.io.*;
import java.util.*;
import java.util.jar.*;

/** Scan every class in a jar and print LDC strings matching sensitive patterns. */
public class ScanSensitive {
    public static void main(String[] args) throws Exception {
        String jarPath = args[0];
        boolean found = false;
        try (JarFile jar = new JarFile(jarPath)) {
            Enumeration<JarEntry> en = jar.entries();
            while (en.hasMoreElements()) {
                JarEntry e = en.nextElement();
                String n = e.getName();
                if (!n.endsWith(".class") || n.startsWith("META-INF/")) continue;
                byte[] data = readAll(jar.getInputStream(e));
                List<String> hits = scan(data);
                if (!hits.isEmpty()) {
                    found = true;
                    System.out.println("== " + n);
                    for (String h : hits) System.out.println("   " + h);
                }
            }
        }
        System.out.println(found ? "FOUND sensitive strings" : "CLEAN: no sensitive strings anywhere");
    }

    static List<String> scan(byte[] data) {
        List<String> hits = new ArrayList<>();
        try {
            ClassNode cn = new ClassNode(Opcodes.ASM9);
            new ClassReader(data).accept(cn, 0);
            for (MethodNode mn : cn.methods) {
                if (mn.instructions == null) continue;
                for (AbstractInsnNode ins : mn.instructions) {
                    if (ins instanceof LdcInsnNode) {
                        Object c = ((LdcInsnNode) ins).cst;
                        if (c instanceof String) check((String) c, hits);
                    }
                }
            }
            for (FieldNode fn : cn.fields) {
                if (fn.value instanceof String) check((String) fn.value, hits);
            }
        } catch (Throwable ignored) {}
        return hits;
    }

    static void check(String s, List<String> hits) {
        String low = s.toLowerCase();
        boolean suspicious = low.contains("http") || low.contains("github")
                || low.contains("license") || low.contains("licenses")
                || low.contains("user-agent") || low.contains("useragent")
                || low.equals("get") || low.equals("post")
                || low.contains("raw.githubusercontent")
                || low.contains("aef0b") || low.contains("xhro") || low.contains("xgttp")
                || s.length() >= 24 && s.matches("[A-Za-z0-9+/=]{24,}");
        if (suspicious) hits.add("\"" + s + "\"");
    }

    private static byte[] readAll(InputStream is) throws IOException {
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        byte[] buf = new byte[8192];
        int n;
        while ((n = is.read(buf)) != -1) bos.write(buf, 0, n);
        return bos.toByteArray();
    }
}
