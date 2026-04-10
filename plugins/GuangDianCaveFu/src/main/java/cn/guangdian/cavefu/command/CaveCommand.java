package cn.guangdian.cavefu.command;

import cn.guangdian.cavefu.GuangDianCaveFu;
import cn.guangdian.cavefu.cave.Cave;
import cn.guangdian.cavefu.cave.CaveLevel;
import cn.guangdian.cavefu.cave.CaveManager;
import cn.guangdian.cavefu.cave.CaveMember;
import cn.guangdian.cavefu.config.ConfigManager;
import cn.guangdian.cavefu.permission.PermissionType;
import cn.guangdian.cavefu.upgrade.UpgradeManager;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 玩家洞府命令
 */
public class CaveCommand implements CommandExecutor, TabCompleter {
    private final GuangDianCaveFu plugin;
    private final CaveManager caveManager;
    private final ConfigManager configManager;
    private final UpgradeManager upgradeManager;

    public CaveCommand(GuangDianCaveFu plugin) {
        this.plugin = plugin;
        this.caveManager = plugin.getCaveManager();
        this.configManager = plugin.getConfigManager();
        this.upgradeManager = plugin.getUpgradeManager();
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage(ChatColor.RED + "此命令只能由玩家执行！");
            return true;
        }

        Player player = (Player) sender;

        if (args.length == 0) {
            sendHelp(player);
            return true;
        }

        String sub = args[0].toLowerCase();

        switch (sub) {
            case "create" -> handleCreate(player);
            case "home" -> handleHome(player);
            case "world" -> handleWorld(player);
            case "sethome" -> handleSetHome(player);
            case "info" -> handleInfo(player, args);
            case "upgrade" -> handleUpgrade(player);
            case "invite" -> handleInvite(player, args);
            case "accept" -> handleAccept(player);
            case "deny" -> handleDeny(player);
            case "kick" -> handleKick(player, args);
            case "members" -> handleMembers(player);
            case "leave" -> handleLeave(player);
            case "transfer" -> handleTransfer(player, args);
            case "visit" -> handleVisit(player, args);
            default -> sendHelp(player);
        }

