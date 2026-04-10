---
alwaysApply: true
---

# Astraea RPG 开发规则

> 本规则适用于所有 GuangDian* 插件开发
> 基于 Paper 1.21.6 + RPGCore 架构

---

## 🤖 可用技能系统

### 技能列表

| 技能ID | 名称 | 描述 | 触发条件 |
|--------|------|------|----------|
| `skill-selector` | 技能选择器 | 自动分析用户请求并选择最佳技能组合 | 每次对话开始时自动调用 |
| `minecraft-rpg-architect` | RPG架构师 | Minecraft RPG插件开发专家 | 涉及GuangDian*插件、RPGCore、MythicMobs配置 |
| `code-reviewer` | 代码审查员 | 代码质量和规范检查 | 代码审查、规范验证 |
| `performance-tuner` | 性能优化师 | JVM调优和性能分析 | 性能问题、优化需求 |
| `ui-designer` | UI设计师 | 游戏界面和用户体验设计 | GUI、菜单、显示系统 |

### 技能文件位置
- **技能目录**: `e:\原创RPG服务端\.trae\skills\`
- **SKILL.md**: `e:\原创RPG服务端\.trae\skills\{skill-name}\SKILL.md`
- **示例文件**: `e:\原创RPG服务端\.trae\skills\minecraft-rpg-architect\examples.md`
- **参考手册**: `e:\原创RPG服务端\.trae\skills\minecraft-rpg-architect\reference.md`

---

## ❌ 禁止模式 (FORBIDDEN PATTERNS)

### 1. 调度器禁止项

**禁止使用的模式：**
```java
// ❌ 禁止 - BukkitRunnable
new BukkitRunnable() { ... }.runTaskTimer(plugin, delay, period);
new BukkitRunnable() { ... }.runTaskLater(plugin, delay);

// ❌ 禁止 - Bukkit Scheduler
Bukkit.getScheduler().scheduleSyncRepeatingTask(plugin, task, delay, period);
Bukkit.getScheduler().runTaskLater(plugin, task, delay);
Bukkit.getScheduler().runTaskTimer(plugin, task, delay, period);
```

**正确做法：**
```java
// ✅ 正确 - 使用 RPGCore SyncScheduler
RPGCore rpgCore = RPGCore.getInstance();
SyncScheduler scheduler = rpgCore.getScheduler();

// 延迟任务
long taskId = scheduler.runSyncLater(() -> {
    // 业务逻辑
}, 50L); // 50 ticks = 2.5秒

// 循环任务
long repeatingTaskId = scheduler.runSyncRepeating(() -> {
    // 业务逻辑
}, 0L, 20L); // 立即开始，每秒执行

// 异步任务
scheduler.runAsync(() -> {
    // 耗时操作
});

// 取消任务
scheduler.cancelTask(taskId);

// 插件禁用时取消所有任务
@Override
protected void onPluginDisable() {
    scheduler.cancelAllTasks();
}
```

---

### 2. RPGCore 获取禁止项

**禁止使用的模式：**
```java
// ❌ 禁止 - 通过名称获取 RPGCore
Bukkit.getPlugin("RPGCore")
Bukkit.getPluginManager().getPlugin("RPGCore")
(RPGCore) Bukkit.getPlugin("RPGCore")
```

**正确做法：**
```java
// ✅ 正确 - 使用单例模式
RPGCore rpgCore = RPGCore.getInstance();
if (rpgCore != null) {
    // 使用 rpgCore
}
```

---

### 3. 外部服务调用禁止项

**禁止使用的模式：**
```java
// ❌ 禁止 - 直接调用 LuckPerms
LuckPermsProvider.get()
luckPerms.getUserManager().getUser(player.getUniqueId())

// ❌ 禁止 - 直接调用 PlaceholderAPI
PlaceholderAPI.setPlaceholders(player, text)

// ❌ 禁止 - 错误的占位符注销方式
expansion.unregister() // PlaceholderExpansion 没有此方法！
```

**正确做法：**
```java
// ✅ 正确 - 通过 RPGCore ExternalServiceIntegration
RPGCore rpgCore = RPGCore.getInstance();
ExternalServiceIntegration externalServices = rpgCore.getExternalServices();

