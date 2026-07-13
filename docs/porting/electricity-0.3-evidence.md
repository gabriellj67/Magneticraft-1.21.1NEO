# 0.3.0 电力行为来源证据

本文件把 Nova 1.12 固定快照 `4108ca9bb332d11965c30e0c592b310d0858f251`
中的可观察行为连接到 0.3.0 契约。旧代码是行为证据，Forge 1.20.1 实现不复用旧
API 或旧 NBT；当旧实现与已冻结的产品决策冲突时，以明确列出的产品决策为准。

## domain-separation

| 证据 | 旧版行为 | 0.3.0 契约 |
|---|---|---|
| `api/energy/**`、`api/heat/**`、`api/pneumatic/**` | 电、热和压力分别定义节点与能力边界。 | 物理域保持独立，不能用 Forge Energy 或一个无类型通用图替代。 |
| `systems/tilemodules/ModulePipe.kt` 与 `ModuleConveyorBelt.kt` | 流体管道与物流传送带分别维护自己的传输状态。 | 流体和物流继续独立于电、热、压力域；跨域转换只能发生在明确机器边界。 |
| `systems/config/Config.kt:80-83,219-220` | 默认 Watts/FE 与 FE/J 比率均为 `1.0`。 | Forge Energy 只作为电力边界适配器，并冻结为 `1 J = 1 FE`。 |

## adjacent-network

| 证据 | 旧版行为 | 0.3.0 契约 |
|---|---|---|
| `api/internal/energy/ElectricNode.kt:17-81` | 节点能量按 `C * V²` 表达；电流诊断累计电荷变化的绝对值。 | 保留电压、电流、电阻、电容与能量的独立量纲，所有模拟量保持有限且不得凭空增能。 |
| `api/internal/energy/ElectricConnection.kt:13-37` | 相邻与长距连接使用距离放大的总电阻、等效电容和 RC 指数项计算电荷交换。 | 特征测试固定同一 RC 交换结果；旧实现可能出现的负能量/符号下溢不作为玩法保留。 |
| `systems/config/Config.kt:80-83,219-220` | 默认 Watts/FE 与 FE/J 比率均为 `1.0`。 | Forge Energy 仅作为边界适配器，固定 `1 J = 1 FE`，模拟调用无副作用。 |
| `features/items/ToolItems.kt:82-112` | 电压表和温度计仅读取节点、向玩家显示结果，并返回 `PASS`。 | 瞬时诊断不得改图或吞掉后续交互；服务端生成权威读数。 |

## long-distance-network

| 证据 | 旧版行为 | 0.3.0 契约 |
|---|---|---|
| `features/electric_conductors/TileEntities.kt:42-54` | 连接器拥有单导线端口；旧候选实现的距离参数为 10。 | 产品冻结覆盖该候选值：连接器最大距离为 8 格。 |
| `features/electric_conductors/TileEntities.kt:127-200` | 普通电线杆使用三个同域端口并自动连接；变压电线杆同时提供三线杆端口和单线连接器端口。 | 普通杆链路最大 16 格；单线与三线端口不能直接混接，只能由变压电线杆桥接。 |
| `systems/tilemodules/ModuleElectricity.kt:135-200` | 自动连接只匹配相同端口尺寸，搜索范围为 16，输出连接上限为 16；未加载端点不被当作断线删除。 | 端点未加载时暂停，不强加载区块；重载后从 `SavedData` 重建运行时图。 |
| `systems/tilemodules/ModuleElectricity.kt:222-235` | 端点破坏时通知对端并清理导线。 | 任一端点破坏都必须删除持久边，不能留下幽灵连接。 |
| `features/items/ToolItems.kt:139-207` | 铜线卷以潜行点击保存第一端点，普通点击连接第二端点，潜行空中右键清除选择；成功连接后选择仍保留。 | 使用版本化、含维度的选择载荷；跨维度拒绝，连接编辑只在服务端执行，线卷不消耗。 |
| `systems/tilemodules/ModuleTeslaTower.kt:20-60` 与 `systems/config/Config.kt:91-92` | Tesla 塔在各轴约 32 格范围内给玩家物品和接收器输送能量，默认每目标每 tick 500 J，并受最低工作电压约束。 | 固定 32 格立方范围、500 J/t/目标和 60 V 最低电压；所有传输必须事务化且守恒。 |
| `features/electric_conductors/TileEntities.kt:285-319` | 能量接收器向背面 FE 设备输出，60 V 以下停止，60–120 V 线性提升至 400/t。 | 固定 0–400 FE/t 的电压曲线，并保持 `1 J = 1 FE`。 |
| `features/electric_machines/TileEntitys.kt:195-228`、`systems/tilemodules/ModuleWindTurbine.kt:53-205` 与 `systems/config/Config.kt:164-165` | 风机使用 80,000 J 内部缓冲，每 200 tick 重扫叶轮与前方 16 格开放度，默认最大产出 200 J/t。 | 叶轮区块未加载或受阻时安全暂停，不强加载；内部 `wind_turbine_gap` 不恢复为可获取注册项。 |

