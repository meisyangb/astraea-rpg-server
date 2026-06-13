# GuangDianQuest

> 任务系统 - 支持主线、支线、每日、成就等多种任务类型

---

## 一、简介

GuangDianQuest 是一个功能完整的任务系统插件，支持多种任务类型、任务链、自动追踪等功能。

### 功能特性

- **多种任务类型** - 击杀、收集、到达、对话、自定义等
- **任务分类** - 主线任务、支线任务、每日任务、成就
- **任务链** - 支持多阶段任务链
- **自动追踪** - 自动追踪任务进度
- **任务奖励** - 支持经验、金币、物品等奖励
- **任务GUI** - 美观的任务界面

### 前置要求

- **必需**: [RPGCore](/RPGCore/README)
- **可选**: PlaceholderAPI, Vault (经济支持)

### 兼容性

- **服务端**: Paper 1.21+
- **Java**: Java 21+
- **依赖插件**: RPGCore

---

## 二、安装

### 2.1 安装步骤

1. 确保已安装 RPGCore
2. 下载 `GuangDianQuest.jar`
3. 将 jar 文件放入 `plugins` 文件夹
4. 重启服务器
5. 编辑 `plugins/GuangDianQuest/quests/` 目录下的任务配置

### 2.2 验证安装

```bash
/gdq info          # 查看插件信息
/quest list       # 查看可用任务
/gdq reload       # 重载配置
```

---

## 三、快速开始

### 3.1 创建击杀任务

在 `plugins/GuangDianQuest/quests/main/` 目录下创建 `kill_zombies.yml`:

```yaml
# 击杀任务配置
# 最后更新: 2026-06-11

id: "kill_zombies"
name: "§e初出茅庐"
description: "击杀10只僵尸"

# 任务类型：kill, collect, reach, talk, custom
type: "kill"

# 任务目标
target: "ZOMBIE"
amount: 10

# 任务分类：main, side, daily, achievement
category: "main"

# 任务等级要求
level-required: 1

# 任务前置（需要先完成的任务）
dependencies: []

# 任务奖励
rewards:
  exp: 100
  gold: 50
  items:
    - "iron_sword:1"
    - "bread:5"

# 任务描述（Lore）
lore:
  - "§7这是你的第一个任务"
  - "§7去击杀10只僵尸吧！"
  - ""
  - "§6奖励："
  - "§7- 经验: 100"
  - "§7- 金币: 50"
  - "§7- 物品: 铁剑 x1, 面包 x5"

# 自动接受任务
auto-accept: false

# 可重复完成（每日任务等）
repeatable: false

# 冷却时间（秒，仅可重复任务）
cooldown: 0
```

### 3.2 创建收集任务

创建 `plugins/GuangDianQuest/quests/side/collect_iron.yml`:

```yaml
# 收集任务配置
id: "collect_iron"
name: "§b铁矿收集者"
description: "收集20个铁锭"

type: "collect"
target: "IRON_INGOT"
amount: 20

category: "side"
level-required: 5

rewards:
  exp: 200
  gold: 100
  items:
    - "diamond:2"

lore:
  - "§7帮助村庄收集铁矿"
  - "§7需要20个铁锭"
```

### 3.3 创建到达任务

创建 `plugins/GuangDianQuest/quests/main/reach_spawn.yml`:

```yaml
# 到达任务配置
id: "reach_spawn"
name: "§a初到贵地"
description: "到达服务器出生点"

type: "reach"
# 目标位置
location:
  world: "world"
  x: 0
  y: 64
  z: 0
  radius: 5  # 到达半径

category: "main"
level-required: 1

rewards:
  exp: 50
  gold: 20

lore:
  - "§7到达服务器出生点"
  - "§7坐标: 0, 64, 0"
```

---

## 四、命令权限

### 4.1 玩家命令

| 命令 | 权限 | 说明 |
|------|------|------|
| `/quest` | 无 | 打开任务界面 |
| `/quest list` | 无 | 查看可用任务 |
| `/quest start <任务ID>` | 无 | 开始任务 |
| `/quest quit` | 无 | 放弃当前任务 |
| `/quest status` | 无 | 查看任务进度 |
| `/quest completed` | 无 | 查看已完成任务 |

### 4.2 管理员命令

| 命令 | 权限 | 说明 |
|------|------|------|
| `/gdq reload` | `guangdianquest.admin` | 重载配置文件 |
| `/gdq info` | `guangdianquest.admin` | 查看插件信息 |
| `/gdq give <玩家> <任务ID>` | `guangdianquest.admin.give` | 给予玩家任务 |
| `/gdq complete <玩家> <任务ID>` | `guangdianquest.admin.complete` | 完成玩家任务 |
| `/gdq reset <玩家> <任务ID>` | `guangdianquest.admin.reset` | 重置玩家任务 |

