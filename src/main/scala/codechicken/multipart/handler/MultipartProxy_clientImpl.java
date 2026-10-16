package codechicken.multipart.handler;

import net.minecraft.tileentity.TileEntity;

import codechicken.lib.packet.PacketCustom;
import codechicken.multipart.ControlKeyHandler;
import codechicken.multipart.MultipartRenderer$;
import cpw.mods.fml.client.registry.ClientRegistry;
import cpw.mods.fml.client.registry.RenderingRegistry;
import cpw.mods.fml.common.FMLCommonHandler;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

public class MultipartProxy_clientImpl extends MultipartProxy_serverImpl {

    @Override
    @SideOnly(Side.CLIENT)
    public void postInit() {
        super.postInit();
        RenderingRegistry.registerBlockHandler(MultipartRenderer$.MODULE$);
        PacketCustom.assignHandler(MultipartCPH$.MODULE$.channel(), MultipartCPH$.MODULE$);
        PacketCustom.assignHandler(MultipartCPH$.MODULE$.registryChannel(), MultipartCPH$.MODULE$);

        FMLCommonHandler.instance().bus().register(ControlKeyHandler.INSTANCE);
        ClientRegistry.registerKeyBinding(ControlKeyHandler.INSTANCE);
    }

    @Override
    @SideOnly(Side.CLIENT)
    public void onTileClassBuilt(Class<? extends TileEntity> type) {
        super.onTileClassBuilt(type);
        ClientRegistry.bindTileEntitySpecialRenderer(type, MultipartRenderer$.MODULE$);
    }
}
