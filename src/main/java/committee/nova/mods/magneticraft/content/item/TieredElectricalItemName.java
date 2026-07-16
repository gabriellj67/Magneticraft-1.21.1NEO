package committee.nova.mods.magneticraft.content.item;

import committee.nova.mods.magneticraft.client.electrical.ClientVoltageTierRegistry;
import committee.nova.mods.magneticraft.system.network.electric.item.ElectricalRatingIds;
import committee.nova.mods.magneticraft.system.network.electric.item.TieredElectricalItemData;
import committee.nova.mods.magneticraft.system.network.electric.profile.ElectricalDataRegistry;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;

/** Shared display-only voltage-tier decoration for every versioned electrical item payload. */
public final class TieredElectricalItemName {
    private static final String TIERED_NAME_KEY = "item.magneticraft.tiered_name";
    private static final String TIERED_RATED_NAME_KEY = "item.magneticraft.tiered_rated_name";
    private static final String RATING_NAME_KEY_PREFIX = "electrical_rating.";

    private TieredElectricalItemName() {
    }

    public static Component decorate(ItemStack stack, Component baseName) {
        return TieredElectricalItemData.read(stack)
                .<Component>map(data -> decorate(data, baseName))
                .orElse(baseName);
    }

    private static Component decorate(TieredElectricalItemData data, Component baseName) {
        Component tierName = tierName(data.tierId());
        return data.ratingId()
                .<Component>map(ratingId -> Component.translatable(
                        TIERED_RATED_NAME_KEY,
                        tierName,
                        ratingName(ratingId),
                        baseName
                ))
                .orElseGet(() -> Component.translatable(TIERED_NAME_KEY, tierName, baseName));
    }

    private static Component ratingName(ResourceLocation ratingId) {
        if (!ElectricalRatingIds.isKnown(ratingId)) {
            return Component.literal(ratingId.toString());
        }
        return Component.translatable(
                RATING_NAME_KEY_PREFIX + ratingId.getNamespace() + "." + ratingId.getPath()
        );
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
