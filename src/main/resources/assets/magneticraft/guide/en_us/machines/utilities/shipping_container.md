# Shipping Container

The shipping container is a formed bulk-item store. It tracks one compatible item identity and a large bounded count through the controller rather than materializing every item as a slot.

Insertion and extraction are atomic and simulation-safe. A rejected type, full capacity, unloaded member, or invalid structure leaves stored content unchanged.

Break the controller only after emptying or recovering storage according to the displayed port and drop rules.
