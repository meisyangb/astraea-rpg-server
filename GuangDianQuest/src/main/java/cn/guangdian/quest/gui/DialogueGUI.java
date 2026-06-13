package cn.guangdian.quest.gui;

import cn.guangdian.quest.GuangDianQuest;
import cn.guangdian.quest.manager.QuestUnlockManager.QuestStatus;
import cn.guangdian.quest.model.PlayerQuestData;
import cn.guangdian.quest.model.Quest;
import cn.guangdian.quest.model.QuestObjective;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.*;

/**
 * NPC对话GUI
 * 玩家右键NPC → 打开对话GUI → 点击完成对话 → 更新任务进度
 */
public class DialogueGUI implements Listener {

    private final GuangDianQuest plugin;
    private final MiniMessage mm = MiniMessage.miniMessage();

    public DialogueGUI(GuangDianQuest plugin) {
        this.plugin = plugin;
        Bukkit.getPluginManager().registerEvents(this, plugin);
    }

    // ==================== GUI Holder ====================

    private static class DialogueHolder implements InventoryHolder {
        private final String npcId;
        private final String npcName;
        private final String npcRole;
        private final String questId;
        private final int objectiveIndex;

        private DialogueHolder(String npcId, String npcName, String npcRole, String questId, int objectiveIndex) {
            this.npcId = npcId;
            this.npcName = npcName;
            this.npcRole = npcRole;
            this.questId = questId;
            this.objectiveIndex = objectiveIndex;
        }

        public String getNpcId() { return npcId; }
        public String getNpcName() { return npcName; }
        public String getNpcRole() { return npcRole; }
        public String getQuestId() { return questId; }
        public int getObjectiveIndex() { return objectiveIndex; }

        @Override
        public Inventory getInventory() { return null; }
    }

    // ==================== 打开对话GUI ====================

