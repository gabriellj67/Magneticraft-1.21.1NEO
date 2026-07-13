# 0.4.0 热、流体与物流行为证据

本文冻结 0.4.0 的行为权威、目标版安全边界和验收映射。Nova 1.12 固定快照是已发布玩法的行为来源；与 1.20.1 服务端生命周期冲突的旧缺陷按本文件中的目标契约重建。

## 热域

- 标准环境温度固定为 `298.15 K`（25°C）。默认热节点质量为 `1 kg`、导热率为 `73 W/(m·K)`、铁摩尔质量为 `55.845 g/mol`，气体常数为 `8.3144598`。
- 相邻热节点按调和导热率交换等量热量，每条加载边每 tick 仅计算一次；热管无传输上限和线路损失。
- 普通热管保留发布版接触灼伤：低于 `80°C` 不伤害、恰好 `80°C` 造成 `20` 点伤害、高于 `80°C` 造成 `2` 点伤害；这个反直觉边界来自发布版 `when` 分支顺序，保温热管始终不伤害实体。普通热管、保温热管的中心内缩分别为 `4 px`、`3 px`。
- 散热器支持六向放置，背面不连接，其余五面可连接；每 tick 最多移除 `T - 298.15` 焦耳，不得降到环境温度以下。

主要来源：

- `.references/nova-1.12/src/main/kotlin/com/cout970/magneticraft/misc/Heat.kt`
- `.references/nova-1.12/src/main/kotlin/com/cout970/magneticraft/api/internal/heat/HeatNode.kt`
- `.references/nova-1.12/src/main/kotlin/com/cout970/magneticraft/api/internal/heat/HeatConnection.kt`
- `.references/nova-1.12/src/main/kotlin/com/cout970/magneticraft/features/heat_machines/Blocks.kt`
- `.references/nova-1.12/src/main/kotlin/com/cout970/magneticraft/features/heat_machines/TileEntities.kt`

## 流体域

- 每根铁质流体管拥有独立、持久化的 `160 mB` 单元和 `4 px` 中心内缩；加载连通分量通过动态聚合视图对外呈现，不把运行图作为唯一数据源。聚合语义与旧版 `FluidHandlerConcatenate` 一致：成员顺序稳定，各成员可以保存不同流体，无参数排放只连续排放第一个非空成员的同种流体。
- 旧版侧状态默认为 `PASSIVE`，并按 `PASSIVE → ACTIVE → DISABLED → PASSIVE` 循环。`PASSIVE` 每 tick 从相邻 handler 向加载分量拉取并暴露可填充、可排放的聚合 capability；`ACTIVE` 每 tick 从加载分量向相邻 handler 推送且不暴露 capability；`DISABLED` 同时断开拓扑和 capability。自动拉取或推送均为每侧每 tick 至多 `160 mB`。
- 中间管卸载时运行图立即分裂。卸载管中的流体只保留在其方块实体数据中，加载侧不可访问；重载注册后重新加入聚合视图。所有邻居访问都必须先确认目标区块已加载。
- 小型储罐容量为 `32,000 mB`，接受任意流体。底部自动导出默认关闭；启用后按目标真实接受量导出，不增加人工 `1,000 mB/t` 上限。拆除后流体与导出开关保存在物品 NBT 中，物品提示显示实际流体和数量，重新放置时恢复。

主要来源：

- `.references/nova-1.12/src/main/kotlin/com/cout970/magneticraft/systems/tilemodules/ModulePipe.kt`
- `.references/nova-1.12/src/main/kotlin/com/cout970/magneticraft/systems/tilemodules/pipe/PipeNetwork.kt`
- `.references/nova-1.12/src/main/kotlin/com/cout970/magneticraft/systems/tilemodules/ModuleFluidExporter.kt`
- `.references/nova-1.12/src/main/kotlin/com/cout970/magneticraft/features/fluid_machines/Blocks.kt`
- `.references/nova-1.12/src/main/kotlin/com/cout970/magneticraft/features/fluid_machines/TileEntities.kt`

## 气动物流

- 气动载荷进度范围为 `0..128`，完整穿过一段管道需要 16 tick。普通管路由附加权重为 0，限制管附加权重为 100；压力状态不改变物流速度。
- 路由使用带权搜索，服务边界硬限制为 4096 个访问节点。等价首边按持久游标轮询，缓存只复用与拓扑有关的数据并按拓扑版本失效；终点接受能力每次事务重新模拟。
- 目标区块未加载时载荷原地暂停；已加载但拓扑失效时清除旧方向并重新寻路；已加载但目标满时保持载荷和方向并施加回压。
- Relay、Filter 和 Transposer 使用有界、持久化 FIFO。每 5 tick 尝试一次输出；目标满时不丢件，开放且安全的输出空间可生成物品实体。`output_blocked` 是由当前目标与区块加载状态重建的运行时派生值，不持久化；重载后的第一次输出尝试会重新建立回压。
- Filter 和 Transposer 的九个过滤位是旧版虚拟样本：设置时复制一个 count=1 的样本但不消耗手中物品，空手点击清除且不返还物品，拆除机器也不生成样本掉落。空过滤器允许全部，匹配忽略数量并比较物品、损伤值和完整 NBT。
- Transposer 输入侧存在物品 capability 时只检查该库存；即使库存为空或全部被过滤，也不会继续拾取同格物品实体。只有不存在 capability 时才走实体拾取路径。

目标版安全偏离：

