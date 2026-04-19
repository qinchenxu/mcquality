# 装备系统详细设计

本文档描述“装备品质”模组从当前的单一品质倍率系统，演进到“稀有度 + 随机词条 + 主动技能 + 粒子效果 + I 键详细属性面板”的目标结构。它不是宣传文案，而是后续实现时使用的技术设计说明。

## 1. 目标

第一阶段目标：

- 保留当前品质概念，但将其降级为“稀有度层级”
- 引入装备类型分池的随机词条系统
- 支持一位小数的随机百分比
- 支持少量主动技能词条
- 支持技能粒子效果
- 支持主手持武器时按 I 键查看独立详细属性面板
- 保持掉落与合成两个入口继续工作
- 保证单人与多人环境下的数据和表现一致

第二阶段目标：

- 扩展复杂被动机制
- 扩展更多主动技能触发方式
- 增加更完整的战斗状态与层数机制
- 扩展 UI 分页、筛选和更多说明信息

## 2. 当前实现概况

当前代码以单一品质为核心：

- 装备通过合成事件和全局 loot modifier 获得品质
- 品质写入 CUSTOM_DATA
- 品质会按固定倍率缩放部分属性
- tooltip 显示品质名称和倍率

当前结构的限制：

- 一个品质只能表达一个总倍率，无法表达多个词条来源
- 无法区分主动技能和被动词条
- 无法为面板展示准备结构化数据
- 无法为粒子效果建立稳定的技能到表现映射

## 3. 总体架构

目标架构分为六层：

1. 稀有度层
2. 词条定义层
3. 词条实例层
4. 属性聚合层
5. 主动技能与表现层
6. 客户端展示层

职责划分如下：

- 稀有度层决定词条数量、词条品质范围、技能词条概率
- 词条定义层描述某个词条是什么、适用于什么装备、怎么显示、怎么结算
- 词条实例层记录某个装备实际抽到了什么数值
- 属性聚合层把多个词条汇总成最终属性修改
- 主动技能与表现层负责右键技能、冷却、粒子和网络同步
- 客户端展示层负责 tooltip 摘要和 I 键详情面板

## 4. 数据模型

### 4.1 稀有度

当前 EquipmentQuality 可以继续保留，但语义改为 rarity，而不是最终强化结果。

建议保留字段：

- id
- color
- weight
- maxAffixCount
- minAffixCount
- skillAffixChance
- displayPriority

建议新增规则：

- 稀有度只决定“能抽多好”，不直接决定所有属性数值
- 稀有度越高，词条数量上限越高
- 稀有度越高，技能词条出现概率越高

示例分布：

| 稀有度 | 词条数范围 | 技能词条概率 |
| --- | --- | --- |
| 磨损 | 0-1 | 0% |
| 普通 | 1-2 | 0% |
| 罕见 | 2-3 | 8% |
| 稀有 | 3-4 | 15% |
| 史诗 | 4-5 | 30% |
| 传说 | 5-6 | 45% |

## 4.2 词条实例

每件装备需要写入一组词条实例，而不是只写一个品质字符串。

建议在 CUSTOM_DATA 下使用单一根节点：

```text
EquipmentQuality
  version: 2
  rarity: "rare"
  affixes: [ ... ]
  active_skill: { ... }
  passive_effects: [ ... ]
  ui_flags: { ... }
```

建议的 affix 实例字段：

- id: 词条定义 ID
- tier: 词条品质或词条阶级
- value: 实际随机值，支持一位小数
- unit: percent 或 flat
- display_order: 用于 UI 排序
- category: offense、defense、utility、skill、passive
- rolled_from: 可选，记录原始范围，便于调试

示例：

```json
{
  "id": "weapon.attack_damage_percent",
  "tier": "rare",
  "value": 12.5,
  "unit": "percent",
  "display_order": 10,
  "category": "offense"
}
```

## 4.3 主动技能数据

主动技能不建议完全依赖词条文本推导，应有独立结构。

建议字段：

- id
- trigger
- cooldown_ticks
- particle_style
- scaling_key
- description_key
- value_bundle

示例：

