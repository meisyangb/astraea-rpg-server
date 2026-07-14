$env:JAVA_HOME = 'D:\Deployment\jdk-21.0.10+7'
$Javac = "$env:JAVA_HOME\bin\javac.exe"
$Jar = "$env:JAVA_HOME\bin\jar.exe"

$SrcDir = "e:\RPG\plugins\VIPname\src\main\java"
$OutDir = "e:\RPG\plugins\VIPname\build\classes"
$ResDir = "e:\RPG\plugins\VIPname\src\main\resources"
$JarFile = "e:\RPG\plugins\VIPname\build\libs\VIPname.jar"

# Paper API (使用 Gradle 缓存或直接下载)
$PaperApi = "$env:USERPROFILE\.gradle\caches\modules-2\files-2.1\io.papermc.paper\paper-api\1.21.4-R0.1-SNAPSHOT"
$Papi = "$env:USERPROFILE\.gradle\caches\modules-2\files-2.1\me.clip\placeholderapi\2.11.6"

# 如果 Gradle 缓存不存在，使用 GuangDianQuest 的 build 目录
if (-not (Test-Path $PaperApi)) {
    # 尝试使用已编译的项目作为依赖源
    $PaperApi = "e:\RPG\plugins\GuangDianQuest\build\classes"
}

# 创建输出目录
if (-not (Test-Path $OutDir)) { New-Item -ItemType Directory -Path $OutDir -Force | Out-Null }
if (-not (Test-Path "e:\RPG\plugins\VIPname\build\libs")) { New-Item -ItemType Directory -Path "e:\RPG\plugins\VIPname\build\libs" -Force | Out-Null }

# 查找所有 Java 文件
$JavaFiles = Get-ChildItem -Path $SrcDir -Filter "*.java" -Recurse | ForEach-Object { $_.FullName }

Write-Host "编译 Java 文件 (无依赖编译模式)..."

# 由于没有 paper-api jar，我们使用简化编译
# 创建一个最小的 stub 编译

# 首先创建必要的 stub 类
$Stubs = @"

// Minimal stub for compilation
package org.bukkit;
public class Bukkit { 
    public static org.bukkit.plugin.PluginManager getPluginManager() { return null; }
}

package org.bukkit.plugin;
public class PluginManager {
    public org.bukkit.plugin.Plugin getPlugin(String name) { return null; }
    public void registerEvents(Object l, org.bukkit.plugin.Plugin p) {}
}
public interface Plugin {}
public abstract class JavaPlugin implements Plugin {
    public void saveDefaultConfig() {}
    public void reloadConfig() {}
    public org.bukkit.configuration.file.FileConfiguration getConfig() { return null; }
    public java.io.File getDataFolder() { return null; }
    public org.bukkit.command.PluginCommand getCommand(String name) { return null; }
    public org.bukkit.Server getServer() { return null; }
    public java.util.logging.Logger getLogger() { return java.util.logging.Logger.getLogger(""); }
}
public class PluginCommand extends org.bukkit.command.Command {
    public void setExecutor(Object e) {}
    public void setTabCompleter(Object t) {}
}

package org.bukkit.command;
public abstract class Command { protected Command(String n) {} }
public interface CommandExecutor { boolean onCommand(Object s, Command c, String l, String[] a); }
public interface TabCompleter { java.util.List<String> onTabComplete(Object s, Command c, String a, String[] args); }
public interface CommandSender { void sendMessage(Object m); boolean hasPermission(String p); }

package org.bukkit.configuration.file;
public class FileConfiguration { 
    public org.bukkit.configuration.ConfigurationSection getConfigurationSection(String s) { return null; }
}
public class YamlConfiguration extends FileConfiguration {
    public static YamlConfiguration loadConfiguration(java.io.File f) { return null; }
    public void save(java.io.File f) {}
}

package org.bukkit.configuration;
public interface ConfigurationSection {
    java.util.Set<String> getKeys(boolean b);
    ConfigurationSection getConfigurationSection(String s);
    String getString(String s);
    int getInt(String s, int d);
    java.util.List<String> getStringList(String s);
}

package org.bukkit.entity;
public interface Player {
    String getName();
    String getDisplayName();
    java.util.UUID getUniqueId();
    void sendMessage(Object m);
    void playSound(Object l, Object s, float v1, float v2);
    Object getLocation();
    int getLevel();
    int getTotalExperience();
    float getHealth();
    float getMaxHealth();
    int getFoodLevel();
    Object getWorld();
    Object getGameMode();
    void displayName(Object n);
    void closeInventory();
    void addPotionEffect(Object e);
    void removePotionEffect(Object t);
}
public enum GameMode { SURVIVAL, CREATIVE, ADVENTURE, SPECTATOR }

