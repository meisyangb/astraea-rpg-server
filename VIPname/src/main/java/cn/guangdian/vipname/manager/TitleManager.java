package cn.guangdian.vipname.manager;

import cn.guangdian.vipname.VIPname;
import cn.guangdian.vipname.model.PlayerTitle;
import cn.guangdian.vipname.model.Title;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;

import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 称号管理器
 */
public class TitleManager {

    private final VIPname plugin;
    
    // 所有称号
    private final Map<String, Title> titles = new ConcurrentHashMap<>();
    
    // 玩家数据
    private final Map<UUID, PlayerTitle> playerData = new ConcurrentHashMap<>();
    
    // 数据文件
    private final File dataFile;
    
    public TitleManager(VIPname plugin) {
        this.plugin = plugin;
        this.dataFile = new File(plugin.getDataFolder(), "data/playerdata.yml");
    }
    
    /**
     * 加载称号配置
     */
    public void load() {
        titles.clear();
        
        FileConfiguration config = plugin.getConfig();
        ConfigurationSection titlesSection = config.getConfigurationSection("titles");
        if (titlesSection == null) return;
        
        for (String id : titlesSection.getKeys(false)) {
            ConfigurationSection titleData = titlesSection.getConfigurationSection(id);
            if (titleData != null) {
                Title title = new Title(
                    id,
                    titleData.getString("name", id),
                    titleData.getString("display", ""),
                    titleData.getString("prefix", ""),
                    titleData.getString("suffix", ""),
                    titleData.getInt("priority", 0),
                    titleData.getString("permission", ""),
                    titleData.getStringList("variables")
                );
                titles.put(id.toLowerCase(), title);
            }
        }
        
        // 加载玩家数据
        loadPlayerData();
        
        plugin.getLogger().info("已加载 " + titles.size() + " 个称号");
    }
    
    /**
     * 加载玩家数据
     */
    private void loadPlayerData() {
        playerData.clear();
        
        if (!dataFile.exists()) return;
        
        YamlConfiguration data = YamlConfiguration.loadConfiguration(dataFile);
        ConfigurationSection playersSection = data.getConfigurationSection("players");
        if (playersSection == null) return;
        
        for (String uuidStr : playersSection.getKeys(false)) {
            try {
                UUID uuid = UUID.fromString(uuidStr);
                ConfigurationSection playerSection = playersSection.getConfigurationSection(uuidStr);
                if (playerSection != null) {
                    PlayerTitle pt = new PlayerTitle(uuid);
                    pt.setCurrentTitle(playerSection.getString("current", null));
                    
                    List<String> owned = playerSection.getStringList("owned");
                    for (String titleId : owned) {
                        pt.addTitle(titleId.toLowerCase());
                    }
                    
                    playerData.put(uuid, pt);
                }
            } catch (Exception ignored) {}
        }
    }
    
    /**
     * 保存玩家数据
     */
    public void save() {
        if (!dataFile.exists()) {
            dataFile.getParentFile().mkdirs();
        }
        
        YamlConfiguration data = new YamlConfiguration();
        
        for (Map.Entry<UUID, PlayerTitle> entry : playerData.entrySet()) {
            String uuidStr = entry.getKey().toString();
            PlayerTitle pt = entry.getValue();
            
            data.set("players." + uuidStr + ".current", pt.getCurrentTitle());
            data.set("players." + uuidStr + ".owned", new ArrayList<>(pt.getOwnedTitles()));
        }
        
        try {
            data.save(dataFile);
        } catch (IOException e) {
            plugin.getLogger().severe("保存玩家数据失败: " + e.getMessage());
        }
    }
    
    /**
     * 获取称号数量
     */
    public int getTitleCount() {
        return titles.size();
    }
    
    /**
     * 获取称号
     */
    public Title getTitle(String id) {
        return titles.get(id.toLowerCase());
    }
    
    /**
     * 获取所有称号
     */
    public Collection<Title> getAllTitles() {
        return titles.values();
    }
    
    /**
     * 获取玩家称号数据
     */
    public PlayerTitle getPlayerData(UUID playerId) {
        return playerData.computeIfAbsent(playerId, PlayerTitle::new);
    }
    
    /**
     * 给玩家授予称号
     */
    public boolean grantTitle(UUID playerId, String titleId) {
        Title title = getTitle(titleId);
        if (title == null) return false;
        
        PlayerTitle pt = getPlayerData(playerId);
        pt.addTitle(titleId.toLowerCase());
        save();
        return true;
    }
    
    /**
     * 移除玩家称号
     */
    public boolean removeTitle(UUID playerId, String titleId) {
        PlayerTitle pt = getPlayerData(playerId);
        if (!pt.hasTitle(titleId)) return false;
        
        pt.removeTitle(titleId.toLowerCase());
        if (titleId.equalsIgnoreCase(pt.getCurrentTitle())) {
            pt.setCurrentTitle(null);
        }
        save();
        return true;
    }
    
    /**
     * 设置玩家当前称号
     */
    public boolean setCurrentTitle(UUID playerId, String titleId) {
        PlayerTitle pt = getPlayerData(playerId);
        
        // 检查玩家是否拥有该称号
        if (titleId != null && !pt.hasTitle(titleId)) {
            return false;
        }
        
        pt.setCurrentTitle(titleId != null ? titleId.toLowerCase() : null);
        save();
        return true;
    }
    
    /**
     * 获取玩家当前称号
     */
    public Title getCurrentTitle(UUID playerId) {
        PlayerTitle pt = getPlayerData(playerId);
        String currentId = pt.getCurrentTitle();
        if (currentId == null) return null;
        return getTitle(currentId);
    }
    
    /**
     * 获取玩家称号显示名
     */
    public String getPlayerDisplayName(Player player) {
        UUID playerId = player.getUniqueId();
        Title title = getCurrentTitle(playerId);
        
        if (title == null) {
            return player.getName();
        }
        
        // 替换变量
        String prefix = plugin.getVariableManager().processVariables(player, title.getPrefix());
        String suffix = plugin.getVariableManager().processVariables(player, title.getSuffix());
        String display = plugin.getVariableManager().processVariables(player, title.getDisplay());
        
        return prefix + display + suffix + player.getName();
    }
}