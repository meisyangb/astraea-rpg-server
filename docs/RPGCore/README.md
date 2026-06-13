# RPGCore

> 光点RPG服务器核心框架 - 所有插件的基础依赖

## 一、简介

RPGCore 是光点RPG插件体系的核心框架，提供服务注册、事件总线、缓存管理、异步执行等基础服务。所有光点插件都依赖此核心运行。

### 功能特性

- **服务注册中心** - 统一的服务注册与发现机制
- **事件总线** - 高性能的跨插件事件通信
- **缓存系统** - 支持多种缓存策略的数据缓存
- **异步执行器** - 安全的异步任务调度
- **GUI框架** - 统一的GUI管理器
- **工具类库** - 颜色处理、PDC数据存储等工具

### 架构设计

```
┌─────────────────────────────────────────┐
│              RPGCore                     │
├─────────────────────────────────────────┤
│  ┌─────────┐ ┌─────────┐ ┌─────────┐   │
│  │Service  │ │EventBus │ │ Cache   │   │
│  │Registry │ │         │ │Provider │   │
│  └─────────┘ └─────────┘ └─────────┘   │
│  ┌─────────┐ ┌─────────┐ ┌─────────┐   │
│  │Async    │ │  GUI    │ │  Util   │   │
│  │Executor │ │Manager  │ │         │   │
│  └─────────┘ └─────────┘ └─────────┘   │
└─────────────────────────────────────────┘
           ▲         ▲         ▲
           │         │         │
    ┌──────┴───┬─────┴────┬────┴──────┐
    │          │          │           │
┌───┴───┐ ┌───┴───┐ ┌────┴────┐ ┌────┴────┐
│Armor  │ │Class  │ │  Mobs   │ │  Quest  │
│Stats  │ │       │ │         │ │         │
└───────┘ └───────┘ └─────────┘ └─────────┘
```

## 二、前置依赖

### 可选依赖

| 插件 | 说明 | 下载 |
|------|------|------|
| PlaceholderAPI | 变量支持 | [SpigotMC](https://www.spigotmc.org/resources/6245/) |
| LuckPerms | 权限管理 | [SpigotMC](https://www.spigotmc.org/resources/28140/) |
| Vault | 经济支持 | [SpigotMC](https://www.spigotmc.org/resources/34315/) |

## 三、安装

1. 将 `RPGCore.jar` 放入 `plugins` 文件夹
2. 启动服务器，等待配置文件生成
3. 根据需要修改 `plugins/RPGCore/config.yml`

## 四、配置文件

### config.yml

```yaml
# RPGCore 核心配置

# 数据库配置
database:
  type: sqlite  # sqlite 或 mysql
  mysql:
    host: localhost
    port: 3306
    database: rpgcore
    username: root
    password: password
    pool-size: 10

# 缓存配置
cache:
  enabled: true
  max-size: 10000
  expire-seconds: 3600

# 异步执行器配置
async:
  pool-size: 4
  queue-capacity: 1000

# 调试模式
debug: false
```

### logback.xml

日志配置文件，支持自定义日志级别和输出格式。

## 五、提供的服务

RPGCore 通过服务注册中心提供以下服务接口：

### ServiceRegistry

服务注册中心，用于注册和获取服务。

```java
// 获取服务
ServiceRegistry registry = Bukkit.getServicesManager().load(ServiceRegistry.class);

// 注册服务
registry.register(MyService.class, myServiceImpl);

// 获取服务
MyService service = registry.get(MyService.class);
```

### EventBus

事件总线，用于跨插件事件通信。

```java
EventBus eventBus = registry.get(EventBus.class);

// 发布事件
eventBus.publish(new CustomEvent(data));

// 订阅事件
eventBus.subscribe(CustomEvent.class, event -> {
    // 处理事件
});
```

### CacheProvider

缓存提供者，支持多种缓存策略。

```java
CacheProvider cache = registry.get(CacheProvider.class);

// 存入缓存
cache.put("key", value, 3600);  // 缓存1小时

// 获取缓存
Object value = cache.get("key");

// 删除缓存
cache.invalidate("key");
```

### AsyncExecutor

异步执行器，安全的异步任务调度。

```java
AsyncExecutor executor = registry.get(AsyncExecutor.class);

// 异步执行
executor.runAsync(() -> {
    // 异步操作
});

// 异步执行后回到主线程
executor.runAsync(() -> {
    // 异步操作
    return result;
}).thenAcceptSync(result -> {
    // 主线程处理结果
});
```

## 六、GUI框架

RPGCore 提供统一的GUI管理框架。

### 创建GUI

```java
GUI gui = new GUIBuilder()
    .title("§a我的菜单")
    .size(27)
    .build();

// 设置物品
gui.setItem(0, ItemStack.of(Material.DIAMOND), click -> {
    player.sendMessage("你点击了钻石！");
});

// 打开GUI
gui.open(player);
```

### GUI事件

```java
gui.onClick(event -> {
    // 处理点击事件
});

gui.onClose(event -> {
    // 处理关闭事件
});
```

## 七、工具类

### ColorUtil

颜色处理工具，支持RGB颜色和传统颜色代码。

```java
// 转换颜色代码
String colored = ColorUtil.translate("&a绿色 &c红色");

// RGB颜色
String rgb = ColorUtil.translate("&#FF0000红色");
```

### PDCKeys

PDC数据存储键管理。

```java
// 获取PDC键
NamespacedKey key = PDCKeys.get("custom_data");

// 存储数据
itemMeta.getPersistentDataContainer().set(key, PersistentDataType.STRING, "value");

// 读取数据
String value = itemMeta.getPersistentDataContainer().get(key, PersistentDataType.STRING);
```

## 八、下一步

- [命令权限](/RPGCore/command) - 查看所有命令和权限
- [API接口](/RPGCore/api) - 开发者API文档
- [配置文件](/RPGCore/config) - 详细配置说明

---

*最后更新: 2026-05-12*
