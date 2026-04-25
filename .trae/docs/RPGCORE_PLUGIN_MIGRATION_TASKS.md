# RPGCore 插件深度集成优化任务清单

> 基于 RPGCORE\_PLUGIN\_INTEGRATION\_REPORT.md 生成
> 生成日期: 2026-04-24
> 总任务数: 45个插件 × 多项优化 = 约120个子任务

***

## 📋 任务优先级定义

| 优先级 | 标识    | 说明             |
| --- | ----- | -------------- |
| P0  | 🔴 紧急 | 影响系统稳定性，必须立即修复 |
| P1  | 🟠 高  | 影响架构一致性，建议本周完成 |
| P2  | 🟡 中  | 影响代码质量，建议本月完成  |
| P3  | 🟢 低  | 优化建议，可排入后续迭代   |

***

## 阶段一：命令系统统一 (P0 - 紧急)

> 目标：所有插件迁移到 RPGCore CommandFramework，替代 Bukkit 命令注册

### 任务列表

| 任务ID    | 插件                   | 任务描述                 | 预估工作量 | 状态                       | <br /> | <br />                   |
| ------- | -------------------- | -------------------- | ----- | ------------------------ | :----- | :----------------------- |
| CMD-001 | GuangDianSignIn      | ✅ 已完成迁移              | 已完成   | ✅                        | <br /> | <br />                   |
| CMD-002 | GuangDianSocket      | ✅ 已完成迁移              | 已完成   | ✅                        | <br /> | <br />                   |
| CMD-003 | GuangDianMarket      | ✅ 已完成迁移              | 已完成   | ✅                        | <br /> | <br />                   |
| CMD-004 | GuangDianMenu        | ✅ 已完成迁移              | 已完成   | ✅                        | <br /> | <br />                   |
| CMD-005 | GuangDianClass       | ✅ 已完成迁移              | 已完成   | ✅                        | <br /> | <br />                   |
| CMD-006 | GuangDianMonthlyCard | 迁移到 CommandFramework | 2h    | ✅ 已完成 (2026-04-24 by AI) | <br /> | <br />                   |
| CMD-007 | GuangDianPoints      | ✅ 已完成迁移              | 已完成   | ✅                        | <br /> | ✅ 已完成 (2026-04-24 by AI) |
| CMD-008 | GuangDianChat        | 迁移到 CommandFramework | 2h    | ✅ 已完成 (2026-04-24 by AI) | <br /> | <br />                   |
| CMD-009 | GuangDianTab         | 迁移到 CommandFramework | 2h    | ✅ 已完成 (2026-04-24 by AI) | <br /> | <br />                   |
| CMD-010 | GuangDianGuild       | 迁移到 CommandFramework | 2h    | ✅ 已完成 (2026-04-24 by AI) | <br /> | <br />                   |
| CMD-011 | GuangDianTrade       | 迁移到 CommandFramework | 2h    | ✅ 已完成 (2026-04-24 by AI) | <br /> | <br />                   |
| CMD-012 | GuangDianName        | 迁移到 CommandFramework | 2h    | ✅ 已完成 (2026-04-24 by AI) | <br /> | <br />                   |
| CMD-013 | GuangDianCollection  | 迁移到 CommandFramework | 2h    | ✅ 已完成 (2026-04-24 by AI) | <br /> | <br />                   |
| CMD-014 | GuangDianForge       | 迁移到 CommandFramework | 2h    | ✅ 已完成 (2026-04-24 by AI) | <br /> | <br />                   |
| CMD-015 | GuangDianCaveFu      | 迁移到 CommandFramework | 2h    | ✅ 已完成 (2026-04-24 by AI) | <br /> | <br />                   |
| CMD-016 | GuangDianBoard       | 迁移到 CommandFramework | 2h    | ✅ 已完成 (2026-04-24 by AI) | <br /> | <br />                   |
| CMD-017 | GuangDianMobHealth   | 迁移到 CommandFramework | 2h    | ✅ 已完成 (2026-04-24 by AI) | <br /> | <br />                   |
| CMD-018 | GuangDianDecompose   | 迁移到 CommandFramework | 2h    | ✅ 已完成 (2026-04-24 by AI) | <br /> | <br />                   |
| CMD-019 | GuangDianDropControl | 迁移到 CommandFramework | 2h    | ✅ 已完成 (2026-04-24 by AI) | <br /> | <br />                   |
| CMD-020 | GuangDianCleaner     | 迁移到 CommandFramework | 2h    | ✅ 已完成 (2026-04-24 by AI) | <br /> | <br />                   |
| CMD-021 | GuangDianItemTrigger | 迁移到 CommandFramework | 2h    | ⬜                        | <br /> | <br />                   |
| CMD-022 | GuangDianLocation    | 迁移到 CommandFramework | 2h    | ⬜                        | <br /> | <br />                   |
| CMD-023 | GuangDianHolo        | 迁移到 CommandFramework | 2h    | ⬜                        | <br /> | <br />                   |
| CMD-024 | GuangDianGearScore   | 迁移到 CommandFramework | 2h    | ⬜                        | <br /> | <br />                   |
| CMD-025 | GuangDianBattlePass  | 迁移到 CommandFramework | 2h    | ⬜                        | <br /> | <br />                   |
| CMD-026 | GuangDianQuest       | 迁移到 CommandFramework | 2h    | ⬜                        | <br /> | <br />                   |
| CMD-027 | GuangDianWorld       | 迁移到 CommandFramework | 2h    | ⬜                        | <br /> | <br />                   |
| CMD-028 | GuangDianRaid        | 迁移到 CommandFramework | 2h    | ⬜                        | <br /> | <br />                   |
| CMD-029 | GuangDianMarriage    | 迁移到 CommandFramework | 2h    | ⬜                        | <br /> | <br />                   |
| CMD-030 | GuangDianSoulBind    | 迁移到 CommandFramework | 2h    | ⬜                        | <br /> | <br />                   |
| CMD-031 | GuangDianItemLabel   | 迁移到 CommandFramework | 2h    | ⬜                        | <br /> | <br />                   |
| CMD-032 | GuangDianChain       | 迁移到 CommandFramework | 2h    | ⬜                        | <br /> | <br />                   |
| CMD-033 | GuangDianAggro       | 迁移到 CommandFramework | 2h    | ⬜                        | <br /> | <br />                   |
| CMD-034 | GuangDianArmorStats  | 迁移到 CommandFramework | 2h    | ⬜                        | <br /> | <br />                   |
| CMD-035 | GuangDianAuth        | 迁移到 CommandFramework | 2h    | ⬜                        | <br /> | <br />                   |
| CMD-036 | GuangDianNPC         | 迁移到 CommandFramework | 2h    | ⬜                        | <br /> | <br />                   |
| CMD-037 | GuangDianBank        | 迁移到 CommandFramework | 2h    | ⬜                        | <br /> | <br />                   |
| CMD-038 | GuangDianGift        | 迁移到 CommandFramework | 2h    | ⬜                        | <br /> | <br />                   |
| CMD-039 | GuangDianAccessory   | 迁移到 CommandFramework | 2h    | ⬜                        | <br /> | <br />                   |

