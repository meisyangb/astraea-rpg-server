# GuangDianDungeon

> 副本系统 - 支持多楼层副本、Boss战、队伍匹配、进度保存

---

## 一、简介

多楼层战斗副本系统，支持队伍匹配、Boss战、进度保存和奖励分配。

### 功能特性
- 多楼层副本（F1-F8）
- Boss战斗与阶段切换
- 队伍匹配系统
- 副本进度保存
- 掉落奖励分配系统
- 副本冷却管理
- 与 MythicMobs 深度集成

---

## 二、配置文件 (`config.yml`)

```yaml
# 退出副本位置
exit-world: world, 0,64,0

# 全局设置
global:
  max-instances: 50
  instance-timeout: 3600

# 队伍设置
party:
  default-max-players: 5

# 数据库
database:
  type: sqlite

# 性能
performance:
  async-world-load: true
```

---

## 三、命令权限

| 命令 | 权限 | 说明 |
|------|------|------|
| `/dungeon join <楼层>` | 无 | 加入副本队列 |
| `/dungeon leave` | 无 | 离开副本 |
| `/dungeon party create` | 无 | 创建队伍 |
| `/dungeon party invite <玩家>` | 无 | 邀请玩家 |
| `/dungeon party kick <玩家>` | `dungeon.leader` | 踢出玩家 |
| `/dungeon start` | `dungeon.leader` | 开始副本 |
| `/dungeon admin reset` | `dungeon.admin` | 重置副本 |

---

## 四、楼层配置

### MobBridge 配置（在 MobBridge.yml 中）
```yaml
floors:
  F1:
    display: "第一层 - 寄生森林"
    mobs:
      - "寄生幼体"
      - "寄生战士"
      - "寄生母体"
    boss: "寄生统领"
  F2:
    display: "第二层 - 骷髅墓地"
```

### 副本怪物掉落
```
一层: 奥莱斯碎片/精华（用于锻造奥莱斯系列装备）
二层: 卡尔萨斯碎片/精华（用于锻造卡尔萨斯系列装备）
```

---

*最后更新: 2026-06-13*
