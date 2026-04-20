package cn.guangdian.monthlycard.gui;

import cn.guangdian.monthlycard.GuangDianMonthlyCard;
import cn.guangdian.monthlycard.data.DailyReward;
import cn.guangdian.monthlycard.data.MonthlyCardData;
import cn.guangdian.monthlycard.data.MonthlyCardType;
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
import java.util.Optional;

/**
 * 月卡GUI系统 - 使用RPGCore GUI服务
 *
 * @author Astraea RPG Team
 * @since 1.1.0
 */
public class MonthlyCardGUI implements Listener {

    private final GuangDianMonthlyCard plugin;
    private final MiniMessageService msg;

    public MonthlyCardGUI(GuangDianMonthlyCard plugin) {
        this.plugin = plugin;
        this.msg = MiniMessageService.getInstance();
    }

    /**
     * 打开月卡主菜单
     */
    public void openMainMenu(Player player) {
        MonthlyCardData data = plugin.getPlayerData(player.getUniqueId());

        GUI gui = GUIBuilder.create("<gold>☆ 月卡中心 ☆", 6)
            .setFiller(Material.BLACK_STAINED_GLASS_PANE)
            .build();

        refreshMainMenu(gui, player, data);
        gui.open(player);
    }

    private void refreshMainMenu(GUI gui, Player player, MonthlyCardData data) {
        gui.clear();

        // 装饰边框
        ItemStack border = createItem(Material.PURPLE_STAINED_GLASS_PANE, Component.text(" "));
        gui.fillEmptySlots(border);

        // 月卡状态信息
        ItemStack statusItem = createStatusItem(data);
        gui.setItem(4, statusItem);

        // 每日奖励按钮
        ItemStack dailyRewardItem = createDailyRewardItem(data);
        gui.setItem(20, dailyRewardItem, event -> {
            if (!data.hasActiveCard()) {
                player.sendMessage(msg.red("你还没有激活月卡!"));
                return;
            }
            if (!data.canClaimToday()) {
                player.sendMessage(msg.yellow("今日奖励已领取!"));
                return;
            }
            if (plugin.claimDailyReward(player.getUniqueId())) {
                player.sendMessage(msg.green("成功领取今日月卡奖励!"));
                openMainMenu(player);
            }
        });

        // 月卡详情按钮
        ItemStack detailsItem = createDetailsItem(data);
        gui.setItem(22, detailsItem, event -> {
            if (!data.hasActiveCard()) {
                player.sendMessage(msg.red("你还没有激活月卡!"));
                return;
            }
            openRewardPreview(player, data);
        });

        // 购买月卡按钮
        ItemStack buyItem = createBuyItem();
        gui.setItem(24, buyItem, event -> openCardShop(player));

        // 奖励预览按钮
        ItemStack previewItem = createPreviewItem();
        gui.setItem(31, previewItem, event -> openCardShop(player));

        // 关闭按钮
        ItemStack closeItem = createItem(Material.BARRIER, Component.text("关闭").color(NamedTextColor.RED));
        gui.setItem(49, closeItem, event -> gui.close(player));

        // 帮助按钮
        ItemStack helpItem = createHelpItem();
        gui.setItem(52, helpItem);
    }

    /**
     * 打开月卡商店
     */
    public void openCardShop(Player player) {
        GUI gui = GUIBuilder.create("<gold>☆ 购买月卡 ☆", 6)
            .setFiller(Material.GRAY_STAINED_GLASS_PANE)
            .build();

        // 装饰边框
        ItemStack border = createItem(Material.CYAN_STAINED_GLASS_PANE, Component.text(" "));
        gui.fillEmptySlots(border);

        // 返回按钮
        ItemStack backItem = createItem(Material.ARROW, Component.text("返回主菜单").color(NamedTextColor.YELLOW));
        gui.setItem(45, backItem, event -> openMainMenu(player));

        // 关闭按钮
        ItemStack closeItem = createItem(Material.BARRIER, Component.text("关闭").color(NamedTextColor.RED));
        gui.setItem(49, closeItem, event -> gui.close(player));

        // 显示所有月卡类型
        List<MonthlyCardType> cardTypes = plugin.getCardManager().getAllCardTypes();
        int[] slots = {20, 22, 24, 29, 31, 33};
        int index = 0;

        for (MonthlyCardType type : cardTypes) {
            if (index >= slots.length) break;

            ItemStack cardItem = createCardTypeItem(type);
            int slot = slots[index++];

            gui.setItem(slot, cardItem, event -> {
                if (plugin.getService().activateCard(player.getUniqueId(), type.getId(), true)) {
                    player.sendMessage(msg.green("成功购买并激活月卡: " + type.getDisplayName()));
                    openMainMenu(player);
                } else {
                    player.sendMessage(msg.red("购买失败，请检查余额是否充足!"));
                }
            });
        }

        gui.open(player);
    }

