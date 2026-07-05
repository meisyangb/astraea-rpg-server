package cn.guangdian.mobs;

import org.bukkit.GameMode;
import org.bukkit.entity.*;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 怪物 AI + 定时技能执行
 * <p>每 1tick: 目标选择 + 执行 onTimer 技能</p>
 * <p>技能冷却由 SkillEngine 内部管理</p>
 */
public class MobAIController {

    private final GuangDianMobs plugin;
    private final Map<UUID, MobTemplate> states = new ConcurrentHashMap<>();
    private int taskId = -1;

    public MobAIController(GuangDianMobs plugin) { this.plugin = plugin; }

    public void start() {
        // 每 1 tick 执行一次，让技能冷却精确生效
        taskId = plugin.getServer().getScheduler().runTaskTimer(plugin, this::tick, 1, 1).getTaskId();
    }

    public void stop() {
        if (taskId != -1) plugin.getServer().getScheduler().cancelTask(taskId);
        states.clear();
    }

    public void attach(LivingEntity entity, MobTemplate template) {
        states.put(entity.getUniqueId(), template);
    }

    public void detach(UUID id) { states.remove(id); }

    private void tick() {
        var it = states.entrySet().iterator();
        while (it.hasNext()) {
            var e = it.next();
            Entity ent = plugin.getServer().getEntity(e.getKey());
            if (!(ent instanceof LivingEntity living) || living.isDead()) { it.remove(); continue; }
            update(living, e.getValue());
        }
    }

    private void update(LivingEntity entity, MobTemplate t) {
        // 1. 目标选择
        if (entity instanceof Mob mob) {
            LivingEntity target = findTarget(entity);
            if (target != null) mob.setTarget(target);
        }

        // 2. 执行 timer 技能 (粒子/伤害/AOE/光环)
        Player killer = entity.getKiller(); // 可能为 null
        plugin.getSkillEngine().execute(entity, t.skills(), "timer", killer);
    }

    private LivingEntity findTarget(LivingEntity self) {
        Player best = null;
        double bestD = Double.MAX_VALUE;
        for (Entity e : self.getNearbyEntities(50, 50, 50)) {
            if (!(e instanceof Player p)) continue;
            if (p.isDead() || p.getGameMode() == GameMode.CREATIVE || p.getGameMode() == GameMode.SPECTATOR) continue;
            double d = p.getLocation().distanceSquared(self.getLocation());
            if (d < bestD) { bestD = d; best = p; }
        }
        return best;
    }
}
