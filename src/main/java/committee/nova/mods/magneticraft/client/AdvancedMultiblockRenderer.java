package committee.nova.mods.magneticraft.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import committee.nova.mods.magneticraft.content.machine.framework.module.FluidTankModule;
import committee.nova.mods.magneticraft.content.multiblock.AdvancedMultiblockBlockEntity;
import committee.nova.mods.magneticraft.content.multiblock.MultiblockDefinition;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.core.BlockPos;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

/**
 * Dynamic controller-local overlays for formed multiblocks. The renderer never
 * predicts machine state: it consumes the latest block-entity snapshot and only
 * interpolates visual transforms between bounded server updates.
 */
public final class AdvancedMultiblockRenderer implements BlockEntityRenderer<AdvancedMultiblockBlockEntity> {
    public AdvancedMultiblockRenderer(BlockEntityRendererProvider.Context context) {
    }

    @Override
    public void render(
            AdvancedMultiblockBlockEntity machine,
            float partialTick,
            PoseStack poseStack,
            MultiBufferSource buffers,
            int packedLight,
            int packedOverlay
    ) {
        if (!machine.formed()) {
            return;
        }
        poseStack.pushPose();
        MachineRenderHelper.faceMachine(poseStack, machine.facing());
        renderMovingPart(machine, partialTick, poseStack, buffers, packedLight, packedOverlay);
        renderFluids(machine, poseStack, buffers, packedLight);
        poseStack.popPose();
    }

    private static void renderMovingPart(
            AdvancedMultiblockBlockEntity machine,
            float partialTick,
            PoseStack poseStack,
            MultiBufferSource buffers,
            int packedLight,
            int packedOverlay
    ) {
        long gameTime = machine.getLevel() == null ? 0L : machine.getLevel().getGameTime();
        double animationTick = gameTime % 1_048_576L + partialTick;
        double phase = machine.working() ? animationTick : machine.progress();
        MultiblockDefinition definition = machine.definition();
        switch (definition) {
            case GRINDER, SIEVE, STEAM_ENGINE, STEAM_TURBINE -> {
                poseStack.pushPose();
                poseStack.translate(0.5D, 0.72D, 0.5D);
                poseStack.mulPose(Axis.YP.rotationDegrees((float) (phase * 18.0D % 360.0D)));
                poseStack.scale(0.42F, 0.16F, 0.42F);
                MachineRenderHelper.renderItem(new ItemStack(Items.IRON_INGOT), poseStack, buffers,
                        packedLight, packedOverlay, definition.ordinal());
                poseStack.popPose();
            }
            case HYDRAULIC_PRESS, PUMPJACK -> {
                float travel = machine.working()
                        ? (float) ((Math.sin(animationTick * 0.35D) + 1.0D) * 0.12D)
                        : 0.0F;
                poseStack.pushPose();
                poseStack.translate(0.5D, 0.82D - travel, 0.5D);
                poseStack.scale(0.34F, 0.52F, 0.34F);
                MachineRenderHelper.renderItem(new ItemStack(Items.PISTON), poseStack, buffers,
                        packedLight, packedOverlay, definition.ordinal());
                poseStack.popPose();
            }
            case SOLAR_PANEL -> {
                BlockPos target = machine.solarTowerPosition();
                double x = target == null ? 0.0D : target.getX() - machine.getBlockPos().getX();
                double y = target == null ? 1.0D : target.getY() - machine.getBlockPos().getY();
                double z = target == null ? 1.0D : target.getZ() - machine.getBlockPos().getZ();
                var facing = machine.facing();
                var localRight = facing.getClockWise();
                double localX = x * localRight.getStepX() + z * localRight.getStepZ();
                double localZ = -(x * facing.getStepX() + z * facing.getStepZ());
                float yaw = (float) Math.toDegrees(Math.atan2(localX, localZ));
                float pitch = (float) -Math.toDegrees(
                        Math.atan2(y, Math.max(0.001D, Math.hypot(localX, localZ)))
                );
                poseStack.pushPose();
                poseStack.translate(0.5D, 0.95D, 0.5D);
                poseStack.mulPose(Axis.YP.rotationDegrees(yaw));
                poseStack.mulPose(Axis.XP.rotationDegrees(pitch));
                poseStack.scale(0.72F, 0.12F, 0.72F);
                MachineRenderHelper.renderItem(new ItemStack(Items.DAYLIGHT_DETECTOR), poseStack, buffers,
                        packedLight, packedOverlay, definition.ordinal());
                poseStack.popPose();
            }
            case BIG_COMBUSTION_CHAMBER -> {
                if (machine.working()) {
                    poseStack.pushPose();
                    poseStack.translate(0.5D, 0.42D, 0.5D);
                    poseStack.scale(0.38F, 0.38F, 0.38F);
                    MachineRenderHelper.renderItem(new ItemStack(Items.CAMPFIRE), poseStack, buffers,
                            packedLight, packedOverlay, definition.ordinal());
                    poseStack.popPose();
                }
            }
            case BIG_ELECTRIC_FURNACE -> {
                if (machine.working()) {
                    double travel = animationTick * 0.055D % 0.72D - 0.36D;
                    poseStack.pushPose();
                    poseStack.translate(0.5D, 0.34D, 0.5D + travel);
                    poseStack.scale(0.26F, 0.26F, 0.26F);
                    MachineRenderHelper.renderItem(new ItemStack(Items.IRON_INGOT), poseStack, buffers,
                            packedLight, packedOverlay, definition.ordinal());
                    poseStack.popPose();
                }
            }
            default -> {
                // Static-only definitions intentionally have no BER moving part.
            }
        }
    }

    private static void renderFluids(
            AdvancedMultiblockBlockEntity machine,
            PoseStack poseStack,
            MultiBufferSource buffers,
            int packedLight
    ) {
        int count = machine.definition().tankCount();
        for (int index = 0; index < count; index++) {
            FluidTankModule module = machine.tank(index);
            if (module == null || module.tank().isEmpty()) {
                continue;
            }
            float fillRatio = (float) module.tank().getFluidAmount() / module.tank().getCapacity();
            float y = 0.18F + Math.min(0.58F, fillRatio * 0.58F) + index * 0.012F;
            MachineRenderHelper.renderFluidSurface(
                    module.tank().getFluid(),
                    fillRatio,
                    y,
                    Math.max(0.17F, 0.34F - index * 0.035F),
                    poseStack,
                    buffers,
                    packedLight
            );
        }
    }
}
