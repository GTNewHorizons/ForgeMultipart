package codechicken.microblock;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.lang.reflect.Proxy;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

import net.minecraft.block.Block.SoundType;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.IIcon;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.objectweb.asm.Type;

import codechicken.lib.data.MCDataInput;
import codechicken.lib.data.MCDataOutput;
import codechicken.lib.data.MCDataOutputWrapper;
import codechicken.lib.raytracer.IndexedCuboid6;
import codechicken.lib.vec.Cuboid6;
import codechicken.lib.vec.Vector3;
import codechicken.microblock.MicroMaterialRegistry.IMicroMaterial;
import codechicken.microblock.handler.MicroblockProxy;
import codechicken.multipart.JPartialOcclusion;
import codechicken.multipart.TCuboidPart;
import codechicken.multipart.TIconHitEffects;
import codechicken.multipart.TMultiPart;
import codechicken.multipart.TSlottedPart;
import codechicken.multipart.TileMultipart;

class MicroblockCharacterizationTest {

    private static final String MATERIAL_NAME = "test:microblock_base";
    private static final Set<String> BASE_METHODS = signatures(
            "$lessinit$greater$default$1()I",
            "doesTick()Z",
            "drawBreaking(Lnet/minecraft/client/renderer/RenderBlocks;)V",
            "explosionResistance(Lnet/minecraft/entity/Entity;)F",
            "getCollisionBoxes()Ljava/lang/Iterable;",
            "getDrops()Ljava/lang/Iterable;",
            "getDrops()Ljava/util/List;",
            "getIMaterial()Lcodechicken/microblock/MicroMaterialRegistry$IMicroMaterial;",
            "getLightValue()I",
            "getMaterial()I",
            "getShape()I",
            "getSize()I",
            "getStrength(Lnet/minecraft/util/MovingObjectPosition;Lnet/minecraft/entity/player/EntityPlayer;)F",
            "getSubParts()Ljava/lang/Iterable;",
            "getType()Ljava/lang/String;",
            "isTransparent()Z",
            "itemClassID()I",
            "load(Lnet/minecraft/nbt/NBTTagCompound;)V",
            "material()I",
            "material_$eq(I)V",
            "microClass()Lcodechicken/microblock/MicroblockClass;",
            "pickItem(Lnet/minecraft/util/MovingObjectPosition;)Lnet/minecraft/item/ItemStack;",
            "read(Lcodechicken/lib/data/MCDataInput;)V",
            "readDesc(Lcodechicken/lib/data/MCDataInput;)V",
            "save(Lnet/minecraft/nbt/NBTTagCompound;)V",
            "sendShapeUpdate()V",
            "setShape(II)V",
            "shape()B",
            "shape_$eq(B)V",
            "writeDesc(Lcodechicken/lib/data/MCDataOutput;)V");
    private static final Set<String> CLIENT_METHODS = signatures(
            "addDestroyEffects(Lnet/minecraft/client/particle/EffectRenderer;)V",
            "addHitEffects(Lnet/minecraft/util/MovingObjectPosition;Lnet/minecraft/client/particle/EffectRenderer;)V",
            "getBrokenIcon(I)Lnet/minecraft/util/IIcon;",
            "getRenderBounds()Lcodechicken/lib/vec/Cuboid6;",
            "render(Lcodechicken/lib/vec/Vector3;I)V",
            "renderStatic(Lcodechicken/lib/vec/Vector3;I)Z");
    private static final Set<String> COMMON_METHODS = signatures(
            "getPartialOcclusionBoxes()Ljava/util/List;",
            "getSlot()I",
            "getSlotMask()I",
            "itemClassID()I",
            "microClass()Lcodechicken/microblock/CommonMicroClass;");

    private static final TestMaterial MATERIAL = new TestMaterial();
    private static int materialId;
    private static TestMicroClass microClass;

