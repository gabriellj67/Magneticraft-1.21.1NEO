package committee.nova.mods.magneticraft.content.nuclear.facility;

import committee.nova.mods.magneticraft.Magneticraft;
import committee.nova.mods.magneticraft.content.machine.framework.MachineBlockEntity;
import committee.nova.mods.magneticraft.content.machine.framework.menu.Int32ContainerData;
import committee.nova.mods.magneticraft.content.machine.framework.module.ItemInventoryModule;
import committee.nova.mods.magneticraft.init.ModBlockEntities;
import committee.nova.mods.magneticraft.init.ModNuclearBlocks;
import committee.nova.mods.magneticraft.init.ModRecipeTypes;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.items.IItemHandler;
import net.neoforged.neoforge.items.IItemHandlerModifiable;
import net.neoforged.neoforge.items.ItemStackHandler;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/** Authoritative inventory, structure snapshot and transactional process state. */
public final class NuclearFacilityControllerBlockEntity extends MachineBlockEntity implements MenuProvider {
    public static final int INPUT_SLOTS = 4;
    public static final int OUTPUT_SLOTS = 4;
    public static final int TOTAL_SLOTS = INPUT_SLOTS + OUTPUT_SLOTS;
    public static final int MENU_LOGICAL_DATA_COUNT = 12;
    public static final int MENU_DATA_COUNT = MENU_LOGICAL_DATA_COUNT * 2;
    private static final int STRUCTURE_CHECK_INTERVAL = 20;
    private static final String OWNER_TAG = "owner";
    private static final String FORMED_TAG = "formed";
    private static final String DEPTH_TAG = "depth";
    private static final String PROGRESS_TAG = "progress";
    private static final String RECIPE_TAG = "recipe";

    private final NuclearFacilityType facilityType;
    private final ItemInventoryModule inventory;
    private final IItemHandler inputPort;
    private final IItemHandler outputPort;
    private final ContainerData menuData;
    @Nullable
    private UUID owner;
    @Nullable
    private NuclearFacilitySnapshot snapshot;
    private boolean formed;
    private int savedDepth;
    private int progress;
    private boolean working;
    @Nullable
    private ResourceLocation activeRecipe;

    public NuclearFacilityControllerBlockEntity(BlockPos position, BlockState state) {
        this(position, state, facilityType(state));
    }

    private NuclearFacilityControllerBlockEntity(
            BlockPos position, BlockState state, NuclearFacilityType facilityType
    ) {
        super(ModBlockEntities.NUCLEAR_FACILITY_CONTROLLER.get(), position, state);
        this.facilityType = facilityType;
        inventory = addModule(new ItemInventoryModule(
                Magneticraft.id("inventory"), this, TOTAL_SLOTS,
                (slot, stack) -> true,
                side -> ItemInventoryModule.NONE
        ));
        inputPort = inventory.portHandler(new ItemInventoryModule.SlotAccess(
                new int[]{0, 1, 2, 3}, new int[0]));
        outputPort = inventory.portHandler(new ItemInventoryModule.SlotAccess(
                new int[0], new int[]{4, 5, 6, 7}));
        menuData = Int32ContainerData.readOnly(
                () -> formed ? 1 : 0,
                () -> facing().get2DDataValue(),
                () -> facilityType.width(),
                () -> facilityType.height(),
                this::depth,
                this::portFlags,
                () -> progress,
                this::activeDuration,
                () -> working ? 1 : 0,
                this::storedJoules,
                this::capacityJoules,
                () -> facilityType == NuclearFacilityType.CENTRIFUGE_CASCADE ? Math.max(0, (depth() - 2) * 2) : 1
        );
    }

    public static void serverTick(
            Level level, BlockPos position, BlockState state, NuclearFacilityControllerBlockEntity controller
    ) {
        controller.tickModules();
        controller.working = false;
        if (level.getGameTime() % STRUCTURE_CHECK_INTERVAL == 0L) {
            controller.revalidate();
        }
        if (controller.formed) {
            controller.tickProcess();
        } else {
            controller.resetProgress();
        }
        controller.finishServerTick();
    }

    public NuclearFacilityType facilityType() {
        return facilityType;
    }

