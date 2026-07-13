package committee.nova.mods.magneticraft.init;

import committee.nova.mods.magneticraft.content.network.electric.ElectricCableBlock;
import committee.nova.mods.magneticraft.content.network.fluid.IronPipeBlock;
import committee.nova.mods.magneticraft.content.network.heat.HeatPipeBlock;
import committee.nova.mods.magneticraft.content.network.heat.HeatSinkBlock;
import committee.nova.mods.magneticraft.content.network.logistics.ConveyorBeltBlock;
import committee.nova.mods.magneticraft.content.network.pneumatic.PneumaticTubeBlock;
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

    public static final RegistryObject<Block> ELECTRIC_CABLE = register(
            "electric_cable",
            () -> new ElectricCableBlock(conduitProperties(MapColor.COLOR_ORANGE))
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
            () -> new HeatSinkBlock(machineProperties())
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
}
