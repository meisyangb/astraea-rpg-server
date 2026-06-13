# GuangDianArmorStats 逻辑问题深度分析

## 🔴 严重问题

### 1. **技能列表未合并到 playerSkillsMap**
**位置**: `StatsManager.java` - `mergeStats()` 和 `mergeSkills()`

**问题**:
```java
private PlayerStats mergeStats(UUID uuid) {
    // ... 合并属性
    // ❌ 但是没有调用 mergeSkills()！
}

private List<String> mergeSkills(UUID uuid) {
    // 这个方法定义了但从未被调用
}
```

**影响**: 玩家的技能列表永远不会更新，`/armorstats skill` 命令无法显示技能

**场景**:
- 玩家穿戴带技能的装备
- 属性正常生效，但技能列表为空
- 无法触发主动技能

---

### 2. **FullEquipmentManager 无法访问 addItemAttributes**
**位置**: `FullEquipmentManager.java` line 40

**问题**:
```java
// FullEquipmentManager.java
private PlayerStats parseItemAttributes(ItemStack item) {
    PlayerStats stats = new PlayerStats();
    statsManager.addItemAttributes(stats, null, item);  // ❌ private 方法无法访问
    return stats;
}
```

**影响**: `FullEquipmentManager` 无法正常工作，虽然目前未被使用

---

### 3. **副手装备变更未被监听**
**位置**: `EventListeners.java`

**问题**: 
- `onInventoryClick` 只检查了副手槽位 40
- 但没有监听副手装备的实际变更
- `onPlayerItemHeld` 只监听主手切换

**场景**:
- 玩家在副手装备盾牌/图腾/其他物品
- 如果副手物品有属性，不会被解析
- 副手装备变更不会触发属性刷新

---

### 4. **宝石镶嵌后属性未立即生效**
**位置**: `GemInlayGUI.java` - `confirmInlay()`

**问题**:
```java
private void confirmInlay() {
    // ... 镶嵌宝石
    giveOrDrop(finalItem);
    clearSession();
    finalized = true;
    player.sendMessage(ChatColor.GREEN + "宝石镶嵌成功");
    player.closeInventory();

    // ❌ 延迟1tick刷新，但此时装备可能还在玩家手上或背包里
    Bukkit.getScheduler().runTaskLater(GuangDianArmorStats.getInstance(), () -> {
        GuangDianArmorStats.getInstance().getStatsManager().refreshPlayerStats(player);
        // ...
    }, 1L);
}
```

**场景**:
1. 玩家打开宝石GUI
2. 放入装备（从防具槽取下）
3. 镶嵌宝石
4. 装备返还到背包
5. 1tick后刷新属性 ← **此时装备可能还没穿上**

**正确流程应该是**:
- 如果装备原本在防具槽，应该直接放回防具槽
- 或者等玩家重新穿戴时才刷新

---

### 5. **防具槽位判断错误**
**位置**: `EventListeners.java` - `isArmorSlot()`

**问题**:
```java
private boolean isArmorSlot(int rawSlot) {
    // 玩家背包中的防具槽位：头盔(5)、胸甲(6)、护腿(7)、靴子(8)
    return rawSlot >= 5 && rawSlot <= 8;
}
```

**实际情况**:
- 这个判断**只在玩家自己的背包界面**中有效
- 如果玩家打开箱子、工作台等其他GUI，rawSlot 的含义完全不同
- 应该检查 `event.getClickedInventory() instanceof PlayerInventory`

**场景**:
- 玩家打开箱子
- 点击箱子的第5-8格物品
- 被误判为防具槽，触发不必要的刷新

---

## 🟡 中等问题

### 6. **血量保存逻辑不合理**
**位置**: `StatsManager.java` - `applyMaxHealth()`

**问题**:
```java
double finalHealth;
if (savedHealth <= 0) {
    finalHealth = newMaxHealth;  // 满血
} else if (savedHealth > newMaxHealth) {
    finalHealth = newMaxHealth;  // 上限
} else {
    finalHealth = savedHealth;  // 保持
}
```

**场景问题**:
1. 玩家血量 500/1000
2. 脱下装备，最大血量变为 20
3. `savedHealth (500) > newMaxHealth (20)`
4. 血量被设为 20 ← **玩家瞬间满血**

