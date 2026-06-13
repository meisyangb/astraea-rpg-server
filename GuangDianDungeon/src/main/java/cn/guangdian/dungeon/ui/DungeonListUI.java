package cn.guangdian.dungeon.ui;

import cn.guangdian.dungeon.GuangDianDungeon;
import cn.guangdian.dungeon.model.*;
import net.kyori.adventure.text.Component;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.*;

/**
 * 副本浏览 - 合并列表+详情于一页
 * 点击副本图标 → 查看详情
 * 点击难度按钮 → 直接进入副本
 */
public class DungeonListUI extends AbstractDungeonUI {

    private int page;
    private final int maxPage;
    private final Map<Integer, String> slotToDungeon;
    private final Map<Integer, Difficulty> slotToDifficulty;
    private String filterDifficulty;
    // 分页槽位映射: 每页底部第5-6行用 difficultySlots 排列难度按钮
    private List<DungeonTemplate> filteredTemplates;

    public DungeonListUI(GuangDianDungeon plugin, Player player) {
        super(plugin, player, 54, "<dark_gray>副本浏览");
        this.slotToDungeon = new HashMap<>();
        this.slotToDifficulty = new HashMap<>();
        this.filterDifficulty = null;
        this.filteredTemplates = new ArrayList<>(plugin.getTemplateLoader().getAllTemplates());

        int dungeonCount = filteredTemplates.size();
        this.maxPage = Math.max(0, (dungeonCount - 1) / 28);
        this.page = 0;

        refresh();
    }

    @Override
    protected void refresh() {
        inventory.clear();
        slotToDungeon.clear();
        slotToDifficulty.clear();

        // 重新获取过滤后的模板列表
        filteredTemplates = new ArrayList<>(plugin.getTemplateLoader().getAllTemplates());
        if (filterDifficulty != null) {
            filteredTemplates.removeIf(t -> t.getDifficulties().stream()
                .noneMatch(d -> d.getId().equals(filterDifficulty)));
        }

        // 顶部筛选栏
        inventory.setItem(0, createFilterButton(Material.BOOK, "<white>全部", null));
        inventory.setItem(1, createFilterButton(Material.GREEN_CONCRETE, "<green>普通", "normal"));
        inventory.setItem(2, createFilterButton(Material.ORANGE_CONCRETE, "<gold>困难", "hard"));
        inventory.setItem(3, createFilterButton(Material.RED_CONCRETE, "<red>英雄", "heroic"));
        inventory.setItem(4, createFillItem());
        inventory.setItem(5, createFillItem());
        inventory.setItem(6, createFillItem());
        inventory.setItem(7, createFillItem());
        inventory.setItem(8, createBackItem());

        // 副本列表 (9-44, 共36格) — 但每4格一组展示副本+难度
        int startIndex = page * 7; // 每页显示7个副本
        int endIndex = Math.min(startIndex + 7, filteredTemplates.size());

        int[] rowSlots = {9, 13, 19, 23, 29, 33, 39}; // 左列 (大图标)
        int[] diffStartSlots = {10, 14, 20, 24, 30, 34, 40}; // 右列起始 (难度按钮)

        for (int i = startIndex, r = 0; i < endIndex && r < rowSlots.length; i++, r++) {
            DungeonTemplate template = filteredTemplates.get(i);
            int iconSlot = rowSlots[r];
            inventory.setItem(iconSlot, createDungeonIcon(template));
            slotToDungeon.put(iconSlot, template.getId());

            // 难度快速进入按钮（每行最多3个）
            List<Difficulty> diffs = template.getDifficulties();
            for (int d = 0; d < Math.min(diffs.size(), 3); d++) {
                int diffSlot = diffStartSlots[r] + d * 2;
                if (diffSlot >= 54) break;
                Difficulty difficulty = diffs.get(d);
                inventory.setItem(diffSlot, createDifficultyButton(template, difficulty));
                slotToDifficulty.put(diffSlot, difficulty);
                // 在 difficulty 旁边存 dungeon id 用于进入
                // 通过另一个 map 来存
            }
        }

        // 存储难度按钮对应的副本ID（通过近距离查找）
        for (int i = startIndex, r = 0; i < endIndex && r < rowSlots.length; i++, r++) {
            DungeonTemplate template = filteredTemplates.get(i);
            List<Difficulty> diffs = template.getDifficulties();
            for (int d = 0; d < Math.min(diffs.size(), 3); d++) {
                int diffSlot = diffStartSlots[r] + d * 2;
                if (diffSlot >= 54) break;
                slotToDungeon.put(diffSlot, template.getId());
            }
        }

        // 翻页导航
        if (page > 0) {
            inventory.setItem(45, createItem(Material.SPECTRAL_ARROW, "<green>上一页", "<gray>第 " + page + " 页"));
        }
        inventory.setItem(49, createItem(Material.BOOK, "<yellow>第 " + (page + 1) + " / " + (maxPage + 1) + " 页",
            "<gray>共 " + filteredTemplates.size() + " 个副本"));
        if (page < maxPage) {
            inventory.setItem(53, createItem(Material.SPECTRAL_ARROW, "<green>下一页", "<gray>第 " + (page + 2) + " 页"));
        }

        fillAllEmpty();
    }

    private ItemStack createFilterButton(Material mat, String name, String difficulty) {
        boolean selected = Objects.equals(filterDifficulty, difficulty)
            || (difficulty == null && filterDifficulty == null);
        String indicator = selected ? " <green>◀" : "";
        return createItem(mat, name + indicator, "<gray>点击筛选");
    }

