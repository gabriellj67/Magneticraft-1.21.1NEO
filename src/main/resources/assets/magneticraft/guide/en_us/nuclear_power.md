# Nuclear Power

Nuclear power has two layers: basic operation and optional engineering optimization. A first grid connection only requires fuel, a reactor, a steam train, and spent-fuel cooling. The engineering view, centrifuge enrichment, condensate recovery, and high-capacity primary pumps improve efficiency and control but are not entry requirements.

## Two fuel routes

The basic route processes uranium ore into concentrate, then directly crafts one standard fuel assembly from `5 uranium concentrate + 4 zirconium-alloy cladding`. It only needs the uranium processor and fuel fabricator. The route is short, but produces about 67.5% as much fuel from the same uranium as the advanced route.

The advanced route converts concentrate to uranium hexafluoride, separates it in a centrifuge cascade, and turns low-enriched uranium into pellets. It supports low, standard, and high enrichment assemblies and yields about 1.48 times as much useful fuel per uranium input. JEI labels the key recipe as either the direct starter route or the advanced centrifuge route.

## Three PWR presets

- 7x7x7 Basic PWR: generous cooling for a first grid connection.
- 9x9x7 Standard PWR: balanced power, fuel utilization, and cooling demand.
- 11x11x7 Large PWR: four rod groups improve response but require a stronger primary and steam train.

Blueprints use `F` for standard fuel, `W` for coolant channels, `R` for reflectors, `A` through `D` for control-rod groups, and `I` for instrumentation. Presets are reliable starting points, not mandatory layouts. Freeform designs still change power density, fuel utilization, safety margin, and control response.

The controller side is the structure front. Connect the cold primary inlet, hot primary outlet, station-power port, and instrumentation port on the sides shown by the structure view. Structure members do not show separate status messages; facing, formation, and port state are centralized in the controller screen.

## Startup and control

A newly placed controller starts with protection interlocks and constant-power control enabled. The Basic view exposes startup, shutdown, SCRAM, fuel loading, target power, and reset, plus four summary cards for output, fuel efficiency, safety margin, and control response.

The Engineering view retains rod groups A-D, temperature and load-following modes, engineering override, detailed interlocks, pressure, barrier integrity, and dose rate. Protection blocks startup or triggers SCRAM when the structure, cooling, station power, instrumentation, or core state is unsafe. Engineering override is not a normal startup step.

The shortest generation chain is `PWR → steam generator → turbine`. The turbine may exhaust steam directly, so the condenser and cooling tower are optional efficiency upgrades for condensate recovery. A basic reactor can operate on the loop's natural capacity; large or high-power designs should use a primary circulation pump.

## Shutdown, spent fuel, and accidents

Decay heat remains after shutdown, so cooling cannot be stopped immediately. The Basic view groups accident detail into Normal, Cooling warning, Core damage, and Meltdown or release; the Engineering view retains every detailed stage.

The spent-fuel pool is a 5x5x4 multiblock. Fuel first reaches the temperature threshold for safe transfer, then must continue decaying before it is safe to seal in a storage cask. The pool screen reports transferable, sealable, and total assembly counts separately. Radiation, contamination, vessel breach, and containment failure remain real consequences, so prepare dosimetry, protection, and an emergency shutdown path before operation.
