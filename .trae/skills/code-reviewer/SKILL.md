---
name: code-reviewer
description: "代码审查专家 - 审查代码质量、规范符合度、潜在Bug、性能问题。在代码提交前或用户请求审查时调用。"
---

# 代码审查专家 (Code Reviewer)

> 专业的代码审查技能，确保代码质量和规范符合度

---

## 审查维度

### 1. 规范符合度检查
- [ ] 是否遵循项目编码规范
- [ ] 命名是否符合约定（类名大驼峰、方法小驼峰、常量全大写）
- [ ] 缩进和格式是否统一
- [ ] 是否使用正确的API（参考 FORBIDDEN PATTERNS）

### 2. 代码质量检查
- [ ] 是否存在重复代码（DRY原则）
- [ ] 方法是否过长（超过50行需要拆分）
- [ ] 类是否过大（超过500行需要重构）
- [ ] 参数是否过多（超过5个考虑使用对象封装）
- [ ] 是否存在魔法数字（应使用常量）

### 3. 潜在Bug检查
- [ ] 空指针风险（是否做null检查）
- [ ] 资源泄漏（数据库连接、文件流是否关闭）
- [ ] 并发问题（线程安全、竞态条件）
- [ ] 异常处理（是否捕获特定异常而非Exception）
- [ ] 边界条件（数组越界、除零等）

### 4. 性能检查
- [ ] 是否存在循环中的数据库查询
- [ ] 是否存在不必要的对象创建
- [ ] 集合选择是否合适（List/Set/Map）
- [ ] 字符串拼接是否使用StringBuilder
- [ ] 是否使用了缓存机制

### 5. 可读性检查
- [ ] 是否有适当的注释（复杂逻辑、公共API）
- [ ] 方法名是否清晰表达意图
- [ ] 变量名是否有意义（避免a, b, tmp等）
- [ ] 代码结构是否清晰

---

## 审查流程

```
1. 获取待审查代码
2. 静态分析（规范、格式）
3. 逻辑分析（Bug、性能）
4. 生成审查报告
5. 提供改进建议
```

---

## 输出格式

```
╔══════════════════════════════════════════════════════════╗
║  🔍 代码审查报告                                         ║
╠══════════════════════════════════════════════════════════╣
║  文件: [文件名]                                          ║
║  审查时间: [时间]                                        ║
╠══════════════════════════════════════════════════════════╣
║  严重问题 (Critical): [数量]                             ║
║  警告 (Warning): [数量]                                  ║
║  建议 (Suggestion): [数量]                               ║
╠══════════════════════════════════════════════════════════╣
║  详细问题列表:                                           ║
║                                                          ║
║  [严重] 行号: 问题描述                                   ║
║    → 建议: 如何修复                                      ║
║    → 示例: 正确代码示例                                  ║
║                                                          ║
║  [警告] 行号: 问题描述                                   ║
║    → 建议: 改进方案                                      ║
║                                                          ║
║  [建议] 行号: 问题描述                                   ║
║    → 建议: 可选改进                                      ║
╠══════════════════════════════════════════════════════════╣
║  审查结论: [通过/需修改/需重构]                          ║
╚══════════════════════════════════════════════════════════╝
```

---

## 审查示例

### 示例1: 调度器使用审查

**待审查代码:**
```java
Bukkit.getScheduler().runTaskLater(this, () -> {
    player.sendMessage("Hello");
}, 20L);
```

**审查结果:**
```
[严重] 第1行: 使用了禁止的 Bukkit.getScheduler()
  → 建议: 使用 RPGCore.getInstance().getScheduler()
  → 示例:
    RPGCore rpgCore = RPGCore.getInstance();
    if (rpgCore != null) {
        rpgCore.getScheduler().runSyncLater(() -> {
            player.sendMessage("Hello");
        }, 20L);
    }
```

### 示例2: 空指针风险审查

**待审查代码:**
```java
public void onPlayerJoin(Player player) {
    String name = player.getName();
    player.sendMessage("Welcome " + name);
}
```

**审查结果:**
```
[警告] 第2-3行: 未做null检查
  → 建议: 添加null检查防止空指针
  → 示例:
    public void onPlayerJoin(Player player) {
        if (player == null || !player.isOnline()) {
            return;
        }
        String name = player.getName();
        player.sendMessage("Welcome " + name);
    }
```

---

## 与 minecraft-rpg-architect 协作

当审查 RPG 插件代码时，需要额外检查：
- [ ] 是否继承 AbstractRPGPlugin 而非 JavaPlugin
- [ ] 是否正确使用 SyncScheduler
- [ ] 是否正确获取 RPGCore 实例
- [ ] 是否正确使用 ExternalServiceIntegration
- [ ] 是否正确注销服务和占位符

---

*技能版本: 1.0*
*最后更新: 2026-04-10*
