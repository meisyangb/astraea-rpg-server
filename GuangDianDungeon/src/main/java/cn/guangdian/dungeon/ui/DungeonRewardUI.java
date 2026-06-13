package cn.guangdian.dungeon.ui;

import cn.guangdian.dungeon.GuangDianDungeon;
import cn.guangdian.dungeon.model.*;
import org.bukkit.Material;
import org.bukkit.entity.Player;

import java.util.*;

/**
 * 奖励预览 - 展示副本掉落、经验、金钱
 */
public class DungeonRewardUI extends AbstractDungeonUI {

    private final DungeonTemplate template;

    public DungeonRewardUI(GuangDianDungeon plugin, Player player, DungeonTemplate template) {
        super(plugin, player, 54, "<dark_gray>奖励预览 - " + template.getName());
        this.template = template;
        refresh();
    }

    @Override
    protected void refresh() {
        inventory.clear();

        // 标题
        inventory.setItem(4, createItem(Material.CHEST, "<gold><bold>奖励预览",
            "<gray>副本: <white>" + template.getName()));

        // 分隔
        fillRow(1);

        // 奖励池展示 (slot 18-42)
        int slot = 18;
        Map<String, RewardPool> pools = template.getRewardPools();

        if (pools.isEmpty()) {
            inventory.setItem(22, createItem(Material.BOOK, "<yellow>暂无奖励配置",
                "<gray>此副本暂未配置奖励池",
                "<white>通关后将获得基础经验和评分奖励"));
        } else {
            for (Map.Entry<String, RewardPool> entry : pools.entrySet()) {
                if (slot > 42) break;

                RewardPool pool = entry.getValue();
                List<String> loreLines = new ArrayList<>();
                loreLines.add("<gray>奖励池: <white>" + entry.getKey());
                loreLines.add("");

                for (RewardEntry rewardEntry : pool.getEntries()) {
                    String chance = String.format("%.0f%%", rewardEntry.getChance() * 100);
                    loreLines.add("<white>" + rewardEntry.getItemId()
                        + " x" + rewardEntry.getAmount()
                        + " <gray>(" + chance + "几率)");
                }

                // 经验和金钱
                if (pool.getExp().hasValue()) {
                    loreLines.add("");
                    loreLines.add("<aqua>经验: "
                        + (int)pool.getExp().getMin() + " - " + (int)pool.getExp().getMax());
                }
                if (pool.getMoney().hasValue()) {
                    loreLines.add("<gold>金钱: $"
                        + String.format("%.0f", pool.getMoney().getMin())
                        + " - $" + String.format("%.0f", pool.getMoney().getMax()));
                }

                inventory.setItem(slot, createItem(Material.CHEST_MINECART,
                    "<yellow>" + entry.getKey(), loreLines.toArray(new String[0])));
                slot += 2;
            }
        }

        // 首通奖励 (左下)
        if (!template.getFirstClearRewards().isEmpty()) {
            inventory.setItem(46, createItem(Material.DRAGON_EGG, "<light_purple><bold>首通奖励",
                "<gray>首次通关额外奖励",
                "<gray>奖励数: <white>" + template.getFirstClearRewards().size()));
        }

        // 评分奖励 (中下)
        if (!template.getScoreRewards().isEmpty()) {
            List<String> scoreLore = new ArrayList<>();
            scoreLore.add("<gray>达到指定评分获得额外奖励");
            scoreLore.add("");
            for (ScoreReward sr : template.getScoreRewards()) {
                scoreLore.add("<gold>" + sr.getMinScore() + "分 <gray>- <white>"
                    + sr.getItems().size() + "个奖励");
            }
            inventory.setItem(49, createItem(Material.GOLD_NUGGET, "<gold>评分奖励",
                scoreLore.toArray(new String[0])));
        }

        // 底部导航
        inventory.setItem(45, createBackItem());
        inventory.setItem(53, createCloseItem());

        fillAllEmpty();
    }

    @Override
    protected void handleClick(int slot) {
        playClickSound();

        if (slot == 45) {
            close();
            new DungeonDetailUI(plugin, player, template.getId()).open();
            return;
        }
        if (slot == 53) {
            close();
        }
    }
}
