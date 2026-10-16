package codechicken.microblock;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.File;
import java.io.FileNotFoundException;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

import net.minecraft.block.Block;
import net.minecraft.block.material.Material;
import net.minecraft.item.Item;
import net.minecraft.item.ItemBlock;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ObjectIntIdentityMap;
import net.minecraft.util.RegistryNamespaced;
import net.minecraft.util.RegistrySimple;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.objectweb.asm.Type;

import codechicken.microblock.handler.MicroblockProxy;
import cpw.mods.fml.common.event.FMLInterModComms.IMCMessage;
import scala.collection.JavaConversions;
import scala.collection.Seq;

class ConfigContentCharacterizationTest {

    private static final int TEST_ID = 3980;
    private static final String TEST_NAME = "configcontent:test_block";
    private static final Block TEST_BLOCK = new Block(Material.rock) {}.setBlockName("configContentTest");
    private static final Item TEST_ITEM = new ItemBlock(TEST_BLOCK);
    private static final Set<String> METHODS = signatures(
            "generateDefault(Ljava/io/File;)V",
            "handleIMC(Lscala/collection/Seq;)V",
            "load()V",
            "loadLine(Ljava/lang/String;)V",
            "loadLines(Ljava/io/File;)V",
            "parse(Ljava/io/File;)V");

    private Logger originalLogger;

    @BeforeEach
    void prepareState() {
        names().clear();
        originalLogger = MicroblockProxy.logger();
        MicroblockProxy.logger_$eq(LogManager.getLogger("ConfigContentCharacterizationTest"));
    }

    @AfterEach
    void restoreState() {
        names().clear();
        MicroblockProxy.logger_$eq(originalLogger);
    }

    @Test
    void keepsTheFacadeCompanionAndMutableMapShape() throws Exception {
        assertTrue(Modifier.isPublic(ConfigContent.class.getModifiers()));
        assertTrue(Modifier.isFinal(ConfigContent.class.getModifiers()));
        assertEquals(METHODS, publicDeclaredMethods(ConfigContent.class));
        assertEquals(0, ConfigContent.class.getDeclaredFields().length);
        for (Method method : ConfigContent.class.getDeclaredMethods()) {
            assertTrue(Modifier.isStatic(method.getModifiers()), method.toString());
        }

        assertTrue(Modifier.isPublic(ConfigContent$.class.getModifiers()));
        assertTrue(Modifier.isFinal(ConfigContent$.class.getModifiers()));
        Set<String> companionMethods = new TreeSet<>(METHODS);
        companionMethods.add("codechicken$microblock$ConfigContent$$nameMap()Lscala/collection/mutable/Map;");
        assertEquals(companionMethods, publicDeclaredMethods(ConfigContent$.class));

        Field module = ConfigContent$.class.getField("MODULE$");
        assertSame(ConfigContent$.class, module.getType());
        assertTrue(Modifier.isStatic(module.getModifiers()));
        assertTrue(Modifier.isFinal(module.getModifiers()));
        assertSame(ConfigContent$.MODULE$, module.get(null));

        Field map = ConfigContent$.class.getDeclaredField("codechicken$microblock$ConfigContent$$nameMap");
        assertSame(scala.collection.mutable.Map.class, map.getType());
        assertTrue(Modifier.isPrivate(map.getModifiers()));
        assertTrue(Modifier.isFinal(map.getModifiers()));
        assertEquals(2, ConfigContent$.class.getDeclaredFields().length);
    }

    @Test
    void parsesDefaultListsRangesAliasesAndReplacement() {
        ConfigContent.loadLine("# ignored");
        ConfigContent.loadLine("x");
        assertTrue(names().isEmpty());

        ConfigContent.loadLine("\"stone\"");
        ConfigContent.loadLine("\"tile.machine\":1,3-5");
        ConfigContent.loadLine("\"mod:block\":2");
        ConfigContent.loadLine("\"reverse\":5-3");
        ConfigContent.loadLine("\"stone\":7");

        assertEquals(Arrays.asList(7), values("minecraft:stone"));
        assertEquals(Arrays.asList(1, 3, 4, 5), values("tile.machine"));
        assertEquals(Arrays.asList(2), values("mod:block"));
        assertEquals(Collections.emptyList(), values("minecraft:reverse"));
    }

    @Test
    void rejectsTheReferenceMalformedLineShapes() {
        assertMessage("Line must begin with a quote", () -> ConfigContent.loadLine("stone"));
        assertMessage("Unmatched quotes", () -> ConfigContent.loadLine("\"stone"));
        assertMessage("Name must be followed by a colon separator", () -> ConfigContent.loadLine("\"stone\"=1"));
        assertMessage("Invalid - separated range", () -> ConfigContent.loadLine("\"stone\":1-2-3"));
        assertThrows(NumberFormatException.class, () -> ConfigContent.loadLine("\"stone\":nope"));
    }

