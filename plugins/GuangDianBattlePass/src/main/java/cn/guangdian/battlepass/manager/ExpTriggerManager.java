package cn.guangdian.battlepass.manager;

import cn.guangdian.battlepass.GuangDianBattlePass;
import cn.guangdian.battlepass.hook.MythicMobsHook;
import cn.guangdian.battlepass.model.ExpTrigger;
import cn.guangdian.battlepass.model.PlayerBattlePass;
import cn.guangdian.rpgcore.message.MiniMessageService;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.io.File;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class ExpTriggerManager {

    private final GuangDianBattlePass plugin;
    private final MiniMessageService miniMessage;
    private final Map<String, ExpTrigger> triggers;
    private final Map<UUID, Map<String, Long>> cooldowns;
    private final Map<UUID, Map<String, Integer>> dailyCounts;
    private File triggersFile;
    private YamlConfiguration triggersConfig;

    public ExpTriggerManager(GuangDianBattlePass plugin) {
        this.plugin = plugin;
        this.miniMessage = MiniMessageService.getInstance();
        this.triggers = new ConcurrentHashMap<>();
        this.cooldowns = new ConcurrentHashMap<>();
        this.dailyCounts = new ConcurrentHashMap<>();

        MythicMobsHook.checkMythicMobs();
        if (MythicMobsHook.isMythicMobsEnabled()) {
            plugin.getLogger().info("已检测到 MythicMobs，启用自定义怪物支持");
        }

        loadTriggers();
    }
    
    private void loadTriggers() {
        triggersFile = new File(plugin.getDataFolder(), "exp_triggers.yml");
        if (!triggersFile.exists()) {
            plugin.saveResource("exp_triggers.yml", false);
        }
        triggersConfig = YamlConfiguration.loadConfiguration(triggersFile);
        
        ConfigurationSection triggersSection = triggersConfig.getConfigurationSection("triggers");
        if (triggersSection == null) return;
        
        for (String key : triggersSection.getKeys(false)) {
            ConfigurationSection triggerSection = triggersSection.getConfigurationSection(key);
            if (triggerSection == null) continue;
            
            try {
                ExpTrigger trigger = loadTrigger(triggerSection);
                triggers.put(trigger.getTriggerId(), trigger);
            } catch (Exception e) {
                plugin.getLogger().warning("加载经验触发器 " + key + " 失败: " + e.getMessage());
            }
        }
        
        plugin.getLogger().info("已加载 " + triggers.size() + " 个经验触发器");
    }
    
    private ExpTrigger loadTrigger(ConfigurationSection section) {
        ExpTrigger trigger = new ExpTrigger();
        trigger.setTriggerId(section.getString("id"));
        trigger.setTriggerType(ExpTrigger.TriggerType.valueOf(section.getString("type", "CUSTOM")));
        trigger.setExpAmount(section.getInt("exp", 10));
        trigger.setCooldown(section.getInt("cooldown", 0));
        trigger.setMaxDaily(section.getInt("max-daily", -1));
        trigger.setEnabled(section.getBoolean("enabled", true));
        
        String target = section.getString("target");
        if (target != null) {
            trigger.setTarget(target);
        }
        
        List<String> targets = section.getStringList("targets");
        if (!targets.isEmpty()) {
            trigger.setTargets(targets);
        }
        
        return trigger;
    }
    
    public void processKill(Player killer, Entity victim) {
        String mythicType = MythicMobsHook.getMythicMobType(victim);
        String mobType = victim.getType().name();
        
        for (ExpTrigger trigger : triggers.values()) {
            if (!trigger.isEnabled()) continue;
            
            switch (trigger.getTriggerType()) {
                case KILL_MYTHICMOB:
                    if (mythicType != null && trigger.matchesTarget(mythicType)) {
                        grantExp(killer, trigger);
                    }
                    break;
                case KILL_MOB_TYPE:
                    if (trigger.matchesTarget(mobType)) {
                        grantExp(killer, trigger);
                    }
                    break;
            }
        }
    }
    
    public void processItemObtain(Player player, ItemStack item) {
        String mythicType = MythicMobsHook.getMythicItemType(item);
        Material material = item.getType();
        
        for (ExpTrigger trigger : triggers.values()) {
            if (!trigger.isEnabled()) continue;
            
            switch (trigger.getTriggerType()) {
                case OBTAIN_MYTHIC_ITEM:
                    if (mythicType != null && trigger.matchesTarget(mythicType)) {
                        grantExp(player, trigger);
                    }
                    break;
                case OBTAIN_ITEM:
                    if (trigger.matchesTarget(material.name())) {
                        grantExp(player, trigger);
                    }
                    break;
            }
        }
    }
    
    public void processCustomTrigger(Player player, String triggerType, String target) {
        for (ExpTrigger trigger : triggers.values()) {
            if (!trigger.isEnabled()) continue;
            
            if (trigger.getTriggerType() == ExpTrigger.TriggerType.CUSTOM) {
                if (trigger.matchesTarget(triggerType) || trigger.matchesTarget(target)) {
                    grantExp(player, trigger);
                }
            }
        }
    }
    
    private void grantExp(Player player, ExpTrigger trigger) {
        UUID playerId = player.getUniqueId();
        String triggerId = trigger.getTriggerId();
        
        if (trigger.getCooldown() > 0) {
            Map<String, Long> playerCooldowns = cooldowns.computeIfAbsent(playerId, k -> new ConcurrentHashMap<>());
            Long lastTrigger = playerCooldowns.get(triggerId);
            if (lastTrigger != null) {
                long elapsed = System.currentTimeMillis() - lastTrigger;
                if (elapsed < trigger.getCooldown() * 1000L) {
                    return;
                }
            }
            playerCooldowns.put(triggerId, System.currentTimeMillis());
        }
        
        if (trigger.getMaxDaily() > 0) {
            Map<String, Integer> playerCounts = dailyCounts.computeIfAbsent(playerId, k -> new ConcurrentHashMap<>());
            int count = playerCounts.getOrDefault(triggerId, 0);
            if (count >= trigger.getMaxDaily()) {
                return;
            }
            playerCounts.put(triggerId, count + 1);
        }
        
        plugin.getBattlePassManager().addExp(playerId, trigger.getExpAmount());

        player.sendMessage(miniMessage.green("[战令] ")
            .append(miniMessage.yellow("获得 " + trigger.getExpAmount() + " 经验 "))
            .append(miniMessage.colorize("<gray>(" + trigger.getTriggerId() + ")")));
    }
    
    public void resetDailyCounts() {
        dailyCounts.clear();
        plugin.getLogger().info("已重置每日经验触发次数");
    }
    
    public void reload() {
        triggers.clear();
        loadTriggers();
    }
    
    public Collection<ExpTrigger> getAllTriggers() {
        return triggers.values();
    }
    
    public ExpTrigger getTrigger(String triggerId) {
        return triggers.get(triggerId);
    }
}
