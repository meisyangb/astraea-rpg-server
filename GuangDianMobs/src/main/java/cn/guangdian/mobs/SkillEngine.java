package cn.guangdian.mobs;

import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.NamespacedKey;
import org.bukkit.Particle;
import org.bukkit.Registry;
import org.bukkit.World;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.util.Vector;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 技能引擎 — 参考 MythicMobs 实现粒子效果系统
 * <p>支持格式：</p>
 * <pre>
 * skill:技能ID @Target ~onTimer:100           → 引用 skills/ 中的技能
 * damage{amount=100} @Target ~onTimer:60       → 直接伤害
 * potion{type=SLOW;duration=60;level=1} @Self  → 药水效果
 * heal{amount=300} @Self ~onTimer:120          → 治疗
 * message{m=消息} @PlayersInRadius{r=10} ~onDeath → 广播
 * command{c=rpgitem give <target.name> 物品 1} @Trigger ~onDeath 0.1 → 指令掉落
 * particle{p=flame;amount=20;radius=2} @Self   → 粒子效果
 * particlesphere{p=flame;radius=3;amount=50} @Self → 粒子球体
 * particleline{p=flame;from=@Self;to=@Target} → 粒子连线
 * particlehelix{p=flame;radius=2;height=3} @Self → 粒子螺旋
 * </pre>
 */
public class SkillEngine {

    private final JavaPlugin plugin;
    private final Map<String, Long> cooldowns = new ConcurrentHashMap<>();
    private Map<String, List<MobTemplate.SkillLine>> skillDefs;

    public SkillEngine(JavaPlugin plugin) { this.plugin = plugin; }

    /** 加载 skills/ 文件夹中的技能定义 */
    public void loadSkills(java.io.File dataFolder) {
        skillDefs = new LinkedHashMap<>();
        plugin.getLogger().info("[技能加载] dataFolder=" + dataFolder.getAbsolutePath());
        loadSkillDir(new java.io.File(dataFolder, "skills.yml"));
        java.io.File dir = new java.io.File(dataFolder, "skills");
        plugin.getLogger().info("[技能加载] skills dir=" + dir.getAbsolutePath() + " exists=" + dir.exists());
        java.io.File[] files = dir.listFiles((d, n) -> n.endsWith(".yml"));
        if (files != null) {
            plugin.getLogger().info("[技能加载] 找到 " + files.length + " 个文件");
            for (java.io.File f : files) loadSkillDir(f);
        }
        plugin.getLogger().info("[技能加载] 共 " + skillDefs.size() + " 个技能定义");
    }

    private void loadSkillDir(java.io.File file) {
        plugin.getLogger().info("[技能加载] 读取: " + file.getName());
        if (!file.exists()) { plugin.getLogger().warning("[技能加载] 文件不存在!"); return; }
        org.bukkit.configuration.file.YamlConfiguration cfg =
            org.bukkit.configuration.file.YamlConfiguration.loadConfiguration(file);
        for (String id : cfg.getKeys(false)) {
            var sec = cfg.getConfigurationSection(id);
            if (sec == null) continue;
            List<MobTemplate.SkillLine> lines = new ArrayList<>();
            if (sec.contains("Skills")) {
                // 内联技能格式
                for (String l : sec.getStringList("Skills"))
                    lines.add(new MobTemplate.SkillLine(l));
            } else {
                // 属性格式转换
                String type = sec.getString("type", sec.getString("Type", ""));
                if (!type.isEmpty()) {
                    // 构建参数部分
                    StringBuilder p = new StringBuilder(type.toLowerCase()).append("{");
                    for (String k : sec.getKeys(false)) {
                        if (k.equalsIgnoreCase("type") || k.equalsIgnoreCase("Type") ||
                            k.equalsIgnoreCase("target") || k.equalsIgnoreCase("skills") ||
                            k.equalsIgnoreCase("display-name") || k.equalsIgnoreCase("conditions") ||
                            k.equalsIgnoreCase("sub-skills") || k.equalsIgnoreCase("delay") ||
                            k.equalsIgnoreCase("effects")) continue;
                        p.append(k).append("=").append(sec.get(k)).append(";");
                    }
                    p.append("}");

                    // 构建目标选择器
                    String targetStr = sec.getString("target", sec.getString("Target", "SELF"));
                    String targetSelector = convertTargetToSelector(targetStr, sec);

                    // 构建触发器和间隔
                    int cooldown = sec.getInt("cooldown", sec.getInt("Cooldown", 100));
                    double chance = sec.getDouble("chance", sec.getDouble("Chance", 1.0));

                    String skillLine = p.toString() + " " + targetSelector + " ~onTimer:" + cooldown + " " + chance;
                    lines.add(new MobTemplate.SkillLine(skillLine));

                    // 处理 effects 字段（药水效果）
                    List<String> effects = sec.getStringList("effects");
                    if (!effects.isEmpty() && !type.equalsIgnoreCase("BUFF") && !type.equalsIgnoreCase("DEBUFF")) {
                        for (String effect : effects) {
                            String effectLine = "potion{type=" + effect + ";duration=100;level=1} " + targetSelector + " ~onTimer:" + cooldown;
                            lines.add(new MobTemplate.SkillLine(effectLine));
                        }
                    }

                    // 处理 sub-skills（子技能引用）
                    List<String> subSkills = sec.getStringList("sub-skills");
                    for (String subId : subSkills) {
                        lines.add(new MobTemplate.SkillLine("skill{s=" + subId + "} @Self ~onTimer:0"));
                    }
                }
            }
            if (!lines.isEmpty()) {
                skillDefs.put(id, lines);
                plugin.getLogger().info("[技能加载]   ✓ " + id + " (" + lines.size() + "行)");
            }
        }
    }

