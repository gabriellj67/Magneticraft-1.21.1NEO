package committee.nova.mods.magneticraft.content.multiblock;

import committee.nova.mods.magneticraft.Magneticraft;
import committee.nova.mods.magneticraft.content.machine.framework.MachineBlockEntity;
import committee.nova.mods.magneticraft.content.machine.framework.menu.Int32ContainerData;
import committee.nova.mods.magneticraft.content.machine.framework.module.BulkItemStorageModule;
import committee.nova.mods.magneticraft.content.machine.framework.module.FluidTankModule;
import committee.nova.mods.magneticraft.content.machine.framework.module.ItemInventoryModule;
import committee.nova.mods.magneticraft.content.machine.singleblock.SingleBlockMachineBlockEntity;
import committee.nova.mods.magneticraft.content.network.module.ElectricalNetworkModule;
import committee.nova.mods.magneticraft.content.network.module.ElectricalPowerModule;
import committee.nova.mods.magneticraft.content.network.module.HeatNetworkModule;
import committee.nova.mods.magneticraft.init.ModBlockEntities;
import committee.nova.mods.magneticraft.init.ModRecipeTypes;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.common.ForgeHooks;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.capability.IFluidHandler;
import committee.nova.mods.magneticraft.system.network.electric.ElectricalNodeKind;
import committee.nova.mods.magneticraft.system.network.heat.HeatNode;
import committee.nova.mods.magneticraft.system.network.runtime.NetworkDomain;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Server-authoritative structure lifecycle host. Machine modules are added by
 * the advanced behavior layer without changing formation semantics.
 */
public final class AdvancedMultiblockBlockEntity extends MachineBlockEntity implements MenuProvider {
    public static final int MENU_LOGICAL_DATA_COUNT = 22;
    public static final int MENU_DATA_COUNT = MENU_LOGICAL_DATA_COUNT * 2;
    private static final String FORMED_TAG = "formed";
    private static final String MIRRORED_TAG = "mirrored";
    private static final String OWNER_TAG = "owner";
    private static final String PROGRESS_TAG = "progress";
    private static final String TOTAL_PROGRESS_TAG = "total_progress";
    private static final String WORKING_TAG = "working";
    private static final String ACTIVE_RECIPE_TAG = "active_recipe";
    private static final String BURN_TICKS_TAG = "burn_ticks";
    private static final String BURN_POWER_TAG = "burn_power";
    private static final String SOLAR_TOWER_TAG = "solar_tower";
    private static final String HYDRAULIC_MODE_TAG = "hydraulic_mode";
    private static final String STRUCTURE_SNAPSHOT_TAG = "structure_snapshot";
    private static final String HOLOGRAM_ENABLED_TAG = "hologram_enabled";

    private final MultiblockDefinition definition;
    @Nullable
    private final BulkItemStorageModule bulkStorage;
    @Nullable
    private final ShelvingStorageModule shelvingStorage;
    @Nullable
    private final ItemInventoryModule inventory;
    private final List<FluidTankModule> tanks;
    @Nullable
    private final ElectricalPowerModule energy;
    @Nullable
    private final ElectricalNetworkModule electricity;
    @Nullable
    private final HeatNetworkModule heat;
    private final AdvancedMultiblockLogic logic;
    private final ContainerData menuData;
    private final MultiblockStructureSnapshot structureSnapshot = new MultiblockStructureSnapshot();
    private final Map<PortEndpoint, LazyOptional<?>> portCapabilities = new HashMap<>();
    private List<MultiblockExternalPortService.ExternalNode> externalPortNodes = List.of();
    private boolean formed;
    private boolean structureReady;
    private boolean hologramEnabled = true;
    private boolean mirrored;
    @Nullable
    private UUID owner;
    private int progress;
    private int totalProgress;
    private int burnTicks;
    private double burnPower;
    @Nullable
    private BlockPos solarTowerPosition;
    private HydraulicPressMode hydraulicPressMode = HydraulicPressMode.LIGHT;
    @Nullable
    private ResourceLocation activeRecipe;
    private boolean working;

    public AdvancedMultiblockBlockEntity(BlockPos position, BlockState state) {
        this(position, state, definition(state));
    }

    private AdvancedMultiblockBlockEntity(
            BlockPos position,
            BlockState state,
            MultiblockDefinition definition
    ) {
        super(ModBlockEntities.advancedMultiblock(definition).get(), position, state);
        this.definition = definition;
        formed = state.getValue(AdvancedMultiblockBlock.FORMED);
        bulkStorage = createBulkStorage();
        shelvingStorage = createShelvingStorage();
        inventory = createInventory();
        tanks = createTanks();
        electricity = createElectricity();
        energy = createEnergy(electricity);
        heat = createHeat();
        logic = new AdvancedMultiblockLogic(this);
        menuData = Int32ContainerData.readOnly(
                () -> energy == null ? 0 : energy.storedWholeJoules(),
                () -> energy == null ? 0 : energy.ratedCapacityWholeJoules(),
                () -> progress,
                () -> totalProgress,
                () -> working ? 1 : 0,
                () -> heat == null ? 0 : (int) Math.round(heat.node().temperatureKelvin() * 10.0D),
                () -> electricity == null ? 0 : (int) Math.round(electricity.node().voltage() * 10.0D),
                () -> bulkStorage == null ? 0 : bulkStorage.amount(),
                () -> bulkStorage == null ? 0 : bulkStorage.capacity(),
                () -> shelvingStorage == null ? 0 : shelvingStorage.installedChests(),
                () -> shelvingStorage == null ? 0 : shelvingStorage.unlockedSlots(),
                definition::tankCount,
                () -> tankAmount(0),
                () -> tankCapacity(0),
                () -> tankAmount(1),
                () -> tankCapacity(1),
                () -> tankAmount(2),
                () -> tankCapacity(2),
                () -> tankAmount(3),
                () -> tankCapacity(3),
                () -> tankAmount(4),
                () -> tankCapacity(4)
        );
    }

