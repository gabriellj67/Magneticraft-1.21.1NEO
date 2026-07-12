# Legacy model conversion inventory

## Scope and authority

This document is the release-time inventory for the client models in the fixed
Nova Magneticraft 1.12 snapshot. The behavioral and asset authority is commit
`4108ca9bb332d11965c30e0c592b310d0858f251` of
`Nova-Committee/Magneticraft`, distributed with the GNU GPL v2 license text.
The local port uses the SPDX expression `GPL-2.0-only`, so the listed assets may
be adapted when their exact source path and
conversion are recorded in `PORTING_SOURCES.md`.

The snapshot contains 44 MCX files, 21 glTF files and 23 OBJ files. MCX and
glTF were loaded by the 1.12-only `com.cout970.modelloader` runtime. Forge
1.20.1 cannot load either format directly. No compatibility model loader is
shipped by this port: conversion is deterministic and offline, while runtime
movement is implemented by a `BlockEntityRenderer` driven by synchronized
machine state.

The 23 OBJ files are not referenced by the released 1.12 registrations. They
are useful export sources, but the MCX/glTF model and the observable 1.12
renderer remain authoritative when the two differ.

## Conversion rules

- Static geometry uses generated vanilla models or Forge's OBJ loader.
- Every copied OBJ receives a dedicated MTL. Old `magneticraft:blocks/...` and
  `magneticraft:models/...` texture IDs are rewritten to the 1.20
  `magneticraft:block/...` namespace.
- MCX conversion preserves `parts[].name` and its quad range. Named moving
  parts are exported separately instead of being baked into the static body.
- glTF conversion consumes the matching external `.bin` offline. Static nodes
  are exported to OBJ; animation channels are reduced to explicit progress,
  speed, angle or boolean state consumed by the BER. Raw glTF parsing is never
  performed on the render thread.
- A multiblock controller may use a one-block static base model. Geometry that
  spans the formed structure belongs to the BER and needs expanded render
  bounds; it must not be hidden in an out-of-block baked model.
- Inventory models contain only stable geometry. World-only fluids, moving
  items, belts, rotors, doors and panels are excluded.

## MCX inventory (44)

All paths below are relative to
`assets/magneticraft/models/{block,item}/mcx/`.

| Assets | Purpose | Disposition |
|---|---|---|
| `battery.mcx`, `computer.mcx` | Electrical storage and computer terminal bodies | Use the companion `block_battery.obj` and `computer.obj`. The computer's inserted disk remains a BER item. |
| `conveyor_belt.mcx`, `conveyor_belt_base.mcx` | Complete belt item model and static belt chassis | Audited but not adopted. The legacy body is 0.8125 blocks high, while the accepted 1.20 collision is 4/16 and the parcel BER uses a 0.28-block item height. The generated low cuboid is the release implementation. |
| `crushing_table.mcx` | Manual crushing-table body | Retain the generated cuboid model. Its companion OBJ names a unified texture that is absent from the fixed snapshot, while the released top/side/bottom textures are complete. |
| `feeding_trough.mcx`, `feeding_trough_inv.mcx` | Trough world and inventory bodies | Companion `feeding_trough.obj` and `feeding_trough_tiny.obj` are candidates for a later static pass; food piles remain BER content. |
| `solar_panel.mcx` | Solar-panel multiblock, including individually rotating panel groups | Use `solar_panel_base.obj` only for the static controller base. Export the 18 `PanelN-M` groups separately for the BER. |
| `combustion_chamber.mcx` | Furnace body and `Door` group | Offline split into a static shell and a procedural door BER. |
| `conveyor_belt_anim.mcx`, `conveyor_belt_corner_anim.mcx` | Moving straight and corner belt surfaces | Offline conversion is required only for animated belt rendering. Corner rendering remains deferred until corner transport behavior exists. |
| `conveyor_belt_corner_base.mcx` | Corner belt chassis | Defer: the 1.20 block currently has horizontal facing only and no corner state. |
| `container.mcx` | Bulk-storage multiblock body | Offline static conversion; render as formed multiblock geometry, not as an oversized controller model. |
| `mining_robot.mcx` | Robot body, propellers and five drill groups | Offline split into body, `prop.*` and `drill.*`; movement and rotation are procedural BER transforms. |
| `oil_heater.mcx`, `pumpjack.mcx`, `refinery.mcx` | Oil-processing multiblock bodies | The Nova 1.12 pumpjack/refinery renderers treat these meshes as static. The 1.20 release adds a synchronized procedural pumpjack overlay; refinery geometry remains static while its working/fluid state is rendered independently. Exact legacy mesh conversion is a fidelity follow-up. |
| `shelving_unit.mcx` | Shelf frame and `Crate1` through `Crate24` | Offline split; the BER displays crate groups from the installed chest count. |
| `sluice_box.mcx`, `sluice_box_inv.mcx`, `sluice_box_water.mcx` | Sluice body, gravel level and water surface | Offline split. The static body may be baked; gravel height and water sprite are BER layers. |
| `small_tank.mcx` | Tank shell and base | Offline split. The fluid surface height and fluid sprite are BER state. |
| `solar_mirror.mcx` | Mirror frame and tracking mirror group | Offline split; the mirror group tracks the sun and selected solar tower in the BER. |
| `solar_tower.mcx`, `tube_light.mcx` | Solar receiver and decorative tube light | Offline static conversion is optional after P0 dynamic assets. |
| `electric_cable.mcx`, `electric_furnace.mcx`, `gasification_unit.mcx`, `heat_sink.mcx`, `insulated_heat_pipe.mcx`, `iron_pipe.mcx`, `iron_pipe_dark.mcx`, `steam_boiler.mcx` | Simple machines and adjacent network geometry | Keep the current generated cuboid/multipart models. Only heat tint, if restored, needs a light dynamic layer. |
| `connector.mcx`, `electric_pole.mcx`, `electric_pole_inv.mcx`, `electric_pole_transformer.mcx`, `electric_pole_transformer_inv.mcx` | Long-span electrical connector and pole family | Defer because these blocks are not registered in the current port. Companion OBJ exports remain available for a future task. |
| `fluid_pipe.mcx`, `wind_turbine.mcx` | Removed fluid-pipe variant and wind generator | Defer until the corresponding gameplay content is restored. |
| `broken_gear.mcx`, `iron_gear.mcx`, `steel_gear.mcx`, `tungsten_gear.mcx` | 3D upgrade-gear items | Defer because these item IDs are not registered. A generated item model is preferred unless 3D gears return as a confirmed requirement. |

