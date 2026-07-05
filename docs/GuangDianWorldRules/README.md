# GuangDianWorldRules

> 光点世界规则 — 按世界禁用命令/PvP/破坏/天气/时间锁定

---

## 一、简介

GuangDianWorldRules 针对不同世界设置独立规则，如禁用命令、锁定时间天气、控制 PvP 和破坏。

---

## 二、命令

| 命令 | 权限 | 说明 |
|------|------|------|
| `/worldrules reload` | `guangdian.worldrules.admin` | 重载 |

---

## 三、配置

```yaml
worlds:
  world_spawn:
    pvp: false
    block-break: false
    weather: CLEAR
    time: 6000
    blocked-commands:
      - /tp
      - /home
```

---

*最后更新: 2026-06-13*