    /** 将配置中的 target 字段转换为目标选择器格式 */
    private String convertTargetToSelector(String target, org.bukkit.configuration.ConfigurationSection sec) {
        String t = target.toUpperCase();
        switch (t) {
            case "SELF": return "@Self";
            case "TARGET": return "@Target";
            case "TRIGGER": return "@Trigger";
            default:
                // 检查是否有 range 参数
                if (sec != null && sec.contains("range")) {
                    int range = sec.getInt("range", 10);
                    return "@PlayersInRadius{r=" + range + "}";
                }
                return "@Self";
        }
    }

    // ════════════════════════════════════════
    //  按触发器执行技能列表
    // ════════════════════════════════════════

    public void execute(LivingEntity caster, List<MobTemplate.SkillLine> lines, String trigger, Player killer) {
        if (lines.isEmpty()) return;
        for (MobTemplate.SkillLine line : lines) {
            if (!trigger.equals(line.trigger())) continue;
            if (Math.random() > line.chance()) continue;

            if ("timer".equals(trigger) || "hit".equals(trigger)) {
                String ck = caster.getUniqueId() + "_" + line.raw().hashCode();
                long now = System.currentTimeMillis();
                Long last = cooldowns.get(ck);
                if (last != null && now - last < line.interval() * 50L) continue;
                cooldowns.put(ck, now);
            }

            plugin.getLogger().info("[技能] " + caster.getName() + " 触发: " + line.raw().substring(0, Math.min(60, line.raw().length())));
            execLine(caster, line, killer);
        }
    }

    // ════════════════════════════════════════
    //  执行单行
    // ════════════════════════════════════════

    private static final net.kyori.adventure.text.minimessage.MiniMessage MM =
        net.kyori.adventure.text.minimessage.MiniMessage.miniMessage();

