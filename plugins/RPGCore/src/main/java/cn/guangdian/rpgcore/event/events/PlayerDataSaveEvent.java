package cn.guangdian.rpgcore.event.events;

import cn.guangdian.rpgcore.event.CoreEvent;

import java.util.UUID;

/**
 * 玩家数据保存事件 - RPGCore 基础设施事件
 * 
 * <p>当玩家数据保存完成时触发。这是 RPGCore 的核心基础设施事件，
 * 用于通知其他模块数据保存状态。</p>
 * 
 * <p><strong>注意：</strong>这是 RPGCore 基础设施事件，不应迁移到其他插件。</p>
 * 
 * @author GuangDian
 * @since 1.0.0
 */
public class PlayerDataSaveEvent extends CoreEvent {

    private final UUID playerId;
    private final String module;
    private final Object data;

    /**
     * 创建玩家数据保存事件
     * 
     * @param playerId 玩家UUID
     * @param module 模块名称
     * @param data 保存的数据
     */
    public PlayerDataSaveEvent(UUID playerId, String module, Object data) {
        super(false);
        this.playerId = playerId;
        this.module = module;
        this.data = data;
    }

    /**
     * 获取玩家UUID
     */
    public UUID getPlayerId() {
        return playerId;
    }

    /**
     * 获取模块名称
     */
    public String getModule() {
        return module;
    }

    /**
     * 获取保存的数据
     */
    @SuppressWarnings("unchecked")
    public <T> T getData() {
        return (T) data;
    }
}