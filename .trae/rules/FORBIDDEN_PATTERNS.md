# Astraea RPG 禁止模式清单

> 所有开发必须遵守的禁止模式，违反将导致代码被拒绝

---

## 1. 调度器禁止项

### ❌ 禁止
```java
new BukkitRunnable() { ... }.runTaskTimer(plugin, delay, period);
new BukkitRunnable() { ... }.runTaskLater(plugin, delay);
Bukkit.getScheduler().scheduleSyncRepeatingTask(plugin, task, delay, period);
Bukkit.getScheduler().runTaskLater(plugin, task, delay);
Bukkit.getScheduler().runTaskTimer(plugin, task, delay, period);
```

### ✅ 正确
```java
RPGCore rpgCore = RPGCore.getInstance();
SyncScheduler scheduler = rpgCore.getScheduler();

long taskId = scheduler.runSyncLater(() -> { }, 50L);
long repeatingId = scheduler.runSyncRepeating(() -> { }, 0L, 20L);
scheduler.runAsync(() -> { });
scheduler.cancelTask(taskId);
```

---

## 2. RPGCore 获取禁止项

### ❌ 禁止
```java
Bukkit.getPlugin("RPGCore")
Bukkit.getPluginManager().getPlugin("RPGCore")
(RPGCore) Bukkit.getPlugin("RPGCore")
```

### ✅ 正确
```java
RPGCore rpgCore = RPGCore.getInstance();
if (rpgCore != null) { }
```

---

## 3. 外部服务调用禁止项

### ❌ 禁止
```java
LuckPermsProvider.get()
luckPerms.getUserManager().getUser(player.getUniqueId())
PlaceholderAPI.setPlaceholders(player, text)
expansion.unregister()
```

### ✅ 正确
```java
RPGCore rpgCore = RPGCore.getInstance();
ExternalServiceIntegration externalServices = rpgCore.getExternalServices();

if (externalServices.isLuckPermsEnabled()) {
    String prefix = externalServices.getPlayerPrefix(player);
}
if (externalServices.isPlaceholderAPIEnabled()) {
    String parsed = externalServices.parsePlaceholders(player, text);
}
PlaceholderAPI.unregisterExpansion(expansionInstance);
```

---

## 4. MythicMobs PDC Key 禁止项

### ❌ 禁止
```java
new NamespacedKey("mythicmobs", "item")
```

### ✅ 正确
```java
NamespacedKey typeKey = new NamespacedKey("mythicmobs", "type");
String typeId = meta.getPersistentDataContainer().get(typeKey, PersistentDataType.STRING);
```

---

## 5. 消息发送禁止项

### ❌ 禁止
```java
player.sendMessage(ChatColor.RED + "错误消息");
player.sendMessage("§c错误消息");
```

### ✅ 正确
```java
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;

player.sendMessage(Component.text("错误消息").color(NamedTextColor.RED));
```

---

## 6. 插件主类禁止项

### ❌ 禁止
```java
public class MyPlugin extends JavaPlugin { }
```

### ✅ 正确
```java
public class MyPlugin extends AbstractRPGPlugin {
    @Override protected void onPluginEnable() { }
    @Override protected void onPluginDisable() { }
    @Override protected String getPluginName() { return "MyPlugin"; }
}
```

---

## 7. 全息图显示禁止项

### ❌ 禁止
```java
ArmorStand hologram = location.getWorld().spawn(location, ArmorStand.class);
hologram.setCustomName("全息图文字");
```

### ✅ 正确
```java
TextDisplay textDisplay = location.getWorld().spawn(location, TextDisplay.class);
textDisplay.setText("全息图文字");
textDisplay.setBillboard(Display.Billboard.CENTER);
```

---

*最后更新: 2026-04-10*
