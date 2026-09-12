package committee.nova.mods.magneticraft.content.network.module;

import committee.nova.mods.magneticraft.content.machine.framework.MachineModule;
import committee.nova.mods.magneticraft.content.machine.framework.MachineModuleHost;
import committee.nova.mods.magneticraft.content.network.electric.ElectricalControlKind;
import committee.nova.mods.magneticraft.system.network.electric.ElectricalControlCoupler;
import committee.nova.mods.magneticraft.system.network.electric.ElectricalCoupler;
import committee.nova.mods.magneticraft.system.network.electric.item.TieredElectricalItemData;
import committee.nova.mods.magneticraft.system.network.electric.profile.ElectricalDataRegistry;
import committee.nova.mods.magneticraft.system.network.runtime.RedstoneControlMode;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceLocation;

import java.util.Objects;

/** Persistent controls around one isolated pair of same-tier electrical terminals. */
public final class ElectricalControlModule implements MachineModule {
    public static final int SCHEMA_VERSION = 1;
    public static final int DEFAULT_FIRST_RING = 0;
    public static final int DEFAULT_SECOND_RING = 1;
    public static final int DEFAULT_MULTIPLIER_RING = 0;

    private static final String SCHEMA_VERSION_TAG = "schema_version";
    private static final String REDSTONE_MODE_TAG = "redstone_mode";
    private static final String FIRST_RING_TAG = "first_ring";
    private static final String SECOND_RING_TAG = "second_ring";
    private static final String MULTIPLIER_RING_TAG = "multiplier_ring";
    private static final int RING_RADIX = 10;

    private final ResourceLocation id;
    private final MachineModuleHost host;
    private final ElectricalNetworkModule first;
    private final ElectricalNetworkModule second;
    private final ElectricalControlKind kind;
    private final ElectricalControlCoupler coupler;

    private RedstoneControlMode redstoneMode = RedstoneControlMode.REQUIRES_NO_SIGNAL;
    private int firstRing = DEFAULT_FIRST_RING;
    private int secondRing = DEFAULT_SECOND_RING;
    private int multiplierRing = DEFAULT_MULTIPLIER_RING;

    public ElectricalControlModule(
            ResourceLocation id,
            MachineModuleHost host,
            ElectricalNetworkModule first,
            ElectricalNetworkModule second,
            ElectricalControlKind kind
    ) {
        this.id = Objects.requireNonNull(id, "id");
        this.host = Objects.requireNonNull(host, "host");
        this.first = Objects.requireNonNull(first, "first");
        this.second = Objects.requireNonNull(second, "second");
        this.kind = Objects.requireNonNull(kind, "kind");
        coupler = new ElectricalControlCoupler(
                first.nodeKey(),
                second.nodeKey(),
                kind == ElectricalControlKind.DIODE
                        ? ElectricalControlCoupler.DirectionMode.FIRST_TO_SECOND
                        : ElectricalControlCoupler.DirectionMode.BIDIRECTIONAL,
                this::enabled,
                this::seriesResistanceOhms
        );
    }

    @Override
    public ResourceLocation id() {
        return id;
    }

    @Override
    public void load(CompoundTag tag, HolderLookup.Provider registries) {
        if (tag.isEmpty()) {
            return;
        }
        if (!tag.contains(SCHEMA_VERSION_TAG, Tag.TAG_INT)
                || tag.getInt(SCHEMA_VERSION_TAG) != SCHEMA_VERSION) {
            resetControls();
            return;
        }
        int modeOrdinal = tag.getInt(REDSTONE_MODE_TAG);
        RedstoneControlMode[] modes = RedstoneControlMode.values();
        int nextFirst = tag.getInt(FIRST_RING_TAG);
        int nextSecond = tag.getInt(SECOND_RING_TAG);
        int nextMultiplier = tag.getInt(MULTIPLIER_RING_TAG);
        if (modeOrdinal < 0 || modeOrdinal >= modes.length
                || !validRing(nextFirst) || !validRing(nextSecond) || !validRing(nextMultiplier)) {
            resetControls();
            return;
        }
        redstoneMode = modes[modeOrdinal];
        firstRing = nextFirst;
        secondRing = nextSecond;
        multiplierRing = nextMultiplier;
    }

