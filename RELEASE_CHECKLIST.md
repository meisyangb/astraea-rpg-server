# 版本发布检查清单

> Astraea RPG 版本发布前必须完成的检查项

---

## 📋 发布前检查清单

### 代码质量

- [ ] **所有单元测试通过**
  ```bash
  gradle test
  ```

- [ ] **代码静态检查通过**
  ```bash
  gradle checkstyle
  ```

- [ ] **无编译错误**
  ```bash
  gradle build
  ```

- [ ] **代码审查完成**
  - [ ] 至少 1 人审查通过
  - [ ] 无未解决的审查意见

### 版本管理

- [ ] **版本号已更新**
  - [ ] `build.gradle` 中的 version
  - [ ] `CHANGELOG.md` 中的版本号
  - [ ] 文档中的版本号

- [ ] **CHANGELOG 已更新**
  - [ ] 添加了新版本说明
  - [ ] 列出了所有变更
  - [ ] 标注了破坏性变更

- [ ] **Git 标签已创建**
  ```bash
  git tag -a v[版本号] -m "Release version [版本号]"
  ```

### 文档

- [ ] **API 文档已更新**
  - [ ] 新接口已文档化
  - [ ] 废弃接口已标注

- [ ] **README 已更新**
  - [ ] 版本号已更新
  - [ ] 新功能已说明

- [ ] **部署文档已更新**
  - [ ] 部署步骤已验证
  - [ ] 配置说明已更新

### 数据库

- [ ] **数据库迁移脚本已准备**
  - [ ] 升级脚本已测试
  - [ ] 回滚脚本已准备

- [ ] **数据兼容性已验证**
  - [ ] 旧数据可以正常迁移
  - [ ] 无数据丢失风险

### 测试

- [ ] **集成测试通过**
  - [ ] 所有插件可以正常加载
  - [ ] 插件间通信正常

- [ ] **性能测试通过**
  - [ ] 内存使用正常
  - [ ] CPU 使用正常
  - [ ] 无内存泄漏

- [ ] **兼容性测试通过**
  - [ ] 与 Paper 版本兼容
  - [ ] 与 MythicMobs 兼容
  - [ ] 与其他插件兼容

### 安全

- [ ] **安全审查完成**
  - [ ] 无 SQL 注入风险
  - [ ] 无命令注入风险
  - [ ] 权限检查正确

- [ ] **敏感信息已移除**
  - [ ] 无硬编码密码
  - [ ] 无调试代码
  - [ ] 无测试数据

---

## 🚀 发布流程

### 1. 准备阶段

```bash
# 1.1 确保代码最新
git checkout develop
git pull origin develop

# 1.2 创建 release 分支
git checkout -b release/v[版本号] develop

# 1.3 更新版本号
# 修改 build.gradle 中的 version = '[版本号]'

# 1.4 更新 CHANGELOG
# 在 CHANGELOG.md 添加新版本说明
```

### 2. 验证阶段

```bash
# 2.1 运行测试
gradle clean build test

# 2.2 代码检查
gradle checkstyle

# 2.3 构建发布包
gradle build -x test
```

### 3. 合并阶段

```bash
# 3.1 提交版本变更
git add .
git commit -m "chore: 准备 v[版本号] 发布"

# 3.2 合并到 main
git checkout main
git merge --no-ff release/v[版本号] -m "release: v[版本号]"

# 3.3 创建标签
git tag -a v[版本号] -m "Release version [版本号]"

# 3.4 合并回 develop
git checkout develop
git merge --no-ff release/v[版本号]

# 3.5 删除 release 分支
git branch -d release/v[版本号]
```

### 4. 推送阶段

```bash
# 4.1 推送代码
git push origin main
git push origin develop
git push origin v[版本号]
```

### 5. 部署阶段

```bash
# 5.1 部署到测试环境
./scripts/deploy.ps1 -Environment test -Version [版本号]

# 5.2 验证测试环境
# ... 手动验证 ...

# 5.3 部署到生产环境
./scripts/deploy.ps1 -Environment prod -Version [版本号]
```

---

## 📝 发布后检查

### 验证清单

- [ ] **服务正常启动**
  - [ ] 无启动错误
  - [ ] 所有插件加载成功

- [ ] **核心功能正常**
  - [ ] 玩家可以登录
  - [ ] 基础指令可用
  - [ ] 数据保存正常

- [ ] **监控正常**
  - [ ] 无异常日志
  - [ ] 性能指标正常
  - [ ] 内存使用正常

- [ ] **玩家反馈**
  - [ ] 无严重问题反馈
  - [ ] 新功能工作正常

---

## 🔄 回滚准备

### 回滚检查清单

- [ ] **上一版本标签存在**
  ```bash
  git tag | grep v[上一版本号]
  ```

- [ ] **数据库备份已创建**
  ```bash
  # 备份命令
  ```

- [ ] **回滚步骤已准备**
  - [ ] 代码回滚步骤
  - [ ] 数据库回滚步骤
  - [ ] 配置回滚步骤

### 回滚命令

```bash
# 1. 回滚代码
git checkout v[上一版本号]

# 2. 重新部署
./scripts/deploy.ps1 -Environment prod -Version [上一版本号]

# 3. 回滚数据库
# 执行回滚脚本
```

---

## 📊 发布记录

| 版本 | 日期 | 发布人 | 状态 | 备注 |
|------|------|--------|------|------|
| v0.1.0 | 2026-04-10 | - | 🟡 准备中 | 初始版本 |

---

*最后更新: 2026-04-10*