    @SuppressWarnings("deprecation")
    private void execLine(LivingEntity caster, MobTemplate.SkillLine line, Player killer) {
        String type = line.type();
        Map<String, String> p = line.params();

        // 兼容 MythicMobs 参数格式
        // type → particle, count → amount, offset → hS
        String particleName = p.getOrDefault("particle", 
            p.getOrDefault("p", 
            p.getOrDefault("type", ""))).toUpperCase();
        Particle particle = null;
        if (!particleName.isEmpty()) {
            try { particle = getParticle(particleName); } catch (Exception ignored) {}
        }

        switch (type) {
            case "damage" -> {
                double amt = Double.parseDouble(p.getOrDefault("amount", p.getOrDefault("damage", "10")));
                LivingEntity t = findTarget(caster, line.target());
                if (t != null) {
                    t.damage(amt, caster);
                    if (particle != null) playParticleEffect(caster, t.getLocation(), particle, p);
                    playSound(caster, p.getOrDefault("sound", ""));
                }
            }
            case "projectile" -> {
                // 投射物技能 - 发射箭矢或火球
                double damage = Double.parseDouble(p.getOrDefault("damage", "5"));
                LivingEntity target = findTarget(caster, line.target());
                if (target != null) {
                    org.bukkit.entity.Projectile proj = caster.launchProjectile(
                        org.bukkit.entity.Arrow.class,
                        target.getLocation().add(0, 1, 0).toVector().subtract(caster.getLocation().add(0, 1, 0).toVector()).normalize()
                    );
                    proj.setShooter(caster);
                    // 存储伤害值到 PDC
                    proj.getPersistentDataContainer().set(
                        new NamespacedKey(plugin, "skill_damage"),
                        org.bukkit.persistence.PersistentDataType.DOUBLE,
                        damage
                    );
                    if (particle != null) {
                        // 投射物轨迹粒子效果
                        playProjectileTrail(caster.getLocation(), target.getLocation(), particle, p);
                    }
                    playSound(caster, p.getOrDefault("sound", "ENTITY_ARROW_SHOOT"));
                }
            }
            case "debuff" -> {
                // 减益效果技能
                double damage = Double.parseDouble(p.getOrDefault("damage", "0"));
                LivingEntity t = findTarget(caster, line.target());
                if (t != null) {
                    if (damage > 0) t.damage(damage, caster);
                    // 应用药水效果
                    List<String> effects = parseEffects(p.getOrDefault("effects", ""));
                    for (String effectName : effects) {
                        try {
                            PotionEffectType pt = PotionEffectType.getByName(effectName.toUpperCase());
                            if (pt != null) {
                                int dur = Integer.parseInt(p.getOrDefault("duration", "100"));
                                int lvl = Integer.parseInt(p.getOrDefault("level", "1"));
                                t.addPotionEffect(new PotionEffect(pt, dur, lvl - 1));
                            }
                        } catch (Exception ignored) {}
                    }
                    if (particle != null) playParticleEffect(caster, t.getLocation(), particle, p);
                    playSound(caster, p.getOrDefault("sound", ""));
                }
            }
            case "buff" -> {
                // 增益效果技能 - 施加给自己
                List<String> effects = parseEffects(p.getOrDefault("effects", ""));
                for (String effectName : effects) {
                    try {
                        PotionEffectType pt = PotionEffectType.getByName(effectName.toUpperCase());
                        if (pt != null) {
                            int dur = Integer.parseInt(p.getOrDefault("duration", "200"));
                            int lvl = Integer.parseInt(p.getOrDefault("level", "1"));
                            caster.addPotionEffect(new PotionEffect(pt, dur, lvl - 1));
                        }
                    } catch (Exception ignored) {}
                }
                if (particle != null) {
                    // 环绕粒子效果
                    playRingEffect(caster.getLocation().add(0, 1, 0), particle, p);
                }
                playSound(caster, p.getOrDefault("sound", ""));
            }
            case "heal" -> {
                double amt = Double.parseDouble(p.getOrDefault("amount", p.getOrDefault("heal", "50")));
                caster.setHealth(Math.min(caster.getMaxHealth(), caster.getHealth() + amt));
                if (particle != null) {
                    // 心形粒子效果
                    playHeartEffect(caster.getLocation().add(0, 1.5, 0), particle, p);
                }
                playSound(caster, p.getOrDefault("sound", ""));
            }
            case "summon" -> {
                // 召唤技能 - 生成随从
                int count = Integer.parseInt(p.getOrDefault("count", p.getOrDefault("effects", "1")));
                Location loc = caster.getLocation();
                if (particle != null) {
                    // 爆发粒子效果
                    playBurstEffect(loc, particle, p);
                }
                for (int i = 0; i < count; i++) {
                    Location spawnLoc = loc.clone().add(
                        (Math.random() - 0.5) * 3,
                        0,
                        (Math.random() - 0.5) * 3
                    );
                    org.bukkit.entity.Entity summoned = caster.getWorld().spawnEntity(spawnLoc, org.bukkit.entity.EntityType.ZOMBIE);
                    if (summoned instanceof LivingEntity summonedLiving) {
                        summonedLiving.setCustomName("§7随从");
                    }
                }
                playSound(caster, p.getOrDefault("sound", "ENTITY_WITHER_SPAWN"));
            }
            case "teleport" -> {
                // 传送技能
                double range = Double.parseDouble(p.getOrDefault("range", "10"));
                LivingEntity target = findTarget(caster, line.target());
                if (target != null) {
                    Location oldLoc = caster.getLocation();
                    Location newLoc = target.getLocation().add(
                        (Math.random() - 0.5) * range,
                        0,
                        (Math.random() - 0.5) * range
                    );
                    if (particle != null) {
                        // 传送前粒子效果
                        playBurstEffect(oldLoc.add(0, 1, 0), particle, p);
                    }
                    caster.teleport(newLoc);
                    if (particle != null) {
                        // 传送后粒子效果
                        playBurstEffect(newLoc.add(0, 1, 0), particle, p);
                    }
                    playSound(caster, p.getOrDefault("sound", "ENTITY_ENDERMAN_TELEPORT"));
                }
            }
            case "message" -> {
                String msg = p.getOrDefault("m", p.getOrDefault("message", ""));
                double r = 50;
                String tgt = line.target();
                if (tgt.contains("PlayersInRadius")) {
                    try { r = Double.parseDouble(tgt.replaceAll(".*r=(\\d+).*", "$1")); } catch (Exception ignored) {}
                }
                var parsed = MM.deserialize(msg);
                for (var e : caster.getNearbyEntities(r, r, r))
                    if (e instanceof Player pl) pl.sendMessage(parsed);
            }
            // ════════════════════════════════════════
            //  粒子效果技能 - 参考 MythicMobs 实现
            // ════════════════════════════════════════
            case "particle" -> {
                playParticleEffect(caster, caster.getLocation().add(0, 1, 0), particle, p);
            }
            case "particlesphere" -> {
                // 粒子球体 - 参考 MythicMobs ParticleSphere
                playSphereEffect(caster.getLocation().add(0, 1, 0), particle, p);
            }
            case "particleline" -> {
                // 粒子连线 - 参考 MythicMobs ParticleLine
                LivingEntity target = findTarget(caster, line.target());
                if (target != null) {
                    playLineEffect(caster.getLocation().add(0, 1, 0), target.getLocation().add(0, 1, 0), particle, p);
                }
            }
            case "particlehelix" -> {
                // 粒子螺旋 - 参考 MythicMobs
                playHelixEffect(caster.getLocation(), particle, p);
            }
            case "particlering" -> {
                // 粒子环形 - 参考 MythicMobs ParticleRing
                playRingEffect(caster.getLocation().add(0, 1, 0), particle, p);
            }
            case "particleburst" -> {
                // 粒子爆发 - 参考 MythicMobs
                playBurstEffect(caster.getLocation().add(0, 1, 0), particle, p);
            }
            case "ring" -> {
                playRingEffect(caster.getLocation().add(0, 1, 0), particle, p);
            }
            case "box" -> {
                // box 在 MythicMobs 中是爆发效果，不是球体
                playBurstEffect(caster.getLocation().add(0, 1, 0), particle, p);
            }
            case "delay" -> {
                // delay{ticks=40} — 延迟后执行后续技能（暂不支持）
            }
            case "title" -> {
                NetTitle t = new NetTitle(p.getOrDefault("title", ""),
                    p.getOrDefault("subtitle", ""),
                    Integer.parseInt(p.getOrDefault("duration", "60")));
                double r = 50;
                if (line.target().contains("PlayersInRadius"))
                    try { r = Double.parseDouble(line.target().replaceAll(".*r=(\\d+).*", "$1")); } catch (Exception ig) {}
                for (var e : caster.getNearbyEntities(r, r, r))
                    if (e instanceof Player pl) t.send(pl);
            }
            case "command" -> {
                String cmd = p.getOrDefault("c", "");
                if (killer != null) {
                    cmd = cmd.replace("<target.name>", killer.getName());
                    plugin.getServer().dispatchCommand(plugin.getServer().getConsoleSender(), cmd);
                }
            }
            case "skill" -> {
                String refId = p.getOrDefault("s", "");
                if (!refId.isEmpty() && skillDefs != null) {
                    List<MobTemplate.SkillLine> sub = skillDefs.get(refId);
                    if (sub != null) for (MobTemplate.SkillLine subLine : sub) execLine(caster, subLine, killer);
                }
            }
            case "potion" -> {
                try {
                    PotionEffectType pt = PotionEffectType.getByName(p.getOrDefault("type", "SLOW").toUpperCase());
                    if (pt == null) return;
                    int dur = Integer.parseInt(p.getOrDefault("duration", "100"));
                    int lvl = Integer.parseInt(p.getOrDefault("level", "0"));
                    LivingEntity t = "self".equalsIgnoreCase(line.target().replace("@", "")) ? caster : findTarget(caster, line.target());
                    if (t != null) {
                        t.addPotionEffect(new PotionEffect(pt, dur, lvl));
                        if (particle != null) playParticleEffect(caster, t.getLocation(), particle, p);
                    }
                } catch (Exception ignored) {}
            }
        }
    }

