---
id: knowledge-changelog
type: meta
title: "知识库变更日志"
description: "追踪所有技能文件、规则、记忆的版本变更"
date: 2026-04-10
version: 1.0.0
---

# 知识库变更日志 (Changelog)

> 自动追踪和记录所有知识资产的变更

---

## 变更追踪规则

### 1. 版本号规范
- **主版本号 (X.0.0)**: 架构重大变更、破坏性更新
- **次版本号 (0.X.0)**: 新增功能、新技能、新规则
- **修订号 (0.0.X)**: 修复、优化、文档更新

### 2. 变更类型
| 类型 | 图标 | 说明 |
|------|------|------|
| `added` | 🆕 | 新增内容 |
| `changed` | 🔄 | 变更内容 |
| `deprecated` | ⚠️ | 废弃内容 |
| `removed` | 🗑️ | 移除内容 |
| `fixed` | 🐛 | 修复问题 |
| `security` | 🔒 | 安全更新 |

### 3. 自动更新触发条件
- 代码修复完成后 → 更新相关技能文档
- 新插件开发完成后 → 新增技能文档
- 架构变更后 → 更新规则文档
- 每次会话结束后 → 更新索引和搜索

---

## 变更历史

### [2026-04-10] - 知识管理系统初始化

#### 🆕 Added
- 创建知识管理系统目录结构
- 创建 `INDEX.md` 知识库总索引
- 创建 `index.json` 机器可读索引
- 创建 `search.json` 搜索索引
- 创建 `meta/tags.json` 标签系统
- 创建 `CHANGELOG.md` 变更日志

#### 🆕 Added - 技能文档
- `skill-selector/SKILL.md` - 技能选择器
- `minecraft-rpg-architect/SKILL.md` - RPG架构师 (v2.0.0)
- `code-reviewer/SKILL.md` - 代码审查员
- `performance-tuner/SKILL.md` - 性能优化师
- `ui-designer/SKILL.md` - UI设计师

#### 🆕 Added - 修复记忆
- `memory/fixes/scheduler-migration.md` - 调度器迁移方案

#### 🆕 Added - 迁移日志
- `logs/categories/migration/2026-04-10-scheduler-migration.md`

---

### [2026-04-10] - 调度器迁移修复

#### 🐛 Fixed
- 修复 17 个插件的 Bukkit.getScheduler() 违规
- 违规数量: 47处 → 迁移到 SyncScheduler
- 涉及插件: GuangDianName, GuangDianArmorStats, GuangDianMarket 等

#### 🔄 Changed
- 更新 `kaifa.md` 规则文件，添加知识库导航
- 更新技能文件位置说明

---

### [2026-04-10] - 版本同步系统建立

#### 🆕 Added
- 创建 `meta/version-checker.json` 版本检查器
- 建立版本同步追踪机制
- 创建自动化文档更新流程规范

#### 🔄 Changed
- 更新 `minecraft-rpg-architect/SKILL.md` 版本号到 v2.1.0
- 更新插件数量统计: 27个 → 24个
- 更新 MIGRATION STATUS 表格
- 添加 52处违规扫描结果到文档

#### ✅ Synchronized
- 所有技能文件版本已同步
- 所有索引文件已更新
- 版本状态: `synced`

---

## 自动化更新流程

```
代码变更
    ↓
[触发] 变更检测
    ↓
识别受影响的技能/规则/记忆
    ↓
自动更新相关文档
    ↓
更新版本号和日期
    ↓
更新索引和搜索
    ↓
记录到 CHANGELOG
    ↓
提交变更
```

---

*最后更新: 2026-04-10*
*同步状态: ✅ 全部同步完成*
*下次检查: 每次会话结束后*
