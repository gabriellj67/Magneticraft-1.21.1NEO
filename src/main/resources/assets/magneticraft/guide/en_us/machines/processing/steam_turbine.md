# Steam Turbine

The formed steam turbine consumes steam from its declared input tank and produces native electrical energy. Build all rotor and casing layers exactly as shown.

Steam consumption is bounded by available fluid, output-network capacity, and the controller's per-tick budget. Backpressure retains steam instead of deleting it.

Unloaded members pause the turbine without force-loading chunks or losing controller state.
