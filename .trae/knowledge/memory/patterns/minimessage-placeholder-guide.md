# MiniMessage 占位符解析指南

> 记录 GuangDianMobHealth 插件开发中遇到的 MiniMessage 占位符解析问题及解决方案
> **版本: 1.0.0 | 更新: 2026-04-14**

---

## 问题描述

在使用 MiniMessage 的 `TagResolver` 动态替换占位符时，如果占位符的值本身包含 MiniMessage 标签（如 `<green>`、`<red>`），使用 `Placeholder.unparsed()` 会导致这些标签被转义为纯文本，而不是被解析为颜色。

### 错误示例

```java
String healthBar = "<dark_gray>[<green>||||<red>||||||<dark_gray>]";

TagResolver resolver = TagResolver.resolver(
    Placeholder.unparsed("health_bar", healthBar)  // ❌ 错误：标签会被转义
);

Component result = miniMessage.parse("<name> <health_bar>", resolver);
// 结果：显示为纯文本 "<dark_gray>[<green>||||<red>||||||<dark_gray>]"
```

---

## 解决方案

对于包含 MiniMessage 标签的值，需要先解析为 `Component`，然后使用 `Placeholder.component()`。

### 正确示例

```java
// 1. 构建包含 MiniMessage 标签的字符串
String healthBar = "<dark_gray>[<green>||||<red>||||||<dark_gray>]";

// 2. 先解析为 Component
Component healthBarComponent = miniMessage.parse(healthBar);

// 3. 使用 Placeholder.component() 传递
TagResolver resolver = TagResolver.resolver(
    Placeholder.unparsed("name", entityName),                    // 纯文本
    Placeholder.component("health_bar", healthBarComponent),     // ✅ 正确：已解析的 Component
    Placeholder.unparsed("health", String.valueOf(health))       // 纯文本
);

// 4. 解析最终格式
Component result = miniMessage.parse("<name> <health_bar>", resolver);
// 结果：正确显示颜色 [||||||||||]
```

---

## 关键区别

| 方法 | 用途 | 适用场景 | 示例 |
|------|------|----------|------|
| `Placeholder.unparsed(key, value)` | 纯文本，不解析 MiniMessage 标签 | 玩家名称、数字、普通文本 | `Placeholder.unparsed("name", "Steve")` |
| `Placeholder.component(key, component)` | 已解析的 Component，保留 MiniMessage 效果 | 带颜色的文本、复杂的 MiniMessage 格式 | `Placeholder.component("health_bar", parsedComponent)` |

---

## 完整代码示例

### 配置文件 (config.yml)

```yaml
# 使用 MiniMessage 格式
format: "<name><reset> <health_bar><reset> <green><health>"
display:
  bar:
    # 血量条颜色（MiniMessage 格式）
    colors:
      - "0-100:<green>"
    empty-color: "<red>"
    bracket-left: "<dark_gray>["
    bracket-right: "<dark_gray>]"
```

### Java 代码实现

```java
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import net.kyori.adventure.text.minimessage.tag.resolver.TagResolver;

public class HealthDisplayManager {
    private MiniMessageService miniMessage;
    private String format;
    
    public void updateDisplay(LivingEntity entity, TextDisplay display) {
        double health = entity.getHealth();
        double maxHealth = entity.getAttribute(Attribute.GENERIC_MAX_HEALTH).getValue();
        
        // 获取实体名称（纯文本）
        String name = entity.getName();
        
        // 构建血量条（包含 MiniMessage 标签）
        String healthBar = buildHealthBar(health, maxHealth);
        // 例如: "<dark_gray>[<green>||||<red>||||||<dark_gray>]"
        
        // 关键步骤：先解析 healthBar 中的 MiniMessage 标签
        Component healthBarComponent = miniMessage.parse(healthBar);
        
        // 使用 TagResolver 动态替换占位符
        TagResolver resolver = TagResolver.resolver(
            Placeholder.unparsed("name", name),                          // 纯文本
            Placeholder.component("health_bar", healthBarComponent),     // 已解析的 Component
            Placeholder.unparsed("health", String.valueOf((int) health)),
            Placeholder.unparsed("max_health", String.valueOf((int) maxHealth)),
            Placeholder.unparsed("percent", String.valueOf((int) percent))
        );
        
        // 解析最终格式并设置到 TextDisplay
        display.text(miniMessage.parse(format, resolver));
    }
    
    private String buildHealthBar(double health, double maxHealth) {
        int filled = (int) ((health / maxHealth) * barLength);
        StringBuilder bar = new StringBuilder();
        bar.append(bracketLeft);  // "<dark_gray>["
        
        for (int i = 0; i < barLength; i++) {
            if (i < filled) {
                bar.append(fillColor).append(barSymbol);  // "<green>|"
            } else {
                bar.append(emptyColor).append(barSymbol); // "<red>|"
            }
        }
        
        bar.append(bracketRight);  // "<dark_gray>]"
        return bar.toString();
    }
}
```

---

## 注意事项

1. **unparsed vs component**
   - `unparsed` 会将 `<` 和 `>` 转义，防止注入攻击
   - `component` 需要传入已解析的 Component 对象

2. **混合使用**
   - 纯文本使用 `Placeholder.unparsed()`
   - 带 MiniMessage 标签的文本先解析为 Component，再使用 `Placeholder.component()`

3. **性能考虑**
   - 频繁更新的显示（如血量条）建议缓存解析后的 Component
   - 静态文本可以只解析一次重复使用

---

## 相关文件

- [GuangDianMobHealth 源码](../../../../../plugins/GuangDianMobHealth/src/main/java/cn/guangdian/mobhealth/MobHealthDisplayManager.java)
- [RPGCore MiniMessageService](../../../../../plugins/RPGCore/src/main/java/cn/guangdian/rpgcore/message/MiniMessageService.java)

---

*最后更新: 2026-04-14*
