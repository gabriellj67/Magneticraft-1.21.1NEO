# Computers and Mining Robots

Magneticraft computers run bounded, server-authoritative programs. Choose FORTH, Lisp, or Shell in the editor, enter source code, and upload it while the device menu remains open. A program is limited to 8 KiB of UTF-8 source, 64 instructions and four device calls per tick, and 2,048 terminal characters.

Computers can control redstone. Mining robots additionally expose movement, rotation, mining, scanning, inventory, energy, and quarry commands. World actions check ownership, range, permissions, energy, inventory capacity, and loaded chunks; they never force-load a chunk.

Floppy disks are the only portable program medium. User disks are writable. FORTH, Lisp, Shell, BASIC, editor, and assembler presets share the same `floppy_disk` item state; the last three are retained as inert historical media because their released implementations were incomplete.