// LuckPerms
if (externalServices.isLuckPermsEnabled()) {
    String prefix = externalServices.getPlayerPrefix(player);
    String suffix = externalServices.getPlayerSuffix(player);
}

// PlaceholderAPI
if (externalServices.isPlaceholderAPIEnabled()) {
    String parsed = externalServices.parsePlaceholders(player, text);
}

// Vault 经济
if (externalServices.isVaultEnabled()) {
    double balance = externalServices.getBalance(player);
}

// ✅ 正确 - 占位符注销
PlaceholderAPI.unregisterExpansion(expansionInstance); // 静态方法！
```

---

### 4. MythicMobs PDC Key 禁止项

**禁止使用的模式：**
```java
// ❌ 禁止 - 旧版 Key 已废弃
new NamespacedKey("mythicmobs", "item")
meta.getPersistentDataContainer().get(
    new NamespacedKey("mythicmobs", "item"), 
    PersistentDataType.STRING
);
```

**正确做法：**
```java
// ✅ 正确 - 使用新版 "type" Key
NamespacedKey typeKey = new NamespacedKey("mythicmobs", "type");
String typeId = meta.getPersistentDataContainer().get(typeKey, PersistentDataType.STRING);

// 兼容方案 - 同时检查两个 Key
public String getMythicType(ItemMeta meta) {
    PersistentDataContainer pdc = meta.getPersistentDataContainer();
    
    // 优先检查新版 Key
    String type = pdc.get(new NamespacedKey("mythicmobs", "type"), PersistentDataType.STRING);
    if (type != null) return type;
    
    // 兼容旧版 Key
    type = pdc.get(new NamespacedKey("mythicmobs", "item"), PersistentDataType.STRING);
    return type;
}
```

---

### 5. 消息发送禁止项 (ChatColor)

**禁止使用的模式：**
```java
// ❌ 禁止 - ChatColor 拼接
player.sendMessage(ChatColor.RED + "错误消息");
player.sendMessage(ChatColor.GREEN + "成功: " + ChatColor.WHITE + playerName);

// ❌ 禁止 - § 颜色码
player.sendMessage("§c错误消息");
player.sendMessage("§a成功: §f" + playerName);
```

**正确做法：**
```java
// ✅ 正确 - 使用 Adventure API
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;

// 简单消息
player.sendMessage(Component.text("错误消息").color(NamedTextColor.RED));
player.sendMessage(Component.text("成功消息").color(NamedTextColor.GREEN));

// 组合消息
player.sendMessage(
    Component.text("[系统] ").color(NamedTextColor.GOLD)
        .append(Component.text("欢迎回来, ").color(NamedTextColor.WHITE))
        .append(Component.text(playerName).color(NamedTextColor.YELLOW))
        .append(Component.text("!").color(NamedTextColor.WHITE))
);

// 带装饰的消息
player.sendMessage(
    Component.text("重要提示").color(NamedTextColor.RED)
        .decoration(TextDecoration.BOLD, true)
        .decoration(TextDecoration.UNDERLINED, true)
);

// 可用的 NamedTextColor
// BLACK, DARK_BLUE, DARK_GREEN, DARK_AQUA, DARK_RED, DARK_PURPLE, GOLD, GRAY
// DARK_GRAY, BLUE, GREEN, AQUA, RED, LIGHT_PURPLE, YELLOW, WHITE
```

---

### 6. 插件主类结构

**禁止使用的模式：**
```java
// ❌ 禁止 - 直接继承 JavaPlugin
public class MyPlugin extends JavaPlugin {
    @Override
    public void onEnable() { }
    
    @Override
    public void onDisable() { }
}
```

**正确做法：**
```java
// ✅ 正确 - 继承 AbstractRPGPlugin
import cn.guangdian.rpgcore.plugin.AbstractRPGPlugin;
import cn.guangdian.rpgcore.RPGCore;
import cn.guangdian.rpgcore.api.SyncScheduler;
import cn.guangdian.rpgcore.integration.ExternalServiceIntegration;

public class MyPlugin extends AbstractRPGPlugin {
    
    // 自动注入的服务
    protected RPGCore rpgCore;
    protected SyncScheduler scheduler;
    protected ExternalServiceIntegration externalServices;
    protected ExceptionHandler exceptionHandler;
    
