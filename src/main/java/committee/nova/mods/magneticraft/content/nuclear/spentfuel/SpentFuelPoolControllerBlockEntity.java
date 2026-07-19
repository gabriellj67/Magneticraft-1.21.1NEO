package committee.nova.mods.magneticraft.content.nuclear.spentfuel;

import committee.nova.mods.magneticraft.Magneticraft;
import committee.nova.mods.magneticraft.api.nuclear.radiation.RadiationSource;
import committee.nova.mods.magneticraft.api.nuclear.structure.VariableNuclearStructureValidator;
import committee.nova.mods.magneticraft.content.machine.framework.MachineBlockEntity;
import committee.nova.mods.magneticraft.content.machine.framework.menu.Int32ContainerData;
import committee.nova.mods.magneticraft.content.machine.framework.module.ItemInventoryModule;
import committee.nova.mods.magneticraft.content.nuclear.NuclearMultiblockBounds;
import committee.nova.mods.magneticraft.content.nuclear.fuel.FuelAssemblyItem;
import committee.nova.mods.magneticraft.content.nuclear.fuel.FuelAssemblyState;
import committee.nova.mods.magneticraft.content.nuclear.structure.NuclearStructureState;
import committee.nova.mods.magneticraft.init.ModBlockEntities;
import committee.nova.mods.magneticraft.init.ModMenus;
import committee.nova.mods.magneticraft.init.ModNuclearBlocks;
import committee.nova.mods.magneticraft.system.nuclear.data.ReactorParameterRegistry;
import committee.nova.mods.magneticraft.system.nuclear.radiation.RadiationExposureService;
import committee.nova.mods.magneticraft.system.nuclear.radiation.RadiationSourceRegistry;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraftforge.items.IItemHandlerModifiable;
import org.jetbrains.annotations.Nullable;

import java.util.Optional;
import java.util.UUID;

