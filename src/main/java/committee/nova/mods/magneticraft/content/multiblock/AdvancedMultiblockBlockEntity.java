package committee.nova.mods.magneticraft.content.multiblock;

import committee.nova.mods.magneticraft.Magneticraft;
import committee.nova.mods.magneticraft.content.machine.framework.MachineBlockEntity;
import committee.nova.mods.magneticraft.content.machine.framework.module.EnergyStorageModule;
import committee.nova.mods.magneticraft.content.machine.framework.module.BulkItemStorageModule;
import committee.nova.mods.magneticraft.content.machine.framework.module.FluidTankModule;
import committee.nova.mods.magneticraft.content.machine.framework.module.ItemInventoryModule;
import committee.nova.mods.magneticraft.content.machine.singleblock.SingleBlockMachineBlockEntity;
import committee.nova.mods.magneticraft.content.network.module.ElectricalEnergyBridgeModule;
import committee.nova.mods.magneticraft.content.network.module.ElectricalNetworkModule;
import committee.nova.mods.magneticraft.content.network.module.HeatNetworkModule;
import committee.nova.mods.magneticraft.init.ModBlockEntities;
import committee.nova.mods.magneticraft.init.ModRecipeTypes;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.common.ForgeHooks;
import net.minecraftforge.common.util.LazyOptional;
import committee.nova.mods.magneticraft.system.network.electric.ElectricalNode;
import committee.nova.mods.magneticraft.system.network.heat.HeatNode;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Server-authoritative structure lifecycle host. Machine modules are added by
 * the advanced behavior layer without changing formation semantics.
 */
public final class AdvancedMultiblockBlockEntity extends MachineBlockEntity {
    private static final String FORMED_TAG = "formed";
    private static final String MIRRORED_TAG = "mirrored";
    private static final String OWNER_TAG = "owner";
    private static final String PROGRESS_TAG = "progress";
    private static final String TOTAL_PROGRESS_TAG = "total_progress";
    private static final String ACTIVE_RECIPE_TAG = "active_recipe";
    private static final String BURN_TICKS_TAG = "burn_ticks";
    private static final String BURN_POWER_TAG = "burn_power";
    private static final String SOLAR_TOWER_TAG = "solar_tower";
    private static final String HYDRAULIC_MODE_TAG = "hydraulic_mode";
    private static final int VALIDATION_INTERVAL = 20;

    private final MultiblockDefinition definition;
    @Nullable
    private final BulkItemStorageModule bulkStorage;
    @Nullable
    private final ShelvingStorageModule shelvingStorage;
    @Nullable
    private final ItemInventoryModule inventory;
    private final List<FluidTankModule> tanks;
    @Nullable
    private final EnergyStorageModule energy;
    @Nullable
    private final ElectricalNetworkModule electricity;
    @Nullable
    private final HeatNetworkModule heat;
    private final AdvancedMultiblockLogic logic;
    private boolean formed;
    private boolean structureReady;
    private boolean mirrored;
    @Nullable
    private UUID owner;
    private int validationDelay;
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
        super(ModBlockEntities.ADVANCED_MULTIBLOCK.get(), position, state);
        if (!(state.getBlock() instanceof AdvancedMultiblockBlock controller)) {
            throw new IllegalArgumentException("Advanced multiblock entity attached to " + state.getBlock());
        }
        definition = controller.definition();
        formed = state.getValue(AdvancedMultiblockBlock.FORMED);
        bulkStorage = createBulkStorage();
        shelvingStorage = createShelvingStorage();
        inventory = createInventory();
        tanks = createTanks();
        energy = createEnergy();
        electricity = createElectricity();
        heat = createHeat();
        if (energy != null && electricity != null) {
            boolean generator = isElectricalGenerator();
            addModule(new ElectricalEnergyBridgeModule(
                    Magneticraft.id("advanced_electricity_bridge"),
                    this,
                    electricity,
                    energy,
                    generator ? 120.0D : 60.0D,
                    60.0D,
                    energyTransferRate(),
                    false
            ));
        }
        logic = new AdvancedMultiblockLogic(this);
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

    public boolean operational() {
        return formed && structureReady;
    }

    public boolean mirrored() {
        return mirrored;
    }

