package committee.nova.mods.magneticraft.init;

import committee.nova.mods.magneticraft.content.block.BaseBlockDefinition;
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

/**
 * Registers the stateless solid blocks owned by the base-content task.
 */
public final class ModBlocks {
    private static final Map<BaseBlockDefinition, RegistryObject<Block>> BLOCKS =
            new EnumMap<>(BaseBlockDefinition.class);
    private static final List<RegistryObject<? extends Item>> BLOCK_ITEMS = new ArrayList<>();

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
}
