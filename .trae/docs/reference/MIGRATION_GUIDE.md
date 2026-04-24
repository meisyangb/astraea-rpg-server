# RPGCore API 迁移指南

> 从旧版 API 迁移到新版 API 的完整指南
> **版本: 1.4.0 | 更新: 2026-04-24**

---

## 📋 概述

RPGCore v1.4.0 引入了更成熟的第三方库支持，同时保持向后兼容。本指南帮助开发者从旧 API 迁移到新 API。

### 废弃 API 清单

| 废弃 API | 替代 API | 状态 | 迁移优先级 |
|---------|---------|------|-----------|
| `YamlDataStore` | `ConfigurateSupport` | `@Deprecated(forRemoval=true)` | 中 |
| `ServiceInjector` | `GuiceSupport` | `@Deprecated(forRemoval=true)` | 低 |
| `ColorUtil` | `MiniMessageService` | `@Deprecated(forRemoval=false)` | 低 |

---

## 1. YamlDataStore → ConfigurateSupport

### 旧代码（已废弃）

```java
import cn.guangdian.rpgcore.data.YamlDataStore;

public class OldConfigManager {
    private final YamlDataStore store = YamlDataStore.getInstance();
    
    public void loadConfig(File file) {
        Map<String, Object> data = store.load(file);
        String name = (String) data.get("name");
        int level = (int) data.get("level");
        // 手动类型转换，容易出错
    }
    
    public void saveConfig(File file, String name, int level) {
        Map<String, Object> data = new HashMap<>();
        data.put("name", name);
        data.put("level", level);
        store.save(file, data);
    }
}
```

### 新代码（推荐）

```java
import cn.guangdian.rpgcore.config.ConfigurateSupport;
import org.spongepowered.configurate.objectmapping.ConfigSerializable;

// 1. 定义类型安全的配置类
@ConfigSerializable
public class PlayerConfig {
    private String name = "default";
    private int level = 1;
    private List<String> permissions = new ArrayList<>();
    
    // Getters
    public String getName() { return name; }
    public int getLevel() { return level; }
    public List<String> getPermissions() { return permissions; }
}

// 2. 使用 ConfigurateSupport
public class NewConfigManager {
    private ConfigurateSupport<PlayerConfig> config;
    
    public void init() {
        config = ConfigurateSupport.builder(PlayerConfig.class)
            .file("player.yml")
            .autoSave()
            .build();
    }
    
    public PlayerConfig getConfig() {
        return config.get();  // 类型安全，无需转换
    }
    
    public void updateLevel(int newLevel) {
        config.update(c -> {
            // 使用反射更新，或添加 setter
        });
    }
}
```

### 迁移收益

- ✅ 类型安全，编译期检查
- ✅ 自动序列化/反序列化
- ✅ 支持默认值
- ✅ 自动保存选项
- ✅ 更好的性能

---

## 2. ServiceInjector → GuiceSupport

### 旧代码（已废弃）

```java
import cn.guangdian.rpgcore.inject.ServiceInjector;

public class OldService {
    @Inject
    private DatabaseService database;
    
    public OldService() {
        ServiceInjector.inject(this);  // 已废弃
    }
}
```

### 新代码（推荐）

```java
import cn.guangdian.rpgcore.inject.GuiceSupport;

// 方式1: 简单注入
public class NewService {
    @Inject
    private DatabaseService database;
    
    public NewService() {
        GuiceSupport.injectMembers(this);
    }
}

// 方式2: 使用子注入器（推荐）
public class MyPlugin extends AbstractRPGPlugin {
    @Inject
    private MyService myService;
    
    @Override
    protected void onPluginEnable() {
        initCommonServices();
        
        GuiceSupport.childInjector()
            .with(new MyModule())
            .inject(this);
    }
}

// 方式3: 简单绑定
public class MyPlugin extends AbstractRPGPlugin {
    @Override
    protected void onPluginEnable() {
        initCommonServices();
        
        GuiceSupport.createChildInjector(binder -> {
            binder.bind(MyService.class).to(MyServiceImpl.class).in(Singleton.class);
        }).injectMembers(this);
    }
}
```

### 迁移收益

- ✅ 更完整的 Guice 支持
- ✅ 子注入器支持
- ✅ 构建器模式
- ✅ 更好的错误处理

---

## 3. 新增 API 使用指南

### 3.1 LoggerFactory (SLF4J)

```java
import cn.guangdian.rpgcore.logging.LoggerFactory;
import org.slf4j.Logger;

public class MyService {
    private static final Logger logger = LoggerFactory.getLogger(MyService.class);
    
    public void doSomething() {
        // 占位符支持（高性能）
        logger.info("玩家 {} 执行了命令 {}", playerName, command);
        logger.debug("调试信息: {}", data);
        logger.error("错误发生", exception);
    }
}
```

### 3.2 EventBusSupport

```java
import cn.guangdian.rpgcore.event.EventBusSupport;
import cn.guangdian.rpgcore.event.CoreEvent;

// 定义事件
public class PlayerEvent extends CoreEvent {
    private final Player player;
    
    public PlayerEvent(Player player) {
        this.player = player;
    }
    
    public Player getPlayer() { return player; }
}

// 订阅事件
EventBusSupport.subscribe(PlayerEvent.class, event -> {
    event.getPlayer().sendMessage("收到事件！");
});

// 发布事件
EventBusSupport.publish(new PlayerEvent(player));
EventBusSupport.publishAsync(new PlayerEvent(player));
```

---

## 4. 渐进迁移策略

### 阶段 1: 新代码使用新 API

新开发的插件直接使用新 API：
```java
// 新插件直接使用 ConfigurateSupport
ConfigurateSupport<MyConfig> config = ConfigurateSupport
    .builder(MyConfig.class)
    .file("config.yml")
    .build();
```

### 阶段 2: 旧代码逐步迁移

旧插件可以逐步迁移，无需一次性修改：
```java
// 旧代码仍然可用（显示警告）
YamlDataStore store = YamlDataStore.getInstance();  // 已废弃但可用

// 新代码使用新 API
ConfigurateSupport<MyConfig> config = ConfigurateSupport
    .builder(MyConfig.class)
    .file("config.yml")
    .build();
```

### 阶段 3: 完全迁移

所有插件完成迁移后，废弃 API 将在未来版本中移除。

---

## 5. 常见问题

### Q: 旧代码还能用吗？
A: 可以。废弃 API 仍然可用，只是会显示编译器警告。

### Q: 必须立即迁移吗？
A: 不需要。可以逐步迁移，新旧 API 可以共存。

### Q: 迁移有什么好处？
A: 
- 类型安全
- 更好的性能
- 更少的代码
- 更好的维护性
- 使用成熟库

### Q: 如何禁用废弃警告？
A: 不推荐禁用。但可以使用 `@SuppressWarnings("deprecation")` 临时抑制。

---

## 6. 迁移检查清单

- [ ] 识别使用废弃 API 的代码
- [ ] 评估迁移优先级
- [ ] 编写新 API 的替代代码
- [ ] 测试新代码
- [ ] 逐步替换旧代码
- [ ] 验证功能一致性
- [ ] 更新文档

---

*最后更新: 2026-04-24*
