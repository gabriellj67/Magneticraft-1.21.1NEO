# World Generation

Magneticraft adds metal ores, limestone families, and underground oil deposits through data-driven world generation. Configuration and tags decide where ores participate in processing; decorative limestone variants remain ordinary blocks.

Oil deposits store a finite reserve in block-entity data. A formed pumpjack extracts only from a valid loaded deposit and pauses safely when its source chunk is unavailable.

The oil chain is pumpjack → oil heater → refinery. Route every fraction to the tank declared by the controller guide; surplus fluid is not silently deleted.
