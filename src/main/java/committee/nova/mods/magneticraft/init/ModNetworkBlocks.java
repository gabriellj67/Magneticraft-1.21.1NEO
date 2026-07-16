package committee.nova.mods.magneticraft.init;

import committee.nova.mods.magneticraft.content.network.electric.ElectricCableBlock;
import committee.nova.mods.magneticraft.content.network.electric.BoxTransformerBlock;
import committee.nova.mods.magneticraft.content.network.electric.ElectricConnectorBlock;
import committee.nova.mods.magneticraft.content.network.electric.ElectricPoleBlock;
import committee.nova.mods.magneticraft.content.network.electric.ElectricPoleTransformerBlockItem;
import committee.nova.mods.magneticraft.content.network.electric.TeslaTowerBlock;
import committee.nova.mods.magneticraft.content.network.electric.TransformerBlockItem;
import committee.nova.mods.magneticraft.content.network.electric.WirelessEnergyReceiverBlock;
import committee.nova.mods.magneticraft.content.item.TieredElectricalBlockItem;
import committee.nova.mods.magneticraft.content.item.ProtectionBlockItem;
import committee.nova.mods.magneticraft.content.network.electric.ElectricalProtectionBlock;
import committee.nova.mods.magneticraft.content.network.electric.ElectricalProtectionKind;
import committee.nova.mods.magneticraft.content.network.electric.ElectricalControlBlock;
import committee.nova.mods.magneticraft.content.network.electric.ElectricalControlKind;
import committee.nova.mods.magneticraft.content.machine.windturbine.WindTurbineBlock;
import committee.nova.mods.magneticraft.content.network.fluid.IronPipeBlock;
import committee.nova.mods.magneticraft.content.network.heat.HeatPipeBlock;
import committee.nova.mods.magneticraft.content.network.heat.HeatSinkBlock;
import committee.nova.mods.magneticraft.content.network.logistics.ConveyorBeltBlock;
import committee.nova.mods.magneticraft.content.network.pneumatic.PneumaticTubeBlock;
import committee.nova.mods.magneticraft.content.network.pressure.BrassPressurePipeBlock;
import committee.nova.mods.magneticraft.content.network.pressure.PressureTankBlock;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;
import net.minecraftforge.registries.RegistryObject;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

/**
 * Physical-network conduits and transport components.
 */
public final class ModNetworkBlocks {
    private static final List<RegistryObject<? extends Item>> BLOCK_ITEMS = new ArrayList<>();

    public static final RegistryObject<Block> ELECTRIC_CABLE = registerTiered(
            "electric_cable",
            () -> new ElectricCableBlock(conduitProperties(MapColor.COLOR_ORANGE))
    );
    public static final RegistryObject<Block> BURNT_ELECTRIC_CABLE = register(
            "burnt_electric_cable",
            () -> new Block(conduitProperties(MapColor.COLOR_BLACK))
    );
    public static final RegistryObject<Block> ELECTRIC_CONNECTOR = registerTiered(
            "electric_connector",
            () -> new ElectricConnectorBlock(machineProperties().noOcclusion())
    );
    public static final RegistryObject<Block> ELECTRIC_POLE = registerTiered(
            "electric_pole",
            () -> new ElectricPoleBlock(poleProperties(), false)
    );
    public static final RegistryObject<Block> ELECTRIC_POLE_TRANSFORMER = registerTransformerPole();
    public static final RegistryObject<Block> BOX_TRANSFORMER = registerBoxTransformer();
    public static final RegistryObject<Block> FUSE_BOX = registerProtection(
            "fuse_box",
            ElectricalProtectionKind.FUSE_BOX
    );
    public static final RegistryObject<Block> CIRCUIT_BREAKER = registerProtection(
            "circuit_breaker",
            ElectricalProtectionKind.CIRCUIT_BREAKER
    );
    public static final RegistryObject<Block> ELECTRIC_SWITCH = registerElectricalControl(ElectricalControlKind.SWITCH);
    public static final RegistryObject<Block> DIODE = registerElectricalControl(ElectricalControlKind.DIODE);
    public static final RegistryObject<Block> RESISTOR = registerElectricalControl(ElectricalControlKind.RESISTOR);
    public static final RegistryObject<Block> TESLA_TOWER = register(
            "tesla_tower",
            () -> new TeslaTowerBlock(machineProperties().noOcclusion())
    );
    public static final RegistryObject<Block> WIRELESS_ENERGY_RECEIVER = register(
            "wireless_energy_receiver",
            () -> new WirelessEnergyReceiverBlock(machineProperties().noOcclusion())
    );
    public static final RegistryObject<Block> WIND_TURBINE = register(
            "wind_turbine",
            () -> new WindTurbineBlock(machineProperties().noOcclusion())
    );
    public static final RegistryObject<Block> HEAT_PIPE = register(
            "heat_pipe",
            () -> new HeatPipeBlock(conduitProperties(MapColor.METAL), false)
    );
    public static final RegistryObject<Block> INSULATED_HEAT_PIPE = register(
            "insulated_heat_pipe",
            () -> new HeatPipeBlock(conduitProperties(MapColor.COLOR_BLACK), true)
    );
    public static final RegistryObject<Block> HEAT_SINK = register(
            "heat_sink",
            () -> new HeatSinkBlock(machineProperties().noOcclusion())
    );
    public static final RegistryObject<Block> IRON_PIPE = register(
            "iron_fluid_pipe",
            () -> new IronPipeBlock(conduitProperties(MapColor.METAL))
    );
    public static final RegistryObject<Block> PNEUMATIC_TUBE = register(
            "pneumatic_tube",
            () -> new PneumaticTubeBlock(conduitProperties(MapColor.COLOR_LIGHT_GRAY))
    );
    public static final RegistryObject<Block> PNEUMATIC_RESTRICTION_TUBE = register(
            "pneumatic_restriction_tube",
            () -> new PneumaticTubeBlock(conduitProperties(MapColor.COLOR_RED))
    );
    public static final RegistryObject<Block> BRASS_PRESSURE_PIPE = register(
            "brass_pressure_pipe",
            () -> new BrassPressurePipeBlock(conduitProperties(MapColor.COLOR_YELLOW))
    );
    public static final RegistryObject<Block> PRESSURE_TANK = register(
            "pressure_tank",
            () -> new PressureTankBlock(machineProperties())
    );
    public static final RegistryObject<Block> CONVEYOR_BELT = register(
            "conveyor_belt",
            () -> new ConveyorBeltBlock(BlockBehaviour.Properties.of()
                    .mapColor(MapColor.METAL)
                    .requiresCorrectToolForDrops()
                    .strength(2.5F, 8.0F)
                    .sound(SoundType.METAL)
                    .noOcclusion())
    );