    // ════════════════════════════════════════
    //  粒子效果系统 - 参考 MythicMobs 实现
    // ════════════════════════════════════════

    /** Paper 1.21 安全获取 Particle — Registry 方式 */
    private static Particle getParticle(String name) {
        try { return Particle.valueOf(name); }
        catch (Exception e) { /* fallback */ }
        try { return Registry.PARTICLE_TYPE.get(NamespacedKey.minecraft(name.toLowerCase())); }
        catch (Exception e) { return null; }
    }

    /** 基础粒子效果 - 兼容 MythicMobs 参数格式 */
    private void playParticleEffect(LivingEntity caster, Location loc, Particle particle, Map<String, String> p) {
        if (particle == null) return;
        // 兼容 MythicMobs: count → amount, offset → hS
        int amount = Integer.parseInt(p.getOrDefault("amount", 
            p.getOrDefault("a", 
            p.getOrDefault("count", "20"))));
        double offset = Double.parseDouble(p.getOrDefault("offset", 
            p.getOrDefault("hS", 
            p.getOrDefault("hSpread", "0.5"))));
        double vOffset = Double.parseDouble(p.getOrDefault("vOffset", 
            p.getOrDefault("vS", 
            p.getOrDefault("vSpread", "0.5"))));
        double speed = Double.parseDouble(p.getOrDefault("speed", 
            p.getOrDefault("s", "0.1")));
        
        // 支持颜色粒子 (DUST)
        if (particle == Particle.DUST && p.containsKey("color")) {
            Color color = parseColor(p.get("color"));
            float size = Float.parseFloat(p.getOrDefault("size", "1.0"));
            Particle.DustOptions dustOptions = new Particle.DustOptions(color, size);
            loc.getWorld().spawnParticle(particle, loc, amount, offset, vOffset, offset, dustOptions);
        } else {
            loc.getWorld().spawnParticle(particle, loc, amount, offset, vOffset, offset, speed);
        }
    }

