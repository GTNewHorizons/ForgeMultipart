package codechicken.multipart;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.world.World;

import org.junit.jupiter.api.Test;

/**
 * Covers the part of MultipartHelper that does not need a world: the id guard on createTileFromNBT and the whole of
 * IPartTileConverter. registerTileConverter, createTileFromParts and sendDescPacket need the save/load hooks, the ASM
 * generator or a chunk, so the Forge server suite covers those instead.
 */
class MultipartHelperCharacterizationTest {

    /**
     * MultipartSaveLoad cannot class-initialize headless -- it reflects into TileEntity's static maps through
     * ObfMapping. That makes it a usable probe: returning null rather than raising a LinkageError is what proves the id
     * guard short-circuits before the loadingWorld assignment.
     */
    @Test
    void aTagThatIsNotASavedMultipartReturnsNullBeforeTouchingTheLoader() {
        assertNull(MultipartHelper.createTileFromNBT(null, new NBTTagCompound()));

        NBTTagCompound wrongId = new NBTTagCompound();
        wrongId.setString("id", "somethingElse");
        assertNull(MultipartHelper.createTileFromNBT(null, wrongId));
    }

    /** The other half of the same probe: the matching branch does reach the loader. */
    @Test
    void aSavedMultipartTagReachesTheLoader() {
        NBTTagCompound tag = new NBTTagCompound();
        tag.setString("id", "savedMultipart");

        assertThrows(LinkageError.class, () -> MultipartHelper.createTileFromNBT(null, tag));
    }

    @Test
    void aConverterMatchesOnlyInstancesOfItsOwnTileClass() {
        CountingConverter converter = new CountingConverter(TileMultipart.class);

        assertSame(TileMultipart.class, converter.clazz());
        assertTrue(converter.canConvert(new TileMultipart()));
        assertFalse(converter.canConvert(new PlainTile()));
        assertFalse(converter.canConvert(null));
    }

    @Test
    void convertMultiYieldsNothingWhenConvertOneReturnsNull() {
        CountingConverter converter = new CountingConverter(TileMultipart.class);
        converter.result = null;

        assertTrue(drain(converter.convertMulti(new TileMultipart())).isEmpty());
    }

    @Test
    void convertMultiWrapsTheSinglePartConvertOneReturns() {
        CountingConverter converter = new CountingConverter(TileMultipart.class);
        NamedPart part = new NamedPart("only");
        converter.result = part;

        List<TMultiPart> parts = drain(converter.convertMulti(new TileMultipart()));

        assertEquals(1, parts.size());
        assertSame(part, parts.get(0));
    }

    @Test
    void convertCastsTheTileAndRoutesThroughConvertMulti() {
        CountingConverter converter = new CountingConverter(TileMultipart.class);
        TileMultipart tile = new TileMultipart();
        converter.result = new NamedPart("routed");

        List<TMultiPart> parts = drain(converter.convert(tile));

        assertEquals(1, converter.convertMultiCalls);
        assertSame(tile, converter.lastConvertMultiArgument);
        assertEquals(1, parts.size());
    }

    @Test
    void convertFailsOnATileTheConverterDoesNotAccept() {
        CountingConverter converter = new CountingConverter(TileMultipart.class);

        assertThrows(ClassCastException.class, () -> converter.convert(new PlainTile()));
    }

    /**
     * guidenh reflects on both of these names and on nothing else here, so the ABI diff cannot see this break. The two
     * statics are the members other jars link against.
     */
    @Test
    void theNamesAndStaticsDownstreamReflectsOnStillResolve() throws Exception {
        Method createTileFromNBT = MultipartHelper.class
                .getDeclaredMethod("createTileFromNBT", World.class, NBTTagCompound.class);
        assertSame(TileEntity.class, createTileFromNBT.getReturnType());
        assertPublicStatic(createTileFromNBT);

        Method sendDescPacket = MultipartHelper.class
                .getDeclaredMethod("sendDescPacket", World.class, TileEntity.class);
        assertSame(void.class, sendDescPacket.getReturnType());
        assertPublicStatic(sendDescPacket);

        Class<?> companion = Class.forName("codechicken.multipart.MultipartHelper$");
        Field module = companion.getField("MODULE$");
        assertSame(companion, module.getType());
        assertPublicStatic(module);
        assertNotNull(module.get(null));
    }

    private static void assertPublicStatic(Method method) {
        assertTrue(Modifier.isPublic(method.getModifiers()), method + " must stay public");
        assertTrue(Modifier.isStatic(method.getModifiers()), method + " must stay static");
    }

    private static void assertPublicStatic(Field field) {
        assertTrue(Modifier.isPublic(field.getModifiers()), field + " must stay public");
        assertTrue(Modifier.isStatic(field.getModifiers()), field + " must stay static");
    }

    private static List<TMultiPart> drain(Iterable<TMultiPart> parts) {
        List<TMultiPart> drained = new ArrayList<>();
        for (Iterator<TMultiPart> iterator = parts.iterator(); iterator.hasNext();) {
            drained.add(iterator.next());
        }
        return drained;
    }

    private static class CountingConverter extends MultipartHelper.IPartTileConverter<TileMultipart> {

        TMultiPart result;
        int convertMultiCalls;
        TileMultipart lastConvertMultiArgument;

        CountingConverter(Class<TileMultipart> clazz) {
            super(clazz);
        }

        @Override
        public Iterable<TMultiPart> convertMulti(TileMultipart tile) {
            convertMultiCalls++;
            lastConvertMultiArgument = tile;
            return super.convertMulti(tile);
        }

        @Override
        public TMultiPart convertOne(TileMultipart tile) {
            return result;
        }
    }

    private static final class PlainTile extends TileEntity {
    }

    private static final class NamedPart extends TMultiPart {

        private final String type;

        NamedPart(String type) {
            this.type = type;
        }

        @Override
        public String getType() {
            return type;
        }
    }
}
