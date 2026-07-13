package committee.nova.mods.magneticraft.content.network.module;

/**
 * One conveyor block's 16x16 collision bitmap. Parcel hitboxes are four
 * pixels wide and use half-open cell ranges, so touching edges do not collide.
 */
final class ConveyorOccupancy {
    static final int SIZE = 16;
    private static final double HALF_ITEM_SIZE = 2.0D;

    private final boolean[] cells = new boolean[SIZE * SIZE];

    void mark(ConveyorRoute route, int progress) {
        set(route, progress, true);
    }

    void unmark(ConveyorRoute route, int progress) {
        set(route, progress, false);
    }

    boolean isFree(ConveyorRoute route, int progress) {
        ConveyorRoute.PixelPosition position = route.position(progress);
        int minX = floor(position.x() - HALF_ITEM_SIZE);
        int maxX = ceil(position.x() + HALF_ITEM_SIZE);
        int minZ = floor(position.z() - HALF_ITEM_SIZE);
        int maxZ = ceil(position.z() + HALF_ITEM_SIZE);
        for (int x = minX; x < maxX; x++) {
            for (int z = minZ; z < maxZ; z++) {
                if (get(x, z)) {
                    return false;
                }
            }
        }
        return true;
    }

    private void set(ConveyorRoute route, int progress, boolean occupied) {
        ConveyorRoute.PixelPosition position = route.position(progress);
        int minX = floor(position.x() - HALF_ITEM_SIZE);
        int maxX = ceil(position.x() + HALF_ITEM_SIZE);
        int minZ = floor(position.z() - HALF_ITEM_SIZE);
        int maxZ = ceil(position.z() + HALF_ITEM_SIZE);
        for (int x = minX; x < maxX; x++) {
            for (int z = minZ; z < maxZ; z++) {
                set(x, z, occupied);
            }
        }
    }

    private boolean get(int x, int z) {
        return inside(x, z) && cells[x + z * SIZE];
    }

    private void set(int x, int z, boolean occupied) {
        if (inside(x, z)) {
            cells[x + z * SIZE] = occupied;
        }
    }

    private static boolean inside(int x, int z) {
        return x >= 0 && x < SIZE && z >= 0 && z < SIZE;
    }

    private static int floor(double value) {
        return (int) Math.floor(value);
    }

    private static int ceil(double value) {
        return (int) Math.ceil(value);
    }
}
