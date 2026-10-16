import java.io.DataInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.stream.Collectors;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

/**
 * Scans a directory of mod jars for constant-pool references into ForgeMultipart's packages.
 *
 * Usage: java AbiScan.java <modsDir> [selfJarNameFragment...]
 *
 * Reports, per consumer jar: inherited types, referenced members with exact JVM descriptors,
 * other referenced types, and string constants that look like reflective lookups.
 */
public final class AbiScan {

    private static final String[] TARGETS = { "codechicken/multipart", "codechicken/microblock" };

    /** owner.name descriptor -> jars that reference it. */
    private static final Map<String, Set<String>> MEMBERS = new TreeMap<>();
    /** type -> jars that extend or implement it. */
    private static final Map<String, Set<String>> INHERITED = new TreeMap<>();
    /** type -> jars that reference it any other way. */
    private static final Map<String, Set<String>> TYPES = new TreeMap<>();
    /** string constant -> jars that embed it. */
    private static final Map<String, Set<String>> STRINGS = new TreeMap<>();
    /** jar -> count of classes touching the targets. */
    private static final Map<String, Integer> HITS = new TreeMap<>();

    public static void main(String[] args) throws IOException {
        Path modsDir = Paths.get(args[0]);
        List<String> self = new ArrayList<>();
        for (int i = 1; i < args.length; i++) {
            self.add(args[i].toLowerCase());
        }

        List<Path> jars;
        try (var stream = Files.walk(modsDir)) {
            jars = stream.filter(p -> p.toString().toLowerCase().endsWith(".jar")).sorted().collect(Collectors.toList());
        }

        int skipped = 0;
        for (Path jar : jars) {
            String name = jar.getFileName().toString();
            if (self.stream().anyMatch(name.toLowerCase()::contains)) {
                skipped++;
                continue;
            }
            scanJar(jar, name);
        }

        System.out.println("Scanned " + (jars.size() - skipped) + " jars (" + skipped + " excluded as ForgeMultipart itself)");
        System.out.println();

        System.out.println("== CONSUMERS ==");
        HITS.entrySet().stream()
                .sorted(Map.Entry.<String, Integer>comparingByValue().reversed())
                .forEach(e -> System.out.println("  " + e.getValue() + "\t" + e.getKey()));
        System.out.println();

        dump("INHERITED TYPES (downstream extends/implements - abstract method descriptors are load-bearing)", INHERITED);
        dump("REFERENCED MEMBERS (exact descriptors that must keep linking)", MEMBERS);
        dump("OTHER REFERENCED TYPES (new/cast/instanceof/field descriptors)", TYPES);
        dump("STRING CONSTANTS (reflection / registration candidates)", STRINGS);
    }

    private static void dump(String title, Map<String, Set<String>> data) {
        System.out.println("== " + title + " == (" + data.size() + ")");
        data.entrySet().stream()
                .sorted(Comparator.<Map.Entry<String, Set<String>>>comparingInt(e -> -e.getValue().size())
                        .thenComparing(Map.Entry::getKey))
                .forEach(e -> System.out.println("  " + e.getKey() + "\n      <- " + String.join(", ", e.getValue())));
        System.out.println();
    }

    private static void scanJar(Path jar, String jarName) {
        try (ZipFile zip = new ZipFile(jar.toFile())) {
            var entries = zip.entries();
            while (entries.hasMoreElements()) {
                ZipEntry entry = entries.nextElement();
                if (!entry.getName().endsWith(".class")) {
                    continue;
                }
                try (InputStream in = zip.getInputStream(entry)) {
                    scanClass(new DataInputStream(in), jarName);
                } catch (Exception ignored) {
                    // Unparseable or non-standard class file; not evidence of a reference.
                }
            }
        } catch (IOException ignored) {
            System.err.println("skip unreadable jar: " + jarName);
        }
    }

