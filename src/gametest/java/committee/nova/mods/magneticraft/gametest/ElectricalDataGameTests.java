package committee.nova.mods.magneticraft.gametest;

import committee.nova.mods.magneticraft.Magneticraft;
import committee.nova.mods.magneticraft.system.network.electric.profile.ElectricalDataRegistry;
import committee.nova.mods.magneticraft.system.network.electric.profile.ElectricalDataSnapshot;
import committee.nova.mods.magneticraft.system.network.runtime.PhysicalNetworkManager;
import committee.nova.mods.magneticraft.system.network.runtime.PhysicalNetworkService;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;

/** Loaded-server proof for initial electrical data and reload coordination. */
@GameTestHolder(Magneticraft.MOD_ID)
@PrefixGameTestTemplate(false)
public final class ElectricalDataGameTests {
    private static final String TEMPLATE = "base_content";

    private ElectricalDataGameTests() {
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 10)
    public static void builtInSnapshotLoadsAtomicallyAndCoordinatesReloadWindow(GameTestHelper helper) {
        ElectricalDataSnapshot snapshot = ElectricalDataRegistry.INSTANCE.currentOrThrow();
        helper.assertTrue(snapshot.voltageTiers().size() == 3, "Built-in voltage tiers did not load atomically");
        helper.assertTrue(snapshot.transformerProfiles().size() == 2, "Built-in transformer profiles are incomplete");
        helper.assertTrue(snapshot.machineProfiles().size() == 24, "Built-in machine profiles are incomplete");

        PhysicalNetworkManager manager = PhysicalNetworkService.manager(helper.getLevel());
        manager.onElectricalProfilesReloaded(snapshot, 2);
        helper.assertTrue(manager.electricalSimulationPaused(), "Successful reload did not pause electricity");
        helper.assertTrue(manager.electricalDamageSuppressed(), "Successful reload did not start damage grace");

        manager.tick(helper.getLevel().getGameTime() + 1L);
        helper.assertFalse(manager.electricalSimulationPaused(), "Electricity remained paused beyond one tick");
        helper.assertTrue(manager.electricalDamageGraceTicks() == 1, "Reload grace did not count down once");
        helper.succeed();
    }
}
