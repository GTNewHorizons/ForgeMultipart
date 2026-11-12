package codechicken.multipart.handler;

import net.minecraft.world.World;

import cpw.mods.fml.common.FMLCommonHandler;
import scala.Function4;
import scala.runtime.AbstractFunction4;

/** Scala-object-compatible implementation for the multipart placement compatibility hook. */
public final class MultipartCompatiblity$ {

    public static final MultipartCompatiblity$ MODULE$ = new MultipartCompatiblity$();

    private Function4<World, Object, Object, Object, Object> canAddPart = new AllowPlacement();

    private MultipartCompatiblity$() {}

    public Function4<World, Object, Object, Object, Object> canAddPart() {
        return canAddPart;
    }

    public void canAddPart_$eq(Function4<World, Object, Object, Object, Object> callback) {
        canAddPart = callback;
    }

    public void load() {
        if (FMLCommonHandler.instance().getModName().contains("mcpc")) {
            MCPCCompatModule.load();
        }
    }

    private static final class AllowPlacement extends AbstractFunction4<World, Object, Object, Object, Object> {

        AllowPlacement() {}

        @Override
        public Object apply(World world, Object x, Object y, Object z) {
            return Boolean.TRUE;
        }
    }
}
