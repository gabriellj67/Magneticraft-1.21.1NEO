package committee.nova.mods.magneticraft.content.machine.singleblock;

import committee.nova.mods.magneticraft.Magneticraft;
import committee.nova.mods.magneticraft.content.fluid.FluidDefinition;
import committee.nova.mods.magneticraft.content.machine.framework.MachineBlockEntity;
import committee.nova.mods.magneticraft.content.machine.framework.menu.Int32ContainerData;
import committee.nova.mods.magneticraft.content.machine.framework.module.EnergyStorageModule;
import committee.nova.mods.magneticraft.content.machine.framework.module.FluidTankModule;
import committee.nova.mods.magneticraft.content.machine.framework.module.GhostFilterModule;
import committee.nova.mods.magneticraft.content.machine.framework.module.ItemInventoryModule;
import committee.nova.mods.magneticraft.content.network.module.ElectricalEnergyBridgeModule;
import committee.nova.mods.magneticraft.content.network.module.ElectricalNetworkModule;
import committee.nova.mods.magneticraft.content.network.module.HeatNetworkModule;
import committee.nova.mods.magneticraft.content.multiblock.AdvancedMultiblockBlockEntity;
import committee.nova.mods.magneticraft.init.ModBlockEntities;
import committee.nova.mods.magneticraft.init.ModFluids;
import committee.nova.mods.magneticraft.system.network.electric.ElectricalNode;
import committee.nova.mods.magneticraft.system.network.heat.HeatNode;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.util.LazyOptional;
import org.jetbrains.annotations.Nullable;

/**
 * Module, capability and persistence host for the Task 5 single-block behavior strategies.
 */
public final class SingleBlockMachineBlockEntity extends MachineBlockEntity implements MenuProvider {
    public static final int MENU_LOGICAL_DATA_COUNT = 12;
    public static final int MENU_DATA_COUNT = MENU_LOGICAL_DATA_COUNT * 2;
    public static final int SLUICE_MAX_ITEMS = 10;
    public static final int SLUICE_DURATION = 80;
    private static final String MULTIBLOCK_CONTROLLER_TAG = "multiblock_controller";

    private final SingleBlockMachineDefinition definition;
    @Nullable
    private final ItemInventoryModule inventory;
    @Nullable
    private final GhostFilterModule filters;
    @Nullable
    private final PneumaticEndpointModule pneumaticEndpoint;
    @Nullable
    private final FluidTankModule primaryTank;
    @Nullable
    private final FluidTankModule secondaryTank;
    @Nullable
    private final EnergyStorageModule energy;
    @Nullable
    private final ElectricalNetworkModule electricity;
    @Nullable
    private final HeatNetworkModule heat;
    private final ContainerData menuData;
    private final SingleBlockMachineState state = new SingleBlockMachineState();
    private final SingleBlockMachineLogic logic;
    private final SingleBlockMachineInteractions interactions;
    private final SingleBlockFabricator fabricator;
    @Nullable
    private BlockPos multiblockController;

