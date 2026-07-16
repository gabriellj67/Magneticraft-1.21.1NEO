package committee.nova.mods.magneticraft.init;

import committee.nova.mods.magneticraft.content.block.BaseBlockDefinition;
import committee.nova.mods.magneticraft.content.block.DecorativeBlockFamily;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.SlabBlock;
import net.minecraft.world.level.block.StairBlock;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;
import net.minecraftforge.registries.RegistryObject;

import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Registers the stateless solid blocks owned by the base-content task.
 */
public final class ModBlocks {
    private static final Map<BaseBlockDefinition, RegistryObject<Block>> BLOCKS =
            new EnumMap<>(BaseBlockDefinition.class);
    private static final List<RegistryObject<? extends Item>> BLOCK_ITEMS = new ArrayList<>();
    private static final Map<DecorativeBlockFamily, DecorativeFamilyBlocks> DECORATIVE_FAMILIES =
            new EnumMap<>(DecorativeBlockFamily.class);

    static {
        for (BaseBlockDefinition definition : BaseBlockDefinition.values()) {
            RegistryObject<Block> block = ModRegistries.BLOCKS.register(
                    definition.id(),
                    () -> new Block(properties(definition))
            );
            RegistryObject<Item> item = ModRegistries.ITEMS.register(
                    definition.id(),
                    () -> new BlockItem(block.get(), new Item.Properties())
            );
            BLOCKS.put(definition, block);
            BLOCK_ITEMS.add(item);
        }
        for (DecorativeBlockFamily family : DecorativeBlockFamily.values()) {
            RegistryObject<Block> base = family.registersBase()
                    ? registerDecorativeBase(family)
                    : get(Objects.requireNonNull(family.existingBase()));
            RegistryObject<StairBlock> stairs = ModRegistries.BLOCKS.register(
                    family.stairsId(),
                    () -> new StairBlock(base.get()::defaultBlockState, decorativeProperties(family))
            );
            RegistryObject<SlabBlock> slab = ModRegistries.BLOCKS.register(
                    family.slabId(),
                    () -> new SlabBlock(decorativeProperties(family))
            );
            BLOCK_ITEMS.add(ModRegistries.ITEMS.register(
                    family.stairsId(),
                    () -> new BlockItem(stairs.get(), new Item.Properties())
            ));
            BLOCK_ITEMS.add(ModRegistries.ITEMS.register(
                    family.slabId(),
                    () -> new BlockItem(slab.get(), new Item.Properties())
            ));
            DECORATIVE_FAMILIES.put(family, new DecorativeFamilyBlocks(base, stairs, slab));
        }
    }

    private ModBlocks() {
    }

    public static void bootstrap() {
    }

    public static RegistryObject<Block> get(BaseBlockDefinition definition) {
        RegistryObject<Block> block = BLOCKS.get(definition);
        if (block == null) {
            throw new IllegalArgumentException("No block registered for " + definition);
        }
        return block;
    }

    public static Map<BaseBlockDefinition, RegistryObject<Block>> all() {
        return Collections.unmodifiableMap(BLOCKS);
    }

    public static List<RegistryObject<? extends Item>> blockItems() {
        return List.copyOf(BLOCK_ITEMS);
    }

    public static DecorativeFamilyBlocks decorativeFamily(DecorativeBlockFamily family) {
        DecorativeFamilyBlocks blocks = DECORATIVE_FAMILIES.get(Objects.requireNonNull(family, "family"));
        if (blocks == null) {
            throw new IllegalArgumentException("No decorative family registered for " + family);
        }
        return blocks;
    }

    public static Map<DecorativeBlockFamily, DecorativeFamilyBlocks> decorativeFamilies() {
        return Collections.unmodifiableMap(DECORATIVE_FAMILIES);
    }

    private static BlockBehaviour.Properties properties(BaseBlockDefinition definition) {
        MapColor mapColor = switch (definition.category()) {
            case ORE, DECORATION -> MapColor.STONE;
            case STORAGE -> MapColor.METAL;
        };
        return BlockBehaviour.Properties.of()
                .mapColor(mapColor)
                .requiresCorrectToolForDrops()
                .strength(1.5F, 10.0F)
                .sound(SoundType.STONE);
    }

    private static RegistryObject<Block> registerDecorativeBase(DecorativeBlockFamily family) {
        RegistryObject<Block> block = ModRegistries.BLOCKS.register(
                family.baseId(),
                () -> new Block(decorativeProperties(family))
        );
        BLOCK_ITEMS.add(ModRegistries.ITEMS.register(
                family.baseId(),
                () -> new BlockItem(block.get(), new Item.Properties())
        ));
        return block;
    }

    private static BlockBehaviour.Properties decorativeProperties(DecorativeBlockFamily family) {
        return BlockBehaviour.Properties.of()
                .mapColor(family == DecorativeBlockFamily.TERRACOTTA_ROOF_TILE
                        ? MapColor.TERRACOTTA_RED
                        : MapColor.STONE)
                .requiresCorrectToolForDrops()
                .strength(1.5F, 10.0F)
                .sound(SoundType.STONE);
    }

    public record DecorativeFamilyBlocks(
            RegistryObject<Block> base,
            RegistryObject<StairBlock> stairs,
            RegistryObject<SlabBlock> slab
    ) {
    }
}