    @Override
    protected void onPluginEnable() {
        // 插件启动逻辑
        // 此时 rpgCore, scheduler 等已自动注入
        
        getLogger().info(getPluginName() + " 已启动");
    }
    
    @Override
    protected void onPluginDisable() {
        // 必须取消所有调度任务
        if (scheduler != null) {
            scheduler.cancelAllTasks();
        }
        
        // 注销服务适配器
        if (serviceAdapter != null) {
            serviceAdapter.unregister();
        }
        
        getLogger().info(getPluginName() + " 已关闭");
    }
    
    @Override
    protected String getPluginName() {
        return "MyPlugin";
    }
}
```

---

### 7. 全息图显示禁止项

**禁止使用的模式：**
```java
// ❌ 禁止 - 使用 ArmorStand 做全息图
ArmorStand hologram = location.getWorld().spawn(location, ArmorStand.class);
hologram.setVisible(false);
hologram.setCustomNameVisible(true);
hologram.setCustomName("全息图文字");
```

**正确做法：**
```java
// ✅ 正确 - 使用 TextDisplay (1.19.4+)
TextDisplay textDisplay = location.getWorld().spawn(location, TextDisplay.class);
textDisplay.setText("全息图文字");
textDisplay.setBillboard(Display.Billboard.CENTER);
textDisplay.setBackgroundColor(Color.fromARGB(0, 0, 0, 0));
textDisplay.setShadowed(true);

// 设置变换（高度等）
textDisplay.setTransformation(
    new Transformation(
        new Vector3f(0.0f, 2.8f, 0.0f), // 偏移
        new Quaternionf(),               // 旋转
        new Vector3f(1.0f, 1.0f, 1.0f), // 缩放
        new Quaternionf()
    )
);
```

---

## ✅ 代码审查清单

每次提交代码前，确认以下检查项：

### 结构检查
- [ ] 新插件主类继承 `AbstractRPGPlugin` 而非 `JavaPlugin`
- [ ] 已创建并注册 `ServiceAdapter`
- [ ] `onPluginDisable()` 调用了 `scheduler.cancelAllTasks()`
- [ ] `onPluginDisable()` 调用了 `serviceAdapter.unregister()`

### 调度器检查
- [ ] 无 `new BukkitRunnable()` 调用
- [ ] 无 `Bukkit.getScheduler()` 调用
- [ ] 所有定时任务ID已保存，并在 disable 时取消

### 外部服务检查
- [ ] 无 `LuckPermsProvider.get()` 直接调用
- [ ] 无 `PlaceholderAPI.setPlaceholders()` 直接调用
- [ ] 占位符注销使用 `PlaceholderAPI.unregisterExpansion()`

### API版本检查
- [ ] 无 `ArmorStand` 用于显示目的
- [ ] 无 `§` 颜色码（使用 Adventure API）
- [ ] 无 `ChatColor` 拼接（使用 Adventure API）
- [ ] MythicMobs 物品检测使用 `mythicmobs:type` 而非 `mythicmobs:item`

### 性能检查
- [ ] 玩家数据有缓存层（TTLCacheManager）
- [ ] 显示系统有脏标记/防抖机制
- [ ] 无在主线程执行的数据库 I/O

---

## 📦 常用代码模板

### 服务适配器模板
```java
public class MyServiceAdapter implements MyService {
    
    private final MyPlugin plugin;
    
    public MyServiceAdapter(MyPlugin plugin) {
        this.plugin = plugin;
        
        // 注册到 RPGCore ServiceRegistry
        RPGCore rpgCore = RPGCore.getInstance();
        if (rpgCore != null) {
            rpgCore.getServiceRegistry().registerService(MyService.class, this);
        }
    }
    
    public void unregister() {
        RPGCore rpgCore = RPGCore.getInstance();
        if (rpgCore != null) {
            rpgCore.getServiceRegistry().unregisterService(MyService.class);
        }
    }
    
    // 实现服务接口方法...
}
```

### 占位符扩展模板
```java
public class MyPlaceholder extends PlaceholderExpansion {
    
    private final MyPlugin plugin;
    
    public MyPlaceholder(MyPlugin plugin) {
        this.plugin = plugin;
    }
    
    @Override
    public String getIdentifier() {
        return "myplugin";
    }
    
    @Override
    public String getAuthor() {
        return "Astraea RPG Team";
    }
    
