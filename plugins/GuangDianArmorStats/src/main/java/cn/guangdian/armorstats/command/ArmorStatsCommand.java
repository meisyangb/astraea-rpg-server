package cn.guangdian.armorstats.command;

import cn.guangdian.armorstats.GuangDianArmorStats;
import cn.guangdian.armorstats.data.AttributeValue;
import cn.guangdian.armorstats.data.PlayerStats;
import cn.guangdian.armorstats.manager.StatsManager;
import cn.guangdian.armorstats.parser.LoreParser;
import cn.guangdian.armorstats.skill.Skill;
import cn.guangdian.armorstats.skill.SkillManager;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.OfflinePlayer;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.List;
import java.util.Map;
import java.util.UUID;

public class ArmorStatsCommand implements CommandExecutor {

    private final StatsManager statsManager;
    private final SkillManager skillManager;
    private final GuangDianArmorStats plugin;

    public ArmorStatsCommand(StatsManager statsManager, SkillManager skillManager, GuangDianArmorStats plugin) {
        this.statsManager = statsManager;
        this.skillManager = skillManager;
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length == 0 || args[0].equalsIgnoreCase("view")) {
            if (sender instanceof Player) {
                Player player = (Player) sender;
                if (args.length >= 2 && sender.hasPermission("armorstats.admin")) {
                    Player target = plugin.getServer().getPlayer(args[1]);
                    if (target != null) {
                        showPlayerStats(sender, target, true);
                    } else {
                        sender.sendMessage(ChatColor.RED + "玩家不存在或离线!");
                    }
                } else {
                    showPlayerStats(sender, player, true);
                }
            } else {
                if (args.length >= 2) {
                    Player target = plugin.getServer().getPlayer(args[1]);
                    if (target != null) {
                        showPlayerStats(sender, target, true);
                    } else {
                        sender.sendMessage(ChatColor.RED + "玩家不存在!");
                    }
                } else {
                    sender.sendMessage(ChatColor.RED + "用法: /armorstats view <玩家>");
                }
            }
        } else if (args[0].equalsIgnoreCase("reload")) {
            if (sender.hasPermission("armorstats.admin")) {
                plugin.reloadAllConfigs();
                sender.sendMessage(ChatColor.translateAlternateColorCodes('&',
                    plugin.getConfig().getString("messages.stats_reloaded", "&aConfig reloaded!")));
                sender.sendMessage(ChatColor.GREEN + "在线玩家同步数: " + Bukkit.getOnlinePlayers().size());
            } else {
                sender.sendMessage(ChatColor.RED + "没有权限!");
            }
        } else if (args[0].equalsIgnoreCase("refresh")) {
            if (sender instanceof Player) {
                Player player = (Player) sender;
                statsManager.refreshPlayerStats(player);
                sender.sendMessage(ChatColor.GREEN + "属性已刷新!");
            }
        } else if (args[0].equalsIgnoreCase("reset")) {
            if (args.length >= 2 && sender.hasPermission("armorstats.admin")) {
                Player target = plugin.getServer().getPlayer(args[1]);
                if (target != null) {
                    statsManager.resetPlayer(target);
                    sender.sendMessage(ChatColor.GREEN + "已重置 " + target.getName() + " 的RPG属性");
                    target.sendMessage(ChatColor.YELLOW + "你的RPG属性已被管理员重置。");
                } else {
                    sender.sendMessage(ChatColor.RED + "玩家不存在或离线!");
                }
            } else if (sender instanceof Player) {
                Player player = (Player) sender;
                statsManager.resetPlayer(player);
                sender.sendMessage(ChatColor.GREEN + "你的RPG属性已重置。");
            } else {
                sender.sendMessage(ChatColor.RED + "用法: /armorstats reset <玩家>");
            }
        } else if (args[0].equalsIgnoreCase("clearall")) {
            if (!sender.hasPermission("armorstats.admin")) {
                sender.sendMessage(ChatColor.RED + "没有权限!");
                return true;
            }
            if (args.length >= 2) {
                Player target = plugin.getServer().getPlayer(args[1]);
                if (target != null) {
                    forceClearAllAttributes(target);
                    sender.sendMessage(ChatColor.GREEN + "已强制清除 " + target.getName() + " 的所有属性");
                    target.sendMessage(ChatColor.YELLOW + "你的属性已被管理员强制清除。");
                } else {
                    sender.sendMessage(ChatColor.RED + "玩家不存在或离线!");
                }
            } else {
                sender.sendMessage(ChatColor.RED + "用法: /armorstats clearall <玩家>");
            }
        } else if (args[0].equalsIgnoreCase("debug")) {
            if (sender instanceof Player) {
                Player player = (Player) sender;
                showDebugInfo(sender, player);
            }
        } else if (args[0].equalsIgnoreCase("skill")) {
            if (!(sender instanceof Player)) {
                sender.sendMessage(ChatColor.RED + "只有玩家可以使用此命令!");
                return true;
            }
            Player player = (Player) sender;
            if (args.length < 2) {
                showPlayerSkills(player);
                return true;
            }
            String skillName = args[1];
            boolean triggered = skillManager.triggerActiveSkill(player, skillName);
            if (!triggered) {
                player.sendMessage(ChatColor.RED + "技能 " + skillName + " 不存在或冷却中!");
            }
        } else if (args[0].equalsIgnoreCase("help")) {
            showHelp(sender);
        } else {
            showHelp(sender);
        }

