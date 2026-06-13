package cn.guangdian.portal.command;

import cn.guangdian.portal.GuangDianPortal;
import cn.guangdian.portal.manager.PortalManager;
import cn.guangdian.portal.model.Portal;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class PortalCommand implements CommandExecutor, TabCompleter {

    private final GuangDianPortal plugin;
    private final PortalManager portalManager;

    public PortalCommand(GuangDianPortal plugin) {
        this.plugin = plugin;
        this.portalManager = plugin.getPortalManager();
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length == 0) {
            sendHelp(sender);
            return true;
        }

        String subCommand = args[0].toLowerCase();

        switch (subCommand) {
            case "create" -> handleCreate(sender, args);
            case "delete" -> handleDelete(sender, args);
            case "list" -> handleList(sender);
            case "info" -> handleInfo(sender, args);
            case "setdest" -> handleSetDestination(sender, args);
            case "setpermission" -> handleSetPermission(sender, args);
            case "enable" -> handleEnable(sender, args, true);
            case "disable" -> handleEnable(sender, args, false);
            case "reload" -> handleReload(sender);
            case "help" -> sendHelp(sender);
            default -> sendHelp(sender);
        }

        return true;
    }

    private void handleCreate(CommandSender sender, String[] args) {
        if (!(sender instanceof Player player)) {
            plugin.sendMessage(sender, "<red>只有玩家可以使用此命令!");
            return;
        }

        if (!player.hasPermission("guangdian.portal.create")) {
            plugin.sendMessage(player, "<red>没有权限!");
            return;
        }

        if (args.length < 2) {
            plugin.sendMessage(player, "<red>用法: /portal create <名称> [框架材质]");
            return;
        }

        if (!portalManager.hasCompleteSelection(player)) {
            plugin.sendMessage(player, "<red>请先用木斧选择两个角点!");
            return;
        }

        String name = args[1];
        Material frameMaterial = Material.OBSIDIAN;

        if (args.length >= 3) {
            try {
                frameMaterial = Material.valueOf(args[2].toUpperCase());
            } catch (IllegalArgumentException e) {
                plugin.sendMessage(player, "<red>无效的材质: " + args[2]);
                return;
            }
        }

        Location[] selections = portalManager.getPlayerSelection(player);
        Location loc1 = selections[0];
        Location loc2 = selections[1];

        boolean success = portalManager.createPortal(
            name,
            loc1.getWorld().getName(),
            loc1.getBlockX(), loc1.getBlockY(), loc1.getBlockZ(),
            loc2.getBlockX(), loc2.getBlockY(), loc2.getBlockZ(),
            frameMaterial
        );

        if (success) {
            plugin.sendMessage(player, "<green>成功创建传送门 <yellow>" + name + "<green>!");
            plugin.sendMessage(player, "<gray>使用 <yellow>/portal setdest " + name + " <gray>设置目的地");
            portalManager.clearPlayerSelection(player);
        } else {
            plugin.sendMessage(player, "<red>传送门 <yellow>" + name + " <red>已存在!");
        }
    }

    private void handleDelete(CommandSender sender, String[] args) {
        if (!(sender instanceof Player player)) {
            plugin.sendMessage(sender, "<red>只有玩家可以使用此命令!");
            return;
        }

        if (!player.hasPermission("guangdian.portal.delete")) {
            plugin.sendMessage(player, "<red>没有权限!");
            return;
        }

        if (args.length < 2) {
            plugin.sendMessage(player, "<red>用法: /portal delete <名称>");
            return;
        }

        String name = args[1];
        boolean success = portalManager.deletePortal(name);

        if (success) {
            plugin.sendMessage(player, "<green>已删除传送门 <yellow>" + name + "<green>!");
        } else {
            plugin.sendMessage(player, "<red>传送门 <yellow>" + name + " <red>不存在!");
        }
    }

    private void handleList(CommandSender sender) {
        if (!sender.hasPermission("guangdian.portal.list")) {
            plugin.sendMessage(sender, "<red>没有权限!");
            return;
        }

        List<String> portalNames = portalManager.getPortalNames();

        if (portalNames.isEmpty()) {
            plugin.sendMessage(sender, "<yellow>暂无传送门");
            return;
        }

        plugin.sendMessage(sender, "<gold>===== 传送门列表 =====");

        for (Portal portal : portalManager.getAllPortals()) {
            String status = portal.isEnabled() ? "<green>启用" : "<red>禁用";
            plugin.sendMessage(sender, String.format("<yellow>%s <gray>-> <white>%s <gray>[%s<gray>]",
                portal.getName(), portal.getDestinationString(), status));
        }

        plugin.sendMessage(sender, "<gray>共 <yellow>" + portalNames.size() + " <gray>个传送门");
    }

    private void handleInfo(CommandSender sender, String[] args) {
        if (!sender.hasPermission("guangdian.portal.info")) {
            plugin.sendMessage(sender, "<red>没有权限!");
            return;
        }

        if (args.length < 2) {
            plugin.sendMessage(sender, "<red>用法: /portal info <名称>");
            return;
        }

        String name = args[1];
        Portal portal = portalManager.getPortal(name);

        if (portal == null) {
            plugin.sendMessage(sender, "<red>传送门 <yellow>" + name + " <red>不存在!");
            return;
        }

        plugin.sendMessage(sender, "<gold>===== 传送门信息: <yellow>" + portal.getName() + "<gold> =====");
        plugin.sendMessage(sender, "<gray>世界: <white>" + portal.getWorldName());
        plugin.sendMessage(sender, "<gray>范围: <white>" + portal.getBoundsString());
        plugin.sendMessage(sender, "<gray>目的地: <white>" + portal.getDestinationString());
        plugin.sendMessage(sender, "<gray>框架材质: <white>" + portal.getFrameMaterial().name());
        plugin.sendMessage(sender, "<gray>权限: <white>" + (portal.getPermission() != null ? portal.getPermission() : "无"));
        plugin.sendMessage(sender, "<gray>状态: <white>" + (portal.isEnabled() ? "启用" : "禁用"));
    }

    private void handleSetDestination(CommandSender sender, String[] args) {
        if (!(sender instanceof Player player)) {
            plugin.sendMessage(sender, "<red>只有玩家可以使用此命令!");
            return;
        }

        if (!player.hasPermission("guangdian.portal.setdest")) {
            plugin.sendMessage(player, "<red>没有权限!");
            return;
        }

        if (args.length < 2) {
            plugin.sendMessage(player, "<red>用法: /portal setdest <名称> [目标传送门]");
            return;
        }

        String name = args[1];
        Portal portal = portalManager.getPortal(name);

        if (portal == null) {
            plugin.sendMessage(player, "<red>传送门 <yellow>" + name + " <red>不存在!");
            return;
        }

        if (args.length >= 3) {
            String destPortalName = args[2];
            Portal destPortal = portalManager.getPortal(destPortalName);

            if (destPortal == null) {
                plugin.sendMessage(player, "<red>目标传送门 <yellow>" + destPortalName + " <red>不存在!");
                return;
            }

            portal.setDestinationPortal(destPortalName);
            portal.setDestination(null);
            portalManager.savePortals();

            plugin.sendMessage(player, "<green>已设置传送门 <yellow>" + name + " <green>的目标为传送门 <yellow>" + destPortalName);
        } else {
            Location loc = player.getLocation();
            portal.setDestination(loc);
            portal.setDestinationPortal(null);
            portalManager.savePortals();

            plugin.sendMessage(player, "<green>已设置传送门 <yellow>" + name + " <green>的目的地为当前位置");
        }
    }

    private void handleSetPermission(CommandSender sender, String[] args) {
        if (!sender.hasPermission("guangdian.portal.admin")) {
            plugin.sendMessage(sender, "<red>没有权限!");
            return;
        }

        if (args.length < 2) {
            plugin.sendMessage(sender, "<red>用法: /portal setpermission <名称> [权限节点]");
            return;
        }

        String name = args[1];
        Portal portal = portalManager.getPortal(name);

        if (portal == null) {
            plugin.sendMessage(sender, "<red>传送门 <yellow>" + name + " <red>不存在!");
            return;
        }

        if (args.length >= 3) {
            portal.setPermission(args[2]);
            plugin.sendMessage(sender, "<green>已设置传送门 <yellow>" + name + " <green>的权限为 <yellow>" + args[2]);
        } else {
            portal.setPermission(null);
            plugin.sendMessage(sender, "<green>已清除传送门 <yellow>" + name + " <green>的权限限制");
        }

        portalManager.savePortals();
    }

    private void handleEnable(CommandSender sender, String[] args, boolean enable) {
        if (!sender.hasPermission("guangdian.portal.admin")) {
            plugin.sendMessage(sender, "<red>没有权限!");
            return;
        }

        if (args.length < 2) {
            plugin.sendMessage(sender, "<red>用法: /portal " + (enable ? "enable" : "disable") + " <名称>");
            return;
        }

        String name = args[1];
        Portal portal = portalManager.getPortal(name);

        if (portal == null) {
            plugin.sendMessage(sender, "<red>传送门 <yellow>" + name + " <red>不存在!");
            return;
        }

        portal.setEnabled(enable);
        portalManager.savePortals();

        plugin.sendMessage(sender, "<green>已" + (enable ? "启用" : "禁用") + "传送门 <yellow>" + name);
    }

    private void handleReload(CommandSender sender) {
        if (!sender.hasPermission("guangdian.portal.admin")) {
            plugin.sendMessage(sender, "<red>没有权限!");
            return;
        }

        plugin.reloadConfig();
        portalManager.reload();

        plugin.sendMessage(sender, "<green>配置已重新加载!");
    }

    private void sendHelp(CommandSender sender) {
        plugin.sendMessage(sender, "<gold>===== 传送门系统帮助 =====");
        plugin.sendMessage(sender, "<yellow>木斧左键/右键 <gray>- 选择传送门角点");

        if (sender.hasPermission("guangdian.portal.create")) {
            plugin.sendMessage(sender, "<yellow>/portal create <名称> [材质] <gray>- 创建传送门");
        }
        if (sender.hasPermission("guangdian.portal.delete")) {
            plugin.sendMessage(sender, "<yellow>/portal delete <名称> <gray>- 删除传送门");
        }
        if (sender.hasPermission("guangdian.portal.list")) {
            plugin.sendMessage(sender, "<yellow>/portal list <gray>- 列出所有传送门");
        }
        if (sender.hasPermission("guangdian.portal.info")) {
            plugin.sendMessage(sender, "<yellow>/portal info <名称> <gray>- 查看传送门信息");
        }
        if (sender.hasPermission("guangdian.portal.setdest")) {
            plugin.sendMessage(sender, "<yellow>/portal setdest <名称> [目标] <gray>- 设置目的地");
        }
        if (sender.hasPermission("guangdian.portal.admin")) {
            plugin.sendMessage(sender, "<yellow>/portal setpermission <名称> [权限] <gray>- 设置权限");
            plugin.sendMessage(sender, "<yellow>/portal enable/disable <名称> <gray>- 启用/禁用");
            plugin.sendMessage(sender, "<yellow>/portal reload <gray>- 重载配置");
        }
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        List<String> completions = new ArrayList<>();

        if (args.length == 1) {
            List<String> subCommands = new ArrayList<>(Arrays.asList("list", "info", "help"));

            if (sender.hasPermission("guangdian.portal.create")) {
                subCommands.add("create");
            }
            if (sender.hasPermission("guangdian.portal.delete")) {
                subCommands.add("delete");
            }
            if (sender.hasPermission("guangdian.portal.setdest")) {
                subCommands.add("setdest");
            }
            if (sender.hasPermission("guangdian.portal.admin")) {
                subCommands.add("setpermission");
                subCommands.add("enable");
                subCommands.add("disable");
                subCommands.add("reload");
            }

            for (String subCmd : subCommands) {
                if (subCmd.startsWith(args[0].toLowerCase())) {
                    completions.add(subCmd);
                }
            }
        } else if (args.length == 2) {
            String subCmd = args[0].toLowerCase();
            if (subCmd.equals("delete") || subCmd.equals("info") || 
                subCmd.equals("setdest") || subCmd.equals("setpermission") ||
                subCmd.equals("enable") || subCmd.equals("disable")) {
                for (String name : portalManager.getPortalNames()) {
                    if (name.startsWith(args[1].toLowerCase())) {
                        completions.add(name);
                    }
                }
            } else if (subCmd.equals("create")) {
                completions.add("<名称>");
            }
        } else if (args.length == 3) {
            String subCmd = args[0].toLowerCase();
            if (subCmd.equals("create")) {
                for (Material material : Material.values()) {
                    if (material.name().toLowerCase().startsWith(args[2].toLowerCase())) {
                        completions.add(material.name());
                    }
                }
            } else if (subCmd.equals("setdest")) {
                for (String name : portalManager.getPortalNames()) {
                    if (name.startsWith(args[2].toLowerCase())) {
                        completions.add(name);
                    }
                }
            }
        }

        return completions;
    }
}
