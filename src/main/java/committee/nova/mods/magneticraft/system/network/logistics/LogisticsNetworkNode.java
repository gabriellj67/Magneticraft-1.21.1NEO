package committee.nova.mods.magneticraft.system.network.logistics;

import committee.nova.mods.magneticraft.system.network.runtime.PhysicalNetworkNode;
import net.minecraft.core.Direction;
import net.minecraft.world.item.ItemStack;

import java.util.List;

/**
 * Runtime routing surface implemented by loaded pneumatic tubes.
 */
public interface LogisticsNetworkNode extends PhysicalNetworkNode {
    int routingWeight();

    List<Direction> acceptingExternalOutputs(ItemStack stack);
}
