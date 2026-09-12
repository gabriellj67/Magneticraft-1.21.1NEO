package committee.nova.mods.magneticraft.content.network.pneumatic;

import net.minecraft.core.Direction;

/** A machine-side pneumatic port; deliberately separate from item capabilities. */
public interface PneumaticConnectionHost {
    boolean supportsPneumaticConnection(Direction side);
}
