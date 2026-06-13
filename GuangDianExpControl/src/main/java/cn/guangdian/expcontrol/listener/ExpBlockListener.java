package cn.guangdian.expcontrol.listener;

import cn.guangdian.expcontrol.GuangDianExpControl;
import cn.guangdian.expcontrol.api.ExpControlService;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockExpEvent;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.entity.ExpBottleEvent;
import org.bukkit.event.player.PlayerExpChangeEvent;
import org.bukkit.event.player.PlayerFishEvent;

import java.util.HashSet;
import java.util.Set;

/**
 * 经验拦截监听器
 * 
 * <p>拦截所有经验获取事件，阻止玩家主动获取经验</p>
 */
public class ExpBlockListener implements Listener {
    
    private final GuangDianExpControl plugin;
    private final ExpControlService expService;
    
    private Set<String> blockedSources;
    
    public ExpBlockListener(GuangDianExpControl plugin, ExpControlService expService) {
        this.plugin = plugin;
        this.expService = expService;
        loadBlockedSources();
    }
    
    private void loadBlockedSources() {
        blockedSources = new HashSet<>();
        ConfigurationSection section = plugin.getConfig().getConfigurationSection("blocked-sources");
        if (section != null) {
            for (String key : section.getKeys(false)) {
                if (section.getBoolean(key, true)) {
                    blockedSources.add(key.toLowerCase());
                }
            }
        }
    }
    
    /**
     * 检查玩家是否有绕过权限
     */
    private boolean hasBypass(Player player) {
        return player.hasPermission("guangdian.expcontrol.bypass");
    }
    
    /**
     * 检查是否应该拦截
     */
    private boolean shouldBlock(Player player, String source) {
        if (!plugin.isBlockAllExp()) {
            return false;
        }
        if (hasBypass(player)) {
            return false;
        }
        return blockedSources.contains(source.toLowerCase());
    }
    
    /**
     * 拦截玩家经验变化事件
     * 这是最核心的拦截点，所有经验获取最终都会经过这里
     */
    @EventHandler(priority = EventPriority.HIGHEST)
    public void onPlayerExpChange(PlayerExpChangeEvent event) {
        Player player = event.getPlayer();
        int amount = event.getAmount();
        
        if (amount <= 0) {
            return;
        }
        
        if (hasBypass(player)) {
            return;
        }
        
        if (plugin.isBlockAllExp()) {
            // 取消经验获取
            event.setAmount(0);
            
            if (plugin.getConfig().getBoolean("debug", false)) {
                plugin.getLogger().info(String.format("[经验拦截] 玩家: %s, 拦截数量: %d", 
                    player.getName(), amount));
            }
        }
    }
    
    /**
     * 拦截方块破坏经验
     */
    @EventHandler(priority = EventPriority.HIGHEST)
    public void onBlockBreak(BlockBreakEvent event) {
        if (event.isCancelled()) {
            return;
        }
        
        Player player = event.getPlayer();
        if (shouldBlock(player, "mining")) {
            // 方块经验会在BlockExpEvent中处理
        }
    }
    
    /**
     * 拦截方块经验掉落
     */
    @EventHandler(priority = EventPriority.HIGHEST)
    public void onBlockExp(BlockExpEvent event) {
        if (event.getExpToDrop() <= 0) {
            return;
        }
        
        // 方块经验没有直接的玩家引用，需要通过其他方式判断
        // 这里直接取消经验掉落
        if (plugin.isBlockAllExp() && blockedSources.contains("mining")) {
            event.setExpToDrop(0);
            
            if (plugin.getConfig().getBoolean("debug", false)) {
                plugin.getLogger().info("[经验拦截] 方块经验已拦截");
            }
        }
    }
    
    /**
     * 拦截怪物击杀经验
     */
    @EventHandler(priority = EventPriority.HIGHEST)
    public void onEntityDeath(EntityDeathEvent event) {
        if (event.getDroppedExp() <= 0) {
            return;
        }
        
        Player killer = event.getEntity().getKiller();
        if (killer == null) {
            return;
        }
        
        EntityType entityType = event.getEntityType();
        String source = entityType == EntityType.PLAYER ? "player-kill" : "mob-kill";
        
        if (shouldBlock(killer, source)) {
            event.setDroppedExp(0);
            
            if (plugin.getConfig().getBoolean("debug", false)) {
                plugin.getLogger().info(String.format("[经验拦截] 击杀经验已拦截, 实体: %s, 玩家: %s", 
                    entityType.name(), killer.getName()));
            }
        }
    }
    
    /**
     * 拦截钓鱼经验
     */
    @EventHandler(priority = EventPriority.HIGHEST)
    public void onPlayerFish(PlayerFishEvent event) {
        if (event.getState() != PlayerFishEvent.State.CAUGHT_FISH) {
            return;
        }
        
        Player player = event.getPlayer();
        if (shouldBlock(player, "fishing")) {
            // 钓鱼经验通过经验球掉落，这里无法直接取消
            // 但经验球被拾取时会被PlayerExpChangeEvent拦截
            if (plugin.getConfig().getBoolean("debug", false)) {
                plugin.getLogger().info(String.format("[经验拦截] 钓鱼经验将被拦截, 玩家: %s", player.getName()));
            }
        }
    }
    
    /**
     * 拦截附魔瓶经验
     */
    @EventHandler(priority = EventPriority.HIGHEST)
    public void onExpBottle(ExpBottleEvent event) {
        if (!plugin.isBlockAllExp()) {
            return;
        }
        
        if (blockedSources.contains("exp-bottle")) {
            event.setExperience(0);
            
            if (plugin.getConfig().getBoolean("debug", false)) {
                plugin.getLogger().info("[经验拦截] 附魔瓶经验已拦截");
            }
        }
    }
    
    /**
     * 重新加载配置
     */
    public void reload() {
        loadBlockedSources();
    }
}
