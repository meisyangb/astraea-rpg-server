package cn.guangdian.dungeon.ui;

import cn.guangdian.dungeon.GuangDianDungeon;
import cn.guangdian.dungeon.model.*;
import net.kyori.adventure.text.Component;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class DungeonDetailUI extends AbstractDungeonUI {

    private final DungeonTemplate template;
    private final Map<Integer, Difficulty> difficultySlots;

    public DungeonDetailUI(GuangDianDungeon plugin, Player player, String dungeonId) {
        super(plugin, player, 54, "<dark_gray>副本详情");
        this.template = plugin.getTemplateLoader().getTemplate(dungeonId);
        this.difficultySlots = new HashMap<>();
        refresh();
    }

    @Override
    protected void refresh() {
        inventory.clear();
        difficultySlots.clear();

        if (template == null) {
            inventory.setItem(22, createItem(Material.BARRIER, "<red>副本不存在"));
            fillAllEmpty();
            return;
        }

        // 第一行 - 信息头部
        inventory.setItem(4, createInfoItem());

        // 第二行 - 空行分隔
        fillRow(1);

        // 第三行 - 难度选择 (中间区域)
        int[] difficultySlotsPos = {20, 22, 24};
        int slotIndex = 0;
        for (Difficulty difficulty : template.getDifficulties()) {
            if (slotIndex >= difficultySlotsPos.length) break;
            int slot = difficultySlotsPos[slotIndex];
            inventory.setItem(slot, createDifficultyItem(difficulty));
            difficultySlots.put(slot, difficulty);
            slotIndex++;
        }

        // 第四行 - 空行分隔
        fillRow(3);

        // 第五行 - 功能按钮
        inventory.setItem(38, createItem(Material.WRITABLE_BOOK, "<aqua>通关记录", "<gray>查看你的通关记录"));
        inventory.setItem(40, createItem(Material.CHEST, "<gold>奖励预览", "<gray>查看副本奖励"));
        inventory.setItem(42, createItem(Material.NETHER_STAR, "<green>快速进入", "<gray>使用默认难度进入副本"));

        // 第六行 - 导航
        inventory.setItem(45, createBackItem());
        inventory.setItem(53, createCloseItem());

        fillAllEmpty();
    }

    private ItemStack createInfoItem() {
        ItemStack item = new ItemStack(Material.BOOK);
        org.bukkit.inventory.meta.ItemMeta meta = item.getItemMeta();

        meta.displayName(plugin.color("<gold><bold>" + template.getName()));

        List<Component> lore = new ArrayList<>();
        lore.add(plugin.color("<gray>ID: <white>" + template.getId()));
        lore.add(plugin.color("<gray>描述: <white>" + template.getDescription()));
        lore.add(Component.empty());
        lore.add(plugin.color("<gray>人数限制: <white>" + template.getSettings().getMinPlayers() +
            " - " + template.getSettings().getMaxPlayers() + " 人"));
        lore.add(plugin.color("<gray>时间限制: <white>" + formatTime(template.getSettings().getTimeLimit())));
        lore.add(plugin.color("<gray>冷却时间: <white>" + formatTime(template.getSettings().getCooldown())));
        lore.add(plugin.color("<gray>最大死亡: <white>" + template.getSettings().getMaxDeaths() + " 次"));
        if (template.getSettings().getRecommendedLevel() > 1) {
            lore.add(plugin.color("<yellow>推荐等级: <white>Lv." + template.getSettings().getRecommendedLevel()));
        }
        lore.add(Component.empty());

        // 冷却状态
        var playerData = plugin.getPlayerRepository().getPlayerData(player.getUniqueId());
        if (playerData != null && playerData.isOnCooldown(template.getId())) {
            long remaining = playerData.getRemainingCooldown(template.getId());
            lore.add(plugin.color("<red>冷却中: <yellow>" + formatTime((int)(remaining / 1000))));
            lore.add(Component.empty());
        }

        lore.add(plugin.color("<yellow>选择下方难度进入副本"));

        meta.lore(lore);
        item.setItemMeta(meta);
        return item;
    }

    private ItemStack createDifficultyItem(Difficulty difficulty) {
        var playerData = plugin.getPlayerRepository().getPlayerData(player.getUniqueId());
        String key = template.getId() + ":" + difficulty.getId();
        boolean cleared = playerData != null && playerData.hasCleared(key);

        String diffColor = getDifficultyColor(difficulty.getId());
        Material material = getDifficultyMaterial(difficulty.getId(), cleared);

        ItemStack item = new ItemStack(material);
        org.bukkit.inventory.meta.ItemMeta meta = item.getItemMeta();

        meta.displayName(plugin.color(diffColor + "<bold>" + difficulty.getName()));

        List<Component> lore = new ArrayList<>();
        lore.add(plugin.color("<gray>难度ID: <white>" + difficulty.getId()));
        lore.add(Component.empty());
        lore.add(plugin.color("<gray>怪物血量: <white>" + String.format("%.0f%%", difficulty.getHealthMultiplier() * 100)));
        lore.add(plugin.color("<gray>怪物伤害: <white>" + String.format("%.0f%%", difficulty.getDamageMultiplier() * 100)));
        lore.add(plugin.color("<gray>怪物数量: <white>" + String.format("%.0f%%", difficulty.getMobCountMultiplier() * 100)));
        lore.add(plugin.color("<gray>奖励倍率: <gold>" + String.format("%.0f%%", difficulty.getRewardMultiplier() * 100)));
        lore.add(plugin.color("<gray>经验倍率: <aqua>" + String.format("%.0f%%", difficulty.getExpMultiplier() * 100)));
        lore.add(Component.empty());

        if (difficulty.getTimeLimitModifier() != 0) {
            String modifier = difficulty.getTimeLimitModifier() > 0 ?
                "<green>+" + difficulty.getTimeLimitModifier() + "秒" :
                "<red>" + difficulty.getTimeLimitModifier() + "秒";
            lore.add(plugin.color("<gray>时间调整: " + modifier));
        }

        if (difficulty.getMaxDeathsModifier() != 0) {
            String modifier = difficulty.getMaxDeathsModifier() > 0 ?
                "<green>+" + difficulty.getMaxDeathsModifier() + "次" :
                "<red>" + difficulty.getMaxDeathsModifier() + "次";
            lore.add(plugin.color("<gray>死亡调整: " + modifier));
        }

        lore.add(Component.empty());

        if (cleared) {
            int count = playerData.getClearCount(key);
            int bestScore = playerData.getBestScore(key);
            lore.add(plugin.color("<green><bold>已通关"));
            lore.add(plugin.color("<gray>通关次数: <white>" + count));
            lore.add(plugin.color("<gray>最高评分: <gold>" + bestScore));
        } else {
            lore.add(plugin.color("<red><bold>未通关"));
        }

        lore.add(Component.empty());
        lore.add(plugin.color("<yellow>点击选择此难度进入"));

        meta.lore(lore);
        item.setItemMeta(meta);
        return item;
    }

    private String getDifficultyColor(String diffId) {
        return switch (diffId) {
            case "normal" -> "<green>";
            case "hard" -> "<gold>";
            case "heroic" -> "<red>";
            default -> "<white>";
        };
    }

    private Material getDifficultyMaterial(String diffId, boolean cleared) {
        if (cleared) return Material.EMERALD_BLOCK;
        return switch (diffId) {
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
        if (m > 0) {
            return m + "分" + (s > 0 ? s + "秒" : "");
        }
        return seconds + "秒";
    }

    @Override
    protected void handleClick(int slot) {
        playClickSound();

        // 返回
        if (slot == 45) {
            close();
            new DungeonListUI(plugin, player).open();
            return;
        }

        // 关闭
        if (slot == 53) {
            close();
            return;
        }

        // 通关记录
        if (slot == 38) {
            player.sendMessage(plugin.color("<gold>========== 通关记录 - " + template.getName() + " =========="));
            var playerData = plugin.getPlayerRepository().getPlayerData(player.getUniqueId());
            if (playerData != null) {
                for (Difficulty diff : template.getDifficulties()) {
                    String key = template.getId() + ":" + diff.getId();
                    if (playerData.hasCleared(key)) {
                        int count = playerData.getClearCount(key);
                        int bestScore = playerData.getBestScore(key);
                        long bestTime = playerData.getBestTime(key);
                        player.sendMessage(plugin.color("<gray>" + diff.getName() + ": <green>已通关 <gray>(次数: <white>" +
                            count + "<gray>, 最高分: <gold>" + bestScore + "<gray>, 最快: <white>" + bestTime + "ms<gray>)"));
                    } else {
                        player.sendMessage(plugin.color("<gray>" + diff.getName() + ": <red>未通关"));
                    }
                }
            }
            player.sendMessage(plugin.color("<gold>========================================"));
            return;
        }

        // 奖励预览
        if (slot == 40) {
            close();
            new DungeonRewardUI(plugin, player, template).open();
            return;
        }

        // 快速进入
        if (slot == 42) {
            Difficulty difficulty = template.getDefaultDifficulty();
            if (difficulty == null) {
                player.sendMessage(plugin.color("<red>无法获取默认难度"));
                return;
            }
            close();
            enterDungeon(difficulty);
            return;
        }

        // 难度选择
        Difficulty difficulty = difficultySlots.get(slot);
        if (difficulty != null) {
            close();
            enterDungeon(difficulty);
        }
    }

    private void enterDungeon(Difficulty difficulty) {
        // 委托给 DungeonCommand 的 enter 逻辑
        player.performCommand("dungeon enter " + template.getId() + " " + difficulty.getId());
    }
}
