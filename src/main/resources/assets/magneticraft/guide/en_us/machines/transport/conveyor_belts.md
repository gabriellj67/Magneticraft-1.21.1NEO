# Conveyor Belts

Conveyors keep a bounded internal item payload and move it along the belt direction. Right-click inserts or removes an item where allowed, and compatible adjacent belts automatically form flat corners.

At an occupied or rejecting destination the payload waits under backpressure. A successful endpoint accepts the item exactly once; chunk unload preserves the carried stack.

Sloped and vertical conveyors remain explicitly excluded because the released gameplay never completed them.
