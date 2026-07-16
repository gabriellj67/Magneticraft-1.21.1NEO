package committee.nova.mods.magneticraft.content.network.module;

import committee.nova.mods.magneticraft.Magneticraft;
import committee.nova.mods.magneticraft.content.machine.framework.MachineModuleHost;
import committee.nova.mods.magneticraft.system.network.diagnostic.PressureDiagnosticSource;
import committee.nova.mods.magneticraft.system.network.pressure.PressureGasStack;
import committee.nova.mods.magneticraft.system.network.pressure.PressureLink;
import committee.nova.mods.magneticraft.system.network.pressure.PressureNode;
import committee.nova.mods.magneticraft.system.network.runtime.NetworkDomain;
import committee.nova.mods.magneticraft.system.network.runtime.PhysicalNetworkNode;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.fluids.capability.IFluidHandler;
import org.jetbrains.annotations.Nullable;

import java.util.EnumMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Predicate;

/**
 * Independent pressure node hosted by pneumatic components.
 */
public final class PressureNetworkModule extends AbstractPhysicalNetworkModule implements PressureDiagnosticSource {
    private static final int SCHEMA_VERSION = 2;
    private static final String GAS_ID_TAG = "gas_id";
    private static final String GAS_AMOUNT_TAG = "gas_kpa_liters";
    private static final ResourceLocation LEGACY_GAS = Magneticraft.id("steam");
    private static final AtomicBoolean LEGACY_MIGRATION_LOGGED = new AtomicBoolean();

    private final PressureNode node;
    private final double conductance;
    private final double maxGasPerTick;
    private final double leakFraction;
    private final Predicate<Direction> sideFilter;
    private final boolean exposeFluidCapability;
    private final Map<Direction, LazyOptional<IFluidHandler>> fluidCapabilities = new EnumMap<>(Direction.class);
    private LazyOptional<IFluidHandler> unsidedFluidCapability = LazyOptional.empty();

    public PressureNetworkModule(
            ResourceLocation id,
            MachineModuleHost host,
            PressureNode node,
            double conductance,
            double maxGasPerTick,
            double leakFraction,
            Predicate<Direction> sideFilter
    ) {
        this(id, host, node, conductance, maxGasPerTick, leakFraction, sideFilter, false);
    }

    public PressureNetworkModule(
            ResourceLocation id,
            MachineModuleHost host,
            PressureNode node,
            double conductance,
            double maxGasPerTick,
            double leakFraction,
            Predicate<Direction> sideFilter,
            boolean exposeFluidCapability
    ) {
        super(id, host, NetworkDomain.PRESSURE);
        this.node = Objects.requireNonNull(node);
        this.conductance = conductance;
        this.maxGasPerTick = maxGasPerTick;
        this.leakFraction = leakFraction;
        this.sideFilter = Objects.requireNonNull(sideFilter);
        this.exposeFluidCapability = exposeFluidCapability;
        reviveCapabilities();
    }

    public PressureNode node() {
        return node;
    }

    @Override
    public int persistenceSchemaVersion() {
        return SCHEMA_VERSION;
    }

    @Override
    public boolean canLoadPersistenceSchema(int schemaVersion) {
        return schemaVersion == 1 || schemaVersion == SCHEMA_VERSION;
    }

    @Override
    public Optional<PressureReading> pressureReading(Direction side) {
        if (!supportsSide(side)) {
            return Optional.empty();
        }
        return Optional.of(new PressureReading(
                node.gasId(),
                node.pressureKpa(),
                node.gasKpaLiters(),
                node.capacityKpaLiters(),
                node.fillRatio()
        ));
    }

    @Override
    protected boolean supportsSide(Direction direction) {
        return sideFilter.test(direction);
    }

    @Override
    public void exchangeWith(PhysicalNetworkNode other) {
        if (!(other instanceof PressureNetworkModule pressure)) {
            return;
        }
        PressureLink.Transfer transfer = PressureLink.transfer(
                node,
                pressure.node,
                Math.min(conductance, pressure.conductance),
                Math.min(maxGasPerTick, pressure.maxGasPerTick),
                Math.max(leakFraction, pressure.leakFraction)
        );
        if (transfer.moved()) {
            markStateChanged();
            pressure.markStateChanged();
        }
    }

    @Override
    protected void loadNetworkData(CompoundTag tag) {
        node.clear();
        double amount = Math.max(0.0D, tag.getDouble(GAS_AMOUNT_TAG));
        if (amount <= 0.0D) {
            return;
        }
        ResourceLocation gas = tag.contains(GAS_ID_TAG, Tag.TAG_STRING)
                && !tag.getString(GAS_ID_TAG).isBlank()
                ? ResourceLocation.tryParse(tag.getString(GAS_ID_TAG))
                : null;
        boolean legacyUntyped = !tag.contains(GAS_ID_TAG, Tag.TAG_STRING)
                && (!tag.contains("schema_version", Tag.TAG_INT) || tag.getInt("schema_version") == 1);
        if (gas == null && !legacyUntyped) {
            return;
        }
        if (gas == null) {
            gas = LEGACY_GAS;
            if (LEGACY_MIGRATION_LOGGED.compareAndSet(false, true)) {
                Magneticraft.LOGGER.warn("Migrated legacy untyped pressure data to {}", LEGACY_GAS);
            }
        }
        node.setContents(new PressureGasStack(gas, amount));
    }

    @Override
    protected void saveNetworkData(CompoundTag tag) {
        node.gasId().ifPresent(gas -> tag.putString(GAS_ID_TAG, gas.toString()));
        tag.putDouble(GAS_AMOUNT_TAG, node.gasKpaLiters());
    }

    @Override
    public void loadClientData(CompoundTag tag) {
        loadNetworkData(tag);
    }

    @Override
    public void saveClientData(CompoundTag tag) {
        saveNetworkData(tag);
    }

    @Override
    public void invalidateCapabilities() {
        unsidedFluidCapability.invalidate();
        unsidedFluidCapability = LazyOptional.empty();
        fluidCapabilities.values().forEach(LazyOptional::invalidate);
        fluidCapabilities.clear();
    }

    @Override
    public void reviveCapabilities() {
        if (!exposeFluidCapability || unsidedFluidCapability.isPresent()) {
            return;
        }
        unsidedFluidCapability = LazyOptional.of(this::createFluidHandler);
        for (Direction direction : Direction.values()) {
            fluidCapabilities.put(direction, LazyOptional.of(this::createFluidHandler));
        }
    }

    @Override
    public <T> LazyOptional<T> getCapability(Capability<T> capability, @Nullable Direction side) {
        if (!exposeFluidCapability || capability != ForgeCapabilities.FLUID_HANDLER) {
            return LazyOptional.empty();
        }
        if (side != null && !isSideEnabled(side)) {
            return LazyOptional.empty();
        }
        LazyOptional<IFluidHandler> result = side == null
                ? unsidedFluidCapability
                : fluidCapabilities.get(side);
        return result == null ? LazyOptional.empty() : result.cast();
    }

    private IFluidHandler createFluidHandler() {
        return new PressureFluidHandler(node, this::markStateChangedAndSync);
    }
}
