package committee.nova.mods.magneticraft.content.world;

import com.mojang.authlib.GameProfile;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.common.ForgeHooks;
import net.minecraftforge.common.util.BlockSnapshot;
import net.minecraftforge.common.util.FakePlayer;
import net.minecraftforge.common.util.FakePlayerFactory;
import net.minecraftforge.event.ForgeEventFactory;
import org.jetbrains.annotations.Nullable;

import java.util.UUID;

/** Permission-aware, no-drop world mutations shared by owner-operated machines. */
public final class ProtectedWorldMutation {
    private static final String PROFILE_NAME = "[Magneticraft]";

    private ProtectedWorldMutation() {
    }

    /** Returns false without loading a chunk. */
    public static boolean isSafeLoadedTarget(ServerLevel level, BlockPos target) {
        return !level.isOutsideBuildHeight(target)
                && level.getWorldBorder().isWithinBounds(target)
                && level.getChunkSource().getChunkNow(target.getX() >> 4, target.getZ() >> 4) != null;
    }

    @Nullable
    public static FakePlayer ownerPlayer(ServerLevel level, @Nullable UUID owner, BlockPos actorPosition) {
        if (owner == null) {
            return null;
        }
        FakePlayer player = FakePlayerFactory.get(level, new GameProfile(owner, PROFILE_NAME));
        positionPlayer(player, actorPosition);
        return player;
    }

    public static boolean mayModify(
            ServerLevel level,
            FakePlayer player,
            BlockPos actorPosition,
            BlockPos target,
            Direction face
    ) {
        positionPlayer(player, actorPosition);
        return level.mayInteract(player, target) && player.mayUseItemAt(target, face, ItemStack.EMPTY);
    }

    /**
     * Replaces one loaded block after both break and placement protection hooks approve it.
     * The original state is restored when placement is cancelled.
     */
    public static boolean replaceWithoutDrops(
            ServerLevel level,
            @Nullable UUID owner,
            BlockPos actorPosition,
            BlockPos target,
            Direction face,
            BlockState replacement
    ) {
        if (!isSafeLoadedTarget(level, target)) {
            return false;
        }
        BlockEntity blockEntity = level.getBlockEntity(target);
        BlockState original = level.getBlockState(target);
        if (blockEntity != null || original.getDestroySpeed(level, target) < 0.0F) {
            return false;
        }
        FakePlayer player = ownerPlayer(level, owner, actorPosition);
        if (player == null || !mayModify(level, player, actorPosition, target, face)) {
            return false;
        }
        if (!original.isAir()
                && (ForgeHooks.onBlockBreakEvent(level, GameType.SURVIVAL, player, target) == -1
                || !level.getBlockState(target).equals(original))) {
            return false;
        }

        BlockSnapshot replaced = BlockSnapshot.create(level.dimension(), level, target);
        if (!level.setBlock(target, replacement, Block.UPDATE_ALL | Block.UPDATE_SUPPRESS_DROPS)) {
            return false;
        }
        if (ForgeEventFactory.onBlockPlace(player, replaced, face)) {
            replaced.restore(true, true);
            return false;
        }
        return true;
    }

    private static void positionPlayer(FakePlayer player, BlockPos actorPosition) {
        player.setPos(
                actorPosition.getX() + 0.5D,
                actorPosition.getY() + 0.5D,
                actorPosition.getZ() + 0.5D
        );
    }
}