    public Direction facing() {
        BlockState state = getBlockState();
        return state.hasProperty(NuclearFacilityControllerBlock.FACING)
                ? state.getValue(NuclearFacilityControllerBlock.FACING)
                : Direction.NORTH;
    }

    public boolean formed() {
        return formed;
    }

    public int depth() {
        return snapshot == null ? savedDepth : snapshot.depth();
    }

    public ItemInventoryModule inventory() {
        return inventory;
    }

    public ContainerData menuData() {
        return menuData;
    }

    public void setOwner(UUID owner) {
        this.owner = owner;
        markChanged();
    }

    public boolean canManage(Player player) {
        return player.distanceToSqr(
                worldPosition.getX() + 0.5D,
                worldPosition.getY() + 0.5D,
                worldPosition.getZ() + 0.5D
        ) <= 64.0D && (owner == null || owner.equals(player.getUUID())
                || player instanceof ServerPlayer serverPlayer && serverPlayer.hasPermissions(2));
    }

    public boolean tryForm(ServerPlayer player) {
        NuclearFacilityValidator.Result result = validateStructure();
        if (result.snapshot().isEmpty()) {
            player.displayClientMessage(Component.translatable(
                    "message.magneticraft.nuclear_facility.invalid",
                    Component.translatable("message.magneticraft.nuclear_facility.reason." + result.reason()),
                    result.position().toShortString()
            ), true);
            return false;
        }
        form(result.snapshot().orElseThrow());
        return true;
    }

    public void unform() {
        NuclearFacilitySnapshot previous = snapshot;
        formed = false;
        snapshot = null;
        savedDepth = 0;
        resetProgress();
        updateFormedBlockState(false);
        if (previous != null && level != null) {
            previous.ports().values().forEach(position -> {
                if (level.hasChunkAt(position)
                        && level.getBlockEntity(position) instanceof NuclearFacilityPortBlockEntity port) {
                    port.release(worldPosition);
                }
            });
        }
        markChangedAndSync();
    }

    @Nullable
    public IItemHandler portInventory(NuclearFacilityPartRole role) {
        return switch (role) {
            case ITEM_INPUT -> inputPort;
            case ITEM_OUTPUT -> outputPort;
            default -> null;
        };
    }

    @Override
    public Component getDisplayName() {
        return Component.translatable("block.magneticraft." + facilityType.id());
    }

    @Nullable
    @Override
    public AbstractContainerMenu createMenu(int containerId, Inventory playerInventory, Player player) {
        return canManage(player) ? new NuclearFacilityMenu(containerId, playerInventory, this) : null;
    }

    @Override
    protected void saveMachineData(CompoundTag tag, HolderLookup.Provider registries) {
        if (owner != null) {
            tag.putUUID(OWNER_TAG, owner);
        }
        tag.putBoolean(FORMED_TAG, formed);
        tag.putInt(DEPTH_TAG, depth());
        tag.putInt(PROGRESS_TAG, progress);
        if (activeRecipe != null) {
            tag.putString(RECIPE_TAG, activeRecipe.toString());
        }
    }

    @Override
    protected void loadMachineData(CompoundTag tag, HolderLookup.Provider registries) {
        owner = tag.hasUUID(OWNER_TAG) ? tag.getUUID(OWNER_TAG) : null;
        formed = tag.getBoolean(FORMED_TAG);
        savedDepth = Math.max(0, tag.getInt(DEPTH_TAG));
        snapshot = null;
        progress = Math.max(0, tag.getInt(PROGRESS_TAG));
        activeRecipe = tag.contains(RECIPE_TAG, Tag.TAG_STRING)
                ? ResourceLocation.tryParse(tag.getString(RECIPE_TAG))
                : null;
    }

    private void revalidate() {
        NuclearFacilityValidator.Result result = validateStructure();
        if (result.snapshot().isPresent()) {
            NuclearFacilitySnapshot valid = result.snapshot().orElseThrow();
            if (!formed || snapshot == null || snapshot.depth() != valid.depth()) {
                form(valid);
            }
        } else if (formed) {
            unform();
        }
    }

