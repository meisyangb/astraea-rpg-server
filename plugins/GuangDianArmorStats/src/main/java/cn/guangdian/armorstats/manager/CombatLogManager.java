package cn.guangdian.armorstats.manager;

import cn.guangdian.armorstats.GuangDianArmorStats;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Villager;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.Plugin;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class CombatLogManager {

    private final GuangDianArmorStats plugin;
    private boolean enabled;
    private boolean showPVE;
    private boolean showPVP;
    private boolean showCrit;
    private boolean showDodge;
    private boolean showParry;
    private boolean showLifesteal;
    private boolean showStatus;
    private boolean showSkill;
    private long messageCooldown;
    private String formatDamage;
    private String formatCrit;
    private String formatPVPDamage;
    private String formatPVPCrit;
    private String formatDodge;
    private String formatParry;
    private String formatReflect;
    private String formatLifesteal;
    private String formatPoison;
    private String formatFreeze;
    private String formatBlind;
    private String formatSkillDamage;
    private String formatSkillHeal;

    private final Map<UUID, Long> combatCooldowns = new ConcurrentHashMap<>();
    private final Map<UUID, Map<String, Long>> typeCooldowns = new ConcurrentHashMap<>();

    public CombatLogManager(GuangDianArmorStats plugin) {
        this.plugin = plugin;
        loadConfig();
    }

    public void loadConfig() {
        enabled = plugin.getConfig().getBoolean("combat_log.enabled", true);
        showPVE = plugin.getConfig().getBoolean("combat_log.show_pve", true);
        showPVP = plugin.getConfig().getBoolean("combat_log.show_pvp", true);
        showCrit = plugin.getConfig().getBoolean("combat_log.show_crit", true);
        showDodge = plugin.getConfig().getBoolean("combat_log.show_dodge", true);
        showParry = plugin.getConfig().getBoolean("combat_log.show_parry", true);
        showLifesteal = plugin.getConfig().getBoolean("combat_log.show_lifesteal", true);
        showStatus = plugin.getConfig().getBoolean("combat_log.show_status", true);
        showSkill = plugin.getConfig().getBoolean("combat_log.show_skill", true);
        messageCooldown = plugin.getConfig().getLong("combat_log.message_cooldown", 500);

        formatDamage = plugin.getConfig().getString("combat_log.format.damage", "&c[战斗] &e%attacker% &f对 &e%target% &f造成了 &c%damage% &f伤害");
        formatCrit = plugin.getConfig().getString("combat_log.format.crit", "&c[战斗] &e%attacker% &f对 &e%target% &f造成了 &4&l%damage% &c&l暴击&f伤害!");
        formatPVPDamage = plugin.getConfig().getString("combat_log.format.pvp_damage", "&c[PVP] &e%attacker% &f对 &e%target% &f造成了 &c%damage% &f伤害");
        formatPVPCrit = plugin.getConfig().getString("combat_log.format.pvp_crit", "&c[PVP] &e%attacker% &f对 &e%target% &f造成了 &4&l%damage% &c&l暴击&f伤害!");
        formatDodge = plugin.getConfig().getString("combat_log.format.dodge", "&b[闪避] &e%defender% &f闪避了攻击!");
        formatParry = plugin.getConfig().getString("combat_log.format.parry", "&e[招架] &e%defender% &f招架了攻击!");
        formatReflect = plugin.getConfig().getString("combat_log.format.reflect", "&c[反弹] &e%defender% &f反弹了 &c%damage% &f伤害!");
        formatLifesteal = plugin.getConfig().getString("combat_log.format.lifesteal", "&c[吸血] &e%player% &f恢复了 &c%amount% &f生命值!");
        formatPoison = plugin.getConfig().getString("combat_log.format.poison", "&a[中毒] &e%attacker% &f使 &e%target% &f中毒!");
        formatFreeze = plugin.getConfig().getString("combat_log.format.freeze", "&b[冰冻] &e%attacker% &f冻结了 &e%target%&f!");
        formatBlind = plugin.getConfig().getString("combat_log.format.blind", "&8[致盲] &e%attacker% &f致盲了 &e%target%&f!");
        formatSkillDamage = plugin.getConfig().getString("combat_log.format.skill_damage", "&d[技能] &e%skill% &f对 &e%target% &f造成了 &c%damage% &f伤害!");
        formatSkillHeal = plugin.getConfig().getString("combat_log.format.skill_heal", "&a[治疗] &e%skill% &f恢复了 &c%amount% &f生命值!");
    }

    public void logDamage(Player attacker, LivingEntity target, double damage, boolean isCrit, boolean isPVP) {
        if (!enabled) return;
        if (isPVP && !showPVP) return;
        if (!isPVP && !showPVE) return;
        
        if (isGuangDianNPC(target)) {
            return;
        }

        String attackerName = attacker.getName();
        String targetName = getEntityName(target);
        String damageStr = String.format("%.1f", damage);

        String message;
        String logType;
        if (isPVP) {
            if (isCrit && showCrit) {
                message = formatPVPCrit
                        .replace("%attacker%", attackerName)
                        .replace("%target%", targetName)
                        .replace("%damage%", damageStr);
                logType = "pvp_crit";
            } else {
                message = formatPVPDamage
                        .replace("%attacker%", attackerName)
                        .replace("%target%", targetName)
                        .replace("%damage%", damageStr);
                logType = "pvp_damage";
            }
        } else {
            if (isCrit && showCrit) {
                message = formatCrit
                        .replace("%attacker%", attackerName)
                        .replace("%target%", targetName)
                        .replace("%damage%", damageStr);
                logType = "crit";
            } else {
                message = formatDamage
                        .replace("%attacker%", attackerName)
                        .replace("%target%", targetName)
                        .replace("%damage%", damageStr);
                logType = "damage";
            }
        }

        sendMessageWithCooldown(attacker, message, logType);
    }

    public void logMobDamage(LivingEntity attacker, Player target, double damage, boolean isCrit) {
        if (!enabled || !showPVE) return;

        String attackerName = getEntityName(attacker);
        String targetName = target.getName();
        String damageStr = String.format("%.1f", damage);

        String message;
        String logType;
        if (isCrit && showCrit) {
            message = formatCrit
                    .replace("%attacker%", attackerName)
                    .replace("%target%", targetName)
                    .replace("%damage%", damageStr);
            logType = "mob_crit";
        } else {
            message = formatDamage
                    .replace("%attacker%", attackerName)
                    .replace("%target%", targetName)
                    .replace("%damage%", damageStr);
            logType = "mob_damage";
        }

        sendMessageWithCooldown(target, message, logType);
    }

    public void logDodge(Player defender) {
        if (!enabled || !showDodge) return;
        
        String message = formatDodge.replace("%defender%", defender.getName());
        sendMessageWithCooldown(defender, message, "dodge");
    }

    public void logParry(Player defender) {
        if (!enabled || !showParry) return;
        
        String message = formatParry.replace("%defender%", defender.getName());
        sendMessageWithCooldown(defender, message, "parry");
    }

    public void logReflect(Player defender, LivingEntity attacker, double damage) {
        if (!enabled) return;

        String attackerName = getEntityName(attacker);
        String damageStr = String.format("%.1f", damage);
        String message = formatReflect
                .replace("%defender%", defender.getName())
                .replace("%attacker%", attackerName)
                .replace("%damage%", damageStr);
        sendMessageWithCooldown(defender, message, "reflect");
    }

    public void logLifesteal(Player player, double amount) {
        if (!enabled || !showLifesteal) return;

        String amountStr = String.format("%.1f", amount);
        String message = formatLifesteal
                .replace("%player%", player.getName())
                .replace("%amount%", amountStr);
        sendMessageWithCooldown(player, message, "lifesteal");
    }

    public void logStatusEffect(Player attacker, LivingEntity target, String effectType) {
        if (!enabled || !showStatus) return;

        String targetName = getEntityName(target);
        String message;

        switch (effectType.toLowerCase()) {
            case "poison":
                message = formatPoison
                        .replace("%attacker%", attacker.getName())
                        .replace("%target%", targetName);
                break;
            case "freeze":
            case "ice":
                message = formatFreeze
                        .replace("%attacker%", attacker.getName())
                        .replace("%target%", targetName);
                break;
            case "blind":
                message = formatBlind
                        .replace("%attacker%", attacker.getName())
                        .replace("%target%", targetName);
                break;
            default:
                return;
        }

        sendMessageWithCooldown(attacker, message, "status_" + effectType);
    }

    public void logSkillDamage(Player player, String skillName, LivingEntity target, double damage) {
        if (!enabled || !showSkill) return;

        String targetName = getEntityName(target);
        String damageStr = String.format("%.1f", damage);
        String message = formatSkillDamage
                .replace("%skill%", skillName)
                .replace("%target%", targetName)
                .replace("%damage%", damageStr);
        sendMessageWithCooldown(player, message, "skill_damage");
    }

    public void logSkillHeal(Player player, String skillName, double amount) {
        if (!enabled || !showSkill) return;

        String amountStr = String.format("%.1f", amount);
        String message = formatSkillHeal
                .replace("%skill%", skillName)
                .replace("%amount%", amountStr);
        sendMessageWithCooldown(player, message, "skill_heal");
    }

    private void sendMessageWithCooldown(Player player, String message, String logType) {
        long now = System.currentTimeMillis();
        UUID playerId = player.getUniqueId();

        Map<String, Long> playerTypeCooldowns = typeCooldowns.computeIfAbsent(playerId, k -> new ConcurrentHashMap<>());
        Long lastTime = playerTypeCooldowns.get(logType);

        if (lastTime != null && (now - lastTime) < messageCooldown) {
            return;
        }

        playerTypeCooldowns.put(logType, now);
        player.sendMessage(ChatColor.translateAlternateColorCodes('&', message));
    }

    private String getEntityName(LivingEntity entity) {
        if (entity instanceof Player) {
            return ((Player) entity).getName();
        }
        
        if (entity.getCustomName() != null) {
            return entity.getCustomName();
        }
        
        String name = entity.getType().name();
        StringBuilder result = new StringBuilder();
        for (String word : name.split("_")) {
            if (result.length() > 0) result.append(" ");
            result.append(word.substring(0, 1).toUpperCase()).append(word.substring(1).toLowerCase());
        }
        return result.toString();
    }

    public void clearCooldown(UUID playerId) {
        combatCooldowns.remove(playerId);
    }

    public void clearAllCooldowns() {
        combatCooldowns.clear();
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
}
