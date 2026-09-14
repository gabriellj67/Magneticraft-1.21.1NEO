package committee.nova.mods.magneticraft.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import committee.nova.mods.magneticraft.content.machine.framework.module.FluidTankModule;
import committee.nova.mods.magneticraft.content.multiblock.AdvancedMultiblockBlockEntity;
import committee.nova.mods.magneticraft.content.multiblock.MultiblockCell;
import committee.nova.mods.magneticraft.content.multiblock.MultiblockDefinition;
import committee.nova.mods.magneticraft.content.multiblock.MultiblockRule;
import committee.nova.mods.magneticraft.content.multiblock.MultiblockBounds;
import committee.nova.mods.magneticraft.content.multiblock.MultiblockTransform;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.block.BlockRenderDispatcher;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.neoforged.neoforge.client.model.data.ModelData;

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
    public AABB getRenderBoundingBox(AdvancedMultiblockBlockEntity blockEntity) {
        return MultiblockBounds.renderBounds(
                blockEntity.getBlockPos(),
                blockEntity.facing(),
                blockEntity.mirrored(),
                blockEntity.definition()
        );
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
            renderHologram(machine, poseStack, buffers);
            return;
        }
        int sceneLight = MachineRenderHelper.surroundingLight(
                machine.getLevel(),
                machine.getBlockPos(),
                packedLight
        );
        if (machine.definition() == MultiblockDefinition.POLYMERIZER
                || machine.definition() == MultiblockDefinition.STIRLING_GENERATOR) {
            renderRegisteredStructure(machine, poseStack, buffers, sceneLight, packedOverlay);
            return;
        }
        poseStack.pushPose();
        MachineRenderHelper.faceMachine(poseStack, legacyModelFacing(machine.facing()));
        renderScene(machine, partialTick, poseStack, buffers, sceneLight, packedOverlay);
        renderFluids(machine, poseStack, buffers, sceneLight);
        poseStack.popPose();
    }

    private static void renderHologram(
            AdvancedMultiblockBlockEntity machine,
            PoseStack poseStack,
            MultiBufferSource buffers
    ) {
        if (!machine.hologramEnabled() || machine.getLevel() == null) {
            return;
        }
        BlockRenderDispatcher dispatcher = Minecraft.getInstance().getBlockRenderer();
        BlockPos controller = machine.getBlockPos();
        Direction facing = machine.facing();
        for (MultiblockCell cell : machine.definition().requiredCells()) {
            MultiblockRule rule = cell.rule();
            if (rule == MultiblockRule.CONTROLLER || rule == MultiblockRule.IGNORE) {
                continue;
            }
            BlockPos worldPosition = MultiblockTransform.worldPosition(
                    controller, cell.offset(), machine.definition().center(), facing, machine.mirrored()
            );
            if (!machine.getLevel().hasChunk(
                    worldPosition.getX() >> 4, worldPosition.getZ() >> 4)) {
                continue;
            }
            BlockState actual = machine.getLevel().getBlockState(worldPosition);
            boolean matches = rule.matches(actual, machine.getBlockState().getBlock(), facing);
            int x = worldPosition.getX() - controller.getX();
            int y = worldPosition.getY() - controller.getY();
            int z = worldPosition.getZ() - controller.getZ();
            if (matches) {
                renderOutline(poseStack, buffers, x, y, z, 0.2F, 1.0F, 0.35F, 0.42F);
                continue;
            }
            boolean obstructedAir = rule == MultiblockRule.AIR;
            if (obstructedAir) {
                renderOutline(poseStack, buffers, x, y, z, 1.0F, 0.2F, 0.2F, 0.9F);
                continue;
            }
            BlockState preview = rule.previewState(facing);
            float red = actual.isAir() ? 0.25F : 1.0F;
            float green = actual.isAir() ? 0.9F : 0.2F;
            float blue = actual.isAir() ? 1.0F : 0.2F;
            renderGhostBlock(
                    dispatcher, preview, x, y, z, red, green, blue,
                    actual.isAir() ? 0.38F : 0.52F, poseStack, buffers
            );
            renderOutline(poseStack, buffers, x, y, z, red, green, blue, 0.85F);
        }
    }

    private static void renderGhostBlock(
            BlockRenderDispatcher dispatcher,
            BlockState state,
            int x,
            int y,
            int z,
            float red,
            float green,
            float blue,
            float alpha,
            PoseStack poseStack,
            MultiBufferSource buffers
    ) {
        poseStack.pushPose();
        poseStack.translate(x + 0.01D, y + 0.01D, z + 0.01D);
        poseStack.scale(0.98F, 0.98F, 0.98F);
        BakedModel model = dispatcher.getBlockModel(state);
        RenderType renderType = RenderType.translucentMovingBlock();
        VertexConsumer consumer = new AlphaVertexConsumer(buffers.getBuffer(renderType), alpha);
        dispatcher.getModelRenderer().renderModel(
                poseStack.last(), consumer, state, model, red, green, blue,
                LightTexture.FULL_BRIGHT, 0, ModelData.EMPTY, renderType
        );
        poseStack.popPose();
    }

    private static void renderOutline(
            PoseStack poseStack,
            MultiBufferSource buffers,
            int x,
            int y,
            int z,
            float red,
            float green,
            float blue,
            float alpha
    ) {
        poseStack.pushPose();
        poseStack.translate(x, y, z);
        LevelRenderer.renderLineBox(
                poseStack, buffers.getBuffer(RenderType.lines()),
                0.002D, 0.002D, 0.002D, 0.998D, 0.998D, 0.998D,
                red, green, blue, alpha
        );
        poseStack.popPose();
    }

    private static final class AlphaVertexConsumer implements VertexConsumer {
        private final VertexConsumer delegate;
        private final float alpha;

        private AlphaVertexConsumer(VertexConsumer delegate, float alpha) {
            this.delegate = delegate;
            this.alpha = alpha;
        }

        @Override
        public VertexConsumer addVertex(float x, float y, float z) {
            delegate.addVertex(x, y, z);
            return this;
        }

        @Override
        public VertexConsumer setColor(int red, int green, int blue, int alpha) {
            delegate.setColor(red, green, blue, Math.round(alpha * this.alpha));
            return this;
        }

        @Override
        public VertexConsumer setUv(float u, float v) {
            delegate.setUv(u, v);
            return this;
        }

        @Override
        public VertexConsumer setUv1(int u, int v) {
            delegate.setUv1(u, v);
            return this;
        }

        @Override
        public VertexConsumer setUv2(int u, int v) {
            delegate.setUv2(u, v);
            return this;
        }

        @Override
        public VertexConsumer setNormal(float x, float y, float z) {
            delegate.setNormal(x, y, z);
            return this;
        }
    }

    /**
     * Legacy scenes use the 1.12 placement convention, which stored the player's
     * horizontal facing. The current controller block stores its opposite.
     */
    static Direction legacyModelFacing(Direction controllerFacing) {
        return controllerFacing.getOpposite();
    }

    private static void renderScene(
            AdvancedMultiblockBlockEntity machine,
            float partialTick,
            PoseStack poseStack,
            MultiBufferSource buffers,
            int packedLight,
            int packedOverlay
    ) {
        long gameTime = machine.getLevel() == null ? 0L : machine.getLevel().getGameTime();
        double animationTick = gameTime % 1_048_576L + partialTick;
        double animationSeconds = machine.working() ? animationTick / 20.0D : 0.0D;
        MultiblockDefinition definition = machine.definition();
        switch (definition) {
            case BIG_COMBUSTION_CHAMBER -> {
                translateAndRender(LegacySceneModels.BIG_COMBUSTION_CHAMBER_BODY, null, 0.0D,
                        0.0D, 0.0D, -1.0D, poseStack, buffers, packedLight, packedOverlay);
                translateAndRender(
                        machine.working()
                                ? LegacySceneModels.BIG_COMBUSTION_CHAMBER_FIRE_ON
                                : LegacySceneModels.BIG_COMBUSTION_CHAMBER_FIRE_OFF,
                        null,
                        0.0D,
                        0.0D,
                        0.0D,
                        -1.0D,
                        poseStack,
                        buffers,
                        packedLight,
                        packedOverlay
                );
            }
            case BIG_ELECTRIC_FURNACE -> translateAndRender(
                    LegacySceneModels.BIG_ELECTRIC_FURNACE, null, 0.0D,
                    0.0D, 0.0D, -1.0D, poseStack, buffers, packedLight, packedOverlay
            );
            case BIG_STEAM_BOILER -> renderPart(
                    LegacySceneModels.BIG_STEAM_BOILER, null, 0.0D,
                    poseStack, buffers, packedLight, packedOverlay
            );
            case CONTAINER -> translateAndRender(
                    LegacySceneModels.SHIPPING_CONTAINER, null, 0.0D,
                    0.0D, 0.0D, -3.0D, poseStack, buffers, packedLight, packedOverlay
            );
            case GRINDER -> translateAndRender(
                    LegacySceneModels.GRINDER, machine.working() ? "animation" : null, animationSeconds,
                    0.0D, 0.0D, -1.0D, poseStack, buffers, packedLight, packedOverlay
            );
            case MECHANICAL_GRINDING_MILL -> {
                poseStack.pushPose();
                poseStack.translate(0.125D, -2.0D, -0.125D);
                poseStack.scale(0.75F, 0.75F, 0.75F);
                translateAndRender(
                        LegacySceneModels.GRINDER,
                        machine.working() ? "animation" : null,
                        animationSeconds,
                        0.0D, 0.0D, -1.0D,
                        poseStack, buffers, packedLight, packedOverlay
                );
                poseStack.popPose();
            }
            case SIEVE -> {
                poseStack.pushPose();
                MachineRenderHelper.rotateAroundCenterY(poseStack, 180.0F);
                translateAndRender(
                        LegacySceneModels.SIEVE, machine.working() ? "animation" : null, animationSeconds,
                        0.0D, 0.0D, 2.0D, poseStack, buffers, packedLight, packedOverlay
                );
                poseStack.popPose();
            }
            case HYDRAULIC_PRESS -> translateAndRender(
                    LegacySceneModels.HYDRAULIC_PRESS, machine.working() ? "animation" : null, animationSeconds,
                    0.0D, 0.0D, -1.0D, poseStack, buffers, packedLight, packedOverlay
            );
            case OIL_HEATER -> translateAndRender(
                    LegacySceneModels.OIL_HEATER, null, 0.0D,
                    -1.0D, 0.0D, 0.0D, poseStack, buffers, packedLight, packedOverlay
            );
            case POLYMERIZER -> throw new IllegalStateException("Polymerizer uses its structure renderer");
            case PUMPJACK -> {
                poseStack.pushPose();
                MachineRenderHelper.rotateAroundCenterY(poseStack, 90.0F);
                translateAndRender(
                        LegacySceneModels.PUMPJACK, null, 0.0D,
                        1.0D, 0.0D, 0.0D, poseStack, buffers, packedLight, packedOverlay
                );
                poseStack.popPose();
            }
            case REFINERY -> translateAndRender(
                    LegacySceneModels.REFINERY, null, 0.0D,
                    0.0D, 0.0D, -1.0D, poseStack, buffers, packedLight, packedOverlay
            );
            case SHELVING_UNIT -> renderShelvingUnit(machine, poseStack, buffers, packedLight, packedOverlay);
            case SOLAR_MIRROR -> renderSolarMirror(machine, gameTime, poseStack, buffers, packedLight, packedOverlay);
            case SOLAR_PANEL -> renderSolarPanel(
                    machine, gameTime, poseStack, buffers, packedLight, packedOverlay
            );
            case SOLAR_TOWER -> translateAndRender(
                    LegacySceneModels.SOLAR_TOWER, null, 0.0D,
                    0.0D, 0.0D, -1.0D, poseStack, buffers, packedLight, packedOverlay
            );
            case STIRLING_GENERATOR ->
                    throw new IllegalStateException("Stirling generator uses its structure renderer");
            case STEAM_ENGINE -> {
                translateAndRender(
                        LegacySceneModels.STEAM_ENGINE_BODY, machine.working() ? "animation" : null, animationSeconds,
                        -1.0D, 0.0D, -1.0D, poseStack, buffers, packedLight, packedOverlay
                );
                translateAndRender(
                        LegacySceneModels.STEAM_ENGINE_LID, null, 0.0D,
                        -1.0D, 0.0D, -1.0D, poseStack, buffers, packedLight, packedOverlay
                );
            }
            case STEAM_TURBINE -> renderSteamTurbine(
                    animationTick, machine.working(), poseStack, buffers, packedLight, packedOverlay
            );
        }
    }

    /** Renders restored machines directly from the same registered cells used by formation. */
    private static void renderRegisteredStructure(
            AdvancedMultiblockBlockEntity machine,
            PoseStack poseStack,
            MultiBufferSource buffers,
            int packedLight,
            int packedOverlay
    ) {
        BlockRenderDispatcher dispatcher = Minecraft.getInstance().getBlockRenderer();
        BlockPos controller = machine.getBlockPos();
        renderStructureBlock(
                dispatcher,
                machine.getBlockState().setValue(
                        committee.nova.mods.magneticraft.content.multiblock.AdvancedMultiblockBlock.FORMED,
                        false
                ),
                0,
                0,
                0,
                poseStack,
                buffers,
                packedLight,
                packedOverlay
        );
        for (MultiblockCell cell : machine.definition().memberCells()) {
            if (cell.rule() == MultiblockRule.CONTROLLER) {
                continue;
            }
            BlockPos worldPosition = MultiblockTransform.worldPosition(
                    controller,
                    cell.offset(),
                    machine.definition().center(),
                    machine.facing(),
                    machine.mirrored()
            );
            renderStructureBlock(
                    dispatcher,
                    cell.rule().previewState(machine.facing()),
                    worldPosition.getX() - controller.getX(),
                    worldPosition.getY() - controller.getY(),
                    worldPosition.getZ() - controller.getZ(),
                    poseStack,
                    buffers,
                    packedLight,
                    packedOverlay
            );
        }
    }

    private static void renderStructureBlock(
            BlockRenderDispatcher dispatcher,
            BlockState state,
            int x,
            int y,
            int z,
            PoseStack poseStack,
            MultiBufferSource buffers,
            int packedLight,
            int packedOverlay
    ) {
        if (state.isAir()) {
            return;
        }
        poseStack.pushPose();
        poseStack.translate(x, y, z);
        dispatcher.renderSingleBlock(
                state,
                poseStack,
                buffers,
                packedLight,
                packedOverlay,
                ModelData.EMPTY,
                null
        );
        poseStack.popPose();
    }

    private static void translateAndRender(
            LegacySceneModels.Part part,
            String animation,
            double animationSeconds,
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
        renderPart(part, animation, animationSeconds, poseStack, buffers, packedLight, packedOverlay);
        poseStack.popPose();
    }

    private static void renderPart(
            LegacySceneModels.Part part,
            String animation,
            double animationSeconds,
            PoseStack poseStack,
            MultiBufferSource buffers,
            int packedLight,
            int packedOverlay
    ) {
        LegacySceneModels.render(
                part,
                animation,
                animationSeconds,
                poseStack,
                buffers,
                packedLight,
                packedOverlay
        );
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
        poseStack.translate(-1.0D, 0.0D, 0.0D);
        renderPart(
                LegacySceneModels.STEAM_TURBINE_BODY,
                null,
                0.0D,
                poseStack,
                buffers,
                packedLight,
                packedOverlay
        );
        poseStack.translate(1.5D, 1.5D, 0.0D);
        for (int blade = 0; blade < STEAM_TURBINE_BLADE_COUNT; blade++) {
            poseStack.pushPose();
            poseStack.mulPose(Axis.ZP.rotationDegrees(angle + blade * 360.0F / STEAM_TURBINE_BLADE_COUNT));
            poseStack.translate(-1.5D, -1.5D, 0.0D);
            renderPart(
                    LegacySceneModels.STEAM_TURBINE_BLADE,
                    null,
                    0.0D,
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
        MachineRenderHelper.rotateAroundCenterY(poseStack, -90.0F);
        poseStack.translate(-1.0D, 0.0D, 0.0D);
        renderPart(LegacySceneModels.SOLAR_PANEL_BODY, null, 0.0D,
                poseStack, buffers, packedLight, packedOverlay);
        if (machine.facing().getAxis() == net.minecraft.core.Direction.Axis.X) {
            renderTiltedPanel(LegacySceneModels.SOLAR_PANEL_X_LEFT, angle,
                    1.5D, 11.0D / 16.0D, 0.5D, Axis.ZP,
                    poseStack, buffers, packedLight, packedOverlay);
            renderTiltedPanel(LegacySceneModels.SOLAR_PANEL_X_MIDDLE, angle,
                    0.5D, 11.0D / 16.0D, 0.5D, Axis.ZP,
                    poseStack, buffers, packedLight, packedOverlay);
            renderTiltedPanel(LegacySceneModels.SOLAR_PANEL_X_RIGHT, angle,
                    -0.5D, 11.0D / 16.0D, 0.5D, Axis.ZP,
                    poseStack, buffers, packedLight, packedOverlay);
        } else {
            renderTiltedPanel(LegacySceneModels.SOLAR_PANEL_Z_FRONT, angle,
                    0.0D, 0.75D, 1.25D, Axis.XP,
                    poseStack, buffers, packedLight, packedOverlay);
            renderTiltedPanel(LegacySceneModels.SOLAR_PANEL_Z_BACK, angle,
                    0.0D, 0.75D, -0.25D, Axis.XP,
                    poseStack, buffers, packedLight, packedOverlay);
        }
        poseStack.popPose();
    }

    private static void renderTiltedPanel(
            LegacySceneModels.Part part,
            float angle,
            double pivotX,
            double pivotY,
            double pivotZ,
            Axis axis,
            PoseStack poseStack,
            MultiBufferSource buffers,
            int packedLight,
            int packedOverlay
    ) {
        poseStack.pushPose();
        poseStack.translate(pivotX, pivotY, pivotZ);
        poseStack.mulPose(axis.rotationDegrees(angle));
        poseStack.translate(-pivotX, -pivotY + 0.1D, -pivotZ);
        renderPart(part, null, 0.0D, poseStack, buffers, packedLight, packedOverlay);
        poseStack.popPose();
    }

    private static void renderShelvingUnit(
            AdvancedMultiblockBlockEntity machine,
            PoseStack poseStack,
            MultiBufferSource buffers,
            int packedLight,
            int packedOverlay
    ) {
        renderPart(LegacySceneModels.SHELVING_UNIT_BODY, null, 0.0D,
                poseStack, buffers, packedLight, packedOverlay);
        int installed = machine.shelvingStorage() == null
                ? 0
                : Math.min(24, machine.shelvingStorage().installedChests());
        for (int crate = 1; crate <= installed; crate++) {
            renderPart(LegacySceneModels.shelvingCrate(crate), null, 0.0D,
                    poseStack, buffers, packedLight, packedOverlay);
        }
    }

    private static void renderSolarMirror(
            AdvancedMultiblockBlockEntity machine,
            long gameTime,
            PoseStack poseStack,
            MultiBufferSource buffers,
            int packedLight,
            int packedOverlay
    ) {
        poseStack.pushPose();
        MachineRenderHelper.rotateAroundCenterY(poseStack, 180.0F);
        poseStack.translate(0.0D, 0.0D, 1.0D);
        renderPart(LegacySceneModels.SOLAR_MIRROR_BODY, null, 0.0D,
                poseStack, buffers, packedLight, packedOverlay);
        poseStack.popPose();

        float[] angles = solarMirrorAngles(machine, gameTime);
        poseStack.pushPose();
        poseStack.translate(0.5D, 1.8125D, 0.5D);
        poseStack.mulPose(Axis.YP.rotationDegrees(angles[0]));
        poseStack.mulPose(Axis.ZP.rotationDegrees(angles[1]));
        poseStack.translate(-0.5D, -1.8125D, -0.5D);
        renderPart(LegacySceneModels.SOLAR_MIRROR_MOVING, null, 0.0D,
                poseStack, buffers, packedLight, packedOverlay);
        poseStack.popPose();
    }

    private static float[] solarMirrorAngles(AdvancedMultiblockBlockEntity machine, long gameTime) {
        if (machine.solarTowerPosition() == null) {
            return new float[]{0.0F, 0.0F};
        }
        double dx = machine.solarTowerPosition().getX() - machine.getBlockPos().getX();
        double dy = machine.solarTowerPosition().getY() - machine.getBlockPos().getY();
        double dz = machine.solarTowerPosition().getZ() - machine.getBlockPos().getZ();
        double targetLength = Math.sqrt(dx * dx + dy * dy + dz * dz);
        if (targetLength < 1.0E-6D) {
            return new float[]{0.0F, 0.0F};
        }
        dx /= targetLength;
        dy /= targetLength;
        dz /= targetLength;
        double sunAngle = ((gameTime % 24_000L) / 12_000.0D) * Math.PI - Math.PI / 2.0D;
        double sunX = -Math.sin(sunAngle);
        double sunY = Math.cos(sunAngle);
        double normalX = sunX + dx;
        double normalY = sunY + dy;
        double normalZ = dz;
        double normalLength = Math.sqrt(normalX * normalX + normalY * normalY + normalZ * normalZ);
        if (normalLength < 1.0E-6D) {
            return new float[]{0.0F, 0.0F};
        }
        normalX /= normalLength;
        normalY /= normalLength;
        normalZ /= normalLength;
        double planeLength = Math.sqrt(normalX * normalX + normalZ * normalZ);
        float yaw = (float) Math.toDegrees(Math.atan2(normalX, normalZ) + Math.PI / 2.0D);
        float pitch = (float) Math.toDegrees(Math.atan2(planeLength, normalY));
        return new float[]{yaw, pitch > 90.0F ? 0.0F : pitch};
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

    /** Released multiblock renderers were global because one controller owns the whole model. */
    @Override
    public boolean shouldRenderOffScreen(AdvancedMultiblockBlockEntity machine) {
        return true;
    }

    @Override
    public int getViewDistance() {
        return 128;
    }
}