```json
{
  "id": "weapon.arc_slash",
  "trigger": "right_click",
  "cooldown_ticks": 80,
  "particle_style": "arc",
  "scaling_key": "attack_damage",
  "value_bundle": {
    "damage_multiplier": 1.6,
    "radius": 3.5
  }
}
```

## 4.4 被动效果数据

被动效果可以先和 affix 一起存储，也可以保留独立数组。第一阶段建议保留独立数组，为第二阶段复杂机制预留结构。

建议字段：

- id
- trigger_type
- chance
- magnitude
- duration_ticks
- internal_cooldown_ticks

## 5. 资源定义

建议把词条和技能改为数据驱动资源，而不是全部写在 Java 枚举里。

建议资源目录：

- src/main/resources/data/equipmentquality/affixes/weapon
- src/main/resources/data/equipmentquality/affixes/armor
- src/main/resources/data/equipmentquality/affixes/tool
- src/main/resources/data/equipmentquality/skills
- src/main/resources/assets/equipmentquality/particles

### 5.1 词条定义 JSON 示例

```json
{
  "id": "weapon.attack_damage_percent",
  "equipment_types": ["weapon"],
  "category": "offense",
  "value": {
    "min": 4.0,
    "max": 18.0,
    "precision": 1,
    "unit": "percent"
  },
  "attribute_effect": {
    "target": "minecraft:generic.attack_damage",
    "mode": "multiply_base"
  },
  "text": {
    "name_key": "affix.equipmentquality.weapon.attack_damage_percent",
    "desc_key": "affix_desc.equipmentquality.weapon.attack_damage_percent"
  },
  "weight": 20,
  "conflict_group": "damage_primary",
  "skill_affix": false
}
```

### 5.2 技能定义 JSON 示例

```json
{
  "id": "weapon.arc_slash",
  "equipment_types": ["weapon"],
  "trigger": "right_click",
  "cooldown_ticks": 80,
  "particle_style": "arc",
  "name_key": "skill.equipmentquality.weapon.arc_slash",
  "desc_key": "skill_desc.equipmentquality.weapon.arc_slash",
  "values": {
    "damage_multiplier": 1.6,
    "radius": 3.5
  },
  "weight": 8
}
```

## 6. 随机生成流程

目标流程：

1. 判断物品是否属于支持类型
2. 判断物品是否已经拥有新结构数据
3. 抽取稀有度
4. 根据稀有度决定词条数量
5. 根据装备类型抽取词条池
6. 处理冲突组去重
7. 为每个词条生成实际数值
8. 根据概率决定是否抽中技能词条
9. 写入 CUSTOM_DATA
10. 重新计算属性组件
11. 更新 tooltip 摘要或 lore

关键约束：

- 同一冲突组只允许保留一个主词条
- 同一装备不应抽到重复技能
- 已生成装备不可在普通读取时再次重随机
- 旧版装备需要兼容读取

## 7. 属性聚合策略

当前实现是按品质直接缩放全部属性。后续需要改为按词条聚合。

建议聚合过程：

1. 读取原始 ATTRIBUTE_MODIFIERS
2. 建立聚合上下文
3. 将词条实例按 category 和 target attribute 归类
4. 计算每个属性的总增益
5. 输出新的 ATTRIBUTE_MODIFIERS

建议支持三类模式：

- multiply_base
- add_flat
- special_handler

说明：

- multiply_base 用于攻击伤害、护甲、采掘速度等常规百分比词条
- add_flat 用于固定数值增加
- special_handler 用于攻速这类不能直接线性叠加的属性

关键要求：

- 不重复叠加旧值
- 不因为重新读取或复制物品再次放大
- 与原版组件结构兼容

## 8. 主动技能系统

### 8.1 第一阶段触发方式

第一阶段只支持：

- 右键触发

I 键仅用于打开详情面板，不参与技能释放。

### 8.2 技能执行结构

推荐结构：

- 客户端发起使用动作或直接通过物品使用入口进入服务端逻辑
- 服务端校验主手物品、技能数据、冷却状态
- 服务端执行技能效果
- 服务端向附近客户端广播粒子表现
- 客户端仅负责渲染和 UI 反馈

### 8.3 第一批技能模板

建议先实现 2 到 3 个：

- Arc Slash：前方扇形范围伤害
- Guard Pulse：瞬时护盾或减伤脉冲
- Shock Burst：近距离冲击波

