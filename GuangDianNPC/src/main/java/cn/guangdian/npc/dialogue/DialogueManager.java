package cn.guangdian.npc.dialogue;

import cn.guangdian.npc.GuangDianNPC;
import cn.guangdian.rpgcore.RPGCore;
import cn.guangdian.rpgcore.message.MiniMessageService;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import net.kyori.adventure.title.Title;
import org.bukkit.Bukkit;
import org.bukkit.Sound;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;

import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;

/**
 * NPC 对话管理器
 */
public class DialogueManager {

    private final GuangDianNPC plugin;
    private final Map<String, NPCDialogue> dialogues;
    private final Map<UUID, PlayerDialogueState> activeDialogues;
    private final Map<String, String> npcToDialogueMap;

    private File dialogueFile;
    private FileConfiguration dialogueConfig;

    private static final long DIALOGUE_TIMEOUT = 5 * 60 * 1000; // 5分钟超时
    private static final String CHAT_PREFIX = "§8[§6对话§8] §7";

    public DialogueManager(GuangDianNPC plugin) {
        this.plugin = plugin;
        this.dialogues = new ConcurrentHashMap<>();
        this.activeDialogues = new ConcurrentHashMap<>();
        this.npcToDialogueMap = new ConcurrentHashMap<>();
        this.dialogueFile = new File(plugin.getDataFolder(), "dialogues.yml");
    }

    public void load() {
        loadDialogues();
    }

    public void reload() {
        dialogues.clear();
        npcToDialogueMap.clear();
        loadDialogues();
    }

    private void loadDialogues() {
        if (!dialogueFile.exists()) {
            plugin.saveResource("dialogues.yml", false);
        }

        dialogueConfig = YamlConfiguration.loadConfiguration(dialogueFile);
        ConfigurationSection section = dialogueConfig.getConfigurationSection("dialogues");
        if (section == null) {
            return;
        }

        for (String id : section.getKeys(false)) {
            ConfigurationSection dialogueSection = section.getConfigurationSection(id);
            if (dialogueSection == null) {
                continue;
            }
            try {
                NPCDialogue dialogue = NPCDialogue.fromConfig(id.toLowerCase(), dialogueSection);
                if (dialogue != null) {
                    dialogues.put(dialogue.getId(), dialogue);
                    if (!dialogue.getNpcId().isEmpty()) {
                        npcToDialogueMap.put(dialogue.getNpcId().toLowerCase(), dialogue.getId());
                    }
                }
            } catch (Exception e) {
                plugin.getLogger().log(Level.WARNING, "加载对话失败: " + id, e);
            }
        }

        plugin.getLogger().info("已加载 " + dialogues.size() + " 个对话");
    }

    public void save() {
        try {
            dialogueConfig.set("dialogues", null);
            for (NPCDialogue dialogue : dialogues.values()) {
                String path = "dialogues." + dialogue.getId();
                Map<String, Object> data = dialogue.serialize();
                for (Map.Entry<String, Object> entry : data.entrySet()) {
                    dialogueConfig.set(path + "." + entry.getKey(), entry.getValue());
                }
            }
            dialogueConfig.save(dialogueFile);
        } catch (IOException e) {
            plugin.getLogger().log(Level.SEVERE, "保存对话数据失败", e);
        }
    }

    /**
     * 开始对话
     */
    public boolean startDialogue(Player player, String npcId) {
        String dialogueId = npcToDialogueMap.get(npcId.toLowerCase());
        if (dialogueId == null) {
            return false;
        }

        NPCDialogue dialogue = dialogues.get(dialogueId);
        if (dialogue == null) {
            return false;
        }

        DialogueNode startNode = dialogue.getStartNode();
        if (startNode == null) {
            player.sendMessage(Component.text("对话配置错误：没有起始节点").color(NamedTextColor.RED));
            return false;
        }

        // 结束之前的对话
        endDialogue(player);

        // 创建对话状态
        PlayerDialogueState state = new PlayerDialogueState(player, dialogueId, npcId, startNode.getId());
        activeDialogues.put(player.getUniqueId(), state);

        // 显示对话
        displayNode(player, dialogue, startNode);

        return true;
    }