    @BeforeAll
    static void registerMaterialAndAllocateClass() throws Exception {
        if (MicroMaterialRegistry.getMaterial(MissingMicroMaterial.key()) == null) {
            MicroMaterialRegistry.registerMaterial(MissingMicroMaterial$.MODULE$, MissingMicroMaterial.key());
        }
        if (MicroMaterialRegistry.getMaterial(MATERIAL_NAME) == null) {
            MicroMaterialRegistry.registerMaterial(MATERIAL, MATERIAL_NAME);
        }
        MicroMaterialRegistry.setupIDMap();
        materialId = MicroMaterialRegistry.materialID(MATERIAL_NAME);
        microClass = allocateInstance(TestMicroClass.class);
    }

    @Test
    void keepsTheBaseCompanionAndThreeMixinSurfaces() throws Exception {
        assertTrue(Modifier.isPublic(Microblock.class.getModifiers()));
        assertTrue(Modifier.isAbstract(Microblock.class.getModifiers()));
        assertSame(TMultiPart.class, Microblock.class.getSuperclass());
        assertArrayEquals(new Class<?>[] { TCuboidPart.class }, Microblock.class.getInterfaces());
        assertEquals(BASE_METHODS, publicDeclaredMethods(Microblock.class));
        assertEquals(1, Microblock.class.getDeclaredConstructors().length);
        assertTrue(Modifier.isPublic(Microblock.class.getDeclaredConstructor(int.class).getModifiers()));
        assertField(Microblock.class, "material", int.class);
        assertField(Microblock.class, "shape", byte.class);
        assertEquals(2, Microblock.class.getDeclaredFields().length);

        assertTrue(Modifier.isFinal(Microblock$.class.getModifiers()));
        assertEquals(signatures("$lessinit$greater$default$1()I"), publicDeclaredMethods(Microblock$.class));
        Field module = Microblock$.class.getField("MODULE$");
        assertTrue(Modifier.isStatic(module.getModifiers()));
        assertTrue(Modifier.isFinal(module.getModifiers()));
        assertSame(Microblock$.MODULE$, module.get(null));
        assertEquals(0, Microblock.$lessinit$greater$default$1());
        assertEquals(0, Microblock$.MODULE$.$lessinit$greater$default$1());

        assertTrait(
                MicroblockClient.class,
                new Class<?>[] { TIconHitEffects.class, IMicroMaterialRender.class },
                CLIENT_METHODS);
        assertHelper(
                "codechicken.microblock.MicroblockClient$class",
                signatures(
                        "$init$(Lcodechicken/microblock/MicroblockClient;)V",
                        "addDestroyEffects(Lcodechicken/microblock/MicroblockClient;Lnet/minecraft/client/particle/EffectRenderer;)V",
                        "addHitEffects(Lcodechicken/microblock/MicroblockClient;Lnet/minecraft/util/MovingObjectPosition;Lnet/minecraft/client/particle/EffectRenderer;)V",
                        "getBrokenIcon(Lcodechicken/microblock/MicroblockClient;I)Lnet/minecraft/util/IIcon;",
                        "getRenderBounds(Lcodechicken/microblock/MicroblockClient;)Lcodechicken/lib/vec/Cuboid6;",
                        "renderStatic(Lcodechicken/microblock/MicroblockClient;Lcodechicken/lib/vec/Vector3;I)Z"));

        assertTrait(
                CommonMicroblock.class,
                new Class<?>[] { JPartialOcclusion.class, TMicroOcclusion.class, TSlottedPart.class },
                COMMON_METHODS);
        assertHelper(
                "codechicken.microblock.CommonMicroblock$class",
                signatures(
                        "$init$(Lcodechicken/microblock/CommonMicroblock;)V",
                        "getPartialOcclusionBoxes(Lcodechicken/microblock/CommonMicroblock;)Ljava/util/List;",
                        "getSlot(Lcodechicken/microblock/CommonMicroblock;)I",
                        "getSlotMask(Lcodechicken/microblock/CommonMicroblock;)I",
                        "itemClassID(Lcodechicken/microblock/CommonMicroblock;)I"));

        assertTrait(
                CommonMicroblockClient.class,
                new Class<?>[] { CommonMicroblock.class, MicroblockClient.class, TMicroOcclusionClient.class },
                signatures("render(Lcodechicken/lib/vec/Vector3;I)V"));
        assertHelper(
                "codechicken.microblock.CommonMicroblockClient$class",
                signatures(
                        "$init$(Lcodechicken/microblock/CommonMicroblockClient;)V",
                        "render(Lcodechicken/microblock/CommonMicroblockClient;Lcodechicken/lib/vec/Vector3;I)V"));
    }

