# GuangDianMobs

> 自定义怪物系统 - 支持自定义怪物配置、刷新点、AI 等功能

---

## 一、简介

GuangDianMobs 是一个功能强大的自定义怪物系统，允许你创建自定义怪物、设置刷新点、配置怪物 AI 等行为。

### 功能特性

- **自定义怪物** - 创建具有自定义属性的怪物
- **刷新点系统** - 设置怪物刷新点，支持多种刷新模式
- **怪物 AI** - 配置怪物的 AI 行为
- **掉落系统** - 自定义怪物掉落物
- **Boss 系统** - 支持 Boss 怪物配置
- **怪物技能** - 为怪物配置特殊技能

### 前置要求

- **必需**: [RPGCore](/RPGCore/README), [GuangDianArmorStats](/GuangDianArmorStats/README)
- **可选**: MythicMobs, PlaceholderAPI

### 兼容性

- **服务端**: Paper 1.21+
- **Java**: Java 21+
- **依赖插件**: RPGCore, GuangDianArmorStats

---

## 二、安装

### 2.1 安装步骤

1. 确保已安装 RPGCore 和 GuangDianArmorStats
2. 下载 `GuangDianMobs.jar`
3. 将 jar 文件放入 `plugins` 文件夹
4. 重启服务器
5. 编辑 `plugins/GuangDianMobs/mobs.yml` 配置怪物

### 2.2 验证安装

```bash
/gdm info          # 查看插件信息
/gdm list          # 列出所有自定义怪物
/gdm reload        # 重载配置
```

---

## 三、快速开始

### 3.1 创建自定义怪物

编辑 `plugins/GuangDianMobs/mobs.yml`:

```yaml
# 自定义怪物配置
# 最后更新: 2026-06-11

mobs:
  custom_zombie:
    # 基础设置
    type: ZOMBIE
    name: "§c§l强化僵尸"
    health: 100
    damage: 10
    
    # 外观设置
    equipment:
      mainhand: "DIAMOND_SWORD"
      helmet: "IRON_HELMET"
      chestplate: "IRON_CHESTPLATE"
      leggings: "IRON_LEGGINGS"
      boots: "IRON_BOOTS"
    
    # 属性加成（需要 GuangDianArmorStats）
    attributes:
      strength: 10
      defense: 5
    
    # 掉落物
    drops:
      - item: "DIAMOND"
        amount: "1-3"
        chance: 50  # 50% 概率
      
      - item: "GOLD_INGOT"
        amount: "2-5"
        chance: 80
    
    # 经验掉落
    exp: 50
    
    # AI 设置
    ai:
      follow-range: 20
      attack-knockback: 1.2
      move-speed: 0.3
    
    # 特殊技能
    skills:
      - "rage"      # 狂暴技能
      - "summon"     # 召唤小怪
  
  elite_skeleton:
    type: SKELETON
    name: "§b§l精英骷髅"
    health: 80
    damage: 15
    
    equipment:
      mainhand: "BOW"
      helmet: "LEATHER_HELMET"
    
    attributes:
      strength: 15
      crit_chance: 30
    
    drops:
      - item: "ARROW"
        amount: "5-10"
        chance: 100
      
      - item: "BOW"
        amount: 1
        chance: 10
    
    exp: 40
```

### 3.2 创建刷新点

使用命令创建怪物刷新点：

```bash
# 1. 创建刷新点
/gdmsp create zombie_spawn_1

# 2. 设置刷新点位置（当前位置）
/gdmsp set zombie_spawn_1 location here

# 3. 设置刷新怪物
/gdmsp set zombie_spawn_1 mob custom_zombie

# 4. 设置刷新数量
/gdmsp set zombie_spawn_1 amount 5

# 5. 设置刷新范围
/gdmsp set zombie_spawn_1 radius 10

# 6. 设置刷新间隔（秒）
/gdmsp set zombie_spawn_1 interval 30

# 7. 启用刷新点
/gdmsp enable zombie_spawn_1
```

### 3.3 管理刷新点

```bash
/gdmsp list                   # 列出所有刷新点
/gdmsp info <刷新点ID>        # 查看刷新点详情
/gdmsp remove <刷新点ID>      # 删除刷新点
/gdmsp disable <刷新点ID>     # 禁用刷新点
/gdmsp enable <刷新点ID>      # 启用刷新点
```

---

## 四、命令权限

### 4.1 玩家命令

| 命令 | 权限 | 说明 |
|------|------|------|
| `/gdm list` | 无 | 列出所有自定义怪物 |
| `/gdm info <怪物ID>` | 无 | 查看怪物详情 |

### 4.2 管理员命令

