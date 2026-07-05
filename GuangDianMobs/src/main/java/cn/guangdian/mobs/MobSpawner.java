package cn.guangdian.mobs;

import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.Location;
import org.bukkit.NamespacedKey;
import org.bukkit.attribute.Attribute;
import org.bukkit.boss.BarColor;
import org.bukkit.boss.BarStyle;
import org.bukkit.boss.BossBar;
import org.bukkit.entity.*;
import org.bukkit.inventory.EntityEquipment;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.logging.Logger;

public class MobSpawner {

    private final JavaPlugin plugin;
    private final Logger log;
    private final NamespacedKey mobIdKey;
    private final MiniMessage mm = MiniMessage.miniMessage();

    public MobSpawner(JavaPlugin plugin) {
        this.plugin = plugin;
        this.log = plugin.getLogger();
        this.mobIdKey = new NamespacedKey(plugin, "gm_mob_id");
    }

    public LivingEntity spawn(MobTemplate t, Location loc) {
        Entity entity = loc.getWorld().spawnEntity(loc, t.entityType());
        if (!(entity instanceof LivingEntity living)) { entity.remove(); return null; }

        // 名称
        living.customName(mm.deserialize(t.displayName()));
        living.setCustomNameVisible(t.opts().showName());

        // 属性
        setAttr(living, Attribute.MAX_HEALTH, t.health()); living.setHealth(t.health());
        setAttr(living, Attribute.MOVEMENT_SPEED, t.speed());
        setAttr(living, Attribute.ATTACK_DAMAGE, t.damage());
        setAttr(living, Attribute.ATTACK_SPEED, t.attackSpeed());
        setAttr(living, Attribute.FOLLOW_RANGE, t.followRange());
        if (t.knockbackResist() >= 0) setAttr(living, Attribute.KNOCKBACK_RESISTANCE, t.knockbackResist());

        if (living instanceof Slime sl && t.opts().size() > 0) sl.setSize(t.opts().size());

        // 装备
        EntityEquipment eq = living.getEquipment();
        if (eq != null) {
            if (t.equipment().containsKey("head")) eq.setHelmet(t.equipment().get("head"));
            if (t.equipment().containsKey("chest")) eq.setChestplate(t.equipment().get("chest"));
            if (t.equipment().containsKey("legs")) eq.setLeggings(t.equipment().get("legs"));
            if (t.equipment().containsKey("feet")) eq.setBoots(t.equipment().get("feet"));
            if (t.equipment().containsKey("hand")) eq.setItemInMainHand(t.equipment().get("hand"));
            if (t.equipment().containsKey("offhand")) eq.setItemInOffHand(t.equipment().get("offhand"));
        }

        // BossBar
        if (t.bossBar().enabled()) createBossBar(living, t);

        // PDC
        living.getPersistentDataContainer().set(mobIdKey, PersistentDataType.STRING, t.id());

        if (t.opts().preventDrops()) living.setRemoveWhenFarAway(false);

        return living;
    }

    private void setAttr(LivingEntity e, Attribute a, double v) {
        var inst = e.getAttribute(a);
        if (inst != null) inst.setBaseValue(v);
    }

    private void createBossBar(LivingEntity entity, MobTemplate t) {
        try {
            BarColor barColor = BarColor.valueOf(t.bossBar().color().toUpperCase());
            BarStyle barStyle;
            try { barStyle = BarStyle.valueOf(t.bossBar().style().toUpperCase()); }
            catch (Exception e) { barStyle = BarStyle.SOLID; }

            String titleTemplate = t.bossBar().title();
            String title = buildTitle(titleTemplate, t.displayName(), entity);
            BossBar bar = plugin.getServer().createBossBar(title, barColor, barStyle);
            bar.setProgress(1.0);
            bar.setVisible(true);

            // 动态更新血量 + 目标
            plugin.getServer().getScheduler().runTaskTimer(plugin, () -> {
                if (!entity.isValid() || entity.isDead()) { bar.removeAll(); return; }
                bar.setProgress(Math.max(0, entity.getHealth() / entity.getMaxHealth()));
                bar.setTitle(buildTitle(titleTemplate, t.displayName(), entity));

                var viewers = bar.getPlayers();
                for (Player p : entity.getWorld().getPlayers()) {
                    double d = p.getLocation().distanceSquared(entity.getLocation());
                    if (d < 2500 && !viewers.contains(p)) bar.addPlayer(p);
                    else if (d > 3600 && viewers.contains(p)) bar.removePlayer(p);
                }
            }, 10, 20);
        } catch (Exception e) { log.warning("BossBar 创建失败: " + e.getMessage()); }
    }

    /** BossBar 标题：变量替换 → MiniMessage解析 → 兼容旧§码 */
    private String buildTitle(String template, String mobName, LivingEntity entity) {
        String targetName = "无";
        if (entity instanceof Mob mob && mob.getTarget() instanceof Player p && !p.isDead()) {
            targetName = p.getName();
        }
        String raw = template
            .replace("<mob.name>", mobName)
            .replace("<mob.hp>", String.format("%.0f", entity.getHealth()))
            .replace("<mob.mhp>", String.format("%.0f", entity.getMaxHealth()))
            .replace("<mob.tt.top>", targetName);

        // MiniMessage → § 码 (兼容 BossBar 纯 String API)
        return LegacyComponentSerializer.legacySection()
            .serialize(mm.deserialize(raw));
    }

    public String getMobId(Entity entity) {
        if (entity == null) return null;
        return entity.getPersistentDataContainer().get(mobIdKey, PersistentDataType.STRING);
    }

    public boolean isCustomMob(Entity entity) { return getMobId(entity) != null; }
    public NamespacedKey getMobIdKey() { return mobIdKey; }
}
