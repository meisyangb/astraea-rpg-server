# Minecraft RPG 配置示例

## 示例1: 创建服务适配器

### 新插件服务适配器模板

```java
// 在 plugins/新插件/src/main/java/cn/guangdian/新插件/adapter/ServiceAdapter.java
package cn.guangdian.新插件.adapter;

import cn.guangdian.新插件.新插件Main;
import cn.guangdian.rpgcore.RPGCore;
import cn.guangdian.rpgcore.api.AsyncExecutor;
import cn.guangdian.rpgcore.api.CacheProvider;
import cn.guangdian.rpgcore.api.EventBus;
import cn.guangdian.rpgcore.api.ServiceRegistry;
import cn.guangdian.rpgcore.api.SyncScheduler;
import cn.guangdian.rpgcore.api.ExternalServiceIntegration;
import cn.guangdian.rpgcore.service.api.新Service;
import org.bukkit.Bukkit;

public class 新ServiceAdapter implements 新Service {

    private final 新插件Main plugin;
    private final boolean useRPGCore;
    private AsyncExecutor asyncExecutor;
    private CacheProvider cacheProvider;
    private EventBus eventBus;
    private SyncScheduler scheduler;
    private ExternalServiceIntegration externalServices;

    public 新ServiceAdapter(新插件Main plugin) {
        this.plugin = plugin;
        this.useRPGCore = Bukkit.getPluginManager().isPluginEnabled("RPGCore");

        if (useRPGCore) {
            try {
                RPGCore rpgCore = RPGCore.getInstance();
                ServiceRegistry registry = rpgCore.getServiceRegistry();
                this.asyncExecutor = rpgCore.getAsyncExecutor();
                this.cacheProvider = rpgCore.getCacheProvider();
                this.eventBus = rpgCore.getEventBus();
                this.scheduler = rpgCore.getScheduler();
                this.externalServices = rpgCore.getExternalServices();

                // 注册服务
                registry.registerService(新Service.class, this);
                plugin.getLogger().info("已注册到 RPGCore: 新Service");

                // 订阅其他插件的事件
                subscribeToEvents();

            } catch (Exception e) {
                plugin.getLogger().warning("注册到 RPGCore 失败: " + e.getMessage());
            }
        }
    }

    private void subscribeToEvents() {
        if (eventBus == null) return;

        // 订阅点券交易事件
        eventBus.subscribe(cn.guangdian.rpgcore.event.events.PointsTransactionEvent.class, event -> {
            // 处理点券事件
            plugin.getLogger().info("收到点券交易事件: " + event.getPlayerId());
        });

        plugin.getLogger().info("已订阅 RPGCore 事件系统");
    }

    // 实现服务接口方法...

    /**
     * 异步执行任务 - 使用统一 AsyncExecutor
     */
    private <T> java.util.concurrent.CompletableFuture<T> runAsync(java.util.concurrent.Callable<T> task) {
        if (asyncExecutor != null) {
            return asyncExecutor.execute(task);
        } else {
            return java.util.concurrent.CompletableFuture.supplyAsync(() -> {
                try {
                    return task.call();
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            });
        }
    }

    /**
     * 同步调度任务 - 使用统一 SyncScheduler
     */
    private long scheduleSyncRepeating(Runnable task, long delay, long period) {
        if (scheduler != null) {
            return scheduler.runSyncRepeating(task, delay, period);
        }
        return -1;
    }

    /**
     * 获取玩家称号 - 使用统一 ExternalServiceIntegration
     */
    private String getPlayerPrefix(org.bukkit.entity.Player player) {
        if (externalServices != null) {
            return externalServices.getPlayerPrefix(player);
        }
        return "";
    }

    /**
     * 解析占位符 - 使用统一 ExternalServiceIntegration
     */
    private String parsePlaceholders(org.bukkit.entity.Player player, String text) {
        if (externalServices != null) {
            return externalServices.parsePlaceholders(player, text);
        }
        return text;
    }

    /**
     * 缓存数据 - 使用统一 CacheProvider
     */
    private void cachePlayerData(UUID playerId, Object data) {
        if (cacheProvider != null) {
            cacheProvider.put("player:" + playerId + ":data", data,
                java.time.Duration.ofMinutes(30));
        }
    }

    private Object getCachedPlayerData(UUID playerId) {
        if (cacheProvider != null) {
            return cacheProvider.get("player:" + playerId + ":data", Object.class);
        }
        return null;
    }

    /**
     * 注销服务
     */
    public void unregister() {
        if (useRPGCore) {
            try {
                ServiceRegistry registry = RPGCore.getInstance().getServiceRegistry();
                registry.unregisterService(新Service.class);
                plugin.getLogger().info("已从 RPGCore 注销: 新Service");
            } catch (Exception e) {
                plugin.getLogger().warning("从 RPGCore 注销失败: " + e.getMessage());
            }
        }
    }

    public boolean isUsingRPGCore() {
        return useRPGCore;
    }
}
```

