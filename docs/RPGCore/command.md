# 命令权限

> RPGCore 核心命令与权限说明

## 一、命令

### 主命令

| 命令 | 说明 | 权限 |
|------|------|------|
| `/rpgcore` | 核心命令 | `rpgcore.admin` |
| `/rgc` | 核心命令别名 | `rpgcore.admin` |

### 子命令

| 命令 | 说明 | 权限 |
|------|------|------|
| `/rgc reload` | 重载配置文件 | `rpgcore.reload` |
| `/rgc info` | 显示插件信息 | `rpgcore.info` |
| `/rgc stats` | 显示统计信息 | `rpgcore.admin` |
| `/rgc services` | 列出已注册服务 | `rpgcore.admin` |
| `/rgc modules` | 列出已加载模块 | `rpgcore.admin` |
| `/rgc caches` | 显示缓存状态 | `rpgcore.admin` |
| `/rgc help` | 显示帮助信息 | `rpgcore.admin` |

## 二、权限

### 基础权限

| 权限 | 说明 | 默认 |
|------|------|------|
| `rpgcore.admin` | 管理员权限，包含所有子权限 | OP |
| `rpgcore.reload` | 重载配置权限 | OP |
| `rpgcore.info` | 查看信息权限 | OP |

### 权限继承关系

```
rpgcore.admin
├── rpgcore.reload
├── rpgcore.info
├── rpgcore.stats
├── rpgcore.services
├── rpgcore.modules
└── rpgcore.caches
```

## 三、命令详解

### /rgc reload

重载所有配置文件。

```bash
/rgc reload
```

**输出示例：**
```
[RPGCore] 正在重载配置...
[RPGCore] 数据库配置已重载
[RPGCore] 缓存配置已重载
[RPGCore] 配置重载完成！
```

### /rgc info

显示插件基本信息。

```bash
/rgc info
```

**输出示例：**
```
========== RPGCore 信息 ==========
版本: 1.0.0
作者: GuangDian
服务端: Paper 1.21.6
Java: 21
已注册服务: 5
缓存条目: 128
==================================
```

### /rgc stats

显示运行统计信息。

```bash
/rgc stats
```

**输出示例：**
```
========== RPGCore 统计 ==========
运行时间: 2小时 30分钟
事件处理: 15,234 次
异步任务: 1,024 次
缓存命中: 89.5%
内存使用: 128MB / 512MB
==================================
```

### /rgc services

列出所有已注册的服务。

```bash
/rgc services
```

**输出示例：**
```
========== 已注册服务 ==========
1. ServiceRegistry
2. EventBus
3. CacheProvider
4. AsyncExecutor
5. GameLogger
==================================
```

### /rgc caches

显示缓存状态。

```bash
/rgc caches
```

**输出示例：**
```
========== 缓存状态 ==========
缓存名称          大小    命中率
player_data       128     95.2%
item_cache        256     89.5%
location_cache    64      92.1%
==================================
```

## 四、PlaceholderAPI 变量

RPGCore 提供以下 PAPI 变量：

| 变量 | 说明 |
|------|------|
| `%rpgcore_version%` | 插件版本 |
| `%rpgcore_uptime%` | 运行时间 |
| `%rpgcore_players%` | 在线玩家数 |
| `%rpgcore_tps%` | 服务器TPS |

## 五、配置权限节点

在权限插件中配置：

```yaml
# LuckPerms 配置示例
groups:
  admin:
    permissions:
      - rpgcore.admin
  mod:
    permissions:
      - rpgcore.info
      - rpgcore.stats
```

---

*最后更新: 2026-05-12*
