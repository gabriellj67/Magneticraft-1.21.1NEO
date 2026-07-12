package committee.nova.mods.magneticraft.data;

import committee.nova.mods.magneticraft.Magneticraft;
import committee.nova.mods.magneticraft.config.MagneticraftConfig;
import committee.nova.mods.magneticraft.content.block.BaseBlockDefinition;
import committee.nova.mods.magneticraft.content.fluid.FluidDefinition;
import committee.nova.mods.magneticraft.content.item.CraftingComponent;
import committee.nova.mods.magneticraft.content.item.HammerType;
import committee.nova.mods.magneticraft.content.material.MaterialForm;
import committee.nova.mods.magneticraft.init.ModBlocks;
import committee.nova.mods.magneticraft.init.ModCreativeTabs;
import committee.nova.mods.magneticraft.init.ModFluids;
import committee.nova.mods.magneticraft.init.ModItems;
import net.minecraft.data.PackOutput;
import net.minecraftforge.common.data.LanguageProvider;

/**
 * Generates the shared baseline translations for one locale.
 */
final class ModLanguageProvider extends LanguageProvider {
    private final boolean chinese;

    ModLanguageProvider(PackOutput output, String locale, boolean chinese) {
        super(output, Magneticraft.MOD_ID, locale);
        this.chinese = chinese;
    }

    @Override
    protected void addTranslations() {
        add(
                MagneticraftConfig.CRUSHING_TABLE_CAUSES_FIRE_TRANSLATION_KEY,
                chinese ? "压碎烈焰棒时点燃玩家" : "Crushing Table Causes Fire"
        );
        add(ModCreativeTabs.TRANSLATION_KEY, chinese ? "磁场工艺" : "Magneticraft");

        ModItems.materials().forEach((form, metals) -> metals.forEach((metal, holder) ->
                add(holder.get(), chinese ? form.chineseName(metal) : form.englishName(metal))
        ));
        for (CraftingComponent component : CraftingComponent.values()) {
            add(
                    ModItems.component(component).get(),
                    chinese ? component.chineseName() : component.englishName()
            );
        }
        for (HammerType type : HammerType.values()) {
            add(ModItems.hammer(type).get(), chinese ? type.chineseName() : type.englishName());
        }
        for (BaseBlockDefinition definition : BaseBlockDefinition.values()) {
            add(
                    ModBlocks.get(definition).get(),
                    chinese ? definition.chineseName() : definition.englishName()
            );
        }
        for (FluidDefinition definition : FluidDefinition.values()) {
            String name = chinese ? definition.chineseName() : definition.englishName();
            add(definition.translationKey(), name);
            add(ModFluids.get(definition).bucket().get(), chinese ? name + "桶" : name + " Bucket");
        }
    }
}