## glTF inventory (21)

All paths are relative to `assets/magneticraft/models/block/gltf/`. Each entry
depends on its declared external buffer; `tesla_tower_inv.gltf` shares the
world model's buffer.

| Assets | Purpose | Disposition |
|---|---|---|
| `big_combustion_chamber.gltf` | Multiblock shell with `fire_on` and `fire_off` nodes | Export the shell and both status groups offline. Select the fire group from server-authoritative working state. |
| `big_electric_furnace.gltf` | Furnace shell, dark interior and input/output belt assembly | Export static shell/interior groups. Texture state and belt motion are BER concerns. |
| `electric_engine.gltf` | Piston engine with four effective animation channels | Export named nodes and drive a procedural progress animation. |
| `grinder.gltf` | Grinder multiblock with three effective animation channels | Use `grinder_block.obj` only for the static controller base; convert the moving grinder assembly for the BER. |
| `hydraulic_press.gltf` | Press multiblock with one effective animation channel | Use `hydraulic_press_base.obj` for the static base and the companion OBJ's `Rod`/`Head` groups for the BER. |
| `inserter.gltf` | Inserter arm with ten transition animations and an item attachment node | Highest-risk conversion. Preserve transition names and reduce the animation curves to explicit arm transforms; do not bake the carried item. |
| `sieve.gltf` | Sieve multiblock with three effective animation channels | Export static frame and moving sieve groups separately. `sifter.obj` is only a visual candidate, not runtime authority. |
| `steam_engine.gltf` | Steam engine with 21 effective channels plus a separately transformed gearbox lid | Highest-risk multiblock conversion. Preserve node hierarchy and use synchronized speed/lid state in the BER. |
| `steam_turbine.gltf` | Turbine shell and reusable blade group | Export shell and `blade.*` separately; rotate the repeated blade group procedurally. The single glTF channel is not used by the released renderer. |
| `conveyor_belt_up_base.gltf`, `conveyor_belt_up_anim.gltf`, `conveyor_belt_down_base.gltf`, `conveyor_belt_down_anim.gltf` | Sloped belt chassis and moving surfaces | Defer until upward/downward conveyor behavior is implemented. |
| `big_steam_boiler.gltf` | Static boiler multiblock shell | Current controller placeholder is sufficient for the first pass; convert later for visual fidelity. |
| `pneumatic_tube.gltf`, `pneumatic_tube_inv.gltf`, `pneumatic_restriction_tube.gltf`, `pneumatic_restriction_tube_inv.gltf` | Connected tube parts and inventory variants | Keep the generated connected geometry and existing parcel BER. Their single animation channels are not behaviorally significant. |
| `energy_receiver.gltf`, `tesla_tower.gltf`, `tesla_tower_inv.gltf` | Long-range electrical receiver and Tesla tower | Defer because these blocks are not registered in the current port. |

## OBJ inventory (23)

All paths are relative to `assets/magneticraft/models/block/obj/`. None of
these paths is referenced by the released 1.12 block registration.

