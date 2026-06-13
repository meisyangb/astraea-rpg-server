# GuangDianArmorStats

> RPG装备属性系统 - 为物品添加自定义属性，支持伤害计算、Boss属性等功能

---

## 一、简介

GuangDianArmorStats 是 RPG 装备属性系统，为 Minecraft 物品添加自定义属性，实现复杂的 RPG 装备系统。

### 功能特性
- **自定义属性** - 攻击力、防御力、暴击、吸血、闪避等
- **伤害计算** - 自定义伤害公式，支持 PVP/PVE 分别计算
- **Boss属性** - 支持 Boss 属性配置和阶段变化
- **宝石系统** - 支持装备镶嵌宝石
- **技能触发** - 支持主动/被动技能触发
- **装备识别** - 通过 Lore 关键词自动识别武器/防具

### 前置要求
- **必需**: RPGCore
- **可选**: PlaceholderAPI, Vault

---

## 二、配置文件 (`config.yml`)

```yaml
# 调试模式
debug: false

# 技能触发器
skill_trigger:
  enabled: true
  override_itemtrigger: true  # 优先使用本插件触发器

# Boss公告
boss_announce:
  enabled: false

# 血量显示
health_display:
  enable_scale: true
  max_rows: 2

# 闪避配置
dodge:
  enabled: true

# 暴击抵抗
crit_resist:
  enabled: true
  damage_reduction: 0.01
```

### 装备识别配置
```yaml
equipment_identification:
  weapon:
    lore_keywords:
      - "攻击力"
      - "暴击几率"
    lore_first_line_keywords:
      - "近战武器"
      - "远程武器"
      - "武器"
  armor:
    lore_keywords:
      - "防御力"
      - "生命上限"
    lore_first_line_keywords:
      - "防具"
```

### 物品 Lore 格式要求
武器：
```
武器                    限定副手
攻击力              800-6700
暴击几率            18%
暴击伤害            100%
```

防具：
```
防具                    限定头部
生命上限              +8000
护甲强度              25%
闪避几率              8%
```

---

## 三、命令权限

| 命令 | 权限 | 说明 |
|------|------|------|
| `/astats reload` | `guangdianarmorstats.admin` | 重载配置 |
| `/astats info` | 无 | 查看装备信息 |
| `/astats debug` | `guangdianarmorstats.admin` | 切换调试模式 |

---

## 四、属性列表

### 基础属性
| 属性名 | 格式 | 说明 |
|--------|:----:|------|
| 攻击力 | min-max | 攻击力范围 |
| 防御力 | min-max | 防御力范围 |
| 生命上限 | +数值 | 最大生命值加成 |
| 每秒回血 | +数值 | 每秒回复生命值 |
| 护甲强度 | 数值% | 护甲减伤百分比 |

### 稀有属性
| 属性名 | 格式 | 说明 |
|--------|:----:|------|
| 暴击几率 | 数值% | 暴击触发概率 |
| 暴击伤害 | 数值% | 暴击伤害倍率 |
| 吸血几率 | 数值% | 吸血触发概率 |
| 吸血倍率 | 数值% | 吸血回复比例 |
| 闪避几率 | 数值% | 闪避攻击概率 |
| 招架几率 | 数值% | 招架攻击概率 |
| 暴击抵抗 | 数值% | 减少被暴击概率 |
| 吸血抵抗 | 数值% | 减少被吸血比例 |
| 移动速度 | 数值% | 移动速度加成 |
| 经验加成 | 数值% | 经验获取加成 |

### 状态效果
| 属性名 | 格式 | 说明 |
|--------|:----:|------|
| 中毒 | 数值% | 攻击附加中毒 |
| 冰冻 | 数值% | 攻击附加冰冻 |
| 致盲 | 数值% | 攻击附加致盲 |
| 燃烧 | 数值% | 攻击附加燃烧 |
| 灼烧 | 数值% | 攻击附加灼烧 |

---

## 五、技能配置 (`skills.yml`)

### 技能类型
- `active`: 主动技能（右键触发）
- `damage_trigger`: 被动技能（攻击时有几率触发）
- `on_hit`: 被击中时触发
- `passive`: 被动技能（持续生效）

### 技能配置示例
```yaml
skills:
  烈焰打击:
    type: damage_trigger
    trigger_chance: 15
    range: 3.0
    damage_mult: 0.8
    effect: fire
    status_effects:
      - "wither"
    duration: 3

  狂暴闪电:
    type: active
    range: 8.0
    damage_mult: 1.5
    cooldown: 15
    effect: lightning
    status_effects:
      - "slowness"
    duration: 2
```

### 在物品 Lore 中绑定技能
```
【附带技能】
  主动技能: 狂暴闪电
```

---

## 六、API 接口

```java
// 获取玩家属性
PlayerStats stats = StatsManager.getPlayerStats(player);
double attack = stats.getAttackAverage();
double critChance = stats.getCritChance();

// 监听伤害事件
@EventHandler
public void onDamage(RpgPostDamageEvent event) {
    Player attacker = event.getAttacker();
    LivingEntity target = event.getTarget();
    double damage = event.getDamage();
}
```

---

## 七、常见问题

### Q: 装备不生效？
**A:** 检查 Lore 格式是否包含正确的关键词（攻击力/暴击几率）

### Q: 属性不显示？
**A:** 确保物品有 `HideAttributes: true` 和正确的 Lore 格式

---

*最后更新: 2026-06-13*