    @Test
    void keepsStateMaterialGeometryAndItemConversions() {
        TestMicroblock part = new TestMicroblock(materialId);
        assertEquals(materialId, part.material());
        assertEquals(0, part.shape());
        assertFalse(part.doesTick());
        assertSame(MATERIAL, part.getIMaterial());
        assertEquals("test_microblock", part.getType());
        assertEquals(2.5f, part.getStrength(null, null));
        assertTrue(part.isTransparent());
        assertEquals(11, part.getLightValue());
        assertEquals(2f, part.explosionResistance(null));

        part.setShape(7, 3);
        assertEquals((byte) 0x73, part.shape());
        assertEquals(7, part.getSize());
        assertEquals(3, part.getShape());
        IndexedCuboid6 subPart = part.getSubParts().iterator().next();
        assertEquals(0, subPart.data);
        assertNotSame(part.getBounds(), subPart);
        assertSame(part.getBounds(), part.getCollisionBoxes().iterator().next());

        ItemMicroPart previous = MicroblockProxy.itemMicro();
        ItemMicroPart item = new ItemMicroPart();
        MicroblockProxy.itemMicro_$eq(item);
        try {
            List<ItemStack> drops = part.getDrops();
            assertEquals(3, drops.size());
            assertItem(drops.get(0), item, 1, 7 << 8 | 4);
            assertItem(drops.get(1), item, 1, 7 << 8 | 2);
            assertItem(drops.get(2), item, 1, 7 << 8 | 1);

            part.setShape(6, 1);
            assertItem(part.pickItem(null), item, 1, 7 << 8 | 2);
        } finally {
            MicroblockProxy.itemMicro_$eq(previous);
        }

        part.setShape(8, 15);
        assertEquals(-8, part.getSize(), "The signed-byte size behavior is historical and observable");
        assertEquals(15, part.getShape());
    }

    @Test
    void keepsDescriptionNbtAndIncrementalUpdateSemantics() {
        TestMicroblock part = new TestMicroblock(materialId);
        part.setShape(5, 2);

        ByteArrayOutputStream descriptionBytes = new ByteArrayOutputStream();
        part.writeDesc(output(descriptionBytes));
        assertArrayEquals(new byte[] { (byte) materialId, (byte) 0x52 }, descriptionBytes.toByteArray());

        NBTTagCompound tag = new NBTTagCompound();
        part.save(tag);
        assertEquals((byte) 0x52, tag.getByte("shape"));
        assertEquals(MATERIAL_NAME, tag.getString("material"));

        TestMicroblock loaded = new TestMicroblock(MicroMaterialRegistry.getMissingId());
        loaded.load(tag);
        assertEquals((byte) 0x52, loaded.shape());
        assertEquals(materialId, loaded.material());

        RecordingTile tile = new RecordingTile();
        ByteArrayOutputStream updateBytes = new ByteArrayOutputStream();
        tile.output = output(updateBytes);
        loaded.bind(tile);
        loaded.sendShapeUpdate();
        assertArrayEquals(new byte[] { (byte) 0x52 }, updateBytes.toByteArray());

        loaded.read(input((byte) 0x31));
        assertEquals((byte) 0x31, loaded.shape());
        assertEquals(1, tile.renderMarks);
        assertEquals(1, tile.partChanges);
        assertSame(loaded, tile.changedPart);
    }

    private static void assertTrait(Class<?> type, Class<?>[] interfaces, Set<String> methods) {
        assertTrue(type.isInterface());
        assertArrayEquals(interfaces, type.getInterfaces());
        assertEquals(methods, publicDeclaredMethods(type));
    }

    private static void assertHelper(String name, Set<String> methods) throws Exception {
        Class<?> type = Class.forName(name);
        assertTrue(Modifier.isPublic(type.getModifiers()));
        assertTrue(Modifier.isAbstract(type.getModifiers()));
        assertSame(Object.class, type.getSuperclass());
        assertEquals(methods, publicDeclaredMethods(type));
    }