    private static MultiblockDefinition definition(BlockState state) {
        if (!(state.getBlock() instanceof AdvancedMultiblockBlock controller)) {
            throw new IllegalArgumentException("Advanced multiblock entity attached to " + state.getBlock());
        }
        return controller.definition();
    }

    public static void serverTick(
            Level level,
            BlockPos position,
            BlockState state,
            AdvancedMultiblockBlockEntity controller
    ) {
        controller.serverTick((ServerLevel) level);
    }

    public MultiblockDefinition definition() {
        return definition;
    }

    public boolean formed() {
        return formed;
    }

    public boolean hologramEnabled() {
        return hologramEnabled;
    }

    public void toggleHologram(Player player) {
        if (formed || !canManage(player)) {
            return;
        }
        hologramEnabled = !hologramEnabled;
        markChangedAndSync();
        player.displayClientMessage(Component.translatable(
                hologramEnabled
                        ? "message.magneticraft.multiblock_hologram_enabled"
                        : "message.magneticraft.multiblock_hologram_disabled"
        ), true);
    }

    public boolean operational() {
        return formed && structureReady && validate().valid();
    }

    @Override
    public boolean supportsNetworkConnection(NetworkDomain domain, Direction side) {
        return supportsNetworkConnection(worldPosition, domain, side);
    }

    boolean supportsNetworkConnection(BlockPos position, NetworkDomain domain, Direction side) {
        boolean portsAvailable = level != null && level.isClientSide ? formed : operational();
        return portsAvailable && MultiblockPortLayout.supports(this, position, domain, side);
    }

    public boolean mirrored() {
        return mirrored;
    }

    public Direction facing() {
        return getBlockState().getValue(AdvancedMultiblockBlock.FACING);
    }

    @Override
    public AABB getRenderBoundingBox() {
        return MultiblockBounds.renderBounds(worldPosition, facing(), mirrored, definition);
    }

    @Nullable
    public ItemInventoryModule inventory() {
        return inventory;
    }

    @Nullable
    public BulkItemStorageModule bulkStorage() {
        return bulkStorage;
    }

    @Nullable
    public ShelvingStorageModule shelvingStorage() {
        return shelvingStorage;
    }

    @Nullable
    public FluidTankModule tank(int index) {
        return index >= 0 && index < tanks.size() ? tanks.get(index) : null;
    }

    @Nullable
    public ElectricalPowerModule energy() {
        return energy;
    }

    @Nullable
    public ElectricalNetworkModule electricity() {
        return electricity;
    }

    @Nullable
    public HeatNetworkModule heat() {
        return heat;
    }

    public int progress() {
        return progress;
    }

    public int totalProgress() {
        return totalProgress;
    }

    public ContainerData menuData() {
        return menuData;
    }

    public int burnTicks() {
        return burnTicks;
    }

    public double burnPower() {
        return burnPower;
    }

    @Nullable
    public BlockPos solarTowerPosition() {
        return solarTowerPosition;
    }

    public void setSolarTowerPosition(@Nullable BlockPos solarTowerPosition) {
        BlockPos stable = solarTowerPosition == null ? null : solarTowerPosition.immutable();
        if (!java.util.Objects.equals(this.solarTowerPosition, stable)) {
            this.solarTowerPosition = stable;
            markChangedAndSync();
        }
    }

    public HydraulicPressMode hydraulicPressMode() {
        return hydraulicPressMode;
    }

    public void cycleHydraulicMode(Player player) {
        if (definition != MultiblockDefinition.HYDRAULIC_PRESS || !formed || !canManage(player)) {
            return;
        }
        hydraulicPressMode = hydraulicPressMode.next();
        resetProgress();
        markChangedAndSync();
        player.displayClientMessage(Component.translatable(
                "message.magneticraft.hydraulic_press_mode",
                Component.translatable("message.magneticraft.hydraulic_press_mode." + hydraulicPressMode.serializedName())
        ), true);
    }

    public boolean working() {
        return working;
    }

    @Nullable
    public UUID owner() {
        return owner;
    }

    public void setOwner(UUID owner) {
        if (this.owner == null) {
            this.owner = owner;
            markChanged();
        }
    }