    /**
     * 打开NPC对话GUI
     * @param player 玩家
     * @param npcId NPC ID
     * @param npcName NPC名称
     * @param npcRole NPC角色
     * @param dialogueLines 对话内容
     * @param questId 关联的任务ID (可为null)
     * @param objectiveIndex 目标索引 (可为-1)
     */
    public void openDialogue(Player player, String npcId, String npcName, String npcRole,
                            List<String> dialogueLines, String questId, int objectiveIndex) {
        int size = 27; // 固定3行
        String title = "<dark_gray>对话 - " + npcName;
        Inventory gui = Bukkit.createInventory(
            new DialogueHolder(npcId, npcName, npcRole, questId, objectiveIndex),
            size,
            mm.deserialize(title)
        );

        ItemStack sep = item(Material.GRAY_STAINED_GLASS_PANE, "<dark_gray>─");

        // 填充边框
        for (int i = 0; i < 9; i++) gui.setItem(i, sep);
        for (int i = 18; i < 27; i++) gui.setItem(i, sep);

        // NPC信息 (槽位0)
        gui.setItem(0, item(Material.PLAYER_HEAD, "<yellow><bold>" + npcName,
            "<gray>" + npcRole,
            "",
            "<dark_gray>ID: " + npcId));

        // 对话内容 (槽位10-16)
        int slot = 10;
        for (String line : dialogueLines) {
            if (slot >= 17) break;
            gui.setItem(slot, item(Material.PAPER, line));
            slot++;
        }
        // 填充空白
        for (int i = slot; i < 17; i++) {
            gui.setItem(i, sep);
        }

        // 完成对话按钮 (槽位21)
        if (questId != null && objectiveIndex >= 0) {
            Quest quest = plugin.getQuestManager().getQuest(questId);
            if (quest != null) {
                PlayerQuestData data = plugin.getProgressManager().getPlayerData(player.getUniqueId());
                int[] progress = data.getProgress(questId);
                int current = (progress != null && objectiveIndex < progress.length) ? progress[objectiveIndex] : 0;
                QuestObjective obj = quest.getObjective(objectiveIndex);
                int required = obj != null ? obj.getAmount() : 1;

                if (current >= required) {
                    // 已完成
                    gui.setItem(21, item(Material.GREEN_WOOL, "<green><bold>✔ 已完成对话",
                        "<gray>任务进度: " + current + "/" + required));
                } else {
                    // 可完成
                    gui.setItem(21, item(Material.LIME_WOOL, "<green><bold>完成对话",
                        "<gray>点击完成对话",
                        "<gray>任务进度: " + current + "/" + required));
                }
            }
        } else {
            // 无任务关联，普通对话
            gui.setItem(21, item(Material.LIME_WOOL, "<green><bold>结束对话",
                "<gray>点击关闭"));
        }

        // 关闭按钮 (槽位22)
        gui.setItem(22, item(Material.BARRIER, "<red>关闭"));

        player.openInventory(gui);
        player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_PLING, 1.0f, 1.0f);
    }

    /**
     * 简化版：打开NPC对话GUI（自动从Citizens读取NPC信息和关联任务）
     */
    public void openDialogueForNPC(Player player, String npcId) {
        // 查找关联的任务
        String foundQuestId = null;
        int foundObjectiveIndex = -1;
        List<String> dialogueLines = new ArrayList<>();
        String npcName = npcId;
        String npcRole = "NPC";

        plugin.getLogger().info("[DialogueGUI] 打开对话GUI: npcId=" + npcId + ", player=" + player.getName());

        // 尝试从Citizens获取NPC名称
        try {
            Class<?> citizensApiClass = Class.forName("net.citizensnpcs.api.CitizensAPI");
            Object npcRegistry = citizensApiClass.getMethod("getNPCRegistry").invoke(null);

            // 尝试通过NPC名称查找（npcId可能是NPC名称）
            Object npc = npcRegistry.getClass().getMethod("getNPCByName", String.class).invoke(npcRegistry, npcId);
            if (npc == null) {
                // 尝试通过ID查找
                try {
                    int id = Integer.parseInt(npcId);
                    npc = npcRegistry.getClass().getMethod("getById", int.class).invoke(npcRegistry, id);
                } catch (NumberFormatException ignored) {}
            }

            if (npc != null) {
                npcName = (String) npc.getClass().getMethod("getName").invoke(npc);
                plugin.getLogger().info("[DialogueGUI] 从Citizens获取NPC名称: " + npcName);
            }
        } catch (Exception ignored) {
            // Citizens不可用，使用默认名称
            plugin.getLogger().info("[DialogueGUI] Citizens不可用，使用默认名称");
        }

        PlayerQuestData data = plugin.getProgressManager().getPlayerData(player.getUniqueId());
        plugin.getLogger().info("[DialogueGUI] 玩家活跃任务数: " + data.getActiveQuestIds().size());

        for (String questId : data.getActiveQuestIds()) {
            Quest quest = plugin.getQuestManager().getQuest(questId);
            if (quest == null) continue;

            plugin.getLogger().info("[DialogueGUI] 检查任务: " + questId + ", 目标数: " + quest.getObjectiveCount());

            for (int i = 0; i < quest.getObjectiveCount(); i++) {
                QuestObjective obj = quest.getObjective(i);
                plugin.getLogger().info("[DialogueGUI]   目标" + i + ": type=" + obj.getType() + ", target=" + obj.getTarget());

                if (obj.getType() == QuestObjective.ObjectiveType.TALK) {
                    if (obj.getTarget().equalsIgnoreCase(npcId)) {
                        foundQuestId = questId;
                        foundObjectiveIndex = i;

                        // 获取对话内容
                        if (obj.getDialogue() != null && !obj.getDialogue().isEmpty()) {
                            dialogueLines = obj.getDialogue();
                            plugin.getLogger().info("[DialogueGUI] 找到对话内容: " + dialogueLines.size() + " 行");
                        } else {
                            dialogueLines.add("<gray>与 " + npcName + " 的对话...");
                            plugin.getLogger().info("[DialogueGUI] 使用默认对话内容");
                        }
                        break;
                    }
                }
            }
            if (foundQuestId != null) break;
        }

        // 如果没找到关联任务，使用默认对话
        if (dialogueLines.isEmpty()) {
            dialogueLines.add("<gray>你好，旅行者。");
            dialogueLines.add("<gray>有什么可以帮助你的吗？");
            plugin.getLogger().info("[DialogueGUI] 未找到关联任务，使用默认问候语");
        }

        openDialogue(player, npcId, npcName, npcRole, dialogueLines, foundQuestId, foundObjectiveIndex);
    }

    // ==================== 事件处理 ====================

    @EventHandler(priority = EventPriority.HIGH)
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getInventory().getHolder() instanceof DialogueHolder holder)) return;
        if (!(event.getWhoClicked() instanceof Player player)) return;

        int slot = event.getRawSlot();
        if (slot < 0 || slot >= event.getInventory().getSize()) {
            event.setCancelled(true);
            return;
        }

        event.setCancelled(true);

        ItemStack clicked = event.getCurrentItem();
        if (clicked == null || clicked.getType() == Material.AIR) return;

        // 关闭按钮
        if (clicked.getType() == Material.BARRIER) {
            player.closeInventory();
            return;
        }

        // 完成对话按钮
        if (clicked.getType() == Material.LIME_WOOL && slot == 21) {
            String questId = holder.getQuestId();
            int objIndex = holder.getObjectiveIndex();

            if (questId != null && objIndex >= 0) {
                // 更新任务进度
                int newProgress = plugin.getProgressManager().incrementProgress(
                    player.getUniqueId(), questId, objIndex, 1);

                Quest quest = plugin.getQuestManager().getQuest(questId);
                QuestObjective obj = quest != null ? quest.getObjective(objIndex) : null;
                int required = obj != null ? obj.getAmount() : 1;

                if (newProgress >= required) {
                    player.sendMessage(mm.deserialize(
                        "<green>✔ 与 <yellow>" + holder.getNpcName() + " <green>的对话已完成！"
                    ));
                    player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_PLING, 1.0f, 2.0f);
                }

                player.closeInventory();
            } else {
                // 无任务关联，直接关闭
                player.closeInventory();
            }
        }

        // 已完成的对话按钮
        if (clicked.getType() == Material.GREEN_WOOL && slot == 21) {
            player.sendMessage(mm.deserialize("<gray>你已经完成了这次对话。"));
            player.closeInventory();
        }
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onInventoryDrag(InventoryDragEvent event) {
        if (event.getInventory().getHolder() instanceof DialogueHolder) {
            event.setCancelled(true);
        }
    }

    // ==================== 工具方法 ====================

    private ItemStack item(Material mat, String name, String... loreLines) {
        ItemStack item = new ItemStack(mat);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.displayName(mm.deserialize(name));
            List<Component> lore = new ArrayList<>();
            for (String line : loreLines) {
                if (line != null && !line.isEmpty()) {
                    lore.add(mm.deserialize(line));
                }
            }
            meta.lore(lore);
            item.setItemMeta(meta);
        }
        return item;
    }
}
