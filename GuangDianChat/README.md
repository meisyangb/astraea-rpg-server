# 光点聊天插件 - GuangDianChat

<div align="center">

![Version](https://img.shields.io/badge/版本-1.0.0-brightgreen)
![Minecraft](https://img.shields.io/badge/Minecraft-1.21%2B-blue)
![Java](https://img.shields.io/badge/Java-21-orange)
![Author](https://img.shields.io/badge/作者-Gumin-red)

**一个功能强大的 Minecraft 聊天格式插件**

[功能特性](#功能特性) • [安装说明](#安装说明) • [配置指南](#配置指南) • [占位符列表](#占位符列表) • [权限列表](#权限列表)

</div>

---

## 📖 简介

光点聊天插件（GuangDianChat）是一个专为 Minecraft 服务器设计的聊天格式插件，支持 PlaceholderAPI 占位符和 LuckPerms 权限组，让您可以轻松自定义服务器的聊天格式。

### 作者信息
- **作者**: Gumin
- **QQ**: 2271257344
- **版本**: 1.0.0

---

## ✨ 功能特性

- ✅ **自定义聊天格式** - 完全自定义聊天消息的显示格式
- ✅ **PlaceholderAPI 支持** - 支持所有 PAPI 占位符
- ✅ **LuckPerms 集成** - 根据权限组显示不同的聊天格式
- ✅ **世界别名** - 将世界名称转换为友好的显示名称
- ✅ **颜色代码支持** - 支持玩家在聊天中使用颜色代码
- ✅ **加入/退出消息** - 自定义玩家加入和退出服务器的消息
- ✅ **权限控制** - 细粒度的权限控制

---

## 📥 安装说明

### 前置要求

| 插件 | 必需 | 说明 |
|------|------|------|
| PlaceholderAPI | ✅ 必需 | 提供占位符支持 |
| LuckPerms | ⭕ 可选 | 提供权限组支持 |
| Vault | ⭕ 可选 | 提供经济支持 |

### 安装步骤

1. 下载插件 JAR 文件
2. 将文件放入服务器的 `plugins` 文件夹
3. 重启服务器或使用插件管理器加载
4. 编辑 `plugins/GuangDianChat/config.yml` 配置文件
5. 使用 `/gdchat reload` 重载配置

---

## ⚙️ 配置指南

### 基础配置

```yaml
# 是否启用聊天格式功能
enabled: true

# 默认聊天格式
default-format: "&7[&b%player_world%&7] &7&l[&f&l玩家&7&l]&r &eLv.%player_level% &f%player_name%&7: &f%message%"
```

### 权限组格式配置

```yaml
group-formats:
  owner: "&7[&b%player_world%&7] &6&l[&e&l服主&6&l]&r &eLv.%player_level% &6%player_name%&7: &f%message%"
  admin: "&7[&b%player_world%&7] &c&l[&4&l管理员&c&l]&r &eLv.%player_level% &c%player_name%&7: &f%message%"
  vip: "&7[&b%player_world%&7] &e&l[&6&lVIP&e&l]&r &eLv.%player_level% &e%player_name%&7: &f%message%"
```

### 世界别名配置

```yaml
world-aliases:
  world: "主世界"
  world_nether: "下界"
  world_the_end: "末地"
```

---

## 📝 占位符列表

### 玩家信息

| 占位符 | 说明 |
|--------|------|
| `%player_name%` | 玩家名称 |
| `%player_displayname%` | 玩家显示名 |
| `%player_world%` | 当前世界 |
| `%player_level%` | 玩家等级 |
| `%player_exp%` | 玩家经验值 |
| `%player_health%` | 玩家生命值 |
| `%player_max_health%` | 玩家最大生命值 |
| `%player_food_level%` | 玩家饥饿值 |
| `%player_x%` / `%player_y%` / `%player_z%` | 玩家坐标 |
| `%player_ping%` | 玩家延迟 |

### 权限组信息

| 占位符 | 说明 |
|--------|------|
| `%luckperms_prefix%` | LuckPerms 前缀 |
| `%luckperms_suffix%` | LuckPerms 后缀 |
| `%luckperms_primary_group_name%` | 主权限组名称 |

### 服务器信息

| 占位符 | 说明 |
|--------|------|
| `%server_online%` | 在线玩家数 |
| `%server_max_players%` | 最大玩家数 |
| `%server_tps%` | 服务器 TPS |

### 经济信息

| 占位符 | 说明 |
|--------|------|
| `%vault_eco_balance%` | 经济余额 |
| `%vault_eco_balance_formatted%` | 格式化的经济余额 |

### 其他

| 占位符 | 说明 |
|--------|------|
| `%message%` | 聊天消息内容 |

---

## 🔐 权限列表

| 权限节点 | 说明 | 默认 |
|----------|------|------|
| `guangdian.chat.admin` | 管理员权限 | OP |
| `guangdian.chat.color` | 使用颜色代码 | OP |
| `guangdian.chat.format` | 使用格式代码 | OP |
| `guangdian.chat.magic` | 使用魔法字符 | OP |
| `guangdian.chat.url` | 发送链接 | OP |

---

## 💬 命令列表

| 命令 | 说明 | 权限 |
|------|------|------|
| `/gdchat` | 显示帮助信息 | - |
| `/gdchat reload` | 重新加载配置 | `guangdian.chat.admin` |
| `/gdchat info` | 显示插件信息 | `guangdian.chat.admin` |
| `/gdchat help` | 显示帮助信息 | - |

---

## 🎨 颜色代码

| 代码 | 颜色 | 代码 | 颜色 |
|------|------|------|------|
| `&0` | 黑色 | `&8` | 深灰色 |
| `&1` | 深蓝色 | `&9` | 蓝色 |
| `&2` | 深绿色 | `&a` | 绿色 |
| `&3` | 深青色 | `&b` | 青色 |
| `&4` | 深红色 | `&c` | 红色 |
| `&5` | 深紫色 | `&d` | 粉色 |
| `&6` | 金色 | `&e` | 黄色 |
| `&7` | 灰色 | `&f` | 白色 |

### 格式代码

| 代码 | 效果 |
|------|------|
| `&k` | 随机字符（魔法效果） |
| `&l` | 粗体 |
| `&m` | 删除线 |
| `&n` | 下划线 |
| `&o` | 斜体 |
| `&r` | 重置 |

---

## 📋 格式示例

### 简洁格式
```
%luckperms_prefix% %player_name%&7: &f%message%
```

### 带世界格式
```
&7[&b%player_world%&7] %luckperms_prefix% %player_name%&7: &f%message%
```

### 带等级格式
```
&7[&b%player_world%&7] &eLv.%player_level% %luckperms_prefix% %player_name%&7: &f%message%
```

### 完整信息格式
```
&7[&b%player_world%&7] &c%player_health%❤ &7| &eLv.%player_level% %luckperms_prefix% %player_name%&7: &f%message%
```

---

## 📞 联系方式

如有问题或建议，请联系：
- **QQ**: 2271257344
- **作者**: Gumin

---

## 📜 开源协议

本插件仅供学习交流使用，请勿用于商业用途。

---

<div align="center">

**光点聊天插件** © 2024 - Gumin

</div>
