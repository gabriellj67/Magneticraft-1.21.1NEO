package committee.nova.mods.magneticraft.client;

import com.mojang.blaze3d.vertex.PoseStack;
import committee.nova.mods.magneticraft.client.model.LegacySceneRenderer;
import committee.nova.mods.magneticraft.content.network.block.NetworkComponentBlockEntity;
import committee.nova.mods.magneticraft.content.network.electric.BoxTransformerBlock;
import committee.nova.mods.magneticraft.content.network.electric.BoxTransformerBlockEntity;
import committee.nova.mods.magneticraft.content.network.electric.ElectricalProtectionBlock;
import committee.nova.mods.magneticraft.content.network.electric.ElectricalProtectionBlockEntity;
import committee.nova.mods.magneticraft.content.network.electric.ElectricalProtectionKind;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.core.Direction;

import java.util.Map;

/** Renders named glTF state nodes while the matching static scene remains in the baked model. */
public final class ElectricalDeviceRenderer<T extends NetworkComponentBlockEntity>
        implements BlockEntityRenderer<T> {
    private static final int READY_COLOR = 0xFF55D878;
    private static final int FAULT_COLOR = 0xFFE45345;
    private static final int REDSTONE_COLOR = 0xFFF0A53A;

    public ElectricalDeviceRenderer(BlockEntityRendererProvider.Context context) {
    }

    @Override
    public void render(
            T device,
            float partialTick,
            PoseStack poseStack,
            MultiBufferSource buffers,
            int packedLight,
            int packedOverlay
    ) {
        LegacySceneModels.Part part;
        int tierColor;
        int stateColor;
        Direction facing;
        if (device instanceof BoxTransformerBlockEntity transformer) {
            part = transformer.transformerCoupler().reversed()
                    ? LegacySceneModels.BOX_TRANSFORMER_REVERSE
                    : LegacySceneModels.BOX_TRANSFORMER_FORWARD;
            tierColor = ElectricalTierColors.color(transformer.input().tierId());
            stateColor = transformer.transformerCoupler().electricalControllerBound()
                    ? READY_COLOR
                    : FAULT_COLOR;
            facing = transformer.getBlockState().getValue(BoxTransformerBlock.FACING);
        } else if (device instanceof ElectricalProtectionBlockEntity protection) {
            boolean open = protection.protection().tripped()
                    || protection.protection().blown()
                    || protection.protection().redstoneForcedOpen();
            if (protection.kind() == ElectricalProtectionKind.FUSE_BOX) {
                if (!protection.protection().blown() && protection.protection().fuse().isEmpty()) {
                    return;
                }
                part = protection.protection().blown()
                        ? LegacySceneModels.FUSE_BLOWN
                        : LegacySceneModels.FUSE_INTACT;
            } else {
                part = open ? LegacySceneModels.BREAKER_OPEN : LegacySceneModels.BREAKER_CLOSED;
            }
            tierColor = ElectricalTierColors.color(protection.input().tierId());
            stateColor = protection.protection().redstoneForcedOpen()
                    ? REDSTONE_COLOR
                    : open ? FAULT_COLOR : READY_COLOR;
            facing = protection.getBlockState().getValue(ElectricalProtectionBlock.FACING);
        } else {
            return;
        }

        poseStack.pushPose();
        MachineRenderHelper.faceMachine(poseStack, facing);
        LegacySceneModels.render(
                part,
                null,
                0.0D,
                poseStack,
                buffers,
                packedLight,
                packedOverlay,
                LegacySceneRenderer.RenderStyle.palette(Map.of(0, 0xFF000000 | tierColor, 1, stateColor))
        );
        poseStack.popPose();
    }
}
