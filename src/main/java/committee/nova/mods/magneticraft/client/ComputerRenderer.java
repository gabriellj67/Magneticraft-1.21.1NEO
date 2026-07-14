package committee.nova.mods.magneticraft.client;

import com.mojang.blaze3d.vertex.PoseStack;
import committee.nova.mods.magneticraft.content.computer.ComputerBlockEntity;
import committee.nova.mods.magneticraft.content.computer.ProgrammableBlock;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.LightTexture;

/** Renders the historical screen group with synchronized run-state illumination. */
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
        MachineRenderHelper.renderBakedModel(
                LegacyBakedModels.COMPUTER_SCREEN,
                poseStack,
                buffers,
                computer.visuallyRunning() ? LightTexture.FULL_BRIGHT : packedLight,
                packedOverlay
        );
        poseStack.popPose();
    }
}
