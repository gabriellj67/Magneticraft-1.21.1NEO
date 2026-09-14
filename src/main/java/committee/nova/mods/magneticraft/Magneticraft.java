package committee.nova.mods.magneticraft;

import com.mojang.logging.LogUtils;
import committee.nova.mods.magneticraft.config.MagneticraftConfig;
import committee.nova.mods.magneticraft.data.ModDataGenerators;
import committee.nova.mods.magneticraft.init.ModRegistries;
import committee.nova.mods.magneticraft.network.ModNetwork;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig;
import org.slf4j.Logger;

/**
 * Magneticraft's NeoForge composition root.
 */
@Mod(Magneticraft.MOD_ID)
public final class Magneticraft {
    public static final String MOD_ID = "magneticraft";
    public static final Logger LOGGER = LogUtils.getLogger();

    public Magneticraft(IEventBus modBus, ModContainer container) {
        ModRegistries.register(modBus);
        modBus.addListener(ModNetwork::register);
        modBus.addListener(ModDataGenerators::gatherData);
        container.registerConfig(ModConfig.Type.COMMON, MagneticraftConfig.SPEC);
    }

    public static ResourceLocation id(String path) {
        return ResourceLocation.fromNamespaceAndPath(MOD_ID, path);
    }
}
