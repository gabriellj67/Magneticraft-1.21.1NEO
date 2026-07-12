package committee.nova.mods.magneticraft.data;

import net.minecraft.data.DataGenerator;
import net.minecraft.data.PackOutput;
import net.minecraftforge.data.event.GatherDataEvent;

/**
 * Registers only data providers that own resources in the current migration stage.
 */
public final class ModDataGenerators {
    private ModDataGenerators() {
    }

    public static void gatherData(GatherDataEvent event) {
        DataGenerator generator = event.getGenerator();
        PackOutput output = generator.getPackOutput();
        generator.addProvider(
                event.includeClient(),
                new ModLanguageProvider(output, "en_us", "Crushing Table Causes Fire")
        );
        generator.addProvider(
                event.includeClient(),
                new ModLanguageProvider(output, "zh_cn", "压碎烈焰棒时点燃玩家")
        );
    }
}
