# Shell 1.1

Shell provides `help`, `ls`, `cd`, `mkdir`, `rm`, `format`, `free`, `fs`, `cat`, `touch`, `write`, `update_disk`, `label`, and `quarry`. Its filesystem is a versioned 128 KiB virtual disk stored in NBT, with at most 128 entries and 32 KiB per file. Paths cannot escape the virtual root and never access the host filesystem.

`quarry 10` makes a mining robot clear a bounded 10×10 square below itself using a persisted serpentine cursor. The maximum side is 32 blocks. It pauses at unloaded chunk boundaries without force-loading and stops safely if movement, permissions, energy, or inventory checks fail.

Historical `pastebin` and `update` commands are intentionally rejected because programs have no outbound network access. This also prevents programs from downloading or executing untrusted host content.