    /** 粒子球体效果 - 参考 MythicMobs ParticleSphere */
    private void playSphereEffect(Location center, Particle particle, Map<String, String> p) {
        if (particle == null) return;
        // 兼容 MythicMobs 参数
        double radius = Double.parseDouble(p.getOrDefault("radius", 
            p.getOrDefault("r", "2.0")));
        int amount = Integer.parseInt(p.getOrDefault("amount", 
            p.getOrDefault("a", 
            p.getOrDefault("count", "50"))));
        int points = Integer.parseInt(p.getOrDefault("points", "20"));
        double speed = Double.parseDouble(p.getOrDefault("speed", 
            p.getOrDefault("s", "0.02")));
        
        World world = center.getWorld();
        // 使用球体坐标公式
        for (int i = 0; i < points; i++) {
            double phi = Math.PI * i / points; // 0 到 π
            for (int j = 0; j < points; j++) {
                double theta = 2 * Math.PI * j / points; // 0 到 2π
                double x = radius * Math.sin(phi) * Math.cos(theta);
                double y = radius * Math.cos(phi);
                double z = radius * Math.sin(phi) * Math.sin(theta);
                Location loc = center.clone().add(x, y, z);
                world.spawnParticle(particle, loc, Math.max(1, amount / (points * points)), 0, 0, 0, speed);
            }
        }
    }

