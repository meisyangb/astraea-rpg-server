package cn.guangdian.sect;

import cn.guangdian.rpgcore.message.UnifiedMessageService;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.entity.EntityRegainHealthEvent;
import org.bukkit.event.inventory.BrewEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.BrewerInventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 门派变强监听器
 * 
 * 监听各类事件，根据门派类型给予贡献值：
 * - 青云宗: 强化武器变强
 * - 合欢宗: 结婚次数越多越强
 * - 天音寺: 治疗救人变强
 * - 焚香谷: 造成伤害变强
 * - 鬼王宗: 击杀敌人变强
 * - 长生堂: 炼丹变强
 */
public class SectPowerListener implements Listener {
    private final GuangDianSect plugin;
    private final UnifiedMessageService msg;
    
    // 贡献值累计通知缓存 (避免刷屏)
    private final Map<UUID, Integer> pendingNotify = new ConcurrentHashMap<>();
    private final Map<UUID, Long> lastNotifyTime = new ConcurrentHashMap<>();
    
    // 上次伤害记录 (用于计算增量治疗)
    private final Map<UUID, Double> lastHealthMap = new ConcurrentHashMap<>();
    
    public SectPowerListener(GuangDianSect plugin) {
        this.plugin = plugin;
        this.msg = UnifiedMessageService.getInstance();
    }
    
    /**
     * 给玩家增加贡献值
     */
    public void addContribution(Player player, int amount) {
        if (amount <= 0) return;
        if (!plugin.isInSect(player)) return;
        
        SectPlayer data = plugin.getPlayerData(player);
        int oldContribution = data.getContribution();
        int maxContribution = plugin.getConfig().getInt("settings.max_contribution", 100000);
        
        int newContribution = Math.min(oldContribution + amount, maxContribution);
        data.setContribution(newContribution);
        
        // 通知玩家
        notifyContribution(player, amount, newContribution);
        
        // 检查晋升
        plugin.checkAndPromote(player);
        
        // 保存数据
        plugin.getDataManager().save(data);
    }
    
    /**
     * 通知玩家贡献值变化
     */
    private void notifyContribution(Player player, int amount, int total) {
        UUID uuid = player.getUniqueId();
        
        int notifyInterval = plugin.getConfig().getInt("settings.contribution_notify_interval", 50);
        pendingNotify.merge(uuid, amount, Integer::sum);
        
        // 达到通知阈值或距离上次通知超过5秒
        long now = System.currentTimeMillis();
        long lastNotify = lastNotifyTime.getOrDefault(uuid, 0L);
        
        if (pendingNotify.get(uuid) >= notifyInterval || now - lastNotify > 5000) {
            Sect sect = plugin.getPlayerSect(player);
            if (sect != null) {
                String message = plugin.getConfig().getString("messages.contribution-notify", 
                    "<yellow>[{sect}] 贡献值 +{amount} (累计: {total})");
                message = message.replace("{sect}", sect.getName())
                    .replace("{amount}", String.valueOf(pendingNotify.get(uuid)))
                    .replace("{total}", String.valueOf(total));
                player.sendMessage(msg.colorize(message));
            }
            pendingNotify.put(uuid, 0);
            lastNotifyTime.put(uuid, now);
        }
    }
    
    // ==================== 鬼王宗: 击杀变强 ====================
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onEntityDeath(EntityDeathEvent e) {
        Player killer = e.getEntity().getKiller();
        if (killer == null) return;
        if (!plugin.isInSect(killer)) return;
        
        Sect sect = plugin.getPlayerSect(killer);
        if (sect == null) return;
        
        String powerType = sect.getPowerMode().getType();
        if (!powerType.equals("kill")) return;
        
        Entity dead = e.getEntity();
        int contribution = 0;
        
        if (dead instanceof Player) {
            // 击杀玩家
            contribution = sect.getPowerMode().getInt("contribution_per_player_kill", 50);
            int pvpBonus = sect.getPowerMode().getInt("pvp_bonus", 2);
            contribution *= pvpBonus;
        } else {
            // 判断是否为BOSS (通过MythicMobs或其他方式)
            String mythicName = getMythicMobName(dead);
            if (mythicName != null && isBoss(dead)) {
                contribution = sect.getPowerMode().getInt("contribution_per_boss_kill", 100);
            } else {
                contribution = sect.getPowerMode().getInt("contribution_per_mob_kill", 5);
            }
        }
        
        addContribution(killer, contribution);
    }
    
    private boolean isBoss(Entity entity) {
        // 检查是否为MythicMobs BOSS
        if (entity instanceof LivingEntity) {
            LivingEntity living = (LivingEntity) entity;
            // 通过自定义名称或血量判断
            if (living.getCustomName() != null) {
                String name = living.getCustomName();
                if (name.contains("BOSS") || name.contains("boss") || name.contains("Boss")) {
                    return true;
                }
            }
            // 血量超过100认为是精英/BOSS
            if (living.getMaxHealth() > 100) {
                return true;
            }
        }
        return false;
    }
    
    private String getMythicMobName(Entity entity) {
        // 简化版，实际需要MythicMobs API
        return null;
    }
    
    // ==================== 焚香谷: 伤害变强 ====================
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onEntityDamage(EntityDamageByEntityEvent e) {
        if (!(e.getDamager() instanceof Player)) return;
        Player player = (Player) e.getDamager();
        
        if (!plugin.isInSect(player)) return;
        
        Sect sect = plugin.getPlayerSect(player);
        if (sect == null) return;
        
        String powerType = sect.getPowerMode().getType();
        if (!powerType.equals("damage")) return;
        
        double damage = e.getDamage();
        int divisor = sect.getPowerMode().getInt("damage_divisor", 10);
        int baseContribution = (int) (damage / divisor);
        
        // 根据目标类型加成
        if (e.getEntity() instanceof Player) {
            // PVP加成
            baseContribution *= 2;
        } else if (isBoss(e.getEntity())) {
            baseContribution *= sect.getPowerMode().getInt("boss_bonus", 5);
        } else {
            baseContribution *= sect.getPowerMode().getInt("mob_bonus", 1);
        }
        
        addContribution(player, Math.max(1, baseContribution));
    }
    
