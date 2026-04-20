package cn.guangdian.quest.command;

import cn.guangdian.quest.GuangDianQuest;
import cn.guangdian.quest.model.PlayerQuestData;
import cn.guangdian.quest.model.Quest;
import cn.guangdian.quest.model.QuestObjective;
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

public class QuestCommand implements CommandExecutor, TabExecutor {

    private final GuangDianQuest plugin;

    public QuestCommand(GuangDianQuest plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length == 0) {
            sendHelp(sender);
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
            case "reload" -> handleReload(sender);
            case "resetdaily" -> handleResetDaily(sender);
            default -> sendHelp(sender);
        }

        return true;
    }

    private void handleList(CommandSender sender, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("该命令只能由玩家执行！");
            return;
        }

        PlayerQuestData data = plugin.getProgressManager().getPlayerData(player.getUniqueId());

        sender.sendMessage(color("&e========== 进行中任务 &7(" + data.getActiveQuestCount() + "/" + plugin.getMaxActiveQuests() + ") &e=========="));
        for (String questId : data.getActiveQuestIds()) {
            Quest quest = plugin.getQuestManager().getQuest(questId);
            if (quest != null) {
                int[] progress = data.getProgress(questId);
                int percent = progress != null ? calculateProgressPercent(progress, quest) : 0;
                sender.sendMessage(color("&7- " + quest.getFullName() + " &8(" + percent + "%)"));
                for (int i = 0; i < quest.getObjectiveCount(); i++) {
                    QuestObjective obj = quest.getObjective(i);
                    int current = (progress != null && i < progress.length) ? progress[i] : 0;
                    String status = current >= obj.getAmount() ? "&a✔" : "&7○";
                    sender.sendMessage(color("  " + status + " &f" + obj.getProgressText(current)));
                }
            }
        }

        sender.sendMessage(color("&e========== 可接取任务 =========="));
        List<String> available = plugin.getQuestManager().getAvailableQuests(player.getUniqueId());
        if (available.isEmpty()) {
            sender.sendMessage(color("&7暂无可接取的任务"));
        } else {
            for (String questId : available) {
                Quest quest = plugin.getQuestManager().getQuest(questId);
                if (quest != null) {
                    String levelReq = quest.getRequiredLevel() > 0 ? " &8[需要Lv." + quest.getRequiredLevel() + "]" : "";
                    sender.sendMessage(color("&7- " + quest.getFullName() + levelReq));
                }
            }
        }
    }

    private int calculateProgressPercent(int[] progress, Quest quest) {
        if (progress == null || quest.getObjectiveCount() == 0) return 0;

        int total = 0;
        int completed = 0;
        for (int i = 0; i < quest.getObjectiveCount(); i++) {
            total += quest.getObjective(i).getAmount();
            completed += Math.min(progress[i], quest.getObjective(i).getAmount());
        }

        return total > 0 ? (completed * 100 / total) : 0;
    }

    private void handleAccept(CommandSender sender, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("该命令只能由玩家执行！");
            return;
        }

        if (args.length < 2) {
            sender.sendMessage(color("&e用法: /quest accept <任务ID>"));
            return;
        }

        String questId = args[1].toLowerCase();
        Quest quest = plugin.getQuestManager().getQuest(questId);

        if (quest == null) {
            sender.sendMessage(plugin.getMessage("quest-not-found"));
            return;
        }

        PlayerQuestData data = plugin.getProgressManager().getPlayerData(player.getUniqueId());

        if (data.isQuestActive(questId)) {
            sender.sendMessage(plugin.getMessage("quest-already-active", "{quest}", quest.getName()));
            return;
        }

        if (data.isQuestCompleted(questId)) {
            sender.sendMessage(plugin.getMessage("quest-already-completed", "{quest}", quest.getName()));
            return;
        }

        if (data.getActiveQuestCount() >= plugin.getMaxActiveQuests()) {
            sender.sendMessage(plugin.getMessage("quest-limit-reached"));
            return;
        }

        if (quest.getRequiredLevel() > 0) {
            int level = getPlayerLevel(player);
            if (level < quest.getRequiredLevel()) {
                sender.sendMessage(plugin.getMessage("quest-level-not-met", "{level}", String.valueOf(quest.getRequiredLevel())));
                return;
            }
        }

        if (plugin.getQuestManager().acceptQuest(player.getUniqueId(), questId)) {
            sender.sendMessage(plugin.getMessage("quest-accepted", "{quest}", quest.getName()));
            for (String line : quest.getDescription()) {
                sender.sendMessage(color("&7" + line));
            }
        } else {
            sender.sendMessage(plugin.getMessage("quest-prerequisites-not-met"));
        }
    }

    private void handleAbandon(CommandSender sender, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("该命令只能由玩家执行！");
            return;
        }

        if (args.length < 2) {
            sender.sendMessage(color("&e用法: /quest abandon <任务ID>"));
            return;
        }

        String questId = args[1].toLowerCase();

        if (plugin.getQuestManager().abandonQuest(player.getUniqueId(), questId)) {
            Quest quest = plugin.getQuestManager().getQuest(questId);
            String name = quest != null ? quest.getName() : questId;
            sender.sendMessage(plugin.getMessage("quest-abandoned", "{quest}", name));
        } else {
            sender.sendMessage(color("&c无法放弃任务！任务未进行中。"));
        }
    }

    private void handleComplete(CommandSender sender, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("该命令只能由玩家执行！");
            return;
        }

        if (args.length < 2) {
            sender.sendMessage(color("&e用法: /quest complete <任务ID>"));
            return;
        }

        String questId = args[1].toLowerCase();

        if (plugin.getQuestManager().completeQuest(player.getUniqueId(), questId)) {
            Quest quest = plugin.getQuestManager().getQuest(questId);
            String name = quest != null ? quest.getName() : questId;
            sender.sendMessage(plugin.getMessage("quest-completed", "{quest}", name));
            if (quest != null && quest.getReward().hasRewards()) {
                sender.sendMessage(color("&7奖励: &e" + quest.getReward().getSummary()));
            }
        } else {
            sender.sendMessage(color("&c无法完成任务！请确保所有目标已完成。"));
        }
    }

    private void handleInfo(CommandSender sender, String[] args) {
        if (args.length < 2) {
            sender.sendMessage(color("&e用法: /quest info <任务ID>"));
            return;
        }

        String questId = args[1].toLowerCase();
        Quest quest = plugin.getQuestManager().getQuest(questId);

        if (quest == null) {
            sender.sendMessage(plugin.getMessage("quest-not-found"));
            return;
        }

        sender.sendMessage(color("&e========== 任务详情 =========="));
        sender.sendMessage(color("&7名称: &f" + quest.getFullName()));
        sender.sendMessage(color("&7类型: &b" + quest.getType().getDisplayName()));

        if (quest.getRequiredLevel() > 0) {
            sender.sendMessage(color("&7等级要求: &cLv." + quest.getRequiredLevel()));
        }

        if (!quest.getPrerequisites().isEmpty()) {
            sender.sendMessage(color("&7前置任务: &f" + String.join(", ", quest.getPrerequisites())));
        }

        for (String line : quest.getDescription()) {
            sender.sendMessage(color("&f" + line));
        }

        sender.sendMessage(color("&7目标:"));
        for (QuestObjective obj : quest.getObjectives()) {
            sender.sendMessage(color("&8- &f" + obj.getDescription()));
        }

        if (quest.getReward().hasRewards()) {
            sender.sendMessage(color("&7奖励: &e" + quest.getReward().getSummary()));
        }

        if (sender instanceof Player player) {
            PlayerQuestData data = plugin.getProgressManager().getPlayerData(player.getUniqueId());
            if (data.isQuestActive(questId)) {
                int[] progress = data.getProgress(questId);
                sender.sendMessage(color("&7当前进度:"));
                for (int i = 0; i < quest.getObjectiveCount(); i++) {
                    QuestObjective obj = quest.getObjective(i);
                    int current = (progress != null && i < progress.length) ? progress[i] : 0;
                    sender.sendMessage(color("&8- &f" + obj.getProgressText(current)));
                }
            } else if (data.isQuestCompleted(questId)) {
                sender.sendMessage(color("&a✔ 已完成"));
            } else {
                sender.sendMessage(color("&7状态: &e可接取"));
            }
        }
    }

    private void handleDaily(CommandSender sender, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("该命令只能由玩家执行！");
            return;
        }

        PlayerQuestData data = plugin.getProgressManager().getPlayerData(player.getUniqueId());

        sender.sendMessage(color("&e========== 每日任务 =========="));
        sender.sendMessage(color("&7今日完成: &e" + data.getDailyCompletedCount() + "/" + plugin.getDailyQuestLimit()));

        List<String> dailyQuests = plugin.getDailyManager().getDailyQuests(player.getUniqueId());
        for (String questId : dailyQuests) {
            Quest quest = plugin.getQuestManager().getQuest(questId);
            if (quest != null) {
                String status = data.isQuestActive(questId) ? "&a进行中" :
                    (data.isQuestCompleted(questId) ? "&7已完成" : "&e可接取");
                sender.sendMessage(color("&7- " + quest.getName() + " " + status));
            }
        }
    }

    private void handleTrack(CommandSender sender, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("该命令只能由玩家执行！");
            return;
        }

        PlayerQuestData data = plugin.getProgressManager().getPlayerData(player.getUniqueId());
        if (data.getActiveQuestCount() == 0) {
            sender.sendMessage(color("&7当前没有进行中的任务"));
            return;
        }

        sender.sendMessage(color("&e========== 任务追踪 =========="));
        for (String questId : data.getActiveQuestIds()) {
            Quest quest = plugin.getQuestManager().getQuest(questId);
            if (quest != null) {
                int[] progress = data.getProgress(questId);
                sender.sendMessage(color("&6" + quest.getFullName()));
                for (int i = 0; i < quest.getObjectiveCount(); i++) {
                    QuestObjective obj = quest.getObjective(i);
                    int current = (progress != null && i < progress.length) ? progress[i] : 0;
                    String status = current >= obj.getAmount() ? "&a✔" : "&7○";
                    sender.sendMessage(color("  " + status + " &f" + obj.getProgressText(current)));
                }
            }
        }
    }

    private void handleQuestLine(CommandSender sender, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("该命令只能由玩家执行！");
            return;
        }

        sender.sendMessage(color("&e========== 任务线 =========="));
        for (cn.guangdian.quest.model.QuestLine line : plugin.getQuestLineManager().getAllQuestLines()) {
            int progress = plugin.getQuestLineManager().getQuestLineProgress(player.getUniqueId(), line.getId());
            int percent = line.getProgressPercent(progress);
            sender.sendMessage(color("&7- &f" + line.getName() + " &e" + percent + "% &8(" + (progress + 1) + "/" + line.getLength() + ")"));
        }
    }

    private void handleReload(CommandSender sender) {
        if (!sender.hasPermission("guangdian.quest.admin")) {
            sender.sendMessage(color("&c没有权限！"));
            return;
        }

        plugin.reloadConfigs();
        sender.sendMessage(color("&a配置已重载！"));
    }

    private void handleResetDaily(CommandSender sender) {
        if (!sender.hasPermission("guangdian.quest.admin")) {
            sender.sendMessage(color("&c没有权限！"));
            return;
        }

        if (sender instanceof Player player) {
            plugin.getDailyManager().resetPlayerDaily(player.getUniqueId());
            sender.sendMessage(color("&a每日任务已重置！"));
        } else {
            plugin.getDailyManager().resetAllDaily();
            sender.sendMessage(color("&a所有玩家每日任务已重置！"));
        }
    }

    private void sendHelp(CommandSender sender) {
        sender.sendMessage(color("&e========== 任务命令帮助 =========="));
        sender.sendMessage(color("&e/quest list &7- 查看任务列表"));
        sender.sendMessage(color("&e/quest accept <ID> &7- 接取任务"));
        sender.sendMessage(color("&e/quest abandon <ID> &7- 放弃任务"));
        sender.sendMessage(color("&e/quest complete <ID> &7- 完成任务"));
        sender.sendMessage(color("&e/quest info <ID> &7- 查看任务详情"));
        sender.sendMessage(color("&e/quest daily &7- 查看每日任务"));
        sender.sendMessage(color("&e/quest track &7- 追踪任务进度"));
        sender.sendMessage(color("&e/quest questline &7- 查看任务线"));
        if (sender.hasPermission("guangdian.quest.admin")) {
            sender.sendMessage(color("&e/quest reload &7- 重载配置"));
            sender.sendMessage(color("&e/quest resetdaily &7- 重置每日任务"));
        }
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
            List<String> subs = new ArrayList<>(Arrays.asList("list", "accept", "abandon", "complete", "info", "daily", "track", "questline"));
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
}