    /**
     * 打开奖励预览
     */
    public void openRewardPreview(Player player, MonthlyCardData data) {
        Optional<MonthlyCardType> typeOpt = plugin.getCardType(data.getCardType());
        if (typeOpt.isEmpty()) {
            player.sendMessage(msg.red("无法加载月卡数据!"));
            return;
        }

        MonthlyCardType type = typeOpt.get();
        int currentDay = data.getDaysSinceActivation();
        int totalPages = (int) Math.ceil(type.getDailyRewards().size() / 28.0);

        openRewardPage(player, data, type, 1, totalPages, currentDay);
    }

    private void openRewardPage(Player player, MonthlyCardData data, MonthlyCardType type,
                                int page, int totalPages, int currentDay) {
        String title = "<gold>☆ 奖励预览 - 第" + page + "/" + totalPages + "页 ☆";
        GUI gui = GUIBuilder.create(title, 6)
            .setFiller(Material.BLACK_STAINED_GLASS_PANE)
            .build();

        // 装饰边框
        ItemStack border = createItem(Material.BLUE_STAINED_GLASS_PANE, Component.text(" "));
        gui.fillEmptySlots(border);

        // 返回按钮
        ItemStack backItem = createItem(Material.ARROW, Component.text("返回主菜单").color(NamedTextColor.YELLOW));
        gui.setItem(45, backItem, event -> openMainMenu(player));

        // 上一页
        if (page > 1) {
            ItemStack prevItem = createItem(Material.PAPER, Component.text("上一页").color(NamedTextColor.YELLOW));
            gui.setItem(48, prevItem, event ->
                openRewardPage(player, data, type, page - 1, totalPages, currentDay));
        }

        // 下一页
        if (page < totalPages) {
            ItemStack nextItem = createItem(Material.PAPER, Component.text("下一页").color(NamedTextColor.YELLOW));
            gui.setItem(50, nextItem, event ->
                openRewardPage(player, data, type, page + 1, totalPages, currentDay));
        }

        // 关闭按钮
        ItemStack closeItem = createItem(Material.BARRIER, Component.text("关闭").color(NamedTextColor.RED));
        gui.setItem(49, closeItem, event -> gui.close(player));

        // 显示奖励
        int startDay = (page - 1) * 28 + 1;
        int endDay = Math.min(startDay + 27, type.getDailyRewards().size());

        int slot = 10;
        for (int day = startDay; day <= endDay; day++) {
            if (slot % 9 == 8) {
                slot += 2;
            }
            if (slot >= 44) break;

            DailyReward reward = type.getRewardForDay(day);
            boolean isCurrentDay = day == currentDay;
            boolean isPast = day < currentDay;
            boolean isClaimed = data.getClaimedDays().contains("day" + day);

            ItemStack rewardItem = createRewardPreviewItem(day, reward, isCurrentDay, isPast, isClaimed);
            gui.setItem(slot, rewardItem);

            slot++;
        }

        gui.open(player);
    }

    // ==================== 物品创建方法 ====================