### 服务接口定义

```java
// 在 plugins/RPGCore/src/main/java/cn/guangdian/rpgcore/service/api/新Service.java
package cn.guangdian.rpgcore.service.api;

import java.util.UUID;
import java.util.concurrent.CompletableFuture;

public interface 新Service {

    /**
     * 检查服务是否可用
     */
    boolean isAvailable();

    // 其他业务方法...
}
```

---

## 示例2: 继承 AbstractRPGPlugin 插件基类

```java
package cn.guangdian.myplugin;

import cn.guangdian.rpgcore.plugin.AbstractRPGPlugin;
import org.bukkit.entity.Player;

public class MyPlugin extends AbstractRPGPlugin {
    
    private MyServiceAdapter serviceAdapter;
    private long updateTaskId = -1;
    
    @Override
    protected void onPluginEnable() {
        // 自动获得: rpgCore, externalServices, scheduler, exceptionHandler
        
        // 注册服务适配器
        serviceAdapter = new MyServiceAdapter(this);
        
        // 使用统一调度器创建定时任务
        if (isSchedulerAvailable()) {
            updateTaskId = scheduler.runSyncRepeating(this::updateAllPlayers, 20L, 20L);
        }
        
        // 使用统一外部服务获取玩家信息
        for (Player player : getServer().getOnlinePlayers()) {
            String prefix = externalServices.getPlayerPrefix(player);
            String parsed = externalServices.parsePlaceholders(player, "%gdrpg_attack%");
            getLogger().info(player.getName() + " prefix: " + prefix + ", attack: " + parsed);
        }
        
        getLogger().info(getPluginName() + " v" + getDescription().getVersion() + " 已启动");
    }
    
    @Override
    protected void onPluginDisable() {
        // 取消定时任务
        if (scheduler != null && updateTaskId >= 0) {
            scheduler.cancelTask(updateTaskId);
        }
        
        // 注销服务
        if (serviceAdapter != null) {
            serviceAdapter.unregister();
        }
        
        getLogger().info(getPluginName() + " 已关闭");
    }
    
    @Override
    protected String getPluginName() {
        return "MyPlugin";
    }
    
    private void updateAllPlayers() {
        // 使用异常处理器安全执行
        safeRun(() -> {
            for (Player player : getServer().getOnlinePlayers()) {
                updatePlayer(player);
            }
        });
    }
    
    private void updatePlayer(Player player) {
        // 业务逻辑...
    }
}
```

---

## 示例3: 使用 ExternalServiceIntegration

```java
// 获取外部服务集成
ExternalServiceIntegration ext = RPGCore.getInstance().getExternalServices();

// 检查服务状态
if (ext.isLuckPermsEnabled()) {
    String prefix = ext.getPlayerPrefix(player);
    String suffix = ext.getPlayerSuffix(player);
    String group = ext.getPlayerPrimaryGroup(player);
}

if (ext.isVaultEnabled()) {
    double balance = ext.getBalance(player);
    ext.deposit(player, 100.0);
    ext.withdraw(player, 50.0);
}

if (ext.isPlaceholderAPIEnabled()) {
    String parsed = ext.parsePlaceholders(player, "%gdrpg_attack% + %gdrpg_health%");
}

// 获取完整状态
String status = ext.getExternalServiceStatus();
// 输出: "LuckPerms: ✓, Vault: ✓, PlaceholderAPI: ✓"
```

---

## 示例4: 使用 SyncScheduler 统一调度器

