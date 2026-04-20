package cn.guangdian.battlepass.gui;

import cn.guangdian.battlepass.GuangDianBattlePass;
import cn.guangdian.battlepass.model.BattlePassLevel;
import cn.guangdian.battlepass.model.PlayerBattlePass;
import cn.guangdian.battlepass.model.Season;
import cn.guangdian.rpgcore.gui.GUI;
import cn.guangdian.rpgcore.gui.GUIBuilder;
import cn.guangdian.rpgcore.message.MiniMessageService;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.List;

/**
 * 战令GUI系统 - 使用RPGCore GUI服务
 *
 * @author Astraea RPG Team
 * @since 1.1.0
 */
public class BattlePassGUI implements Listener {

    private final GuangDianBattlePass plugin;
    private final MiniMessageService msg;

    public BattlePassGUI(GuangDianBattlePass plugin) {
        this.plugin = plugin;
        this.msg = MiniMessageService.getInstance();
    }

    /**
     * 打开战令主界面
     */
    public void openBattlePass(Player player) {
        Season season = plugin.getSeasonManager().getCurrentSeason();
        if (season == null) {
            player.sendMessage(msg.red("当前没有进行中的赛季!"));
            return;
        }

        PlayerBattlePass bp = plugin.getBattlePassManager().getPlayerBattlePass(player.getUniqueId());
        if (bp == null) {
            player.sendMessage(msg.red("无法加载战令数据!"));
            return;
        }

        int totalPages = (int) Math.ceil(season.getMaxLevel() / 7.0);
        openPage(player, 1, season, bp, totalPages);
    }

    /**
     * 打开指定页码
     */
    private void openPage(Player player, int page, Season season, PlayerBattlePass bp, int totalPages) {
        String title = "<gold>☆ 战令 - " + season.getSeasonName() + " <gray>[" + page + "/" + totalPages + "] ☆";

        GUI gui = GUIBuilder.create(title, 6)
            .setFiller(Material.BLACK_STAINED_GLASS_PANE)
            .build();

        // 装饰边框
        ItemStack border = createItem(Material.PURPLE_STAINED_GLASS_PANE, Component.text(" "));
        gui.fillEmptySlots(border);

        int startLevel = (page - 1) * 7 + 1;
        int endLevel = Math.min(startLevel + 6, season.getMaxLevel());

        // 显示等级和奖励
        for (int i = startLevel; i <= endLevel; i++) {
            int slot = (i - startLevel) * 7 + 10;
            addLevelItem(gui, slot, i, season, bp, player);
        }

        // 信息物品
        ItemStack infoItem = createInfoItem(bp, season);
        gui.setItem(4, infoItem);

        // 上一页按钮
        if (page > 1) {
            ItemStack prevItem = createNavigationItem(Material.ARROW, Component.text("上一页").color(NamedTextColor.YELLOW), page - 1);
            gui.setItem(45, prevItem, event -> openPage(player, page - 1, season, bp, totalPages));
        }

        // 下一页按钮
        if (page < totalPages) {
            ItemStack nextItem = createNavigationItem(Material.ARROW, Component.text("下一页").color(NamedTextColor.YELLOW), page + 1);
            gui.setItem(53, nextItem, event -> openPage(player, page + 1, season, bp, totalPages));
        }

        // 购买高级战令按钮
        if (!bp.isPremium()) {
            ItemStack purchaseItem = createPurchaseItem();
            gui.setItem(49, purchaseItem, event -> {
                int price = plugin.getConfig().getInt("premium-price", 1000);
                if (plugin.takePoints(player, price)) {
                    bp.setPremium(true);
                    player.sendMessage(msg.green("成功购买高级战令!"));
                    openBattlePass(player);
                } else {
                    player.sendMessage(msg.red("点券不足! 需要 " + price + " 点券"));
                }
            });
        } else {
            // 已购买高级战令，显示状态
            ItemStack premiumStatusItem = createPremiumStatusItem();
            gui.setItem(49, premiumStatusItem);
        }

        // 任务进度按钮
        ItemStack taskItem = createTaskItem(bp);
        gui.setItem(46, taskItem);

        // 关闭按钮
        ItemStack closeItem = createItem(Material.BARRIER, Component.text("关闭").color(NamedTextColor.RED));
        gui.setItem(48, closeItem, event -> gui.close(player));

        // 帮助按钮
        ItemStack helpItem = createHelpItem();
        gui.setItem(52, helpItem);

        gui.open(player);
    }

