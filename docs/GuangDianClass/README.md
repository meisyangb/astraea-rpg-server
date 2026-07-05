# GuangDianClass

> 光点职业系统 — 9阶位成长、多阶段转职、五种属性点、技能空间、被动加成

---

## 一、简介

GuangDianClass 是 Astraea RPG 核心职业系统，支持从基础职业到神阶(9阶)的完整成长路径。

### 功能特性

- **9阶位系统** — 经验累积提升阶位
- **转职系统** — 3阶一转/6阶二转/8阶三转/9阶神级
- **五种属性** — 力量/体质/敏捷/智力/幸运
- **技能空间** — 技能球/冷却管理/魔力值
- **被动效果** — 属性点自动加成

### 前置要求

| 插件 | 说明 | 必装 |
|------|------|:----:|
| RPGCore | 核心框架 | ✅ 是 |

---

## 二、命令权限

### 2.1 玩家命令

| 命令 | 权限 | 说明 |
|------|------|------|
| `/class` | 无 | 打开职业主界面 |
| `/class info [玩家]` | 无 | 查看职业信息 |
| `/class choose <职业ID>` | 无 | 选择基础职业 |
| `/class advance [职业ID]` | 无 | 转职 |
| `/class attr` | 无 | 属性加点GUI |
| `/class attr add <属性> <点数>` | 无 | 分配属性点 |
| `/class skills` | 无 | 技能空间GUI |
| `/class help` | 无 | 帮助 |

### 2.2 管理员命令

| 命令 | 权限 | 说明 |
|------|------|------|
| `/classadmin set <玩家> <职业ID>` | `guangdian.class.admin` | 设置职业 |
| `/classadmin addexp <玩家> <数量>` | `guangdian.class.admin` | 增加经验 |
| `/classadmin addattr <玩家> <数量>` | `guangdian.class.admin` | 给予属性点 |
| `/classadmin reload` | `guangdian.class.admin` | 重载配置 |

---

## 三、配置 classes.yml

```yaml
classes:
  novice:
    name: "初心者"
    base-class: true
    stats:
      health: 20
      attack: 1
      mana: 10
    advancements:
      - warrior   # 3阶可转
      - mage
  warrior:
    name: "战士"
    tier-requirement: 3
    stats:
      health: 50
      attack: 5
```

---

## 四、属性表

| 属性 | ID | 效果 |
|------|-----|------|
| 力量 | strength | 攻击力 |
| 体质 | vitality | 生命值 |
| 敏捷 | agility | 暴击率/暴击伤害 |
| 智力 | intelligence | 魔力值 |
| 幸运 | luck | 综合加成 |

---

## 五、阶位经验表

| 阶位 | 经验需求 |
|:----:|--------:|
| 1 | 0 |
| 2 | 500 |
| 3 | 2,000 |
| 4 | 5,000 |
| 5 | 10,000 |
| 6 | 50,000 |
| 7 | 200,000 |
| 8 | 500,000 |
| 9 | 1,500,000 |

---

*最后更新: 2026-06-13*
