package committee.nova.mods.magneticraft.system.network.logistics;

import net.minecraft.core.Direction;

/**
 * The next local action; routes are deliberately recomputed at intersections.
 */
public record LogisticsRouteDecision(Direction direction, boolean external, int visitedNodes, boolean truncated) {
}
