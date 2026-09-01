package codechicken.microblock;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.item.ItemStack;
import net.minecraftforge.client.IItemRenderer;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.FieldInsnNode;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.MethodNode;

import codechicken.lib.config.ConfigFile;
import codechicken.lib.config.ConfigTag;
import codechicken.lib.render.CCModel;

class ItemSawCharacterizationTest {

    private static final Set<String> ITEM_METHODS = signatures(
            "doesContainerItemLeaveCraftingGrid(Lnet/minecraft/item/ItemStack;)Z",
            "getContainerItem(Lnet/minecraft/item/ItemStack;)Lnet/minecraft/item/ItemStack;",
            "getCuttingStrength(Lnet/minecraft/item/ItemStack;)I",
            "harvestLevel()I",
            "hasContainerItem()Z");
    private static final Set<String> RENDERER_METHODS = signatures(
            "blade()Lcodechicken/lib/render/CCModel;",
            "handle()Lcodechicken/lib/render/CCModel;",
            "handleRenderType(Lnet/minecraft/item/ItemStack;Lnet/minecraftforge/client/IItemRenderer$ItemRenderType;)Z",
            "holder()Lcodechicken/lib/render/CCModel;",
            "models()Ljava/util/Map;",
            "renderItem(Lnet/minecraftforge/client/IItemRenderer$ItemRenderType;Lnet/minecraft/item/ItemStack;Lscala/collection/Seq;)V",
            "shouldUseRenderHelper(Lnet/minecraftforge/client/IItemRenderer$ItemRenderType;Lnet/minecraft/item/ItemStack;Lnet/minecraftforge/client/IItemRenderer$ItemRendererHelper;)Z");

    @Test
    void keepsItemAndRendererSurfaces() throws Exception {
        assertTrue(Modifier.isPublic(ItemSaw.class.getModifiers()));
        assertFalse(Modifier.isFinal(ItemSaw.class.getModifiers()));
        assertEquals(ITEM_METHODS, publicDeclaredMethods(ItemSaw.class));
        assertEquals(Arrays.asList(Saw.class), Arrays.asList(ItemSaw.class.getInterfaces()));

        Constructor<?>[] constructors = ItemSaw.class.getDeclaredConstructors();
        assertEquals(1, constructors.length);
        assertTrue(Modifier.isPublic(constructors[0].getModifiers()));
        assertEquals(Arrays.asList(ConfigTag.class, int.class), Arrays.asList(constructors[0].getParameterTypes()));

        Field harvestLevel = ItemSaw.class.getDeclaredField("harvestLevel");
        assertSame(int.class, harvestLevel.getType());
        assertTrue(Modifier.isPrivate(harvestLevel.getModifiers()));
        assertTrue(Modifier.isFinal(harvestLevel.getModifiers()));
        assertEquals(1, ItemSaw.class.getDeclaredFields().length);

        assertTrue(Modifier.isPublic(ItemSawRenderer.class.getModifiers()));
        assertTrue(Modifier.isFinal(ItemSawRenderer.class.getModifiers()));
        assertEquals(RENDERER_METHODS, publicDeclaredMethods(ItemSawRenderer.class));
        assertEquals(0, ItemSawRenderer.class.getDeclaredFields().length);
        for (Method method : ItemSawRenderer.class.getDeclaredMethods()) {
            assertTrue(Modifier.isStatic(method.getModifiers()), method.toString());
        }

        assertTrue(Modifier.isPublic(ItemSawRenderer$.class.getModifiers()));
        assertTrue(Modifier.isFinal(ItemSawRenderer$.class.getModifiers()));
        assertEquals(Arrays.asList(IItemRenderer.class), Arrays.asList(ItemSawRenderer$.class.getInterfaces()));
        Set<String> companionMethods = new TreeSet<>(RENDERER_METHODS);
        companionMethods.add(
                "renderItem(Lnet/minecraftforge/client/IItemRenderer$ItemRenderType;Lnet/minecraft/item/ItemStack;[Ljava/lang/Object;)V");
        assertEquals(companionMethods, publicDeclaredMethods(ItemSawRenderer$.class));

        assertField(
                ItemSawRenderer$.class,
                "MODULE$",
                ItemSawRenderer$.class,
                Modifier.PUBLIC | Modifier.STATIC | Modifier.FINAL);
        assertField(ItemSawRenderer$.class, "models", Map.class, Modifier.PRIVATE | Modifier.FINAL);
        assertField(ItemSawRenderer$.class, "handle", CCModel.class, Modifier.PRIVATE | Modifier.FINAL);
        assertField(ItemSawRenderer$.class, "holder", CCModel.class, Modifier.PRIVATE | Modifier.FINAL);
        assertField(ItemSawRenderer$.class, "blade", CCModel.class, Modifier.PRIVATE | Modifier.FINAL);
        assertEquals(5, ItemSawRenderer$.class.getDeclaredFields().length);
    }

    @Test
    void configuresDefaultAndExplicitDurability(@TempDir Path directory) {
        ItemSaw defaultSaw = saw(directory, "default", 3, null);
        assertEquals(3, defaultSaw.harvestLevel());
        assertEquals(3, defaultSaw.getCuttingStrength(new ItemStack(defaultSaw)));
        assertEquals(3, defaultSaw.getMaxCuttingStrength());
        assertEquals(1 << 11, defaultSaw.getMaxDamage());
        assertEquals(1, defaultSaw.getItemStackLimit());
        assertSame(CreativeTabs.tabTools, defaultSaw.getCreativeTab());
        assertFalse(defaultSaw.isRepairable());
        assertTrue(defaultSaw.hasContainerItem());

        ItemSaw explicitSaw = saw(directory, "explicit", 1, 37);
        assertEquals(37, explicitSaw.getMaxDamage());
    }

