# Astraea RPG Server

> 基于 Paper 1.21+ 的 Minecraft RPG 服务器插件体系

[![Minecraft](https://img.shields.io/badge/Minecraft-Paper%201.21+-green)](https://papermc.io)
[![Java](https://img.shields.io/badge/Java-21+-orange)](https://www.oracle.com/java)

## 📖 文档

插件使用文档：[https://meisyangb.github.io/astraea-rpg-server](https://meisyangb.github.io/astraea-rpg-server)

## 🏗️ 插件概览

### 核心框架
| 插件 | 说明 |
|------|------|
| [RPGCore](RPGCore/) | 核心框架，提供服务注册、事件总线、缓存管理等基础服务 |

### 属性与职业
| 插件 | 说明 |
|------|------|
| [GuangDianArmorStats](GuangDianArmorStats/) | RPG装备属性系统 |
| [GuangDianClass](GuangDianClass/) | 职业系统，支持转职、职业技能 |
| [GuangDianAccessory](GuangDianAccessory/) | 饰品系统 |
| [GuangDianGearScore](GuangDianGearScore/) | 装备评分系统 |

### 怪物与战斗
| 插件 | 说明 |
|------|------|
| [GuangDianMobs](GuangDianMobs/) | 自定义怪物系统 |
| [GuangDianMobHealth](GuangDianMobHealth/) | 怪物血量显示 |
| [GuangDianAggro](GuangDianAggro/) | 仇恨系统 |
| [GuangDianRaid](GuangDianRaid/) | 副本系统 |

### 任务系统
| 插件 | 说明 |
|------|------|
| [GuangDianQuest](GuangDianQuest/) | 任务系统，支持主线/支线/每日 |
| [GuangDianBattlePass](GuangDianBattlePass/) | 战令系统 |

### 经济与社交
| 插件 | 说明 |
|------|------|
| [GuangDianMarket](GuangDianMarket/) | 市场/拍卖行 |
| [GuangDianBank](GuangDianBank/) | 银行系统 |
| [GuangDianGuild](GuangDianGuild/) | 公会系统 |
| [GuangDianMarriage](GuangDianMarriage/) | 结婚系统 |
| [GuangDianTrade](GuangDianTrade/) | 交易系统 |

### 装备锻造
| 插件 | 说明 |
|------|------|
| [GuangDianForge](GuangDianForge/) | 锻造系统 |
| [GuangDianDecompose](GuangDianDecompose/) | 分解系统 |
| [GuangDianSocket](GuangDianSocket/) | 宝石镶嵌 |
| [GuangDianDragonCore](GuangDianDragonCore/) | 龙核系统 |

### 界面与显示
| 插件 | 说明 |
|------|------|
| [GuangDianMenu](GuangDianMenu/) | GUI菜单系统 |
| [GuangDianHolo](GuangDianHolo/) | 全息字显示 |
| [GuangDianBoard](GuangDianBoard/) | 计分板 |
| [GuangDianTab](GuangDianTab/) | Tab列表 |
| [GuangDianChat](GuangDianChat/) | 聊天系统 |

### 洞府与领地
| 插件 | 说明 |
|------|------|
| [GuangDianCaveFu](GuangDianCaveFu/) | 洞府系统 |
| [GuangDianWorld](GuangDianWorld/) | 世界管理 |
| [GuangDianWorldRules](GuangDianWorldRules/) | 世界规则 |

### 功能辅助
| 插件 | 说明 |
|------|------|
| [GuangDianNPC](GuangDianNPC/) | NPC系统 |
| [GuangDianMCP](GuangDianMCP/) | MCP服务端桥接 |
| [RPGItems](RPGItems/) | RPG自定义物品 |
| [RPGSkill](RPGSkill/) | RPG技能系统 |

## 🚀 快速开始

1. 安装 Paper 1.21+ 服务端
2. 编译所有插件：`gradle build -x test`
3. 将 `build/libs/*.jar` 放入 `plugins/` 目录
4. 重启服务器
5. 参考 [文档](https://meisyangb.github.io/astraea-rpg-server) 配置

## 🔧 构建要求

- JDK 21+
- Paper 1.21+ API

## 📄 许可

本项目为专有软件，保留所有权利。
