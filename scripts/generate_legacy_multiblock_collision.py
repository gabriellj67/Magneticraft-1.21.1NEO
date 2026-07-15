from __future__ import annotations

import re
from pathlib import Path


ROOT = Path(__file__).resolve().parents[1]
STRUCTURES = ROOT / ".references/nova-1.12/src/main/kotlin/com/cout970/magneticraft/features/multiblocks/structures"
STEAM_ENGINE_MODULE = ROOT / ".references/nova-1.12/src/main/kotlin/com/cout970/magneticraft/systems/tilemodules/ModuleSteamEngineMb.kt"
TARGET = ROOT / "src/main/java/committee/nova/mods/magneticraft/content/multiblock/LegacyMultiblockCollision.java"

BOX = re.compile(
    r"Vec3d\(([-\d.]+),\s*([-\d.]+),\s*([-\d.]+)\)\s*\*\s*PIXEL\s*"
    r"(?:to|createAABBUsing)\s*Vec3d\(([-\d.]+),\s*([-\d.]+),\s*([-\d.]+)\)\s*\*\s*PIXEL"
)

SOURCES = {
    "BIG_COMBUSTION_CHAMBER": ("MultiblockBigCombustionChamber.kt", "hitboxes", "SOUTH", (0, 0, 1)),
    "BIG_ELECTRIC_FURNACE": ("MultiblockBigElectricFurnace.kt", "hitboxes", "SOUTH", (0, 0, 1)),
    "BIG_STEAM_BOILER": ("MultiblockBigSteamBoiler.kt", "hitboxes", "SOUTH", (0, 0, 0)),
    "CONTAINER": ("MultiblockContainer.kt", "hitboxes", "SOUTH", (0, 0, 3)),
    "GRINDER": ("MultiblockGrinder.kt", "hitbox", "SOUTH", (0, 0, 1)),
    "HYDRAULIC_PRESS": ("MultiblockHydraulicPress.kt", "hitboxes", "SOUTH", (0, 0, 1)),
    "OIL_HEATER": ("MultiblockOilHeater.kt", "hitboxes", "SOUTH", (1, 0, 0)),
    "PUMPJACK": ("MultiblockPumpjack.kt", "hitboxes", "EAST", (0, 0, 1)),
    "REFINERY": ("MultiblockRefinery.kt", "hitboxes", "SOUTH", (0, 0, 1)),
    "SIEVE": ("MultiblockSieve.kt", None, "NORTH", (0, 0, 2)),
    "SOLAR_MIRROR": ("MultiblockSolarMirror.kt", "hitbox", "SOUTH", (0, 0, 1)),
    "SOLAR_PANEL": ("MultiblockSolarPanel.kt", "hitbox", "NORTH", (0, 0, 0)),
    "SOLAR_TOWER": ("MultiblockSolarTower.kt", "hitbox", "SOUTH", (0, 0, 1)),
    "STEAM_ENGINE": ("MultiblockSteamEngine.kt", "hitbox", "SOUTH", (1, 0, 1)),
    "STEAM_TURBINE": ("MultiblockSteamTurbine.kt", "hitboxes", "SOUTH", (1, 0, 0)),
}

EXPECTED_COUNTS = {
    "BIG_COMBUSTION_CHAMBER": 35,
    "BIG_ELECTRIC_FURNACE": 29,
    "BIG_STEAM_BOILER": 32,
    "CONTAINER": 44,
    "GRINDER": 56,
    "HYDRAULIC_PRESS": 42,
    "OIL_HEATER": 15,
    "PUMPJACK": 85,
    "REFINERY": 34,
    "SHELVING_UNIT": 4,
    "SIEVE": 66,
    "SOLAR_MIRROR": 4,
    "SOLAR_PANEL": 33,
    "SOLAR_TOWER": 12,
    "STEAM_ENGINE": 76,
    "STEAM_TURBINE": 51,
}


def list_body(source: str, variable: str | None) -> str:
    if variable is None:
        start = source.index("getGlobalCollisionBoxes")
        opening = source.index("listOf(", start) + len("listOf")
    else:
        start = source.index(f"val {variable} = listOf(")
        opening = source.index("(", start)
    depth = 0
    for index in range(opening, len(source)):
        char = source[index]
        if char == "(":
            depth += 1
        elif char == ")":
            depth -= 1
            if depth == 0:
                return source[opening + 1:index]
    raise ValueError(f"Unterminated listOf for {variable}")


