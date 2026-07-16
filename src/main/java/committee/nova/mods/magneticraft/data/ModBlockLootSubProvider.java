package committee.nova.mods.magneticraft.data;

import committee.nova.mods.magneticraft.init.ModAdvancedBlocks;
import committee.nova.mods.magneticraft.init.ModBlocks;
import committee.nova.mods.magneticraft.init.ModComputerContent;
import committee.nova.mods.magneticraft.init.ModMachineBlocks;
import committee.nova.mods.magneticraft.init.ModNetworkBlocks;
import committee.nova.mods.magneticraft.content.network.electric.ElectricPoleBlock;
import committee.nova.mods.magneticraft.content.network.electric.PoleSegment;
import committee.nova.mods.magneticraft.content.network.electric.TeslaTowerBlock;
import committee.nova.mods.magneticraft.content.network.electric.TeslaTowerPart;
import net.minecraft.data.loot.BlockLootSubProvider;
import net.minecraft.advancements.critereon.StatePropertiesPredicate;
import net.minecraft.world.flag.FeatureFlags;
import net.minecraft.world.level.ItemLike;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.properties.Property;
import net.minecraft.world.level.storage.loot.LootPool;
import net.minecraft.world.level.storage.loot.LootTable;
import net.minecraft.world.level.storage.loot.entries.LootItem;
import net.minecraft.world.level.storage.loot.predicates.LootItemBlockStatePropertyCondition;
import net.minecraft.world.level.storage.loot.providers.number.ConstantValue;
import net.minecraftforge.registries.RegistryObject;

import java.util.Set;
import java.util.stream.Stream;

/**
 * Preserves the legacy default of every stateless solid block dropping itself.
 */
final class ModBlockLootSubProvider extends BlockLootSubProvider {
    ModBlockLootSubProvider() {
        super(Set.of(), FeatureFlags.REGISTRY.allFlags());
    }

    @Override
    protected void generate() {
        Set<Block> decorativeSlabs = ModBlocks.decorativeFamilies().values().stream()
                .map(family -> (Block) family.slab().get())
                .collect(java.util.stream.Collectors.toUnmodifiableSet());
        ModBlocks.blockItems().stream()
                .map(RegistryObject::get)
                .map(Block::byItem)
                .filter(block -> !decorativeSlabs.contains(block))
                .forEach(this::dropSelf);
        decorativeSlabs.forEach(slab -> add(slab, createSlabItemTable(slab)));
        ModMachineBlocks.blockItems().forEach(item -> dropSelf(Block.byItem(item.get())));
        Set<Block> structureBlocks = Set.of(
                ModNetworkBlocks.ELECTRIC_POLE.get(),
                ModNetworkBlocks.ELECTRIC_POLE_TRANSFORMER.get(),
                ModNetworkBlocks.TESLA_TOWER.get()
        );
        ModNetworkBlocks.blockItems().stream()
                .map(item -> Block.byItem(item.get()))
                .filter(block -> !structureBlocks.contains(block))
                .forEach(this::dropSelf);
        dropWhenProperty(
                ModNetworkBlocks.ELECTRIC_POLE.get(),
                ElectricPoleBlock.SEGMENT,
                PoleSegment.BASE,
                ModNetworkBlocks.ELECTRIC_POLE.get()
        );
        dropWhenProperty(
                ModNetworkBlocks.ELECTRIC_POLE_TRANSFORMER.get(),
                ElectricPoleBlock.SEGMENT,
                PoleSegment.BASE,
                ModNetworkBlocks.ELECTRIC_POLE_TRANSFORMER.get(),
                ModNetworkBlocks.ELECTRIC_POLE.get()
        );
        dropWhenProperty(
                ModNetworkBlocks.TESLA_TOWER.get(),
                TeslaTowerBlock.PART,
                TeslaTowerPart.BOTTOM,
                ModNetworkBlocks.TESLA_TOWER.get()
        );
        ModAdvancedBlocks.blockItems().stream()
                .map(item -> Block.byItem(item.get()))
                .filter(block -> block != ModAdvancedBlocks.OIL_DEPOSIT.get())
                .forEach(this::dropSelf);
        dropSelf(ModComputerContent.COMPUTER.get());
        dropSelf(ModComputerContent.MINING_ROBOT.get());
    }

    @Override
    protected Iterable<Block> getKnownBlocks() {
        return Stream.of(
                ModBlocks.blockItems().stream().map(RegistryObject::get).map(Block::byItem),
                ModMachineBlocks.blockItems().stream().map(item -> Block.byItem(item.get())),
                ModNetworkBlocks.blockItems().stream().map(item -> Block.byItem(item.get())),
                ModAdvancedBlocks.blockItems().stream()
                        .map(item -> Block.byItem(item.get()))
                        .filter(block -> block != ModAdvancedBlocks.OIL_DEPOSIT.get()),
                Stream.of(ModComputerContent.COMPUTER.get(), ModComputerContent.MINING_ROBOT.get())
        ).flatMap(stream -> stream)::iterator;
    }

    @SafeVarargs
    private final <T extends Comparable<T> & net.minecraft.util.StringRepresentable> void dropWhenProperty(
            Block block,
            Property<T> property,
            T value,
            ItemLike... drops
    ) {
        var condition = LootItemBlockStatePropertyCondition.hasBlockStateProperties(block)
                .setProperties(StatePropertiesPredicate.Builder.properties().hasProperty(property, value));
        LootTable.Builder table = LootTable.lootTable();
        for (ItemLike drop : drops) {
            table.withPool(applyExplosionCondition(
                    drop,
                    LootPool.lootPool()
                            .setRolls(ConstantValue.exactly(1.0F))
                            .when(condition)
                            .add(LootItem.lootTableItem(drop))
            ));
        }
        add(block, table);
    }
}
