# Porting sources and provenance

This document is the source-of-truth for code and asset provenance during the
Minecraft 1.20.1 port. A source being listed here does not by itself authorize
copying from it; the reuse policy in the table is mandatory.

## Fixed reference snapshots

| Source | Fixed commit | Declared/detected license | Role and reuse policy |
|---|---|---|---|
| [Nova-Committee/Magneticraft](https://github.com/Nova-Committee/Magneticraft) | `4108ca9bb332d11965c30e0c592b310d0858f251` | GPL-2.0 | Behavioral and asset authority for the 1.12 release. Derived work must remain GPL-2.0-compatible and retain attribution. |
| [Magneticraft-Team/Magneticraft, 1.14 branch](https://github.com/Magneticraft-Team/Magneticraft/tree/1.14) | `9c980e328b2c3368fa92b6fd464df0ff95a8f979` | GPL-2.0 | Incomplete official transition reference only. It does not override working 1.12 behavior. |
| [MochiButter/Magneticraft](https://github.com/MochiButter/Magneticraft) | `140fdf5c1076570aebf35a65e44a499482863812` | GPL-2.0 | Java/Forge 1.20.1 reference. Code may be adapted only after file-level review, with the derived location recorded below. |
| [hypersmc/magneticraft2-1.20.x](https://github.com/hypersmc/magneticraft2-1.20.x) | `9a70e6f64971d626b3668316cb8221448e48c471` | `All Rights Reserved` in mod metadata; repository license is not a reusable mod-source grant | Architecture comparison only. Do not copy source, resources, data, identifiers, or text. |

The snapshots are downloaded to the ignored `.references/` directory for local
inspection. They are not build inputs and must never be added to a commit or a
published artifact.

## Decision precedence

1. Observable behavior from the fixed Nova 1.12 snapshot.
2. Explicit product decisions recorded for this port.
3. Forge 1.20.1 and Minecraft 1.20.1 lifecycle and data contracts.
4. Official 1.14 and audited GPL-compatible 1.20.1 examples as implementation
   evidence, never as a substitute for behavior analysis.

## Reuse log

No source or asset from a reference repository was copied into the engineering
baseline. The first migrated configuration default,
`general.crushing_table_causes_fire=true`, was independently reimplemented from
the behavior declared by Nova 1.12 `systems/config/Config.kt`.

Future adaptations must add one row before the corresponding commit:

| Local path | Source path and commit | Adaptation summary | Reviewer |
|---|---|---|---|
| `src/main/resources/assets/magneticraft/textures/item/*_{ingot,nugget,light_plate,heavy_plate,chunk,dust,rocky_chunk}.png` | Nova 1.12 `textures/items/{ingots,nugget,light_plate,heavy_plate,chunk,dust,rocky_chunk}/*.png` at `4108ca9bb332d11965c30e0c592b310d0858f251` | Copied only the 76 registered variants; renamed metadata variants to stable singular item IDs and moved them to the 1.20 `textures/item` layout. | Codex |
| `src/main/resources/assets/magneticraft/textures/item/{sulfur,alternator,motor,fine_copper_wire,magnet,iron_mesh,fabric_mesh,*_hammer}.png` | Nova 1.12 `textures/items/{crafting,tools}/*.png` at `4108ca9bb332d11965c30e0c592b310d0858f251` | Copied the seven in-scope crafting components and three hammers; renamed files to their split registry IDs. | Codex |
| `src/main/resources/assets/magneticraft/textures/block/*.png` | Nova 1.12 `textures/blocks/{ore_block,decoration}/**/*.png` at `4108ca9bb332d11965c30e0c592b310d0858f251` | Copied the four ores, four storage blocks and eight limestone variants; excluded copper, oil-source and tube-light assets owned by other stages. | Codex |
| `src/main/resources/assets/magneticraft/textures/fluid/*` | Nova 1.12 `textures/fluids/*` at `4108ca9bb332d11965c30e0c592b310d0858f251` | Copied the 16 still/flow texture pairs and their animation metadata verbatim, changing only the directory from `textures/fluids` to the 1.20 `textures/fluid` layout. | Codex |
| `src/main/java/committee/nova/mods/magneticraft/data/recipe/CountedCookingRecipeBuilder.java` | MochiButter/Magneticraft `datagen/providers/util/FurnaceMultiRecipeBuilder.java` at `140fdf5c1076570aebf35a65e44a499482863812` (GPL-2.0) | File-level audited and adapted to a validated, smelting-only builder for Forge's counted cooking-result extension; renamed, reduced and independently wired to Nova recipe behavior. | Codex |
| `src/gametest/resources/data/magneticraft/structures/base_content.nbt` | MinecraftForge/MinecraftForge `src/test_old/resources/data/gametest_test/structures/gametesttest.teststone.nbt` at `b7efb8287b6743400c98e3f826e0ac7def943c49` (LGPL-2.1) | Reused the official minimal 3x3x3 GameTest structure under a Magneticraft-local name; tests replace the template contents before assertions. SHA-256 `f1f8adacd54763fcf7a8a6b84251862678966309ac45bc322c78929d6de8ba75`. | Codex |
| `src/main/resources/assets/magneticraft/textures/block/{crushing_table_*,battery,grate,electric_furnace_*}.png` | Nova 1.12 `textures/blocks/{machines,electric_machines,multiblock_parts}/**/*.png` at `4108ca9bb332d11965c30e0c592b310d0858f251` | Copied the three crushing-table faces, battery face, iron grate and three electric-furnace faces; flattened the metadata-era directories into stable 1.20 block texture IDs. | Codex |
| `src/main/resources/assets/magneticraft/textures/item/battery_item_low.png` | Nova 1.12 `textures/items/electric_items/battery_item_low.png` at `4108ca9bb332d11965c30e0c592b310d0858f251` | Copied the low-capacity battery texture to the 1.20 singular item texture layout. | Codex |
| `src/main/resources/assets/magneticraft/sounds/crushing_{hit1,hit2,hit3,final1,final2}.ogg` | Nova 1.12 `sounds/crushing_*.ogg` at `4108ca9bb332d11965c30e0c592b310d0858f251` | Copied the five crushing-table samples verbatim and rebuilt only the 1.20 sound-event manifest. | Codex |
| `src/main/resources/assets/magneticraft/models/block/battery.{obj,mtl}` | Nova 1.12 `models/block/obj/block_battery.obj` at `4108ca9bb332d11965c30e0c592b310d0858f251` | Copied the battery mesh verbatim and supplied a minimal 1.20 material-library override that points its sole material to `magneticraft:block/battery`; no Magneticraft2 model data was used. | Codex |

## Magneticraft2 boundary

Allowed: compare package responsibilities, graph boundaries, save/load failure
modes, and public behavior visible in a running build.

Forbidden: transcribing or mechanically translating its classes, JSON schemas,
models, textures, language strings, recipes, or documentation. When it suggests
an architecture, implement that architecture independently from the Nova 1.12
behavior and Forge documentation.
