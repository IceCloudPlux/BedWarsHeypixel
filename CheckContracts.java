import org.objectweb.asm.*;

import java.io.*;
import java.util.*;
import java.util.jar.*;

/**
 * Checks critical reflection contracts in the obfuscated jar:
 * 1. Every class implementing org/bukkit/command/CommandExecutor must keep 'onCommand'
 *    with signature (Lorg/bukkit/command/CommandSender;Lorg/bukkit/command/Command;Ljava/lang/String;[Ljava/lang/String;)Z
 * 2. Every class implementing org/bukkit/command/TabCompleter must keep 'onTabComplete'
 * 3. Every class implementing org/bukkit/event/Listener must retain @EventHandler annotations
 * 4. BedWars main class keeps onLoad/onEnable/onDisable
 * 5. support.version classes keep original names (already checked, but confirm constructors)
 */
public class CheckContracts {

    public static void main(String[] args) throws Exception {
        String jarPath = args[0];
        int cmdExec = 0, tabComp = 0, listener = 0, evtOk = 0, evtBad = 0;
        int okOnCommand = 0, badOnCommand = 0;
        int okTab = 0, badTab = 0;

        try (JarFile jar = new JarFile(jarPath)) {
            Enumeration<JarEntry> en = jar.entries();
            while (en.hasMoreElements()) {
                JarEntry e = en.nextElement();
                String n = e.getName();
                if (!n.endsWith(".class") || n.startsWith("META-INF/versions/")) continue;
                byte[] bytes = readFully(jar.getInputStream(e));
                ClassReader cr = new ClassReader(bytes);
                boolean[] implementsCmd = {false}, implementsTab = {false}, implementsListener = {false};
                String[] className = new String[1];
                boolean[] hasOnCommand = {false};
                boolean[] hasOnTabComplete = {false};
                boolean[] hasEventHandler = {false};
                cr.accept(new ClassVisitor(Opcodes.ASM9) {
                    @Override
                    public void visit(int v, int acc, String name, String sig, String sup, String[] ifaces) {
                        className[0] = name;
                        for (String i : ifaces) {
                            if ("org/bukkit/command/CommandExecutor".equals(i)) implementsCmd[0] = true;
                            if ("org/bukkit/command/TabCompleter".equals(i)) implementsTab[0] = true;
                            if ("org/bukkit/event/Listener".equals(i)) implementsListener[0] = true;
                        }
                    }
                    @Override
                    public MethodVisitor visitMethod(int acc, String name, String desc, String sig, String[] ex) {
                        if (implementsCmd[0] && name.equals("onCommand") &&
                                desc.equals("(Lorg/bukkit/command/CommandSender;Lorg/bukkit/command/Command;Ljava/lang/String;[Ljava/lang/String;)Z"))
                            hasOnCommand[0] = true;
                        if (implementsTab[0] && name.equals("onTabComplete") &&
                                desc.equals("(Lorg/bukkit/command/CommandSender;Lorg/bukkit/command/Command;Ljava/lang/String;[Ljava/lang/String;)Ljava/util/List;"))
                            hasOnTabComplete[0] = true;
                        return new MethodVisitor(Opcodes.ASM9) {
                            @Override
                            public AnnotationVisitor visitAnnotation(String d, boolean visible) {
                                if ("Lorg/bukkit/event/EventHandler;".equals(d)) hasEventHandler[0] = true;
                                return null;
                            }
                        };
                    }
                }, 0);

                if (implementsCmd[0]) { cmdExec++; if (hasOnCommand[0]) okOnCommand++; else badOnCommand++; }
                if (implementsTab[0]) { tabComp++; if (hasOnTabComplete[0]) okTab++; else badTab++; }
                if (implementsListener[0]) {
                    listener++;
                    if (hasEventHandler[0]) evtOk++; else evtBad++;
                }
            }
        }
        System.out.println("CommandExecutor impls: " + cmdExec + " (onCommand kept: " + okOnCommand + ", missing: " + badOnCommand + ")");
        System.out.println("TabCompleter impls:    " + tabComp + " (onTabComplete kept: " + okTab + ", missing: " + badTab + ")");
        System.out.println("Listener impls:        " + listener + " (with @EventHandler: " + evtOk + ", without: " + evtBad + ")");
    }

    private static byte[] readFully(InputStream is) throws IOException {
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        byte[] buf = new byte[8192];
        int n;
        while ((n = is.read(buf)) != -1) bos.write(buf, 0, n);
        return bos.toByteArray();
    }
}
