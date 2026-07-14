# Fluid Pipes

Iron fluid pipes form a dedicated fluid network between compatible Forge fluid handlers. Side configuration controls whether a face is passive, an active output, or disabled.

Each transfer first simulates destination acceptance, then commits only the accepted amount. A full destination applies backpressure; unloaded members stop safely without force-loading.

Use small tanks as local buffers and the wrench interaction to inspect or change pipe sides.