```java
// 获取调度器
SyncScheduler scheduler = RPGCore.getInstance().getScheduler();

// 同步执行
scheduler.runSync(() -> {
    player.sendMessage("Hello!");
});

// 延迟执行 (20 ticks = 1秒)
long taskId = scheduler.runSyncLater(() -> {
    player.sendMessage("Delayed message!");
}, 20L);

// 重复执行
long repeatingId = scheduler.runSyncRepeating(() -> {
    updateScoreboard();
}, 0L, 20L);  // 立即开始，每秒执行

// 异步执行
scheduler.runAsync(() -> {
    // 数据库操作等耗时任务
    loadDataFromDatabase();
});

// 取消任务
scheduler.cancelTask(repeatingId);

// 取消所有任务
scheduler.cancelAllTasks();

// 查看活跃任务数
int active = scheduler.getActiveTaskCount();
```

---

## 示例5: 使用高性能缓存 (TTLCacheManager)

### 缓存基本操作

```java
// 获取 RPGCore 缓存提供者
CacheProvider cache = RPGCore.getInstance().getCacheProvider();

// 存入缓存 (30分钟过期)
cache.put("player:" + playerId + ":stats", playerStats, Duration.ofMinutes(30));

// 获取缓存
PlayerStats stats = cache.get("player:" + playerId + ":stats", PlayerStats.class);

// 带加载器的获取 (缓存未命中时自动加载)
PlayerStats stats = cache.getOrLoad(
    "player:" + playerId + ":stats",
    PlayerStats.class,
    () -> loadFromDatabase(playerId),  // 加载器
    Duration.ofMinutes(30)
);

// 批量失效 (使用通配符)
cache.invalidatePattern("player:" + playerId + ":*");

// 单个失效
cache.invalidate("player:" + playerId + ":stats");
```

### 性能特性

```java
// TTLCacheManager 内部实现已优化:

// 1. LRU淘汰 - O(1) 复杂度 (LinkedHashMap)
// 当缓存满时自动淘汰最久未访问的条目

// 2. Pattern缓存 - 避免重复编译正则
// invalidatePattern("player:*:stats") 只编译一次正则

// 3. 增量过期清理
// 每分钟随机抽样50个条目检查过期，避免全量扫描

// 4. 查看缓存统计
CacheStats stats = cache.getStats();
System.out.println("命中: " + stats.getHitCount());
System.out.println("未命中: " + stats.getMissCount());
System.out.println("命中率: " + stats.getHitRate() + "%");
System.out.println("淘汰数: " + stats.getEvictionCount());
```

---

## 示例6: 使用事件总线 (SimpleEventBus)

### 发布事件

```java
// 获取事件总线
EventBus eventBus = RPGCore.getInstance().getEventBus();

// 创建并发布事件
PointsTransactionEvent event = new PointsTransactionEvent(
    playerId,
    PointsTransactionEvent.TransactionType.DEPOSIT,
    amount,
    beforeBalance,
    afterBalance,
    "管理员充值"
);
eventBus.publish(event);

// 异步发布 (适用于耗时操作或非关键事件)
eventBus.publishAsync(new PlayerDataLoadEvent(playerId));
```

### 订阅事件

```java
// 在插件启动时订阅
private void subscribeToEvents() {
    EventBus eventBus = RPGCore.getInstance().getEventBus();

    // 订阅点券交易事件
    eventBus.subscribe(PointsTransactionEvent.class, event -> {
        if (event.getAmount() > 10000) {
            getLogger().info("大额交易: " + event.getPlayerId() + " +" + event.getAmount());
        }
    });

    // 订阅属性变更事件
    eventBus.subscribe(PlayerStatsChangedEvent.class, event -> {
        // 更新计分板显示
        updateScoreboard(event.getPlayer());
    });
}
```

---

## 示例7: 使用 PlayerLockManager

### 单玩家操作

```java
PlayerLockManager lockManager = RPGCore.getInstance().getLockManager();

// 单玩家操作
public void updatePlayerData(UUID playerId, Runnable task) {
    try {
        lockManager.executeWithLock(playerId, task);
    } catch (LockTimeoutException e) {
        getLogger().warning("获取玩家锁超时: " + playerId);
    }
}
```

### 双玩家操作 (转账)

```java
// 转账操作 - 自动按UUID排序防止死锁
public void transfer(UUID from, UUID to, long amount) {
    try {
        lockManager.executeWithDualLock(from, to, () -> {
            long fromBalance = getBalance(from);
            if (fromBalance < amount) {
                throw new RuntimeException("余额不足");
            }
            setBalance(from, fromBalance - amount);
            setBalance(to, getBalance(to) + amount);
        });
    } catch (LockTimeoutException e) {
        getLogger().warning("转账获取锁超时");
    }
}
```

