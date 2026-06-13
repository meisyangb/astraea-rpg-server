# GuangDianForge 命令使用指南

## 📋 目录
- [玩家命令](#玩家命令)
- [管理员命令](#管理员命令)
- [权限说明](#权限说明)
- [使用示例](#使用示例)

---

## 🎮 玩家命令

### `/forge` - 显示帮助
显示所有可用的锻造命令。

**用法**: `/forge`  
**权限**: `guangdian.forge.use`  
**示例**:
```
/forge
```

---

### `/forge open` - 打开锻造界面
打开锻造系统界面，可以查看所有已学图纸并进行锻造。

**用法**: `/forge open [图纸ID]`  
**权限**: `guangdian.forge.use`  
**示例**:
```
/forge open                    # 打开图纸选择界面
/forge open forger_sword       # 直接打开指定图纸的锻造界面
```

---

### `/forge learn` - 学习图纸
打开图纸学习界面或学习指定图纸。

**用法**: `/forge learn [图纸ID]`  
**权限**: `guangdian.forge.learn`  
**示例**:
```
/forge learn                   # 打开图纸学习界面
```

---

### `/forge info` - 查看锻造信息
查看自己或其他玩家的锻造等级、经验、成功率等信息。

**用法**: `/forge info [玩家名]`  
**权限**: 
- 查看自己: `guangdian.forge.info`
- 查看他人: `guangdian.forge.admin`

**示例**:
```
/forge info                    # 查看自己的信息
/forge info PlayerName         # 查看指定玩家的信息（需要管理员权限）
```

**输出示例**:
```

═══ 锻造信息 ═══

  锻造等级: 5
  锻造经验: 1200
  升级还需: 800
  
  总锻造次数: 25
  成功次数: 20
  成功率: 80.0%
  已学图纸: 8

═══════════════

```

---

### `/forge list` - 查看图纸列表
查看已学、未学或所有图纸。

**用法**: `/forge list [all|learned|unlearned] [等级]`  
**权限**: `guangdian.forge.list`  
**示例**:
```
/forge list                    # 查看已学图纸（默认）
/forge list all                # 查看所有图纸
/forge list learned            # 查看已学图纸
/forge list unlearned          # 查看未学图纸
/forge list all 2              # 查看2级所有图纸
```

**输出示例**:
```

═══ 已学图纸 ═══

  ● 锻造师·黎明 [Lv.1]
  ● 锻造师·守誓 [Lv.1]
  ● 圣树·艾弗洛 [Lv.5]
  ● 暗焰·噬魂 [Lv.10]
  
  总计: 4 张图纸

═══════════════

```

---

## 👑 管理员命令

### `/forgeadmin` - 显示管理帮助
显示所有可用的管理命令。

**用法**: `/forgeadmin`  
**权限**: `guangdian.forge.admin`  
**示例**:
```
/forgeadmin
```

---

### `/forgeadmin give` - 给予图纸
给予玩家图纸物品。

**用法**: `/forgeadmin give <玩家> <图纸ID> [数量]`  
**权限**: `guangdian.forge.admin.give`  
**示例**:
```
/forgeadmin give Player1 forger_sword           # 给予1张图纸
/forgeadmin give Player1 forger_sword 5         # 给予5张图纸
/forgeadmin give Player1 all                    # 给予所有图纸（TODO）
```

---

### `/forgeadmin setlevel` - 设置锻造等级
设置玩家的锻造等级。

**用法**: `/forgeadmin setlevel <玩家> <等级>`  
**权限**: `guangdian.forge.admin.setlevel`  
**示例**:
```
/forgeadmin setlevel Player1 10      # 设置为10级
/forgeadmin setlevel Player1 max     # 设置为最高等级
```

---

### `/forgeadmin addexp` - 添加锻造经验
给玩家添加锻造经验。

**用法**: `/forgeadmin addexp <玩家> <经验值>`  
**权限**: `guangdian.forge.admin.addexp`  
**示例**:
```
/forgeadmin addexp Player1 1000      # 添加1000经验
```

---

### `/forgeadmin reset` - 重置玩家数据
重置玩家的锻造数据到初始状态。

**用法**: `/forgeadmin reset <玩家>`  
**权限**: `guangdian.forge.admin.reset`  
**示例**:
```
/forgeadmin reset Player1            # 重置为初始状态
```

---

### `/forgeadmin reload` - 重载配置
重载配置文件和图纸配置。

**用法**: `/forgeadmin reload`  
**权限**: `guangdian.forge.admin.reload`  
**示例**:
```
/forgeadmin reload                   # 重载配置
```

---

### `/forgeadmin stats` - 查看统计信息
查看服务器锻造系统的统计信息。

**用法**: `/forgeadmin stats`  
**权限**: `guangdian.forge.admin.stats`  
**示例**:
```
/forgeadmin stats                    # 显示统计数据
```

---

## 🔐 权限说明

### 玩家权限
| 权限节点 | 描述 | 默认 |
|---------|------|------|
| `guangdian.forge.use` | 使用锻造系统 | true |
| `guangdian.forge.learn` | 学习图纸 | true |
| `guangdian.forge.info` | 查看自己信息 | true |
| `guangdian.forge.list` | 查看图纸列表 | true |

### 管理员权限
| 权限节点 | 描述 | 父权限 |
|---------|------|--------|
| `guangdian.forge.admin` | 管理员总权限 | op |
| `guangdian.forge.admin.give` | 给予图纸 | admin |
| `guangdian.forge.admin.setlevel` | 设置等级 | admin |
| `guangdian.forge.admin.addexp` | 添加经验 | admin |
| `guangdian.forge.admin.reset` | 重置数据 | admin |
| `guangdian.forge.admin.reload` | 重载配置 | admin |
| `guangdian.forge.admin.stats` | 查看统计 | admin |

---

## 💡 使用技巧

### ✨ Tab 自动补全（重要！）

所有命令都支持 **Tab 键自动补全**，大幅提升操作效率！

#### `/forge` 命令补全
```bash
# 子命令补全
/forge <Tab>
→ open, learn, info, list, help

# 图纸ID补全（已学图纸）
/forge open <Tab>
→ forger_sword, forger_chestplate, holy_tree_sword...

# 图纸ID补全（未学图纸）
/forge learn <Tab>
→ demon_sword, elf_sword, fallen_sword...

# 玩家名补全（需要管理员权限）
/forge info <Tab>
→ Player1, Player2, Admin...

# 筛选选项补全
/forge list <Tab>
→ all, learned, unlearned
```

#### `/forgeadmin` 命令补全
```bash
# 子命令补全
/forgeadmin <Tab>
→ give, setlevel, addexp, reset, reload, stats, help

# 玩家名补全
/forgeadmin give <Tab>
→ Player1, Player2, Admin...

# 图纸ID补全（所有图纸）
/forgeadmin give Player1 <Tab>
→ forger_sword, forger_chestplate, holy_tree_sword, demon_sword...

# 数量补全（1-64）
/forgeadmin give Player1 forger_sword <Tab>
→ 1, 2, 3... 64

# 等级补全（包括max）
/forgeadmin setlevel Player1 <Tab>
→ 0, 1, 2, 3, 4, max
```

### 快捷命令
- `/forge i` = `/forge info`
- `/forge l` = `/forge list`
- `/forge ?` = `/forge help`

### Tab 补全
输入命令时按 Tab 键可以自动补全：
```
/forge o<Tab>  →  /forge open
/forge l<Tab>  →  /forge list
```

### 常见场景

#### 场景1: 新手开始锻造
```bash
1. /forge learn          # 学习基础图纸
2. /forge open           # 打开锻造界面
3. 选择图纸进行锻造
```

#### 场景2: 查看进度
```bash
/forge info              # 查看当前等级和经验
/forge list unlearned    # 查看还有哪些图纸可以学习
```

#### 场景3: 管理员批量操作
```bash
/forgeadmin setlevel Player1 10    # 快速提升等级
/forgeadmin give Player1 all       # 给予所有图纸
/forgeadmin reload                 # 更新配置后重载
```

---

## ❓ 常见问题

### Q: 为什么我无法使用 `/forge` 命令？
A: 请确保你有 `guangdian.forge.use` 权限。默认情况下所有玩家都有此权限。

### Q: 如何查看某个玩家的锻造等级？
A: 使用 `/forge info 玩家名`（需要管理员权限）。

### Q: 图纸学习后如何锻造？
A: 使用 `/forge open` 打开锻造界面，选择已学习的图纸进行锻造。

### Q: 如何重置某个玩家的数据？
A: 使用 `/forgeadmin reset 玩家名`。

---

## 📝 更新日志

### v1.0.0 (2026-05-20)
- ✨ 重新设计命令系统
- ✨ 分离玩家和管理员命令
- ✨ 添加完整的帮助系统
- ✨ 支持查看他人信息
- ✨ 支持图纸列表筛选
- ✨ 添加管理员管理功能
- 🗑️ 移除旧的 `/forgegive` 命令

---

**插件版本**: 1.0.0  
**最后更新**: 2026-05-20
