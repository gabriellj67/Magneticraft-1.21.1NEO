package committee.nova.mods.magneticraft.system.network.electric;

import committee.nova.mods.magneticraft.system.network.diagnostic.ElectricalDiagnosticSource.FaultKind;
import net.minecraft.resources.ResourceLocation;

/** Narrow runtime view used by the topology manager to exchange native electricity. */
public interface ElectricalNodeAccess {
    ElectricalNode electricalNode();

    ResourceLocation electricalTierId();

    boolean electricalProfileBound();

    default double electricalRatedCurrentAmps() {
        return Double.POSITIVE_INFINITY;
    }

    default double electricalThermalStress() {
        return 0.0D;
    }

    default FaultKind electricalFaultKind() {
        return electricalProfileBound() ? FaultKind.NONE : FaultKind.MISSING_PROFILE;
    }

    void markElectricalStateChanged();
}
