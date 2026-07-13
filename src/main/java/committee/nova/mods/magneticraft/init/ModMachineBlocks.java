package committee.nova.mods.magneticraft.init;

import committee.nova.mods.magneticraft.content.machine.battery.BatteryBlock;
import committee.nova.mods.magneticraft.content.machine.crushingtable.CrushingTableBlock;
import committee.nova.mods.magneticraft.content.machine.electricfurnace.ElectricFurnaceBlock;
import committee.nova.mods.magneticraft.content.machine.singleblock.AirBubbleBlock;
import committee.nova.mods.magneticraft.content.machine.singleblock.SingleBlockMachineBlock;
import committee.nova.mods.magneticraft.content.machine.singleblock.SingleBlockMachineDefinition;
import committee.nova.mods.magneticraft.content.machine.singleblock.SmallTankBlockItem;
import committee.nova.mods.magneticraft.content.machine.singleblock.TubeLightBlock;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;
import net.minecraftforge.registries.RegistryObject;

import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

/**
 * Registers the machine-framework task's blocks and their items.
 */
public final class ModMachineBlocks {
    private static final List<RegistryObject<? extends Item>> BLOCK_ITEMS = new ArrayList<>();
    private static final Map<SingleBlockMachineDefinition, RegistryObject<Block>> MACHINES =
            new EnumMap<>(SingleBlockMachineDefinition.class);

    public static final RegistryObject<Block> CRUSHING_TABLE = register(
            "crushing_table",
            () -> new CrushingTableBlock(BlockBehaviour.Properties.of()
                    .mapColor(MapColor.WOOD)
                    .strength(2.0F)
                    .sound(SoundType.WOOD))
    );
    public static final RegistryObject<Block> BATTERY = register(
            "battery_box",
            () -> new BatteryBlock(machineProperties())
    );
    public static final RegistryObject<Block> GRATE = register(
            "iron_grate",
            () -> new Block(BlockBehaviour.Properties.of()
                    .mapColor(MapColor.METAL)
                    .requiresCorrectToolForDrops()
                    .strength(3.0F, 10.0F)
                    .sound(SoundType.METAL)
                    .noOcclusion())
    );
    public static final RegistryObject<Block> ELECTRIC_FURNACE = register(
            "electric_furnace",
            () -> new ElectricFurnaceBlock(machineProperties())
    );
    public static final RegistryObject<Block> AIR_BUBBLE = ModRegistries.BLOCKS.register(
            "air_bubble",
            () -> new AirBubbleBlock(BlockBehaviour.Properties.of()
                    .mapColor(MapColor.WATER)
                    .noCollission()
                    .noOcclusion()
                    .randomTicks()
                    .strength(-1.0F, 3_600_000.0F)
                    .sound(SoundType.GLASS)
                    .noLootTable())
    );
    public static final RegistryObject<Block> TUBE_LIGHT = register(
            "tube_light",
            () -> new TubeLightBlock(BlockBehaviour.Properties.of()
                    .mapColor(MapColor.COLOR_YELLOW)
                    .strength(0.5F)
                    .sound(SoundType.GLASS)
                    .lightLevel(state -> 15)
                    .noOcclusion())
    );

    static {
        for (SingleBlockMachineDefinition definition : SingleBlockMachineDefinition.values()) {
            RegistryObject<Block> block = registerMachine(definition);
            MACHINES.put(definition, block);
        }
    }

    private ModMachineBlocks() {
    }

    public static void bootstrap() {
    }

    public static List<RegistryObject<? extends Item>> blockItems() {
        return List.copyOf(BLOCK_ITEMS);
    }

    public static RegistryObject<Block> machine(SingleBlockMachineDefinition definition) {
        RegistryObject<Block> block = MACHINES.get(definition);
        if (block == null) {
            throw new IllegalArgumentException("No single-block machine registered for " + definition);
        }
        return block;
    }

    public static Map<SingleBlockMachineDefinition, RegistryObject<Block>> machines() {
        return Collections.unmodifiableMap(MACHINES);
    }

    private static RegistryObject<Block> register(String id, Supplier<Block> factory) {
        RegistryObject<Block> block = ModRegistries.BLOCKS.register(id, factory);
        BLOCK_ITEMS.add(ModRegistries.ITEMS.register(id, () -> new BlockItem(block.get(), new Item.Properties())));
        return block;
    }

    private static RegistryObject<Block> registerMachine(SingleBlockMachineDefinition definition) {
        RegistryObject<Block> block = ModRegistries.BLOCKS.register(
                definition.id(),
                () -> new SingleBlockMachineBlock(definition, machineProperties(definition))
        );
        BLOCK_ITEMS.add(ModRegistries.ITEMS.register(
                definition.id(),
                () -> definition == SingleBlockMachineDefinition.SMALL_TANK
                        ? new SmallTankBlockItem(block.get(), new Item.Properties())
                        : new BlockItem(block.get(), new Item.Properties())
        ));
        return block;
    }

    private static BlockBehaviour.Properties machineProperties() {
        return BlockBehaviour.Properties.of()
                .mapColor(MapColor.METAL)
                .requiresCorrectToolForDrops()
                .strength(3.5F, 10.0F)
                .sound(SoundType.METAL);
    }

    private static BlockBehaviour.Properties machineProperties(SingleBlockMachineDefinition definition) {
        if (definition.isWooden()) {
            return BlockBehaviour.Properties.of()
                    .mapColor(MapColor.WOOD)
                    .strength(2.0F, 5.0F)
                    .sound(SoundType.WOOD);
        }
        return machineProperties();
    }
}