    /** 粒子环形效果 - 参考 MythicMobs ParticleRing */
    private void playRingEffect(Location center, Particle particle, Map<String, String> p) {
        if (particle == null) return;
        // 兼容 MythicMobs 参数
        double radius = Double.parseDouble(p.getOrDefault("radius", 
            p.getOrDefault("r", "2.0")));
        int points = Integer.parseInt(p.getOrDefault("points", "24"));
        int amount = Integer.parseInt(p.getOrDefault("amount", 
            p.getOrDefault("a", 
            p.getOrDefault("count", "5"))));
        double yOff = Double.parseDouble(p.getOrDefault("y", "0"));
        double speed = Double.parseDouble(p.getOrDefault("speed", 
            p.getOrDefault("s", "0.03")));
        
        World world = center.getWorld();
        Location adjustedCenter = center.clone().add(0, yOff, 0);
        for (int i = 0; i < points; i++) {
            double angle = 2 * Math.PI * i / points;
            double x = Math.cos(angle) * radius;
            double z = Math.sin(angle) * radius;
            Location loc = adjustedCenter.clone().add(x, 0, z);
            world.spawnParticle(particle, loc, amount, 0, 0, 0, speed);
        }
    }

    /** 粒子连线效果 - 参考 MythicMobs ParticleLine */
    private void playLineEffect(Location from, Location to, Particle particle, Map<String, String> p) {
        if (particle == null) return;
        int amount = Integer.parseInt(p.getOrDefault("amount", p.getOrDefault("a", "30")));
        double speed = Double.parseDouble(p.getOrDefault("speed", "0.02"));
        
        World world = from.getWorld();
        Vector direction = to.toVector().subtract(from.toVector());
        double length = direction.length();
        direction.normalize();
        
        int steps = Math.max(10, (int) (length * 5));
        for (int i = 0; i <= steps; i++) {
            double ratio = i / (double) steps;
            Location loc = from.clone().add(direction.clone().multiply(length * ratio));
            world.spawnParticle(particle, loc, Math.max(1, amount / steps), 0, 0, 0, speed);
        }
    }

    /** 粒子螺旋效果 - 参考 MythicMobs */
    private void playHelixEffect(Location center, Particle particle, Map<String, String> p) {
        if (particle == null) return;
        double radius = Double.parseDouble(p.getOrDefault("radius", p.getOrDefault("r", "1.5")));
        double height = Double.parseDouble(p.getOrDefault("height", p.getOrDefault("h", "3.0")));
        int revolutions = Integer.parseInt(p.getOrDefault("revolutions", "3"));
        int points = Integer.parseInt(p.getOrDefault("points", "50"));
        int amount = Integer.parseInt(p.getOrDefault("amount", p.getOrDefault("a", "3")));
        double speed = Double.parseDouble(p.getOrDefault("speed", "0.02"));
        
        World world = center.getWorld();
        double totalAngle = 2 * Math.PI * revolutions;
        for (int i = 0; i <= points; i++) {
            double ratio = i / (double) points;
            double angle = totalAngle * ratio;
            double y = height * ratio;
            double x = Math.cos(angle) * radius;
            double z = Math.sin(angle) * radius;
            Location loc = center.clone().add(x, y, z);
            world.spawnParticle(particle, loc, amount, 0, 0, 0, speed);
        }
    }

    /** 粒子爆发效果 - 参考 MythicMobs (box 类型) */
    private void playBurstEffect(Location center, Particle particle, Map<String, String> p) {
        if (particle == null) return;
        // 兼容 MythicMobs 参数
        int amount = Integer.parseInt(p.getOrDefault("amount", 
            p.getOrDefault("a", 
            p.getOrDefault("count", "50"))));
        double radius = Double.parseDouble(p.getOrDefault("radius", 
            p.getOrDefault("r", "2.0")));
        double speed = Double.parseDouble(p.getOrDefault("speed", 
            p.getOrDefault("s", "0.3")));
        
        center.getWorld().spawnParticle(particle, center, amount, radius, radius, radius, speed);
    }

    /** 投射物轨迹粒子效果 */
    private void playProjectileTrail(Location from, Location to, Particle particle, Map<String, String> p) {
        if (particle == null) return;
        int amount = Integer.parseInt(p.getOrDefault("trailAmount", "10"));
        playLineEffect(from.add(0, 1, 0), to.add(0, 1, 0), particle, Map.of("amount", String.valueOf(amount), "speed", "0.05"));
    }