**应该**:
- 按比例保持血量百分比
- 或者至少不要让玩家因为脱装备而满血

---

### 7. **装备识别逻辑过于简单**
**位置**: `SmartAttributeDetector.java`

**问题**:
```java
public EquipmentCategory categorize(ItemStack item) {
    // 只根据属性权重判断
    // 没有考虑物品的实际槽位
}
```

**场景**:
- 一个头盔有攻击力属性
- 被识别为武器
- 放在防具槽时不会被正确解析

**应该**:
- 优先根据物品槽位判断
- 属性权重作为辅助判断

---

### 8. **快捷栏切换逻辑不完整**
**位置**: `EventListeners.java` - `onInventoryClick()`

**问题**:
```java
if (isHotbarSwap) {
    int hotbarButton = event.getHotbarButton();
    if (hotbarButton >= 0 && hotbarButton <= 8) {
        scheduleDelayedCheck(player, false, true);  // 只检查武器
    }
}
```

**场景**:
- 玩家在防具槽按数字键
- 快捷栏的防具和防具槽交换
- 但只检查了武器变化，没检查防具变化

**应该**:
- 判断交换的是防具槽还是其他槽位
- 相应地检查防具或武器变化

---

### 9. **缓存一致性问题**
**位置**: `StatsManager.java` - 多个缓存 Map

**问题**:
- `armorStatsCache` 和 `playerStatsMap` 可能不同步
- `mergeStats()` 每次都重新计算，但 `playerStatsMap` 可能包含旧数据

**场景**:
1. 玩家穿戴装备，`armorStatsCache` 更新
2. `mergeStats()` 计算新的 `playerStatsMap`
3. 但如果某个地方直接读取 `playerStatsMap`，可能读到旧数据

---

### 10. **技能触发时机不明确**
**位置**: `SkillManager.java` - `tryTriggerPassiveSkill()`

**问题**:
- 被动技能在哪里触发？
- 代码中没有找到调用 `tryTriggerPassiveSkill()` 的地方
- 只有主动技能有触发逻辑

**影响**: 被动技能可能无法触发

---

## 🟢 轻微问题

### 11. **调试日志未清理**
**位置**: `PostDamageInterceptor.java`

```java
plugin.getLogger().info("[PostDamage] process called, combatLogManager=...");
```

应该改为 `fine()` 或移除。

---

### 12. **配置重载时玩家状态不一致**
**位置**: `GuangDianArmorStats.java` - `reloadAllConfigs()`

**问题**:
```java
public void reloadAllConfigs() {
    configManager.reloadAll();
    // ... 重载各种配置
    
    getServer().getOnlinePlayers().forEach(player -> {
        statsManager.refreshPlayerStats(player);  // 完整刷新
        // ...
    });
}
```

**场景**:
- 管理员执行 `/armorstats reload`
- 所有在线玩家的属性被完整刷新
- 但玩家的血量可能会异常变化

---

### 13. **宝石拆卸成功率计算不直观**
**位置**: `GemInlayGUI.java` - `reworkInlay()`

```java
boolean success = ThreadLocalRandom.current().nextDouble() <= settings.successChance;
```

如果 `successChance = 0.85`，这是 85% 成功率，但配置文件中应该明确说明。

---

## 💡 优化建议

### 优先级 1 - 立即修复

1. **修复技能合并逻辑**
   - 在 `mergeStats()` 中调用 `mergeSkills()`
   - 确保技能列表正确更新

2. **修复 FullEquipmentManager 访问权限**
   - 将 `addItemAttributes` 改为 public
   - 或者在 FullEquipmentManager 中重新实现

3. **修复副手监听**
   - 添加副手装备变更监听
   - 在 `refreshWeaponCache()` 中已经处理了副手，但事件监听缺失

4. **修复防具槽位判断**
   - 添加 `event.getClickedInventory()` 类型检查
   - 避免在其他GUI中误判

### 优先级 2 - 重要改进

5. **改进血量保存逻辑**
   - 保存血量百分比而不是绝对值
   - 或者在脱装备时不要让玩家满血

6. **改进装备识别**
   - 优先根据槽位判断
   - 属性权重作为辅助

