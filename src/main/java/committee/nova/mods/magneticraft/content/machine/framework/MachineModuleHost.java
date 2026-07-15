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

    /**
     * Requests one coalesced update after all modules have ticked. Modules with
     * parallel state (for example several tanks) should prefer this over sending
     * one complete block-entity packet each.
     */
    default void requestClientSync() {
        markChangedAndSync();
    }

    /**
     * Requests a client-side rebuild of the host's static block model after
     * synchronized module data changes a model input such as a tint tier.
     */
    default void requestModelRefresh() {
    }

    @Nullable Level level();

    BlockPos position();
}
