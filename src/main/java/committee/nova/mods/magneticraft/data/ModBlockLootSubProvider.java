package committee.nova.mods.magneticraft.data;

import committee.nova.mods.magneticraft.init.ModBlocks;
import net.minecraft.data.loot.BlockLootSubProvider;
import net.minecraft.world.flag.FeatureFlags;
import net.minecraft.world.level.block.Block;
import net.minecraftforge.registries.RegistryObject;

import java.util.Set;

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
    }

    @Override
    protected Iterable<Block> getKnownBlocks() {
        return ModBlocks.all().values().stream().map(RegistryObject::get)::iterator;
    }
}