    @Override
    public void save(CompoundTag tag, HolderLookup.Provider registries) {
        tag.putInt(SCHEMA_VERSION_TAG, SCHEMA_VERSION);
        tag.putInt(REDSTONE_MODE_TAG, redstoneMode.ordinal());
        tag.putInt(FIRST_RING_TAG, firstRing);
        tag.putInt(SECOND_RING_TAG, secondRing);
        tag.putInt(MULTIPLIER_RING_TAG, multiplierRing);
    }

    @Override
    public void loadClientData(CompoundTag tag, HolderLookup.Provider registries) {
        load(tag, registries);
    }

    @Override
    public void saveClientData(CompoundTag tag, HolderLookup.Provider registries) {
        save(tag, registries);
    }

    @Override
    public void onLoad() {
        first.registerElectricalCoupler(coupler);
    }

    @Override
    public void onUnload() {
        first.unregisterElectricalCoupler(coupler);
    }

    public boolean applyElectricalItemData(TieredElectricalItemData data) {
        Objects.requireNonNull(data, "data");
        if (data.ratingId().isPresent()
                || data.transformerProfileId().isPresent()
                || ElectricalDataRegistry.INSTANCE.current()
                .flatMap(snapshot -> snapshot.voltageTier(data.tierId()))
                .isEmpty()) {
            return false;
        }
        first.applyTierFromPlacementData(data.tierId());
        second.applyTierFromPlacementData(data.tierId());
        host.markChangedAndSync();
        return true;
    }

    public TieredElectricalItemData itemData() {
        return TieredElectricalItemData.forTier(first.tierId());
    }

    public void cycleRedstoneMode() {
        if (kind != ElectricalControlKind.SWITCH) {
            return;
        }
        redstoneMode = switch (redstoneMode) {
            case REQUIRES_NO_SIGNAL -> RedstoneControlMode.REQUIRES_SIGNAL;
            case REQUIRES_SIGNAL -> RedstoneControlMode.IGNORED;
            case IGNORED -> RedstoneControlMode.REQUIRES_NO_SIGNAL;
        };
        host.markChangedAndSync();
    }

    public void cycleRing(int ring) {
        if (kind != ElectricalControlKind.RESISTOR) {
            return;
        }
        switch (ring) {
            case 0 -> firstRing = nextRing(firstRing);
            case 1 -> secondRing = nextRing(secondRing);
            case 2 -> multiplierRing = nextRing(multiplierRing);
            default -> throw new IllegalArgumentException("Unknown resistor ring " + ring);
        }
        host.markChangedAndSync();
    }

    public boolean enabled() {
        if (kind != ElectricalControlKind.SWITCH) {
            return true;
        }
        boolean signal = host.level() != null && host.level().hasNeighborSignal(host.position());
        return redstoneMode.allows(signal);
    }

    public double resistanceOhms() {
        return Math.max(1.0D, (firstRing * 10.0D + secondRing) * Math.pow(10.0D, multiplierRing));
    }

    public RedstoneControlMode redstoneMode() {
        return redstoneMode;
    }

    public int firstRing() {
        return firstRing;
    }

    public int secondRing() {
        return secondRing;
    }

    public int multiplierRing() {
        return multiplierRing;
    }

    public ElectricalControlKind kind() {
        return kind;
    }

    public ElectricalControlCoupler coupler() {
        return coupler;
    }

    public ElectricalCoupler.CouplingResult lastTransfer() {
        return coupler.lastResult();
    }

    private double seriesResistanceOhms() {
        return kind == ElectricalControlKind.RESISTOR ? resistanceOhms() : 0.0D;
    }

    private void resetControls() {
        redstoneMode = RedstoneControlMode.REQUIRES_NO_SIGNAL;
        firstRing = DEFAULT_FIRST_RING;
        secondRing = DEFAULT_SECOND_RING;
        multiplierRing = DEFAULT_MULTIPLIER_RING;
    }

    private static int nextRing(int value) {
        return (value + 1) % RING_RADIX;
    }

    private static boolean validRing(int value) {
        return value >= 0 && value < RING_RADIX;
    }
}
