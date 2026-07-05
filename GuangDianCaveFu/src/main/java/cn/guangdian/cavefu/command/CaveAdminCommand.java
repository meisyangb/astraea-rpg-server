package cn.guangdian.cavefu.command;

import cn.guangdian.cavefu.GuangDianCaveFu;
import cn.guangdian.cavefu.cave.Cave;
import cn.guangdian.cavefu.cave.CaveLevel;
import cn.guangdian.cavefu.config.ConfigManager;
import cn.guangdian.cavefu.storage.DataManager;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 管理员洞府命令
 * 使用 Adventure MiniMessage 发送彩色消息
 */
public class CaveAdminCommand implements CommandExecutor, TabCompleter {
    private final GuangDianCaveFu plugin;
    private final ConfigManager configManager;
    private final DataManager dataManager;

    public CaveAdminCommand(GuangDianCaveFu plugin) {
        this.plugin = plugin;
        this.configManager = plugin.getConfigManager();
        this.dataManager = plugin.getDataManager();
    }

    /**
     * 使用 MiniMessage 发送消息
     */
    private void sendMessage(CommandSender sender, String text) {
        plugin.sendMiniMessage(sender, text);
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.hasPermission("guangdian.cave.admin")) {
            sendMessage(sender, configManager.getMessage("no-permission"));
            return true;
        }

        if (args.length == 0) {
            sendHelp(sender);
            return true;
        }

        String sub = args[0].toLowerCase();

        switch (sub) {
            case "tp" -> handleTp(sender, args);
            case "delete" -> handleDelete(sender, args);
            case "setlevel" -> handleSetLevel(sender, args);
            case "reload" -> handleReload(sender);
            case "list" -> handleList(sender);
            default -> sendHelp(sender);
        }

        return true;
    }

    private void sendHelp(CommandSender sender) {
        sendMessage(sender, "<gold>========== 洞府管理帮助 ==========");
        sendMessage(sender, "<yellow>/caveadmin tp <玩家> <gray>- 传送至玩家洞府");
        sendMessage(sender, "<yellow>/caveadmin delete <玩家> <gray>- 删除玩家洞府");
        sendMessage(sender, "<yellow>/caveadmin setlevel <玩家> <等级> <gray>- 设置洞府等级");
        sendMessage(sender, "<yellow>/caveadmin reload <gray>- 重载配置");
        sendMessage(sender, "<yellow>/caveadmin list <gray>- 查看所有洞府");
        sendMessage(sender, "<gold>================================");
    }

    private void handleTp(CommandSender sender, String[] args) {
        if (!(sender instanceof Player)) {
            sendMessage(sender, "<red>此命令只能由玩家执行！");
            return;
        }

        if (args.length < 2) {
            sendMessage(sender, "<red>用法: /caveadmin tp <玩家>");
            return;
        }

        String targetName = args[1];
        OfflinePlayer target = plugin.getServer().getOfflinePlayer(targetName);

        Cave cave = dataManager.getCaveByOwner(target.getUniqueId());
        if (cave == null) {
            sendMessage(sender, configManager.getMessage("target-no-cave"));
            return;
        }

        Player player = (Player) sender;
        player.teleport(cave.getHomeLocation());
        sendMessage(player, "<green>已传送到 " + targetName + " 的洞府");
    }

    private void handleDelete(CommandSender sender, String[] args) {
        if (args.length < 2) {
            sendMessage(sender, "<red>用法: /caveadmin delete <玩家>");
            return;
        }

        String targetName = args[1];
        OfflinePlayer target = plugin.getServer().getOfflinePlayer(targetName);

        Cave cave = dataManager.getCaveByOwner(target.getUniqueId());
        if (cave == null) {
            sendMessage(sender, configManager.getMessage("target-no-cave"));
            return;
        }

        plugin.getCaveManager().deleteCave(target.getUniqueId());
        sendMessage(sender, "<green>已删除 " + targetName + " 的洞府");
    }

    private void handleSetLevel(CommandSender sender, String[] args) {
        if (args.length < 3) {
            sendMessage(sender, "<red>用法: /caveadmin setlevel <玩家> <等级>");
            return;
        }

        String targetName = args[1];
        int level;

        try {
            level = Integer.parseInt(args[2]);
        } catch (NumberFormatException e) {
            sendMessage(sender, "<red>等级必须是数字！");
            return;
        }

        OfflinePlayer target = plugin.getServer().getOfflinePlayer(targetName);

        Cave cave = dataManager.getCaveByOwner(target.getUniqueId());
        if (cave == null) {
            sendMessage(sender, configManager.getMessage("target-no-cave"));
            return;
        }

        CaveLevel levelConfig = configManager.getLevel(level);
        if (levelConfig == null) {
            sendMessage(sender, "<red>无效的等级！");
            return;
        }

        cave.setLevel(level);
        dataManager.saveCave(cave);
        sendMessage(sender, "<green>已将 " + targetName + " 的洞府等级设置为 " + level);
    }

    private void handleReload(CommandSender sender) {
        configManager.reload();
        sendMessage(sender, "<green>配置已重新加载！");
    }

    private void handleList(CommandSender sender) {
        sendMessage(sender, "<gold>========== 洞府列表 ==========");

        int count = 0;
        for (Cave cave : dataManager.getAllCaves()) {
            CaveLevel level = configManager.getLevel(cave.getLevel());
            String levelName = level != null ? level.getName() : "未知";
            sendMessage(sender, "<yellow>" + cave.getOwnerName() + " <gray>- <white>等级: " + levelName + " 成员: " + cave.getMembers().size());
            count++;
        }

        sendMessage(sender, "<gold>总计: <white>" + count + " 个洞府");
        sendMessage(sender, "<gold>================================");
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        List<String> completions = new ArrayList<>();

        if (args.length == 1) {
            completions.addAll(Arrays.asList("tp", "delete", "setlevel", "reload", "list"));
        } else if (args.length == 2) {
            String sub = args[0].toLowerCase();
            if (sub.equals("tp") || sub.equals("delete") || sub.equals("setlevel")) {
                for (Cave cave : dataManager.getAllCaves()) {
                    completions.add(cave.getOwnerName());
                }
            }
        } else if (args.length == 3) {
            String sub = args[0].toLowerCase();
            if (sub.equals("setlevel")) {
                for (int i = 1; i <= configManager.getMaxLevel(); i++) {
                    completions.add(String.valueOf(i));
                }
            }
        }

        String lastArg = args[args.length - 1].toLowerCase();
        return completions.stream()
            .filter(s -> s.toLowerCase().startsWith(lastArg))
            .collect(Collectors.toList());
    }
}
