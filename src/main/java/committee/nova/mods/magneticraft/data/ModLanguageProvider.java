package committee.nova.mods.magneticraft.data;

import committee.nova.mods.magneticraft.Magneticraft;
import committee.nova.mods.magneticraft.config.MagneticraftConfig;
import net.minecraft.data.PackOutput;
import net.minecraftforge.common.data.LanguageProvider;

/**
 * Generates the shared baseline translations for one locale.
 */
final class ModLanguageProvider extends LanguageProvider {
    private final String crushingTableCausesFireLabel;

    ModLanguageProvider(PackOutput output, String locale, String crushingTableCausesFireLabel) {
        super(output, Magneticraft.MOD_ID, locale);
        this.crushingTableCausesFireLabel = crushingTableCausesFireLabel;
    }

    @Override
    protected void addTranslations() {
        add(MagneticraftConfig.CRUSHING_TABLE_CAUSES_FIRE_TRANSLATION_KEY, crushingTableCausesFireLabel);
    }
}