        return true;
    }

    private void showHelp(CommandSender sender) {
        sender.sendMessage(ChatColor.GOLD + "=== 装备属性指令 ===");
        sender.sendMessage(ChatColor.YELLOW + "/armorstats view [玩家]" + ChatColor.GRAY + " - 查看属性");
        sender.sendMessage(ChatColor.YELLOW + "/armorstats refresh" + ChatColor.GRAY + " - 刷新属性");
        sender.sendMessage(ChatColor.YELLOW + "/armorstats reset [玩家]" + ChatColor.GRAY + " - 重置RPG属性");
        sender.sendMessage(ChatColor.YELLOW + "/armorstats clearall <玩家>" + ChatColor.GRAY + " - 强制清除所有属性");
        sender.sendMessage(ChatColor.YELLOW + "/armorstats skill" + ChatColor.GRAY + " - 查看技能");
        sender.sendMessage(ChatColor.YELLOW + "/armorstats reload" + ChatColor.GRAY + " - 重载配置");
        sender.sendMessage(ChatColor.YELLOW + "/armorstats debug" + ChatColor.GRAY + " - 调试信息");
    }

    private void forceClearAllAttributes(Player player) {
        plugin.getLogger().info("Force clearing ALL attributes for " + player.getName());
        
        AttributeInstance healthAttr = player.getAttribute(Attribute.MAX_HEALTH);
        if (healthAttr != null) {
            healthAttr.getModifiers().forEach(modifier -> {
                healthAttr.removeModifier(modifier);
                plugin.getLogger().info("Removed modifier: " + modifier.getName());
            });
            healthAttr.setBaseValue(20.0);
            plugin.getLogger().info("Set health base value to 20");
            
            if (player.getHealth() > 20.0) {
                player.setHealth(20.0);
            }
        }

        AttributeInstance speedAttr = player.getAttribute(Attribute.MOVEMENT_SPEED);
        if (speedAttr != null) {
            speedAttr.getModifiers().forEach(modifier -> {
                speedAttr.removeModifier(modifier);
                plugin.getLogger().info("Removed speed modifier: " + modifier.getName());
            });
            speedAttr.setBaseValue(0.1);
            plugin.getLogger().info("Set speed base value to 0.1");
        }

        AttributeInstance attackAttr = player.getAttribute(Attribute.ATTACK_DAMAGE);
        if (attackAttr != null) {
            attackAttr.getModifiers().forEach(modifier -> {
                attackAttr.removeModifier(modifier);
                plugin.getLogger().info("Removed attack modifier: " + modifier.getName());
            });
            attackAttr.setBaseValue(1.0);
        }

        AttributeInstance armorAttr = player.getAttribute(Attribute.ARMOR);
        if (armorAttr != null) {
            armorAttr.getModifiers().forEach(modifier -> {
                armorAttr.removeModifier(modifier);
                plugin.getLogger().info("Removed armor modifier: " + modifier.getName());
            });
            armorAttr.setBaseValue(0.0);
        }

        statsManager.removePlayer(player.getUniqueId());
        plugin.getLogger().info("Force clear completed for " + player.getName());
    }

    private void showPlayerSkills(Player player) {
        List<String> skills = statsManager.getPlayerSkills(player);
        if (skills.isEmpty()) {
            player.sendMessage(ChatColor.YELLOW + "你没有主动技能。");
            return;
        }

        player.sendMessage(ChatColor.GOLD + "=== 你的技能 ===");
        for (String skillName : skills) {
            Skill skill = skillManager.getSkill(skillName);
            if (skill != null && skill.isActive()) {
                long remaining = skillManager.getCooldownRemaining(player.getUniqueId(), skillName);
                String status = remaining > 0 ?
                    ChatColor.RED + " (冷却: " + remaining + "秒)" :
                    ChatColor.GREEN + " (就绪)";
                player.sendMessage(ChatColor.YELLOW + skillName + status);
            }
        }
    }

    private void showDebugInfo(CommandSender sender, Player player) {
        StringBuilder sb = new StringBuilder();
        sb.append("=== Debug Info for " + player.getName() + " ===\n");

        sb.append("Config patterns loaded: " + cn.guangdian.armorstats.parser.LoreParser.getPatternCount() + "\n");

        AttributeInstance healthAttr = player.getAttribute(Attribute.MAX_HEALTH);
        if (healthAttr != null) {
            sb.append("Health Attribute:\n");
            sb.append("  Base Value: ").append(healthAttr.getBaseValue()).append("\n");
            sb.append("  Current Value: ").append(healthAttr.getValue()).append("\n");
            sb.append("  Modifiers: ").append(healthAttr.getModifiers().size()).append("\n");
            for (var mod : healthAttr.getModifiers()) {
                sb.append("    - ").append(mod.getName()).append(": ").append(mod.getAmount()).append("\n");
            }
        }

        AttributeInstance speedAttr = player.getAttribute(Attribute.MOVEMENT_SPEED);
        if (speedAttr != null) {
            sb.append("Speed Attribute:\n");
            sb.append("  Base Value: ").append(speedAttr.getBaseValue()).append("\n");
            sb.append("  Current Value: ").append(speedAttr.getValue()).append("\n");
            sb.append("  Modifiers: ").append(speedAttr.getModifiers().size()).append("\n");
            for (var mod : speedAttr.getModifiers()) {
                sb.append("    - ").append(mod.getName()).append(": ").append(mod.getAmount()).append("\n");
            }
        }

        for (ItemStack item : player.getInventory().getArmorContents()) {
            if (item != null && item.getType() != org.bukkit.Material.AIR) {
                sb.append("Item: ").append(item.getType()).append("\n");
                if (item.hasItemMeta() && item.getItemMeta().hasLore()) {
                    sb.append("  Raw Lore:\n");
                    for (String lore : item.getItemMeta().getLore()) {
                        sb.append("    '").append(lore).append("'\n");
                        String stripped = cn.guangdian.armorstats.parser.LoreParser.stripColorStatic(lore);
                        sb.append("    Stripped: '").append(stripped).append("'\n");
                    }
                }
                Map<String, AttributeValue> attrs = LoreParser.parse(item);
                if (attrs.isEmpty()) {
                    sb.append("  No attributes parsed!\n");
                } else {
                    for (Map.Entry<String, AttributeValue> entry : attrs.entrySet()) {
                        sb.append("  ").append(entry.getKey()).append(": ").append(formatAttrValue(entry.getValue())).append("\n");
                    }
                }
            }
        }

        ItemStack mainHand = player.getInventory().getItemInMainHand();
        if (mainHand != null && mainHand.getType() != org.bukkit.Material.AIR) {
            sb.append("MainHand: ").append(mainHand.getType()).append("\n");
            if (mainHand.hasItemMeta() && mainHand.getItemMeta().hasLore()) {
                sb.append("  Raw Lore:\n");
                for (String lore : mainHand.getItemMeta().getLore()) {
                    sb.append("    '").append(lore).append("'\n");
                }
            }
            Map<String, AttributeValue> attrs = LoreParser.parse(mainHand);
            if (attrs.isEmpty()) {
                sb.append("  No attributes parsed!\n");
            } else {
                for (Map.Entry<String, AttributeValue> entry : attrs.entrySet()) {
                    sb.append("  ").append(entry.getKey()).append(": ").append(formatAttrValue(entry.getValue())).append("\n");
                }
            }
        }

        String result = sb.toString();
        plugin.getLogger().info(result);
        sender.sendMessage(ChatColor.GOLD + "调试信息已发送到控制台!");
    }

    private String formatAttrValue(AttributeValue value) {
        if (value instanceof AttributeValue.RangeValue) {
            AttributeValue.RangeValue range = (AttributeValue.RangeValue) value;
            return (int) range.getMin() + "-" + (int) range.getMax();
        } else {
            return String.valueOf((int) value.getValue());
        }
    }

    private void showPlayerStats(CommandSender sender, Player target, boolean refresh) {
        if (refresh) {
            statsManager.refreshPlayerStats(target);
        }

        PlayerStats stats = statsManager.getPlayerStats(target);

        sender.sendMessage(ChatColor.GOLD + "╔══════════════════════════════╗");
        String titleSuffix = " 的角色属性";
        int titleLen = titleSuffix.length();
        int nameLen = target.getName().length();
        int usedLen = Math.min(nameLen + titleLen, 16);
        sender.sendMessage(ChatColor.GOLD + "║  " + ChatColor.YELLOW + target.getName() + ChatColor.GOLD + titleSuffix.substring(0, usedLen - nameLen) + "              ".substring(0, 16 - usedLen) + "║");
        sender.sendMessage(ChatColor.GOLD + "╠══════════════════════════════╣");

        // 基础属性
        sender.sendMessage(ChatColor.AQUA + "  【基础属性】");
        sender.sendMessage(ChatColor.YELLOW + "  最大生命: " + ChatColor.WHITE + (int) (20 + stats.getMaxHealth()));
        sender.sendMessage(ChatColor.YELLOW + "  攻击力: " + ChatColor.WHITE + (int) (1 + stats.getMinAttack()) + " - " + (int) (1 + stats.getMaxAttack()));
        sender.sendMessage(ChatColor.YELLOW + "  防御力: " + ChatColor.WHITE + (int) stats.getDefenseMin() + " - " + (int) stats.getDefenseMax());
        sender.sendMessage(ChatColor.YELLOW + "  PVP攻击: " + ChatColor.WHITE + (int) stats.getPvpMinAttack() + " - " + (int) stats.getPvpMaxAttack());
        sender.sendMessage(ChatColor.YELLOW + "  PVP防御: " + ChatColor.WHITE + (int) stats.getPvpDefenseMin() + " - " + (int) stats.getPvpDefenseMax());

        sender.sendMessage(ChatColor.GOLD + "╠══════════════════════════════╣");

        // 攻击属性
        sender.sendMessage(ChatColor.RED + "  【攻击属性】");
        sender.sendMessage(ChatColor.YELLOW + "  暴击几率: " + ChatColor.WHITE + String.format("%.1f%%", stats.getCritChancePercent()));
        sender.sendMessage(ChatColor.YELLOW + "  暴击伤害: " + ChatColor.WHITE + String.format("%.1f%%", 150.0 + stats.getCritDamagePercent()));
        sender.sendMessage(ChatColor.YELLOW + "  吸血几率: " + ChatColor.WHITE + String.format("%.1f%%", stats.getLifestealPercent()));
        sender.sendMessage(ChatColor.YELLOW + "  吸血倍率: " + ChatColor.WHITE + String.format("%.1f%%", stats.getLifestealMultiplier()));
        sender.sendMessage(ChatColor.YELLOW + "  中毒几率: " + ChatColor.WHITE + String.format("%.1f%%", stats.getPoisonPercent()));
        sender.sendMessage(ChatColor.YELLOW + "  冰冻几率: " + ChatColor.WHITE + String.format("%.1f%%", stats.getFreezePercent()));
        sender.sendMessage(ChatColor.YELLOW + "  致盲几率: " + ChatColor.WHITE + String.format("%.1f%%", stats.getBlindPercent()));
        sender.sendMessage(ChatColor.YELLOW + "  燃烧几率: " + ChatColor.WHITE + String.format("%.1f%%", stats.getBurnPercent()));
        sender.sendMessage(ChatColor.YELLOW + "  灼烧几率: " + ChatColor.WHITE + String.format("%.1f%%", stats.getScorchPercent()));

        sender.sendMessage(ChatColor.GOLD + "╠══════════════════════════════╣");

        // 防御属性
        sender.sendMessage(ChatColor.GREEN + "  【防御属性】");
        sender.sendMessage(ChatColor.YELLOW + "  闪避几率: " + ChatColor.WHITE + String.format("%.1f%%", stats.getDodgePercent()));
        sender.sendMessage(ChatColor.YELLOW + "  招架几率: " + ChatColor.WHITE + String.format("%.1f%%", stats.getParryPercent()));
        sender.sendMessage(ChatColor.YELLOW + "  暴击抵抗: " + ChatColor.WHITE + String.format("%.1f%%", stats.getCritResistPercent()));
        sender.sendMessage(ChatColor.YELLOW + "  暴伤抵抗: " + ChatColor.WHITE + String.format("%.1f%%", stats.getCritDamageResistPercent()));
        sender.sendMessage(ChatColor.YELLOW + "  吸血抵抗: " + ChatColor.WHITE + String.format("%.1f%%", stats.getLifestealResistPercent()));
        sender.sendMessage(ChatColor.YELLOW + "  伤害反弹: " + ChatColor.WHITE + String.format("%.1f%%", stats.getDamageReflectPercent()));
        sender.sendMessage(ChatColor.YELLOW + "  反伤比例: " + ChatColor.WHITE + String.format("%.1f%%", stats.getReflectPercent()));
        sender.sendMessage(ChatColor.YELLOW + "  躲避反伤: " + ChatColor.WHITE + String.format("%.1f%%", stats.getDodgeReflectPercent()));
        sender.sendMessage(ChatColor.YELLOW + "  躲避反弹: " + ChatColor.WHITE + String.format("%.1f%%", stats.getDodgeReflectRatio()));
        sender.sendMessage(ChatColor.YELLOW + "  击退抗性: " + ChatColor.WHITE + String.format("%.1f%%", stats.getKnockbackResistPercent()));

        sender.sendMessage(ChatColor.GOLD + "╠══════════════════════════════╣");

        // 护甲与穿透
        sender.sendMessage(ChatColor.LIGHT_PURPLE + "  【护甲与穿透】");
        sender.sendMessage(ChatColor.YELLOW + "  护甲值: " + ChatColor.WHITE + String.format("%.1f%%", stats.getArmorPercent()));
        sender.sendMessage(ChatColor.YELLOW + "  护甲强度: " + ChatColor.WHITE + String.format("%.1f%%", stats.getArmorStrength()));
        sender.sendMessage(ChatColor.YELLOW + "  护甲穿透: " + ChatColor.WHITE + String.format("%.1f%%", stats.getArmorPenetration()));
        sender.sendMessage(ChatColor.YELLOW + "  防御穿透: " + ChatColor.WHITE + String.format("%.1f%%", stats.getDefensePenetration()));
        sender.sendMessage(ChatColor.YELLOW + "  额外减伤: " + ChatColor.WHITE + String.format("%.1f%%", stats.getDamageReductionBonus()));

        sender.sendMessage(ChatColor.GOLD + "╠══════════════════════════════╣");

        // 环境抗性
        sender.sendMessage(ChatColor.BLUE + "  【环境抗性】");
        sender.sendMessage(ChatColor.YELLOW + "  火焰抗性: " + ChatColor.WHITE + String.format("%.1f%%", stats.getFireResistPercent()));
        sender.sendMessage(ChatColor.YELLOW + "  摔落抗性: " + ChatColor.WHITE + String.format("%.1f%%", stats.getFallResistPercent()));
        sender.sendMessage(ChatColor.YELLOW + "  溺水抗性: " + ChatColor.WHITE + String.format("%.1f%%", stats.getDrowningResistPercent()));
        sender.sendMessage(ChatColor.YELLOW + "  中毒抗性: " + ChatColor.WHITE + String.format("%.1f%%", stats.getPoisonResistPercent()));
        sender.sendMessage(ChatColor.YELLOW + "  凋零抗性: " + ChatColor.WHITE + String.format("%.1f%%", stats.getWitherResistPercent()));
        sender.sendMessage(ChatColor.YELLOW + "  岩浆抗性: " + ChatColor.WHITE + String.format("%.1f%%", stats.getLavaResistPercent()));
        sender.sendMessage(ChatColor.YELLOW + "  魔法抗性: " + ChatColor.WHITE + String.format("%.1f%%", stats.getMagicResistPercent()));
        sender.sendMessage(ChatColor.YELLOW + "  爆炸抗性: " + ChatColor.WHITE + String.format("%.1f%%", stats.getExplosionResistPercent()));
        sender.sendMessage(ChatColor.YELLOW + "  弹射物抗: " + ChatColor.WHITE + String.format("%.1f%%", stats.getProjectileResistPercent()));

        sender.sendMessage(ChatColor.GOLD + "╠══════════════════════════════╣");

        // 回复属性
        sender.sendMessage(ChatColor.YELLOW + "  【回复属性】");
        sender.sendMessage(ChatColor.YELLOW + "  每秒回血: " + ChatColor.WHITE + (int) stats.getHealthRegen());
        sender.sendMessage(ChatColor.YELLOW + "  生命恢复: " + ChatColor.WHITE + String.format("%.1f%%", stats.getHealthRegenPercent()));

        sender.sendMessage(ChatColor.GOLD + "╠══════════════════════════════╣");

        // 其他属性
        sender.sendMessage(ChatColor.WHITE + "  【其他属性】");
        sender.sendMessage(ChatColor.YELLOW + "  移动速度: " + ChatColor.WHITE + String.format("%.1f%%", stats.getMoveSpeedPercent()));
        sender.sendMessage(ChatColor.YELLOW + "  经验加成: " + ChatColor.WHITE + String.format("%.1f%%", stats.getExpBonusPercent()));

        List<String> skills = statsManager.getPlayerSkills(target);
        if (!skills.isEmpty()) {
            sender.sendMessage(ChatColor.GOLD + "╠══════════════════════════════╣");
            sender.sendMessage(ChatColor.GOLD + "  【技能列表】");
            for (String skillName : skills) {
                Skill skill = skillManager.getSkill(skillName);
                if (skill != null) {
                    String type = skill.isPassive() ? "被动" : "主动";
                    sender.sendMessage(ChatColor.YELLOW + "  " + skillName + ChatColor.GRAY + " (" + type + ")");
                }
            }
        }

        sender.sendMessage(ChatColor.GOLD + "╚══════════════════════════════╝");
    }
}
