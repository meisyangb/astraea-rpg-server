# 配置文件

> RPGCore 详细配置说明

## 一、config.yml

主配置文件，包含数据库、缓存、异步执行器等核心配置。

```yaml
# ========================================
# RPGCore 核心配置
# ========================================

# 数据库配置
database:
  # 数据库类型: sqlite 或 mysql
  type: sqlite
  
  # MySQL 配置（仅当 type 为 mysql 时生效）
  mysql:
    host: localhost
    port: 3306
    database: rpgcore
    username: root
    password: password
    
    # 连接池配置
    pool:
      # 最小连接数
      minimum-idle: 5
      # 最大连接数
      maximum-pool-size: 10
      # 连接超时时间（毫秒）
      connection-timeout: 30000
      # 空闲超时时间（毫秒）
      idle-timeout: 600000
      # 最大存活时间（毫秒）
      max-lifetime: 1800000

# 缓存配置
cache:
  # 是否启用缓存
  enabled: true
  
  # 缓存类型: memory, caffeine, redis
  type: caffeine
  
  # 最大缓存条目数
  max-size: 10000
  
  # 默认过期时间（秒）
  expire-seconds: 3600
  
  # Caffeine 配置
  caffeine:
    # 初始容量
    initial-capacity: 100
    # 最大容量
    maximum-size: 10000
    # 写入后过期时间（秒）
    expire-after-write: 3600
    # 访问后过期时间（秒）
    expire-after-access: 1800

# 异步执行器配置
async:
  # 线程池大小
  pool-size: 4
  
  # 队列容量
  queue-capacity: 1000
  
  # 线程名称前缀
  thread-name-prefix: "RPGCore-"

# 日志配置
logging:
  # 日志级别: DEBUG, INFO, WARN, ERROR
  level: INFO
  
  # 是否输出到控制台
  console: true
  
  # 是否输出到文件
  file: true
  
  # 日志文件路径
  file-path: "logs/rpgcore.log"
  
  # 日志保留天数
  retention-days: 30

# 调试模式
debug: false

# 语言配置
language:
  # 语言文件
  locale: zh_CN
```

## 二、logback.xml

日志配置文件，基于 Logback 框架。

```xml
<?xml version="1.0" encoding="UTF-8"?>
<configuration>
    <!-- 控制台输出 -->
    <appender name="CONSOLE" class="ch.qos.logback.core.ConsoleAppender">
        <encoder>
            <pattern>[%d{HH:mm:ss}] [%thread/%level]: %msg%n</pattern>
        </encoder>
    </appender>
    
    <!-- 文件输出 -->
    <appender name="FILE" class="ch.qos.logback.core.rolling.RollingFileAppender">
        <file>logs/rpgcore.log</file>
        <rollingPolicy class="ch.qos.logback.core.rolling.TimeBasedRollingPolicy">
            <fileNamePattern>logs/rpgcore.%d{yyyy-MM-dd}.log</fileNamePattern>
            <maxHistory>30</maxHistory>
        </rollingPolicy>
        <encoder>
            <pattern>[%d{yyyy-MM-dd HH:mm:ss}] [%thread/%level]: %msg%n</pattern>
        </encoder>
    </appender>
    
    <!-- 根日志级别 -->
    <root level="INFO">
        <appender-ref ref="CONSOLE"/>
        <appender-ref ref="FILE"/>
    </root>
    
    <!-- RPGCore 日志 -->
    <logger name="cn.guangdian.rpgcore" level="DEBUG" additivity="false">
        <appender-ref ref="CONSOLE"/>
        <appender-ref ref="FILE"/>
    </logger>
</configuration>
```

## 三、数据库配置详解

### SQLite

SQLite 是默认的数据库类型，无需额外配置。

```yaml
database:
  type: sqlite
```

数据文件位置：`plugins/RPGCore/data.db`

### MySQL

MySQL 适合大型服务器，支持多服务器数据共享。

```yaml
database:
  type: mysql
  mysql:
    host: localhost
    port: 3306
    database: rpgcore
    username: root
    password: password
    pool:
      minimum-idle: 5
      maximum-pool-size: 10
```

#### 创建数据库

```sql
CREATE DATABASE rpgcore CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
CREATE USER 'rpgcore'@'localhost' IDENTIFIED BY 'password';
GRANT ALL PRIVILEGES ON rpgcore.* TO 'rpgcore'@'localhost';
FLUSH PRIVILEGES;
```

## 四、缓存配置详解

### 内存缓存 (Memory)

最简单的缓存实现，适合小型服务器。

```yaml
cache:
  type: memory
  max-size: 10000
  expire-seconds: 3600
```

### Caffeine 缓存

高性能本地缓存，推荐使用。

```yaml
cache:
  type: caffeine
  caffeine:
    initial-capacity: 100
    maximum-size: 10000
    expire-after-write: 3600
    expire-after-access: 1800
```

### Redis 缓存

分布式缓存，适合多服务器集群。

```yaml
cache:
  type: redis
  redis:
    host: localhost
    port: 6379
    password: ""
    database: 0
```

## 五、性能调优

### 数据库连接池

根据服务器规模调整连接池大小：

| 服务器规模 | 最小连接 | 最大连接 |
|------------|----------|----------|
| 小型 (<50人) | 2 | 5 |
| 中型 (50-200人) | 5 | 10 |
| 大型 (>200人) | 10 | 20 |

### 缓存大小

根据内存情况调整缓存大小：

| 内存 | 推荐缓存大小 |
|------|--------------|
| 4GB | 5000 |
| 8GB | 10000 |
| 16GB | 20000 |

### 异步线程池

根据 CPU 核心数调整：

| CPU核心 | 推荐线程数 |
|---------|------------|
| 2核 | 2 |
| 4核 | 4 |
| 8核 | 6-8 |

## 六、配置重载

修改配置后，使用命令重载：

```
/rgc reload
```

或重启服务器。

---

*最后更新: 2026-05-12*
