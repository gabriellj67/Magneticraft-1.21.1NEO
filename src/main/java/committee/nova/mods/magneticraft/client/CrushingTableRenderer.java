package committee.nova.mods.magneticraft.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import committee.nova.mods.magneticraft.content.machine.crushingtable.CrushingTableBlockEntity;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.AbstractSkullBlock;

/**
 * Renders the server-synchronized crushing-table display stack.
 */
public final class CrushingTableRenderer implements BlockEntityRenderer<CrushingTableBlockEntity> {
    public CrushingTableRenderer(BlockEntityRendererProvider.Context context) {
    }

    @Override
    public void render(
            CrushingTableBlockEntity table,
            float partialTick,
            PoseStack poseStack,
            MultiBufferSource buffers,
            int packedLight,
            int packedOverlay
    ) {
        ItemStack stack = table.storedItem();
        if (stack.isEmpty()) {
            return;
        }
        poseStack.pushPose();
        poseStack.translate(0.5D, 0.9375D, 0.3125D);
        Minecraft minecraft = Minecraft.getInstance();
        boolean skull = stack.getItem() instanceof BlockItem blockItem
                && blockItem.getBlock() instanceof AbstractSkullBlock;
        boolean flat = skull || !minecraft.getItemRenderer()
                .getModel(stack, table.getLevel(), null, (int) table.getBlockPos().asLong())
                .isGui3d();
        if (flat) {
            poseStack.translate(0.0D, -0.045D, 0.0625D);
            poseStack.mulPose(Axis.XP.rotationDegrees(90.0F));
        } else {
            poseStack.translate(0.0D, -0.125D, 0.1875D);
        }
        minecraft.getItemRenderer().renderStatic(
                stack,
                ItemDisplayContext.GROUND,
                packedLight,
                packedOverlay,
                poseStack,
                buffers,
                table.getLevel(),
                (int) table.getBlockPos().asLong()
        );
        poseStack.popPose();
    }
}
