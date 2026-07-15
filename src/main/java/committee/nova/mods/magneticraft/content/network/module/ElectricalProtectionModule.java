package committee.nova.mods.magneticraft.content.network.module;

import committee.nova.mods.magneticraft.config.MagneticraftConfig;
import committee.nova.mods.magneticraft.content.machine.framework.MachineModule;
import committee.nova.mods.magneticraft.content.machine.framework.MachineModuleHost;
import committee.nova.mods.magneticraft.content.network.electric.ElectricalProtectionKind;
import committee.nova.mods.magneticraft.init.ModNetworkItems;
import committee.nova.mods.magneticraft.system.network.electric.ElectricalEdgeTelemetry;
import committee.nova.mods.magneticraft.system.network.electric.ElectricalNode;
import committee.nova.mods.magneticraft.system.network.electric.ElectricalStressState;
import committee.nova.mods.magneticraft.system.network.electric.ElectricalTickParticipant;
import committee.nova.mods.magneticraft.system.network.electric.item.ElectricalRatingIds;
import committee.nova.mods.magneticraft.system.network.electric.item.TieredElectricalItemData;
import committee.nova.mods.magneticraft.system.network.electric.profile.ElectricalDataRegistry;
import committee.nova.mods.magneticraft.system.network.electric.profile.VoltageTier;
import committee.nova.mods.magneticraft.system.network.runtime.PhysicalNetworkManager;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;

import java.util.Objects;
import java.util.Optional;

/** Server-authoritative fuse/breaker state around one explicit same-block internal edge. */
public final class ElectricalProtectionModule implements MachineModule, ElectricalTickParticipant {
    public static final double FUSE_THERMAL_CAPACITY = 42.0D;
    public static final double BREAKER_THERMAL_CAPACITY = 60.0D;
    public static final double IMMEDIATE_BREAKER_CURRENT_MULTIPLIER = 4.0D;

    private static final int SCHEMA_VERSION = 1;
    private static final String SCHEMA_VERSION_TAG = "schema_version";
    private static final String RATING_ID_TAG = "rating_id";
    private static final String FUSE_TAG = "fuse";
    private static final String STRESS_TAG = "thermal_stress";
    private static final String TRIPPED_TAG = "tripped";
    private static final String BLOWN_TAG = "blown";

    private final ResourceLocation id;
    private final MachineModuleHost host;
    private final ElectricalNetworkModule first;
    private final ElectricalNetworkModule second;
    private final ElectricalProtectionKind kind;
    private final ElectricalStressState stress = new ElectricalStressState();

    private ResourceLocation ratingId = ElectricalRatingIds.STANDARD;
    private ItemStack fuse = ItemStack.EMPTY;
    private boolean tripped;
    private boolean blown;
    private boolean lastConnectionClosed;

    public ElectricalProtectionModule(
            ResourceLocation id,
            MachineModuleHost host,
            ElectricalNetworkModule first,
            ElectricalNetworkModule second,
            ElectricalProtectionKind kind
    ) {
        this.id = Objects.requireNonNull(id, "id");
        this.host = Objects.requireNonNull(host, "host");
        this.first = Objects.requireNonNull(first, "first");
        this.second = Objects.requireNonNull(second, "second");
        this.kind = Objects.requireNonNull(kind, "kind");
        first.disableIntrinsicDamage();
        second.disableIntrinsicDamage();
        first.attachTickParticipant(this);
    }

    @Override
    public ResourceLocation id() {
        return id;
    }

    @Override
    public void load(CompoundTag tag) {
        if (!tag.contains(SCHEMA_VERSION_TAG, Tag.TAG_INT)
                || tag.getInt(SCHEMA_VERSION_TAG) != SCHEMA_VERSION) {
            resetSafeDisconnectedState();
            return;
        }
        ResourceLocation savedRating = ResourceLocation.tryParse(tag.getString(RATING_ID_TAG));
        if (savedRating == null || !ElectricalRatingIds.isKnown(savedRating)) {
            resetSafeDisconnectedState();
            return;
        }
        ratingId = savedRating;
        fuse = tag.contains(FUSE_TAG, Tag.TAG_COMPOUND)
                ? ItemStack.of(tag.getCompound(FUSE_TAG))
                : ItemStack.EMPTY;
        if (!validFuse(fuse)) {
            fuse = ItemStack.EMPTY;
        }
        stress.restore(tag.getDouble(STRESS_TAG));
        tripped = tag.getBoolean(TRIPPED_TAG);
        blown = tag.getBoolean(BLOWN_TAG);
    }

    @Override
    public void save(CompoundTag tag) {
        tag.putInt(SCHEMA_VERSION_TAG, SCHEMA_VERSION);
        tag.putString(RATING_ID_TAG, ratingId.toString());
        if (!fuse.isEmpty()) {
            tag.put(FUSE_TAG, fuse.save(new CompoundTag()));
        }
        tag.putDouble(STRESS_TAG, stress.stress());
        tag.putBoolean(TRIPPED_TAG, tripped);
        tag.putBoolean(BLOWN_TAG, blown);
    }

    @Override
    public void onLoad() {
        refreshConnection();
    }

    @Override
    public void onUnload() {
        first.setInternalConnection(second, false);
        lastConnectionClosed = false;
    }

    @Override
    public void serverTick() {
        refreshConnection();
    }

