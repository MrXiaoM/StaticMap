# StaticMap

地图画跨服插件重制版。

支持 `1.8-1.21`，需要 Paper 服务端。非 Paper 服务端运行本插件，物品展示框上的跨服地图画可能会在区块重新加载后无法正常加载。

`1.17+` 除了保存地图画图像外，可保存地图上的玩家图标、旗帜图标等到地图中。此功能在 1.16 及以下未测试，不一定可用。

# 用法

将地图放置到铁砧，花费经验等级将其转换为跨服地图画。  
适合在服务器有背包同步插件，需要在不同服区显示地图内容的情况使用。  
跨服地图画无法使用工作台、制图台等方式复制。

# 构建

请使用 Java 21 执行构建命令。别担心，构建产物兼容至 Java 8。
```shell
./gradlew build
```

# 鸣谢

+ [StaticMap 原作者](https://github.com/KujouMolean/StaticMap) 插件底层逻辑
+ [RoseWoodDev](https://repo.rosewooddev.io/repository/public/) 分发 Spigot NMS 依赖