    public boolean tryForm(Player player) {
        if (formed || !canManage(player) || !(level instanceof ServerLevel serverLevel)) {
            return false;
        }
        // Nova 1.12 exposed four rotations only. Keep the persisted flag solely
        // so already formed 0.2-0.5 structures can finish loading compatibly.
        if (mirrored) {
            mirrored = false;
            markChangedAndSync();
        }
        MultiblockValidationResult result = validate();
        if (!result.valid()) {
            player.displayClientMessage(validationMessage(result), false);
            return false;
        }
        List<BlockPos> members = members();
        if (!canClaimMemberCapabilities()
                || !MultiblockMembershipService.register(
                serverLevel, worldPosition, definition, members)) {
            player.displayClientMessage(Component.translatable(
                    "message.magneticraft.multiblock_member_conflict"
            ), false);
            return false;
        }
        setMemberCapabilities(false);
        if (!structureSnapshot.captureAndHide(serverLevel, worldPosition, members)
                || !configureGapCollision(serverLevel, members)) {
            structureSnapshot.restore(serverLevel, null);
            MultiblockMembershipService.unregister(serverLevel, worldPosition);
            setMemberCapabilities(true);
            player.displayClientMessage(Component.translatable(
                    "message.magneticraft.multiblock_invalid"
            ), false);
            return false;
        }
        setFormed(true);
        player.displayClientMessage(Component.translatable(
                "message.magneticraft.multiblock_formed",
                Component.translatable(getBlockState().getBlock().getDescriptionId())
        ), false);
        return true;
    }

    public void unform() {
        unform(null);
    }

    private void unform(@Nullable BlockPos brokenMember) {
        if (level instanceof ServerLevel serverLevel) {
            structureSnapshot.restore(serverLevel, brokenMember);
            MultiblockMembershipService.unregister(serverLevel, worldPosition);
        }
        if (formed) {
            setFormed(false);
        }
    }

    public void onMemberBroken(BlockPos member) {
        if (!member.equals(worldPosition)) {
            unform(member);
        }
    }

    public void saveToItem(ItemStack stack) {
        CompoundTag blockEntityTag = saveWithFullMetadata();
        blockEntityTag.remove("id");
        blockEntityTag.remove("x");
        blockEntityTag.remove("y");
        blockEntityTag.remove("z");
        blockEntityTag.putBoolean(FORMED_TAG, false);
        blockEntityTag.putBoolean(MIRRORED_TAG, false);
        blockEntityTag.remove(OWNER_TAG);
        blockEntityTag.remove(SOLAR_TOWER_TAG);
        blockEntityTag.remove(STRUCTURE_SNAPSHOT_TAG);
        if (definition == MultiblockDefinition.SHELVING_UNIT) {
            CompoundTag modules = blockEntityTag.getCompound(MachineBlockEntity.MODULES_TAG);
            modules.remove(Magneticraft.id("advanced_shelving_inventory").toString());
            blockEntityTag.put(MachineBlockEntity.MODULES_TAG, modules);
        }
        logic.stripPortableState(blockEntityTag);
        stack.addTagElement("BlockEntityTag", blockEntityTag);
    }

    public void describe(Player player) {
        player.displayClientMessage(Component.translatable(
                "message.magneticraft.multiblock_status",
                Component.translatable(getBlockState().getBlock().getDescriptionId()),
                mirrored
                        ? Component.translatable("message.magneticraft.multiblock_mirrored_state")
                        : Component.translatable("message.magneticraft.multiblock_normal_state")
        ), true);
        if (shelvingStorage != null) {
            player.displayClientMessage(Component.translatable(
                    "message.magneticraft.shelving_capacity",
                    shelvingStorage.installedChests(),
                    ShelvingStorageModule.MAX_CHESTS,
                    shelvingStorage.unlockedSlots()
            ), false);
        }
    }

    public boolean handleShelvingChest(Player player, ItemStack heldStack) {
        if (shelvingStorage == null || !ShelvingStorageModule.isChestUpgrade(heldStack)) {
            return false;
        }
        if (!formed) {
            player.displayClientMessage(Component.translatable(
                    "message.magneticraft.shelving_requires_formed"
            ), true);
            return true;
        }
        if (!canManage(player)) {
            player.displayClientMessage(Component.translatable(
                    "message.magneticraft.shelving_owner_denied"
            ), true);
            return true;
        }
        if (!shelvingStorage.installChest(heldStack)) {
            player.displayClientMessage(Component.translatable(
                    "message.magneticraft.shelving_full",
                    ShelvingStorageModule.MAX_CHESTS
            ), true);
            return true;
        }
        if (!player.getAbilities().instabuild) {
            heldStack.shrink(1);
        }
        player.displayClientMessage(Component.translatable(
                "message.magneticraft.shelving_chest_installed",
                shelvingStorage.installedChests(),
                ShelvingStorageModule.MAX_CHESTS,
                shelvingStorage.unlockedSlots()
        ), true);
        return true;
    }

    public boolean canManage(Player player) {
        return owner == null || owner.equals(player.getUUID()) || player.hasPermissions(2);
    }

    @Override
    public Component getDisplayName() {
        return Component.translatable(getBlockState().getBlock().getDescriptionId());
    }

    @Nullable
    @Override
    public AbstractContainerMenu createMenu(int containerId, Inventory playerInventory, Player player) {
        return formed && canManage(player)
                ? new AdvancedMultiblockMenu(containerId, playerInventory, this)
                : null;
    }

