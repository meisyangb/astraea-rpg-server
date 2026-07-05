# GuangDianDropControl

> 光点掉落控制 — 按世界/按生物/按物品精确控制掉落率

---

## 一、简介

GuangDianDropControl 提供精细的掉落率控制，可针对不同世界、生物类型、物品类型设置独立掉落倍率。

---

## 二、命令

| 命令 | 权限 | 说明 |
|------|------|------|
| `/dropcontrol reload` | `guangdian.dropcontrol.admin` | 重载配置 |

---

## 三、配置

```yaml
multipliers:
  global: 1.0
  worlds:
    world_dungeon: 2.0
  entities:
    ZOMBIE: 1.5
```

---

*最后更新: 2026-06-13*
