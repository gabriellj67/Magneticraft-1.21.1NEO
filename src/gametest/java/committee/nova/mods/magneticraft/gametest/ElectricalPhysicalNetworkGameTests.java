package committee.nova.mods.magneticraft.gametest;

import committee.nova.mods.magneticraft.Magneticraft;
import committee.nova.mods.magneticraft.system.network.electric.ElectricalNode;
import committee.nova.mods.magneticraft.system.network.electric.ElectricalNodeAccess;
import committee.nova.mods.magneticraft.system.network.runtime.NetworkDomain;
import committee.nova.mods.magneticraft.system.network.runtime.PhysicalNetworkManager;
import committee.nova.mods.magneticraft.system.network.runtime.PhysicalNetworkNode;
import committee.nova.mods.magneticraft.system.network.runtime.PhysicalNetworkService;
import committee.nova.mods.magneticraft.system.network.runtime.PhysicalNodeKey;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;

import java.util.EnumSet;
import java.util.Set;

/** Server-runtime contracts that require Forge's loaded level and GameTest lifecycle. */
@GameTestHolder(Magneticraft.MOD_ID)
@PrefixGameTestTemplate(false)
public final class ElectricalPhysicalNetworkGameTests {
    private static final String TEMPLATE = "base_content";
    private static final ResourceLocation LOW = Magneticraft.id("low_voltage");
    private static final ResourceLocation MEDIUM = Magneticraft.id("medium_voltage");

    private ElectricalPhysicalNetworkGameTests() {
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 20)
    public static void sameBlockTerminalsStayIsolatedUntilExplicitInternalEdge(GameTestHelper helper) {
        PhysicalNetworkManager manager = PhysicalNetworkService.manager(helper.getLevel());
        BlockPos position = helper.absolutePos(new BlockPos(0, 2, 0));
        TestTerminal input = terminal(position, Magneticraft.id("test_input"), LOW);
        TestTerminal output = terminal(position, Magneticraft.id("test_output"), LOW);
        manager.register(input);
        manager.register(output);
        try {
            helper.assertTrue(manager.keysAt(NetworkDomain.ELECTRICITY, position).size() == 2,
                    "Two terminals at one position overwrote each other");
            helper.assertTrue(manager.component(NetworkDomain.ELECTRICITY, input.nodeKey()).size() == 1,
                    "Input terminal was implicitly connected");
            helper.assertTrue(manager.component(NetworkDomain.ELECTRICITY, output.nodeKey()).size() == 1,
                    "Output terminal was implicitly connected");

            manager.setInternalConnection(input.nodeKey(), output.nodeKey(), true);
            helper.assertTrue(manager.component(NetworkDomain.ELECTRICITY, input.nodeKey()).size() == 2,
                    "Closed internal edge did not join same-tier terminals");
            manager.setInternalConnection(input.nodeKey(), output.nodeKey(), false);
            helper.assertTrue(manager.component(NetworkDomain.ELECTRICITY, input.nodeKey()).size() == 1,
                    "Opened internal edge did not isolate terminals again");
        } finally {
            manager.unregister(input);
            manager.unregister(output);
        }
        helper.succeed();
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 20)
    public static void differentTierAdjacentTerminalsDoNotJoin(GameTestHelper helper) {
        PhysicalNetworkManager manager = PhysicalNetworkService.manager(helper.getLevel());
        BlockPos lowPosition = helper.absolutePos(new BlockPos(0, 2, 0));
        TestTerminal low = terminal(lowPosition, PhysicalNodeKey.MAIN_TERMINAL, LOW);
        TestTerminal medium = terminal(lowPosition.east(), PhysicalNodeKey.MAIN_TERMINAL, MEDIUM);
        manager.register(low);
        manager.register(medium);
        try {
            helper.assertTrue(manager.neighborKeys(NetworkDomain.ELECTRICITY, low.nodeKey()).isEmpty(),
                    "Different voltage tiers formed an ordinary edge");
            helper.assertTrue(manager.component(NetworkDomain.ELECTRICITY, low.nodeKey()).size() == 1,
                    "Different voltage tiers merged components");
        } finally {
            manager.unregister(low);
            manager.unregister(medium);
        }
        helper.succeed();
    }

    private static TestTerminal terminal(
            BlockPos position,
            ResourceLocation terminalId,
            ResourceLocation tierId
    ) {
        return new TestTerminal(
                new PhysicalNodeKey(position, terminalId),
                tierId,
                new ElectricalNode(0.5D, 2_000.0D, 0.005D)
        );
    }

    private record TestTerminal(
            PhysicalNodeKey nodeKey,
            ResourceLocation electricalTierId,
            ElectricalNode electricalNode
    ) implements PhysicalNetworkNode, ElectricalNodeAccess {
        private static final Set<Direction> SIDES = Set.copyOf(EnumSet.allOf(Direction.class));

        @Override
        public NetworkDomain domain() {
            return NetworkDomain.ELECTRICITY;
        }

        @Override
        public BlockPos position() {
            return nodeKey.position();
        }

        @Override
        public Set<Direction> connectionSides() {
            return SIDES;
        }

        @Override
        public boolean electricalProfileBound() {
            return true;
        }

        @Override
        public void markElectricalStateChanged() {
        }
    }
}
