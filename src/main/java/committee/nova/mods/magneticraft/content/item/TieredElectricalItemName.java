package committee.nova.mods.magneticraft.content.item;

import committee.nova.mods.magneticraft.client.electrical.ClientVoltageTierRegistry;
import committee.nova.mods.magneticraft.system.network.electric.item.TieredElectricalItemData;
import committee.nova.mods.magneticraft.system.network.electric.profile.ElectricalDataRegistry;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;

/** Shared display-only voltage-tier decoration for every versioned electrical item payload. */
public final class TieredElectricalItemName {
    private static final String TIERED_NAME_KEY = "item.magneticraft.tiered_name";

    private TieredElectricalItemName() {
    }

    public static Component decorate(ItemStack stack, Component baseName) {
        return TieredElectricalItemData.read(stack)
                .<Component>map(data -> Component.translatable(
                        TIERED_NAME_KEY,
                        tierName(data.tierId()),
                        baseName
                ))
                .orElse(baseName);
    }

    private static Component tierName(ResourceLocation tierId) {
        String clientTranslation = ClientVoltageTierRegistry.current()
                .tier(tierId)
                .map(display -> display.translationKey())
                .orElse(null);
        if (clientTranslation != null) {
            return Component.translatable(clientTranslation);
        }
        return ElectricalDataRegistry.INSTANCE.current()
                .flatMap(snapshot -> snapshot.voltageTier(tierId))
                .<Component>map(tier -> Component.translatable(tier.translationKey()))
                .orElseGet(() -> Component.literal(tierId.toString()));
    }
}
