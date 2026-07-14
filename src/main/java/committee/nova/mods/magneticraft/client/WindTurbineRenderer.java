package committee.nova.mods.magneticraft.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import committee.nova.mods.magneticraft.content.machine.windturbine.WindTurbineBlockEntity;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;

/** Renders the bounded historical rotor from the server-synchronized wind speed. */
public final class WindTurbineRenderer implements BlockEntityRenderer<WindTurbineBlockEntity> {
    private static final double ROTOR_X = 0.5D;
    private static final double ROTOR_Y = 0.375D;
    private static final double ROTOR_Z = 0.75D;

    public WindTurbineRenderer(BlockEntityRendererProvider.Context context) {
    }

    @Override
    public void render(
            WindTurbineBlockEntity turbine,
            float partialTick,
            PoseStack poseStack,
            MultiBufferSource buffers,
            int packedLight,
            int packedOverlay
    ) {
        long gameTick = turbine.getLevel() == null ? 0L : turbine.getLevel().getGameTime();
        double speed = Math.max(0.0D, Math.min(5.0D, turbine.wind().rotationSpeed()));
        float rotation = (float) ((gameTick + Math.max(0.0F, Math.min(1.0F, partialTick))) * speed * 7.2D % 360.0D);

        poseStack.pushPose();
        MachineRenderHelper.faceMachine(poseStack, turbine.facing());
        poseStack.translate(ROTOR_X, ROTOR_Y, ROTOR_Z);
        poseStack.mulPose(Axis.ZP.rotationDegrees(rotation));
        poseStack.translate(-ROTOR_X, -ROTOR_Y, -ROTOR_Z);
        LegacySceneModels.render(
                LegacySceneModels.WIND_TURBINE_ROTOR,
                null,
                0.0D,
                poseStack,
                buffers,
                packedLight,
                packedOverlay
        );
        poseStack.popPose();
    }

    @Override
    public boolean shouldRenderOffScreen(WindTurbineBlockEntity turbine) {
        return true;
    }

    @Override
    public int getViewDistance() {
        return 96;
    }
}
