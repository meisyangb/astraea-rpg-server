# GuangDianCaveFu

> 光点洞府系统 — 玩家私人副本空间，支持创建/升级/邀请/传送，双模式存储(YAML/SQLite)，区域保护，LuckPerms 权限集成

---

## 一、简介

GuangDianCaveFu 为每个玩家提供专属洞府(私人领地)，支持创建、升级、成员管理和传送。洞府拥有独立的虚空世界，通过区域保护确保安全。

### 功能特性

- **洞府创建** — 每个玩家一个洞府，独立虚空世界
- **洞府升级** — 消耗资源提升洞府等级，扩大空间、增加成员上限
- **成员管理** — 邀请/踢出成员、转让所有权
- **主城传送** — 一键回家功能
- **区域保护** — 洞府内防破坏、防 PvP
- **双模式存储** — 自动选择 SQLite 或 YAML 文件存储
- **SQLite 事务** — 数据库模式使用完整事务保护，失败自动回滚
- **脏标记机制** — 仅保存变更数据，避免不必要的 IO
- **同步保存** — 所有数据变更立即写入磁盘
- **LuckPerms 集成** — 洞府世界自动继承 LP 权限

### 前置要求

| 插件 | 说明 | 必装 |
|------|------|:----:|
| RPGCore | 核心框架 | ✅ 是 |
| Multiverse-Core | 多世界管理(洞府世界创建) | ✅ 是 |
| LuckPerms | 权限继承 | ❌ 否 |
| RPGItems | 升级材料支持 | ❌ 否 |
| PlaceholderAPI | 变量扩展 | ❌ 否 |

### 兼容性

- **服务端**: Paper 1.21+
- **Java**: Java 21+

---

## 二、安装

1. 确保 RPGCore 和 Multiverse-Core 已安装
2. 将 `GuangDianCaveFu.jar` 放入 `plugins/`
3. 重启服务器
4. 检查 `plugins/GuangDianCaveFu/config.yml` 配置

---

## 三、命令权限

### 3.1 玩家命令

| 命令 | 权限 | 说明 |
|------|------|------|
| `/cave` | 无 | 打开洞府主菜单 |
| `/cave create` | 无 | 创建洞府 |
| `/cave home` | 无 | 传送到洞府 |
| `/cave upgrade` | 无 | 升级洞府 |
| `/cave info [玩家]` | 无 | 查看洞府信息 |
| `/cave invite <玩家>` | 无(需为洞主) | 邀请玩家加入 |
| `/cave kick <玩家>` | 无(需为洞主) | 踢出成员 |
| `/cave leave` | 无 | 离开洞府(非洞主) |
| `/cave transfer <玩家>` | 无(需为洞主) | 转让所有权 |
| `/cave sethome` | 无(需为洞主) | 设置传送点 |
| `/cave list` | 无 | 查看成员列表 |
| `/cave help` | 无 | 帮助信息 |

### 3.2 管理员命令

| 命令 | 权限 | 说明 |
|------|------|------|
| `/caveadmin setlevel <玩家> <等级>` | `guangdian.cavefu.admin` | 设置洞府等级 |
| `/caveadmin delete <玩家>` | `guangdian.cavefu.admin` | 强制删除洞府 |
| `/caveadmin reload` | `guangdian.cavefu.admin` | 重载配置 |
| `/caveadmin info` | `guangdian.cavefu.admin` | 查看系统统计 |
| `/caveadmin tp <玩家>` | `guangdian.cavefu.admin` | 传送到指定洞府 |

---

## 四、配置文件

### 4.1 config.yml

```yaml
# 存储模式
storage:
  # sqlite 或 yaml
  use-sqlite: true

# 洞府世界设置
world:
  # 世界名称前缀
  prefix: "cave_"
  # 虚空生成器
  generator: "VoidGenerator"

# 升级消耗
levels:
  1:
    size: 50
    max-members: 3
    upgrade-cost: 0
  2:
    size: 70
    max-members: 5
    upgrade-cost: 5000
  3:
    size: 100
    max-members: 8
    upgrade-cost: 50000
```

---

## 五、数据存储

### SQLite 模式(推荐)

```
plugins/GuangDianCaveFu/data.db
├── caves (id, owner_uuid, owner_name, level, home_x, home_y, home_z, ...)
├── members (cave_id, player_uuid, permission_type)
```

- 使用事务写入(autocommit=false)
- 保存失败不清脏标记，下次自动重试
- 成员保存使用临时表 + 恢复机制

### YAML 模式

```
plugins/GuangDianCaveFu/data.yml
```

::: tip 提示
SQLite 模式推荐用于大型服务器，YAML 适合开发调试。
:::

---

## 六、PlaceholderAPI 变量

| 占位符 | 说明 |
|--------|------|
| `%gdcave_level%` | 洞府等级 |
| `%gdcave_members%` | 洞府成员数量 |
| `%gdcave_has_cave%` | 是否有洞府(true/false) |

---

## 七、常见问题

**Q: 洞府世界不生成？**
A: 确保 Multiverse-Core 已安装并正常运行。

**Q: 升级后洞府空间没变大？**
A: 检查 config.yml 中的 levels 配置是否更新。

**Q: SQLite 和 YAML 可以互相切换吗？**
A: 不支持自动迁移，切换后需重新创建洞府。

---

## 八、更新日志

### v1.1.0 (2026-06-13)
- 移除异步保存，全部改为同步直接写入磁盘
- 清理无用 AsyncExecutor 引用

### v1.0.0
- 初始版本：洞府创建/升级/成员管理

---

*最后更新: 2026-06-13*
*维护者: Gumin*
