package committee.nova.mods.magneticraft.client;

import com.mojang.blaze3d.vertex.PoseStack;
import committee.nova.mods.magneticraft.content.machine.framework.module.FluidTankModule;
import committee.nova.mods.magneticraft.content.machine.singleblock.SingleBlockMachineBlock;
import committee.nova.mods.magneticraft.content.machine.singleblock.SingleBlockMachineBlockEntity;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;

/** Draws only the moving and fluid portions of single-block machines. */
public final class SingleBlockMachineRenderer implements BlockEntityRenderer<SingleBlockMachineBlockEntity> {
    public SingleBlockMachineRenderer(BlockEntityRendererProvider.Context context) {
    }

    @Override
    public void render(
            SingleBlockMachineBlockEntity machine,
            float partialTick,
            PoseStack poseStack,
            MultiBufferSource buffers,
            int packedLight,
            int packedOverlay
    ) {
        poseStack.pushPose();
        MachineRenderHelper.faceMachine(
                poseStack,
                machine.getBlockState().getValue(SingleBlockMachineBlock.FACING)
        );
        renderMovingPart(machine, partialTick, poseStack, buffers, packedLight, packedOverlay);
        renderTank(machine.primaryTank(), poseStack, buffers, packedLight);
        poseStack.popPose();
    }

    private static void renderMovingPart(
            SingleBlockMachineBlockEntity machine,
            float partialTick,
            PoseStack poseStack,
            MultiBufferSource buffers,
            int packedLight,
            int packedOverlay
    ) {
        long gameTime = machine.getLevel() == null ? 0L : machine.getLevel().getGameTime();
        switch (machine.definition()) {
            case INSERTER -> MachineRenderHelper.renderBakedModel(
                    LegacyBakedModels.frame(
                            LegacyBakedModels.INSERTER,
                            gameTime,
                            partialTick,
                            1.5F,
                            machine.working()
                    ),
                    poseStack,
                    buffers,
                    packedLight,
                    packedOverlay
            );
            case ELECTRIC_ENGINE -> MachineRenderHelper.renderBakedModel(
                    LegacyBakedModels.frame(
                            LegacyBakedModels.ELECTRIC_ENGINE,
                            gameTime,
                            partialTick,
                            1.0F,
                            machine.working()
                    ),
                    poseStack,
                    buffers,
                    packedLight,
                    packedOverlay
            );
            default -> {
                // Static-only definitions intentionally have no BER moving part.
            }
        }
    }

    private static void renderTank(
            FluidTankModule module,
            PoseStack poseStack,
            MultiBufferSource buffers,
            int packedLight
    ) {
        if (module == null || module.tank().isEmpty()) {
            return;
        }
        float fillRatio = (float) module.tank().getFluidAmount() / module.tank().getCapacity();
        MachineRenderHelper.renderFluidSurface(
                module.tank().getFluid(),
                fillRatio,
                0.16F + fillRatio * 0.68F,
                0.34F,
                poseStack,
                buffers,
                packedLight
        );
    }
}
