package codechicken.multipart.handler;

import java.lang.reflect.Field;
import java.util.Iterator;
import java.util.Map;

import net.minecraft.tileentity.TileEntity;
import net.minecraft.world.ChunkPosition;
import net.minecraft.world.World;
import net.minecraft.world.chunk.Chunk;

import codechicken.lib.asm.ObfMapping;
import codechicken.multipart.MultipartHelper;
import codechicken.multipart.MultipartHelper.IPartTileConverter;
import codechicken.multipart.TMultiPart;
import codechicken.multipart.TileMultipart;
import scala.collection.mutable.MutableList;

/** Scala companion singleton retained for binary compatibility with compiled Scala consumers. */
public final class MultipartSaveLoad$ {

    public static final MultipartSaveLoad$ MODULE$ = new MultipartSaveLoad$();

    private final MutableList<IPartTileConverter<?>> converters = new MutableList<>();
    private World loadingWorld;
    private final Map<Class<? extends TileEntity>, String> classToNameMap = getClassToNameMap();

    private MultipartSaveLoad$() {}

    public MutableList<IPartTileConverter<?>> converters() {
        return converters;
    }

    public World loadingWorld() {
        return loadingWorld;
    }

    public void loadingWorld_$eq(World world) {
        loadingWorld = world;
    }

    public void hookLoader() {
        Map<String, Class<? extends TileEntity>> nameToClass = tileEntityMap("field_145855_i");
        nameToClass.put("savedMultipart", MultipartSaveLoad.TileNBTContainer.class);
    }

    public void registerTileClass(Class<? extends TileEntity> tileClass) {
        classToNameMap.put(tileClass, "savedMultipart");
    }

    public Map<Class<? extends TileEntity>, String> getClassToNameMap() {
        return tileEntityMap("field_145853_j");
    }

    @SuppressWarnings("unchecked")
    private static <K, V> Map<K, V> tileEntityMap(String fieldName) {
        try {
            Field field = TileEntity.class.getDeclaredField(
                    new ObfMapping("net/minecraft/tileentity/TileEntity", fieldName, "Ljava/util/Map;")
                            .toRuntime().s_name);
            field.setAccessible(true);
            return (Map<K, V>) field.get(null);
        } catch (ReflectiveOperationException e) {
            return MultipartSaveLoad$.<Map<K, V>, RuntimeException>throwUnchecked(e);
        }
    }

    @SuppressWarnings("unchecked")
    private static <T, E extends Throwable> T throwUnchecked(Throwable throwable) throws E {
        throw (E) throwable;
    }

    @SuppressWarnings("unchecked")
    public void loadTiles(Chunk chunk) {
        loadingWorld = chunk.worldObj;
        Iterator<Map.Entry<ChunkPosition, TileEntity>> iterator = ((Map<ChunkPosition, TileEntity>) chunk.chunkTileEntityMap)
                .entrySet().iterator();

        while (iterator.hasNext()) {
            Map.Entry<ChunkPosition, TileEntity> entry = iterator.next();
            TileEntity oldTile = entry.getValue();
            TileMultipart replacement;

            if (oldTile instanceof MultipartSaveLoad.TileNBTContainer
                    && "savedMultipart".equals(((MultipartSaveLoad.TileNBTContainer) oldTile).tag().getString("id"))) {
                replacement = TileMultipart.createFromNBT(((MultipartSaveLoad.TileNBTContainer) oldTile).tag());
            } else {
                IPartTileConverter<?> converter = findConverter(oldTile);
                if (converter == null) {
                    continue;
                }

                Iterable<TMultiPart> parts = converter.convert(oldTile);
                replacement = parts.iterator().hasNext() ? MultipartHelper.createTileFromParts(parts) : null;
            }

            if (replacement == null) {
                iterator.remove();
            } else {
                replacement.setWorldObj(oldTile.getWorldObj());
                replacement.validate();
                entry.setValue(replacement);
            }
        }
    }

    private IPartTileConverter<?> findConverter(TileEntity tile) {
        scala.collection.Iterator<IPartTileConverter<?>> iterator = converters.iterator();
        while (iterator.hasNext()) {
            IPartTileConverter<?> converter = iterator.next();
            if (converter.canConvert(tile)) {
                return converter;
            }
        }
        return null;
    }

}
