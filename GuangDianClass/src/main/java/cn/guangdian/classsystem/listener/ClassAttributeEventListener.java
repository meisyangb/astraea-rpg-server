package cn.guangdian.classsystem.listener;

import cn.guangdian.rpgcore.event.events.AttributeChangeEvent;
import cn.guangdian.classsystem.event.ClassChooseEvent;
import cn.guangdian.classsystem.event.ClassAdvanceEvent;
import cn.guangdian.classsystem.event.TierUpEvent;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;

import java.util.HashMap;
import java.util.Map;

/**
 * 职业事件监听器
 * 监听职业变更，触发属性变更事件
 */
public class ClassAttributeEventListener implements Listener {
    
    /**
     * 监听职业选择事件
     */
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onClassChoose(ClassChooseEvent event) {
        Player player = event.getPlayer();
        
        Map<String, Object> data = new HashMap<>();
        data.put("classId", event.getGameClass().getId());
        
        AttributeChangeEvent attrEvent = new AttributeChangeEvent(
            player,
            AttributeChangeEvent.Type.CLASS_CHANGE,
            "GuangDianClass",
            data
        );
        Bukkit.getPluginManager().callEvent(attrEvent);
    }
    
    /**
     * 监听职业转职事件
     */
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onClassAdvance(ClassAdvanceEvent event) {
        Player player = event.getPlayer();
        
        Map<String, Object> data = new HashMap<>();
        data.put("oldClass", event.getFromClass().getId());
        data.put("newClass", event.getToClass().getId());
        
        AttributeChangeEvent attrEvent = new AttributeChangeEvent(
            player,
            AttributeChangeEvent.Type.CLASS_CHANGE,
            "GuangDianClass",
            data
        );
        Bukkit.getPluginManager().callEvent(attrEvent);
    }
    
    /**
     * 监听阶位提升事件
     */
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onTierUp(TierUpEvent event) {
        Player player = event.getPlayer();
        
        Map<String, Object> data = new HashMap<>();
        data.put("oldTier", event.getFromTier());
        data.put("newTier", event.getToTier());
        
        AttributeChangeEvent attrEvent = new AttributeChangeEvent(
            player,
            AttributeChangeEvent.Type.CLASS_LEVEL_UP,
            "GuangDianClass",
            data
        );
        Bukkit.getPluginManager().callEvent(attrEvent);
    }
}
