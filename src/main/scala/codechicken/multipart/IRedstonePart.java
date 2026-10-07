package codechicken.multipart;

/** A part with redstone interaction. Its presence selects the generated redstone tile trait. */
public interface IRedstonePart {

    int strongPowerLevel(int side);

    int weakPowerLevel(int side);

    boolean canConnectRedstone(int side);
}
