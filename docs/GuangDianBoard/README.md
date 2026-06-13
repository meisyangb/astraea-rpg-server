# GuangDianBoard

> 计分板系统 - 自定义侧边栏显示

---

## 一、简介

自定义侧边栏计分板系统，支持变量显示、世界别名、分组格式。

### 功能特性
- 自定义侧边栏内容
- PlaceholderAPI 变量
- 世界别名
- 分组显示格式
- 刷新间隔可配置

---

## 二、配置文件 (`config.yml`)

```yaml
# 刷新间隔（毫秒）
refresh-interval: 5000

# 标题
title: '<white>Lv.<yellow>%player_level% <green>%player_name%'

# 侧边栏内容
lines:
  - ""
  - "&f☄ 金币: &e%vault_eco_balance_formatted%"
  - "&f✪ 点券: &b%guangdianpoints_points%"
  - "&f❤ 生命: &c%player_health%&f/&c%player_max_health%"
  - "&f⚔ 攻击: &c%guangdian_stats_attack%"
  - ""
  - "&f⛏ 公会: &d%guangdianguild_name%"
  - "&f❤ 伴侣: &d%guangdianmarriage_partner%"
  - "&f⚒ 锻造: &6Lv.%guangdianforge_level%"
  - ""
  - "&7官方群: 1104425711"
```

---

## 三、命令权限

| 命令 | 权限 | 说明 |
|------|------|------|
| `/board reload` | `board.admin` | 重载配置 |

---

*最后更新: 2026-06-13*