**阶段一总计**: 39个子任务 (5已完成, 34待分配)
**预计总工作量**: \~68小时

***

## 阶段二：并发保护 (P1 - 高优先级)

> 目标：为所有处理玩家数据的插件添加 PlayerLockManager 保护

### 任务列表

| 任务ID     | 插件                   | 任务描述                         | 预估工作量 | 状态 |
| -------- | -------------------- | ---------------------------- | ----- | -- |
| LOCK-001 | GuangDianPoints      | ✅ 已添加 PlayerLockManager      | 已完成   | ✅  |
| LOCK-002 | GuangDianSignIn      | 添加 PlayerLockManager 保护签到数据  | 3h    | ⬜  |
| LOCK-003 | GuangDianClass       | 添加 PlayerLockManager 保护职业数据  | 3h    | ⬜  |
| LOCK-004 | GuangDianMonthlyCard | 添加 PlayerLockManager 保护月卡数据  | 3h    | ⬜  |
| LOCK-005 | GuangDianMarket      | 添加 PlayerLockManager 保护交易数据  | 4h    | ⬜  |
| LOCK-006 | GuangDianTrade       | 添加 PlayerLockManager 保护交易数据  | 4h    | ⬜  |
| LOCK-007 | GuangDianGuild       | 添加 PlayerLockManager 保护公会数据  | 4h    | ⬜  |
| LOCK-008 | GuangDianCollection  | 添加 PlayerLockManager 保护图鉴数据  | 3h    | ⬜  |
| LOCK-009 | GuangDianQuest       | 添加 PlayerLockManager 保护任务数据  | 3h    | ⬜  |
| LOCK-010 | GuangDianBattlePass  | 添加 PlayerLockManager 保护通行证数据 | 3h    | ⬜  |
| LOCK-011 | GuangDianMarriage    | 添加 PlayerLockManager 保护婚姻数据  | 3h    | ⬜  |
| LOCK-012 | GuangDianForge       | 添加 PlayerLockManager 保护锻造数据  | 3h    | ⬜  |
| LOCK-013 | GuangDianBank        | 添加 PlayerLockManager 保护银行数据  | 3h    | ⬜  |
| LOCK-014 | GuangDianGift        | 添加 PlayerLockManager 保护礼物数据  | 2h    | ⬜  |
| LOCK-015 | GuangDianSoulBind    | 添加 PlayerLockManager 保护绑定数据  | 2h    | ⬜  |
| LOCK-016 | GuangDianAggro       | 添加 PlayerLockManager 保护仇恨数据  | 3h    | ⬜  |
| LOCK-017 | GuangDianName        | 添加 PlayerLockManager 保护名字数据  | 2h    | ⬜  |
| LOCK-018 | GuangDianGearScore   | 添加 PlayerLockManager 保护评分数据  | 2h    | ⬜  |
| LOCK-019 | GuangDianRaid        | 添加 PlayerLockManager 保护副本数据  | 3h    | ⬜  |
| LOCK-020 | GuangDianWorld       | 添加 PlayerLockManager 保护世界数据  | 3h    | ⬜  |

