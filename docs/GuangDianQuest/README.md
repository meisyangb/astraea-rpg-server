# GuangDianQuest

> 光点任务系统 — 支持主线/支线/每日/成就四类任务，KILL/TALK/COLLECT/SUBMIT/REACH/BREAK/CRAFT/FISH 九种目标类型，NPC 对话 GUI、任务线追踪、MythicMobs/RPGItems 集成

---

## 一、简介

GuangDianQuest 是 Astraea RPG 核心任务插件，管理玩家从接取任务到完成奖励的完整生命周期。支持四种任务类型、九种目标类型、内建 NPC 对话界面与聊天框消息服务，与 MythicMobs、RPGItems 深度集成。

### 功能特性

- **四种任务类型** — 主线(MAIN)、支线(SIDE)、每日(DAILY)、成就(ACHIEVEMENT)
- **九种目标类型** — KILL(击杀)、TALK(对话)、COLLECT(拾取)、SUBMIT(提交)、REACH(到达)、USE(使用)、BREAK(破坏)、CRAFT(合成)、FISH(钓鱼)
- **NPC 对话系统** — 右键 NPC 弹出 DialogueGUI，支持 Citizens NPC 集成
- **任务线系统** — questlines.yml 定义多章节主线任务链，按 order 顺序解锁
- **GUI 菜单** — 主菜单、进行中、可接取、任务线、每日、支线、详情共七种界面
- **聊天框消息** — ChatMessageService 提供精美进度条、颜色编码、图标
- **MythicMobs 集成** — KILL 目标自动匹配 MythicMob 内部名称
- **RPGItems 集成** — COLLECT/SUBMIT 目标识别 RPGItems 物品 ID
- **奖励系统** — 经验(优先给职业阶位)、点券、物品(原版/MythicMobs/RPGItems)、命令
- **任务公告** — 可配置主线/成就任务完成全服公告
- **PlaceholderAPI** — 14 种变量 (%gdquest_*%)
- **同步存储** — 所有数据变更立即写入 YAML 文件，杜绝异步丢失

### 前置要求

| 插件 | 说明 | 必装 |
|------|------|:----:|
| RPGCore | 核心框架(调度器、事件总线、MiniMessage) | ✅ 是 |
| MythicMobs | 怪物匹配(KILL目标) | ❌ 否 |
| RPGItems | 物品匹配(COLLECT/SUBMIT目标) | ❌ 否 |
| Citizens | NPC 集成 | ❌ 否 |
| PlaceholderAPI | 变量扩展 | ❌ 否 |
| GuangDianPoints | 点券奖励 | ❌ 否 |

### 兼容性

- **服务端**: Paper 1.21+
- **Java**: Java 21+
- **消息格式**: Adventure MiniMessage

---

## 二、安装

1. 确保 RPGCore 已安装
2. 将 `GuangDianQuest.jar` 放入 `plugins/`
3. 重启服务器，自动生成 `config.yml` 和 `questlines.yml`
4. 在 `plugins/GuangDianQuest/quests/` 下创建任务 YAML
5. 使用 `/quest reload` 重载

```bash
/quest
# 查看是否正常启动
```

---

## 三、命令权限

### 3.1 玩家命令

| 命令 | 权限 | 说明 |
|------|------|------|
| `/quest` | 无 | 打开任务 GUI 主菜单 |
| `/quest list` | 无 | 查看进行中+可接取任务列表 |
| `/quest info <ID>` | 无 | 查看任务详情 |
| `/quest accept <ID>` | 无 | 接取任务 |
| `/quest complete <ID>` | 无 | 提交完成(目标全达成后) |
| `/quest abandon <ID>` | 无 | 放弃任务(进度清零) |
| `/quest daily` | 无 | 查看每日任务列表 |
| `/quest track` | 无 | 追踪进行中任务进度 |
| `/quest questline` | 无 | 查看任务线进度 |
| `/quest available` | 无 | 查看可接取任务 |
| `/quest help` | 无 | 帮助信息 |

### 3.2 管理员命令

| 命令 | 权限 | 说明 |
|------|------|------|
| `/quest reload` | `guangdian.quest.admin` | 重载任务配置和 config.yml |
| `/quest resetdaily [玩家]` | `guangdian.quest.admin` | 重置每日任务 |
| `/quest talk <NPCID> [玩家]` | `guangdian.quest.admin` | 手动触发 NPC 对话(控制台可指定玩家) |

### 3.3 聊天快捷命令

| 命令 | 说明 |
|------|------|
| `/quest:detail <ID>` | 聊天框查看详情 |
| `/quest:accept <ID>` | 聊天框接取 |
| `/quest:complete <ID>` | 聊天框提交 |

---

## 四、配置文件

### 4.1 config.yml

