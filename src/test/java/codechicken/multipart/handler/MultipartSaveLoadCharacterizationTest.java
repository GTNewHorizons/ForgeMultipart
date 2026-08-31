package codechicken.multipart.handler;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.Arrays;
import java.util.Set;
import java.util.TreeSet;

import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.tileentity.TileEntity;

import org.junit.jupiter.api.Test;
import org.objectweb.asm.Type;

class MultipartSaveLoadCharacterizationTest {

    private static final Set<String> METHODS = new TreeSet<>(
            Arrays.asList(
                    "converters()Lscala/collection/mutable/MutableList;",
                    "getClassToNameMap()Ljava/util/Map;",
                    "hookLoader()V",
                    "loadTiles(Lnet/minecraft/world/chunk/Chunk;)V",
                    "loadingWorld()Lnet/minecraft/world/World;",
                    "loadingWorld_$eq(Lnet/minecraft/world/World;)V",
                    "registerTileClass(Ljava/lang/Class;)V"));

    @Test
    void keepsTheStaticFacadeAndLoadBearingCompanion() throws Exception {
        Class<?> facade = load("codechicken.multipart.handler.MultipartSaveLoad");
        Class<?> companion = load("codechicken.multipart.handler.MultipartSaveLoad$");

        assertTrue(Modifier.isPublic(facade.getModifiers()));
        assertTrue(Modifier.isFinal(facade.getModifiers()));
        assertEquals(METHODS, publicMethodSignatures(facade));
        for (Method method : facade.getDeclaredMethods()) {
            if (Modifier.isPublic(method.getModifiers())) {
                assertTrue(Modifier.isStatic(method.getModifiers()), method.toString());
            }
        }

        assertTrue(Modifier.isPublic(companion.getModifiers()));
        assertTrue(Modifier.isFinal(companion.getModifiers()));
        assertSame(Object.class, companion.getSuperclass());
        assertArrayEquals(new Class<?>[0], companion.getInterfaces());
        assertEquals(METHODS, publicMethodSignatures(companion));
        for (Method method : companion.getDeclaredMethods()) {
            if (Modifier.isPublic(method.getModifiers())) {
                assertFalse(Modifier.isStatic(method.getModifiers()), method.toString());
            }
        }

        Field module = companion.getField("MODULE$");
        assertSame(companion, module.getType());
        assertTrue(Modifier.isStatic(module.getModifiers()));
        assertTrue(Modifier.isFinal(module.getModifiers()));
    }

    @Test
    void keepsTheCompanionStateFields() throws Exception {
        Class<?> companion = load("codechicken.multipart.handler.MultipartSaveLoad$");

        assertField(companion, "converters", "scala.collection.mutable.MutableList", true);
        assertField(companion, "loadingWorld", "net.minecraft.world.World", false);
        assertField(companion, "classToNameMap", "java.util.Map", true);
    }

    @Test
    void keepsTheNbtContainerShapeAndReadBehavior() throws Exception {
        Class<?> type = load("codechicken.multipart.handler.MultipartSaveLoad$TileNBTContainer");
        assertTrue(Modifier.isPublic(type.getModifiers()));
        assertTrue(Modifier.isStatic(type.getModifiers()));
        assertFalse(Modifier.isFinal(type.getModifiers()));
        assertSame(load("codechicken.multipart.handler.MultipartSaveLoad"), type.getEnclosingClass());
        assertSame(TileEntity.class, type.getSuperclass());
        assertArrayEquals(new Class<?>[0], type.getInterfaces());
        assertEquals(
                new TreeSet<>(
                        Arrays.asList(
                                "readFromNBT(Lnet/minecraft/nbt/NBTTagCompound;)V",
                                "tag()Lnet/minecraft/nbt/NBTTagCompound;",
                                "tag_$eq(Lnet/minecraft/nbt/NBTTagCompound;)V")),
                publicMethodSignatures(type));

        Field field = type.getDeclaredField("tag");
        assertSame(NBTTagCompound.class, field.getType());
        assertTrue(Modifier.isPrivate(field.getModifiers()));
        assertFalse(Modifier.isFinal(field.getModifiers()));

        TileEntity tile = (TileEntity) type.getDeclaredConstructor().newInstance();
        NBTTagCompound tag = new NBTTagCompound();
        tag.setInteger("x", 12);
        tag.setInteger("y", 34);
        tag.setInteger("z", 56);
        type.getMethod("readFromNBT", NBTTagCompound.class).invoke(tile, tag);

        assertEquals(12, tile.xCoord);
        assertEquals(34, tile.yCoord);
        assertEquals(56, tile.zCoord);
        assertSame(tag, type.getMethod("tag").invoke(tile));
    }

    private static Class<?> load(String name) throws ClassNotFoundException {
        return Class.forName(name, false, MultipartSaveLoadCharacterizationTest.class.getClassLoader());
    }

    private static void assertField(Class<?> owner, String name, String typeName, boolean isFinal) throws Exception {
        Field field = owner.getDeclaredField(name);
        assertEquals(typeName, field.getType().getName());
        assertTrue(Modifier.isPrivate(field.getModifiers()));
        assertEquals(isFinal, Modifier.isFinal(field.getModifiers()));
    }

    private static Set<String> publicMethodSignatures(Class<?> type) {
        Set<String> signatures = new TreeSet<>();
        for (Method method : type.getDeclaredMethods()) {
            if (Modifier.isPublic(method.getModifiers())) {
                signatures.add(method.getName() + Type.getMethodDescriptor(method));
            }
        }
        return signatures;
    }
}
