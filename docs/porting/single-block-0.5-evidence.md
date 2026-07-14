# 0.5.0 单方块机器行为证据

本文冻结 0.5.0 的单方块机器行为权威、目标版安全边界和验收路由。Nova 1.12 固定快照是已发布玩法的行为来源；当前 1.20.1 实现只有在通过这些契约后才保留。

## single-block-catalog

| 范围 | Nova 1.12 行为证据 | 0.5.0 目标契约 | 真实验收入口 |
|---|---|---|---|
| 压碎台、木箱、淘洗槽、装配台 | `.references/nova-1.12/src/main/kotlin/com/cout970/magneticraft/features/manual_machines/{Blocks,TileEntities,Containers}.kt` | 命中次数、配方、双格朝向、链式复位和幽灵图样点击语义由服务端执行 | `MachineFrameworkGameTests`、`SingleBlockMachineGameTests` |
| 小型储罐 | `.references/nova-1.12/src/main/kotlin/com/cout970/magneticraft/features/fluid_machines/{Blocks,TileEntities}.kt` | 单流体持久化、容器原子交互、可切换向下主动输出以及侧面能力保持 0.4.0 已冻结行为 | `HeatFluidGameTests`、`SingleBlockMachineGameTests` |
| 喂食槽、供水器、机械臂、气动中继器、过滤器与转置器 | `.references/nova-1.12/src/main/kotlin/com/cout970/magneticraft/features/automatic_machines/{Blocks,TileEntities}.kt` | 400 刻喂食节拍；供水器无需环境激活并向每个已加载相邻侧独立输出 20 mB/t；过滤、方向、速度与整组升级保留；物品与气动域分离 | `SingleBlockMachineGameTests`、`PneumaticEndpointGameTests` |
| 蓄电、电炉、无限能源、气闸、热电堆、FE 变压器与发动机 | `.references/nova-1.12/src/main/kotlin/com/cout970/magneticraft/features/electric_machines/{Blocks,TileEntitys}.kt` | 电池不暴露外部 FE；电、FE 与热保持独立端口；气闸只扫描已加载区域；无限能源持续维持 125 V | `MachineFrameworkGameTests`、`EnergyThermalMachineGameTests`、`AirlockGameTests`、`SingleBlockMachineGameTests`、`BaseContentGameTests` |
| 燃烧室、蒸汽锅炉、气化装置、砖炉 | `.references/nova-1.12/src/main/kotlin/com/cout970/magneticraft/features/heat_machines/{Blocks,TileEntities}.kt` | 燃料、流体、温度、配方进度、侧面能力和主动输出按机器局部状态持久化；模拟能力调用无副作用 | `EnergyThermalMachineGameTests`、`SingleBlockMachineGameTests` |
| 电加热器与 Forge Energy 加热器 | `.references/nova-1.12/src/main/kotlin/com/cout970/magneticraft/features/electric_machines/Blocks.kt`、`.references/nova-1.12/src/main/kotlin/com/cout970/magneticraft/features/heat_machines/TileEntities.kt` | 注册所有权与热转换实现分别按旧版实际源码归属；电、FE 与热端口不得合并 | `EnergyThermalMachineGameTests`、`SingleBlockMachineGameTests` |
| 数据、指南与可选配方入口 | 上述旧源码以及 `.references/nova-1.12/src/main/resources/assets/magneticraft/guide/{en_us,zh_cn}` | `migration-matrix.json#single_block_machine_catalog` 是 24 项目标 ID、来源、可获取性、生成资源和测试定位的闭合目录；21 项通用机器由 `SingleBlockMachineDefinition` 投影，压碎台、电池箱和电炉显式纳入同一目录 | `MigrationMatrixContractTest`、`SingleBlockMachineDefinitionContractTest`、`AdvancedGuideDataProviderTest`、`GuideRepositoryTest`、`GeneratedDataContractTest`、`OptionalIntegrationContractTest` |