    public boolean hasPortableState() {
        if (bulkStorage != null && bulkStorage.amount() > 0) {
            return true;
        }
        if (shelvingStorage != null && !shelvingStorage.dropContents().isEmpty()) {
            return true;
        }
        if (inventory != null) {
            for (int slot = 0; slot < inventory.slots(); slot++) {
                if (!inventory.getStackInSlot(slot).isEmpty()) {
                    return true;
                }
            }
        }
        for (FluidTankModule tank : tanks) {
            if (!tank.tank().getFluid().isEmpty()) {
                return true;
            }
        }
        return (energy != null && energy.storedJoules() > 0.0D)
                || (heat != null && Math.abs(
                heat.node().temperatureKelvin() - HeatNode.AMBIENT_TEMPERATURE_KELVIN) > 1.0E-6D)
                || progress > 0
                || burnTicks > 0;
    }

    @Override
    public <T> LazyOptional<T> getCapability(Capability<T> capability, @Nullable Direction side) {
        if (!operational() && (capability == ForgeCapabilities.ITEM_HANDLER
                || capability == ForgeCapabilities.FLUID_HANDLER
                || capability == ForgeCapabilities.ENERGY)) {
            return LazyOptional.empty();
        }
        if (exposesExactPorts(capability)) {
            return side == null
                    ? LazyOptional.empty()
                    : portCapability(worldPosition, side, capability);
        }
        return super.getCapability(capability, side);
    }

    public <T> LazyOptional<T> portCapability(
            BlockPos position,
            Direction side,
            Capability<T> capability
    ) {
        if (!operational()) {
            return LazyOptional.empty();
        }
        MultiblockPortLayout.Kind kind;
        if (capability == ForgeCapabilities.ITEM_HANDLER) {
            kind = MultiblockPortLayout.Kind.ITEM;
        } else if (capability == ForgeCapabilities.FLUID_HANDLER) {
            kind = MultiblockPortLayout.Kind.FLUID;
        } else {
            return LazyOptional.empty();
        }
        List<MultiblockPortLayout.Port> ports = MultiblockPortLayout.findAll(this, position, side, kind);
        if (ports.isEmpty()) {
            return LazyOptional.empty();
        }
        PortEndpoint endpoint = new PortEndpoint(position, side, kind);
        LazyOptional<?> result = portCapabilities.computeIfAbsent(
                endpoint,
                ignored -> createPortCapability(ports)
        );
        return result.cast();
    }

    private boolean exposesExactPorts(Capability<?> capability) {
        MultiblockPortLayout.Kind kind = capability == ForgeCapabilities.ITEM_HANDLER
                ? MultiblockPortLayout.Kind.ITEM
                : capability == ForgeCapabilities.FLUID_HANDLER
                ? MultiblockPortLayout.Kind.FLUID
                : null;
        return kind != null && MultiblockPortLayout.ports(definition).stream()
                .anyMatch(port -> port.kind() == kind);
    }

    private LazyOptional<?> createPortCapability(List<MultiblockPortLayout.Port> ports) {
        MultiblockPortLayout.Port first = ports.get(0);
        return switch (first.kind()) {
            case ITEM -> inventory == null
                    ? LazyOptional.empty()
                    : LazyOptional.of(() -> inventory.portHandler(first.itemAccess()));
            case FLUID -> {
                List<IFluidHandler> handlers = ports.stream()
                        .map(port -> {
                            FluidTankModule tank = tank(port.target());
                            return tank == null ? null : tank.portHandler(port.fluidAccess());
                        })
                        .filter(java.util.Objects::nonNull)
                        .toList();
                if (handlers.isEmpty()) {
                    yield LazyOptional.empty();
                }
                yield LazyOptional.of(() -> handlers.size() == 1
                        ? handlers.get(0)
                        : new PortFluidHandler(handlers));
            }
            default -> LazyOptional.empty();
        };
    }

    @Override
    public void invalidateCaps() {
        portCapabilities.values().forEach(LazyOptional::invalidate);
        portCapabilities.clear();
        super.invalidateCaps();
    }

    public MultiblockValidationResult validate() {
        BlockState state = getBlockState();
        return MultiblockMatcher.validate(
                level,
                worldPosition,
                state.getValue(AdvancedMultiblockBlock.FACING),
                mirrored,
                definition,
                state.getBlock()
        );
    }

    public List<BlockPos> members() {
        return MultiblockMatcher.memberPositions(
                worldPosition,
                getBlockState().getValue(AdvancedMultiblockBlock.FACING),
                mirrored,
                definition
        );
    }

    private boolean configureGapCollision(ServerLevel serverLevel, List<BlockPos> members) {
        Direction structureFacing = facing();
        for (BlockPos member : members) {
            if (member.equals(worldPosition)) {
                continue;
            }
            if (!(serverLevel.getBlockEntity(member) instanceof MultiblockGapBlockEntity gap)) {
                return false;
            }
            gap.configure(worldPosition, definition, structureFacing);
        }
        return true;
    }

    @Override
    public void onLoad() {
        super.onLoad();
        structureReady = false;
        rebindPhysicalModules();
    }

    @Override
    public void setRemoved() {
        super.setRemoved();
    }

