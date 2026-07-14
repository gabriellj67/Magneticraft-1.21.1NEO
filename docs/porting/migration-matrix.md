# Magneticraft 1.20.1 迁移契约

本目录中的 [`migration-matrix.json`](migration-matrix.json) 是 Nova 1.12 固定快照到 Forge 1.20.1 的权威行为矩阵；[`registry-id-map.json`](registry-id-map.json) 是其注册命名投影。当前 1.20.1 代码只作为候选实现，不能覆盖旧版可观察行为证据。

0.3.0 电力阶段的逐项源码位置、产品覆盖决策和验收路由见
[`electricity-0.3-evidence.md`](electricity-0.3-evidence.md)。

## 冻结边界

- 目标平台固定为 Minecraft 1.20.1、Forge 47.4.20、Java 17。
- `0.2.0` 是唯一一次破坏式重置，不读取 1.12 或早于 0.2.0 的世界、物品和 NBT。
- 从 `0.2.0` 起注册 ID 与持久化信封冻结；后续字段变化必须有向前迁移，不能再次以风格调整为由改名。
- 所有自有 NBT 键使用 `lower_snake_case`，每个所有权边界保存局部 `schema_version`。方块实体保存局部状态；关卡级连接保存到 `SavedData`；运行时图必须可以重建。
- 电、热、流体、压力和物流是独立物理域；Forge Energy 只作为 `1 J = 1 FE` 的适配层。模拟与交互由服务端权威执行。

## 关键 ID 决策

| 旧 ID | 0.2.0 冻结 ID | 理由 |
|---|---|---|
| `battery_item_low` / `battery_item_medium` | `low_voltage_battery` / `medium_voltage_battery` | 分级设备使用 tier-first 命名。 |
| `battery` | `battery_box` | 区分固定机器与便携电池。 |
| `box` | `wooden_crate` | 删除含糊的容器名称。 |
| `iron_pipe` | `iron_fluid_pipe` | 显式标记物理域。 |
| `relay` / `filter` / `transposer` | `pneumatic_relay` / `pneumatic_filter` / `pneumatic_transposer` | 避免与其他系统同名。 |
| `rf_heater` / `rf_transformer` | `forge_energy_heater` / `forge_energy_transformer` | 删除历史 RF 术语。 |
| `connector` / `energy_receiver` | `electric_connector` / `wireless_energy_receiver` | 明确电力与无线语义。 |
| `multiblock_base` / `electric_multiblock_part` / `striped_multiblock_part` | `machine_casing` / `electrical_machine_casing` / `striped_machine_casing` | 删除实现导向的 part/base 术语。 |
| `multiblock_column` | `machine_support_column` | 使用结构职责命名。 |
| `big_*` / `container` | `industrial_*` / `shipping_container` | 删除含糊大小词和通用容器名。 |
| `oil` / `hot_crude` / `plastic` / `fuel` | `crude_oil` / `heated_crude_oil` / `liquid_plastic` / `fuel_oil` | 流体 ID 表达物质与状态。 |
| `cobbled_limestone` / `cobbled_burnt_limestone` | `limestone_cobblestone` / `burnt_limestone_cobblestone` | 坚持 `<material>_<form>`，避免形态前置。 |
| `crushing_hit` / `crushing_final` | `crushing_table_hit` / `crushing_table_complete` | 声音归属到具体机器。 |
| `water_flow` / `water_flow_end` | `sluice_box_water_flow` / `sluice_box_water_flow_end` | 在 0.5.0 随淘洗槽重建；不虚报为 0.2.0 已实现。 |

`water_generator` 有意保留：旧实现是每 tick 主动向相邻储罐供水的机器，`water_source` 会错误暗示静态世界水源。`galena_ore` 也有意保留：旧 `lead` metadata 的显示名为 “Galena Ore (Lead)”，并同时参与 galena、lead、silver 标签语义。

## 共享基础设施例外

共享实现类不再拥有通用注册 ID。每个方块实体、菜单和自定义配方类型均使用其具体内容 ID；热管、气动管、单方块机器、多方块以及电脑仍复用实现，但 Forge 注册身份逐内容分离。`single_block_machine`、`advanced_multiblock`、`programmable`、`advanced_processing`、`fluid_fuel` 不得出现在运行时注册表中。

## 明确排除

- `broken_gear`、`iron_gear`、`steel_gear`、`tungsten_gear`：旧版没有真实玩法消费者。
- `air_bubble`、`wind_turbine_gap`、`moving_robot`、`pumpjack_drill`、`multiblock_gap`：可以重建内部语义，但不得作为可获取内容恢复。
- 上下坡或垂直传送带、未完成窑炉、BuildCraft 集成、1.12 世界导入和旧 Java API 二进制兼容。
- 旧网络卡的任意 TCP/SSL 出站访问；电脑只允许受限内部设备总线。
- 通过第三方硬编码名称注入的旧 OreDictionary/流体燃料配方；后续兼容只通过明确的 Forge tags、配方数据和列出的可选集成扩展。

## 完整性规则

- 矩阵固定列出 137 个旧 JSON 配方和 52 个英文指南路径，并为每个条目分配唯一决策组；路径清单和文件内容分别具有固定 SHA-256 指纹。
- 每个旧 JSON 配方成员都有同位置的 `target_paths` 叶：重建/保留项指向唯一目标数据路径，明确排除项为 `null`；六个旧电缆配方和两个旧绝缘热管配方允许有证据地折叠到同一目标。
- 注册、metadata、材料形态、状态、排除项、派生流体族和辅助注册均有固定叶数量与投影指纹；映射、材料与排除集合必须互斥。
- `normalized_row_contracts` 与 `normalized_recipe_contracts` 将每个叶连接到数据路径和行为契约；所有 `MIG-*` 验收 ID 必须解析到一个且仅一个 JUnit、GameTest、Gradle 门禁、运行矩阵或人工发布检查路由。
- 程序化配方按可执行目标 ID 列出；油加热器和炼油厂配方不能继续隐藏在 Java 常量中。
- 七种软盘预设 `user/lisp/forth/shell/basic/editor/asm` 与 BIOS 启动行为全部进入 0.7.0 契约。
- 旧 `oil_source` 的十一种持久状态映射到版本化 `oil_deposit` 方块实体载荷 `remaining_millibuckets`，而不是伪造方块状态；`full_100` 和 `empty` 原本可见，因此保留创意/管理放置入口。
- 旧版流体没有公开可放置流体方块。1.20.1 所需 source/flowing/block/bucket 注册属于适配族，世界放置不是冻结玩法承诺。

每个阶段必须依次通过 `compileJava`、连续两次无差异 `runData`、`test`、`runGameTestServer`、`build` 以及基础服务端/客户端冒烟，之后才允许创建一次中文提交。
