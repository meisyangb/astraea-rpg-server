---
name: knowledge-index
description: "Astraea RPG 知识管理系统 - 统一的文档、日志、记忆索引和检索中心"
version: 1.0.0
---

# Astraea RPG 知识管理系统 (Knowledge Management System)

> 基于 Markdown + JSON 索引的专业知识管理方案

---

## 📁 目录结构

```
.trae/knowledge/
├── INDEX.md                 # 本文件 - 知识库总索引
├── index.json               # 机器可读索引文件
├── search.json              # 搜索索引
├── taxonomy.json            # 分类体系定义
│
├── docs/                    # 文档中心
│   ├── skills/             # 技能文档
│   │   ├── minecraft-rpg-architect/
│   │   │   ├── SKILL.md
│   │   │   ├── examples.md
│   │   │   └── reference.md
│   │   ├── code-reviewer/
│   │   ├── performance-tuner/
│   │   └── ui-designer/
│   │
│   ├── rules/              # 规则文档
│   │   └── kaifa.md
│   │
│   └── guides/             # 指南文档
│       ├── getting-started.md
│       ├── architecture.md
│       └── best-practices.md
│
├── logs/                   # 日志中心
│   ├── INDEX.md            # 日志索引
│   ├── 2026/
│   │   ├── 04/
│   │   │   ├── 2026-04-10-plugin-migration.md
│   │   │   └── 2026-04-10-bug-fixes.md
│   │   └── 05/
│   └── categories/         # 按分类组织的日志
│       ├── migration/      # 迁移日志
│       ├── bugfix/         # 修复日志
│       ├── optimization/   # 优化日志
│       └── feature/        # 功能日志
│
├── memory/                 # 记忆中心
│   ├── INDEX.md            # 记忆索引
│   ├── fixes/              # 修复记忆
│   │   ├── scheduler-migration.md
│   │   ├── rpgcore-singleton.md
│   │   ├── chatcolor-adventure.md
│   │   └── placeholder-unregister.md
│   ├── patterns/           # 模式记忆
│   │   ├── forbidden-patterns.md
│   │   └── best-practices.md
│   ├── solutions/          # 解决方案
│   │   ├── performance-issues.md
│   │   └── common-bugs.md
│   └── decisions/          # 决策记录 (ADR)
│       └── 001-use-syncscheduler.md
│
└── meta/                   # 元数据
    ├── tags.json           # 标签定义
    ├── contributors.json   # 贡献者信息
    └── stats.json          # 统计信息
```

---

## 🏷️ 分类体系 (Taxonomy)

### 主分类

| 分类ID | 名称 | 说明 | 路径 |
|--------|------|------|------|
| `docs` | 文档 | 技能文档、规则、指南 | `docs/` |
| `logs` | 日志 | 操作记录、修复记录 | `logs/` |
| `memory` | 记忆 | 解决方案、模式、决策 | `memory/` |

### 子分类

#### 日志分类 (logs/categories/)
| 分类ID | 名称 | 说明 |
|--------|------|------|
| `migration` | 迁移 | 代码迁移、架构迁移 |
| `bugfix` | 修复 | Bug修复、问题解决 |
| `optimization` | 优化 | 性能优化、代码优化 |
| `feature` | 功能 | 新功能开发 |
| `refactor` | 重构 | 代码重构 |
| `review` | 审查 | 代码审查记录 |

#### 记忆分类 (memory/)
| 分类ID | 名称 | 说明 |
|--------|------|------|
| `fixes` | 修复 | 具体修复方案 |
| `patterns` | 模式 | 设计模式、代码模式 |
| `solutions` | 解决方案 | 问题解决方案 |
| `decisions` | 决策 | 架构决策记录 (ADR) |
| `snippets` | 代码片段 | 可复用代码 |

---

## 🔍 索引系统

### 1. 全局索引 (index.json)

```json
{
  "version": "1.0.0",
  "lastUpdated": "2026-04-10T12:00:00Z",
  "totalDocuments": 25,
  "categories": {
    "docs": { "count": 8, "lastUpdated": "2026-04-10" },
    "logs": { "count": 12, "lastUpdated": "2026-04-10" },
    "memory": { "count": 5, "lastUpdated": "2026-04-10" }
  },
  "documents": [
    {
      "id": "doc-skill-minecraft-rpg-architect",
      "type": "doc",
      "category": "skills",
      "title": "Minecraft RPG 架构师",
      "path": "docs/skills/minecraft-rpg-architect/SKILL.md",
      "tags": ["skill", "rpg", "minecraft", "architecture"],
      "created": "2026-04-10",
      "updated": "2026-04-10",
      "version": "2.0.0"
    }
  ]
}
```

