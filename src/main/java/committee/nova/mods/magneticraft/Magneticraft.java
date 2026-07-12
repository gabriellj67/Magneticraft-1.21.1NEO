package committee.nova.mods.magneticraft;

import com.mojang.logging.LogUtils;
import committee.nova.mods.magneticraft.config.MagneticraftConfig;
import committee.nova.mods.magneticraft.data.ModDataGenerators;
import committee.nova.mods.magneticraft.init.ModRegistries;
import committee.nova.mods.magneticraft.network.ModNetwork;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.config.ModConfig;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import org.slf4j.Logger;

/**
 * Magneticraft's Forge composition root.
 */
@Mod(Magneticraft.MOD_ID)
public final class Magneticraft {
    public static final String MOD_ID = "magneticraft";
    public static final Logger LOGGER = LogUtils.getLogger();

    @SuppressWarnings("removal")
    public Magneticraft() {
        IEventBus modBus = FMLJavaModLoadingContext.get().getModEventBus();

        ModRegistries.register(modBus);
        ModNetwork.register();
        modBus.addListener(ModDataGenerators::gatherData);
        ModLoadingContext.get().registerConfig(ModConfig.Type.COMMON, MagneticraftConfig.SPEC);
    }

    public static ResourceLocation id(String path) {
        return ResourceLocation.fromNamespaceAndPath(MOD_ID, path);
    }
}
