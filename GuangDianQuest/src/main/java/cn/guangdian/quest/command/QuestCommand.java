package cn.guangdian.quest.command;

import cn.guangdian.quest.GuangDianQuest;
import cn.guangdian.quest.model.PlayerQuestData;
import cn.guangdian.quest.model.Quest;
import cn.guangdian.quest.service.ChatMessageService;
import net.kyori.adventure.text.Component;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabExecutor;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 任务命令处理器
 * 所有交互通过聊天框完成
 */
public class QuestCommand implements CommandExecutor, TabExecutor {

    private final GuangDianQuest plugin;
    private final ChatMessageService messageService;

    public QuestCommand(GuangDianQuest plugin) {
        this.plugin = plugin;
        this.messageService = plugin.getChatMessageService();
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length == 0) {
            // 默认打开GUI
            if (sender instanceof Player player) {
                plugin.getQuestGUIManager().openMainMenu(player);
            } else {
                sendHelp(sender);
            }
            return true;
        }

        String subCommand = args[0].toLowerCase();

        switch (subCommand) {
            case "list" -> handleList(sender, args);
            case "accept" -> handleAccept(sender, args);
            case "abandon" -> handleAbandon(sender, args);
            case "complete" -> handleComplete(sender, args);
            case "info" -> handleInfo(sender, args);
            case "daily" -> handleDaily(sender, args);
            case "track" -> handleTrack(sender, args);
            case "questline" -> handleQuestLine(sender, args);
            case "talk" -> handleTalk(sender, args);
            case "dialogue" -> handleDialogue(sender, args);
            case "available" -> handleAvailable(sender);
            case "reload" -> handleReload(sender);
            case "resetdaily" -> handleResetDaily(sender);
            case "help" -> sendHelp(sender);
            default -> {
                if (sender instanceof Player player) {
                    messageService.sendError(player, "未知命令！使用 /quest help 查看帮助");
                } else {
                    sender.sendMessage(color("<red>未知命令！"));
                }
            }
        }

