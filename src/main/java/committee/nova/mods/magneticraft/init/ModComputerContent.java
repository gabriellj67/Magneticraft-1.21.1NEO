package committee.nova.mods.magneticraft.init;

import committee.nova.mods.magneticraft.content.computer.ComputerBlock;
import committee.nova.mods.magneticraft.content.computer.ComputerBlockEntity;
import committee.nova.mods.magneticraft.content.computer.FloppyDiskItem;
import committee.nova.mods.magneticraft.content.computer.MiningRobotBlock;
import committee.nova.mods.magneticraft.content.computer.MiningRobotBlockEntity;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;
import net.minecraftforge.registries.RegistryObject;

import java.util.List;

/**
 * Local registration boundary for computer content. The composition root only calls {@link #bootstrap()}.
 */
public final class ModComputerContent {
    public static final RegistryObject<Block> COMPUTER = ModRegistries.BLOCKS.register(
            "computer",
            () -> new ComputerBlock(machineProperties())
    );
    public static final RegistryObject<Block> MINING_ROBOT = ModRegistries.BLOCKS.register(
            "mining_robot",
            () -> new MiningRobotBlock(machineProperties())
    );
    public static final RegistryObject<Item> COMPUTER_ITEM = ModRegistries.ITEMS.register(
            "computer",
            () -> new BlockItem(COMPUTER.get(), new Item.Properties())
    );
    public static final RegistryObject<Item> MINING_ROBOT_ITEM = ModRegistries.ITEMS.register(
            "mining_robot",
            () -> new BlockItem(MINING_ROBOT.get(), new Item.Properties())
    );
    public static final RegistryObject<Item> FLOPPY_DISK = ModRegistries.ITEMS.register(
            "floppy_disk",
            () -> new FloppyDiskItem(new Item.Properties())
    );
    public static final RegistryObject<BlockEntityType<ComputerBlockEntity>> COMPUTER_BLOCK_ENTITY =
            ModRegistries.BLOCK_ENTITY_TYPES.register(
                    "computer",
                    () -> BlockEntityType.Builder.of(ComputerBlockEntity::new, COMPUTER.get()).build(null)
            );
    public static final RegistryObject<BlockEntityType<MiningRobotBlockEntity>> MINING_ROBOT_BLOCK_ENTITY =
            ModRegistries.BLOCK_ENTITY_TYPES.register(
                    "mining_robot",
                    () -> BlockEntityType.Builder.of(MiningRobotBlockEntity::new, MINING_ROBOT.get()).build(null)
            );

    private ModComputerContent() {
    }

    public static void bootstrap() {
    }

    public static List<RegistryObject<? extends Item>> creativeItems() {
        return List.of(COMPUTER_ITEM, MINING_ROBOT_ITEM, FLOPPY_DISK);
    }

    private static BlockBehaviour.Properties machineProperties() {
        return BlockBehaviour.Properties.of()
                .mapColor(MapColor.METAL)
                .requiresCorrectToolForDrops()
                .strength(3.5F, 10.0F)
                .sound(SoundType.METAL)
                .noOcclusion();
    }
}
