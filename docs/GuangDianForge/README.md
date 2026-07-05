# GuangDianForge

> 光点锻造系统 — 图纸学习、装备锻造、锻造等级成长、成功率计算、RPGItems 深度集成

---

## 一、简介

GuangDianForge 是装备锻造系统，玩家收集材料和学习图纸来锻造 RPGItems 自定义装备，支持锻造等级成长和成功率动态计算。

### 功能特性

- **图纸系统** — 右键图纸物品学习配方
- **锻造等级** — 锻造积累经验，每级+2%成功率
- **材料消耗** — 支持 RPGItems 物品作为锻造材料
- **GUI 界面** — 27格锻造界面，图纸选择界面
- **成功率** — 基础率+等级加成，最高95%

### 前置要求

| 插件 | 说明 | 必装 |
|------|------|:----:|
| RPGCore | 核心框架 | ✅ 是 |
| RPGItems | 物品系统 | ✅ 是 |

---

## 二、命令权限

### 2.1 玩家命令

| 命令 | 权限 | 说明 |
|------|------|------|
| `/forge` | `guangdian.forge.use` | 打开锻造界面 |
| `/forge open [图纸ID]` | `guangdian.forge.use` | 打开指定图纸 |
| `/forge info [玩家]` | `guangdian.forge.use` | 查看锻造信息 |
| `/forge list [all/learned/unlearned]` | `guangdian.forge.use` | 查看图纸列表 |
| `/forge help` | 无 | 帮助信息 |

### 2.2 管理员命令

| 命令 | 权限 | 说明 |
|------|------|------|
| `/forgeadmin give <玩家> <图纸ID>` | `guangdian.forge.admin` | 给予图纸 |
| `/forgeadmin setlevel <玩家> <等级>` | `guangdian.forge.admin` | 设置锻造等级 |
| `/forgeadmin addexp <玩家> <经验>` | `guangdian.forge.admin` | 添加经验 |
| `/forgeadmin reset <玩家>` | `guangdian.forge.admin` | 重置玩家数据 |
| `/forgeadmin reload` | `guangdian.forge.admin` | 重载配置 |
| `/forgeadmin stats` | `guangdian.forge.admin` | 查看统计 |

---

## 三、配置 recipes.yml

```yaml
recipes:
  dawn_sword:
    name: "<green>黎明之刃"
    required-forge-level: 0
    ingredients:
      "rpg:原初之石1星": 1
      "rpg:一阶锻造精华": 5
    result:
      rpg-item: "黎明之刃"
    base-success-rate: 0.8
    exp-reward: 50
    blueprint:
      display: "<green><bold>【图纸】黎明之刃"
      is-book: true
```

### 成功率公式

```
最终成功率 = min(基础成功率 + (玩家等级-图纸需求等级) × 0.02, 0.95)
```

### PlaceholderAPI

| 占位符 | 说明 |
|--------|------|
| `%forge_level%` | 锻造等级 |
| `%forge_exp%` | 当前经验 |
| `%forge_recipes%` | 已学图纸数 |
| `%forge_rate%` | 成功率 |

---

*最后更新: 2026-06-13*
