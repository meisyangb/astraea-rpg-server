package cn.guangdian.worldrules.command;

import cn.guangdian.rpgcore.message.MiniMessageService;
import cn.guangdian.worldrules.GuangDianWorldRules;
import cn.guangdian.worldrules.listener.ChunkLoadListener;
import cn.guangdian.worldrules.model.ProtectedRegion;
import cn.guangdian.worldrules.model.WorldRules;
import cn.guangdian.worldrules.util.ChunkTrimmer;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.Map;
import java.util.Set;

public class WorldRulesCommand implements CommandExecutor {

    private final GuangDianWorldRules plugin;
    private final MiniMessageService miniMessage;

    public WorldRulesCommand(GuangDianWorldRules plugin) {
        this.plugin = plugin;
        this.miniMessage = MiniMessageService.getInstance();
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.hasPermission("guangdian.worldrules.admin")) {
            sender.sendMessage(miniMessage.red("你没有权限使用此命令！"));
            return true;
        }

        if (args.length == 0) {
            sendHelp(sender);
            return true;
        }

        String subCommand = args[0].toLowerCase();

        switch (subCommand) {
            case "reload":
                handleReload(sender);
                break;
            case "info":
                handleInfo(sender, args);
                break;
            case "list":
                handleList(sender);
                break;
            case "check":
                handleCheck(sender, args);
                break;
            case "debug":
                handleDebug(sender, args);
                break;
            // 区域管理命令
            case "create":
                handleCreateRegion(sender, args);
                break;
            case "delete":
                handleDeleteRegion(sender, args);
                break;
            case "region":
                handleRegionInfo(sender, args);
                break;
            case "regions":
                handleRegionsList(sender);
                break;
            case "setregion":
                handleSetRegionRule(sender, args);
                break;
            // 区块裁剪命令
            case "trim":
                handleTrim(sender, args);
                break;
            case "trimlock":
                handleTrimLock(sender, args);
                break;
            case "trimunlock":
                handleTrimUnlock(sender, args);
                break;
            case "triminfo":
                handleTrimInfo(sender);
                break;
            default:
                sendHelp(sender);
                break;
        }