    public SingleBlockMachineBlockEntity(BlockPos position, BlockState state) {
        super(ModBlockEntities.SINGLE_BLOCK_MACHINE.get(), position, state);
        if (!(state.getBlock() instanceof SingleBlockMachineBlock block)) {
            throw new IllegalArgumentException("Single-block machine entity attached to " + state.getBlock());
        }
        definition = block.definition();
        filters = definition.ghostSlots() == 0
                ? null
                : addModule(new GhostFilterModule(Magneticraft.id("filters"), this, definition.ghostSlots()));
        pneumaticEndpoint = switch (definition) {
            case RELAY, FILTER, TRANSPOSER -> addModule(new PneumaticEndpointModule(
                    Magneticraft.id("pneumatic_endpoint"),
                    this,
                    definition == SingleBlockMachineDefinition.FILTER,
                    () -> facing().getOpposite(),
                    stack -> SingleBlockMachineSupport.filterAllows(this, this.state, stack, true)
            ));
            default -> null;
        };
        inventory = definition.inventorySlots() == 0
                ? null
                : addModule(new ItemInventoryModule(
                        Magneticraft.id("inventory"),
                        this,
                        definition.inventorySlots(),
                        this::isInventoryItemValid,
                        this::inventoryAccess
                ));

        FluidTankModule[] tanks = createFluidTanks();
        primaryTank = tanks[0];
        secondaryTank = tanks[1];

        EnergyStorageModule createdEnergy = createEnergyStorage();
        energy = createdEnergy;
        ElectricalNetworkModule createdElectricity = createElectricalNetwork();
        electricity = createdElectricity;
        heat = createHeatNetwork();
        addElectricalBridge(createdEnergy, createdElectricity);
        logic = new SingleBlockMachineLogic(this, this.state);
        interactions = new SingleBlockMachineInteractions(this, this.state);
        fabricator = new SingleBlockFabricator(this);
        menuData = Int32ContainerData.readOnly(
                () -> energy == null ? 0 : energy.getEnergyStored(),
                () -> energy == null ? 0 : energy.getMaxEnergyStored(),
                () -> this.state.progress,
                () -> this.state.totalProgress,
                () -> heat == null ? 0 : (int) Math.round(heat.node().temperatureKelvin() * 10.0D),
                () -> primaryTank == null ? 0 : primaryTank.tank().getFluidAmount(),
                () -> primaryTank == null ? 0 : primaryTank.tank().getCapacity(),
                () -> secondaryTank == null ? 0 : secondaryTank.tank().getFluidAmount(),
                () -> secondaryTank == null ? 0 : secondaryTank.tank().getCapacity(),
                () -> definition == SingleBlockMachineDefinition.INSERTER
                        ? inserterFlags()
                        : this.state.doorOpen ? 1 : 0,
                () -> electricity == null ? 0 : (int) Math.round(electricity.node().voltage() * 10.0D),
                () -> (int) Math.round(this.state.thermopileFlux)
        );
    }

    public static void serverTick(
            Level level,
            BlockPos position,
            BlockState state,
            SingleBlockMachineBlockEntity machine
    ) {
        if (machine.multiblockController != null) {
            ServerLevel serverLevel = (ServerLevel) level;
            BlockPos controller = machine.multiblockController;
            if (!serverLevel.hasChunk(controller.getX() >> 4, controller.getZ() >> 4)) {
                return;
            }
            if (serverLevel.getBlockEntity(controller) instanceof AdvancedMultiblockBlockEntity multiblock
                    && multiblock.formed()
                    && multiblock.members().contains(position)) {
                return;
            }
            machine.releaseMultiblockClaim(controller);
        }
        machine.tickModules();
        machine.logic.tick((ServerLevel) level);
        boolean shouldBeLit = machine.definition.hasLitState() && machine.logic.isVisuallyLit();
        if (state.getValue(SingleBlockMachineBlock.LIT) != shouldBeLit) {
            level.setBlock(position, state.setValue(SingleBlockMachineBlock.LIT, shouldBeLit), Block.UPDATE_ALL);
        }
        machine.syncClientState(visualStateHash(machine.state.progress, machine.state.totalProgress, machine.state.working));
        machine.finishServerTick();
    }

    public SingleBlockMachineDefinition definition() {
        return definition;
    }

    @Nullable
    public ItemInventoryModule inventory() {
        return inventory;
    }

    @Nullable
    public GhostFilterModule filters() {
        return filters;
    }

    @Nullable
    PneumaticEndpointModule pneumaticEndpoint() {
        return pneumaticEndpoint;
    }

    @Nullable
    public FluidTankModule primaryTank() {
        return primaryTank;
    }