- 旧版无界载荷与端点队列统一限制为 64，防止存档和 tick 工作量无界增长。
- 不复刻旧版“无路即丢弃”、损坏缓冲反序列化和可能跨区块查询的缺陷。
- 压力节点的容量、导通和传输率属于 1.20.1 目标版内部契约；旧版仅为限制管提供物流权重证据。

主要来源：

- `.references/nova-1.12/src/main/kotlin/com/cout970/magneticraft/api/pneumatic/PneumaticBox.java`
- `.references/nova-1.12/src/main/kotlin/com/cout970/magneticraft/api/internal/pneumatic/PneumaticUtils.kt`
- `.references/nova-1.12/src/main/kotlin/com/cout970/magneticraft/api/internal/pneumatic/PneumaticBuffer.kt`
- `.references/nova-1.12/src/main/kotlin/com/cout970/magneticraft/systems/tilemodules/ModulePneumaticTube.kt`
- `.references/nova-1.12/src/main/kotlin/com/cout970/magneticraft/systems/tilemodules/ModulePneumaticEndpoint.kt`
- `.references/nova-1.12/src/main/kotlin/com/cout970/magneticraft/systems/tilemodules/ModuleRelay.kt`
- `.references/nova-1.12/src/main/kotlin/com/cout970/magneticraft/systems/tilemodules/ModuleFilter.kt`
- `.references/nova-1.12/src/main/kotlin/com/cout970/magneticraft/systems/tilemodules/ModuleTransposer.kt`

## 传送带与机械臂

- 传送带维护服务端权威的有限内部载荷。二维 `16×16` 占用模型使用 `4×4` 载荷碰撞，支持左右直行、短路、长路和转角共八条水平路径。
- 自动转角由相邻输入和输出方向解析；短路径每 tick 前进 2，其余路径每 tick 前进 1。堵塞时载荷停在安全边界并向上游传播回压。
- 右键整组放入、空手取出最靠前载荷；持有传送带时交互返回给原版放置流程。末端优先输出正前方库存，正前方无目标时允许输出前下方库存。
- 明确不恢复上坡、下坡、阶梯和垂直传送带。
- 机械臂先模拟目标真实容量，再从同高、低一格或容器矿车中提取该数量。默认搬运 8 个、动作延迟 10 tick；堆叠升级改为 64，速度升级改为 5 tick，两者不叠加到其他维度。
- 机械臂九个过滤位同样是非所有权虚拟样本，支持白/黑名单、标签、损伤值和 NBT；空白名单拒绝全部，空黑名单允许全部。世界拾取和投递均由显式开关控制，机械臂内部携带槽和升级槽不对外暴露。
- 地面拾取和投递均优先低一格，再尝试同高位置；携带槽拒绝速度与堆叠升级，升级仅能进入两个专用槽。
- `broken_gear`、`iron_gear`、`steel_gear`、`tungsten_gear` 没有发布玩法消费者，继续明确排除。

主要来源：

- `.references/nova-1.12/src/main/kotlin/com/cout970/magneticraft/systems/tilemodules/ModuleConveyorBelt.kt`
- `.references/nova-1.12/src/main/kotlin/com/cout970/magneticraft/api/conveyorbelt/Route.kt`
- `.references/nova-1.12/src/main/kotlin/com/cout970/magneticraft/systems/tilemodules/conveyorbelt/BitMap.kt`
- `.references/nova-1.12/src/main/kotlin/com/cout970/magneticraft/systems/tilemodules/ModuleInserter.kt`
- `.references/nova-1.12/src/main/resources/assets/magneticraft/guide/en_us/machines/transport/2-conveyor-belts.md`

## 统一事务与验收

物品和流体自动转移遵循：模拟目标接受量、把待转移数量封装为本次事务的临时载荷、提交目标，并按目标实际返回的余量更新或恢复源。若第三方 handler 在模拟后改变状态，源只移除目标实际接收的数量，不能同时保留完整源载荷和部分目标载荷。所有模拟调用必须可重复且无副作用。

验收至少覆盖：

- `MIG-TRANSFER-EXACTLY-ONCE-001`：正常、部分接受、满载和重载路径总量守恒。
- `MIG-TRANSFER-SIMULATE-001`：重复模拟不改变源、目标、路由游标或载荷进度。
- `MIG-TRANSFER-UNLOAD-001`：跨未加载区块暂停，不强加载；重载后只恢复一次。
- `MIG-CONVEYOR-CORNER-001`：八路径、双向转角、合流碰撞和链式回压。
- `MIG-INSERTER-FILTER-001`：九槽过滤与六个选项逐一映射。
- `MIG-LOGISTICS-ROUTE-001`：4096 硬上限、旧版权重阈值、远端公平轮询和拓扑版本失效。
- `MIG-FLUID-COMPONENT-001`：已加载分量容量、分裂、重连、模拟与总量守恒。
- `MIG-HEAT-LEGACY-001`：环境温度、默认材料、传热、散热器和热管接触行为。

## 0.4.0 门禁记录

2026-07-14 在 Forge 47.4.20、Java 17 环境完成：

- `compileJava` 与 `compileGameTestJava`：通过。
- 连续两次 `runData`：通过，第二次 `written: 0`。
- `test`：171/171 通过。
- `runGameTestServer`：94/94 通过。
- `build`：通过。
- `runServer` 冒烟：专服完成世界加载并进入 `Done`；stderr 为空，未发现客户端类加载、重复注册或网络线程错误。
- `runClient` 冒烟：声音引擎与方块纹理图集加载完成；stderr 为空，未发现缺失模型或纹理。