    @Override
    public String getVersion() {
        return "1.0.0";
    }
    
    @Override
    public boolean persist() {
        return true; // 持久化占位符
    }
    
    @Override
    public String onPlaceholderRequest(Player player, String identifier) {
        if (player == null) return "";
        
        switch (identifier) {
            case "name":
                return player.getName();
            // 更多占位符...
            default:
                return null;
        }
    }
}

// 注册方式（在插件 onEnable 中）
new MyPlaceholder(this).register();

// 注销方式（在插件 onDisable 中）
PlaceholderAPI.unregisterExpansion(myPlaceholderInstance);
```

### 数据处理器模板
```java
public class MyDataHandler extends AbstractPlayerDataHandler {
    
    private final MyPlugin plugin;
    private final Map<UUID, PlayerData> dataCache = new ConcurrentHashMap<>();
    
    public MyDataHandler(MyPlugin plugin) {
        super(plugin);
        this.plugin = plugin;
    }
    
    @Override
    protected void onPlayerLoad(Player player) {
        // 异步加载玩家数据
        plugin.getScheduler().runAsync(() -> {
            PlayerData data = loadFromDatabase(player.getUniqueId());
            
            // 切回主线程更新缓存
            plugin.getScheduler().runSyncLater(() -> {
                dataCache.put(player.getUniqueId(), data);
            }, 0L);
        });
    }
    
    @Override
    protected void onPlayerSave(Player player) {
        PlayerData data = dataCache.remove(player.getUniqueId());
        if (data != null) {
            // 异步保存
            plugin.getScheduler().runAsync(() -> {
                saveToDatabase(player.getUniqueId(), data);
            });
        }
    }
    
    @Override
    public int getPriority() {
        return 100; // 加载优先级（越小越先加载）
    }
    
    @Override
    public String getHandlerName() {
        return "MyData";
    }
}
```

---

## 🔧 构建命令

```powershell
# 环境设置
$env:JAVA_HOME="e:\原创RPG服务端\tools\jdk-21.0.10+7"

# 全量构建
& "D:\gradle\gradle-9.4.0\bin\gradle.bat" build --no-configuration-cache -x test

# 单插件构建
& "D:\gradle\gradle-9.4.0\bin\gradle.bat" :plugins:RPGCore:build --no-configuration-cache -x test

# 含静态检查的构建 (推荐)
& "D:\gradle\gradle-9.4.0\bin\gradle.bat" build checkstyle --no-configuration-cache -x test

# 清理重建
& "D:\gradle\gradle-9.4.0\bin\gradle.bat" clean build --no-configuration-cache -x test

# 部署所有插件到服务器
& "D:\gradle\gradle-9.4.0\bin\gradle.bat" deployAll --no-configuration-cache -x test
```

---

## 📚 知识库导航

### 技能文档
- **SKILL.md**: `e:\原创RPG服务端\.trae\skills\minecraft-rpg-architect\SKILL.md`
- **示例文件**: `e:\原创RPG服务端\.trae\skills\minecraft-rpg-architect\examples.md`
- **参考手册**: `e:\原创RPG服务端\.trae\skills\minecraft-rpg-architect\reference.md`

### 知识管理系统 (Knowledge Management)
- **知识库索引**: `e:\原创RPG服务端\.trae\knowledge\INDEX.md`
- **全局索引**: `e:\原创RPG服务端\.trae\knowledge\index.json`
- **搜索索引**: `e:\原创RPG服务端\.trae\knowledge\search.json`
- **标签系统**: `e:\原创RPG服务端\.trae\knowledge\meta\tags.json`

### 日志中心 (Logs)
- **迁移日志**: `e:\原创RPG服务端\.trae\knowledge\logs\categories\migration\`
- **修复日志**: `e:\原创RPG服务端\.trae\knowledge\logs\categories\bugfix\`

### 记忆中心 (Memory)
- **修复方案**: `e:\原创RPG服务端\.trae\knowledge\memory\fixes\`
- **模式记忆**: `e:\原创RPG服务端\.trae\knowledge\memory\patterns\`
- **解决方案**: `e:\原创RPG服务端\.trae\knowledge\memory\solutions\`

---

*规则版本: 2026-04-10*
*适用于: Astraea RPG 阿斯特瑞亚服务器*
