# Astraea RPG 版本控制规范

> 版本管理策略、分支隔离、标签规范、发布流程

---

## 1. 版本分支策略 (Git Flow)

### 分支结构

```
main (生产环境)
  ↑
develop (开发集成)
  ↑
feature/* (功能开发)  fix/* (修复)  release/* (发布)
```

### 分支说明

| 分支 | 用途 | 保护级别 | 生命周期 |
|------|------|----------|----------|
| `main` | 生产环境代码 | 🔒 受保护 | 永久 |
| `develop` | 开发集成 | 🔒 受保护 | 永久 |
| `feature/*` | 新功能开发 | 📝 代码审查 | 合并后删除 |
| `fix/*` | Bug修复 | 📝 代码审查 | 合并后删除 |
| `release/*` | 版本发布准备 | 🔒 受保护 | 发布后删除 |
| `hotfix/*` | 生产环境紧急修复 | 🔒 受保护 | 合并后删除 |

---

## 2. 版本号规范 (SemVer)

### 格式
```
主版本号.次版本号.修订号[-预发布标识]
```

示例:
- `1.0.0` - 正式版本
- `1.1.0-beta.1` - 测试版本
- `2.0.0-alpha` - 预览版本

### 版本规则

| 版本类型 | 说明 | 示例 |
|----------|------|------|
| **主版本号** | 破坏性变更、架构升级 | `1.x.x` → `2.0.0` |
| **次版本号** | 新功能、向后兼容 | `x.0.x` → `x.1.0` |
| **修订号** | Bug修复、小优化 | `x.x.0` → `x.x.1` |
| **预发布** | alpha/beta/rc 阶段 | `1.0.0-alpha` |

### 版本升级场景

```
1.0.0 → 1.0.1  修复Bug
1.0.1 → 1.1.0  添加新功能
1.1.0 → 2.0.0  架构重构/破坏性变更
```

---

## 3. 版本标签 (Git Tag)

### 标签命名

```
v[版本号]
```

示例:
- `v1.0.0`
- `v1.1.0-beta.1`
- `v2.0.0-rc.1`

### 标签类型

| 标签 | 用途 | 创建时机 |
|------|------|----------|
| **轻量标签** | 临时标记 | 开发测试 |
| **附注标签** | 正式版本 | 发布时 |

### 标签命令

```bash
# 创建附注标签
git tag -a v1.0.0 -m "Release version 1.0.0"

# 推送标签到远程
git push origin v1.0.0

# 推送所有标签
git push origin --tags

# 删除本地标签
git tag -d v1.0.0

# 删除远程标签
git push origin --delete v1.0.0
```

---

## 4. 版本隔离机制

### 环境隔离

| 环境 | 分支 | 用途 | 部署频率 |
|------|------|------|----------|
| **开发环境** | `feature/*` | 功能开发 | 随时 |
| **测试环境** | `develop` | 集成测试 | 每日 |
| **预发布** | `release/*` | UAT测试 | 每版本 |
| **生产环境** | `main` | 正式运行 | 每版本 |

### 数据隔离

```
server/
├── dev/          # 开发环境数据
├── test/         # 测试环境数据
├── staging/      # 预发布环境数据
└── prod/         # 生产环境数据 (不提交到Git)
```

### 配置隔离

```yaml
# config-dev.yml    - 开发配置
# config-test.yml   - 测试配置
# config-prod.yml   - 生产配置 (不提交)
```

---

## 5. 版本更新说明 (CHANGELOG)

### 文件位置

```
CHANGELOG.md          # 主更新日志
docs/CHANGELOG/       # 历史版本归档
├── v1.0.0.md
├── v1.1.0.md
└── v2.0.0.md
```

### 更新日志格式

```markdown
## [1.0.0] - 2026-04-10

### 🆕 Added
- 新增玩家数据缓存系统
- 添加装备属性计算模块
- 实现统一调度器 SyncScheduler

### 🔄 Changed
- 优化数据库查询性能
- 重构玩家生命周期管理

### 🐛 Fixed
- 修复 Bukkit.getScheduler() 内存泄漏
- 修复并发访问数据竞争问题

### ⚠️ Deprecated
- 废弃旧版 API `getPlayerDataOld()`

### 🔒 Security
- 修复 SQL 注入漏洞
```

### 变更类型标识

