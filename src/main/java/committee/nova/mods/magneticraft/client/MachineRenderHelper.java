package committee.nova.mods.magneticraft.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.Sheets;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.Direction;
import net.minecraft.core.BlockPos;
import net.minecraft.world.inventory.InventoryMenu;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraftforge.client.extensions.common.IClientFluidTypeExtensions;
import net.minecraftforge.fluids.FluidStack;
import org.joml.Matrix3f;
import org.joml.Matrix4f;

/**
 * Stateless primitives shared by machine block-entity renderers.
 */
final class MachineRenderHelper {
    private static final double HEAT_GLOW_START_KELVIN = 423.15D;
    private static final double HEAT_GLOW_RANGE_KELVIN = 300.0D;

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

    static void rotateAroundCenterY(PoseStack poseStack, float degrees) {
        poseStack.translate(0.5D, 0.5D, 0.5D);
        poseStack.mulPose(com.mojang.math.Axis.YP.rotationDegrees(degrees));
        poseStack.translate(-0.5D, -0.5D, -0.5D);
    }

    static int heatTint(double temperatureKelvin) {
        double visibility = heatVisibility(temperatureKelvin);
        int[] blackBody = blackBodyColor(temperatureKelvin);
        int red = blend(255, blackBody[0], visibility);
        int green = blend(255, blackBody[1], visibility);
        int blue = blend(255, blackBody[2], visibility);
        return 0xFF000000 | red << 16 | green << 8 | blue;
    }

    static int heatPackedLight(int packedLight, double temperatureKelvin) {
        int glow = (int) Math.round(15.0D * heatVisibility(temperatureKelvin));
        return LightTexture.pack(
                Math.max(LightTexture.block(packedLight), glow),
                Math.max(LightTexture.sky(packedLight), glow)
        );
    }

    private static double heatVisibility(double temperatureKelvin) {
        return Math.max(0.0D, Math.min(
                1.0D,
                (temperatureKelvin - HEAT_GLOW_START_KELVIN) / HEAT_GLOW_RANGE_KELVIN
        ));
    }

    private static int[] blackBodyColor(double kelvin) {
        double temperature = Math.max(100.0D, kelvin) / 100.0D;
        double red = temperature < 66.0D
                ? 255.0D
                : 351.97690566805693D
                + 0.114206453784165D * (temperature - 55.0D)
                - 40.25366309332127D * Math.log(temperature - 55.0D);
        double green = temperature < 66.0D
                ? -155.25485562709179D
                - 0.44596950469579133D * (temperature - 2.0D)
                + 104.49216199393888D * Math.log(Math.max(0.000_001D, temperature - 2.0D))
                : 325.4494125711974D
                + 0.07943456536662342D * (temperature - 50.0D)
                - 28.0852963507957D * Math.log(temperature - 50.0D);
        double blue = temperature >= 66.0D
                ? 255.0D
                : temperature <= 20.0D
                ? 0.0D
                : -254.76935184120902D
                + 0.8274096064007395D * (temperature - 10.0D)
                + 115.67994401066147D * Math.log(temperature - 10.0D);
        return new int[]{clampColor(red), clampColor(green), clampColor(blue)};
    }

    private static int blend(int from, int to, double amount) {
        return clampColor(from + (to - from) * amount);
    }

    private static int clampColor(double value) {
        return (int) Math.round(Math.max(0.0D, Math.min(255.0D, value)));
    }

    /** Reproduces the original ModelLoader electric-engine DOWN-authored transform. */
    static void orientElectricEngine(PoseStack poseStack, Direction facing) {
        poseStack.translate(0.5D, 0.5D, 0.5D);
        switch (facing) {
            case DOWN -> poseStack.mulPose(com.mojang.math.Axis.XP.rotationDegrees(180.0F));
            case NORTH -> {
                poseStack.mulPose(com.mojang.math.Axis.YP.rotationDegrees(-90.0F));
                poseStack.mulPose(com.mojang.math.Axis.ZP.rotationDegrees(90.0F));
            }
            case SOUTH -> {
                poseStack.mulPose(com.mojang.math.Axis.YP.rotationDegrees(90.0F));
                poseStack.mulPose(com.mojang.math.Axis.ZP.rotationDegrees(90.0F));
            }
            case WEST -> poseStack.mulPose(com.mojang.math.Axis.ZP.rotationDegrees(90.0F));
            case EAST -> poseStack.mulPose(com.mojang.math.Axis.ZP.rotationDegrees(-90.0F));
            case UP -> {
            }
        }
        poseStack.mulPose(com.mojang.math.Axis.ZP.rotationDegrees(-90.0F));
        poseStack.translate(-0.5D, -0.5D, -0.5D);
    }