    private ItemStack createStatusItem(MonthlyCardData data) {
        Material material = data.hasActiveCard() ? Material.GOLDEN_CARROT : Material.BARRIER;
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();

        if (data.hasActiveCard()) {
            Optional<MonthlyCardType> typeOpt = plugin.getCardType(data.getCardType());
            String typeName = typeOpt.map(MonthlyCardType::getDisplayName).orElse(data.getCardType());

            meta.displayName(Component.text("✦ 月卡状态 ✦").color(NamedTextColor.GOLD).decoration(TextDecoration.BOLD, true));

            List<Component> lore = new ArrayList<>();
            lore.add(Component.empty());
            lore.add(Component.text("月卡类型: ").color(NamedTextColor.YELLOW).append(Component.text(typeName).color(NamedTextColor.WHITE)));
            lore.add(Component.text("剩余天数: ").color(NamedTextColor.YELLOW).append(Component.text(data.getRemainingDaysInt() + " 天").color(NamedTextColor.GREEN)));
            lore.add(Component.text("已签到: ").color(NamedTextColor.YELLOW).append(Component.text(data.getTotalClaimedDays() + " 天").color(NamedTextColor.AQUA)));
            lore.add(Component.text("当前第: ").color(NamedTextColor.YELLOW).append(Component.text(data.getDaysSinceActivation() + " 天").color(NamedTextColor.AQUA)));
            lore.add(Component.empty());

            if (data.canClaimToday()) {
                lore.add(Component.text("✔ 今日奖励可领取!").color(NamedTextColor.GREEN).decoration(TextDecoration.BOLD, true));
            } else {
                lore.add(Component.text("今日奖励已领取").color(NamedTextColor.GRAY));
            }

            meta.lore(lore);
        } else {
            meta.displayName(Component.text("✘ 未激活月卡").color(NamedTextColor.RED).decoration(TextDecoration.BOLD, true));

            List<Component> lore = new ArrayList<>();
            lore.add(Component.empty());
            lore.add(Component.text("你还没有激活月卡").color(NamedTextColor.GRAY));
            lore.add(Component.text("点击购买月卡开始领取每日奖励!").color(NamedTextColor.YELLOW));
            lore.add(Component.empty());
            lore.add(Component.text("月卡特权:").color(NamedTextColor.AQUA));
            lore.add(Component.text("- 每日点券奖励").color(NamedTextColor.GRAY));
            lore.add(Component.text("- 每日游戏币奖励").color(NamedTextColor.GRAY));
            lore.add(Component.text("- 累计签到额外奖励").color(NamedTextColor.GRAY));

            meta.lore(lore);
        }

        item.setItemMeta(meta);
        return item;
    }

    private ItemStack createDailyRewardItem(MonthlyCardData data) {
        Material material = data.canClaimToday() ? Material.CHEST : Material.TRAPPED_CHEST;
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();

        if (data.canClaimToday()) {
            meta.displayName(Component.text("✦ 领取每日奖励 ✦").color(NamedTextColor.GREEN).decoration(TextDecoration.BOLD, true));
            meta.addEnchant(org.bukkit.enchantments.Enchantment.UNBREAKING, 1, true);
            meta.addItemFlags(ItemFlag.HIDE_ENCHANTS);
        } else if (!data.hasActiveCard()) {
            meta.displayName(Component.text("未激活月卡").color(NamedTextColor.RED));
        } else {
            meta.displayName(Component.text("今日已领取").color(NamedTextColor.GRAY));
        }

        List<Component> lore = new ArrayList<>();
        lore.add(Component.empty());

        if (data.hasActiveCard()) {
            Optional<MonthlyCardType> typeOpt = plugin.getCardType(data.getCardType());
            if (typeOpt.isPresent()) {
                MonthlyCardType type = typeOpt.get();
                int day = data.getDaysSinceActivation();
                DailyReward reward = type.getRewardForDay(day);

                if (reward != null && reward.hasAnyReward()) {
                    lore.add(Component.text("今日奖励内容:").color(NamedTextColor.YELLOW));
                    if (reward.getPoints() > 0) {
                        lore.add(Component.text("  + " + reward.getPoints() + " 点券").color(NamedTextColor.GOLD));
                    }
                    if (reward.getMoney() > 0) {
                        lore.add(Component.text("  + " + reward.getMoney() + " 游戏币").color(NamedTextColor.GREEN));
                    }
                }
            }
            lore.add(Component.empty());
        }

        if (data.canClaimToday()) {
            lore.add(Component.text("点击领取今日奖励!").color(NamedTextColor.GREEN).decoration(TextDecoration.BOLD, true));
        } else if (!data.hasActiveCard()) {
            lore.add(Component.text("请先购买月卡").color(NamedTextColor.RED));
        } else {
            lore.add(Component.text("明日再来领取吧~").color(NamedTextColor.GRAY));
        }

        meta.lore(lore);
        item.setItemMeta(meta);
        return item;
    }

