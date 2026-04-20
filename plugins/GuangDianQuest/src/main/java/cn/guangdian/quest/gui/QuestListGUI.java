package cn.guangdian.quest.gui;

import cn.guangdian.quest.GuangDianQuest;
import cn.guangdian.quest.model.PlayerQuestData;
import cn.guangdian.quest.model.Quest;
import cn.guangdian.rpgcore.gui.GUI;
import cn.guangdian.rpgcore.gui.GUIBuilder;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.List;

/**
 * 任务列表 GUI
 * 显示进行中和可接取的任务列表
 * 导航: 主页 -> 任务列表 -> 任务详情
 */
public class QuestListGUI extends QuestGUI {

    public enum ListType {
        ACTIVE("进行中任务", "任务中心 > 进行中任务"),
        AVAILABLE("可接取任务", "任务中心 > 可接取任务"),
        ALL("所有任务", "任务中心 > 所有任务");

        private final String displayName;
        private final String breadcrumb;

        ListType(String displayName, String breadcrumb) {
            this.displayName = displayName;
            this.breadcrumb = breadcrumb;
        }

        public String getDisplayName() {
            return displayName;
        }

        public String getBreadcrumb() {
            return breadcrumb;
        }
    }

    private final ListType listType;
    private final QuestItemFactory itemFactory;
    private final QuestNavigation nav;
    private int currentPage = 1;
    private static final int ITEMS_PER_PAGE = 21; // 3行 * 7列

    public QuestListGUI(GuangDianQuest plugin, Player player, ListType listType) {
        super(plugin, player);
        this.listType = listType;
        this.itemFactory = new QuestItemFactory(plugin);
        this.nav = new QuestNavigation(plugin, player);
    }

    public QuestListGUI(GuangDianQuest plugin, Player player, ListType listType, int page) {
        this(plugin, player, listType);
        this.currentPage = Math.max(1, page);
    }

    @Override
    public void open() {
        build();
        if (gui != null) {
            gui.open(player);
        }
    }

    @Override
    protected void build() {
        List<Quest> quests = getQuests();
        int totalPages = (int) Math.ceil(quests.size() / (double) ITEMS_PER_PAGE);
        if (totalPages == 0) totalPages = 1;
        if (currentPage > totalPages) currentPage = totalPages;

        String title = "<gold>" + listType.getDisplayName() + " <gray>[" + currentPage + "/" + totalPages + "]";
        GUIBuilder builder = createBuilder(title, 6);

        // 边框 (0-8, 36-44, 以及左右列)
        for (int i = 0; i < 54; i++) {
            if (i < 9 || (i >= 36 && i < 45) || i % 9 == 0 || i % 9 == 8) {
                builder.setItem(i, createBorder());
            }
        }

        // 面包屑导航 (slot 4)
        builder.setItem(4, createItem(Material.PAPER, "<yellow>" + listType.getBreadcrumb(), List.of(
            "<gray>点击返回任务中心主页"
        )), nav.getHomeHandler());

        // 填充任务物品 (slot 10-34, 3行 * 7列)
        int startIndex = (currentPage - 1) * ITEMS_PER_PAGE;
        int endIndex = Math.min(startIndex + ITEMS_PER_PAGE, quests.size());

        int[] slots = getItemSlots();
        for (int i = startIndex; i < endIndex; i++) {
            Quest quest = quests.get(i);
            int slot = slots[i - startIndex];
            ItemStack questItem = itemFactory.createQuestItem(quest, player);
            builder.setItem(slot, questItem, event -> handleQuestClick(quest));
        }

        // 空状态提示
        if (quests.isEmpty()) {
            ItemStack emptyItem = createItem(Material.BARRIER, "<red>暂无任务", List.of(
                "<gray>当前没有" + listType.getDisplayName()
            ));
            builder.setItem(22, emptyItem);
        }

        // ===== 底部导航栏 =====
        // 主页按钮
        builder.setItem(QuestNavigation.SLOT_HOME, nav.createHomeButton(), nav.getHomeHandler());

        // 返回按钮 (返回主页)
        builder.setItem(QuestNavigation.SLOT_BACK, nav.createBackButton("任务中心"), nav.getHomeHandler());

        // 上一页按钮
        if (currentPage > 1) {
            builder.setItem(QuestNavigation.SLOT_PREV, nav.createPrevPageButton(currentPage, totalPages), event -> {
                playClickSound();
                new QuestListGUI(plugin, player, listType, currentPage - 1).open();
            });
        } else {
            builder.setItem(QuestNavigation.SLOT_PREV, nav.createNavFiller());
        }

        // 当前位置指示
        builder.setItem(QuestNavigation.SLOT_INFO, nav.createLocationIndicator(
            "第 " + currentPage + "/" + totalPages + " 页",
            "共 " + quests.size() + " 个任务"
        ), null);

        // 下一页按钮
        if (currentPage < totalPages) {
            builder.setItem(QuestNavigation.SLOT_NEXT, nav.createNextPageButton(currentPage, totalPages), event -> {
                playClickSound();
                new QuestListGUI(plugin, player, listType, currentPage + 1).open();
            });
        } else {
            builder.setItem(QuestNavigation.SLOT_NEXT, nav.createNavFiller());
        }

        // 关闭按钮
        builder.setItem(QuestNavigation.SLOT_CLOSE, nav.createCloseButton(), nav.getCloseHandler());

        // 刷新按钮
        builder.setItem(QuestNavigation.SLOT_REFRESH, nav.createRefreshButton(),
            nav.getRefreshHandler(() -> new QuestListGUI(plugin, player, listType, currentPage).open()));

        gui = builder.build();
    }

