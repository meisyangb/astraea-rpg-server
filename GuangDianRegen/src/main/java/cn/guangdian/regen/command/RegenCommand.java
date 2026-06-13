package cn.guangdian.regen.command;

import cn.guangdian.regen.GuangDianRegen;
import cn.guangdian.regen.manager.RegionManager;
import cn.guangdian.regen.manager.RegenManager;
import cn.guangdian.regen.manager.SelectionManager;
import cn.guangdian.regen.model.RegenRegion;
import cn.guangdian.regen.model.RegenType;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 主命令处理器
 */
public class RegenCommand implements CommandExecutor, TabCompleter {

    private final GuangDianRegen plugin;
    private final RegionManager regionManager;
    private final SelectionManager selectionManager;
    private final RegenManager regenManager;

    public RegenCommand(GuangDianRegen plugin, RegionManager regionManager,
                        SelectionManager selectionManager, RegenManager regenManager) {
        this.plugin = plugin;
        this.regionManager = regionManager;
        this.selectionManager = selectionManager;
        this.regenManager = regenManager;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length == 0) {
            sendHelp(sender);
            return true;
        }

        String subCommand = args[0].toLowerCase();

        switch (subCommand) {
            case "wand":
                return handleWand(sender);
            case "pos1":
                return handlePos1(sender);
            case "pos2":
                return handlePos2(sender);
            case "create":
                return handleCreate(sender, args);
            case "delete":
                return handleDelete(sender, args);
            case "list":
                return handleList(sender);
            case "info":
                return handleInfo(sender, args);
            case "reload":
                return handleReload(sender);
            case "stats":
                return handleStats(sender);
            case "init":
                return handleInit(sender, args);
            case "test":
                return handleTest(sender);
            default:
                sendHelp(sender);
                return true;
        }
    }

    /**
     * 发送帮助信息
     */
    private void sendHelp(CommandSender sender) {
        String prefix = getMessage("prefix");
        sender.sendMessage(ChatColor.translateAlternateColorCodes('&', prefix + "&e矿场/林场管理命令:"));
        sender.sendMessage(ChatColor.translateAlternateColorCodes('&', "&a/regen wand &7- 获取选区工具"));
        sender.sendMessage(ChatColor.translateAlternateColorCodes('&', "&a/regen pos1 &7- 设置第一个点"));
        sender.sendMessage(ChatColor.translateAlternateColorCodes('&', "&a/regen pos2 &7- 设置第二个点"));
        sender.sendMessage(ChatColor.translateAlternateColorCodes('&', "&a/regen create <名称> <类型> &7- 创建区域"));
        sender.sendMessage(ChatColor.translateAlternateColorCodes('&', "&a/regen delete <名称> &7- 删除区域"));
        sender.sendMessage(ChatColor.translateAlternateColorCodes('&', "&a/regen list &7- 列出所有区域"));
        sender.sendMessage(ChatColor.translateAlternateColorCodes('&', "&a/regen info <名称> &7- 查看区域信息"));
        sender.sendMessage(ChatColor.translateAlternateColorCodes('&', "&a/regen init <名称> &7- 初始化区域方块"));
        sender.sendMessage(ChatColor.translateAlternateColorCodes('&', "&a/regen test &7- 测试当前位置"));
        sender.sendMessage(ChatColor.translateAlternateColorCodes('&', "&a/regen stats &7- 查看统计信息"));
        sender.sendMessage(ChatColor.translateAlternateColorCodes('&', "&a/regen reload &7- 重载配置"));
    }

    /**
     * 处理wand命令
     */
    private boolean handleWand(CommandSender sender) {
        if (!(sender instanceof Player)) {
            sender.sendMessage(ChatColor.RED + "此命令只能由玩家执行!");
            return true;
        }

        Player player = (Player) sender;
        if (!player.hasPermission("regen.admin")) {
            player.sendMessage(ChatColor.translateAlternateColorCodes('&',
                    getMessage("prefix") + getMessage("no_permission")));
            return true;
        }

        player.getInventory().addItem(new ItemStack(Material.WOODEN_AXE));
        player.sendMessage(ChatColor.translateAlternateColorCodes('&',
                getMessage("prefix") + getMessage("wand_given")));

        return true;
    }

    /**
     * 处理pos1命令
     */
    private boolean handlePos1(CommandSender sender) {
        if (!(sender instanceof Player)) {
            sender.sendMessage(ChatColor.RED + "此命令只能由玩家执行!");
            return true;
        }

        Player player = (Player) sender;
        selectionManager.setPos1(player, player.getLocation());

        int x = player.getLocation().getBlockX();
        int y = player.getLocation().getBlockY();
        int z = player.getLocation().getBlockZ();

        String msg = getMessage("selection_pos1")
                .replace("{x}", String.valueOf(x))
                .replace("{y}", String.valueOf(y))
                .replace("{z}", String.valueOf(z));

        player.sendMessage(ChatColor.translateAlternateColorCodes('&', getMessage("prefix") + msg));

        return true;
    }

    /**
     * 处理pos2命令
     */
    private boolean handlePos2(CommandSender sender) {
        if (!(sender instanceof Player)) {
            sender.sendMessage(ChatColor.RED + "此命令只能由玩家执行!");
            return true;
        }

        Player player = (Player) sender;
        selectionManager.setPos2(player, player.getLocation());

        int x = player.getLocation().getBlockX();
        int y = player.getLocation().getBlockY();
        int z = player.getLocation().getBlockZ();

        String msg = getMessage("selection_pos2")
                .replace("{x}", String.valueOf(x))
                .replace("{y}", String.valueOf(y))
                .replace("{z}", String.valueOf(z));

        player.sendMessage(ChatColor.translateAlternateColorCodes('&', getMessage("prefix") + msg));

        return true;
    }

    /**
     * 处理create命令
     */
    private boolean handleCreate(CommandSender sender, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage(ChatColor.RED + "此命令只能由玩家执行!");
            return true;
        }

        if (args.length < 3) {
            sender.sendMessage(ChatColor.RED + "用法: /regen create <名称> <类型>");
            return true;
        }

        Player player = (Player) sender;
        if (!player.hasPermission("regen.create")) {
            player.sendMessage(ChatColor.translateAlternateColorCodes('&',
                    getMessage("prefix") + getMessage("no_permission")));
            return true;
        }

        String name = args[1];
        String typeStr = args[2].toUpperCase();

        // 检查类型
        RegenType type;
        try {
            type = RegenType.valueOf(typeStr);
        } catch (IllegalArgumentException e) {
            sender.sendMessage(ChatColor.RED + "无效的区域类型! 可用类型: MINE, FOREST, FARM");
            return true;
        }

        // 检查选区
        if (!selectionManager.isValidSelection(player)) {
            player.sendMessage(ChatColor.translateAlternateColorCodes('&',
                    getMessage("prefix") + getMessage("selection_invalid")));
            return true;
        }

        SelectionManager.Selection selection = selectionManager.getSelection(player);
        String worldName = selection.getWorldName();

        // 检查区域数量限制
        int maxRegions = plugin.getConfig().getInt("settings.max_regions_per_world", 10);
        if (regionManager.getRegionCount(worldName) >= maxRegions) {
            sender.sendMessage(ChatColor.RED + "该世界的区域数量已达上限!");
            return true;
        }

        // 创建区域
        boolean success = regionManager.createRegion(name, type, worldName,
                selection.getMinX(), selection.getMinY(), selection.getMinZ(),
                selection.getMaxX(), selection.getMaxY(), selection.getMaxZ());

        if (success) {
            String msg = getMessage("region_created").replace("{name}", name);
            player.sendMessage(ChatColor.translateAlternateColorCodes('&', getMessage("prefix") + msg));
        } else {
            sender.sendMessage(ChatColor.RED + "区域已存在!");
        }

        return true;
    }

    /**
     * 处理delete命令
     */
    private boolean handleDelete(CommandSender sender, String[] args) {
        if (args.length < 2) {
            sender.sendMessage(ChatColor.RED + "用法: /regen delete <名称>");
            return true;
        }

        if (!sender.hasPermission("regen.delete")) {
            sender.sendMessage(ChatColor.translateAlternateColorCodes('&',
                    getMessage("prefix") + getMessage("no_permission")));
            return true;
        }

        String name = args[1];
        boolean success = regionManager.deleteRegion(name);

        if (success) {
            String msg = getMessage("region_deleted").replace("{name}", name);
            sender.sendMessage(ChatColor.translateAlternateColorCodes('&', getMessage("prefix") + msg));
        } else {
            String msg = getMessage("region_not_found").replace("{name}", name);
            sender.sendMessage(ChatColor.translateAlternateColorCodes('&', getMessage("prefix") + msg));
        }

        return true;
    }

    /**
     * 处理list命令
     */
    private boolean handleList(CommandSender sender) {
        String prefix = getMessage("prefix");
        sender.sendMessage(ChatColor.translateAlternateColorCodes('&', prefix + "&e所有区域:"));

        Map<String, RegenRegion> regions = regionManager.getRegions();
        if (regions.isEmpty()) {
            sender.sendMessage(ChatColor.GRAY + "  无区域");
            return true;
        }

        for (Map.Entry<String, RegenRegion> entry : regions.entrySet()) {
            String name = entry.getKey();
            RegenRegion region = entry.getValue();

            String status = region.isEnabled() ? "&a启用" : "&c禁用";
            sender.sendMessage(ChatColor.translateAlternateColorCodes('&',
                    String.format("&6%s &7- &e%s &7| &f%s &7| %s",
                            name, region.getType().getDisplayName(), region.getWorldName(), status)));
        }

        return true;
    }

    /**
     * 处理info命令
     */
    private boolean handleInfo(CommandSender sender, String[] args) {
        if (args.length < 2) {
            sender.sendMessage(ChatColor.RED + "用法: /regen info <名称>");
            return true;
        }

        String name = args[1];
        RegenRegion region = regionManager.getRegion(name);

        if (region == null) {
            String msg = getMessage("region_not_found").replace("{name}", name);
            sender.sendMessage(ChatColor.translateAlternateColorCodes('&', getMessage("prefix") + msg));
            return true;
        }

        String prefix = getMessage("prefix");
        sender.sendMessage(ChatColor.translateAlternateColorCodes('&', prefix + "&e区域信息:"));
        sender.sendMessage(ChatColor.translateAlternateColorCodes('&',
                "&a名称: &f" + region.getName()));
        sender.sendMessage(ChatColor.translateAlternateColorCodes('&',
                "&a类型: &f" + region.getType().getDisplayName()));
        sender.sendMessage(ChatColor.translateAlternateColorCodes('&',
                "&a世界: &f" + region.getWorldName()));
        sender.sendMessage(ChatColor.translateAlternateColorCodes('&',
                String.format("&a范围: &f(%d,%d,%d) -> (%d,%d,%d)",
                        region.getMinX(), region.getMinY(), region.getMinZ(),
                        region.getMaxX(), region.getMaxY(), region.getMaxZ())));
        sender.sendMessage(ChatColor.translateAlternateColorCodes('&',
                "&a体积: &f" + region.getVolume() + " 方块"));
        sender.sendMessage(ChatColor.translateAlternateColorCodes('&',
                "&a状态: " + (region.isEnabled() ? "&a启用" : "&c禁用")));
        sender.sendMessage(ChatColor.translateAlternateColorCodes('&',
                "&a配置方块: &f" + region.getBlockConfigs().size() + " 种"));

        return true;
    }

    /**
     * 处理reload命令
     */
    private boolean handleReload(CommandSender sender) {
        if (!sender.hasPermission("regen.reload")) {
            sender.sendMessage(ChatColor.translateAlternateColorCodes('&',
                    getMessage("prefix") + getMessage("no_permission")));
            return true;
        }

        plugin.reloadConfig();
        regionManager.loadRegions();

        sender.sendMessage(ChatColor.translateAlternateColorCodes('&',
                getMessage("prefix") + getMessage("reloaded")));

        return true;
    }

    /**
     * 处理stats命令
     */
    private boolean handleStats(CommandSender sender) {
        String prefix = getMessage("prefix");
        sender.sendMessage(ChatColor.translateAlternateColorCodes('&', prefix + "&e统计信息:"));
        sender.sendMessage(ChatColor.translateAlternateColorCodes('&',
                "&a总区域数: &f" + regionManager.getRegions().size()));
        sender.sendMessage(ChatColor.translateAlternateColorCodes('&',
                "&a待刷新方块: &f" + regenManager.getPendingCount()));

        return true;
    }

    /**
     * 处理init命令 - 初始化区域内的方块
     */
    private boolean handleInit(CommandSender sender, String[] args) {
        if (!sender.hasPermission("regen.admin")) {
            sender.sendMessage(ChatColor.RED + "你没有权限执行此操作!");
            return true;
        }

        if (args.length < 2) {
            sender.sendMessage(ChatColor.RED + "用法: /regen init <区域名称>");
            return true;
        }

        String regionName = args[1];
        RegenRegion region = regionManager.getRegion(regionName);

        if (region == null) {
            sender.sendMessage(ChatColor.RED + "区域 " + regionName + " 不存在!");
            return true;
        }

        if (region.getType() != RegenType.MINE && region.getType() != RegenType.FARM) {
            sender.sendMessage(ChatColor.RED + "只有矿场或农场类型的区域才能初始化!");
            return true;
        }

        org.bukkit.World world = plugin.getServer().getWorld(region.getWorldName());
        if (world == null) {
            sender.sendMessage(ChatColor.RED + "世界 " + region.getWorldName() + " 不存在!");
            return true;
        }

        // 统计区域内的方块
        int count = 0;
        java.util.Map<Material, Integer> blockCounts = new java.util.HashMap<>();

        for (int x = region.getMinX(); x <= region.getMaxX(); x++) {
            for (int y = region.getMinY(); y <= region.getMaxY(); y++) {
                for (int z = region.getMinZ(); z <= region.getMaxZ(); z++) {
                    org.bukkit.block.Block block = world.getBlockAt(x, y, z);
                    Material material = block.getType();

                    if (region.hasBlockConfig(material)) {
                        count++;
                        blockCounts.put(material, blockCounts.getOrDefault(material, 0) + 1);
                    }
                }
            }
        }

        sender.sendMessage(ChatColor.GREEN + "区域 " + regionName + " 初始化完成:");
        sender.sendMessage(ChatColor.YELLOW + "找到 " + count + " 个可刷新方块");

        for (Map.Entry<Material, Integer> entry : blockCounts.entrySet()) {
            sender.sendMessage(ChatColor.AQUA + "  " + entry.getKey().name() + ": " + entry.getValue() + " 个");
        }

        sender.sendMessage(ChatColor.GRAY + "这些方块已经是原始状态，破坏后会自动刷新");

        return true;
    }

    /**
     * 处理test命令 - 在当前位置测试刷新功能
     */
    private boolean handleTest(CommandSender sender) {
        if (!(sender instanceof Player)) {
            sender.sendMessage(ChatColor.RED + "此命令只能由玩家执行!");
            return true;
        }

        Player player = (Player) sender;
        org.bukkit.Location location = player.getLocation();
        RegenRegion region = regionManager.getRegionAt(location);

        if (region == null) {
            sender.sendMessage(ChatColor.RED + "你不在任何刷新区域内!");
            return true;
        }

        sender.sendMessage(ChatColor.GREEN + "当前区域: " + region.getName());
        sender.sendMessage(ChatColor.YELLOW + "区域类型: " + region.getType().getDisplayName());
        sender.sendMessage(ChatColor.YELLOW + "世界: " + region.getWorldName());
        sender.sendMessage(ChatColor.YELLOW + "范围: (" + region.getMinX() + "," + region.getMinY() + "," + region.getMinZ() +
                ") -> (" + region.getMaxX() + "," + region.getMaxY() + "," + region.getMaxZ() + ")");
        sender.sendMessage(ChatColor.YELLOW + "待刷新方块数: " + regenManager.getPendingCount());

        // 显示配置的方块类型
        if (region.getType() == RegenType.MINE || region.getType() == RegenType.FARM) {
            sender.sendMessage(ChatColor.AQUA + "配置的方块类型:");
            for (Map.Entry<Material, cn.guangdian.regen.model.RegenBlock> entry : region.getBlockConfigs().entrySet()) {
                sender.sendMessage(ChatColor.GRAY + "  - " + entry.getKey().name());
            }
        }

        return true;
    }

    /**
     * 获取消息
     */
    private String getMessage(String key) {
        return plugin.getConfig().getString("messages." + key, "");
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length == 1) {
            List<String> subCommands = Arrays.asList("wand", "pos1", "pos2", "create", "delete", "list", "info", "init", "test", "reload", "stats");
            return subCommands.stream()
                    .filter(s -> s.startsWith(args[0].toLowerCase()))
                    .collect(Collectors.toList());
        }

        if (args.length == 2) {
            String subCommand = args[0].toLowerCase();
            if (subCommand.equals("delete") || subCommand.equals("info") || subCommand.equals("init")) {
                return new ArrayList<>(regionManager.getRegions().keySet());
            }
            if (subCommand.equals("create")) {
                return new ArrayList<>();
            }
        }

        if (args.length == 3) {
            String subCommand = args[0].toLowerCase();
            if (subCommand.equals("create")) {
                return Arrays.asList("MINE", "FOREST", "FARM");
            }
        }

        return new ArrayList<>();
    }
}
