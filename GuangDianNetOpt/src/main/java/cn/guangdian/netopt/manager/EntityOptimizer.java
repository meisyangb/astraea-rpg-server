package cn.guangdian.netopt.manager;

import cn.guangdian.netopt.GuangDianNetOpt;
import cn.guangdian.netopt.config.NetOptConfig;
import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.entity.*;
import org.bukkit.scheduler.BukkitTask;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 实体AI优化器
 * 
 * 职责：禁用静态实体（NPC/动物）的AI计算
 * 适用场景：静态世界的RPG服务器
 * 
 * 效果：
 * - 减少CPU计算（mob.setAware(false)）
 * - 减少实体位置更新包（不移动=不发送）
 * - 保持实体持久化（不意外消失）
 */
public class EntityOptimizer {

    private final GuangDianNetOpt plugin;
    private final NetOptConfig config;
    private BukkitTask optimizerTask;
    private volatile boolean running;
    private final Map<UUID, Boolean> entityOptimized;
    private int entitiesOptimized;
    private int aiDisabled;
    private int movementDisabled;

    public EntityOptimizer(GuangDianNetOpt plugin, NetOptConfig config) {
        this.plugin = plugin;
        this.config = config;
        this.running = false;
        this.entityOptimized = new ConcurrentHashMap<>();
        this.entitiesOptimized = 0;
        this.aiDisabled = 0;
        this.movementDisabled = 0;
    }

    /**
     * 禁用所有静态实体的AI
     */
    public void disableEntityAI() {
        if (!config.isEntityOptEnabled()) {
            plugin.getLogger().info("Entity optimization is disabled in config");
            return;
        }

        running = true;
        
        // 立即优化现有实体
        optimizeExistingEntities();
        
        // 启动定时任务维持优化状态
        startMaintainTask();
        
        plugin.getLogger().info("=== Entity Optimization Applied ===");
        plugin.getLogger().info("AI disabled: " + aiDisabled + " entities");
        plugin.getLogger().info("Movement disabled: " + movementDisabled + " entities");
    }

    private void optimizeExistingEntities() {
        for (World world : Bukkit.getWorlds()) {
            for (Entity entity : world.getEntities()) {
                if (entity instanceof Mob mob) {
                    optimizeEntity(mob);
                }
            }
        }
        entitiesOptimized = aiDisabled;
    }

    private void optimizeEntity(Mob mob) {
        UUID uuid = mob.getUniqueId();
        
        // 已经优化过的跳过
        if (entityOptimized.containsKey(uuid)) {
            return;
        }
        
        // 判断是否应该禁用AI
        boolean shouldDisableAI = shouldDisableEntityAI(mob);
        
        if (config.isDisableMobAI() && shouldDisableAI) {
            mob.setAware(false);      // 禁用AI感知
            mob.setPersistent(true);  // 保持持久化
            entityOptimized.put(uuid, true);
            aiDisabled++;
        }
        
        // 判断是否应该禁用移动
        boolean shouldDisableMovement = shouldDisableEntityMovement(mob);
        
        if (config.isDisableMobMovement() && shouldDisableMovement) {
            // 禁用移动：静止实体不发送位置更新包
            mob.setVelocity(mob.getVelocity().zero());
            movementDisabled++;
        }
    }

    /**
     * 判断实体是否应该禁用AI
     * 静态NPC、村民、动物等不需要AI
     */
    private boolean shouldDisableEntityAI(Mob mob) {
        // 村民 - 静态NPC
        if (mob instanceof Villager) return true;
        
        // 盔甲架 - 完全静态
        if (mob instanceof ArmorStand) return true;
        
        // 农场动物 - 静态养殖
        if (mob instanceof Pig) return true;
        if (mob instanceof Cow) return true;
        if (mob instanceof Sheep) return true;
        if (mob instanceof Chicken) return true;
        if (mob instanceof Horse) return true;
        if (mob instanceof Donkey) return true;
        if (mob instanceof Mule) return true;
        if (mob instanceof Llama) return true;
        
        // 其他静态生物
        if (mob instanceof IronGolem) return true;
        if (mob instanceof Snowman) return true;
        
        // 动态生物（怪物）保留AI
        // Zombie, Skeleton, Spider等需要AI战斗
        
        return false;
    }

    /**
     * 判断实体是否应该禁用移动
     * 完全静态的实体不需要移动
     */
    private boolean shouldDisableEntityMovement(Mob mob) {
        // 盔甲架 - 完全静止
        if (mob instanceof ArmorStand) return true;
        
        // 村民 - 站在固定位置
        if (mob instanceof Villager) return true;
        
        // 其他动态实体可能需要移动
        return false;
    }

    /**
     * 定时任务：维持优化状态
     * 每秒检查一次，确保新实体也被优化
     */
    private void startMaintainTask() {
        optimizerTask = Bukkit.getScheduler().runTaskTimer(plugin, () -> {
            if (!running) return;
            
            // 检查所有实体，确保优化状态
            for (World world : Bukkit.getWorlds()) {
                for (Entity entity : world.getEntities()) {
                    if (entity instanceof Mob mob) {
                        // 检查是否需要重新优化
                        UUID uuid = mob.getUniqueId();
                        if (!entityOptimized.containsKey(uuid)) {
                            optimizeEntity(mob);
                        }
                        
                        // 维持优化状态
                        if (config.isDisableMobAI() && entityOptimized.getOrDefault(uuid, false)) {
                            if (mob.isAware()) {
                                mob.setAware(false);
                            }
                        }
                        
                        // 维持静止状态
                        if (shouldDisableEntityMovement(mob) && config.isDisableMobMovement()) {
                            if (mob.getVelocity().lengthSquared() > 0.001) {
                                mob.setVelocity(mob.getVelocity().zero());
                            }
                        }
                    }
                }
            }
        }, 20L, 20L); // 每秒执行一次
    }

    public void stop() {
        running = false;
        if (optimizerTask != null) {
            optimizerTask.cancel();
        }
        
        // 恢复实体AI（可选）
        plugin.getLogger().info("Restoring entity AI...");
        for (World world : Bukkit.getWorlds()) {
            for (Entity entity : world.getEntities()) {
                if (entity instanceof Mob mob) {
                    if (entityOptimized.containsKey(mob.getUniqueId())) {
                        mob.setAware(true);
                    }
                }
            }
        }
        entityOptimized.clear();
    }

    public int getEntitiesOptimized() {
        return entitiesOptimized;
    }

    public int getAiDisabled() {
        return aiDisabled;
    }

    public int getMovementDisabled() {
        return movementDisabled;
    }

    /**
     * 强制优化某个实体
     */
    public void forceOptimize(Mob mob) {
        optimizeEntity(mob);
    }
}