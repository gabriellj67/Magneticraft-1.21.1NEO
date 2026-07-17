package committee.nova.mods.magneticraft.client;

import com.mojang.blaze3d.vertex.PoseStack;
import committee.nova.mods.magneticraft.Magneticraft;
import committee.nova.mods.magneticraft.client.model.LegacySceneRenderer;
import committee.nova.mods.magneticraft.content.machine.framework.module.FluidTankModule;
import committee.nova.mods.magneticraft.content.machine.singleblock.SingleBlockMachineBlock;
import committee.nova.mods.magneticraft.content.machine.singleblock.SingleBlockMachineBlockEntity;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;

/** Restores the original ModelLoader block-entity rendering path for legacy single-block machines. */
public final class SingleBlockMachineRenderer implements BlockEntityRenderer<SingleBlockMachineBlockEntity> {
    private static final float PIXEL = 1.0F / 16.0F;
    private static final ResourceLocation WATER_FLOW = ResourceLocation.withDefaultNamespace("block/water_flow");
    private static final ResourceLocation SMALL_TANK_IN = Magneticraft.id("block/fluid_machines/small_tank_in");
    private static final ResourceLocation SMALL_TANK_OUT = Magneticraft.id("block/fluid_machines/small_tank_out");
    private static final FeedItemTransform[] FEED_ITEMS = {
            new FeedItemTransform(1, 2.0F, 1.1F, 6.0F, 90.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F),
            new FeedItemTransform(1, 7.5F, 1.5F, 7.0F, 90.0F, 30.0F, 0.0F, 0.0F, 0.0F, 0.0F),
            new FeedItemTransform(2, 0.0F, 2.0F, 12.0F, 90.0F, -30.0F, 0.0F, 16.0F, 0.0F, 0.0F),
            new FeedItemTransform(2, 16.5F, 2.5F, 12.5F, 90.0F, -30.0F, 0.0F, 16.0F, 0.0F, 0.0F),
            new FeedItemTransform(3, 1.0F, 1.0F, 7.0F, -10.0F, 22.0F, 4.0F, 0.0F, 0.0F, 0.0F),
            new FeedItemTransform(3, 0.0F, 1.0F, 15.0F, 10.0F, -22.0F, -4.0F, 16.0F, 0.0F, 0.0F),
            new FeedItemTransform(3, -2.5F, -2.0F, -4.0F, 20.0F, -90.0F, 5.0F, 0.0F, 0.0F, 8.0F),
            new FeedItemTransform(4, 5.0F, 0.0F, 4.5F, -10.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F),
            new FeedItemTransform(4, 7.0F, 0.0F, 11.0F, 14.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F),
            new FeedItemTransform(4, 12.0F, 0.0F, 10.0F, 14.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F)
    };

    public SingleBlockMachineRenderer(BlockEntityRendererProvider.Context context) {
    }

    @Override
    public void render(
            SingleBlockMachineBlockEntity machine,
            float partialTick,
            PoseStack poseStack,
            MultiBufferSource buffers,
            int packedLight,
            int packedOverlay
    ) {
        if (machine.definition().doubleLength()
                && !machine.getBlockState().getValue(SingleBlockMachineBlock.MASTER)) {
            return;
        }
        int sceneLight = MachineRenderHelper.surroundingLight(
                machine.getLevel(),
                machine.getBlockPos(),
                packedLight
        );
        switch (machine.definition()) {
            case SLUICE_BOX -> renderSluice(machine, poseStack, buffers, sceneLight, packedOverlay);
            case FEEDING_TROUGH -> renderFeedingTrough(machine, poseStack, buffers, sceneLight, packedOverlay);
            case SMALL_TANK -> renderSmallTank(machine, poseStack, buffers, sceneLight, packedOverlay);
            case COMBUSTION_CHAMBER -> renderCombustionChamber(machine, poseStack, buffers, sceneLight, packedOverlay);
            case STEAM_BOILER -> renderPart(
                    LegacySceneModels.STEAM_BOILER, null, 0.0D, poseStack, buffers, sceneLight, packedOverlay
            );
            case GASIFICATION_UNIT -> renderGasificationUnit(
                    machine, poseStack, buffers, sceneLight, packedOverlay
            );
            case INSERTER -> renderInserter(machine, partialTick, poseStack, buffers, sceneLight, packedOverlay);
            case ELECTRIC_ENGINE -> renderElectricEngine(machine, partialTick, poseStack, buffers, sceneLight, packedOverlay);
            default -> {
            }
        }
    }

