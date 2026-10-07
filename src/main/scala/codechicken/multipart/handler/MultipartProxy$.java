package codechicken.multipart.handler;

import net.minecraft.world.ChunkCoordIntPair;

import codechicken.lib.vec.BlockCoord;

public final class MultipartProxy$ extends MultipartProxy_clientImpl {

    public static final MultipartProxy$ MODULE$ = new MultipartProxy$();

    private MultipartProxy$() {}

    public BlockCoord indexInChunk(ChunkCoordIntPair chunk, int index) {
        return new BlockCoord(
                chunk.chunkXPos << 4 | index & 0xF,
                index >> 8 & 0xFF,
                chunk.chunkZPos << 4 | (index & 0xF0) >> 4);
    }

    public int indexInChunk(BlockCoord pos) {
        return pos.x & 0xF | pos.y << 8 | (pos.z & 0xF) << 4;
    }
}