`connector` 的旧距离 10 与 0.3.0 产品上限 8 是有意差异，不得通过“贴近旧代码”放宽。
同理，跨维度拒绝、关卡级 `SavedData` 和不强加载区块是 1.20.1 的持久化与服务器安全
契约，不是对旧 TileEntity 引用格式的机械翻译。

## portable-devices

| 证据 | 旧版行为 | 0.3.0 契约 |
|---|---|---|
| `systems/config/Config.kt:179-202` | 工具容量 512,000；攻击、破坏、活塞消耗分别为 2,000、1,000、4,000；低/中电池容量为 250,000/2,500,000；电池箱槽位速率为 500/t。 | 数值固定为行为契约；物品采用 0.2.0 后的局部 `schema_version`，不读取旧 `energy` NBT。 |
| `features/items/ElectricItems.kt:53-77` | 有电钻头可采掘任意方块，石材/金属与土质材料使用不同速度，破坏非零硬度方块消耗能量。 | 破坏与消耗由服务端校验；创造模式也不豁免旧行为定义的非零硬度消耗。 |
| `features/items/ElectricItems.kt:79-144` | 活塞从玩家脚部视线向前检测 6 格，普通/潜行推力为 1.75/0.25，能量不足时不处理。 | 固定射线、推力、消耗和 `PASS` 失败语义，客户端不得伪造移动。 |
| `features/items/ElectricItems.kt:146-166` | 电锯对木、植物、藤、叶、仙人掌和蛛网加速，攻击伤害为 14。 | 保留目标材料语义与服务端能量扣除，不引入额外工具升级系统。 |
| `systems/items/ElectricItemBase.kt:20-81` | 便携物品通过 Forge Energy 能力暴露全部剩余容量，模拟收发不写入。 | 不添加全局 500 FE/调用限流；500/t 只属于电池箱槽位。能量条在非空时可见。 |

## supporting-registry-projection

0.3.0 新增的六个公开方块 ID 已在 0.2.0 主注册投影中冻结；本阶段只补齐同名所有者的
`block_entity_type` 辅助注册投影：

| 旧方块实体 ID | 冻结方块/BlockItem/方块实体 ID |
|---|---|
| `connector` | `magneticraft:electric_connector` |
| `electric_pole` | `magneticraft:electric_pole` |
| `electric_pole_transformer` | `magneticraft:electric_pole_transformer` |
| `tesla_tower` | `magneticraft:tesla_tower` |
| `energy_receiver` | `magneticraft:wireless_energy_receiver` |
| `wind_turbine` | `magneticraft:wind_turbine` |

旧 `tesla_tower_part` 由同一 `tesla_tower` 方块状态和底部所有者承接，不新增公开辅助
注册 ID；旧 `wind_turbine_gap` 继续属于明确排除的内部占位语义。

## verification-routing

- RC、电荷守恒与 FE 桥接：`ElectricalNetworkTest`、`ElectricalEnergyBridgeModuleTest`。
- 便携载荷与边界：`PortableEnergyStateTest`、`PortableElectricGameTests`。
- 长距图、距离、端口、序列化与稳定读取：`LongDistanceConnectionGraphTest` 及阶段 GameTest。
- 风机几何和产出上限：`WindTurbineMathTest` 及阶段 GameTest。
- 注册投影：`MigrationMatrixContractTest.supportingRegistryProjectionMatchesEveryCurrentJavaRegistration`。
