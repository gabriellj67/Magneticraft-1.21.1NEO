package committee.nova.mods.magneticraft.content.machine.framework;

import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Side-neutral handoff for model refreshes that must run after the current
 * client block-entity update packet has finished applying.
 */
public final class MachineModelRefreshQueue {
    private static final int REFRESH_DELAY_TICKS = 2;
    private static final Map<Request, Integer> PENDING = new LinkedHashMap<>();

    private MachineModelRefreshQueue() {
    }

    public static synchronized void enqueue(ResourceLocation dimension, BlockPos position) {
        PENDING.merge(new Request(dimension, position), REFRESH_DELAY_TICKS, Math::max);
    }

    /** Advances one client tick and returns requests whose initial section build has had time to finish. */
    public static synchronized List<Request> tickAndDrain() {
        List<Request> ready = new ArrayList<>();
        var iterator = PENDING.entrySet().iterator();
        while (iterator.hasNext()) {
            Map.Entry<Request, Integer> entry = iterator.next();
            int remaining = entry.getValue() - 1;
            if (remaining <= 0) {
                ready.add(entry.getKey());
                iterator.remove();
            } else {
                entry.setValue(remaining);
            }
        }
        return List.copyOf(ready);
    }

    public static synchronized void clear() {
        PENDING.clear();
    }

    public record Request(ResourceLocation dimension, BlockPos position) {
        public Request {
            dimension = Objects.requireNonNull(dimension, "dimension");
            position = Objects.requireNonNull(position, "position").immutable();
        }
    }
}