7. **添加被动技能触发**
   - 在 `DamageManager` 或拦截器中触发被动技能

### 优先级 3 - 可选优化

8. **添加缓存验证**
   - 定期检查缓存一致性
   - 添加调试命令查看缓存状态

9. **改进配置重载**
   - 重载时保持玩家血量百分比
   - 避免不必要的属性刷新

---

## 🎯 实际应用场景分析

### 场景 1: 玩家登录
**当前流程**:
1. `onPlayerJoin` → 延迟 40tick
2. `loadPlayerData()` → 从存储加载防具属性
3. `refreshWeaponCache()` → 解析当前武器
4. `mergeStats()` → 合并属性
5. ❌ **技能列表未合并**

### 场景 2: 玩家切换武器（滚轮）
**当前流程**:
1. `onPlayerItemHeld` → 检测主手变化
2. `scheduleDelayedAttributeRefresh(2L)` → 延迟2tick
3. `refreshWeaponOnly()` → 只刷新武器
4. ✅ **正确，不会改变血量**

### 场景 3: 玩家穿戴防具（Shift+点击）
**当前流程**:
1. `onInventoryClick` → 检测到 `MOVE_TO_OTHER_INVENTORY`
2. `scheduleDelayedCheck(true, false)` → 延迟1tick
3. `isArmorChanged()` → 检查防具是否变化
4. `scheduleArmorRefresh()` → 延迟1tick刷新
5. ✅ **基本正确，但可能有血量问题**

### 场景 4: 玩家在箱子界面点击
**当前流程**:
1. `onInventoryClick` → rawSlot 5-8
2. `isArmorSlot(5-8)` → ❌ **误判为防具槽**
3. 触发不必要的刷新

### 场景 5: 玩家镶嵌宝石
**当前流程**:
1. 打开GUI，放入装备（从防具槽取下）
2. 放入宝石
3. 确认镶嵌
4. 装备返还到背包
5. 延迟1tick刷新属性
6. ❌ **此时装备在背包，不在防具槽，属性不会生效**

### 场景 6: 玩家脱下装备
**当前流程**:
1. 玩家血量 500/1000
2. 脱下装备，最大血量变为 20
3. `applyMaxHealth()` → `savedHealth (500) > newMaxHealth (20)`
4. 设置血量为 20
5. ❌ **玩家瞬间满血**

---

## 🔧 修复方案

### 修复 1: 技能合并
```java
private PlayerStats mergeStats(UUID uuid) {
    // ... 现有代码
    
    // 添加：合并技能
    mergeSkills(uuid);
    
    return stats;
}
```

### 修复 2: 访问权限
```java
// 方案A: 改为 public
public void addItemAttributes(PlayerStats stats, List<String> skills, ItemStack item) {
    // ...
}

// 方案B: 删除 FullEquipmentManager（目前未使用）
```

### 修复 3: 副手监听
```java
@EventHandler
public void onPlayerSwapHandItems(PlayerSwapHandItemsEvent event) {
    Player player = event.getPlayer();
    scheduleDelayedCheck(player, false, true);  // 检查武器变化
}
```

### 修复 4: 防具槽位判断
```java
private boolean isArmorSlot(int rawSlot, Inventory clickedInventory) {
    if (!(clickedInventory instanceof PlayerInventory)) {
        return false;
    }
    return rawSlot >= 5 && rawSlot <= 8;
}
```

### 修复 5: 宝石镶嵌
```java
// 方案A: 记住装备来源槽位，镶嵌后放回
// 方案B: 不自动刷新，等玩家穿戴时自然触发
// 方案C: 检测装备是否在防具槽，如果是则刷新
```

### 修复 6: 血量保存
```java
// 保存血量百分比
double healthPercent = savedHealth / savedMaxHealth;
double finalHealth = newMaxHealth * healthPercent;
finalHealth = Math.max(1.0, Math.min(finalHealth, newMaxHealth));
```

---

## 📊 优先级排序

1. **P0 - 立即修复**: 问题 1, 2, 4
2. **P1 - 重要**: 问题 3, 5, 6
3. **P2 - 建议**: 问题 7, 8, 9, 10
4. **P3 - 优化**: 问题 11, 12, 13
