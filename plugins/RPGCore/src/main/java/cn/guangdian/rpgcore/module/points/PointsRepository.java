package cn.guangdian.rpgcore.module.points;

import cn.guangdian.rpgcore.storage.YamlRepository;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.UUID;

/**
 * 点券数据仓库
 * 
 * @author GuangDian
 * @since 1.0.0
 */
public class PointsRepository extends YamlRepository<PlayerPointsData> {

    private final long defaultBalance;

    /**
     * 创建点券数据仓库
     * 
     * @param plugin 插件实例
     * @param defaultBalance 默认余额
     */
    public PointsRepository(JavaPlugin plugin, long defaultBalance) {
        super(plugin, "data");
        this.defaultBalance = defaultBalance;
    }

    @Override
    protected PlayerPointsData createDefault(UUID id) {
        return new PlayerPointsData(id, defaultBalance);
    }

    @Override
    protected PlayerPointsData deserialize(FileConfiguration config) {
        UUID playerId = UUID.fromString(config.getString("playerId"));
        
        PlayerPointsData data = new PlayerPointsData(playerId);
        data.setBalance(config.getLong("balance", defaultBalance));
        
        // 使用公开setter方法设置私有字段（不再使用反射）
        data.setTotalEarned(config.getLong("totalEarned", 0));
        data.setTotalSpent(config.getLong("totalSpent", 0));
        data.setCreatedAt(config.getLong("createdAt", System.currentTimeMillis()));
        
        return data;
    }

    @Override
    protected void serialize(PlayerPointsData data, FileConfiguration config) {
        config.set("playerId", data.getPlayerId().toString());
        config.set("balance", data.getBalance());
        config.set("totalEarned", data.getTotalEarned());
        config.set("totalSpent", data.getTotalSpent());
        config.set("createdAt", data.getCreatedAt());
        config.set("updatedAt", data.getUpdatedAt());
    }
}