package committee.nova.mods.magneticraft.content.machine;

import committee.nova.mods.magneticraft.MinecraftTestBootstrap;
import committee.nova.mods.magneticraft.content.machine.singleblock.SingleBlockMachineDefinition;
import committee.nova.mods.magneticraft.content.machine.singleblock.SingleBlockMachineMath;
import committee.nova.mods.magneticraft.content.machine.singleblock.recipe.FluidFuelRecipe;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.material.Fluids;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeAll;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SingleBlockRecipeTest {
    @BeforeAll
    static void bootstrapMinecraft() {
        MinecraftTestBootstrap.ensureBootstrapped();
    }

    @Test
    void singleBlockInventoryHasStableUniqueIds() {
        Set<String> ids = new HashSet<>();
        Arrays.stream(SingleBlockMachineDefinition.values()).forEach(definition -> ids.add(definition.id()));
        assertEquals(21, ids.size());
        assertTrue(ids.containsAll(Set.of("sluice_box", "gasification_unit", "thermopile", "electric_engine")));
    }

    @Test
    void thermalSpeedPreservesLegacyOneKelvinClampAndQuantization() {
        assertEquals(0, SingleBlockMachineMath.thermalSpeed(332.0D, 333.0D, 40));
        assertEquals(10, SingleBlockMachineMath.thermalSpeed(333.25D, 333.0D, 40));
        assertEquals(40, SingleBlockMachineMath.thermalSpeed(334.0D, 333.0D, 40));
        assertEquals(40, SingleBlockMachineMath.thermalSpeed(600.0D, 333.0D, 40));
    }

    @Test
    void fuelAndThermopileMathRemainFiniteAtAmbientTemperature() {
        assertEquals(800_000.0D, SingleBlockMachineMath.fluidFuelEnergy(10_000, 80.0D));
        FluidFuelRecipe fuel = new FluidFuelRecipe(
                ResourceLocation.fromNamespaceAndPath("magneticraft", "test_fuel"),
                Fluids.WATER,
                10_000,
                80.0D
        );
        assertEquals(800.0D, fuel.totalEnergyPerMilliBucket());
        double conductivity = SingleBlockMachineMath.balancedConductivity(293.15D);
        assertTrue(Double.isFinite(conductivity));
        assertTrue(conductivity > 0.0D);
    }
}
