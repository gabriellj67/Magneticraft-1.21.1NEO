package committee.nova.mods.magneticraft.client;

import com.mojang.blaze3d.platform.Lighting;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import committee.nova.mods.magneticraft.Magneticraft;
import committee.nova.mods.magneticraft.client.guide.GuideRepository;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.block.BlockRenderDispatcher;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.Property;
import net.neoforged.neoforge.client.model.data.ModelData;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/** Interactive, resource-pack-driven three-dimensional multiblock preview. */
final class GuideStructurePreview {
    private static final float DEFAULT_YAW = 42.0F;
    private static final float DEFAULT_PITCH = 28.0F;
    private static final float MIN_ZOOM = 0.60F;
    private static final float MAX_ZOOM = 1.70F;

    private final Map<GuideRepository.LegendEntry, BlockState> stateCache = new HashMap<>();
    private GuideRepository.MultiblockGuide cachedGuide;
    private int left;
    private int top;
    private int right;
    private int bottom;
    private float yaw = DEFAULT_YAW;
    private float pitch = DEFAULT_PITCH;
    private float zoom = 1.0F;
    private boolean dragging;
    private double lastMouseX;
    private double lastMouseY;

    void render(
            GuiGraphics graphics,
            GuideRepository.MultiblockGuide guide,
            int visibleLayer,
            int left,
            int top,
            int right,
            int bottom
    ) {
        this.left = left;
        this.top = top;
        this.right = right;
        this.bottom = bottom;
        prepareGuide(guide);
        MachineScreenLayout.drawInset(graphics, left, top, right - left, bottom - top);
        graphics.fill(left + 2, top + 2, right - 2, bottom - 2, 0xFF080E14);

        Dimensions dimensions = dimensions(guide);
        List<StructureBlock> blocks = visibleBlocks(guide, visibleLayer);
        if (blocks.isEmpty() || right - left < 16 || bottom - top < 16) {
            return;
        }

        graphics.flush();
        graphics.enableScissor(left + 2, top + 2, right - 2, bottom - 2);
        PoseStack poseStack = graphics.pose();
        poseStack.pushPose();
        try {
            RenderSystem.enableDepthTest();
            Lighting.setupForEntityInInventory();
            float scale = fitScale(right - left, bottom - top, dimensions) * zoom;
            poseStack.translate((left + right) / 2.0D, (top + bottom) / 2.0D, 300.0D);
            poseStack.scale(scale, -scale, scale);
            poseStack.mulPose(Axis.XP.rotationDegrees(pitch));
            poseStack.mulPose(Axis.YP.rotationDegrees(yaw));
            poseStack.translate(
                    -dimensions.width() / 2.0D,
                    -dimensions.height() / 2.0D,
                    -dimensions.depth() / 2.0D
            );

            BlockRenderDispatcher dispatcher = Minecraft.getInstance().getBlockRenderer();
            for (StructureBlock block : blocks) {
                BlockState state = stateFor(guide, block.legend());
                if (state == null || state.isAir()) {
                    renderOutline(
                            poseStack,
                            graphics,
                            block,
                            0.25F,
                            0.75F,
                            1.0F,
                            block.y() == visibleLayer ? 0.90F : 0.42F
                    );
                    continue;
                }
                poseStack.pushPose();
                poseStack.translate(block.x(), block.y(), block.z());
                dispatcher.renderSingleBlock(
                        state,
                        poseStack,
                        graphics.bufferSource(),
                        LightTexture.FULL_BRIGHT,
                        OverlayTexture.NO_OVERLAY,
                        ModelData.EMPTY,
                        null
                );
                poseStack.popPose();
                if (block.y() == visibleLayer) {
                    renderOutline(poseStack, graphics, block, 0.22F, 0.83F, 0.91F, 0.34F);
                }
            }
            graphics.flush();
        } finally {
            poseStack.popPose();
            Lighting.setupFor3DItems();
            graphics.disableScissor();
        }
    }

    BlockState stateFor(
            GuideRepository.MultiblockGuide guide,
            GuideRepository.LegendEntry legend
    ) {
        prepareGuide(guide);
        if (legend == null || legend.ignored()) {
            return null;
        }
        return stateCache.computeIfAbsent(legend, GuideStructurePreview::resolveState);
    }

    void resetView() {
        yaw = DEFAULT_YAW;
        pitch = DEFAULT_PITCH;
        zoom = 1.0F;
        dragging = false;
    }

    boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button != 0 || !contains(mouseX, mouseY)) {
            return false;
        }
        dragging = true;
        lastMouseX = mouseX;
        lastMouseY = mouseY;
        return true;
    }

    boolean mouseDragged(double mouseX, double mouseY, int button) {
        if (!dragging || button != 0) {
            return false;
        }
        yaw += (float) ((mouseX - lastMouseX) * 0.75D);
        pitch = Mth.clamp(pitch + (float) ((mouseY - lastMouseY) * 0.55D), -75.0F, 75.0F);
        lastMouseX = mouseX;
        lastMouseY = mouseY;
        return true;
    }

    boolean mouseReleased(int button) {
        if (button != 0 || !dragging) {
            return false;
        }
        dragging = false;
        return true;
    }

    boolean mouseScrolled(double mouseX, double mouseY, double delta) {
        if (!contains(mouseX, mouseY) || delta == 0.0D) {
            return false;
        }
        zoom = Mth.clamp(zoom + (float) Math.copySign(0.10D, delta), MIN_ZOOM, MAX_ZOOM);
        return true;
    }

    private boolean contains(double mouseX, double mouseY) {
        return mouseX >= left && mouseX < right && mouseY >= top && mouseY < bottom;
    }

    private void prepareGuide(GuideRepository.MultiblockGuide guide) {
        if (cachedGuide != guide) {
            cachedGuide = guide;
            stateCache.clear();
        }
    }

    static Dimensions dimensions(GuideRepository.MultiblockGuide guide) {
        int width = 1;
        int depth = 1;
        for (List<String> layer : guide.layers()) {
            depth = Math.max(depth, layer.size());
            for (String row : layer) {
                width = Math.max(width, row.length());
            }
        }
        return new Dimensions(width, guide.layers().size(), depth);
    }

    static List<StructureBlock> visibleBlocks(
            GuideRepository.MultiblockGuide guide,
            int visibleLayer
    ) {
        int lastLayer = Mth.clamp(visibleLayer, 0, guide.layers().size() - 1);
        List<StructureBlock> blocks = new ArrayList<>();
        for (int y = 0; y <= lastLayer; y++) {
            List<String> rows = guide.layers().get(y);
            for (int z = 0; z < rows.size(); z++) {
                String row = rows.get(z);
                for (int x = 0; x < row.length(); x++) {
                    GuideRepository.LegendEntry legend = guide.legend().get(row.charAt(x));
                    if (legend != null && !legend.ignored()) {
                        blocks.add(new StructureBlock(x, y, z, legend));
                    }
                }
            }
        }
        return List.copyOf(blocks);
    }

    static float fitScale(int viewportWidth, int viewportHeight, Dimensions dimensions) {
        float horizontalSpan = Math.max(1.0F, (dimensions.width() + dimensions.depth()) * 0.72F);
        float verticalSpan = Math.max(
                1.0F,
                dimensions.height() + (dimensions.width() + dimensions.depth()) * 0.25F
        );
        float horizontalScale = Math.max(1, viewportWidth - 20) / horizontalSpan;
        float verticalScale = Math.max(1, viewportHeight - 20) / verticalSpan;
        return Mth.clamp(Math.min(horizontalScale, verticalScale), 5.0F, 32.0F);
    }

    private static BlockState resolveState(GuideRepository.LegendEntry legend) {
        ResourceLocation blockId = legend.blockId();
        if (blockId == null || !BuiltInRegistries.BLOCK.containsKey(blockId)) {
            Magneticraft.LOGGER.warn(
                    "Guide structure rule {} references missing block {}",
                    legend.rule(),
                    blockId
            );
            return Blocks.BARRIER.defaultBlockState();
        }
        Block block = BuiltInRegistries.BLOCK.get(blockId);
        BlockState state = block.defaultBlockState();
        for (Map.Entry<String, String> entry : legend.properties().entrySet()) {
            Property<?> property = block.getStateDefinition().getProperty(entry.getKey());
            if (property == null) {
                Magneticraft.LOGGER.warn(
                        "Guide structure block {} has no property {}",
                        blockId,
                        entry.getKey()
                );
                return Blocks.BARRIER.defaultBlockState();
            }
            state = setProperty(state, property, entry.getValue(), blockId);
        }
        return state;
    }

    private static <T extends Comparable<T>> BlockState setProperty(
            BlockState state,
            Property<T> property,
            String value,
            ResourceLocation blockId
    ) {
        return property.getValue(value)
                .map(parsed -> state.setValue(property, parsed))
                .orElseGet(() -> {
                    Magneticraft.LOGGER.warn(
                            "Guide structure block {} has invalid {} value {}",
                            blockId,
                            property.getName(),
                            value
                    );
                    return Blocks.BARRIER.defaultBlockState();
                });
    }

    private static void renderOutline(
            PoseStack poseStack,
            GuiGraphics graphics,
            StructureBlock block,
            float red,
            float green,
            float blue,
            float alpha
    ) {
        poseStack.pushPose();
        poseStack.translate(block.x(), block.y(), block.z());
        LevelRenderer.renderLineBox(
                poseStack,
                graphics.bufferSource().getBuffer(RenderType.lines()),
                0.025D,
                0.025D,
                0.025D,
                0.975D,
                0.975D,
                0.975D,
                red,
                green,
                blue,
                alpha
        );
        poseStack.popPose();
    }

    record Dimensions(int width, int height, int depth) {
    }

    record StructureBlock(int x, int y, int z, GuideRepository.LegendEntry legend) {
    }
}