    public Direction facing() {
        return getBlockState().getValue(AdvancedMultiblockBlock.FACING);
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
    public EnergyStorageModule energy() {
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

    public void toggleMirrored(Player player) {
        if (formed || !canManage(player)) {
            return;
        }
        mirrored = !mirrored;
        markChangedAndSync();
        player.displayClientMessage(Component.translatable(
                mirrored
                        ? "message.magneticraft.multiblock_mirrored"
                        : "message.magneticraft.multiblock_not_mirrored"
        ), true);
    }

    public boolean tryForm(Player player) {
        if (formed || !canManage(player) || !(level instanceof ServerLevel serverLevel)) {
            return false;
        }
        MultiblockValidationResult result = validate();
        if (!result.valid()) {
            player.displayClientMessage(validationMessage(result), false);
            return false;
        }
        List<BlockPos> members = members();
        if (!MultiblockMembershipService.register(serverLevel, worldPosition, members)) {
            player.displayClientMessage(Component.translatable(
                    "message.magneticraft.multiblock_member_conflict"
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
        if (level instanceof ServerLevel serverLevel) {
            MultiblockMembershipService.unregister(serverLevel, worldPosition);
        }
        if (formed) {
            setFormed(false);
        }
    }

    public void onMemberBroken(BlockPos member) {
        if (!member.equals(worldPosition)) {
            validationDelay = 1;
        }
    }

    public void saveToItem(ItemStack stack) {
        CompoundTag blockEntityTag = saveWithFullMetadata();
        blockEntityTag.remove("id");
        blockEntityTag.remove("x");
        blockEntityTag.remove("y");
        blockEntityTag.remove("z");
        blockEntityTag.putBoolean(FORMED_TAG, false);
        blockEntityTag.remove(OWNER_TAG);
        blockEntityTag.remove(SOLAR_TOWER_TAG);
        if (definition == MultiblockDefinition.SHELVING_UNIT) {
            CompoundTag modules = blockEntityTag.getCompound(MachineBlockEntity.MODULES_TAG);
            modules.remove(Magneticraft.id("advanced_shelving_inventory").toString());
            blockEntityTag.put(MachineBlockEntity.MODULES_TAG, modules);
        }
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
        return (energy != null && energy.getEnergyStored() > 0)
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
        return super.getCapability(capability, side);
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

    @Override
    public void onLoad() {
        super.onLoad();
        validationDelay = 1;
        structureReady = false;
        rebindPhysicalModules();
        rebindMemberCapabilities();
    }

    @Override
    public void setRemoved() {
        if (level instanceof ServerLevel serverLevel) {
            MultiblockMembershipService.unregister(serverLevel, worldPosition);
        }
        super.setRemoved();
    }

    @Override
    protected void saveMachineData(CompoundTag tag) {
        tag.putBoolean(FORMED_TAG, formed);
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
    }

    @Override
    protected void loadMachineData(CompoundTag tag) {
        formed = tag.getBoolean(FORMED_TAG);
        structureReady = false;
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
    }

    @Override
    protected void saveClientData(CompoundTag tag) {
        tag.putBoolean(FORMED_TAG, formed);
        tag.putBoolean(MIRRORED_TAG, mirrored);
        if (solarTowerPosition != null) {
            tag.putLong(SOLAR_TOWER_TAG, solarTowerPosition.asLong());
        }
        tag.putString(HYDRAULIC_MODE_TAG, hydraulicPressMode.serializedName());
    }

    @Override
    protected void loadClientData(CompoundTag tag) {
        formed = tag.getBoolean(FORMED_TAG);
        mirrored = tag.getBoolean(MIRRORED_TAG);
        solarTowerPosition = tag.contains(SOLAR_TOWER_TAG)
                ? BlockPos.of(tag.getLong(SOLAR_TOWER_TAG))
                : null;
        hydraulicPressMode = parseHydraulicMode(tag.getString(HYDRAULIC_MODE_TAG));
    }

    private void serverTick(ServerLevel serverLevel) {
        if (!formed) {
            return;
        }
        if (--validationDelay <= 0) {
            validationDelay = VALIDATION_INTERVAL;
            MultiblockValidationResult result = validate();
            if (result.status() == MultiblockValidationResult.Status.MISMATCH) {
                unform();
                return;
            }
            if (result.status() == MultiblockValidationResult.Status.UNLOADED) {
                setStructureReady(false);
                return;
            }
            if (!MultiblockMembershipService.register(serverLevel, worldPosition, members())) {
                unform();
                return;
            }
            setStructureReady(true);
            rebindMemberCapabilities();
        }
        if (structureReady) {
            tickModules();
            logic.tick(serverLevel);
        }
    }

    private void setFormed(boolean formed) {
        if (this.formed == formed) {
            return;
        }
        this.formed = formed;
        structureReady = formed;
        validationDelay = VALIDATION_INTERVAL;
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
                    side -> port.access(side, facing())
            )));
        }
        return List.copyOf(created);
    }

    @Nullable
    private EnergyStorageModule createEnergy() {
        if (!definition.usesElectricity()) {
            return null;
        }
        boolean generator = isElectricalGenerator();
        return addModule(new EnergyStorageModule(
                Magneticraft.id("advanced_energy"),
                this,
                energyCapacity(),
                energyTransferRate(),
                energyTransferRate(),
                side -> true,
                !generator,
                generator
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
                new ElectricalNode(1.0D, 125.0D, 0.001D),
                0.001D,
                16.0D,
                side -> operational()
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
                side -> operational()
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
                    .getAllRecipesFor(ModRecipeTypes.ADVANCED_PROCESSING_TYPE.get())
                    .stream()
                    .anyMatch(recipe -> recipe.machine() == definition && recipe.input().test(stack));
            case BIG_ELECTRIC_FURNACE -> serverLevel.getRecipeManager()
                    .getRecipeFor(RecipeType.SMELTING, new SimpleContainer(stack), serverLevel)
                    .isPresent();
            case BIG_COMBUSTION_CHAMBER -> ForgeHooks.getBurnTime(stack, RecipeType.SMELTING) > 0;
            default -> false;
        };
    }

    private boolean isElectricalGenerator() {
        return definition == MultiblockDefinition.SOLAR_PANEL
                || definition == MultiblockDefinition.STEAM_ENGINE
                || definition == MultiblockDefinition.STEAM_TURBINE;
    }

    private int energyCapacity() {
        return switch (definition) {
            case STEAM_ENGINE, STEAM_TURBINE -> 80_000;
            default -> 10_000;
        };
    }

    private int energyTransferRate() {
        return switch (definition) {
            case SOLAR_PANEL -> 100;
            case STEAM_ENGINE -> 240;
            case STEAM_TURBINE -> 1_200;
            case GRINDER, SIEVE, PUMPJACK -> 40;
            case HYDRAULIC_PRESS -> 60;
            case BIG_ELECTRIC_FURNACE -> 200;
            default -> 0;
        };
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

    private void unregisterPhysicalModules() {
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
}
