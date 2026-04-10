package cn.guangdian.battlepass.manager;

import cn.guangdian.battlepass.GuangDianBattlePass;
import cn.guangdian.battlepass.model.BattlePassLevel;
import cn.guangdian.battlepass.model.Season;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

public class SeasonManager {
    
    private final GuangDianBattlePass plugin;
    private final Map<Integer, Season> seasons;
    private Season currentSeason;
    private File seasonsFile;
    private YamlConfiguration seasonsConfig;
    
    public SeasonManager(GuangDianBattlePass plugin) {
        this.plugin = plugin;
        this.seasons = new HashMap<>();
        loadSeasons();
    }
    
    private void loadSeasons() {
        seasonsFile = new File(plugin.getDataFolder(), "seasons.yml");
        if (!seasonsFile.exists()) {
            plugin.saveResource("seasons.yml", false);
        }
        seasonsConfig = YamlConfiguration.loadConfiguration(seasonsFile);
        
        ConfigurationSection seasonsSection = seasonsConfig.getConfigurationSection("seasons");
        if (seasonsSection == null) return;
        
        for (String key : seasonsSection.getKeys(false)) {
            ConfigurationSection seasonSection = seasonsSection.getConfigurationSection(key);
            if (seasonSection == null) continue;
            
            try {
                Season season = loadSeason(seasonSection);
                seasons.put(season.getSeasonId(), season);
                
                if (seasonSection.getBoolean("active", false)) {
                    currentSeason = season;
                    season.setActiveNow(true);
                }
            } catch (Exception e) {
                plugin.getLogger().warning("加载赛季 " + key + " 失败: " + e.getMessage());
            }
        }
        
        plugin.getLogger().info("已加载 " + seasons.size() + " 个赛季");
    }
    
    private Season loadSeason(ConfigurationSection section) {
        int seasonId = section.getInt("id");
        String seasonName = section.getString("name", "赛季 " + seasonId);
        int maxLevel = section.getInt("max-level", 50);
        
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
        LocalDateTime startTime = LocalDateTime.parse(section.getString("start-time"), formatter);
        LocalDateTime endTime = LocalDateTime.parse(section.getString("end-time"), formatter);
        
        Season season = new Season(seasonId, seasonName, startTime, endTime, maxLevel);
        
        ConfigurationSection levelsSection = section.getConfigurationSection("levels");
        if (levelsSection != null) {
            for (String levelKey : levelsSection.getKeys(false)) {
                ConfigurationSection levelSection = levelsSection.getConfigurationSection(levelKey);
                if (levelSection == null) continue;
                
                BattlePassLevel level = loadLevel(levelSection);
                season.addLevel(level);
            }
        }
        
        return season;
    }
    
    private BattlePassLevel loadLevel(ConfigurationSection section) {
        int level = section.getInt("level");
        int requiredExp = section.getInt("required-exp", 1000);
        
        BattlePassLevel bpLevel = new BattlePassLevel(level, requiredExp);
        
        return bpLevel;
    }
    
    public Season getCurrentSeason() {
        if (currentSeason != null && !currentSeason.isActive()) {
            currentSeason = null;
        }
        return currentSeason;
    }
    
    public Season getSeason(int seasonId) {
        return seasons.get(seasonId);
    }
    
    public Collection<Season> getAllSeasons() {
        return seasons.values();
    }
    
    public void createSeason(int seasonId, String name, LocalDateTime startTime, LocalDateTime endTime, int maxLevel) {
        Season season = new Season(seasonId, name, startTime, endTime, maxLevel);
        seasons.put(seasonId, season);
        saveSeason(season);
    }
    
    public void activateSeason(int seasonId) {
        if (currentSeason != null) {
            currentSeason.setActiveNow(false);
            updateSeasonConfig(currentSeason);
        }
        
        Season season = seasons.get(seasonId);
        if (season != null) {
            season.setActiveNow(true);
            currentSeason = season;
            updateSeasonConfig(season);
        }
    }
    
    private void saveSeason(Season season) {
        String path = "seasons." + season.getSeasonId();
        seasonsConfig.set(path + ".id", season.getSeasonId());
        seasonsConfig.set(path + ".name", season.getSeasonName());
        seasonsConfig.set(path + ".max-level", season.getMaxLevel());
        
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
        seasonsConfig.set(path + ".start-time", season.getStartTime().format(formatter));
        seasonsConfig.set(path + ".end-time", season.getEndTime().format(formatter));
        seasonsConfig.set(path + ".active", season.isActive());
        
        try {
            seasonsConfig.save(seasonsFile);
        } catch (IOException e) {
            plugin.getLogger().severe("保存赛季配置失败: " + e.getMessage());
        }
    }
    
    private void updateSeasonConfig(Season season) {
        String path = "seasons." + season.getSeasonId();
        seasonsConfig.set(path + ".active", season.isActive());
        
        try {
            seasonsConfig.save(seasonsFile);
        } catch (IOException e) {
            plugin.getLogger().severe("更新赛季配置失败: " + e.getMessage());
        }
    }
    
    public void reload() {
        seasons.clear();
        currentSeason = null;
        loadSeasons();
    }
}
