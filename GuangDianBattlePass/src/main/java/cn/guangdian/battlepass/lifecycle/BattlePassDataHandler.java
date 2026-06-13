package cn.guangdian.battlepass.lifecycle;

import cn.guangdian.battlepass.GuangDianBattlePass;
import cn.guangdian.battlepass.model.PlayerBattlePass;
import cn.guangdian.rpgcore.lifecycle.AbstractPlayerDataHandler;
import cn.guangdian.rpgcore.lifecycle.PlayerDataLoadEvent;
import cn.guangdian.rpgcore.lifecycle.PlayerDataSaveEvent;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;

import java.io.File;
import java.io.IOException;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

public class BattlePassDataHandler extends AbstractPlayerDataHandler {
    
    private final GuangDianBattlePass plugin;
    private final File dataFolder;
    
    public BattlePassDataHandler(GuangDianBattlePass plugin) {
        super(plugin);
        this.plugin = plugin;
        this.dataFolder = new File(plugin.getDataFolder(), "playerdata");
        if (!dataFolder.exists()) {
            dataFolder.mkdirs();
        }
    }
    
    @Override
    protected void onPlayerLoad(Player player) {
        File playerFile = new File(dataFolder, player.getUniqueId().toString() + ".yml");
        if (!playerFile.exists()) {
            return;
        }
        
        plugin.getScheduler().runAsync(() -> {
            YamlConfiguration config = YamlConfiguration.loadConfiguration(playerFile);
            PlayerBattlePass bp = loadPlayerData(player.getUniqueId(), config);
            
            plugin.getScheduler().runSyncLater(() -> {
                plugin.getBattlePassManager().loadPlayerData(player.getUniqueId(), bp);
            }, 0L);
        });
    }
    
    @Override
    protected void onPlayerSave(Player player) {
        PlayerBattlePass bp = plugin.getBattlePassManager().getPlayerBattlePass(player.getUniqueId());
        if (bp == null) return;
        
        plugin.getScheduler().runAsync(() -> {
            File playerFile = new File(dataFolder, player.getUniqueId().toString() + ".yml");
            YamlConfiguration config = new YamlConfiguration();
            savePlayerData(bp, config);
            
            try {
                config.save(playerFile);
            } catch (IOException e) {
                plugin.getLogger().severe("保存玩家数据失败: " + player.getName() + " - " + e.getMessage());
            }
        });
        
        plugin.getBattlePassManager().unloadPlayerData(player.getUniqueId());
    }
    
    @Override
    public int getPriority() {
        return 100;
    }
    
    @Override
    public String getHandlerName() {
        return "BattlePass";
    }
    
    private PlayerBattlePass loadPlayerData(UUID playerId, FileConfiguration config) {
        PlayerBattlePass bp = new PlayerBattlePass();
        bp.setPlayerId(playerId);
        bp.setSeasonId(config.getInt("season-id", 1));
        bp.setLevel(config.getInt("level", 1));
        bp.setCurrentExp(config.getInt("current-exp", 0));
        bp.setTotalExp(config.getInt("total-exp", 0));
        bp.setPremium(config.getBoolean("premium", false));
        bp.setPremiumPurchaseTime(config.getLong("premium-time", 0));
        
        Set<Integer> claimedFree = new HashSet<>();
        for (String level : config.getStringList("claimed-free")) {
            try {
                claimedFree.add(Integer.parseInt(level));
            } catch (NumberFormatException ignored) {
            }
        }
        bp.setClaimedFreeRewards(claimedFree);
        
        Set<Integer> claimedPremium = new HashSet<>();
        for (String level : config.getStringList("claimed-premium")) {
            try {
                claimedPremium.add(Integer.parseInt(level));
            } catch (NumberFormatException ignored) {
            }
        }
        bp.setClaimedPremiumRewards(claimedPremium);
        
        return bp;
    }
    
    private void savePlayerData(PlayerBattlePass bp, FileConfiguration config) {
        config.set("season-id", bp.getSeasonId());
        config.set("level", bp.getLevel());
        config.set("current-exp", bp.getCurrentExp());
        config.set("total-exp", bp.getTotalExp());
        config.set("premium", bp.isPremium());
        config.set("premium-time", bp.getPremiumPurchaseTime());
        
        config.set("claimed-free", new java.util.ArrayList<>(bp.getClaimedFreeRewards()));
        config.set("claimed-premium", new java.util.ArrayList<>(bp.getClaimedPremiumRewards()));
    }
}
