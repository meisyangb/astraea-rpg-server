package cn.guangdian.quest.gui;

import cn.guangdian.quest.GuangDianQuest;
import cn.guangdian.quest.manager.QuestUnlockManager.QuestStatus;
import cn.guangdian.quest.model.PlayerQuestData;
import cn.guangdian.quest.model.Quest;
import cn.guangdian.quest.model.QuestLine;
import cn.guangdian.quest.model.QuestObjective;
import cn.guangdian.quest.model.QuestType;
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
 * 任务GUI菜单系统
 * 使用 InventoryHolder 模式，参考 GuangDianMenu 实现
 */
public class QuestGUIManager implements Listener {

    private final GuangDianQuest plugin;
    private final MiniMessage mm = MiniMessage.miniMessage();

    public QuestGUIManager(GuangDianQuest plugin) {
        this.plugin = plugin;
        Bukkit.getPluginManager().registerEvents(this, plugin);
    }

    // ==================== GUI类型 ====================

    private enum GUIType {
        MAIN_MENU, ACTIVE_LIST, AVAILABLE_LIST, QUEST_LINES, MAIN_LINE, DAILY_LIST, SIDE_LIST, QUEST_DETAIL
    }

    // ==================== GUI Holder ====================

    private static class QuestMenuHolder implements InventoryHolder {
        private final GUIType type;
        private final String data; // 任务ID等数据

        private QuestMenuHolder(GUIType type, String data) {
            this.type = type;
            this.data = data;
        }

        public GUIType getType() { return type; }
        public String getData() { return data; }

        @Override
        public Inventory getInventory() { return null; }
    }

    // ==================== 主菜单 ====================

    public void openMainMenu(Player player) {
        Inventory gui = createInventory(GUIType.MAIN_MENU, null, 27, "<dark_gray>任务日志");
        ItemStack sep = item(Material.GRAY_STAINED_GLASS_PANE, "<dark_gray>─");

        for (int i = 0; i < 27; i++) gui.setItem(i, sep);

        PlayerQuestData data = plugin.getProgressManager().getPlayerData(player.getUniqueId());
        int active = data.getActiveQuestCount();

        gui.setItem(11, item(Material.WRITABLE_BOOK, "<yellow><bold>进行中",
            "<gray>当前: <white>" + active + "/" + plugin.getMaxActiveQuests(),
            "", "<yellow>点击查看"));

        List<String> available = getAvailableQuests(player);
        gui.setItem(13, item(Material.LIME_WOOL, "<green><bold>可接取",
            "<gray>可接取: <white>" + available.size() + " 个",
            "", "<yellow>点击查看"));

        gui.setItem(15, item(Material.BOOKSHELF, "<gold><bold>任务线",
            "<gray>查看主线/支线进度",
            "", "<yellow>点击查看"));

        gui.setItem(22, item(Material.BARRIER, "<red>关闭"));

        player.openInventory(gui);
    }

    // ==================== 进行中任务列表 ====================

    private void openActiveQuests(Player player) {
        PlayerQuestData data = plugin.getProgressManager().getPlayerData(player.getUniqueId());
        List<String> activeIds = new ArrayList<>(data.getActiveQuestIds());

        int size = calcSize(activeIds.size());
        Inventory gui = createInventory(GUIType.ACTIVE_LIST, null, size, "<dark_gray>进行中的任务");
        fillBorder(gui, size);

        int slot = 9;
        for (String questId : activeIds) {
            if (slot >= size - 9) break;
            Quest quest = plugin.getQuestManager().getQuest(questId);
            if (quest == null) continue;

            int[] progress = data.getProgress(questId);
            int percent = calcPercent(progress, quest);
            boolean canComplete = plugin.getQuestManager().canComplete(player.getUniqueId(), questId);

            Material mat = canComplete ? Material.GOLD_BLOCK : Material.YELLOW_WOOL;
            List<String> lore = new ArrayList<>();
            lore.add("<gray>进度: " + percent + "%");
            lore.add("<gray>" + buildBar(percent));
            if (quest.getObjectiveCount() > 0 && progress != null) {
                QuestObjective obj = quest.getObjective(0);
                int cur = Math.min(progress[0], obj.getAmount());
                lore.add("<white>" + obj.getDescription() + " <gray>" + cur + "/" + obj.getAmount());
            }
            lore.add("");
            lore.add(canComplete ? "<green>点击完成" : "<yellow>点击查看详情");

            gui.setItem(slot, item(mat, quest.getType().getPrefix() + " " + quest.getName(), lore.toArray(new String[0])));
            slot++;
        }

        gui.setItem(size - 5, item(Material.ARROW, "<yellow>返回"));
        gui.setItem(size - 1, item(Material.BARRIER, "<red>关闭"));

        player.openInventory(gui);
    }

