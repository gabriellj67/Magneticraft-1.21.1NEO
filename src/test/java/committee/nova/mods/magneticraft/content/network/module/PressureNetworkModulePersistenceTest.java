package committee.nova.mods.magneticraft.content.network.module;

import committee.nova.mods.magneticraft.content.machine.framework.MachineModuleHost;
import committee.nova.mods.magneticraft.system.network.pressure.PressureNode;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.Level;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PressureNetworkModulePersistenceTest {
    @Test
    void migratesLegacyUntypedGasToSteamAndWritesSchemaTwoPayload() {
        PressureNetworkModule module = module();
        CompoundTag legacy = new CompoundTag();
        legacy.putDouble("gas_kpa_liters", 125.5D);

        module.load(legacy);

        assertTrue(module.canLoadPersistenceSchema(1));
        assertEquals(2, module.persistenceSchemaVersion());
        assertEquals(ResourceLocation.fromNamespaceAndPath("magneticraft", "steam"),
                module.node().gasId().orElseThrow());
        assertEquals(125.5D, module.node().gasKpaLiters(), 1.0E-9D);

        CompoundTag saved = new CompoundTag();
        module.save(saved);
        assertEquals("magneticraft:steam", saved.getString("gas_id"));
        assertEquals(125.5D, saved.getDouble("gas_kpa_liters"), 1.0E-9D);
    }

    @Test
    void restoresTypedGasWithoutMaintainingLegacyParallelState() {
        PressureNetworkModule module = module();
        CompoundTag typed = new CompoundTag();
        typed.putString("gas_id", "magneticraft:natural_gas");
        typed.putDouble("gas_kpa_liters", 64.0D);

        module.load(typed);

        assertEquals(ResourceLocation.fromNamespaceAndPath("magneticraft", "natural_gas"),
                module.node().gasId().orElseThrow());
        assertEquals(64.0D, module.node().gasKpaLiters(), 1.0E-9D);
    }

    @Test
    void rejectsCorruptSchemaTwoGasInsteadOfReinterpretingItAsLegacySteam() {
        PressureNetworkModule module = module();
        CompoundTag corrupt = new CompoundTag();
        corrupt.putInt("schema_version", 2);
        corrupt.putString("gas_id", "not a valid id");
        corrupt.putDouble("gas_kpa_liters", 64.0D);

        module.load(corrupt);

        assertTrue(module.node().gasId().isEmpty());
        assertEquals(0.0D, module.node().gasKpaLiters(), 1.0E-9D);
    }

    @Test
    void diagnosticsOnlyExposeStructurallySupportedFaces() {
        PressureNetworkModule module = new PressureNetworkModule(
                ResourceLocation.fromNamespaceAndPath("magneticraft", "pressure"),
                new TestHost(),
                new PressureNode(2.0D, 20_000.0D),
                4.0D,
                400.0D,
                0.0D,
                side -> side == Direction.UP
        );

        assertTrue(module.pressureReading(Direction.UP).isPresent());
        assertTrue(module.pressureReading(Direction.DOWN).isEmpty());
    }

    private static PressureNetworkModule module() {
        return new PressureNetworkModule(
                ResourceLocation.fromNamespaceAndPath("magneticraft", "pressure"),
                new TestHost(),
                new PressureNode(2.0D, 20_000.0D),
                4.0D,
                400.0D,
                0.0D,
                side -> true
        );
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
