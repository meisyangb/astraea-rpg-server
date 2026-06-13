package cn.guangdian.trigger.adapter;

import cn.guangdian.trigger.GuangDianItemTrigger;
import cn.guangdian.rpgcore.RPGCore;
import cn.guangdian.rpgcore.api.ServiceRegistry;
import cn.guangdian.rpgcore.message.MiniMessageService;
import cn.guangdian.rpgcore.service.api.ItemTriggerService;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * 物品触发器服务适配器
 * 
 * <p>连接 GuangDianItemTrigger 实现与 RPGCore 服务接口。</p>
 * 
 * @author GuangDian
 * @since 1.0.0
 */
public class ItemTriggerServiceAdapter implements ItemTriggerService {

    private final GuangDianItemTrigger plugin;
    private final boolean useRPGCore;
    private final MiniMessage miniMessage;

    public ItemTriggerServiceAdapter(GuangDianItemTrigger plugin) {
        this.plugin = plugin;
        this.useRPGCore = Bukkit.getPluginManager().isPluginEnabled("RPGCore");

        // 获取 MiniMessage 实例
        MiniMessageService mmService = plugin.getMiniMessage();
        this.miniMessage = mmService != null ? mmService.getMiniMessage() : MiniMessage.miniMessage();

        if (useRPGCore) {
            try {
                ServiceRegistry registry = RPGCore.getInstance().getServiceRegistry();
                registry.registerService(ItemTriggerService.class, this);
                plugin.getLogger().info("已注册到 RPGCore: ItemTriggerService");
            } catch (Exception e) {
                plugin.getLogger().warning("注册到 RPGCore 失败: " + e.getMessage());
            }
        }
    }

    @Override
    public boolean trigger(Player player, String triggerName) {
        // 通过反射调用内部触发器
        try {
            var triggersField = plugin.getClass().getDeclaredField("triggers");
            triggersField.setAccessible(true);
            @SuppressWarnings("unchecked")
            var triggers = (Map<String, ?>) triggersField.get(plugin);
            
            var triggerConfig = triggers.get(triggerName);
            if (triggerConfig == null) return false;
            
            // 获取执行方法
            var executeMethod = plugin.getClass().getDeclaredMethod("executeTrigger", 
                Player.class, triggerConfig.getClass(), ItemStack.class);
            executeMethod.setAccessible(true);
            
            // 使用主手物品执行
            ItemStack item = player.getInventory().getItemInMainHand();
            executeMethod.invoke(plugin, player, triggerConfig, item);
            return true;
        } catch (Exception e) {
            plugin.getLogger().warning("触发器执行失败: " + triggerName + " - " + e.getMessage());
            return false;
        }
    }

    @Override
    public boolean hasTrigger(String triggerName) {
        try {
            var triggersField = plugin.getClass().getDeclaredField("triggers");
            triggersField.setAccessible(true);
            @SuppressWarnings("unchecked")
            var triggers = (Map<String, ?>) triggersField.get(plugin);
            return triggers.containsKey(triggerName);
        } catch (Exception e) {
            return false;
        }
    }

    @Override
    public long getCooldown(UUID playerId, String triggerName) {
        try {
            var cooldownsField = plugin.getClass().getDeclaredField("cooldowns");
            cooldownsField.setAccessible(true);
            @SuppressWarnings("unchecked")
            var cooldowns = (Map<UUID, Map<String, Long>>) cooldownsField.get(plugin);
            
            Map<String, Long> playerCooldowns = cooldowns.get(playerId);
            if (playerCooldowns == null) return 0;
            
            Long lastUse = playerCooldowns.get(triggerName);
            if (lastUse == null) return 0;
            
            // 获取触发器冷却配置
            var triggersField = plugin.getClass().getDeclaredField("triggers");
            triggersField.setAccessible(true);
            @SuppressWarnings("unchecked")
            var triggers = (Map<String, ?>) triggersField.get(plugin);
            var triggerConfig = triggers.get(triggerName);
            if (triggerConfig == null) return 0;
            
            var cooldownMethod = triggerConfig.getClass().getDeclaredMethod("getCooldown");
            int cooldownSeconds = (int) cooldownMethod.invoke(triggerConfig);
            
            long remaining = (lastUse + cooldownSeconds * 1000L) - System.currentTimeMillis();
            return Math.max(0, remaining);
        } catch (Exception e) {
            return 0;
        }
    }

