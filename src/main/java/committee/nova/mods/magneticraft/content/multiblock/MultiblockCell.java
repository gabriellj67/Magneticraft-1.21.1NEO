package committee.nova.mods.magneticraft.content.multiblock;

import java.util.Objects;

/**
 * One local cell and its immutable matching rule.
 */
public record MultiblockCell(StructureOffset offset, MultiblockRule rule) {
    public MultiblockCell {
        Objects.requireNonNull(offset);
        Objects.requireNonNull(rule);
    }
}
