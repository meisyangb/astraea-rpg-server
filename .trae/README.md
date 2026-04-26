# Astraea RPG 文档中心

> 统一的文档导航和索引中心
> **版本: 2.1.0 | 更新: 2026-04-26**

---

## 📚 文档导航

### 🚀 快速开始

| 文档 | 说明 | 适用场景 |
|------|------|----------|
| [rules/DEVELOPMENT_GUIDE.md](rules/DEVELOPMENT_GUIDE.md) | **开发指南** - 禁止模式、代码模板、API参考 | 所有开发任务 |
| [docs/reference/BUILD_GUIDE.md](docs/reference/BUILD_GUIDE.md) | **构建指南** - 唯一构建方法 | 编译打包 |
| [docs/workflow/WORKFLOW.md](docs/workflow/WORKFLOW.md) | **工作流程** - 开发流程概览 | 了解工作流程 |

---

### 📋 规则与规范

| 文档 | 说明 |
|------|------|
| [rules/DEVELOPMENT_GUIDE.md](rules/DEVELOPMENT_GUIDE.md) | 开发指南（禁止模式 + 代码模板 + 任务路由） |
| [rules/COMMIT_STANDARDS.md](rules/COMMIT_STANDARDS.md) | 代码提交规范 |
| [rules/RPGCORE_DEVELOPMENT_STANDARD.md](rules/RPGCORE_DEVELOPMENT_STANDARD.md) | RPGCore 开发标准 |
| [docs/PROJECT_NAMING.md](docs/PROJECT_NAMING.md) | 项目命名规范 |

---

### 🔧 工作流程

| 文档 | 说明 |
|------|------|
| [docs/workflow/WORKFLOW.md](docs/workflow/WORKFLOW.md) | 工作流程概览（快速参考） |
| [docs/workflow/TASK_WORKFLOW.md](docs/workflow/TASK_WORKFLOW.md) | 任务执行流程（详细版） |

---

### 📖 参考文档

| 文档 | 说明 |
|------|------|
| [docs/reference/BUILD_GUIDE.md](docs/reference/BUILD_GUIDE.md) | 唯一构建方法 (CMD) |
| [docs/reference/RPGCORE_API_REFERENCE.md](docs/reference/RPGCORE_API_REFERENCE.md) | RPGCore API 参考手册 |
| [docs/reference/VERSION_CONTROL.md](docs/reference/VERSION_CONTROL.md) | 版本控制规范 |
| [docs/reference/RELEASE_CHECKLIST.md](docs/reference/RELEASE_CHECKLIST.md) | 发布检查清单 |
| [docs/reference/ARCHITECTURE_BOUNDARIES.md](docs/reference/ARCHITECTURE_BOUNDARIES.md) | 架构边界定义 |

---

### 🏗️ 架构文档

| 文档 | 说明 |
|------|------|
| [docs/RPGCore/ARCHITECTURE.md](docs/RPGCore/ARCHITECTURE.md) | RPGCore 核心框架详细文档 |
| [docs/RPGCore/UNIFIED_SERVICES_OVERVIEW.md](docs/RPGCore/UNIFIED_SERVICES_OVERVIEW.md) | 统一服务总览 |
| [docs/RPGCore/ARCHITECTURE_UPGRADE_GUIDE.md](docs/RPGCore/ARCHITECTURE_UPGRADE_GUIDE.md) | 架构升级指南 |
| [docs/RPGCore/RPGCORE_REFACTOR_PLAN.md](docs/RPGCore/RPGCORE_REFACTOR_PLAN.md) | 微内核架构重构方案 |

---

### 🛠️ 技能文档

| 技能 | 文档位置 |
|------|----------|
| minecraft-rpg-architect | [skills/minecraft-rpg-architect/SKILL.md](skills/minecraft-rpg-architect/SKILL.md) |
| code-reviewer | [skills/code-reviewer/SKILL.md](skills/code-reviewer/SKILL.md) |
| performance-tuner | [skills/performance-tuner/SKILL.md](skills/performance-tuner/SKILL.md) |
| ui-designer | [skills/ui-designer/SKILL.md](skills/ui-designer/SKILL.md) |