    @Nullable
    public FluidTankModule secondaryTank() {
        return secondaryTank;
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

    public ContainerData menuData() {
        return menuData;
    }

    public int progress() {
        return state.progress;
    }

    public int totalProgress() {
        return state.totalProgress;
    }

    public boolean working() {
        return state.working;
    }

    public int inserterFlags() {
        return state.inserterFlags();
    }

    public void toggleInserterFlag(int flag) {
        if (state.toggleInserterFlag(flag)) {
            markChangedAndSync();
        }
    }

    public InteractionResult interact(Player player, InteractionHand hand, BlockHitResult hit) {
        if (multiblockController != null) {
            return InteractionResult.CONSUME;
        }
        return interactions.interact(player, hand);
    }

    public boolean claimedByMultiblock() {
        return multiblockController != null;
    }

    public void claimForMultiblock(BlockPos controller) {
        if (definition != SingleBlockMachineDefinition.SMALL_TANK) {
            throw new IllegalStateException("Only small tanks can be multiblock members");
        }
        BlockPos stable = controller.immutable();
        if (!stable.equals(multiblockController)) {
            multiblockController = stable;
            invalidateCaps();
            markChangedAndSync();
        }
    }

    public void releaseMultiblockClaim(BlockPos controller) {
        if (multiblockController != null && multiblockController.equals(controller)) {
            multiblockController = null;
            reviveCaps();
            markChangedAndSync();
        }
    }

    @Override
    public <T> LazyOptional<T> getCapability(Capability<T> capability, @Nullable Direction side) {
        return multiblockController == null
                ? super.getCapability(capability, side)
                : LazyOptional.empty();
    }

    public void onBroken() {
        if (getLevel() instanceof ServerLevel level) {
            logic.onBroken(level);
        }
    }

    public void dropContents(Level level) {
        onBroken();
        if (pneumaticEndpoint != null) {
            for (ItemStack stack : pneumaticEndpoint.removeAllItems()) {
                dropStack(level, stack);
            }
        }
        if (inventory != null) {
            for (int slot = 0; slot < inventory.slots(); slot++) {
                dropStack(level, inventory.extractInternal(slot, Integer.MAX_VALUE, false));
            }
        }
    }

    private void dropStack(Level level, ItemStack stack) {
        if (!stack.isEmpty()) {
            net.minecraft.world.Containers.dropItemStack(
                    level,
                    worldPosition.getX() + 0.5D,
                    worldPosition.getY() + 0.5D,
                    worldPosition.getZ() + 0.5D,
                    stack
            );
        }
    }

    public void saveTankToItem(ItemStack stack) {
        CompoundTag blockEntityTag = saveWithFullMetadata();
        blockEntityTag.remove("id");
        blockEntityTag.remove("x");
        blockEntityTag.remove("y");
        blockEntityTag.remove("z");
        blockEntityTag.remove(MULTIBLOCK_CONTROLLER_TAG);
        stack.addTagElement("BlockEntityTag", blockEntityTag);
    }

    @Override
    public Component getDisplayName() {
        return Component.translatable("container.magneticraft." + definition.id());
    }

    @Nullable
    @Override
    public AbstractContainerMenu createMenu(int containerId, Inventory playerInventory, Player player) {
        return new SingleBlockMachineMenu(containerId, playerInventory, this);
    }

    public ItemStack fabricatorResult() {
        return fabricator.result();
    }

    public boolean canCraftFabricator() {
        return fabricator.canCraft();
    }

    public boolean craftFabricator() {
        return fabricator.craft();
    }

    @Override
    protected void saveMachineData(CompoundTag tag) {
        state.save(tag);
        if (multiblockController != null) {
            tag.putLong(MULTIBLOCK_CONTROLLER_TAG, multiblockController.asLong());
        }
    }

    @Override
    protected void loadMachineData(CompoundTag tag) {
        state.load(tag);
        multiblockController = tag.contains(MULTIBLOCK_CONTROLLER_TAG)
                ? BlockPos.of(tag.getLong(MULTIBLOCK_CONTROLLER_TAG))
                : null;
    }

    @Override
    protected void saveClientData(CompoundTag tag) {
        state.save(tag);
    }

    @Override
    protected void loadClientData(CompoundTag tag) {
        state.load(tag);
    }

    private static int visualStateHash(int progress, int totalProgress, boolean working) {
        return 31 * (31 * progress + totalProgress) + (working ? 1 : 0);
    }

    private FluidTankModule[] createFluidTanks() {
        FluidTankModule primary = null;
        FluidTankModule secondary = null;
        switch (definition) {
            case SMALL_TANK -> primary = addModule(tank(
                    "fluid",
                    32_000,
                    fluid -> true,
                    side -> FluidTankModule.TankAccess.BOTH
            ));
            case WATER_GENERATOR -> primary = addModule(tank(
                    "water",
                    32_000,
                    fluid -> fluid.getFluid().defaultFluidState().is(FluidTags.WATER),
                    side -> FluidTankModule.TankAccess.OUTPUT
            ));
            case STEAM_BOILER -> {
                primary = addModule(tank(
                        "water",
                        1_000,
                        fluid -> fluid.getFluid().defaultFluidState().is(FluidTags.WATER),
                        side -> side == Direction.UP
                                ? FluidTankModule.TankAccess.NONE
                                : FluidTankModule.TankAccess.INPUT
                ));
                secondary = addModule(tank(
                        "steam",
                        16_000,
                        fluid -> fluid.getFluid() == ModFluids.get(FluidDefinition.STEAM).source().get(),
                        side -> side == Direction.UP
                                ? FluidTankModule.TankAccess.OUTPUT
                                : FluidTankModule.TankAccess.NONE
                ));
            }
            case GASIFICATION_UNIT -> primary = addModule(tank(
                    "wood_gas",
                    4_000,
                    fluid -> fluid.getFluid() == ModFluids.get(FluidDefinition.WOOD_GAS).source().get(),
                    side -> FluidTankModule.TankAccess.OUTPUT
            ));
            default -> {
            }
        }
        return new FluidTankModule[]{primary, secondary};
    }

    private FluidTankModule tank(
            String id,
            int capacity,
            java.util.function.Predicate<FluidStack> validator,
            java.util.function.Function<Direction, FluidTankModule.TankAccess> access
    ) {
        return FluidTankModule.directional(Magneticraft.id(id), this, capacity, validator, access);
    }

    @Nullable
    private EnergyStorageModule createEnergyStorage() {
        return switch (definition) {
            case ELECTRIC_HEATER -> addModule(new EnergyStorageModule(
                    Magneticraft.id("energy_storage"), this, 10_000, 640, 80, side -> false, false, false
            ));
            case RF_HEATER -> addModule(new EnergyStorageModule(
                    Magneticraft.id("energy_storage"), this, 80_000, 80, 80, side -> true, true, false
            ));
            case RF_TRANSFORMER -> addModule(new EnergyStorageModule(
                    Magneticraft.id("energy_storage"), this, 80_000, 100, 100, side -> true, true, false
            ));
            case ELECTRIC_ENGINE -> addModule(new EnergyStorageModule(
                    Magneticraft.id("energy_storage"),
                    this,
                    80_000,
                    1_000,
                    1_000,
                    side -> side == null || side == facing().getOpposite(),
                    false,
                    true
            ));
            default -> null;
        };
    }

    @Nullable
    private ElectricalNetworkModule createElectricalNetwork() {
        return switch (definition) {
            case ELECTRIC_HEATER, INFINITE_ENERGY, AIRLOCK, THERMOPILE, RF_TRANSFORMER, ELECTRIC_ENGINE ->
                    addModule(new ElectricalNetworkModule(
                            Magneticraft.id("electricity"),
                            this,
                            new ElectricalNode(1.0D, 125.0D, 0.001D),
                            side -> true
                    ));
            default -> null;
        };
    }

    @Nullable
    private HeatNetworkModule createHeatNetwork() {
        return switch (definition) {
            case COMBUSTION_CHAMBER -> addModule(new HeatNetworkModule(
                    Magneticraft.id("heat"), this, new HeatNode(1.0D, 73.0D), Double.MAX_VALUE,
                    side -> side == Direction.UP
            ));
            case ELECTRIC_HEATER, RF_HEATER -> addModule(new HeatNetworkModule(
                    Magneticraft.id("heat"), this, new HeatNode(1.0D, 73.0D), Double.MAX_VALUE,
                    side -> side != null && side.getAxis() == Direction.Axis.Y
            ));
            case STEAM_BOILER, GASIFICATION_UNIT, BRICK_FURNACE -> addModule(new HeatNetworkModule(
                    Magneticraft.id("heat"), this, new HeatNode(1.0D, 73.0D), Double.MAX_VALUE, side -> true
            ));
            default -> null;
        };
    }

    private void addElectricalBridge(
            @Nullable EnergyStorageModule createdEnergy,
            @Nullable ElectricalNetworkModule createdElectricity
    ) {
        if (createdEnergy == null || createdElectricity == null) {
            return;
        }
        switch (definition) {
            case ELECTRIC_HEATER -> addModule(new ElectricalEnergyBridgeModule(
                    Magneticraft.id("electricity_bridge"), createdElectricity, createdEnergy,
                    60.0D, 60.0D, 640
            ));
            case RF_TRANSFORMER -> addModule(new ElectricalEnergyBridgeModule(
                    Magneticraft.id("electricity_bridge"), createdElectricity, createdEnergy,
                    126.0D, 120.0D, 100
            ));
            case ELECTRIC_ENGINE -> addModule(new ElectricalEnergyBridgeModule(
                    Magneticraft.id("electricity_bridge"), createdElectricity, createdEnergy,
                    60.0D, 60.0D, 1_000
            ));
            default -> {
            }
        }
    }

    private boolean isInventoryItemValid(int slot, ItemStack stack) {
        return switch (definition) {
            case SLUICE_BOX -> slot == 0 && SingleBlockMachineSupport.findSluiceRecipe(this, stack).isPresent();
            case FEEDING_TROUGH -> slot == 0 && SingleBlockMachineSupport.isTroughFood(stack);
            case INSERTER -> (slot == 0 && !SingleBlockMachineSupport.isInserterUpgrade(stack))
                    || (slot >= 1 && slot <= 2 && SingleBlockMachineSupport.isInserterUpgrade(stack));
            case FILTER -> slot == 0 && SingleBlockMachineSupport.filterAllows(this, state, stack, true);
            case COMBUSTION_CHAMBER -> slot == 0 && SingleBlockMachineSupport.isCombustionFuel(stack);
            case GASIFICATION_UNIT -> slot == 0
                    && SingleBlockMachineSupport.findGasificationRecipe(this, stack).isPresent();
            case BRICK_FURNACE -> slot == 0
                    && SingleBlockMachineSupport.findSmeltingRecipe(this, stack).isPresent();
            default -> true;
        };
    }

    @Nullable
    private ItemInventoryModule.SlotAccess inventoryAccess(@Nullable Direction side) {
        int[] all = allSlots(definition.inventorySlots());
        return switch (definition) {
            case SLUICE_BOX, FEEDING_TROUGH, INSERTER, FILTER -> ItemInventoryModule.NONE;
            case RELAY -> side == facing()
                    ? new ItemInventoryModule.SlotAccess(new int[0], new int[0])
                    : new ItemInventoryModule.SlotAccess(all, all);
            case GASIFICATION_UNIT, BRICK_FURNACE -> new ItemInventoryModule.SlotAccess(new int[]{0}, new int[]{1});
            default -> new ItemInventoryModule.SlotAccess(all, all);
        };
    }

    private static int[] allSlots(int slots) {
        int[] result = new int[slots];
        for (int slot = 0; slot < slots; slot++) {
            result[slot] = slot;
        }
        return result;
    }

    private Direction facing() {
        return getBlockState().getValue(SingleBlockMachineBlock.FACING);
    }

    void activateSluiceChain() {
        logic.activateSluiceChain();
    }

}
