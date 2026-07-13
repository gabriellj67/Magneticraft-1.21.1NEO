package committee.nova.mods.magneticraft.client;

import com.mojang.blaze3d.vertex.PoseStack;
import committee.nova.mods.magneticraft.content.network.logistics.ConveyorBeltBlockEntity;
import committee.nova.mods.magneticraft.content.network.module.ConveyorBeltModule;
import committee.nova.mods.magneticraft.content.network.module.ConveyorRoute;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.world.item.ItemDisplayContext;

/**
 * Renders only dynamic parcel items; the belt body remains a static model.
 */
public final class ConveyorBeltRenderer implements BlockEntityRenderer<ConveyorBeltBlockEntity> {
    public ConveyorBeltRenderer(BlockEntityRendererProvider.Context context) {
    }

    @Override
    public void render(
            ConveyorBeltBlockEntity belt,
            float partialTick,
            PoseStack poseStack,
            MultiBufferSource buffers,
            int packedLight,
            int packedOverlay
    ) {
        var facing = belt.facing();
        var lateral = facing.getClockWise();
        long gameTick = belt.getLevel() == null ? 0L : belt.getLevel().getGameTime();
        float interpolationTicks = AnimationMath.boundedSnapshotAge(
                gameTick,
                belt.belt().clientSnapshotTick(),
                partialTick,
                4.0F,
                belt.belt().clientMovementEnabled()
        );
        int seed = 0;
        for (ConveyorBeltModule.ParcelView parcel : belt.belt().parcels()) {
            float interpolatedProgress = parcel.locked()
                    ? parcel.progress()
                    : parcel.progress() + interpolationTicks * parcel.route().speedPixelsPerTick();
            ConveyorRoute.PixelPosition position = parcel.route().position(interpolatedProgress);
            double along = 0.5D - position.z() / ConveyorBeltModule.MAX_PROGRESS;
            double lateralOffset = position.x() / ConveyorBeltModule.MAX_PROGRESS - 0.5D;
            double x = 0.5D + facing.getStepX() * along + lateral.getStepX() * lateralOffset;
            double z = 0.5D + facing.getStepZ() * along + lateral.getStepZ() * lateralOffset;

            poseStack.pushPose();
            poseStack.translate(x, 0.28D, z);
            poseStack.scale(0.35F, 0.35F, 0.35F);
            Minecraft.getInstance().getItemRenderer().renderStatic(
                    parcel.stack(),
                    ItemDisplayContext.GROUND,
                    packedLight,
                    packedOverlay,
                    poseStack,
                    buffers,
                    belt.getLevel(),
                    seed++
            );
            poseStack.popPose();
        }
    }
}