**阶段二总计**: 20个子任务 (1已完成, 19待分配)
**预计总工作量**: \~56小时

***

## 阶段三：缓存管理统一 (P1 - 高优先级)

> 目标：所有插件使用 CacheProvider 替代 ConcurrentHashMap

### 任务列表

| 任务ID      | 插件                   | 任务描述                | 预估工作量 | 状态 |
| --------- | -------------------- | ------------------- | ----- | -- |
| CACHE-001 | GuangDianArmorStats  | ✅ 已使用 CacheProvider | 已完成   | ✅  |
| CACHE-002 | GuangDianMarket      | 迁移到 CacheProvider   | 3h    | ⬜  |
| CACHE-003 | GuangDianMenu        | 迁移到 CacheProvider   | 2h    | ⬜  |
| CACHE-004 | GuangDianClass       | 迁移到 CacheProvider   | 3h    | ⬜  |
| CACHE-005 | GuangDianMonthlyCard | 迁移到 CacheProvider   | 2h    | ⬜  |
| CACHE-006 | GuangDianPoints      | 迁移到 CacheProvider   | 2h    | ⬜  |
| CACHE-007 | GuangDianSignIn      | 迁移到 CacheProvider   | 2h    | ⬜  |
| CACHE-008 | GuangDianGuild       | 迁移到 CacheProvider   | 3h    | ⬜  |
| CACHE-009 | GuangDianTrade       | 迁移到 CacheProvider   | 3h    | ⬜  |
| CACHE-010 | GuangDianCollection  | 迁移到 CacheProvider   | 3h    | ⬜  |
| CACHE-011 | GuangDianQuest       | 迁移到 CacheProvider   | 3h    | ⬜  |
| CACHE-012 | GuangDianBattlePass  | 迁移到 CacheProvider   | 3h    | ⬜  |
| CACHE-013 | GuangDianForge       | 迁移到 CacheProvider   | 2h    | ⬜  |
| CACHE-014 | GuangDianBank        | 迁移到 CacheProvider   | 2h    | ⬜  |
| CACHE-015 | GuangDianGift        | 迁移到 CacheProvider   | 2h    | ⬜  |
| CACHE-016 | GuangDianAggro       | 迁移到 CacheProvider   | 2h    | ⬜  |
| CACHE-017 | GuangDianName        | 迁移到 CacheProvider   | 2h    | ⬜  |
| CACHE-018 | GuangDianNPC         | 迁移到 CacheProvider   | 2h    | ⬜  |
| CACHE-019 | GuangDianHolo        | 迁移到 CacheProvider   | 2h    | ⬜  |
| CACHE-020 | GuangDianMobHealth   | 迁移到 CacheProvider   | 2h    | ⬜  |

