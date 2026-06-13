# GuangDianForge

> 锻造系统 - 装备合成、锻造等级、图纸学习

---

## 一、简介

装备锻造系统，通过收集材料和图纸来锻造装备，支持锻造等级和经验系统。

### 功能特性
- **图纸系统** - 学习图纸后即可锻造
- **锻造等级** - 通过锻造提升等级，增加成功率
- **材料消耗** - 支持 RPGItems 物品作为材料
- **成功率计算** - 基础成功率 + 等级加成
- **经验奖励** - 锻造成功/失败均获得经验

---

## 二、配置文件 (`config.yml`)

```yaml
# 每次锻造消耗的经验等级
forge-exp-cost: 3

# 锻造等级每级提供的成功率加成
success-rate-per-forge-level: 0.02

# 锻造成功获得的经验（当图纸未配置 exp-reward 时使用）
exp-on-success: 50

# 锻造失败获得的经验
exp-on-failure: 10

# 等级经验需求
level-thresholds:
  1: 0
  2: 200
  3: 500
  4: 1000
  5: 2000
  6: 3500
  7: 5500
  8: 8000
  9: 11000
  10: 15000
```

### 图纸配置 (`recipes.yml`)
```yaml
recipes:
  dawn_sword:
    name: "<green>黎明之刃"
    required-forge-level: 0
    exp-reward: 50           # 锻造成功经验
    ingredients:
      rpg:原初之石1星: 1
      rpg:一阶锻造精华: 5
      rpg:下级装备核心: 5
    result:
      rpg-item: "黎明之刃"
    base-success-rate: 0.8
    blueprint:
      display: "<green><bold>【图纸】黎明之刃"
      is-book: true
```

### 锻造系列一览
| 阶位 | 系列 | 需求等级 | 经验 |
|:----:|:----:|:--------:|:----:|
| 一阶 | 黎明 | 0 | 30-50 |
| 二阶 | 精灵 | 1 | 70-100 |
| 三阶 | 暗影 | 2 | 150-200 |
| 四阶 | 星辰 | 3 | 150-200 |
| 五阶 | 圣殿 | 4 | 220-300 |

---

## 三、命令权限

| 命令 | 权限 | 说明 |
|------|------|------|
| `/forge` | 无 | 打开锻造界面 |
| `/forge info` | 无 | 查看锻造信息 |
| `/forgeadmin give <玩家> <图纸ID>` | `forge.admin` | 给予图纸 |
| `/forgeadmin setlevel <玩家> <等级>` | `forge.admin` | 设置锻造等级 |
| `/forgeadmin reload` | `forge.admin` | 重载配置 |

---

*最后更新: 2026-06-13*
