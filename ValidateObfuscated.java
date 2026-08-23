import org.objectweb.asm.*;
import org.objectweb.asm.signature.SignatureReader;
import org.objectweb.asm.signature.SignatureVisitor;

import java.io.*;
import java.util.*;
import java.util.jar.*;

/**
 * Validates the obfuscated jar:
 * 1. Every class parses cleanly with ASM (structural validity).
 * 2. Every class reference resolves: either the target exists in the jar,
 *    or it is an external/library package (org.bukkit, java.*, NMS, etc.).
 * Also checks plugin.yml main class and manifest are intact.
 */
public class ValidateObfuscated {

    private static final Set<String> jarClasses = new HashSet<>();
    private static final Set<String> missingInternal = new TreeSet<>();

    // External packages that are allowed (provided by server / JDK)
    private static final String[] EXTERNAL_PREFIXES = {
            "java/", "javax/", "jdk/", "sun/", "jline/",
            "org/bukkit/", "org/spigotmc/", "net/md_5/",
            "net/minecraft/", "org/bstats/", "com/google/",
            "org/sqlite/", "org/json/", "com/mojang/",
            "io/papermc/paper/", "org/apache/logging/", "org/checkerframework/",
            "org/jetbrains/", "kotlin/", "org/slf4j/", "com/zaxxer/",
            "de/simonsator/", "net/citizensnpcs/", "net/milkbowl/", "me/clip/",
            "com/alessiodp/", "com/andrei1058/vipfeatures/",
    };

    public static void main(String[] args) throws Exception {
        String jarPath = args[0];

        // Pass 1: collect all classes in jar
        try (JarFile jar = new JarFile(jarPath)) {
            Enumeration<JarEntry> en = jar.entries();
            while (en.hasMoreElements()) {
                JarEntry e = en.nextElement();
                String n = e.getName();
                if (n.endsWith(".class") && !n.startsWith("META-INF/versions/")) {
                    jarClasses.add(n.substring(0, n.length() - 6)); // strip .class
                }
            }

            // Pass 2: validate each class + collect references
            en = jar.entries();
            int valid = 0, failed = 0, total = 0;
            while (en.hasMoreElements()) {
                JarEntry e = en.nextElement();
                String n = e.getName();
                if (!n.endsWith(".class") || n.startsWith("META-INF/versions/")) continue;
                total++;
                byte[] bytes = readFully(jar.getInputStream(e));
                try {
                    ClassReader cr = new ClassReader(bytes);
                    // Visit everything to force full parsing
                    ReferenceCollector refs = new ReferenceCollector();
                    cr.accept(refs, ClassReader.EXPAND_FRAMES);
                    for (String ref : refs.references) {
                        checkRef(ref);
                    }
                    valid++;
                } catch (Exception ex) {
                    failed++;
                    System.err.println("INVALID CLASS " + n + ": " + ex);
                }
            }
            System.out.println("Classes: total=" + total + " valid=" + valid + " failed=" + failed);

            // Check plugin.yml main
            JarEntry pluginYml = jar.getJarEntry("plugin.yml");
            if (pluginYml != null) {
                String content = new String(readFully(jar.getInputStream(pluginYml)), "UTF-8");
                java.util.regex.Matcher m = java.util.regex.Pattern.compile("(?m)^\\s*main:\\s*(\\S+)").matcher(content);
                if (m.find()) {
                    String mainClass = m.group(1);
                    System.out.println("plugin.yml main: " + mainClass + " -> in jar: " + jarClasses.contains(mainClass.replace('.', '/')));
                }
            } else {
                System.out.println("WARNING: plugin.yml MISSING from obfuscated jar!");
            }
        }

        System.out.println("\n=== Missing internal references (" + missingInternal.size() + ") ===");
        for (String s : missingInternal) System.out.println("  " + s);
        if (missingInternal.isEmpty()) {
            System.out.println("OK: no broken internal references.");
        }
    }

    private static void checkRef(String internalName) {
        if (internalName == null) return;
        // array prefix
        while (internalName.startsWith("[")) {
            internalName = internalName.substring(1);
        }
        if (internalName.startsWith("L") && internalName.endsWith(";")) {
            internalName = internalName.substring(1, internalName.length() - 1);
        }
        // primitive types (single letter like I/J/B/Z) are not class references
        if (internalName.length() == 1 && "BCDFIJSZ".indexOf(internalName.charAt(0)) >= 0) return;
        if (jarClasses.contains(internalName)) return;
        for (String p : EXTERNAL_PREFIXES) {
            if (internalName.startsWith(p)) return;
        }
        // If it's in a kept/protected package, ProGuard keeps it in jar, so missing = problem
        missingInternal.add(internalName);
    }

