# Astraea RPG 插件文档

> 光点RPG服务器专属插件体系 · 为 Minecraft Paper 1.21+ 打造的高性能RPG插件解决方案

[![Version](https://img.shields.io/badge/版本-1.0.0-blue)](https://github.com)
[![Minecraft](https://img.shields.io/badge/Minecraft-Paper%201.21+-green)](https://papermc.io)
[![Java](https://img.shields.io/badge/Java-21+-orange)](https://www.oracle.com/java)
[![License](https://img.shields.io/badge/协议-专有-black)](https://guangdian.cn)

---

## 📖 简介

**Astraea RPG**（阿斯特瑞亚）是一套完整的 Minecraft RPG 服务器插件体系，专为 Paper 1.21+ 服务器设计。所有插件均采用现代化架构，支持高性能异步处理、完整的API接口和丰富的自定义配置。

### ✨ 核心特点

- 🚀 **高性能设计** - 基于异步处理和缓存优化，支持大规模玩家在线
- 🔧 **模块化架构** - 插件间松耦合，可按需安装使用
- 🔌 **完整API** - 提供丰富的开发者接口，方便扩展和集成
- ⚙️ **高度可配置** - 几乎所有功能都支持自定义配置
- 🇨🇳 **中文友好** - 完整的中文支持和文档

---

## 🏗️ 插件体系

### 核心框架
| 插件 | 说明 | 必装 |
|------|------|:----:|
| [RPGCore](/RPGCore/README) | 核心框架，提供服务注册、事件总线、缓存管理 | ✅ |

### 属性与职业
| 插件 | 说明 |
|------|------|
| [GuangDianArmorStats](/GuangDianArmorStats/README) | RPG装备属性，伤害计算、Boss属性 |
| [GuangDianClass](/GuangDianClass/README) | 职业系统，阶位、转职、属性成长 |
| [GuangDianAccessory](/GuangDianAccessory/README) | 饰品系统，徽章、勋章、圣物 |
| [GuangDianGearScore](/GuangDianGearScore/README) | 装备评分系统 |

### 怪物与战斗
| 插件 | 说明 |
|------|------|
| [GuangDianMobs](/GuangDianMobs/README) | 自定义怪物系统 |
| [GuangDianDungeon](/GuangDianDungeon/README) | 副本系统，多楼层Boss战 |
| [GuangDianCombat](/GuangDianCombat/README) | 战斗状态管理，PVP/PVE控制 |
| [GuangDianMobHealth](/GuangDianMobHealth/README) | 怪物血量显示 |
| [GuangDianAggro](/GuangDianAggro/README) | 仇恨管理系统 |
| [GuangDianRaid](/GuangDianRaid/README) | 战术副本系统 |
| [GuangDianRegen](/GuangDianRegen/README) | 生命恢复系统 |
| [GuangDianKillAura](/GuangDianKillAura/README) | 杀戮光环检测 |

### 任务系统
| 插件 | 说明 |
|------|------|
| [GuangDianQuest](/GuangDianQuest/README) | 任务系统，主线/支线/每日 |
| [GuangDianBattlePass](/GuangDianBattlePass/README) | 战令系统 |

### 社交系统
| 插件 | 说明 |
|------|------|
| [GuangDianGuild](/GuangDianGuild/README) | 公会系统 |
| [GuangDianMarriage](/GuangDianMarriage/README) | 结婚系统 |
| [GuangDianTrade](/GuangDianTrade/README) | 玩家交易系统 |

### 经济系统
| 插件 | 说明 |
|------|------|
| [GuangDianBank](/GuangDianBank/README) | 银行系统，存取款、利息 |
| [GuangDianMarket](/GuangDianMarket/README) | 全球市场/拍卖行 |
| [GuangDianPoints](/GuangDianPoints/README) | 点券系统 |
| [GuangDianLottery](/GuangDianLottery/README) | 抽奖系统 |

### 装备系统
| 插件 | 说明 |
|------|------|
| [GuangDianForge](/GuangDianForge/README) | 锻造系统，图纸学习、锻造等级 |
| [GuangDianDecompose](/GuangDianDecompose/README) | 装备分解系统 |
| [GuangDianSocket](/GuangDianSocket/README) | 宝石镶嵌系统 |
| [GuangDianEnhance](/GuangDianEnhance/README) | 装备强化系统 |
| [GuangDianDevour](/GuangDianDevour/README) | 装备吞噬升级 |
| [GuangDianSoulBag](/GuangDianSoulBag/README) | 灵魂背包，死亡不掉落 |
| [GuangDianDragonCore](/GuangDianDragonCore/README) | 龙核系统 |
| [GuangDianCollection](/GuangDianCollection/README) | 图鉴收集系统 |
| [GuangDianSoulBind](/GuangDianSoulBind/README) | 灵魂绑定系统 |

### 世界管理
| 插件 | 说明 |
|------|------|
| [GuangDianWorld](/GuangDianWorld/README) | 世界管理，替代Multiverse |
| [GuangDianCaveFu](/GuangDianCaveFu/README) | 空岛洞府系统 |
| [GuangDianWorldRules](/GuangDianWorldRules/README) | 世界规则管理 |
| [GuangDianPortal](/GuangDianPortal/README) | 传送门系统 |

### 签到奖励
| 插件 | 说明 |
|------|------|
| [GuangDianSignIn](/GuangDianSignIn/README) | 签到系统 |
| [GuangDianMonthlyCard](/GuangDianMonthlyCard/README) | 月卡系统 |
| [GuangDianGift](/GuangDianGift/README) | 礼包系统 |

### 界面显示
| 插件 | 说明 |
|------|------|
| [GuangDianMenu](/GuangDianMenu/README) | GUI菜单系统 |
| [GuangDianHolo](/GuangDianHolo/README) | 全息显示 |
| [GuangDianBoard](/GuangDianBoard/README) | 侧边栏计分板 |
| [GuangDianTab](/GuangDianTab/README) | Tab列表显示 |
| [GuangDianChat](/GuangDianChat/README) | 聊天格式 |
| [GuangDianName](/GuangDianName/README) | 玩家名称显示 |
| [GuangDianItemLabel](/GuangDianItemLabel/README) | 物品标签 |
| [GuangDianDynamicView](/GuangDianDynamicView/README) | 动态视距 |
| [GuangDianParticleBlocker](/GuangDianParticleBlocker/README) | 粒子屏蔽 |

### 功能辅助
| 插件 | 说明 |
|------|------|
| [GuangDianNPC](/GuangDianNPC/README) | NPC管理系统 |
| [GuangDianNPCCommand](/GuangDianNPCCommand/README) | NPC命令绑定 |
| [GuangDianItemTrigger](/GuangDianItemTrigger/README) | 物品触发（右键兑换等） |
| [GuangDianSignCommand](/GuangDianSignCommand/README) | 牌子命令 |
| [GuangDianSignMenu](/GuangDianSignMenu/README) | 牌子菜单 |
| [GuangDianLocation](/GuangDianLocation/README) | 定位系统 |
| [GuangDianVillagerTrade](/GuangDianVillagerTrade/README) | 村民交易 |
| [GuangDianMusic](/GuangDianMusic/README) | 音乐系统 |
| [GuangDianDropControl](/GuangDianDropControl/README) | 丢弃控制 |
| [GuangDianBlockDrop](/GuangDianBlockDrop/README) | 方块掉落 |

### 管理工具
| 插件 | 说明 |
|------|------|
| [GuangDianAuth](/GuangDianAuth/README) | 登录验证 |
| [GuangDianCleaner](/GuangDianCleaner/README) | 扫地娘 |
| [GuangDianChain](/GuangDianChain/README) | 连锁采集 |
| [GuangDianMCP](/GuangDianMCP/README) | MCP服务端桥接 |
| [GuangDianExpControl](/GuangDianExpControl/README) | 经验控制 |

### 扩展系统
| 插件 | 说明 |
|------|------|
| [RPGItems](/RPGItems/README) | RPG自定义物品 |
| [RPGSkill](/RPGSkill/README) | RPG技能系统 | |
| [GuangDianLocation](/GuangDianLocation/README) | 坐标点传送系统 |
| [GuangDianItemTrigger](/GuangDianItemTrigger/README) | 物品触发系统 |
| [GuangDianVillagerTrade](/GuangDianVillagerTrade/README) | 村民兑换系统 |

### 管理工具

| 插件 | 说明 |
|------|------|
| [GuangDianAuth](/GuangDianAuth/README) | 登录验证系统 |
| [GuangDianCleaner](/GuangDianCleaner/README) | 地面掉落物清理 |
| [GuangDianDropControl](/GuangDianDropControl/README) | 物品丢弃控制 |
| [GuangDianChain](/GuangDianChain/README) | 连锁挖矿/砍伐 |
| [GuangDianMCP](/GuangDianMCP/README) | MCP服务器管理，支持AI管理 |

### 扩展系统

| 插件 | 说明 |
|------|------|
| [RPGItems](/RPGItems/README) | 自定义物品系统 |
| [RPGSkill](/RPGSkill/README) | 技能系统 |

---

## 🚀 快速开始

### 第一步：安装核心

1. 下载并安装 [Paper 1.21+](https://papermc.io/downloads) 服务端
2. 下载 [PlaceholderAPI](https://www.spigotmc.org/resources/6245/) 并放入 `plugins` 文件夹
3. 下载 `RPGCore.jar` 并放入 `plugins` 文件夹
4. 启动服务器

### 第二步：安装功能插件

根据服务器需求，选择安装其他插件。查看 [快速开始指南](/quickstart) 获取详细说明。

### 第三步：配置插件

每个插件都有独立的配置文件夹，位于 `plugins/插件名/` 目录下。

```bash
plugins/
├── RPGCore.jar              # 核心（必装）
├── GuangDianArmorStats.jar  # 属性系统
├── GuangDianClass.jar       # 职业系统
├── GuangDianMobs.jar        # 怪物系统
└── ...
```

---

## 🔗 前置依赖

### 必需依赖

| 插件 | 说明 | 下载 |
|------|------|------|
| PlaceholderAPI | 变量支持 | [SpigotMC](https://www.spigotmc.org/resources/6245/) |

### 可选依赖

| 插件 | 说明 | 下载 |
|------|------|------|
| Vault | 经济支持 | [SpigotMC](https://www.spigotmc.org/resources/34315/) |
| LuckPerms | 权限管理 | [SpigotMC](https://www.spigotmc.org/resources/28140/) |
| MythicMobs | 怪物支持 | [SpigotMC](https://www.spigotmc.org/resources/5702/) |
| Citizens | NPC支持 | [SpigotMC](https://www.spigotmc.org/resources/13811/) |

---

## 💻 系统要求

| 项目 | 最低要求 | 推荐配置 |
|------|----------|----------|
| 服务端 | Paper 1.21 | Paper 1.21.6 |
| Java | Java 21 | Java 21 LTS |
| 内存 | 4GB | 8GB+ |
| CPU | 2核 | 4核+ |
| 数据库 | SQLite (默认) | MySQL (可选) |

---

## 📚 文档导航

- 📖 [快速开始](/quickstart) - 10分钟搭建RPG服务器
- 🔧 [RPGCore 文档](/RPGCore/README) - 核心框架说明
- 📋 [文档编写规范](/DOCUMENTATION_RULES) - 贡献指南

---

## 🆘 技术支持

- **官网**: [https://guangdian.store](https://guangdian.store)
- **作者**: GuangDian / Gumin
- **QQ号**: 2271257344
- **GitHub**: 待补充

---

## 📝 更新日志

### v1.0.0 (2026-06-11)
- 🎉 初始版本发布
- 📚 完成基础文档结构
- ✨ 优化文档样式和用户体验

---

*最后更新: 2026-06-11*