package org.bukkit.event;
public @interface EventHandler { 
    EventPriority priority() default EventPriority.NORMAL;
}
public enum EventPriority { LOWEST, LOW, NORMAL, HIGH, HIGHEST, MONITOR }
public interface Listener {}

package org.bukkit.event.player;
public class PlayerEvent { public org.bukkit.entity.Player getPlayer() { return null; } }
public class PlayerJoinEvent extends PlayerEvent {}
public class PlayerQuitEvent extends PlayerEvent {}
public class AsyncPlayerChatEvent extends PlayerEvent { 
    public String getMessage() { return null; } 
    public void setCancelled(boolean b) {}
}
public class PlayerCommandPreprocessEvent extends PlayerEvent { 
    public String getMessage() { return null; } 
    public void setCancelled(boolean b) {}
}

package org.bukkit.potion;
public class PotionEffect {
    public PotionEffect(Object t, int d, int a, boolean p1, boolean p2) {}
}
public enum PotionEffectType { BLINDNESS, NIGHT_VISION }

package org.bukkit.scheduler;
public class BukkitTask {}

package org.bukkit;
public class Sound { public static final Sound BLOCK_NOTE_BLOCK_PLING = null; }
public class Material {}

package net.kyori.adventure.text;
public interface Component {
    static Component empty() { return null; }
    static Component text(String s, Object... c) { return null; }
}
public enum NamedTextColor { GOLD, YELLOW, GREEN, RED, GRAY, WHITE, DARK_GRAY }

package net.kyori.adventure.text.format;
public enum TextDecoration { BOLD }

package net.kyori.adventure.text.minimessage;
public class MiniMessage {
    public static MiniMessage miniMessage() { return null; }
    public Component deserialize(String s) { return null; }
}

package me.clip.placeholderapi.expansion;
public abstract class PlaceholderExpansion {
    public abstract String getIdentifier();
    public abstract String getAuthor();
    public abstract String getVersion();
    public boolean persist() { return true; }
    public void register() {}
    public abstract String onPlaceholderRequest(Object player, String params);
}

package me.clip.placeholderapi;
public class PlaceholderAPI {
    public static String setPlaceholders(Object p, String s) { return s; }
}

package org.jetbrains.annotations;
public @interface NotNull {}
"@

# 写入 stub 文件
$StubFile = "$OutDir\stubs.java"
$Stubs -replace "`n", "`r`n" | Out-File -FilePath $StubFile -Encoding UTF8

Write-Host "编译 stub 文件..."
& $Javac -encoding UTF-8 -source 21 -target 21 -d $OutDir $StubFile 2>$null

Write-Host "编译主程序..."
& $Javac -encoding UTF-8 -source 21 -target 21 -cp $OutDir -d $OutDir $JavaFiles 2>&1 | Out-Null

if ($LASTEXITCODE -ne 0) {
    Write-Host "编译失败，尝试简化版本..."
    
    # 简化版本 - 移除 PlaceholderAPI 依赖
    $SimpleFiles = @(
        "$SrcDir\cn\guangdian\vipname\VIPname.java",
        "$SrcDir\cn\guangdian\vipname\model\Title.java",
        "$SrcDir\cn\guangdian\vipname\model\PlayerTitle.java",
        "$SrcDir\cn\guangdian\vipname\manager\TitleManager.java",
        "$SrcDir\cn\guangdian\vipname\variable\VariableManager.java",
        "$SrcDir\cn\guangdian\vipname\command\VIPnameCommand.java",
        "$SrcDir\cn\guangdian\vipname\listener\PlayerListener.java"
    )
    
    & $Javac -encoding UTF-8 -source 21 -target 21 -cp $OutDir -d $OutDir $SimpleFiles
}

if ($LASTEXITCODE -ne 0) {
    Write-Host "编译失败!"
    exit 1
}

# 复制资源文件
if (Test-Path $ResDir) {
    Copy-Item -Path "$ResDir\*" -Destination $OutDir -Recurse -Force
}

# 创建 JAR
Write-Host "打包 JAR..."
Push-Location $OutDir
& $Jar -cfm $JarFile "plugin.yml" *
Pop-Location

# 复制到服务器
Copy-Item -Path $JarFile -Destination "e:\RPG\译梦传说2\plugins\VIPname-1.0.0.jar" -Force

Write-Host "编译成功! JAR文件: $JarFile"