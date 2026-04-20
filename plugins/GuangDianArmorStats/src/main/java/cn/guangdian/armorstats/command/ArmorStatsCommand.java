package cn.guangdian.armorstats.command;

import cn.guangdian.armorstats.GuangDianArmorStats;
import cn.guangdian.armorstats.data.AttributeValue;
import cn.guangdian.armorstats.data.PlayerStats;
import cn.guangdian.armorstats.manager.StatsManager;
import cn.guangdian.armorstats.parser.LoreParser;
import cn.guangdian.armorstats.skill.SkillIntegration;
import cn.guangdian.rpgcore.message.MiniMessageService;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.Bukkit;
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

/**
 * 装备属性命令处理器
 *
 * RPGCore 服务集成:
 * - MiniMessageService: 使用 RPGCore 统一消息服务进行文本格式化
 */
public class ArmorStatsCommand implements CommandExecutor {

    private final StatsManager statsManager;
    private final SkillIntegration skillIntegration;  // 通过 RPGSkill 执行技能（解耦）
    private final GuangDianArmorStats plugin;
    private final MiniMessage miniMessage;

    public ArmorStatsCommand(StatsManager statsManager, SkillIntegration skillIntegration, GuangDianArmorStats plugin) {
        this.statsManager = statsManager;
        this.skillIntegration = skillIntegration;
        this.plugin = plugin;
        this.miniMessage = plugin.getMiniMessage().getMiniMessage();
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
                        sender.sendMessage(Component.text("玩家不存在或离线!").color(NamedTextColor.RED));
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
                        sender.sendMessage(Component.text("玩家不存在!").color(NamedTextColor.RED));
                    }
                } else {
                    sender.sendMessage(Component.text("用法: /armorstats view <玩家>").color(NamedTextColor.RED));
                }
            }
        } else if (args[0].equalsIgnoreCase("reload")) {
            if (sender.hasPermission("armorstats.admin")) {
                plugin.reloadAllConfigs();
                // 使用 RPGCore MiniMessageService
                String reloadMessage = plugin.getConfig().getString("messages.stats_reloaded", "<green>Config reloaded!");
                sender.sendMessage(plugin.getMiniMessage().colorize(reloadMessage));
                sender.sendMessage(Component.text("在线玩家同步数: " + Bukkit.getOnlinePlayers().size()).color(NamedTextColor.GREEN));
            } else {
                sender.sendMessage(Component.text("没有权限!").color(NamedTextColor.RED));
            }
        } else if (args[0].equalsIgnoreCase("refresh")) {
            if (sender instanceof Player) {
                Player player = (Player) sender;
                statsManager.refreshPlayerStats(player);
                sender.sendMessage(Component.text("属性已刷新!").color(NamedTextColor.GREEN));
            }
        } else if (args[0].equalsIgnoreCase("reset")) {
            if (args.length >= 2 && sender.hasPermission("armorstats.admin")) {
                Player target = plugin.getServer().getPlayer(args[1]);
                if (target != null) {
                    statsManager.resetPlayer(target);
                    sender.sendMessage(Component.text("已重置 " + target.getName() + " 的RPG属性").color(NamedTextColor.GREEN));
                    target.sendMessage(Component.text("你的RPG属性已被管理员重置。").color(NamedTextColor.YELLOW));
                } else {
                    sender.sendMessage(Component.text("玩家不存在或离线!").color(NamedTextColor.RED));
                }
            } else if (sender instanceof Player) {
                Player player = (Player) sender;
                statsManager.resetPlayer(player);
                sender.sendMessage(Component.text("你的RPG属性已重置。").color(NamedTextColor.GREEN));
            } else {
                sender.sendMessage(Component.text("用法: /armorstats reset <玩家>").color(NamedTextColor.RED));
            }
        } else if (args[0].equalsIgnoreCase("clearall")) {
            if (!sender.hasPermission("armorstats.admin")) {
            sender.sendMessage(Component.text("没有权限!").color(NamedTextColor.RED));
            return true;
        }
        if (args.length >= 2) {
            Player target = plugin.getServer().getPlayer(args[1]);
            if (target != null) {
                forceClearAllAttributes(target);
                sender.sendMessage(Component.text("已强制清除 " + target.getName() + " 的所有属性").color(NamedTextColor.GREEN));
                target.sendMessage(Component.text("你的属性已被管理员强制清除。").color(NamedTextColor.YELLOW));
            } else {
                sender.sendMessage(Component.text("玩家不存在或离线!").color(NamedTextColor.RED));
            }
        } else {
            sender.sendMessage(Component.text("用法: /armorstats clearall <玩家>").color(NamedTextColor.RED));
        }
        } else if (args[0].equalsIgnoreCase("debug")) {
            if (sender instanceof Player) {
                Player player = (Player) sender;
                showDebugInfo(sender, player);
            }
        } else if (args[0].equalsIgnoreCase("skill")) {
            if (!(sender instanceof Player)) {
                sender.sendMessage(Component.text("只有玩家可以使用此命令!").color(NamedTextColor.RED));
                return true;
            }
            Player player = (Player) sender;
            if (args.length < 2) {
                showPlayerSkills(player);
                return true;
            }
            String skillName = args[1];
            boolean triggered = skillIntegration.executeSkill(player, skillName);
            if (!triggered) {
                long remaining = skillIntegration.getCooldownRemaining(player, skillName);
                if (remaining > 0) {
                    player.sendMessage(Component.text("技能 " + skillName + " 冷却中 (" + remaining + "秒)!").color(NamedTextColor.RED));
                } else {
                    player.sendMessage(Component.text("技能 " + skillName + " 不存在!").color(NamedTextColor.RED));
                }
            }
        } else if (args[0].equalsIgnoreCase("help")) {
            showHelp(sender);
        } else {
            showHelp(sender);
        }

        return true;
    }

    private void showHelp(CommandSender sender) {
        sender.sendMessage(Component.text("=== 装备属性指令 ===").color(NamedTextColor.GOLD));
        sender.sendMessage(Component.text("/armorstats view [玩家]").color(NamedTextColor.YELLOW)
            .append(Component.text(" - 查看属性").color(NamedTextColor.GRAY)));
        sender.sendMessage(Component.text("/armorstats refresh").color(NamedTextColor.YELLOW)
            .append(Component.text(" - 刷新属性").color(NamedTextColor.GRAY)));
        sender.sendMessage(Component.text("/armorstats reset [玩家]").color(NamedTextColor.YELLOW)
            .append(Component.text(" - 重置RPG属性").color(NamedTextColor.GRAY)));
        sender.sendMessage(Component.text("/armorstats clearall <玩家>").color(NamedTextColor.YELLOW)
            .append(Component.text(" - 强制清除所有属性").color(NamedTextColor.GRAY)));
        sender.sendMessage(Component.text("/armorstats skill").color(NamedTextColor.YELLOW)
            .append(Component.text(" - 查看技能").color(NamedTextColor.GRAY)));
        sender.sendMessage(Component.text("/armorstats reload").color(NamedTextColor.YELLOW)
            .append(Component.text(" - 重载配置").color(NamedTextColor.GRAY)));
        sender.sendMessage(Component.text("/armorstats debug").color(NamedTextColor.YELLOW)
            .append(Component.text(" - 调试信息").color(NamedTextColor.GRAY)));
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
            player.sendMessage(Component.text("你没有主动技能。").color(NamedTextColor.YELLOW));
            return;
        }

        player.sendMessage(Component.text("=== 你的技能 ===").color(NamedTextColor.GOLD));
        for (String skillName : skills) {
            // 通过 RPGSkill 检查技能状态
            boolean hasSkill = skillIntegration.hasSkill(skillName);
            if (hasSkill) {
                long remaining = skillIntegration.getCooldownRemaining(player, skillName);
                Component status = remaining > 0 ?
                    Component.text(" (冷却: " + remaining + "秒)").color(NamedTextColor.RED) :
                    Component.text(" (就绪)").color(NamedTextColor.GREEN);
                player.sendMessage(Component.text(skillName).color(NamedTextColor.YELLOW).append(status));
            } else {
                player.sendMessage(Component.text(skillName).color(NamedTextColor.YELLOW)
                    .append(Component.text(" (未加载)").color(NamedTextColor.GRAY)));
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
        sender.sendMessage(Component.text("调试信息已发送到控制台!").color(NamedTextColor.GOLD));
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

        sender.sendMessage(Component.text("╔══════════════════════════════╗").color(NamedTextColor.GOLD));
        String titleSuffix = " 的角色属性";
        int titleLen = titleSuffix.length();
        int nameLen = target.getName().length();
        int usedLen = Math.min(nameLen + titleLen, 16);
        sender.sendMessage(Component.text("║  ").color(NamedTextColor.GOLD)
            .append(Component.text(target.getName()).color(NamedTextColor.YELLOW))
            .append(Component.text(titleSuffix.substring(0, usedLen - nameLen) + "              ".substring(0, 16 - usedLen) + "║").color(NamedTextColor.GOLD)));
        sender.sendMessage(Component.text("╠══════════════════════════════╣").color(NamedTextColor.GOLD));

        // 基础属性
        sender.sendMessage(Component.text("  【基础属性】").color(NamedTextColor.AQUA));
        sender.sendMessage(Component.text("  最大生命: ").color(NamedTextColor.YELLOW)
            .append(Component.text(String.valueOf((int) (20 + stats.getMaxHealth()))).color(NamedTextColor.WHITE)));
        sender.sendMessage(Component.text("  攻击力: ").color(NamedTextColor.YELLOW)
            .append(Component.text((int) (1 + stats.getMinAttack()) + " - " + (int) (1 + stats.getMaxAttack())).color(NamedTextColor.WHITE)));
        sender.sendMessage(Component.text("  防御力: ").color(NamedTextColor.YELLOW)
            .append(Component.text((int) stats.getDefenseMin() + " - " + (int) stats.getDefenseMax()).color(NamedTextColor.WHITE)));
        sender.sendMessage(Component.text("  PVP攻击: ").color(NamedTextColor.YELLOW)
            .append(Component.text((int) stats.getPvpMinAttack() + " - " + (int) stats.getPvpMaxAttack()).color(NamedTextColor.WHITE)));
        sender.sendMessage(Component.text("  PVP防御: ").color(NamedTextColor.YELLOW)
            .append(Component.text((int) stats.getPvpDefenseMin() + " - " + (int) stats.getPvpDefenseMax()).color(NamedTextColor.WHITE)));

        sender.sendMessage(Component.text("╠══════════════════════════════╣").color(NamedTextColor.GOLD));

        // 攻击属性
        sender.sendMessage(Component.text("  【攻击属性】").color(NamedTextColor.RED));
        sender.sendMessage(Component.text("  暴击几率: ").color(NamedTextColor.YELLOW)
            .append(Component.text(String.format("%.1f%%", stats.getCritChancePercent())).color(NamedTextColor.WHITE)));
        sender.sendMessage(Component.text("  暴击伤害: ").color(NamedTextColor.YELLOW)
            .append(Component.text(String.format("%.1f%%", 150.0 + stats.getCritDamagePercent())).color(NamedTextColor.WHITE)));
        sender.sendMessage(Component.text("  吸血几率: ").color(NamedTextColor.YELLOW)
            .append(Component.text(String.format("%.1f%%", stats.getLifestealPercent())).color(NamedTextColor.WHITE)));
        sender.sendMessage(Component.text("  吸血倍率: ").color(NamedTextColor.YELLOW)
            .append(Component.text(String.format("%.1f%%", stats.getLifestealMultiplier())).color(NamedTextColor.WHITE)));
        sender.sendMessage(Component.text("  中毒几率: ").color(NamedTextColor.YELLOW)
            .append(Component.text(String.format("%.1f%%", stats.getPoisonPercent())).color(NamedTextColor.WHITE)));
        sender.sendMessage(Component.text("  冰冻几率: ").color(NamedTextColor.YELLOW)
            .append(Component.text(String.format("%.1f%%", stats.getFreezePercent())).color(NamedTextColor.WHITE)));
        sender.sendMessage(Component.text("  致盲几率: ").color(NamedTextColor.YELLOW)
            .append(Component.text(String.format("%.1f%%", stats.getBlindPercent())).color(NamedTextColor.WHITE)));
        sender.sendMessage(Component.text("  燃烧几率: ").color(NamedTextColor.YELLOW)
            .append(Component.text(String.format("%.1f%%", stats.getBurnPercent())).color(NamedTextColor.WHITE)));
        sender.sendMessage(Component.text("  灼烧几率: ").color(NamedTextColor.YELLOW)
            .append(Component.text(String.format("%.1f%%", stats.getScorchPercent())).color(NamedTextColor.WHITE)));

        sender.sendMessage(Component.text("╠══════════════════════════════╣").color(NamedTextColor.GOLD));

        // 防御属性
        sender.sendMessage(Component.text("  【防御属性】").color(NamedTextColor.GREEN));
        sender.sendMessage(Component.text("  闪避几率: ").color(NamedTextColor.YELLOW)
            .append(Component.text(String.format("%.1f%%", stats.getDodgePercent())).color(NamedTextColor.WHITE)));
        sender.sendMessage(Component.text("  招架几率: ").color(NamedTextColor.YELLOW)
            .append(Component.text(String.format("%.1f%%", stats.getParryPercent())).color(NamedTextColor.WHITE)));
        sender.sendMessage(Component.text("  暴击抵抗: ").color(NamedTextColor.YELLOW)
            .append(Component.text(String.format("%.1f%%", stats.getCritResistPercent())).color(NamedTextColor.WHITE)));
        sender.sendMessage(Component.text("  暴伤抵抗: ").color(NamedTextColor.YELLOW)
            .append(Component.text(String.format("%.1f%%", stats.getCritDamageResistPercent())).color(NamedTextColor.WHITE)));
        sender.sendMessage(Component.text("  吸血抵抗: ").color(NamedTextColor.YELLOW)
            .append(Component.text(String.format("%.1f%%", stats.getLifestealResistPercent())).color(NamedTextColor.WHITE)));
        sender.sendMessage(Component.text("  伤害反弹: ").color(NamedTextColor.YELLOW)
            .append(Component.text(String.format("%.1f%%", stats.getDamageReflectPercent())).color(NamedTextColor.WHITE)));
        sender.sendMessage(Component.text("  反伤比例: ").color(NamedTextColor.YELLOW)
            .append(Component.text(String.format("%.1f%%", stats.getReflectPercent())).color(NamedTextColor.WHITE)));
        sender.sendMessage(Component.text("  躲避反伤: ").color(NamedTextColor.YELLOW)
            .append(Component.text(String.format("%.1f%%", stats.getDodgeReflectPercent())).color(NamedTextColor.WHITE)));
        sender.sendMessage(Component.text("  躲避反弹: ").color(NamedTextColor.YELLOW)
            .append(Component.text(String.format("%.1f%%", stats.getDodgeReflectRatio())).color(NamedTextColor.WHITE)));
        sender.sendMessage(Component.text("  击退抗性: ").color(NamedTextColor.YELLOW)
            .append(Component.text(String.format("%.1f%%", stats.getKnockbackResistPercent())).color(NamedTextColor.WHITE)));

        sender.sendMessage(Component.text("╠══════════════════════════════╣").color(NamedTextColor.GOLD));

        // 护甲与穿透
        sender.sendMessage(Component.text("  【护甲与穿透】").color(NamedTextColor.LIGHT_PURPLE));
        sender.sendMessage(Component.text("  护甲值: ").color(NamedTextColor.YELLOW)
            .append(Component.text(String.format("%.1f%%", stats.getArmorPercent())).color(NamedTextColor.WHITE)));
        sender.sendMessage(Component.text("  护甲强度: ").color(NamedTextColor.YELLOW)
            .append(Component.text(String.format("%.1f%%", stats.getArmorStrength())).color(NamedTextColor.WHITE)));
        sender.sendMessage(Component.text("  护甲穿透: ").color(NamedTextColor.YELLOW)
            .append(Component.text(String.format("%.1f%%", stats.getArmorPenetration())).color(NamedTextColor.WHITE)));
        sender.sendMessage(Component.text("  防御穿透: ").color(NamedTextColor.YELLOW)
            .append(Component.text(String.format("%.1f%%", stats.getDefensePenetration())).color(NamedTextColor.WHITE)));
        sender.sendMessage(Component.text("  额外减伤: ").color(NamedTextColor.YELLOW)
            .append(Component.text(String.format("%.1f%%", stats.getDamageReductionBonus())).color(NamedTextColor.WHITE)));

        sender.sendMessage(Component.text("╠══════════════════════════════╣").color(NamedTextColor.GOLD));

        // 环境抗性
        sender.sendMessage(Component.text("  【环境抗性】").color(NamedTextColor.BLUE));
        sender.sendMessage(Component.text("  火焰抗性: ").color(NamedTextColor.YELLOW)
            .append(Component.text(String.format("%.1f%%", stats.getFireResistPercent())).color(NamedTextColor.WHITE)));
        sender.sendMessage(Component.text("  摔落抗性: ").color(NamedTextColor.YELLOW)
            .append(Component.text(String.format("%.1f%%", stats.getFallResistPercent())).color(NamedTextColor.WHITE)));
        sender.sendMessage(Component.text("  溺水抗性: ").color(NamedTextColor.YELLOW)
            .append(Component.text(String.format("%.1f%%", stats.getDrowningResistPercent())).color(NamedTextColor.WHITE)));
        sender.sendMessage(Component.text("  中毒抗性: ").color(NamedTextColor.YELLOW)
            .append(Component.text(String.format("%.1f%%", stats.getPoisonResistPercent())).color(NamedTextColor.WHITE)));
        sender.sendMessage(Component.text("  凋零抗性: ").color(NamedTextColor.YELLOW)
            .append(Component.text(String.format("%.1f%%", stats.getWitherResistPercent())).color(NamedTextColor.WHITE)));
        sender.sendMessage(Component.text("  岩浆抗性: ").color(NamedTextColor.YELLOW)
            .append(Component.text(String.format("%.1f%%", stats.getLavaResistPercent())).color(NamedTextColor.WHITE)));
        sender.sendMessage(Component.text("  魔法抗性: ").color(NamedTextColor.YELLOW)
            .append(Component.text(String.format("%.1f%%", stats.getMagicResistPercent())).color(NamedTextColor.WHITE)));
        sender.sendMessage(Component.text("  爆炸抗性: ").color(NamedTextColor.YELLOW)
            .append(Component.text(String.format("%.1f%%", stats.getExplosionResistPercent())).color(NamedTextColor.WHITE)));
        sender.sendMessage(Component.text("  弹射物抗: ").color(NamedTextColor.YELLOW)
            .append(Component.text(String.format("%.1f%%", stats.getProjectileResistPercent())).color(NamedTextColor.WHITE)));

        sender.sendMessage(Component.text("╠══════════════════════════════╣").color(NamedTextColor.GOLD));

        // 回复属性
        sender.sendMessage(Component.text("  【回复属性】").color(NamedTextColor.YELLOW));
        sender.sendMessage(Component.text("  每秒回血: ").color(NamedTextColor.YELLOW)
            .append(Component.text(String.valueOf((int) stats.getHealthRegen())).color(NamedTextColor.WHITE)));
        sender.sendMessage(Component.text("  生命恢复: ").color(NamedTextColor.YELLOW)
            .append(Component.text(String.format("%.1f%%", stats.getHealthRegenPercent())).color(NamedTextColor.WHITE)));

        sender.sendMessage(Component.text("╠══════════════════════════════╣").color(NamedTextColor.GOLD));

        // 其他属性
        sender.sendMessage(Component.text("  【其他属性】").color(NamedTextColor.WHITE));
        sender.sendMessage(Component.text("  移动速度: ").color(NamedTextColor.YELLOW)
            .append(Component.text(String.format("%.1f%%", stats.getMoveSpeedPercent())).color(NamedTextColor.WHITE)));
        sender.sendMessage(Component.text("  经验加成: ").color(NamedTextColor.YELLOW)
            .append(Component.text(String.format("%.1f%%", stats.getExpBonusPercent())).color(NamedTextColor.WHITE)));

        List<String> skills = statsManager.getPlayerSkills(target);
        if (!skills.isEmpty()) {
            sender.sendMessage(Component.text("╠══════════════════════════════╣").color(NamedTextColor.GOLD));
            sender.sendMessage(Component.text("  【技能列表】").color(NamedTextColor.GOLD));
            for (String skillName : skills) {
                // 通过 SkillIntegration 检查技能状态
                boolean hasSkill = skillIntegration.hasSkill(skillName);
                if (hasSkill) {
                    sender.sendMessage(Component.text("  " + skillName).color(NamedTextColor.YELLOW)
                        .append(Component.text(" (已学习)").color(NamedTextColor.GREEN)));
                } else {
                    sender.sendMessage(Component.text("  " + skillName).color(NamedTextColor.YELLOW)
                        .append(Component.text(" (未加载)").color(NamedTextColor.GRAY)));
                }
            }
        }

        sender.sendMessage(Component.text("╚══════════════════════════════╝").color(NamedTextColor.GOLD));
    }
}
