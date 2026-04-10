# 🤝 贡献指南

感谢您考虑为 Astraea RPG 做出贡献！本文档将帮助您了解如何参与项目开发。

---

## 📑 目录

- [行为准则](#行为准则)
- [如何贡献](#如何贡献)
- [开发环境设置](#开发环境设置)
- [代码规范](#代码规范)
- [提交规范](#提交规范)
- [分支策略](#分支策略)
- [Pull Request 流程](#pull-request-流程)
- [问题报告](#问题报告)
- [功能建议](#功能建议)

---

## 行为准则

### 我们的承诺

为了营造一个开放和友好的环境，我们作为贡献者和维护者承诺：无论年龄、体型、残疾、种族、性别认同和表达、经验水平、教育程度、社会经济地位、国籍、外貌、种族、宗教或性取向如何，参与我们的项目和社区都将为每个人提供无骚扰的体验。

### 我们的标准

**积极行为示例：**

- 使用友好和包容的语言
- 尊重不同的观点和经验
- 优雅地接受建设性批评
- 关注对社区最有利的事情
- 对其他社区成员表示同理心

**不可接受的行为示例：**

- 使用性化的语言或图像，以及不受欢迎的性关注或性骚扰
- 捣乱、侮辱/贬损评论以及人身或政治攻击
- 公开或私下骚扰
- 未经明确许可，发布他人的私人信息
- 其他在专业环境中可能被合理认为不适当的行为

---

## 如何贡献

### 贡献方式

您可以通过以下方式为项目做出贡献：

1. **报告 Bug** - 提交 Issue 描述问题
2. **建议功能** - 提交 Issue 描述新功能
3. **改进文档** - 修复错别字、改进说明
4. **提交代码** - 修复 Bug 或实现新功能
5. **代码审查** - 帮助审查 Pull Request

### 贡献流程

```
1. Fork 仓库
     ↓
2. 创建分支 (feature/xxx 或 fix/xxx)
     ↓
3. 进行更改
     ↓
4. 提交更改 (遵循提交规范)
     ↓
5. 推送到 Fork 仓库
     ↓
6. 创建 Pull Request
     ↓
7. 等待代码审查
     ↓
8. 合并到主仓库
```

---

## 开发环境设置

### 必需软件

| 软件 | 版本 | 说明 |
|------|------|------|
| JDK | 21+ | Eclipse Temurin 或 Oracle JDK |
| Gradle | 9.4.0 | 构建工具 |
| Git | 最新版 | 版本控制 |
| IDE | - | IntelliJ IDEA (推荐) 或 Eclipse |

### 克隆仓库

```powershell
# Fork 后克隆您的仓库
git clone https://github.com/YOUR_USERNAME/astraea-rpg-server.git
cd astraea-rpg-server

# 添加上游仓库
git remote add upstream https://github.com/meisyangb/astraea-rpg-server.git
```

### 构建项目

```powershell
# 设置 JAVA_HOME
$env:JAVA_HOME="e:\原创RPG服务端\tools\jdk-21.0.10+7"

# 构建所有插件
& "D:\gradle\gradle-9.4.0\bin\gradle.bat" build --no-configuration-cache -x test
```

### IDE 配置

#### IntelliJ IDEA

1. 打开 `File → Settings → Build, Execution, Deployment → Build Tools → Gradle`
2. 设置 `Gradle JVM` 为 JDK 21
3. 启用 `Annotation Processors`

#### Eclipse

1. 安装 Buildship Gradle 插件
2. 导入项目时选择 Gradle 项目
3. 设置 JRE 为 JDK 21

---

## 代码规范

### Java 代码规范

#### 命名规范

| 类型 | 规范 | 示例 |
|------|------|------|
| 类名 | PascalCase | `PlayerManager` |
| 方法名 | camelCase | `getPlayerData()` |
| 变量名 | camelCase | `playerName` |
| 常量名 | UPPER_SNAKE_CASE | `MAX_PLAYERS` |
| 包名 | 小写 | `cn.guangdian.armorstats` |

#### 代码格式

```java
public class ExampleClass {
    
    private static final int MAX_SIZE = 100;
    private final String name;
    
    public ExampleClass(String name) {
        this.name = name;
    }
    
    public String getName() {
        return name;
    }
    
    public void processData(Player player) {
        if (player == null) {
            return;
        }
        
        String playerName = player.getName();
        int level = player.getLevel();
        
        if (level > MAX_SIZE) {
            handleHighLevel(player);
        }
    }
    
    private void handleHighLevel(Player player) {
        // 处理逻辑
    }
}
```

### 必须遵守的规则

#### 1. 插件主类

```java
// ✅ 正确
public class MyPlugin extends AbstractRPGPlugin {
    @Override
    protected void onPluginEnable() {
        // 初始化逻辑
    }
    
    @Override
    protected void onPluginDisable() {
        // 清理逻辑
    }
    
    @Override
    protected String getPluginName() {
        return "MyPlugin";
    }
}

// ❌ 禁止
public class MyPlugin extends JavaPlugin { }
```

#### 2. 调度器

```java
// ✅ 正确
SyncScheduler scheduler = RPGCore.getInstance().getScheduler();
scheduler.runSyncLater(() -> { }, 20L);
scheduler.runSyncRepeating(() -> { }, 0L, 20L);
scheduler.runAsync(() -> { });

// ❌ 禁止
new BukkitRunnable() { }.runTaskTimer(plugin, delay, period);
Bukkit.getScheduler().runTaskLater(plugin, task, delay);
```

#### 3. 外部服务

```java
// ✅ 正确
ExternalServiceIntegration external = RPGCore.getInstance().getExternalServices();
String prefix = external.getPlayerPrefix(player);
String parsed = external.parsePlaceholders(player, text);

// ❌ 禁止
LuckPermsProvider.get().getUserManager().getUser(uuid);
PlaceholderAPI.setPlaceholders(player, text);
```

#### 4. 消息发送

```java
// ✅ 正确
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;

player.sendMessage(Component.text("消息").color(NamedTextColor.GREEN));

// ❌ 禁止
player.sendMessage(ChatColor.GREEN + "消息");
player.sendMessage("§a消息");
```

### 注释规范

```java
/**
 * 玩家数据管理器
 * <p>
 * 负责玩家数据的加载、缓存和保存
 * </p>
 *
 * @author Astraea RPG Team
 * @since 1.0.0
 */
public class PlayerDataManager {
    
    /**
     * 获取玩家数据
     *
     * @param uuid 玩家 UUID
     * @return 玩家数据，如果不存在返回 null
     */
    public PlayerData getPlayerData(UUID uuid) {
        // 实现
    }
    
    /**
     * 保存玩家数据
     * <p>
     * 此方法会异步保存数据到数据库
     * </p>
     *
     * @param data 玩家数据
     * @throws IllegalArgumentException 如果 data 为 null
     */
    public void savePlayerData(PlayerData data) {
        // 实现
    }
}
```

---

## 提交规范

### 提交消息格式

```
[类型]: [简要描述]

[详细描述]

[关联Issue]
```

### 类型标识

| 类型 | 说明 | 示例 |
|------|------|------|
| `feat` | 新功能 | `feat: 添加玩家数据缓存系统` |
| `fix` | Bug修复 | `fix: 修复占位符显示问题` |
| `docs` | 文档更新 | `docs: 更新 README.md` |
| `style` | 代码格式（不影响功能） | `style: 格式化代码` |
| `refactor` | 重构（不是新功能也不是修复） | `refactor: 重构调度器` |
| `perf` | 性能优化 | `perf: 优化缓存性能` |
| `test` | 添加测试 | `test: 添加单元测试` |
| `chore` | 构建过程或辅助工具变动 | `chore: 更新 Gradle 配置` |

### 提交示例

#### 新功能

```
feat: 添加玩家数据缓存系统

- 实现 TTLCacheManager 用于自动过期缓存
- 添加缓存统计功能，监控命中率
- 集成到 PlayerLifecycleManager
- 添加配置选项控制缓存大小和过期时间

Closes #123
```

#### Bug 修复

```
fix: 修复记分板占位符显示问题

问题：
- config.yml 文件包含 BOM 导致 YAML 解析失败
- PlaceholderAPI 在 RPGCore 加载时未被检测到

修复：
- 移除 config.yml 的 BOM
- 添加 refreshPlaceholderAPI() 方法支持运行时重新检测
- 调整占位符处理顺序，先本地替换再 PlaceholderAPI 解析

Fixes #456
```

#### 文档更新

```
docs: 更新 README.md 安装指南

- 添加详细的构建步骤
- 添加环境要求说明
- 添加常见问题解答
```

---

## 分支策略

### 分支类型

```
main                    # 生产环境，稳定版本
  ↑
develop                 # 开发集成分支
  ↑
├── feature/xxx         # 新功能开发
├── fix/xxx             # Bug 修复
├── release/x.x.x       # 版本发布准备
└── hotfix/xxx          # 紧急修复
```

### 分支命名规范

| 分支类型 | 命名格式 | 示例 |
|----------|----------|------|
| 功能 | `feature/功能名称` | `feature/player-cache` |
| 修复 | `fix/问题描述` | `fix/placeholder-display` |
| 发布 | `release/版本号` | `release/1.1.0` |
| 热修复 | `hotfix/问题描述` | `hotfix/critical-bug` |

### 分支工作流

```powershell
# 1. 从 develop 创建功能分支
git checkout develop
git pull upstream develop
git checkout -b feature/my-feature

# 2. 进行开发...
git add .
git commit -m "feat: 添加新功能"

# 3. 保持与上游同步
git fetch upstream
git rebase upstream/develop

# 4. 推送到您的 Fork
git push origin feature/my-feature

# 5. 创建 Pull Request
```

---

## Pull Request 流程

### 提交前检查清单

- [ ] 代码遵循项目代码规范
- [ ] 新插件主类继承 `AbstractRPGPlugin`
- [ ] 使用 `SyncScheduler` 而非 `Bukkit.getScheduler()`
- [ ] 通过 `ExternalServiceIntegration` 访问外部服务
- [ ] 使用 `Adventure API` 而非 `ChatColor`
- [ ] 插件卸载时取消所有调度任务
- [ ] 添加适当的日志记录
- [ ] 更新相关文档
- [ ] 提交消息遵循规范

### 创建 Pull Request

1. **确保分支是最新的**

```powershell
git fetch upstream
git rebase upstream/develop
```

2. **推送分支**

```powershell
git push origin feature/my-feature
```

3. **在 GitHub 上创建 Pull Request**

- 访问您的 Fork 页面
- 点击 "New Pull Request"
- 选择正确的分支
- 填写 PR 描述

### PR 描述模板

```markdown
## 变更类型
- [ ] Bug 修复
- [ ] 新功能
- [ ] 重构
- [ ] 文档更新
- [ ] 其他

## 变更说明
<!-- 详细描述您的变更 -->

## 关联 Issue
<!-- 关联的 Issue 编号，如 Closes #123 -->

## 测试说明
<!-- 如何测试这些变更 -->

## 截图（如适用）
<!-- 如果是 UI 变更，请提供截图 -->

## 检查清单
- [ ] 代码遵循项目规范
- [ ] 已添加必要的注释
- [ ] 已更新相关文档
- [ ] 已测试变更
```

### 代码审查

所有 Pull Request 都需要至少一位维护者审查后才能合并。审查过程中：

1. **响应审查意见**
   - 及时回复审查者的评论
   - 按建议修改代码
   - 解释您不同意的地方

2. **保持 PR 简洁**
   - 一个 PR 只做一件事
   - 避免不必要的变更
   - 保持提交历史清晰

3. **通过 CI 检查**
   - 确保所有测试通过
   - 解决代码风格问题

---

## 问题报告

### 报告 Bug

如果您发现了 Bug，请创建 Issue 并包含以下信息：

```markdown
## Bug 描述
<!-- 清晰简洁地描述这个 Bug -->

## 复现步骤
1. 进入 '...'
2. 点击 '...'
3. 滚动到 '...'
4. 看到错误

## 预期行为
<!-- 描述您期望发生什么 -->

## 实际行为
<!-- 描述实际发生了什么 -->

## 截图
<!-- 如果适用，添加截图帮助解释问题 -->

## 环境信息
- 服务端版本: [如 Paper 1.21.6-123]
- 插件版本: [如 1.0.0]
- Java 版本: [如 21.0.1]
- 其他相关插件: [如 PlaceholderAPI 2.11.6]

## 日志
<!-- 粘贴相关的日志片段 -->
```

### Bug 报告标签

| 标签 | 说明 |
|------|------|
| `bug` | 确认的 Bug |
| `critical` | 严重 Bug，需要立即修复 |
| `help wanted` | 需要社区帮助 |
| `wontfix` | 不会修复的问题 |

---

## 功能建议

### 提交建议

如果您有功能建议，请创建 Issue 并包含以下信息：

```markdown
## 功能描述
<!-- 清晰简洁地描述您希望添加的功能 -->

## 问题背景
<!-- 描述这个功能要解决什么问题 -->

## 建议方案
<!-- 描述您建议的解决方案 -->

## 替代方案
<!-- 描述您考虑过的其他方案 -->

## 附加信息
<!-- 其他相关信息或截图 -->
```

### 功能建议标签

| 标签 | 说明 |
|------|------|
| `enhancement` | 功能增强 |
| `feature` | 新功能请求 |
| `discussion` | 需要讨论 |
| `good first issue` | 适合新贡献者 |

---

## 获取帮助

如果您在贡献过程中遇到问题，可以：

1. 查看 [Wiki](https://github.com/meisyangb/astraea-rpg-server/wiki)
2. 在 [Discussions](https://github.com/meisyangb/astraea-rpg-server/discussions) 提问
3. 查看现有的 [Issues](https://github.com/meisyangb/astraea-rpg-server/issues)

---

## 许可证

通过贡献代码，您同意您的代码将按照项目的 [MIT License](LICENSE) 进行许可。

---

**感谢您的贡献！** 🎉

*Astraea RPG Team*