    // ==================== 可接取任务列表 ====================

    private void openAvailableQuests(Player player) {
        List<String> available = getAvailableQuests(player);

        int size = calcSize(available.size());
        Inventory gui = createInventory(GUIType.AVAILABLE_LIST, null, size, "<dark_gray>可接取的任务");
        fillBorder(gui, size);

        int slot = 9;
        for (String questId : available) {
            if (slot >= size - 9) break;
            Quest quest = plugin.getQuestManager().getQuest(questId);
            if (quest == null) continue;

            List<String> lore = new ArrayList<>();
            lore.add("<gray>" + quest.getType().getDisplayName());
            if (quest.getObjectiveCount() > 0) {
                lore.add("<white>" + quest.getObjective(0).getDescription());
            }
            lore.add("");
            lore.add("<green>点击接取");

            gui.setItem(slot, item(Material.LIME_WOOL, quest.getType().getPrefix() + " " + quest.getName(), lore.toArray(new String[0])));
            slot++;
        }

        gui.setItem(size - 5, item(Material.ARROW, "<yellow>返回"));
        gui.setItem(size - 1, item(Material.BARRIER, "<red>关闭"));

        player.openInventory(gui);
    }

    // ==================== 任务线列表 ====================

    // ==================== 任务线列表（按门派过滤） ====================

    private void openQuestLines(Player player) {
        UUID pid = player.getUniqueId();
        
        // 收集该玩家可见的任务线
        List<QuestLine> availableLines = new ArrayList<>();
        for (QuestLine line : plugin.getQuestLineManager().getAllQuestLines()) {
            String lineSect = line.getSect();
            if (lineSect == null || lineSect.isEmpty()) {
                availableLines.add(line); // 通用任务线，所有人可见
            } else {
                String playerSect = plugin.getQuestUnlockManager().getPlayerSect(pid);
                if (lineSect.equalsIgnoreCase(playerSect)) {
                    availableLines.add(line); // 门派专属任务线
                }
            }
        }
        
        int size = calcSize(availableLines.size());
        Inventory gui = createInventory(GUIType.QUEST_LINES, null, size, "<dark_gray>任务线");
        fillBorder(gui, size);

        int slot = 9;
        for (QuestLine line : availableLines) {
            if (slot >= size - 9) break;
            
            // 用不同材质区分
            Material mat = Material.NETHER_STAR;
            String lineName = line.getName();
            if (lineName.contains("鬼王")) mat = Material.SOUL_SAND;
            else if (lineName.contains("青云")) mat = Material.DIAMOND_SWORD;
            else if (lineName.contains("合欢")) mat = Material.NETHER_STAR;
            else if (lineName.contains("天音")) mat = Material.GOLDEN_APPLE;
            else if (lineName.contains("焚香")) mat = Material.BLAZE_POWDER;
            else if (lineName.contains("长生")) mat = Material.BREWING_STAND;
            else if (lineName.contains("圣门")) mat = Material.NETHER_STAR;
            
            List<String> lore = new ArrayList<>();
            lore.add("<gray>" + line.getDescription());
            if (line.getChapters() != null && !line.getChapters().isEmpty()) {
                lore.add("");
                for (String ch : line.getChapters()) {
                    lore.add(ch);
                }
            }
            lore.add("");
            lore.add("<yellow>点击查看详情");
            
            gui.setItem(slot, item(mat, line.getName(), lore.toArray(new String[0])));
            slot++;
        }

        gui.setItem(size - 5, item(Material.ARROW, "<yellow>返回"));
        gui.setItem(size - 1, item(Material.BARRIER, "<red>关闭"));

        player.openInventory(gui);
    }