    @Override
    public boolean isAvailable(UUID playerId, String triggerName) {
        return getCooldown(playerId, triggerName) == 0;
    }

    @Override
    public void resetCooldown(UUID playerId, String triggerName) {
        try {
            var cooldownsField = plugin.getClass().getDeclaredField("cooldowns");
            cooldownsField.setAccessible(true);
            @SuppressWarnings("unchecked")
            var cooldowns = (Map<UUID, Map<String, Long>>) cooldownsField.get(plugin);
            
            Map<String, Long> playerCooldowns = cooldowns.get(playerId);
            if (playerCooldowns != null) {
                playerCooldowns.remove(triggerName);
            }
        } catch (Exception e) {
            // 反射访问失败，触发器可能未注册
            plugin.getLogger().fine("清除触发器冷却失败: " + e.getMessage());
        }
    }

    @Override
    public List<String> getTriggerNames() {
        try {
            var triggersField = plugin.getClass().getDeclaredField("triggers");
            triggersField.setAccessible(true);
            @SuppressWarnings("unchecked")
            var triggers = (Map<String, ?>) triggersField.get(plugin);
            return new ArrayList<>(triggers.keySet());
        } catch (Exception e) {
            return new ArrayList<>();
        }
    }

    @Override
    public int getTriggerCount() {
        try {
            var triggersField = plugin.getClass().getDeclaredField("triggers");
            triggersField.setAccessible(true);
            @SuppressWarnings("unchecked")
            var triggers = (Map<String, ?>) triggersField.get(plugin);
            return triggers.size();
        } catch (Exception e) {
            return 0;
        }
    }

    @Override
    public boolean hasTriggerKeyword(ItemStack item) {
        if (item == null || !item.hasItemMeta()) return false;
        
        ItemMeta meta = item.getItemMeta();
        if (meta == null) return false;
        
        List<String> lore = meta.hasLore() ? meta.getLore() : new ArrayList<>();
        String displayName = meta.hasDisplayName() ? stripColor(meta.getDisplayName()) : "";

        try {
            var triggersField = plugin.getClass().getDeclaredField("triggers");
            triggersField.setAccessible(true);
            @SuppressWarnings("unchecked")
            var triggers = (Map<String, ?>) triggersField.get(plugin);
            
            for (var trigger : triggers.values()) {
                var keywordMethod = trigger.getClass().getDeclaredMethod("getLoreKeyword");
                String keyword = (String) keywordMethod.invoke(trigger);
                
                if (keyword != null && !keyword.isEmpty()) {
                    // 检查Lore
                    for (String line : lore) {
                        String stripped = stripColor(line);
                        if (stripped != null && stripped.toLowerCase().contains(keyword.toLowerCase())) {
                            return true;
                        }
                    }
                    // 检查名称
                    if (displayName.toLowerCase().contains(keyword.toLowerCase())) {
                        return true;
                    }
                }
            }
        } catch (Exception e) {
            // 反射访问失败
            plugin.getLogger().fine("检查物品触发关键词失败: " + e.getMessage());
        }

        return false;
    }

    @Override
    public boolean isAvailable() {
        return true;
    }

    /**
     * 注销服务
     */
    public void unregister() {
        if (useRPGCore) {
            try {
                ServiceRegistry registry = RPGCore.getInstance().getServiceRegistry();
                registry.unregisterService(ItemTriggerService.class);
                plugin.getLogger().info("已从 RPGCore 注销: ItemTriggerService");
            } catch (Exception e) {
                plugin.getLogger().warning("从 RPGCore 注销失败: " + e.getMessage());
            }
        }
    }

    public boolean isUsingRPGCore() {
        return useRPGCore;
    }

    /**
     * 移除文本中的颜色代码
     * 支持传统 & 颜色代码和 MiniMessage 格式
     */
    private String stripColor(String text) {
        if (text == null) return "";
        // 先尝试解析为 Component，然后获取纯文本
        try {
            net.kyori.adventure.text.Component component = miniMessage.deserialize(text);
            return net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer.plainText().serialize(component);
        } catch (Exception e) {
            // 如果解析失败，使用传统方式移除 & 颜色代码
            return text.replaceAll("&[0-9a-fk-or]", "");
        }
    }
}