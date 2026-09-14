package committee.nova.mods.magneticraft.content.nuclear.structure;

import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;

/** Solid structure member whose baked geometry is hidden after formation. */
public class NuclearStructurePartBlock extends Block {
    public NuclearStructurePartBlock(Properties properties) {
        super(properties);
        registerDefaultState(stateDefinition.any().setValue(NuclearStructureState.FORMED, false));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(NuclearStructureState.FORMED);
    }
}
