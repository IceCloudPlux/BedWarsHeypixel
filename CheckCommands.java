import org.objectweb.asm.*;

import java.io.*;
import java.util.*;
import java.util.jar.*;

/**
 * Checks that any class in the obfuscated jar whose superclass chain includes
 * org/bukkit/command/Command keeps the overridden execute()/tabComplete() method
 * names (Bukkit CommandMap dispatches these by name on the Command base class).
 */
public class CheckCommands {

    public static void main(String[] args) throws Exception {
        String jarPath = args[0];
        // build superclass map from jar
        Map<String, String[]> supMap = new HashMap<>(); // class -> [super, ifaces...]
        Map<String, byte[]> classData = new HashMap<>();
        try (JarFile jar = new JarFile(jarPath)) {
            Enumeration<JarEntry> en = jar.entries();
            while (en.hasMoreElements()) {
                JarEntry e = en.nextElement();
                String n = e.getName();
                if (!n.endsWith(".class") || n.startsWith("META-INF/versions/")) continue;
                byte[] bytes = readFully(jar.getInputStream(e));
                classData.put(n, bytes);
                ClassReader cr = new ClassReader(bytes);
                final String[] sup = new String[1];
                final String[][] ifaces = new String[1][];
                cr.accept(new ClassVisitor(Opcodes.ASM9) {
                    @Override public void visit(int v, int a, String name, String sig, String s, String[] i) {
                        sup[0] = s; ifaces[0] = i;
                    }
                }, ClassReader.SKIP_CODE | ClassReader.SKIP_DEBUG | ClassReader.SKIP_FRAMES);
                supMap.put(n, new String[]{sup[0], String.join(",", ifaces[0])});
            }
        }

        // Find classes whose chain reaches org/bukkit/command/Command
        int commandSubs = 0, missingExecute = 0, missingTab = 0;
        for (String name : classData.keySet()) {
            if (!extendsCommand(name, supMap)) continue;
            commandSubs++;
            boolean[] hasExecute = {false}, hasTab = {false};
            byte[] bytes = classData.get(name);
            ClassReader cr = new ClassReader(bytes);
            cr.accept(new ClassVisitor(Opcodes.ASM9) {
                @Override public MethodVisitor visitMethod(int a, String mn, String desc, String s, String[] e) {
                    if (mn.equals("execute") && desc.equals("(Lorg/bukkit/command/CommandSender;Ljava/lang/String;[Ljava/lang/String;)Z"))
                        hasExecute[0] = true;
                    if (mn.equals("tabComplete") && desc.equals("(Lorg/bukkit/command/CommandSender;Ljava/lang/String;[Ljava/lang/String;)Ljava/util/List;"))
                        hasTab[0] = true;
                    return null;
                }
            }, ClassReader.SKIP_CODE | ClassReader.SKIP_DEBUG | ClassReader.SKIP_FRAMES);
            if (!hasExecute[0]) missingExecute++;
            if (!hasTab[0]) missingTab++;
            System.out.println("  command class: " + name + " execute=" + hasExecute[0] + " tabComplete=" + hasTab[0]);
        }
        System.out.println("Command subclasses: " + commandSubs + ", missing execute: " + missingExecute + ", missing tabComplete: " + missingTab);
    }

    static boolean extendsCommand(String name, Map<String, String[]> supMap) {
        String cur = name;
        int depth = 0;
        while (cur != null && depth++ < 30) {
            if (cur.equals("org/bukkit/command/Command")) return true;
            String[] info = supMap.get(cur);
            if (info == null) break;
            cur = info[0];
            if (info.length > 1 && Arrays.asList(info[1].split(",")).contains("org/bukkit/command/Command")) return true;
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
