package cn.guangdian.blockdrop;

import cn.guangdian.rpgcore.RPGCore;
import cn.guangdian.rpgcore.plugin.AbstractRPGPlugin;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.configuration.file.FileConfiguration;

import java.util.ArrayList;
import java.util.List;

public class GuangDianBlockDrop extends AbstractRPGPlugin implements CommandExecutor, TabCompleter {

    private static GuangDianBlockDrop instance;
    private BlockDropListener listener;
    private DropConfigManager configManager;

    @Override
    protected void onPluginEnable() {
        instance = this;

        initCommonServices();

        configManager = new DropConfigManager(this);
        configManager.load();

        MythicMobsIntegration.initialize();

        listener = new BlockDropListener(this);
        Bukkit.getPluginManager().registerEvents(listener, this);

        if (getCommand("blockdrop") != null) {
            getCommand("blockdrop").setExecutor(this);
            getCommand("blockdrop").setTabCompleter(this);
        }

        getLogger().info(getPluginName() + " v" + getDescription().getVersion() + " 已启动!");
        getLogger().info("已加载 " + configManager.getBlockCount() + " 个方块掉落配置");
    }

    @Override
    protected void onPluginDisable() {
        cancelAllTasks();
        getLogger().info(getPluginName() + " 已关闭!");
    }

    @Override
    protected String getPluginName() {
        return "GuangDianBlockDrop";
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!command.getName().equalsIgnoreCase("blockdrop")) {
            return true;
        }

        if (args.length == 0) {
            sendHelp(sender);
            return true;
        }

        switch (args[0].toLowerCase()) {
            case "reload" -> {
                if (!sender.hasPermission("blockdrop.reload")) {
                    sender.sendMessage(miniMessageService.red("没有权限!"));
                    return true;
                }
                configManager.load();
                sender.sendMessage(miniMessageService.green("方块掉落配置已重新加载!"));
                sender.sendMessage(miniMessageService.colorize(
                    "<yellow>已加载 <white>" + configManager.getBlockCount() + " <yellow>个方块掉落配置"));
            }
            case "list" -> {
                if (!sender.hasPermission("blockdrop.admin")) {
                    sender.sendMessage(miniMessageService.red("没有权限!"));
                    return true;
                }
                configManager.sendBlockList(sender);
            }
            case "debug" -> {
                if (!sender.hasPermission("blockdrop.admin")) {
                    sender.sendMessage(miniMessageService.red("没有权限!"));
                    return true;
                }
                boolean current = configManager.isDebug();
                configManager.setDebug(!current);
                sender.sendMessage(miniMessageService.colorize(
                    "<yellow>调试模式: " + (!current ? "<green>开启" : "<red>关闭")));
            }
            default -> sendHelp(sender);
        }

        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        List<String> completions = new ArrayList<>();
        if (args.length == 1) {
            if (sender.hasPermission("blockdrop.reload")) {
                completions.add("reload");
            }
            if (sender.hasPermission("blockdrop.admin")) {
                completions.add("list");
                completions.add("debug");
            }
        }
        return completions;
    }

    private void sendHelp(CommandSender sender) {
        sender.sendMessage(miniMessageService.gold("========== GuangDianBlockDrop 帮助 =========="));
        sender.sendMessage(miniMessageService.colorize("<yellow>/blockdrop reload <gray>- 重新加载配置"));
        sender.sendMessage(miniMessageService.colorize("<yellow>/blockdrop list <gray>- 列出方块掉落配置"));
        sender.sendMessage(miniMessageService.colorize("<yellow>/blockdrop debug <gray>- 切换调试模式"));
        sender.sendMessage(miniMessageService.gold("============================================"));
    }

    public DropConfigManager getDropConfigManager() {
        return configManager;
    }

    public static GuangDianBlockDrop getInstance() {
        return instance;
    }
}