        return true;
    }

    private void sendHelp(CommandSender sender) {
        sender.sendMessage(Component.text("===== 世界规则管理 =====").color(NamedTextColor.GOLD));
        sender.sendMessage(Component.text("/gworldrules reload").color(NamedTextColor.YELLOW)
                .append(Component.text(" - 重新加载配置").color(NamedTextColor.WHITE)));
        sender.sendMessage(Component.text("/gworldrules info <世界名>").color(NamedTextColor.YELLOW)
                .append(Component.text(" - 查看世界规则").color(NamedTextColor.WHITE)));
        sender.sendMessage(Component.text("/gworldrules list").color(NamedTextColor.YELLOW)
                .append(Component.text(" - 列出已配置的世界").color(NamedTextColor.WHITE)));
        sender.sendMessage(Component.text("/gworldrules check <世界名> <规则名>").color(NamedTextColor.YELLOW)
                .append(Component.text(" - 检查特定规则").color(NamedTextColor.WHITE)));
        sender.sendMessage(Component.text("/gworldrules debug <true|false>").color(NamedTextColor.YELLOW)
                .append(Component.text(" - 开关调试模式").color(NamedTextColor.WHITE)));

        sender.sendMessage(Component.text("===== 区域管理 =====").color(NamedTextColor.GOLD));
        sender.sendMessage(Component.text("/gworldrules create <区域名>").color(NamedTextColor.YELLOW)
                .append(Component.text(" - 创建区域（需先用木斧选点）").color(NamedTextColor.WHITE)));
        sender.sendMessage(Component.text("/gworldrules delete <区域名>").color(NamedTextColor.YELLOW)
                .append(Component.text(" - 删除区域").color(NamedTextColor.WHITE)));
        sender.sendMessage(Component.text("/gworldrules region <区域名>").color(NamedTextColor.YELLOW)
                .append(Component.text(" - 查看区域信息").color(NamedTextColor.WHITE)));
        sender.sendMessage(Component.text("/gworldrules regions").color(NamedTextColor.YELLOW)
                .append(Component.text(" - 列出所有区域").color(NamedTextColor.WHITE)));
        sender.sendMessage(Component.text("/gworldrules setregion <区域名> <规则> <值>").color(NamedTextColor.YELLOW)
                .append(Component.text(" - 设置区域规则").color(NamedTextColor.WHITE)));

        sender.sendMessage(Component.text("===== 区块裁剪 =====").color(NamedTextColor.GOLD));
        sender.sendMessage(Component.text("/gworldrules trim <世界名> <区域名> [delete]").color(NamedTextColor.YELLOW)
                .append(Component.text(" - 裁剪世界区块").color(NamedTextColor.WHITE)));
        sender.sendMessage(Component.text("/gworldrules trimlock <世界名> <区域名>").color(NamedTextColor.YELLOW)
                .append(Component.text(" - 锁定裁剪区域").color(NamedTextColor.WHITE)));
        sender.sendMessage(Component.text("/gworldrules trimunlock <世界名>").color(NamedTextColor.YELLOW)
                .append(Component.text(" - 解锁裁剪区域").color(NamedTextColor.WHITE)));
        sender.sendMessage(Component.text("/gworldrules triminfo").color(NamedTextColor.YELLOW)
                .append(Component.text(" - 查看裁剪状态").color(NamedTextColor.WHITE)));
    }

    private void handleReload(CommandSender sender) {
        plugin.reloadAll();
        sender.sendMessage(miniMessage.green("配置已重新加载！"));
    }

    private void handleInfo(CommandSender sender, String[] args) {
        String worldName;
        if (args.length > 1) {
            worldName = args[1];
        } else if (sender instanceof Player) {
            worldName = ((Player) sender).getWorld().getName();
        } else {
            sender.sendMessage(miniMessage.red("请指定世界名称！"));
            return;
        }

        World world = Bukkit.getWorld(worldName);
        if (world == null) {
            sender.sendMessage(miniMessage.red("世界 " + worldName + " 不存在！"));
            return;
        }

        WorldRules rules = plugin.getWorldRulesManager().getWorldRules(worldName);

        sender.sendMessage(Component.text("===== 世界规则: " + worldName + " =====").color(NamedTextColor.GOLD));

        // 死亡相关
        sender.sendMessage(Component.text("【死亡相关】").color(NamedTextColor.AQUA));
        sendRuleStatus(sender, "死亡不掉落", rules.isKeepInventory());
        sendRuleStatus(sender, "死亡不掉经验", rules.isKeepExp());

        // 生物刷新
        sender.sendMessage(Component.text("【生物刷新】").color(NamedTextColor.AQUA));
        sendRuleStatus(sender, "禁止自然刷新", rules.isDisableNaturalSpawn());
        sendRuleStatus(sender, "禁止怪物刷新", rules.isDisableMonsterSpawn());
        sendRuleStatus(sender, "禁止动物刷新", rules.isDisableAnimalSpawn());

        // 环境
        sender.sendMessage(Component.text("【环境】").color(NamedTextColor.AQUA));
        sendRuleStatus(sender, "禁止天气变化", rules.isDisableWeatherChange());
        sendRuleStatus(sender, "禁止时间变化", rules.isDisableTimeChange());

        // 玩家状态
        sender.sendMessage(Component.text("【玩家状态】").color(NamedTextColor.AQUA));
        sendRuleStatus(sender, "禁止饥饿度下降", rules.isDisableHunger());
        sendRuleStatus(sender, "禁止摔落伤害", rules.isDisableFallDamage());
        sendRuleStatus(sender, "禁止火焰伤害", rules.isDisableFireDamage());
        sendRuleStatus(sender, "禁止溺水伤害", rules.isDisableDrowningDamage());

        // 破坏
        sender.sendMessage(Component.text("【破坏】").color(NamedTextColor.AQUA));
        sendRuleStatus(sender, "禁止爆炸破坏", rules.isDisableExplosionBlockDamage());
        sendRuleStatus(sender, "禁止生物破坏", rules.isDisableMobGriefing());

        // PVP
        sender.sendMessage(Component.text("【PVP】").color(NamedTextColor.AQUA));
        sendRuleStatus(sender, "允许PVP", rules.isPvp());

        // 物品
        sender.sendMessage(Component.text("【物品】").color(NamedTextColor.AQUA));
        sendRuleStatus(sender, "禁止物品丢弃", rules.isDisableItemDrop());
        sendRuleStatus(sender, "禁止物品拾取", rules.isDisableItemPickup());

        // 禁用生物
        if (!rules.getDisabledMobs().isEmpty()) {
            sender.sendMessage(Component.text("【禁用生物类型】").color(NamedTextColor.AQUA));
            sender.sendMessage(Component.text(String.join(", ", rules.getDisabledMobs())).color(NamedTextColor.WHITE));
        }
    }

    private void sendRuleStatus(CommandSender sender, String ruleName, boolean enabled) {
        Component status = enabled
                ? Component.text("[开启]").color(NamedTextColor.GREEN)
                : Component.text("[关闭]").color(NamedTextColor.RED);
        sender.sendMessage(Component.text("  " + ruleName + ": ").color(NamedTextColor.WHITE).append(status));
    }

    private void handleList(CommandSender sender) {
        Set<String> worlds = plugin.getWorldRulesManager().getConfiguredWorlds();

        sender.sendMessage(Component.text("===== 已配置规则的世界 =====").color(NamedTextColor.GOLD));

        if (worlds.isEmpty()) {
            sender.sendMessage(Component.text("暂无配置特定规则的世界").color(NamedTextColor.YELLOW));
            sender.sendMessage(Component.text("所有世界使用默认规则").color(NamedTextColor.GRAY));
        } else {
            for (String worldName : worlds) {
                sender.sendMessage(Component.text("- " + worldName).color(NamedTextColor.WHITE));
            }
            sender.sendMessage(Component.text("总计: " + worlds.size() + " 个世界").color(NamedTextColor.GRAY));
        }

        // 显示所有世界
        sender.sendMessage(Component.text("===== 服务器所有世界 =====").color(NamedTextColor.GOLD));
        for (World world : Bukkit.getWorlds()) {
            boolean hasConfig = plugin.getWorldRulesManager().hasWorldRules(world.getName());
            Component indicator = hasConfig
                    ? Component.text("[已配置] ").color(NamedTextColor.GREEN)
                    : Component.text("[默认] ").color(NamedTextColor.GRAY);
            sender.sendMessage(indicator.append(Component.text(world.getName()).color(NamedTextColor.WHITE)));
        }
    }

    private void handleCheck(CommandSender sender, String[] args) {
        if (args.length < 3) {
            sender.sendMessage(miniMessage.red("用法: /gworldrules check <世界名> <规则名>"));
            sender.sendMessage(miniMessage.yellow("可用规则: keep-inventory, keep-exp, disable-natural-spawn, disable-monster-spawn, pvp"));
            return;
        }

        String worldName = args[1];
        String ruleName = args[2].toLowerCase();

        World world = Bukkit.getWorld(worldName);
        if (world == null) {
            sender.sendMessage(miniMessage.red("世界 " + worldName + " 不存在！"));
            return;
        }

        WorldRules rules = plugin.getWorldRulesManager().getWorldRules(worldName);
        boolean value;

        switch (ruleName) {
            case "keep-inventory":
                value = rules.isKeepInventory();
                break;
            case "keep-exp":
                value = rules.isKeepExp();
                break;
            case "disable-natural-spawn":
                value = rules.isDisableNaturalSpawn();
                break;
            case "disable-monster-spawn":
                value = rules.isDisableMonsterSpawn();
                break;
            case "disable-animal-spawn":
                value = rules.isDisableAnimalSpawn();
                break;
            case "pvp":
                value = rules.isPvp();
                break;
            case "disable-hunger":
                value = rules.isDisableHunger();
                break;
            case "disable-fall-damage":
                value = rules.isDisableFallDamage();
                break;
            default:
                sender.sendMessage(miniMessage.red("未知规则: " + ruleName));
                return;
        }

        Component status = value
                ? Component.text("开启").color(NamedTextColor.GREEN)
                : Component.text("关闭").color(NamedTextColor.RED);
        sender.sendMessage(Component.text("世界 " + worldName + " 的规则 " + ruleName + ": ").color(NamedTextColor.WHITE).append(status));
    }

    private void handleDebug(CommandSender sender, String[] args) {
        if (args.length < 2) {
            boolean current = plugin.getConfigManager().isDebug();
            sender.sendMessage(Component.text("当前调试模式: ").color(NamedTextColor.WHITE)
                    .append(current ? Component.text("开启").color(NamedTextColor.GREEN)
                            : Component.text("关闭").color(NamedTextColor.RED)));
            return;
        }

        boolean enable = Boolean.parseBoolean(args[1]);
        plugin.getConfigManager().setDebug(enable);
        sender.sendMessage(Component.text("调试模式已").color(NamedTextColor.WHITE)
                .append(enable ? Component.text("开启").color(NamedTextColor.GREEN)
                        : Component.text("关闭").color(NamedTextColor.RED)));
    }

    // ========== 区域管理方法 ==========

    private void handleCreateRegion(CommandSender sender, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage(miniMessage.red("此命令只能由玩家执行！"));
            return;
        }

        if (args.length < 2) {
            sender.sendMessage(miniMessage.red("用法: /gworldrules create <区域名>"));
            return;
        }

        Player player = (Player) sender;
        String regionName = args[1];

        // 检查是否已选择两个点
        if (!plugin.getRegionSelectionListener().hasSelection(player)) {
            sender.sendMessage(miniMessage.red("请先用木斧选择两个点！"));
            sender.sendMessage(miniMessage.yellow("左键点击方块设置第一个点，右键点击方块设置第二个点"));
            return;
        }

        Location loc1 = plugin.getRegionSelectionListener().getFirstPoint(player);
        Location loc2 = plugin.getRegionSelectionListener().getSecondPoint(player);

        // 创建区域
        ProtectedRegion region = new ProtectedRegion(
                regionName,
                loc1.getWorld().getName(),
                loc1.getBlockX(), loc1.getBlockY(), loc1.getBlockZ(),
                loc2.getBlockX(), loc2.getBlockY(), loc2.getBlockZ()
        );

        plugin.getRegionManager().addRegion(region);
        plugin.getRegionSelectionListener().clearSelection(player);

        sender.sendMessage(miniMessage.green("区域 " + regionName + " 创建成功！"));
        sender.sendMessage(Component.text()
                .color(NamedTextColor.GRAY)
                .content(String.format("范围: (%d,%d,%d) -> (%d,%d,%d) @ %s",
                        region.getMinX(), region.getMinY(), region.getMinZ(),
                        region.getMaxX(), region.getMaxY(), region.getMaxZ(),
                        region.getWorldName())));
    }

    private void handleDeleteRegion(CommandSender sender, String[] args) {
        if (args.length < 2) {
            sender.sendMessage(miniMessage.red("用法: /gworldrules delete <区域名>"));
            return;
        }

        String regionName = args[1];
        ProtectedRegion region = plugin.getRegionManager().getRegion(regionName);

        if (region == null) {
            sender.sendMessage(miniMessage.red("区域 " + regionName + " 不存在！"));
            return;
        }

        plugin.getRegionManager().removeRegion(regionName);
        sender.sendMessage(miniMessage.green("区域 " + regionName + " 已删除！"));
    }

    private void handleRegionInfo(CommandSender sender, String[] args) {
        if (args.length < 2) {
            sender.sendMessage(miniMessage.red("用法: /gworldrules region <区域名>"));
            return;
        }

        String regionName = args[1];
        ProtectedRegion region = plugin.getRegionManager().getRegion(regionName);

        if (region == null) {
            sender.sendMessage(miniMessage.red("区域 " + regionName + " 不存在！"));
            return;
        }

        sender.sendMessage(Component.text("===== 区域信息: " + regionName + " =====").color(NamedTextColor.GOLD));
        sender.sendMessage(Component.text("世界: ").color(NamedTextColor.WHITE)
                .append(Component.text(region.getWorldName()).color(NamedTextColor.YELLOW)));
        sender.sendMessage(Component.text()
                .color(NamedTextColor.WHITE)
                .content(String.format("最小点: (%d, %d, %d)", region.getMinX(), region.getMinY(), region.getMinZ())));
        sender.sendMessage(Component.text()
                .color(NamedTextColor.WHITE)
                .content(String.format("最大点: (%d, %d, %d)", region.getMaxX(), region.getMaxY(), region.getMaxZ())));
        sender.sendMessage(Component.text()
                .color(NamedTextColor.WHITE)
                .content(String.format("体积: %d 个方块", region.getVolume())));

        sender.sendMessage(Component.text("【规则覆盖】").color(NamedTextColor.AQUA));
        sendRegionRule(sender, "允许破坏", region.getAllowBreak());
        sendRegionRule(sender, "允许放置", region.getAllowPlace());
        sendRegionRule(sender, "允许交互", region.getAllowInteract());
        sendRegionRule(sender, "允许PVP", region.getAllowPVP());
        sendRegionRule(sender, "允许丢弃物品", region.getAllowItemDrop());
        sendRegionRule(sender, "允许拾取物品", region.getAllowItemPickup());

        sender.sendMessage(Component.text("【提示信息】").color(NamedTextColor.AQUA));
        if (region.getEnterTitle() != null) {
            sender.sendMessage(Component.text("  进入标题: ").color(NamedTextColor.WHITE)
                    .append(Component.text(region.getEnterTitle()).color(NamedTextColor.GREEN)));
        }
        if (region.getEnterSubtitle() != null) {
            sender.sendMessage(Component.text("  进入副标题: ").color(NamedTextColor.WHITE)
                    .append(Component.text(region.getEnterSubtitle()).color(NamedTextColor.GREEN)));
        }
        if (region.getLeaveTitle() != null) {
            sender.sendMessage(Component.text("  离开标题: ").color(NamedTextColor.WHITE)
                    .append(Component.text(region.getLeaveTitle()).color(NamedTextColor.GREEN)));
        }
        if (region.getLeaveSubtitle() != null) {
            sender.sendMessage(Component.text("  离开副标题: ").color(NamedTextColor.WHITE)
                    .append(Component.text(region.getLeaveSubtitle()).color(NamedTextColor.GREEN)));
        }
    }

    private void sendRegionRule(CommandSender sender, String ruleName, Boolean value) {
        if (value == null) {
            sender.sendMessage(Component.text("  " + ruleName + ": ").color(NamedTextColor.WHITE)
                    .append(Component.text("[使用世界规则]").color(NamedTextColor.GRAY)));
        } else {
            Component status = value
                    ? Component.text("[允许]").color(NamedTextColor.GREEN)
                    : Component.text("[禁止]").color(NamedTextColor.RED);
            sender.sendMessage(Component.text("  " + ruleName + ": ").color(NamedTextColor.WHITE).append(status));
        }
    }

    private void handleRegionsList(CommandSender sender) {
        sender.sendMessage(Component.text("===== 所有保护区域 =====").color(NamedTextColor.GOLD));

        if (plugin.getRegionManager().getRegionCount() == 0) {
            sender.sendMessage(Component.text("暂无保护区域").color(NamedTextColor.YELLOW));
            return;
        }

        for (ProtectedRegion region : plugin.getRegionManager().getAllRegions()) {
            sender.sendMessage(Component.text("- " + region.getName()).color(NamedTextColor.WHITE)
                    .append(Component.text(" @ " + region.getWorldName()).color(NamedTextColor.GRAY)));
        }

        sender.sendMessage(Component.text("总计: " + plugin.getRegionManager().getRegionCount() + " 个区域").color(NamedTextColor.GRAY));
    }

    private void handleSetRegionRule(CommandSender sender, String[] args) {
        if (args.length < 4) {
            sender.sendMessage(miniMessage.red("用法: /gworldrules setregion <区域名> <规则> <值>"));
            sender.sendMessage(miniMessage.yellow("可用规则: allow-break, allow-place, allow-interact, allow-pvp, enter-title, leave-title"));
            return;
        }

        String regionName = args[1];
        String ruleName = args[2].toLowerCase();
        String value = args[3];

        ProtectedRegion region = plugin.getRegionManager().getRegion(regionName);
        if (region == null) {
            sender.sendMessage(miniMessage.red("区域 " + regionName + " 不存在！"));
            return;
        }

        switch (ruleName) {
            case "allow-break":
                region.setAllowBreak(parseBoolean(value));
                break;
            case "allow-place":
                region.setAllowPlace(parseBoolean(value));
                break;
            case "allow-interact":
                region.setAllowInteract(parseBoolean(value));
                break;
            case "allow-pvp":
                region.setAllowPVP(parseBoolean(value));
                break;
            case "allow-item-drop":
                region.setAllowItemDrop(parseBoolean(value));
                break;
            case "allow-item-pickup":
                region.setAllowItemPickup(parseBoolean(value));
                break;
            case "enter-title":
                region.setEnterTitle(value.equals("null") ? null : value.replace("_", " "));
                break;
            case "enter-subtitle":
                region.setEnterSubtitle(value.equals("null") ? null : value.replace("_", " "));
                break;
            case "leave-title":
                region.setLeaveTitle(value.equals("null") ? null : value.replace("_", " "));
                break;
            case "leave-subtitle":
                region.setLeaveSubtitle(value.equals("null") ? null : value.replace("_", " "));
                break;
            default:
                sender.sendMessage(miniMessage.red("未知规则: " + ruleName));
                return;
        }

        plugin.getRegionManager().saveRegions();
        sender.sendMessage(miniMessage.green("区域 " + regionName + " 的规则 " + ruleName + " 已设置为: " + value));
    }

    private Boolean parseBoolean(String value) {
        if (value.equalsIgnoreCase("null") || value.equalsIgnoreCase("default")) {
            return null;
        }
        return Boolean.parseBoolean(value);
    }

    // ========== 区块裁剪方法 ==========

    private void handleTrim(CommandSender sender, String[] args) {
        if (args.length < 3) {
            sender.sendMessage(miniMessage.red("用法: /gworldrules trim <世界名> <区域名> [delete]"));
            sender.sendMessage(miniMessage.yellow("delete 参数表示是否删除区块文件（不可恢复！）"));
            return;
        }

        String worldName = args[1];
        String regionName = args[2];
        boolean deleteFiles = args.length > 3 && args[3].equalsIgnoreCase("delete");

        World world = Bukkit.getWorld(worldName);
        if (world == null) {
            sender.sendMessage(miniMessage.red("世界 " + worldName + " 不存在！"));
            return;
        }

        ProtectedRegion region = plugin.getRegionManager().getRegion(regionName);
        if (region == null) {
            sender.sendMessage(miniMessage.red("区域 " + regionName + " 不存在！"));
            return;
        }

        if (!region.getWorldName().equals(worldName)) {
            sender.sendMessage(miniMessage.red("区域 " + regionName + " 不在世界 " + worldName + " 中！"));
            return;
        }

        // 警告确认
        if (deleteFiles) {
            sender.sendMessage(miniMessage.red("⚠ 警告: 这将永久删除区域外的区块文件！"));
            sender.sendMessage(miniMessage.yellow("如果确定，请在5秒内再次执行相同命令"));
        }

        sender.sendMessage(miniMessage.aqua("开始裁剪世界 " + worldName + " ..."));

        ChunkTrimmer trimmer = new ChunkTrimmer(plugin);
        ChunkTrimmer.TrimResult result = trimmer.trimWorld(world, region, deleteFiles);

        sender.sendMessage(Component.text("===== 裁剪完成 =====").color(NamedTextColor.GOLD));
        sender.sendMessage(Component.text("世界: ").color(NamedTextColor.WHITE)
                .append(Component.text(result.worldName).color(NamedTextColor.YELLOW)));
        sender.sendMessage(Component.text("保留区域: ").color(NamedTextColor.WHITE)
                .append(Component.text(result.regionName).color(NamedTextColor.YELLOW)));
        sender.sendMessage(Component.text("世界边界: ").color(NamedTextColor.WHITE)
                .append(Component.text(result.borderSet ? "已设置" : "失败").color(result.borderSet ? NamedTextColor.GREEN : NamedTextColor.RED)));
        sender.sendMessage(Component.text("区域内区块数: ").color(NamedTextColor.WHITE)
                .append(Component.text(String.valueOf(result.totalChunksInRange)).color(NamedTextColor.AQUA)));
        sender.sendMessage(Component.text("卸载区块数: ").color(NamedTextColor.WHITE)
                .append(Component.text(String.valueOf(result.unloadedChunks)).color(NamedTextColor.AQUA)));
        if (deleteFiles) {
            sender.sendMessage(Component.text("删除文件数: ").color(NamedTextColor.WHITE)
                    .append(Component.text(String.valueOf(result.deletedFiles)).color(NamedTextColor.RED)));
        }

        sender.sendMessage(miniMessage.green("区块裁剪完成！"));
        sender.sendMessage(miniMessage.yellow("使用 /gworldrules trimlock " + worldName + " " + regionName + " 来锁定裁剪区域"));
    }

    private void handleTrimLock(CommandSender sender, String[] args) {
        if (args.length < 3) {
            sender.sendMessage(miniMessage.red("用法: /gworldrules trimlock <世界名> <区域名>"));
            return;
        }

        String worldName = args[1];
        String regionName = args[2];

        World world = Bukkit.getWorld(worldName);
        if (world == null) {
            sender.sendMessage(miniMessage.red("世界 " + worldName + " 不存在！"));
            return;
        }

        ProtectedRegion region = plugin.getRegionManager().getRegion(regionName);
        if (region == null) {
            sender.sendMessage(miniMessage.red("区域 " + regionName + " 不存在！"));
            return;
        }

        if (!region.getWorldName().equals(worldName)) {
            sender.sendMessage(miniMessage.red("区域 " + regionName + " 不在世界 " + worldName + " 中！"));
            return;
        }

        ChunkLoadListener listener = plugin.getChunkLoadListener();
        if (listener == null) {
            sender.sendMessage(miniMessage.red("区块加载监听器未启用！"));
            return;
        }

        listener.setTrimRegion(worldName, regionName);
        listener.setEnabled(true);

        sender.sendMessage(miniMessage.green("已锁定世界 " + worldName + " 的裁剪区域为 " + regionName));
        sender.sendMessage(miniMessage.yellow("区域外的区块将不会被加载"));
    }

    private void handleTrimUnlock(CommandSender sender, String[] args) {
        if (args.length < 2) {
            sender.sendMessage(miniMessage.red("用法: /gworldrules trimunlock <世界名>"));
            return;
        }

        String worldName = args[1];

        ChunkLoadListener listener = plugin.getChunkLoadListener();
        if (listener == null) {
            sender.sendMessage(miniMessage.red("区块加载监听器未启用！"));
            return;
        }

        String oldRegion = listener.getTrimRegion(worldName);
        if (oldRegion == null) {
            sender.sendMessage(miniMessage.yellow("世界 " + worldName + " 没有设置裁剪区域"));
            return;
        }

        listener.removeTrimRegion(worldName);

        sender.sendMessage(miniMessage.green("已解锁世界 " + worldName + " 的裁剪区域"));
    }

    private void handleTrimInfo(CommandSender sender) {
        ChunkLoadListener listener = plugin.getChunkLoadListener();
        if (listener == null) {
            sender.sendMessage(miniMessage.red("区块加载监听器未启用！"));
            return;
        }

        sender.sendMessage(Component.text("===== 区块裁剪状态 =====").color(NamedTextColor.GOLD));
        sender.sendMessage(Component.text("监听器状态: ").color(NamedTextColor.WHITE)
                .append(Component.text(listener.isEnabled() ? "启用" : "禁用").color(listener.isEnabled() ? NamedTextColor.GREEN : NamedTextColor.RED)));

        Map<String, String> trimRegions = listener.getAllTrimRegions();
        if (trimRegions.isEmpty()) {
            sender.sendMessage(Component.text("暂无锁定的裁剪区域").color(NamedTextColor.YELLOW));
        } else {
            sender.sendMessage(Component.text("【锁定的裁剪区域】").color(NamedTextColor.AQUA));
            for (Map.Entry<String, String> entry : trimRegions.entrySet()) {
                sender.sendMessage(Component.text("  " + entry.getKey() + " -> " + entry.getValue()).color(NamedTextColor.WHITE));
            }
        }
    }
}
