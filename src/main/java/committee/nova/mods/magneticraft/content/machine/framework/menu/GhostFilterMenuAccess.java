package committee.nova.mods.magneticraft.content.machine.framework.menu;

import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

/**
 * Server-side menu boundary for generic ghost-filter actions.
 */
public interface GhostFilterMenuAccess {
    BlockPos machinePosition();

    boolean stillValid(Player player);

    int ghostFilterCount();

    void setGhostFilter(int slot, ItemStack sample);
}
