package committee.nova.mods.magneticraft.data;

import committee.nova.mods.magneticraft.Magneticraft;
import committee.nova.mods.magneticraft.config.MagneticraftConfig;
import committee.nova.mods.magneticraft.content.block.BaseBlockDefinition;
import committee.nova.mods.magneticraft.content.computer.vm.ComputerOpcode;
import committee.nova.mods.magneticraft.content.fluid.FluidDefinition;
import committee.nova.mods.magneticraft.content.item.CraftingComponent;
import committee.nova.mods.magneticraft.content.item.HammerType;
import committee.nova.mods.magneticraft.content.material.MaterialForm;
import committee.nova.mods.magneticraft.content.machine.singleblock.SingleBlockMachineDefinition;
import committee.nova.mods.magneticraft.content.multiblock.MultiblockDefinition;
import committee.nova.mods.magneticraft.init.ModAdvancedBlocks;
import committee.nova.mods.magneticraft.init.ModBlocks;
import committee.nova.mods.magneticraft.init.ModComputerContent;
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
        add(ModMachineItems.INSERTER_SPEED_UPGRADE.get(), chinese ? "机械臂速度升级" : "Inserter Speed Upgrade");
        add(ModMachineItems.INSERTER_STACK_UPGRADE.get(), chinese ? "机械臂整组升级" : "Inserter Stack Upgrade");
        add("container.magneticraft.battery", chinese ? "电池" : "Battery");
        add("container.magneticraft.electric_furnace", chinese ? "电炉" : "Electric Furnace");
        ModMachineBlocks.machines().forEach((definition, holder) -> {
            add(holder.get(), chinese ? definition.chineseName() : definition.englishName());
            add(
                    "container.magneticraft." + definition.id(),
                    chinese ? definition.chineseName() : definition.englishName()
            );
        });
        add(ModMachineBlocks.TUBE_LIGHT.get(), chinese ? "管灯" : "Tube Light");
        add("message.magneticraft.tank_export_enabled", chinese ? "储罐底部主动输出已启用" : "Tank bottom export enabled");
        add("message.magneticraft.tank_export_disabled", chinese ? "储罐底部主动输出已禁用" : "Tank bottom export disabled");
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

        add(ModAdvancedBlocks.MULTIBLOCK_BASE.get(), chinese ? "多方块基座" : "Multiblock Base");
        add(ModAdvancedBlocks.CORRUGATED_IRON.get(), chinese ? "波纹铁板" : "Corrugated Iron");
        add(ModAdvancedBlocks.COPPER_COIL.get(), chinese ? "铜线圈" : "Copper Coil");
        add(ModAdvancedBlocks.MULTIBLOCK_COLUMN.get(), chinese ? "多方块立柱" : "Multiblock Column");
        add(ModAdvancedBlocks.STRIPED_MULTIBLOCK_PART.get(), chinese ? "警示条纹部件" : "Striped Multiblock Part");
        add(ModAdvancedBlocks.ELECTRIC_MULTIBLOCK_PART.get(), chinese ? "电气多方块部件" : "Electric Multiblock Part");
        add(ModAdvancedBlocks.OIL_DEPOSIT.get(), chinese ? "地下油藏" : "Oil Deposit");
        for (MultiblockDefinition definition : MultiblockDefinition.values()) {
            add(
                    ModAdvancedBlocks.controller(definition).get(),
                    chinese ? definition.chineseName() : definition.englishName()
            );
        }
        add("message.magneticraft.multiblock_mirrored", chinese ? "结构镜像已启用" : "Structure mirroring enabled");
        add("message.magneticraft.multiblock_not_mirrored", chinese ? "结构镜像已禁用" : "Structure mirroring disabled");
        add("message.magneticraft.multiblock_invalid", chinese ? "多方块结构无效" : "Invalid multiblock structure");
        add("message.magneticraft.multiblock_unloaded", chinese ? "结构位置 %s 所在区块未加载" : "Structure position %s is in an unloaded chunk");
        add("message.magneticraft.multiblock_mismatch", chinese ? "结构位置 %s 应满足 %s" : "Structure position %s must match %s");
        add("message.magneticraft.multiblock_member_conflict", chinese ? "结构部件已属于另一个多方块" : "A structure member belongs to another multiblock");
        add("message.magneticraft.multiblock_formed", chinese ? "%s 已组装" : "%s formed");
        add("message.magneticraft.multiblock_status", chinese ? "%s：%s" : "%s: %s");
        add("message.magneticraft.multiblock_mirrored_state", chinese ? "镜像" : "mirrored");
        add("message.magneticraft.multiblock_normal_state", chinese ? "标准" : "normal");
        add("message.magneticraft.hydraulic_press_mode", chinese ? "液压机模式：%s" : "Hydraulic press mode: %s");
        add("message.magneticraft.hydraulic_press_mode.light", chinese ? "轻压" : "light");
        add("message.magneticraft.hydraulic_press_mode.medium", chinese ? "中压" : "medium");
        add("message.magneticraft.hydraulic_press_mode.heavy", chinese ? "重压" : "heavy");
        add("message.magneticraft.shelving_capacity", chinese ? "已安装箱体 %s/%s，可用槽位 %s" : "%s/%s chests installed; %s slots available");
        add("message.magneticraft.shelving_requires_formed", chinese ? "请先组装货架结构" : "Form the shelving structure first");
        add("message.magneticraft.shelving_owner_denied", chinese ? "无权升级此货架" : "You cannot upgrade this shelving unit");
        add("message.magneticraft.shelving_full", chinese ? "货架已安装全部 %s 个箱体" : "All %s shelving chests are installed");
        add("message.magneticraft.shelving_chest_installed", chinese ? "箱体已安装：%s/%s，可用槽位 %s" : "Chest installed: %s/%s; %s slots available");

        add(ModComputerContent.COMPUTER.get(), chinese ? "可编程计算机" : "Programmable Computer");
        add(ModComputerContent.MINING_ROBOT.get(), chinese ? "采矿机器人" : "Mining Robot");
        add(ModComputerContent.FLOPPY_DISK.get(), chinese ? "软盘" : "Floppy Disk");
        add("tooltip.magneticraft.floppy_disk.instructions", chinese ? "已存储 %s 条指令" : "%s instructions stored");
        add("message.magneticraft.computer.access_denied", chinese ? "无权管理此设备" : "Access denied");
        add("message.magneticraft.computer.floppy_written", chinese ? "程序已写入软盘" : "Program written to floppy disk");
        add("message.magneticraft.computer.floppy_loaded", chinese ? "程序已从软盘载入" : "Program loaded from floppy disk");
        add("message.magneticraft.computer.floppy_invalid", chinese ? "软盘程序无效" : "Invalid floppy program");
        add("message.magneticraft.computer.fault", chinese ? "计算机故障：%s" : "Computer fault: %s");
        add("message.magneticraft.computer.running", chinese ? "运行中（PC=%s，版本=%s）" : "Running (PC=%s, revision=%s)");
        add("message.magneticraft.computer.stopped", chinese ? "已停止（PC=%s，版本=%s）" : "Stopped (PC=%s, revision=%s)");
        for (ComputerOpcode opcode : ComputerOpcode.values()) {
            add(opcode.descriptionTranslationKey(), chinese ? opcodeChinese(opcode) : opcodeEnglish(opcode));
        }
    }

    private static String opcodeEnglish(ComputerOpcode opcode) {
        return switch (opcode) {
            case NOP -> "Do nothing";
            case HALT -> "Stop execution";
            case SET -> "Set register A to immediate B";
            case ADD -> "Add register B to register A";
            case SUBTRACT -> "Subtract register B from register A";
            case MULTIPLY -> "Multiply register A by register B";
            case DIVIDE -> "Divide register A by register B";
            case MODULO -> "Store register A modulo register B";
            case LOAD -> "Load RAM address B into register A";
            case STORE -> "Store register B at RAM address A";
            case JUMP -> "Jump to instruction A";
            case JUMP_IF_ZERO -> "Jump to B when register A is zero";
            case MOVE -> "Move in relative direction A and store the result in register B";
            case MINE -> "Mine in relative direction A and store the result in register B";
            case SET_REDSTONE -> "Set redstone output to A and store the result in register B";
        };
    }

    private static String opcodeChinese(ComputerOpcode opcode) {
        return switch (opcode) {
            case NOP -> "不执行操作";
            case HALT -> "停止执行";
            case SET -> "将立即数 B 写入寄存器 A";
            case ADD -> "将寄存器 B 加到寄存器 A";
            case SUBTRACT -> "从寄存器 A 减去寄存器 B";
            case MULTIPLY -> "寄存器 A 乘以寄存器 B";
            case DIVIDE -> "寄存器 A 除以寄存器 B";
            case MODULO -> "将寄存器 A 对寄存器 B 取模";
            case LOAD -> "将 RAM 地址 B 载入寄存器 A";
            case STORE -> "将寄存器 B 写入 RAM 地址 A";
            case JUMP -> "跳转到指令 A";
            case JUMP_IF_ZERO -> "寄存器 A 为零时跳转到 B";
            case MOVE -> "向相对方向 A 移动并将结果写入寄存器 B";
            case MINE -> "向相对方向 A 采掘并将结果写入寄存器 B";
            case SET_REDSTONE -> "将红石输出设为 A 并将结果写入寄存器 B";
        };
    }
}