    /**
     * 处理玩家输入
     */
    public boolean handlePlayerInput(Player player, int optionIndex) {
        PlayerDialogueState state = activeDialogues.get(player.getUniqueId());
        if (state == null) {
            return false;
        }

        // 检查超时
        if (state.isExpired(DIALOGUE_TIMEOUT)) {
            endDialogue(player);
            player.sendMessage(Component.text("对话已超时").color(NamedTextColor.GRAY));
            return true;
        }

        NPCDialogue dialogue = dialogues.get(state.getDialogueId());
        if (dialogue == null) {
            endDialogue(player);
            return true;
        }

        DialogueNode currentNode = dialogue.getNode(state.getCurrentNodeId());
        if (currentNode == null) {
            endDialogue(player);
            return true;
        }

        DialogueOption option = currentNode.getOptionByIndex(optionIndex);
        if (option == null) {
            // 无效选项，重新显示当前节点
            displayNode(player, dialogue, currentNode);
            return true;
        }

        // 执行选项动作
        if (option.hasAction()) {
            executeAction(player, option.getAction());
        }

        // 跳转到下一个节点
        if (option.hasNextNode()) {
            DialogueNode nextNode = dialogue.getNode(option.getNextNodeId());
            if (nextNode != null) {
                state.setCurrentNodeId(nextNode.getId());
                displayNode(player, dialogue, nextNode);
            } else {
                endDialogue(player);
            }
        } else {
            endDialogue(player);
        }

        return true;
    }