    @Override
    protected void saveMachineData(CompoundTag tag) {
        tag.putBoolean(FORMED_TAG, formed);
        tag.putBoolean(HOLOGRAM_ENABLED_TAG, hologramEnabled);
        tag.putBoolean(MIRRORED_TAG, mirrored);
        if (owner != null) {
            tag.putUUID(OWNER_TAG, owner);
        }
        tag.putInt(PROGRESS_TAG, progress);
        tag.putInt(TOTAL_PROGRESS_TAG, totalProgress);
        tag.putInt(BURN_TICKS_TAG, burnTicks);
        tag.putDouble(BURN_POWER_TAG, burnPower);
        if (solarTowerPosition != null) {
            tag.putLong(SOLAR_TOWER_TAG, solarTowerPosition.asLong());
        }
        tag.putString(HYDRAULIC_MODE_TAG, hydraulicPressMode.serializedName());
        if (activeRecipe != null) {
            tag.putString(ACTIVE_RECIPE_TAG, activeRecipe.toString());
        }
        logic.savePersistentState(tag);
        if (!structureSnapshot.isEmpty()) {
            tag.put(STRUCTURE_SNAPSHOT_TAG, structureSnapshot.save());
        }
    }

    @Override
    protected void loadMachineData(CompoundTag tag) {
        formed = tag.getBoolean(FORMED_TAG);
        structureReady = false;
        hologramEnabled = !tag.contains(HOLOGRAM_ENABLED_TAG)
                || tag.getBoolean(HOLOGRAM_ENABLED_TAG);
        mirrored = tag.getBoolean(MIRRORED_TAG);
        owner = tag.hasUUID(OWNER_TAG) ? tag.getUUID(OWNER_TAG) : null;
        progress = Math.max(0, tag.getInt(PROGRESS_TAG));
        totalProgress = Math.max(0, tag.getInt(TOTAL_PROGRESS_TAG));
        burnTicks = Math.max(0, tag.getInt(BURN_TICKS_TAG));
        burnPower = Math.max(0.0D, tag.getDouble(BURN_POWER_TAG));
        solarTowerPosition = tag.contains(SOLAR_TOWER_TAG)
                ? BlockPos.of(tag.getLong(SOLAR_TOWER_TAG))
                : null;
        hydraulicPressMode = parseHydraulicMode(tag.getString(HYDRAULIC_MODE_TAG));
        activeRecipe = tag.contains(ACTIVE_RECIPE_TAG)
                ? ResourceLocation.tryParse(tag.getString(ACTIVE_RECIPE_TAG))
                : null;
        logic.loadPersistentState(tag);
        structureSnapshot.load(tag.contains(STRUCTURE_SNAPSHOT_TAG, Tag.TAG_LIST)
                ? tag.getList(STRUCTURE_SNAPSHOT_TAG, Tag.TAG_COMPOUND)
                : new ListTag());
        working = false;
    }

    @Override
    protected void resetMachineData() {
        ListTag preservedStructure = structureSnapshot.save();
        super.resetMachineData();
        structureSnapshot.load(preservedStructure);
        // Formation is world-derived state. Preserve only the controller block's
        // claim, then force a full structure validation before processing resumes.
        formed = getBlockState().getValue(AdvancedMultiblockBlock.FORMED);
    }

    @Override
    protected void saveClientData(CompoundTag tag) {
        tag.putBoolean(FORMED_TAG, formed);
        tag.putBoolean(HOLOGRAM_ENABLED_TAG, hologramEnabled);
        tag.putBoolean(MIRRORED_TAG, mirrored);
        tag.putBoolean(WORKING_TAG, working);
        tag.putInt(PROGRESS_TAG, progress);
        tag.putInt(TOTAL_PROGRESS_TAG, totalProgress);
        if (solarTowerPosition != null) {
            tag.putLong(SOLAR_TOWER_TAG, solarTowerPosition.asLong());
        }
        tag.putString(HYDRAULIC_MODE_TAG, hydraulicPressMode.serializedName());
    }

    @Override
    protected void loadClientData(CompoundTag tag) {
        formed = tag.getBoolean(FORMED_TAG);
        hologramEnabled = !tag.contains(HOLOGRAM_ENABLED_TAG)
                || tag.getBoolean(HOLOGRAM_ENABLED_TAG);
        mirrored = tag.getBoolean(MIRRORED_TAG);
        working = tag.getBoolean(WORKING_TAG);
        progress = Math.max(0, tag.getInt(PROGRESS_TAG));
        totalProgress = Math.max(0, tag.getInt(TOTAL_PROGRESS_TAG));
        solarTowerPosition = tag.contains(SOLAR_TOWER_TAG)
                ? BlockPos.of(tag.getLong(SOLAR_TOWER_TAG))
                : null;
        hydraulicPressMode = parseHydraulicMode(tag.getString(HYDRAULIC_MODE_TAG));
    }

    private void serverTick(ServerLevel serverLevel) {
        if (!formed) {
            return;
        }
        MultiblockValidationResult result = validate();
        if (result.status() == MultiblockValidationResult.Status.MISMATCH) {
            unform(result.position());
            return;
        }
        if (result.status() == MultiblockValidationResult.Status.UNLOADED) {
            setStructureReady(false);
            return;
        }
        if (!structureReady) {
            if (!canClaimMemberCapabilities()
                    || !MultiblockMembershipService.register(
                    serverLevel, worldPosition, definition, members())) {
                unform();
                return;
            }
            List<BlockPos> members = members();
            if ((structureSnapshot.isEmpty()
                    && !structureSnapshot.captureAndHide(serverLevel, worldPosition, members))
                    || !configureGapCollision(serverLevel, members)) {
                unform();
                return;
            }
            setStructureReady(true);
            rebindMemberCapabilities();
        }
        if (structureReady) {
            tickModules();
            if (electricalFaulted()) {
                setWorking(false);
            } else {
                logic.tick(serverLevel);
            }
            syncClientState(visualStateHash(progress, totalProgress, working));
            finishServerTick();
        }
    }