/** Owns spent-fuel inventory, water cooling, structure state and its shielded radiation source. */
public final class SpentFuelPoolControllerBlockEntity extends MachineBlockEntity
        implements MenuProvider, RadiationSource {
    public static final int SLOTS = 16;
    public static final int MENU_LOGICAL_DATA_COUNT = 14;
    public static final int MENU_DATA_COUNT = MENU_LOGICAL_DATA_COUNT * 2;
    private static final int SCHEMA_VERSION = 1;
    private static final String RENDER_WIDTH_TAG = "render_width";
    private static final String RENDER_LENGTH_TAG = "render_length";
    private static final String RENDER_HEIGHT_TAG = "render_height";

    private final SpentFuelPoolStructureValidator validator = new SpentFuelPoolStructureValidator();
    private final ItemInventoryModule inventory;
    private final ContainerData menuData;
    @Nullable private UUID owner;
    @Nullable private SpentFuelPoolSnapshot snapshot;
    private int savedWidth;
    private int savedLength;
    private int savedHeight;
    private boolean coolingActive;
    private int safeAssemblies;
    private int transferableAssemblies;
    private double releasedHeatJoules;

    public SpentFuelPoolControllerBlockEntity(BlockPos position, BlockState state) {
        super(ModBlockEntities.SPENT_FUEL_POOL_CONTROLLER.get(), position, state);
        inventory = addModule(new ItemInventoryModule(
                Magneticraft.id("spent_fuel_inventory"), this, SLOTS,
                (slot, stack) -> isSpentFuel(stack), side -> ItemInventoryModule.NONE));
        menuData = Int32ContainerData.readOnly(
                () -> formed() ? 1 : 0,
                () -> facing().get2DDataValue(),
                () -> snapshot == null ? savedWidth : snapshot.width(),
                () -> snapshot == null ? savedLength : snapshot.length(),
                () -> snapshot == null ? savedHeight : snapshot.height(),
                () -> port() == null ? 0 : 1,
                () -> coolingActive ? 1 : 0,
                this::fuelCount,
                () -> safeAssemblies,
                () -> snapshot == null ? 0 : snapshot.waterBlocks(),
                () -> (int) Math.min(Integer.MAX_VALUE, Math.round(releasedHeatJoules)),
                () -> (int) Math.round(portTemperatureKelvin() * 10.0D),
                () -> (int) Math.min(Integer.MAX_VALUE, Math.round(doseRateMillisievertsPerHour() * 1000.0D)),
                () -> transferableAssemblies);
    }

    public static void serverTick(Level level, BlockPos pos, BlockState state,
                                  SpentFuelPoolControllerBlockEntity controller) {
        controller.tickModules();
        if (controller.snapshot != null && level.getGameTime() % 40L == 0L) controller.revalidate();
        if (controller.snapshot != null && level.getGameTime() % 20L == 0L) controller.tickCooling();
        controller.finishServerTick();
    }

    public boolean formed() { return snapshot != null; }
    public Optional<SpentFuelPoolSnapshot> snapshot() { return Optional.ofNullable(snapshot); }
    public ItemInventoryModule inventory() { return inventory; }
    public ContainerData menuData() { return menuData; }
    public ItemStack stack(int slot) { return validSlot(slot) ? inventory.getStackInSlot(slot) : ItemStack.EMPTY; }
    public int renderWidth() { return savedWidth; }
    public int renderLength() { return savedLength; }
    public int renderHeight() { return savedHeight; }

    @Override
    public AABB getRenderBoundingBox() {
        if (!getBlockState().getValue(SpentFuelPoolControllerBlock.FORMED)) {
            return new AABB(worldPosition);
        }
        return NuclearMultiblockBounds.renderBounds(
                worldPosition, facing(), savedWidth, savedHeight, savedLength
        );
    }

    public boolean canManage(Player player) {
        return player.distanceToSqr(worldPosition.getX() + 0.5D, worldPosition.getY() + 0.5D,
                worldPosition.getZ() + 0.5D) <= 64.0D && player.getAbilities().mayBuild
                && (owner == null || owner.equals(player.getUUID())
                || player instanceof ServerPlayer serverPlayer && serverPlayer.hasPermissions(2));
    }

    public void setOwner(UUID owner) { if (this.owner == null) { this.owner = owner; markChanged(); } }

    public boolean tryForm(ServerPlayer player) {
        var result = validator.validate(worldPosition, facing(), partLookup());
        if (result.snapshot().isEmpty()) {
            player.displayClientMessage(Component.translatable(
                    "message.magneticraft.spent_fuel_pool.invalid", result.reason(), result.position().toShortString()), true);
            return false;
        }
        if (owner == null) owner = player.getUUID();
        applySnapshot(result.snapshot().orElseThrow());
        return true;
    }

    public void unform() {
        SpentFuelPoolSnapshot current = snapshot;
        coolingActive = false;
        if (current != null && level != null
                && level.getBlockEntity(current.port()) instanceof SpentFuelPoolPortBlockEntity port) {
            port.release(worldPosition);
        }
        if (current != null && level instanceof ServerLevel serverLevel) {
            NuclearStructureState.setFormed(
                    serverLevel, current.minimum(), current.maximum(), false
            );
        }
        snapshot = null;
        setFormed(false);
        markChangedAndSync();
    }

    public boolean isSpentFuel(ItemStack stack) {
        return stack.getItem() instanceof FuelAssemblyItem item
                && item.state(stack).map(state -> state.burnupFraction() > 0.0D
                || state.decayHeatJoules() > 0.0D).orElse(false);
    }

    public ItemStack insertFuel(int slot, ItemStack stack, boolean simulate) {
        return validSlot(slot) && isSpentFuel(stack)
                ? inventory.menuHandler().insertItem(slot, stack, simulate) : stack;
    }

    public ItemStack extractCooledFuel(int slot, int amount, boolean simulate) {
        if (!validSlot(slot) || !transferable(stack(slot))) return ItemStack.EMPTY;
        return inventory.extractInternal(slot, amount, simulate);
    }

    public boolean transferable(ItemStack stack) {
        if (!(stack.getItem() instanceof FuelAssemblyItem item)) return false;
        return item.state(stack).map(state -> state.temperatureKelvin()
                <= parameters().safeUnloadTemperatureKelvin()).orElse(false);
    }

    public boolean safe(ItemStack stack) {
        if (!(stack.getItem() instanceof FuelAssemblyItem item)) return false;
        var p = parameters();
        return item.state(stack).map(state -> transferable(stack)
                && state.decayHeatJoules() <= p.spentFuelSafeDecayHeatJoules()).orElse(false);
    }

    @Override public Component getDisplayName() {
        return Component.translatable("block.magneticraft.spent_fuel_pool_controller");
    }

    @Nullable @Override public AbstractContainerMenu createMenu(int id, Inventory playerInventory, Player player) {
        return canManage(player) && formed() ? new SpentFuelPoolMenu(id, playerInventory, this) : null;
    }

    @Override protected void saveMachineData(CompoundTag tag) {
        tag.putInt("schema_version", SCHEMA_VERSION);
        if (owner != null) tag.putUUID("owner", owner);
        SpentFuelPoolSnapshot current = snapshot;
        tag.putBoolean("formed", current != null);
        tag.putInt("width", current == null ? savedWidth : current.width());
        tag.putInt("length", current == null ? savedLength : current.length());
        tag.putInt("height", current == null ? savedHeight : current.height());
        tag.putDouble("released_heat_joules", releasedHeatJoules);
    }

    @Override protected void loadMachineData(CompoundTag tag) {
        owner = tag.hasUUID("owner") ? tag.getUUID("owner") : null;
        if (tag.getInt("schema_version") != SCHEMA_VERSION) {
            savedWidth = savedLength = savedHeight = 0;
            snapshot = null;
            setFormed(false);
            return;
        }
        savedWidth = Math.max(0, tag.getInt("width"));
        savedLength = Math.max(0, tag.getInt("length"));
        savedHeight = Math.max(0, tag.getInt("height"));
        releasedHeatJoules = Math.max(0.0D, tag.getDouble("released_heat_joules"));
    }

    @Override protected void saveClientData(CompoundTag tag) {
        super.saveClientData(tag);
        if (savedWidth > 0 && savedLength > 0 && savedHeight > 0) {
            tag.putInt(RENDER_WIDTH_TAG, savedWidth);
            tag.putInt(RENDER_LENGTH_TAG, savedLength);
            tag.putInt(RENDER_HEIGHT_TAG, savedHeight);
        }
    }

    @Override protected void loadClientData(CompoundTag tag) {
        super.loadClientData(tag);
        int width = tag.getInt(RENDER_WIDTH_TAG);
        int length = tag.getInt(RENDER_LENGTH_TAG);
        int height = tag.getInt(RENDER_HEIGHT_TAG);
        if (validator.descriptor().accepts(width, length, height)) {
            savedWidth = width;
            savedLength = length;
            savedHeight = height;
        } else {
            savedWidth = savedLength = savedHeight = 0;
        }
    }

    @Override public void onLoad() {
        super.onLoad();
        if (!(level instanceof ServerLevel serverLevel)) return;
        RadiationSourceRegistry.register(serverLevel, worldPosition);
        if (getBlockState().getValue(SpentFuelPoolControllerBlock.FORMED)
                && validator.descriptor().accepts(savedWidth, savedLength, savedHeight)) {
            var result = validator.validateExact(worldPosition, facing(), savedWidth, savedLength, savedHeight, partLookup());
            result.snapshot().ifPresentOrElse(this::applySnapshot, this::unform);
        }
    }

    @Override public void setRemoved() {
        if (level instanceof ServerLevel serverLevel) RadiationSourceRegistry.unregister(serverLevel, worldPosition);
        super.setRemoved();
    }

    @Override public BlockPos radiationOrigin() { return worldPosition; }

    @Override public double doseRateMillisievertsPerHour() {
        double dose = 0.0D;
        for (int slot = 0; slot < SLOTS; slot++) {
            dose += RadiationExposureService.itemReading(stack(slot)).doseRateMillisievertsPerHour();
        }
        int depth = snapshot == null ? 0 : Math.max(0, snapshot.height() - 2);
        return dose * Math.pow(0.88D, depth) * 0.5D;
    }

    @Override public boolean contaminationSource() {
        for (int slot = 0; slot < SLOTS; slot++) {
            if (RadiationExposureService.itemReading(stack(slot)).contaminationRateMillisievertsPerHour() > 0.0D) return true;
        }
        return false;
    }

    private void tickCooling() {
        SpentFuelPoolPortBlockEntity port = port();
        coolingActive = port != null && port.heat().node().temperatureKelvin() < 550.0D;
        safeAssemblies = 0;
        transferableAssemblies = 0;
        var p = parameters();
        double waterScale = Math.min(2.0D, snapshot.waterBlocks() / 18.0D);
        double released = 0.0D;
        IItemHandlerModifiable handler = inventory.menuHandler();
        for (int slot = 0; slot < SLOTS; slot++) {
            ItemStack stack = handler.getStackInSlot(slot);
            if (!(stack.getItem() instanceof FuelAssemblyItem item)) continue;
            Optional<FuelAssemblyState> stateValue = item.state(stack);
            if (stateValue.isEmpty()) continue;
            FuelAssemblyState state = stateValue.orElseThrow();
            if (coolingActive) {
                double nextTemperature = Math.max(FuelAssemblyState.AMBIENT_TEMPERATURE_KELVIN,
                        state.temperatureKelvin() - p.spentFuelCoolingKelvinPerTick() * 20.0D * waterScale);
                double nextDecay = state.decayHeatJoules()
                        * Math.pow(1.0D - p.decayHeatLossPerTick(), 20.0D);
                released += Math.max(0.0D, state.temperatureKelvin() - nextTemperature) * 200.0D
                        + state.decayHeatJoules() * 20.0D;
                FuelAssemblyState next = new FuelAssemblyState(
                        state.fuelId(), state.burnupFraction(), state.poisonFraction(), nextDecay,
                        nextTemperature, state.claddingIntegrity(), level.getGameTime());
                item.writeState(stack, next);
                handler.setStackInSlot(slot, stack);
            }
            if (transferable(stack)) transferableAssemblies++;
            if (safe(stack)) safeAssemblies++;
        }
        releasedHeatJoules = released;
        if (released > 0.0D && port != null) port.heat().node().addHeat(released, false);
        markChangedAndSync();
    }

    private void revalidate() {
        if (snapshot == null) return;
        var result = validator.validateExact(worldPosition, facing(), snapshot.width(), snapshot.length(),
                snapshot.height(), partLookup());
        result.snapshot().ifPresentOrElse(this::applySnapshot, this::unform);
    }

    private void applySnapshot(SpentFuelPoolSnapshot next) {
        snapshot = next;
        savedWidth = next.width(); savedLength = next.length(); savedHeight = next.height();
        if (level != null && level.getBlockEntity(next.port()) instanceof SpentFuelPoolPortBlockEntity port) {
            port.claim(worldPosition);
        }
        if (level instanceof ServerLevel serverLevel) {
            NuclearStructureState.setFormed(
                    serverLevel, next.minimum(), next.maximum(), true
            );
        }
        setFormed(true);
        markChangedAndSync();
    }

    @Nullable private SpentFuelPoolPortBlockEntity port() {
        return snapshot != null && level != null
                && level.getBlockEntity(snapshot.port()) instanceof SpentFuelPoolPortBlockEntity port
                && port.claimedBy(worldPosition) ? port : null;
    }

    private double portTemperatureKelvin() {
        SpentFuelPoolPortBlockEntity port = port();
        return port == null ? 293.15D : port.heat().node().temperatureKelvin();
    }

    private int fuelCount() {
        int count = 0;
        for (int slot = 0; slot < SLOTS; slot++) if (!stack(slot).isEmpty()) count++;
        return count;
    }

    private void setFormed(boolean value) {
        BlockState state = getBlockState();
        if (level != null && state.getValue(SpentFuelPoolControllerBlock.FORMED) != value) {
            level.setBlock(worldPosition, state.setValue(SpentFuelPoolControllerBlock.FORMED, value), Block.UPDATE_CLIENTS);
        }
    }

    public Direction facing() { return getBlockState().getValue(SpentFuelPoolControllerBlock.FACING); }
    private boolean validSlot(int slot) { return slot >= 0 && slot < SLOTS; }
    private committee.nova.mods.magneticraft.system.nuclear.data.ReactorParameters parameters() {
        return ReactorParameterRegistry.INSTANCE.current().parameters();
    }

    private VariableNuclearStructureValidator.PartLookup<SpentFuelPoolPart> partLookup() {
        return new VariableNuclearStructureValidator.PartLookup<>() {
            @Nullable @Override public SpentFuelPoolPart partAt(BlockPos position) {
                if (level == null) return null;
                BlockState state = level.getBlockState(position);
                if (state.isAir()) return new SpentFuelPoolPart(SpentFuelPoolPart.Kind.AIR, null);
                if (state.is(Blocks.WATER)) return new SpentFuelPoolPart(SpentFuelPoolPart.Kind.WATER, null);
                if (state.is(ModNuclearBlocks.FACILITY_CASING.get()))
                    return new SpentFuelPoolPart(SpentFuelPoolPart.Kind.CASING, null);
                if (position.equals(worldPosition) && state.is(ModNuclearBlocks.SPENT_FUEL_POOL_CONTROLLER.get()))
                    return new SpentFuelPoolPart(SpentFuelPoolPart.Kind.CONTROLLER, facing());
                if (state.is(ModNuclearBlocks.SPENT_FUEL_POOL_PORT.get()))
                    return new SpentFuelPoolPart(SpentFuelPoolPart.Kind.PORT,
                            state.getValue(SpentFuelPoolPortBlock.FACING));
                return null;
            }
            @Override public boolean isLoaded(BlockPos position) {
                return level instanceof ServerLevel serverLevel && serverLevel.hasChunkAt(position);
            }
        };
    }
}