    @Test
    void damagesContainerOnlyWhenTheSawIsDamageable(@TempDir Path directory) {
        ItemSaw damageableSaw = saw(directory, "damageable", 2, 20);
        ItemStack input = new ItemStack(damageableSaw, 4, 7);
        ItemStack container = damageableSaw.getContainerItem(input);
        assertNotSame(input, container);
        assertSame(damageableSaw, container.getItem());
        assertEquals(1, container.stackSize);
        assertEquals(8, container.getItemDamage());
        assertFalse(damageableSaw.doesContainerItemLeaveCraftingGrid(input));

        ItemSaw reusableSaw = saw(directory, "reusable", 2, 0);
        ItemStack reusable = new ItemStack(reusableSaw, 3, 4);
        assertFalse(reusableSaw.isDamageable());
        assertSame(reusable, reusableSaw.getContainerItem(reusable));
    }

    @Test
    void keepsRendererSelectionAndUnsupportedTypeNoOp() throws IOException {
        ClassNode type = new ClassNode();
        new ClassReader(ItemSawRenderer$.class.getName()).accept(type, 0);

        MethodNode handles = method(
                type,
                "handleRenderType",
                "(Lnet/minecraft/item/ItemStack;Lnet/minecraftforge/client/IItemRenderer$ItemRenderType;)Z");
        assertTrue(hasCall(handles, "codechicken/microblock/handler/MicroblockProxy", "useSawIcons", "()Z"));
        assertTrue(
                hasCall(
                        handles,
                        "codechicken/lib/render/TextureUtils",
                        "isMissing",
                        "(Lnet/minecraft/util/IIcon;Lnet/minecraft/util/ResourceLocation;)Z"));

        MethodNode helper = method(
                type,
                "shouldUseRenderHelper",
                "(Lnet/minecraftforge/client/IItemRenderer$ItemRenderType;Lnet/minecraft/item/ItemStack;Lnet/minecraftforge/client/IItemRenderer$ItemRendererHelper;)Z");
        assertEquals(1, countOpcode(helper, Opcodes.ICONST_1));
        assertEquals(1, countOpcode(helper, Opcodes.IRETURN));

        MethodNode render = method(
                type,
                "renderItem",
                "(Lnet/minecraftforge/client/IItemRenderer$ItemRenderType;Lnet/minecraft/item/ItemStack;Lscala/collection/Seq;)V");
        String enumOwner = "net/minecraftforge/client/IItemRenderer$ItemRenderType";
        String enumDescriptor = "L" + enumOwner + ";";
        for (String renderType : Arrays.asList("INVENTORY", "ENTITY", "EQUIPPED_FIRST_PERSON", "EQUIPPED")) {
            assertTrue(hasField(render, enumOwner, renderType, enumDescriptor));
        }
        assertFalse(hasField(render, enumOwner, "FIRST_PERSON_MAP", enumDescriptor));
        assertEquals(2, countOpcode(render, Opcodes.RETURN));
    }

    private static ItemSaw saw(Path directory, String name, int harvestLevel, Integer durability) {
        ConfigFile config = new ConfigFile(directory.resolve(name + ".cfg").toFile());
        ConfigTag sawTag = config.getTag("saw").useBraces();
        if (durability != null) {
            sawTag.getTag("durability").setIntValue(durability);
        }
        return new ItemSaw(sawTag, harvestLevel);
    }

    private static void assertField(Class<?> owner, String name, Class<?> type, int modifiers) throws Exception {
        Field field = owner.getDeclaredField(name);
        assertSame(type, field.getType());
        assertEquals(modifiers, field.getModifiers());
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

    private static MethodNode method(ClassNode type, String name, String descriptor) {
        for (MethodNode method : type.methods) {
            if (name.equals(method.name) && descriptor.equals(method.desc)) {
                return method;
            }
        }
        throw new AssertionError("Missing method " + name + descriptor);
    }

    private static boolean hasField(MethodNode method, String owner, String name, String descriptor) {
        for (AbstractInsnNode instruction : method.instructions.toArray()) {
            if (instruction instanceof FieldInsnNode) {
                FieldInsnNode field = (FieldInsnNode) instruction;
                if (owner.equals(field.owner) && name.equals(field.name) && descriptor.equals(field.desc)) {
                    return true;
                }
            }
        }
        return false;
    }

    private static boolean hasCall(MethodNode method, String owner, String name, String descriptor) {
        for (AbstractInsnNode instruction : method.instructions.toArray()) {
            if (instruction instanceof MethodInsnNode) {
                MethodInsnNode call = (MethodInsnNode) instruction;
                if (owner.equals(call.owner) && name.equals(call.name) && descriptor.equals(call.desc)) {
                    return true;
                }
            }
        }
        return false;
    }

    private static int countOpcode(MethodNode method, int opcode) {
        int count = 0;
        for (AbstractInsnNode instruction : method.instructions.toArray()) {
            if (instruction.getOpcode() == opcode) {
                count++;
            }
        }
        return count;
    }
}
