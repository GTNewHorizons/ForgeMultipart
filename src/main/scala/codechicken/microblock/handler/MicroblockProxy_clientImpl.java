package codechicken.microblock.handler;

import net.minecraft.client.renderer.RenderBlocks;
import net.minecraft.item.Item;
import net.minecraftforge.client.MinecraftForgeClient;

import codechicken.lib.packet.PacketCustom;
import codechicken.microblock.AngelicaCompat;
import codechicken.microblock.ItemMicroPartRenderer$;
import codechicken.microblock.ItemSawRenderer$;
import codechicken.microblock.MicroMaterialRegistry;
import cpw.mods.fml.common.Loader;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import scala.collection.Iterator;

public class MicroblockProxy_clientImpl extends MicroblockProxy_serverImpl {

    @SideOnly(Side.CLIENT)
    private RenderBlocks renderBlocks;

    private volatile boolean bitmap$0;

    private RenderBlocks renderBlocks$lzycompute() {
        synchronized (this) {
            if (!bitmap$0) {
                renderBlocks = new RenderBlocks();
                bitmap$0 = true;
            }
            return renderBlocks;
        }
    }

    public RenderBlocks renderBlocks() {
        return bitmap$0 ? renderBlocks : renderBlocks$lzycompute();
    }

    @Override
    @SideOnly(Side.CLIENT)
    public void postInit() {
        super.postInit();
        MicroMaterialRegistry.loadIcons();
        MinecraftForgeClient.registerItemRenderer(itemMicro(), ItemMicroPartRenderer$.MODULE$);
        PacketCustom.assignHandler(MicroblockCPH.registryChannel(), MicroblockCPH$.MODULE$);

        if (Loader.isModLoaded("angelica")) {
            MicroblockMod.angelicaCompat_$eq(new AngelicaCompat());
        }
    }

    @Override
    @SideOnly(Side.CLIENT)
    public void init() {
        super.init();
        Iterator<Item> iterator = saws().iterator();
        while (iterator.hasNext()) {
            MinecraftForgeClient.registerItemRenderer(iterator.next(), ItemSawRenderer$.MODULE$);
        }
    }
}
