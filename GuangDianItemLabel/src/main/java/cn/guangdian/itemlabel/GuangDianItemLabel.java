package cn.guangdian.itemlabel;

import cn.guangdian.rpgcore.RPGCore;
import cn.guangdian.rpgcore.api.SyncScheduler;
import cn.guangdian.rpgcore.plugin.AbstractRPGPlugin;
import cn.guangdian.rpgcore.message.MiniMessageService;
import org.bukkit.Bukkit;

public class GuangDianItemLabel extends AbstractRPGPlugin {

    private static GuangDianItemLabel instance;
    private ItemLabelManager itemLabelManager;
    private ItemLabelListener itemLabelListener;
    protected MiniMessageService miniMessage;

    @Override
    protected void onPluginEnable() {
        instance = this;
        miniMessage = MiniMessageService.getInstance();

        saveDefaultConfig();

        itemLabelManager = new ItemLabelManager(this);
        itemLabelManager.cleanOrphanedLabels();

        itemLabelListener = new ItemLabelListener(this);

        Bukkit.getPluginManager().registerEvents(itemLabelListener, this);

        ItemLabelCommand command = new ItemLabelCommand(this);
        getCommand("itemlabel").setExecutor(command);
        getCommand("itemlabel").setTabCompleter(command);

        long updateInterval = getConfig().getLong("update-interval", 20L);
        scheduler.runSyncRepeating(itemLabelManager::updateLabels, updateInterval, updateInterval);

        getLogger().info(getPluginName() + " 已启动 - 物品标签系统已激活");
    }

    @Override
    protected void onPluginDisable() {
        if (itemLabelManager != null) {
            itemLabelManager.clearAllLabels();
        }
        scheduler.cancelAllTasks();
        getLogger().info(getPluginName() + " 已关闭 - 所有标签已清理");
    }

    @Override
    protected String getPluginName() {
        return "GuangDianItemLabel";
    }

    public static GuangDianItemLabel getInstance() {
        return instance;
    }

    public ItemLabelManager getItemLabelManager() {
        return itemLabelManager;
    }

    public MiniMessageService getMiniMessage() {
        return miniMessage;
    }
}