    private static int visualStateHash(int progress, int totalProgress, boolean working) {
        return 31 * (31 * progress + totalProgress) + (working ? 1 : 0);
    }

    private void setFormed(boolean formed) {
        if (this.formed == formed) {
            return;
        }
        this.formed = formed;
        structureReady = formed;
        refreshCapabilities();
        rebindPhysicalModules();
        rebindMemberCapabilities();
        markChangedAndSync();
        if (level != null) {
            BlockState worldState = level.getBlockState(worldPosition);
            if (worldState.getBlock() instanceof AdvancedMultiblockBlock controller
                    && controller.definition() == definition
                    && worldState.getValue(AdvancedMultiblockBlock.FORMED) != formed) {
                level.setBlock(
                        worldPosition,
                        worldState.setValue(AdvancedMultiblockBlock.FORMED, formed),
                        Block.UPDATE_ALL
                );
            }
            if (level instanceof ServerLevel serverLevel) {
                if (formed) {
                    logic.onFormed(serverLevel);
                } else {
                    logic.onUnformed();
                }
            }
        }
    }

    private Component validationMessage(MultiblockValidationResult result) {
        if (result.position() == null) {
            return Component.translatable("message.magneticraft.multiblock_invalid");
        }
        String coordinates = result.position().getX() + ", "
                + result.position().getY() + ", " + result.position().getZ();
        return result.status() == MultiblockValidationResult.Status.UNLOADED
                ? Component.translatable("message.magneticraft.multiblock_unloaded", coordinates)
                : Component.translatable(
                "message.magneticraft.multiblock_mismatch",
                coordinates,
                result.expected() == null ? "?" : result.expected().name().toLowerCase()
        );
    }

    boolean recipeMatches(ResourceLocation recipeId, int duration) {
        return recipeId.equals(activeRecipe) && totalProgress == duration;
    }

    void startRecipe(ResourceLocation recipeId, int duration) {
        activeRecipe = recipeId;
        progress = 0;
        totalProgress = duration;
        markChanged();
    }

    void advanceProgress() {
        advanceProgress(1);
    }

    void advanceProgress(int amount) {
        progress += Math.max(0, amount);
        markChanged();
    }

    void resetProgress() {
        if (progress != 0 || totalProgress != 0 || activeRecipe != null) {
            progress = 0;
            totalProgress = 0;
            activeRecipe = null;
            markChanged();
        }
    }

    void startBurn(int burnTicks, double burnPower) {
        this.burnTicks = Math.max(0, burnTicks);
        this.burnPower = Math.max(0.0D, burnPower);
        markChanged();
    }

    int consumeBurnWork(int limit) {
        int consumed = Math.min(Math.max(0, limit), burnTicks);
        if (consumed > 0) {
            burnTicks -= consumed;
            markChanged();
        }
        return consumed;
    }

    void setWorking(boolean working) {
        if (this.working != working) {
            this.working = working;
            markChanged();
        }
    }

    @Nullable
    private BulkItemStorageModule createBulkStorage() {
        return definition.bulkItemCapacity() == 0
                ? null
                : addModule(new BulkItemStorageModule(
                Magneticraft.id("advanced_bulk_inventory"),
                this,
                definition.bulkItemCapacity()
        ));
    }

    @Nullable
    private ShelvingStorageModule createShelvingStorage() {
        return definition == MultiblockDefinition.SHELVING_UNIT
                ? addModule(new ShelvingStorageModule(
                Magneticraft.id("advanced_shelving_inventory"),
                this
        ))
                : null;
    }

    @Nullable
    private ItemInventoryModule createInventory() {
        if (definition.inventorySlots() == 0 || definition == MultiblockDefinition.SHELVING_UNIT) {
            return null;
        }
        return addModule(new ItemInventoryModule(
                Magneticraft.id("advanced_inventory"),
                this,
                definition.inventorySlots(),
                this::inventoryItemValid,
                this::inventoryAccess
        ));
    }

    private List<FluidTankModule> createTanks() {
        List<FluidTankModule> created = new ArrayList<>(definition.tankCount());
        for (int index = 0; index < definition.tankCount(); index++) {
            int tankIndex = index;
            MultiblockPortProfile.TankPort port = MultiblockPortProfile.tank(definition, tankIndex);
            created.add(addModule(FluidTankModule.directional(
                    Magneticraft.id("advanced_tank_" + tankIndex),
                    this,
                    port.capacity(),
                    stack -> port.accepts(stack, level instanceof ServerLevel serverLevel ? serverLevel : null),
                    side -> FluidTankModule.TankAccess.NONE
            )));
        }
        return List.copyOf(created);
    }