    /**
     * 添加等级物品到GUI
     */
    private void addLevelItem(GUI gui, int slot, int level, Season season, PlayerBattlePass bp, Player player) {
        BattlePassLevel bpLevel = season.getLevel(level);
        if (bpLevel == null) return;

        boolean isCurrentLevel = bp.getLevel() == level;
        boolean isUnlocked = bp.getLevel() >= level;
        boolean freeClaimed = bp.hasClaimedFreeReward(level);
        boolean premiumClaimed = bp.hasClaimedPremiumReward(level);

        // 等级标识物品
        ItemStack levelItem = createLevelIndicatorItem(level, isCurrentLevel, isUnlocked, bp);
        gui.setItem(slot, levelItem);

        // 免费奖励
        if (bpLevel.getFreeReward() != null) {
            boolean canClaimFree = isUnlocked && !freeClaimed;
            ItemStack freeItem = createRewardItem(bpLevel.getFreeReward(), "免费", canClaimFree, freeClaimed);
            gui.setItem(slot + 1, freeItem, event -> {
                if (bp.canClaimFreeReward(level)) {
                    if (plugin.getBattlePassManager().claimFreeReward(player, level)) {
                        player.sendMessage(msg.green("成功领取等级 " + level + " 的免费奖励!"));
                        openBattlePass(player);
                    }
                } else {
                    player.sendMessage(msg.red("无法领取此奖励!"));
                }
            });
        }

        // 高级奖励
        if (bpLevel.getPremiumReward() != null) {
            boolean canClaimPremium = isUnlocked && bp.isPremium() && !premiumClaimed;
            ItemStack premiumItem = createRewardItem(bpLevel.getPremiumReward(), "高级", canClaimPremium, premiumClaimed);
            gui.setItem(slot + 2, premiumItem, event -> {
                if (bp.canClaimPremiumReward(level)) {
                    if (plugin.getBattlePassManager().claimPremiumReward(player, level)) {
                        player.sendMessage(msg.green("成功领取等级 " + level + " 的高级奖励!"));
                        openBattlePass(player);
                    }
                } else if (!bp.isPremium()) {
                    player.sendMessage(msg.red("需要购买高级战令才能领取!"));
                } else {
                    player.sendMessage(msg.red("无法领取此奖励!"));
                }
            });
        }
    }

    // ==================== 物品创建方法 ====================

    private ItemStack createLevelIndicatorItem(int level, boolean isCurrentLevel, boolean isUnlocked, PlayerBattlePass bp) {
        Material material;
        NamedTextColor color;

        if (isCurrentLevel) {
            material = Material.GOLD_BLOCK;
            color = NamedTextColor.GOLD;
        } else if (isUnlocked) {
            material = Material.EMERALD_BLOCK;
            color = NamedTextColor.GREEN;
        } else {
            material = Material.REDSTONE_BLOCK;
            color = NamedTextColor.RED;
        }

        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();

        meta.displayName(Component.text("等级 " + level).color(color).decoration(TextDecoration.BOLD, true));

        List<Component> lore = new ArrayList<>();
        lore.add(Component.empty());

        if (isCurrentLevel) {
            lore.add(Component.text("▶ 当前等级").color(NamedTextColor.GOLD).decoration(TextDecoration.BOLD, true));
            int expNeeded = getExpNeededForNextLevel(bp);
            if (expNeeded > 0) {
                lore.add(Component.text("升级还需: " + expNeeded + " 经验").color(NamedTextColor.YELLOW));
            }
        } else if (isUnlocked) {
            lore.add(Component.text("✔ 已解锁").color(NamedTextColor.GREEN));
        } else {
            lore.add(Component.text("✘ 未解锁").color(NamedTextColor.RED));
        }

        meta.lore(lore);
        item.setItemMeta(meta);
        return item;
    }

