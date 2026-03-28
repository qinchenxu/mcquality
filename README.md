# 装备品质

Minecraft Forge 1.21.1 模组。为合成产物和战利品箱中的装备随机分配品质，并按品质倍率调整属性。

## 品质档位

- 磨损: -20%
- 普通: +10%
- 罕见: +30%
- 稀有: +50%
- 史诗: +80%
- 传说: +100%

## 当前效果

- 合成获得的护甲、武器、工具会自动生成品质
- 战利品表产出的护甲、武器、工具会自动生成品质
- 护甲会影响护甲值、护甲韧性、击退抗性
- 武器和工具会影响攻击力、攻击速度
- 物品 tooltip 会显示品质和倍率

## 运行

1. 安装 Java 21
2. 在工程目录执行 `gradle genIntellijRuns` 或 `gradle genEclipseRuns`
3. 执行 `gradle runClient`
4. 游戏目录可在 IDE 运行配置里改到 `/Volumes/KINGSTON/hmcl`

如果本机没有安装 Gradle，可以先安装后再生成 wrapper，或直接在 IDE 中导入 Gradle 工程。