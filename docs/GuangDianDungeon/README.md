# GuangDianDungeon

> 光点副本系统 — 支持多 Boss 阶段、复活机制、动态难度、MythicMobs 集成、RPGCore 事件驱动的完整副本框架

---

## 一、简介

GuangDianDungeon 提供完整的副本流程管理：入场检测、Boss 战阶段切换、玩家复活、通关奖励和退出传送。与 MythicMobs 深度集成，通过 RPGCore EventBus 驱动。

### 功能特性

- **多阶段 Boss 战** — Boss 血量百分比触发阶段切换
- **复活机制** — 副本内死亡可选复活或退出
- **动态难度** — 根据参与人数调整 Boss 属性
- **MythicMobs 集成** — Boss 通过 MythicMob ID 配置
- **RPGCore 事件** — 入场/通关/Boss击杀事件通知
- **奖励结算** — 通关后发放点券、经验、物品
- **退出传送** — 副本结束时传送回入场点

### 前置要求

| 插件 | 说明 | 必装 |
|------|------|:----:|
| RPGCore | 核心框架 | ✅ 是 |
| MythicMobs | Boss 怪物生成 | ❌ 否 |

### 兼容性

- **服务端**: Paper 1.21+
- **Java**: Java 21+

---

## 二、命令权限

### 2.1 玩家命令

| 命令 | 权限 | 说明 |
|------|------|------|
| `/dungeon` | 无 | 打开副本菜单 |
| `/dungeon join <副本ID>` | 无 | 加入副本 |
| `/dungeon leave` | 无 | 离开当前副本 |
| `/dungeon list` | 无 | 查看可用副本 |
| `/dungeon help` | 无 | 帮助信息 |

### 2.2 管理员命令

| 命令 | 权限 | 说明 |
|------|------|------|
| `/dungeonadmin reload` | `guangdian.dungeon.admin` | 重载配置 |
| `/dungeonadmin start <副本ID>` | `guangdian.dungeon.admin` | 强制启动副本 |
| `/dungeonadmin stop <副本ID>` | `guangdian.dungeon.admin` | 强制停止副本 |

---

## 三、配置文件

```yaml
# dungeons.yml
dungeons:
  beginner_dungeon:
    name: "新手试炼"
    min-players: 1
    max-players: 4
    entry-location: world,0,64,0        # 入场坐标
    boss-room: world_dungeon,0,64,0     # Boss战坐标
    exit-location: world,0,64,0         # 退出坐标
    phases:
      - boss: "DungeonBoss_Phase1"       # MythicMob ID
        health-threshold: 1.0            # 从满血开始
        duration: 600                    # 阶段时长(秒)
      - boss: "DungeonBoss_Phase2"
        health-threshold: 0.5            # 50%血切换
        duration: 600
    rewards:
      experience: 500
      points: 1000
      items:
        "rpg:新手奖励": 1
    # 复活设置
    revive:
      enabled: true
      max-revives: 3
      revive-cost: 100    # 点券消耗
```

---

## 四、更新日志

### v1.0.0
- 初始版本

---

*最后更新: 2026-06-13*
