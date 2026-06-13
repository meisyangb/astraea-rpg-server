package cn.guangdian.regen;

import cn.guangdian.regen.command.RegenCommand;
import cn.guangdian.regen.listener.BlockBreakListener;
import cn.guangdian.regen.listener.SelectionListener;
import cn.guangdian.regen.manager.RegionManager;
import cn.guangdian.regen.manager.RegenManager;
import cn.guangdian.regen.manager.SelectionManager;
import org.bukkit.plugin.java.JavaPlugin;

/**
 * GuangDianRegen 主类
 * 矿场/林场自动刷新系统
 */
public class GuangDianRegen extends JavaPlugin {

    private static GuangDianRegen instance;

    private RegionManager regionManager;
    private SelectionManager selectionManager;
    private RegenManager regenManager;

    @Override
    public void onEnable() {
        instance = this;

        // 保存默认配置
        saveDefaultConfig();

        // 初始化管理器
        regionManager = new RegionManager(this);
        selectionManager = new SelectionManager();
        regenManager = new RegenManager(this, regionManager);

        // 注册监听器
        getServer().getPluginManager().registerEvents(new BlockBreakListener(regenManager), this);
        getServer().getPluginManager().registerEvents(new SelectionListener(this, selectionManager), this);

        // 注册命令
        RegenCommand regenCommand = new RegenCommand(this, regionManager, selectionManager, regenManager);
        getCommand("regen").setExecutor(regenCommand);
        getCommand("regen").setTabCompleter(regenCommand);

        getLogger().info("GuangDianRegen 已启动!");
        getLogger().info("已加载 " + regionManager.getRegions().size() + " 个区域");
    }

    @Override
    public void onDisable() {
        // 清理刷新任务
        if (regenManager != null) {
            regenManager.clearAllTasks();
        }

        // 保存区域配置
        if (regionManager != null) {
            regionManager.saveRegions();
        }

        getLogger().info("GuangDianRegen 已关闭!");
    }

    /**
     * 获取实例
     */
    public static GuangDianRegen getInstance() {
        return instance;
    }

    /**
     * 获取区域管理器
     */
    public RegionManager getRegionManager() {
        return regionManager;
    }

    /**
     * 获取选区管理器
     */
    public SelectionManager getSelectionManager() {
        return selectionManager;
    }

    /**
     * 获取刷新管理器
     */
    public RegenManager getRegenManager() {
        return regenManager;
    }
}
