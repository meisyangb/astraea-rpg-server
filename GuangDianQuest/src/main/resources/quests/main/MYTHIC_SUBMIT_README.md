# GuangDianQuest MythicMobs 材料提交功能

## 功能概述

GuangDianQuest 现在支持 MythicMobs 材料提交功能，包括两种目标类型：

1. **COLLECT（收集）** - 自动拾取触发，不消耗物品
2. **SUBMIT（提交）** - 手动触发（右键点击），会消耗物品

## 支持的物品识别方式

### 1. 标准物品
直接使用 Minecraft 材质名称：
```yaml
- type: COLLECT
  target: "DIAMOND"
  amount: 5
  description: "收集钻石"
```

### 2. COLLECT 方式识别

#### 2.1 通过材质名称
```yaml
- type: COLLECT
  target: "DIAMOND"
  amount: 5
```

#### 2.2 通过 material: 前缀明确指定
```yaml
- type: COLLECT
  target: "material:EMERALD"
  amount: 3
```

#### 2.3 通过 mythic: 前缀识别 MythicMobs 物品
```yaml
- type: COLLECT
  target: "mythic:MagicCrystal"
  amount: 5
```

### 3. SUBMIT 方式识别

#### 3.1 通过材质名称
```yaml
- type: SUBMIT
  target: "DRAGON_SCALE"
  amount: 10
```

#### 3.2 通过 mythic: 前缀识别 MythicMobs 物品
```yaml
- type: SUBMIT
  target: "mythic:RareMaterial"
  amount: 3
```

#### 3.3 直接使用 MythicMobs PDC type
```yaml
- type: SUBMIT
  target: "HeroSwordFragment"
  amount: 5
```

## 物品识别优先级

当配置 `target: "xxx"` 时，系统按以下顺序识别：

1. 检查物品是否具有 `mythicmobs:type` PDC 标签
   - 如果有且值等于 "xxx"，则匹配成功
2. 检查物品材质名称是否等于 "xxx"
3. 如果 target 包含冒号 (`:`)，则按前缀处理
   - `mythic:物品ID` - 检查 mythicmobs:type PDC 标签
   - `material:材质名` - 检查物品材质名称

## 使用示例

### 示例1：收集和提交任务
```yaml
name: "材料收集者"
type: MAIN
objectives:
  - type: COLLECT
    target: "DIAMOND"
    amount: 10
    description: "收集钻石"
  - type: SUBMIT
    target: "mythic:AncientCore"
    amount: 3
    description: "提交远古核心"
rewards:
  points: 100
  experience: 1000
```

### 示例2：MythicMobs 专属物品任务
```yaml
name: "收集稀有材料"
type: SIDE
objectives:
  - type: SUBMIT
    target: "mythic:DragonScale"
    amount: 20
    description: "提交龙鳞碎片"
  - type: SUBMIT
    target: "mythic:PhoenixFeather"
    amount: 5
    description: "提交凤凰羽毛"
rewards:
  points: 200
  experience: 2000
```

## 注意事项

### SUBMIT 类型的特殊行为

1. **消耗物品** - 提交时物品会被消耗
2. **右键触发** - 玩家需要右键点击手持物品
3. **实时反馈** - 系统会显示提交进度和完成提示
4. **智能消耗** - 根据任务进度自动计算需要消耗的数量

### MythicMobs 物品识别

MythicMobs 自定义物品会在其持久化数据容器（PDC）中存储 `mythicmobs:type` 标签。
系统通过读取此标签来识别物品。

示例 MythicMobs 配置：
```yaml
MagicCrystal:
  Id: NETHER_STAR
  Display: "&b魔法水晶"
  Models:
    - key mythicmobs type value MagicCrystal
```

## 调试提示

如果物品无法被识别，请检查：

1. 物品是否具有正确的 `mythicmobs:type` PDC 标签
2. 物品的材质名称是否正确
3. 配置中的 target 是否与物品 ID 完全匹配（区分大小写）
4. 玩家是否正在接取对应的任务

## 事件流程

### COLLECT 类型
1. 玩家拾取物品
2. 系统检查所有活动任务的 COLLECT 目标
3. 如果物品匹配，增加进度
4. 物品保留在玩家背包

### SUBMIT 类型
1. 玩家右键点击手持物品
2. 系统检查所有活动任务的 SUBMIT 目标
3. 如果物品匹配，消耗物品并增加进度
4. 显示提交反馈消息
5. 完成后提示玩家领取奖励