    // ==================== 天音寺: 治疗变强 ====================
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onEntityHeal(EntityRegainHealthEvent e) {
        if (!(e.getEntity() instanceof Player)) return;
        Player healed = (Player) e.getEntity();
        
        // 查找治疗者 (通过伤害事件反向追踪，这里简化处理)
        // 实际需要更复杂的治疗来源追踪
        
        // 简化版：自我治疗也计算
        // 实际应该追踪治疗来源
        
        EntityRegainHealthEvent.RegainReason reason = e.getRegainReason();
        if (reason == EntityRegainHealthEvent.RegainReason.MAGIC || 
            reason == EntityRegainHealthEvent.RegainReason.MAGIC_REGEN ||
            reason == EntityRegainHealthEvent.RegainReason.CUSTOM) {
            
            // 需要通过其他方式获取治疗者
            // 这里暂时跳过，需要在实际使用时配合技能插件
        }
    }
    
    /**
     * 提供给外部调用的治疗贡献计算方法
     */
    public void onPlayerHeal(Player healer, Player target, double healAmount) {
        if (!plugin.isInSect(healer)) return;
        
        Sect sect = plugin.getPlayerSect(healer);
        if (sect == null) return;
        
        String powerType = sect.getPowerMode().getType();
        if (!powerType.equals("heal")) return;
        
        int minHeal = sect.getPowerMode().getInt("min_heal_amount", 10);
        if (healAmount < minHeal) return;
        
        int contributionPerHeal = sect.getPowerMode().getInt("contribution_per_heal", 1);
        int cap = sect.getPowerMode().getInt("contribution_cap_per_action", 50);
        
        int contribution = Math.min((int) (healAmount * contributionPerHeal), cap);
        addContribution(healer, contribution);
    }
    
    // ==================== 长生堂: 炼丹变强 ====================
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onBrew(BrewEvent e) {
        // 获取酿造者
        // Bukkit的BrewEvent不直接提供玩家信息，需要通过其他方式追踪
        
        // 简化版：检查最近操作酿造台的玩家
        BrewerInventory inventory = e.getContents();
        // 需要在实际使用时配合玩家操作追踪
        
        // 暂时通过占位符方法处理
    }
    
    /**
     * 提供给外部调用的炼丹贡献计算方法
     */
    public void onPotionBrew(Player brewer, ItemStack potion) {
        if (!plugin.isInSect(brewer)) return;
        
        Sect sect = plugin.getPlayerSect(brewer);
        if (sect == null) return;
        
        String powerType = sect.getPowerMode().getType();
        if (!powerType.equals("alchemy")) return;
        
        int contribution = sect.getPowerMode().getInt("contribution_per_potion_brew", 10);
        
        // 检查是否为自定义丹药 (通过PDC或其他标识)
        if (isCustomPotion(potion)) {
            contribution = sect.getPowerMode().getInt("contribution_per_custom_item", 30);
            int tierMultiplier = sect.getPowerMode().getInt("tier_bonus_multiplier", 2);
            // 根据丹药等级加成
            contribution *= tierMultiplier;
        }
        
        addContribution(brewer, contribution);
    }
    
    private boolean isCustomPotion(ItemStack item) {
        if (item == null || !item.hasItemMeta()) return false;
        ItemMeta meta = item.getItemMeta();
        PersistentDataContainer pdc = meta.getPersistentDataContainer();
        // 检查自定义丹药标识
        return pdc.has(new NamespacedKey(plugin, "custom_potion"), PersistentDataType.INTEGER);
    }
    
    // ==================== 青云宗: 强化武器变强 ====================
    /**
     * 提供给外部调用的强化贡献计算方法
     * 由GuangDianEnhance插件调用
     */
    public void onWeaponEnhance(Player player, ItemStack weapon, int newLevel) {
        if (!plugin.isInSect(player)) return;
        
        Sect sect = plugin.getPlayerSect(player);
        if (sect == null) return;
        
        String powerType = sect.getPowerMode().getType();
        if (!powerType.equals("enhance_weapon")) return;
        
        int baseContribution = sect.getPowerMode().getInt("contribution_per_enhance", 10);
        int bonusPerLevel = sect.getPowerMode().getInt("bonus_per_level", 5);
        
        int contribution = baseContribution + newLevel * bonusPerLevel;
        addContribution(player, contribution);
    }
    
    // ==================== 合欢宗: 结婚变强 ====================
    /**
     * 提供给外部调用的结婚贡献计算方法
     * 由结婚插件调用
     */
    public void onMarriage(Player player, Player spouse) {
        if (!plugin.isInSect(player)) return;
        
        Sect sect = plugin.getPlayerSect(player);
        if (sect == null) return;
        
        String powerType = sect.getPowerMode().getType();
        if (!powerType.equals("marriage")) return;
        
        int baseContribution = sect.getPowerMode().getInt("contribution_per_marriage", 100);
        int spouseBonus = sect.getPowerMode().getInt("bonus_per_spouse_level", 20);
        
        // 可以根据配偶等级/贡献值等加成
        addContribution(player, baseContribution);
        
        // 双方都加贡献
        if (plugin.isInSect(spouse) && 
            plugin.getPlayerSect(spouse).getId().equals(sect.getId())) {
            addContribution(spouse, baseContribution);
        }
    }
}