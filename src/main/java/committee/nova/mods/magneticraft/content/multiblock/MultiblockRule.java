package committee.nova.mods.magneticraft.content.multiblock;

import committee.nova.mods.magneticraft.content.machine.singleblock.SingleBlockMachineDefinition;
import committee.nova.mods.magneticraft.init.ModAdvancedBlocks;
import committee.nova.mods.magneticraft.init.ModMachineBlocks;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.RotatedPillarBlock;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Closed first-release structure vocabulary derived from the Nova 1.12 schemes.
 */
public enum MultiblockRule {
    IGNORE('.'),
    AIR('A'),
    CONTROLLER('M'),
    BASE('#'),
    GRATE('G'),
    CORRUGATED_IRON('R'),
    COPPER_COIL('C'),
    COLUMN_X('X'),
    COLUMN_Y('Y'),
    COLUMN_Z('Z'),
    BRICKS('b'),
    SMALL_TANK('T'),
    STRIPED('S'),
    ELECTRIC('E');

    private final char symbol;

    MultiblockRule(char symbol) {
        this.symbol = symbol;
    }

    public char symbol() {
        return symbol;
    }

    public boolean matches(BlockState state, Block controller, Direction facing) {
        return switch (this) {
            case IGNORE -> true;
            case AIR -> state.isAir();
            case CONTROLLER -> state.is(controller);
            case BASE -> state.is(ModAdvancedBlocks.MULTIBLOCK_BASE.get());
            case GRATE -> state.is(ModMachineBlocks.GRATE.get());
            case CORRUGATED_IRON -> state.is(ModAdvancedBlocks.CORRUGATED_IRON.get());
            case COPPER_COIL -> state.is(ModAdvancedBlocks.COPPER_COIL.get());
            case COLUMN_X -> columnMatches(state, rotatedAxis(Direction.Axis.X, facing));
            case COLUMN_Y -> columnMatches(state, Direction.Axis.Y);
            case COLUMN_Z -> columnMatches(state, rotatedAxis(Direction.Axis.Z, facing));
            case BRICKS -> state.is(Blocks.BRICKS);
            case SMALL_TANK -> state.is(ModMachineBlocks.machine(SingleBlockMachineDefinition.SMALL_TANK).get());
            case STRIPED -> state.is(ModAdvancedBlocks.STRIPED_MULTIBLOCK_PART.get());
            case ELECTRIC -> state.is(ModAdvancedBlocks.ELECTRIC_MULTIBLOCK_PART.get());
        };
    }

    public static MultiblockRule bySymbol(char symbol) {
        for (MultiblockRule rule : values()) {
            if (rule.symbol == symbol) {
                return rule;
            }
        }
        throw new IllegalArgumentException("Unknown multiblock rule symbol: " + symbol);
    }

    private static boolean columnMatches(BlockState state, Direction.Axis axis) {
        return state.is(ModAdvancedBlocks.MULTIBLOCK_COLUMN.get())
                && state.getValue(RotatedPillarBlock.AXIS) == axis;
    }

    private static Direction.Axis rotatedAxis(Direction.Axis localAxis, Direction facing) {
        if (facing.getAxis().isVertical()) {
            throw new IllegalArgumentException("Multiblock facing must be horizontal: " + facing);
        }
        if (localAxis == Direction.Axis.Y || facing.getAxis() == Direction.Axis.Z) {
            return localAxis;
        }
        return localAxis == Direction.Axis.X ? Direction.Axis.Z : Direction.Axis.X;
    }
}