| 标识 | 含义 | 图标 |
|------|------|------|
| `Added` | 新增功能 | 🆕 |
| `Changed` | 变更 | 🔄 |
| `Deprecated` | 废弃 | ⚠️ |
| `Removed` | 移除 | 🗑️ |
| `Fixed` | 修复 | 🐛 |
| `Security` | 安全 | 🔒 |

---

## 6. 修复记录系统

### 文件结构

```
docs/FIXES/
├── INDEX.md              # 修复索引
├── TEMPLATE.md           # 修复记录模板
├── 2026/
│   ├── 04/
│   │   ├── 2026-04-10-scheduler-leak.md
│   │   └── 2026-04-15-concurrent-access.md
│   └── 05/
└── BY_CATEGORY/
    ├── scheduler/        # 调度器相关修复
    ├── database/         # 数据库相关修复
    └── api/              # API相关修复
```

### 修复记录模板

```markdown
---
id: FIX-2026-0410-001
date: 2026-04-10
severity: high|medium|low
category: scheduler|database|api|performance
plugin: RPGCore|GuangDianName|...
status: fixed|testing|pending
affects: [v1.0.0, v1.0.1]
fixed_in: v1.0.2
---

# 修复记录: [标题]

## 问题描述
[详细描述问题现象]

## 影响范围
- 版本: v1.0.0 - v1.0.1
- 模块: SyncScheduler
- 玩家影响: 所有在线玩家

## 根因分析
[分析问题根本原因]

## 修复方案
[详细修复步骤]

## 代码变更
```diff
- 旧代码
+ 新代码
```

## 验证步骤
1. [验证步骤1]
2. [验证步骤2]

## 预防措施
[如何避免类似问题]

## 相关链接
- Issue: #123
- PR: #456
- Commit: abc123
```

---

## 7. 版本发布流程

### 发布前检查清单

- [ ] 所有单元测试通过
- [ ] 代码审查完成
- [ ] 更新日志已更新
- [ ] 版本号已更新
- [ ] 文档已更新
- [ ] 数据库迁移脚本已准备

### 发布步骤

```bash
# 1. 从 develop 创建 release 分支
git checkout -b release/v1.0.0 develop

# 2. 更新版本号 (在代码中)
# 修改 build.gradle 中的 version

# 3. 更新 CHANGELOG.md
# 添加版本更新说明

# 4. 提交版本变更
git add .
git commit -m "chore: 准备 v1.0.0 发布"

# 5. 合并到 main
git checkout main
git merge --no-ff release/v1.0.0 -m "release: v1.0.0"

# 6. 创建标签
git tag -a v1.0.0 -m "Release version 1.0.0"

# 7. 合并回 develop
git checkout develop
git merge --no-ff release/v1.0.0

# 8. 删除 release 分支
git branch -d release/v1.0.0

# 9. 推送到远程
git push origin main
git push origin develop
git push origin v1.0.0
```

---

## 8. 紧急修复流程 (Hotfix)

```bash
# 1. 从 main 创建 hotfix 分支
git checkout -b hotfix/v1.0.1 main

# 2. 修复问题
# ... 修改代码 ...

# 3. 提交修复
git add .
git commit -m "fix: 修复 [问题描述]"

# 4. 合并到 main
git checkout main
git merge --no-ff hotfix/v1.0.1 -m "hotfix: v1.0.1"

# 5. 创建标签
git tag -a v1.0.1 -m "Hotfix version 1.0.1"

# 6. 合并到 develop
git checkout develop
git merge --no-ff hotfix/v1.0.1

# 7. 删除 hotfix 分支
git branch -d hotfix/v1.0.1

# 8. 推送
git push origin main
git push origin develop
git push origin v1.0.1
```

---

## 9. 版本兼容性矩阵

| 版本 | RPGCore | Paper | Java | 数据库 |
|------|---------|-------|------|--------|
| v1.0.0 | 1.0.0+ | 1.21.6 | 21 | MySQL 8.0 |
| v1.1.0 | 1.1.0+ | 1.21.6 | 21 | MySQL 8.0 |
| v2.0.0 | 2.0.0+ | 1.21.6+ | 21+ | MySQL 8.0+ |

---

## 10. 版本回滚策略

### 回滚命令

```bash
# 查看历史版本
git log --oneline

# 回滚到指定版本
git revert [commit-hash]

# 强制回滚 (谨慎使用)
git reset --hard [commit-hash]
git push origin main --force

# 使用标签回滚
git checkout v1.0.0
```

---

*最后更新: 2026-04-10*
*版本: 1.0.0*
