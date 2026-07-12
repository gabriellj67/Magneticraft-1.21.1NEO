package committee.nova.mods.magneticraft.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.Sheets;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.Direction;
import net.minecraft.world.inventory.InventoryMenu;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.client.extensions.common.IClientFluidTypeExtensions;
import net.minecraftforge.fluids.FluidStack;
import org.joml.Matrix3f;
import org.joml.Matrix4f;

/**
 * Stateless primitives shared by machine block-entity renderers. Static machine
 * bodies remain baked models; this class only draws server-synchronized moving
 * parts and fluid surfaces.
 */
final class MachineRenderHelper {
    private MachineRenderHelper() {
    }

    static void faceMachine(PoseStack poseStack, Direction facing) {
        poseStack.translate(0.5D, 0.5D, 0.5D);
        switch (facing) {
            case UP -> poseStack.mulPose(com.mojang.math.Axis.XP.rotationDegrees(90.0F));
            case DOWN -> poseStack.mulPose(com.mojang.math.Axis.XP.rotationDegrees(-90.0F));
            default -> poseStack.mulPose(com.mojang.math.Axis.YP.rotationDegrees(180.0F - facing.toYRot()));
        }
        poseStack.translate(-0.5D, -0.5D, -0.5D);
    }

    static void renderItem(
            ItemStack stack,
            PoseStack poseStack,
            MultiBufferSource buffers,
            int packedLight,
            int packedOverlay,
            int seed
    ) {
        Minecraft.getInstance().getItemRenderer().renderStatic(
                stack,
                ItemDisplayContext.FIXED,
                packedLight,
                packedOverlay,
                poseStack,
                buffers,
                Minecraft.getInstance().level,
                seed
        );
    }

    static void renderFluidSurface(
            FluidStack fluid,
            float fillRatio,
            float y,
            float halfWidth,
            PoseStack poseStack,
            MultiBufferSource buffers,
            int packedLight
    ) {
        if (fluid.isEmpty() || fillRatio <= 0.0F) {
            return;
        }
        IClientFluidTypeExtensions properties = IClientFluidTypeExtensions.of(fluid.getFluid());
        TextureAtlasSprite sprite = Minecraft.getInstance()
                .getTextureAtlas(InventoryMenu.BLOCK_ATLAS)
                .apply(properties.getStillTexture(fluid));
        int tint = properties.getTintColor(fluid);
        float alpha = ((tint >>> 24) & 0xFF) / 255.0F;
        float red = ((tint >>> 16) & 0xFF) / 255.0F;
        float green = ((tint >>> 8) & 0xFF) / 255.0F;
        float blue = (tint & 0xFF) / 255.0F;
        float inset = Math.max(0.01F, halfWidth * (1.0F - Math.min(fillRatio, 1.0F)) * 0.08F);
        float min = 0.5F - halfWidth + inset;
        float max = 0.5F + halfWidth - inset;

        VertexConsumer consumer = buffers.getBuffer(Sheets.translucentCullBlockSheet());
        PoseStack.Pose pose = poseStack.last();
        Matrix4f matrix = pose.pose();
        Matrix3f normal = pose.normal();
        int light = packedLight;
        vertex(consumer, matrix, normal, min, y, min, red, green, blue, alpha,
                sprite.getU0(), sprite.getV0(), light);
        vertex(consumer, matrix, normal, min, y, max, red, green, blue, alpha,
                sprite.getU0(), sprite.getV1(), light);
        vertex(consumer, matrix, normal, max, y, max, red, green, blue, alpha,
                sprite.getU1(), sprite.getV1(), light);
        vertex(consumer, matrix, normal, max, y, min, red, green, blue, alpha,
                sprite.getU1(), sprite.getV0(), light);
    }

    private static void vertex(
            VertexConsumer consumer,
            Matrix4f matrix,
            Matrix3f normal,
            float x,
            float y,
            float z,
            float red,
            float green,
            float blue,
            float alpha,
            float u,
            float v,
            int packedLight
    ) {
        consumer.vertex(matrix, x, y, z)
                .color(red, green, blue, alpha)
                .uv(u, v)
                .overlayCoords(OverlayTexture.NO_OVERLAY)
                .uv2(packedLight)
                .normal(normal, 0.0F, 1.0F, 0.0F)
                .endVertex();
    }
}
