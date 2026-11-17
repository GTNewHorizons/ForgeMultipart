package codechicken.microblock;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.Arrays;
import java.util.Set;
import java.util.TreeSet;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.util.MovingObjectPosition;
import net.minecraft.world.World;

import org.junit.jupiter.api.Test;
import org.objectweb.asm.Type;

import codechicken.lib.vec.BlockCoord;

class MicroblockPlacementCharacterizationTest {

    private static final Set<String> EXECUTABLE_METHODS = signatures(
            "consume(Lnet/minecraft/world/World;Lnet/minecraft/entity/player/EntityPlayer;Lnet/minecraft/item/ItemStack;)V",
            "part()Lcodechicken/microblock/Microblock;",
            "place(Lnet/minecraft/world/World;Lnet/minecraft/entity/player/EntityPlayer;Lnet/minecraft/item/ItemStack;)V",
            "pos()Lcodechicken/lib/vec/BlockCoord;");
    private static final Set<String> CONCRETE_METHODS = signatures(
            "consume(Lnet/minecraft/world/World;Lnet/minecraft/entity/player/EntityPlayer;Lnet/minecraft/item/ItemStack;)V",
            "place(Lnet/minecraft/world/World;Lnet/minecraft/entity/player/EntityPlayer;Lnet/minecraft/item/ItemStack;)V");
    private static final Set<String> PROPERTY_METHODS = signatures(
            "customPlacement(Lcodechicken/microblock/MicroblockPlacement;)Lcodechicken/microblock/ExecutablePlacement;",
            "expand(II)Z",
            "microClass()Lcodechicken/microblock/MicroblockClass;",
            "opposite(II)I",
            "placementGrid()Lcodechicken/microblock/PlacementGrid;",
            "sneakOpposite(II)Z");
    private static final Set<String> PLACEMENT_METHODS = signatures(
            "apply()Lcodechicken/microblock/ExecutablePlacement;",
            "checkMaterial()Z",
            "create(III)Lcodechicken/microblock/Microblock;",
            "d()D",
            "doExpand()Z",
            "expand(Lcodechicken/microblock/CommonMicroblock;)Lcodechicken/microblock/ExecutablePlacement;",
            "expand(Lcodechicken/microblock/Microblock;Lcodechicken/microblock/Microblock;)Lcodechicken/microblock/ExecutablePlacement;",
            "externalPlacement(I)Lcodechicken/microblock/ExecutablePlacement;",
            "externalPlacement(Lcodechicken/microblock/Microblock;)Lcodechicken/microblock/ExecutablePlacement;",
            "getHitDepth(Lcodechicken/lib/vec/Vector3;I)D",
            "gtile()Lscala/Tuple2;",
            "hit()Lnet/minecraft/util/MovingObjectPosition;",
            "htile()Lcodechicken/multipart/TileMultipart;",
            "internal()Z",
            "internalPlacement(Lcodechicken/multipart/TileMultipart;I)Lcodechicken/microblock/ExecutablePlacement;",
            "internalPlacement(Lcodechicken/multipart/TileMultipart;Lcodechicken/microblock/Microblock;)Lcodechicken/microblock/ExecutablePlacement;",
            "material()I",
            "mcrClass()Lcodechicken/microblock/MicroblockClass;",
            "oppMod()Z",
            "oslot()I",
            "player()Lnet/minecraft/entity/player/EntityPlayer;",
            "pos()Lcodechicken/lib/vec/BlockCoord;",
            "pp()Lcodechicken/microblock/PlacementProperties;",
            "side()I",
            "size()I",
            "slot()I",
            "useOppMod()Z",
            "vhit()Lcodechicken/lib/vec/Vector3;",
            "world()Lnet/minecraft/world/World;");

    @Test
    void keepsTheSixClassHierarchyAndCallableSurface() throws Exception {
        assertClass(ExecutablePlacement.class, Object.class, true, false, EXECUTABLE_METHODS);
        assertClass(AdditionPlacement.class, ExecutablePlacement.class, false, false, CONCRETE_METHODS);
        assertClass(ExpandingPlacement.class, ExecutablePlacement.class, false, false, CONCRETE_METHODS);
        assertClass(PlacementProperties.class, Object.class, true, false, PROPERTY_METHODS);
        assertClass(MicroblockPlacement.class, Object.class, false, false, PLACEMENT_METHODS);
        assertClass(
                MicroblockPlacement$.class,
                Object.class,
                false,
                true,
                signatures(
                        "apply(Lnet/minecraft/entity/player/EntityPlayer;Lnet/minecraft/util/MovingObjectPosition;IIZLcodechicken/microblock/PlacementProperties;)Lcodechicken/microblock/ExecutablePlacement;"));

        assertConstructor(ExecutablePlacement.class, BlockCoord.class, Microblock.class);
        assertConstructor(AdditionPlacement.class, BlockCoord.class, Microblock.class);
        assertConstructor(ExpandingPlacement.class, BlockCoord.class, Microblock.class, Microblock.class);
        assertConstructor(PlacementProperties.class);
        assertConstructor(
                MicroblockPlacement.class,
                EntityPlayer.class,
                MovingObjectPosition.class,
                int.class,
                int.class,
                boolean.class,
                PlacementProperties.class);

        Field module = MicroblockPlacement$.class.getField("MODULE$");
        assertSame(MicroblockPlacement$.class, module.getType());
        assertTrue(Modifier.isStatic(module.getModifiers()));
        assertTrue(Modifier.isFinal(module.getModifiers()));
        assertSame(MicroblockPlacement$.MODULE$, module.get(null));
    }

