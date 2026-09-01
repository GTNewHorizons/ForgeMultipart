package codechicken.multipart.test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Blocks;
import net.minecraft.item.ItemStack;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.MovingObjectPosition;
import net.minecraft.util.Vec3;
import net.minecraft.world.WorldServer;
import net.minecraftforge.common.util.FakePlayerFactory;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import codechicken.lib.raytracer.ExtendedMOP;
import codechicken.lib.vec.BlockCoord;
import codechicken.microblock.AdditionPlacement;
import codechicken.microblock.CommonMicroblock;
import codechicken.microblock.EdgePlacementGrid$;
import codechicken.microblock.ExecutablePlacement;
import codechicken.microblock.ExpandingPlacement;
import codechicken.microblock.FaceMicroClass$;
import codechicken.microblock.FacePlacement$;
import codechicken.microblock.MicroMaterialRegistry;
import codechicken.microblock.Microblock;
import codechicken.microblock.MicroblockClass;
import codechicken.microblock.MicroblockPlacement;
import codechicken.microblock.MicroblockPlacement$;
import codechicken.microblock.PlacementGrid;
import codechicken.microblock.PlacementProperties;
import codechicken.multipart.ControlKeyModifer;
import codechicken.multipart.TileMultipart;
import scala.Tuple2;

class MicroblockPlacementFunctionalTest {

    private static final BlockCoord EXTERNAL_POS = new BlockCoord(160, 200, 160);
    private static final BlockCoord INTERNAL_POS = new BlockCoord(164, 200, 160);
    private static final BlockCoord EXPAND_POS = new BlockCoord(168, 200, 160);
    private static final BlockCoord CUSTOM_POS = new BlockCoord(172, 200, 160);

    private WorldServer world;
    private EntityPlayer player;
    private int material;

    @BeforeEach
    void setUp() {
        world = MinecraftServer.getServer().worldServers[0];
        assertNotNull(world);
        player = FakePlayerFactory.getMinecraft(world);
        assertNotNull(player);
        player.setSneaking(false);
        ControlKeyModifer.map().clear();
        material = MicroMaterialRegistry.materialID("minecraft:stone");
        assertTrue(material >= 0);
        clearPositions();
    }

    @AfterEach
    void tearDown() {
        player.setSneaking(false);
        ControlKeyModifer.map().clear();
        clearPositions();
    }

    @Test
    void selectsPlacesAndConsumesAnExternalFace() {
        world.setBlock(EXTERNAL_POS.x, EXTERNAL_POS.y, EXTERNAL_POS.z, Blocks.stone);
        MovingObjectPosition hit = hit(EXTERNAL_POS, 1.0);

        MicroblockPlacement decision = new MicroblockPlacement(player, hit, 1, material, true, FacePlacement$.MODULE$);
        ExecutablePlacement placement = decision.apply();

        AdditionPlacement addition = assertInstanceOf(AdditionPlacement.class, placement);
        BlockCoord target = EXTERNAL_POS.copy().offset(1);
        assertEquals(target, addition.pos());
        assertEquals(0, slot(addition.part()));
        assertSame(player, decision.player());
        assertSame(hit, decision.hit());
        assertSame(world, decision.world());
        assertSame(FaceMicroClass$.MODULE$, decision.mcrClass());
        assertEquals(EXTERNAL_POS, decision.pos());
        assertEquals(0, decision.slot());
        assertEquals(1, decision.oslot());
        assertEquals(1.0, decision.d());
        assertFalse(decision.internal());
        assertFalse(decision.oppMod());

        ItemStack stack = new ItemStack(net.minecraft.init.Items.stick, 3);
        addition.place(world, player, stack);
        addition.consume(world, player, stack);

        TileMultipart tile = TileMultipart.getTile(world, target);
        assertNotNull(tile);
        assertSame(addition.part(), tile.jPartList().get(0));
        assertEquals(2, stack.stackSize);
    }

