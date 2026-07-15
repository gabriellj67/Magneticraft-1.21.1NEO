package committee.nova.mods.magneticraft.content.network.electric;

import committee.nova.mods.magneticraft.content.item.TieredElectricalDrops;
import committee.nova.mods.magneticraft.content.network.block.ConduitBlock;
import committee.nova.mods.magneticraft.content.network.module.ElectricalNetworkModule;
import committee.nova.mods.magneticraft.content.network.module.TieredElectricalHost;
import committee.nova.mods.magneticraft.init.ModNetworkBlocks;
import committee.nova.mods.magneticraft.system.network.runtime.NetworkDomain;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

public final class ElectricCableBlock extends ConduitBlock {
    public ElectricCableBlock(Properties properties) {
        super(properties);
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos position, BlockState state) {
        return new ElectricCableBlockEntity(position, state);
    }

    @Override
    protected NetworkDomain connectionDomain() {
        return NetworkDomain.ELECTRICITY;
    }

    @Override
    protected boolean connectsVisuallyTo(BlockState neighbor) {
        return neighbor.is(ModNetworkBlocks.ELECTRIC_CABLE.get());
    }

    @Override
    protected boolean connectsVisuallyTo(
            LevelAccessor level,
            BlockPos position,
            Direction direction,
            BlockState neighbor
    ) {
        return connectsVisuallyTo(neighbor)
                && tierCompatible(level.getBlockEntity(position), level.getBlockEntity(position.relative(direction)));
    }

    @Override
    protected boolean connectsToMachine(LevelAccessor level, BlockPos position, Direction side) {
        BlockEntity self = level.getBlockEntity(position.relative(side));
        BlockEntity target = level.getBlockEntity(position);
        return (!(target instanceof TieredElectricalHost) || tierCompatible(self, target))
                && super.connectsToMachine(level, position, side);
    }

    private static boolean tierCompatible(BlockEntity first, BlockEntity second) {
        if (!(first instanceof TieredElectricalHost firstHost)
                || !(second instanceof TieredElectricalHost secondHost)) {
            return false;
        }
        ElectricalNetworkModule firstModule = firstHost.tieredElectricalModule();
        ElectricalNetworkModule secondModule = secondHost.tieredElectricalModule();
        return firstModule != null
                && secondModule != null
                && firstModule.electricalProfileBound()
                && secondModule.electricalProfileBound()
                && firstModule.tierId().equals(secondModule.tierId());
    }

    @Override
    public List<ItemStack> getDrops(BlockState state, LootParams.Builder builder) {
        return TieredElectricalDrops.preserveTier(
                new ArrayList<>(super.getDrops(state, builder)),
                asItem(),
                builder.getOptionalParameter(LootContextParams.BLOCK_ENTITY)
        );
    }
}
