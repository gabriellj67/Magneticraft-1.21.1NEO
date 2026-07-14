# 历史模型离线转换契约

## 权威来源

视觉资产以 Nova Magneticraft 1.12 固定提交
`4108ca9bb332d11965c30e0c592b310d0858f251` 为权威，来源资产树的
SHA-256 为
`bc3937d85016780665a0fad9f4657da90c5e8747a44d379b2510c0d21f99711b`。
上游与本项目均按 GPL-2.0-only 分发，具体来源登记见
[`PORTING_SOURCES.md`](../PORTING_SOURCES.md)。被忽略的 `.references/`
只用于本地审计，不是构建输入，也不会进入制品。

固定快照包含 44 个 MCX、21 个 glTF 和 23 个旧 OBJ。MCX 与 glTF 曾由
Minecraft 1.12 专用运行时加载器处理；1.20.1 运行时不包含该加载器，也不解析
MCX、glTF 或外部 `.bin`。

## 可复现转换

机器可读清单位于 [`scripts/legacy_model_manifest.json`](../scripts/legacy_model_manifest.json)，
转换器位于 [`scripts/convert_legacy_models.py`](../scripts/convert_legacy_models.py)。
转换仅依赖 Python 3 标准库：

```powershell
py -3.14 scripts/convert_legacy_models.py
py -3.14 scripts/convert_legacy_models.py --check
```

转换器在写入前验证完整来源资产树摘要，并独占以下生成目录：

- `assets/magneticraft/models/block/legacy`：OBJ 与 MTL；
- `assets/magneticraft/models/legacy`：Forge OBJ 模型 JSON；
- `assets/magneticraft/textures/block/legacy`：去重后的历史纹理。

`--check` 在内存中重新生成全部文件，逐字节比较缺失、过期和意外文件。输出顺序、
浮点序列化、材质名和 JSON 键顺序固定；重复执行不会产生差异。

## 转换规则

- MCX 的命名部件、顶点、UV、法线与面顺序保持稳定；可按部件名拆分静态主体和
  动态部件。
- glTF 2.0 的外部 buffer、accessor、节点层级、TRS/矩阵、材质和动画采样仅在
  离线阶段解析。
- 六组历史关键帧动画各采样为 8 个烘焙帧；运行时只按服务端同步的工作状态和
  有界 tick 选择帧。
- 旧 `magneticraft:blocks/...` 纹理地址统一转换为
  `magneticraft:block/legacy/...`，不会覆盖冻结的注册 ID。
- 稳定单方块主体与长距离设备直接由方块/物品烘焙模型引用。
- 动态部件由有界 BER 引用已经烘焙的 OBJ；渲染线程不读取磁盘模型格式、不重建
  网格，也不决定游戏逻辑。
- 完整历史多方块外壳作为 `reference_baked` 进入模型烘焙注册和自动校验，用于
  逐项视觉对比。世界中的实际结构块继续表达碰撞、破坏、端口和归属，避免用一个
  控制器模型隐藏结构状态或产生重复几何；精确动态部件叠加在控制器 BER 中。

## 动态资产映射

| 内容 | 离线输出 | 运行时上限与状态来源 |
|---|---|---|
| 电动机、机械臂 | 主体 + 8 帧运动部件 | 每实例只选择 1 帧；服务端同步工作状态 |
| 粉碎机、筛选机、液压机、蒸汽机 | 8 帧历史运动组件 | 每实例只选择 1 帧；未工作时固定第 0 帧 |
| 蒸汽轮机 | 单片历史叶片 | 固定 12 片，程序化旋转，不动态扩容 |
| 太阳能板 | 历史跟踪组件 | 使用世界日时与已同步朝向；无模型重解析 |
| 风力涡轮机 | 静态轮毂 + 历史转子 | 使用服务端同步转速；96 格视距上限 |
| 电脑 | 静态机身 + 屏幕 | 仅运行状态使用全亮屏幕，不显示程序内容 |
| 采矿机器人 | 静态机身 + 螺旋桨/钻头 | 使用已同步移动和工作状态 |
| 工业燃烧室 | 静态结构 + 火焰组件 | 仅工作状态渲染火焰 |
| 长距离电力 | 电线杆/变压器静态模型 + 导线 | 每端点连接快照至多 64 条，导线至多 32 段 |

流体液面、传送带物品和长距离导线继续使用专用有界 BER，因为它们是连续状态或
运行时连接数据，不适合离线网格帧。

## 清单范围

清单目前包含 59 个逻辑资产：单方块主体、物品模型、结构模型、动态部件与完整
参考外壳。转换出的 372 个确定性文件全部由契约测试覆盖。完整逐项列表以 JSON
清单为唯一权威，文档不复制第二份易漂移清单。

明确排除且不会进入运行时资源：

- 4 个上下坡传送带 glTF；相应玩法未发布；
- `broken/iron/steel/tungsten` 4 个无消费者历史齿轮 MCX；
- 未完成窑炉原型及其他只有模型、没有完成玩法的内容。

## 验收

`LegacyVisualAssetContractTest` 验证固定来源、59 个唯一资产、8 帧预算、每个
OBJ/MTL/JSON 输出、8 项排除及运行时不含 MCX/glTF/GLB/BIN。
`ReleaseCandidateBudgetContractTest` 固定 8 帧、17 个参考模型、12 片轮机叶片和
长距离导线顶点上限。客户端冒烟必须完成全部注册模型的资源烘焙，并且日志不得
出现 Magneticraft 模型、材质或纹理加载错误。
