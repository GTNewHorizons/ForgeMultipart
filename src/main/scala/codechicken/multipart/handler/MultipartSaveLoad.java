package codechicken.multipart.handler;

import java.util.Map;

import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.world.World;
import net.minecraft.world.chunk.Chunk;

import codechicken.multipart.MultipartHelper.IPartTileConverter;
import scala.collection.mutable.MutableList;

/** Hack due to lack of TileEntityLoadEvent in Forge. */
public final class MultipartSaveLoad {

    private MultipartSaveLoad() {}

    public static MutableList<IPartTileConverter<?>> converters() {
        return MultipartSaveLoad$.MODULE$.converters();
    }

    public static World loadingWorld() {
        return MultipartSaveLoad$.MODULE$.loadingWorld();
    }

    public static void loadingWorld_$eq(World world) {
        MultipartSaveLoad$.MODULE$.loadingWorld_$eq(world);
    }

    public static void hookLoader() {
        MultipartSaveLoad$.MODULE$.hookLoader();
    }

    public static void registerTileClass(Class<? extends TileEntity> tileClass) {
        MultipartSaveLoad$.MODULE$.registerTileClass(tileClass);
    }

    public static Map<Class<? extends TileEntity>, String> getClassToNameMap() {
        return MultipartSaveLoad$.MODULE$.getClassToNameMap();
    }

    public static void loadTiles(Chunk chunk) {
        MultipartSaveLoad$.MODULE$.loadTiles(chunk);
    }

    /** Dummy registered tile that holds multipart NBT until its chunk finishes loading. */
    public static class TileNBTContainer extends TileEntity {

        private NBTTagCompound tag;

        public NBTTagCompound tag() {
            return tag;
        }

        public void tag_$eq(NBTTagCompound tag) {
            this.tag = tag;
        }

        @Override
        public void readFromNBT(NBTTagCompound tag) {
            super.readFromNBT(tag);
            this.tag = tag;
        }
    }
}
