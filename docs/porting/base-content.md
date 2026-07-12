# Base-content migration map

This document fixes the Nova 1.12 metadata-to-ID boundary implemented by the
second migration stage. The behavioral and asset source is Nova commit
`4108ca9bb332d11965c30e0c592b310d0858f251`; the target is Forge 1.20.1.

## Registry totals

| Registry area | Owned entries |
|---|---:|
| Stateless solid blocks | 16 |
| Material-form items | 76 |
| Crafting components | 7 |
| Hammers | 3 |
| Fluid types | 16 |
| Source + flowing fluids | 32 |
| Liquid blocks | 16 |
| Fluid buckets | 16 |
| Creative tabs | 1 |

The 118 item-registry entries are 16 BlockItems, 76 material forms, 7 crafting
components, 3 hammers and 16 buckets.

## Material metadata split

Every generated ID follows `<metal>_<form>`. The table lists every metal for
which that form is registered, so it is also the complete 76-item inventory.

| Legacy container | New suffix | Registered metals |
|---|---|---|
| `ingots` | `_ingot` | lead, cobalt, tungsten, steel, aluminium, mithril, nickel, osmium, silver, tin, zinc |
| `nuggets` | `_nugget` | copper, lead, cobalt, tungsten, steel, aluminium, mithril, nickel, osmium, silver, tin, zinc |
| `light_plates` | `_light_plate` | iron, gold, copper, lead, tungsten, steel |
| `heavy_plates` | `_heavy_plate` | iron, gold, copper, lead, tungsten, steel |
| `chunks` | `_chunk` | iron, gold, copper, lead, cobalt, tungsten, aluminium, mithril, nickel, osmium, silver, tin, zinc |
| `dusts` | `_dust` | iron, gold, copper, lead, cobalt, tungsten, steel, aluminium, mithril, nickel, osmium, silver, tin, zinc |
| `rocky_chunks` | `_rocky_chunk` | iron, gold, copper, lead, cobalt, tungsten, aluminium, galena, mithril, nickel, osmium, silver, tin, zinc |

Iron and gold continue to use vanilla ingots and nuggets. Copper uses the
vanilla ingot, ores and storage block, while `magneticraft:copper_nugget`
remains necessary because Minecraft 1.20.1 has no vanilla copper nugget.
Galena remains a composite ore and owns only
`magneticraft:galena_rocky_chunk`; it does not gain invented refined forms.

The US-spelling Forge aliases `ingots/aluminum`, `nuggets/aluminum` and
`dusts/aluminum`, plus Magneticraft chunk aliases, point at the corresponding
`aluminium` items.

## Block metadata split

| Legacy entry | Metadata/state | Forge 1.20.1 ID |
|---|---|---|
| `ores` | copper | vanilla `minecraft:copper_ore` and `minecraft:deepslate_copper_ore` |
| `ores` | lead/galena | `magneticraft:galena_ore` |
| `ores` | cobalt | `magneticraft:cobalt_ore` |
| `ores` | tungsten | `magneticraft:tungsten_ore` |
| `ores` | pyrite | `magneticraft:pyrite_ore` |
| `storage_blocks` | copper | vanilla `minecraft:copper_block` |
| `storage_blocks` | lead | `magneticraft:lead_block` |
| `storage_blocks` | cobalt | `magneticraft:cobalt_block` |
| `storage_blocks` | tungsten | `magneticraft:tungsten_block` |
| `storage_blocks` | pyrite/sulfur | `magneticraft:sulfur_block` |
| `limestone` | normal | `magneticraft:limestone` |
| `limestone` | brick | `magneticraft:limestone_bricks` |
| `limestone` | cobble | `magneticraft:cobbled_limestone` |
| `burnt_limestone` | normal | `magneticraft:burnt_limestone` |
| `burnt_limestone` | brick | `magneticraft:burnt_limestone_bricks` |
| `burnt_limestone` | cobble | `magneticraft:cobbled_burnt_limestone` |
| `tile_limestone` | normal | `magneticraft:limestone_tiles` |
| `tile_limestone` | inverted | `magneticraft:inverted_limestone_tiles` |

All sixteen owned blocks preserve the legacy `BlockBuilder` defaults of
hardness 1.5 and explosion resistance 10.0. Galena and pyrite require a stone
tool; cobalt and tungsten require an iron tool. Loot tables drop the block
itself, matching the old default block behavior.

