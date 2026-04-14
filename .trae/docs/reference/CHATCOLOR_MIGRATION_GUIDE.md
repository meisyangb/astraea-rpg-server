# ChatColor 迁移到 MiniMessageService 指南

> 将 `ChatColor` 消息构建迁移到 `MiniMessageService` 的完整指南

## 迁移规则

### 1. 必须迁移的模式

```java
// ❌ 必须迁移
player.sendMessage(ChatColor.RED + "错误消息");
player.sendMessage(ChatColor.GREEN + "成功: " + ChatColor.WHITE + playerName);

// ✅ 迁移为
player.sendMessage(MiniMessageService.getInstance().red("错误消息"));
player.sendMessage(MiniMessageService.getInstance().green("成功: ").append(MiniMessageService.getInstance().white(playerName)));
```

### 2. 无需迁移的模式

```java
// ✅ 无需迁移 - stripColor 是必要功能
String plain = ChatColor.stripColor(coloredText);

// ✅ 无需迁移 - 技术需要
line = ChatColor.RESET.toString();
line = line + ChatColor.RESET + ChatColor.COLOR_CHAR + suffixChar;

// ✅ 无需迁移 - Inventory 标题（Bukkit API 限制）
this.inventory = Bukkit.createInventory(this, SIZE, ChatColor.GOLD + "全球市场");
```

## 批量替换规则

### 导入替换

```java
// 旧导入
import org.bukkit.ChatColor;

// 新导入
import cn.guangdian.rpgcore.message.MiniMessageService;
```

### 简单消息替换

| 原代码 | 新代码 |
|--------|--------|
| `ChatColor.RED + "消息"` | `mm.red("消息")` |
| `ChatColor.GREEN + "消息"` | `mm.green("消息")` |
| `ChatColor.YELLOW + "消息"` | `mm.yellow("消息")` |
| `ChatColor.GOLD + "消息"` | `mm.gold("消息")` |
| `ChatColor.AQUA + "消息"` | `mm.aqua("消息")` |
| `ChatColor.GRAY + "消息"` | `mm.gray("消息")` |
| `ChatColor.WHITE + "消息"` | `mm.white("消息")` |

### 组合消息替换

```java
// 原代码
player.sendMessage(ChatColor.YELLOW + "洞主: " + ChatColor.WHITE + cave.getOwnerName());

// 新代码
MiniMessageService mm = MiniMessageService.getInstance();
player.sendMessage(mm.yellow("洞主: ").append(mm.white(cave.getOwnerName())));
```

## 需要修复的文件清单

### 高优先级

| 插件 | 文件 | 行数 | 说明 |
|------|------|------|------|
| GuangDianCaveFu | CaveCommand.java | 20+ | 命令消息 |
| GuangDianCaveFu | CaveAdminCommand.java | 10+ | 管理员命令 |

### 中优先级

| 插件 | 文件 | 行数 | 说明 |
|------|------|------|------|
| GuangDianMarket | MarketGUI.java | 30+ | GUI 消息（部分可保留） |
| GuangDianGearScore | GearScorePlaceholder.java | 5+ | 占位符 |
| GuangDianRaid | RaidConfigManager.java | 5+ | 配置消息 |

### 低优先级（可选）

| 插件 | 文件 | 说明 |
|------|------|------|
| GuangDianBoard | DifferentialUpdater.java | 技术需要，可保留 |
| GuangDianItemTrigger | ItemTriggerServiceAdapter.java | 使用 stripColor，无需修改 |

## 示例：完整文件迁移

### 迁移前

```java
import org.bukkit.ChatColor;

public class CaveCommand implements CommandExecutor {
    
    private void sendHelp(Player player) {
        player.sendMessage(ChatColor.GOLD + "========== 洞府帮助 ==========");
        player.sendMessage(ChatColor.YELLOW + "/cave create " + ChatColor.GRAY + "- 创建洞府");
        player.sendMessage(ChatColor.RED + "此命令只能由玩家执行！");
    }
}
```

### 迁移后

```java
import cn.guangdian.rpgcore.message.MiniMessageService;

public class CaveCommand implements CommandExecutor {
    
    private void sendHelp(Player player) {
        MiniMessageService mm = MiniMessageService.getInstance();
        player.sendMessage(mm.gold("========== 洞府帮助 =========="));
        player.sendMessage(mm.yellow("/cave create ").append(mm.gray("- 创建洞府")));
        player.sendMessage(mm.red("此命令只能由玩家执行！"));
    }
}
```

## PowerShell 批量替换脚本

```powershell
# 注意：使用前请备份代码
# 此脚本仅作为参考，实际使用前需要测试

$plugins = @(
    "GuangDianCaveFu",
    "GuangDianMarket",
    "GuangDianGearScore",
    "GuangDianRaid"
)

foreach ($plugin in $plugins) {
    $path = "e:\原创RPG服务端\plugins\$plugin\src\main\java"
    if (Test-Path $path) {
        Write-Host "处理插件: $plugin"
        
        # 获取所有 Java 文件
        $files = Get-ChildItem -Path $path -Filter "*.java" -Recurse
        
        foreach ($file in $files) {
            $content = Get-Content $file.FullName -Raw
            
            # 检查是否包含 ChatColor 构建消息
            if ($content -match 'ChatColor\.(RED|GREEN|YELLOW|GOLD|AQUA|GRAY|WHITE) \+') {
                Write-Host "  需要修复: $($file.Name)"
                # 这里可以添加自动替换逻辑
            }
        }
    }
}
```

## 手动修复步骤

1. **打开文件** - 使用 IDE 打开需要修复的文件
2. **替换导入** - 将 `import org.bukkit.ChatColor;` 替换为 `import cn.guangdian.rpgcore.message.MiniMessageService;`
3. **查找替换** - 使用 IDE 的查找替换功能，按上表规则替换
4. **验证编译** - 确保修改后能正常编译
5. **测试运行** - 在游戏中测试消息显示是否正常

## 验证清单

- [ ] 文件能正常编译
- [ ] 游戏内消息颜色显示正确
- [ ] 没有遗漏的 ChatColor 使用
- [ ] 没有引入新的错误

---
*创建时间: 2026-04-14*
*适用版本: Paper 1.21.6 + RPGCore*
