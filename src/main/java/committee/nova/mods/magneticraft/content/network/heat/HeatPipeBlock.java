package committee.nova.mods.magneticraft.content.network.heat;

import committee.nova.mods.magneticraft.content.network.block.ConduitBlock;
import committee.nova.mods.magneticraft.init.ModNetworkBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

public final class HeatPipeBlock extends ConduitBlock {
    private final boolean insulated;

    public HeatPipeBlock(Properties properties, boolean insulated) {
        super(properties);
        this.insulated = insulated;
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos position, BlockState state) {
        return new HeatPipeBlockEntity(position, state);
    }

    @Override
    public void entityInside(BlockState state, Level level, BlockPos position, Entity entity) {
        if (!insulated
                && !level.isClientSide
                && entity instanceof LivingEntity living
                && level.getBlockEntity(position) instanceof HeatPipeBlockEntity pipe) {
            double celsius = pipe.heat().node().temperatureKelvin() - 273.15D;
            if (celsius >= 80.0D) {
                float damage = (float) Math.min(10.0D, 0.5D + (celsius - 80.0D) / 250.0D);
                living.hurt(level.damageSources().hotFloor(), damage);
            }
        }
        super.entityInside(state, level, position, entity);
    }

    @Override
    protected boolean connectsVisuallyTo(BlockState neighbor) {
        return neighbor.is(ModNetworkBlocks.HEAT_PIPE.get())
                || neighbor.is(ModNetworkBlocks.INSULATED_HEAT_PIPE.get())
                || neighbor.is(ModNetworkBlocks.HEAT_SINK.get());
    }
}