    private ItemStack createDetailsItem(MonthlyCardData data) {
        Material material = data.hasActiveCard() ? Material.BOOK : Material.PAPER;
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();

        meta.displayName(Component.text("📖 月卡详情").color(NamedTextColor.AQUA).decoration(TextDecoration.BOLD, true));

        List<Component> lore = new ArrayList<>();
        lore.add(Component.empty());

        if (data.hasActiveCard()) {
            lore.add(Component.text("点击查看30天完整奖励列表").color(NamedTextColor.YELLOW));
            lore.add(Component.text("了解每一天的奖励内容").color(NamedTextColor.GRAY));
        } else {
            lore.add(Component.text("激活月卡后查看详情").color(NamedTextColor.GRAY));
        }

        lore.add(Component.empty());
        lore.add(Component.text("点击打开奖励预览").color(NamedTextColor.AQUA));

        meta.lore(lore);
        item.setItemMeta(meta);
        return item;
    }

    private ItemStack createBuyItem() {
        ItemStack item = new ItemStack(Material.EMERALD);
        ItemMeta meta = item.getItemMeta();

        meta.displayName(Component.text("💎 购买月卡").color(NamedTextColor.GREEN).decoration(TextDecoration.BOLD, true));

        List<Component> lore = new ArrayList<>();
        lore.add(Component.empty());
        lore.add(Component.text("点击查看所有可购买的月卡").color(NamedTextColor.YELLOW));
        lore.add(Component.text("多种类型任你选择").color(NamedTextColor.GRAY));
        lore.add(Component.empty());
        lore.add(Component.text("可用月卡类型:").color(NamedTextColor.AQUA));

        for (MonthlyCardType type : plugin.getCardManager().getAllCardTypes()) {
            lore.add(Component.text("- " + type.getDisplayName()).color(NamedTextColor.GRAY));
        }

        lore.add(Component.empty());
        lore.add(Component.text("点击打开商店!").color(NamedTextColor.GREEN).decoration(TextDecoration.BOLD, true));

        meta.lore(lore);
        item.setItemMeta(meta);
        return item;
    }

    private ItemStack createPreviewItem() {
        ItemStack item = new ItemStack(Material.ENDER_EYE);
        ItemMeta meta = item.getItemMeta();

        meta.displayName(Component.text("👁 奖励预览").color(NamedTextColor.LIGHT_PURPLE).decoration(TextDecoration.BOLD, true));

        List<Component> lore = new ArrayList<>();
        lore.add(Component.empty());
        lore.add(Component.text("查看所有月卡的奖励内容").color(NamedTextColor.YELLOW));
        lore.add(Component.text("对比不同月卡的价值").color(NamedTextColor.GRAY));
        lore.add(Component.empty());
        lore.add(Component.text("点击预览").color(NamedTextColor.AQUA));

        meta.lore(lore);
        item.setItemMeta(meta);
        return item;
    }

    private ItemStack createHelpItem() {
        ItemStack item = new ItemStack(Material.WRITABLE_BOOK);
        ItemMeta meta = item.getItemMeta();

        meta.displayName(Component.text("❓ 使用帮助").color(NamedTextColor.YELLOW).decoration(TextDecoration.BOLD, true));

        List<Component> lore = new ArrayList<>();
        lore.add(Component.empty());
        lore.add(Component.text("月卡系统说明:").color(NamedTextColor.YELLOW));
        lore.add(Component.text("1. 购买月卡后有效期30天").color(NamedTextColor.GRAY));
        lore.add(Component.text("2. 每天可领取一次奖励").color(NamedTextColor.GRAY));
        lore.add(Component.text("3. 累计签到有额外奖励").color(NamedTextColor.GRAY));
        lore.add(Component.text("4. 过期后需重新购买").color(NamedTextColor.GRAY));
        lore.add(Component.empty());
        lore.add(Component.text("命令:").color(NamedTextColor.AQUA));
        lore.add(Component.text("/monthlycard - 打开此菜单").color(NamedTextColor.GRAY));
        lore.add(Component.text("/monthlycard claim - 领取奖励").color(NamedTextColor.GRAY));

        meta.lore(lore);
        item.setItemMeta(meta);
        return item;
    }