    @Override
    public boolean shouldRenderOffScreen(SingleBlockMachineBlockEntity machine) {
        return machine.definition().doubleLength();
    }

    private static void renderSluice(
            SingleBlockMachineBlockEntity machine,
            PoseStack poseStack,
            MultiBufferSource buffers,
            int packedLight,
            int packedOverlay
    ) {
        poseStack.pushPose();
        orientHorizontal(machine, poseStack, 180.0F);
        renderPart(LegacySceneModels.SLUICE_BOX_BODY, null, 0.0D, poseStack, buffers, packedLight, packedOverlay);

        ItemStack item = displayItem(machine);
        if (!item.isEmpty()) {
            float remaining = machine.progress() == 0
                    ? 1.0F
                    : Math.min(1.0F, machine.progress() / (float) SingleBlockMachineBlockEntity.SLUICE_DURATION);
            float itemLevel = Math.min(SingleBlockMachineBlockEntity.SLUICE_MAX_ITEMS, item.getCount())
                    / (float) SingleBlockMachineBlockEntity.SLUICE_MAX_ITEMS;
            poseStack.pushPose();
            poseStack.translate(0.0D, 4.0F * PIXEL * remaining * itemLevel, 0.0D);
            renderPart(LegacySceneModels.SLUICE_BOX_GRAVEL, null, 0.0D,
                    poseStack, buffers, packedLight, packedOverlay);
            poseStack.popPose();
        }
        if (machine.progress() > 0) {
            LegacySceneModels.render(
                    LegacySceneModels.SLUICE_BOX_WATER,
                    null,
                    0.0D,
                    poseStack,
                    buffers,
                    packedLight,
                    packedOverlay,
                    LegacySceneRenderer.RenderStyle.translucentTexture(WATER_FLOW)
            );
        }
        poseStack.popPose();
    }

    private static void renderFeedingTrough(
            SingleBlockMachineBlockEntity machine,
            PoseStack poseStack,
            MultiBufferSource buffers,
            int packedLight,
            int packedOverlay
    ) {
        poseStack.pushPose();
        orientHorizontal(machine, poseStack, 90.0F);
        renderPart(LegacySceneModels.FEEDING_TROUGH, null, 0.0D,
                poseStack, buffers, packedLight, packedOverlay);
        ItemStack item = displayItem(machine);
        int level = feedLevel(item);
        if (level > 0) {
            MachineRenderHelper.rotateAroundCenterY(poseStack, 90.0F);
            renderFeedSide(level, item, 0, poseStack, buffers, packedLight, packedOverlay);
            poseStack.mulPose(com.mojang.math.Axis.YP.rotationDegrees(180.0F));
            poseStack.translate(-1.0D, 0.0D, -2.0D);
            renderFeedSide(level, item, FEED_ITEMS.length, poseStack, buffers, packedLight, packedOverlay);
        }
        poseStack.popPose();
    }

    private static void renderSmallTank(
            SingleBlockMachineBlockEntity machine,
            PoseStack poseStack,
            MultiBufferSource buffers,
            int packedLight,
            int packedOverlay
    ) {
        LegacySceneModels.render(
                LegacySceneModels.SMALL_TANK_BASE,
                null,
                0.0D,
                poseStack,
                buffers,
                packedLight,
                packedOverlay,
                LegacySceneRenderer.RenderStyle.texture(machine.tankExportEnabled() ? SMALL_TANK_OUT : SMALL_TANK_IN)
        );
        renderPart(LegacySceneModels.SMALL_TANK_SHELL, null, 0.0D,
                poseStack, buffers, packedLight, packedOverlay);

        FluidTankModule tank = machine.primaryTank();
        if (tank == null || tank.tank().isEmpty()) {
            return;
        }
        float fill = Math.min(1.0F, tank.tank().getFluidAmount() / (float) tank.tank().getCapacity());
        float min = 0.001F;
        float minY = PIXEL + min;
        float maxY = PIXEL + 15.0F * PIXEL * fill - min;
        MachineRenderHelper.renderFluidCuboid(
                tank.tank().getFluid(),
                min,
                minY,
                min,
                1.0F - min,
                maxY,
                1.0F - min,
                poseStack,
                buffers,
                packedLight
        );
    }