    private NuclearFacilityValidator.Result validateStructure() {
        if (!(level instanceof ServerLevel serverLevel)) {
            return NuclearFacilityValidator.Result.failure("not_server", worldPosition);
        }
        return NuclearFacilityValidator.validate(facilityType, worldPosition, facing(),
                new NuclearFacilityValidator.CellLookup() {
                    @Override
                    public NuclearFacilityValidator.ObservedCell cellAt(BlockPos position) {
                        return observe(serverLevel.getBlockState(position));
                    }

                    @Override
                    public boolean isLoaded(BlockPos position) {
                        return serverLevel.hasChunkAt(position);
                    }
                });
    }

    private void form(NuclearFacilitySnapshot next) {
        if (snapshot != null && snapshot.depth() != next.depth()) {
            unform();
        }
        snapshot = next;
        savedDepth = next.depth();
        formed = true;
        updateFormedBlockState(true);
        if (level != null) {
            next.ports().values().forEach(position -> {
                if (level.getBlockEntity(position) instanceof NuclearFacilityPortBlockEntity port) {
                    port.claim(worldPosition);
                }
            });
        }
        markChangedAndSync();
    }

    private void updateFormedBlockState(boolean next) {
        if (level == null) {
            return;
        }
        BlockState state = getBlockState();
        if (state.hasProperty(NuclearFacilityControllerBlock.FORMED)
                && state.getValue(NuclearFacilityControllerBlock.FORMED) != next) {
            level.setBlock(worldPosition, state.setValue(NuclearFacilityControllerBlock.FORMED, next),
                    Block.UPDATE_CLIENTS);
        }
    }

    private void tickProcess() {
        Optional<RecipeHolder<NuclearProcessRecipe>> recipeHolder = matchingRecipe();
        if (recipeHolder.isEmpty() || !canAccept(recipeHolder.orElseThrow().value().results())) {
            resetProgress();
            return;
        }
        RecipeHolder<NuclearProcessRecipe> holder = recipeHolder.orElseThrow();
        NuclearProcessRecipe value = holder.value();
        if (!holder.id().equals(activeRecipe)) {
            activeRecipe = holder.id();
            progress = 0;
        }
        NuclearFacilityPortBlockEntity electricalPort = electricalPort();
        if (electricalPort == null) {
            return;
        }
        int processingRate = facilityType == NuclearFacilityType.CENTRIFUGE_CASCADE
                ? Math.max(1, depth() - 2)
                : 1;
        if (progress < value.durationTicks()) {
            int processingUnits = Math.min(processingRate, value.durationTicks() - progress);
            int requiredJoules = Math.multiplyExact(value.joulesPerTick(), processingUnits);
            if (electricalPort.consumeJoules(requiredJoules, true) != requiredJoules) {
                return;
            }
            electricalPort.consumeJoules(requiredJoules, false);
            working = true;
            progress += processingUnits;
        }
        if (progress < value.durationTicks()) {
            markChanged();
            return;
        }
        SimpleContainer inputs = inputContainer();
        Optional<NuclearProcessRecipe.ConsumptionPlan> plan = value.consumptionPlan(inputs);
        if (plan.isEmpty() || !canAccept(value.results())) {
            resetProgress();
            return;
        }
        plan.orElseThrow().apply(inventory.menuHandler());
        insertResults(value.results());
        resetProgress();
        markChangedAndSync();
    }

    private Optional<RecipeHolder<NuclearProcessRecipe>> matchingRecipe() {
        if (level == null) {
            return Optional.empty();
        }
        NuclearProcessRecipe.Input input = new NuclearProcessRecipe.Input(inputContainer());
        return level.getRecipeManager().getAllRecipesFor(ModRecipeTypes.NUCLEAR_PROCESSING_TYPE.get()).stream()
                .filter(holder -> holder.value().facility() == facilityType)
                .filter(holder -> holder.value().matches(input, level))
                .findFirst();
    }

    private SimpleContainer inputContainer() {
        SimpleContainer container = new SimpleContainer(INPUT_SLOTS);
        for (int slot = 0; slot < INPUT_SLOTS; slot++) {
            container.setItem(slot, inventory.getStackInSlot(slot).copy());
        }
        return container;
    }

    private boolean canAccept(List<ItemStack> results) {
        ItemStackHandler simulation = new ItemStackHandler(OUTPUT_SLOTS);
        for (int slot = 0; slot < OUTPUT_SLOTS; slot++) {
            simulation.setStackInSlot(slot, inventory.getStackInSlot(INPUT_SLOTS + slot).copy());
        }
        for (ItemStack result : results) {
            ItemStack remainder = insertRange(simulation, result.copy(), 0, OUTPUT_SLOTS);
            if (!remainder.isEmpty()) {
                return false;
            }
        }
        return true;
    }