    private ItemStack createCardTypeItem(MonthlyCardType type) {
        Material material;
        NamedTextColor color;

        switch (type.getId().toLowerCase()) {
            case "vip":
            case "premium":
                material = Material.NETHER_STAR;
                color = NamedTextColor.LIGHT_PURPLE;
                break;
            case "normal":
            default:
                material = Material.SUNFLOWER;
                color = NamedTextColor.YELLOW;
                break;
        }

        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();

        meta.displayName(msg.parse(type.getDisplayName()).color(color).decoration(TextDecoration.BOLD, true));

        List<Component> lore = new ArrayList<>();
        lore.add(Component.empty());
        lore.add(Component.text("价格: " + type.getPrice() + " " + type.getCurrencyType()).color(NamedTextColor.YELLOW));
        lore.add(Component.text("时长: " + type.getDurationDays() + " 天").color(NamedTextColor.AQUA));
        lore.add(Component.empty());
        lore.add(Component.text(type.getDescription()).color(NamedTextColor.GRAY));
        lore.add(Component.empty());

        // 显示前几天的奖励预览
        lore.add(Component.text("奖励预览:").color(NamedTextColor.YELLOW));
        for (int i = 1; i <= Math.min(3, type.getDailyRewards().size()); i++) {
            DailyReward reward = type.getRewardForDay(i);
            if (reward != null) {
                lore.add(Component.text("第" + i + "天: " + reward.getPoints() + "点券").color(NamedTextColor.GRAY));
            }
        }
        if (type.getDailyRewards().size() > 3) {
            lore.add(Component.text("... 共" + type.getDailyRewards().size() + "天奖励").color(NamedTextColor.GRAY));
        }

        lore.add(Component.empty());
        lore.add(Component.text("点击购买!").color(NamedTextColor.GREEN).decoration(TextDecoration.BOLD, true));

        meta.lore(lore);
        item.setItemMeta(meta);
        return item;
    }

    private ItemStack createRewardPreviewItem(int day, DailyReward reward, boolean isCurrentDay,
                                               boolean isPast, boolean isClaimed) {
        Material material;
        NamedTextColor color;

        if (isClaimed) {
            material = Material.LIME_STAINED_GLASS_PANE;
            color = NamedTextColor.GREEN;
        } else if (isCurrentDay) {
            material = Material.GOLD_BLOCK;
            color = NamedTextColor.GOLD;
        } else if (isPast) {
            material = Material.GRAY_STAINED_GLASS_PANE;
            color = NamedTextColor.GRAY;
        } else {
            material = Material.WHITE_STAINED_GLASS_PANE;
            color = NamedTextColor.WHITE;
        }

        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();

        String prefix = isClaimed ? "✔ " : (isCurrentDay ? "▶ " : "  ");
        meta.displayName(Component.text(prefix + "第 " + day + " 天").color(color));

        List<Component> lore = new ArrayList<>();
        lore.add(Component.empty());

        if (reward != null && reward.hasAnyReward()) {
            if (reward.getPoints() > 0) {
                lore.add(Component.text("点券: +" + reward.getPoints()).color(NamedTextColor.GOLD));
            }
            if (reward.getMoney() > 0) {
                lore.add(Component.text("游戏币: +" + reward.getMoney()).color(NamedTextColor.GREEN));
            }
            if (!reward.getItems().isEmpty()) {
                lore.add(Component.text("物品: " + reward.getItems().size() + " 个").color(NamedTextColor.YELLOW));
            }
        } else {
            lore.add(Component.text("无特殊奖励").color(NamedTextColor.GRAY));
        }

        lore.add(Component.empty());

        if (isClaimed) {
            lore.add(Component.text("✔ 已领取").color(NamedTextColor.GREEN));
        } else if (isCurrentDay) {
            lore.add(Component.text("▶ 今日可领取").color(NamedTextColor.GOLD));
        } else if (isPast) {
            lore.add(Component.text("✘ 已过期").color(NamedTextColor.RED));
        } else {
            lore.add(Component.text("未解锁").color(NamedTextColor.GRAY));
        }

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

    // ==================== 事件监听 ====================

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        // GUI点击事件由RPGCore GUIManager统一处理
        // 这里可以添加额外的全局处理逻辑
    }
}
