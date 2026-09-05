package codechicken.multipart.mixin.early;

import net.minecraft.block.Block;
import net.minecraft.client.multiplayer.WorldClient;
import net.minecraft.client.renderer.RenderBlocks;
import net.minecraft.client.renderer.RenderGlobal;
import net.minecraft.util.IIcon;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

import com.gtnewhorizon.gtnhlib.client.model.ModelISBRH;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;

import codechicken.multipart.JsonModeledPart;
import codechicken.multipart.RenderPartResolver;
import codechicken.multipart.TMultiPart;

@Mixin(RenderGlobal.class)
public class MixinRenderGlobal {

    @Shadow
    private WorldClient theWorld;

    @Redirect(
            method = "drawBlockDamageTexture(Lnet/minecraft/client/renderer/Tessellator;Lnet/minecraft/entity/EntityLivingBase;F)V",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/multiplayer/WorldClient;getBlock(III)Lnet/minecraft/block/Block;",
                    remap = false),
            remap = false)
    private Block forgemultipart$redirectDamageBlock(WorldClient world, int x, int y, int z) {
        Block block = world.getBlock(x, y, z);
        TMultiPart part = RenderPartResolver.resolve(world, x, y, z);
        if (part instanceof JsonModeledPart) {
            JsonModeledPart jsonPart = (JsonModeledPart) part;
            return jsonPart.getBlock();
        }
        return block;
    }

    @WrapOperation(
            method = "drawBlockDamageTexture(Lnet/minecraft/client/renderer/Tessellator;Lnet/minecraft/entity/EntityLivingBase;F)V",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/renderer/RenderBlocks;renderBlockUsingTexture(Lnet/minecraft/block/Block;IIILnet/minecraft/util/IIcon;)V",
                    remap = false),
            remap = false)
    private void forgemultipart$redirectBreakingModeledPartDraw(RenderBlocks instance, Block block, int x, int y, int z,
            IIcon overrideTexture, Operation<Void> original) {
        TMultiPart part = RenderPartResolver.resolve(theWorld, x, y, z);
        if (part instanceof JsonModeledPart) {
            JsonModeledPart jsonModeledPart = (JsonModeledPart) part;
            instance.setOverrideBlockTexture(overrideTexture);
            ModelISBRH.INSTANCE.get().renderWorldBlock(
                    jsonModeledPart.getRenderWorld(),
                    x,
                    y,
                    z,
                    jsonModeledPart.getBlock(),
                    ModelISBRH.JSON_ISBRH_ID,
                    instance);
            instance.clearOverrideBlockTexture();
        } else {
            original.call(instance, block, x, y, z, overrideTexture);
        }
        RenderPartResolver.clear();
    }
}