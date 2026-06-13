package cn.guangdian.killaura.command;

import cn.guangdian.killaura.GuangDianKillAura;
import cn.guangdian.killaura.config.KillAuraConfig;
import cn.guangdian.killaura.manager.AttackManager;
import cn.guangdian.killaura.model.KillAuraProfile;
import cn.guangdian.killaura.model.TargetStrategy;
import net.kyori.adventure.text.Component;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabExecutor;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class KillAuraCommand implements CommandExecutor, TabExecutor {

    private final GuangDianKillAura plugin;
    private final AttackManager attackManager;

    public KillAuraCommand(GuangDianKillAura plugin, AttackManager attackManager) {
        this.plugin = plugin;
        this.attackManager = attackManager;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length == 0) {
            if (sender instanceof Player player) {
                sendPlayerStatus(player);
            } else {
                sendHelp(sender);
            }
            return true;
        }

        return switch (args[0].toLowerCase()) {
            case "toggle", "on", "off" -> handleToggle(sender, args);
            case "status", "info" -> handleStatus(sender);
            case "strategy", "mode" -> handleStrategy(sender, args);
            case "range" -> handleRange(sender, args);
            case "stats" -> handleStats(sender);
            case "reload" -> handleReload(sender);
            case "help" -> { sendHelp(sender); yield true; }
            default -> { sendHelp(sender); yield true; }
        };
    }

    private boolean handleToggle(CommandSender sender, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(miniMessage().red("该命令只能由玩家执行"));
            return true;
        }

        if (!player.hasPermission("killaura.use")) {
            player.sendMessage(miniMessage().red("你没有使用杀戮模式的权限"));
            return true;
        }

        if (args.length >= 2) {
            boolean enabled = args[1].equalsIgnoreCase("on") || args[1].equalsIgnoreCase("true");
            attackManager.setKillAuraEnabled(player.getUniqueId(), enabled);
            player.sendMessage(enabled
                ? miniMessage().green("杀戮模式已开启")
                : miniMessage().red("杀戮模式已关闭"));
        } else {
            boolean newState = attackManager.toggleKillAura(player.getUniqueId());
            player.sendMessage(newState
                ? miniMessage().green("杀戮模式已开启")
                : miniMessage().red("杀戮模式已关闭"));
        }
        return true;
    }

    private boolean handleStatus(CommandSender sender) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(miniMessage().red("该命令只能由玩家执行"));
            return true;
        }

        sendPlayerStatus(player);
        return true;
    }

    private void sendPlayerStatus(Player player) {
        KillAuraProfile profile = attackManager.getProfile(player.getUniqueId());
        if (profile == null) {
            player.sendMessage(miniMessage().parse("<yellow>你尚未使用过杀戮模式"));
            return;
        }

        String statusStr = profile.isEnabled() ? "<green>开启</green>" : "<red>关闭</red>";
        LivingEntity target = attackManager.getCurrentTarget(player.getUniqueId());
        String targetStr = target != null ? target.getName() : "无";

        player.sendMessage(miniMessage().parse(
            "<gold>═══════ 杀戮模式 ═══════</reset>\n" +
            "<yellow>状态: </yellow>" + statusStr + "\n" +
            "<yellow>策略: </yellow><white>" + profile.getStrategy().getDisplayName() + "</white>\n" +
            "<yellow>范围: </yellow><white>" + String.format("%.1f", profile.getAttackRange()) + " 格</white>\n" +
            "<yellow>当前目标: </yellow><white>" + targetStr + "</white>\n" +
            "<yellow>击杀数: </yellow><white>" + profile.getKillCount() + "</white>\n" +
            "<yellow>总伤害: </yellow><white>" + String.format("%.1f", profile.getTotalDamage()) + "</white>\n" +
            "<gold>═══════════════════════</reset>"
        ));
    }

    private boolean handleStrategy(CommandSender sender, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(miniMessage().red("该命令只能由玩家执行"));
            return true;
        }

        if (!player.hasPermission("killaura.use")) {
            player.sendMessage(miniMessage().red("你没有权限"));
            return true;
        }

        if (args.length < 2) {
            player.sendMessage(miniMessage().parse("<yellow>可用策略: <white>nearest</white>(最近), <white>lowest_health</white>(最低血量), <white>highest_aggro</white>(最高仇恨)"));
            return true;
        }

        TargetStrategy strategy = TargetStrategy.fromKey(args[1]);
        attackManager.setTargetStrategy(player.getUniqueId(), strategy);
        player.sendMessage(miniMessage().parse("<green>目标策略已切换为: <white>" + strategy.getDisplayName() + "</white></green>"));
        return true;
    }

    private boolean handleRange(CommandSender sender, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(miniMessage().red("该命令只能由玩家执行"));
            return true;
        }

        if (!player.hasPermission("killaura.use")) {
            player.sendMessage(miniMessage().red("你没有权限"));
            return true;
        }

        if (args.length < 2) {
            double currentRange = attackManager.getAttackRange(player.getUniqueId());
            player.sendMessage(miniMessage().parse("<yellow>当前攻击范围: <white>" + String.format("%.1f", currentRange) + "</white> 格"));
            return true;
        }

        try {
            double range = Double.parseDouble(args[1]);
            double maxRange = plugin.getKillAuraConfig().getMaxAttackRange();
            if (range < 1.0 || range > maxRange) {
                player.sendMessage(miniMessage().parse("<red>攻击范围必须在 1.0 ~ " + maxRange + " 之间</red>"));
                return true;
            }
            attackManager.setAttackRange(player.getUniqueId(), range);
            player.sendMessage(miniMessage().parse("<green>攻击范围已设置为: <white>" + String.format("%.1f", range) + "</white> 格</green>"));
        } catch (NumberFormatException e) {
            player.sendMessage(miniMessage().red("请输入有效的数字"));
        }
        return true;
    }

    private boolean handleStats(CommandSender sender) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(miniMessage().red("该命令只能由玩家执行"));
            return true;
        }

        KillAuraProfile profile = attackManager.getProfile(player.getUniqueId());
        if (profile == null) {
            player.sendMessage(miniMessage().parse("<yellow>暂无统计数据"));
            return true;
        }

        player.sendMessage(miniMessage().parse(
            "<gold>═══════ 杀戮统计 ═══════</reset>\n" +
            "<yellow>击杀数: </yellow><white>" + profile.getKillCount() + "</white>\n" +
            "<yellow>总伤害: </yellow><white>" + String.format("%.1f", profile.getTotalDamage()) + "</white>\n" +
            "<gold>═══════════════════════</reset>"
        ));
        return true;
    }

    private boolean handleReload(CommandSender sender) {
        if (!sender.hasPermission("killaura.admin")) {
            sender.sendMessage(miniMessage().red("你没有管理员权限"));
            return true;
        }

        plugin.getKillAuraConfig().load();
        sender.sendMessage(miniMessage().green("配置已重载"));
        return true;
    }

    private void sendHelp(CommandSender sender) {
        sender.sendMessage(miniMessage().parse(
            "<gold>═══════ 杀戮模式帮助 ═══════</reset>\n" +
            "<yellow>/killaura toggle [on|off]</yellow> <gray>- 开关杀戮模式</gray>\n" +
            "<yellow>/killaura status</yellow> <gray>- 查看当前状态</gray>\n" +
            "<yellow>/killaura strategy <策略></yellow> <gray>- 切换目标策略</gray>\n" +
            "<yellow>/killaura range [范围]</yellow> <gray>- 设置/查看攻击范围</gray>\n" +
            "<yellow>/killaura stats</yellow> <gray>- 查看杀戮统计</gray>\n" +
            "<yellow>/killaura reload</yellow> <gray>- 重载配置(管理员)</gray>\n" +
            "<gold>═══════════════════════════</reset>"
        ));
    }

    private cn.guangdian.rpgcore.message.MiniMessageService miniMessage() {
        return plugin.getMiniMessageService();
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        List<String> completions = new ArrayList<>();

        if (args.length == 1) {
            List<String> subs = new ArrayList<>(Arrays.asList("toggle", "status", "strategy", "range", "stats", "help"));
            if (sender.hasPermission("killaura.admin")) {
                subs.add("reload");
            }
            for (String sub : subs) {
                if (sub.startsWith(args[0].toLowerCase())) {
                    completions.add(sub);
                }
            }
        } else if (args.length == 2) {
            switch (args[0].toLowerCase()) {
                case "toggle" -> {
                    for (String opt : Arrays.asList("on", "off")) {
                        if (opt.startsWith(args[1].toLowerCase())) {
                            completions.add(opt);
                        }
                    }
                }
                case "strategy" -> {
                    for (TargetStrategy s : TargetStrategy.values()) {
                        if (s.getKey().startsWith(args[1].toLowerCase())) {
                            completions.add(s.getKey());
                        }
                    }
                }
            }
        }

        return completions;
    }
}