    private static void renderCombustionChamber(
            SingleBlockMachineBlockEntity machine,
            PoseStack poseStack,
            MultiBufferSource buffers,
            int packedLight,
            int packedOverlay
    ) {
        poseStack.pushPose();
        orientHorizontal(machine, poseStack, 180.0F);
        renderPart(LegacySceneModels.COMBUSTION_CHAMBER_BODY, null, 0.0D,
                poseStack, buffers, packedLight, packedOverlay);
        if (machine.doorOpen()) {
            poseStack.translate(13.5F * PIXEL, 0.0D, 0.5F * PIXEL);
            poseStack.mulPose(com.mojang.math.Axis.YP.rotationDegrees(-135.0F));
            poseStack.translate(-13.5F * PIXEL, 0.0D, -0.5F * PIXEL);
        }
        renderPart(LegacySceneModels.COMBUSTION_CHAMBER_DOOR, null, 0.0D,
                poseStack, buffers, packedLight, packedOverlay);
        poseStack.popPose();
    }

    private static void renderGasificationUnit(
            SingleBlockMachineBlockEntity machine,
            PoseStack poseStack,
            MultiBufferSource buffers,
            int packedLight,
            int packedOverlay
    ) {
        double temperature = machine.heat() == null
                ? committee.nova.mods.magneticraft.system.network.heat.HeatNode.AMBIENT_TEMPERATURE_KELVIN
                : machine.heat().node().temperatureKelvin();
        LegacySceneModels.render(
                LegacySceneModels.GASIFICATION_UNIT,
                null,
                0.0D,
                poseStack,
                buffers,
                MachineRenderHelper.heatPackedLight(packedLight, temperature),
                packedOverlay,
                LegacySceneRenderer.RenderStyle.tinted(MachineRenderHelper.heatTint(temperature))
        );
    }

    private static void renderInserter(
            SingleBlockMachineBlockEntity machine,
            float partialTick,
            PoseStack poseStack,
            MultiBufferSource buffers,
            int packedLight,
            int packedOverlay
    ) {
        poseStack.pushPose();
        orientHorizontal(machine, poseStack, 180.0F);
        double time = animationSeconds(machine, partialTick);
        renderPart(LegacySceneModels.INSERTER, "animation0", time,
                poseStack, buffers, packedLight, packedOverlay);
        renderInserterItem(machine, time, poseStack, buffers, packedLight, packedOverlay);
        poseStack.popPose();
    }

    private static void renderElectricEngine(
            SingleBlockMachineBlockEntity machine,
            float partialTick,
            PoseStack poseStack,
            MultiBufferSource buffers,
            int packedLight,
            int packedOverlay
    ) {
        poseStack.pushPose();
        MachineRenderHelper.orientElectricEngine(
                poseStack,
                machine.getBlockState().getValue(SingleBlockMachineBlock.FACING)
        );
        renderPart(
                LegacySceneModels.ELECTRIC_ENGINE,
                "animation",
                animationSeconds(machine, partialTick),
                poseStack,
                buffers,
                packedLight,
                packedOverlay
        );
        poseStack.popPose();
    }

    private static void renderInserterItem(
            SingleBlockMachineBlockEntity machine,
            double animationSeconds,
            PoseStack poseStack,
            MultiBufferSource buffers,
            int packedLight,
            int packedOverlay
    ) {
        ItemStack item = displayItem(machine);
        if (item.isEmpty()) {
            return;
        }
        org.joml.Matrix4f itemTransform = LegacySceneModels.nodeTransform(
                LegacySceneModels.INSERTER,
                "animation0",
                animationSeconds,
                "item"
        );
        if (itemTransform == null) {
            return;
        }
        poseStack.pushPose();
        poseStack.mulPoseMatrix(itemTransform);
        poseStack.translate(0.0D, -7.5F * PIXEL, 0.0D);
        boolean threeDimensional = net.minecraft.client.Minecraft.getInstance()
                .getItemRenderer()
                .getModel(item, machine.getLevel(), null, 0)
                .isGui3d();
        if (threeDimensional) {
            poseStack.mulPose(com.mojang.math.Axis.YP.rotationDegrees(180.0F));
            poseStack.translate(0.0D, -1.9F * PIXEL, 0.0D);
            poseStack.scale(0.9F, 0.9F, 0.9F);
        } else {
            poseStack.scale(0.75F, 0.75F, 0.75F);
        }
        MachineRenderHelper.renderItem(
                item,
                ItemDisplayContext.GROUND,
                poseStack,
                buffers,
                packedLight,
                packedOverlay,
                0
        );
        poseStack.popPose();
    }

