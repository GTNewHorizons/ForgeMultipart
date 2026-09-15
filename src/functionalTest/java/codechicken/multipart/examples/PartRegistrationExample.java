package codechicken.multipart.examples;

import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.world.World;

import codechicken.lib.data.MCDataInput;
import codechicken.lib.data.MCDataOutput;
import codechicken.lib.vec.BlockCoord;
import codechicken.multipart.MultiPartRegistry;
import codechicken.multipart.MultiPartRegistry.IPartFactory2;
import codechicken.multipart.TMultiPart;
import codechicken.multipart.TileMultipart;

/** Compiling example for the part-registration and custom-part lifecycle guides. */
public final class PartRegistrationExample implements IPartFactory2 {

    public static final String PART_TYPE = "yourmod:part";

    /** Call from the mod's common preInit/init handler, on both sides. */
    public static void register() {
        MultiPartRegistry.registerPartFactory(new PartRegistrationExample(), PART_TYPE);
    }

    @Override
    public TMultiPart createPart(String name, NBTTagCompound nbt) {
        return new ExamplePart();
    }

    @Override
    public TMultiPart createPart(String name, MCDataInput packet) {
        return new ExamplePart();
    }

    /** Call from the server-side item/action after choosing the target position. */
    public static TileMultipart place(World world, BlockCoord pos, int value) {
        ExamplePart part = new ExamplePart(value);
        if (world.isRemote || !TileMultipart.canPlacePart(world, pos, part)) {
            return null;
        }
        return TileMultipart.addPart(world, pos, part);
    }

    public static final class ExamplePart extends TMultiPart {

        private int value;

        public ExamplePart() {}

        public ExamplePart(int value) {
            this.value = value;
        }

        @Override
        public String getType() {
            return PART_TYPE;
        }

        @Override
        public boolean doesTick() {
            return false;
        }

        public int value() {
            return value;
        }

        /** Example for state that affects persistence, clients, sibling parts and rendering. */
        public void setValue(int value) {
            if (this.value == value) {
                return;
            }
            this.value = value;
            if (world() != null && !world().isRemote) {
                sendDescUpdate();
                tile().notifyPartChange(this);
                tile().markDirty();
            }
        }

        @Override
        public void save(NBTTagCompound tag) {
            tag.setInteger("value", value);
        }

        @Override
        public void load(NBTTagCompound tag) {
            value = tag.getInteger("value");
        }

        @Override
        public void writeDesc(MCDataOutput packet) {
            packet.writeInt(value);
        }

        @Override
        public void readDesc(MCDataInput packet) {
            value = packet.readInt();
        }
    }
}
