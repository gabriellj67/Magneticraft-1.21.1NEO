package committee.nova.mods.magneticraft.content.network.module;

import committee.nova.mods.magneticraft.content.machine.framework.MachineModuleHost;
import committee.nova.mods.magneticraft.content.network.electric.ElectricalControlKind;
import committee.nova.mods.magneticraft.system.network.electric.ElectricalNodeKind;
import committee.nova.mods.magneticraft.system.network.electric.profile.VoltageTierIds;
import committee.nova.mods.magneticraft.system.network.runtime.RedstoneControlMode;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ElectricalControlModulePersistenceTest {
    @Test
    void switchDefaultsToNoSignalAndCyclesInSpecifiedOrder() {
        ElectricalControlModule module = fixture(ElectricalControlKind.SWITCH);

        assertEquals(RedstoneControlMode.REQUIRES_NO_SIGNAL, module.redstoneMode());
        assertTrue(module.enabled());
        module.cycleRedstoneMode();
        assertEquals(RedstoneControlMode.REQUIRES_SIGNAL, module.redstoneMode());
        assertFalse(module.enabled());
        module.cycleRedstoneMode();
        assertEquals(RedstoneControlMode.IGNORED, module.redstoneMode());
        assertTrue(module.enabled());
        module.cycleRedstoneMode();
        assertEquals(RedstoneControlMode.REQUIRES_NO_SIGNAL, module.redstoneMode());
    }

    @Test
    void resistorBandsRoundTripAndUseOneOhmMinimum() {
        ElectricalControlModule source = fixture(ElectricalControlKind.RESISTOR);
        assertEquals(1.0D, source.resistanceOhms());
        source.cycleRing(0);
        source.cycleRing(1);
        source.cycleRing(2);
        assertEquals(120.0D, source.resistanceOhms());

        CompoundTag saved = new CompoundTag();
        source.save(saved);
        ElectricalControlModule restored = fixture(ElectricalControlKind.RESISTOR);
        restored.load(saved);

        assertEquals(1, restored.firstRing());
        assertEquals(2, restored.secondRing());
        assertEquals(1, restored.multiplierRing());
        assertEquals(120.0D, restored.resistanceOhms());
    }

    @Test
    void invalidSchemaRestoresSafeDefaults() {
        ElectricalControlModule module = fixture(ElectricalControlKind.RESISTOR);
        module.cycleRing(0);
        CompoundTag corrupt = new CompoundTag();
        corrupt.putInt("schema_version", 99);

        module.load(corrupt);

        assertEquals(ElectricalControlModule.DEFAULT_FIRST_RING, module.firstRing());
        assertEquals(ElectricalControlModule.DEFAULT_SECOND_RING, module.secondRing());
        assertEquals(ElectricalControlModule.DEFAULT_MULTIPLIER_RING, module.multiplierRing());
    }

    private static ElectricalControlModule fixture(ElectricalControlKind kind) {
        TestHost host = new TestHost();
        ElectricalNetworkModule back = terminal(host, "back");
        ElectricalNetworkModule front = terminal(host, "front");
        return new ElectricalControlModule(id("control"), host, back, front, kind);
    }

    private static ElectricalNetworkModule terminal(TestHost host, String terminal) {
        return new ElectricalNetworkModule(
                id("electricity_" + terminal),
                host,
                VoltageTierIds.LOW,
                id(terminal),
                ElectricalNodeKind.MACHINE,
                side -> true
        );
    }

    private static ResourceLocation id(String path) {
        return ResourceLocation.fromNamespaceAndPath("magneticraft", path);
    }

    private static final class TestHost implements MachineModuleHost {
        @Override
        public void markChanged() {
        }

        @Override
        public void markChangedAndSync() {
        }

        @Nullable
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