**阶段三总计**: 20个子任务 (1已完成, 19待分配)
**预计总工作量**: \~50小时

***

## 阶段四：降级逻辑完善 (P2 - 中优先级)

> 目标：所有插件实现 RPGCore 不可用时的优雅降级

### 任务列表

| 任务ID         | 插件                   | 任务描述            | 预估工作量 | 状态 |
| ------------ | -------------------- | --------------- | ----- | -- |
| FALLBACK-001 | GuangDianSignIn      | ✅ 已实现降级逻辑       | 已完成   | ✅  |
| FALLBACK-002 | GuangDianSocket      | ✅ 已实现降级逻辑       | 已完成   | ✅  |
| FALLBACK-003 | GuangDianMarket      | 添加 RPGCore 降级逻辑 | 3h    | ⬜  |
| FALLBACK-004 | GuangDianMenu        | 添加 RPGCore 降级逻辑 | 3h    | ⬜  |
| FALLBACK-005 | GuangDianClass       | 添加 RPGCore 降级逻辑 | 3h    | ⬜  |
| FALLBACK-006 | GuangDianMonthlyCard | 添加 RPGCore 降级逻辑 | 3h    | ⬜  |
| FALLBACK-007 | GuangDianPoints      | 添加 RPGCore 降级逻辑 | 3h    | ⬜  |
| FALLBACK-008 | GuangDianArmorStats  | 添加 RPGCore 降级逻辑 | 3h    | ⬜  |
| FALLBACK-009 | GuangDianAuth        | 添加 RPGCore 降级逻辑 | 2h    | ⬜  |
| FALLBACK-010 | GuangDianAggro       | 添加 RPGCore 降级逻辑 | 2h    | ⬜  |
| FALLBACK-011 | GuangDianChat        | 添加 RPGCore 降级逻辑 | 2h    | ⬜  |
| FALLBACK-012 | GuangDianTab         | 添加 RPGCore 降级逻辑 | 2h    | ⬜  |
| FALLBACK-013 | GuangDianGuild       | 添加 RPGCore 降级逻辑 | 3h    | ⬜  |
| FALLBACK-014 | GuangDianTrade       | 添加 RPGCore 降级逻辑 | 3h    | ⬜  |
| FALLBACK-015 | GuangDianName        | 添加 RPGCore 降级逻辑 | 2h    | ⬜  |
| FALLBACK-016 | GuangDianCollection  | 添加 RPGCore 降级逻辑 | 2h    | ⬜  |
| FALLBACK-017 | GuangDianForge       | 添加 RPGCore 降级逻辑 | 2h    | ⬜  |
| FALLBACK-018 | GuangDianCaveFu      | 添加 RPGCore 降级逻辑 | 2h    | ⬜  |
| FALLBACK-019 | GuangDianBoard       | 添加 RPGCore 降级逻辑 | 2h    | ⬜  |
| FALLBACK-020 | GuangDianMobHealth   | 添加 RPGCore 降级逻辑 | 2h    | ⬜  |
| FALLBACK-021 | GuangDianDecompose   | 添加 RPGCore 降级逻辑 | 2h    | ⬜  |
| FALLBACK-022 | GuangDianDropControl | 添加 RPGCore 降级逻辑 | 2h    | ⬜  |
| FALLBACK-023 | GuangDianCleaner     | 添加 RPGCore 降级逻辑 | 2h    | ⬜  |
| FALLBACK-024 | GuangDianItemTrigger | 添加 RPGCore 降级逻辑 | 2h    | ⬜  |
| FALLBACK-025 | GuangDianLocation    | 添加 RPGCore 降级逻辑 | 2h    | ⬜  |
| FALLBACK-026 | GuangDianHolo        | 添加 RPGCore 降级逻辑 | 2h    | ⬜  |
| FALLBACK-027 | GuangDianGearScore   | 添加 RPGCore 降级逻辑 | 2h    | ⬜  |
| FALLBACK-028 | GuangDianBattlePass  | 添加 RPGCore 降级逻辑 | 3h    | ⬜  |
| FALLBACK-029 | GuangDianQuest       | 添加 RPGCore 降级逻辑 | 3h    | ⬜  |
| FALLBACK-030 | GuangDianWorld       | 添加 RPGCore 降级逻辑 | 3h    | ⬜  |
| FALLBACK-031 | GuangDianRaid        | 添加 RPGCore 降级逻辑 | 3h    | ⬜  |
| FALLBACK-032 | GuangDianMarriage    | 添加 RPGCore 降级逻辑 | 3h    | ⬜  |
| FALLBACK-033 | GuangDianSoulBind    | 添加 RPGCore 降级逻辑 | 2h    | ⬜  |
| FALLBACK-034 | GuangDianItemLabel   | 添加 RPGCore 降级逻辑 | 2h    | ⬜  |
| FALLBACK-035 | GuangDianChain       | 添加 RPGCore 降级逻辑 | 2h    | ⬜  |
| FALLBACK-036 | GuangDianNPC         | 添加 RPGCore 降级逻辑 | 2h    | ⬜  |
| FALLBACK-037 | GuangDianBank        | 添加 RPGCore 降级逻辑 | 2h    | ⬜  |
| FALLBACK-038 | GuangDianGift        | 添加 RPGCore 降级逻辑 | 2h    | ⬜  |
| FALLBACK-039 | GuangDianAccessory   | 添加 RPGCore 降级逻辑 | 2h    | ⬜  |