    private ModNetworkBlocks() {
    }

    public static void bootstrap() {
    }

    public static List<RegistryObject<? extends Item>> blockItems() {
        return List.copyOf(BLOCK_ITEMS);
    }

    private static RegistryObject<Block> register(String id, Supplier<Block> factory) {
        RegistryObject<Block> block = ModRegistries.BLOCKS.register(id, factory);
        BLOCK_ITEMS.add(ModRegistries.ITEMS.register(id, () -> new BlockItem(block.get(), new Item.Properties())));
        return block;
    }

    private static RegistryObject<Block> registerTiered(String id, Supplier<Block> factory) {
        RegistryObject<Block> block = ModRegistries.BLOCKS.register(id, factory);
        BLOCK_ITEMS.add(ModRegistries.ITEMS.register(
                id,
                () -> new TieredElectricalBlockItem(block.get(), new Item.Properties())
        ));
        return block;
    }

    private static RegistryObject<Block> registerTransformerPole() {
        RegistryObject<Block> block = ModRegistries.BLOCKS.register(
                "electric_pole_transformer",
                () -> new ElectricPoleBlock(poleProperties(), true)
        );
        BLOCK_ITEMS.add(ModRegistries.ITEMS.register(
                "electric_pole_transformer",
                () -> new ElectricPoleTransformerBlockItem((ElectricPoleBlock) block.get(), new Item.Properties())
        ));
        return block;
    }

    private static RegistryObject<Block> registerBoxTransformer() {
        RegistryObject<Block> block = ModRegistries.BLOCKS.register(
                "box_transformer",
                () -> new BoxTransformerBlock(machineProperties().noOcclusion())
        );
        BLOCK_ITEMS.add(ModRegistries.ITEMS.register(
                "box_transformer",
                () -> new TransformerBlockItem(block.get(), new Item.Properties())
        ));
        return block;
    }

    private static RegistryObject<Block> registerProtection(String id, ElectricalProtectionKind kind) {
        RegistryObject<Block> block = ModRegistries.BLOCKS.register(
                id,
                () -> new ElectricalProtectionBlock(machineProperties().noOcclusion(), kind)
        );
        BLOCK_ITEMS.add(ModRegistries.ITEMS.register(
                id,
                () -> new ProtectionBlockItem(block.get(), new Item.Properties(), kind)
        ));
        return block;
    }

    private static RegistryObject<Block> registerElectricalControl(ElectricalControlKind kind) {
        RegistryObject<Block> block = ModRegistries.BLOCKS.register(
                kind.id(),
                () -> new ElectricalControlBlock(machineProperties().noOcclusion(), kind)
        );
        BLOCK_ITEMS.add(ModRegistries.ITEMS.register(
                kind.id(),
                () -> new TieredElectricalBlockItem(block.get(), new Item.Properties())
        ));
        return block;
    }

    private static BlockBehaviour.Properties conduitProperties(MapColor color) {
        return BlockBehaviour.Properties.of()
                .mapColor(color)
                .requiresCorrectToolForDrops()
                .strength(2.0F, 8.0F)
                .sound(SoundType.METAL)
                .noOcclusion();
    }

    private static BlockBehaviour.Properties machineProperties() {
        return BlockBehaviour.Properties.of()
                .mapColor(MapColor.METAL)
                .requiresCorrectToolForDrops()
                .strength(3.5F, 10.0F)
                .sound(SoundType.METAL);
    }

    private static BlockBehaviour.Properties poleProperties() {
        return BlockBehaviour.Properties.of()
                .mapColor(MapColor.WOOD)
                .strength(2.5F, 6.0F)
                .sound(SoundType.WOOD)
                .noOcclusion();
    }
}
