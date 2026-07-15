package committee.nova.mods.magneticraft.client;

import committee.nova.mods.magneticraft.client.electrical.ClientVoltageTierRegistry;
import committee.nova.mods.magneticraft.content.network.electric.BoxTransformerBlockEntity;
import committee.nova.mods.magneticraft.content.network.module.TieredElectricalHost;
import committee.nova.mods.magneticraft.system.network.electric.item.TieredElectricalItemData;
import net.minecraft.client.color.block.BlockColor;
import net.minecraft.client.color.item.ItemColor;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.entity.BlockEntity;

import java.util.Optional;

/** Client-only projection from synchronized tier display data to model tint colors. */
final class ElectricalTierColors {
    static final int FALLBACK = 0xFFFFFF;

    private ElectricalTierColors() {
    }

    static int color(ResourceLocation tierId) {
        return ClientVoltageTierRegistry.current().tier(tierId)
                .map(display -> display.colorRgb())
                .orElse(FALLBACK);
    }

    static BlockColor blockColor() {
        return (state, level, position, tintIndex) -> {
            if (tintIndex != 0 || level == null || position == null) {
                return FALLBACK;
            }
            BlockEntity blockEntity = level.getBlockEntity(position);
            if (blockEntity instanceof BoxTransformerBlockEntity transformer) {
                return color(transformer.input().tierId());
            }
            if (blockEntity instanceof TieredElectricalHost host && host.tieredElectricalModule() != null) {
                return color(host.tieredElectricalModule().tierId());
            }
            return FALLBACK;
        };
    }

    static ItemColor itemColor() {
        return (stack, tintIndex) -> itemColor(tintIndex, TieredElectricalItemData.read(stack));
    }

    static int itemColor(int tintIndex, Optional<TieredElectricalItemData> data) {
        if (tintIndex != 0) {
            return FALLBACK;
        }
        return data.map(value -> color(value.tierId())).orElse(FALLBACK);
    }
}
