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
import committee.nova.mods.magneticraft.init.ModMachineBlocks;
import committee.nova.mods.magneticraft.init.ModMachineItems;
import committee.nova.mods.magneticraft.init.ModNetworkBlocks;
import committee.nova.mods.magneticraft.init.ModNetworkItems;
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
        add(ModMachineBlocks.CRUSHING_TABLE.get(), chinese ? "压碎台" : "Crushing Table");
        add(ModMachineBlocks.BATTERY.get(), chinese ? "电池" : "Battery");
        add(ModMachineBlocks.GRATE.get(), chinese ? "格栅" : "Grate");
        add(ModMachineBlocks.ELECTRIC_FURNACE.get(), chinese ? "电炉" : "Electric Furnace");
        add(ModMachineItems.LOW_BATTERY.get(), chinese ? "小型电池" : "Low-Capacity Battery");
        add("container.magneticraft.battery", chinese ? "电池" : "Battery");
        add("container.magneticraft.electric_furnace", chinese ? "电炉" : "Electric Furnace");
        add(ModNetworkItems.WRENCH.get(), chinese ? "扳手" : "Wrench");
        add(ModNetworkBlocks.ELECTRIC_CABLE.get(), chinese ? "电缆" : "Electric Cable");
        add(ModNetworkBlocks.HEAT_PIPE.get(), chinese ? "热管" : "Heat Pipe");
        add(ModNetworkBlocks.INSULATED_HEAT_PIPE.get(), chinese ? "保温热管" : "Insulated Heat Pipe");
        add(ModNetworkBlocks.HEAT_SINK.get(), chinese ? "散热器" : "Heat Sink");
        add(ModNetworkBlocks.IRON_PIPE.get(), chinese ? "铁管" : "Iron Pipe");
        add(ModNetworkBlocks.PNEUMATIC_TUBE.get(), chinese ? "气动管" : "Pneumatic Tube");
        add(ModNetworkBlocks.PNEUMATIC_RESTRICTION_TUBE.get(), chinese ? "气动限制管" : "Pneumatic Restriction Tube");
        add(ModNetworkBlocks.CONVEYOR_BELT.get(), chinese ? "传送带" : "Conveyor Belt");

        add("message.magneticraft.connection_enabled", chinese ? "%s 连接已启用" : "%s connection enabled");
        add("message.magneticraft.connection_disabled", chinese ? "%s 连接已禁用" : "%s connection disabled");
        add("message.magneticraft.redstone_mode", chinese ? "红石控制：%s" : "Redstone control: %s");
        add("message.magneticraft.redstone_mode.ignored", chinese ? "忽略" : "Ignored");
        add("message.magneticraft.redstone_mode.requires_signal", chinese ? "需要信号" : "Requires signal");
        add("message.magneticraft.redstone_mode.requires_no_signal", chinese ? "需要无信号" : "Requires no signal");
        add("message.magneticraft.fluid_side_mode", chinese ? "%s 端口：%s" : "%s port: %s");
        add("message.magneticraft.fluid_side_mode.passive", chinese ? "被动输入" : "Passive input");
        add("message.magneticraft.fluid_side_mode.active", chinese ? "主动输出" : "Active output");
        add("message.magneticraft.fluid_side_mode.disabled", chinese ? "禁用" : "Disabled");

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