def parse_boxes(path: Path, variable: str | None) -> list[tuple[str, ...]]:
    source = path.read_text(encoding="utf-8")
    body = list_body(source, variable)
    body = re.sub(r"//.*", "", body)
    return BOX.findall(body)


def flat_java(boxes: list[tuple[str, ...]]) -> str:
    values = [value for box in boxes for value in box]
    rows = []
    for index in range(0, len(values), 12):
        rows.append("                " + ", ".join(values[index:index + 12]))
    return ",\n".join(rows)


def register_block(definition: str, direction: str, offset: tuple[int, int, int], boxes: list[tuple[str, ...]]) -> str:
    dx, dy, dz = offset
    return f"""        register(
                MultiblockDefinition.{definition}, Direction.{direction}, {dx}.0D, {dy}.0D, {dz}.0D,
                new double[]{{
{flat_java(boxes)}
                }}
        );"""


def generate() -> str:
    registrations = []
    counts = {}
    for definition, (filename, variable, direction, offset) in SOURCES.items():
        boxes = parse_boxes(STRUCTURES / filename, variable)
        if definition == "STEAM_ENGINE":
            boxes += parse_boxes(STEAM_ENGINE_MODULE, "lidBoxes")
            boxes += parse_boxes(STEAM_ENGINE_MODULE, "gearboxShell")
        counts[definition] = len(boxes)
        registrations.append(register_block(definition, direction, offset, boxes))

    shelving = [
        ("0", "0", "0", "80", "11", "32"),
        ("0", "11", "0", "80", "27", "32"),
        ("0", "27", "0", "80", "43", "32"),
        ("0", "43", "0", "80", "48", "32"),
    ]
    counts["SHELVING_UNIT"] = len(shelving)
    registrations.insert(9, register_block("SHELVING_UNIT", "NORTH", (-2, 0, 0), shelving))
    if counts != EXPECTED_COUNTS:
        raise ValueError(f"Legacy collision count drift: {counts}")

    return f'''package committee.nova.mods.magneticraft.content.multiblock;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.shapes.BooleanOp;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Released 1.12 multiblock collision catalogue. Coordinates and the two-stage
 * facing transform are preserved from the original Kotlin definitions.
 */
public final class LegacyMultiblockCollision {{
    private static final double PIXEL = 1.0D / 16.0D;
    private static final Map<MultiblockDefinition, List<AABB>> AUTHORED =
            new EnumMap<>(MultiblockDefinition.class);
    private static final Map<OrientedKey, List<AABB>> ORIENTED = new ConcurrentHashMap<>();
    private static final Map<ShapeKey, VoxelShape> SHAPES = new ConcurrentHashMap<>();

    static {{
{chr(10).join(registrations)}
        if (AUTHORED.size() != MultiblockDefinition.values().length) {{
            throw new IllegalStateException("Incomplete legacy multiblock collision catalogue");
        }}
    }}

    private LegacyMultiblockCollision() {{
    }}

    public static List<AABB> authoredBoxes(MultiblockDefinition definition) {{
        return AUTHORED.get(Objects.requireNonNull(definition));
    }}

    public static List<AABB> controllerRelativeBoxes(
            MultiblockDefinition definition,
            Direction controllerFacing
    ) {{
        requireHorizontal(controllerFacing);
        OrientedKey key = new OrientedKey(definition, controllerFacing);
        return ORIENTED.computeIfAbsent(key, ignored -> AUTHORED.get(definition).stream()
                .map(box -> rotate(rotate(box, Direction.SOUTH), controllerFacing.getOpposite()))
                .toList());
    }}

    public static VoxelShape shapeAt(
            BlockPos blockPosition,
            BlockPos controllerPosition,
            MultiblockDefinition definition,
            Direction controllerFacing
    ) {{
        BlockPos relative = blockPosition.subtract(controllerPosition);
        ShapeKey key = new ShapeKey(
                definition, controllerFacing, relative.getX(), relative.getY(), relative.getZ()
        );
        return SHAPES.computeIfAbsent(key, LegacyMultiblockCollision::buildShape);
    }}

    public static AABB worldBounds(
            BlockPos controllerPosition,
            MultiblockDefinition definition,
            Direction controllerFacing
    ) {{
        List<AABB> boxes = controllerRelativeBoxes(definition, controllerFacing);
        if (boxes.isEmpty()) {{
            return new AABB(controllerPosition);
        }}
        AABB bounds = boxes.get(0);
        for (int index = 1; index < boxes.size(); index++) {{
            bounds = union(bounds, boxes.get(index));
        }}
        return bounds.move(controllerPosition);
    }}

    private static VoxelShape buildShape(ShapeKey key) {{
        VoxelShape shape = Shapes.empty();
        double cellMinX = key.x;
        double cellMinY = key.y;
        double cellMinZ = key.z;
        double cellMaxX = cellMinX + 1.0D;
        double cellMaxY = cellMinY + 1.0D;
        double cellMaxZ = cellMinZ + 1.0D;
        for (AABB box : controllerRelativeBoxes(key.definition, key.facing)) {{
            double minX = Math.max(box.minX, cellMinX);
            double minY = Math.max(box.minY, cellMinY);
            double minZ = Math.max(box.minZ, cellMinZ);
            double maxX = Math.min(box.maxX, cellMaxX);
            double maxY = Math.min(box.maxY, cellMaxY);
            double maxZ = Math.min(box.maxZ, cellMaxZ);
            if (minX >= maxX || minY >= maxY || minZ >= maxZ) {{
                continue;
            }}
            VoxelShape clipped = Shapes.create(new AABB(
                    minX - cellMinX, minY - cellMinY, minZ - cellMinZ,
                    maxX - cellMinX, maxY - cellMinY, maxZ - cellMinZ
            ));
            shape = Shapes.joinUnoptimized(shape, clipped, BooleanOp.OR);
        }}
        return shape.optimize();
    }}

    private static void register(
            MultiblockDefinition definition,
            Direction authoredFacing,
            double offsetX,
            double offsetY,
            double offsetZ,
            double[] pixels
    ) {{
        if (pixels.length % 6 != 0 || AUTHORED.containsKey(definition)) {{
            throw new IllegalArgumentException("Invalid legacy collision data for " + definition);
        }}
        List<AABB> boxes = new ArrayList<>(pixels.length / 6);
        for (int index = 0; index < pixels.length; index += 6) {{
            AABB raw = new AABB(
                    pixels[index] * PIXEL,
                    pixels[index + 1] * PIXEL,
                    pixels[index + 2] * PIXEL,
                    pixels[index + 3] * PIXEL,
                    pixels[index + 4] * PIXEL,
                    pixels[index + 5] * PIXEL
            );
            boxes.add(rotate(raw, authoredFacing).move(offsetX, offsetY, offsetZ));
        }}
        AUTHORED.put(definition, List.copyOf(boxes));
    }}

    private static AABB rotate(AABB box, Direction direction) {{
        requireHorizontal(direction);
        return switch (direction) {{
            case NORTH -> box;
            case SOUTH -> new AABB(
                    1.0D - box.maxX, box.minY, 1.0D - box.maxZ,
                    1.0D - box.minX, box.maxY, 1.0D - box.minZ
            );
            case EAST -> new AABB(
                    1.0D - box.maxZ, box.minY, box.minX,
                    1.0D - box.minZ, box.maxY, box.maxX
            );
            case WEST -> new AABB(
                    box.minZ, box.minY, 1.0D - box.maxX,
                    box.maxZ, box.maxY, 1.0D - box.minX
            );
            default -> throw new IllegalArgumentException("Vertical multiblock facing " + direction);
        }};
    }}

    private static AABB union(AABB first, AABB second) {{
        return new AABB(
                Math.min(first.minX, second.minX),
                Math.min(first.minY, second.minY),
                Math.min(first.minZ, second.minZ),
                Math.max(first.maxX, second.maxX),
                Math.max(first.maxY, second.maxY),
                Math.max(first.maxZ, second.maxZ)
        );
    }}

    private static void requireHorizontal(Direction direction) {{
        Objects.requireNonNull(direction);
        if (direction.getAxis().isVertical()) {{
            throw new IllegalArgumentException("Multiblock facing must be horizontal: " + direction);
        }}
    }}

    private record OrientedKey(MultiblockDefinition definition, Direction facing) {{
    }}

    private record ShapeKey(
            MultiblockDefinition definition,
            Direction facing,
            int x,
            int y,
            int z
    ) {{
    }}
}}
'''


if __name__ == "__main__":
    TARGET.write_text(generate(), encoding="utf-8", newline="\n")
    print(TARGET)