Galena participates in `forge:ores/galena`, `forge:ores/lead` and
`forge:ores/silver`. Pyrite participates in `forge:ores/pyrite` and
`forge:ores/sulfur`.

## Crafting components and hammers

| Legacy metadata/item | Forge 1.20.1 ID | Preserved behavior |
|---|---|---|
| `crafting:0` | `magneticraft:sulfur` | 800-tick furnace fuel |
| `crafting:1` | `magneticraft:alternator` | legacy shaped recipe, output 4 |
| `crafting:2` | `magneticraft:motor` | legacy shaped recipe, output 4 |
| `crafting:3` | `magneticraft:fine_copper_wire` | legacy shaped recipe, output 8 |
| `crafting:4` | `magneticraft:magnet` | lapis/redstone/iron recipe |
| `crafting:5` | `magneticraft:iron_mesh` | string + light iron plate recipe |
| `crafting:6` | `magneticraft:fabric_mesh` | nine-string recipe |
| `stone_hammer` | `magneticraft:stone_hammer` | durability 130, bonus damage 2.0, cost 2 durability/hit |
| `iron_hammer` | `magneticraft:iron_hammer` | durability 250, bonus damage 3.5, cost 2 durability/hit |
| `steel_hammer` | `magneticraft:steel_hammer` | durability 750, bonus damage 5.0, cost 2 durability/hit |

Forge's counted smelting-result extension preserves the old two-ingot output
from chunks and galena rocky chunks. No blasting recipes are added because the
1.12 behavior only registered furnace recipes.

## Fluid families

Every row owns `<id>` FluidType/source/LiquidBlock, `<id>_flowing`, and
`<id>_bucket`. Temperatures are Kelvin, with density and viscosity retaining
the arbitrary values used by the legacy Forge fluid API.

| ID | Temperature | Density | Viscosity | Legacy gaseous flag |
|---|---:|---:|---:|---|
| steam | 373 | 1 | 10 | yes |
| oil | 298 | 1100 | 2000 | no |
| hot_crude | 873 | 10 | 20 | yes |
| lpg | 298 | 300 | 1000 | no |
| light_oil | 298 | 700 | 1000 | no |
| heavy_oil | 298 | 800 | 1000 | no |
| natural_gas | 298 | 1 | 10 | yes |
| naphtha | 298 | 800 | 1000 | no |
| plastic | 298 | 900 | 1000 | no |
| gasoline | 298 | 500 | 1000 | no |
| kerosene | 298 | 600 | 1000 | no |
| diesel | 298 | 700 | 1000 | no |
| lubricant | 298 | 600 | 1000 | no |
| fuel | 298 | 700 | 1000 | no |
| oil_residue | 298 | 800 | 2000 | no |
| wood_gas | 373 | 1 | 10 | yes |

Each family is in both `magneticraft:<id>` and `forge:<id>` fluid tags. The
four legacy gaseous families also enter Forge's official `forge:gaseous` tag.
Their positive legacy densities are not changed to Forge's `density <= 0`
“lighter than air” convention, so this stage does not invent upward flow.

The legacy `textures/fluid` layout is retained. Datagen adds all 32 still and
flowing sprites to Minecraft's block atlas and emits one particle-only model
plus a default blockstate for every LiquidBlock. This keeps both dynamic
bucket models and world rendering complete without duplicating or relocating
the audited source textures. Client texture selection derives the fluid ID
from the already initialized description key because Forge invokes
`FluidType#initializeClient` from its superclass constructor.

## Generated-data boundary

`runData` owns 548 generated JSON resources for this stage, including 32
blockstates, 32 block models, 118 item models, 16 loot tables, 91 recipes and
their advancements, language files, tags, and the block-atlas additions. A
second unchanged `runData` invocation must write zero files; unit tests parse
every generated JSON document and verify catalogue coverage.

## Deferred ownership

- Guide book: guide/client stage.
- Copper coil, voltmeter, thermometer and wrench: network/machine stages whose
  interaction contracts they require.
- Tube light: BlockEntity and client-rendering stage.
- Oil source: world-generation and depletion stage.
- Electric items, batteries, upgrades, gears and floppy disks: their first
  real system consumer.
- Multiblock parts and machine recipes: multiblock/machine stages.

This prevents inactive placeholders and keeps later changes tied to their
actual behavior owner.
