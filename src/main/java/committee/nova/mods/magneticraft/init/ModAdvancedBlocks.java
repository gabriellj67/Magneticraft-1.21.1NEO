package committee.nova.mods.magneticraft.init;

import committee.nova.mods.magneticraft.content.multiblock.AdvancedMultiblockBlock;
import committee.nova.mods.magneticraft.content.multiblock.MultiblockDefinition;
import committee.nova.mods.magneticraft.content.multiblock.MultiblockGapBlock;
import committee.nova.mods.magneticraft.content.worldgen.OilDepositBlock;
import committee.nova.mods.magneticraft.content.worldgen.OilDepositBlockItem;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.RotatedPillarBlock;
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
 * Advanced-system structure parts and controller blocks.
 */
public final class ModAdvancedBlocks {
    private static final List<RegistryObject<? extends Item>> BLOCK_ITEMS = new ArrayList<>();
    private static final Map<MultiblockDefinition, RegistryObject<Block>> CONTROLLERS =
            new EnumMap<>(MultiblockDefinition.class);

    public static final RegistryObject<Block> MULTIBLOCK_BASE = register(
            "machine_casing",
            () -> new Block(partProperties())
    );
    public static final RegistryObject<Block> CORRUGATED_IRON = register(
            "corrugated_iron",
            () -> new Block(partProperties())
    );
    public static final RegistryObject<Block> COPPER_COIL = register(
            "copper_coil",
            () -> new Block(partProperties())
    );
    public static final RegistryObject<Block> MULTIBLOCK_COLUMN = register(
            "machine_support_column",
            () -> new RotatedPillarBlock(partProperties())
    );
    public static final RegistryObject<Block> STRIPED_MULTIBLOCK_PART = register(
            "striped_machine_casing",
            () -> new Block(partProperties())
    );
    public static final RegistryObject<Block> ELECTRIC_MULTIBLOCK_PART = register(
            "electrical_machine_casing",
            () -> new Block(partProperties())
    );
    /** Runtime-only invisible replacement for formed structure members. */
    public static final RegistryObject<Block> MULTIBLOCK_GAP = ModRegistries.BLOCKS.register(
            "multiblock_gap",
            () -> new MultiblockGapBlock(BlockBehaviour.Properties.of()
                    .mapColor(MapColor.NONE)
                    .strength(3.5F, 10.0F)
                    .sound(SoundType.METAL)
                    .dynamicShape()
                    .noOcclusion()
                    .noLootTable())
    );
    /** Pumpjack-owned drill column. It intentionally has no item, recipe, loot or creative entry. */
    public static final RegistryObject<Block> PUMPJACK_DRILL = ModRegistries.BLOCKS.register(
            "pumpjack_drill",
            () -> new Block(BlockBehaviour.Properties.of()
                    .mapColor(MapColor.METAL)
                    .strength(2.0F, 10.0F)
                    .sound(SoundType.METAL)
                    .noLootTable())
    );
    public static final RegistryObject<Block> OIL_DEPOSIT = ModRegistries.BLOCKS.register(
            "oil_deposit",
            () -> new OilDepositBlock(BlockBehaviour.Properties.of()
                    .mapColor(MapColor.COLOR_BLACK)
                    .strength(-1.0F, 3_600_000.0F)
                    .sound(SoundType.STONE)
                    .noLootTable())
    );
    public static final RegistryObject<Item> OIL_DEPOSIT_ITEM = ModRegistries.ITEMS.register(
            "oil_deposit",
            () -> new OilDepositBlockItem(OIL_DEPOSIT.get(), new Item.Properties())
    );

    static {
        BLOCK_ITEMS.add(OIL_DEPOSIT_ITEM);
        for (MultiblockDefinition definition : MultiblockDefinition.values()) {
            RegistryObject<Block> controller = registerController(
                    definition.id(),
                    () -> new AdvancedMultiblockBlock(definition, controllerProperties())
            );
            CONTROLLERS.put(definition, controller);
        }
    }

    private ModAdvancedBlocks() {
    }

    public static void bootstrap() {
    }

    public static RegistryObject<Block> controller(MultiblockDefinition definition) {
        RegistryObject<Block> block = CONTROLLERS.get(definition);
        if (block == null) {
            throw new IllegalArgumentException("No advanced multiblock controller registered for " + definition);
        }
        return block;
    }

    public static Map<MultiblockDefinition, RegistryObject<Block>> controllers() {
        return Collections.unmodifiableMap(CONTROLLERS);
    }

    public static List<RegistryObject<? extends Item>> blockItems() {
        return List.copyOf(BLOCK_ITEMS);
    }

    private static RegistryObject<Block> register(String id, Supplier<Block> factory) {
        RegistryObject<Block> block = ModRegistries.BLOCKS.register(id, factory);
        BLOCK_ITEMS.add(ModRegistries.ITEMS.register(id, () -> new BlockItem(block.get(), new Item.Properties())));
        return block;
    }

    private static RegistryObject<Block> registerController(String id, Supplier<Block> factory) {
        RegistryObject<Block> block = ModRegistries.BLOCKS.register(id, factory);
        BLOCK_ITEMS.add(ModRegistries.ITEMS.register(
                id,
                () -> new BlockItem(block.get(), new Item.Properties().stacksTo(1))
        ));
        return block;
    }

    private static BlockBehaviour.Properties partProperties() {
        return BlockBehaviour.Properties.of()
                .mapColor(MapColor.METAL)
                .requiresCorrectToolForDrops()
                .strength(3.5F, 10.0F)
                .sound(SoundType.METAL);
    }

    private static BlockBehaviour.Properties controllerProperties() {
        return BlockBehaviour.Properties.of()
                .mapColor(MapColor.METAL)
                .strength(4.0F, 12.0F)
                .sound(SoundType.METAL)
                .noOcclusion();
    }
}