**阶段四总计**: 39个子任务 (2已完成, 37待分配)
**预计总工作量**: \~96小时

***

## 阶段五：GUI 系统统一 (P2 - 中优先级)

> 目标：所有菜单类插件使用 GUIManager

### 任务列表

| 任务ID    | 插件                   | 任务描述                   | 预估工作量 | 状态 |
| ------- | -------------------- | ---------------------- | ----- | -- |
| GUI-001 | GuangDianMenu        | ✅ 已使用 GUIManager       | 已完成   | ✅  |
| GUI-002 | GuangDianMarket      | 迁移到 GUIManager         | 4h    | ⬜  |
| GUI-003 | GuangDianMonthlyCard | 迁移到 GUIManager         | 4h    | ⬜  |
| GUI-004 | GuangDianClass       | 迁移到 GUIManager         | 4h    | ⬜  |
| GUI-005 | GuangDianTrade       | 迁移到 GUIManager         | 4h    | ⬜  |
| GUI-006 | GuangDianGuild       | 迁移到 GUIManager         | 4h    | ⬜  |
| GUI-007 | GuangDianBattlePass  | 迁移到 GUIManager         | 4h    | ⬜  |
| GUI-008 | GuangDianQuest       | 迁移到 GUIManager         | 4h    | ⬜  |
| GUI-009 | GuangDianCollection  | 迁移到 GUIManager         | 4h    | ⬜  |
| GUI-010 | GuangDianForge       | 迁移到 GUIManager         | 4h    | ⬜  |
| GUI-011 | GuangDianGift        | 迁移到 GUIManager         | 4h    | ⬜  |
| GUI-012 | GuangDianSignIn      | 迁移到 GUIManager         | 3h    | ⬜  |
| GUI-013 | GuangDianBank        | 迁移到 GUIManager         | 3h    | ⬜  |
| GUI-014 | GuangDianMarriage    | 迁移到 GUIManager         | 3h    | ⬜  |
| GUI-015 | GuangDianHolo        | 迁移到 TextDisplayService | 3h    | ⬜  |
| GUI-016 | GuangDianMobHealth   | 迁移到 TextDisplayService | 3h    | ⬜  |
| GUI-017 | GuangDianBoard       | 迁移到 BossBarService     | 3h    | ⬜  |
| GUI-018 | GuangDianTab         | 迁移到 TabService         | 3h    | ⬜  |