    @Test
    void generatesAndParsesTheExactDefaultFile(@TempDir Path directory) throws Exception {
        ConfigContent.parse(directory.toFile());
        Path config = directory.resolve("microblocks.cfg");
        assertEquals(
                Arrays.asList(
                        "#Configuration file for adding microblock materials for aesthetic blocks added by mods",
                        "#Each line needs to be of the form <name>:<meta>",
                        "#<name> is the unlocalised name or registry key of the block/item enclosed in quotes. NEI can help you find these",
                        "#<meta> may be ommitted, in which case it defaults to 0, otherwise it can be a number, a comma separated list of numbers, or a dash separated range",
                        "#Ex. \"dirt\" \"minecraft:planks\":3 \"iron_ore\":1,2,3,5 \"ThermalFoundation:Storage\":0-15"),
                Files.readAllLines(config));
        assertTrue(names().isEmpty());

        Files.write(config, Arrays.asList("invalid", "\"stone\":2-3"));
        ConfigContent$.MODULE$.parse(directory.toFile());
        assertEquals(Arrays.asList(2, 3), values("minecraft:stone"));

        File missingParent = directory.resolve("missing").resolve("microblocks.cfg").toFile();
        assertThrows(FileNotFoundException.class, () -> ConfigContent.generateDefault(missingParent));
        assertThrows(FileNotFoundException.class, () -> ConfigContent.loadLines(missingParent));
        assertDoesNotThrow(() -> ConfigContent.parse(directory.resolve("missing").toFile()));
    }

    @Test
    void consumesBothBlockAliasesInOrderAndLeavesUnknownNames() {
        ensureTestBlockRegistered();
        ConfigContent.loadLine("\"tile.configContentTest\":1,2");
        ConfigContent.loadLine("\"" + TEST_NAME + "\":3");
        ConfigContent.loadLine("\"configcontent:missing\":4");

        ConfigContent.load();

        assertMaterial(TEST_NAME + "_1", 1);
        assertMaterial(TEST_NAME + "_2", 2);
        assertMaterial(TEST_NAME + "_3", 3);
        assertEquals(Collections.singleton("configcontent:missing"), names().keySet());
    }

    @Test
    void filtersAndValidatesImcBeforeRegisteringExactItemStacks() throws Exception {
        ensureTestBlockRegistered();
        IMCMessage ignored = message("other", null);
        IMCMessage wrongType = message("microMaterial", "not an item stack");
        IMCMessage invalidMeta = message("microMaterial", new ItemStack(TEST_ITEM, 1, 16));
        IMCMessage invalidBlock = message("microMaterial", new ItemStack(new Item(), 1, 0));
        IMCMessage valid = message("microMaterial", new ItemStack(TEST_ITEM, 1, 7));

        ConfigContent.handleIMC(
                JavaConversions.asScalaBuffer(Arrays.asList(ignored, wrongType, invalidMeta, invalidBlock, valid)));

        assertMaterial(TEST_NAME + "_7", 7);
        assertNull(MicroMaterialRegistry.getMaterial(TEST_NAME + "_16"));
    }

    private static void ensureTestBlockRegistered() {
        Object block = Block.blockRegistry.getObjectById(TEST_ID);
        if (block == null) {
            addRaw(Block.blockRegistry, TEST_ID, TEST_NAME, TEST_BLOCK);
        } else {
            assertSame(TEST_BLOCK, block);
        }

        Object item = Item.itemRegistry.getObjectById(TEST_ID);
        if (item == null) {
            addRaw(Item.itemRegistry, TEST_ID, TEST_NAME, TEST_ITEM);
        } else {
            assertSame(TEST_ITEM, item);
        }
    }

    @SuppressWarnings({ "rawtypes", "unchecked" })
    private static void addRaw(RegistryNamespaced registry, int id, String name, Object value) {
        try {
            Field integerMap = RegistryNamespaced.class.getDeclaredField("underlyingIntegerMap");
            integerMap.setAccessible(true);
            ((ObjectIntIdentityMap) integerMap.get(registry)).func_148746_a(value, id);

            Field names = RegistrySimple.class.getDeclaredField("registryObjects");
            names.setAccessible(true);
            ((Map) names.get(registry)).put(name, value);
        } catch (ReflectiveOperationException exception) {
            throw new AssertionError(exception);
        }
    }

    private static IMCMessage message(String key, Object value) throws Exception {
        Constructor<IMCMessage> constructor = IMCMessage.class.getDeclaredConstructor(String.class, Object.class);
        constructor.setAccessible(true);
        return constructor.newInstance(key, value);
    }

    private static void assertMaterial(String name, int meta) {
        BlockMicroMaterial material = (BlockMicroMaterial) MicroMaterialRegistry.getMaterial(name);
        assertSame(TEST_BLOCK, material.block());
        assertEquals(meta, material.meta());
    }

    private static void assertMessage(String expected, Runnable action) {
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, action::run);
        assertEquals(expected, exception.getMessage());
    }

    private static List<Object> values(String name) {
        return JavaConversions.seqAsJavaList(names().get(name));
    }

    private static Map<String, Seq<Object>> names() {
        return JavaConversions
                .mutableMapAsJavaMap(ConfigContent$.MODULE$.codechicken$microblock$ConfigContent$$nameMap());
    }

    private static Set<String> publicDeclaredMethods(Class<?> type) {
        Set<String> methods = new TreeSet<>();
        for (Method method : type.getDeclaredMethods()) {
            if (Modifier.isPublic(method.getModifiers())) {
                methods.add(method.getName() + Type.getMethodDescriptor(method));
            }
        }
        return methods;
    }

    private static Set<String> signatures(String... signatures) {
        return new TreeSet<>(Arrays.asList(signatures));
    }
}
