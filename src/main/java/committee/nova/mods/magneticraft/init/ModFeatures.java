package committee.nova.mods.magneticraft.init;

import committee.nova.mods.magneticraft.content.worldgen.OilFieldFeature;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.configurations.NoneFeatureConfiguration;
import net.minecraftforge.registries.RegistryObject;

/** Custom world-generation features with stable registry identities. */
public final class ModFeatures {
    public static final RegistryObject<Feature<?>> OIL_FIELD = ModRegistries.FEATURES.register(
            "oil_field",
            () -> new OilFieldFeature(NoneFeatureConfiguration.CODEC)
    );

    private ModFeatures() {
    }

    public static void bootstrap() {
    }
}
