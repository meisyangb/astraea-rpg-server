package cn.guangdian.quest.dialogue;

import cn.guangdian.quest.GuangDianQuest;
import cn.guangdian.quest.model.PlayerQuestData;
import cn.guangdian.quest.model.Quest;
import cn.guangdian.quest.model.QuestObjective;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Bukkit;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitTask;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 聊天式对话管理器
 * 
 * 特性：
 * - 致盲效果：玩家进入对话时周围环境变黑
 * - 聊天框展示：对话内容在聊天框显示
 * - 方框框定：对话内容用方框包裹，增强辨识度
 * - 多选项支持：NPC 可配置多个选项（[接受]、[拒绝]等）
 */
public class ChatDialogueManager {

    private final GuangDianQuest plugin;
    
    // 正在对话中的玩家
    private final Map<UUID, DialogueSession> activeSessions = new ConcurrentHashMap<>();
    
    // 致盲效果强度（1-10，数值越大越黑）
    private static final int BLINDNESS_AMPLIFIER = 10;
    
    // 方框宽度（字符数）
    private static final int BOX_WIDTH = 50;
    
    public ChatDialogueManager(GuangDianQuest plugin) {
        this.plugin = plugin;
    }
    
    /**
     * 开始对话
     */
    public void startDialogue(Player player, String npcId, String npcName, String npcRole,
                             List<String> dialogueLines, String questId, int objectiveIndex,
                             List<DialogueOption> options) {
        
        UUID playerId = player.getUniqueId();
        
        // 如果已经在对话中，先结束之前的对话
        if (activeSessions.containsKey(playerId)) {
            endDialogue(player);
        }
        
        // 创建对话会话
        DialogueSession session = new DialogueSession(
            npcId, npcName, npcRole, dialogueLines, 
            questId, objectiveIndex, options, 0
        );
        activeSessions.put(playerId, session);
        
        // 应用致盲效果
        applyBlindness(player);
        
        // 显示对话开始提示
        player.sendMessage(Component.empty());
        sendBoxLine(player, "═", NamedTextColor.GOLD);
        player.sendMessage(Component.text("  ⚔ 对话开始", NamedTextColor.GOLD, TextDecoration.BOLD)
            .append(Component.text(" - " + npcName, NamedTextColor.YELLOW)));
        sendBoxLine(player, "═", NamedTextColor.GOLD);
        
        // 显示 NPC 信息
        player.sendMessage(Component.text("  【" + npcRole + "】", NamedTextColor.GRAY));
        player.sendMessage(Component.empty());
        
        // 显示对话内容
        for (String line : dialogueLines) {
            sendDialogueLine(player, line);
        }
        
        player.sendMessage(Component.empty());
        
        // 显示选项（如果有）
        if (options != null && !options.isEmpty()) {
            sendBoxLine(player, "─", NamedTextColor.DARK_GRAY);
            player.sendMessage(Component.text("  请选择：", NamedTextColor.YELLOW));
            for (int i = 0; i < options.size(); i++) {
                DialogueOption opt = options.get(i);
                player.sendMessage(Component.text("  [" + (i + 1) + "] ", NamedTextColor.GREEN)
                    .append(Component.text(opt.getText(), NamedTextColor.WHITE)));
            }
            player.sendMessage(Component.text("  输入数字选择选项", NamedTextColor.GRAY));
        } else {
            // 无选项时，显示结束对话提示
            sendBoxLine(player, "─", NamedTextColor.DARK_GRAY);
            player.sendMessage(Component.text("  输入 ", NamedTextColor.GRAY)
                .append(Component.text("123456789", NamedTextColor.YELLOW))
                .append(Component.text(" 可跳过对话", NamedTextColor.GRAY)));
        }
        
        sendBoxLine(player, "═", NamedTextColor.GOLD);
        
        // 播放音效
        player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_PLING, 1.0f, 1.0f);
    }
    
    /**
     * 简化版：自动从配置和任务系统加载数据开始对话
     */
    public void startDialogueForNPC(Player player, String npcId) {
        // 查找关联的任务
        String foundQuestId = null;
        int foundObjectiveIndex = -1;
        List<String> dialogueLines = new ArrayList<>();
        List<DialogueOption> options = null;
        String npcName = npcId;
        String npcRole = "NPC";
        
        // 尝试从 Citizens 获取 NPC 名称
        try {
            Class<?> citizensApiClass = Class.forName("net.citizensnpcs.api.CitizensAPI");
            Object npcRegistry = citizensApiClass.getMethod("getNPCRegistry").invoke(null);
            
            Object npc = npcRegistry.getClass().getMethod("getNPCByName", String.class).invoke(npcRegistry, npcId);
            if (npc == null) {
                try {
                    int id = Integer.parseInt(npcId);
                    npc = npcRegistry.getClass().getMethod("getById", int.class).invoke(npcRegistry, id);
                } catch (NumberFormatException ignored) {}
            }
            
            if (npc != null) {
                npcName = (String) npc.getClass().getMethod("getName").invoke(npc);
            }
        } catch (Exception ignored) {}
        
        // 查找关联的对话任务
        PlayerQuestData data = plugin.getProgressManager().getPlayerData(player.getUniqueId());
        for (String questId : data.getActiveQuestIds()) {
            Quest quest = plugin.getQuestManager().getQuest(questId);
            if (quest == null) continue;
            
            for (int i = 0; i < quest.getObjectiveCount(); i++) {
                QuestObjective obj = quest.getObjective(i);
                if (obj.getType() == QuestObjective.ObjectiveType.TALK) {
                    if (obj.getTarget().equalsIgnoreCase(npcId)) {
                        foundQuestId = questId;
                        foundObjectiveIndex = i;
                        
                        if (obj.getDialogue() != null && !obj.getDialogue().isEmpty()) {
                            dialogueLines = obj.getDialogue();
                        } else {
                            dialogueLines.add("<gray>与 " + npcName + " 的对话...");
                        }
                        
                        // 从配置加载选项
                        options = loadDialogueOptions(npcId, questId, i);
                        break;
                    }
                }
            }
            if (foundQuestId != null) break;
        }
        
        // 如果没找到关联任务，使用默认问候语
        if (dialogueLines.isEmpty()) {
            dialogueLines.add("<gray>你好，旅行者。");
            dialogueLines.add("<gray>有什么可以帮助你的吗？");
            
            // 默认选项
            options = Arrays.asList(
                new DialogueOption("好的，谢谢你", null, null),
                new DialogueOption("离开", null, null)
            );
        }
        
        startDialogue(player, npcId, npcName, npcRole, dialogueLines, foundQuestId, foundObjectiveIndex, options);
    }
    
    /**
     * 处理玩家输入
     */
    public boolean handlePlayerInput(Player player, String message) {
        UUID playerId = player.getUniqueId();
        DialogueSession session = activeSessions.get(playerId);
        
        if (session == null) {
            return false; // 玩家不在对话中
        }
        
        // 尝试解析数字选项
        try {
            int choice = Integer.parseInt(message.trim());
            
            // 检查是否是跳过对话的数字（123456789）
            if (message.trim().equals("123456789")) {
                endDialogue(player);
                player.sendMessage(Component.text("已跳过对话", NamedTextColor.GRAY));
                return true;
            }
            
            // 检查是否是有效选项
            List<DialogueOption> options = session.getOptions();
            if (options != null && choice >= 1 && choice <= options.size()) {
                DialogueOption selected = options.get(choice - 1);
                handleOptionSelection(player, selected, session);
                return true;
            }
        } catch (NumberFormatException ignored) {
            // 不是数字，忽略
        }
        
        return false; // 不是有效的选项输入
    }
    
    /**
     * 处理选项选择
     */
    private void handleOptionSelection(Player player, DialogueOption option, DialogueSession session) {
        String npcName = session.getNpcName();
        
        // 执行选项动作
        if (option.getAction() != null) {
            switch (option.getAction()) {
                case "accept":
                    // 接受任务
                    if (session.getQuestId() != null) {
                        boolean accepted = plugin.getQuestManager().acceptQuest(
                            player.getUniqueId(), session.getQuestId());
                        if (accepted) {
                            player.sendMessage(Component.text("✔ 已接取任务", NamedTextColor.GREEN));
                            player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_PLING, 1.0f, 2.0f);
                        } else {
                            player.sendMessage(Component.text("接取失败", NamedTextColor.RED));
                        }
                    }
                    break;
                    
                case "complete_talk":
                    // 完成对话目标
                    if (session.getQuestId() != null && session.getObjectiveIndex() >= 0) {
                        int newProgress = plugin.getProgressManager().incrementProgress(
                            player.getUniqueId(), session.getQuestId(), session.getObjectiveIndex(), 1);
                        
                        Quest quest = plugin.getQuestManager().getQuest(session.getQuestId());
                        QuestObjective obj = quest != null ? quest.getObjective(session.getObjectiveIndex()) : null;
                        int required = obj != null ? obj.getAmount() : 1;
                        
                        if (newProgress >= required) {
                            player.sendMessage(Component.text("✔ 与 " + npcName + " 的对话已完成！", NamedTextColor.GREEN));
                            player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_PLING, 1.0f, 2.0f);
                        }
                    }
                    break;
                    
                case "reject":
                    player.sendMessage(Component.text("你拒绝了请求", NamedTextColor.GRAY));
                    break;
                    
                case "command":
                    // 执行命令
                    if (option.getCommand() != null) {
                        String cmd = option.getCommand().replace("<player>", player.getName());
                        Bukkit.dispatchCommand(Bukkit.getConsoleSender(), cmd);
                    }
                    break;
            }
        }
        
        // 执行自定义命令
        if (option.getCommand() != null && !"command".equals(option.getAction())) {
            String cmd = option.getCommand().replace("<player>", player.getName());
            Bukkit.dispatchCommand(Bukkit.getConsoleSender(), cmd);
        }
        
        // 结束对话
        endDialogue(player);
    }
    
    /**
     * 结束对话
     */
    public void endDialogue(Player player) {
        UUID playerId = player.getUniqueId();
        DialogueSession session = activeSessions.remove(playerId);
        
        if (session != null) {
            // 移除致盲效果
            removeBlindness(player);
            
            // 显示对话结束提示
            player.sendMessage(Component.empty());
            sendBoxLine(player, "═", NamedTextColor.DARK_GRAY);
            player.sendMessage(Component.text("  ⚔ 对话结束", NamedTextColor.GRAY));
            sendBoxLine(player, "═", NamedTextColor.DARK_GRAY);
            player.sendMessage(Component.empty());
            
            player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_PLING, 1.0f, 0.5f);
        }
    }
    
    /**
     * 检查玩家是否在对话中
     */
    public boolean isInDialogue(UUID playerId) {
        return activeSessions.containsKey(playerId);
    }
    
    /**
     * 应用致盲效果
     */
    private void applyBlindness(Player player) {
        // 添加致盲效果，持续时间够长，在对话结束时移除
        player.addPotionEffect(new PotionEffect(
            PotionEffectType.BLINDNESS,
            Integer.MAX_VALUE,  // 持续时间
            BLINDNESS_AMPLIFIER,
            false,  // 不显示粒子
            false   // 不显示图标
        ));
        
        // 添加夜视效果，使近距离区域仍然可见
        player.addPotionEffect(new PotionEffect(
            PotionEffectType.NIGHT_VISION,
            Integer.MAX_VALUE,
            0,  // 等级0，最小效果
            false,
            false
        ));
    }
    
    /**
     * 移除致盲效果
     */
    private void removeBlindness(Player player) {
        player.removePotionEffect(PotionEffectType.BLINDNESS);
        player.removePotionEffect(PotionEffectType.NIGHT_VISION);
    }
    
    /**
     * 发送方框线
     */
    private void sendBoxLine(Player player, String pattern, NamedTextColor color) {
        StringBuilder line = new StringBuilder("  ");
        for (int i = 0; i < BOX_WIDTH; i++) {
            line.append(pattern);
        }
        player.sendMessage(Component.text(line.toString(), color));
    }
    
    /**
     * 发送对话行（带方框）
     */
    private void sendDialogueLine(Player player, String line) {
        // 解析颜色代码
        Component text = GuangDianQuest.color(line);
        
        player.sendMessage(Component.text("  │ ", NamedTextColor.DARK_GRAY)
            .append(text));
    }
    
    /**
     * 从配置加载对话选项
     */
    private List<DialogueOption> loadDialogueOptions(String npcId, String questId, int objectiveIndex) {
        // TODO: 从配置文件加载选项
        // 默认返回接受/拒绝选项
        return Arrays.asList(
            new DialogueOption("[接受]", "complete_talk", null),
            new DialogueOption("[拒绝]", "reject", null)
        );
    }
    
    /**
     * 对话会话数据
     */
    public static class DialogueSession {
        private final String npcId;
        private final String npcName;
        private final String npcRole;
        private final List<String> dialogueLines;
        private final String questId;
        private final int objectiveIndex;
        private final List<DialogueOption> options;
        private final int currentLine;
        
        public DialogueSession(String npcId, String npcName, String npcRole,
                              List<String> dialogueLines, String questId, 
                              int objectiveIndex, List<DialogueOption> options, int currentLine) {
            this.npcId = npcId;
            this.npcName = npcName;
            this.npcRole = npcRole;
            this.dialogueLines = dialogueLines;
            this.questId = questId;
            this.objectiveIndex = objectiveIndex;
            this.options = options;
            this.currentLine = currentLine;
        }
        
        public String getNpcId() { return npcId; }
        public String getNpcName() { return npcName; }
        public String getNpcRole() { return npcRole; }
        public List<String> getDialogueLines() { return dialogueLines; }
        public String getQuestId() { return questId; }
        public int getObjectiveIndex() { return objectiveIndex; }
        public List<DialogueOption> getOptions() { return options; }
        public int getCurrentLine() { return currentLine; }
    }
}