        return true;
    }

    private void handleList(CommandSender sender, String[] args) {
        if (!(sender instanceof Player player)) {
            sendErrorMessage(sender, "该命令只能由玩家执行！");
            return;
        }

        messageService.sendQuestList(player);
    }

    private void handleAccept(CommandSender sender, String[] args) {
        if (!(sender instanceof Player player)) {
            sendErrorMessage(sender, "该命令只能由玩家执行！");
            return;
        }

        if (args.length < 2) {
            messageService.sendWarning(player, "用法: /quest accept <任务ID>");
            return;
        }

        String questId = args[1].toLowerCase();
        Quest quest = plugin.getQuestManager().getQuest(questId);

        if (quest == null) {
            messageService.sendError(player, "任务不存在！");
            return;
        }

        PlayerQuestData data = plugin.getProgressManager().getPlayerData(player.getUniqueId());

        if (data.isQuestActive(questId)) {
            messageService.sendWarning(player, "任务已在进行中！");
            return;
        }

        if (data.isQuestCompleted(questId)) {
            messageService.sendWarning(player, "任务已完成！");
            return;
        }

        if (data.getActiveQuestCount() >= plugin.getMaxActiveQuests()) {
            messageService.sendError(player, "已达到任务上限！");
            return;
        }

        if (quest.getRequiredLevel() > 0) {
            int level = getPlayerLevel(player);
            if (level < quest.getRequiredLevel()) {
                messageService.sendError(player, "等级不足！需要 Lv." + quest.getRequiredLevel());
                return;
            }
        }

        if (plugin.getQuestManager().acceptQuest(player.getUniqueId(), questId)) {
            messageService.sendSuccess(player, "已接取任务: " + quest.getName());
            messageService.sendQuestDetail(player, questId);
        } else {
            messageService.sendError(player, "无法接取任务！前置任务未完成。");
        }
    }

    private void handleAbandon(CommandSender sender, String[] args) {
        if (!(sender instanceof Player player)) {
            sendErrorMessage(sender, "该命令只能由玩家执行！");
            return;
        }

        if (args.length < 2) {
            messageService.sendWarning(player, "用法: /quest abandon <任务ID>");
            return;
        }

        String questId = args[1].toLowerCase();

        if (plugin.getQuestManager().abandonQuest(player.getUniqueId(), questId)) {
            Quest quest = plugin.getQuestManager().getQuest(questId);
            String name = quest != null ? quest.getName() : questId;
            messageService.sendSuccess(player, "已放弃任务: " + name);
        } else {
            messageService.sendError(player, "无法放弃任务！任务未进行中。");
        }
    }

    private void handleComplete(CommandSender sender, String[] args) {
        if (!(sender instanceof Player player)) {
            sendErrorMessage(sender, "该命令只能由玩家执行！");
            return;
        }

        if (args.length < 2) {
            messageService.sendWarning(player, "用法: /quest complete <任务ID>");
            return;
        }

        String questId = args[1].toLowerCase();

        if (plugin.getQuestManager().completeQuest(player.getUniqueId(), questId)) {
            Quest quest = plugin.getQuestManager().getQuest(questId);
            String name = quest != null ? quest.getName() : questId;
            messageService.sendSuccess(player, "已完成任务: " + name);
            if (quest != null && quest.getReward().hasRewards()) {
                messageService.sendInfo(player, "奖励: " + quest.getReward().getSummary());
            }
        } else {
            messageService.sendError(player, "无法完成任务！请确保所有目标已完成。");
        }
    }

    private void handleInfo(CommandSender sender, String[] args) {
        if (!(sender instanceof Player player)) {
            sendErrorMessage(sender, "该命令只能由玩家执行！");
            return;
        }

        if (args.length < 2) {
            messageService.sendWarning(player, "用法: /quest info <任务ID>");
            return;
        }

        String questId = args[1].toLowerCase();
        messageService.sendQuestDetail(player, questId);
    }

    private void handleDaily(CommandSender sender, String[] args) {
        if (!(sender instanceof Player player)) {
            sendErrorMessage(sender, "该命令只能由玩家执行！");
            return;
        }

        messageService.sendDailyQuests(player);
    }

    private void handleTrack(CommandSender sender, String[] args) {
        if (!(sender instanceof Player player)) {
            sendErrorMessage(sender, "该命令只能由玩家执行！");
            return;
        }

        messageService.sendQuestTrack(player);
    }

    private void handleQuestLine(CommandSender sender, String[] args) {
        if (!(sender instanceof Player player)) {
            sendErrorMessage(sender, "该命令只能由玩家执行！");
            return;
        }

        messageService.sendQuestLines(player);
    }

    private void handleAvailable(CommandSender sender) {
        if (!(sender instanceof Player player)) {
            sendErrorMessage(sender, "该命令只能由玩家执行！");
            return;
        }

        // 显示可接取的任务列表
        messageService.sendQuestList(player);
    }

    private void handleTalk(CommandSender sender, String[] args) {
        // talk 命令可以由控制台执行（由 NPC 命令触发）
        // 用法: /quest talk <npcId> [playerName]
        if (args.length < 2) {
            sendErrorMessage(sender, "用法: /quest talk <NPC ID> [玩家名]");
            return;
        }

        String npcId = args[1].toLowerCase();
        Player targetPlayer;

        if (args.length >= 3) {
            // 指定了玩家名（控制台或 NPC 命令调用）
            targetPlayer = plugin.getServer().getPlayer(args[2]);
            if (targetPlayer == null) {
                sendErrorMessage(sender, "玩家不在线: " + args[2]);
                return;
            }
        } else if (sender instanceof Player player) {
            // 玩家自己执行
            targetPlayer = player;
        } else {
            sendErrorMessage(sender, "控制台必须指定玩家名！");
            return;
        }

        // 触发 NPC 对话事件
        plugin.getQuestEventListener().onNPCInteract(npcId, targetPlayer);
        
        // 发送调试信息（可选）
        if (sender instanceof Player player && player.hasPermission("guangdian.quest.admin")) {
            messageService.sendInfo(player, "已触发 NPC 对话: " + npcId);
        }
    }

    /**
     * 处理对话GUI命令
     * 用法: /quest dialogue <npcId> [playerName]
     */
    private void handleDialogue(CommandSender sender, String[] args) {
        if (args.length < 2) {
            sendErrorMessage(sender, "用法: /quest dialogue <NPC ID> [玩家名]");
            return;
        }

        String npcId = args[1].toLowerCase();
        Player targetPlayer;

        if (args.length >= 3) {
            // 指定了玩家名（控制台或 NPC 命令调用）
            targetPlayer = plugin.getServer().getPlayer(args[2]);
            if (targetPlayer == null) {
                sendErrorMessage(sender, "玩家不在线: " + args[2]);
                return;
            }
        } else if (sender instanceof Player player) {
            // 玩家自己执行
            targetPlayer = player;
        } else {
            sendErrorMessage(sender, "控制台必须指定玩家名！");
            return;
        }

        // 打开对话GUI（自动查找NPC信息）
        plugin.getDialogueGUI().openDialogueForNPC(targetPlayer, npcId);
    }

    private void handleReload(CommandSender sender) {
        if (!sender.hasPermission("guangdian.quest.admin")) {
            sendErrorMessage(sender, "没有权限！");
            return;
        }

        plugin.reloadConfigs();
        sendSuccessMessage(sender, "配置已重载！");
    }

    private void handleResetDaily(CommandSender sender) {
        if (!sender.hasPermission("guangdian.quest.admin")) {
            sendErrorMessage(sender, "没有权限！");
            return;
        }

        if (sender instanceof Player player) {
            plugin.getDailyManager().resetPlayerDaily(player.getUniqueId());
            messageService.sendSuccess(player, "每日任务已重置！");
        } else {
            plugin.getDailyManager().resetAllDaily();
            sender.sendMessage(color("<green>所有玩家每日任务已重置！"));
        }
    }

    private void sendHelp(CommandSender sender) {
        sender.sendMessage(color("<gold><bold>╔═══════════════════════════════════╗"));
        sender.sendMessage(color("<gold><bold>║  📖 任务命令帮助                   ║"));
        sender.sendMessage(color("<gold><bold>╠═══════════════════════════════════╣"));
        sender.sendMessage(color("<yellow>║  /quest <gray>查看任务列表"));
        sender.sendMessage(color("<yellow>║  /quest list <gray>查看任务列表"));
        sender.sendMessage(color("<yellow>║  /quest info <ID> <gray>查看任务详情"));
        sender.sendMessage(color("<yellow>║  /quest accept <ID> <gray>接取任务"));
        sender.sendMessage(color("<yellow>║  /quest complete <ID> <gray>完成任务"));
        sender.sendMessage(color("<yellow>║  /quest abandon <ID> <gray>放弃任务"));
        sender.sendMessage(color("<yellow>║  /quest daily <gray>查看每日任务"));
        sender.sendMessage(color("<yellow>║  /quest track <gray>追踪任务进度"));
        sender.sendMessage(color("<yellow>║  /quest questline <gray>查看任务线"));
        if (sender.hasPermission("guangdian.quest.admin")) {
            sender.sendMessage(color("<gold><bold>║                                    ║"));
            sender.sendMessage(color("<red>║  /quest reload <gray>重载配置"));
            sender.sendMessage(color("<red>║  /quest resetdaily <gray>重置每日任务"));
        }
        sender.sendMessage(color("<gold><bold>╚═══════════════════════════════════╝"));
    }

    private int getPlayerLevel(Player player) {
        cn.guangdian.rpgcore.integration.ExternalServiceIntegration externalServices = plugin.getExternalServices();
        if (externalServices != null) {
            try {
                String levelStr = externalServices.parsePlaceholders(player, "%rpgcore_level%");
                if (levelStr != null && !levelStr.isEmpty() && !levelStr.equals("%rpgcore_level%")) {
                    try { return Integer.parseInt(levelStr); } catch (NumberFormatException ignored) {}
                }
                levelStr = externalServices.parsePlaceholders(player, "%player_level%");
                if (levelStr != null && !levelStr.isEmpty() && !levelStr.equals("%player_level%")) {
                    try { return Integer.parseInt(levelStr); } catch (NumberFormatException ignored) {}
                }
            } catch (Exception ignored) {}
        }
        return player.getLevel();
    }

    private Component color(String text) {
        return GuangDianQuest.color(text);
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        List<String> completions = new ArrayList<>();

        if (args.length == 1) {
            List<String> subs = new ArrayList<>(Arrays.asList(
                "list", "accept", "abandon", "complete", "info", "daily", "track", "questline", "help"
            ));
            if (sender.hasPermission("guangdian.quest.admin")) {
                subs.addAll(Arrays.asList("reload", "resetdaily"));
            }
            completions.addAll(subs);
        } else if (args.length == 2) {
            String subCommand = args[0].toLowerCase();
            if (subCommand.equals("accept")) {
                if (sender instanceof Player player) {
                    completions.addAll(plugin.getQuestManager().getAvailableQuests(player.getUniqueId()));
                }
            } else if (subCommand.equals("abandon") || subCommand.equals("complete")) {
                if (sender instanceof Player player) {
                    PlayerQuestData data = plugin.getProgressManager().getPlayerData(player.getUniqueId());
                    completions.addAll(data.getActiveQuestIds());
                }
            } else if (subCommand.equals("info")) {
                completions.addAll(plugin.getQuestRepository().getAllQuests().stream()
                    .map(Quest::getId).collect(Collectors.toList()));
            }
        }

        String lastArg = args[args.length - 1].toLowerCase();
        return completions.stream()
            .filter(s -> s.toLowerCase().startsWith(lastArg))
            .collect(Collectors.toList());
    }

    // 为 CommandSender 提供消息服务的辅助方法
    private void sendErrorMessage(CommandSender sender, String message) {
        if (sender instanceof Player player) {
            messageService.sendError(player, message);
        } else {
            sender.sendMessage(color("<red>" + message));
        }
    }

    private void sendWarningMessage(CommandSender sender, String message) {
        if (sender instanceof Player player) {
            messageService.sendWarning(player, message);
        } else {
            sender.sendMessage(color("<yellow>" + message));
        }
    }

    private void sendSuccessMessage(CommandSender sender, String message) {
        if (sender instanceof Player player) {
            messageService.sendSuccess(player, message);
        } else {
            sender.sendMessage(color("<green>" + message));
        }
    }

    private void sendInfoMessage(CommandSender sender, String message) {
        if (sender instanceof Player player) {
            messageService.sendInfo(player, message);
        } else {
            sender.sendMessage(color("<aqua>" + message));
        }
    }
}
