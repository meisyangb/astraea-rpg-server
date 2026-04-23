package cn.guangdian.armorstats.manager;

import cn.guangdian.armorstats.GuangDianArmorStats;
import cn.guangdian.armorstats.boss.BossStatsManager;
import cn.guangdian.armorstats.combat.DamageContext;
import cn.guangdian.armorstats.combat.DamageSource;
import cn.guangdian.armorstats.combat.interceptor.impl.*;
import cn.guangdian.armorstats.combat.pipeline.DamagePipeline;
import cn.guangdian.armorstats.data.PlayerStats;
import cn.guangdian.armorstats.event.RpgPreDamageEvent;
import cn.guangdian.armorstats.event.RpgDamageCalculateEvent;
import cn.guangdian.armorstats.event.RpgPostDamageEvent;
import cn.guangdian.armorstats.formula.DamageFormulaManager;
import cn.guangdian.armorstats.parser.LoreParser;
import cn.guangdian.armorstats.skill.SkillIntegration;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Villager;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.Plugin;

import java.lang.reflect.Method;

public class DamageManager {

    private final StatsManager statsManager;
    private final DamagePipeline pipeline;
    private SkillIntegration skillIntegration;  // 通过 RPGSkill 执行技能（解耦）
    private CombatLogManager combatLogManager;
    private PostDamageInterceptor postInterceptor;
    private BossStatsInterceptor bossStatsInterceptor;
    private DamageFormulaManager formulaManager;

    private Object mythicMobsPlugin;
    private Object mythicMobManager;
    private Method isMythicMobMethod;
    private Method getMythicMobInstanceMethod;

    private BossStatsManager bossStatsManager;

    private double minDamage;

    public DamageManager(StatsManager statsManager, SkillIntegration skillIntegration) {
        this.statsManager = statsManager;
        this.skillIntegration = skillIntegration;
        this.pipeline = new DamagePipeline(GuangDianArmorStats.getInstance());
        this.formulaManager = new DamageFormulaManager(GuangDianArmorStats.getInstance());

        loadConfig();
        registerDefaultInterceptors();
    }

    private void loadConfig() {
        var config = GuangDianArmorStats.getInstance().getConfig();
        var damageSection = config.getConfigurationSection("damage");
        if (damageSection != null) {
            minDamage = damageSection.getDouble("min_damage", 1.0);
        } else {
            minDamage = 1.0;
        }
    }

    private void registerDefaultInterceptors() {
        // BOSS 属性拦截器 - 暂时注释，使用 MythicMobs 自带属性
        // bossStatsInterceptor = new BossStatsInterceptor();
        // pipeline.registerInterceptor(bossStatsInterceptor);
        
        pipeline.registerInterceptor(new AttackInterceptor());
        pipeline.registerInterceptor(new DefenseInterceptor());
        pipeline.registerInterceptor(new CritInterceptor());
        postInterceptor = new PostDamageInterceptor(GuangDianArmorStats.getInstance());
        pipeline.registerInterceptor(postInterceptor);
    }

    public void initMythicMobs() {
        try {
            Plugin plugin = Bukkit.getPluginManager().getPlugin("MythicMobs");
            if (plugin != null && plugin.isEnabled()) {
                mythicMobsPlugin = plugin;
                Class<?> mythicBukkitClass = Class.forName("io.lumine.mythic.bukkit.MythicBukkit");
                Method instMethod = mythicBukkitClass.getMethod("inst");
                Object mythicBukkitInst = instMethod.invoke(null);
                Method getMobManagerMethod = mythicBukkitClass.getMethod("getMobManager");
                mythicMobManager = getMobManagerMethod.invoke(mythicBukkitInst);

                Class<?> mobManagerClass = mythicMobManager.getClass();
                isMythicMobMethod = mobManagerClass.getMethod("isMythicMob", Entity.class);
                getMythicMobInstanceMethod = mobManagerClass.getMethod("getMythicMobInstance", Entity.class);

                GuangDianArmorStats.getInstance().getLogger().info("MythicMobs integration initialized successfully");
                // initBossStatsManager(); // 暂时注释，使用 MythicMobs 自带属性
            }
        } catch (Exception e) {
            GuangDianArmorStats.getInstance().getLogger().info("MythicMobs not found: " + e.getMessage());
        }
    }

    // 暂时注释，使用 MythicMobs 自带属性
    /*
    private void initBossStatsManager() {
        bossStatsManager = new BossStatsManager(GuangDianArmorStats.getInstance());
        if (bossStatsInterceptor != null) {
            bossStatsInterceptor.setBossStatsManager(bossStatsManager);
            
            var config = GuangDianArmorStats.getInstance().getConfig();
            var globalSection = config.getConfigurationSection("global");
            if (globalSection != null) {
                boolean overrideDamage = globalSection.getBoolean("override_mythic_damage", true);
                bossStatsInterceptor.setOverrideMythicDamage(overrideDamage);
                GuangDianArmorStats.getInstance().getLogger().info(
                    "BossStats override MythicMobs damage: " + overrideDamage);
            }
        }
        GuangDianArmorStats.getInstance().getLogger().info("BossStatsManager initialized");
    }
    */

