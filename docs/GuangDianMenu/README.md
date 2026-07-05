# GuangDianMenu

> 光点菜单系统 — YAML 驱动 GUI 菜单，支持按钮/条件/命令/打开子菜单/PlaceholderAPI

---

## 一、简介

GuangDianMenu 基于 YAML 配置文件驱动 Inventory GUI 菜单，完全无需 Java 编码即可创建交互式菜单。

### 功能特性

- **YAML 驱动** — 纯配置文件创建菜单，无需编码
- **按钮系统** — 自定义物品材质、名称、Lore
- **条件判断** — 权限/点券/等级多条件组合
- **命令执行** — 按钮支持玩家/控制台命令
- **子菜单** — 支持菜单间跳转
- **PlaceholderAPI** — 按钮文本支持变量

### 前置要求

| 插件 | 说明 | 必装 |
|------|------|:----:|
| RPGCore | 核心框架 | ✅ 是 |
| PlaceholderAPI | 变量支持 | ❌ 否 |

---

## 二、命令权限

| 命令 | 权限 | 说明 |
|------|------|------|
| `/menu open <菜单名>` | 无 | 打开指定菜单 |
| `/menu list` | 无 | 列出可用菜单 |
| `/menu reload` | `guangdian.menu.admin` | 重载所有菜单 |

---

## 三、菜单配置示例

```yaml
# menus/main_menu.yml
main_menu:
  title: "主菜单"
  rows: 3
  items:
    button1:
      slot: 11
      material: "DIAMOND_SWORD"
      name: "&a副本传送"
      lore:
        - "&7点击传送到副本区域"
      commands:
        - "console:warp dungeons {player}"
    button2:
      slot: 15
      material: "CHEST"
      name: "&e锻造工坊"
      open-menu: "forge_menu"
```

---

*最后更新: 2026-06-13*