    @Override
    public void commitElectricalState(PhysicalNetworkManager manager) {
        Optional<VoltageTier> tier = ElectricalDataRegistry.INSTANCE.current()
                .flatMap(snapshot -> snapshot.voltageTier(first.tierId()));
        if (tier.isEmpty() || !protectionInstalled()) {
            refreshConnection();
            return;
        }
        VoltageTier value = tier.orElseThrow();
        double ratedCharge = ratingId.equals(ElectricalRatingIds.HEAVY)
                ? value.heavyProtectionChargePerTick()
                : value.standardProtectionChargePerTick();
        double ratedCurrent = ratedCharge * ElectricalNode.TICKS_PER_SECOND;
        double current = manager.electricalEdgeCurrentAmps(
                first.nodeKey(),
                second.nodeKey(),
                ElectricalEdgeTelemetry.EdgeType.INTERNAL
        );
        boolean accumulationEnabled = MagneticraftConfig.ENABLE_ELECTRICAL_DAMAGE.get()
                && !manager.electricalDamageSuppressed();
        double before = stress.stress();
        boolean failedNow = kind == ElectricalProtectionKind.CIRCUIT_BREAKER
                && accumulationEnabled
                && current >= ratedCurrent * IMMEDIATE_BREAKER_CURRENT_MULTIPLIER;
        failedNow |= stress.update(
                current,
                ratedCurrent,
                Math.max(first.node().voltage(), second.node().voltage()),
                value.maximumVoltage(),
                kind == ElectricalProtectionKind.FUSE_BOX
                        ? FUSE_THERMAL_CAPACITY
                        : BREAKER_THERMAL_CAPACITY,
                value.cableCoolingPerTick(),
                accumulationEnabled
        );
        if (stress.stress() != before) {
            host.markChanged();
        }
        if (failedNow) {
            failProtection();
        }
        refreshConnection();
    }

    public boolean applyElectricalItemData(TieredElectricalItemData data) {
        Objects.requireNonNull(data, "data");
        if (ElectricalDataRegistry.INSTANCE.current()
                .flatMap(snapshot -> snapshot.voltageTier(data.tierId()))
                .isEmpty()) {
            return false;
        }
        if (kind == ElectricalProtectionKind.CIRCUIT_BREAKER) {
            Optional<ResourceLocation> rating = data.ratingId().filter(ElectricalRatingIds::isKnown);
            if (rating.isEmpty()) {
                return false;
            }
            ratingId = rating.orElseThrow();
        } else if (data.ratingId().isPresent()) {
            return false;
        }
        first.applyTierFromPlacementData(data.tierId());
        second.applyTierFromPlacementData(data.tierId());
        stress.reset();
        tripped = false;
        blown = false;
        refreshConnection();
        host.markChangedAndSync();
        return true;
    }

    public boolean insertFuse(ItemStack candidate) {
        if (kind != ElectricalProtectionKind.FUSE_BOX || !fuse.isEmpty() || !validFuse(candidate)) {
            return false;
        }
        TieredElectricalItemData data = TieredElectricalItemData.read(candidate).orElseThrow();
        fuse = candidate.copyWithCount(1);
        ratingId = data.ratingId().orElseThrow();
        blown = false;
        stress.reset();
        refreshConnection();
        host.markChangedAndSync();
        return true;
    }

    public ItemStack removeFuseForDrop() {
        ItemStack removed = fuse;
        fuse = ItemStack.EMPTY;
        refreshConnection();
        return removed;
    }

    public boolean resetBreaker() {
        if (kind != ElectricalProtectionKind.CIRCUIT_BREAKER || !tripped || redstoneForcedOpen()) {
            return false;
        }
        tripped = false;
        stress.reset();
        refreshConnection();
        host.markChangedAndSync();
        return true;
    }

    public void refreshConnection() {
        boolean closed = protectionInstalled()
                && !redstoneForcedOpen()
                && first.electricalProfileBound()
                && second.electricalProfileBound()
                && first.tierId().equals(second.tierId());
        if (closed != lastConnectionClosed) {
            first.setInternalConnection(second, closed);
            lastConnectionClosed = closed;
        }
    }

    public TieredElectricalItemData itemData() {
        return new TieredElectricalItemData(
                first.tierId(),
                kind == ElectricalProtectionKind.CIRCUIT_BREAKER
                        ? Optional.of(ratingId)
                        : Optional.empty(),
                Optional.empty()
        );
    }

    public ResourceLocation ratingId() {
        return ratingId;
    }

    public ItemStack fuse() {
        return fuse.copy();
    }

    public double thermalStress() {
        return stress.stress();
    }

    public boolean tripped() {
        return tripped;
    }

    public boolean blown() {
        return blown;
    }

    public boolean redstoneForcedOpen() {
        return host.level() != null && host.level().hasNeighborSignal(host.position());
    }

    private boolean protectionInstalled() {
        return kind == ElectricalProtectionKind.CIRCUIT_BREAKER
                ? !tripped
                : !fuse.isEmpty() && !blown;
    }

    private boolean validFuse(ItemStack candidate) {
        if (candidate.isEmpty() || !candidate.is(ModNetworkItems.FUSE.get())) {
            return false;
        }
        return TieredElectricalItemData.read(candidate)
                .filter(data -> data.tierId().equals(first.tierId()))
                .flatMap(TieredElectricalItemData::ratingId)
                .filter(ElectricalRatingIds::isKnown)
                .isPresent();
    }

    private void failProtection() {
        if (kind == ElectricalProtectionKind.FUSE_BOX) {
            fuse = ItemStack.EMPTY;
            blown = true;
        } else {
            tripped = true;
        }
        host.markChangedAndSync();
    }

    private void resetSafeDisconnectedState() {
        ratingId = ElectricalRatingIds.STANDARD;
        fuse = ItemStack.EMPTY;
        stress.reset();
        tripped = kind == ElectricalProtectionKind.CIRCUIT_BREAKER;
        blown = false;
        lastConnectionClosed = false;
    }
}
