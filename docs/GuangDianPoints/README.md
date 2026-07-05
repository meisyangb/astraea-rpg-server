# GuangDianPoints

> 光点点券系统 — SQLite 持久化、转账/查询/排行榜、RPGCore PointsService 接口、性能监控

---

## 一、简介

GuangDianPoints 是 Astraea RPG 的统一经济代币系统，基于 SQLite 数据库持久化存储，支持点券转账、排行榜、交易记录、性能监控等完整功能，同时作为 RPGCore PointsService 接口的实现供其他插件调用。

### 功能特性

- **SQLite 持久化** — 点券余额和交易记录完整保存
- **转账系统** — 玩家间点券转账
- **排行榜** — 按点券余额排名
- **性能监控** — `/points perfmon` 查看数据库性能
- **交易记录** — 所有操作自动记录

### 前置要求

| 插件 | 说明 | 必装 |
|------|------|:----:|
| RPGCore | 核心框架 | ✅ 是 |
| Vault | 经济API(可选) | ❌ 否 |

---

## 二、命令权限

### 2.1 玩家命令

| 命令 | 权限 | 说明 |
|------|------|------|
| `/points` | 无 | 查看自己的点券余额 |
| `/points balance [玩家]` | 无 | 查看余额(管理员查别人) |
| `/points pay <玩家> <数量>` | 无 | 转账给其他玩家 |
| `/points top` | 无 | 查看点券排行榜 |
| `/points help` | 无 | 帮助信息 |

### 2.2 管理员命令

| 命令 | 权限 | 说明 |
|------|------|------|
| `/points give <玩家> <数量>` | `guangdian.points.admin` | 给予点券 |
| `/points take <玩家> <数量>` | `guangdian.points.admin` | 扣除点券 |
| `/points set <玩家> <数量>` | `guangdian.points.admin` | 设置点券 |
| `/points reload` | `guangdian.points.admin` | 重载配置 |
| `/points perfmon` | `guangdian.points.admin` | 性能监控 |

---

## 三、PlaceholderAPI

| 占位符 | 说明 |
|--------|------|
| `%gdpoints_balance%` | 点券余额 |
| `%gdpoints_rank%` | 排行榜排名 |

---

## 四、数据库

```
plugins/GuangDianPoints/points.db
├── player_points (uuid, balance, updated_at)
├── transactions (id, sender, receiver, amount, type, timestamp)
```

---

*最后更新: 2026-06-13*
