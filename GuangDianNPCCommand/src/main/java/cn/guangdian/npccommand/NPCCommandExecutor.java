package cn.guangdian.npccommand;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;

public class NPCCommandExecutor implements CommandExecutor {

    private final GuangDianNPCCommand plugin;

    public NPCCommandExecutor(GuangDianNPCCommand plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length == 0) {
            sendHelp(sender);
            return true;
        }

        switch (args[0].toLowerCase()) {
            case "add":
                return handleAdd(sender, args);
            case "remove":
                return handleRemove(sender, args);
            case "list":
                return handleList(sender, args);
            case "cooldown":
                return handleCooldown(sender, args);
            case "clear":
                return handleClear(sender, args);
            case "reload":
                return handleReload(sender);
            default:
                sendHelp(sender);
                return true;
        }
    }

    private boolean handleAdd(CommandSender sender, String[] args) {
        if (!sender.hasPermission("npcmd.admin")) {
            sender.sendMessage(plugin.getMiniMessage().colorize(getMessage("no-permission")));
            return true;
        }

        if (args.length < 4) {
            sender.sendMessage(plugin.getMiniMessage().colorize("<red>用法: /npcmd add <npc-id> <type> <command>"));
            return true;
        }

        int npcId;
        try {
            npcId = Integer.parseInt(args[1]);
        } catch (NumberFormatException e) {
            sender.sendMessage(plugin.getMiniMessage().colorize(getMessage("npc-not-found")));
            return true;
        }

        NPCCommandData.CommandType type;
        try {
            type = NPCCommandData.CommandType.valueOf(args[2].toUpperCase());
        } catch (IllegalArgumentException e) {
            sender.sendMessage(plugin.getMiniMessage().colorize("<red>无效的类型! 可用类型: console, player, op, command, no_perms"));
            return true;
        }

        StringBuilder commandBuilder = new StringBuilder();
        for (int i = 3; i < args.length; i++) {
            if (i > 3) commandBuilder.append(" ");
            commandBuilder.append(args[i]);
        }
        String cmd = commandBuilder.toString();

        plugin.getNPCCommandService().addCommand(npcId, type, cmd);
        sender.sendMessage(plugin.getMiniMessage().colorize(getMessage("command-added")));
        return true;
    }

    private boolean handleRemove(CommandSender sender, String[] args) {
        if (!sender.hasPermission("npcmd.admin")) {
            sender.sendMessage(plugin.getMiniMessage().colorize(getMessage("no-permission")));
            return true;
        }

        if (args.length < 3) {
            sender.sendMessage(plugin.getMiniMessage().colorize("<red>用法: /npcmd remove <npc-id> <index>"));
            return true;
        }

        int npcId;
        try {
            npcId = Integer.parseInt(args[1]);
        } catch (NumberFormatException e) {
            sender.sendMessage(plugin.getMiniMessage().colorize(getMessage("npc-not-found")));
            return true;
        }

        int index;
        try {
            index = Integer.parseInt(args[2]) - 1;
        } catch (NumberFormatException e) {
            sender.sendMessage(plugin.getMiniMessage().colorize("<red>无效的索引!"));
            return true;
        }

        NPCCommandData data = plugin.getNPCCommandService().getNPCCommandData(npcId);
        if (data == null || index < 0 || index >= data.getCommands().size()) {
            sender.sendMessage(plugin.getMiniMessage().colorize("<red>无效的命令索引!"));
            return true;
        }

        plugin.getNPCCommandService().removeCommand(npcId, index);
        sender.sendMessage(plugin.getMiniMessage().colorize(getMessage("command-removed")));
        return true;
    }

    private boolean handleList(CommandSender sender, String[] args) {
        if (!sender.hasPermission("npcmd.admin")) {
            sender.sendMessage(plugin.getMiniMessage().colorize(getMessage("no-permission")));
            return true;
        }

        if (args.length < 2) {
            sender.sendMessage(plugin.getMiniMessage().colorize("<red>用法: /npcmd list <npc-id>"));
            return true;
        }

        int npcId;
        try {
            npcId = Integer.parseInt(args[1]);
        } catch (NumberFormatException e) {
            sender.sendMessage(plugin.getMiniMessage().colorize(getMessage("npc-not-found")));
            return true;
        }

        NPCCommandData data = plugin.getNPCCommandService().getNPCCommandData(npcId);
        if (data == null || data.getCommands().isEmpty()) {
            sender.sendMessage(plugin.getMiniMessage().colorize(getMessage("no-commands")));
            return true;
        }

        sender.sendMessage(plugin.getMiniMessage().colorize(getMessage("command-list-header")));
        sender.sendMessage(plugin.getMiniMessage().colorize("<yellow>NPC ID: <white>" + npcId));
        sender.sendMessage(plugin.getMiniMessage().colorize("<yellow>冷却时间: <white>" + data.getCooldown() + " 秒"));
        sender.sendMessage(plugin.getMiniMessage().colorize("<yellow>命令列表:"));

        for (int i = 0; i < data.getCommands().size(); i++) {
            NPCCommandData.CommandEntry entry = data.getCommands().get(i);
            String msg = String.format("<gray>%d. <aqua>[%s] <white>%s", i + 1, entry.getType().name().toLowerCase(), entry.getCommand());
            sender.sendMessage(plugin.getMiniMessage().colorize(msg));
        }
        return true;
    }

    private boolean handleCooldown(CommandSender sender, String[] args) {
        if (!sender.hasPermission("npcmd.admin")) {
            sender.sendMessage(plugin.getMiniMessage().colorize(getMessage("no-permission")));
            return true;
        }

        if (args.length < 3) {
            sender.sendMessage(plugin.getMiniMessage().colorize("<red>用法: /npcmd cooldown <npc-id> <seconds>"));
            return true;
        }

        int npcId;
        try {
            npcId = Integer.parseInt(args[1]);
        } catch (NumberFormatException e) {
            sender.sendMessage(plugin.getMiniMessage().colorize(getMessage("npc-not-found")));
            return true;
        }

        long cooldown;
        try {
            cooldown = Long.parseLong(args[2]);
        } catch (NumberFormatException e) {
            sender.sendMessage(plugin.getMiniMessage().colorize("<red>无效的冷却时间!"));
            return true;
        }

        if (!plugin.getNPCCommandService().hasNPCCommandData(npcId)) {
            sender.sendMessage(plugin.getMiniMessage().colorize(getMessage("npc-not-found")));
            return true;
        }

        plugin.getNPCCommandService().setCooldown(npcId, cooldown);
        sender.sendMessage(plugin.getMiniMessage().parseUnified(getMessage("cooldown-set"), "time", String.valueOf(cooldown)));
        return true;
    }

    private boolean handleClear(CommandSender sender, String[] args) {
        if (!sender.hasPermission("npcmd.admin")) {
            sender.sendMessage(plugin.getMiniMessage().colorize(getMessage("no-permission")));
            return true;
        }

        if (args.length < 2) {
            sender.sendMessage(plugin.getMiniMessage().colorize("<red>用法: /npcmd clear <npc-id>"));
            return true;
        }

        int npcId;
        try {
            npcId = Integer.parseInt(args[1]);
        } catch (NumberFormatException e) {
            sender.sendMessage(plugin.getMiniMessage().colorize(getMessage("npc-not-found")));
            return true;
        }

        plugin.getNPCCommandService().removeNPCCommandData(npcId);
        sender.sendMessage(plugin.getMiniMessage().colorize("<green>已清除NPC " + npcId + " 的所有命令!"));
        return true;
    }

    private boolean handleReload(CommandSender sender) {
        if (!sender.hasPermission("npcmd.admin")) {
            sender.sendMessage(plugin.getMiniMessage().colorize(getMessage("no-permission")));
            return true;
        }

        plugin.reloadConfig();
        plugin.getNPCCommandService().reloadConfig();
        sender.sendMessage(plugin.getMiniMessage().colorize(getMessage("reload-success")));
        return true;
    }

    private void sendHelp(CommandSender sender) {
        sender.sendMessage(plugin.getMiniMessage().colorize("<green>=== GuangDianNPCCommand 帮助 ==="));
        sender.sendMessage(plugin.getMiniMessage().colorize("<yellow>/npcmd add <npc-id> <type> <command> <gray>- 添加命令"));
        sender.sendMessage(plugin.getMiniMessage().colorize("<yellow>/npcmd remove <npc-id> <index> <gray>- 移除命令"));
        sender.sendMessage(plugin.getMiniMessage().colorize("<yellow>/npcmd list <npc-id> <gray>- 查看命令列表"));
        sender.sendMessage(plugin.getMiniMessage().colorize("<yellow>/npcmd cooldown <npc-id> <seconds> <gray>- 设置冷却时间"));
        sender.sendMessage(plugin.getMiniMessage().colorize("<yellow>/npcmd clear <npc-id> <gray>- 清除所有命令"));
        sender.sendMessage(plugin.getMiniMessage().colorize("<yellow>/npcmd reload <gray>- 重新加载配置"));
        sender.sendMessage(plugin.getMiniMessage().colorize("<gray>类型: console, player, op, command, no_perms"));
    }

    private String getMessage(String key) {
        String prefix = plugin.getConfig().getString("messages.prefix", "<gray>[<green>NPC命令<gray>] ");
        String message = plugin.getConfig().getString("messages." + key, "<red>消息未找到: " + key);
        return prefix + message;
    }
}
