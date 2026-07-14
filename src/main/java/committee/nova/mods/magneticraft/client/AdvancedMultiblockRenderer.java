package committee.nova.mods.magneticraft.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import committee.nova.mods.magneticraft.content.machine.framework.module.FluidTankModule;
import committee.nova.mods.magneticraft.content.multiblock.AdvancedMultiblockBlockEntity;
import committee.nova.mods.magneticraft.content.multiblock.MultiblockDefinition;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.resources.ResourceLocation;

import java.util.List;

/**
 * Dynamic controller-local overlays for formed multiblocks. The renderer never
 * predicts machine state: it consumes the latest block-entity snapshot and only
 * interpolates visual transforms between bounded server updates.
 */
public final class AdvancedMultiblockRenderer implements BlockEntityRenderer<AdvancedMultiblockBlockEntity> {
    static final int STEAM_TURBINE_BLADE_COUNT = 12;

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
        MultiblockDefinition definition = machine.definition();
        switch (definition) {
            case GRINDER -> renderFrame(
                    LegacyBakedModels.GRINDER,
                    gameTime,
                    partialTick,
                    1.0F,
                    machine.working(),
                    0.0D,
                    0.0D,
                    -1.0D,
                    poseStack,
                    buffers,
                    packedLight,
                    packedOverlay
            );
            case SIEVE -> {
                poseStack.pushPose();
                poseStack.mulPose(Axis.YP.rotationDegrees(180.0F));
                renderFrame(
                        LegacyBakedModels.SIEVE,
                        gameTime,
                        partialTick,
                        0.75F,
                        machine.working(),
                        0.0D,
                        0.0D,
                        2.0D,
                        poseStack,
                        buffers,
                        packedLight,
                        packedOverlay
                );
                poseStack.popPose();
            }
            case HYDRAULIC_PRESS -> renderFrame(
                    LegacyBakedModels.HYDRAULIC_PRESS,
                    gameTime,
                    partialTick,
                    1.5F,
                    machine.working(),
                    0.0D,
                    0.0D,
                    -1.0D,
                    poseStack,
                    buffers,
                    packedLight,
                    packedOverlay
            );
            case STEAM_ENGINE -> renderFrame(
                    LegacyBakedModels.STEAM_ENGINE,
                    gameTime,
                    partialTick,
                    1.0F,
                    machine.working(),
                    -1.0D,
                    0.0D,
                    -1.0D,
                    poseStack,
                    buffers,
                    packedLight,
                    packedOverlay
            );
            case STEAM_TURBINE -> renderSteamTurbine(
                    animationTick,
                    machine.working(),
                    poseStack,
                    buffers,
                    packedLight,
                    packedOverlay
            );
            case SOLAR_PANEL -> renderSolarPanel(
                    machine,
                    gameTime,
                    poseStack,
                    buffers,
                    packedLight,
                    packedOverlay
            );
            case BIG_COMBUSTION_CHAMBER -> {
                if (machine.working()) {
                    poseStack.pushPose();
                    poseStack.translate(0.0D, 0.0D, -1.0D);
                    MachineRenderHelper.renderBakedModel(
                            LegacyBakedModels.BIG_COMBUSTION_CHAMBER_FIRE,
                            poseStack,
                            buffers,
                            packedLight,
                            packedOverlay
                    );
                    poseStack.popPose();
                }
            }
            default -> {
                // The released pumpjack, refinery and remaining definitions used static historical models.
            }
        }
    }

    private static void renderFrame(
            List<ResourceLocation> frames,
            long gameTime,
            float partialTick,
            float ticksPerFrame,
            boolean working,
            double x,
            double y,
            double z,
            PoseStack poseStack,
            MultiBufferSource buffers,
            int packedLight,
            int packedOverlay
    ) {
        poseStack.pushPose();
        poseStack.translate(x, y, z);
        MachineRenderHelper.renderBakedModel(
                LegacyBakedModels.frame(frames, gameTime, partialTick, ticksPerFrame, working),
                poseStack,
                buffers,
                packedLight,
                packedOverlay
        );
        poseStack.popPose();
    }

    private static void renderSteamTurbine(
            double animationTick,
            boolean working,
            PoseStack poseStack,
            MultiBufferSource buffers,
            int packedLight,
            int packedOverlay
    ) {
        float angle = working ? (float) (animationTick * 18.0D % 360.0D) : 0.0F;
        poseStack.pushPose();
        poseStack.translate(0.5D, 1.5D, 0.0D);
        for (int blade = 0; blade < STEAM_TURBINE_BLADE_COUNT; blade++) {
            poseStack.pushPose();
            poseStack.mulPose(Axis.ZP.rotationDegrees(angle + blade * 360.0F / STEAM_TURBINE_BLADE_COUNT));
            poseStack.translate(-1.5D, -1.5D, 0.0D);
            MachineRenderHelper.renderBakedModel(
                    LegacyBakedModels.STEAM_TURBINE_BLADE,
                    poseStack,
                    buffers,
                    packedLight,
                    packedOverlay
            );
            poseStack.popPose();
        }
        poseStack.popPose();
    }

    private static void renderSolarPanel(
            AdvancedMultiblockBlockEntity machine,
            long gameTime,
            PoseStack poseStack,
            MultiBufferSource buffers,
            int packedLight,
            int packedOverlay
    ) {
        float normalizedDay = (gameTime % 24_000L) / 12_000.0F;
        float angle = normalizedDay > 1.0F ? 0.0F : (normalizedDay * 2.0F - 1.0F) * 30.0F;
        if (machine.facing().getAxisDirection() == net.minecraft.core.Direction.AxisDirection.POSITIVE) {
            angle = -angle;
        }
        poseStack.pushPose();
        poseStack.translate(0.5D, 0.6875D, 0.5D);
        poseStack.mulPose(Axis.XP.rotationDegrees(angle));
        poseStack.translate(-0.5D, -0.6875D, -0.5D);
        MachineRenderHelper.renderBakedModel(
                LegacyBakedModels.SOLAR_PANEL_TRACKING,
                poseStack,
                buffers,
                packedLight,
                packedOverlay
        );
        poseStack.popPose();
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
