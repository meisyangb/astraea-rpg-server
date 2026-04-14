# 单例模式 - 双重检查锁定 (DCL)

> 高性能线程安全的单例实现模式

## 问题背景

传统的 `synchronized` 方法虽然线程安全，但每次获取实例都会加锁，性能较差。

```java
// ❌ 性能较差：每次调用都同步
public static synchronized Singleton getInstance() {
    if (instance == null) {
        instance = new Singleton();
    }
    return instance;
}
```

## 双重检查锁定 (DCL) 模式

```java
// ✅ 高性能：只在首次创建时同步
public static Singleton getInstance() {
    if (instance == null) {                      // 第一次检查（无锁）
        synchronized (Singleton.class) {          // 同步块
            if (instance == null) {              // 第二次检查（有锁）
                instance = new Singleton();
            }
        }
    }
    return instance;
}
```

## 工作原理

1. **第一次检查**（无锁）：快速路径，实例已存在时直接返回
2. **同步块**：确保线程安全
3. **第二次检查**（有锁）：防止多个线程同时创建实例

## 性能对比

| 场景 | synchronized 方法 | DCL |
|------|------------------|-----|
| 实例已存在 | 每次加锁 | 无锁 |
| 实例不存在 | 加锁创建 | 加锁创建 |
| 并发获取 | 串行 | 并行（实例存在时）|

## RPGCore 应用实例

### MiniMessageService

位置: `MiniMessageService.java`

```java
public final class MiniMessageService {
    private static MiniMessageService instance;
    
    private MiniMessageService() {
        // 初始化
    }
    
    public static MiniMessageService getInstance() {
        if (instance == null) {
            synchronized (MiniMessageService.class) {
                if (instance == null) {
                    instance = new MiniMessageService();
                }
            }
        }
        return instance;
    }
}
```

## 注意事项

1. **必须加 `volatile`**（Java 5+）：防止指令重排序
   ```java
   private static volatile Singleton instance;
   ```

2. **反射攻击**：可通过私有构造函数检查防止
   ```java
   private Singleton() {
       if (instance != null) {
           throw new IllegalStateException("Instance already exists");
       }
   }
   ```

3. **序列化**：需实现 `readResolve()` 方法

## 替代方案

### 枚举单例（最简洁）

```java
public enum Singleton {
    INSTANCE;
    
    public void doSomething() {
        // ...
    }
}

// 使用
Singleton.INSTANCE.doSomething();
```

### 静态内部类（延迟加载）

```java
public class Singleton {
    private Singleton() {}
    
    private static class Holder {
        static final Singleton INSTANCE = new Singleton();
    }
    
    public static Singleton getInstance() {
        return Holder.INSTANCE;
    }
}
```

## 选择建议

| 场景 | 推荐方案 |
|------|----------|
| 简单单例 | 枚举 |
| 需要延迟加载 | 静态内部类 |
| 需要传递参数 | DCL |
| 高频访问 | DCL 或 静态内部类 |

## 相关文件

- `MiniMessageService.java` - DCL 实现示例

---
*记录时间: 2026-04-14*
*类型: 代码审查优化*
