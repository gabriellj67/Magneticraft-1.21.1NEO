package committee.nova.mods.magneticraft.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import committee.nova.mods.magneticraft.content.machine.framework.module.FluidTankModule;
import committee.nova.mods.magneticraft.content.machine.singleblock.SingleBlockMachineBlock;
import committee.nova.mods.magneticraft.content.machine.singleblock.SingleBlockMachineBlockEntity;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

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
        double phase = gameTime % 1_048_576L + partialTick;
        switch (machine.definition()) {
            case INSERTER -> {
                float swing = machine.working() ? (float) Math.sin(phase * 0.45D) * 38.0F : 0.0F;
                poseStack.pushPose();
                poseStack.translate(0.5D, 0.48D, 0.5D);
                poseStack.mulPose(Axis.XP.rotationDegrees(-25.0F + swing));
                poseStack.translate(0.0D, 0.28D, 0.0D);
                poseStack.scale(0.12F, 0.62F, 0.12F);
                MachineRenderHelper.renderItem(new ItemStack(Items.IRON_INGOT), poseStack, buffers,
                        packedLight, packedOverlay, machine.definition().ordinal());
                poseStack.popPose();
            }
            case ELECTRIC_ENGINE -> {
                poseStack.pushPose();
                poseStack.translate(0.5D, 0.55D, 0.5D);
                poseStack.mulPose(Axis.ZP.rotationDegrees(machine.working()
                        ? (float) (phase * 24.0D % 360.0D)
                        : machine.progress()));
                poseStack.scale(0.32F, 0.32F, 0.16F);
                MachineRenderHelper.renderItem(new ItemStack(Items.COPPER_INGOT), poseStack, buffers,
                        packedLight, packedOverlay, machine.definition().ordinal());
                poseStack.popPose();
            }
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
