package committee.nova.mods.magneticraft.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import committee.nova.mods.magneticraft.content.computer.MiningRobotBlockEntity;
import committee.nova.mods.magneticraft.content.computer.ProgrammableBlock;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

/** Dynamic propeller and drill overlay for the server-authoritative mining robot. */
public final class MiningRobotRenderer implements BlockEntityRenderer<MiningRobotBlockEntity> {
    private static final double[][] ROTOR_OFFSETS = {
            {0.22D, 0.82D, 0.22D},
            {0.78D, 0.82D, 0.22D},
            {0.22D, 0.82D, 0.78D},
            {0.78D, 0.82D, 0.78D}
    };

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
        for (int index = 0; index < ROTOR_OFFSETS.length; index++) {
            double[] offset = ROTOR_OFFSETS[index];
            poseStack.pushPose();
            poseStack.translate(offset[0], offset[1], offset[2]);
            poseStack.mulPose(Axis.YP.rotationDegrees(rotation + index * 45.0F));
            poseStack.scale(0.22F, 0.05F, 0.42F);
            MachineRenderHelper.renderItem(new ItemStack(Items.IRON_INGOT), poseStack, buffers,
                    packedLight, packedOverlay, index);
            poseStack.popPose();
        }

        poseStack.pushPose();
        poseStack.translate(0.5D, 0.38D, 0.06D);
        poseStack.mulPose(Axis.XP.rotationDegrees(90.0F));
        poseStack.mulPose(Axis.ZP.rotationDegrees(moving ? rotation * 1.5F : 0.0F));
        poseStack.scale(0.30F, 0.30F, 0.30F);
        MachineRenderHelper.renderItem(new ItemStack(Items.DIAMOND_PICKAXE), poseStack, buffers,
                packedLight, packedOverlay, 5);
        poseStack.popPose();
        poseStack.popPose();
    }
}
