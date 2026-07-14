# FORTH 1.1

FORTH 是一种栈式语言，各单词使用空白分隔。`2 5 + .` 会输出 `7`，`WORDS` 会列出可用单词。使用 `: square dup * ;` 定义单词，再以 `9 square .` 调用。

运行时提供 64 项数据栈、32 项返回栈、64 个内存单元和最多 1,024 条编译指令，并支持 `IF ELSE THEN` 与 `BEGIN UNTIL/AGAIN`。无限循环耗尽本 tick 预算后会暂停，不会阻塞服务器。

在采矿机器人上，`FRONT`/`FORWARD`、`BACK`、`LEFT`、`RIGHT`、`UP`、`DOWN`、`MINE` 和 `SCAN` 会执行历史设备动作；`INVENTORY`、`ENERGY` 与 `QUARRY` 是有界扩展。世界操作需要等待时，同一条指令会在下一 tick 重试，程序计数器不会提前移动。