    @Nullable
    private ElectricalPowerModule createEnergy(@Nullable ElectricalNetworkModule createdElectricity) {
        if (createdElectricity == null) {
            return null;
        }
        return addModule(new ElectricalPowerModule(
                Magneticraft.id("advanced_energy"),
                Magneticraft.id(definition.id()),
                this,
                createdElectricity,
                ElectricalPowerModule.ForgeEnergyAccess.NONE,
                side -> false,
                false
        ));
    }

    @Nullable
    private ElectricalNetworkModule createElectricity() {
        if (!definition.usesElectricity()) {
            return null;
        }
        return addModule(new ElectricalNetworkModule(
                Magneticraft.id("advanced_electricity"),
                this,
                electricalTier(),
                ElectricalNodeKind.MACHINE,
                side -> operational() && MultiblockPortLayout.supports(
                        this, worldPosition, NetworkDomain.ELECTRICITY, side
                )
        ));
    }

    @Nullable
    private HeatNetworkModule createHeat() {
        if (!definition.usesHeat()) {
            return null;
        }
        return addModule(new HeatNetworkModule(
                Magneticraft.id("advanced_heat"),
                this,
                new HeatNode(1.0D, 73.0D),
                Double.MAX_VALUE,
                side -> operational() && MultiblockPortLayout.supports(
                        this, worldPosition, NetworkDomain.HEAT, side
                )
        ));
    }

    private ItemInventoryModule.SlotAccess inventoryAccess(@Nullable Direction side) {
        int slots = definition.inventorySlots();
        if (slots == 0) {
            return ItemInventoryModule.NONE;
        }
        if (side == null) {
            int[] all = allSlots(slots);
            return new ItemInventoryModule.SlotAccess(all, all);
        }
        boolean recoverySide = side == facing().getOpposite();
        return switch (definition) {
            case GRINDER, SIEVE, HYDRAULIC_PRESS, BIG_ELECTRIC_FURNACE ->
                    new ItemInventoryModule.SlotAccess(
                            new int[]{0},
                            recoverySide ? allSlots(slots) : range(1, slots)
                    );
            case BIG_COMBUSTION_CHAMBER -> new ItemInventoryModule.SlotAccess(
                    new int[]{0},
                    recoverySide ? new int[]{0} : new int[0]
            );
            default -> ItemInventoryModule.NONE;
        };
    }

    private boolean inventoryItemValid(int slot, ItemStack stack) {
        if (stack.isEmpty()) {
            return false;
        }
        if (slot > 0) {
            return true;
        }
        if (slot != 0 || !(level instanceof ServerLevel serverLevel)) {
            return false;
        }
        return switch (definition) {
            case GRINDER, SIEVE, HYDRAULIC_PRESS -> serverLevel.getRecipeManager()
                    .getAllRecipesFor(ModRecipeTypes.advancedProcessingType(definition).get())
                    .stream()
                    .anyMatch(recipe -> recipe.input().test(stack));
            case BIG_ELECTRIC_FURNACE -> serverLevel.getRecipeManager()
                    .getRecipeFor(RecipeType.SMELTING, new SimpleContainer(stack), serverLevel)
                    .isPresent();
            case BIG_COMBUSTION_CHAMBER -> ForgeHooks.getBurnTime(stack, RecipeType.SMELTING) > 0;
            default -> false;
        };
    }

    private ResourceLocation electricalTier() {
        return switch (definition) {
            case SOLAR_PANEL -> ElectricalNetworkModule.LOW_VOLTAGE;
            case STEAM_TURBINE -> Magneticraft.id("high_voltage");
            default -> Magneticraft.id("medium_voltage");
        };
    }

    private int tankAmount(int index) {
        FluidTankModule tank = tank(index);
        return tank == null ? 0 : tank.tank().getFluidAmount();
    }

    private int tankCapacity(int index) {
        FluidTankModule tank = tank(index);
        return tank == null ? 0 : tank.tank().getCapacity();
    }

    private void rebindPhysicalModules() {
        unregisterPhysicalModules();
        if (operational()) {
            if (electricity != null) {
                electricity.onLoad();
            }
            if (heat != null) {
                heat.onLoad();
            }
            if (level instanceof ServerLevel serverLevel) {
                externalPortNodes = MultiblockExternalPortService.register(serverLevel, this);
            }
        }
    }

    private void setStructureReady(boolean structureReady) {
        if (this.structureReady == structureReady) {
            return;
        }
        this.structureReady = structureReady;
        refreshCapabilities();
        rebindPhysicalModules();
        if (structureReady && level instanceof ServerLevel serverLevel) {
            logic.onFormed(serverLevel);
        }
        markChangedAndSync();
    }

    private void refreshCapabilities() {
        invalidateCaps();
        reviveCaps();
    }

    private void rebindMemberCapabilities() {
        setMemberCapabilities(!formed);
    }

    private void setMemberCapabilities(boolean enabled) {
        if (!(level instanceof ServerLevel)) {
            return;
        }
        for (MultiblockCell cell : definition.cells()) {
            if (cell.rule() != MultiblockRule.SMALL_TANK) {
                continue;
            }
            BlockPos member = MultiblockTransform.worldPosition(
                    worldPosition, cell.offset(), definition.center(), facing(), mirrored
            );
            if (!level.hasChunk(member.getX() >> 4, member.getZ() >> 4)) {
                continue;
            }
            if (level.getBlockEntity(member) instanceof SingleBlockMachineBlockEntity blockEntity) {
                if (enabled) {
                    blockEntity.releaseMultiblockClaim(worldPosition);
                } else {
                    blockEntity.claimForMultiblock(worldPosition);
                }
            }
        }
    }

