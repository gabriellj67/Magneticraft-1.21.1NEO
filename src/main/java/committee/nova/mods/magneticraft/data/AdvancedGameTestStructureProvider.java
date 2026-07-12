package committee.nova.mods.magneticraft.data;

import com.google.common.hash.Hashing;
import committee.nova.mods.magneticraft.Magneticraft;
import net.minecraft.data.CachedOutput;
import net.minecraft.data.DataProvider;
import net.minecraft.data.PackOutput;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.IntTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtIo;
import net.minecraft.nbt.NbtUtils;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.file.Path;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;

/** Generates an isolated empty volume large enough for advanced-system GameTests. */
final class AdvancedGameTestStructureProvider implements DataProvider {
    static final int WIDTH = 48;
    static final int HEIGHT = 16;
    static final int DEPTH = 48;

    private final Path outputPath;

    AdvancedGameTestStructureProvider(PackOutput output) {
        outputPath = output.createPathProvider(PackOutput.Target.DATA_PACK, "structures")
                .file(Magneticraft.id("advanced_systems"), "nbt");
    }

    @Override
    @SuppressWarnings("deprecation")
    public CompletableFuture<?> run(CachedOutput output) {
        try {
            ByteArrayOutputStream bytes = new ByteArrayOutputStream();
            NbtIo.writeCompressed(template(), bytes);
            byte[] encoded = bytes.toByteArray();
            output.writeIfNeeded(outputPath, encoded, Hashing.sha1().hashBytes(encoded));
            return CompletableFuture.completedFuture(null);
        } catch (IOException exception) {
            return CompletableFuture.failedFuture(new CompletionException(exception));
        }
    }

    @Override
    public String getName() {
        return "Magneticraft advanced-system GameTest structure";
    }

    private static CompoundTag template() {
        CompoundTag root = new CompoundTag();
        root.put("size", intList(WIDTH, HEIGHT, DEPTH));
        root.put("entities", new ListTag());

        ListTag palette = new ListTag();
        CompoundTag air = new CompoundTag();
        air.putString("Name", "minecraft:air");
        palette.add(air);
        root.put("palette", palette);

        ListTag blocks = new ListTag();
        for (int y = 0; y < HEIGHT; y++) {
            for (int z = 0; z < DEPTH; z++) {
                for (int x = 0; x < WIDTH; x++) {
                    CompoundTag block = new CompoundTag();
                    block.put("pos", intList(x, y, z));
                    block.putInt("state", 0);
                    blocks.add(block);
                }
            }
        }
        root.put("blocks", blocks);
        return NbtUtils.addCurrentDataVersion(root);
    }

    private static ListTag intList(int... values) {
        ListTag result = new ListTag();
        for (int value : values) {
            result.add(IntTag.valueOf(value));
        }
        return result;
    }
}