    static int surroundingLight(Level level, BlockPos position, int fallback) {
        if (level == null) {
            return fallback;
        }
        int blockLight = LightTexture.block(fallback);
        int skyLight = LightTexture.sky(fallback);
        for (Direction direction : Direction.values()) {
            int neighborLight = LevelRenderer.getLightColor(level, position.relative(direction));
            blockLight = Math.max(blockLight, LightTexture.block(neighborLight));
            skyLight = Math.max(skyLight, LightTexture.sky(neighborLight));
        }
        return LightTexture.pack(blockLight, skyLight);
    }

    static void renderItem(
            ItemStack stack,
            PoseStack poseStack,
            MultiBufferSource buffers,
            int packedLight,
            int packedOverlay,
            int seed
    ) {
        renderItem(stack, ItemDisplayContext.FIXED, poseStack, buffers, packedLight, packedOverlay, seed);
    }

    static void renderItem(
            ItemStack stack,
            ItemDisplayContext context,
            PoseStack poseStack,
            MultiBufferSource buffers,
            int packedLight,
            int packedOverlay,
            int seed
    ) {
        Minecraft.getInstance().getItemRenderer().renderStatic(
                stack,
                context,
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

    static void renderFluidCuboid(
            FluidStack fluid,
            float minX,
            float minY,
            float minZ,
            float maxX,
            float maxY,
            float maxZ,
            PoseStack poseStack,
            MultiBufferSource buffers,
            int packedLight
    ) {
        if (fluid.isEmpty() || maxX <= minX || maxY <= minY || maxZ <= minZ) {
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
        VertexConsumer consumer = buffers.getBuffer(Sheets.translucentCullBlockSheet());
        PoseStack.Pose pose = poseStack.last();
        Matrix4f matrix = pose.pose();
        Matrix3f normal = pose.normal();
        float u0 = sprite.getU0();
        float u1 = sprite.getU1();
        float v0 = sprite.getV0();
        float v1 = sprite.getV1();

        fluidQuad(consumer, matrix, normal, packedLight, red, green, blue, alpha,
                minX, maxY, minZ, minX, maxY, maxZ, maxX, maxY, maxZ, maxX, maxY, minZ,
                u0, v0, u1, v1, 0.0F, 1.0F, 0.0F);
        fluidQuad(consumer, matrix, normal, packedLight, red, green, blue, alpha,
                minX, minY, maxZ, minX, minY, minZ, maxX, minY, minZ, maxX, minY, maxZ,
                u0, v0, u1, v1, 0.0F, -1.0F, 0.0F);
        fluidQuad(consumer, matrix, normal, packedLight, red, green, blue, alpha,
                minX, minY, minZ, minX, maxY, minZ, maxX, maxY, minZ, maxX, minY, minZ,
                u0, v1, u1, v0, 0.0F, 0.0F, -1.0F);
        fluidQuad(consumer, matrix, normal, packedLight, red, green, blue, alpha,
                maxX, minY, maxZ, maxX, maxY, maxZ, minX, maxY, maxZ, minX, minY, maxZ,
                u0, v1, u1, v0, 0.0F, 0.0F, 1.0F);
        fluidQuad(consumer, matrix, normal, packedLight, red, green, blue, alpha,
                minX, minY, maxZ, minX, maxY, maxZ, minX, maxY, minZ, minX, minY, minZ,
                u0, v1, u1, v0, -1.0F, 0.0F, 0.0F);
        fluidQuad(consumer, matrix, normal, packedLight, red, green, blue, alpha,
                maxX, minY, minZ, maxX, maxY, minZ, maxX, maxY, maxZ, maxX, minY, maxZ,
                u0, v1, u1, v0, 1.0F, 0.0F, 0.0F);
    }

    private static void fluidQuad(
            VertexConsumer consumer,
            Matrix4f matrix,
            Matrix3f normal,
            int light,
            float red,
            float green,
            float blue,
            float alpha,
            float x0, float y0, float z0,
            float x1, float y1, float z1,
            float x2, float y2, float z2,
            float x3, float y3, float z3,
            float u0, float v0, float u1, float v1,
            float normalX, float normalY, float normalZ
    ) {
        fluidVertex(consumer, matrix, normal, x0, y0, z0, red, green, blue, alpha, u0, v0, light,
                normalX, normalY, normalZ);
        fluidVertex(consumer, matrix, normal, x1, y1, z1, red, green, blue, alpha, u0, v1, light,
                normalX, normalY, normalZ);
        fluidVertex(consumer, matrix, normal, x2, y2, z2, red, green, blue, alpha, u1, v1, light,
                normalX, normalY, normalZ);
        fluidVertex(consumer, matrix, normal, x3, y3, z3, red, green, blue, alpha, u1, v0, light,
                normalX, normalY, normalZ);
    }

    private static void fluidVertex(
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
            int packedLight,
            float normalX,
            float normalY,
            float normalZ
    ) {
        consumer.vertex(matrix, x, y, z)
                .color(red, green, blue, alpha)
                .uv(u, v)
                .overlayCoords(OverlayTexture.NO_OVERLAY)
                .uv2(packedLight)
                .normal(normal, normalX, normalY, normalZ)
                .endVertex();
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
