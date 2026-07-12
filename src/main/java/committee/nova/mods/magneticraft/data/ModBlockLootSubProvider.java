package committee.nova.mods.magneticraft.data;

import committee.nova.mods.magneticraft.init.ModBlocks;
import committee.nova.mods.magneticraft.init.ModMachineBlocks;
import committee.nova.mods.magneticraft.init.ModNetworkBlocks;
import net.minecraft.data.loot.BlockLootSubProvider;
import net.minecraft.world.flag.FeatureFlags;
import net.minecraft.world.level.block.Block;
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
        ModBlocks.all().values().stream().map(RegistryObject::get).forEach(this::dropSelf);
        ModMachineBlocks.blockItems().forEach(item -> dropSelf(Block.byItem(item.get())));
        ModNetworkBlocks.blockItems().forEach(item -> dropSelf(Block.byItem(item.get())));
    }

    @Override
    protected Iterable<Block> getKnownBlocks() {
        return Stream.of(
                ModBlocks.all().values().stream().map(RegistryObject::get),
                ModMachineBlocks.blockItems().stream().map(item -> Block.byItem(item.get())),
                ModNetworkBlocks.blockItems().stream().map(item -> Block.byItem(item.get()))
        ).flatMap(stream -> stream)::iterator;
    }
}