---

### 🧠 知识库

| 类型 | 位置 | 说明 |
|------|------|------|
| 知识索引 | [knowledge/INDEX.md](knowledge/INDEX.md) | 知识库总索引 |
| 修复记录 | [knowledge/fixes/README.md](knowledge/fixes/README.md) | 问题修复记录 |
| 设计模式 | [knowledge/memory/patterns/](knowledge/memory/patterns/) | 代码模式总结 |

---

### 📝 变更日志

| 文档 | 说明 |
|------|------|
| [docs/CHANGELOG.md](docs/CHANGELOG.md) | 版本更新日志 |

---

## 🎯 任务路由

根据任务类型快速定位文档：

| 任务类型 | 关键词 | 主要文档 |
|----------|--------|----------|
| **构建** | 构建、build、编译、打包 | [BUILD_GUIDE.md](docs/reference/BUILD_GUIDE.md) |
| **开发** | 开发、创建、实现、添加 | [DEVELOPMENT_GUIDE.md](rules/DEVELOPMENT_GUIDE.md) |
| **修复** | 修复、解决、Bug、错误 | [fixes/README.md](knowledge/fixes/README.md) |
| **审查** | 审查、检查、规范、优化 | [code-reviewer/SKILL.md](skills/code-reviewer/SKILL.md) |
| **性能** | 性能、优化、卡顿、内存 | [performance-tuner/SKILL.md](skills/performance-tuner/SKILL.md) |
| **UI** | UI、界面、菜单、GUI | [ui-designer/SKILL.md](skills/ui-designer/SKILL.md) |
| **版本** | 版本、发布、标签、Git | [VERSION_CONTROL.md](docs/reference/VERSION_CONTROL.md) |

---

## 📁 目录结构

```
.trae/
├── README.md                    # 本文档 - 文档导航中心
├── rules/                       # 规则与规范
│   ├── DEVELOPMENT_GUIDE.md    # 开发指南（主文档）
│   ├── COMMIT_STANDARDS.md     # 提交规范
│   └── RPGCORE_DEVELOPMENT_STANDARD.md
├── docs/
│   ├── workflow/               # 工作流程
│   │   ├── WORKFLOW.md         # 流程概览
│   │   └── TASK_WORKFLOW.md    # 详细流程
│   ├── reference/              # 参考文档
│   │   ├── BUILD_GUIDE.md      # 构建指南
│   │   ├── RPGCORE_API_REFERENCE.md
│   │   ├── VERSION_CONTROL.md
│   │   └── RELEASE_CHECKLIST.md
│   ├── RPGCore/                # 架构文档
│   │   ├── ARCHITECTURE.md
│   │   ├── UNIFIED_SERVICES_OVERVIEW.md
│   │   └── ARCHITECTURE_UPGRADE_GUIDE.md
│   ├── FIXES/                  # 修复记录（已迁移到 knowledge/fixes/）
│   │   └── INDEX.md            # 迁移提示
│   ├── CHANGELOG.md            # 更新日志
│   └── PROJECT_NAMING.md       # 命名规范
├── knowledge/                   # 知识库
│   ├── INDEX.md                # 知识库索引
│   ├── fixes/                  # 修复记录
│   │   ├── README.md
│   │   ├── TEMPLATE.md
│   │   └── scheduler-migration.md
│   └── memory/
│       └── patterns/           # 设计模式
└── skills/                      # 技能文档
    ├── minecraft-rpg-architect/
    ├── code-reviewer/
    ├── performance-tuner/
    └── ui-designer/
```

---

## 🔧 环境基线

| 项目 | 值 |
|------|---|
| 服务端 | Paper 1.21.6 |
| JDK | JDK 21 |
| Gradle | 9.4.0 |
| 项目根目录 | `e:\原创RPG服务端` |

---

*最后更新: 2026-04-26*
*版本: 2.1.0*
