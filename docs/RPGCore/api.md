# API 接口

> RPGCore 开发者 API 文档

## 一、概述

RPGCore 提供了一套完整的 API 接口，供其他插件调用和扩展。

### 获取 API 实例

```java
// 方式1：通过 Bukkit ServicesManager
RPGCoreAPI api = Bukkit.getServicesManager().load(RPGCoreAPI.class);

// 方式2：通过静态方法
RPGCoreAPI api = RPGCore.getAPI();
```

## 二、服务注册中心

### ServiceRegistry

服务注册中心用于管理插件服务。

```java
public interface ServiceRegistry {
    
    // 注册服务
    <T> void register(Class<T> serviceClass, T service);
    
    // 获取服务
    <T> T get(Class<T> serviceClass);
    
    // 检查服务是否存在
    <T> boolean has(Class<T> serviceClass);
    
    // 注销服务
    <T> void unregister(Class<T> serviceClass);
}
```

### 使用示例

```java
// 获取服务注册中心
ServiceRegistry registry = RPGCore.getServiceRegistry();

// 注册自定义服务
registry.register(MyCustomService.class, new MyCustomServiceImpl());

// 获取其他插件注册的服务
OtherService service = registry.get(OtherService.class);
```

## 三、事件总线

### EventBus

事件总线提供跨插件的事件通信机制。

```java
public interface EventBus {
    
    // 发布事件
    <T> void publish(T event);
    
    // 订阅事件
    <T> void subscribe(Class<T> eventClass, Consumer<T> handler);
    
    // 取消订阅
    <T> void unsubscribe(Class<T> eventClass, Consumer<T> handler);
    
    // 异步发布事件
    <T> CompletableFuture<Void> publishAsync(T event);
}
```

### 自定义事件

```java
public class PlayerLevelUpEvent {
    private final Player player;
    private final int oldLevel;
    private final int newLevel;
    
    public PlayerLevelUpEvent(Player player, int oldLevel, int newLevel) {
        this.player = player;
        this.oldLevel = oldLevel;
        this.newLevel = newLevel;
    }
    
    // getters...
}
```

### 使用示例

```java
EventBus eventBus = RPGCore.getEventBus();

// 发布事件
eventBus.publish(new PlayerLevelUpEvent(player, 1, 2));

// 订阅事件
eventBus.subscribe(PlayerLevelUpEvent.class, event -> {
    Player player = event.getPlayer();
    player.sendMessage("§a恭喜升级到 " + event.getNewLevel() + " 级！");
});
```

## 四、缓存系统

### CacheProvider

缓存提供者支持多种缓存策略。

```java
public interface CacheProvider {
    
    // 存入缓存（永不过期）
    void put(String key, Object value);
    
    // 存入缓存（指定过期时间，秒）
    void put(String key, Object value, long expireSeconds);
    
    // 获取缓存
    <T> T get(String key);
    
    // 获取缓存（带类型）
    <T> T get(String key, Class<T> type);
    
    // 检查缓存是否存在
    boolean exists(String key);
    
    // 删除缓存
    void invalidate(String key);
    
    // 清空所有缓存
    void invalidateAll();
    
    // 获取缓存统计
    CacheStats getStats();
}
```

### 使用示例

```java
CacheProvider cache = RPGCore.getCacheProvider();

// 存入缓存
cache.put("player:" + player.getUniqueId(), playerData, 3600);

// 获取缓存
PlayerData data = cache.get("player:" + player.getUniqueId(), PlayerData.class);

// 检查缓存
if (cache.exists("player:" + uuid)) {
    // 缓存命中
}

// 删除缓存
cache.invalidate("player:" + uuid);
```

## 五、异步执行器

### AsyncExecutor

异步执行器提供安全的异步任务调度。

```java
public interface AsyncExecutor {
    
    // 异步执行任务
    CompletableFuture<Void> runAsync(Runnable task);
    
    // 异步执行带返回值的任务
    <T> CompletableFuture<T> supplyAsync(Supplier<T> supplier);
    
    // 在主线程执行任务
    void runSync(Runnable task);
    
    // 异步执行后在主线程处理结果
    <T> CompletableFuture<Void> thenAcceptSync(
        CompletableFuture<T> future, 
        Consumer<T> consumer
    );
}
```