    public void setSkillIntegration(SkillIntegration skillIntegration) {
        this.skillIntegration = skillIntegration;
    }

    public void setCombatLogManager(CombatLogManager combatLogManager) {
        this.combatLogManager = combatLogManager;
        if (postInterceptor != null) {
            postInterceptor.setCombatLogManager(combatLogManager);
        }
    }

    public void handlePlayerAttack(EntityDamageByEntityEvent event) {
        Entity damagerEntity = event.getDamager();
        Entity targetEntity = event.getEntity();

        if (!(damagerEntity instanceof Player attacker)) {
            return;
        }

        if (!(targetEntity instanceof LivingEntity target)) {
            return;
        }

        boolean isNPC = isGuangDianNPC(target);
        if (isNPC) {
            event.setCancelled(true);
            event.setDamage(0);
            return;
        }

        PlayerStats attackerStats = statsManager.getPlayerStats(attacker);
        if (attackerStats == null) {
            return;
        }

        boolean isPVP = targetEntity instanceof Player;
        Player targetPlayer = isPVP ? (Player) targetEntity : null;
        PlayerStats targetStats = targetPlayer != null ? statsManager.getPlayerStats(targetPlayer) : null;

        // 技能伤害检测 - 使用 RPGSkill 的元数据键（如果 RPGSkill 设置了）
        boolean isSkillDamage = attacker.hasMetadata("SKILL_DAMAGE");
        double baseDamage = event.getDamage();

        if (isSkillDamage) {
            baseDamage = attacker.getMetadata("SKILL_DAMAGE").get(0).asDouble();
        }

        boolean isVanillaAttack = !isSkillDamage && isVanillaWeapon(attacker);

        DamageContext context = new DamageContext(
            event,
            attacker,
            target,
            attacker,
            targetPlayer,
            attackerStats,
            targetStats,
            isPVP,
            baseDamage
        );

        if (isSkillDamage) {
            context.setSkillDamage(true);
            context.setDamageSource(DamageSource.SKILL);
        }
        
        if (isVanillaAttack) {
            context.setVanillaAttack(true);
        }
        
        context.setNPC(isNPC);

        RpgPreDamageEvent preEvent = new RpgPreDamageEvent(context);
        Bukkit.getPluginManager().callEvent(preEvent);
        
        if (preEvent.isCancelled()) {
            event.setCancelled(true);
            return;
        }

        if (isVanillaAttack) {
            event.setDamage(baseDamage);
            return;
        }

        context = pipeline.processPlayerAttack(context);

        RpgDamageCalculateEvent calculateEvent = new RpgDamageCalculateEvent(
            context, context.getBaseDamage(), context.getFinalDamage());
        Bukkit.getPluginManager().callEvent(calculateEvent);
        
        if (calculateEvent.isCancelled()) {
            event.setCancelled(true);
            return;
        }
        
        context.setFinalDamage(calculateEvent.getFinalDamage());

        if (!context.isCancelled() && !context.isDodged() && !context.isParried()) {
            double finalDamage = Math.max(minDamage, context.getFinalDamage());
            event.setDamage(finalDamage);
            
            RpgPostDamageEvent postEvent = new RpgPostDamageEvent(context, finalDamage, false);
            Bukkit.getPluginManager().callEvent(postEvent);
        } else {
            event.setDamage(0);
            event.setCancelled(context.isCancelled());
        }
    }
    
    private boolean isGuangDianNPC(LivingEntity entity) {
        if (!(entity instanceof Villager)) {
            return false;
        }
        
        if (entity.getScoreboardTags().contains("guangdian_npc")) {
            return true;
        }
        
        Plugin npcPlugin = Bukkit.getPluginManager().getPlugin("GuangDianNPC");
        if (npcPlugin != null && npcPlugin.isEnabled()) {
            NamespacedKey key = new NamespacedKey(npcPlugin, "npc_id");
            return entity.getPersistentDataContainer().has(key, PersistentDataType.STRING);
        }
        
        return false;
    }
    
    private boolean isVanillaWeapon(Player player) {
        ItemStack mainHand = player.getInventory().getItemInMainHand();
        ItemStack offHand = player.getInventory().getItemInOffHand();
        
        boolean mainHandHasRpg = hasRpgAttributes(mainHand);
        boolean offHandHasRpg = hasRpgAttributes(offHand);
        
        return !mainHandHasRpg && !offHandHasRpg;
    }
    
    private boolean hasRpgAttributes(ItemStack item) {
        if (item == null || item.getType() == Material.AIR) {
            return false;
        }
        
        if (!item.hasItemMeta()) {
            return false;
        }
        
        var lore = item.getItemMeta().getLore();
        if (lore == null || lore.isEmpty()) {
            return false;
        }
        
        return LoreParser.hasRpgAttributes(item);
    }

