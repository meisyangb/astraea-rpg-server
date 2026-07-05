package cn.guangdian.fakeplayer;

import me.clip.placeholderapi.expansion.PlaceholderExpansion;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

/**
 * PlaceholderAPI 扩展
 * 提供伪装数据的占位符
 */
public class PlaceholderAPIHook extends PlaceholderExpansion {

    private final GuangDianFakePlayer plugin;

    public PlaceholderAPIHook(GuangDianFakePlayer plugin) {
        this.plugin = plugin;
    }

    @Override
    public @NotNull String getIdentifier() {
        return plugin.getFakePlayerConfig().getPlaceholderPrefix();
    }

    @Override
    public @NotNull String getAuthor() {
        return "GuangDian Team";
    }

    @Override
    public @NotNull String getVersion() {
        return "1.0.0";
    }

    @Override
    public boolean persist() {
        return true; // 持久化注册
    }

    /**
     * 注销扩展
     */
    public void unregister() {
        try {
            // PlaceholderAPI 的注销方式
            me.clip.placeholderapi.PlaceholderAPI.unregisterExpansion(this);
        } catch (Exception e) {
            // 忽略异常
        }
    }

    @Override
    public String onPlaceholderRequest(Player player, @NotNull String params) {
        if (params == null || params.isEmpty()) {
            return null;
        }

        FakePlayerConfig config = plugin.getFakePlayerConfig();
        int realOnline = Bukkit.getOnlinePlayers().size();
        int fakeOnline = config.getFakeOnlineCount(realOnline);
        int virtualCount = fakeOnline - realOnline; // 虚拟人数 = 伪装在线数 - 真实在线数

        switch (params.toLowerCase()) {
            // 服务器信息占位符
            case "online":
            case "fake_online":
                return String.valueOf(fakeOnline); // 伪装在线数 = 真实在线数 + 虚拟人数

            case "real_online":
                return String.valueOf(realOnline); // 真实在线数

            case "virtual":
            case "virtual_count":
                return String.valueOf(virtualCount); // 虚拟人数

            case "max":
            case "fake_max":
                return String.valueOf(config.getFakeMaxPlayers());

            case "total":
                return String.valueOf(fakeOnline); // 总在线数 = 伪装在线数

            case "mode":
                return config.getMode();

            case "fixed_online":
                return String.valueOf(config.getFakeOnlineCount(0) - 0); // 固定虚拟人数

            case "min_online":
                return String.valueOf(config.getFakeOnlineCount(0) - 0); // 最小虚拟人数

            case "max_online":
                return String.valueOf(config.getFakeOnlineCount(0) - 0); // 最大虚拟人数

            default:
                return null;
        }
    }
}