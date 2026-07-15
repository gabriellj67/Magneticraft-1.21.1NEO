package committee.nova.mods.magneticraft.system.network.runtime;

import committee.nova.mods.magneticraft.system.network.electric.profile.ElectricalDataSnapshot;
import committee.nova.mods.magneticraft.system.network.electric.profile.ElectricalProfileBinding;
import committee.nova.mods.magneticraft.system.network.electric.profile.ElectricalRole;
import committee.nova.mods.magneticraft.system.network.electric.profile.MachineElectricalProfile;
import committee.nova.mods.magneticraft.system.network.electric.profile.TransformerProfile;
import committee.nova.mods.magneticraft.system.network.electric.profile.VoltageTier;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import org.junit.jupiter.api.Test;

import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ElectricalReloadWindowTest {
    @Test
    void reloadRebindsLoadedElectricalNodesPausesOneTickAndCountsDownGrace() {
        PhysicalNetworkManager manager = new PhysicalNetworkManager(null);
        BindingNode electrical = new BindingNode(NetworkDomain.ELECTRICITY, BlockPos.ZERO);
        BindingNode thermal = new BindingNode(NetworkDomain.HEAT, BlockPos.ZERO.offset(2, 0, 0));
        manager.register(electrical);
        manager.register(thermal);
        ElectricalDataSnapshot snapshot = snapshot();

        manager.onElectricalProfilesReloaded(snapshot, 2);

        assertSame(snapshot, electrical.snapshot);
        assertEquals(1, electrical.rebinds);
        assertEquals(0, thermal.rebinds);
        assertTrue(manager.electricalSimulationPaused());
        assertTrue(manager.electricalDamageSuppressed());

        manager.tick(1L);
        assertEquals(0, electrical.beforeTicks);
        assertEquals(1, thermal.beforeTicks);
        assertFalse(manager.electricalSimulationPaused());
        assertEquals(1, manager.electricalDamageGraceTicks());

        manager.tick(2L);
        assertEquals(1, electrical.beforeTicks);
        assertFalse(manager.electricalDamageSuppressed());
    }

    private static ElectricalDataSnapshot snapshot() {
        ResourceLocation low = id("low_voltage");
        ResourceLocation medium = id("medium_voltage");
        VoltageTier lowTier = tier(low, 60.0, 120.0, 125.0);
        VoltageTier mediumTier = tier(medium, 240.0, 480.0, 500.0);
        TransformerProfile transformer = new TransformerProfile(id("lv_to_mv"), low, medium, 800.0, 0.96);
        MachineElectricalProfile machine = new MachineElectricalProfile(
                id("load"), low, ElectricalRole.CONSUMER, 1_000.0, 40.0, 8.0
        );
        return new ElectricalDataSnapshot(
                1L,
                Map.of(low, lowTier, medium, mediumTier),
                Map.of(transformer.id(), transformer),
                Map.of(machine.id(), machine)
        );
    }

    private static VoltageTier tier(ResourceLocation id, double minimum, double nominal, double maximum) {
        return new VoltageTier(
                id,
                "voltage_tier." + id.getNamespace() + "." + id.getPath(),
                minimum,
                nominal,
                maximum,
                maximum,
                1.0,
                0.25,
                0.005,
                8.0,
                16.0,
                8.0,
                16.0,
                400.0,
                0.0025,
                800.0,
                0.00125,
                400.0,
                8,
                16,
                1_000_000L,
                640.0,
                0xD98245
        );
    }

    private static ResourceLocation id(String path) {
        return ResourceLocation.fromNamespaceAndPath("magneticraft", path);
    }

    private static final class BindingNode implements PhysicalNetworkNode, ElectricalProfileBinding {
        private final NetworkDomain domain;
        private final BlockPos position;
        private ElectricalDataSnapshot snapshot;
        private int rebinds;
        private int beforeTicks;

        private BindingNode(NetworkDomain domain, BlockPos position) {
            this.domain = domain;
            this.position = position;
        }

        @Override
        public NetworkDomain domain() {
            return domain;
        }

        @Override
        public BlockPos position() {
            return position;
        }

        @Override
        public Set<Direction> connectionSides() {
            return Set.of();
        }

        @Override
        public void beforeNetworkTick(PhysicalNetworkManager manager) {
            beforeTicks++;
        }

        @Override
        public void rebindElectricalProfile(ElectricalDataSnapshot snapshot) {
            this.snapshot = snapshot;
            rebinds++;
        }
    }
}