    private boolean canClaimMemberCapabilities() {
        if (!(level instanceof ServerLevel)) {
            return false;
        }
        for (MultiblockCell cell : definition.cells()) {
            if (cell.rule() != MultiblockRule.SMALL_TANK) {
                continue;
            }
            BlockPos member = MultiblockTransform.worldPosition(
                    worldPosition, cell.offset(), definition.center(), facing(), mirrored
            );
            if (!level.hasChunk(member.getX() >> 4, member.getZ() >> 4)
                    || (!structureSnapshot.contains(member)
                    && (!(level.getBlockEntity(member) instanceof SingleBlockMachineBlockEntity blockEntity)
                    || !blockEntity.canClaimForMultiblock(worldPosition)))) {
                return false;
            }
        }
        return true;
    }

    private void unregisterPhysicalModules() {
        if (level instanceof ServerLevel serverLevel && !externalPortNodes.isEmpty()) {
            MultiblockExternalPortService.unregister(serverLevel, externalPortNodes);
            externalPortNodes = List.of();
        }
        if (electricity != null) {
            electricity.onUnload();
        }
        if (heat != null) {
            heat.onUnload();
        }
    }

    private static int[] allSlots(int slots) {
        return range(0, slots);
    }

    private static HydraulicPressMode parseHydraulicMode(String value) {
        if (value == null || value.isBlank()) {
            return HydraulicPressMode.LIGHT;
        }
        try {
            return HydraulicPressMode.parse(value);
        } catch (IllegalArgumentException ignored) {
            return HydraulicPressMode.LIGHT;
        }
    }

    private static int[] range(int startInclusive, int endExclusive) {
        int[] result = new int[Math.max(0, endExclusive - startInclusive)];
        for (int index = 0; index < result.length; index++) {
            result[index] = startInclusive + index;
        }
        return result;
    }

    private record PortEndpoint(
            BlockPos position,
            Direction side,
            MultiblockPortLayout.Kind kind
    ) {
        private PortEndpoint {
            position = position.immutable();
        }
    }

    /** Combines the released boiler's water-input and steam-output tanks on one physical face. */
    private static final class PortFluidHandler implements IFluidHandler {
        private final List<IFluidHandler> handlers;

        private PortFluidHandler(List<IFluidHandler> handlers) {
            this.handlers = List.copyOf(handlers);
        }

        @Override
        public int getTanks() {
            return handlers.stream().mapToInt(IFluidHandler::getTanks).sum();
        }

        @Override
        public FluidStack getFluidInTank(int tank) {
            TankView view = tankView(tank);
            return view.handler().getFluidInTank(view.index());
        }

        @Override
        public int getTankCapacity(int tank) {
            TankView view = tankView(tank);
            return view.handler().getTankCapacity(view.index());
        }

        @Override
        public boolean isFluidValid(int tank, FluidStack stack) {
            TankView view = tankView(tank);
            return view.handler().isFluidValid(view.index(), stack);
        }

        @Override
        public int fill(FluidStack resource, FluidAction action) {
            if (resource.isEmpty()) {
                return 0;
            }
            int filled = 0;
            for (IFluidHandler handler : handlers) {
                FluidStack remaining = resource.copy();
                remaining.setAmount(resource.getAmount() - filled);
                filled += handler.fill(remaining, action);
                if (filled >= resource.getAmount()) {
                    break;
                }
            }
            return filled;
        }

        @Override
        public FluidStack drain(FluidStack resource, FluidAction action) {
            if (resource.isEmpty()) {
                return FluidStack.EMPTY;
            }
            FluidStack drained = FluidStack.EMPTY;
            for (IFluidHandler handler : handlers) {
                FluidStack request = resource.copy();
                request.setAmount(resource.getAmount() - drained.getAmount());
                FluidStack part = handler.drain(request, action);
                if (!part.isEmpty()) {
                    if (drained.isEmpty()) {
                        drained = part.copy();
                    } else {
                        drained.grow(part.getAmount());
                    }
                }
                if (drained.getAmount() >= resource.getAmount()) {
                    break;
                }
            }
            return drained;
        }

        @Override
        public FluidStack drain(int maxDrain, FluidAction action) {
            if (maxDrain <= 0) {
                return FluidStack.EMPTY;
            }
            for (IFluidHandler handler : handlers) {
                FluidStack candidate = handler.drain(maxDrain, FluidAction.SIMULATE);
                if (!candidate.isEmpty()) {
                    candidate.setAmount(maxDrain);
                    return drain(candidate, action);
                }
            }
            return FluidStack.EMPTY;
        }

        private TankView tankView(int tank) {
            int localIndex = tank;
            for (IFluidHandler handler : handlers) {
                if (localIndex < handler.getTanks()) {
                    return new TankView(handler, localIndex);
                }
                localIndex -= handler.getTanks();
            }
            throw new IndexOutOfBoundsException("Tank " + tank + " of " + getTanks());
        }

        private record TankView(IFluidHandler handler, int index) {
        }
    }
}
