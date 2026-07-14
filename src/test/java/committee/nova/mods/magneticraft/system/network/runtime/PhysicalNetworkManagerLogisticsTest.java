package committee.nova.mods.magneticraft.system.network.runtime;

import committee.nova.mods.magneticraft.MinecraftTestBootstrap;
import committee.nova.mods.magneticraft.system.network.logistics.LogisticsNetworkNode;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.EnumSet;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PhysicalNetworkManagerLogisticsTest {
    @BeforeAll
    static void bootstrapMinecraft() {
        MinecraftTestBootstrap.ensureBootstrapped();
    }

    @Test
    void cacheHitStillRevalidatesDynamicDestinationCapacityAndVisitBudget() {
        PhysicalNetworkManager manager = new PhysicalNetworkManager(null);
        TestLogisticsNode start = node(0, false, 0);
        TestLogisticsNode middle = node(1, false, 0);
        TestLogisticsNode destination = node(2, true, 0);
        manager.register(start);
        manager.register(middle);
        manager.register(destination);

        ItemStack stack = new ItemStack(Items.IRON_INGOT, 8);
        assertEquals(
                Direction.EAST,
                manager.findLogisticsRoute(start.position(), null, stack, 0, 4_096).orElseThrow().direction()
        );

        destination.accepting = false;
        assertTrue(manager.findLogisticsRoute(start.position(), null, stack, 0, 4_096).isEmpty());

        destination.accepting = true;
        assertTrue(manager.findLogisticsRoute(start.position(), null, stack, 0, 1).isEmpty(),
                "A route cached with a larger search budget must not bypass a one-node budget");
    }

    @Test
    void serviceClampsEveryCallerTo4096VisitedNodes() {
        assertEquals(4_096, PhysicalNetworkManager.MAX_LOGISTICS_ROUTE_VISITS);
        PhysicalNetworkManager manager = new PhysicalNetworkManager(null);
        int destinationX = PhysicalNetworkManager.MAX_LOGISTICS_ROUTE_VISITS + 1;
        for (int x = 0; x <= destinationX; x++) {
            manager.register(node(x, x == destinationX, 0));
        }

        assertTrue(manager.findLogisticsRoute(
                new BlockPos(0, 0, 0),
                null,
                new ItemStack(Items.IRON_INGOT),
                0,
                Integer.MAX_VALUE
        ).isEmpty());
    }

    @Test
    void roundRobinOffsetSelectsAmongEqualRemoteFirstEdges() {
        PhysicalNetworkManager manager = new PhysicalNetworkManager(null);
        TestLogisticsNode start = node(0, false, 0);
        TestLogisticsNode east = new TestLogisticsNode(new BlockPos(1, 0, 0), false, 0);
        TestLogisticsNode eastDestination = new TestLogisticsNode(new BlockPos(2, 0, 0), true, 0);
        TestLogisticsNode west = new TestLogisticsNode(new BlockPos(-1, 0, 0), false, 0);
        TestLogisticsNode westDestination = new TestLogisticsNode(new BlockPos(-2, 0, 0), true, 0);
        manager.register(start);
        manager.register(east);
        manager.register(eastDestination);
        manager.register(west);
        manager.register(westDestination);

        ItemStack stack = new ItemStack(Items.IRON_INGOT);
        Direction first = manager.findLogisticsRoute(start.position(), null, stack, 0, 4_096)
                .orElseThrow().direction();
        Direction second = manager.findLogisticsRoute(start.position(), null, stack, 1, 4_096)
                .orElseThrow().direction();

        assertEquals(first.getOpposite(), second);
    }

    private static TestLogisticsNode node(int x, boolean accepting, int weight) {
        return new TestLogisticsNode(new BlockPos(x, 0, 0), accepting, weight);
    }

    private static final class TestLogisticsNode implements LogisticsNetworkNode {
        private static final Set<Direction> CONNECTIONS = EnumSet.allOf(Direction.class);

        private final BlockPos position;
        private final int weight;
        private boolean accepting;

        private TestLogisticsNode(BlockPos position, boolean accepting, int weight) {
            this.position = position;
            this.accepting = accepting;
            this.weight = weight;
        }

        @Override
        public NetworkDomain domain() {
            return NetworkDomain.LOGISTICS;
        }

        @Override
        public BlockPos position() {
            return position;
        }

        @Override
        public Set<Direction> connectionSides() {
            return CONNECTIONS;
        }

        @Override
        public int routingWeight() {
            return weight;
        }

        @Override
        public List<Direction> acceptingExternalOutputs(ItemStack stack) {
            return accepting ? List.of(Direction.UP) : List.of();
        }
    }
}