    private ItemStack createRewardItem(cn.guangdian.battlepass.model.BattlePassReward reward, String type, boolean canClaim, boolean claimed) {
        Material material;
        NamedTextColor color;

        if (claimed) {
            material = Material.LIME_STAINED_GLASS_PANE;
            color = NamedTextColor.GREEN;
        } else if (canClaim) {
            material = Material.CHEST;
            color = NamedTextColor.GOLD;
        } else {
            material = Material.GRAY_STAINED_GLASS_PANE;
            color = NamedTextColor.GRAY;
        }

        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();

        String prefix = claimed ? "✔ " : (canClaim ? "▶ " : "  ");
        meta.displayName(Component.text(prefix + type + "奖励").color(color));

        if (canClaim) {
            meta.addEnchant(org.bukkit.enchantments.Enchantment.UNBREAKING, 1, true);
            meta.addItemFlags(ItemFlag.HIDE_ENCHANTS);
        }

        List<Component> lore = new ArrayList<>();
        lore.add(Component.empty());
        lore.add(Component.text(reward.getDisplayName()).color(NamedTextColor.YELLOW));
        lore.add(Component.empty());

        if (claimed) {
            lore.add(Component.text("✔ 已领取").color(NamedTextColor.GREEN));
        } else if (canClaim) {
            lore.add(Component.text("点击领取!").color(NamedTextColor.GOLD).decoration(TextDecoration.BOLD, true));
        } else {
            lore.add(Component.text("未解锁").color(NamedTextColor.GRAY));
        }

        meta.lore(lore);
        item.setItemMeta(meta);
        return item;
    }

    private ItemStack createInfoItem(PlayerBattlePass bp, Season season) {
        ItemStack item = new ItemStack(Material.BOOK);
        ItemMeta meta = item.getItemMeta();

        meta.displayName(Component.text("✦ 战令信息 ✦").color(NamedTextColor.GOLD).decoration(TextDecoration.BOLD, true));

        List<Component> lore = new ArrayList<>();
        lore.add(Component.empty());
        lore.add(Component.text("赛季: ").color(NamedTextColor.YELLOW).append(Component.text(season.getSeasonName()).color(NamedTextColor.WHITE)));
        lore.add(Component.text("等级: ").color(NamedTextColor.YELLOW).append(Component.text(bp.getLevel() + "/" + season.getMaxLevel()).color(NamedTextColor.GREEN)));
        lore.add(Component.text("经验: ").color(NamedTextColor.YELLOW).append(Component.text(String.valueOf(bp.getCurrentExp())).color(NamedTextColor.AQUA)));
        lore.add(Component.empty());

        if (bp.isPremium()) {
            lore.add(Component.text("✦ 高级战令").color(NamedTextColor.LIGHT_PURPLE).decoration(TextDecoration.BOLD, true));
        } else {
            lore.add(Component.text("状态: 免费战令").color(NamedTextColor.YELLOW));
            lore.add(Component.text("购买高级战令解锁更多奖励!").color(NamedTextColor.GRAY));
        }

        lore.add(Component.empty());
        lore.add(Component.text("剩余时间: " + season.getRemainingDays() + " 天").color(NamedTextColor.GRAY));

        meta.lore(lore);
        item.setItemMeta(meta);
        return item;
    }

    private ItemStack createNavigationItem(Material material, Component name, int page) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        meta.displayName(name);

        List<Component> lore = new ArrayList<>();
        lore.add(Component.empty());
        lore.add(Component.text("前往第 " + page + " 页").color(NamedTextColor.GRAY));

