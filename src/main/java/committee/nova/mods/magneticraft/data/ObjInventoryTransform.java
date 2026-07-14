package committee.nova.mods.magneticraft.data;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.PackType;
import net.minecraftforge.common.data.ExistingFileHelper;
import org.joml.Quaternionf;
import org.joml.Vector3f;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.Reader;

/** Computes a GUI-only transform that fits an OBJ model into a vanilla item slot. */
record ObjInventoryTransform(float translationX, float translationY, float translationZ, float scale) {
    static final float GUI_ROTATION_X = 30.0F;
    static final float GUI_ROTATION_Y = 225.0F;
    private static final float VANILLA_BLOCK_GUI_SCALE = 0.625F;
    private static final float MAX_ITEM_SCALE = 4.0F;
    private static final float MAX_ITEM_TRANSLATION = 80.0F;
    private static final float ZERO_EPSILON = 0.000001F;

    static ObjInventoryTransform load(ExistingFileHelper existingFileHelper, ResourceLocation modelLocation) {
        try (Reader reader = existingFileHelper
                .getResource(modelLocation, PackType.CLIENT_RESOURCES)
                .openAsReader()) {
            return parse(reader);
        } catch (IOException exception) {
            throw new IllegalStateException("Unable to read OBJ model " + modelLocation, exception);
        }
    }

    static ObjInventoryTransform parse(Reader reader) throws IOException {
        BufferedReader bufferedReader = reader instanceof BufferedReader buffered
                ? buffered
                : new BufferedReader(reader);
        double minX = Double.POSITIVE_INFINITY;
        double minY = Double.POSITIVE_INFINITY;
        double minZ = Double.POSITIVE_INFINITY;
        double maxX = Double.NEGATIVE_INFINITY;
        double maxY = Double.NEGATIVE_INFINITY;
        double maxZ = Double.NEGATIVE_INFINITY;
        int vertexCount = 0;

        String line;
        int lineNumber = 0;
        while ((line = bufferedReader.readLine()) != null) {
            lineNumber++;
            String trimmed = line.strip();
            if (trimmed.isEmpty() || trimmed.startsWith("#")) {
                continue;
            }
            String[] parts = trimmed.split("\\s+");
            if (!parts[0].equals("v")) {
                continue;
            }
            if (parts.length < 4) {
                throw new IllegalArgumentException("Malformed OBJ vertex at line " + lineNumber);
            }

            double x = parseCoordinate(parts[1], lineNumber);
            double y = parseCoordinate(parts[2], lineNumber);
            double z = parseCoordinate(parts[3], lineNumber);
            minX = Math.min(minX, x);
            minY = Math.min(minY, y);
            minZ = Math.min(minZ, z);
            maxX = Math.max(maxX, x);
            maxY = Math.max(maxY, y);
            maxZ = Math.max(maxZ, z);
            vertexCount++;
        }

        if (vertexCount == 0) {
            throw new IllegalArgumentException("OBJ model contains no vertices");
        }
        return fromBounds(minX, minY, minZ, maxX, maxY, maxZ);
    }

    static ObjInventoryTransform fromBounds(
            double minX,
            double minY,
            double minZ,
            double maxX,
            double maxY,
            double maxZ
    ) {
        double largestSpan = Math.max(maxX - minX, Math.max(maxY - minY, maxZ - minZ));
        if (!Double.isFinite(largestSpan) || largestSpan <= 0.0D) {
            throw new IllegalArgumentException("OBJ model has no finite extent");
        }

        float scale = (float) Math.min(MAX_ITEM_SCALE, VANILLA_BLOCK_GUI_SCALE / largestSpan);
        Vector3f centerOffset = new Vector3f(
                (float) ((minX + maxX) * 0.5D - 0.5D),
                (float) ((minY + maxY) * 0.5D - 0.5D),
                (float) ((minZ + maxZ) * 0.5D - 0.5D)
        ).mul(scale);
        new Quaternionf().rotationXYZ(
                (float) Math.toRadians(GUI_ROTATION_X),
                (float) Math.toRadians(GUI_ROTATION_Y),
                0.0F
        ).transform(centerOffset);

        float translationX = normalizeZero(-centerOffset.x() * 16.0F);
        float translationY = normalizeZero(-centerOffset.y() * 16.0F);
        float translationZ = normalizeZero(-centerOffset.z() * 16.0F);
        if (Math.abs(translationX) > MAX_ITEM_TRANSLATION
                || Math.abs(translationY) > MAX_ITEM_TRANSLATION
                || Math.abs(translationZ) > MAX_ITEM_TRANSLATION) {
            throw new IllegalArgumentException("OBJ model requires an unsupported item translation");
        }
        return new ObjInventoryTransform(translationX, translationY, translationZ, scale);
    }

    private static double parseCoordinate(String value, int lineNumber) {
        try {
            double coordinate = Double.parseDouble(value);
            if (!Double.isFinite(coordinate)) {
                throw new NumberFormatException("non-finite coordinate");
            }
            return coordinate;
        } catch (NumberFormatException exception) {
            throw new IllegalArgumentException("Invalid OBJ vertex at line " + lineNumber, exception);
        }
    }

    private static float normalizeZero(float value) {
        return Math.abs(value) < ZERO_EPSILON ? 0.0F : value;
    }
}
