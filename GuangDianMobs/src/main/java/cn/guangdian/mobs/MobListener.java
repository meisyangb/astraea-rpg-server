package cn.guangdian.mobs;

import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.CreatureSpawnEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDeathEvent;

/**
 * 事件监听器 — 技能触发 + 伤害修正 + 指令掉落
 */
public class MobListener implements Listener {

    private final GuangDianMobs plugin;
    private final MobSpawner spawner;
    private final SkillEngine skills;

    public MobListener(GuangDianMobs plugin) {
        this.plugin = plugin;
        this.spawner = plugin.getMobSpawner();
        this.skills = plugin.getSkillEngine();
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onSpawn(CreatureSpawnEvent e) {
        MobTemplate t = getTemplate(e.getEntity());
        if (t == null) return;
        skills.execute(e.getEntity(), t.skills(), "spawn", null);
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onDamage(EntityDamageByEntityEvent e) {
        if (!(e.getEntity() instanceof LivingEntity victim)) return;
        MobTemplate t = getTemplate(victim);
        if (t == null) return;

        // 伤害修正
        String cause = e.getCause().name();
        double mod = t.damageMods().getOrDefault(cause, t.damageMods().getOrDefault(cause.replace("ENTITY_", "").replace("_ATTACK", ""), 1.0));
        if (mod != 1.0) e.setDamage(e.getDamage() * mod);

        // on-hit 技能
        LivingEntity attacker = e.getDamager() instanceof LivingEntity le ? le : null;
        skills.execute(victim, t.skills(), "hit", attacker instanceof Player p ? p : null);
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onDeath(EntityDeathEvent e) {
        MobTemplate t = getTemplate(e.getEntity());
        if (t == null) return;

        // 禁止原版掉落
        if (t.opts().preventDrops()) { e.getDrops().clear(); e.setDroppedExp(0); }

        // 给经验 (从 Drops 节读取)
        if (plugin.getConfig() != null) {
            // 经验从 Drops 列表: "exp 50"
        }

        Player killer = e.getEntity().getKiller();

        // 死亡技能（含指令掉落）
        skills.execute(e.getEntity(), t.skills(), "death", killer);
    }

    private MobTemplate getTemplate(LivingEntity entity) {
        String id = spawner.getMobId(entity);
        return id != null ? plugin.getMobTemplates().get(id) : null;
    }
}
