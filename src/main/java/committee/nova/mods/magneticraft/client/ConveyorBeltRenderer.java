package committee.nova.mods.magneticraft.client;

import com.mojang.blaze3d.vertex.PoseStack;
import committee.nova.mods.magneticraft.content.network.logistics.ConveyorBeltBlockEntity;
import committee.nova.mods.magneticraft.content.network.module.ConveyorBeltModule;
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
        int seed = 0;
        for (ConveyorBeltModule.ParcelView parcel : belt.belt().parcels()) {
            float progress = Math.min(ConveyorBeltModule.MAX_PROGRESS, parcel.progress() + partialTick);
            double along = progress / ConveyorBeltModule.MAX_PROGRESS - 0.5D;
            double laneOffset = parcel.lane() == ConveyorBeltModule.Lane.LEFT ? -0.18D : 0.18D;
            double x = 0.5D + facing.getStepX() * along + lateral.getStepX() * laneOffset;
            double z = 0.5D + facing.getStepZ() * along + lateral.getStepZ() * laneOffset;

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
