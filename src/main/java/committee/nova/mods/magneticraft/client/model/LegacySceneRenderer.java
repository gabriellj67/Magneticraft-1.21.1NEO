package committee.nova.mods.magneticraft.client.model;

import com.google.gson.JsonParseException;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import committee.nova.mods.magneticraft.Magneticraft;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.Sheets;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.inventory.InventoryMenu;

import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/** Runtime renderer shared by MCX named parts and animated glTF node trees. */
public final class LegacySceneRenderer {
    private static final Set<String> REPORTED_ERRORS = ConcurrentHashMap.newKeySet();
    private static final ConcurrentMap<ResourceLocation, ConcurrentMap<ModelSceneSelection, SelectedNodes>>
            SELECTED_NODES = new ConcurrentHashMap<>();

    private LegacySceneRenderer() {
    }

    static void clearCaches() {
        SELECTED_NODES.clear();
        REPORTED_ERRORS.clear();
    }

    public static void render(
            ResourceLocation source,
            ModelSceneSelection selection,
            ModelTransform sourceTransform,
            String animationName,
            double animationSeconds,
            PoseStack poseStack,
            MultiBufferSource buffers,
            int packedLight,
            int packedOverlay
    ) {
        ModelScene scene;
        try {
            scene = LegacyModelLoader.INSTANCE.load(source);
        } catch (JsonParseException exception) {
            reportOnce(source + ":load", "Unable to render legacy scene " + source, exception);
            return;
        }

        ModelScene.Animation animation = null;
        if (animationName != null) {
            animation = scene.animation(animationName).orElse(null);
            if (animation == null) {
                reportOnce(
                        source + ":animation:" + animationName,
                        "Legacy scene " + source + " has no animation named " + animationName,
                        null
                );
            }
        }
        Map<Integer, ModelTransform> animatedTransforms = ModelAnimationSampler.sample(
                scene,
                animation,
                animationSeconds
        );
        Set<Integer> selectedNodes = selectedNodes(source, selection, scene);
        VertexConsumer consumer = buffers.getBuffer(Sheets.solidBlockSheet());

        poseStack.pushPose();
        poseStack.mulPoseMatrix(sourceTransform.matrix());
        for (int root : scene.rootNodes()) {
            renderNode(
                    scene,
                    root,
                    selectedNodes,
                    animatedTransforms,
                    poseStack,
                    consumer,
                    packedLight,
                    packedOverlay
            );
        }
        poseStack.popPose();
    }

    private static void renderNode(
            ModelScene scene,
            int nodeIndex,
            Set<Integer> selectedNodes,
            Map<Integer, ModelTransform> animatedTransforms,
            PoseStack poseStack,
            VertexConsumer consumer,
            int packedLight,
            int packedOverlay
    ) {
        ModelScene.Node node = scene.node(nodeIndex);
        ModelTransform transform = animatedTransforms.getOrDefault(nodeIndex, node.transform());
        poseStack.pushPose();
        poseStack.mulPoseMatrix(transform.matrix());
        if (selectedNodes.contains(nodeIndex)) {
            for (ModelScene.Primitive primitive : node.primitives()) {
                renderPrimitive(primitive, poseStack, consumer, packedLight, packedOverlay);
            }
        }
        for (int child : node.children()) {
            renderNode(
                    scene,
                    child,
                    selectedNodes,
                    animatedTransforms,
                    poseStack,
                    consumer,
                    packedLight,
                    packedOverlay
            );
        }
        poseStack.popPose();
    }

    private static void renderPrimitive(
            ModelScene.Primitive primitive,
            PoseStack poseStack,
            VertexConsumer consumer,
            int packedLight,
            int packedOverlay
    ) {
        ResourceLocation texture = ResourceLocation.tryParse(primitive.texture());
        if (texture == null) {
            texture = ResourceLocation.withDefaultNamespace("missingno");
        }
        TextureAtlasSprite sprite = Minecraft.getInstance()
                .getTextureAtlas(InventoryMenu.BLOCK_ATLAS)
                .apply(texture);
        float[] positions = primitive.positionsView();
        float[] textureCoordinates = primitive.textureCoordinatesView();
        float[] normals = primitive.normalsView();
        PoseStack.Pose pose = poseStack.last();
        int vertexCount = positions.length / 3;
        for (int vertex = 0; vertex < vertexCount; vertex++) {
            int positionOffset = vertex * 3;
            int textureOffset = vertex * 2;
            consumer.vertex(
                            pose.pose(),
                            positions[positionOffset],
                            positions[positionOffset + 1],
                            positions[positionOffset + 2]
                    )
                    .color(1.0F, 1.0F, 1.0F, 1.0F)
                    .uv(
                            sprite.getU(textureCoordinates[textureOffset] * 16.0F),
                            sprite.getV(textureCoordinates[textureOffset + 1] * 16.0F)
                    )
                    .overlayCoords(packedOverlay)
                    .uv2(packedLight)
                    .normal(
                            pose.normal(),
                            normals[positionOffset],
                            normals[positionOffset + 1],
                            normals[positionOffset + 2]
                    )
                    .endVertex();
        }
    }

    private static void reportOnce(String key, String message, Exception exception) {
        if (!REPORTED_ERRORS.add(key)) {
            return;
        }
        if (exception == null) {
            Magneticraft.LOGGER.error(message);
        } else {
            Magneticraft.LOGGER.error(message, exception);
        }
    }

    private static Set<Integer> selectedNodes(
            ResourceLocation source,
            ModelSceneSelection selection,
            ModelScene scene
    ) {
        ConcurrentMap<ModelSceneSelection, SelectedNodes> sourceSelections = SELECTED_NODES.computeIfAbsent(
                source,
                ignored -> new ConcurrentHashMap<>()
        );
        SelectedNodes selected = sourceSelections.compute(
                selection,
                (ignored, current) -> current != null && current.scene == scene
                        ? current
                        : new SelectedNodes(scene, selection.select(scene))
        );
        return selected.nodes;
    }

    private record SelectedNodes(ModelScene scene, Set<Integer> nodes) {
    }
}
