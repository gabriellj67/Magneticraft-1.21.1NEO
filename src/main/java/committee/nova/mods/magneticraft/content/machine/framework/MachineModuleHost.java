package committee.nova.mods.magneticraft.content.machine.framework;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;

/**
 * Narrow host contract exposed to machine modules.
 */
public interface MachineModuleHost {
    void markChanged();

    void markChangedAndSync();

    @Nullable Level level();

    BlockPos position();
}