        meta.lore(lore);
        item.setItemMeta(meta);
        return item;
    }

    private ItemStack createPurchaseItem() {
        ItemStack item = new ItemStack(Material.NETHER_STAR);
        ItemMeta meta = item.getItemMeta();

        meta.displayName(Component.text("💎 购买高级战令").color(NamedTextColor.LIGHT_PURPLE).decoration(TextDecoration.BOLD, true));

        int price = plugin.getConfig().getInt("premium-price", 1000);

        List<Component> lore = new ArrayList<>();
        lore.add(Component.empty());
        lore.add(Component.text("解锁所有高级奖励!").color(NamedTextColor.YELLOW));
        lore.add(Component.empty());
        lore.add(Component.text("价格: " + price + " 点券").color(NamedTextColor.GOLD));
        lore.add(Component.empty());
        lore.add(Component.text("点击购买!").color(NamedTextColor.GREEN).decoration(TextDecoration.BOLD, true));

        meta.lore(lore);
        item.setItemMeta(meta);
        return item;
    }

    private ItemStack createPremiumStatusItem() {
        ItemStack item = new ItemStack(Material.DIAMOND);
        ItemMeta meta = item.getItemMeta();

        meta.displayName(Component.text("✦ 高级战令已激活 ✦").color(NamedTextColor.LIGHT_PURPLE).decoration(TextDecoration.BOLD, true));

        List<Component> lore = new ArrayList<>();
        lore.add(Component.empty());
        lore.add(Component.text("你已购买高级战令!").color(NamedTextColor.GREEN));
        lore.add(Component.text("可以领取所有高级奖励").color(NamedTextColor.YELLOW));

        meta.lore(lore);
        item.setItemMeta(meta);
        return item;
    }

    private ItemStack createTaskItem(PlayerBattlePass bp) {
        ItemStack item = new ItemStack(Material.WRITABLE_BOOK);
        ItemMeta meta = item.getItemMeta();

        meta.displayName(Component.text("📋 任务进度").color(NamedTextColor.AQUA).decoration(TextDecoration.BOLD, true));

        List<Component> lore = new ArrayList<>();
        lore.add(Component.empty());
        lore.add(Component.text("今日任务完成情况:").color(NamedTextColor.YELLOW));
        lore.add(Component.text("完成任务获得经验值").color(NamedTextColor.GRAY));
        lore.add(Component.text("提升战令等级").color(NamedTextColor.GRAY));
        lore.add(Component.empty());
        lore.add(Component.text("使用 /battlepass tasks 查看详情").color(NamedTextColor.AQUA));

        meta.lore(lore);
        item.setItemMeta(meta);
        return item;
    }

    private ItemStack createHelpItem() {
        ItemStack item = new ItemStack(Material.BOOK);
        ItemMeta meta = item.getItemMeta();

        meta.displayName(Component.text("❓ 使用帮助").color(NamedTextColor.YELLOW).decoration(TextDecoration.BOLD, true));

        List<Component> lore = new ArrayList<>();
        lore.add(Component.empty());
        lore.add(Component.text("战令系统说明:").color(NamedTextColor.YELLOW));
        lore.add(Component.text("1. 完成任务获得经验").color(NamedTextColor.GRAY));
        lore.add(Component.text("2. 升级解锁奖励").color(NamedTextColor.GRAY));
        lore.add(Component.text("3. 免费奖励所有人可领").color(NamedTextColor.GRAY));
        lore.add(Component.text("4. 高级奖励需购买解锁").color(NamedTextColor.GRAY));
        lore.add(Component.empty());
        lore.add(Component.text("命令:").color(NamedTextColor.AQUA));
        lore.add(Component.text("/battlepass - 打开此菜单").color(NamedTextColor.GRAY));
        lore.add(Component.text("/battlepass tasks - 查看任务").color(NamedTextColor.GRAY));

        meta.lore(lore);
        item.setItemMeta(meta);
        return item;
    }

    private ItemStack createItem(Material material, Component name) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        meta.displayName(name);
        item.setItemMeta(meta);
        return item;
    }

    private int getExpNeededForNextLevel(PlayerBattlePass bp) {
        // 这里可以根据实际经验需求计算
        // 简化处理，返回固定值或根据配置计算
        return 100; // 示例值
    }

    // ==================== 事件监听 ====================

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        // GUI点击事件由RPGCore GUIManager统一处理
        // 这里可以添加额外的全局处理逻辑
    }
}
