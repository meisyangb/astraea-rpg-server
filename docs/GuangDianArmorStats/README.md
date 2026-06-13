# GuangDianArmorStats

> RPG装备属性系统 - 为物品添加自定义属性，支持伤害计算、Boss属性等功能

---

## 一、简介

GuangDianArmorStats 是一个功能强大的 RPG 装备属性系统，允许你为 Minecraft 中的物品添加自定义属性，实现复杂的 RPG 装备系统。

### 功能特性

- **自定义属性** - 支持创建任意数量的自定义属性（攻击力、防御力、暴击率等）
- **伤害计算** - 基于属性的智能伤害计算系统
- **Boss属性** - 为怪物配置特殊属性和技能
- **属性显示** - 在物品 Lore 中美观地显示属性
- **属性计算** - 支持加法、乘法、百分比等多种计算方式

### 前置要求

- **必需**: [RPGCore](/RPGCore/README)
- **可选**: PlaceholderAPI (用于变量显示)

### 兼容性

- **服务端**: Paper 1.21+
- **Java**: Java 21+
- **依赖插件**: RPGCore

---

## 二、安装

### 2.1 安装步骤

1. 确保已安装 [RPGCore](/RPGCore/README)
2. 下载 `GuangDianArmorStats.jar`
3. 将 jar 文件放入 `plugins` 文件夹
4. 重启服务器
5. 编辑 `plugins/GuangDianArmorStats/attributes.yml` 配置属性

### 2.2 验证安装

```bash
/gdas info          # 查看插件信息
/gdas reload        # 重载配置
```

---

## 三、快速开始

### 3.1 创建自定义属性

编辑 `plugins/GuangDianArmorStats/attributes.yml`:

```yaml
# 属性配置文件
# 最后更新: 2026-06-11

attributes:
  strength:
    display: "§c攻击力"
    description: "增加物理伤害"
    default: 0
    max: 1000
    format: "+{value}"
  
  defense:
    display: "§9防御力"
    description: "减少受到的伤害"
    default: 0
    max: 500
    format: "+{value}"
  
  health:
    display: "§a生命值"
    description: "增加最大生命值"
    default: 0
    max: 2000
    format: "+{value}"
  
  crit_chance:
    display: "§e暴击率"
    description: "暴击触发概率"
    default: 0
    max: 100
    format: "{value}%"
```

### 3.2 为物品添加属性

使用命令为物品添加属性：

```bash
# 手持物品并添加属性
/gdas add <属性名> <数值>

# 示例：添加攻击力
/gdas add strength 10

# 示例：添加防御力
/gdas add defense 5
```

### 3.3 查看物品属性

```bash
/gdas view          # 查看手持物品的属性
/gdas list          # 列出所有可用属性
```

---

## 四、命令权限

### 4.1 玩家命令

| 命令 | 权限 | 说明 |
|------|------|------|
| `/gdas view` | 无 | 查看手持物品的属性 |
| `/gdas list` | 无 | 列出所有可用属性 |

### 4.2 管理员命令

| 命令 | 权限 | 说明 |
|------|------|------|
| `/gdas add <属性> <数值>` | `guangdianarmorstats.use` | 为手持物品添加属性 |
| `/gdas remove <属性>` | `guangdianarmorstats.use` | 移除手持物品的属性 |
| `/gdas reload` | `guangdianarmorstats.admin` | 重载配置文件 |
| `/gdas info` | `guangdianarmorstats.admin` | 查看插件信息 |
| `/gdas debug` | `guangdianarmorstats.admin.debug` | 切换调试模式 |

---

## 五、配置文件

### 5.1 attributes.yml

属性配置文件，定义所有可用属性。

```yaml
# 属性配置示例
attributes:
  <属性ID>:
    display: "显示名称"
    description: "属性描述"
    default: 默认值
    max: 最大值
    format: "显示格式"
```

**配置项说明：**

| 配置项 | 类型 | 说明 |
|--------|------|------|
| `display` | String | 属性显示名称，支持颜色代码 |
| `description` | String | 属性描述 |
| `default` | Number | 默认值 |
| `max` | Number | 最大值限制 |
| `format` | String | 显示格式，`{value}` 会被替换为实际数值 |

### 5.2 config.yml

主配置文件。

```yaml
# 主配置
debug: false

# 属性计算方式
calculation:
  # 伤害计算公式
  damage-formula: "strength * 1.5 + crit_damage * crit_chance / 100"
  
  # 防御计算公式
  defense-formula: "defense * 0.8"

# 显示设置
display:
  # 是否显示属性 Lore
  show-lore: true
  
  # Lore 显示位置（before: 在描述前，after: 在描述后）
  lore-position: "after"
  
  # 属性分隔符
  separator: "§7----------------"
```

---

## 六、API 接口

### 6.1 获取属性管理器

```java
// 获取属性管理器
AttributeManager manager = RPGCore.getServiceRegistry().get(AttributeManager.class);

// 获取物品属性
Map<String, Double> attributes = manager.getItemAttributes(itemStack);

// 设置物品属性
manager.setAttribute(itemStack, "strength", 10);

// 移除物品属性
manager.removeAttribute(itemStack, "strength");
```

### 6.2 监听属性事件

```java
// 监听伤害计算事件
@EventHandler
public void onDamageCalc(DamageCalculateEvent event) {
    LivingEntity attacker = event.getAttacker();
    LivingEntity defender = event.getDefender();
    double damage = event.getDamage();
    
    // 修改伤害
    event.setDamage(damage * 1.5);
}

// 监听属性应用事件
@EventHandler
public void onAttributeApply(AttributeApplyEvent event) {
    ItemStack item = event.getItem();
    Map<String, Double> attributes = event.getAttributes();
    
    // 处理属性应用逻辑
}
```

---

## 七、常见问题

### Q: 属性不显示？

**A:** 检查以下事项：
1. 确保 `config.yml` 中 `display.show-lore` 为 `true`
2. 确保物品已正确添加属性
3. 使用 `/gdas view` 查看物品属性

### Q: 伤害计算不正确？

**A:** 检查 `config.yml` 中的伤害计算公式是否正确。

### Q: 如何创建百分比属性？

**A:** 在 `attributes.yml` 中配置 `format: "{value}%"`，然后在计算时使用百分比公式。

---

## 八、更新日志

### v1.0.0 (2026-06-11)
- 🎉 初始版本发布
- ✨ 支持自定义属性
- ✨ 支持伤害计算
- ✨ 支持 Boss 属性配置

---

## 九、下一步

- 📖 查看 [命令权限](/GuangDianArmorStats/command) 了解所有命令
- ⚙️ 查看 [配置文件](/GuangDianArmorStats/config) 了解详细配置
- 🔌 查看 [API接口](/GuangDianArmorStats/api) 了解开发者接口

---

*最后更新: 2026-06-11*