    // ==================== 主线任务进度（按任务线） ====================

    private void openMainQuestLine(Player player, String questLineId) {
        List<Quest> mainQuests = new ArrayList<>();
        UUID pid = player.getUniqueId();
        for (Quest q : plugin.getQuestRepository().getAllQuests()) {
            if (q.getType() == QuestType.MAIN && questLineId.equals(q.getQuestLine())) {
                // 门派过滤
                if (plugin.getQuestUnlockManager().matchesSect(pid, q)) {
                    mainQuests.add(q);
                }
            }
        }
        mainQuests.sort(Comparator.comparingInt(Quest::getOrder));

        plugin.getLogger().info("[DEBUG] openMainQuestLine: questLineId=" + questLineId + ", quests=" + mainQuests.size());
        for (Quest q : mainQuests) {
            plugin.getLogger().info("[DEBUG]   - " + q.getId() + " (order=" + q.getOrder() + ")");
        }

        QuestLine line = plugin.getQuestLineManager().getQuestLine(questLineId);
        String title = line != null ? line.getName() : questLineId;

        int size = calcSize(mainQuests.size());
        Inventory gui = createInventory(GUIType.MAIN_LINE, questLineId, size, "<dark_gray>" + title);
        fillBorder(gui, size);

        int slot = 9;
        for (Quest quest : mainQuests) {
            if (slot >= size - 9) break;
            QuestStatus status = plugin.getQuestUnlockManager().getStatus(pid, quest.getId());

            Material mat;
            List<String> lore = new ArrayList<>();

            switch (status) {
                case LOCKED -> {
                    mat = Material.GRAY_WOOL;
                    String prevName = plugin.getQuestUnlockManager().getPreviousQuestName(quest);
                    lore.add("<red>🔒 未解锁");
                    if (prevName != null) lore.add("<gray>需完成: <yellow>" + prevName);
                }
                case CAN_ACCEPT -> {
                    mat = Material.LIME_WOOL;
                    lore.add("<green>🔓 可接取");
                    lore.add("<green>点击接取");
                }
                case IN_PROGRESS -> {
                    mat = Material.YELLOW_WOOL;
                    int[] prog = plugin.getProgressManager().getPlayerData(pid).getProgress(quest.getId());
                    int pct = calcPercent(prog, quest);
                    lore.add("<yellow>⏳ 进行中 " + pct + "%");
                    lore.add("<yellow>点击查看详情");
                }
                case CAN_COMPLETE -> {
                    mat = Material.GOLD_BLOCK;
                    lore.add("<gold>✓ 可完成");
                    lore.add("<green>点击完成");
                }
                case COMPLETED -> {
                    mat = Material.GREEN_WOOL;
                    lore.add("<green>✔ 已完成");
                }
                default -> mat = Material.WHITE_WOOL;
            }

            gui.setItem(slot, item(mat, quest.getType().getPrefix() + " " + quest.getName(), lore.toArray(new String[0])));
            slot++;
        }

        gui.setItem(size - 5, item(Material.ARROW, "<yellow>返回"));
        gui.setItem(size - 1, item(Material.BARRIER, "<red>关闭"));

        player.openInventory(gui);
    }

    // ==================== 每日任务列表 ====================

