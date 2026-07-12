package committee.nova.mods.magneticraft.system.network.core;

/**
 * Cumulative evidence that topology work stays local to topology changes.
 */
public record GraphMetrics(
        long topologyVersion,
        long componentRebuilds,
        long nodesVisitedDuringRebuilds,
        int maxRebuildQueueDepth
) {
}