    /**
     * 显示对话节点
     */
    private void displayNode(Player player, NPCDialogue dialogue, DialogueNode node) {
        MiniMessageService mm = plugin.getMiniMessageService();
        PlayerDialogueState state = activeDialogues.get(player.getUniqueId());
        if (state == null) return;

        // 清空聊天栏效果
        sendChatFrame(player);

        // 显示 NPC 名称
        String npcDisplayName = dialogue.getDisplayName();
        if (npcDisplayName == null) {
            npcDisplayName = plugin.getNpcManager().getNPC(dialogue.getNpcId()) != null 
                ? plugin.getNpcManager().getNPC(dialogue.getNpcId()).getFullDisplayName()
                : "NPC";
        }

        // 发送 NPC 消息 (使用聊天框样式)
        Component npcHeader = Component.text("╔══════════════════════════════╗")
            .color(NamedTextColor.DARK_GRAY);
        Component npcNameLine = Component.text("║ " + stripColor(npcDisplayName) + " »")
            .color(NamedTextColor.GOLD)
            .append(Component.text("                                    ").color(NamedTextColor.DARK_GRAY))
            .append(Component.text("║").color(NamedTextColor.DARK_GRAY));
        
        player.sendMessage(npcHeader);
        player.sendMessage(npcNameLine);

        // 发送对话内容
        String[] lines = node.getNpcText().split("\\n");
        for (String line : lines) {
            Component textLine = mm.colorize("║ <white>" + line);
            player.sendMessage(textLine);
        }

        // 执行节点动作
        if (node.hasAction()) {
            executeAction(player, node.getAction());
        }

        // 显示选项或结束
        if (node.hasOptions()) {
            player.sendMessage(Component.text("╠══════════════════════════════╣").color(NamedTextColor.DARK_GRAY));
            player.sendMessage(Component.text("║ <gray>请选择:").color(NamedTextColor.GRAY));
            
            int index = 1;
            for (DialogueOption option : node.getOptions()) {
                Component optionLine = mm.colorize("║ <yellow>" + index + ". <white>" + option.getText());
                player.sendMessage(optionLine);
                index++;
            }
            
            player.sendMessage(Component.text("║ <gray>输入数字选择选项，或输入 0 结束对话").color(NamedTextColor.GRAY));
        } else if (node.isEndDialogue()) {
            player.sendMessage(Component.text("║ <gray>对话结束").color(NamedTextColor.GRAY));
            player.sendMessage(Component.text("╚══════════════════════════════╝").color(NamedTextColor.DARK_GRAY));
            
            // 延迟结束对话
            Bukkit.getAsyncScheduler().runDelayed(plugin, task -> {
                Bukkit.getScheduler().runTask(plugin, () -> endDialogue(player));
            }, 2000, java.util.concurrent.TimeUnit.MILLISECONDS);
            return;
        }

        player.sendMessage(Component.text("╚══════════════════════════════╝").color(NamedTextColor.DARK_GRAY));

        // 自动继续
        if (node.isAutoContinue()) {
            final PlayerDialogueState currentState = state;
            final String autoNextNodeId = node.getAutoContinueNode();
            Bukkit.getAsyncScheduler().runDelayed(plugin, task -> {
                Bukkit.getScheduler().runTask(plugin, () -> {
                    DialogueNode nextNode = dialogue.getNode(autoNextNodeId);
                    if (nextNode != null) {
                        currentState.setCurrentNodeId(nextNode.getId());
                        displayNode(player, dialogue, nextNode);
                    } else {
                        endDialogue(player);
                    }
                });
            }, node.getAutoContinueDelay() * 50L, java.util.concurrent.TimeUnit.MILLISECONDS);
        }

        // 播放声音
        player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_AMBIENT, 1.0f, 1.0f);
    }

    /**
     * 发送聊天框装饰
     */
    private void sendChatFrame(Player player) {
        // 发送空行来清空可见区域
        for (int i = 0; i < 3; i++) {
            player.sendMessage(Component.empty());
        }
    }

    /**
     * 执行动作
     */
    private void executeAction(Player player, String action) {
        if (action == null || action.isBlank()) {
            return;
        }

        if (action.startsWith("message:")) {
            String message = action.substring("message:".length());
            player.sendMessage(plugin.getMiniMessageService().colorize(message));
        } else if (action.startsWith("command:")) {
            String command = action.substring("command:".length());
            player.performCommand(command);
        } else if (action.startsWith("console:")) {
            String command = action.substring("console:".length()).replace("%player%", player.getName());
            Bukkit.dispatchCommand(Bukkit.getConsoleSender(), command);
        } else if (action.startsWith("menu:")) {
            String menuId = action.substring("menu:".length());
            plugin.getNpcAPI().openMenu(player, menuId);
        } else if (action.startsWith("gdmenu:")) {
            String menuId = action.substring("gdmenu:".length());
            player.performCommand("menu " + menuId);
        } else if (action.startsWith("quest:")) {
            String questCmd = action.substring("quest:".length());
            player.performCommand("quest " + questCmd);
        } else if (action.startsWith("sound:")) {
            String soundName = action.substring("sound:".length());
            try {
                Sound sound = Sound.valueOf(soundName.toUpperCase());
                player.playSound(player.getLocation(), sound, 1.0f, 1.0f);
            } catch (IllegalArgumentException e) {
                plugin.getLogger().warning("无效的声音: " + soundName);
            }
        } else if (action.startsWith("title:")) {
            String titleText = action.substring("title:".length());
            player.showTitle(Title.title(
                plugin.getMiniMessageService().colorize(titleText),
                Component.empty()
            ));
        }
    }

    /**
     * 结束对话
     */
    public void endDialogue(Player player) {
        activeDialogues.remove(player.getUniqueId());
    }

    /**
     * 检查玩家是否在对话中
     */
    public boolean isInDialogue(Player player) {
        PlayerDialogueState state = activeDialogues.get(player.getUniqueId());
        if (state == null) {
            return false;
        }
        if (state.isExpired(DIALOGUE_TIMEOUT)) {
            activeDialogues.remove(player.getUniqueId());
            return false;
        }
        return true;
    }

    /**
     * 获取玩家的对话状态
     */
    public PlayerDialogueState getDialogueState(Player player) {
        return activeDialogues.get(player.getUniqueId());
    }

    /**
     * 获取 NPC 绑定的对话
     */
    public String getDialogueIdForNPC(String npcId) {
        return npcToDialogueMap.get(npcId.toLowerCase());
    }

    /**
     * 检查 NPC 是否有对话
     */
    public boolean hasDialogue(String npcId) {
        return npcToDialogueMap.containsKey(npcId.toLowerCase());
    }

    /**
     * 获取对话
     */
    public NPCDialogue getDialogue(String dialogueId) {
        return dialogues.get(dialogueId.toLowerCase());
    }

    /**
     * 获取所有对话
     */
    public Collection<NPCDialogue> getAllDialogues() {
        return Collections.unmodifiableCollection(dialogues.values());
    }

    /**
     * 获取对话数量
     */
    public int getDialogueCount() {
        return dialogues.size();
    }

    /**
     * 注册对话到 NPC
     */
    public void registerDialogueToNPC(String npcId, String dialogueId) {
        npcToDialogueMap.put(npcId.toLowerCase(), dialogueId.toLowerCase());
    }

    /**
     * 取消注册 NPC 对话
     */
    public void unregisterDialogueFromNPC(String npcId) {
        npcToDialogueMap.remove(npcId.toLowerCase());
    }

    /**
     * 添加对话
     */
    public void addDialogue(NPCDialogue dialogue) {
        dialogues.put(dialogue.getId(), dialogue);
        if (!dialogue.getNpcId().isEmpty()) {
            npcToDialogueMap.put(dialogue.getNpcId().toLowerCase(), dialogue.getId());
        }
        save();
    }

    /**
     * 移除对话
     */
    public boolean removeDialogue(String dialogueId) {
        NPCDialogue dialogue = dialogues.remove(dialogueId.toLowerCase());
        if (dialogue != null && !dialogue.getNpcId().isEmpty()) {
            npcToDialogueMap.remove(dialogue.getNpcId().toLowerCase());
        }
        save();
        return dialogue != null;
    }

    private String stripColor(String text) {
        if (text == null) return "";
        return text.replaceAll("§[0-9a-fk-or]", "").replaceAll("<[^>]+>", "");
    }
}
