# 快速开始

> 本指南将帮助你在 10 分钟内搭建一个完整的 RPG 服务器

---

## 📋 目录

- [一、环境准备](#一环境准备)
- [二、安装核心插件](#二安装核心插件)
- [三、安装功能插件](#三安装功能插件)
- [四、基础配置](#四基础配置)
- [五、验证安装](#五验证安装)

---

## 一、环境准备

### 1.1 系统要求

| 项目 | 最低要求 | 推荐配置 |
|------|----------|----------|
| 服务端 | Paper 1.21 | Paper 1.21.6 |
| Java | Java 21 | Java 21 LTS |
| 内存 | 4GB | 8GB+ |
| CPU | 2核 | 4核+ |
| 硬盘 | 10GB | 20GB+ |

### 1.2 下载服务端

推荐使用 **Paper** 服务端，它提供了更好的性能和插件兼容性。

::: tip 下载 Paper
访问 [PaperMC 官网](https://papermc.io/downloads) 下载最新版本的 Paper 1.21+
:::

```bash
# 下载 Paper 1.21.6
wget https://api.papermc.io/v2/projects/paper/versions/1.21.6/builds/xxx/downloads/paper-1.21.6-xxx.jar

# 重命名为 paper.jar
mv paper-1.21.6-xxx.jar paper.jar
```

### 1.3 启动服务器

创建启动脚本 `start.sh`（Linux/Mac）或 `start.bat`（Windows）：

```bash
# Linux/Mac
java -Xms4G -Xmx4G -XX:+UseG1GC -jar paper.jar nogui

# Windows
java -Xms4G -Xmx4G -XX:+UseG1GC -jar paper.jar nogui
```

::: warning 注意
首次启动会生成配置文件，你需要同意 EULA。编辑 `eula.txt` 并将 `eula=false` 改为 `eula=true`
:::

---

## 二、安装核心插件

### 2.1 安装 PlaceholderAPI

PlaceholderAPI 是必需的前置插件，许多插件都依赖它提供的变量功能。

**安装步骤：**

1. 下载 [PlaceholderAPI](https://www.spigotmc.org/resources/6245/)
2. 将 `PlaceholderAPI.jar` 放入 `plugins` 文件夹
3. 重启服务器
4. 运行命令 `/papi parse me server_online` 验证安装

### 2.2 安装 RPGCore

RPGCore 是所有光点插件的核心依赖，必须首先安装。

**安装步骤：**

1. 将 `RPGCore.jar` 放入 `plugins` 文件夹
2. 启动服务器，等待配置生成
3. 检查 `plugins/RPGCore/config.yml` 确认配置

```bash
plugins/
├── PlaceholderAPI.jar    # 前置依赖
└── RPGCore.jar           # 核心框架
```

### 2.3 验证安装

使用以下命令验证核心插件是否安装成功：

```bash
/rgc info          # 查看 RPGCore 信息
/rgc version       # 查看版本信息
```

::: tip 成功标志
如果显示 RPGCore 的版本和状态信息，说明安装成功！
:::

---

## 三、安装功能插件

### 3.1 基础RPG功能

推荐安装以下插件构建基础RPG体验：

```bash
plugins/
├── RPGCore.jar              # 核心
├── GuangDianArmorStats.jar  # 属性系统
├── GuangDianClass.jar       # 职业系统
├── GuangDianMobs.jar        # 怪物系统
├── GuangDianMenu.jar        # 菜单系统
└── GuangDianBoard.jar       # 侧边栏
```

**功能说明：**

- **GuangDianArmorStats** - 提供装备属性系统
- **GuangDianClass** - 提供职业和技能系统
- **GuangDianMobs** - 提供自定义怪物功能
- **GuangDianMenu** - 提供GUI菜单功能
- **GuangDianBoard** - 提供侧边栏信息显示

### 3.2 社交与经济系统

```bash
plugins/
├── GuangDianGuild.jar       # 工会系统
├── GuangDianTrade.jar       # 交易系统
├── GuangDianBank.jar        # 银行系统
├── GuangDianMarket.jar      # 市场系统
└── GuangDianPoints.jar      # 点卷系统
```

### 3.3 进阶功能

```bash
plugins/
├── GuangDianQuest.jar       # 任务系统
├── GuangDianRaid.jar        # 副本系统
├── GuangDianForge.jar       # 锻造系统
├── GuangDianSocket.jar      # 宝石系统
└── GuangDianBattlePass.jar  # 战令系统
```

---

## 四、基础配置

### 4.1 配置属性系统

编辑 `plugins/GuangDianArmorStats/attributes.yml`：

```yaml
# 属性配置文件
# 最后更新: 2026-06-11

attributes:
  strength:
    display: "§c攻击力"
    description: "增加物理伤害"
    default: 0
    max: 1000
  
  defense:
    display: "§9防御力"
    description: "减少受到的伤害"
    default: 0
    max: 500
  
  health:
    display: "§a生命值"
    description: "增加最大生命值"
    default: 0
    max: 2000
```

### 4.2 配置职业系统

编辑 `plugins/GuangDianClass/classes.yml`：

```yaml
# 职业配置文件

classes:
  warrior:
    name: "§c战士"
    description: "近战物理输出"
    base_health: 100
    base_attack: 10
    skills:
      - "slash"
      - "shield_bash"
  
  mage:
    name: "§b法师"
    description: "远程魔法输出"
    base_health: 60
    base_attack: 15
    skills:
      - "fireball"
      - "ice_spike"
```

### 4.3 配置怪物系统

编辑 `plugins/GuangDianMobs/mobs.yml`：

```yaml
# 自定义怪物配置

mobs:
  custom_zombie:
    type: ZOMBIE
    name: "§c强化僵尸"
    health: 100
    damage: 10
    drops:
      - "exp:50"
      - "gold:10-20"
```

---

## 五、创建刷新点

### 5.1 设置刷新点

使用以下命令创建怪物刷新点：

```bash
/gdmsp create <刷新点ID>     # 创建刷新点
/gdmsp set <ID> mob <怪物ID>  # 设置怪物类型
/gdmsp set <ID> amount <数量>  # 设置刷新数量
/gdmsp set <ID> radius <范围>  # 设置刷新范围
/gdmsp enable <ID>            # 启用刷新点
```

### 5.2 查看刷新点

```bash
/gdmsp list                   # 列出所有刷新点
/gdmsp info <刷新点ID>        # 查看刷新点详情
/gdmsp remove <刷新点ID>      # 删除刷新点
```

---

## 六、创建任务

### 6.1 创建任务配置

编辑 `plugins/GuangDianQuest/quests/main/example.yml`：

```yaml
# 任务配置文件

id: "example_quest"
name: "§e初出茅庐"
description: "击杀10只僵尸"
type: "kill"
target: "ZOMBIE"
amount: 10

rewards:
  exp: 100
  gold: 50
  items:
    - "iron_sword:1"
```

### 6.2 任务命令

```bash
/quest list                   # 查看可用任务
/quest start <任务ID>         # 开始任务
/quest quit                   # 放弃任务
/quest status                 # 查看任务进度
```

---

## 七、常用命令速查

### 玩家命令

| 命令 | 说明 |
|------|------|
| `/menu` | 打开主菜单 |
| `/class` | 职业系统 |
| `/quest` | 任务系统 |
| `/guild` | 工会系统 |
| `/bank` | 银行系统 |
| `/market` | 全球市场 |
| `/signin` | 每日签到 |

### 管理员命令

| 命令 | 权限 | 说明 |
|------|------|------|
| `/rgc reload` | `rpgcore.admin` | 重载配置 |
| `/gdas reload` | `guangdianarmorstats.admin` | 重载属性配置 |
| `/gdclass reload` | `guangdianclass.admin` | 重载职业配置 |

---

## 八、下一步

恭喜！你已经成功搭建了一个基础的RPG服务器。接下来可以：

- 📖 阅读 [RPGCore 文档](/RPGCore/README) 了解核心功能
- ⚔️ 阅读 [GuangDianArmorStats 文档](/GuangDianArmorStats/README) 配置属性系统
- 🎭 阅读 [GuangDianClass 文档](/GuangDianClass/README) 配置职业系统
- 👾 阅读 [GuangDianMobs 文档](/GuangDianMobs/README) 配置怪物系统
- 📝 查看 [文档编写规范](/DOCUMENTATION_RULES) 学习如何贡献文档

---

## 🆘 常见问题

### Q: 服务器启动后插件没有加载？

**A:** 检查以下内容：
1. 确保使用 Paper 1.21+ 服务端
2. 确保 Java 版本为 21+
3. 检查控制台是否有错误信息

### Q: 如何更新插件？

**A:**  Simply replace the old .jar file with the new one and restart the server.

### Q: 配置修改后不生效？

**A:**  Most plugins require a reload or restart to apply configuration changes. Use `/plugin reload` or restart the server.

---

*最后更新: 2026-06-11*
