---
alwaysApply: true
---

# Astraea RPG 任务分类系统

> 本文件是任务分类和路由中心，每次对话都会读取

---

## 🎯 任务分类表

根据用户输入的关键词，快速识别任务类型并路由：

| 任务类型 | 关键词 | 处理方式 | 详细文档 |
|----------|--------|----------|----------|
| **构建** | 构建、build、编译、打包 | 执行唯一构建命令 | [.trae/docs/reference/BUILD_GUIDE.md](.trae/docs/reference/BUILD_GUIDE.md) |
| **开发** | 开发、创建、实现、添加 | 激活 `minecraft-rpg-architect` | [.trae/skills/minecraft-rpg-architect/SKILL.md](.trae/skills/minecraft-rpg-architect/SKILL.md) |
| **修复** | 修复、解决、Bug、错误 | 激活 `minecraft-rpg-architect` + 记录修复 | [.trae/docs/reference/RELEASE_CHECKLIST.md](.trae/docs/reference/RELEASE_CHECKLIST.md) |
| **审查** | 审查、检查、规范、优化 | 激活 `code-reviewer` | [.trae/knowledge/memory/patterns/review-checklist.md](.trae/knowledge/memory/patterns/review-checklist.md) |
| **性能** | 性能、优化、卡顿、内存 | 激活 `performance-tuner` | [.trae/skills/performance-tuner/SKILL.md](.trae/skills/performance-tuner/SKILL.md) |
| **UI** | UI、界面、菜单、GUI | 激活 `ui-designer` | [.trae/skills/ui-designer/SKILL.md](.trae/skills/ui-designer/SKILL.md) |
| **版本** | 版本、发布、标签、Git | 执行版本控制流程 | [.trae/docs/reference/VERSION_CONTROL.md](.trae/docs/reference/VERSION_CONTROL.md) |
| **文档** | 文档、说明、更新日志 | 更新相关文档 | [.trae/knowledge/INDEX.md](.trae/knowledge/INDEX.md) |

---

## 🚀 快速路由

### 1. 构建任务
```powershell
# 唯一构建命令
cd e:\原创RPG服务端
$env:JAVA_HOME="e:\原创RPG服务端\tools\jdk-21.0.10+7"
& "D:\gradle\gradle-9.4.0\bin\gradle.bat" build --no-configuration-cache -x test
```
详见: [.trae/docs/reference/BUILD_GUIDE.md](.trae/docs/reference/BUILD_GUIDE.md)

### 2. 开发任务
1. 激活技能: `minecraft-rpg-architect`
2. 阅读禁止模式: [.trae/rules/FORBIDDEN_PATTERNS.md](.trae/rules/FORBIDDEN_PATTERNS.md)
3. 使用代码模板: [.trae/rules/CODE_TEMPLATES.md](.trae/rules/CODE_TEMPLATES.md)
4. 执行构建验证

### 3. 修复任务
1. 激活技能: `minecraft-rpg-architect`
2. 分析问题根因
3. 实施修复
4. 记录到: [.trae/docs/FIXES/](.trae/docs/FIXES/)
5. 更新知识库

### 4. 审查任务
1. 激活技能: `code-reviewer`
2. 检查禁止模式
3. 验证代码规范
4. 输出审查报告

---

## 📚 文档索引

### 核心规则
| 文档 | 用途 |
|------|------|
| [.trae/rules/FORBIDDEN_PATTERNS.md](.trae/rules/FORBIDDEN_PATTERNS.md) | 禁止模式清单 |
| [.trae/rules/CODE_TEMPLATES.md](.trae/rules/CODE_TEMPLATES.md) | 代码模板库 |

### 工作流程
| 文档 | 用途 |
|------|------|
| [.trae/docs/workflow/WORKFLOW.md](.trae/docs/workflow/WORKFLOW.md) | 完整工作流程 |
| [.trae/docs/workflow/TASK_WORKFLOW.md](.trae/docs/workflow/TASK_WORKFLOW.md) | 任务执行流程 |

### 参考文档
| 文档 | 用途 |
|------|------|
| [.trae/docs/reference/BUILD_GUIDE.md](.trae/docs/reference/BUILD_GUIDE.md) | 唯一构建方法 |
| [.trae/docs/reference/VERSION_CONTROL.md](.trae/docs/reference/VERSION_CONTROL.md) | 版本控制规范 |
| [.trae/docs/reference/RELEASE_CHECKLIST.md](.trae/docs/reference/RELEASE_CHECKLIST.md) | 发布检查清单 |
| [.trae/docs/CHANGELOG.md](.trae/docs/CHANGELOG.md) | 更新日志 |
| [.trae/docs/PROJECT_NAMING.md](.trae/docs/PROJECT_NAMING.md) | 命名规范 |

### 技能文档
| 技能 | 文档位置 |
|------|----------|
| minecraft-rpg-architect | [.trae/skills/minecraft-rpg-architect/SKILL.md](.trae/skills/minecraft-rpg-architect/SKILL.md) |
| code-reviewer | [.trae/skills/code-reviewer/SKILL.md](.trae/skills/code-reviewer/SKILL.md) |
| performance-tuner | [.trae/skills/performance-tuner/SKILL.md](.trae/skills/performance-tuner/SKILL.md) |
| ui-designer | [.trae/skills/ui-designer/SKILL.md](.trae/skills/ui-designer/SKILL.md) |

### 知识库
| 类型 | 位置 |
|------|------|
| 知识索引 | [.trae/knowledge/INDEX.md](.trae/knowledge/INDEX.md) |
| 模式记忆 | [.trae/knowledge/memory/patterns/](.trae/knowledge/memory/patterns/) |
| 修复记录 | [.trae/docs/FIXES/](.trae/docs/FIXES/) |

---

## ⚡ 执行流程

```
用户输入
    ↓
识别任务类型 (关键词匹配)
    ↓
路由到对应处理方式
    ↓
激活技能 / 执行命令
    ↓
验证结果
    ↓
更新文档/知识库
    ↓
完成
```

---

## 🔧 环境基线

| 项目 | 值 |
|------|---|
| 服务端 | Paper 1.21.6 |
| JDK | JDK 21 (`tools/jdk-21.0.10+7`) |
| Gradle | 9.4.0 (`D:\gradle\gradle-9.4.0`) |
| 项目根目录 | `e:\原创RPG服务端` |
| 插件数量 | 24个 GuangDian* + RPGCore |

---

*此文件是任务分类中心，保持简洁*
*详细规则见对应文档*
