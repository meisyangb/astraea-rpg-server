package cn.guangdian.armorstats.manager;

import cn.guangdian.armorstats.GuangDianArmorStats;
import cn.guangdian.rpgcore.message.MiniMessageService;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.Bukkit;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Villager;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.Plugin;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 战斗日志管理器
 *
 * RPGCore 服务集成:
 * - MiniMessageService: 使用 RPGCore 统一消息服务进行文本格式化
 */
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
    private final MiniMessage miniMessage;

    public CombatLogManager(GuangDianArmorStats plugin) {
        this.plugin = plugin;
        this.miniMessage = plugin.getMiniMessage().getMiniMessage();
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

        formatDamage = plugin.getConfig().getString("combat_log.format.damage", "<red>[战斗] <yellow>%attacker% <white>对 <yellow>%target% <white>造成了 <red>%damage% <white>伤害");
        formatCrit = plugin.getConfig().getString("combat_log.format.crit", "<red>[战斗] <yellow>%attacker% <white>对 <yellow>%target% <white>造成了 <dark_red><bold>%damage% <red><bold>暴击<white>伤害!");
        formatPVPDamage = plugin.getConfig().getString("combat_log.format.pvp_damage", "<red>[PVP] <yellow>%attacker% <white>对 <yellow>%target% <white>造成了 <red>%damage% <white>伤害");
        formatPVPCrit = plugin.getConfig().getString("combat_log.format.pvp_crit", "<red>[PVP] <yellow>%attacker% <white>对 <yellow>%target% <white>造成了 <dark_red><bold>%damage% <red><bold>暴击<white>伤害!");
        formatDodge = plugin.getConfig().getString("combat_log.format.dodge", "<aqua>[闪避] <yellow>%defender% <white>闪避了攻击!");
        formatParry = plugin.getConfig().getString("combat_log.format.parry", "<yellow>[招架] <yellow>%defender% <white>招架了攻击!");
        formatReflect = plugin.getConfig().getString("combat_log.format.reflect", "<red>[反弹] <yellow>%defender% <white>反弹了 <red>%damage% <white>伤害!");
        formatLifesteal = plugin.getConfig().getString("combat_log.format.lifesteal", "<red>[吸血] <yellow>%player% <white>恢复了 <red>%amount% <white>生命值!");
        formatPoison = plugin.getConfig().getString("combat_log.format.poison", "<green>[中毒] <yellow>%attacker% <white>使 <yellow>%target% <white>中毒!");
        formatFreeze = plugin.getConfig().getString("combat_log.format.freeze", "<aqua>[冰冻] <yellow>%attacker% <white>冻结了 <yellow>%target%<white>!");
        formatBlind = plugin.getConfig().getString("combat_log.format.blind", "<dark_gray>[致盲] <yellow>%attacker% <white>致盲了 <yellow>%target%<white>!");
        formatSkillDamage = plugin.getConfig().getString("combat_log.format.skill_damage", "<light_purple>[技能] <yellow>%skill% <white>对 <yellow>%target% <white>造成了 <red>%damage% <white>伤害!");
        formatSkillHeal = plugin.getConfig().getString("combat_log.format.skill_heal", "<green>[治疗] <yellow>%skill% <white>恢复了 <red>%amount% <white>生命值!");
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
        // 使用 RPGCore MiniMessageService 进行颜色格式化
        // 配置中已使用 MiniMessage 格式，直接解析
        player.sendMessage(miniMessage.deserialize(message));
    }

    private String getEntityName(LivingEntity entity) {
        if (entity instanceof Player) {
            return ((Player) entity).getName();
        }

        if (entity.getCustomName() != null) {
            // 将旧版颜色代码转换为MiniMessage格式
            return convertLegacyColorsToMiniMessage(entity.getCustomName());
        }

        String name = entity.getType().name();
        StringBuilder result = new StringBuilder();
        for (String word : name.split("_")) {
            if (result.length() > 0) result.append(" ");
            result.append(word.substring(0, 1).toUpperCase()).append(word.substring(1).toLowerCase());
        }
        return result.toString();
    }

    /**
     * 将旧版颜色代码 (&x 和 §x) 转换为MiniMessage格式
     */
    private String convertLegacyColorsToMiniMessage(String text) {
        if (text == null) return "";
        return cn.guangdian.rpgcore.util.TextStripper.legacyToMiniMessage(text);
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
