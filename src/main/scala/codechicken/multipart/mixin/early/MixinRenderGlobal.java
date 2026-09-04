package codechicken.multipart.mixin.early;

import codechicken.lib.raytracer.ExtendedMOP;
import codechicken.multipart.JsonModeledPart;
import codechicken.multipart.TMultiPart;
import codechicken.multipart.TileMultipart;
import codechicken.multipart.TileMultipartClient;

import net.minecraft.block.Block;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.WorldClient;
import net.minecraft.client.renderer.RenderBlocks;
import net.minecraft.client.renderer.RenderGlobal;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.IIcon;
import net.minecraft.util.MovingObjectPosition;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

import com.gtnewhorizon.gtnhlib.client.model.ModelISBRH;

import scala.Tuple2;

@Mixin(RenderGlobal.class)
public class MixinRenderGlobal 
{
    @Unique
    private JsonModeledPart forgemultipart$breakingPart;

    @Redirect(
            method = "drawBlockDamageTexture(Lnet/minecraft/client/renderer/Tessellator;Lnet/minecraft/entity/EntityLivingBase;F)V",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/multiplayer/WorldClient;getBlock(III)Lnet/minecraft/block/Block;",
                    remap = false
            ),
            remap = false
    )
    private Block forgemultipart$redirectDamageBlock(
            WorldClient world,
            int x,
            int y,
            int z) {

        Block block = world.getBlock(x, y, z);

        forgemultipart$breakingPart = null;

        TileEntity tile = world.getTileEntity(x, y, z);
        if (!(tile instanceof TileMultipartClient )) {
            return block;
        }
        
        // IDK why but the scala compiler doesn't like pattern variables?
        // Also, the scala trait DOES NOT expose the methods from TileMultipart, so I have to cast it up.
        TileMultipartClient clientTile = (TileMultipartClient) tile;
        TileMultipart multipart = (TileMultipart) clientTile;

        MovingObjectPosition hit = Minecraft.getMinecraft().objectMouseOver;
        if (hit == null
                || hit.blockX != x
                || hit.blockY != y
                || hit.blockZ != z) {
            return block;
        }
        Object data = ExtendedMOP.getData(hit);
        if (data instanceof Tuple2) {
            Tuple2<?, ?> hitInfo = (Tuple2<?, ?>) data;
            Object indexObject = hitInfo._1();

            if (indexObject instanceof Integer) {
                int index = (Integer) indexObject;

                if (index < 0 || index >= multipart.jPartList().size()) {
                    return block;
                }
                TMultiPart part = multipart.jPartList().get(index);

                if (part instanceof JsonModeledPart) {
                    JsonModeledPart jsonPart = (JsonModeledPart) part;
                    forgemultipart$breakingPart = jsonPart;
                    return forgemultipart$breakingPart.getBlock();
                }
            }
        }
        
        return block;
    }

    @WrapOperation(
            method = "drawBlockDamageTexture(Lnet/minecraft/client/renderer/Tessellator;Lnet/minecraft/entity/EntityLivingBase;F)V",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/renderer/RenderBlocks;renderBlockUsingTexture(Lnet/minecraft/block/Block;IIILnet/minecraft/util/IIcon;)V",
                    remap = false
            ),
            remap = false
    )
    private void forgemultipart$redirectBreakingModeledPartDraw(RenderBlocks instance, 
        Block block, int x, int y, int z, IIcon overrideTexture, Operation<Void> original)
    {
        if (forgemultipart$breakingPart != null) {
            JsonModeledPart renderPart = forgemultipart$breakingPart;
            forgemultipart$breakingPart = null;
            
            instance.setOverrideBlockTexture(overrideTexture);
            ModelISBRH.INSTANCE.get().renderWorldBlock(
                    renderPart.getRenderWorld(),
                    x, y, z,
                    renderPart.getBlock(),
                    ModelISBRH.JSON_ISBRH_ID,
                    instance
            );
            instance.clearOverrideBlockTexture();
        }
        else
        {
            original.call(instance, block, x, y, z, overrideTexture);
        }
    }
}