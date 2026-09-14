package committee.nova.mods.magneticraft.client.model;

import com.mojang.math.Transformation;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.renderer.block.model.ItemOverrides;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.client.resources.model.Material;
import net.minecraft.client.resources.model.ModelBaker;
import net.minecraft.client.resources.model.ModelState;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.inventory.InventoryMenu;
import net.neoforged.neoforge.client.RenderTypeGroup;
import net.neoforged.neoforge.client.model.IModelBuilder;
import net.neoforged.neoforge.client.model.geometry.IGeometryBakingContext;
import net.neoforged.neoforge.client.model.geometry.SimpleUnbakedGeometry;
import net.neoforged.neoforge.client.model.pipeline.QuadBakingVertexConsumer;
import org.joml.Matrix3f;
import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.joml.Vector4f;

import java.util.Set;
import java.util.function.Function;

/** NeoForge geometry adapter that bakes a selected MCX/glTF scene into block-atlas quads. */
public final class LegacySceneGeometry extends SimpleUnbakedGeometry<LegacySceneGeometry> {
    private final ModelScene scene;
    private final Set<Integer> selectedNodes;
    private final ModelTransform sourceTransform;
    private final ModelRenderManifest manifest;

    LegacySceneGeometry(
            ModelScene scene,
            ModelSceneSelection selection,
            ModelTransform sourceTransform,
            ModelRenderManifest manifest
    ) {
        this.scene = scene;
        selectedNodes = selection.select(scene);
        this.sourceTransform = sourceTransform;
        this.manifest = manifest;
    }

    @Override
    public BakedModel bake(
            IGeometryBakingContext context,
            ModelBaker baker,
            Function<Material, TextureAtlasSprite> spriteGetter,
            ModelState modelState,
            ItemOverrides overrides
    ) {
        TextureAtlasSprite particle = spriteGetter.apply(context.getMaterial("particle"));
        ResourceLocation renderTypeHint = context.getRenderTypeHint();
        if (renderTypeHint == null) {
            renderTypeHint = inferredRenderType(scene.alphaMode());
        }
        RenderTypeGroup renderTypes = context.getRenderType(renderTypeHint);
        IModelBuilder<?> builder = IModelBuilder.of(
                context.useAmbientOcclusion(),
                context.useBlockLight(),
                context.isGui3d(),
                context.getTransforms(),
                overrides,
                particle,
                renderTypes
        );
        addQuads(context, builder, baker, spriteGetter, modelState);
        return builder.build();
    }

    static ResourceLocation inferredRenderType(ModelScene.AlphaMode alphaMode) {
        return ResourceLocation.withDefaultNamespace(switch (alphaMode) {
            case OPAQUE -> "solid";
            case MASK -> "cutout";
            case BLEND -> "translucent";
        });
    }

    @Override
    protected void addQuads(
            IGeometryBakingContext owner,
            IModelBuilder<?> modelBuilder,
            ModelBaker baker,
            Function<Material, TextureAtlasSprite> spriteGetter,
            ModelState modelTransform
    ) {
        Transformation rootTransform = owner.getRootTransform();
        Transformation transform = rootTransform.isIdentity()
                ? modelTransform.getRotation()
                : modelTransform.getRotation().compose(rootTransform);
        Transformation cornerTransform = transform.isIdentity() ? transform : transform.blockCenterToCorner();
        for (int root : scene.rootNodes()) {
            bakeNode(root, sourceTransform.matrix(), cornerTransform, modelBuilder, spriteGetter);
        }
    }

    private void bakeNode(
            int nodeIndex,
            Matrix4f parentTransform,
            Transformation rootTransform,
            IModelBuilder<?> modelBuilder,
            Function<Material, TextureAtlasSprite> spriteGetter
    ) {
        ModelScene.Node node = scene.node(nodeIndex);
        Matrix4f worldTransform = new Matrix4f(parentTransform).mul(node.transform().matrix());
        if (selectedNodes.contains(nodeIndex)) {
            for (ModelScene.Primitive primitive : node.primitives()) {
                bakePrimitive(node.name(), primitive, worldTransform, rootTransform, modelBuilder, spriteGetter);
            }
        }
        for (int child : node.children()) {
            bakeNode(child, worldTransform, rootTransform, modelBuilder, spriteGetter);
        }
    }

    private void bakePrimitive(
            String nodeName,
            ModelScene.Primitive primitive,
            Matrix4f worldTransform,
            Transformation rootTransform,
            IModelBuilder<?> modelBuilder,
            Function<Material, TextureAtlasSprite> spriteGetter
    ) {
        ResourceLocation texture = ResourceLocation.tryParse(primitive.texture());
        if (texture == null) {
            texture = ResourceLocation.withDefaultNamespace("missingno");
        }
        TextureAtlasSprite sprite = spriteGetter.apply(new Material(InventoryMenu.BLOCK_ATLAS, texture));
        float[] positions = primitive.positions();
        float[] textureCoordinates = primitive.textureCoordinates();
        float[] normals = primitive.normals();
        Matrix3f worldNormalTransform = worldTransform.normal(new Matrix3f());
        for (int face = 0; face < primitive.faceCount(); face++) {
            QuadBakingVertexConsumer consumer = new QuadBakingVertexConsumer();
            consumer.setSprite(sprite);
            consumer.setTintIndex(manifest.tintIndex(nodeName, primitive.materialName()));
            consumer.setShade(true);
            consumer.setHasAmbientOcclusion(scene.ambientOcclusion());
            Direction direction = null;
            for (int vertex = 0; vertex < 4; vertex++) {
                int positionOffset = face * 12 + vertex * 3;
                int textureOffset = face * 8 + vertex * 2;
                Vector4f position = new Vector4f(
                        positions[positionOffset],
                        positions[positionOffset + 1],
                        positions[positionOffset + 2],
                        1.0F
                );
                worldTransform.transform(position);
                rootTransform.transformPosition(position);
                Vector3f normal = new Vector3f(
                        normals[positionOffset],
                        normals[positionOffset + 1],
                        normals[positionOffset + 2]
                );
                worldNormalTransform.transform(normal).normalize();
                rootTransform.transformNormal(normal);
                normal.normalize();
                if (direction == null) {
                    direction = Direction.getNearest(normal.x, normal.y, normal.z);
                    consumer.setDirection(direction);
                }
                consumer.addVertex(position.x, position.y, position.z);
                consumer.setColor(1.0F, 1.0F, 1.0F, 1.0F);
                consumer.setUv(
                        sprite.getU(textureCoordinates[textureOffset]),
                        sprite.getV(textureCoordinates[textureOffset + 1])
                );
                consumer.setUv2(0, 0);
                consumer.setNormal(normal.x, normal.y, normal.z);
            }
            BakedQuad quad = consumer.bakeQuad();
            if (primitive.cullFace() == null) {
                modelBuilder.addUnculledFace(quad);
            } else {
                modelBuilder.addCulledFace(direction, quad);
            }
        }
    }
}