| Assets | Purpose | Disposition |
|---|---|---|
| `block_battery.obj` | Battery body | Reused as `battery.obj`; dedicated MTL already supplied. |
| `computer.obj` | Computer body | Reused as the static computer model with a dedicated MTL. |
| `conveyor_belt.obj` | Straight belt body | Not reused. Its 0.8125-block height conflicts with the accepted 4/16 collision and low parcel-render contract; changing that contract is outside Task 7. |
| `crushing_table.obj` | Crushing-table body | Not reused: `crushing_table.mtl` references missing `blocks/machines/crushing_table`, so the complete generated top/side/bottom model is safer. |
| `feeding_trough.obj`, `feeding_trough_tiny.obj` | World and inventory trough bodies | Valid later candidates; not part of the first static closure. |
| `grinder.obj`, `grinder_block.obj` | Complete grinder and one-block controller base | Reuse only `grinder_block.obj` now. The full model includes moving groups and is reserved for BER conversion. |
| `hydraulic_press.obj`, `hydraulic_press_base.obj` | Complete press and static base | Reuse only `hydraulic_press_base.obj` now. Split `Rod` and `Head` from the full OBJ for the BER later. |
| `solar_panel.obj`, `solar_panel_base.obj` | Complete panel assembly and static energy-input base | Reuse only `solar_panel_base.obj` now. Its broken legacy `models/solar_panel` material reference is replaced with the released multiblock texture. |
| `sifter.obj`, `table_sieve.obj` | Alternate sieve and table-sieve exports | Visual comparison candidates only; the released glTF/MCX models remain authoritative. |
| `beehive_kiln.obj`, `kiln.obj`, `kiln_shelf.obj` | Unregistered kiln prototypes | Defer; no corresponding current content exists. |
| `electric_connector.obj`, `electric_pole.obj`, `electric_pole_tiny.obj`, `pole_adapter.obj`, `pole_adapter_tiny.obj` | Unregistered long-span electrical family | Defer with the gameplay content. |
| `incendiary_generator.obj` | Older combustion-generator export | Do not substitute it for `combustion_chamber.mcx` without an in-game equivalence review. |

## Static assets integrated in this pass

The following source geometry is intentionally limited to stable parts:

- `computer.obj` and `computer.png`;
- `grinder_block.obj` and `grinder.png`;
- `hydraulic_press_base.obj` and `hydraulic_press.png`;
- `solar_panel_base.obj` and the released multiblock `solar_panel.png`.

Each OBJ has a local one-material MTL and is generated through the same Forge
OBJ helper as the existing battery. The grinder, hydraulic press and solar
panel models are controller-local bases only. The release supplies synchronized
programmatic dynamic layers through `SingleBlockMachineRenderer`,
`AdvancedMultiblockRenderer`, `ComputerRenderer` and `MiningRobotRenderer`.
What remains deferred is a part-for-part conversion of the legacy MCX/glTF
named meshes, not the BER or server-authoritative animation contract itself.

## Current registered-content release disposition

| Content class | Release implementation | Explicit boundary |
|---|---|---|
| Computer and stable controller bodies | Audited static OBJ/MTL plus generated item model | Inserted media and status are BER layers. |
| Grinder, hydraulic press and solar panel | Controller-local static OBJ plus synchronized procedural BER | Exact named moving groups are a later visual-fidelity enhancement. |
| Other registered machines and sixteen advanced multiblocks | Generated vanilla cuboids/multipart models plus bounded state-driven BER overlays | No runtime MCX/glTF parser or hidden structure-wide baked geometry. |
| Pneumatic tubes and conveyors | Generated connection geometry plus existing parcel/item BER | Sloped/corner conveyors are not registered and remain outside the release. |
| Electric poles, Tesla towers and long-span wires | Not registered | Reconsider Strut Your Stuff only when this gameplay family returns. |

## Strut Your Stuff decision

Strut Your Stuff commit `26a43d7c7de1cdbebefd2990ed2b15c150199dd0`
targets Java 17, Minecraft 1.20.1 and Forge 47 as version
`1.1.0+mc1.20.1`. It provides point-to-point strut collision, clipping,
interaction and optional Flywheel rendering. It does not convert MCX/glTF,
animate machine parts, render fluids or validate multiblocks.

It is therefore not a Task 7 dependency. The current port has no registered
electric-pole or other long-span strut consumer, while adding the library
would expand the Mixin, optional Flywheel and load-matrix surface. The fixed
snapshot declares MIT in mod metadata but contains no root license text, so no
source or resource is copied. Re-evaluate it only if selectable, collidable
long-span electrical wires become an explicit future requirement.

## Release validation status

The release gate ran `runData` twice, generated-resource contract tests,
`build`, a dedicated server and client model-bakery/runtime smoke tests. The
second unchanged datagen run wrote zero files, and client logs contained no
missing Magneticraft OBJ, MTL, texture, material or blockstate diagnostics.
See the [release-readiness report](porting/release-readiness.md) for counts,
hashes and the optional-mod matrix. Manual screenshot comparison at every GUI
scale and part-for-part legacy animation fidelity remain recommended visual QA,
not an assertion made by this inventory.