**阶段五总计**: 18个子任务 (1已完成, 17待分配)
**预计总工作量**: \~63小时

***

## 阶段六：日志与监控统一 (P3 - 低优先级)

> 目标：所有插件使用 GameLogger 和 PerformanceMonitor

### 任务列表

| 任务ID    | 插件                   | 任务描述             | 预估工作量 | 状态 |
| ------- | -------------------- | ---------------- | ----- | -- |
| LOG-001 | GuangDianAggro       | ✅ 已使用 GameLogger | 已完成   | ✅  |
| LOG-002 | GuangDianMarket      | 迁移到 GameLogger   | 2h    | ⬜  |
| LOG-003 | GuangDianMenu        | 迁移到 GameLogger   | 2h    | ⬜  |
| LOG-004 | GuangDianClass       | 迁移到 GameLogger   | 2h    | ⬜  |
| LOG-005 | GuangDianMonthlyCard | 迁移到 GameLogger   | 2h    | ⬜  |
| LOG-006 | GuangDianPoints      | 迁移到 GameLogger   | 2h    | ⬜  |
| LOG-007 | GuangDianSignIn      | 迁移到 GameLogger   | 2h    | ⬜  |
| LOG-008 | GuangDianSocket      | 迁移到 GameLogger   | 2h    | ⬜  |
| LOG-009 | GuangDianChat        | 迁移到 GameLogger   | 2h    | ⬜  |
| LOG-010 | GuangDianTab         | 迁移到 GameLogger   | 2h    | ⬜  |
| LOG-011 | GuangDianGuild       | 迁移到 GameLogger   | 2h    | ⬜  |
| LOG-012 | GuangDianTrade       | 迁移到 GameLogger   | 2h    | ⬜  |
| LOG-013 | GuangDianName        | 迁移到 GameLogger   | 2h    | ⬜  |
| LOG-014 | GuangDianCollection  | 迁移到 GameLogger   | 2h    | ⬜  |
| LOG-015 | GuangDianForge       | 迁移到 GameLogger   | 2h    | ⬜  |
| LOG-016 | GuangDianBattlePass  | 迁移到 GameLogger   | 2h    | ⬜  |
| LOG-017 | GuangDianQuest       | 迁移到 GameLogger   | 2h    | ⬜  |
| LOG-018 | GuangDianWorld       | 迁移到 GameLogger   | 2h    | ⬜  |
| LOG-019 | GuangDianRaid        | 迁移到 GameLogger   | 2h    | ⬜  |
| LOG-020 | GuangDianMarriage    | 迁移到 GameLogger   | 2h    | ⬜  |

**阶段六总计**: 20个子任务 (1已完成, 19待分配)
**预计总工作量**: \~38小时

***

## 阶段七：配置迁移支持 (P3 - 低优先级)

> 目标：为所有插件添加 ConfigMigrator 支持

### 任务列表

