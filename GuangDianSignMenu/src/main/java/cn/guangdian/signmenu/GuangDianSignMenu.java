package cn.guangdian.signmenu;

import cn.guangdian.rpgcore.message.MiniMessageService;
import cn.guangdian.rpgcore.plugin.AbstractRPGPlugin;
import cn.guangdian.signmenu.config.SignMenuConfig;
import cn.guangdian.signmenu.listener.SignCommandListener;
import org.bukkit.command.PluginCommand;

public final class GuangDianSignMenu extends AbstractRPGPlugin {

    private static GuangDianSignMenu instance;
    private SignMenuConfig configManager;
    private MiniMessageService miniMessage;

    @Override
    protected void onPluginEnable() {
        instance = this;
        miniMessage = MiniMessageService.getInstance();

        configManager = new SignMenuConfig(this);
        configManager.load();

        getServer().getPluginManager().registerEvents(new SignCommandListener(this), this);

        registerCommands();

        getLogger().info("GuangDianSignMenu 已启用！");
    }

    @Override
    protected void onPluginDisable() {
        if (scheduler != null) {
            scheduler.cancelAllTasks();
        }

        getLogger().info("GuangDianSignMenu 已关闭！");
    }

    @Override
    protected String getPluginName() {
        return "GuangDianSignMenu";
    }

    private void registerCommands() {
        PluginCommand cmd = getCommand("signmenu");
        if (cmd != null) {
            cmd.setExecutor((sender, command, label, args) -> {
                if (args.length > 0 && args[0].equalsIgnoreCase("reload")) {
                    if (!sender.hasPermission("signmenu.admin")) {
                        sender.sendMessage(miniMessage.red("没有权限执行此操作"));
                        return true;
                    }
                    configManager.reload();
                    sender.sendMessage(miniMessage.green("配置已重新加载！"));
                    return true;
                }
                sender.sendMessage(miniMessage.colorize("<yellow>用法: /signmenu reload"));
                return true;
            });
        }
    }

    public static GuangDianSignMenu getInstance() {
        return instance;
    }

    public SignMenuConfig getConfigManager() {
        return configManager;
    }
}
