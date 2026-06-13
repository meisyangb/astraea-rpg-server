package cn.guangdian.rpgcore.service.api;

import org.bukkit.entity.LivingEntity;

/**
 * 怪物血量显示服务接口
 * 
 * <p>提供怪物血量显示、隐藏等功能。</p>
 * 
 * @author GuangDian
 * @since 1.0.0
 */
public interface MobHealthService {

    /**
     * 显示实体血量
     * 
     * @param entity 实体
     */
    void showHealth(LivingEntity entity);

    /**
     * 隐藏实体血量
     * 
     * @param entity 实体
     */
    void hideHealth(LivingEntity entity);

    /**
     * 更新实体血量显示
     * 
     * @param entity 实体
     */
    void updateHealth(LivingEntity entity);

    /**
     * 清除所有血量显示
     */
    void clearAll();

    /**
     * 检查血量显示是否启用
     * 
     * @return 是否启用
     */
    boolean isEnabled();

    /**
     * 设置血量显示启用状态
     * 
     * @param enabled 是否启用
     */
    void setEnabled(boolean enabled);

    /**
     * 重载配置
     */
    void reload();

    /**
     * 检查服务是否可用
     * 
     * @return 如果服务可用返回 true
     */
    boolean isAvailable();
}