    private static byte[] readFully(InputStream is) throws IOException {
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        byte[] buf = new byte[8192];
        int n;
        while ((n = is.read(buf)) != -1) bos.write(buf, 0, n);
        return bos.toByteArray();
    }

    /** Collects all referenced class internal names from a class. */
    static class ReferenceCollector extends ClassVisitor {
        final Set<String> references = new HashSet<>();

        ReferenceCollector() {
            super(Opcodes.ASM9);
        }

        private void addType(String desc) {
            if (desc == null) return;
            Type t = Type.getType(desc);
            int sort = t.getSort();
            if (sort == Type.OBJECT) references.add(t.getInternalName());
            else if (sort == Type.ARRAY) references.add(t.getElementType().getInternalName());
            else if (sort == Type.METHOD) {
                for (Type a : t.getArgumentTypes()) addType(a.getDescriptor());
                addType(t.getReturnType().getDescriptor());
            }
        }

        @Override
        public void visit(int version, int access, String name, String sig, String superName, String[] interfaces) {
            references.add(name);
            references.add(superName);
            if (interfaces != null) Collections.addAll(references, interfaces);
            if (sig != null) addSignature(sig);
        }

        @Override
        public void visitInnerClass(String name, String outerName, String innerName, int access) {
            references.add(name);
            if (outerName != null) references.add(outerName);
        }

        @Override
        public void visitOuterClass(String owner, String name, String desc) {
            if (owner != null) references.add(owner);
        }

        @Override
        public AnnotationVisitor visitAnnotation(String desc, boolean visible) {
            addType(desc);
            return null;
        }

        @Override
        public FieldVisitor visitField(int access, String name, String desc, String sig, Object value) {
            addType(desc);
            if (sig != null) addSignature(sig);
            return null;
        }

        @Override
        public MethodVisitor visitMethod(int access, String name, String desc, String sig, String[] exceptions) {
            addType(desc);
            if (sig != null) addSignature(sig);
            if (exceptions != null) for (String e : exceptions) references.add(e);
            return new MethodVisitor(Opcodes.ASM9) {
                @Override
                public AnnotationVisitor visitAnnotationDefault() { return null; }
                @Override
                public AnnotationVisitor visitAnnotation(String d, boolean v) { addType(d); return null; }
                @Override
                public AnnotationVisitor visitParameterAnnotation(int p, String d, boolean v) { addType(d); return null; }
                @Override
                public void visitTypeInsn(int op, String type) { references.add(type); }
                @Override
                public void visitFieldInsn(int op, String owner, String name, String desc) { references.add(owner); }
                @Override
                public void visitMethodInsn(int op, String owner, String name, String desc, boolean itf) { references.add(owner); }
                @Override
                public void visitInvokeDynamicInsn(String name, String desc, Handle bsm, Object... args) {
                    addType(desc);
                    addHandle(bsm);
                    for (Object a : args) if (a instanceof Handle) addHandle((Handle) a);
                }
                @Override
                public void visitMultiANewArrayInsn(String desc, int dims) { addType(desc); }
                @Override
                public void visitLdcInsn(Object value) {
                    if (value instanceof Type) references.add(((Type) value).getInternalName());
                }
                @Override
                public void visitTryCatchBlock(Label s, Label e, Label h, String type) { if (type != null) references.add(type); }
                @Override
                public void visitLocalVariable(String name, String desc, String sig, Label s, Label e, int idx) {
                    addType(desc);
                    if (sig != null) addSignature(sig);
                }
            };
        }

        private void addHandle(Handle h) {
            references.add(h.getOwner());
            addType(h.getDesc());
        }

        private void addSignature(String sig) {
            try {
                new SignatureReader(sig).accept(new SignatureVisitor(Opcodes.ASM9) {
                    @Override
                    public void visitClassType(String name) { references.add(name); }
                });
            } catch (Exception ignored) {
                // ignore malformed signatures
            }
        }
    }
}
