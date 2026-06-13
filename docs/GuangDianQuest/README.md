# GuangDianQuest

> 任务系统 - 支持主线/支线/每日任务，TALK/KILL/COLLECT 目标类型

---

## 一、简介

完整的任务系统，支持主线、支线、每日任务，多种任务目标类型和奖励系统。

### 功能特性
- **主线任务** - 19个主线任务（F1-F6）
- **每日任务** - 每日刷怪/Boss任务（可重复）
- **支线任务** - 一次性特殊任务
- **任务目标** - KILL（击杀）、TALK（对话）
- **任务线进度** - 自动追踪任务线
- **奖励系统** - 金币、经验、物品、点券、命令

---

## 二、配置文件 (`config.yml`)

```yaml
# 主配置
settings:
  save-interval: 300
  max-active-quests: 5
  max-daily-quests: 3
```

### 任务配置格式 (`quests/main/`)
```yaml
# 示例: 主线任务 F1_01
name: "初入冒险"
type: MAIN
description: "欢迎来到 RPG 世界！开始你的冒险之旅"
objectives:
  - type: TALK
    target: "冒险导师"
    count: 1
prerequisites: []
required_level: 1
questline: main_story
order: 1
rewards:
  points: 10
  experience: 100
  money: 1000
  items:
    - "rpg:新手之剑:1"
```

### 每日任务配置 (`quests/daily/`)
```yaml
name: "每日-第一层讨伐"
type: DAILY
description: "前往第一层击杀怪物"
objectives:
  - type: KILL
    target: "寄生幼体"
    count: 20
repeatable: true
daily_weight: 10
rewards:
  points: 5
  experience: 50
  money: 500
```

---

## 三、命令权限

| 命令 | 权限 | 说明 |
|------|------|------|
| `/quest list` | 无 | 查看任务列表 |
| `/quest accept <ID>` | 无 | 接受任务 |
| `/quest progress` | 无 | 查看任务进度 |
| `/quest abandon <ID>` | 无 | 放弃任务 |
| `/questadmin give <玩家> <ID>` | `quest.admin` | 管理员给予任务 |
| `/questadmin reset <玩家>` | `quest.admin` | 重置玩家任务数据 |

---

## 四、任务线一览

| 楼层 | 任务数 | 说明 |
|:----:|:-----:|------|
| F1 | 3 | 初入冒险 |
| F2 | 3 | 骷髅墓地 |
| F3 | 3 | 死灵深渊 |
| F4 | 3 | 寄生森林 |
| F5 | 4 | 深渊卡尔萨斯 |
| F6 | 3 | 终焉奈克萨斯 |

---

*最后更新: 2026-06-13*