    private ItemStack createDungeonIcon(DungeonTemplate template) {
        Material material = getDungeonIcon(template);
        ItemStack item = new ItemStack(material);
        org.bukkit.inventory.meta.ItemMeta meta = item.getItemMeta();

        meta.displayName(plugin.color("<gold><bold>" + template.getName()));

        List<Component> lore = new ArrayList<>();
        lore.add(plugin.color("<gray>" + template.getId()));
        lore.add(plugin.color("<white>" + template.getDescription()));
        lore.add(Component.empty());
        lore.add(plugin.color("<gray>👥 " + template.getSettings().getMinPlayers()
            + "-" + template.getSettings().getMaxPlayers() + "人"));
        lore.add(plugin.color("<gray>⏱ " + formatTime(template.getSettings().getTimeLimit())));

        if (template.getSettings().getRecommendedLevel() > 1) {
            lore.add(plugin.color("<yellow>⚔ 推荐等级: " + template.getSettings().getRecommendedLevel()));
        }

        // 通关状态
        var playerData = plugin.getPlayerRepository().getPlayerData(player.getUniqueId());
        if (playerData != null) {
            for (Difficulty diff : template.getDifficulties()) {
                String key = template.getId() + ":" + diff.getId();
                if (playerData.hasCleared(key)) {
                    lore.add(plugin.color("<green>✓ " + diff.getName() + ": 已通关 (" +
                        playerData.getClearCount(key) + "次)"));
                }
            }
        }

        // 冷却状态
        if (playerData != null && playerData.isOnCooldown(template.getId())) {
            long remaining = playerData.getRemainingCooldown(template.getId());
            lore.add(Component.empty());
            lore.add(plugin.color("<red>⏳ 冷却: " + formatTime((int)(remaining / 1000))));
        }

        lore.add(Component.empty());
        lore.add(plugin.color("<yellow>▶ 点击查看详情"));
        lore.add(plugin.color("<gray>右侧按钮快速进入"));

        meta.lore(lore);
        item.setItemMeta(meta);
        return item;
    }

    private ItemStack createDifficultyButton(DungeonTemplate template, Difficulty difficulty) {
        String diffColor = getDiffColor(difficulty.getId());
        Material mat = getDiffMaterial(difficulty.getId());
        ItemStack item = new ItemStack(mat);
        org.bukkit.inventory.meta.ItemMeta meta = item.getItemMeta();

        meta.displayName(plugin.color(diffColor + difficulty.getName()));

        List<Component> lore = new ArrayList<>();
        lore.add(plugin.color("<gray>x" + (int)(difficulty.getHealthMultiplier() * 100) + "% HP"));
        lore.add(plugin.color("<gray>x" + (int)(difficulty.getRewardMultiplier() * 100) + "% 奖励"));
        lore.add(Component.empty());
        lore.add(plugin.color("<yellow>▶ 点击进入"));

        meta.lore(lore);
        item.setItemMeta(meta);
        return item;
    }

    private Material getDungeonIcon(DungeonTemplate template) {
        String iconName = template.getSettings().getIconMaterial();
        if (iconName != null) {
            try {
                return Material.valueOf(iconName.toUpperCase());
            } catch (IllegalArgumentException ignored) {}
        }
        // 根据难度回退默认图标
        for (Difficulty d : template.getDifficulties()) {
            if (d.getId().equals("heroic")) return Material.DRAGON_EGG;
        }
        return Material.DIAMOND_SWORD;
    }

    private String getDiffColor(String id) {
        return switch (id) {
            case "normal" -> "<green>";
            case "hard" -> "<gold>";
            case "heroic" -> "<red>";
            default -> "<white>";
        };
    }

    private Material getDiffMaterial(String id) {
        return switch (id) {
            case "normal" -> Material.GREEN_CONCRETE;
            case "hard" -> Material.ORANGE_CONCRETE;
            case "heroic" -> Material.RED_CONCRETE;
            default -> Material.STONE;
        };
    }

    private String formatTime(int seconds) {
        if (seconds >= 3600) {
            int h = seconds / 3600;
            int m = (seconds % 3600) / 60;
            return h + "小时" + (m > 0 ? m + "分" : "");
        }
        int m = seconds / 60;
        int s = seconds % 60;
        if (m > 0) return m + "分" + (s > 0 ? s + "秒" : "");
        return seconds + "秒";
    }

    @Override
    protected void handleClick(int slot) {
        playClickSound();

        // 筛选按钮
        if (slot == 0) { filterDifficulty = null; page = 0; refresh(); return; }
        if (slot == 1) { filterDifficulty = "normal"; page = 0; refresh(); return; }
        if (slot == 2) { filterDifficulty = "hard"; page = 0; refresh(); return; }
        if (slot == 3) { filterDifficulty = "heroic"; page = 0; refresh(); return; }

        // 返回
        if (slot == 8) { close(); new DungeonMainMenuUI(plugin, player).open(); return; }

        // 翻页
        if (slot == 45 && page > 0) { page--; refresh(); return; }
        if (slot == 53 && page < maxPage) { page++; refresh(); return; }

        String dungeonId = slotToDungeon.get(slot);
        if (dungeonId == null) return;

        Difficulty diff = slotToDifficulty.get(slot);
        if (diff != null) {
            // 直接进入副本
            close();
            player.performCommand("dungeon enter " + dungeonId + " " + diff.getId());
        } else {
            // 点击图标 → 显示详情
            close();
            new DungeonDetailUI(plugin, player, dungeonId).open();
        }
    }
}