    private static void orientHorizontal(
            SingleBlockMachineBlockEntity machine,
            PoseStack poseStack,
            float offset
    ) {
        MachineRenderHelper.faceMachine(
                poseStack,
                machine.getBlockState().getValue(SingleBlockMachineBlock.FACING)
        );
        MachineRenderHelper.rotateAroundCenterY(poseStack, offset);
    }

    private static void renderFeedSide(
            int level,
            ItemStack item,
            int seedOffset,
            PoseStack poseStack,
            MultiBufferSource buffers,
            int packedLight,
            int packedOverlay
    ) {
        for (int index = 0; index < FEED_ITEMS.length; index++) {
            FeedItemTransform transform = FEED_ITEMS[index];
            if (level < transform.level()) {
                continue;
            }
            poseStack.pushPose();
            applyFeedItemTransform(poseStack, transform);
            MachineRenderHelper.renderItem(
                    item,
                    ItemDisplayContext.GROUND,
                    poseStack,
                    buffers,
                    packedLight,
                    packedOverlay,
                    seedOffset + index
            );
            poseStack.popPose();
        }
    }

    private static void applyFeedItemTransform(PoseStack poseStack, FeedItemTransform transform) {
        poseStack.translate(1.0D, 0.0D, 0.0D);
        poseStack.mulPose(com.mojang.math.Axis.YP.rotationDegrees(-90.0F));
        poseStack.translate(transform.x() * PIXEL, transform.y() * PIXEL, transform.z() * PIXEL);
        poseStack.translate(transform.pivotX() * PIXEL, transform.pivotY() * PIXEL, transform.pivotZ() * PIXEL);
        poseStack.mulPose(com.mojang.math.Axis.ZP.rotationDegrees(transform.rotationZ()));
        poseStack.mulPose(com.mojang.math.Axis.YP.rotationDegrees(transform.rotationY()));
        poseStack.mulPose(com.mojang.math.Axis.XP.rotationDegrees(transform.rotationX()));
        poseStack.translate(-transform.pivotX() * PIXEL, -transform.pivotY() * PIXEL, -transform.pivotZ() * PIXEL);
        poseStack.translate(4.0F * PIXEL, 0.0D, 0.0D);
    }

    private static int feedLevel(ItemStack item) {
        if (item.isEmpty()) {
            return 0;
        }
        if (item.getCount() > 32) {
            return 4;
        }
        if (item.getCount() > 16) {
            return 3;
        }
        return item.getCount() > 8 ? 2 : 1;
    }

    private static ItemStack displayItem(SingleBlockMachineBlockEntity machine) {
        return machine.inventory() == null ? ItemStack.EMPTY : machine.inventory().getStackInSlot(0);
    }

    private static double animationSeconds(SingleBlockMachineBlockEntity machine, float partialTick) {
        if (!machine.working() || machine.getLevel() == null) {
            return 0.0D;
        }
        float boundedPartialTick = Math.max(0.0F, Math.min(1.0F, partialTick));
        return (machine.getLevel().getGameTime() + boundedPartialTick) / 20.0D;
    }

    private static void renderPart(
            LegacySceneModels.Part part,
            String animationName,
            double animationSeconds,
            PoseStack poseStack,
            MultiBufferSource buffers,
            int packedLight,
            int packedOverlay
    ) {
        LegacySceneModels.render(
                part,
                animationName,
                animationSeconds,
                poseStack,
                buffers,
                packedLight,
                packedOverlay
        );
    }

    private record FeedItemTransform(
            int level,
            float x,
            float y,
            float z,
            float rotationX,
            float rotationY,
            float rotationZ,
            float pivotX,
            float pivotY,
            float pivotZ
    ) {
    }
}
