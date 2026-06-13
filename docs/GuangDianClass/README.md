# GuangDianClass

> 职业系统 - 支持阶位、转职、职业技能和属性成长

---

## 一、简介

完整的职业系统，支持多阶位转职、属性成长和职业技能。

### 功能特性
- **多阶位** - 最多9阶转职
- **属性系统** - 力量、活力、敏捷、智力、幸运
- **经验系统** - 击杀、任务、AFK 获得经验
- **进阶阶段** - first(3阶)、second(6阶)、third(8阶)、divine(9阶)

---

## 二、配置文件 (`config.yml`)

```yaml
default-class: "novice"
max-tier: 9
exp-sources:
  mob-kill:
    base: 10
  quest-completion:
    base: 100
  afk:
    amount: 1
    interval: 60

tier-exp-requirements:
  1: 0      2: 500     3: 2000
  4: 5000   5: 10000   6: 50000
  7: 200000 8: 500000  9: 1500000

attribute-types:
  - "力量"
  - "活力"
  - "敏捷"
  - "智力"
  - "幸运"

advancement-stages:
  first:
    tier: 3
    name: "一转入职"
  second:
    tier: 6
    name: "二转转职"
  third:
    tier: 8
    name: "三转觉醒"
  divine:
    tier: 9
    name: "神阶"
```

---

## 三、命令权限

| 命令 | 权限 | 说明 |
|------|------|------|
| `/class info` | 无 | 查看职业信息 |
| `/class skills` | 无 | 查看职业技能 |
| `/class attributes` | 无 | 查看属性 |
| `/classadmin set <玩家> <阶位>` | `class.admin` | 管理员设置阶位 |

---

*最后更新: 2026-06-13*
