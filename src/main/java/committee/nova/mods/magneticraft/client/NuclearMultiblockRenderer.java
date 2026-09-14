package committee.nova.mods.magneticraft.client;

import com.mojang.blaze3d.vertex.PoseStack;
import committee.nova.mods.magneticraft.content.machine.framework.MachineBlockEntity;
import committee.nova.mods.magneticraft.content.nuclear.NuclearMultiblockBounds;
import committee.nova.mods.magneticraft.content.nuclear.reactor.NuclearReactorControllerBlock;
import committee.nova.mods.magneticraft.content.nuclear.reactor.NuclearReactorControllerBlockEntity;
import committee.nova.mods.magneticraft.content.nuclear.spentfuel.SpentFuelPoolControllerBlock;
import committee.nova.mods.magneticraft.content.nuclear.spentfuel.SpentFuelPoolControllerBlockEntity;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.core.Direction;
import net.minecraft.world.phys.AABB;

/** Renders one scalable assembled scene in place of the formed nuclear structure blocks. */
public final class NuclearMultiblockRenderer<T extends MachineBlockEntity> implements BlockEntityRenderer<T> {
    public NuclearMultiblockRenderer(BlockEntityRendererProvider.Context context) {
    }

    @Override
    public AABB getRenderBoundingBox(T machine) {
        if (machine instanceof NuclearReactorControllerBlockEntity reactor) {
            if (!reactor.getBlockState().getValue(NuclearReactorControllerBlock.FORMED)) {
                return new AABB(reactor.getBlockPos());
            }
            return NuclearMultiblockBounds.renderBounds(
                    reactor.getBlockPos(), reactor.facing(), reactor.renderWidth(), reactor.renderHeight(), reactor.renderLength()
            );
        }
        if (machine instanceof SpentFuelPoolControllerBlockEntity pool) {
            if (!pool.getBlockState().getValue(SpentFuelPoolControllerBlock.FORMED)) {
                return new AABB(pool.getBlockPos());
            }
            return NuclearMultiblockBounds.renderBounds(
                    pool.getBlockPos(), pool.facing(), pool.renderWidth(), pool.renderHeight(), pool.renderLength()
            );
        }
        return BlockEntityRenderer.super.getRenderBoundingBox(machine);
    }

    @Override
    public void render(
            T machine,
            float partialTick,
            PoseStack poseStack,
            MultiBufferSource buffers,
            int packedLight,
            int packedOverlay
    ) {
        if (machine instanceof NuclearReactorControllerBlockEntity reactor) {
            if (!reactor.getBlockState().getValue(NuclearReactorControllerBlock.FORMED)) {
                return;
            }
            renderScene(
                    LegacySceneModels.PRESSURIZED_WATER_REACTOR,
                    reactor.facing(),
                    reactor.renderWidth(),
                    reactor.renderHeight(),
                    reactor.renderLength(),
                    reactor,
                    poseStack,
                    buffers,
                    packedLight,
                    packedOverlay
            );
            return;
        }
        if (machine instanceof SpentFuelPoolControllerBlockEntity pool
                && pool.getBlockState().getValue(SpentFuelPoolControllerBlock.FORMED)) {
            renderScene(
                    LegacySceneModels.SPENT_FUEL_POOL,
                    pool.facing(),
                    pool.renderWidth(),
                    pool.renderHeight(),
                    pool.renderLength(),
                    pool,
                    poseStack,
                    buffers,
                    packedLight,
                    packedOverlay
            );
        }
    }

    private static void renderScene(
            LegacySceneModels.Part model,
            Direction facing,
            int width,
            int height,
            int length,
            MachineBlockEntity controller,
            PoseStack poseStack,
            MultiBufferSource buffers,
            int packedLight,
            int packedOverlay
    ) {
        if (width <= 0 || height <= 0 || length <= 0) {
            return;
        }
        int sceneLight = MachineRenderHelper.surroundingLight(
                controller.getLevel(), controller.getBlockPos(), packedLight
        );
        poseStack.pushPose();
        MachineRenderHelper.faceMachine(poseStack, facing);
        poseStack.translate(-Math.floorDiv(width, 2), -1.0D, 0.0D);
        poseStack.scale(width, height, length);
        LegacySceneModels.render(
                model, null, 0.0D, poseStack, buffers, sceneLight, packedOverlay
        );
        poseStack.popPose();
    }

    @Override
    public boolean shouldRenderOffScreen(T machine) {
        return true;
    }

    @Override
    public int getViewDistance() {
        return 160;
    }
}
