package cn.guangdian.chunktrim;

import cn.guangdian.chunktrim.command.ChunkTrimCommand;
import cn.guangdian.chunktrim.config.TrimConfig;
import cn.guangdian.chunktrim.manager.TrimManager;
import cn.guangdian.chunktrim.task.TrimTask;
import org.bukkit.plugin.java.JavaPlugin;

/**
 * GuangDianChunkTrim - 区块裁剪插件
 * 
 * 功能：
 * - 设置保留区域（以指定位置为中心）
 * - 删除区域外的所有区块
 * - 支持多世界批量处理
 * - 实时进度显示
 * - 支持暂停/继续/取消
 */
public class GuangDianChunkTrim extends JavaPlugin {

    private static GuangDianChunkTrim instance;
    private TrimConfig config;
    private TrimManager trimManager;

    @Override
    public void onEnable() {
        instance = this;
        
        saveDefaultConfig();
        config = new TrimConfig(this);
        trimManager = new TrimManager(this);
        
        // 注册命令
        if (getCommand("chunktrim") != null) {
            getCommand("chunktrim").setExecutor(new ChunkTrimCommand(this));
        }
        
        getLogger().info("=== GuangDianChunkTrim 已启动 ===");
        getLogger().info("功能：保留指定区域，删除其他区块");
        getLogger().info("用法：/chunktrim help");
    }

    @Override
    public void onDisable() {
        if (trimManager != null) {
            trimManager.cancelAll();
        }
        getLogger().info("GuangDianChunkTrim 已关闭");
    }

    public static GuangDianChunkTrim getInstance() {
        return instance;
    }

    public TrimConfig getTrimConfig() {
        return config;
    }

    public TrimManager getTrimManager() {
        return trimManager;
    }
}