    private void openDailyQuests(Player player) {
        List<String> dailyIds = plugin.getDailyManager().getDailyQuests(player.getUniqueId());

        int size = calcSize(dailyIds.size());
        Inventory gui = createInventory(GUIType.DAILY_LIST, null, size, "<dark_gray>每日任务");
        fillBorder(gui, size);

        PlayerQuestData data = plugin.getProgressManager().getPlayerData(player.getUniqueId());
        int slot = 9;
        for (String questId : dailyIds) {
            if (slot >= size - 9) break;
            Quest quest = plugin.getQuestManager().getQuest(questId);
            if (quest == null) continue;

            boolean active = data.isQuestActive(questId);
            boolean completed = data.isDailyQuestCompleted(questId);
            boolean canComplete = active && plugin.getQuestManager().canComplete(player.getUniqueId(), questId);

            Material mat;
            List<String> lore = new ArrayList<>();
            lore.add("<yellow>每日任务");
            if (quest.getObjectiveCount() > 0) {
                lore.add("<white>" + quest.getObjective(0).getDescription());
            }
            lore.add("");

            if (completed) {
                mat = Material.GREEN_WOOL;
                lore.add("<green>✔ 今日已完成");
            } else if (canComplete) {
                mat = Material.GOLD_BLOCK;
                lore.add("<gold>✓ 可完成");
                lore.add("<green>点击完成");
            } else if (active) {
                mat = Material.YELLOW_WOOL;
                int[] progress = data.getProgress(questId);
                int percent = calcPercent(progress, quest);
                lore.add("<yellow>⏳ 进行中 " + percent + "%");
                lore.add("<yellow>点击查看详情");
            } else {
                mat = Material.LIME_WOOL;
                lore.add("<green>点击接取");
            }

            gui.setItem(slot, item(mat, quest.getName(), lore.toArray(new String[0])));
            slot++;
        }

        gui.setItem(size - 5, item(Material.ARROW, "<yellow>返回"));
        gui.setItem(size - 1, item(Material.BARRIER, "<red>关闭"));

        player.openInventory(gui);
    }

    // ==================== 支线任务列表 ====================

    private void openSideQuests(Player player) {
        List<Quest> sideQuests = new ArrayList<>();
        for (Quest q : plugin.getQuestRepository().getAllQuests()) {
            if (q.getType() == QuestType.SIDE) {
                sideQuests.add(q);
            }
        }

        int size = calcSize(sideQuests.size());
        Inventory gui = createInventory(GUIType.SIDE_LIST, null, size, "<dark_gray>支线任务");
        fillBorder(gui, size);

        UUID pid = player.getUniqueId();
        int slot = 9;
        for (Quest quest : sideQuests) {
            if (slot >= size - 9) break;
            QuestStatus status = plugin.getQuestUnlockManager().getStatus(pid, quest.getId());

            Material mat;
            List<String> lore = new ArrayList<>();
            lore.add("<aqua>支线任务");
            if (quest.getObjectiveCount() > 0) {
                lore.add("<white>" + quest.getObjective(0).getDescription());
            }
            lore.add("");

            switch (status) {
                case LOCKED -> {
                    mat = Material.GRAY_WOOL;
                    lore.add("<red>🔒 未解锁");
                }
                case CAN_ACCEPT -> {
                    mat = Material.LIME_WOOL;
                    lore.add("<green>点击接取");
                }
                case IN_PROGRESS -> {
                    mat = Material.YELLOW_WOOL;
                    int[] prog = plugin.getProgressManager().getPlayerData(pid).getProgress(quest.getId());
                    int pct = calcPercent(prog, quest);
                    lore.add("<yellow>⏳ 进行中 " + pct + "%");
                    lore.add("<yellow>点击查看详情");
                }
                case CAN_COMPLETE -> {
                    mat = Material.GOLD_BLOCK;
                    lore.add("<gold>✓ 可完成");
                    lore.add("<green>点击完成");
                }
                case COMPLETED -> {
                    mat = Material.GREEN_WOOL;
                    lore.add("<green>✔ 已完成");
                }
                default -> mat = Material.WHITE_WOOL;
            }

            gui.setItem(slot, item(mat, quest.getName(), lore.toArray(new String[0])));
            slot++;
        }

        gui.setItem(size - 5, item(Material.ARROW, "<yellow>返回"));
        gui.setItem(size - 1, item(Material.BARRIER, "<red>关闭"));

        player.openInventory(gui);
    }