    private void insertResults(List<ItemStack> results) {
        IItemHandlerModifiable handler = inventory.menuHandler();
        for (ItemStack result : results) {
            ItemStack remainder = insertRange(handler, result.copy(), INPUT_SLOTS, TOTAL_SLOTS);
            if (!remainder.isEmpty()) {
                throw new IllegalStateException("Validated nuclear recipe output no longer fits");
            }
        }
    }

    private static ItemStack insertRange(
            IItemHandler handler, ItemStack stack, int firstSlot, int endSlot
    ) {
        ItemStack remainder = stack;
        for (int slot = firstSlot; slot < endSlot && !remainder.isEmpty(); slot++) {
            remainder = handler.insertItem(slot, remainder, false);
        }
        return remainder;
    }

    @Nullable
    private NuclearFacilityPortBlockEntity electricalPort() {
        if (level == null || snapshot == null) {
            return null;
        }
        BlockPos position = snapshot.ports().get(NuclearFacilityPartRole.ELECTRICAL);
        return position != null && level.getBlockEntity(position) instanceof NuclearFacilityPortBlockEntity port
                && port.claimedBy(worldPosition) ? port : null;
    }

    private int storedJoules() {
        NuclearFacilityPortBlockEntity port = electricalPort();
        return port == null ? 0 : port.storedJoules();
    }

    private int capacityJoules() {
        NuclearFacilityPortBlockEntity port = electricalPort();
        return port == null ? 0 : port.capacityJoules();
    }

    private int activeDuration() {
        if (activeRecipe == null || level == null) {
            return 0;
        }
        return level.getRecipeManager().byKey(activeRecipe)
                .map(RecipeHolder::value)
                .filter(NuclearProcessRecipe.class::isInstance)
                .map(NuclearProcessRecipe.class::cast)
                .map(NuclearProcessRecipe::durationTicks)
                .orElse(0);
    }

    private int portFlags() {
        if (snapshot == null) {
            return 0;
        }
        int flags = 0;
        flags |= snapshot.ports().containsKey(NuclearFacilityPartRole.ITEM_INPUT) ? 1 : 0;
        flags |= snapshot.ports().containsKey(NuclearFacilityPartRole.ITEM_OUTPUT) ? 2 : 0;
        flags |= snapshot.ports().containsKey(NuclearFacilityPartRole.ELECTRICAL) ? 4 : 0;
        return flags;
    }

    private void resetProgress() {
        if (progress != 0 || activeRecipe != null) {
            progress = 0;
            activeRecipe = null;
            markChanged();
        }
    }

    @Nullable
    private NuclearFacilityValidator.ObservedCell observe(BlockState state) {
        Block block = state.getBlock();
        if (block == ModNuclearBlocks.controller(facilityType).get()) {
            return new NuclearFacilityValidator.ObservedCell(NuclearFacilityPartRole.CONTROLLER, facing());
        }
        if (block == ModNuclearBlocks.FACILITY_CASING.get()) {
            return new NuclearFacilityValidator.ObservedCell(NuclearFacilityPartRole.CASING, null);
        }
        if (block == ModNuclearBlocks.PROCESS_CORE.get()) {
            return new NuclearFacilityValidator.ObservedCell(NuclearFacilityPartRole.PROCESS_CORE, null);
        }
        if (block == ModNuclearBlocks.CENTRIFUGE_STAGE.get()) {
            return new NuclearFacilityValidator.ObservedCell(NuclearFacilityPartRole.CENTRIFUGE_STAGE, null);
        }
        if (block instanceof NuclearFacilityPortBlock port) {
            return new NuclearFacilityValidator.ObservedCell(port.role(), state.getValue(NuclearFacilityPortBlock.FACING));
        }
        return null;
    }

    private static NuclearFacilityType facilityType(BlockState state) {
        if (state.getBlock() instanceof NuclearFacilityControllerBlock controller) {
            return controller.facilityType();
        }
        throw new IllegalArgumentException("Nuclear controller entity requires a controller block");
    }
}
