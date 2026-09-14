# Pumpjack

Build the pumpjack above a valid underground oil deposit and form every displayed support layer. The controller extracts a bounded amount of crude oil from the deposit's finite reserve.

Extraction pauses when the deposit, a structure member, or the output destination is unloaded or invalid. The pumpjack never force-loads chunks and never lets the reserve become negative.

Route crude oil from the declared tank output to an oil heater.