    // ==================== 任务详情 ====================

    private void openQuestDetail(Player player, String questId) {
        Quest quest = plugin.getQuestManager().getQuest(questId);
        if (quest == null) return;

        UUID pid = player.getUniqueId();
        QuestStatus status = plugin.getQuestUnlockManager().getStatus(pid, questId);
        PlayerQuestData data = plugin.getProgressManager().getPlayerData(pid);
        int[] progress = data.isQuestActive(questId) ? data.getProgress(questId) : null;

        Inventory gui = createInventory(GUIType.QUEST_DETAIL, questId, 45, "<dark_gray>" + quest.getName());
        ItemStack sep = item(Material.GRAY_STAINED_GLASS_PANE, "<dark_gray>─");

        gui.setItem(0, item(getTypeMat(quest.getType()), quest.getType().getPrefix() + " " + quest.getName(),
            "<gray>" + quest.getType().getDisplayName(),
            quest.getRequiredLevel() > 0 ? "<gray>等级: <red>Lv." + quest.getRequiredLevel() : ""));
        for (int i = 1; i < 9; i++) gui.setItem(i, sep);

        int dSlot = 9;
        for (String line : quest.getDescription()) {
            if (dSlot >= 18) break;
            gui.setItem(dSlot, item(Material.PAPER, "<white>" + line));
            dSlot++;
        }
        for (int i = dSlot; i < 18; i++) gui.setItem(i, sep);

        gui.setItem(18, item(Material.WRITABLE_BOOK, "<green><bold>目标"));
        for (int i = 19; i < 27; i++) gui.setItem(i, sep);

        int oSlot = 27;
        for (int i = 0; i < quest.getObjectiveCount() && oSlot < 36; i++) {
            QuestObjective obj = quest.getObjective(i);
            int cur = (progress != null && i < progress.length) ? progress[i] : 0;
            boolean done = cur >= obj.getAmount();
            String icon = done ? "<green>✔" : "<gray>○";
            Material m = done ? Material.LIME_WOOL : Material.WHITE_WOOL;
            gui.setItem(oSlot, item(m, icon + " " + obj.getDescription() + " <gray>" + cur + "/" + obj.getAmount()));
            oSlot++;
        }
        for (int i = oSlot; i < 36; i++) gui.setItem(i, sep);

        for (int i = 36; i < 45; i++) gui.setItem(i, sep);

        switch (status) {
            case LOCKED -> {
                String prevName = plugin.getQuestUnlockManager().getPreviousQuestName(quest);
                gui.setItem(40, item(Material.RED_WOOL, "<red><bold>🔒 未解锁",
                    prevName != null ? "<gray>需完成: <yellow>" + prevName : "<gray>前置任务未完成"));
            }
            case CAN_ACCEPT -> gui.setItem(40, item(Material.LIME_WOOL, "<green><bold>接受任务", "<gray>点击接取"));
            case IN_PROGRESS -> {
                gui.setItem(38, item(Material.RED_WOOL, "<red><bold>放弃任务", "<red>⚠ 进度清零"));
                gui.setItem(42, item(Material.WRITABLE_BOOK, "<aqua>进行中...", "<gray>完成所有目标后可提交"));
            }
            case CAN_COMPLETE -> gui.setItem(40, item(Material.GOLD_BLOCK, "<gold><bold>完成任务", "<green>点击领取奖励"));
            case COMPLETED -> gui.setItem(40, item(Material.GREEN_WOOL, "<green><bold>✔ 已完成"));
        }

        gui.setItem(36, item(Material.ARROW, "<yellow>返回"));
        gui.setItem(44, item(Material.BARRIER, "<red>关闭"));

        player.openInventory(gui);
    }

