package committee.nova.mods.magneticraft.content.nuclear.reactor;

import committee.nova.mods.magneticraft.api.nuclear.reactor.NuclearReactorColumnType;
import net.minecraft.world.level.block.Block;

import java.util.Objects;

/** One logical full-height column marker placed at the active-core base. */
public final class NuclearReactorColumnBlock extends Block {
    private final NuclearReactorColumnType columnType;

    public NuclearReactorColumnBlock(NuclearReactorColumnType columnType, Properties properties) {
        super(properties);
        this.columnType = Objects.requireNonNull(columnType);
    }

    public NuclearReactorColumnType columnType() {
        return columnType;
    }
}
