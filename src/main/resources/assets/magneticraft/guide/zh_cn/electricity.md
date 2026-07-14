# 电力

磁场工艺电力系统模拟电压、电流、电阻和储存的焦耳。网络拓扑变更由服务端处理，稳定网络会复用图结构，不会每 tick 重建。

相邻方块使用电缆，长距离使用导线端点。Forge Energy 不会替代此物理域；变压器和加热器提供显式桥接，固定为 `1 J = 1 FE`。

继续阅读[基础概念](electricity/fundamentals.md)、[换算率](electricity/conversion_rates.md)和[输电](electricity/transportation.md)。