| 命令 | 权限 | 说明 |
|------|------|------|
| `/gdm reload` | `guangdianmobs.admin` | 重载配置文件 |
| `/gdm info` | `guangdianmobs.admin` | 查看插件信息 |
| `/gdm create <怪物ID>` | `guangdianmobs.admin` | 创建新怪物 |
| `/gdm delete <怪物ID>` | `guangdianmobs.admin` | 删除怪物 |
| `/gdmsp <子命令>` | `guangdianmobs.admin.spawnpoint` | 管理刷新点 |

---

## 五、配置文件

### 5.1 mobs.yml

怪物配置文件，定义所有自定义怪物。

**配置结构：**

```yaml
mobs:
  <怪物ID>:
    type: "实体类型"
    name: "显示名称"
    health: 生命值
    damage: 伤害
    
    equipment:
      mainhand: "主手物品"
      offhand: "副手物品"
      helmet: "头盔"
      chestplate: "胸甲"
      leggings: "护腿"
      boots: "靴子"
    
    attributes:
      <属性名>: 数值
    
    drops:
      - item: "物品ID"
        amount: "数量范围"
        chance: 概率
    
    exp: 经验值
    
    ai:
      follow-range: 追踪范围
      attack-knockback: 击退力度
      move-speed: 移动速度
    
    skills:
      - "技能ID"
```

### 5.2 config.yml

主配置文件。

```yaml
# 主配置
debug: false

# 刷新点设置
spawnpoint:
  # 最大刷新点数量
  max-spawnpoints: 100
  
  # 每个刷新点最大怪物数量
  max-mobs-per-spawnpoint: 10
  
  # 刷新点检查间隔（秒）
  check-interval: 5

# 怪物设置
mob:
  # 怪物是否显示名称
  show-name: true
  
  # 怪物是否显示血量条
  show-health-bar: true
  
  # 怪物攻击玩家距离
  attack-range: 15
```

---

## 六、刷新点系统

### 6.1 刷新点配置

刷新点可以配置多种参数：

```bash
# 设置刷新点位置
/gdmsp set <ID> location here        # 当前位置
/gdmsp set <ID> location <x> <y> <z> <world>  # 指定坐标

# 设置刷新怪物
/gdmsp set <ID> mob <怪物ID>

# 设置刷新数量
/gdmsp set <ID> amount <数量>

# 设置刷新范围
/gdmsp set <ID> radius <范围>

# 设置刷新间隔
/gdmsp set <ID> interval <秒数>

# 设置最大存活数量
/gdmsp set <ID> max-alive <数量>
```

### 6.2 刷新模式

```yaml
# 刷新点可以配置不同的刷新模式
spawnpoint:
  <ID>:
    # 刷新模式：normal（正常）、wave（波次）、boss（Boss）
    mode: "normal"
    
    # 波次设置（仅 wave 模式）
    wave:
      total-waves: 5
      mobs-per-wave: 10
      interval-between-waves: 60
```

---

## 七、API 接口

### 7.1 获取怪物管理器

```java
// 获取怪物管理器
MobManager manager = RPGCore.getServiceRegistry().get(MobManager.class);

// 获取自定义怪物
CustomMob mob = manager.getMob("custom_zombie");

// 生成怪物
manager.spawnMob("custom_zombie", location);

// 获取所有怪物
Map<String, CustomMob> mobs = manager.getAllMobs();
```

### 7.2 监听怪物事件

```java
// 监听怪物生成事件
@EventHandler
public void onMobSpawn(MobSpawnEvent event) {
    CustomMob mob = event.getMob();
    Location location = event.getLocation();
    
    // 处理怪物生成逻辑
}

// 监听怪物死亡事件
@EventHandler
public void onMobDeath(MobDeathEvent event) {
    CustomMob mob = event.getMob();
    Player killer = event.getKiller();
    
    // 处理怪物死亡逻辑
}
```

---

## 八、常见问题

### Q: 怪物不刷新？

**A:** 检查以下事项：
1. 确保刷新点已启用（`/gdmsp enable <ID>`）
2. 确保刷新点配置正确
3. 确保服务器内有玩家在刷新点附近

### Q: 怪物不掉落物品？

**A:** 检查 `mobs.yml` 中的 `drops` 配置，确保 `chance` 设置正确。

### Q: 如何创建 Boss 怪物？

**A:** 在 `mobs.yml` 中配置高生命值、高伤害，并添加特殊技能。

---

## 九、更新日志

### v1.0.0 (2026-06-11)
- 🎉 初始版本发布
- ✨ 支持自定义怪物
- ✨ 支持刷新点系统
- ✨ 支持怪物 AI 配置
- ✨ 支持怪物技能

---

## 十、下一步

- 📖 查看 [命令权限](/GuangDianMobs/command) 了解所有命令
- ⚙️ 查看 [配置文件](/GuangDianMobs/config) 了解详细配置
- 🔌 查看 [API接口](/GuangDianMobs/api) 了解开发者接口

---

*最后更新: 2026-06-11*