    private static void assertField(Class<?> owner, String name, Class<?> type) throws Exception {
        Field field = owner.getDeclaredField(name);
        assertSame(type, field.getType());
        assertTrue(Modifier.isPrivate(field.getModifiers()));
        assertFalse(Modifier.isFinal(field.getModifiers()));
        assertFalse(Modifier.isStatic(field.getModifiers()));
    }

    private static void assertItem(ItemStack stack, ItemMicroPart item, int amount, int damage) {
        assertSame(item, stack.getItem());
        assertEquals(amount, stack.stackSize);
        assertEquals(damage, stack.getItemDamage());
        assertEquals(MATERIAL_NAME, stack.getTagCompound().getString("mat"));
    }

    private static MCDataOutput output(ByteArrayOutputStream bytes) {
        return new MCDataOutputWrapper(new DataOutputStream(bytes));
    }

    private static MCDataInput input(byte value) {
        DataInputStream data = new DataInputStream(new ByteArrayInputStream(new byte[] { value }));
        return (MCDataInput) Proxy.newProxyInstance(
                MCDataInput.class.getClassLoader(),
                new Class<?>[] { MCDataInput.class },
                (proxy, method, arguments) -> {
                    if (method.getName().equals("readByte")) {
                        return data.readByte();
                    }
                    throw new AssertionError("Unexpected read method: " + method.getName());
                });
    }

    private static Set<String> signatures(String... values) {
        return new TreeSet<>(Arrays.asList(values));
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

    @SuppressWarnings("unchecked")
    private static <T> T allocateInstance(Class<T> type) throws Exception {
        Class<?> unsafe = Class.forName("sun.misc.Unsafe");
        Field field = unsafe.getDeclaredField("theUnsafe");
        field.setAccessible(true);
        return (T) unsafe.getMethod("allocateInstance", Class.class).invoke(field.get(null), type);
    }

    private static final class TestMicroblock extends Microblock {

        private static final Cuboid6 BOUNDS = new Cuboid6(0, 0, 0, 0.5, 0.75, 1);

        private TestMicroblock(int material) {
            super(material);
        }

        @Override
        public MicroblockClass microClass() {
            return microClass;
        }

        @Override
        public int itemClassID() {
            return 7;
        }

        @Override
        public Cuboid6 getBounds() {
            return BOUNDS;
        }
    }

    private static final class TestMicroClass extends MicroblockClass {

        @Override
        public String getName() {
            return "test_microblock";
        }

        @Override
        public Class<? extends Microblock> baseTrait() {
            return TestMicroblock.class;
        }

        @Override
        public Class<? extends MicroblockClient> clientTrait() {
            return MicroblockClient.class;
        }

        @Override
        public float getResistanceFactor() {
            return 0.25f;
        }
    }

    private static final class RecordingTile extends TileMultipart {

        private MCDataOutput output;
        private int renderMarks;
        private int partChanges;
        private TMultiPart changedPart;

        @Override
        public MCDataOutput getWriteStream(TMultiPart part) {
            return output;
        }

        @Override
        public void markRender() {
            renderMarks++;
        }

        @Override
        public void notifyPartChange(TMultiPart part) {
            partChanges++;
            changedPart = part;
        }
    }

    private static final class TestMaterial implements IMicroMaterial {

        @Override
        public IIcon getBreakingIcon(int side) {
            return null;
        }

        @Override
        public void renderMicroFace(Vector3 pos, int pass, Cuboid6 bounds) {}

        @Override
        public boolean isTransparent() {
            return true;
        }

        @Override
        public int getLightValue() {
            return 11;
        }

        @Override
        public float getStrength(EntityPlayer player) {
            return 2.5f;
        }

        @Override
        public String getLocalizedName() {
            return "Test Microblock";
        }

        @Override
        public ItemStack getItem() {
            return null;
        }

        @Override
        public int getCutterStrength() {
            return 0;
        }

        @Override
        public SoundType getSound() {
            return null;
        }

        @Override
        public float explosionResistance(Entity entity) {
            return 8f;
        }
    }
}