    /** 心形粒子效果 */
    private void playHeartEffect(Location center, Particle particle, Map<String, String> p) {
        if (particle == null) return;
        int amount = Integer.parseInt(p.getOrDefault("amount", "15"));
        center.getWorld().spawnParticle(particle, center, amount, 0.3, 0.3, 0.3, 0.05);
    }

    /** 解析颜色字符串 */
    private Color parseColor(String colorStr) {
        try {
            if (colorStr.startsWith("#")) {
                // 十六进制颜色
                int rgb = Integer.parseInt(colorStr.substring(1), 16);
                return Color.fromRGB(rgb);
            } else if (colorStr.contains(",")) {
                // RGB 格式 "255,0,0"
                String[] parts = colorStr.split(",");
                int r = Integer.parseInt(parts[0].trim());
                int g = Integer.parseInt(parts[1].trim());
                int b = Integer.parseInt(parts[2].trim());
                return Color.fromRGB(r, g, b);
            } else {
                // 预定义颜色名称
                return switch (colorStr.toUpperCase()) {
                    case "RED" -> Color.RED;
                    case "GREEN" -> Color.GREEN;
                    case "BLUE" -> Color.BLUE;
                    case "YELLOW" -> Color.YELLOW;
                    case "PURPLE" -> Color.PURPLE;
                    case "ORANGE" -> Color.ORANGE;
                    case "WHITE" -> Color.WHITE;
                    case "BLACK" -> Color.BLACK;
                    default -> Color.RED;
                };
            }
        } catch (Exception e) {
            return Color.RED;
        }
    }

    // ════════════════════════════════════════
    //  目标选择
    // ════════════════════════════════════════

    private static class NetTitle {
        final net.kyori.adventure.title.Title title;
        NetTitle(String t, String st, int duration) {
            this.title = net.kyori.adventure.title.Title.title(
                MM.deserialize(t), MM.deserialize(st),
                net.kyori.adventure.title.Title.Times.times(
                    java.time.Duration.ofMillis(500),
                    java.time.Duration.ofMillis(duration * 50L),
                    java.time.Duration.ofMillis(500)));
        }
        void send(Player p) { p.showTitle(title); }
    }

    private LivingEntity findTarget(LivingEntity caster, String targetStr) {
        String t = targetStr.replace("@", "").toLowerCase();
        if (t.equals("self")) return caster;
        if (t.equals("target") || t.equals("trigger")) {
            if (caster instanceof org.bukkit.entity.Mob m) return m.getTarget();
            return null;
        }
        if (t.startsWith("playersinradius")) {
            double r = 5;
            try { r = Double.parseDouble(t.replaceAll(".*r=(\\d+).*", "$1")); } catch (Exception ignored) {}
            double best = Double.MAX_VALUE; Player bestP = null;
            for (var e : caster.getNearbyEntities(r, r, r)) {
                if (e instanceof Player p && !p.isDead()) {
                    double d = p.getLocation().distanceSquared(caster.getLocation());
                    if (d < best) { best = d; bestP = p; }
                }
            }
            return bestP;
        }
        return null;
    }

    /** 解析 effects 字段，格式可能是 "POISON" 或 "[POISON, WEAKNESS]" */
    private List<String> parseEffects(String effectsStr) {
        List<String> result = new ArrayList<>();
        if (effectsStr.isEmpty()) return result;
        String cleaned = effectsStr.replace("[", "").replace("]", "").replace("\"", "");
        for (String e : cleaned.split(",")) {
            String trimmed = e.trim();
            if (!trimmed.isEmpty()) result.add(trimmed);
        }
        return result;
    }

    /** 播放音效 */
    private void playSound(LivingEntity entity, String soundName) {
        if (soundName.isEmpty()) return;
        try {
            org.bukkit.Sound sound = org.bukkit.Sound.valueOf(soundName.toUpperCase());
            float volume = 1.0f;
            float pitch = 1.0f;
            entity.getWorld().playSound(entity.getLocation(), sound, volume, pitch);
        } catch (Exception ignored) {}
    }
}