# GuangDianMenu

> 菜单系统 - 自定义GUI菜单，支持条件、动作、分页

---

## 一、简介

强大的GUI菜单系统，支持自定义菜单布局、条件判断、多动作触发和分页功能。

### 功能特性
- 自定义菜单布局（1-54格）
- MiniMessage 格式文本
- 条件系统（金币、权限、点券）
- 动作系统（命令、消息、音效、菜单跳转）
- 分页支持
- PlaceholderAPI 变量支持

---

## 二、菜单配置示例

### 主菜单 (`menus/主菜单.yml`)
```yaml
title: '<dark_gray><bold>【主菜单】</bold></dark_gray>'
size: 27
items:
  锻造:
    material: ANVIL
    slot: 11
    name: '<gold><bold>⚒ 锻造'
    lore:
      - '<gray>点击打开锻造界面'
    action: 'console:forge open %player%'
```

### 条件菜单
```yaml
  传送:
    material: ENDER_PEARL
    slot: 13
    name: '<green><bold>世界传送'
    action: 'open:世界传送.yml'
    conditions:
      money: 100.0
      fail-message: '<red>金币不足！需要100金币'
```

### 经济条件菜单
```yaml
  购买:
    material: DIAMOND
    slot: 15
    name: '<gold><bold>购买物品'
    action: 'console:rpgitem give %player% 物品ID 1'
    conditions:
      money: 5000.0
      fail-message: '<red>金币不足！'
```

---

## 三、动作类型

| 动作 | 格式 | 说明 |
|:----|:-----|------|
| 命令 | `console:<命令>` | 控制台执行 |
| 玩家命令 | `player:<命令>` | 玩家执行 |
| 打开菜单 | `open:<文件名>` | 打开另一个菜单 |
| 消息 | `message:<文本>` | 发送消息 |
| 音效 | `sound:<音效>` | 播放音效 |

---

*最后更新: 2026-06-13*
