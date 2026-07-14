package committee.nova.mods.magneticraft.content.network.module;

import committee.nova.mods.magneticraft.Magneticraft;
import committee.nova.mods.magneticraft.content.machine.framework.MachineModuleContainer;
import committee.nova.mods.magneticraft.content.machine.framework.MachineModuleHost;
import committee.nova.mods.magneticraft.system.network.heat.HeatNode;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.Level;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class HeatNetworkModuleTest {
    @Test
    void missingOrIncompatiblePersistenceResetsToAmbientTemperature() {
        HeatNode node = new HeatNode(10.0D, 0.5D);
        HeatNetworkModule heat = new HeatNetworkModule(
                Magneticraft.id("test_heat"),
                new TestHost(),
                node,
                1_000.0D,
                ignored -> true
        );
        MachineModuleContainer modules = new MachineModuleContainer();
        modules.add(heat);

        node.setTemperature(500.0D);
        modules.load(new CompoundTag());
        assertEquals(HeatNode.AMBIENT_TEMPERATURE_KELVIN, node.temperatureKelvin(), 0.000_001D);

        CompoundTag future = modules.save();
        future.getCompound(heat.id().toString()).putInt("schema_version", 2);
        node.setTemperature(500.0D);
        modules.load(future);
        assertEquals(HeatNode.AMBIENT_TEMPERATURE_KELVIN, node.temperatureKelvin(), 0.000_001D);
    }

    @Test
    void clientPayloadCarriesTheCurrentTemperature() {
        HeatNode sourceNode = new HeatNode(10.0D, 0.5D);
        HeatNetworkModule source = new HeatNetworkModule(
                Magneticraft.id("source_heat"),
                new TestHost(),
                sourceNode,
                1_000.0D,
                ignored -> true
        );
        HeatNode targetNode = new HeatNode(10.0D, 0.5D);
        HeatNetworkModule target = new HeatNetworkModule(
                Magneticraft.id("target_heat"),
                new TestHost(),
                targetNode,
                1_000.0D,
                ignored -> true
        );

        sourceNode.setTemperature(723.15D);
        CompoundTag tag = new CompoundTag();
        source.saveClientData(tag);
        target.loadClientData(tag);

        assertEquals(723.15D, targetNode.temperatureKelvin(), 0.000_001D);
    }

    private static final class TestHost implements MachineModuleHost {
        @Override
        public void markChanged() {
        }

        @Override
        public void markChangedAndSync() {
        }

        @Override
        public Level level() {
            return null;
        }

        @Override
        public BlockPos position() {
            return BlockPos.ZERO;
        }
    }
}
