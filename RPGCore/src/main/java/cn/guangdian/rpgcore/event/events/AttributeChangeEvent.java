package cn.guangdian.rpgcore.event.events;

import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

import java.util.Collections;
import java.util.Map;

/**
 * 属性变更事件
 * 当玩家属性发生变更时触发，通知其他插件重新计算属性
 */
public class AttributeChangeEvent extends Event {

    private static final HandlerList HANDLERS = new HandlerList();
    
    /**
     * 事件类型
     */
    public enum Type {
        EQUIPMENT_CHANGE,    // 装备变更（穿戴/卸下）
        WEAPON_CHANGE,       // 武器变更（切换主手/副手）
        CLASS_CHANGE,        // 职业变更（选择/转职）
        CLASS_LEVEL_UP,      // 职业升级（等级/阶位提升）
        BUFF_ADD,            // 添加 Buff
        BUFF_REMOVE,         // 移除 Buff
        FULL_REFRESH         // 完整刷新
    }
    
    private final Player player;
    private final Type type;
    private final String source;  // 事件来源插件
    private final Map<String, Object> data;  // 额外数据
    
    public AttributeChangeEvent(Player player, Type type, String source) {
        this(player, type, source, Collections.emptyMap());
    }
    
    public AttributeChangeEvent(Player player, Type type, String source, Map<String, Object> data) {
        this.player = player;
        this.type = type;
        this.source = source;
        this.data = data != null ? data : Collections.emptyMap();
    }
    
    public Player getPlayer() {
        return player;
    }
    
    public Type getType() {
        return type;
    }
    
    public String getSource() {
        return source;
    }
    
    public Map<String, Object> getData() {
        return data;
    }
    
    public Object getData(String key) {
        return data.get(key);
    }
    
    @Override
    public HandlerList getHandlers() {
        return HANDLERS;
    }
    
    public static HandlerList getHandlerList() {
        return HANDLERS;
    }
}
