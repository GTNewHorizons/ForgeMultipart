package codechicken.multipart.test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.WorldServer;
import net.minecraftforge.common.util.FakePlayerFactory;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import codechicken.multipart.ControlKeyModifer;

/**
 * isControlDown needs a player with a world to pick its branch. On a dedicated server the world is never remote, so
 * these cover the server half: the map the client keeps in sync through packet 1. The client half reads
 * isClientPressing instead and no test harness has a client.
 */
class ControlKeyModiferFunctionalTest {

    @BeforeEach
    @AfterEach
    void clearRecordedPlayers() {
        clearAll();
    }

    @Test
    void aPlayerTheServerHasNotHeardFromIsNotHoldingTheKey() {
        assertFalse(ControlKeyModifer.isControlDown(player()));
    }

    @Test
    void reflectsWhatTheServerLastRecordedForThePlayer() {
        EntityPlayer player = player();

        record(player, true);
        assertTrue(ControlKeyModifer.isControlDown(player));

        record(player, false);
        assertFalse(ControlKeyModifer.isControlDown(player));
    }

    /** Server stop clears the map, so a player must not carry a held key into the next run. */
    @Test
    void clearingResetsThePlayerToNotHolding() {
        EntityPlayer player = player();
        record(player, true);

        clearAll();

        assertFalse(ControlKeyModifer.isControlDown(player));
    }

    // The two adapters below are the only places the map's collection type is named.

    private static void record(EntityPlayer player, boolean pressed) {
        ControlKeyModifer.map().put(player, pressed);
    }

    private static void clearAll() {
        ControlKeyModifer.map().clear();
    }

    private static EntityPlayer player() {
        WorldServer world = MinecraftServer.getServer().worldServers[0];
        assertNotNull(world);
        EntityPlayer player = FakePlayerFactory.getMinecraft(world);
        assertNotNull(player);
        assertFalse(player.worldObj.isRemote, "A dedicated server world is never remote");
        return player;
    }
}
