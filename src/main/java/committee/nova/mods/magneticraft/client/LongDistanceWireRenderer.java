package committee.nova.mods.magneticraft.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import committee.nova.mods.magneticraft.content.network.electric.ElectricPoleBlock;
import committee.nova.mods.magneticraft.content.network.electric.ElectricPoleBlockEntity;
import committee.nova.mods.magneticraft.content.network.module.LongDistanceEndpointModule;
import committee.nova.mods.magneticraft.content.network.module.LongDistanceWireHost;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.core.BlockPos;
import net.minecraft.core.SectionPos;
import net.minecraft.world.level.block.entity.BlockEntity;
import org.joml.Matrix3f;
import org.joml.Matrix4f;

/** Draws a bounded, server-synchronized wire snapshot without loading remote chunks. */
public final class LongDistanceWireRenderer<T extends BlockEntity & LongDistanceWireHost>
        implements BlockEntityRenderer<T> {
    static final int MAX_SEGMENTS = 32;
    private static final float WIRE_SPACING = 0.12F;

    public LongDistanceWireRenderer(BlockEntityRendererProvider.Context context) {
    }

    @Override
    public void render(
            T endpoint,
            float partialTick,
            PoseStack poseStack,
            MultiBufferSource buffers,
            int packedLight,
            int packedOverlay
    ) {
        renderHistoricalPole(endpoint, poseStack, buffers, packedLight, packedOverlay);
        BlockPos local = endpoint.getBlockPos();
        PoseStack.Pose pose = poseStack.last();
        VertexConsumer consumer = buffers.getBuffer(RenderType.lines());
        for (LongDistanceEndpointModule.WireView connection : endpoint.longDistance().clientConnections()) {
            BlockPos remote = connection.remotePosition();
            if (!shouldRenderFrom(local, remote, remoteWireHostIsLoaded(endpoint, remote))) {
                continue;
            }
            renderConnection(
                    local,
                    remote,
                    connection.port().wireCount(),
                    connection.tierId(),
                    pose,
                    consumer
            );
        }
    }

    private static void renderHistoricalPole(
            BlockEntity endpoint,
            PoseStack poseStack,
            MultiBufferSource buffers,
            int packedLight,
            int packedOverlay
    ) {
        if (!(endpoint instanceof ElectricPoleBlockEntity pole)) {
            return;
        }
        ElectricPoleBlock block = (ElectricPoleBlock) pole.getBlockState().getBlock();
        int orientation = pole.getBlockState().getValue(ElectricPoleBlock.DIRECTION).ordinal();
        poseStack.pushPose();
        poseStack.translate(0.5D, 0.5D, 0.5D);
        poseStack.mulPose(Axis.YP.rotationDegrees(orientation * 45.0F));
        poseStack.translate(-0.5D, -0.5D, -0.5D);
        LegacySceneModels.render(
                block.isTransformer()
                        ? LegacySceneModels.ELECTRIC_POLE_TRANSFORMER
                        : LegacySceneModels.ELECTRIC_POLE,
                null,
                0.0D,
                poseStack,
                buffers,
                packedLight,
                packedOverlay
        );
        poseStack.popPose();
    }

    static boolean shouldRenderFrom(BlockPos local, BlockPos remote, boolean remoteWireHostLoaded) {
        return !remoteWireHostLoaded || local.compareTo(remote) < 0;
    }

    private static boolean remoteWireHostIsLoaded(BlockEntity endpoint, BlockPos remote) {
        if (endpoint.getLevel() == null) {
            return false;
        }
        var remoteChunk = endpoint.getLevel().getChunkSource().getChunkNow(
                SectionPos.blockToSectionCoord(remote.getX()),
                SectionPos.blockToSectionCoord(remote.getZ())
        );
        return remoteChunk != null && remoteChunk.getBlockEntity(remote) instanceof LongDistanceWireHost;
    }

    private static void renderConnection(
            BlockPos local,
            BlockPos remote,
            int wireCount,
            ResourceLocation tierId,
            PoseStack.Pose pose,
            VertexConsumer consumer
    ) {
        float deltaX = remote.getX() - local.getX();
        float deltaY = remote.getY() - local.getY();
        float deltaZ = remote.getZ() - local.getZ();
        float horizontalLength = (float) Math.hypot(deltaX, deltaZ);
        float perpendicularX = horizontalLength > 1.0E-4F ? -deltaZ / horizontalLength : 1.0F;
        float perpendicularZ = horizontalLength > 1.0E-4F ? deltaX / horizontalLength : 0.0F;
        float distance = (float) Math.sqrt(deltaX * deltaX + deltaY * deltaY + deltaZ * deltaZ);
        int segmentCount = segmentCount(distance);
        float sag = Math.min(2.0F, distance * 0.08F);
        int color = ElectricalTierColors.color(tierId);
        float red = ((color >>> 16) & 0xFF) / 255.0F;
        float green = ((color >>> 8) & 0xFF) / 255.0F;
        float blue = (color & 0xFF) / 255.0F;

        for (int wire = 0; wire < wireCount; wire++) {
            float offset = (wire - (wireCount - 1) * 0.5F) * WIRE_SPACING;
            float offsetX = perpendicularX * offset;
            float offsetZ = perpendicularZ * offset;
            for (int segment = 0; segment < segmentCount; segment++) {
                float firstT = (float) segment / segmentCount;
                float secondT = (float) (segment + 1) / segmentCount;
                vertex(pose, consumer, point(deltaX, deltaY, deltaZ, firstT, sag, offsetX, offsetZ), red, green, blue);
                vertex(pose, consumer, point(deltaX, deltaY, deltaZ, secondT, sag, offsetX, offsetZ), red, green, blue);
            }
        }
    }

    static int segmentCount(float distance) {
        return Math.min(MAX_SEGMENTS, Math.max(8, (int) Math.ceil(distance * 2.0F)));
    }

    private static WirePoint point(
            float deltaX,
            float deltaY,
            float deltaZ,
            float progress,
            float sag,
            float offsetX,
            float offsetZ
    ) {
        float sagOffset = -4.0F * sag * progress * (1.0F - progress);
        return new WirePoint(
                0.5F + offsetX + deltaX * progress,
                0.5F + deltaY * progress + sagOffset,
                0.5F + offsetZ + deltaZ * progress
        );
    }

    private static void vertex(
            PoseStack.Pose pose,
            VertexConsumer consumer,
            WirePoint point,
            float red,
            float green,
            float blue
    ) {
        Matrix4f matrix = pose.pose();
        Matrix3f normal = pose.normal();
        consumer.vertex(matrix, point.x(), point.y(), point.z())
                .color(red, green, blue, 1.0F)
                .normal(normal, 0.0F, 1.0F, 0.0F)
                .endVertex();
    }

    @Override
    public boolean shouldRenderOffScreen(T endpoint) {
        return true;
    }

    @Override
    public int getViewDistance() {
        return 96;
    }

    private record WirePoint(float x, float y, float z) {
    }
}
