package committee.nova.mods.magneticraft.client;

import com.mojang.blaze3d.vertex.PoseStack;
import committee.nova.mods.magneticraft.content.network.module.LogisticsTubeModule;
import committee.nova.mods.magneticraft.content.network.pneumatic.PneumaticTubeBlockEntity;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.core.Direction;
import net.minecraft.world.item.ItemDisplayContext;

/**
 * Dynamic in-tube item renderer; tube geometry remains a generated static model.
 */
public final class PneumaticTubeRenderer implements BlockEntityRenderer<PneumaticTubeBlockEntity> {
    public PneumaticTubeRenderer(BlockEntityRendererProvider.Context context) {
    }

    @Override
    public void render(
            PneumaticTubeBlockEntity tube,
            float partialTick,
            PoseStack poseStack,
            MultiBufferSource buffers,
            int packedLight,
            int packedOverlay
    ) {
        long gameTick = tube.getLevel() == null ? 0L : tube.getLevel().getGameTime();
        float interpolationTicks = AnimationMath.boundedSnapshotAge(
                gameTick,
                tube.logistics().clientSnapshotTick(),
                partialTick,
                4.0F,
                tube.logistics().clientMovementEnabled()
        );
        int seed = 0;
        for (LogisticsTubeModule.TravelingItemView item : tube.logistics().itemsSnapshot()) {
            double[] offset = offset(item, interpolationTicks);
            poseStack.pushPose();
            poseStack.translate(0.5D + offset[0], 0.5D + offset[1], 0.5D + offset[2]);
            poseStack.scale(0.25F, 0.25F, 0.25F);
            Minecraft.getInstance().getItemRenderer().renderStatic(
                    item.stack(),
                    ItemDisplayContext.GROUND,
                    packedLight,
                    packedOverlay,
                    poseStack,
                    buffers,
                    tube.getLevel(),
                    seed++
            );
            poseStack.popPose();
        }
    }

    private static double[] offset(LogisticsTubeModule.TravelingItemView item, float interpolationTicks) {
        double progress = Math.min(
                LogisticsTubeModule.MAX_PROGRESS,
                item.progress() + interpolationTicks * item.progressPerTick()
        );
        Direction direction;
        double distance;
        if (progress <= LogisticsTubeModule.CENTER_PROGRESS) {
            direction = item.incoming();
            distance = 0.42D * (1.0D - progress / LogisticsTubeModule.CENTER_PROGRESS);
        } else {
            direction = item.outgoing();
            distance = direction == null
                    ? 0.0D
                    : 0.42D * ((progress - LogisticsTubeModule.CENTER_PROGRESS) / LogisticsTubeModule.CENTER_PROGRESS);
        }
        if (direction == null) {
            return new double[]{0.0D, 0.0D, 0.0D};
        }
        return new double[]{
                direction.getStepX() * distance,
                direction.getStepY() * distance,
                direction.getStepZ() * distance
        };
    }
}
