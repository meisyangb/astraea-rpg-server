# GuangDianCleaner

> 光点清理系统 — 定时清理掉落物/实体/区块/自动公告

---

## 一、简介

GuangDianCleaner 定时清理服务器中的地面掉落物、生物实体等，减少延迟。

---

## 二、命令

| 命令 | 权限 | 说明 |
|------|------|------|
| `/cleaner now` | `guangdian.cleaner.admin` | 立即清理 |
| `/cleaner reload` | `guangdian.cleaner.admin` | 重载 |

---

## 三、配置

```yaml
# 清理间隔(分钟)
interval: 15
# 清理前广播(秒)
warning: 30
# 清理类型
clear:
  items: true
  mobs: false
  xp-orbs: true
```

---

*最后更新: 2026-06-13*