### 使用示例

```java
AsyncExecutor executor = RPGCore.getAsyncExecutor();

// 异步执行数据库查询
executor.supplyAsync(() -> {
    return database.getPlayerData(uuid);
}).thenAcceptSync(data -> {
    // 回到主线程更新玩家数据
    player.setHealth(data.getHealth());
});

// 异步执行多个任务
CompletableFuture.allOf(
    executor.runAsync(() -> loadPlayerData()),
    executor.runAsync(() -> loadInventory()),
    executor.runAsync(() -> loadLocation())
).thenRun(() -> {
    Bukkit.getLogger().info("所有数据加载完成");
});
```

## 六、GUI 框架

### GUI 接口

```java
public interface GUI {
    
    // 设置物品
    void setItem(int slot, ItemStack item);
    
    // 设置物品（带点击事件）
    void setItem(int slot, ItemStack item, Consumer<GUIClickEvent> handler);
    
    // 填充空白区域
    void fill(ItemStack item);
    
    // 打开GUI
    void open(Player player);
    
    // 关闭GUI
    void close(Player player);
    
    // 获取标题
    String getTitle();
    
    // 获取大小
    int getSize();
}
```

### GUIBuilder

```java
public class GUIBuilder {
    
    // 设置标题
    public GUIBuilder title(String title);
    
    // 设置大小（行数 × 9）
    public GUIBuilder size(int size);
    
    // 设置物品
    public GUIBuilder item(int slot, ItemStack item);
    
    // 设置物品（带点击事件）
    public GUIBuilder item(int slot, ItemStack item, Consumer<GUIClickEvent> handler);
    
    // 构建
    public GUI build();
}
```

### 使用示例

```java
// 创建GUI
GUI gui = new GUIBuilder()
    .title("§a我的菜单")
    .size(27)
    .item(0, createItem(Material.DIAMOND, "§b钻石"), click -> {
        click.getPlayer().sendMessage("你点击了钻石！");
    })
    .item(1, createItem(Material.GOLD_INGOT, "§e金币"), click -> {
        click.getPlayer().sendMessage("你点击了金币！");
    })
    .build();

// 填充空白
gui.fill(createItem(Material.GRAY_STAINED_GLASS_PANE, " "));

// 打开GUI
gui.open(player);
```

## 七、工具类

### ColorUtil

```java
public class ColorUtil {
    
    // 转换颜色代码
    public static String translate(String text);
    
    // 移除颜色代码
    public static String strip(String text);
    
    // 获取RGB颜色
    public static String rgb(int r, int g, int b);
}
```

### PDCKeys

```java
public class PDCKeys {
    
    // 获取或创建键
    public static NamespacedKey get(String key);
    
    // 获取所有键
    public static Set<NamespacedKey> getAll();
}
```

## 八、事件监听

### CoreEvent

RPGCore 提供的基础事件类。

```java
public abstract class CoreEvent extends Event {
    
    // 获取事件ID
    public String getEventId();
    
    // 获取事件时间
    public long getTimestamp();
}
```

### 可用事件

| 事件 | 说明 |
|------|------|
| `ServiceRegisterEvent` | 服务注册事件 |
| `ServiceUnregisterEvent` | 服务注销事件 |
| `CacheInvalidateEvent` | 缓存失效事件 |

## 九、Maven 依赖

```xml
<repository>
    <id>guangdian</id>
    <url>https://repo.guangdian.cn/repository/maven-public/</url>
</repository>

<dependency>
    <groupId>cn.guangdian</groupId>
    <artifactId>rpgcore-api</artifactId>
    <version>1.0.0</version>
    <scope>provided</scope>
</dependency>
```

## 十、Gradle 依赖

```groovy
repositories {
    maven {
        url = 'https://repo.guangdian.cn/repository/maven-public/'
    }
}

dependencies {
    compileOnly 'cn.guangdian:rpgcore-api:1.0.0'
}
```

---

*最后更新: 2026-05-12*
