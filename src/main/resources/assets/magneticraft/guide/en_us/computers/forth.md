# FORTH 1.1

FORTH is a stack language. Words are separated by whitespace. `2 5 + .` prints `7`; `WORDS` lists the available words. Define a word with `: square dup * ;` and invoke it with `9 square .`.

The runtime has a 64-value data stack, a 32-entry return stack, 64 memory cells, and at most 1,024 compiled instructions. `IF ELSE THEN` and `BEGIN UNTIL/AGAIN` are supported. An endless loop pauses when the tick budget is exhausted instead of blocking the server.

On a mining robot, `FRONT`/`FORWARD`, `BACK`, `LEFT`, `RIGHT`, `UP`, `DOWN`, `MINE`, and `SCAN` issue the historical device actions. `INVENTORY`, `ENERGY`, and `QUARRY` provide bounded extensions. A waiting world action retries the same instruction without advancing the program counter.
