package committee.nova.mods.magneticraft.content.machine.crushingtable;

import committee.nova.mods.magneticraft.Magneticraft;
import committee.nova.mods.magneticraft.config.MagneticraftConfig;
import committee.nova.mods.magneticraft.content.item.HammerItem;
import committee.nova.mods.magneticraft.content.machine.framework.MachineBlockEntity;
import committee.nova.mods.magneticraft.content.machine.framework.module.ItemInventoryModule;
import committee.nova.mods.magneticraft.init.ModBlockEntities;
import committee.nova.mods.magneticraft.init.ModRecipeTypes;
import committee.nova.mods.magneticraft.init.ModSounds;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

import java.util.Optional;

/**
 * Player-driven single-slot crushing-table state and interaction logic.
 */
public final class CrushingTableBlockEntity extends MachineBlockEntity {
    public static final int REQUIRED_PROGRESS = 40;

    private static final String PROGRESS_TAG = "progress";
    private static final String LAST_ITEM_TAG = "last_item";
    private static final String DISPLAY_ITEM_TAG = "display_item";

    private final ItemInventoryModule inventory;
    private int progress;
    private ItemStack lastItem = ItemStack.EMPTY;

    public CrushingTableBlockEntity(BlockPos position, BlockState state) {
        super(ModBlockEntities.CRUSHING_TABLE.get(), position, state);
        inventory = addModule(new ItemInventoryModule(
                Magneticraft.id("inventory"),
                this,
                1,
                (slot, stack) -> true,
                side -> ItemInventoryModule.NONE
        ));
    }

    public boolean interact(Player player, InteractionHand hand) {
        Level currentLevel = getLevel();
        if (currentLevel == null || currentLevel.isClientSide) {
            return true;
        }

        ItemStack held = player.getItemInHand(hand);
        if (storedItem().isEmpty()) {
            return placeFromPlayer(player, held);
        }

        if (held.getItem() instanceof HammerItem hammer) {
            Optional<CrushingRecipe> recipe = findRecipe(storedItem());
            if (recipe.isEmpty()) {
                returnStoredItem(player);
                return true;
            }
            if (recipe.get().requiredLevel() > hammer.type().crushingLevel()) {
                return true;
            }

            ItemStack input = storedItem().copyWithCount(1);
            progress += hammer.type().crushingSpeed();
            held.hurtAndBreak(
                    hammer.type().crushingDurabilityCost(),
                    player,
                    entity -> entity.broadcastBreakEvent(EquipmentSlot.MAINHAND)
            );

            if (progress >= REQUIRED_PROGRESS) {
                lastItem = input;
                inventory.setStackInSlot(0, recipe.get().output());
                progress = 0;
                currentLevel.playSound(null, worldPosition, ModSounds.CRUSHING_FINAL.get(), SoundSource.BLOCKS, 1.0F, 1.0F);
            } else {
                currentLevel.playSound(null, worldPosition, ModSounds.CRUSHING_HIT.get(), SoundSource.BLOCKS, 1.0F, 1.0F);
            }
            if (storedItem().is(net.minecraft.world.item.Items.BLAZE_ROD)
                    && MagneticraftConfig.CRUSHING_TABLE_CAUSES_FIRE.get()) {
                player.setSecondsOnFire(5);
            }
            markChangedAndSync();
            return true;
        }

        returnStoredItem(player);
        return true;
    }

    public ItemStack storedItem() {
        return inventory.getStackInSlot(0);
    }

    public int progress() {
        return progress;
    }

    public ItemStack removeStoredItem() {
        ItemStack result = inventory.extractInternal(0, inventory.getStackInSlot(0).getCount(), false);
        if (!result.isEmpty()) {
            markChangedAndSync();
        }
        return result;
    }

    @Override
    protected void saveMachineData(CompoundTag tag) {
        tag.putInt(PROGRESS_TAG, progress);
        if (!lastItem.isEmpty()) {
            tag.put(LAST_ITEM_TAG, lastItem.save(new CompoundTag()));
        }
    }

    @Override
    protected void loadMachineData(CompoundTag tag) {
        progress = Math.max(0, Math.min(REQUIRED_PROGRESS - 1, tag.getInt(PROGRESS_TAG)));
        lastItem = tag.contains(LAST_ITEM_TAG) ? ItemStack.of(tag.getCompound(LAST_ITEM_TAG)) : ItemStack.EMPTY;
        if (!lastItem.isEmpty()) {
            lastItem.setCount(1);
        }
    }

    @Override
    protected void saveClientData(CompoundTag tag) {
        tag.putInt(PROGRESS_TAG, progress);
        if (!storedItem().isEmpty()) {
            tag.put(DISPLAY_ITEM_TAG, storedItem().save(new CompoundTag()));
        }
    }

    @Override
    protected void loadClientData(CompoundTag tag) {
        progress = Math.max(0, Math.min(REQUIRED_PROGRESS - 1, tag.getInt(PROGRESS_TAG)));
        inventory.setStackInSlot(
                0,
                tag.contains(DISPLAY_ITEM_TAG) ? ItemStack.of(tag.getCompound(DISPLAY_ITEM_TAG)) : ItemStack.EMPTY
        );
    }

    private boolean placeFromPlayer(Player player, ItemStack held) {
        if (held.isEmpty()) {
            return false;
        }
        if (held.getItem() instanceof HammerItem) {
            return refillLastInput(player);
        }
        inventory.setStackInSlot(0, held.copyWithCount(1));
        if (!player.getAbilities().instabuild) {
            held.shrink(1);
        }
        progress = 0;
        markChangedAndSync();
        return true;
    }

    private boolean refillLastInput(Player player) {
        if (lastItem.isEmpty()) {
            return false;
        }
        Inventory playerInventory = player.getInventory();
        for (int slot = 0; slot < playerInventory.getContainerSize(); slot++) {
            ItemStack candidate = playerInventory.getItem(slot);
            if (!candidate.isEmpty() && ItemStack.isSameItemSameTags(candidate, lastItem) && findRecipe(candidate).isPresent()) {
                inventory.setStackInSlot(0, candidate.copyWithCount(1));
                if (!player.getAbilities().instabuild) {
                    candidate.shrink(1);
                }
                progress = 0;
                markChangedAndSync();
                return true;
            }
        }
        return false;
    }

    private void returnStoredItem(Player player) {
        ItemStack stored = storedItem();
        if (!stored.isEmpty()) {
            ItemStack remainder = stored.copy();
            player.getInventory().add(remainder);
            if (remainder.getCount() != stored.getCount()) {
                inventory.setStackInSlot(0, remainder);
                progress = 0;
                markChangedAndSync();
            }
        }
    }

    private Optional<CrushingRecipe> findRecipe(ItemStack input) {
        Level currentLevel = getLevel();
        if (currentLevel == null || input.isEmpty()) {
            return Optional.empty();
        }
        SimpleContainer container = new SimpleContainer(input.copyWithCount(1));
        return currentLevel.getRecipeManager().getRecipeFor(ModRecipeTypes.CRUSHING_TYPE.get(), container, currentLevel);
    }
}
