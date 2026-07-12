package committee.nova.mods.magneticraft.client;

import com.mojang.blaze3d.vertex.PoseStack;
import committee.nova.mods.magneticraft.content.computer.ComputerBlockEntity;
import committee.nova.mods.magneticraft.content.computer.ProgrammableBlock;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

/** Renders the synchronized computer run-state indicator over the static OBJ body. */
public final class ComputerRenderer implements BlockEntityRenderer<ComputerBlockEntity> {
    public ComputerRenderer(BlockEntityRendererProvider.Context context) {
    }

    @Override
    public void render(
            ComputerBlockEntity computer,
            float partialTick,
            PoseStack poseStack,
            MultiBufferSource buffers,
            int packedLight,
            int packedOverlay
    ) {
        poseStack.pushPose();
        MachineRenderHelper.faceMachine(
                poseStack,
                computer.getBlockState().getValue(ProgrammableBlock.FACING)
        );
        poseStack.translate(0.5D, 0.60D, 0.165D);
        poseStack.scale(0.12F, 0.12F, 0.12F);
        MachineRenderHelper.renderItem(
                new ItemStack(computer.visuallyRunning() ? Items.REDSTONE_TORCH : Items.REDSTONE),
                poseStack,
                buffers,
                packedLight,
                packedOverlay,
                (int) computer.getBlockPos().asLong()
        );
        poseStack.popPose();
    }
}
