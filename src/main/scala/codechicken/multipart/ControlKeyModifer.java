package codechicken.multipart;

import java.util.HashMap;
import java.util.Map;

import net.minecraft.entity.player.EntityPlayer;

/**
 * A class that maintains a map server&lt;-&gt;client of which players are holding the control (or placement modifier
 * key) much like sneaking.
 * <p>
 * The misspelling in the class name is the published one and is kept deliberately.
 */
public final class ControlKeyModifer {

    private static final Map<EntityPlayer, Boolean> map = new HashMap<>();

    private static boolean clientPressing;

    private ControlKeyModifer() {}

    /** Players the server has been told are holding the key. Absent means not holding. */
    public static Map<EntityPlayer, Boolean> map() {
        return map;
    }

    public static boolean isClientPressing() {
        return clientPressing;
    }

    public static void setClientPressing(boolean pressing) {
        clientPressing = pressing;
    }

    /**
     * On a client world this reports what this client is pressing; on a server world it reports what the given player
     * last told the server.
     */
    public static boolean isControlDown(EntityPlayer p) {
        if (p.worldObj.isRemote) {
            return clientPressing;
        }
        Boolean pressed = map.get(p);
        return pressed != null && pressed;
    }
}