        return true;
    }

    private void sendHelp(Player player) {
        player.sendMessage(ChatColor.GOLD + "========== 洞府帮助 ==========");
        player.sendMessage(ChatColor.YELLOW + "/cave create " + ChatColor.GRAY + "- 创建洞府");
        player.sendMessage(ChatColor.YELLOW + "/cave home " + ChatColor.GRAY + "- 回到洞府");
        player.sendMessage(ChatColor.YELLOW + "/cave world " + ChatColor.GRAY + "- 传送到洞府世界");
        player.sendMessage(ChatColor.YELLOW + "/cave sethome " + ChatColor.GRAY + "- 设置传送点");
        player.sendMessage(ChatColor.YELLOW + "/cave info " + ChatColor.GRAY + "- 查看洞府信息");
        player.sendMessage(ChatColor.YELLOW + "/cave upgrade " + ChatColor.GRAY + "- 升级洞府");
        player.sendMessage(ChatColor.YELLOW + "/cave invite <玩家> " + ChatColor.GRAY + "- 邀请成员");
        player.sendMessage(ChatColor.YELLOW + "/cave kick <玩家> " + ChatColor.GRAY + "- 移除成员");
        player.sendMessage(ChatColor.YELLOW + "/cave members " + ChatColor.GRAY + "- 查看成员");
        player.sendMessage(ChatColor.YELLOW + "/cave leave " + ChatColor.GRAY + "- 离开洞府");
        player.sendMessage(ChatColor.YELLOW + "/cave transfer <玩家> " + ChatColor.GRAY + "- 转让洞主");
        player.sendMessage(ChatColor.YELLOW + "/cave visit <玩家> " + ChatColor.GRAY + "- 访问洞府");
        player.sendMessage(ChatColor.GOLD + "==============================");
    }

    private void handleCreate(Player player) {
        if (!player.hasPermission("guangdian.cave.create")) {
            player.sendMessage(configManager.getMessage("no-permission"));
            return;
        }

        Cave existing = caveManager.getPlayerCave(player.getUniqueId());
        if (existing != null) {
            player.sendMessage(configManager.getMessage("already-have-cave"));
            return;
        }

        Cave cave = caveManager.createCave(player);
        if (cave == null) {
            player.sendMessage(configManager.getMessage("cave-full"));
            return;
        }

        player.sendMessage(configManager.getMessage("cave-created"));
        player.teleport(cave.getHomeLocation());
    }

    private void handleHome(Player player) {
        if (!player.hasPermission("guangdian.cave.home")) {
            player.sendMessage(configManager.getMessage("no-permission"));
            return;
        }

        Cave cave = caveManager.getPlayerCave(player.getUniqueId());
        if (cave == null) {
            player.sendMessage(configManager.getMessage("no-cave"));
            return;
        }

        player.sendMessage(configManager.getMessage("teleporting"));
        caveManager.teleportHome(player);
    }

    private void handleWorld(Player player) {
        if (!player.hasPermission("guangdian.cave.world")) {
            player.sendMessage(configManager.getMessage("no-permission"));
            return;
        }

        org.bukkit.Location spawnLoc = plugin.getWorldManager().getWorldSpawnLocation();
        if (spawnLoc == null) {
            player.sendMessage(ChatColor.RED + "洞府世界未加载！");
            return;
        }

        player.sendMessage(configManager.getMessage("teleporting"));
        player.teleport(spawnLoc);
    }

    private void handleSetHome(Player player) {
        if (!player.hasPermission("guangdian.cave.sethome")) {
            player.sendMessage(configManager.getMessage("no-permission"));
            return;
        }

        Cave cave = caveManager.getPlayerCave(player.getUniqueId());
        if (cave == null) {
            player.sendMessage(configManager.getMessage("no-cave"));
            return;
        }

        if (!cave.isInside(player.getLocation())) {
            player.sendMessage(configManager.getMessage("not-in-cave"));
            return;
        }

        if (caveManager.setHome(player)) {
            player.sendMessage(configManager.getMessage("home-set"));
        }
    }

    private void handleInfo(Player player, String[] args) {
        Cave cave;

        if (args.length >= 2) {
            // 查看他人洞府
            String targetName = args[1];
            Player target = plugin.getServer().getPlayer(targetName);
            if (target != null) {
                cave = caveManager.getPlayerCave(target.getUniqueId());
            } else {
                // 离线玩家查找
                cave = plugin.getDataManager().getCavesByOwner().values().stream()
                    .filter(c -> c.getOwnerName().equalsIgnoreCase(targetName))
                    .findFirst().orElse(null);
            }
        } else {
            cave = caveManager.getPlayerCave(player.getUniqueId());
        }

        if (cave == null) {
            player.sendMessage(configManager.getMessage("no-cave"));
            return;
        }

        CaveLevel level = configManager.getLevel(cave.getLevel());

        player.sendMessage(ChatColor.GOLD + "========== 洞府信息 ==========");
        player.sendMessage(ChatColor.YELLOW + "洞主: " + ChatColor.WHITE + cave.getOwnerName());
        player.sendMessage(ChatColor.YELLOW + "等级: " + ChatColor.WHITE + (level != null ? level.getName() : cave.getLevel()));
        player.sendMessage(ChatColor.YELLOW + "大小: " + ChatColor.WHITE + (level != null ? level.getSize() + "x" + level.getSize() : "未知"));
        player.sendMessage(ChatColor.YELLOW + "成员: " + ChatColor.WHITE + cave.getMembers().size() + "/" + configManager.getMaxMembers());
        player.sendMessage(ChatColor.GOLD + "==============================");
    }

    private void handleUpgrade(Player player) {
        if (!player.hasPermission("guangdian.cave.upgrade")) {
            player.sendMessage(configManager.getMessage("no-permission"));
            return;
        }

        Cave cave = caveManager.getOwnerCave(player.getUniqueId());
        if (cave == null) {
            player.sendMessage(configManager.getMessage("no-cave"));
            return;
        }

        CaveLevel nextLevel = configManager.getNextLevel(cave.getLevel());
        if (nextLevel == null) {
            player.sendMessage(configManager.getMessage("upgrade-max"));
            return;
        }

        if (!upgradeManager.canUpgrade(player, cave)) {
            String cost = upgradeManager.getUpgradeCostDescription(nextLevel.getLevel());
            player.sendMessage(configManager.getMessage("upgrade-failed") + " 需要: " + cost);
            return;
        }

        if (upgradeManager.upgrade(player, cave)) {
            player.sendMessage(configManager.getMessage("upgrade-success",
                "level", String.valueOf(nextLevel.getLevel()),
                "name", nextLevel.getName()));
        }
    }

    private void handleInvite(Player player, String[] args) {
        if (!player.hasPermission("guangdian.cave.invite")) {
            player.sendMessage(configManager.getMessage("no-permission"));
            return;
        }

        if (args.length < 2) {
            player.sendMessage(ChatColor.RED + "用法: /cave invite <玩家>");
            return;
        }

        String targetName = args[1];
        Player target = plugin.getServer().getPlayer(targetName);
        if (target == null) {
            player.sendMessage(configManager.getMessage("player-not-online"));
            return;
        }

        Cave cave = caveManager.getOwnerCave(player.getUniqueId());
        if (cave == null) {
            player.sendMessage(configManager.getMessage("not-owner"));
            return;
        }

        if (cave.isMember(target.getUniqueId())) {
            player.sendMessage(configManager.getMessage("player-already-member"));
            return;
        }

        if (caveManager.inviteMember(player, target)) {
            player.sendMessage(configManager.getMessage("invite-sent", "player", target.getName()));
            target.sendMessage(configManager.getMessage("invite-received", "player", player.getName()));
        } else {
            player.sendMessage(ChatColor.RED + "邀请失败！");
        }
    }

    private void handleAccept(Player player) {
        // 这里简化处理，实际邀请已在invite时直接加入
        player.sendMessage(configManager.getMessage("invite-none"));
    }

    private void handleDeny(Player player) {
        player.sendMessage(configManager.getMessage("invite-none"));
    }

    private void handleKick(Player player, String[] args) {
        if (!player.hasPermission("guangdian.cave.kick")) {
            player.sendMessage(configManager.getMessage("no-permission"));
            return;
        }

        if (args.length < 2) {
            player.sendMessage(ChatColor.RED + "用法: /cave kick <玩家>");
            return;
        }

        String targetName = args[1];

        Cave cave = caveManager.getOwnerCave(player.getUniqueId());
        if (cave == null) {
            player.sendMessage(configManager.getMessage("not-owner"));
            return;
        }

        // 通过名字找UUID
        java.util.UUID targetUuid = null;
        for (Map.Entry<java.util.UUID, CaveMember> entry : cave.getMembers().entrySet()) {
            if (entry.getValue().getName().equalsIgnoreCase(targetName)) {
                targetUuid = entry.getKey();
                break;
            }
        }

        if (targetUuid == null) {
            player.sendMessage(configManager.getMessage("player-not-member"));
            return;
        }

        if (caveManager.kickMember(player, targetUuid)) {
            player.sendMessage(ChatColor.GREEN + "已移除成员: " + targetName);
            Player target = plugin.getServer().getPlayer(targetUuid);
            if (target != null) {
                target.sendMessage(configManager.getMessage("member-kicked"));
            }
        }
    }

    private void handleMembers(Player player) {
        if (!player.hasPermission("guangdian.cave.members")) {
            player.sendMessage(configManager.getMessage("no-permission"));
            return;
        }

        Cave cave = caveManager.getPlayerCave(player.getUniqueId());
        if (cave == null) {
            player.sendMessage(configManager.getMessage("no-cave"));
            return;
        }

        player.sendMessage(ChatColor.GOLD + "========== 洞府成员 ==========");
        for (CaveMember member : cave.getMembers().values()) {
            String prefix = member.getPermission() == PermissionType.OWNER ? ChatColor.RED + "[洞主]" :
                ChatColor.GREEN + "[成员]";
            player.sendMessage(prefix + " " + ChatColor.WHITE + member.getName());
        }
        player.sendMessage(ChatColor.GOLD + "==============================");
    }

    private void handleLeave(Player player) {
        if (!player.hasPermission("guangdian.cave.leave")) {
            player.sendMessage(configManager.getMessage("no-permission"));
            return;
        }

        Cave cave = caveManager.getPlayerCave(player.getUniqueId());
        if (cave == null) {
            player.sendMessage(configManager.getMessage("no-cave"));
            return;
        }

        if (cave.getOwnerUuid().equals(player.getUniqueId())) {
            player.sendMessage(ChatColor.RED + "洞主不能直接离开，请先转让洞主或解散洞府！");
            return;
        }

        if (caveManager.leaveCave(player.getUniqueId())) {
            player.sendMessage(ChatColor.GREEN + "你已离开洞府！");
        }
    }

    private void handleTransfer(Player player, String[] args) {
        if (!player.hasPermission("guangdian.cave.transfer")) {
            player.sendMessage(configManager.getMessage("no-permission"));
            return;
        }

        if (args.length < 2) {
            player.sendMessage(ChatColor.RED + "用法: /cave transfer <玩家>");
            return;
        }

        String targetName = args[1];
        Player target = plugin.getServer().getPlayer(targetName);
        if (target == null) {
            player.sendMessage(configManager.getMessage("player-not-online"));
            return;
        }

        if (caveManager.transferOwner(player, target)) {
            player.sendMessage(configManager.getMessage("transfer-success", "player", target.getName()));
            target.sendMessage(ChatColor.GREEN + "你已成为新的洞主！");
        } else {
            player.sendMessage(ChatColor.RED + "转让失败！");
        }
    }

    private void handleVisit(Player player, String[] args) {
        if (!player.hasPermission("guangdian.cave.visit")) {
            player.sendMessage(configManager.getMessage("no-permission"));
            return;
        }

        if (args.length < 2) {
            player.sendMessage(ChatColor.RED + "用法: /cave visit <玩家>");
            return;
        }

        String targetName = args[1];

        // 查找目标洞府
        Cave targetCave = null;
        for (Cave cave : plugin.getDataManager().getAllCaves()) {
            if (cave.getOwnerName().equalsIgnoreCase(targetName) || cave.isMember(plugin.getServer().getOfflinePlayer(targetName).getUniqueId())) {
                targetCave = cave;
                break;
            }
        }

        if (targetCave == null) {
            player.sendMessage(configManager.getMessage("target-no-cave"));
            return;
        }

        if (targetCave.isMember(player.getUniqueId())) {
            // 成员直接传送
            player.sendMessage(configManager.getMessage("teleporting"));
            player.teleport(targetCave.getHomeLocation());
        } else if (configManager.isAllowVisitor()) {
            // 访客模式
            player.sendMessage(configManager.getMessage("teleporting"));
            player.sendMessage(configManager.getMessage("visitor-allowed"));
            player.teleport(targetCave.getHomeLocation());
        } else {
            player.sendMessage(configManager.getMessage("visitor-denied"));
        }
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        List<String> completions = new ArrayList<>();

        if (args.length == 1) {
            completions.addAll(Arrays.asList(
                "create", "home", "world", "sethome", "info", "upgrade",
                "invite", "kick", "members", "leave", "transfer", "visit"
            ));
        } else if (args.length == 2) {
            String sub = args[0].toLowerCase();
            if (sub.equals("invite") || sub.equals("kick") || sub.equals("transfer")) {
                for (Player p : plugin.getServer().getOnlinePlayers()) {
                    completions.add(p.getName());
                }
            } else if (sub.equals("visit") || sub.equals("info")) {
                for (Cave cave : plugin.getDataManager().getAllCaves()) {
                    completions.add(cave.getOwnerName());
                }
            }
        }

        String lastArg = args[args.length - 1].toLowerCase();
        return completions.stream()
            .filter(s -> s.toLowerCase().startsWith(lastArg))
            .collect(Collectors.toList());
    }
}