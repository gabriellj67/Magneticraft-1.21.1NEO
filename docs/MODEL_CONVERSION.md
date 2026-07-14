# 历史模型运行时移植契约

## 权威来源

模型资产固定到 Nova Magneticraft 1.12 提交
`4108ca9bb332d11965c30e0c592b310d0858f251`，完整来源树 SHA-256 为
`bc3937d85016780665a0fad9f4657da90c5e8747a44d379b2510c0d21f99711b`。
机器可读清单位于 [`scripts/legacy_model_manifest.json`](../scripts/legacy_model_manifest.json)，
来源登记见 [`PORTING_SOURCES.md`](../PORTING_SOURCES.md)。

## 运行时架构

- MCX 与 glTF 2.0 文件保持原格式进入制品，不再转换为 OBJ、MTL 或逐帧 JSON。
- `LegacyModelLoader` 在资源重载边界解析并缓存不可变 `ModelScene`；静态方块与 BER 共用同一份场景。
- MCX 严格保留 `parts`、`from/to`、顶点/UV 索引和原始法线规则。
- glTF 严格保留 buffer、bufferView、accessor、场景、节点层级、材质和连续动画通道。
- 命名节点/子树选择沿用 ModelLoader 的过滤语义；动画由关键帧连续采样，不再固定成 8 个离线帧。
- 纹理通过方块图集的 `blocks/` 目录源注册，运行时不直接绑定 OpenGL 纹理或 VAO。

## 多方块显示语义

成型时，结构成员被无损快照并替换为不可见 `multiblock_gap`，控制器 BER 按 1.12.2
原始变换绘制完整 MCX/glTF 场景。解体或成员破坏时恢复原方块状态与方块实体 NBT。
这避免结构方块与完整模型重叠产生的 Z-fighting，并保持原版交互模型。

## 验收

```powershell
./gradlew.bat test --no-daemon
./gradlew.bat runData --no-daemon
./gradlew.bat runGameTestServer --no-daemon
./gradlew.bat build --no-daemon
```

`LegacySourceModelParserContractTest` 解析清单中的全部唯一 MCX/glTF 源；
`LegacyDynamicSceneContractTest` 固定命名部件、子树分区和动画名；
`LegacyVisualAssetContractTest` 验证原格式文件及 buffer 被打包；GameTest 验证 gap
投影、方块实体状态恢复、持久化和跨区块生命周期。