    private static void scanClass(DataInputStream in, String jarName) throws IOException {
        if (in.readInt() != 0xCAFEBABE) {
            return;
        }
        in.readUnsignedShort(); // minor
        in.readUnsignedShort(); // major
        int count = in.readUnsignedShort();

        int[] tags = new int[count];
        String[] utf8 = new String[count];
        int[] refA = new int[count];
        int[] refB = new int[count];

        for (int i = 1; i < count; i++) {
            int tag = in.readUnsignedByte();
            tags[i] = tag;
            switch (tag) {
                case 1 -> utf8[i] = in.readUTF();
                case 7, 8, 16, 19, 20 -> refA[i] = in.readUnsignedShort();
                case 15 -> { in.readUnsignedByte(); refA[i] = in.readUnsignedShort(); }
                case 3, 4 -> in.readInt();
                case 5, 6 -> { in.readLong(); i++; }
                case 9, 10, 11, 12, 17, 18 -> { refA[i] = in.readUnsignedShort(); refB[i] = in.readUnsignedShort(); }
                default -> throw new IOException("bad constant pool tag " + tag);
            }
        }

        in.readUnsignedShort(); // access_flags
        int thisClass = in.readUnsignedShort();
        int superClass = in.readUnsignedShort();
        int interfaceCount = in.readUnsignedShort();
        int[] interfaces = new int[interfaceCount];
        for (int i = 0; i < interfaceCount; i++) {
            interfaces[i] = in.readUnsignedShort();
        }

        String owner = className(tags, utf8, refA, thisClass);
        if (owner != null && isTarget(owner)) {
            return; // a repackaged copy of ForgeMultipart, not a consumer
        }

        boolean hit = false;
        Set<String> inheritedHere = new TreeSet<>();
        String superName = className(tags, utf8, refA, superClass);
        if (superName != null && isTarget(superName)) {
            inheritedHere.add(superName);
        }
        for (int itf : interfaces) {
            String name = className(tags, utf8, refA, itf);
            if (name != null && isTarget(name)) {
                inheritedHere.add(name);
            }
        }
        for (String type : inheritedHere) {
            INHERITED.computeIfAbsent(type, k -> new TreeSet<>()).add(jarName);
            hit = true;
        }

        for (int i = 1; i < count; i++) {
            switch (tags[i]) {
                case 7 -> {
                    String name = utf8[refA[i]];
                    if (name != null && isTarget(name) && !inheritedHere.contains(strip(name))) {
                        TYPES.computeIfAbsent(strip(name), k -> new TreeSet<>()).add(jarName);
                        hit = true;
                    }
                }
                case 9, 10, 11 -> {
                    String ownerName = className(tags, utf8, refA, refA[i]);
                    if (ownerName == null || !isTarget(ownerName)) {
                        break;
                    }
                    int nat = refB[i];
                    String member = utf8[refA[nat]] + " " + utf8[refB[nat]];
                    MEMBERS.computeIfAbsent(ownerName + "." + member, k -> new TreeSet<>()).add(jarName);
                    hit = true;
                }
                case 8 -> {
                    String value = utf8[refA[i]];
                    if (value != null && (value.contains("codechicken.multipart") || value.contains("codechicken.microblock")
                            || value.contains("codechicken/multipart") || value.contains("codechicken/microblock"))) {
                        STRINGS.computeIfAbsent(value, k -> new TreeSet<>()).add(jarName);
                        hit = true;
                    }
                }
                default -> { }
            }
        }

        if (hit) {
            HITS.merge(jarName, 1, Integer::sum);
        }
    }

    private static String className(int[] tags, String[] utf8, int[] refA, int index) {
        if (index <= 0 || index >= tags.length || tags[index] != 7) {
            return null;
        }
        String name = utf8[refA[index]];
        return name == null ? null : strip(name);
    }

    /** Array class references are encoded as descriptors; reduce them to the element type. */
    private static String strip(String name) {
        int i = 0;
        while (i < name.length() && name.charAt(i) == '[') {
            i++;
        }
        if (i == 0) {
            return name;
        }
        String rest = name.substring(i);
        if (rest.startsWith("L") && rest.endsWith(";")) {
            rest = rest.substring(1, rest.length() - 1);
        }
        return rest;
    }

    private static boolean isTarget(String name) {
        String stripped = strip(name);
        for (String target : TARGETS) {
            if (stripped.startsWith(target + "/")) {
                return true;
            }
        }
        return false;
    }
}