    @Test
    void selectsTheInternalOrOppositeSlotFromTheControlModifier() {
        player.setSneaking(true);
        TileMultipart tile = TileMultipart.addPart(world, INTERNAL_POS, face(1, 2));

        MicroblockPlacement normal = new MicroblockPlacement(
                player,
                hit(INTERNAL_POS, 0.25),
                1,
                material,
                true,
                FacePlacement$.MODULE$);
        AdditionPlacement normalPlacement = assertInstanceOf(AdditionPlacement.class, normal.apply());
        assertTrue(normal.internal());
        assertFalse(normal.doExpand());
        assertEquals(0, slot(normalPlacement.part()));
        normalPlacement.place(world, player, new ItemStack(net.minecraft.init.Items.stick));
        assertSame(normalPlacement.part(), tile.partMap(0));

        clear(INTERNAL_POS);
        tile = TileMultipart.addPart(world, INTERNAL_POS, face(1, 2));
        ControlKeyModifer.map().put(player, true);

        MicroblockPlacement opposite = new MicroblockPlacement(
                player,
                hit(INTERNAL_POS, 0.25),
                1,
                material,
                true,
                FacePlacement$.MODULE$);
        AdditionPlacement oppositePlacement = assertInstanceOf(AdditionPlacement.class, opposite.apply());
        assertTrue(opposite.oppMod());
        assertEquals(1, slot(oppositePlacement.part()));
        oppositePlacement.place(world, player, new ItemStack(net.minecraft.init.Items.stick));
        assertSame(oppositePlacement.part(), tile.partMap(1));
    }

    @Test
    void expandsTheHitPartInPlaceAndConsumesOneItem() {
        Microblock original = face(1, 0);
        TileMultipart tile = TileMultipart.addPart(world, EXPAND_POS, original);
        ExtendedMOP hit = indexedHit(EXPAND_POS, 0.25, 0);

        MicroblockPlacement decision = new MicroblockPlacement(player, hit, 1, material, true, FacePlacement$.MODULE$);
        ExpandingPlacement placement = assertInstanceOf(ExpandingPlacement.class, decision.apply());
        assertTrue(decision.internal());
        assertTrue(decision.doExpand());
        assertEquals(2, placement.part().getSize());
        assertEquals(0, slot(placement.part()));

        ItemStack stack = new ItemStack(net.minecraft.init.Items.stick, 3);
        placement.place(world, player, stack);
        placement.consume(world, player, stack);

        assertSame(original, tile.jPartList().get(0));
        assertEquals(2, original.getSize());
        assertEquals(0, slot(original));
        assertEquals(2, stack.stackSize);
    }

    @Test
    void customPlacementWinsEvenWhenTheGridRejectsTheHit() {
        world.setBlock(CUSTOM_POS.x, CUSTOM_POS.y, CUSTOM_POS.z, Blocks.stone);
        ExecutablePlacement marker = new AdditionPlacement(CUSTOM_POS, null);
        PlacementProperties properties = new PlacementProperties() {

            @Override
            public int opposite(int slot, int side) {
                return slot;
            }

            @Override
            public MicroblockClass microClass() {
                return FaceMicroClass$.MODULE$;
            }

            @Override
            public PlacementGrid placementGrid() {
                return EdgePlacementGrid$.MODULE$;
            }

            @Override
            public ExecutablePlacement customPlacement(MicroblockPlacement placement) {
                return marker;
            }
        };

        ExecutablePlacement result = MicroblockPlacement$.MODULE$
                .apply(player, hit(CUSTOM_POS, 1.0), 2, material, false, properties);

        assertSame(marker, result);
    }

    private Microblock face(int size, int slot) {
        Microblock part = FaceMicroClass$.MODULE$.create(false, material);
        part.setShape(size, slot);
        return part;
    }

    private static int slot(Microblock part) {
        return ((CommonMicroblock) part).getSlot();
    }

    private static MovingObjectPosition hit(BlockCoord pos, double relativeY) {
        return new MovingObjectPosition(
                pos.x,
                pos.y,
                pos.z,
                1,
                Vec3.createVectorHelper(pos.x + 0.5, pos.y + relativeY, pos.z + 0.5));
    }

    private static ExtendedMOP indexedHit(BlockCoord pos, double relativeY, int index) {
        return new ExtendedMOP(
                pos.x,
                pos.y,
                pos.z,
                1,
                Vec3.createVectorHelper(pos.x + 0.5, pos.y + relativeY, pos.z + 0.5),
                new Tuple2<Object, Object>(index, null));
    }

    private void clearPositions() {
        clear(EXTERNAL_POS);
        clear(EXTERNAL_POS.copy().offset(1));
        clear(INTERNAL_POS);
        clear(EXPAND_POS);
        clear(CUSTOM_POS);
    }

    private void clear(BlockCoord pos) {
        world.removeTileEntity(pos.x, pos.y, pos.z);
        world.setBlockToAir(pos.x, pos.y, pos.z);
    }
}