| 任务ID        | 插件                   | 任务描述              | 预估工作量 | 状态 |
| ----------- | -------------------- | ----------------- | ----- | -- |
| MIGRATE-001 | GuangDianMarket      | 添加 ConfigMigrator | 2h    | ⬜  |
| MIGRATE-002 | GuangDianMenu        | 添加 ConfigMigrator | 2h    | ⬜  |
| MIGRATE-003 | GuangDianClass       | 添加 ConfigMigrator | 2h    | ⬜  |
| MIGRATE-004 | GuangDianMonthlyCard | 添加 ConfigMigrator | 2h    | ⬜  |
| MIGRATE-005 | GuangDianPoints      | 添加 ConfigMigrator | 2h    | ⬜  |
| MIGRATE-006 | GuangDianSignIn      | 添加 ConfigMigrator | 2h    | ⬜  |
| MIGRATE-007 | GuangDianGuild       | 添加 ConfigMigrator | 2h    | ⬜  |
| MIGRATE-008 | GuangDianTrade       | 添加 ConfigMigrator | 2h    | ⬜  |
| MIGRATE-009 | GuangDianBattlePass  | 添加 ConfigMigrator | 2h    | ⬜  |
| MIGRATE-010 | GuangDianQuest       | 添加 ConfigMigrator | 2h    | ⬜  |

**阶段七总计**: 10个子任务 (0已完成, 10待分配)
**预计总工作量**: \~20小时

***

## 阶段八：事件系统集成 (P3 - 低优先级)

> 目标：所有插件使用 EventBus 进行跨插件通信

### 任务列表

| 任务ID      | 插件                  | 任务描述                | 预估工作量 | 状态 |
| --------- | ------------------- | ------------------- | ----- | -- |
| EVENT-001 | GuangDianMarket     | 使用 EventBus 发布交易事件  | 2h    | ⬜  |
| EVENT-002 | GuangDianClass      | 使用 EventBus 发布职业事件  | 2h    | ⬜  |
| EVENT-003 | GuangDianGuild      | 使用 EventBus 发布公会事件  | 2h    | ⬜  |
| EVENT-004 | GuangDianTrade      | 使用 EventBus 发布交易事件  | 2h    | ⬜  |
| EVENT-005 | GuangDianMarriage   | 使用 EventBus 发布婚姻事件  | 2h    | ⬜  |
| EVENT-006 | GuangDianQuest      | 使用 EventBus 发布任务事件  | 2h    | ⬜  |
| EVENT-007 | GuangDianBattlePass | 使用 EventBus 发布通行证事件 | 2h    | ⬜  |
| EVENT-008 | GuangDianRaid       | 使用 EventBus 发布副本事件  | 2h    | ⬜  |

**阶段八总计**: 8个子任务 (0已完成, 8待分配)
**预计总工作量**: \~16小时

***

## 📊 任务汇总统计

| 阶段         | 任务数     | 已完成   | 待分配     | 预计工时     |
| ---------- | ------- | ----- | ------- | -------- |
| 一、命令系统统一   | 39      | 2     | 37      | 74h      |
| 二、并发保护     | 20      | 1     | 19      | 56h      |
| 三、缓存管理统一   | 20      | 1     | 19      | 50h      |
| 四、降级逻辑完善   | 39      | 2     | 37      | 96h      |
| 五、GUI 系统统一 | 18      | 1     | 17      | 63h      |
| 六、日志与监控统一  | 20      | 1     | 19      | 38h      |
| 七、配置迁移支持   | 10      | 0     | 10      | 20h      |
| 八、事件系统集成   | 8       | 0     | 8       | 16h      |
| **总计**     | **174** | **8** | **166** | **413h** |

***

## 🎯 任务分配建议

### 按优先级分配

#### P0 紧急 (阶段一)

- **建议**: 分配给 3-4 名开发人员
- **周期**: 1周
- **每日进度**: 5-6个插件

#### P1 高 (阶段二、三)

- **建议**: 分配给 2-3 名开发人员
- **周期**: 2周
- **每日进度**: 3-4个插件

#### P2 中 (阶段四、五)

- **建议**: 分配给 2 名开发人员
- **周期**: 3周
- **每日进度**: 3-4个插件