---

## 五、配置文件

### 5.1 任务配置文件

任务配置文件位于 `plugins/GuangDianQuest/quests/` 目录下，按分类组织：

```
quests/
├── main/           # 主线任务
│   ├── kill_zombies.yml
│   └── reach_spawn.yml
├── side/           # 支线任务
│   └── collect_iron.yml
├── daily/          # 每日任务
└── achievement/    # 成就任务
```

**配置项说明：**

| 配置项 | 类型 | 说明 |
|--------|------|------|
| `id` | String | 任务唯一ID |
| `name` | String | 任务显示名称 |
| `description` | String | 任务描述 |
| `type` | String | 任务类型：kill, collect, reach, talk, custom |
| `target` | String | 任务目标（根据类型不同） |
| `amount` | Number | 任务目标数量 |
| `category` | String | 任务分类：main, side, daily, achievement |
| `level-required` | Number | 等级要求 |
| `dependencies` | List | 前置任务ID列表 |
| `rewards` | Map | 任务奖励 |
| `lore` | List | 任务描述 Lore |
| `auto-accept` | Boolean | 是否自动接受 |
| `repeatable` | Boolean | 是否可重复完成 |
| `cooldown` | Number | 冷却时间（秒） |

### 5.2 config.yml

主配置文件。

```yaml
# 主配置
debug: false

# 任务设置
settings:
  # 最大同时进行的任务数量
  max-active-quests: 5
  
  # 是否启用任务自动追踪
  enable-auto-tracking: true
  
  # 任务追踪显示位置
  tracking-position: "actionbar"  # actionbar, bossbar, sidebar
  
  # 任务完成后是否自动接受下一个任务
  auto-accept-next: false
```

---

## 六、任务类型详解

### 6.1 击杀任务 (kill)

```yaml
type: "kill"
target: "ZOMBIE"  # 实体类型
amount: 10
```

### 6.2 收集任务 (collect)

```yaml
type: "collect"
target: "IRON_INGOT"  # 物品类型
amount: 20
```

### 6.3 到达任务 (reach)

```yaml
type: "reach"
location:
  world: "world"
  x: 0
  y: 64
  z: 0
  radius: 5
```

### 6.4 对话任务 (talk)

```yaml
type: "talk"
target: "Villager"  # NPC 名称
# 需要配合 GuangDianNPC 插件使用
```

### 6.5 自定义任务 (custom)

```yaml
type: "custom"
# 需要开发者自己实现逻辑
```

---

## 七、API 接口

### 7.1 获取任务管理器

```java
// 获取任务管理器
QuestManager manager = RPGCore.getServiceRegistry().get(QuestManager.class);

// 获取任务
Quest quest = manager.getQuest("kill_zombies");

// 给予玩家任务
manager.giveQuest(player, "kill_zombies");

// 完成玩家任务
manager.completeQuest(player, "kill_zombies");

// 获取玩家活跃任务
List<QuestProgress> activeQuests = manager.getActiveQuests(player);
```

### 7.2 监听任务事件

```java
// 监听任务接受事件
@EventHandler
public void onQuestAccept(QuestAcceptEvent event) {
    Player player = event.getPlayer();
    Quest quest = event.getQuest();
    
    // 处理任务接受逻辑
}

// 监听任务完成事件
@EventHandler
public void onQuestComplete(QuestCompleteEvent event) {
    Player player = event.getPlayer();
    Quest quest = event.getQuest();
    
    // 处理任务完成逻辑
    player.sendMessage("§a恭喜完成task：" + quest.getName());
}
```

---

## 八、常见问题

### Q: 任务不显示？

**A:** 检查以下事项：
1. 确保任务配置文件格式正确
2. 确保任务 ID 唯一
3. 使用 `/gdq reload` 重载配置

### Q: 任务进度不更新？

**A:** 检查以下事项：
1. 确保任务类型配置正确
2. 确保任务目标配置正确
3. 检查控制台是否有错误信息

### Q: 如何创建任务链？

**A:** 使用 `dependencies` 配置项设置前置任务：

```yaml
dependencies:
  - "quest_1"
  - "quest_2"
```

---

## 九、更新日志

### v1.0.0 (2026-06-11)
- 🎉 初始版本发布
- ✨ 支持多种任务类型
- ✨ 支持任务链
- ✨ 支持任务自动追踪

---

## 十、下一步

- 📖 查看 [命令权限](/GuangDianQuest/command) 了解所有命令
- ⚙️ 查看 [配置文件](/GuangDianQuest/config) 了解详细配置
- 🔌 查看 [API接口](/GuangDianQuest/api) 了解开发者接口

---

*最后更新: 2026-06-11*
