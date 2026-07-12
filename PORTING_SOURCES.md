# Porting sources and provenance

This document is the source-of-truth for code and asset provenance during the
Minecraft 1.20.1 port. A source being listed here does not by itself authorize
copying from it; the reuse policy in the table is mandatory.

## Fixed reference snapshots

| Source | Fixed commit | Declared/detected license | Role and reuse policy |
|---|---|---|---|
| [Nova-Committee/Magneticraft](https://github.com/Nova-Committee/Magneticraft) | `4108ca9bb332d11965c30e0c592b310d0858f251` | GNU GPL v2 text; released here as `GPL-2.0-only` | Behavioral and asset authority for the 1.12 release. Derived work remains GPL-2.0-only and retains attribution. |
| [Magneticraft-Team/Magneticraft, 1.14 branch](https://github.com/Magneticraft-Team/Magneticraft/tree/1.14) | `9c980e328b2c3368fa92b6fd464df0ff95a8f979` | GNU GPL v2 text | Incomplete official transition reference only. It does not override working 1.12 behavior. |
| [MochiButter/Magneticraft](https://github.com/MochiButter/Magneticraft) | `140fdf5c1076570aebf35a65e44a499482863812` | GNU GPL v2 text | Java/Forge 1.20.1 reference. Code may be adapted only after file-level review, with the derived location recorded below. |
| [hypersmc/magneticraft2-1.20.x](https://github.com/hypersmc/magneticraft2-1.20.x) | `9a70e6f64971d626b3668316cb8221448e48c471` | `All Rights Reserved` in mod metadata; repository license is not a reusable mod-source grant | Architecture comparison only. Do not copy source, resources, data, identifiers, or text. |
| [cakeGit/StrutYourStuff, Forge 1.20.1 branch](https://github.com/cakeGit/StrutYourStuff/tree/1.20.1-forge) | `26a43d7c7de1cdbebefd2990ed2b15c150199dd0` | Mod metadata declares MIT, but the fixed repository snapshot has no root license text | Evaluated only as a possible optional point-to-point structural-rendering integration. It is not a multiblock validator and is not a required dependency; no source or resource may be copied without a separate license audit. |

The snapshots are downloaded to the ignored `.references/` directory for local
inspection. They are not build inputs and must never be added to a commit or a
published artifact.

## Official API and data-schema references

These dependencies are compile-only integrations and opt-in development
runtimes. Their classes and resources are not embedded in the Magneticraft JAR.
Mod coordinates are fixed for reproducible testing even where `mods.toml`
declares a broader compatible range.

| Integration or platform | Release-tested artifact/reference | License and use boundary |
|---|---|---|
| [Forge 1.20.1 documentation](https://docs.minecraftforge.net/en/1.20.1/) | Forge `47.4.20`; official lifecycle, registry, datagen and conditional-recipe contracts | Documentation/API reference only. No Forge source is copied into this project. |
| [ModDevGradle Legacy remapping](https://github.com/neoforged/ModDevGradle/blob/main/LEGACY.md) and the [official 1.20.1 MDK](https://github.com/NeoForgeMDKs/MDK-Forge-1.20.1-ModDevGradle/blob/main/build.gradle) | LegacyForge plugin resolved by `settings.gradle`; `modCompileOnly` plus isolated `modLocalRuntime` | Build-contract reference only. Optional production mod JARs are remapped for development without leaking into the published POM. |
| [Just Enough Items](https://github.com/mezz/JustEnoughItems/tree/1.20.1) | `mezz.jei:jei-1.20.1-{common-api,forge-api}:15.20.0.133`; runtime modules at the same version | MIT; public plugin/category/recipe/catalyst API only. No JEI class or resource is copied. |
| [CraftTweaker](https://github.com/CraftTweaker/CraftTweaker) | `com.blamejared.crafttweaker:CraftTweaker-forge-1.20.1:14.0.60`, ZenCode Java libraries `0.3.8` | MIT; public Java recipe-manager/action API only. No reflection, Mixin or embedded CraftTweaker class is used. |
| [Mantle](https://github.com/SlimeKnights/Mantle) | `slimeknights.mantle:Mantle:1.20.1-1.11.97` | MIT; opt-in Tinkers' Construct development/runtime companion only. Magneticraft does not call its API directly. |
| [Tinkers' Construct](https://github.com/SlimeKnights/TinkersConstruct/releases/tag/v3.11.2.166) and its [material guide](https://slimeknights.github.io/docs/guides/material/) | `slimeknights.tconstruct:TConstruct:1.20.1-3.11.2.166` | MIT; public data schema only. Magneticraft independently defines tungsten data and does not duplicate TConstruct's existing lead or steel material IDs. |

## Decision precedence

1. Observable behavior from the fixed Nova 1.12 snapshot.
2. Explicit product decisions recorded for this port.
3. Forge 1.20.1 and Minecraft 1.20.1 lifecycle and data contracts.
4. Official 1.14 and audited GPL-compatible 1.20.1 examples as implementation
   evidence, never as a substitute for behavior analysis.

## Reuse log

The initial engineering baseline copied no source or asset from a reference
repository. Later audited adaptations are recorded row by row below. The first
migrated configuration default,
`general.crushing_table_causes_fire=true`, was independently reimplemented from
the behavior declared by Nova 1.12 `systems/config/Config.kt`.

Future adaptations must add one row before the corresponding commit:

| Local path | Source path and commit | Adaptation summary | Reviewer |
|---|---|---|---|
| `src/main/resources/assets/magneticraft/textures/item/*_{ingot,nugget,light_plate,heavy_plate,chunk,dust,rocky_chunk}.png` | Nova 1.12 `textures/items/{ingots,nugget,light_plate,heavy_plate,chunk,dust,rocky_chunk}/*.png` at `4108ca9bb332d11965c30e0c592b310d0858f251` | Copied only the 76 registered variants; renamed metadata variants to stable singular item IDs and moved them to the 1.20 `textures/item` layout. | Codex |
| `src/main/resources/assets/magneticraft/textures/item/{sulfur,alternator,motor,fine_copper_wire,magnet,iron_mesh,fabric_mesh,*_hammer}.png` | Nova 1.12 `textures/items/{crafting,tools}/*.png` at `4108ca9bb332d11965c30e0c592b310d0858f251` | Copied the seven in-scope crafting components and three hammers; renamed files to their split registry IDs. | Codex |
| `src/main/resources/assets/magneticraft/textures/block/*.png` | Nova 1.12 `textures/blocks/{ore_block,decoration}/**/*.png` at `4108ca9bb332d11965c30e0c592b310d0858f251` | Copied the four ores, four storage blocks and eight limestone variants; excluded copper, oil-source and tube-light assets owned by other stages. | Codex |
| `src/main/resources/assets/magneticraft/textures/fluid/*` | Nova 1.12 `textures/fluids/*` at `4108ca9bb332d11965c30e0c592b310d0858f251` | Copied the 16 still/flow texture pairs and their animation metadata verbatim, changing only the directory from `textures/fluids` to the 1.20 `textures/fluid` layout. | Codex |
| `src/main/java/committee/nova/mods/magneticraft/data/recipe/CountedCookingRecipeBuilder.java` | MochiButter/Magneticraft `datagen/providers/util/FurnaceMultiRecipeBuilder.java` at `140fdf5c1076570aebf35a65e44a499482863812` (GNU GPL v2 text) | File-level audited and adapted to a validated, smelting-only builder for Forge's counted cooking-result extension; renamed, reduced and independently wired to Nova recipe behavior. | Codex |
| `src/gametest/resources/data/magneticraft/structures/base_content.nbt` | MinecraftForge/MinecraftForge `src/test_old/resources/data/gametest_test/structures/gametesttest.teststone.nbt` at `b7efb8287b6743400c98e3f826e0ac7def943c49` (LGPL-2.1) | Reused the official minimal 3x3x3 GameTest structure under a Magneticraft-local name; tests replace the template contents before assertions. SHA-256 `f1f8adacd54763fcf7a8a6b84251862678966309ac45bc322c78929d6de8ba75`. | Codex |
| `src/main/resources/assets/magneticraft/textures/block/{crushing_table_*,battery,grate,electric_furnace_*}.png` | Nova 1.12 `textures/blocks/{machines,electric_machines,multiblock_parts}/**/*.png` at `4108ca9bb332d11965c30e0c592b310d0858f251` | Copied the three crushing-table faces, battery face, iron grate and three electric-furnace faces; flattened the metadata-era directories into stable 1.20 block texture IDs. | Codex |
| `src/main/resources/assets/magneticraft/textures/item/battery_item_low.png` | Nova 1.12 `textures/items/electric_items/battery_item_low.png` at `4108ca9bb332d11965c30e0c592b310d0858f251` | Copied the low-capacity battery texture to the 1.20 singular item texture layout. | Codex |
| `src/main/resources/assets/magneticraft/sounds/crushing_{hit1,hit2,hit3,final1,final2}.ogg` | Nova 1.12 `sounds/crushing_*.ogg` at `4108ca9bb332d11965c30e0c592b310d0858f251` | Copied the five crushing-table samples verbatim and rebuilt only the 1.20 sound-event manifest. | Codex |
| `src/main/resources/assets/magneticraft/models/block/battery.{obj,mtl}` | Nova 1.12 `models/block/obj/block_battery.obj` at `4108ca9bb332d11965c30e0c592b310d0858f251` | Copied the battery mesh verbatim and supplied a minimal 1.20 material-library override that points its sole material to `magneticraft:block/battery`; no Magneticraft2 model data was used. | Codex |
| `src/main/resources/assets/magneticraft/models/block/computer.{obj,mtl}`, `src/main/resources/assets/magneticraft/textures/block/computer.png` | Nova 1.12 `models/block/obj/computer.obj` and `textures/models/computer.png` at `4108ca9bb332d11965c30e0c592b310d0858f251` | Copied the static computer mesh and texture, changed only its material-library filename, and supplied a dedicated 1.20 MTL using `magneticraft:block/computer`. Inserted media remains separate BER content. | Codex |
| `src/main/resources/assets/magneticraft/models/block/grinder_block.{obj,mtl}`, `src/main/resources/assets/magneticraft/textures/block/grinder.png` | Nova 1.12 `models/block/obj/grinder_block.obj` and `textures/models/grinder.png` at `4108ca9bb332d11965c30e0c592b310d0858f251` | Copied only the controller-local static grinder base, changed its material-library filename and supplied a singular 1.20 texture mapping. The animated full grinder mesh was intentionally excluded. | Codex |
| `src/main/resources/assets/magneticraft/models/block/hydraulic_press_base.{obj,mtl}`, `src/main/resources/assets/magneticraft/textures/block/hydraulic_press.png` | Nova 1.12 `models/block/obj/hydraulic_press_base.obj` and `textures/models/hydraulic_press.png` at `4108ca9bb332d11965c30e0c592b310d0858f251` | Copied only the static press base with a dedicated 1.20 MTL. The full OBJ's moving rod and head remain reserved for a later BER conversion. | Codex |
| `src/main/resources/assets/magneticraft/models/block/solar_panel_base.{obj,mtl}`, `src/main/resources/assets/magneticraft/textures/block/solar_panel.png` | Nova 1.12 `models/block/obj/solar_panel_base.obj` and released runtime texture `textures/blocks/multiblocks/solar_panel.png` at `4108ca9bb332d11965c30e0c592b310d0858f251` | Copied the controller-local energy-input base and mapped it to the released runtime texture because the unused legacy OBJ MTL names a missing `textures/models/solar_panel.png`. The 18 tracking panel groups remain reserved for BER conversion. | Codex |
| `src/main/java/committee/nova/mods/magneticraft/system/network/**`, `content/network/**` | Nova 1.12 `api/{energy,heat,pneumatic}/**`, `systems/tilemodules/{ModuleElectricity,ModuleHeat,ModulePipe,ModulePneumaticTube,ModuleConveyorBelt}.kt` and English guide pages at `4108ca9bb332d11965c30e0c592b310d0858f251` | Independently reimplemented the documented voltage, internal-energy, 160mB/t pipe, weighted pneumatic routing and entity-free conveyor behavior as pure Java cores plus Forge 1.20.1 adapters. No Kotlin source was mechanically translated; lifecycle, conservation, bounded routing and incremental topology were redesigned for 1.20.1. | Codex |
| `src/generated/resources/assets/magneticraft/{blockstates,models}/**/{electric_cable,heat_pipe,insulated_heat_pipe,heat_sink,iron_pipe,pneumatic_tube,pneumatic_restriction_tube,conveyor_belt}.json` | Independently generated from local Java datagen; textures reference existing Magneticraft or vanilla assets | Static geometry was recreated with cuboids and existing texture references. No Magneticraft2 model, texture, JSON or text was copied. | Codex |
| `src/main/java/committee/nova/mods/magneticraft/content/{multiblock,worldgen,computer}/**`, `src/main/java/committee/nova/mods/magneticraft/data/Advanced*Provider.java` | Nova 1.12 `features/{multiblocks,worldgen,computer}/**` and related machine tile entities at `4108ca9bb332d11965c30e0c592b310d0858f251` | Transcribed the 16 released structure layer layouts and independently reimplemented their observable behavior, bounded program execution, finite oil deposits, vanilla ore placement and generated guide data in pure Java for Forge 1.20.1. No StrutYourStuff or Magneticraft2 source, model, mixin, resource, schema or text was copied, and neither project is a runtime dependency. | Codex |
| `src/main/java/committee/nova/mods/magneticraft/integration/**`, `src/generated/resources/data/magneticraft/tinkering/**`, `src/generated/resources/assets/magneticraft/tinkering/**` | Public JEI 15.20 API, CraftTweaker 14 public Java API and Tinkers' Construct 3.11 material-data schema listed above | Independently implemented six JEI categories, the `mods.magneticraft.Recipes` recipe-manager surface and one namespaced tungsten material. No optional-mod source, bytecode, texture, language text or other resource is copied or embedded. | Codex |

## Magneticraft2 boundary

Allowed: compare package responsibilities, graph boundaries, save/load failure
modes, and public behavior visible in a running build.

Forbidden: transcribing or mechanically translating its classes, JSON schemas,
models, textures, language strings, recipes, or documentation. When it suggests
an architecture, implement that architecture independently from the Nova 1.12
behavior and Forge documentation.
