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
        add("jei.magneticraft.polymerizing", chinese ? "聚合" : "Polymerizing");
        add("jei.magneticraft.heat_per_tick", chinese ? "热耗：%s J/t" : "Heat: %s J/t");
        add(
                MagneticraftConfig.CRUSHING_TABLE_CAUSES_FIRE_TRANSLATION_KEY,
                chinese ? "压碎烈焰棒时点燃玩家" : "Crushing Table Causes Fire"
        );
        add(
                MagneticraftConfig.WATER_GENERATOR_PER_TICK_WATER_TRANSLATION_KEY,
                chinese ? "供水器每侧每刻供水量" : "Water Generator Output Per Side"
        );
        add(
                MagneticraftConfig.ENABLE_ELECTRICAL_DAMAGE_TRANSLATION_KEY,
                chinese ? "启用电气损坏" : "Enable Electrical Damage"
        );
        add(
                MagneticraftConfig.ELECTRICAL_RELOAD_GRACE_TICKS_TRANSLATION_KEY,
                chinese ? "电气数据重载保护刻数" : "Electrical Reload Grace Ticks"
        );
        add("voltage_tier.magneticraft.low_voltage", chinese ? "低压" : "Low Voltage");
        add("voltage_tier.magneticraft.medium_voltage", chinese ? "中压" : "Medium Voltage");
        add("voltage_tier.magneticraft.high_voltage", chinese ? "高压" : "High Voltage");
        add("item.magneticraft.tiered_name", "[%1$s] %2$s");
        add("item.magneticraft.tiered_rated_name", "[%1$s · %2$s] %3$s");
        add("electrical_rating.magneticraft.standard", chinese ? "标准" : "Standard");
        add("electrical_rating.magneticraft.heavy", chinese ? "重载" : "Heavy");
        add(ModCreativeTabs.TRANSLATION_KEY, chinese ? "磁场工艺" : "Magneticraft");
        add(ModMachineBlocks.CRUSHING_TABLE.get(), chinese ? "压碎台" : "Crushing Table");
        add(ModMachineBlocks.BATTERY.get(), chinese ? "电池箱" : "Battery Box");
        add(ModMachineBlocks.GRATE.get(), chinese ? "铁格栅" : "Iron Grate");
        add(ModMachineBlocks.ELECTRIC_FURNACE.get(), chinese ? "电炉" : "Electric Furnace");
        add(ModMachineItems.LOW_BATTERY.get(), chinese ? "低压电池" : "Low-Voltage Battery");
        add(ModMachineItems.MEDIUM_BATTERY.get(), chinese ? "中压电池" : "Medium-Voltage Battery");
        add(ModMachineItems.ELECTRIC_DRILL.get(), chinese ? "电钻" : "Electric Drill");
        add(ModMachineItems.ELECTRIC_CHAINSAW.get(), chinese ? "电锯" : "Electric Chainsaw");
        add(ModMachineItems.ELECTRIC_PISTON.get(), chinese ? "电动活塞" : "Electric Piston");
        add(ModMachineItems.VOLTMETER.get(), chinese ? "电压表" : "Voltmeter");
        add(ModMachineItems.THERMOMETER.get(), chinese ? "温度计" : "Thermometer");
        add(ModMachineItems.OIL_PROSPECTOR.get(), chinese ? "油量探测器" : "Oil Prospector");
        add(ModMachineItems.PRESSURE_GAUGE.get(), chinese ? "压力表" : "Pressure Gauge");
        add(ModMachineItems.INSERTER_SPEED_UPGRADE.get(), chinese ? "机械臂速度升级" : "Inserter Speed Upgrade");
        add(ModMachineItems.INSERTER_STACK_UPGRADE.get(), chinese ? "机械臂批量升级" : "Inserter Stack Upgrade");
        add("container.magneticraft.battery_box", chinese ? "电池箱" : "Battery Box");
        add("container.magneticraft.electric_furnace", chinese ? "电炉" : "Electric Furnace");
        add("container.magneticraft.box_transformer", chinese ? "箱式变压器" : "Box Transformer");
        add("container.magneticraft.fuse_box", chinese ? "保险丝盒" : "Fuse Box");
        add("container.magneticraft.circuit_breaker", chinese ? "断路器" : "Circuit Breaker");
        add("container.magneticraft.electric_switch", chinese ? "电气开关" : "Electric Switch");
        add("container.magneticraft.diode", chinese ? "二极管" : "Diode");
        add("container.magneticraft.resistor", chinese ? "可调电阻" : "Adjustable Resistor");
        add("container.magneticraft.pressure_tank", chinese ? "压力罐" : "Pressure Tank");
        ModMachineBlocks.machines().forEach((definition, holder) -> {
            add(holder.get(), chinese ? definition.chineseName() : definition.englishName());
            add(
                    "container.magneticraft." + definition.id(),
                    chinese ? definition.chineseName() : definition.englishName()
            );
        });
        add(
                "message.magneticraft.pneumatic_endpoint.blocked",
                chinese ? "气动端点输出受阻，载荷已保留" : "Pneumatic endpoint output blocked; payload retained"
        );
        add(
                "message.magneticraft.pneumatic_endpoint.unloaded",
                chinese ? "目标区块未加载，气动传输已暂停" : "Target chunk unloaded; pneumatic transfer paused"
        );
        add(
                "message.magneticraft.pneumatic_endpoint.filter_rejected",
                chinese ? "物品不符合气动过滤样本" : "Item does not match the pneumatic filter samples"
        );
        add(ModMachineBlocks.TUBE_LIGHT.get(), chinese ? "管灯" : "Tube Light");
        add(ModMachineBlocks.GEOTHERMAL_DRILL_PIPE.get(), chinese ? "地热钻管" : "Geothermal Drill Pipe");
        add("message.magneticraft.oil_prospector.insufficient_energy", chinese
                ? "电量不足：扫描需要 %s J"
                : "Insufficient energy: a scan requires %s J");
        add("message.magneticraft.oil_prospector.not_found", chinese
                ? "未探测到已生成油田"
                : "No generated oil field was detected");
        add("message.magneticraft.oil_prospector.result", chinese
                ? "油田位于%s方向，中心距离约%s格，油层高度 Y %s–%s；已探明剩余 %s mB（%s%%）"
                : "Oil field %s, center about %s blocks away, layer Y %s–%s; surveyed reserve %s mB (%s%%)");
        add("message.magneticraft.oil_prospector.depleted", chinese
                ? "油田位于%s方向，中心距离约%s格，油层高度 Y %s–%s；已探明剩余 %s mB（%s%%）：储量耗尽"
                : "Oil field %s, center about %s blocks away, layer Y %s–%s; surveyed reserve %s mB (%s%%): depleted");
        addOilProspectorDirections();
        add("message.magneticraft.tank_export_enabled", chinese ? "储罐底部主动输出已启用" : "Tank bottom export enabled");
        add("message.magneticraft.tank_export_disabled", chinese ? "储罐底部主动输出已禁用" : "Tank bottom export disabled");
        add(ModNetworkItems.WRENCH.get(), chinese ? "扳手" : "Wrench");
        add(ModNetworkItems.COPPER_WIRE_COIL.get(), chinese ? "铜线卷" : "Copper Wire Coil");
        add(ModNetworkItems.FUSE.get(), chinese ? "保险丝" : "Electrical Fuse");
        add(ModNetworkItems.ELECTRICAL_REPAIR_TOOL.get(), chinese ? "电气维修工具" : "Electrical Repair Tool");
        add(ModNetworkBlocks.ELECTRIC_CABLE.get(), chinese ? "电缆" : "Electric Cable");
        add(ModNetworkBlocks.BURNT_ELECTRIC_CABLE.get(), chinese ? "烧毁的电缆" : "Burnt Electric Cable");
        add(ModNetworkBlocks.ELECTRIC_CONNECTOR.get(), chinese ? "电力连接器" : "Electric Connector");
        add(ModNetworkBlocks.ELECTRIC_POLE.get(), chinese ? "电线杆" : "Electric Pole");
        add(ModNetworkBlocks.ELECTRIC_POLE_TRANSFORMER.get(), chinese ? "变压电线杆升级件" : "Transformer Pole Upgrade");
        add(ModNetworkBlocks.BOX_TRANSFORMER.get(), chinese ? "箱式变压器" : "Box Transformer");
        add(ModNetworkBlocks.FUSE_BOX.get(), chinese ? "保险丝盒" : "Fuse Box");
        add(ModNetworkBlocks.CIRCUIT_BREAKER.get(), chinese ? "断路器" : "Circuit Breaker");
        add(ModNetworkBlocks.ELECTRIC_SWITCH.get(), chinese ? "电气开关" : "Electric Switch");
        add(ModNetworkBlocks.DIODE.get(), chinese ? "二极管" : "Diode");
        add(ModNetworkBlocks.RESISTOR.get(), chinese ? "可调电阻" : "Adjustable Resistor");
        add(ModNetworkBlocks.TESLA_TOWER.get(), chinese ? "特斯拉塔" : "Tesla Tower");
        add(ModNetworkBlocks.WIRELESS_ENERGY_RECEIVER.get(), chinese ? "无线能量接收器" : "Wireless Energy Receiver");
        add(ModNetworkBlocks.WIND_TURBINE.get(), chinese ? "风力发电机" : "Wind Turbine");
        add(ModNetworkBlocks.HEAT_PIPE.get(), chinese ? "热管" : "Heat Pipe");
        add(ModNetworkBlocks.INSULATED_HEAT_PIPE.get(), chinese ? "保温热管" : "Insulated Heat Pipe");
        add(ModNetworkBlocks.HEAT_SINK.get(), chinese ? "散热器" : "Heat Sink");
        add(ModNetworkBlocks.IRON_PIPE.get(), chinese ? "铁质流体管" : "Iron Fluid Pipe");
        add(ModNetworkBlocks.PNEUMATIC_TUBE.get(), chinese ? "气动管" : "Pneumatic Tube");
        add(ModNetworkBlocks.PNEUMATIC_RESTRICTION_TUBE.get(), chinese ? "气动限制管" : "Pneumatic Restriction Tube");
        add(ModNetworkBlocks.BRASS_PRESSURE_PIPE.get(), chinese ? "黄铜压力管" : "Brass Pressure Pipe");
        add(ModNetworkBlocks.PRESSURE_TANK.get(), chinese ? "压力罐" : "Pressure Tank");
        add(ModNetworkBlocks.CONVEYOR_BELT.get(), chinese ? "传送带" : "Conveyor Belt");

        add("message.magneticraft.connection_enabled", chinese ? "%s 连接已启用" : "%s connection enabled");
        add("message.magneticraft.connection_disabled", chinese ? "%s 连接已禁用" : "%s connection disabled");
        add("message.magneticraft.invalid_voltage_tier", chinese
                ? "无法放置：电压层级 %s 不存在或物品数据已损坏"
                : "Cannot place: voltage tier %s is missing or the item data is invalid");
        add("message.magneticraft.invalid_transformer_profile", chinese
                ? "无法使用：变压配置 %s 不存在或物品数据已损坏"
                : "Cannot use: transformer profile %s is missing or the item data is invalid");
        add("message.magneticraft.transformer_status", chinese
                ? "变压配置：%s，反向：%s，红石模式：%s"
                : "Transformer profile: %s, reversed: %s, redstone mode: %s");
        add("message.magneticraft.protection_status", chinese
                ? "保护规格：%s，过载度：%s，已断开：%s"
                : "Protection rating: %s, stress: %s, open: %s");
        add("message.magneticraft.electrical_control_status", chinese
                ? "%s：导通 %s，设定电阻 %s Ω"
                : "%s: conducting %s, configured resistance %s Ω");
        add("message.magneticraft.invalid_fuse", chinese
                ? "保险丝必须与保险丝盒的电压层级一致"
                : "The fuse must match the fuse box voltage tier");
        add("message.magneticraft.repair_voltage_unsafe", chinese
                ? "维修失败：所有电气端子必须低于标称电压的 95%"
                : "Repair failed: every electrical terminal must be below 95% of nominal voltage");
        add("message.magneticraft.electrical_repaired", chinese
                ? "电气设备已修复"
                : "Electrical equipment repaired");
        add("message.magneticraft.redstone_mode", chinese ? "红石控制：%s" : "Redstone control: %s");
        add("message.magneticraft.redstone_mode.ignored", chinese ? "忽略" : "Ignored");
        add("message.magneticraft.redstone_mode.requires_signal", chinese ? "有红石信号时启用" : "Requires signal");
        add("message.magneticraft.redstone_mode.requires_no_signal", chinese ? "无红石信号时启用" : "Requires no signal");
        add("message.magneticraft.fluid_side_mode", chinese ? "%s 端口：%s" : "%s port: %s");
        add("message.magneticraft.fluid_side_mode.passive", chinese ? "被动输入/输出" : "Passive I/O");
        add("message.magneticraft.fluid_side_mode.active", chinese ? "主动输出" : "Active output");
        add("message.magneticraft.fluid_side_mode.disabled", chinese ? "禁用" : "Disabled");
        add("message.magneticraft.voltmeter", chinese ? "%s V · %s A · %s W" : "%s V · %s A · %s W");
        add("message.magneticraft.voltmeter.mode", chinese ? "电压表模式：%s" : "Voltmeter mode: %s");
        add("mode.magneticraft.voltmeter.point", chinese ? "点测" : "Point probe");
        add("mode.magneticraft.voltmeter.network_summary", chinese ? "网络概览" : "Network summary");
        add("mode.magneticraft.voltmeter.fault_locator", chinese ? "故障定位" : "Fault locator");
        add("message.magneticraft.voltmeter.point", chinese
                ? "端子 %s · %s · %s V · %s C/t · %s A · %s J/t · %s W · %s · 负载 %s · %s"
                : "Terminal %s · %s · %s V · %s C/t · %s A · %s J/t · %s W · %s · load %s · %s");
        add("message.magneticraft.voltmeter.network", chinese
                ? "网络：%s 节点 / %s 边 · 储能 %s J · 发电 %s J/t · 用电 %s J/t · 损耗 %s J/t · 最大负载 %s · 故障 %s%s"
                : "Network: %s nodes / %s edges · stored %s J · generated %s J/t · consumed %s J/t · loss %s J/t · max load %s · faults %s%s");
        add("message.magneticraft.voltmeter.fault", chinese
                ? "最近故障：%s · 端子 %s · %s · 距离 %s 格%s"
                : "Nearest fault: %s · terminal %s · %s · %s blocks away%s");
        add("message.magneticraft.voltmeter.no_fault", chinese
                ? "已检查 %s 个节点，未发现故障%s"
                : "No fault found across %s visited nodes%s");
        add("message.magneticraft.voltmeter.here", chinese ? "当前位置" : "here");
        add("message.magneticraft.voltmeter.truncated", chinese ? "（达到搜索上限）" : " (search limit reached)");
        add("flow.magneticraft.input", chinese ? "输入" : "input");
        add("flow.magneticraft.output", chinese ? "输出" : "output");
        add("flow.magneticraft.bidirectional", chinese ? "双向" : "bidirectional");
        add("flow.magneticraft.idle", chinese ? "空闲" : "idle");
        add("fault.magneticraft.none", chinese ? "正常" : "normal");
        add("fault.magneticraft.missing_profile", chinese ? "缺失配置" : "missing profile");
        add("fault.magneticraft.machine_fault", chinese ? "机器故障" : "machine fault");
        add("fault.magneticraft.fuse_blown", chinese ? "保险丝熔断" : "fuse blown");
        add("fault.magneticraft.breaker_tripped", chinese ? "断路器跳闸" : "breaker tripped");
        add("fault.magneticraft.redstone_open", chinese ? "红石强制断开" : "redstone-forced open");
        add("message.magneticraft.thermometer", chinese ? "%s °C" : "%s °C");
        add("message.magneticraft.pressure_gauge", chinese
                ? "%s：%s kPa（%s bar / %s psi）· %s/%s kPa·L%s"
                : "%s: %s kPa (%s bar / %s psi) · %s/%s kPa·L%s");
        add("message.magneticraft.pressure_gauge.warning", chinese ? " · 压力接近额定上限" : " · near rated pressure");
        add("message.magneticraft.voltage", chinese ? "电压：%s V" : "Voltage: %s V");
        add("message.magneticraft.long_distance_connections", chinese
                ? "长距离连接：%s"
                : "Long-distance connections: %s");
        add("text.magneticraft.wire_connect.updated_position", chinese
                ? "已选择端点：%s, %s, %s"
                : "Selected endpoint: %s, %s, %s");
        add("text.magneticraft.wire_connect.success", chinese ? "导线连接成功" : "Wire connected");
        add("text.magneticraft.wire_connect.too_far", chinese ? "两个端点距离过远" : "The endpoints are too far apart");
        add("text.magneticraft.wire_connect.not_a_connector", chinese ? "目标不是已加载的导线端点" : "The target is not a loaded wire endpoint");
        add("text.magneticraft.wire_connect.invalid_connector", chinese ? "两个端点类型或维度不兼容" : "The endpoints have incompatible types or dimensions");
        add("text.magneticraft.wire_connect.same_connector", chinese ? "不能将端点连接到自身" : "An endpoint cannot connect to itself");
        add("text.magneticraft.wire_connect.already_connected", chinese ? "两个端点已经连接" : "The endpoints are already connected");
        add("text.magneticraft.wire_connect.no_other_connector", chinese ? "请先潜行右击选择第一个端点" : "Sneak-use a first endpoint before connecting");
        add("tooltip.magneticraft.energy", chinese ? "能量：%s / %s J" : "Energy: %s / %s J");
        add("tooltip.magneticraft.joules", chinese ? "储能：%s / %s J" : "Stored: %s / %s J");
        add("config.jade.plugin_magneticraft.machine_status", chinese ? "磁场工艺机器状态" : "Magneticraft Machine Status");
        add("tooltip.magneticraft.jade.process", chinese ? "进度：%s / %s（%s）" : "Progress: %s / %s (%s)");
        add("tooltip.magneticraft.jade.active", chinese ? "运行中" : "active");
        add("tooltip.magneticraft.jade.idle", chinese ? "空闲" : "idle");
        add("tooltip.magneticraft.jade.electrical_terminal", chinese
                ? "电气端子：%s · %s"
                : "Electrical terminal: %s · %s");
        add("tooltip.magneticraft.jade.electrical_flow", chinese
                ? "%s V · %s C/t · %s A · %s J/t · %s W"
                : "%s V · %s C/t · %s A · %s J/t · %s W");
        add("tooltip.magneticraft.jade.electrical_state", chinese
                ? "负载 %s · 过载度 %s · %s · %s"
                : "Load %s · thermal stress %s · %s · %s");
        add("tooltip.magneticraft.jade.temperature", chinese ? "温度：%s K" : "Temperature: %s K");
        add("tooltip.magneticraft.jade.pressure", chinese
                ? "%s：%s kPa · %s/%s kPa·L"
                : "%s: %s kPa · %s/%s kPa·L");
        add("tooltip.magneticraft.jade.pressure_warning", chinese
                ? "警告：压力已达额定容量的 %s"
                : "Warning: pressure is at %s of rated capacity");
        add("tooltip.magneticraft.jade.tank", chinese ? "%s：%s / %s mB" : "%s: %s / %s mB");
        add("tooltip.magneticraft.jade.empty", chinese ? "空" : "Empty");
        add("tooltip.magneticraft.jade.structure", chinese ? "结构：%s，%s" : "Structure: %s, %s");
        add("tooltip.magneticraft.jade.formed", chinese ? "已成型" : "formed");
        add("tooltip.magneticraft.jade.unformed", chinese ? "未成型" : "unformed");
        add("tooltip.magneticraft.jade.operational", chinese ? "可运行" : "operational");
        add("tooltip.magneticraft.jade.paused", chinese ? "已暂停" : "paused");
        add("gui.magneticraft.electrical.title", chinese ? "电气状态" : "Electrical Status");
        add("gui.magneticraft.electrical.unavailable", chinese ? "此侧没有电气端口" : "No electrical terminal on this side");
        add("gui.magneticraft.electrical.terminal", chinese ? "%s · %s" : "%s · %s");
        add("gui.magneticraft.electrical.voltage_current", chinese ? "%s V · %s A" : "%s V · %s A");
        add("gui.magneticraft.electrical.throughput", chinese ? "%s J/t · %s W" : "%s J/t · %s W");
        add("gui.magneticraft.electrical.load_stress", chinese ? "负载 %s%% · 过载度 %s%%" : "Load %s%% · stress %s%%");
        add("gui.magneticraft.electrical.storage", chinese ? "储能 %s / %s J" : "Stored %s / %s J");
        add("gui.magneticraft.electrical.state", chinese ? "%s · %s" : "%s · %s");
        add("gui.magneticraft.electrical.point_tooltip", chinese
                ? "%s V · %s C/t · %s A · %s J/t · %s W"
                : "%s V · %s C/t · %s A · %s J/t · %s W");
        add("gui.magneticraft.electrical.node_tooltip", chinese
                ? "节点储能 %s / %s J · 负载 %s · 过载度 %s"
                : "Node %s / %s J · load %s · stress %s");
        add("gui.magneticraft.electrical.reset", chinese ? "复位" : "Reset");
        add("gui.magneticraft.electrical.profile_state", chinese ? "配置：%s" : "Profile: %s");
        add("gui.magneticraft.electrical.bound", chinese ? "已绑定" : "bound");
        add("gui.magneticraft.electrical.missing_profile", chinese ? "缺失" : "missing");
        add("gui.magneticraft.electrical.rating", chinese ? "额定规格：%s" : "Rating: %s");
        add("gui.magneticraft.electrical.rating.standard", chinese ? "标准 8 C/t" : "Standard 8 C/t");
        add("gui.magneticraft.electrical.rating.heavy", chinese ? "重载 16 C/t" : "Heavy 16 C/t");
        add("gui.magneticraft.electrical.protection_state", chinese ? "状态：%s" : "State: %s");
        add("gui.magneticraft.electrical.direction.forward", chinese ? "正向" : "Forward");
        add("gui.magneticraft.electrical.direction.reverse", chinese ? "反向" : "Reverse");
        add("gui.magneticraft.electrical.direction.none", chinese ? "无电流" : "No current");
        add("gui.magneticraft.electrical.direction.back_to_front", chinese ? "后端 → 前端" : "Back → front");
        add("gui.magneticraft.electrical.direction.front_to_back", chinese ? "前端 → 后端" : "Front → back");
        add("gui.magneticraft.electrical.redstone.ignored", chinese ? "忽略红石" : "Ignore redstone");
        add("gui.magneticraft.electrical.redstone.requires_signal", chinese ? "有红石信号时启用" : "Requires signal");
        add("gui.magneticraft.electrical.redstone.requires_no_signal", chinese ? "无红石信号时启用" : "Requires no signal");
        add("gui.magneticraft.electrical.switch_redstone.ignored", chinese ? "忽略红石，始终导通" : "Ignore redstone, always conduct");
        add("gui.magneticraft.electrical.switch_redstone.requires_signal", chinese ? "有信号时导通" : "Conduct with signal");
        add("gui.magneticraft.electrical.switch_redstone.requires_no_signal", chinese ? "无信号时导通" : "Conduct without signal");
        add("gui.magneticraft.electrical.control_state", chinese ? "状态：%s" : "State: %s");
        add("gui.magneticraft.electrical.state.conducting", chinese ? "导通" : "conducting");
        add("gui.magneticraft.electrical.state.open", chinese ? "断开" : "open");
        add("gui.magneticraft.electrical.control_telemetry", chinese
                ? "%s · %s A · 损耗 %s J/t"
                : "%s · %s A · loss %s J/t");
        add("gui.magneticraft.electrical.resistance", chinese ? "电阻：%s Ω" : "Resistance: %s Ω");
        add("gui.magneticraft.electrical.resistor_ring", chinese ? "色环 %s：%s" : "Band %s: %s");
        add("gui.magneticraft.electrical.state.redstone_open", chinese ? "红石断开" : "redstone open");
        add("gui.magneticraft.electrical.state.tripped", chinese ? "已跳闸" : "tripped");
        add("gui.magneticraft.electrical.state.blown", chinese ? "已熔断" : "blown");
        add("gui.magneticraft.electrical.state.ready", chinese ? "就绪" : "ready");
        add("jei.magneticraft.electrical.lines", chinese
                ? "层级决定电压、额定电流、颜色和连接距离；不同层级线路不会连接。"
                : "The tier controls voltage, current rating, color, and connection range; unlike tiers never connect.");
        add("jei.magneticraft.electrical.storage", chinese
                ? "原生电力储能按层级限制容量和充放电速率，不向任意方块面暴露通用 FE。"
                : "Native electrical storage is tier-limited and does not expose universal FE on arbitrary faces.");
        add("jei.magneticraft.electrical.protection", chinese
                ? "保险丝和断路器必须匹配线路层级与额定规格，并在受保护电缆之前动作。"
                : "Fuses and breakers must match the line tier and rating, and operate before the protected cable.");
        add("jei.magneticraft.electrical.transformers", chinese
                ? "变压配置固定两侧层级、功率上限和效率；两侧端子保持物理隔离。"
                : "A transformer profile fixes both tiers, transfer limit, and efficiency while keeping terminals isolated.");
        add("jei.magneticraft.electrical.voltmeter", chinese
                ? "潜行空中右击循环点测、网络摘要和故障定位模式。"
                : "Sneak-use in the air to cycle point probe, network summary, and fault locator modes.");
        add("tooltip.magneticraft.small_tank.line_0", chinese
                ? "装有 %s mB %s"
                : "Holding %s mB of %s");
        add("gui.magneticraft.pressure.value", chinese
                ? "压力：%s kPa / %s bar / %s psi"
                : "Pressure: %s kPa / %s bar / %s psi");
        add("gui.magneticraft.pressure.amount", chinese
                ? "存量：%s / %s kPa·L"
                : "Amount: %s / %s kPa·L");
        add("gui.magneticraft.pressure.warning", chinese ? "警告：压力接近额定上限" : "Warning: near rated pressure");
        add("gui.magneticraft.pressure.empty", chinese ? "空" : "Empty");

        ModItems.materials().forEach((form, metals) -> metals.forEach((metal, holder) ->
                add(holder.get(), chinese ? form.chineseName(metal) : form.englishName(metal))
        ));
        add(ModItems.PLASTIC_SHEET.get(), chinese ? "塑料片" : "Plastic Sheet");
        add(ModItems.RUBBER.get(), chinese ? "橡胶" : "Rubber");
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

        add(ModAdvancedBlocks.MULTIBLOCK_BASE.get(), chinese ? "机器外壳" : "Machine Casing");
        add(ModAdvancedBlocks.CORRUGATED_IRON.get(), chinese ? "波纹铁板" : "Corrugated Iron");
        add(ModAdvancedBlocks.COPPER_COIL.get(), chinese ? "铜线圈" : "Copper Coil");
        add(ModAdvancedBlocks.MULTIBLOCK_COLUMN.get(), chinese ? "机器支撑柱" : "Machine Support Column");
        add(ModAdvancedBlocks.STRIPED_MULTIBLOCK_PART.get(), chinese ? "警示条纹机器外壳" : "Striped Machine Casing");
        add(ModAdvancedBlocks.ELECTRIC_MULTIBLOCK_PART.get(), chinese ? "电气机器外壳" : "Electrical Machine Casing");
        add(ModAdvancedBlocks.MULTIBLOCK_GAP.get(), chinese ? "已成型多方块结构" : "Formed Multiblock Structure");
        add(ModAdvancedBlocks.PUMPJACK_DRILL.get(), chinese ? "抽油机钻杆" : "Pumpjack Drill");
        add(ModAdvancedBlocks.OIL_DEPOSIT.get(), chinese ? "地下油藏" : "Oil Deposit");
        add("tooltip.magneticraft.oil_deposit.full", chinese ? "储量：100%" : "Reserve: 100%");
        add("tooltip.magneticraft.oil_deposit.empty", chinese ? "储量：已耗尽" : "Reserve: depleted");
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
        add("message.magneticraft.multiblock_formed", chinese ? "%s 已成型" : "%s formed");
        add("message.magneticraft.multiblock_status", chinese ? "%s：%s" : "%s: %s");
        add("message.magneticraft.multiblock_mirrored_state", chinese ? "镜像" : "mirrored");
        add("message.magneticraft.multiblock_normal_state", chinese ? "非镜像" : "normal");
        add("gui.magneticraft.multiblock_structure_state", chinese ? "结构：%s" : "Structure: %s");
        add("gui.magneticraft.multiblock_overview", chinese ? "%1$s · 朝向%2$s · 端口%3$s" : "%1$s · %2$s · %3$s ports");
        add("gui.magneticraft.multiblock.formed", chinese ? "已成型" : "Formed");
        add("gui.magneticraft.multiblock.unformed", chinese ? "未成型" : "Unformed");
        add("gui.magneticraft.multiblock_structure_detail", chinese ? "结构形态：%s" : "Structure layout: %s");
        add("gui.magneticraft.multiblock_port_status", chinese
                ? "%1$s%2$s · %3$s面 ×%4$s · %5$s"
                : "%1$s %2$s · %3$s ×%4$s · %5$s");
        add("gui.magneticraft.multiblock.port_active", chinese ? "已开放" : "Available");
        add("gui.magneticraft.multiblock.port_inactive", chinese ? "已关闭" : "Unavailable");
        add("gui.magneticraft.multiblock.port_kind.item", chinese ? "物品" : "Item");
        add("gui.magneticraft.multiblock.port_kind.fluid", chinese ? "流体" : "Fluid");
        add("gui.magneticraft.multiblock.port_kind.electricity", chinese ? "电力" : "Electricity");
        add("gui.magneticraft.multiblock.port_kind.heat", chinese ? "热力" : "Heat");
        add("gui.magneticraft.multiblock.port_mode.input", chinese ? "输入" : "input");
        add("gui.magneticraft.multiblock.port_mode.output", chinese ? "输出" : "output");
        add("gui.magneticraft.multiblock.port_mode.both", chinese ? "双向" : "bidirectional");
        add("gui.magneticraft.multiblock.port_mode.connection", chinese ? "连接" : "connection");
        add("gui.magneticraft.multiblock.port_mode.none", chinese ? "禁用" : "disabled");
        add("message.magneticraft.multiblock_hologram_enabled", chinese ? "多方块全息投影已开启" : "Multiblock hologram enabled");
        add("message.magneticraft.multiblock_hologram_disabled", chinese ? "多方块全息投影已关闭" : "Multiblock hologram disabled");
        add("commands.magneticraft.multiblock.fill.no_controller", chinese ? "请将准星对准未成型的多方块主体" : "Look at an unformed multiblock controller");
        add("commands.magneticraft.multiblock.fill.already_formed", chinese ? "目标多方块已经组装" : "The targeted multiblock is already formed");
        add("commands.magneticraft.multiblock.fill.unavailable", chinese ? "无法填充未加载或越界的结构位置：%s" : "Cannot fill unloaded or out-of-bounds structure position: %s");
        add("commands.magneticraft.multiblock.fill.success", chinese ? "已为 %s 更新 %s 个结构位置" : "Updated %s at %s structure positions");
        add("message.magneticraft.hydraulic_press_mode", chinese ? "液压机模式：%s" : "Hydraulic press mode: %s");
        add("message.magneticraft.hydraulic_press_mode.light", chinese ? "轻型" : "light");
        add("message.magneticraft.hydraulic_press_mode.medium", chinese ? "中型" : "medium");
        add("message.magneticraft.hydraulic_press_mode.heavy", chinese ? "重型" : "heavy");
        add("message.magneticraft.shelving_capacity", chinese ? "已安装箱体 %s/%s，可用槽位 %s" : "%s/%s chests installed; %s slots available");
        add("message.magneticraft.shelving_requires_formed", chinese ? "请先组装货架结构" : "Form the shelving structure first");
        add("message.magneticraft.shelving_owner_denied", chinese ? "无权升级此货架" : "You cannot upgrade this shelving unit");
        add("message.magneticraft.shelving_full", chinese ? "货架已安装全部 %s 个箱体" : "All %s shelving chests are installed");
        add("message.magneticraft.shelving_chest_installed", chinese ? "箱体已安装：%s/%s，可用槽位 %s" : "Chest installed: %s/%s; %s slots available");

        add(ModComputerContent.COMPUTER.get(), chinese ? "可编程计算机" : "Programmable Computer");
        add(ModComputerContent.MINING_ROBOT.get(), chinese ? "采矿机器人" : "Mining Robot");
        add(ModComputerContent.FLOPPY_DISK.get(), chinese ? "软盘" : "Floppy Disk");
        add("tooltip.magneticraft.floppy_disk.instructions", chinese ? "已存储 %s 条指令" : "%s instructions stored");
        add("tooltip.magneticraft.floppy_disk.preset", chinese ? "介质：%s" : "Media: %s");
        add("tooltip.magneticraft.floppy_disk.language", chinese ? "语言：%s" : "Language: %s");
        add("tooltip.magneticraft.floppy_disk.storage", chinese ? "存储：%s/%s 字节" : "Storage: %s/%s bytes");
        add("tooltip.magneticraft.floppy_disk.read_only", chinese ? "只读预置介质" : "Read-only preset media");
        add("tooltip.magneticraft.floppy_disk.corrupt", chinese ? "损坏的软盘数据" : "Corrupt floppy data");
        add("message.magneticraft.computer.access_denied", chinese ? "无权管理此设备" : "Access denied");
        add("message.magneticraft.computer.floppy_written", chinese ? "程序已写入软盘" : "Program written to floppy disk");
        add("message.magneticraft.computer.floppy_read_only", chinese ? "此预置软盘为只读介质" : "This preset floppy is read-only");
        add("message.magneticraft.computer.floppy_loaded", chinese ? "程序已从软盘载入" : "Program loaded from floppy disk");
        add("message.magneticraft.computer.floppy_invalid", chinese ? "软盘程序无效" : "Invalid floppy program");
        add("message.magneticraft.computer.fault", chinese ? "计算机故障：%s" : "Computer fault: %s");
        add("message.magneticraft.computer.running", chinese ? "运行中（PC=%s，版本=%s）" : "Running (PC=%s, revision=%s)");
        add("message.magneticraft.computer.stopped", chinese ? "已停止（PC=%s，版本=%s）" : "Stopped (PC=%s, revision=%s)");
        for (ComputerOpcode opcode : ComputerOpcode.values()) {
            add(opcode.descriptionTranslationKey(), chinese ? opcodeChinese(opcode) : opcodeEnglish(opcode));
        }
        addClientTranslations();
    }

    private void addClientTranslations() {
        add(ModItems.GUIDE_BOOK.get(), chinese ? "磁场工艺指南" : "Magneticraft Guide");
        add("container.magneticraft.computer", chinese ? "可编程计算机" : "Programmable Computer");
        add("container.magneticraft.mining_robot", chinese ? "采矿机器人" : "Mining Robot");
        add("gui.magneticraft.state.running", chinese ? "运行中" : "Running");
        add("gui.magneticraft.state.stopped", chinese ? "已停止" : "Stopped");
        add("gui.magneticraft.energy.tooltip", chinese ? "储能：%s / %s J" : "Stored: %s / %s J");
        add("gui.magneticraft.forge_energy.tooltip", chinese ? "能量：%s / %s FE" : "Energy: %s / %s FE");
        add("gui.magneticraft.progress.tooltip", chinese ? "进度：%s / %s" : "Progress: %s / %s");
        add("gui.magneticraft.machine.rate.tooltip", chinese ? "消耗：%s/t；产出：%s/t" : "Consumption: %s/t; production: %s/t");
        add("gui.magneticraft.items.tooltip", chinese ? "物品：%s / %s" : "Items: %s / %s");
        add("gui.magneticraft.fluid.tooltip", chinese ? "流体：%s / %s mB" : "Fluid: %s / %s mB");
        add("gui.magneticraft.temperature_kelvin", chinese ? "%s K" : "%s K");
        add("gui.magneticraft.temperature_celsius", chinese ? "%s °C" : "%s °C");
        add("gui.magneticraft.voltage", chinese ? "%s V" : "%s V");
        add("gui.magneticraft.inserter.whitelist", chinese ? "白" : "W");
        add("gui.magneticraft.inserter.match_damage", chinese ? "损" : "D");
        add("gui.magneticraft.inserter.match_tags", chinese ? "标" : "T");
        add("gui.magneticraft.inserter.match_nbt", chinese ? "NBT" : "NBT");
        add("gui.magneticraft.inserter.allow_stacking", chinese ? "组" : "S");
        add("gui.magneticraft.inserter.reverse", chinese ? "反" : "R");
        add("gui.magneticraft.shelving_summary", chinese ? "箱体：%s，可用槽位：%s" : "Chests: %s, available slots: %s");
        add("gui.magneticraft.programmable.instruction", chinese ? "代码行 %s" : "Source line %s");
        add("gui.magneticraft.programmable.upload", chinese ? "上传" : "Upload");
        add("gui.magneticraft.programmable.page", chinese ? "页 %s/%s" : "Page %s/%s");
        add("gui.magneticraft.programmable.counters", chinese ? "PC %s · 红石 %s" : "PC %s · RS %s");
        add("gui.magneticraft.programmable.invalid_program", chinese ? "程序无效：%s" : "Invalid program: %s");
        add("gui.magneticraft.programmable.fault.invalid_program_counter", chinese ? "故障：指令地址无效" : "Fault: invalid program counter");
        add("gui.magneticraft.programmable.fault.invalid_register", chinese ? "故障：寄存器无效" : "Fault: invalid register");
        add("gui.magneticraft.programmable.fault.invalid_memory_address", chinese ? "故障：内存地址无效" : "Fault: invalid memory address");
        add("gui.magneticraft.programmable.fault.division_by_zero", chinese ? "故障：除数为零" : "Fault: division by zero");
        add("gui.magneticraft.programmable.fault.unsupported_device_instruction", chinese ? "故障：设备指令不支持" : "Fault: unsupported device instruction");
        add("gui.magneticraft.programmable.fault.invalid_source", chinese ? "故障：源程序无效" : "Fault: invalid source");
        add("gui.magneticraft.programmable.fault.data_stack_underflow", chinese ? "故障：数据栈下溢" : "Fault: data stack underflow");
        add("gui.magneticraft.programmable.fault.data_stack_overflow", chinese ? "故障：数据栈溢出" : "Fault: data stack overflow");
        add("gui.magneticraft.programmable.fault.return_stack_overflow", chinese ? "故障：返回栈溢出" : "Fault: return stack overflow");
        add("gui.magneticraft.programmable.fault.memory_exhausted", chinese ? "故障：内存耗尽" : "Fault: memory exhausted");
        add("gui.magneticraft.programmable.fault.execution_limit", chinese ? "故障：执行预算耗尽" : "Fault: execution budget exhausted");
        add("gui.magneticraft.programmable.fault.device_budget_exhausted", chinese ? "故障：设备调用预算耗尽" : "Fault: device-call budget exhausted");
        add("gui.magneticraft.programmable.fault.invalid_snapshot", chinese ? "故障：状态快照无效" : "Fault: invalid snapshot");
        add("gui.magneticraft.guide.title", chinese ? "磁场工艺指南" : "Magneticraft Guide");
        add("gui.magneticraft.guide.search", chinese ? "搜索" : "Search");
        add("gui.magneticraft.guide.no_results", chinese ? "没有匹配内容" : "No matching entries");
        add("gui.magneticraft.guide.mode.structures", chinese ? "多方块结构" : "Multiblocks");
        add("gui.magneticraft.guide.mode.machines", chinese ? "单方块机器" : "Single-Block Machines");
        add("gui.magneticraft.guide.mode.opcodes", chinese ? "计算机指令" : "Computer Opcodes");
        add("gui.magneticraft.guide.mode.items", chinese ? "设备与工具" : "Equipment & Tools");
        add("gui.magneticraft.guide.mode.languages", chinese ? "编程语言" : "Programming Languages");
        add("gui.magneticraft.guide.computer.version", chinese ? "历史语义版本：%s" : "Historical semantics: %s");
        add("gui.magneticraft.guide.computer.source_limits", chinese
                ? "源码上限：%s 字节；输出上限：%s 字符"
                : "Source limit: %s bytes; output limit: %s characters");
        add("gui.magneticraft.guide.computer.tick_limits", chinese
                ? "每游戏刻：%s 条指令，%s 次设备调用"
                : "Per tick: %s instructions, %s device calls");
        add("gui.magneticraft.guide.computer.storage_limits", chinese
                ? "软盘：%s 字节/%s 条目；采石场最大边长：%s"
                : "Floppy: %s bytes/%s entries; maximum quarry side: %s");
        add("gui.magneticraft.guide.computer.examples", chinese ? "示例：%s" : "Examples: %s");
        add("gui.magneticraft.guide.computer.commands", chinese ? "命令：%s" : "Commands: %s");
        add("gui.magneticraft.guide.computer.security", chinese
                ? "服务端验证：%s；重复请求防护：%s；服务器文件访问：%s；外部网络：%s；区块强加载：%s"
                : "Server authority: %s; replay protection: %s; host files: %s; outbound network: %s; chunk loading: %s");
        add("guide.magneticraft.computer.language.forth.name", chinese ? "Forth" : "Forth");
        add("guide.magneticraft.computer.language.forth.description", chinese
                ? "受限的栈式 Forth 核心，包含历史算术、控制流、词典和设备词；栈、词典及每游戏刻执行量均有上限。"
                : "A bounded stack-based Forth core with historical arithmetic, control flow, dictionary, and device words; stacks, dictionary, and per-tick execution are limited.");
        add("guide.magneticraft.computer.language.lisp.name", chinese ? "Lisp" : "Lisp");
        add("guide.magneticraft.computer.language.lisp.description", chinese
                ? "运行在受限虚拟机上的 Lisp 层，支持定义、表达式、环境查询和设备调用；内存与求值预算会阻止无限增长。"
                : "A Lisp layer on the bounded VM with definitions, expressions, environment inspection, and device calls; memory and evaluation budgets prevent unbounded growth.");
        add("guide.magneticraft.computer.language.shell.name", chinese ? "Shell" : "Shell");
        add("guide.magneticraft.computer.language.shell.description", chinese
                ? "面向虚拟软盘与采石场命令的受限 Shell；它不能访问主机文件系统、外部网络或未加载区块。"
                : "A constrained Shell for the virtual floppy and quarry commands; it cannot access the host filesystem, outbound network, or unloaded chunks.");
        add("gui.magneticraft.guide.capacity", chinese ? "容量：%s FE" : "Capacity: %s FE");
        add("gui.magneticraft.guide.break_cost", chinese ? "破坏耗能：%s FE" : "Block cost: %s FE");
        add("gui.magneticraft.guide.attack_cost", chinese ? "攻击耗能：%s FE" : "Attack cost: %s FE");
        add("gui.magneticraft.guide.use_cost", chinese ? "使用耗能：%s FE" : "Use cost: %s FE");
        add("guide.magneticraft.item.low_voltage_battery.description", chinese
                ? "便携式 250 kFE 电池，可在电池方块中充放电。"
                : "A portable 250 kFE cell that charges and discharges in the battery block.");
        add("guide.magneticraft.item.medium_voltage_battery.description", chinese
                ? "容量为小型电池十倍的 2.5 MFE 便携电池。"
                : "A 2.5 MFE portable cell with ten times the low battery's capacity.");
        add("guide.magneticraft.item.electric_drill.description", chinese
                ? "以钻石级采掘能力高速开采镐类与铲类方块；电量不足时退化为徒手速度。"
                : "Mines pickaxe and shovel blocks at diamond tier; without enough energy it falls back to hand speed.");
        add("guide.magneticraft.item.electric_chainsaw.description", chinese
                ? "高速切割木材、树叶、藤蔓、植物、仙人掌与蜘蛛网。"
                : "Rapidly cuts wood, leaves, vines, plants, cactus, and cobwebs.");
        add("guide.magneticraft.item.electric_piston.description", chinese
                ? "命中六格内方块后反向推动使用者，也可将目标实体推离。"
                : "Pushes its user backward after targeting a block within six blocks, or pushes a targeted entity away.");
        add("guide.magneticraft.item.voltmeter.description", chinese
                ? "提供点测、网络概览和故障定位三种模式；潜行时对空气右击可切换模式，结果由服务端计算。"
                : "Provides server-authoritative point, network-summary, and fault-locator modes; sneak-use in air to cycle modes.");
        add("guide.magneticraft.item.thermometer.description", chinese
                ? "以一位小数的摄氏度读取热力节点温度。"
                : "Reads a thermal node's temperature in Celsius with one decimal place.");
        add("guide.magneticraft.item.pressure_gauge.description", chinese
                ? "按点击面读取气体类型、kPa、bar、psi、存量与容量；不支持的面不会拦截交互。"
                : "Reads gas type, kPa, bar, psi, amount, and capacity from the clicked face; unsupported faces do not consume the interaction.");
        addGuideItemDescription("brass_pressure_pipe", "输送单一类型气体；容量 2 L，额定压力 20,000 kPa。",
                "Carries one gas type with 2 L volume and a 20,000 kPa rating.");
        addGuideItemDescription("pressure_tank", "储存单一类型气体；容量 16 L，所有面均可连接压力网络与气态流体管线。",
                "Stores one gas type in 16 L and connects pressure networks and gaseous fluid handlers on every face.");
        add("guide.magneticraft.item.copper_wire_coil.description", chinese
                ? "潜行右击选择第一个端点，再右击同层级、同端口且同维度的端点建立连接；无效连接不消耗线卷。"
                : "Sneak-use a first endpoint, then use an endpoint with the same tier, port type, and dimension; invalid links do not consume the coil.");
        add("guide.magneticraft.item.electric_connector.description", chinese
                ? "壁挂式分级长距端点：低/中/高压范围为 8/16/32 格。朝外连接本模组电气端口时直接传输 J；面对仅 FE 设备时，各层级按电压数据包参数以 1 J = 1 FE 自动输出，内置满速上限为 400/1600/6400 FE/t。"
                : "A tiered wall endpoint with 8/16/32-block LV/MV/HV ranges. Its outward face carries native J to Magneticraft electrical ports; every tier automatically exports to FE-only targets at 1 J = 1 FE using its voltage-tier data rate, with built-in full-rate limits of 400/1600/6400 FE/t.");
        add("guide.magneticraft.item.electric_pole.description", chinese
                ? "五格高的分级架空线路端点：低/中/高压范围为 16/32/64 格。只连接同层级电线杆且不会强加载区块。"
                : "A five-block tiered overhead endpoint with 16/32/64-block LV/MV/HV ranges. It only links the same tier and never force-loads chunks.");
        add("guide.magneticraft.item.electric_pole_transformer.description", chinese
                ? "把完整电线杆升级为双端子架空变压设备；配置固定两侧层级、方向、功率上限与 96% 默认效率。"
                : "Upgrades a complete pole into a two-terminal overhead transformer whose profile fixes tiers, direction, limit, and default 96% efficiency.");
        add("guide.magneticraft.item.tesla_tower.description", chinese
                ? "三格高的无线发射塔。电压达到 60 V 后，可在 32 格范围内每刻传输最多 500 J。"
                : "A three-block wireless transmitter. At 60 V or more it transfers up to 500 J per tick within 32 blocks.");
        add("guide.magneticraft.item.wireless_energy_receiver.description", chinese
                ? "接收特斯拉塔传来的 J；朝外连接本模组电气端口时直接传输 J，面对仅 FE 设备时自动按 1 J = 1 FE 输出 0–400 FE/t。"
                : "Receives J from a Tesla tower. Its outward face carries native J to Magneticraft electrical ports, or automatically exports 0-400 FE/t at 1 J = 1 FE to an FE-only target.");
        add("guide.magneticraft.item.wind_turbine.description", chinese
                ? "风力发电机最高产生 200 J/t。叶轮平面与前方 16 格需要保持开阔；扫描不会加载区块。"
                : "Generates up to 200 J/t. Keep the rotor plane and 16 blocks ahead clear; its scan never loads chunks.");
        addGuideItemDescription("wrench", "配置机器、管道与网络侧面；潜行交互用于次要配置。",
                "Configures machine, pipe, and network sides; sneak-use selects secondary actions.");
        addGuideItemDescription("electric_cable", "只连接相邻且同电压层级的电气端口；持续过载会提高过载度，达到上限后电缆将烧毁。",
                "Connects adjacent same-tier electrical ports; sustained overload accumulates stress and can burn the cable.");
        addGuideItemDescription("electrical_fuse", "按层级和 8/16 C/t 规格匹配保险丝盒；熔断后需要更换。",
                "Matches a fuse box by tier and 8/16 C/t rating and must be replaced after it blows.");
        addGuideItemDescription("electrical_repair_tool", "在所有端子低于标称电压 95% 时修复故障机器；耐久为 16。",
                "Repairs a faulted machine when every terminal is below 95% nominal voltage; durability is 16.");
        addGuideItemDescription("box_transformer", "以两个隔离端子按配置在相邻电压层级之间定向变压；GUI 可安全反向。",
                "Transfers directionally between adjacent tiers through two isolated terminals; its GUI can reverse safely.");
        addGuideItemDescription("fuse_box", "使用与线路同层级、同额定规格的保险丝，过载时优先熔断并断开内部边。",
                "Uses a fuse matching line tier and rating; overload blows it and opens the internal edge first.");
        addGuideItemDescription("circuit_breaker", "过载时跳闸，可在故障消失且无红石强制断开时从 GUI 手动复位。",
                "Trips on overload and can be reset from its GUI only after the fault and redstone-forced open are gone.");
        addGuideItemDescription("electric_switch", "前后端子相互隔离，并按无信号导通、有信号导通或始终导通三种红石模式控制连接。",
                "Keeps front and back isolated and connects them according to conduct-without-signal, conduct-with-signal, or always-conduct redstone modes.");
        addGuideItemDescription("diode", "只允许电能从后端流向前端，且仅在后端电压高于前端时导通，不附加正向压降。",
                "Allows energy only from back to front while back voltage exceeds front voltage, with no added forward drop.");
        addGuideItemDescription("resistor", "用三色环设定 R=(前两位)×10^第三位 Ω，按欧姆定律双向传输并把电阻损耗散入环境。",
                "Uses three bands for R=(first two digits)×10^third digit Ω, transfers bidirectionally by Ohm's law, and dissipates resistive loss.");
        addGuideItemDescription("heat_pipe", "连接热力节点并向环境散热；区块卸载时暂停。",
                "Connects thermal nodes with environmental heat loss and pauses during chunk unload.");
        addGuideItemDescription("insulated_heat_pipe", "降低环境散热的热力传输管，不会强加载邻接区块。",
                "A thermal pipe with reduced environmental loss that never force-loads neighboring chunks.");
        addGuideItemDescription("heat_sink", "从热网主动耗散热量，用于限制机器与管网温度。",
                "Deliberately dissipates thermal-network energy to limit machine and pipe temperatures.");
        addGuideItemDescription("iron_fluid_pipe", "在兼容的流体端口之间输送流体；只有目标能够接收时才会完成传输。",
                "Performs bounded simulate-then-commit transfers between compatible fluid endpoints.");
        addGuideItemDescription("pneumatic_tube", "组成气动物流网络；目标堵塞或所在区块未加载时，运输中的物品不会丢失。",
                "Forms a bounded pneumatic logistics graph whose payloads survive blocked or unloaded targets.");
        addGuideItemDescription("pneumatic_restriction_tube", "提高该路径的物流代价，使搜索优先选择普通气动管。",
                "Raises route cost so logistics searches prefer ordinary pneumatic tubes when possible.");
        addGuideItemDescription("conveyor_belt", "水平搬运物品；支持右键放入或取出、转弯连接，前方堵塞时停止输送。",
                "Moves an internal item payload horizontally with right-click access, corners, and blockage backpressure.");
        addGuideItemDescription("inserter_speed_upgrade", "缩短机械臂动作间隔；只对机械臂的真实升级槽生效。",
                "Reduces inserter action delay and applies only through its real upgrade slot.");
        addGuideItemDescription("inserter_stack_upgrade", "允许机械臂单次搬运更大物品组，不绕过目标容量检查。",
                "Lets an inserter move larger stacks without bypassing destination-capacity checks.");
        addGuideItemDescription("computer", "运行受限的 Forth、Lisp 或 Shell 程序，并通过由服务端验证的内部设备总线访问世界。",
                "Runs bounded Forth, Lisp, or Shell programs and reaches the world only through a server-validated device bus.");
        addGuideItemDescription("mining_robot", "按权限、能量、距离、区块和每游戏刻执行上限完成移动、扫描、采掘与采石场任务。",
                "Executes movement, scan, mining, and quarry tasks under permission, energy, range, chunk, and tick budgets.");
        addGuideItemDescription("floppy_disk", "保存版本化程序与虚拟文件系统；损坏、未来版本及只读预置介质会安全拒绝写入。",
                "Stores versioned programs and a virtual filesystem; corrupt, future-version, and read-only preset media fail safely.");
        addGuideItemDescription("oil_deposit", "保存有限原油储量；抽油机只在油藏与结构均已加载且有效时抽取。",
                "Stores a finite crude-oil reserve that a pumpjack extracts only while source and structure are loaded and valid.");
        addGuideItemDescription("oil_prospector", "消耗 500 J 扫描点击位置水平 128 格内已生成并登记的油田，只报告方向、约略距离、高度范围与剩余比例。",
                "Spends 500 J to survey registered generated oil fields within 128 horizontal blocks, reporting only direction, rounded distance, height range, and remaining reserve.");
        addMultiblockGuideTranslations();
        add("gui.magneticraft.guide.layer", chinese ? "层 %s/%s" : "Layer %s/%s");
        add("gui.magneticraft.guide.yes", chinese ? "是" : "yes");
        add("gui.magneticraft.guide.no", chinese ? "否" : "no");
        add("gui.magneticraft.guide.none", chinese ? "无" : "none");
        add("gui.magneticraft.guide.mirroring", chinese ? "支持镜像：%s" : "Mirroring: %s");
        add("gui.magneticraft.guide.ports.items", chinese ? "物品槽：%s，大宗容量：%s" : "Item slots: %s, bulk: %s");
        add("gui.magneticraft.guide.ports.energy", chinese ? "电力：%s，热力：%s" : "Electricity: %s, heat: %s");
        add("gui.magneticraft.guide.ports.tanks", chinese ? "流体罐容量：%s mB" : "Tank capacities: %s mB");
        add("gui.magneticraft.guide.opcode_signature", chinese ? "编号 %s · 操作数 %s" : "Code %s · Operands %s");
        add("gui.magneticraft.guide.category.storage", chinese ? "存储" : "Storage");
        add("gui.magneticraft.guide.category.processing", chinese ? "加工" : "Processing");
        add("gui.magneticraft.guide.category.oil", chinese ? "石油" : "Oil");
        add("gui.magneticraft.guide.category.energy", chinese ? "能源" : "Energy");
        add("gui.magneticraft.guide.category.utility", chinese ? "实用设备" : "Utility");
        add("gui.magneticraft.guide.machine.inventory", chinese ? "物品槽：%s；虚拟槽：%s" : "Item slots: %s; ghost slots: %s");
        add("gui.magneticraft.guide.machine.menu", chinese ? "界面：%s" : "Menu: %s");
        add("gui.magneticraft.guide.machine.redstone", chinese ? "红石控制：%s" : "Redstone control: %s");
        add("gui.magneticraft.guide.machine.processing", chinese ? "处理类型：%s" : "Processing: %s");
        add("gui.magneticraft.guide.machine.recipe", chinese ? "配方入口：%s" : "Recipe entry: %s");
        add("gui.magneticraft.guide.machine.automation", chinese ? "自动化：%s" : "Automation: %s");
        add("gui.magneticraft.guide.machine.slots", chinese ? "槽位用途：%s" : "Slot roles: %s");
        add("gui.magneticraft.guide.machine.ports", chinese ? "外部端口：%s" : "Physical ports: %s");
        add("gui.magneticraft.guide.redstone.ignored", chinese ? "忽略" : "Ignored");
        addGuideValues("processing", new String[][]{
                {"none", "无", "None"},
                {"crushing_table", "压碎", "Crushing"},
                {"crafting", "合成", "Crafting"},
                {"sluice_box", "淘洗", "Sluicing"},
                {"smelting", "冶炼", "Smelting"},
                {"gasification", "气化", "Gasification"},
                {"thermopile", "温差发电", "Thermopile"}
        });
        addGuideValues("automation", new String[][]{
                {"none", "无外部自动化", "No external automation"},
                {"all_sides", "所有侧面", "All sides"},
                {"all_except_output_face", "除输出面外的所有侧面", "All sides except output face"},
                {"item_input_output", "物品输入与输出", "Item input and output"},
                {"fluid_output_all_sides", "所有侧面流体输出", "Fluid output on all sides"},
                {"boiler_all_sides", "所有侧面锅炉端口", "Boiler ports on all sides"},
                {"electric_top_bottom_back", "顶部、底部与背面电力端口", "Electrical ports on the top, bottom, and back"},
                {"electricity_with_vertical_heat", "全侧电网与垂直热端口", "Electricity on all sides with vertical heat ports"},
                {"all_sides_forge_energy_with_vertical_heat", "全侧 Forge Energy 与垂直热端口", "Forge Energy on all sides with vertical heat ports"},
                {"pneumatic_only", "仅气动网络", "Pneumatic network only"},
                {"fluid_top_electric_rear_heat_bottom", "顶部流体输入、后部电力输出与底部热输出", "Top fluid input, rear electrical output, and bottom heat output"},
                {"heat_top", "顶部热输出", "Top heat output"}
        });
        addGuideValues("slot", new String[][]{
                {"storage", "存储", "Storage"},
                {"input", "输入", "Input"},
                {"output", "输出", "Output"},
                {"fuel", "燃料", "Fuel"},
                {"charge", "充电", "Charge"},
                {"discharge", "放电", "Discharge"},
                {"carried", "搬运中物品", "Carried item"},
                {"upgrade", "升级", "Upgrade"},
                {"internal_buffer", "内部缓冲", "Internal buffer"}
        });
        addGuideValues("port", new String[][]{
                {"item", "物品", "Item"},
                {"item_transfer", "物品搬运", "Item transfer"},
                {"ghost_filter", "虚拟过滤槽", "Ghost filter"},
                {"fluid_input", "流体输入", "Fluid input"},
                {"fluid_output", "流体输出", "Fluid output"},
                {"forge_energy", "Forge Energy", "Forge Energy"},
                {"electricity", "电力", "Electricity"},
                {"heat", "热力", "Heat"},
                {"pneumatic", "气动物流", "Pneumatic logistics"}
        });
        add("jei.magneticraft.advanced_processing", chinese ? "高级加工" : "Advanced Processing");
        add("jei.magneticraft.chance", chinese ? "概率：%s%%" : "Chance: %s%%");
        add("jei.magneticraft.conductivity", chinese ? "导热系数：%s" : "Conductivity: %s");
        add("jei.magneticraft.duration", chinese ? "耗时：%s 刻" : "Duration: %s ticks");
        add("jei.magneticraft.energy_per_tick", chinese ? "能耗：%s FE/t" : "Energy: %s FE/t");
        add("jei.magneticraft.minimum_temperature", chinese ? "最低温度：%s K" : "Minimum: %s K");
        add("jei.magneticraft.machine", chinese ? "机器：%s" : "Machine: %s");
        add("jei.magneticraft.power", chinese ? "功率：%s FE/t" : "Power: %s FE/t");
        add("jei.magneticraft.press_mode", chinese ? "压力模式：%s" : "Press mode: %s");
        add("jei.magneticraft.required_level", chinese ? "所需等级：%s" : "Required level: %s");
        add("jei.magneticraft.temperature", chinese ? "温度：%s K" : "Temperature: %s K");
        add("jei.magneticraft.total_energy", chinese ? "能量：%s FE/mB" : "Energy: %s FE/mB");
        add("material.magneticraft.tungsten", chinese ? "钨" : "Tungsten");
        add(
                "material.magneticraft.tungsten.flavor",
                chinese ? "致密、坚韧，经久耐用。" : "Dense, stubborn, and built to last."
        );
        add(
                "material.magneticraft.tungsten.encyclopedia",
                chinese
                        ? "提供高耐久与伤害，但会降低挖掘和攻击速度。"
                        : "Grants high durability and damage at the cost of mining and attack speed."
        );
        add(
                "material.magneticraft.tungsten.ranged",
                chinese
                        ? "提供高耐久与弹射物威力，但会降低拉弓速度和精准度。"
                        : "Grants high durability and projectile power at the cost of draw speed and accuracy."
        );
        addGuideRule("ignore", "忽略", "Ignored");
        addGuideRule("air", "空气", "Air");
        addGuideRule("controller", "控制器", "Controller");
        addGuideRule("base", "机器外壳", "Machine Casing");
        addGuideRule("grate", "铁格栅", "Iron Grate");
        addGuideRule("corrugated_iron", "波纹铁板", "Corrugated Iron");
        addGuideRule("copper_coil", "铜线圈", "Copper Coil");
        addGuideRule("bricks", "砖块", "Bricks");
        addGuideRule("small_tank", "小型储罐", "Small Tank");
        addGuideRule("striped", "警示条纹机器外壳", "Striped Machine Casing");
        addGuideRule("electric", "电气机器外壳", "Electrical Machine Casing");
        addGuideRule("column_x", "X 轴机器支撑柱", "X-axis Machine Support Column");
        addGuideRule("column_y", "Y 轴机器支撑柱", "Y-axis Machine Support Column");
        addGuideRule("column_z", "Z 轴机器支撑柱", "Z-axis Machine Support Column");
        addGuideRule("unknown", "未知部件", "Unknown Part");
        addSingleBlockGuideTranslations();
    }

    private void addGuideItemDescription(String id, String chineseText, String englishText) {
        add("guide.magneticraft.item." + id + ".description", chinese ? chineseText : englishText);
    }

    private void addOilProspectorDirections() {
        String[][] directions = {
                {"here", "当前位置", "at the current position"},
                {"north", "北", "north"},
                {"northeast", "东北", "northeast"},
                {"east", "东", "east"},
                {"southeast", "东南", "southeast"},
                {"south", "南", "south"},
                {"southwest", "西南", "southwest"},
                {"west", "西", "west"},
                {"northwest", "西北", "northwest"}
        };
        for (String[] direction : directions) {
            add(
                    "message.magneticraft.oil_prospector.direction." + direction[0],
                    chinese ? direction[1] : direction[2]
            );
        }
    }

    private void addMultiblockGuideTranslations() {
        for (MultiblockDefinition definition : MultiblockDefinition.values()) {
            add(
                    "guide.magneticraft.multiblock." + definition.id() + ".description",
                    chinese ? multiblockGuideChinese(definition) : multiblockGuideEnglish(definition)
            );
        }
    }

    private static String multiblockGuideChinese(MultiblockDefinition definition) {
        return switch (definition) {
            case STIRLING_GENERATOR -> "接收外部热量或燃烧固体燃料，以 75% 热电效率输出低压 J；输出受阻时不抽热也不耗燃料。";
            case POLYMERIZER -> "达到配方最低温度后持续消耗热量，将液态塑料制成塑料片，或用天然气与硫磺制成橡胶；输入或输出受阻时暂停。";
            case BIG_COMBUSTION_CHAMBER -> "燃烧固体燃料并向大型热力网络供热；结构或输出受阻时保留燃料。";
            case BIG_ELECTRIC_FURNACE -> "使用原生电力执行高容量熔炼；完整输出可提交时才消耗输入。";
            case BIG_STEAM_BOILER -> "把水与热量转换为蒸汽；输出空间不足时暂停工作，并保留已有的水、蒸汽和热量。";
            case CONTAINER -> "用于大量存储同一种物品，存储数量存在上限；自动化模拟输入或输出时不会改变库存。";
            case GRINDER -> "执行研磨高级加工配方，库存、能量、配方和进度均可持久化。";
            case HYDRAULIC_PRESS -> "按轻型、中型或重型冲压模式处理对应配方；模式切换由服务端确认。";
            case OIL_HEATER -> "消耗热量把原油转换为加热原油，输出罐不足时暂停。";
            case PUMPJACK -> "从已加载的有限油藏抽取原油，不强加载区块且不会使储量为负。";
            case REFINERY -> "将加热原油一次性分离到五个专用储罐；任一输出储罐已满时暂停。";
            case SHELVING_UNIT -> "通过受所有权保护的箱体升级解锁存储槽，自动化无法访问未解锁容量。";
            case SIEVE -> "执行筛分高级加工配方，只有完整产物可接收时才提交输入。";
            case SOLAR_MIRROR -> "在日照和加载条件允许时向有效太阳能塔贡献聚光热量。";
            case SOLAR_PANEL -> "在天空无遮挡的日照条件下产生原生电力；夜晚或电网无法继续接收能量时暂停发电。";
            case SOLAR_TOWER -> "汇集有效反射镜的聚光贡献，并从声明的热力端口输出热量。";
            case STEAM_ENGINE -> "消耗蒸汽并产生原生电力；电网无法继续接收能量时停止消耗蒸汽。";
            case STEAM_TURBINE -> "以设定的最高速率将蒸汽转换为 J；所在区块卸载时暂停工作。";
        };
    }

    private static String multiblockGuideEnglish(MultiblockDefinition definition) {
        return switch (definition) {
            case STIRLING_GENERATOR -> "Accepts external heat or burns solid fuel and converts heat to low-voltage joules at 75% efficiency; blocked output consumes neither heat nor fuel.";
            case POLYMERIZER -> "Consumes heat above the recipe temperature to turn liquid plastic into sheets or natural gas and sulfur into rubber, pausing under input or output backpressure.";
            case BIG_COMBUSTION_CHAMBER -> "Burns solid fuel into a large thermal network and preserves fuel while structure or output is blocked.";
            case BIG_ELECTRIC_FURNACE -> "Performs high-capacity smelting with native electricity and consumes input only when full output can commit.";
            case BIG_STEAM_BOILER -> "Converts water and heat into steam while preserving all input, output, and thermal state under backpressure.";
            case CONTAINER -> "Provides bulk storage as one item identity and bounded count with side-effect-free simulated insertion and extraction.";
            case GRINDER -> "Runs grinder advanced-processing recipes with persistent inventory, energy, recipe identity, and progress.";
            case HYDRAULIC_PRESS -> "Runs matching light, medium, or heavy press recipes with server-validated mode changes.";
            case OIL_HEATER -> "Uses heat to convert crude oil into heated crude oil and pauses when its output tank lacks capacity.";
            case PUMPJACK -> "Extracts a finite loaded oil deposit without force-loading chunks or allowing a negative reserve.";
            case REFINERY -> "Atomically separates heated crude oil into five role-specific tanks and pauses when any output is blocked.";
            case SHELVING_UNIT -> "Unlocks storage through owner-protected chest upgrades and hides locked capacity from automation.";
            case SIEVE -> "Runs sieve advanced-processing recipes and commits input only when the complete result can be accepted.";
            case SOLAR_MIRROR -> "Contributes concentrated heat to a valid solar tower while daylight and loaded-chunk conditions permit.";
            case SOLAR_PANEL -> "Generates native electricity under unobstructed daylight and pauses at night or under backpressure.";
            case SOLAR_TOWER -> "Collects valid mirror contributions and outputs their concentrated heat through the declared thermal port.";
            case STEAM_ENGINE -> "Consumes steam to generate native electricity and retains input fluid under electrical backpressure.";
            case STEAM_TURBINE -> "Converts steam into native electricity at a bounded rate and pauses safely while unloaded.";
        };
    }

    private void addGuideValues(String group, String[][] values) {
        for (String[] value : values) {
            add(
                    "gui.magneticraft.guide." + group + "." + value[0],
                    chinese ? value[1] : value[2]
            );
        }
    }

    private void addSingleBlockGuideTranslations() {
        for (SingleBlockMachineDefinition definition : SingleBlockMachineDefinition.values()) {
            add(
                    "guide.magneticraft.machine." + definition.id() + ".description",
                    chinese ? singleBlockGuideChinese(definition) : singleBlockGuideEnglish(definition)
            );
        }
        add("guide.magneticraft.machine.crushing_table.description", chinese
                ? "用对应等级的锤子反复击打台面上的物品，按旧版命中次数完成压碎；服务端决定产物和工具耐久。"
                : "Strike the item on the table with a suitable hammer for the legacy hit count; the server owns results and tool damage.");
        add("guide.magneticraft.machine.battery_box.description", chinese
                ? "在两个内部槽位与磁场工艺电网之间充放便携电池；不向外暴露 Forge Energy 能力。"
                : "Charges and discharges portable cells through two internal slots and the Magneticraft grid; it exposes no external Forge Energy capability.");
        add("guide.magneticraft.machine.electric_furnace.description", chinese
                ? "消耗磁场工艺电力执行原版熔炼配方；输入与输出槽分别接受自动化访问，进度随存档保存。"
                : "Uses Magneticraft electricity for vanilla smelting recipes; automation sees separate input and output slots, and progress persists.");
    }

    private static String singleBlockGuideChinese(SingleBlockMachineDefinition definition) {
        return switch (definition) {
            case INTERNAL_COMBUSTION_ENGINE -> "消耗流体燃料并按 70% 电能、30% 热量分配；温度超过 423.15 K 后逐步降功率，达到 773.15 K 时停机。";
            case GEOTHERMAL_PUMP -> "只在已加载区块中向下铺设钻管；发现熔岩层后按需将源方块转为黑曜石并输出热量。";
            case BOX -> "提供 27 格木制存储，所有侧面均可进行物品自动化。";
            case SLUICE_BOX -> "成链放置并供水后处理淘洗配方；上游状态改变会重置已加载的下游链路。";
            case FEEDING_TROUGH -> "每 400 刻尝试用小麦、胡萝卜或小麦种子喂养范围内两只可繁殖动物。";
            case SMALL_TANK -> "保存单种流体并支持侧面输入输出；使用桶等容器交互时，只有完整操作能够成功才会执行，失败时不会消耗容器。";
            case FABRICATOR -> "用虚拟槽记录合成图样并读取相邻库存；右击结果清除图样，左击请求合成。";
            case INSERTER -> "在相邻库存间搬运物品，可配置过滤、方向、速度升级和批量升级。";
            case WATER_GENERATOR -> "内置无限水源，六个侧面各自最多输出 20 mB/t，且不会强加载区块。";
            case RELAY -> "在气动物流网络与九格物品缓冲之间转接，输出面不接受普通物品自动化。";
            case FILTER -> "用虚拟样本约束气动载荷，只在气动网络中工作。";
            case TRANSPOSER -> "按虚拟样本在气动网络与相邻目标间转运物品。";
            case COMBUSTION_CHAMBER -> "燃烧内部燃料并向热端口供热；门板用于直接装填或切换状态。";
            case STEAM_BOILER -> "从任意侧面接收水和输出蒸汽，以热量驱动转换；主动输出遵循旧版目标侧规则。";
            case ELECTRIC_HEATER -> "以每刻 80 J 的批量将磁场工艺电力转为热量；电力不足 80 J 时停止转换，机器热量会逐步散失。";
            case RF_HEATER -> "使用 80 kFE 双向缓冲从任意侧面交换 FE，并通过垂直端口输出热量。";
            case GASIFICATION_UNIT -> "在足够温度下按数据配方把物品转为气体，物品输入输出与流体输出可自动化。";
            case BRICK_FURNACE -> "以外部热量执行熔炼配方；更换配方不会抹除已积累进度，工作显示会短暂延迟熄灭。";
            case INFINITE_ENERGY -> "创意管理设备，持续维持 125 V 电源；没有生存配方。";
            case RF_TRANSFORMER -> "从侧面接收 FE，并按 1 FE = 1 J 输入低压 J 电网；机器内部不会额外保存一份 FE。";
            case ELECTRIC_ENGINE -> "仅接入中压 J 电网，不会直接输出或转换 FE；如需为 FE 设备供电，请使用同层级电力连接器。";
            case AIRLOCK -> "每 40 刻扫描半径 9 的已加载区域，以电力维持边界水泡并清除内部水体；欠压后逐步失效。";
            case THERMOPILE -> "根据两侧温差向对应层级的 J 电网发电；发电量受节点容量、输出电压和发电速率限制。";
        };
    }

    private static String singleBlockGuideEnglish(SingleBlockMachineDefinition definition) {
        return switch (definition) {
            case INTERNAL_COMBUSTION_ENGINE -> "Consumes fluid fuel and preserves a 70% electrical / 30% thermal split, throttling above 423.15 K and stopping at 773.15 K.";
            case GEOTHERMAL_PUMP -> "Extends drill pipe only through loaded chunks, then converts discovered lava sources to obsidian on demand to produce heat.";
            case BOX -> "Provides 27 wooden storage slots with item automation on every side.";
            case SLUICE_BOX -> "Processes sluice recipes when chained and watered; upstream changes reset the loaded downstream chain.";
            case FEEDING_TROUGH -> "Every 400 ticks, attempts to feed two breedable animals with wheat, carrots, or wheat seeds.";
            case SMALL_TANK -> "Stores one fluid with sided input and output; container interaction is atomic and never consumes a failed container.";
            case FABRICATOR -> "Records a crafting pattern in ghost slots and reads adjacent inventories; right-click clears the result pattern and left-click requests crafting.";
            case INSERTER -> "Moves items between adjacent inventories with filter, direction, speed, and stack upgrades.";
            case WATER_GENERATOR -> "Its internal infinite water source outputs up to 20 mB/t independently on all six sides without loading chunks.";
            case RELAY -> "Bridges a pneumatic network and a nine-slot item buffer; its output face rejects ordinary item automation.";
            case FILTER -> "Constrains pneumatic payloads with ghost samples and operates only on the pneumatic network.";
            case TRANSPOSER -> "Moves matching items between the pneumatic network and an adjacent target using ghost samples.";
            case COMBUSTION_CHAMBER -> "Burns internal fuel into heat; use its door to insert fuel directly or change its state.";
            case STEAM_BOILER -> "Accepts water and exposes steam on every side, converting with heat and retaining the legacy active-output target-side rule.";
            case ELECTRIC_HEATER -> "Converts Magneticraft electricity into heat in 80 J/t steps; below 80 J, conversion stops while stored heat gradually dissipates.";
            case RF_HEATER -> "Uses an 80 kFE bidirectional buffer on every side and emits heat through its vertical ports.";
            case GASIFICATION_UNIT -> "Converts items into gas from data recipes at sufficient temperature, with automated item I/O and fluid output.";
            case BRICK_FURNACE -> "Runs smelting recipes from external heat; recipe changes preserve accumulated progress and the working display lingers briefly.";
            case INFINITE_ENERGY -> "A creative administration device that continuously holds a 125 V source; it has no survival recipe.";
            case RF_TRANSFORMER -> "Accepts Forge Energy on its sides and writes it into the low-voltage native node at 1 FE = 1 J, without a second FE buffer.";
            case ELECTRIC_ENGINE -> "Connects only to its medium-voltage native electrical node and no longer exposes or converts Forge Energy; route J through a matching-tier connector to power FE devices.";
            case AIRLOCK -> "Every 40 ticks, spends electricity across the loaded radius-9 area to maintain boundary bubbles and clear interior water; it decays when undervolted.";
            case THERMOPILE -> "Reads a temperature difference and generates joules directly into its rated native node, bounded by node capacity, output voltage, and generation rate.";
        };
    }

    private void addGuideRule(String id, String chineseName, String englishName) {
        add("gui.magneticraft.guide.rule." + id, chinese ? chineseName : englishName);
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
