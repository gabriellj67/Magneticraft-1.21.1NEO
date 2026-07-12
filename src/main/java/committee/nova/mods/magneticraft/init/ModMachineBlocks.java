package committee.nova.mods.magneticraft.init;

import committee.nova.mods.magneticraft.content.machine.battery.BatteryBlock;
import committee.nova.mods.magneticraft.content.machine.crushingtable.CrushingTableBlock;
import committee.nova.mods.magneticraft.content.machine.electricfurnace.ElectricFurnaceBlock;
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
 * Registers the machine-framework task's blocks and their items.
 */
public final class ModMachineBlocks {
    private static final List<RegistryObject<? extends Item>> BLOCK_ITEMS = new ArrayList<>();

    public static final RegistryObject<Block> CRUSHING_TABLE = register(
            "crushing_table",
            () -> new CrushingTableBlock(BlockBehaviour.Properties.of()
                    .mapColor(MapColor.WOOD)
                    .strength(2.0F)
                    .sound(SoundType.WOOD))
    );
    public static final RegistryObject<Block> BATTERY = register(
            "battery",
            () -> new BatteryBlock(machineProperties())
    );
    public static final RegistryObject<Block> GRATE = register(
            "grate",
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

    private ModMachineBlocks() {
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

    private static BlockBehaviour.Properties machineProperties() {
        return BlockBehaviour.Properties.of()
                .mapColor(MapColor.METAL)
                .requiresCorrectToolForDrops()
                .strength(3.5F, 10.0F)
                .sound(SoundType.METAL);
    }
}
