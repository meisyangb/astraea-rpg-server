# GuangDianClass

> 职业系统 - 为玩家提供职业、阶位、转职和职业技能功能

---

## 一、简介

GuangDianClass 是一个功能完整的职业系统插件，支持职业阶位、转职、职业技能等功能，为 RPG 服务器提供丰富的职业体验。

### 功能特性

- **职业系统** - 支持多个职业，每个职业有独特的属性和技能
- **阶位系统** - 职业可以设置多个阶位，玩家可以升级
- **转职系统** - 支持职业转职，可以设置转职条件
- **职业技能** - 每个职业可以配置独特的技能
- **属性加成** - 不同职业提供不同的属性加成
- **职业限制** - 可以限制某些功能只能特定职业使用

### 前置要求

- **必需**: [RPGCore](/RPGCore/README), [GuangDianArmorStats](/GuangDianArmorStats/README)
- **可选**: PlaceholderAPI

### 兼容性

- **服务端**: Paper 1.21+
- **Java**: Java 21+
- **依赖插件**: RPGCore, GuangDianArmorStats

---

## 二、安装

### 2.1 安装步骤

1. 确保已安装 RPGCore 和 GuangDianArmorStats
2. 下载 `GuangDianClass.jar`
3. 将 jar 文件放入 `plugins` 文件夹
4. 重启服务器
5. 编辑 `plugins/GuangDianClass/classes.yml` 配置职业

### 2.2 验证安装

```bash
/gdc info          # 查看插件信息
/gdc list         # 列出所有职业
/gdc reload       # 重载配置
```

---

## 三、快速开始

### 3.1 创建职业

编辑 `plugins/GuangDianClass/classes.yml`:

```yaml
# 职业配置文件
# 最后更新: 2026-06-11

classes:
  warrior:
    name: "§c战士"
    description: "近战物理输出职业"
    icon: "IRON_SWORD"
    
    # 基础属性
    base_health: 100
    base_mana: 50
    base_attack: 10
    base_defense: 5
    
    # 成长属性（每级增加）
    growth_health: 10
    growth_mana: 5
    growth_attack: 2
    growth_defense: 1
    
    # 技能列表
    skills:
      - "slash"
      - "shield_bash"
      - "battle_cry"
    
    # 转职要求
    requirements:
      level: 0
      prev-class: ""
    
    # 阶位系统
    ranks:
      - name: "初级战士"
        level-required: 1
        bonus:
          attack: 5
          defense: 3
      
      - name: "中级战士"
        level-required: 10
        bonus:
          attack: 10
          defense: 6
      
      - name: "高级战士"
        level-required: 20
        bonus:
          attack: 20
          defense: 12
  
  mage:
    name: "§b法师"
    description: "远程魔法输出职业"
    icon: "BLAZE_ROD"
    
    base_health: 60
    base_mana: 100
    base_attack: 15
    base_defense: 3
    
    growth_health: 5
    growth_mana: 10
    growth_attack: 3
    growth_defense: 0.5
    
    skills:
      - "fireball"
      - "ice_spike"
      - "teleport"
```

### 3.2 选择职业

玩家使用以下命令选择职业：

```bash
/class list              # 查看所有可用职业
/class select <职业>    # 选择职业
/class info              # 查看当前职业信息
/class skills            # 查看职业技能
```

### 3.3 职业升级

```bash
/class levelup          # 升级职业（需要经验）
/class rankup           # 提升阶位（需要等级）
```

---

## 四、命令权限

### 4.1 玩家命令

| 命令 | 权限 | 说明 |
|------|------|------|
| `/class list` | 无 | 查看所有可用职业 |
| `/class select <职业>` | 无 | 选择职业 |
| `/class info` | 无 | 查看当前职业信息 |
| `/class skills` | 无 | 查看职业技能 |
| `/class levelup` | 无 | 升级职业 |
| `/class rankup` | 无 | 提升阶位 |

### 4.2 管理员命令

| 命令 | 权限 | 说明 |
|------|------|------|
| `/gdc reload` | `guangdianclass.admin` | 重载配置文件 |
| `/gdc info` | `guangdianclass.admin` | 查看插件信息 |
| `/gdc setlevel <玩家> <等级>` | `guangdianclass.admin` | 设置玩家职业等级 |
| `/gdc setclass <玩家> <职业>` | `guangdianclass.admin` | 设置玩家职业 |

