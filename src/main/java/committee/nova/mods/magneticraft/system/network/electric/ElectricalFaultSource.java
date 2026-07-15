package committee.nova.mods.magneticraft.system.network.electric;

import committee.nova.mods.magneticraft.system.network.diagnostic.ElectricalDiagnosticSource.FaultKind;

/** Optional device-level failure state shared by every terminal owned by that device. */
public interface ElectricalFaultSource {
    FaultKind electricalFaultKind();

    default double electricalRatedCurrentAmps() {
        return 0.0D;
    }

    default double electricalThermalStress() {
        return 0.0D;
    }
}