    public void handlePlayerDamage(EntityDamageByEntityEvent event) {
        Entity damagerEntity = event.getDamager();
        Entity damagedEntity = event.getEntity();

        if (!(damagedEntity instanceof Player defender)) {
            return;
        }

        PlayerStats defenderStats = statsManager.getPlayerStats(defender);
        if (defenderStats == null) return;

        double mobDamage = event.getDamage();
        LivingEntity mobAttacker = null;

        if (damagerEntity instanceof LivingEntity) {
            mobAttacker = (LivingEntity) damagerEntity;
            mobDamage = getMythicMobDamage(mobAttacker, mobDamage);
        }

        DamageContext context = new DamageContext(
            event,
            mobAttacker,
            defender,
            null,
            defender,
            null,
            defenderStats,
            false,
            mobDamage
        );

        if (mobAttacker != null && isMythicMob(mobAttacker)) {
            boolean isSkillDamage = isMythicMobSkillDamage(event, mobAttacker);
            context.setDamageSource(isSkillDamage ? DamageSource.MYTHICMOB_SKILL : DamageSource.MYTHICMOB);
            context.setSkillDamage(isSkillDamage);
        }

        RpgPreDamageEvent preEvent = new RpgPreDamageEvent(context);
        Bukkit.getPluginManager().callEvent(preEvent);
        
        if (preEvent.isCancelled()) {
            event.setCancelled(true);
            return;
        }

        context = pipeline.processPlayerDamage(context);

        RpgDamageCalculateEvent calculateEvent = new RpgDamageCalculateEvent(
            context, context.getBaseDamage(), context.getFinalDamage());
        Bukkit.getPluginManager().callEvent(calculateEvent);
        
        if (calculateEvent.isCancelled()) {
            event.setCancelled(true);
            return;
        }
        
        context.setFinalDamage(calculateEvent.getFinalDamage());

        if (!context.isCancelled() && !context.isDodged() && !context.isParried()) {
            double finalDamage = Math.max(minDamage, context.getFinalDamage());
            event.setDamage(finalDamage);
            
            RpgPostDamageEvent postEvent = new RpgPostDamageEvent(context, finalDamage, false);
            Bukkit.getPluginManager().callEvent(postEvent);
        } else {
            event.setDamage(0);
            event.setCancelled(context.isCancelled());
        }
    }

    private boolean isMythicMobSkillDamage(EntityDamageByEntityEvent event, LivingEntity mob) {
        var cause = event.getCause();
        if (cause == org.bukkit.event.entity.EntityDamageEvent.DamageCause.MAGIC ||
            cause == org.bukkit.event.entity.EntityDamageEvent.DamageCause.CUSTOM ||
            cause == org.bukkit.event.entity.EntityDamageEvent.DamageCause.WITHER ||
            cause == org.bukkit.event.entity.EntityDamageEvent.DamageCause.POISON) {
            return true;
        }
        
        if (event.getDamager() != mob) {
            return true;
        }
        
        return false;
    }

    public boolean isMythicMob(LivingEntity entity) {
        if (mythicMobManager == null || isMythicMobMethod == null) {
            return false;
        }
        try {
            return (boolean) isMythicMobMethod.invoke(mythicMobManager, entity);
        } catch (Exception e) {
            return false;
        }
    }

    private double getMythicMobDamage(LivingEntity mob, double baseDamage) {
        if (!isMythicMob(mob)) {
            return baseDamage;
        }
        try {
            Object mythicMobInstance = getMythicMobInstanceMethod.invoke(mythicMobManager, mob);
            if (mythicMobInstance != null) {
                Class<?> instanceClass = mythicMobInstance.getClass();
                Method getTypeMethod = instanceClass.getMethod("getType");
                Object mythicType = getTypeMethod.invoke(mythicMobInstance);

                if (mythicType != null) {
                    Class<?> typeClass = mythicType.getClass();
                    Method getDamageMethod = typeClass.getMethod("getDamage");
                    Object damageObj = getDamageMethod.invoke(mythicType);

                    if (damageObj instanceof Number) {
                        return ((Number) damageObj).doubleValue();
                    }
                }
            }
        } catch (Exception e) {
            // MythicMobs 反射调用失败，使用基础伤害
            java.util.logging.Logger.getLogger("GuangDianArmorStats").fine("[GuangDianArmorStats] 获取MythicMobs伤害失败: " + e.getMessage());
        }
        return baseDamage;
    }

    public DamagePipeline getPipeline() {
        return pipeline;
    }

    public void reloadConfig() {
        loadConfig();
        if (bossStatsManager != null) {
            bossStatsManager.reloadConfig();
        }
        if (formulaManager != null) {
            formulaManager.reloadConfig();
        }
    }

    public BossStatsManager getBossStatsManager() {
        return bossStatsManager;
    }

    public DamageFormulaManager getFormulaManager() {
        return formulaManager;
    }

    public double getTotalAttack(Player player) {
        PlayerStats stats = statsManager.getPlayerStats(player);
        if (stats == null) return 1.0;
        return 1.0 + (stats.getMinAttack() + stats.getMaxAttack()) / 2.0;
    }

    public double getTotalDefense(Player player) {
        PlayerStats stats = statsManager.getPlayerStats(player);
        if (stats == null) return 0;
        return (stats.getDefenseMin() + stats.getDefenseMax()) / 2.0;
    }
}