---

## 五、配置文件

### 5.1 classes.yml

职业配置文件，定义所有职业和阶位。

**配置结构：**

```yaml
classes:
  <职业ID>:
    name: "显示名称"
    description: "职业描述"
    icon: "物品ID"
    
    base_health: 基础生命值
    base_mana: 基础魔法值
    base_attack: 基础攻击力
    base_defense: 基础防御力
    
    growth_health: 生命成长
    growth_mana: 魔法成长
    growth_attack: 攻击成长
    growth_defense: 防御成长
    
    skills:
      - "技能1"
      - "技能2"
    
    requirements:
      level: 要求等级
      prev-class: "前置职业"
    
    ranks:
      - name: "阶位名称"
        level-required: 要求等级
        bonus:
          attack: 攻击加成
          defense: 防御加成
```

### 5.2 config.yml

主配置文件。

```yaml
# 主配置
debug: false

# 职业设置
settings:
  # 是否允许转职
  allow-class-change: true
  
  # 转职冷却时间（秒）
  class-change-cooldown: 300
  
  # 最大等级
  max-level: 100
  
  # 每级所需经验
  exp-per-level: 100
  
  # 是否启用阶位系统
  enable-ranks: true
```

---

## 六、技能配置

### 6.1 技能文件结构

技能配置文件位于 `plugins/GuangDianClass/skills/` 目录下。

```yaml
# skills/slash.yml - 猛击技能

id: "slash"
name: "§c猛击"
description: "对目标造成150%伤害"
icon: "IRON_SWORD"
cooldown: 5  # 冷却时间（秒）

# 技能效果
effects:
  - type: "damage"
    value: "1.5 * attack"  # 伤害公式
  
  - type: "potion"
    effect: "SLOW"
    duration: 3
    level: 1

# 技能要求
requirements:
  level: 5  # 需要职业等级 5
  mana: 10  # 需要消耗 10 魔法值
```

### 6.2 使用技能

```bash
/skill <技能名>        # 使用技能（需要目标）
/skill list            # 查看可用技能
/skill info <技能名>   # 查看技能详情
```

---

## 七、API 接口

### 7.1 获取职业管理器

```java
// 获取职业管理器
ClassManager manager = RPGCore.getServiceRegistry().get(ClassManager.class);

// 获取玩家职业
PlayerClass playerClass = manager.getPlayerClass(player);

// 获取职业信息
String className = playerClass.getClassName();
int level = playerClass.getLevel();
int rank = playerClass.getRank();

// 设置玩家职业
manager.setPlayerClass(player, "warrior");

// 添加经验
manager.addExp(player, 100);
```

### 7.2 监听职业事件

```java
// 监听职业选择事件
@EventHandler
public void onClassSelect(ClassSelectEvent event) {
    Player player = event.getPlayer();
    String className = event.getClassName();
    
    // 处理职业选择逻辑
}

// 监听职业升级事件
@EventHandler
public void onClassLevelUp(ClassLevelUpEvent event) {
    Player player = event.getPlayer();
    int newLevel = event.getNewLevel();
    
    // 处理升级逻辑
    player.sendMessage("§a恭喜！你的职业等级提升到 " + newLevel + " 级！");
}
```

---

## 八、常见问题

### Q: 玩家无法选择职业？

**A:** 检查以下事项：
1. 确保 `classes.yml` 中正确配置了职业
2. 确保玩家满足职业选择条件
3. 使用 `/class list` 查看可用职业

### Q: 技能无法使用？

**A:** 检查以下事项：
1. 确保玩家职业包含该技能
2. 确保玩家满足技能等级要求
3. 确保玩家有足够的魔法值

### Q: 如何创建自定义技能？

**A:** 在 `plugins/GuangDianClass/skills/` 目录下创建新的 YAML 文件，参考现有技能配置。

---

## 九、更新日志

### v1.0.0 (2026-06-11)
- 🎉 初始版本发布
- ✨ 支持职业系统
- ✨ 支持阶位系统
- ✨ 支持转职系统
- ✨ 支持职业技能

---

## 十、下一步

- 📖 查看 [命令权限](/GuangDianClass/command) 了解所有命令
- ⚙️ 查看 [配置文件](/GuangDianClass/config) 了解详细配置
- 🔌 查看 [API接口](/GuangDianClass/api) 了解开发者接口

---

*最后更新: 2026-06-11*