    @Test
    void keepsTheExactStoredStateShape() throws Exception {
        assertPrivateFinalField(ExecutablePlacement.class, "pos", BlockCoord.class);
        assertPrivateFinalField(ExecutablePlacement.class, "part", Microblock.class);
        assertEquals(0, AdditionPlacement.class.getDeclaredFields().length);
        assertPrivateFinalField(ExpandingPlacement.class, "opart", Microblock.class);

        assertPrivateFinalField(MicroblockPlacement.class, "player", EntityPlayer.class);
        assertPrivateFinalField(MicroblockPlacement.class, "hit", MovingObjectPosition.class);
        assertPrivateFinalField(MicroblockPlacement.class, "size", int.class);
        assertPrivateFinalField(MicroblockPlacement.class, "material", int.class);
        assertPrivateFinalField(MicroblockPlacement.class, "checkMaterial", boolean.class);
        assertPrivateFinalField(MicroblockPlacement.class, "pp", PlacementProperties.class);
        assertPrivateFinalField(MicroblockPlacement.class, "world", World.class);
        assertPrivateFinalField(MicroblockPlacement.class, "mcrClass", MicroblockClass.class);
        assertPrivateFinalField(MicroblockPlacement.class, "pos", BlockCoord.class);
        assertPrivateFinalField(MicroblockPlacement.class, "vhit", codechicken.lib.vec.Vector3.class);
        assertPrivateFinalField(MicroblockPlacement.class, "gtile", scala.Tuple2.class);
        assertPrivateFinalField(MicroblockPlacement.class, "htile", codechicken.multipart.TileMultipart.class);
        assertPrivateFinalField(MicroblockPlacement.class, "slot", int.class);
        assertPrivateFinalField(MicroblockPlacement.class, "oslot", int.class);
        assertPrivateFinalField(MicroblockPlacement.class, "d", double.class);
        assertPrivateFinalField(MicroblockPlacement.class, "useOppMod", boolean.class);
        assertPrivateFinalField(MicroblockPlacement.class, "oppMod", boolean.class);
        assertPrivateFinalField(MicroblockPlacement.class, "internal", boolean.class);
        assertPrivateFinalField(MicroblockPlacement.class, "doExpand", boolean.class);
        assertPrivateFinalField(MicroblockPlacement.class, "side", int.class);
    }

    @Test
    void keepsDefaultPropertiesAndExecutableConsumption() {
        PlacementProperties properties = new PlacementProperties() {

            @Override
            public int opposite(int slot, int side) {
                return slot + side;
            }

            @Override
            public MicroblockClass microClass() {
                return null;
            }

            @Override
            public PlacementGrid placementGrid() {
                return null;
            }
        };
        assertTrue(properties.sneakOpposite(2, 3));
        assertTrue(properties.expand(2, 3));
        assertNull(properties.customPlacement(null));

        BlockCoord pos = new BlockCoord(1, 2, 3);
        AdditionPlacement addition = new AdditionPlacement(pos, null);
        ExpandingPlacement expansion = new ExpandingPlacement(pos, null, null);
        assertSame(pos, addition.pos());
        assertNull(addition.part());
        assertSame(pos, expansion.pos());
        assertNull(expansion.part());

        ItemStack stack = new ItemStack(net.minecraft.init.Items.stick, 4);
        addition.consume(null, null, stack);
        expansion.consume(null, null, stack);
        assertEquals(2, stack.stackSize);
    }

    private static Set<String> signatures(String... signatures) {
        return new TreeSet<>(Arrays.asList(signatures));
    }

    private static void assertClass(Class<?> type, Class<?> superclass, boolean isAbstract, boolean isFinal,
            Set<String> methods) {
        assertTrue(Modifier.isPublic(type.getModifiers()));
        assertEquals(isAbstract, Modifier.isAbstract(type.getModifiers()));
        assertEquals(isFinal, Modifier.isFinal(type.getModifiers()));
        assertSame(superclass, type.getSuperclass());
        assertArrayEquals(new Class<?>[0], type.getInterfaces());
        assertEquals(methods, publicDeclaredMethods(type));
    }

    private static Set<String> publicDeclaredMethods(Class<?> type) {
        Set<String> signatures = new TreeSet<>();
        for (Method method : type.getDeclaredMethods()) {
            if (Modifier.isPublic(method.getModifiers())) {
                signatures.add(method.getName() + Type.getMethodDescriptor(method));
            }
        }
        return signatures;
    }

    private static void assertConstructor(Class<?> type, Class<?>... parameterTypes) throws Exception {
        Constructor<?> constructor = type.getDeclaredConstructor(parameterTypes);
        assertTrue(Modifier.isPublic(constructor.getModifiers()));
        assertEquals(1, type.getDeclaredConstructors().length);
    }

    private static void assertPrivateFinalField(Class<?> owner, String name, Class<?> type) throws Exception {
        Field field = owner.getDeclaredField(name);
        assertSame(type, field.getType());
        assertTrue(Modifier.isPrivate(field.getModifiers()));
        assertTrue(Modifier.isFinal(field.getModifiers()));
    }
}
