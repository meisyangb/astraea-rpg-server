package cn.guangdian.rpgskill.command;

import cn.guangdian.rpgcore.message.MiniMessageService;
import cn.guangdian.rpgskill.RPGSkill;
import cn.guangdian.rpgskill.skill.SkillDefinition;
import cn.guangdian.rpgskill.skill.SkillType;
import cn.guangdian.rpgskill.skill.TriggerType;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * RPGSkill 管理命令
 */
public class SkillCommand implements CommandExecutor, TabCompleter {

    private final RPGSkill plugin;
    private final MiniMessageService miniMessage;

    public SkillCommand(RPGSkill plugin) {
        this.plugin = plugin;
        this.miniMessage = MiniMessageService.getInstance();
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length < 1) {
            sendHelp(sender);
            return true;
        }

        String subCommand = args[0].toLowerCase();

        switch (subCommand) {
            case "reload" -> handleReload(sender);
            case "list" -> handleList(sender, args);
            case "info" -> handleInfo(sender, args);
            case "cooldown" -> handleCooldown(sender, args);
            case "clearcooldown" -> handleClearCooldown(sender, args);
            default -> sendHelp(sender);
        }

        return true;
    }

    /**
     * 重载配置
     */
    private void handleReload(CommandSender sender) {
        if (!sender.hasPermission("rpgskill.admin")) {
            sender.sendMessage(miniMessage.red("你没有权限使用此命令"));
            return;
        }

        // 重载配置
        plugin.getConfigManager().reload();
        
        // 重新加载技能注册表
        plugin.getSkillRegistry().clear();
        plugin.getSkillRegistry().loadFromConfig(plugin.getConfigManager().getSkillConfig());

        // 清理冷却缓存
        plugin.getCooldownManager().clearAll();

        sender.sendMessage(miniMessage.green("RPGSkill 配置已重载"));
        sender.sendMessage(miniMessage.colorize("<gray>已加载 <gold>" + plugin.getSkillRegistry().getSkillCount() + " <gray>个技能"));
    }

    /**
     * 列出技能
     */
    private void handleList(CommandSender sender, String[] args) {
        if (!sender.hasPermission("rpgskill.admin")) {
            sender.sendMessage(miniMessage.red("你没有权限使用此命令"));
            return;
        }

        Map<String, SkillDefinition> skills = plugin.getSkillRegistry().getAllSkills();

        // 过滤参数
        String filter = args.length >= 2 ? args[1].toLowerCase() : null;
        SkillType typeFilter = null;
        TriggerType triggerFilter = null;

        if (filter != null) {
            try {
                typeFilter = SkillType.valueOf(filter.toUpperCase());
            } catch (IllegalArgumentException ignored) {
            }
            try {
                triggerFilter = TriggerType.valueOf(filter.toUpperCase());
            } catch (IllegalArgumentException ignored) {
            }
        }

        sender.sendMessage(miniMessage.colorize("<gold><bold>===== 技能列表 ====="));
        sender.sendMessage(miniMessage.colorize("<gray>共 <gold>" + skills.size() + " <gray>个技能"));

        if (filter != null && typeFilter == null && triggerFilter == null) {
            sender.sendMessage(miniMessage.colorize("<yellow>过滤条件: <white>" + filter));
        }

        int index = 0;
        for (Map.Entry<String, SkillDefinition> entry : skills.entrySet()) {
            SkillDefinition skill = entry.getValue();

            // 应用过滤
            if (typeFilter != null && skill.getType() != typeFilter) continue;
            if (triggerFilter != null && skill.getTrigger() != triggerFilter) continue;

            index++;
            String typeColor = skill.getType() == SkillType.ACTIVE ? "<green>" : 
                              skill.getType() == SkillType.PASSIVE ? "<aqua>" : "<yellow>";
            String triggerStr = skill.getTrigger().name();

            sender.sendMessage(miniMessage.colorize(
                String.format("<gray>%d. <gold>%s <gray>- %s%s <gray>[<white>%s<gray>] <gray>CD: <white>%ds",
                    index, entry.getKey(), typeColor, skill.getType().name(), triggerStr, skill.getCooldown())));
        }

        if (index == 0) {
            sender.sendMessage(miniMessage.colorize("<red>没有匹配的技能"));
        }

        sender.sendMessage(miniMessage.colorize(""));
        sender.sendMessage(miniMessage.colorize("<gray>用法: /rpgskill list [active|passive|toggle|right_click|left_click|on_hit]"));
    }

    /**
     * 查看技能详情
     */
    private void handleInfo(CommandSender sender, String[] args) {
        if (!sender.hasPermission("rpgskill.admin")) {
            sender.sendMessage(miniMessage.red("你没有权限使用此命令"));
            return;
        }

        if (args.length < 2) {
            sender.sendMessage(miniMessage.colorize("<red>用法: /rpgskill info <技能ID>"));
            return;
        }

        String skillId = args[1];
        SkillDefinition skill = plugin.getSkillRegistry().getSkill(skillId).orElse(null);

        if (skill == null) {
            sender.sendMessage(miniMessage.red("技能不存在: " + skillId));
            return;
        }

        sender.sendMessage(miniMessage.colorize("<gold><bold>===== 技能详情 ====="));
        sender.sendMessage(miniMessage.colorize("<gray>ID: <gold>" + skillId));
        sender.sendMessage(miniMessage.colorize("<gray>名称: <white>" + skill.getName()));
        sender.sendMessage(miniMessage.colorize("<gray>类型: <green>" + skill.getType().name()));
        sender.sendMessage(miniMessage.colorize("<gray>触发: <aqua>" + skill.getTrigger().name()));
        sender.sendMessage(miniMessage.colorize("<gray>冷却: <white>" + skill.getCooldown() + "秒"));
        sender.sendMessage(miniMessage.colorize("<gray>法力消耗: <white>" + skill.getManaCost()));
        sender.sendMessage(miniMessage.colorize("<gray>执行器: <yellow>" + skill.getExecutor().getTypeId()));

        // 显示参数
        Map<String, Object> params = skill.getParams();
        if (!params.isEmpty()) {
            sender.sendMessage(miniMessage.colorize("<gray>参数:"));
            for (Map.Entry<String, Object> param : params.entrySet()) {
                sender.sendMessage(miniMessage.colorize("  <dark_gray>- <white>" + param.getKey() + ": <aqua>" + param.getValue()));
            }
        }
    }

    /**
     * 查看玩家冷却
     */
    private void handleCooldown(CommandSender sender, String[] args) {
        if (!sender.hasPermission("rpgskill.admin")) {
            sender.sendMessage(miniMessage.red("你没有权限使用此命令"));
            return;
        }

        if (args.length < 2) {
            sender.sendMessage(miniMessage.colorize("<red>用法: /rpgskill cooldown <玩家名> [技能ID]"));
            return;
        }

        String playerName = args[1];
        var target = org.bukkit.Bukkit.getPlayer(playerName);
        if (target == null) {
            sender.sendMessage(miniMessage.red("玩家不在线: " + playerName));
            return;
        }

        String playerId = target.getUniqueId().toString();

        if (args.length >= 3) {
            // 查看特定技能冷却
            String skillId = args[2];
            SkillDefinition skill = plugin.getSkillRegistry().getSkill(skillId).orElse(null);
            if (skill == null) {
                sender.sendMessage(miniMessage.red("技能不存在: " + skillId));
                return;
            }

            long remaining = plugin.getCooldownManager().getCooldownRemaining(playerId, skillId, skill.getCooldown());
            if (remaining > 0) {
                sender.sendMessage(miniMessage.colorize("<yellow>" + target.getName() + " <gray>的技能 <gold>" + skillId + " <gray>冷却中，剩余 <red>" + remaining + " <gray>秒"));
            } else {
                sender.sendMessage(miniMessage.colorize("<yellow>" + target.getName() + " <gray>的技能 <gold>" + skillId + " <green>已就绪"));
            }
        } else {
            // 查看所有技能冷却
            sender.sendMessage(miniMessage.colorize("<gold><bold>===== " + target.getName() + " 的技能冷却 ====="));
            
            int activeCount = 0;
            for (Map.Entry<String, SkillDefinition> entry : plugin.getSkillRegistry().getAllSkills().entrySet()) {
                String skillId = entry.getKey();
                SkillDefinition skill = entry.getValue();
                long remaining = plugin.getCooldownManager().getCooldownRemaining(playerId, skillId, skill.getCooldown());

                if (remaining > 0) {
                    activeCount++;
                    sender.sendMessage(miniMessage.colorize("<gray>- <gold>" + skillId + "<gray>: <red>" + remaining + "秒"));
                }
            }

            if (activeCount == 0) {
                sender.sendMessage(miniMessage.colorize("<green>所有技能已就绪"));
            } else {
                sender.sendMessage(miniMessage.colorize("<gray>共 <red>" + activeCount + " <gray>个技能冷却中"));
            }
        }
    }

    /**
     * 清除玩家冷却
     */
    private void handleClearCooldown(CommandSender sender, String[] args) {
        if (!sender.hasPermission("rpgskill.admin")) {
            sender.sendMessage(miniMessage.red("你没有权限使用此命令"));
            return;
        }

        if (args.length < 2) {
            sender.sendMessage(miniMessage.colorize("<red>用法: /rpgskill clearcooldown <玩家名> [技能ID]"));
            return;
        }

        String playerName = args[1];
        var target = org.bukkit.Bukkit.getPlayer(playerName);
        if (target == null) {
            sender.sendMessage(miniMessage.red("玩家不在线: " + playerName));
            return;
        }

        String playerId = target.getUniqueId().toString();

        if (args.length >= 3) {
            // 清除特定技能冷却
            String skillId = args[2];
            plugin.getCooldownManager().clearCooldown(playerId, skillId);
            sender.sendMessage(miniMessage.green("已清除 " + target.getName() + " 的技能 " + skillId + " 冷却"));
        } else {
            // 清除所有冷却
            plugin.getCooldownManager().clearPlayerCooldowns(playerId);
            sender.sendMessage(miniMessage.green("已清除 " + target.getName() + " 的所有技能冷却"));
        }
    }

    /**
     * 发送帮助信息
     */
    private void sendHelp(CommandSender sender) {
        sender.sendMessage(miniMessage.colorize("<gold><bold>===== RPGSkill 命令 ====="));
        sender.sendMessage(miniMessage.colorize("<gray>/rpgskill reload <green>- 重载配置"));
        sender.sendMessage(miniMessage.colorize("<gray>/rpgskill list [类型/触发] <green>- 列出技能"));
        sender.sendMessage(miniMessage.colorize("<gray>/rpgskill info <技能ID> <green>- 查看技能详情"));
        sender.sendMessage(miniMessage.colorize("<gray>/rpgskill cooldown <玩家> [技能ID] <green>- 查看冷却"));
        sender.sendMessage(miniMessage.colorize("<gray>/rpgskill clearcooldown <玩家> [技能ID] <green>- 清除冷却"));
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        List<String> completions = new ArrayList<>();

        if (args.length == 1) {
            completions.addAll(List.of("reload", "list", "info", "cooldown", "clearcooldown"));
        } else if (args.length == 2) {
            switch (args[0].toLowerCase()) {
                case "list" -> completions.addAll(List.of("active", "passive", "toggle", "right_click", "left_click", "on_hit"));
                case "info" -> completions.addAll(plugin.getSkillRegistry().getAllSkills().keySet());
                case "cooldown", "clearcooldown" -> completions.addAll(
                    org.bukkit.Bukkit.getOnlinePlayers().stream()
                        .map(org.bukkit.entity.Player::getName)
                        .collect(Collectors.toList())
                );
            }
        } else if (args.length == 3) {
            if (args[0].equalsIgnoreCase("cooldown") || args[0].equalsIgnoreCase("clearcooldown")) {
                completions.addAll(plugin.getSkillRegistry().getAllSkills().keySet());
            }
        }

        return completions.stream()
            .filter(s -> s.toLowerCase().startsWith(args[args.length - 1].toLowerCase()))
            .collect(Collectors.toList());
    }
}
