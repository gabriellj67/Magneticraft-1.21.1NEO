package committee.nova.mods.magneticraft.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import committee.nova.mods.magneticraft.content.computer.MiningRobotBlockEntity;
import committee.nova.mods.magneticraft.content.computer.ProgrammableBlock;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;

/** Dynamic propeller and drill overlay for the server-authoritative mining robot. */
public final class MiningRobotRenderer implements BlockEntityRenderer<MiningRobotBlockEntity> {
    public MiningRobotRenderer(BlockEntityRendererProvider.Context context) {
    }

    @Override
    public void render(
            MiningRobotBlockEntity robot,
            float partialTick,
            PoseStack poseStack,
            MultiBufferSource buffers,
            int packedLight,
            int packedOverlay
    ) {
        long gameTime = robot.getLevel() == null ? 0L : robot.getLevel().getGameTime();
        boolean moving = robot.visuallyRunning() || robot.isRelocating();
        float rotation = moving ? (gameTime % 10L + partialTick) * 36.0F : 0.0F;

        poseStack.pushPose();
        MachineRenderHelper.faceMachine(
                poseStack,
                robot.getBlockState().getValue(ProgrammableBlock.FACING)
        );
        poseStack.pushPose();
        poseStack.translate(0.5D, 0.34375D, 0.5D);
        poseStack.mulPose(Axis.ZP.rotationDegrees(rotation));
        poseStack.translate(-0.5D, -0.34375D, -0.5D);
        MachineRenderHelper.renderBakedModel(
                LegacyBakedModels.MINING_ROBOT_PROPELLERS,
                poseStack,
                buffers,
                packedLight,
                packedOverlay
        );
        poseStack.popPose();

        poseStack.pushPose();
        poseStack.translate(0.5D, 0.5D, 0.8125D);
        poseStack.mulPose(Axis.ZP.rotationDegrees(moving ? rotation * 1.5F : 0.0F));
        poseStack.translate(-0.5D, -0.5D, -0.8125D);
        MachineRenderHelper.renderBakedModel(
                LegacyBakedModels.MINING_ROBOT_DRILL,
                poseStack,
                buffers,
                packedLight,
                packedOverlay
        );
        poseStack.popPose();
        poseStack.popPose();
    }
}