---

## 示例8: 创建完整装备套装

### 武器 (神话剑)
```yaml
神话剑:
  Id: 276
  Display: '&d&l神话之剑'
  Lore:
  - '&e锻造武器                        传说'
  - '&4&m&l一一一一一一一一一'
  - '&8攻击力: &b5000-12000'
  - '&8暴击几率: &b15%'
  - '&8暴击伤害: &b280%'
  - '&8生命吸取: &b8%'
  - '&8中毒: &b12%'
  - '&4&m&l一一一一一一一一一'
  - '&6&l主动技能: &e毒刃风暴'
  - '&7冷却时间: 30秒'
  - '&7对周围敌人释放剧毒'
  - '&4&m&l一一一一一一一一一'
  - '&e传说中的神器,蕴含远古力量'
  - '&4&m&l一一一一一一一一一'
  - '&5分解后可获得&8[&e神话碎片&8]&510颗'
  - '&4&m&l一一一一一一一一一'
  - '&4*&3[&7可镶嵌<红宝石>&3]'
  - '&4*&3[&7可镶嵌<红宝石>&3]'
  - '&e*&3[&7可镶嵌<黄宝石>&3]'
  - '&9*&3[&7可镶嵌<蓝宝石>&3]'
  Options:
    Unbreakable: true
```

---

## 示例9: 创建副本BOSS

### BOSS怪物配置
```yaml
暗影龙王:
  Type: WITHER_SKELETON
  Display: '&4&l【BOSS】暗影龙王'
  Health: 80000
  Damage: 800
  Options:
    MovementSpeed: 0.32
    FollowRange: 35
    KnockbackResistance: 1.0
    AlwaysShowName: true
    PreventOtherDrops: true
    MaxCombatDistance: 30
    Despawn: false
    Persistent: true
  Modules:
    ThreatTable: true
  Equipment:
  - NETHERITE_SWORD HAND
  - NETHERITE_HELMET HEAD
  - NETHERITE_CHESTPLATE CHEST
  Skills:
  - skill{s=暗影侵蚀} @Target ~onAttack 1.0
  - skill{s=龙息风暴} @PlayersInRadius{r=8} ~onTimer:400
  - skill{s=召唤暗影仆从} @Self ~onTimer:1200
  - skill{s=暗影狂暴} @Self ~onDamaged <30% 1.0
  - message{msg="&4&l[BOSS] &f暗影龙王苏醒了！"} @PlayersInRadius{r=60} ~onSpawn
  - message{msg="&4&l[BOSS] &f暗影龙王已被击杀！"} @PlayersInRadius{r=60} ~onDeath
  Drops:
  - 暗影龙王掉落表 1
```

---

## 示例10: RPGCore 配置文件

```yaml
# server/plugins/RPGCore/config.yml

# 数据库配置
database:
  enabled: true
  url: "jdbc:mysql://localhost:3306/mc_rpg?useSSL=false&serverTimezone=Asia/Shanghai"
  username: "root"
  password: "password"
  max-pool-size: 20

# 异步执行器配置
async:
  thread-pool-size: 4

# 缓存配置
cache:
  max-size: 2000
  default-ttl-minutes: 30

# 锁配置
lock:
  timeout-ms: 3000

# 性能监控
monitor:
  enabled: true
  retention-minutes: 60

# 调试
debug:
  enabled: false
  verbose: false
```

---

## 示例11: 使用 ExceptionHandler

```java
// 获取异常处理器
ExceptionHandler handler = RPGCore.getInstance().getExceptionHandler();

// 安全执行（带默认值）
String result = handler.safeCall(() -> {
    return riskyOperation();
}, "default_value");

// 安全执行（无返回值）
handler.safeRun(() -> {
    potentiallyFailingCode();
});

// 带上下文日志的执行
handler.safeRunWithLog(() -> {
    criticalOperation();
}, "CriticalOperation");

// 手动处理异常
try {
    riskyCode();
} catch (Exception e) {
    handler.handleException(e, "Context", player);
}

// 设置日志级别
handler.setLogLevel(ExceptionHandler.LogLevel.DEBUG);
```