    // ==================== 事件处理 ====================

    @EventHandler(priority = EventPriority.HIGH)
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getInventory().getHolder() instanceof QuestMenuHolder holder)) return;
        if (!(event.getWhoClicked() instanceof Player player)) return;

        int slot = event.getRawSlot();
        // 点击玩家背包不处理，但取消事件
        if (slot < 0 || slot >= event.getInventory().getSize()) {
            event.setCancelled(true);
            return;
        }

        event.setCancelled(true);

        ItemStack clicked = event.getCurrentItem();
        if (clicked == null || clicked.getType() == Material.AIR) return;

        switch (holder.getType()) {
            case MAIN_MENU -> handleMainMenu(player, clicked);
            case ACTIVE_LIST -> handleActiveList(player, slot, clicked);
            case AVAILABLE_LIST -> handleAvailableList(player, slot, clicked);
            case QUEST_LINES -> handleQuestLines(player, slot, clicked);
            case MAIN_LINE -> handleMainLine(player, slot, clicked);
            case DAILY_LIST -> handleDailyList(player, slot, clicked);
            case SIDE_LIST -> handleSideList(player, slot, clicked);
            case QUEST_DETAIL -> handleDetail(player, holder.getData(), clicked);
        }
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onInventoryDrag(InventoryDragEvent event) {
        if (event.getInventory().getHolder() instanceof QuestMenuHolder) {
            event.setCancelled(true);
        }
    }

    private void handleMainMenu(Player player, ItemStack clicked) {
        Material mat = clicked.getType();
        if (mat == Material.WRITABLE_BOOK) openActiveQuests(player);
        else if (mat == Material.LIME_WOOL) openAvailableQuests(player);
        else if (mat == Material.BOOKSHELF) openQuestLines(player);
        else if (mat == Material.BARRIER) player.closeInventory();
    }

    private void handleActiveList(Player player, int slot, ItemStack clicked) {
        if (clicked.getType() == Material.BARRIER) { player.closeInventory(); return; }
        if (clicked.getType() == Material.ARROW) { openMainMenu(player); return; }
        if (slot < 9) return;

        PlayerQuestData data = plugin.getProgressManager().getPlayerData(player.getUniqueId());
        List<String> ids = new ArrayList<>(data.getActiveQuestIds());
        int idx = slot - 9;
        if (idx < ids.size()) openQuestDetail(player, ids.get(idx));
    }

    private void handleAvailableList(Player player, int slot, ItemStack clicked) {
        if (clicked.getType() == Material.BARRIER) { player.closeInventory(); return; }
        if (clicked.getType() == Material.ARROW) { openMainMenu(player); return; }
        if (slot < 9) return;

        List<String> available = getAvailableQuests(player);
        int idx = slot - 9;
        if (idx < available.size()) openQuestDetail(player, available.get(idx));
    }

    private void handleQuestLines(Player player, int slot, ItemStack clicked) {
        Material mat = clicked.getType();
        if (mat == Material.ARROW) { openMainMenu(player); return; }
        if (mat == Material.BARRIER) { player.closeInventory(); return; }
        if (slot < 9) return;
        
        // 从当前打开的 GUI 中重建任务线列表
        UUID pid = player.getUniqueId();
        List<QuestLine> availableLines = new ArrayList<>();
        for (QuestLine line : plugin.getQuestLineManager().getAllQuestLines()) {
            String lineSect = line.getSect();
            if (lineSect == null || lineSect.isEmpty()) {
                availableLines.add(line);
            } else {
                String playerSect = plugin.getQuestUnlockManager().getPlayerSect(pid);
                if (lineSect.equalsIgnoreCase(playerSect)) {
                    availableLines.add(line);
                }
            }
        }
        
        int idx = slot - 9;
        if (idx < availableLines.size()) {
            openMainQuestLine(player, availableLines.get(idx).getId());
        }
    }

    private void handleMainLine(Player player, int slot, ItemStack clicked) {
        if (clicked.getType() == Material.BARRIER) { player.closeInventory(); return; }
        if (clicked.getType() == Material.ARROW) { openQuestLines(player); return; }
        if (slot < 9) return;

        // 从 holder 获取任务线 ID
        String questLineId = null;
        if (player.getOpenInventory().getTopInventory().getHolder() instanceof QuestMenuHolder holder) {
            questLineId = holder.getData();
        }
        
        plugin.getLogger().info("[DEBUG] handleMainLine: questLineId=" + questLineId + ", slot=" + slot);

        List<Quest> mainQuests = new ArrayList<>();
        UUID pid = player.getUniqueId();
        for (Quest q : plugin.getQuestRepository().getAllQuests()) {
            if (q.getType() == QuestType.MAIN && questLineId != null && questLineId.equals(q.getQuestLine())) {
                if (plugin.getQuestUnlockManager().matchesSect(pid, q)) {
                    mainQuests.add(q);
                }
            }
        }
        mainQuests.sort(Comparator.comparingInt(Quest::getOrder));
        
        plugin.getLogger().info("[DEBUG] mainQuests count: " + mainQuests.size());

        int idx = slot - 9;
        plugin.getLogger().info("[DEBUG] idx=" + idx);
        
        if (idx < mainQuests.size()) {
            Quest quest = mainQuests.get(idx);
            plugin.getLogger().info("[DEBUG] opening quest detail: " + quest.getId());
            try {
                openQuestDetail(player, quest.getId());
                plugin.getLogger().info("[DEBUG] openQuestDetail completed successfully");
            } catch (Exception e) {
                plugin.getLogger().severe("[DEBUG] openQuestDetail failed: " + e.getMessage());
                e.printStackTrace();
            }
        } else {
            plugin.getLogger().info("[DEBUG] idx out of bounds: idx=" + idx + ", size=" + mainQuests.size());
        }
    }

    private void handleDailyList(Player player, int slot, ItemStack clicked) {
        if (clicked.getType() == Material.BARRIER) { player.closeInventory(); return; }
        if (clicked.getType() == Material.ARROW) { openQuestLines(player); return; }
        if (slot < 9) return;

        List<String> dailyIds = plugin.getDailyManager().getDailyQuests(player.getUniqueId());
        int idx = slot - 9;
        if (idx < dailyIds.size()) openQuestDetail(player, dailyIds.get(idx));
    }

    private void handleSideList(Player player, int slot, ItemStack clicked) {
        if (clicked.getType() == Material.BARRIER) { player.closeInventory(); return; }
        if (clicked.getType() == Material.ARROW) { openQuestLines(player); return; }
        if (slot < 9) return;

        List<Quest> sideQuests = new ArrayList<>();
        for (Quest q : plugin.getQuestRepository().getAllQuests()) {
            if (q.getType() == QuestType.SIDE) {
                sideQuests.add(q);
            }
        }

        int idx = slot - 9;
        if (idx < sideQuests.size()) openQuestDetail(player, sideQuests.get(idx).getId());
    }

    private void handleDetail(Player player, String questId, ItemStack clicked) {
        if (questId == null) return;

        Material mat = clicked.getType();
        if (mat == Material.BARRIER) { player.closeInventory(); return; }
        if (mat == Material.ARROW) { openMainMenu(player); return; }

        QuestStatus status = plugin.getQuestUnlockManager().getStatus(player.getUniqueId(), questId);

        if (status == QuestStatus.CAN_ACCEPT && mat == Material.LIME_WOOL) {
            if (plugin.getQuestManager().acceptQuest(player.getUniqueId(), questId)) {
                Quest q = plugin.getQuestManager().getQuest(questId);
                player.sendMessage(GuangDianQuest.color("<green>✔ 已接取: <gold>" + (q != null ? q.getName() : questId)));
                player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_PLING, 1.0f, 1.5f);
                openQuestDetail(player, questId);
            }
        }

        if (status == QuestStatus.IN_PROGRESS && mat == Material.RED_WOOL) {
            if (plugin.getQuestManager().abandonQuest(player.getUniqueId(), questId)) {
                Quest q = plugin.getQuestManager().getQuest(questId);
                player.sendMessage(GuangDianQuest.color("<red>已放弃: <gold>" + (q != null ? q.getName() : questId)));
                player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_BASS, 1.0f, 0.5f);
                openMainMenu(player);
            }
        }

        if (status == QuestStatus.CAN_COMPLETE && mat == Material.GOLD_BLOCK) {
            if (plugin.getQuestManager().completeQuest(player.getUniqueId(), questId)) {
                Quest q = plugin.getQuestManager().getQuest(questId);
                player.sendMessage(GuangDianQuest.color("<gold>✔ 任务完成: " + (q != null ? q.getName() : questId)));
                player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_PLING, 1.0f, 1.0f);
                plugin.getQuestUnlockManager().onQuestComplete(player, questId);
                openMainMenu(player);
            }
        }
    }

    // ==================== 工具方法 ====================

    private Inventory createInventory(GUIType type, String data, int size, String title) {
        return Bukkit.createInventory(new QuestMenuHolder(type, data), size, mm.deserialize(title));
    }

    private void fillBorder(Inventory gui, int size) {
        ItemStack sep = item(Material.GRAY_STAINED_GLASS_PANE, "<dark_gray>─");
        for (int i = 0; i < 9; i++) gui.setItem(i, sep);
        for (int i = size - 9; i < size; i++) gui.setItem(i, sep);
    }

    private int calcSize(int itemCount) {
        return Math.min(54, Math.max(27, ((itemCount + 2) / 9 + 2) * 9));
    }

    private List<String> getAvailableQuests(Player player) {
        UUID pid = player.getUniqueId();
        PlayerQuestData data = plugin.getProgressManager().getPlayerData(pid);
        List<String> result = new ArrayList<>();

        for (Quest quest : plugin.getQuestRepository().getAllQuests()) {
            String id = quest.getId();
            if (data.isQuestActive(id) || data.isQuestCompleted(id)) continue;
            if (!plugin.getQuestUnlockManager().isQuestUnlocked(pid, id)) continue;
            if (quest.getRequiredLevel() > 0 && player.getLevel() < quest.getRequiredLevel()) continue;
            result.add(id);
        }
        return result;
    }

    private ItemStack item(Material mat, String name, String... loreLines) {
        ItemStack item = new ItemStack(mat);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.displayName(mm.deserialize(name));
            List<Component> lore = new ArrayList<>();
            for (String line : loreLines) lore.add(mm.deserialize(line));
            meta.lore(lore);
            item.setItemMeta(meta);
        }
        return item;
    }

    private Material getTypeMat(QuestType type) {
        return switch (type) {
            case MAIN -> Material.NETHER_STAR;
            case SIDE -> Material.BOOK;
            case DAILY -> Material.CLOCK;
            case ACHIEVEMENT -> Material.DIAMOND;
        };
    }

    private String buildBar(int percent) {
        StringBuilder bar = new StringBuilder();
        for (int i = 0; i < 10; i++) bar.append(i < percent / 10 ? '█' : '░');
        return bar.toString();
    }

    private int calcPercent(int[] progress, Quest quest) {
        if (progress == null || quest.getObjectiveCount() == 0) return 0;
        int total = 0, done = 0;
        for (int i = 0; i < quest.getObjectiveCount(); i++) {
            total += quest.getObjective(i).getAmount();
            done += Math.min(progress[i], quest.getObjective(i).getAmount());
        }
        return total > 0 ? (done * 100 / total) : 0;
    }
}