这些技能的共同点：

- 目标明确
- 数值容易调试
- 粒子表现容易绑定
- 不依赖复杂状态系统

## 9. 粒子效果设计

粒子不只是装饰，它必须和技能生效时机一致。

建议设计为风格枚举或注册表：

- arc
- shield
- burst
- aura

每种风格至少包含：

- 默认粒子类型
- 颜色或亮度倾向
- 发射形状
- 持续时间或脉冲次数

建议最小实现：

- 服务端确认技能释放后发送一个包含位置、朝向、风格、强度的消息
- 客户端收到后按风格生成粒子

## 10. I 键详细属性面板

### 10.1 打开条件

第一阶段建议条件：

- 玩家存在
- 当前无其他屏幕阻塞
- 主手持有受支持武器
- 按下 I 键

### 10.2 面板职责

面板只负责读和展示，不负责强化、洗练、切换词条或其他修改操作。

### 10.3 面板内容分区

建议固定为五个区块：

1. 顶部基础信息
2. 左侧核心属性
3. 中部词条列表
4. 右侧技能说明
5. 底部操作提示

建议字段：

- 物品名称
- 稀有度
- 物品类型
- 主属性摘要
- 所有词条与精确数值
- 主动技能名称、说明、冷却、触发方式
- 被动技能名称、说明、触发方式
- 粒子风格描述
- 操作提示，例如“右键释放技能”

### 10.4 tooltip 与面板分工

建议职责明确分离：

- tooltip：只展示稀有度、核心词条摘要、技能提示摘要
- I 键面板：展示完整说明和精确值

## 11. 客户端结构建议

建议新增的客户端类：

- 客户端初始化类
- 按键注册类
- 详情面板 Screen
- 粒子注册类
- 粒子 Provider 类
- 客户端事件订阅类

建议新增职责：

- 客户端初始化类负责注册 key mapping、屏幕相关入口和粒子 provider
- 按键注册类负责 I 键绑定
- Screen 负责读取主手物品并渲染详细信息
- 客户端事件类负责监听按键并打开面板

## 12. 网络与同步

当前仓库没有自定义网络层，第一阶段必须补最小链路。

建议至少包含两类消息：

- 技能表现广播消息
- 必要的技能状态或冷却同步消息

说明：

- 面板数据优先从手持物品本地读取，尽量不为面板单独做请求包
- 粒子表现由服务端触发后广播，避免纯客户端假动作
- 如果冷却完全走原版物品冷却系统，可以减少自定义同步负担

## 13. 本地化与文案

需要补全中英文词条：

- affix 名称
- affix 说明
- skill 名称
- skill 说明
- 面板标题
- 分类标签
- 操作提示
- 冷却与触发方式描述
- 粒子风格或技能风格描述

## 14. 向后兼容

旧版本装备目前只有 quality 标签。建议兼容策略：

- 读取到旧结构时，识别为旧版物品
- 可选择自动迁移为 rarity-only 数据
- 若暂不迁移，也必须保证不会崩溃
- 旧版物品至少仍能显示基础品质信息

## 15. 推荐实现顺序

1. 重构数据结构和读取写入工具
2. 定义 affix 与 skill 的资源格式
3. 改造掉落与合成生成逻辑
4. 改造属性聚合
5. 接入基础主动技能
6. 接入粒子注册和同步
7. 实现 I 键详细属性面板
8. 收尾 tooltip 和本地化
9. 做兼容与验证

## 16. 验证清单

- 掉落物生成后能稳定拥有稀有度和词条
- 合成产物生成后不会丢失数据
- 词条数和技能概率符合预期
- 一位小数随机值显示正确
- 属性不会重复叠加
- 右键技能在单人和多人下都能正常生效
- 粒子只在真实技能触发时播放
- I 键面板只在合法条件下打开
- 面板显示的数据与实际结算一致
- 旧版物品不会读崩或写坏

## 17. 非目标

以下内容不属于第一阶段：

- 完整职业化技能树
- 装备养成 UI
- 洗练、重铸、词条锁定
- 复杂多段连击状态机
- 大规模平衡性打磨

第一阶段的重点是把结构做稳，而不是把所有玩法一次塞满。