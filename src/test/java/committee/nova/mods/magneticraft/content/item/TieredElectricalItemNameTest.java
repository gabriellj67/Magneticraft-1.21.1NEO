package committee.nova.mods.magneticraft.content.item;

import committee.nova.mods.magneticraft.MinecraftTestBootstrap;
import committee.nova.mods.magneticraft.client.electrical.ClientVoltageTierRegistry;
import committee.nova.mods.magneticraft.system.network.electric.item.TieredElectricalItemData;
import committee.nova.mods.magneticraft.system.network.electric.profile.VoltageTierDisplay;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertTrue;

class TieredElectricalItemNameTest {
    private static final ResourceLocation CUSTOM_TIER =
            ResourceLocation.fromNamespaceAndPath("test", "custom_voltage");
    private static final String CUSTOM_TRANSLATION = "voltage_tier.test.custom_voltage";

    @BeforeAll
    static void bootstrapMinecraft() {
        MinecraftTestBootstrap.ensureBootstrapped();
    }

    @AfterEach
    void resetClientSnapshot() {
        ClientVoltageTierRegistry.reset();
    }

    @Test
    void everyTierPayloadItemDisplaysTheSyncedTierInItsName() {
        ClientVoltageTierRegistry.apply(1L, List.of(new VoltageTierDisplay(
                CUSTOM_TIER,
                CUSTOM_TRANSLATION,
                10.0D,
                20.0D,
                30.0D,
                0x12_34_56
        )));
        ItemStack stack = new ItemStack(Items.STONE);
        TieredElectricalItemData.forTier(CUSTOM_TIER).write(stack);

        Component name = TieredElectricalItemName.decorate(stack, Component.literal("Device"));
        String serializedName = Component.Serializer.toJson(name);

        assertTrue(serializedName.contains("item.magneticraft.tiered_name"),
                "Tiered display name did not use the shared wrapper");
        assertTrue(serializedName.contains(CUSTOM_TRANSLATION),
                "Tiered display name ignored the synced custom tier name");
    }
}
