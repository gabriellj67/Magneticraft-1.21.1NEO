# Feeding Trough

The feeding trough periodically searches its bounded loaded area for a valid pair of breedable animals and consumes the matching feed item only when feeding succeeds.

Supported feed includes the machine's declared wheat, carrot, and seed choices. The server validates animal state, distance, cooldown, and inventory before mutation.

No animal or chunk is force-loaded, and a failed feeding attempt preserves the stored item.