#### P3 低 (阶段六、七、八)

- **建议**: 分配给 1-2 名开发人员
- **周期**: 4周
- **每日进度**: 2-3个插件

### 按插件类型分配

#### A组：核心业务插件 (优先处理)

- GuangDianMarket (商城)
- GuangDianMenu (菜单)
- GuangDianClass (职业)
- GuangDianMonthlyCard (月卡)
- GuangDianPoints (积分)
- GuangDianTrade (交易)

#### B组：社交互动插件

- GuangDianGuild (公会)
- GuangDianMarriage (结婚)
- GuangDianChat (聊天)
- GuangDianTab (Tab列表)

#### C组：游戏内容插件

- GuangDianQuest (任务)
- GuangDianBattlePass (通行证)
- GuangDianRaid (副本)
- GuangDianWorld (世界)

#### D组：功能辅助插件

- GuangDianSignIn (签到)
- GuangDianSocket (宝石)
- GuangDianForge (锻造)
- GuangDianCollection (图鉴)

***

## 📝 任务分配模板

### 单个插件优化检查清单

对于每个插件，需要完成以下检查项：

```
插件名称: GuangDianXXX
分配给: [开发者]
开始日期: [日期]
预计完成: [日期]

命令系统迁移:
☐ 添加 CommandFramework 依赖
☐ 创建 XXXCommand 类
☐ 在 onPluginEnable() 中注册命令
☐ 实现降级逻辑（当 CommandFramework 不可用时）
☐ 测试命令功能正常

并发保护:
☐ 识别需要保护的数据
☐ 添加 PlayerLockManager 调用
☐ 测试并发安全性

缓存管理:
☐ 识别 ConcurrentHashMap 使用
☐ 迁移到 CacheProvider
☐ 配置合适的 TTL
☐ 测试缓存功能正常

降级逻辑:
☐ 实现 isRPGCoreAvailable() 检查
☐ 为每个 RPGCore 服务提供降级方案
☐ 测试 RPGCore 不可用时插件仍可运行

GUI 统一:
☐ 识别自定义 GUI 实现
☐ 迁移到 GUIManager 或 TextDisplayService
☐ 测试菜单功能正常

日志统一:
☐ 替换 Bukkit.getLogger() 为 GameLogger
☐ 添加 PerformanceMonitor 关键操作监控

配置迁移:
☐ 添加 config-version 字段
☐ 实现 ConfigMigrator 支持

事件系统:
☐ 识别需要发布的事件
☐ 使用 EventBus 替代 Bukkit 事件

构建验证:
☐ 编译通过
☐ 无警告
☐ 功能测试通过
```

***

## ✅ 验收标准

### 代码质量

- [ ] 无编译错误
- [ ] 无 Deprecated API 警告
- [ ] 无 FORBIDDEN\_PATTERNS.md 中禁止的模式
- [ ] 通过代码审查

### 功能测试

- [ ] 所有命令正常工作
- [ ] 数据读写正常工作
- [ ] 并发安全测试通过
- [ ] 缓存命中/失效正常

### 集成测试

- [ ] RPGCore 可用时所有功能正常
- [ ] RPGCore 不可用时降级功能正常
- [ ] 与其他插件兼容性好

***

## 📅 里程碑规划

| 里程碑          | 完成标准      | 预计日期    |
| ------------ | --------- | ------- |
| M1: 命令系统迁移完成 | 阶段一 100%  | Week 1  |
| M2: 并发保护完成   | 阶段二 100%  | Week 3  |
| M3: 缓存管理完成   | 阶段三 100%  | Week 3  |
| M4: 降级逻辑完成   | 阶段四 100%  | Week 6  |
| M5: GUI 统一完成 | 阶段五 100%  | Week 7  |
| M6: 全面优化完成   | 所有阶段 100% | Week 10 |

***

*文档生成时间: 2026-04-24*
*基于: RPGCORE\_PLUGIN\_INTEGRATION\_REPORT.md*
*下次更新: 完成 M1 里程碑后*
