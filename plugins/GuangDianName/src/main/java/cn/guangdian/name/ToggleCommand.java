package cn.guangdian.name;

import cn.guangdian.rpgcore.message.MiniMessageService;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class ToggleCommand implements CommandExecutor {

    private final GuangDianName plugin;
    private final NameDisplayManager displayManager;
    private final MiniMessageService miniMessage;

    public ToggleCommand(GuangDianName plugin, NameDisplayManager displayManager) {
        this.plugin = plugin;
        this.displayManager = displayManager;
        this.miniMessage = plugin.getMiniMessageService();
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage(miniMessage.red("该命令只能由玩家执行!"));
            return true;
        }

        Player player = (Player) sender;

        if (args.length == 0) {
            sendHelp(player);
            return true;
        }

        String subCommand = args[0].toLowerCase();

        switch (subCommand) {
            case "title":
                handleToggleTitle(player);
                break;
            case "guild":
                handleToggleGuild(player);
                break;
            case "marriage":
                handleToggleMarriage(player);
                break;
            case "health":
                handleToggleHealth(player);
                break;
            case "status":
                handleStatus(player);
                break;
            case "help":
                sendHelp(player);
                break;
            default:
                sendMessage(player, "<red>未知的子命令! 使用 /gdnametoggle help 查看帮助");
                break;
        }

        return true;
    }

    private void sendMessage(Player player, String text) {
        player.sendMessage(miniMessage.colorize(text));
    }

    private void handleToggleTitle(Player player) {
        boolean newState = displayManager.toggleTitle(player);
        String status = newState ? "<green>开启" : "<red>关闭";
        sendMessage(player, "<yellow>[头顶显示] <gray>称号显示已" + status);
    }

    private void handleToggleGuild(Player player) {
        boolean newState = displayManager.toggleGuild(player);
        String status = newState ? "<green>开启" : "<red>关闭";
        sendMessage(player, "<yellow>[头顶显示] <gray>工会显示已" + status);
    }

    private void handleToggleMarriage(Player player) {
        boolean newState = displayManager.toggleMarriage(player);
        String status = newState ? "<green>开启" : "<red>关闭";
        sendMessage(player, "<yellow>[头顶显示] <gray>婚姻显示已" + status);
    }

    private void handleToggleHealth(Player player) {
        boolean newState = displayManager.toggleHealth(player);
        String status = newState ? "<green>开启" : "<red>关闭";
        sendMessage(player, "<yellow>[头顶显示] <gray>血量显示已" + status);
    }

    private void handleStatus(Player player) {
        String titleStatus = displayManager.getShowTitleStatus(player);
        String guildStatus = displayManager.getShowGuildStatus(player);
        String marriageStatus = displayManager.getShowMarriageStatus(player);
        String healthStatus = displayManager.getShowHealthStatus(player);

        sendMessage(player, "<gold>===== 头顶显示状态 =====");
        sendMessage(player, "<yellow>血量显示: " + healthStatus);
        sendMessage(player, "<yellow>称号显示: " + titleStatus);
        sendMessage(player, "<yellow>工会显示: " + guildStatus);
        sendMessage(player, "<yellow>婚姻显示: " + marriageStatus);
    }

    private void sendHelp(Player player) {
        sendMessage(player, "<gold>===== 头顶显示帮助 =====");
        sendMessage(player, "<yellow>/gdnametoggle health <gray>- 切换血量显示");
        sendMessage(player, "<yellow>/gdnametoggle title <gray>- 切换称号显示");
        sendMessage(player, "<yellow>/gdnametoggle guild <gray>- 切换工会显示");
        sendMessage(player, "<yellow>/gdnametoggle marriage <gray>- 切换婚姻显示");
        sendMessage(player, "<yellow>/gdnametoggle status <gray>- 查看当前状态");
    }
}