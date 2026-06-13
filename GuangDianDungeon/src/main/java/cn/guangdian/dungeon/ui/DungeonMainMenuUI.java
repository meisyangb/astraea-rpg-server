package cn.guangdian.dungeon.ui;

import cn.guangdian.dungeon.GuangDianDungeon;
import cn.guangdian.dungeon.model.session.DungeonSession;
import org.bukkit.Material;
import org.bukkit.entity.Player;

public class DungeonMainMenuUI extends AbstractDungeonUI {

    public DungeonMainMenuUI(GuangDianDungeon plugin, Player player) {
        super(plugin, player, 27, "<dark_gray>副本系统"); // 缩小到27格，更紧凑
        refresh();
    }

    @Override
    protected void refresh() {
        inventory.clear();

        // 中央标题
        inventory.setItem(4, createItem(Material.DRAGON_HEAD,
            "<dark_purple><bold>副本系统",
            "<gray>选择一个功能开始"));

        // 功能入口（居中紧凑布局）
        inventory.setItem(11, createItem(Material.COMPASS, "<gold>副本浏览",
            "<gray>浏览并进入副本"));
        inventory.setItem(13, createItem(Material.GOLDEN_HELMET, "<yellow>我的队伍",
            "<gray>查看或管理队伍"));
        inventory.setItem(15, createItem(Material.GOLDEN_APPLE, "<green>排行榜",
            "<gray>查看副本排名"));

        // 底部 - 当前状态
        boolean inDungeon = plugin.getSessionManager().isInDungeon(player.getUniqueId());
        if (inDungeon) {
            DungeonSession session = plugin.getSessionManager().getPlayerSession(player.getUniqueId());
            if (session != null) {
                var template = plugin.getTemplateLoader().getTemplate(session.getDungeonId());
                var stage = session.getCurrentStage();
                inventory.setItem(22, createItem(Material.ENDER_EYE,
                    "<green>当前副本: <gold>" + (template != null ? template.getName() : session.getDungeonId()),
                    "<gray>阶段: <white>" + (stage != null ? stage.getName() : "未开始"),
                    "<gray>用时: <white>" + (session.getElapsedTime() / 1000) + "秒",
                    "<gray>状态: <white>" + session.getState().name(),
                    "<yellow>使用 /dungeon status 查看详情"
                ));
            }
        } else {
            inventory.setItem(22, createItem(Material.GRAY_DYE,
                "<gray>当前未在副本中",
                "<yellow>点击副本浏览开始冒险"
            ));
        }

        // 关闭按钮
        inventory.setItem(26, createCloseItem());

        fillAllEmpty();
    }

    @Override
    protected void handleClick(int slot) {
        playClickSound();

        switch (slot) {
            case 11 -> { // 副本浏览
                close();
                new DungeonListUI(plugin, player).open();
            }
            case 13 -> { // 我的队伍
                close();
                new PartyUI(plugin, player).open();
            }
            case 15 -> { // 排行榜
                close();
                new DungeonRankUI(plugin, player).open();
            }
            case 22 -> { // 当前状态
                if (plugin.getSessionManager().isInDungeon(player.getUniqueId())) {
                    player.performCommand("dungeon status");
                } else {
                    close();
                    new DungeonListUI(plugin, player).open();
                }
            }
            case 26 -> close();
        }
    }
}
