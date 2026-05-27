package cn.guangdian.armorstats.listener;

import cn.guangdian.armorstats.GuangDianArmorStats;
import cn.guangdian.armorstats.skill.SkillManager;
import cn.guangdian.rpgcore.message.MiniMessageService;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 技能触发监听器
 * 
 * 触发格式：Lore 中包含 "主动技能:" 或 "【主动技能】" 标记
 * 与 GuangDianItemTrigger 使用不同格式，避免冲突
 * 
 * GuangDianItemTrigger: 使用 lore-keyword 匹配任意关键词
 * 本监听器: 只匹配 "主动技能: xxx" 或 "【主动技能】xxx" 格式
 */
public class SkillTriggerListener implements Listener {

    private final GuangDianArmorStats plugin;
    private final SkillManager skillManager;
    private final MiniMessage miniMessage;
    
    private final Map<UUID, Map<String, Long>> cooldowns = new HashMap<>();
    
    // 匹配格式: "主动技能: xxx" 或 "技能: xxx" 或 "技能名称: xxx" 或 "【主动技能】xxx"
    private static final Pattern SKILL_PATTERN = Pattern.compile("(?:主动技能|技能名称|技能)\\s*[:：]\\s*(\\S+)|【主动技能】\\s*(\\S+)");
    private static final Pattern COOLDOWN_PATTERN = Pattern.compile("技能冷却\\s*[:：]?\\s*(\\d+)");
    
    private boolean enabled = true;
    
    public SkillTriggerListener(GuangDianArmorStats plugin, SkillManager skillManager) {
        this.plugin = plugin;
        this.skillManager = skillManager;
        MiniMessageService mm = plugin.getMiniMessage();
        this.miniMessage = mm != null ? mm.getMiniMessage() : MiniMessage.miniMessage();
        
        // 检查是否启用
        this.enabled = plugin.getConfig().getBoolean("skill_trigger.enabled", true);
        
        // 如果 GuangDianItemTrigger 存在，默认禁用此监听器
        if (Bukkit.getPluginManager().isPluginEnabled("GuangDianItemTrigger")) {
            this.enabled = plugin.getConfig().getBoolean("skill_trigger.override_itemtrigger", false);
            if (!this.enabled) {
                plugin.getLogger().info("检测到 GuangDianItemTrigger，技能触发监听器已禁用");
                plugin.getLogger().info("如需启用，请在 config.yml 中设置 skill_trigger.override_itemtrigger: true");
            }
        }
    }
    
    /**
     * 处理玩家对方块/空气右键事件
     */
    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onPlayerInteract(PlayerInteractEvent event) {
        if (!enabled) return;
        if (event.getHand() != EquipmentSlot.HAND) return;
        
        Action action = event.getAction();
        if (action != Action.RIGHT_CLICK_AIR && action != Action.RIGHT_CLICK_BLOCK) return;
        
        if (tryTriggerSkill(event.getPlayer())) {
            event.setCancelled(true);
        }
    }
    
    /**
     * 处理玩家对实体右键事件（生物、NPC等）
     */
    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onPlayerInteractEntity(PlayerInteractEntityEvent event) {
        if (!enabled) return;
        if (event.getHand() != EquipmentSlot.HAND) return;
        
        if (tryTriggerSkill(event.getPlayer())) {
            event.setCancelled(true);
        }
    }
    
    /**
     * 尝试触发技能
     * @return 是否成功触发
     */
    private boolean tryTriggerSkill(Player player) {
        ItemStack item = player.getInventory().getItemInMainHand();
        
        if (item == null || item.getType() == Material.AIR) return false;
        
        ItemMeta meta = item.getItemMeta();
        if (meta == null || !meta.hasLore()) return false;
        
        List<String> lore = meta.getLore();
        if (lore == null) return false;
        
        String skillName = null;
        int cooldown = 15;
        
        for (String line : lore) {
            String stripped = stripColor(line);
            
            Matcher skillMatcher = SKILL_PATTERN.matcher(stripped);
            if (skillMatcher.find()) {
                skillName = skillMatcher.group(1) != null ? skillMatcher.group(1) : skillMatcher.group(2);
            }
            
            Matcher cooldownMatcher = COOLDOWN_PATTERN.matcher(stripped);
            if (cooldownMatcher.find()) {
                cooldown = Integer.parseInt(cooldownMatcher.group(1));
            }
        }
        
        if (skillName == null) return false;
        
        if (isOnCooldown(player.getUniqueId(), skillName, cooldown)) {
            long remaining = getRemainingCooldown(player.getUniqueId(), skillName, cooldown);
            player.sendMessage(miniMessage.deserialize("<red>技能冷却中，剩余 " + remaining + " 秒"));
            return true;
        }
        
        setCooldown(player.getUniqueId(), skillName);
        
        boolean triggered = skillManager.triggerActiveSkill(player, skillName);
        
        if (!triggered) {
            player.sendMessage(miniMessage.deserialize("<red>技能 " + skillName + " 不存在或无法触发!"));
        }
        
        return true;
    }
    
    private String stripColor(String text) {
        if (text == null) return "";
        return text.replaceAll("<[^>]+>", "").replaceAll("§[0-9a-fk-or]", "");
    }
    
    private boolean isOnCooldown(UUID playerId, String skillName, int cooldownSeconds) {
        Map<String, Long> playerCooldowns = cooldowns.get(playerId);
        if (playerCooldowns == null) return false;
        
        Long lastUse = playerCooldowns.get(skillName);
        if (lastUse == null) return false;
        
        return System.currentTimeMillis() - lastUse < cooldownSeconds * 1000L;
    }
    
    private long getRemainingCooldown(UUID playerId, String skillName, int cooldownSeconds) {
        Map<String, Long> playerCooldowns = cooldowns.get(playerId);
        if (playerCooldowns == null) return 0;
        
        Long lastUse = playerCooldowns.get(skillName);
        if (lastUse == null) return 0;
        
        long remaining = cooldownSeconds * 1000L - (System.currentTimeMillis() - lastUse);
        return Math.max(0, remaining / 1000);
    }
    
    private void setCooldown(UUID playerId, String skillName) {
        cooldowns.computeIfAbsent(playerId, k -> new HashMap<>())
            .put(skillName, System.currentTimeMillis());
    }
    
    public void clearCooldowns(UUID playerId) {
        cooldowns.remove(playerId);
    }
    
    public boolean isEnabled() {
        return enabled;
    }
}