旧指南与已发布源码冲突时，以可执行源码为准：压碎台采用实际的 5/4/3 次命中，淘洗链最大长度采用实际的 10，喂食槽接受小麦、小麦种子和胡萝卜。旧版没有机器级红石门控，因此 0.5.0 明确冻结为 `ignored`，不新增未经行为证据支持的控制模式。

每项当前指南资源固定为 `assets/magneticraft/guide/machines/<target_path>.json`，生存可获取机器同时具有 `data/magneticraft/recipes/crafting/<target_path>.json`；`infinite_energy_source` 是唯一无生存配方的创意专用机器。压碎台、淘洗槽、气化装置与热电堆另映射各自的数据配方目录；装配台、砖炉和电炉分别消费原版 crafting 或 smelting 配方类型。

目标版保留以下安全修正，它们不改变正常玩法结果：流体容器交互原子化且失败不吞物品；NBT 过滤严格匹配；配方处理进度跨保存恢复；所有世界修改、能量消耗和菜单动作由服务端校验；邻接查询跳过未加载区块且不触发强加载。

## creative-and-internal-boundary

| 内容 | 决策 | 证据与边界 |
|---|---|---|
| `infinite_energy_source` | 保留公开创意/管理方块 | Nova 1.12 `electric_machines/Blocks.kt` 以旧 ID `infinite_energy` 公开注册；目标实现持续维持 125 V，不提供生存配方 |
| `oil_deposit` | 保留公开创意放置入口 | 作为后续 0.6.0 油藏生成的管理工具保留，现阶段不把内部结构占位公开为物品 |
| `air_bubble` | 仅内部运行时方块 | 气闸可在服务端创建和衰减；不得注册 BlockItem、战利品或生存配方 |
| 移动物体、结构间隙及未完成原型 | 明确排除 | 不进入创造物品栏、配方、战利品或指南；不得借兼容层恢复旧占位 ID |
| `tube_light` | 保留公开内容 | 它是历史公开方块，不属于内部空气或结构占位，不能被内部内容过滤器误删 |

公开/内部边界由注册投影、创造物品清单、配方和战利品共同验证；仅检查 Java 注册声明不足以证明内容不可获取。

## stage-gates

2026-07-14 的 0.5.0 收口结果如下：

| 门禁 | 结果 | 可复核证据 |
|---|---|---|
| 编译 | 通过 | `compileJava`、`compileTestJava`、`compileGameTestJava` 强制重跑成功 |
| 数据生成 | 通过 | 连续两次 `runData` 成功；对 `.cache` 之外 1271 个生成资源执行 SHA-256 清单比较，两次聚合摘要均为 `8860630acc70e7f4e51ab33c3f4ea21256b7b6a6234b7eea6d074ab9c717576f` |
| JUnit | 通过 | `test --rerun-tasks`：187 项，0 失败、0 错误 |
| Forge GameTest | 通过 | 排除可选 Jade 的基础类路径运行：120 项全部通过；气闸重复运行隔离、服务端权限、保存重载和 capability 生命周期行为均由真实关卡测试覆盖 |
| 构建 | 通过 | `build --rerun-tasks` 成功，主制品与源码制品均生成 |
| 专服冒烟 | 通过 | 隔离目录 `build/tmp/base-server-run` 启动至 `Done (2.968s)`；日志无客户端类加载、重复注册、缺失映射、网络线程或可选 Jade 链接错误 |
| 客户端冒烟 | 通过 | 隔离目录 `build/tmp/base-client-run` 完成声音引擎与方块纹理图集加载；无缺失模型/纹理、重复注册或可选 Jade 链接错误 |

基础运行时通过仅用于验证的 Gradle init 脚本从开发类路径排除用户工作区中的 Jade 实验依赖，没有修改或纳入该实验配置。客户端日志中的 Realms 本地鉴权警告来自无有效会话的开发启动，不属于模组资源、注册或网络错误。
