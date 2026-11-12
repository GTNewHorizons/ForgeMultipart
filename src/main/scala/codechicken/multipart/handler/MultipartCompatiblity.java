package codechicken.multipart.handler;

import net.minecraft.world.World;

import scala.Function4;

/** Static facade for the multipart placement compatibility hook. */
public final class MultipartCompatiblity {

    private MultipartCompatiblity() {}

    public static Function4<World, Object, Object, Object, Object> canAddPart() {
        return MultipartCompatiblity$.MODULE$.canAddPart();
    }

    public static void canAddPart_$eq(Function4<World, Object, Object, Object, Object> callback) {
        MultipartCompatiblity$.MODULE$.canAddPart_$eq(callback);
    }

    public static void load() {
        MultipartCompatiblity$.MODULE$.load();
    }
}
