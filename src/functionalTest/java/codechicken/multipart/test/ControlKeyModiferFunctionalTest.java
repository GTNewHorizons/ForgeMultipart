package codechicken.multipart.test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.UUID;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.WorldServer;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.common.util.FakePlayerFactory;
import net.minecraftforge.event.entity.player.PlayerEvent.Clone;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.mojang.authlib.GameProfile;

import codechicken.multipart.ControlKeyModifer;
import cpw.mods.fml.common.FMLCommonHandler;
import cpw.mods.fml.common.gameevent.PlayerEvent.PlayerLoggedOutEvent;

/**
 * isControlDown needs a player with a world to pick its branch. On a dedicated server the world is never remote, so
 * these cover the server half: the map the client keeps in sync through packet 1. The client half reads
 * isClientPressing instead and no test harness has a client.
 */
class ControlKeyModiferFunctionalTest {

    @Test
    void replacementAndLogoutReleaseOldPlayerReferences() {
        EntityPlayer original = player();
        EntityPlayer replacement = FakePlayerFactory.get(
                (WorldServer) original.worldObj,
                new GameProfile(UUID.fromString("5f09d0b1-0aa8-4bc2-a362-5dd96d81eb55"), "modifier-test"));
        record(original, true);

        MinecraftForge.EVENT_BUS.post(new Clone(replacement, original, true));
        assertFalse(ControlKeyModifer.map().containsKey(original));
        assertTrue(ControlKeyModifer.isControlDown(replacement));

        FMLCommonHandler.instance().bus().post(new PlayerLoggedOutEvent(replacement));
        assertFalse(ControlKeyModifer.map().containsKey(replacement));
        assertFalse(ControlKeyModifer.isControlDown(replacement));
    }

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
