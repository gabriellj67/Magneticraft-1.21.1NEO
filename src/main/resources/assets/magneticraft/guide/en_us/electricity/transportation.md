# Electricity Transportation

Electric cables connect adjacent compatible faces. Breaking a cable or device removes its runtime edge, and saved local state reconstructs the graph after reload.

Use a copper wire coil to select one endpoint while sneaking, then use it on a compatible second endpoint. Connectors reach at most 8 blocks; electric-pole links reach at most 16 blocks. Cross-dimension links are rejected.

Unloaded remote endpoints pause transfer. Connections do not force-load chunks, and breaking either endpoint cleans the saved link before it can transfer again.
