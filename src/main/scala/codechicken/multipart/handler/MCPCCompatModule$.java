package codechicken.multipart.handler;

import java.lang.reflect.Method;

import net.minecraft.block.Block;
import net.minecraft.world.World;

import scala.runtime.AbstractFunction4;

/** Scala-object-compatible implementation for the optional MCPC placement hook. */
public final class MCPCCompatModule$ {

    public static final MCPCCompatModule$ MODULE$ = new MCPCCompatModule$();

    private MCPCCompatModule$() {}

    public void load() {
        try {
            Method canPlacePart = World.class
                    .getDeclaredMethod("canPlaceMultipart", Block.class, Integer.TYPE, Integer.TYPE, Integer.TYPE);
            MultipartCompatiblity.canAddPart_$eq(new MCPCPlacement(canPlacePart));
        } catch (Exception e) {
            MultipartProxy.logger().error("Failed to integrate MCPC placement hooks", e);
        }
    }

    private static final class MCPCPlacement extends AbstractFunction4<World, Object, Object, Object, Object> {

        private final Method canPlacePart;

        MCPCPlacement(Method canPlacePart) {
            this.canPlacePart = canPlacePart;
        }

        @Override
        public Object apply(World world, Object x, Object y, Object z) {
            try {
                return ((Boolean) canPlacePart.invoke(world, MultipartProxy.block(), x, y, z)).booleanValue();
            } catch (Exception e) {
                return throwUnchecked(e);
            }
        }

        @SuppressWarnings("unchecked")
        private static <E extends Throwable, R> R throwUnchecked(Throwable throwable) throws E {
            throw (E) throwable;
        }
    }
}