    /**
     * 获取要显示的任务列表
     */
    private List<Quest> getQuests() {
        List<Quest> result = new ArrayList<>();
        PlayerQuestData data = plugin.getProgressManager().getPlayerData(player.getUniqueId());

        switch (listType) {
            case ACTIVE -> {
                for (String questId : data.getActiveQuestIds()) {
                    Quest quest = plugin.getQuestManager().getQuest(questId);
                    if (quest != null) {
                        result.add(quest);
                    }
                }
            }
            case AVAILABLE -> {
                List<String> availableIds = plugin.getQuestManager().getAvailableQuests(player.getUniqueId());
                for (String questId : availableIds) {
                    Quest quest = plugin.getQuestManager().getQuest(questId);
                    if (quest != null) {
                        result.add(quest);
                    }
                }
            }
            case ALL -> result.addAll(plugin.getQuestRepository().getAllQuests());
        }

        return result;
    }

    /**
     * 获取物品槽位数组 (3行 * 7列 = 21个槽位)
     */
    private int[] getItemSlots() {
        int[] slots = new int[ITEMS_PER_PAGE];
        int index = 0;
        for (int row = 1; row <= 3; row++) {
            for (int col = 1; col <= 7; col++) {
                slots[index++] = row * 9 + col;
            }
        }
        return slots;
    }

    /**
     * 处理任务点击
     */
    private void handleQuestClick(Quest quest) {
        playClickSound();

        PlayerQuestData data = plugin.getProgressManager().getPlayerData(player.getUniqueId());
        boolean isActive = data.isQuestActive(quest.getId());
        boolean isCompleted = data.isQuestCompleted(quest.getId());
        boolean canComplete = isActive && plugin.getQuestManager().canComplete(player.getUniqueId(), quest.getId());

        if (isCompleted) {
            // 已完成，只显示详情
            new QuestDetailGUI(plugin, player, quest, this).open();
        } else if (canComplete) {
            // 可完成，尝试完成
            if (plugin.getQuestManager().completeQuest(player.getUniqueId(), quest.getId())) {
                playSuccessSound();
                sendMessage("<green>✔ 任务完成: " + quest.getName());
                // 完成后刷新当前界面
                new QuestListGUI(plugin, player, listType, currentPage).open();
            } else {
                playErrorSound();
                sendMessage("<red>任务完成失败！");
            }
        } else if (isActive) {
            // 进行中，显示详情
            new QuestDetailGUI(plugin, player, quest, this).open();
        } else {
            // 可接取，尝试接取
            if (plugin.getQuestManager().acceptQuest(player.getUniqueId(), quest.getId())) {
                playSuccessSound();
                sendMessage("<green>✔ 已接取任务: " + quest.getName());
                // 接取后刷新当前界面
                new QuestListGUI(plugin, player, listType, currentPage).open();
            } else {
                playErrorSound();
                sendMessage("<red>无法接取任务！请检查前置条件。");
            }
        }
    }
}