### 2. 搜索索引 (search.json)

```json
{
  "index": {
    "BukkitRunnable": ["doc-skill-minecraft-rpg-architect", "mem-fix-scheduler"],
    "SyncScheduler": ["doc-skill-minecraft-rpg-architect", "mem-fix-scheduler", "log-2026-04-10-migration"],
    "RPGCore": ["doc-skill-minecraft-rpg-architect", "mem-fix-rpgcore-singleton"],
    "性能优化": ["doc-skill-performance-tuner", "mem-solution-performance"]
  }
}
```

### 3. 标签系统 (meta/tags.json)

```json
{
  "tags": {
    "skill": { "name": "技能", "color": "#4CAF50", "count": 5 },
    "rpg": { "name": "RPG", "color": "#2196F3", "count": 8 },
    "minecraft": { "name": "Minecraft", "color": "#795548", "count": 10 },
    "performance": { "name": "性能", "color": "#FF9800", "count": 3 },
    "bugfix": { "name": "修复", "color": "#F44336", "count": 12 },
    "migration": { "name": "迁移", "color": "#9C27B0", "count": 5 },
    "pattern": { "name": "模式", "color": "#607D8B", "count": 4 }
  }
}
```

---

## 📝 文档格式规范

### Frontmatter 标准

```yaml
---
id: unique-identifier          # 唯一标识符
type: doc|log|memory           # 文档类型
category: skills|rules|guides  # 主分类
subcategory: migration|fix     # 子分类
title: "文档标题"
description: "简短描述"
author: "作者名"
date: 2026-04-10               # 创建日期
updated: 2026-04-10            # 更新日期
version: 1.0.0                 # 版本
tags: [tag1, tag2, tag3]       # 标签
status: draft|published|archived # 状态
related: [id1, id2]            # 相关文档
---
```

### 日志格式

```yaml
---
id: log-2026-04-10-scheduler-migration
type: log
category: migration
subcategory: scheduler
title: "调度器迁移日志"
date: 2026-04-10
plugin: GuangDianName
severity: high
tags: [migration, scheduler, bukkitrunnable]
related: [mem-fix-scheduler, doc-skill-minecraft-rpg-architect]
---

# 调度器迁移日志

## 摘要
迁移 GuangDianName 插件的 BukkitRunnable 到 SyncScheduler

## 变更详情
- 修改文件: GuangDianName.java
- 违规数量: 3处
- 修复方式: 使用 RPGCore.getInstance().getScheduler()

## 代码对比
```java
// 修改前
Bukkit.getScheduler().runTaskLater(...)

// 修改后
scheduler.runSyncLater(...)
```

## 验证
- [x] 编译通过
- [x] 功能测试
- [x] 性能测试
```

### 记忆格式

```yaml
---
id: mem-fix-scheduler
type: memory
category: fixes
title: "调度器修复方案"
description: "BukkitRunnable 迁移到 SyncScheduler 的完整方案"
date: 2026-04-10
tags: [fix, scheduler, pattern]
related: [log-2026-04-10-scheduler-migration]
---

# 调度器修复方案

## 问题
BukkitRunnable 和 Bukkit.getScheduler() 分散在各插件，难以管理

## 解决方案
使用 RPGCore 的 SyncScheduler 统一调度

## 正确代码
```java
RPGCore rpgCore = RPGCore.getInstance();
SyncScheduler scheduler = rpgCore.getScheduler();
long taskId = scheduler.runSyncLater(() -> {...}, 50L);
```

## 注意事项
1. 保存 taskId 以便取消
2. 在 onDisable 中调用 scheduler.cancelAllTasks()

## 相关日志
- [2026-04-10 调度器迁移](log-2026-04-10-scheduler-migration.md)
```

---

## 🔧 使用工具

### 1. 文档生成器
自动生成索引、搜索索引、标签统计

### 2. 链接检查器
检查文档间链接的有效性

### 3. 搜索工具
基于关键词快速检索相关文档

### 4. 统计面板
- 文档数量统计
- 更新频率分析
- 标签云
- 贡献者排名

---

## 📊 统计信息

| 指标 | 数值 |
|------|------|
| 总文档数 | 25 |
| 技能文档 | 5 |
| 日志记录 | 12 |
| 记忆条目 | 5 |
| 标签数量 | 15 |
| 最后更新 | 2026-04-10 |

---

*知识管理系统版本: 1.0.0*
*最后更新: 2026-04-10*
