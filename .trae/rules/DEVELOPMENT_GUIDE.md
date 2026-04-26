# Astraea RPG 开发指南

> 完整的开发规范、禁止模式清单和代码模板库
> **版本: 2.0.0 | 更新: 2026-04-26**

---

## 📋 目录

1. [基本原则](#-基本原则)
2. [禁止模式清单](#-禁止模式清单)
3. [代码模板](#-代码模板)
4. [任务路由指南](#-任务路由指南)
5. [API 参考](#-api-参考)
6. [构建指南](#-构建指南)
7. [环境基线](#-环境基线)

---

## ⚠️ 基本原则

1. **不可随意回滚** - 每次提交必须有明确的理由和测试
2. **脚本采用 CMD 执行** - 禁止使用 PowerShell 执行构建脚本
3. **先构建验证后提交** - 禁止未验证就提交

---

## ❌ 禁止模式清单

### 1. 调度器禁止项

**❌ 禁止 (Bukkit 传统调度器)**
```java
new BukkitRunnable() { ... }.runTaskTimer(plugin, delay, period);
new BukkitRunnable() { ... }.runTaskLater(plugin, delay);
Bukkit.getScheduler().scheduleSyncRepeatingTask(plugin, task, delay, period);
Bukkit.getScheduler().runTaskAsynchronously(plugin, task);
```

**✅ 正确 (Paper 1.21.6 调度器)**
```java
// 异步任务
Bukkit.getAsyncScheduler().runNow(plugin, scheduledTask -> {
    // 异步执行
});

Bukkit.getAsyncScheduler().runDelayed(plugin, scheduledTask -> {
    // 延迟执行
}, 5000, java.util.concurrent.TimeUnit.MILLISECONDS);

// 同步任务 - 使用 RPGCore SyncScheduler
RPGCore rpgCore = RPGCore.getInstance();
SyncScheduler scheduler = rpgCore.getScheduler();
scheduler.runSyncLater(() -> { }, 50L);
scheduler.runSyncRepeating(() -> { }, 0L, 20L);
```

### 2. RPGCore 获取禁止项

**❌ 禁止**
```java
Bukkit.getPlugin("RPGCore")
(RPGCore) Bukkit.getPluginManager().getPlugin("RPGCore")
```

**✅ 正确**
```java
RPGCore rpgCore = RPGCore.getInstance();
// 或在 AbstractRPGPlugin 子类中自动注入
```

### 3. 颜色服务禁止项

**❌ 禁止**
```java
player.sendMessage(ChatColor.RED + "错误消息");
player.sendMessage("§c错误消息");
player.sendMessage("&a成功消息");
```

**✅ 正确**
```java
MiniMessageService mm = MiniMessageService.getInstance();
player.sendMessage(mm.red("错误消息"));
player.sendMessage(mm.green("成功消息"));
player.sendMessage(mm.colorize("<yellow>普通消息<reset> <red>错误"));
```

### 4. 外部服务调用禁止项

**❌ 禁止**
```java
LuckPermsProvider.get()
PlaceholderAPI.setPlaceholders(player, text)
expansion.unregister()
```

**✅ 正确**
```java
RPGCore rpgCore = RPGCore.getInstance();
ExternalServiceIntegration externalServices = rpgCore.getExternalServices();

if (externalServices.isLuckPermsEnabled()) {
    String prefix = externalServices.getPlayerPrefix(player);
}
if (externalServices.isPlaceholderAPIEnabled()) {
    String parsed = externalServices.parsePlaceholders(player, text);
}

// 注销占位符
PlaceholderAPI.unregisterExpansion(expansionInstance);
```

### 5. 插件主类禁止项

**❌ 禁止**
```java
public class MyPlugin extends JavaPlugin { }
```

**✅ 正确**
```java
public class MyPlugin extends AbstractRPGPlugin {
    @Override protected void onPluginEnable() {
        initCommonServices();
    }
    @Override protected void onPluginDisable() {
        cancelAllTasks();
    }
    @Override protected String getPluginName() { return "MyPlugin"; }
}
```

### 6. 全息图显示禁止项

**❌ 禁止 (ArmorStand)**
```java
ArmorStand hologram = location.getWorld().spawn(location, ArmorStand.class);
```

**✅ 正确 (TextDisplay)**
```java
TextDisplay textDisplay = location.getWorld().spawn(location, TextDisplay.class);
textDisplay.setText("全息图文字");
```

### 7. 日志服务禁止项

**❌ 禁止**
```java
System.out.println("调试信息");
Bukkit.getLogger().info("消息");
```

**✅ 正确**
```java
Logger logger = LoggerFactory.getLogger(MyClass.class);
logger.info("消息");
logger.warn("警告");
```

### 8. 脚本执行禁止项

**❌ 禁止 (PowerShell)**
```powershell
& "D:\gradle\gradle-9.4.0\bin\gradle.bat" build
```

**✅ 正确 (CMD)**
```cmd
cd /d e:\原创RPG服务端
set JAVA_HOME=e:\原创RPG服务端\tools\jdk-21.0.10+7
D:\gradle\gradle-9.4.0\bin\gradle.bat build --no-configuration-cache -x test
```

### 9. 事件系统规范

**架构**: Bukkit 事件 + RPGCore EventPublisher 管控层

**✅ 推荐 (使用 EventPublisher)**
```java
EventPublisher.publish(new MyCustomEvent(player, data));
EventPublisher.publishAsync(new MyCustomEvent(player, data));
```

**✅ 备选 (直接使用 Bukkit)**
```java
Bukkit.getPluginManager().callEvent(new MyCustomEvent(player, data));
```

**订阅事件 (标准 Bukkit 方式)**
```java
@EventHandler
public void onMyEvent(MyCustomEvent event) {
    // 处理事件
}
```

### 10. 事件位置规范

**原则**: 基础设施事件在 Core，业务事件在插件

| 事件类型 | 归属 |
|---------|------|
| 数据生命周期 | RPGCore |
| 模块管理 | RPGCore |
| 属性/血量 | GuangDianArmorStats |
| 等级/经验 | GuangDianClass |
| 经济/交易 | GuangDianPoints/Market |

---

## 📋 代码模板

### 模板1: 插件主类

```java
import cn.guangdian.rpgcore.plugin.AbstractRPGPlugin;
import cn.guangdian.rpgcore.RPGCore;
import cn.guangdian.rpgcore.api.SyncScheduler;
import cn.guangdian.rpgcore.api.ServiceRegistry;
import cn.guangdian.rpgcore.integration.ExternalServiceIntegration;
import cn.guangdian.rpgcore.message.MiniMessageService;
import cn.guangdian.rpgcore.sound.SoundService;
import cn.guangdian.rpgcore.inject.GuiceSupport;
import javax.inject.Inject;

public class MyPlugin extends AbstractRPGPlugin {

    // RPGCore 自动注入的字段
    // protected RPGCore rpgCore;
    // protected SyncScheduler scheduler;
    // protected ExternalServiceIntegration externalServices;
    
    @Inject
    private MyService myService;

    @Override
    protected void onPluginEnable() {
        initCommonServices();
        ServiceRegistry registry = rpgCore.getServiceRegistry();
        getLogger().info(getPluginName() + " 已启动");
    }

    @Override
    protected void onPluginDisable() {
        cancelAllTasks();
        getLogger().info(getPluginName() + " 已关闭");
    }

    @Override
    protected String getPluginName() {
        return "MyPlugin";
    }
}
```

### 模板2: 服务适配器

```java
public class MyServiceAdapter implements MyService {

    private final MyPlugin plugin;

    public MyServiceAdapter(MyPlugin plugin) {
        this.plugin = plugin;
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
}
```

### 模板3: 数据处理器

```java
import cn.guangdian.rpgcore.lifecycle.AbstractPlayerDataHandler;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.Map;

public class MyDataHandler extends AbstractPlayerDataHandler {

    private final Map<UUID, PlayerData> dataCache = new ConcurrentHashMap<>();

    public MyDataHandler(JavaPlugin plugin) {
        super(plugin);
    }

    @Override
    protected void onPlayerLoad(Player player) {
        UUID uuid = player.getUniqueId();
        try {
            PlayerData data = loadFromDatabase(uuid);
            if (data != null) {
                dataCache.put(uuid, data);
            }
        } catch (Exception e) {
            plugin.getLogger().warning("加载 " + player.getName() + " 数据失败: " + e.getMessage());
        }
    }

    @Override
    protected void onPlayerSave(Player player) {
        UUID uuid = player.getUniqueId();
        PlayerData data = dataCache.get(uuid);
        if (data != null) {
            try {
                saveToDatabase(uuid, data);
                dataCache.remove(uuid);
            } catch (Exception e) {
                plugin.getLogger().warning("保存 " + player.getName() + " 数据失败: " + e.getMessage());
            }
        }
    }

    @Override
    public int getPriority() { return 100; }
    
    @Override
    public String getHandlerName() { return "MyData"; }
}

// 注册: new MyDataHandler(this).register();
// 注销: new MyDataHandler(this).unregister();
```

### 模板4: 自定义事件

```java
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

public class MyCustomEvent extends Event {

    private static final HandlerList HANDLERS = new HandlerList();
    
    private final Player player;
    private final String data;

    public MyCustomEvent(Player player, String data) {
        super(!Bukkit.isPrimaryThread());
        this.player = player;
        this.data = data;
    }

    public Player getPlayer() { return player; }
    public String getData() { return data; }

    @Override
    public HandlerList getHandlers() {
        return HANDLERS;
    }

    public static HandlerList getHandlerList() {
        return HANDLERS;
    }
}
```

### 模板5: build.gradle

```gradle
plugins {
    id 'java'
}

repositories {
    mavenCentral()
    maven { url = 'https://repo.papermc.io/repository/maven-public/' }
    flatDir { dirs 'libs' }
}

dependencies {
    compileOnly 'io.papermc.paper:paper-api:1.21.6-R0.1-SNAPSHOT'
    compileOnly project(':plugins:RPGCore')
    compileOnly name: 'Vault'
}

jar {
    archiveBaseName.set('插件名')
}

java {
    sourceCompatibility = JavaVersion.VERSION_21
    targetCompatibility = JavaVersion.VERSION_21
}
```

### 模板6: 并发安全

```java
import cn.guangdian.rpgcore.RPGCore;
import cn.guangdian.rpgcore.concurrency.PlayerLockManager;
import cn.guangdian.rpgcore.concurrency.LockTimeoutException;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.Map;

public class SafeDataManager {
    
    private final PlayerLockManager lockManager;
    private final Map<UUID, PlayerData> cache = new ConcurrentHashMap<>();
    
    public SafeDataManager() {
        RPGCore rpgCore = RPGCore.getInstance();
        this.lockManager = rpgCore.getLockManager();
    }
    
    // 单玩家数据修改
    public void addPoints(UUID playerUUID, int amount) {
        try {
            lockManager.executeWithLock(playerUUID, () -> {
                PlayerData data = cache.computeIfAbsent(playerUUID, 
                    uuid -> new PlayerData(uuid));
                data.addPoints(amount);
            });
        } catch (LockTimeoutException e) {
            plugin.getLogger().warning("获取玩家 " + playerUUID + " 锁超时");
        }
    }
    
    // 多玩家数据修改 (转账、交易等)
    public boolean transferPoints(UUID from, UUID to, int amount) {
        try {
            return lockManager.executeWithDualLock(from, to, () -> {
                PlayerData fromData = cache.get(from);
                PlayerData toData = cache.get(to);
                
                if (fromData == null || toData == null) {
                    return false;
                }
                
                if (!fromData.hasPoints(amount)) {
                    return false;
                }
                
                fromData.removePoints(amount);
                toData.addPoints(amount);
                return true;
            });
        } catch (LockTimeoutException e) {
            plugin.getLogger().warning("转账操作锁超时");
            return false;
        }
    }
}
```

### 模板7: Guice 依赖注入

```java
import cn.guangdian.rpgcore.inject.GuiceSupport;
import com.google.inject.AbstractModule;
import com.google.inject.Inject;
import javax.inject.Singleton;

public class MyPlugin extends AbstractRPGPlugin {
    @Inject
    private MyService myService;
    
    @Override
    protected void onPluginEnable() {
        initCommonServices();
        GuiceSupport.injectMembers(this);
        myService.doSomething();
    }
}

// 自定义 Module
public class MyModule extends AbstractModule {
    @Override
    protected void configure() {
        bind(MyService.class).to(MyServiceImpl.class).in(Singleton.class);
    }
}

@Singleton
public class MyServiceImpl implements MyService {
    @Override
    public void doSomething() {
        // 实现逻辑
    }
}
```

### 模板8: Configurate 配置管理

```java
import cn.guangdian.rpgcore.config.ConfigurateSupport;
import org.spongepowered.configurate.objectmapping.ConfigSerializable;

@ConfigSerializable
public class DatabaseConfig {
    private String host = "localhost";
    private int port = 3306;
    private String username = "root";
    private String password = "";
    private String database = "minecraft";
    
    public String getHost() { return host; }
    public int getPort() { return port; }
    public String getUsername() { return username; }
    public String getPassword() { return password; }
    public String getDatabase() { return database; }
}

// 使用
ConfigurateSupport<DatabaseConfig> config = ConfigurateSupport.builder(DatabaseConfig.class)
    .file("database.yml")
    .autoSave()
    .build();

DatabaseConfig data = config.get();
```

### 模板9: SLF4J 日志

```java
import cn.guangdian.rpgcore.logging.LoggerFactory;
import org.slf4j.Logger;

public class MyService {
    private static final Logger logger = LoggerFactory.getLogger(MyService.class);
    
    public void doSomething() {
        logger.trace("跟踪信息");
        logger.debug("调试信息: {}", someData);
        logger.info("普通信息: 玩家 {} 执行了命令 {}", playerName, command);
        logger.warn("警告信息");
        logger.error("错误信息", exception);
    }
}
```

### 模板10: 占位符扩展

```java
import me.clip.placeholderapi.expansion.PlaceholderExpansion;
import org.bukkit.entity.Player;

public class MyPlaceholder extends PlaceholderExpansion {

    private final MyPlugin plugin;

    public MyPlaceholder(MyPlugin plugin) {
        this.plugin = plugin;
    }

    @Override public String getIdentifier() { return "myplugin"; }
    @Override public String getAuthor() { return "Astraea RPG Team"; }
    @Override public String getVersion() { return "1.0.0"; }
    @Override public boolean persist() { return true; }

    @Override
    public String onPlaceholderRequest(Player player, String identifier) {
        if (player == null) return "";
        
        switch (identifier) {
            case "name": return player.getName();
            case "level": return String.valueOf(getPlayerLevel(player));
            default: return null;
        }
    }
}

// 注册: new MyPlaceholder(this).register();
// 注销: PlaceholderAPI.unregisterExpansion(myPlaceholderInstance);
```

---

## 🎯 任务路由指南

### 任务分类表

根据用户输入的关键词，快速识别任务类型并路由：

| 任务类型 | 关键词 | 处理方式 | 详细文档 |
|----------|--------|----------|----------|
| **构建** | 构建、build、编译、打包 | 执行唯一构建命令 | [BUILD_GUIDE.md](../docs/reference/BUILD_GUIDE.md) |
| **开发** | 开发、创建、实现、添加 | 激活 `minecraft-rpg-architect` | [SKILL.md](../skills/minecraft-rpg-architect/SKILL.md) |
| **修复** | 修复、解决、Bug、错误 | 激活 `minecraft-rpg-architect` + 记录修复 | [RELEASE_CHECKLIST.md](../docs/reference/RELEASE_CHECKLIST.md) |
| **审查** | 审查、检查、规范、优化 | 激活 `code-reviewer` | [SKILL.md](../skills/code-reviewer/SKILL.md) |
| **性能** | 性能、优化、卡顿、内存 | 激活 `performance-tuner` | [SKILL.md](../skills/performance-tuner/SKILL.md) |
| **UI** | UI、界面、菜单、GUI | 激活 `ui-designer` | [SKILL.md](../skills/ui-designer/SKILL.md) |
| **版本** | 版本、发布、标签、Git | 执行版本控制流程 | [VERSION_CONTROL.md](../docs/reference/VERSION_CONTROL.md) |
| **文档** | 文档、说明、更新日志 | 更新相关文档 | [INDEX.md](../knowledge/INDEX.md) |

### 快速路由

#### 1. 构建任务 (CMD)

```cmd
cd /d e:\原创RPG服务端
set JAVA_HOME=e:\原创RPG服务端\tools\jdk-21.0.10+7
D:\gradle\gradle-9.4.0\bin\gradle.bat build --no-configuration-cache -x test
```

#### 2. 开发任务流程
1. 激活技能: `minecraft-rpg-architect`
2. 阅读开发指南 (本文档)
3. 使用代码模板 (见下方)
4. 执行构建验证 (CMD)
5. 提交到仓库

#### 3. 修复任务流程
1. 激活技能: `minecraft-rpg-architect`
2. 分析问题根因
3. 实施修复
4. 记录到知识库
5. 更新文档

---

## 📚 API 参考

### 核心服务获取

```java
RPGCore rpgCore = RPGCore.getInstance();

// 服务
SyncScheduler scheduler = rpgCore.getScheduler();
ServiceRegistry registry = rpgCore.getServiceRegistry();
CacheProvider cacheProvider = rpgCore.getCacheProvider();
PlayerLockManager lockManager = rpgCore.getLockManager();
ExternalServiceIntegration externalServices = rpgCore.getExternalServices();

// 独立服务
MiniMessageService mm = MiniMessageService.getInstance();
CooldownManager cooldown = CooldownManager.getInstance();
```

### MiniMessage 消息

```java
MiniMessageService mm = MiniMessageService.getInstance();

// 简单颜色
player.sendMessage(mm.red("错误消息"));
player.sendMessage(mm.green("成功消息"));
player.sendMessage(mm.yellow("警告消息"));

// 复杂格式
player.sendMessage(mm.colorize("<gradient:#ff0000:#00ff00>渐变文字</gradient>"));

// ActionBar
player.sendActionBar(mm.green("ActionBar消息"));
```

### 缓存使用

```java
RPGCore rpgCore = RPGCore.getInstance();
CacheProvider cacheProvider = rpgCore.getCacheProvider();

// LoadingCache
LoadingCache<String, PlayerData> cache = cacheProvider.getLoadingCache(
    "playerData",
    uuid -> loadPlayerDataFromDatabase(uuid)
);

PlayerData data = cache.get(uuid);
cache.invalidate(uuid);
```

### 全息显示

```java
RPGCore rpgCore = RPGCore.getInstance();
TextDisplayService textDisplayService = rpgCore.getTextDisplayService();

// 创建全息图
textDisplayService.createHologram("id", location, mm.green("文字"));

// 显示给玩家
textDisplayService.showHologramToPlayer("id", player);

// 更新/移除
textDisplayService.updateHologram("id", mm.red("新消息!"));
textDisplayService.removeHologram("id");
```

---

## 🔨 构建指南

### 唯一构建方法 (CMD)

```cmd
cd /d e:\原创RPG服务端
set JAVA_HOME=e:\原创RPG服务端\tools\jdk-21.0.10+7
D:\gradle\gradle-9.4.0\bin\gradle.bat build --no-configuration-cache -x test
```

### 构建输出位置

```
plugins/{插件名}/build/libs/{插件名}-1.0.0.jar
```

### 代码审查清单

- [ ] 新插件主类继承 `AbstractRPGPlugin` 而非 `JavaPlugin`
- [ ] 已创建并注册 `ServiceAdapter`
- [ ] `onPluginDisable()` 调用了 `scheduler.cancelAllTasks()`
- [ ] `onPluginDisable()` 调用了 `serviceAdapter.unregister()`
- [ ] 异步任务使用 `Bukkit.getAsyncScheduler()`
- [ ] 同步任务使用 `SyncScheduler`
- [ ] 无 `new BukkitRunnable()` 调用
- [ ] 无 `ChatColor.` 调用
- [ ] 无 `§` 颜色码
- [ ] 无 `LuckPermsProvider.get()` 直接调用
- [ ] 无 `PlaceholderAPI.setPlaceholders()` 直接调用

## 🔧 环境基线

| 项目 | 值 |
|------|---|
| 服务端 | Paper 1.21.6 |
| JDK | JDK 21 (`tools/jdk-21.0.10+7`) |
| Gradle | 9.4.0 (`D:\gradle\gradle-9.4.0`) |
| 项目根目录 | `e:\原创RPG服务端` |
| 插件数量 | 24个 GuangDian* + RPGCore |
| Adventure | 4.26.1 |
| Caffeine | 3.1.8 |
| Guice | 7.0.0 |
| Configurate | 4.1.2 |
| SLF4J | 2.0.9 |
| MBassador | 1.3.2 |

---

*最后更新: 2026-04-26*
*版本: 2.1.0*
