package cn.guangdian.fakeonline;

import com.destroystokyo.paper.event.server.PaperServerListPingEvent;
import com.destroystokyo.paper.profile.PlayerProfile;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.ArrayList;
import java.util.List;

/**
 * GuangDianFakeOnline - 伪装玩家在线数插件（简化版）
 *
 * 功能：
 * 1. 修改服务器列表中显示的玩家在线数（包括API查询）
 * 2. 提供PlaceholderAPI占位符供计分板使用
 *
 * 模式：
 * - FIXED: 固定显示指定数量
 * - RANGE: 在范围内随机显示
 * - MULTIPLIER: 真实在线数乘以倍数
 */
public class GuangDianFakeOnline extends JavaPlugin implements Listener {

    private FakeOnlineConfig config;

    @Override
    public void onEnable() {
        saveDefaultConfig();
        config = new FakeOnlineConfig(this);

        Bukkit.getPluginManager().registerEvents(this, this);
        getCommand("gdfakeonline").setExecutor(new FakeOnlineCommand(this));

        getLogger().info("GuangDianFakeOnline 已启动（简化版）");
        getLogger().info("模式: " + config.getMode());
        getLogger().info("伪装在线数: " + config.getFakeOnlineCount(Bukkit.getOnlinePlayers().size()));
        getLogger().info("伪装最大玩家数: " + config.getFakeMaxPlayers());
    }

    @Override
    public void onDisable() {
        getLogger().info("GuangDianFakeOnline 已关闭");
    }

    /**
     * 拦截服务器列表 ping 事件
     * 这个事件会在服务器列表查询时触发，包括API查询（如 motd.minebbs.com）
     */
    @EventHandler(priority = EventPriority.HIGHEST)
    public void onServerListPing(PaperServerListPingEvent event) {
        int realOnline = Bukkit.getOnlinePlayers().size();
        int fakeOnline = config.getFakeOnlineCount(realOnline);
        int fakeMax = config.getFakeMaxPlayers();

        // 修改在线玩家数（这会影响API查询结果）
        event.setNumPlayers(fakeOnline);

        // 修改最大玩家数
        if (fakeMax > 0) {
            event.setMaxPlayers(fakeMax);
        }

        // 修改 MOTD
        if (config.hasCustomMotd()) {
            String motd = config.getMotd(realOnline, fakeOnline);
            event.setMotd(motd);
        }

        // 修改玩家列表示例（鼠标悬停显示的玩家名）
        if (config.hasCustomSamplePlayers()) {
            List<String> samplePlayers = config.getSamplePlayers();
            List<PaperServerListPingEvent.ListedPlayerInfo> listedPlayers = event.getListedPlayers();
            listedPlayers.clear();
            for (String name : samplePlayers) {
                PlayerProfile profile = Bukkit.createProfile(name);
                listedPlayers.add(new PaperServerListPingEvent.ListedPlayerInfo(name, profile.getId()));
            }
        }
    }

    /**
     * 重新加载配置
     */
    public void reloadConfig() {
        super.reloadConfig();
        config = new FakeOnlineConfig(this);

        getLogger().info("配置已重新加载");
        getLogger().info("模式: " + config.getMode());
        getLogger().info("伪装在线数: " + config.getFakeOnlineCount(Bukkit.getOnlinePlayers().size()));
        getLogger().info("伪装最大玩家数: " + config.getFakeMaxPlayers());
    }

    public FakeOnlineConfig getFakeOnlineConfig() {
        return config;
    }
}