```yaml
# 最多同时进行的任务数
max-active-quests: 10

# 每日任务每日可完成数量上限
daily-quest-limit: 5

# 自动保存间隔(秒)
auto-save-interval: 300

# 任务完成广播设置
quest-complete-broadcast:
  main: true       # 主线广播
  achievement: true # 成就广播
  side: false
  daily: false

# 消息模板(支持 & + MiniMessage)
messages:
  quest-accepted: "&a✔ 已接取任务: {name}"
  quest-completed: "&6★ 任务完成: {name}"
  quest-abandoned: "&c✘ 已放弃任务: {name}"
```

### 4.2 任务文件格式 (quests/side/example.yml)

```yaml
example_side:
  name: "新手试炼"
  type: SIDE
  description:
    - "击败 5 只僵尸"
  prerequisites: []         # 前置任务ID
  required-level: 1          # 需要等级
  quest-line: ""             # 所属任务线ID(可选)
  order: 1                   # 任务线中顺序
  objectives:
    - type: KILL
      target: "ZOMBIE"       # 支持原版/MythicMob/RPGItems
      amount: 5
      description: "击败僵尸"
  rewards:
    experience: 100
    points: 500
    items:
      "vanilla:IRON_SWORD": 1
      "mythic:新手之剑": 1
      "rpgitems:新手之剑": 1
    commands:
      - "[console]give {player} diamond 1"
      - "[message]&a恭喜完成新手试炼!"
    messages:
      - "&e你已经踏出了冒险的第一步"
```

### 4.3 questlines.yml

```yaml
first_chapter:
  name: "第一章: 觉醒"
  description: "冒险的开始"
  type: MAIN
  quests:
    - quest_id: "f1_01"
      name: "初识世界"
      order: 1
    - quest_id: "f1_02"
      name: "第一场战斗"
      order: 2
```

---

## 五、目标类型详解

| 类型 | 触发方式 | target 格式 | amount 含义 |
|------|---------|------------|------------|
| KILL | 击杀实体 | 原版实体名 / MythicMob ID | 击杀数量 |
| TALK | 右键 NPC / 对话事件 | NPC名称 / Citizens NPC ID | 对话次数 |
| COLLECT | 捡起物品(自动) | 物品类型 / MythicMob物品 / RPGItems ID | 收集数量 |
| SUBMIT | 手持物品右键(手动) | 同上 | 提交数量 |
| REACH | 走进半径 | world,x,y,z,radius | 到达 1 次 |
| USE | 使用物品 | 物品类型 | 使用次数 |
| BREAK | 破坏方块 | 方块类型 | 破坏数量 |
| CRAFT | 合成物品 | 物品类型 | 合成数量 |
| FISH | 钓鱼上钩 | (任意) | 钓鱼次数 |

---

## 六、PlaceholderAPI 变量

| 占位符 | 说明 |
|--------|------|
| `%gdquest_total%` | 总完成任务数 |
| `%gdquest_active%` | 进行中任务数 |
| `%gdquest_daily%` | 今日已完成每日数 |
| `%gdquest_achievement%` | 成就点数 |
| `%gdquest_questline_<ID>%` | 任务线当前进度索引 |

---

## 七、数据存储

玩家数据存储于 `plugins/GuangDianQuest/playerdata/<UUID>.yml`：

```yaml
playerId: "uuid"
activeQuests:
  quest_id:
    - 5      # 目标0进度
    - 3      # 目标1进度
completedQuests:
  quest_id: 1718000000000
dailyCompletedCount: 2
dailyResetTime: 1718000000000
questLineProgress:
  first_chapter: 3
totalCompletedCount: 15
achievementPoints: 50
```

::: warning 注意
所有数据变更是**同步直接写入磁盘**的，不需要担心异步任务丢失。服务器正常关闭或 `/reload` 均会自动保存全部玩家数据。
:::

---

## 八、常见问题

**Q: 如何添加新任务？**
A: 在 `quests/main/`、`quests/side/` 或 `quests/daily/` 目录创建 YAML 文件，然后 `/quest reload`。

**Q: 任务线如何工作？**
A: 在 `questlines.yml` 定义任务线，主线任务的 `quest-line` 字段关联。玩家完成 order=N 的任务后自动解锁 order=N+1。

**Q: 每日任务何时重置？**
A: 服务器每日凌晨自动重置，也可用 `/quest resetdaily` 手动重置。

---

## 九、更新日志

### v1.1.0 (2026-06-13)
- 修复重启服务器数据丢失问题：所有保存改为同步直接写入
- 修正 onPluginDisable 执行顺序

### v1.0.0
- 初始版本：支持四种任务类型、九种目标类型、NPC对话系统、任务线追踪

---

*最后更新: 2026-06-13*
*维护者: GuangDian*
