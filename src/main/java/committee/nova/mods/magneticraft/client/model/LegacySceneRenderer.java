package committee.nova.mods.magneticraft.client.model;

import com.google.gson.JsonParseException;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import committee.nova.mods.magneticraft.Magneticraft;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.inventory.InventoryMenu;
import org.jetbrains.annotations.Nullable;
import org.joml.Matrix4f;

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
        render(
                source,
                selection,
                sourceTransform,
                animationName,
                animationSeconds,
                poseStack,
                buffers,
                packedLight,
                packedOverlay,
                RenderStyle.DEFAULT
        );
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
            int packedOverlay,
            RenderStyle style
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
        VertexConsumer consumer = buffers.getBuffer(
                style.translucent()
                        ? RenderType.entityTranslucent(InventoryMenu.BLOCK_ATLAS)
                        : RenderType.entityCutoutNoCull(InventoryMenu.BLOCK_ATLAS)
        );

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
                    packedOverlay,
                    style
            );
        }
        poseStack.popPose();
    }

    @Nullable
    public static Matrix4f nodeTransform(
            ResourceLocation source,
            ModelTransform sourceTransform,
            String animationName,
            double animationSeconds,
            String nodeName
    ) {
        ModelScene scene;
        try {
            scene = LegacyModelLoader.INSTANCE.load(source);
        } catch (JsonParseException exception) {
            reportOnce(source + ":load", "Unable to inspect legacy scene " + source, exception);
            return null;
        }

        ModelScene.Animation animation = animationName == null
                ? null
                : scene.animation(animationName).orElse(null);
        Map<Integer, ModelTransform> animatedTransforms = ModelAnimationSampler.sample(
                scene,
                animation,
                animationSeconds
        );
        Matrix4f rootTransform = sourceTransform.matrix();
        for (int root : scene.rootNodes()) {
            Matrix4f result = nodeTransform(
                    scene,
                    root,
                    nodeName,
                    animatedTransforms,
                    rootTransform
            );
            if (result != null) {
                return result;
            }
        }
        reportOnce(
                source + ":node:" + nodeName,
                "Legacy scene " + source + " has no node named " + nodeName,
                null
        );
        return null;
    }

    @Nullable
    private static Matrix4f nodeTransform(
            ModelScene scene,
            int nodeIndex,
            String nodeName,
            Map<Integer, ModelTransform> animatedTransforms,
            Matrix4f parentTransform
    ) {
        ModelScene.Node node = scene.node(nodeIndex);
        ModelTransform transform = animatedTransforms.getOrDefault(nodeIndex, node.transform());
        Matrix4f currentTransform = new Matrix4f(parentTransform).mul(transform.matrix());
        if (nodeName.equals(node.name())) {
            return currentTransform;
        }
        for (int child : node.children()) {
            Matrix4f result = nodeTransform(
                    scene,
                    child,
                    nodeName,
                    animatedTransforms,
                    currentTransform
            );
            if (result != null) {
                return result;
            }
        }
        return null;
    }

    private static void renderNode(
            ModelScene scene,
            int nodeIndex,
            Set<Integer> selectedNodes,
            Map<Integer, ModelTransform> animatedTransforms,
            PoseStack poseStack,
            VertexConsumer consumer,
            int packedLight,
            int packedOverlay,
            RenderStyle style
    ) {
        ModelScene.Node node = scene.node(nodeIndex);
        ModelTransform transform = animatedTransforms.getOrDefault(nodeIndex, node.transform());
        poseStack.pushPose();
        poseStack.mulPoseMatrix(transform.matrix());
        if (selectedNodes.contains(nodeIndex)) {
            for (ModelScene.Primitive primitive : node.primitives()) {
                renderPrimitive(primitive, poseStack, consumer, packedLight, packedOverlay, style);
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
                    packedOverlay,
                    style
            );
        }
        poseStack.popPose();
    }

    private static void renderPrimitive(
            ModelScene.Primitive primitive,
            PoseStack poseStack,
            VertexConsumer consumer,
            int packedLight,
            int packedOverlay,
            RenderStyle style
    ) {
        ResourceLocation texture = style.textureOverride() == null
                ? ResourceLocation.tryParse(primitive.texture())
                : style.textureOverride();
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
        int tint = style.tint();
        float alpha = ((tint >>> 24) & 0xFF) / 255.0F;
        float red = ((tint >>> 16) & 0xFF) / 255.0F;
        float green = ((tint >>> 8) & 0xFF) / 255.0F;
        float blue = (tint & 0xFF) / 255.0F;
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
                    .color(red, green, blue, alpha)
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

    public record RenderStyle(int tint, @Nullable ResourceLocation textureOverride, boolean translucent) {
        public static final RenderStyle DEFAULT = new RenderStyle(0xFFFFFFFF, null, false);

        public static RenderStyle tinted(int tint) {
            return new RenderStyle(tint, null, ((tint >>> 24) & 0xFF) < 0xFF);
        }

        public static RenderStyle texture(ResourceLocation texture) {
            return new RenderStyle(0xFFFFFFFF, texture, false);
        }

        public static RenderStyle translucentTexture(ResourceLocation texture) {
            return new RenderStyle(0xFFFFFFFF, texture, true);
        }
    }
}
