package cn.guangdian.rpgcore.entity;

import org.bukkit.Location;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.vehicle.VehicleEntityCollisionEvent;
import org.bukkit.util.Vector;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * RPGCore 实体服务
 *
 * 封装 Paper 1.21.6 实体相关 API，提供统一的实体管理接口
 * 解决 setCollisionCancelled() 等弃用问题
 */
public final class EntityService {

    private static EntityService instance;

    // 碰撞禁用缓存
    private final Map<UUID, Boolean> collisionDisabled = new HashMap<>();

    private EntityService() {}

    public static synchronized EntityService getInstance() {
        if (instance == null) {
            instance = new EntityService();
        }
        return instance;
    }

    /**
     * 设置实体碰撞状态（解决 VehicleEntityCollisionEvent.setCollisionCancelled() 弃用问题）
     *
     * 通过设置实体的碰撞状态来实现，而不是依赖事件
     *
     * @param entity 目标实体
     * @param cancelled 是否禁用碰撞
     */
    public void setCollisionCancelled(Entity entity, boolean cancelled) {
        if (entity == null) return;

        UUID entityId = entity.getUniqueId();
        if (cancelled) {
            collisionDisabled.put(entityId, true);
            // 对于 LivingEntity，可以通过设置无敌状态来间接禁用碰撞
            if (entity instanceof org.bukkit.entity.LivingEntity living) {
                living.setInvulnerable(true);
            }
        } else {
            collisionDisabled.remove(entityId);
            if (entity instanceof org.bukkit.entity.LivingEntity living) {
                living.setInvulnerable(false);
            }
        }
    }

    /**
     * 检查实体是否禁用了碰撞
     */
    public boolean isCollisionCancelled(Entity entity) {
        if (entity == null) return false;
        return collisionDisabled.getOrDefault(entity.getUniqueId(), false);
    }

    /**
     * 处理载具碰撞事件（替代 setCollisionCancelled）
     *
     * @param event 载具碰撞事件
     * @param cancelled 是否取消碰撞
     */
    public void handleVehicleCollision(VehicleEntityCollisionEvent event, boolean cancelled) {
        if (cancelled) {
            // 取消碰撞的方式：将碰撞实体推开
            Entity vehicle = event.getVehicle();
            Entity collided = event.getEntity();

            Vector pushDirection = collided.getLocation().toVector()
                .subtract(vehicle.getLocation().toVector())
                .normalize()
                .multiply(0.5);

            collided.setVelocity(pushDirection);
            event.setCancelled(true);
        }
    }

    /**
     * 安全传送实体（带碰撞检测）
     */
    public boolean teleportSafely(Entity entity, Location location) {
        if (entity == null || location == null) return false;

        // 检查目标位置是否安全
        if (!isLocationSafe(location)) {
            return false;
        }

        return entity.teleport(location);
    }

    /**
     * 检查位置是否安全（无碰撞）
     */
    public boolean isLocationSafe(Location location) {
        if (location == null) return false;

        // 检查是否为固体方块
        return location.getBlock().isPassable();
    }

    /**
     * 设置实体无敌状态
     */
    public void setInvulnerable(Entity entity, boolean invulnerable) {
        if (entity == null) return;
        entity.setInvulnerable(invulnerable);
    }

    /**
     * 检查实体是否无敌
     */
    public boolean isInvulnerable(Entity entity) {
        if (entity == null) return false;
        return entity.isInvulnerable();
    }

    /**
     * 设置实体静默状态
     */
    public void setSilent(Entity entity, boolean silent) {
        if (entity == null) return;
        entity.setSilent(silent);
    }

    /**
     * 设置实体发光状态
     */
    public void setGlowing(Entity entity, boolean glowing) {
        if (entity == null) return;
        entity.setGlowing(glowing);
    }

    /**
     * 设置实体可见性
     */
    public void setVisible(Entity entity, boolean visible) {
        if (entity == null) return;
        entity.setVisibleByDefault(visible);
    }

    /**
     * 对实体造成伤害
     */
    public void damage(Entity entity, double damage) {
        if (entity == null || damage <= 0) return;

        if (entity instanceof Player player) {
            player.damage(damage);
        } else if (entity instanceof org.bukkit.entity.LivingEntity living) {
            living.damage(damage);
        }
    }

    /**
     * 获取实体朝向向量
     */
    public Vector getDirection(Entity entity) {
        if (entity == null) return new Vector(0, 0, 0);
        return entity.getLocation().getDirection();
    }

    /**
     * 计算两个实体之间的距离
     */
    public double getDistance(Entity entity1, Entity entity2) {
        if (entity1 == null || entity2 == null) return Double.MAX_VALUE;
        return entity1.getLocation().distance(entity2.getLocation());
    }

    /**
     * 计算两个实体之间的距离（平方，性能更好）
     */
    public double getDistanceSquared(Entity entity1, Entity entity2) {
        if (entity1 == null || entity2 == null) return Double.MAX_VALUE;
        return entity1.getLocation().distanceSquared(entity2.getLocation());
    }

    /**
     * 检查实体是否在指定范围内
     */
    public boolean isInRange(Entity entity1, Entity entity2, double range) {
        return getDistanceSquared(entity1, entity2) <= range * range;
    }

    /**
     * 清除实体的碰撞禁用状态
     */
    public void clearCollisionState(UUID entityId) {
        collisionDisabled.remove(entityId);
    }

    /**
     * 清除所有碰撞禁用状态
     */
    public void clearAllCollisionStates() {
        collisionDisabled.clear();
    }
}
