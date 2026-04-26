---
name: knowledge-index
description: "Astraea RPG 知识管理系统 - 统一的文档、日志、记忆索引和检索中心"
version: 2.1.0
---

# Astraea RPG 知识管理系统 (Knowledge Management System)

> 基于 Markdown + JSON 索引的专业知识管理方案
> **版本: 2.1.0 | 更新: 2026-04-26**

---

## 📁 目录结构

```
.trae/knowledge/
├── INDEX.md                 # 本文件 - 知识库总索引
├── index.json               # 机器可读索引文件
├── search.json              # 搜索索引
├── fixes/                   # 修复记录中心
│   ├── README.md            # 修复记录索引
│   ├── TEMPLATE.md          # 修复记录模板
│   └── scheduler-migration.md # 调度器迁移修复
└── memory/                  # 记忆中心
    └── patterns/            # 模式记忆
        ├── singleton-dcl-pattern.md
        ├── resource-shutdown.md
        ├── build-process.md
        └── minimessage-placeholder-guide.md
```

---

## 🏷️ 分类体系 (Taxonomy)

### 主分类

| 分类ID | 名称 | 说明 | 路径 |
|--------|------|------|------|
| `fixes` | 修复记录 | 问题修复方案 | `fixes/` |
| `patterns` | 模式 | 设计模式、代码模式 | `memory/patterns/` |

---

## 🔍 索引系统

### 1. 全局索引 (index.json)

```json
{
  "version": "2.1.0",
  "lastUpdated": "2026-04-26T12:00:00Z",
  "totalDocuments": 6,
  "categories": {
    "fixes": { "count": 2, "lastUpdated": "2026-04-26" },
    "patterns": { "count": 4, "lastUpdated": "2026-04-14" }
  }
}
```

### 2. 修复记录索引

| 文档 | 类别 | 日期 | 标签 |
|------|------|------|------|
| [scheduler-migration](fixes/scheduler-migration.md) | 调度器 | 2026-04-10 | fix, scheduler, migration |

### 3. 模式记忆索引

| 文档 | 类别 | 日期 | 标签 |
|------|------|------|------|
| [singleton-dcl-pattern](memory/patterns/singleton-dcl-pattern.md) | 设计模式 | 2026-04-14 | pattern, singleton |
| [resource-shutdown](memory/patterns/resource-shutdown.md) | 资源管理 | 2026-04-14 | pattern, resource |
| [build-process](memory/patterns/build-process.md) | 构建流程 | 2026-04-14 | pattern, build |
| [minimessage-placeholder-guide](memory/patterns/minimessage-placeholder-guide.md) | 消息格式 | 2026-04-14 | pattern, message |

---

## 📝 文档格式规范

### Frontmatter 标准

```yaml
---
id: unique-identifier          # 唯一标识符
type: fix|pattern|doc          # 文档类型
category: scheduler|database   # 主分类
title: "文档标题"
description: "简短描述"
date: 2026-04-10               # 创建日期
version: 1.0.0                 # 版本
tags: [tag1, tag2, tag3]       # 标签
status: draft|published        # 状态
---
```

---

## 📊 统计信息

| 指标 | 数值 |
|------|------|
| 总文档数 | 6 |
| 修复记录 | 1 |
| 模式条目 | 4 |
| 最后更新 | 2026-04-26 |

---

## 🔗 相关文档

- [.trae/README.md](../README.md) - 文档导航中心
- [rules/DEVELOPMENT_GUIDE.md](../rules/DEVELOPMENT_GUIDE.md) - 开发指南
- [docs/reference/BUILD_GUIDE.md](../docs/reference/BUILD_GUIDE.md) - 构建指南

---

*知识管理系统版本: 2.1.0*
*最后更新: 2026-04-26*
