package cn.guangdian.monthlycard.gui;

import cn.guangdian.monthlycard.GuangDianMonthlyCard;
import cn.guangdian.monthlycard.config.GUIConfig;
import cn.guangdian.monthlycard.config.GUIConfig.*;
import cn.guangdian.monthlycard.data.DailyReward;
import cn.guangdian.monthlycard.data.MonthlyCardData;
import cn.guangdian.monthlycard.data.MonthlyCardType;
import cn.guangdian.rpgcore.RPGCore;
import cn.guangdian.rpgcore.gui.GUI;
import cn.guangdian.rpgcore.gui.GUIBuilder;
import cn.guangdian.rpgcore.integration.ExternalServiceIntegration;
import cn.guangdian.rpgcore.message.MiniMessageService;
import net.kyori.adventure.text.Component;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.time.LocalDate;
import java.util.*;

public class MonthlyCardGUI {

    private final GuangDianMonthlyCard plugin;
    private final GUIConfig guiConfig;
    private final MiniMessageService miniMessage;
    private final ExternalServiceIntegration externalServices;
    private static final int REWARDS_PER_PAGE = 28;

    public MonthlyCardGUI(GuangDianMonthlyCard plugin) {
        this.plugin = plugin;
        this.guiConfig = new GUIConfig(plugin);

        RPGCore rpgCore = RPGCore.getInstance();
        if (rpgCore != null) {
            this.miniMessage = rpgCore.getMiniMessageService();
            this.externalServices = rpgCore.getExternalServices();
        } else {
            this.miniMessage = MiniMessageService.getInstance();
            this.externalServices = null;
        }
    }

    public void reloadConfig() {
        guiConfig.loadConfig();
    }

    public void openMainMenu(Player player) {
        MenuConfig menu = guiConfig.getMainMenu();
        if (menu == null) {
            player.sendMessage(miniMessage.red("GUI配置加载失败"));
            return;
        }

        MonthlyCardData data = plugin.getCardManager().getPlayerData(player.getUniqueId());
        if (data == null) {
            player.sendMessage(miniMessage.red("无法获取玩家数据"));
            return;
        }

        boolean hasCard = data.isActive();

        GUIBuilder builder = GUIBuilder.create(menu.title, menu.rows);

        if (menu.filler != null) {
            ItemStack filler = createItemFromConfig(menu.filler);
            for (int i = 0; i < menu.rows * 9; i++) {
                builder.setItem(i, filler);
            }
        }

        if (menu.border != null && menu.border.enabled) {
            ItemStack borderItem = createItemFromConfig(menu.border.item);
            for (int slot : menu.border.slots) {
                if (slot < menu.rows * 9) {
                    builder.setItem(slot, borderItem);
                }
            }
        }

        ButtonConfig statusBtn = menu.buttons.get("status");
        if (statusBtn != null) {
            String state = hasCard ? "active" : "inactive";
            ItemConfig itemConfig = statusBtn.states.get(state);
            if (itemConfig != null) {
                builder.setItem(statusBtn.slot, createItemFromConfig(itemConfig, data));
            }
        }

        ButtonConfig dailyBtn = menu.buttons.get("daily-reward");
        if (dailyBtn != null) {
            String state;
            if (!hasCard) {
                state = "no-card";
            } else if (data.hasClaimedToday()) {
                state = "claimed";
            } else {
                state = "can-claim";
            }
            ItemConfig itemConfig = dailyBtn.states.get(state);
            if (itemConfig != null) {
                ItemStack item = createItemFromConfig(itemConfig, data);
                builder.setItem(dailyBtn.slot, item, event -> {
                    handleDailyRewardClick(player);
                });
            }
        }

        ButtonConfig makeupBtn = menu.buttons.get("makeup");
        if (makeupBtn != null) {
            List<String> missedDays = plugin.getCardManager().getMissedDays(player.getUniqueId());
            String state = hasCard && !missedDays.isEmpty() ? "available" : "unavailable";
            ItemConfig itemConfig = makeupBtn.states.get(state);
            if (itemConfig != null) {
                ItemStack item = createItemFromConfig(itemConfig, data);
                builder.setItem(makeupBtn.slot, item, event -> {
                    handleMakeupClick(player);
                });
            }
        }

        ButtonConfig previewBtn = menu.buttons.get("preview");
        if (previewBtn != null && previewBtn.defaultItem != null) {
            builder.setItem(previewBtn.slot, createItemFromConfig(previewBtn.defaultItem), event -> {
                openRewardPreview(player, 0);
            });
        }

        ButtonConfig milestoneBtn = menu.buttons.get("milestone");
        if (milestoneBtn != null && milestoneBtn.defaultItem != null) {
            builder.setItem(milestoneBtn.slot, createItemFromConfig(milestoneBtn.defaultItem), event -> {
                handleMilestoneClick(player);
            });
        }

        ButtonConfig closeBtn = menu.buttons.get("close");
        if (closeBtn != null && closeBtn.defaultItem != null) {
            builder.setItem(closeBtn.slot, createItemFromConfig(closeBtn.defaultItem), event -> {
                player.closeInventory();
            });
        }

        GUI gui = builder.build();
        gui.open(player);
    }

    public void openShopMenu(Player player) {
        MenuConfig menu = guiConfig.getShopMenu();
        if (menu == null) {
            player.sendMessage(miniMessage.red("商店配置加载失败"));
            return;
        }

        GUIBuilder builder = GUIBuilder.create(menu.title, menu.rows);

        if (menu.filler != null) {
            ItemStack filler = createItemFromConfig(menu.filler);
            for (int i = 0; i < menu.rows * 9; i++) {
                builder.setItem(i, filler);
            }
        }

        if (menu.border != null && menu.border.enabled) {
            ItemStack borderItem = createItemFromConfig(menu.border.item);
            for (int slot : menu.border.slots) {
                if (slot < menu.rows * 9) {
                    builder.setItem(slot, borderItem);
                }
            }
        }

        ButtonConfig backBtn = menu.buttons.get("back");
        if (backBtn != null && backBtn.defaultItem != null) {
            builder.setItem(backBtn.slot, createItemFromConfig(backBtn.defaultItem), event -> {
                openMainMenu(player);
            });
        }

        ButtonConfig closeBtn = menu.buttons.get("close");
        if (closeBtn != null && closeBtn.defaultItem != null) {
            builder.setItem(closeBtn.slot, createItemFromConfig(closeBtn.defaultItem), event -> {
                player.closeInventory();
            });
        }

        List<MonthlyCardType> cardTypes = new ArrayList<>(plugin.getCardManager().getAllCardTypes());
        List<Integer> slots = menu.cardSlots;

        for (int i = 0; i < Math.min(cardTypes.size(), slots.size()); i++) {
            MonthlyCardType cardType = cardTypes.get(i);
            int slot = slots.get(i);

            ItemConfig template = menu.cardTemplates.get(cardType.getId());
            if (template == null) {
                template = menu.cardTemplates.getOrDefault("normal", 
                    menu.cardTemplates.values().iterator().next());
            }

            ItemStack item = createCardItem(template, cardType);
            builder.setItem(slot, item, event -> {
                handleCardPurchase(player, cardType);
            });
        }

        GUI gui = builder.build();
        gui.open(player);
    }

    public void openRewardPreview(Player player, int page) {
        MenuConfig menu = guiConfig.getRewardPreviewMenu();
        if (menu == null) {
            player.sendMessage(miniMessage.red("奖励预览配置加载失败"));
            return;
        }

        String title = menu.title.replace("%page%", String.valueOf(page + 1))
                                 .replace("%total_pages%", "3");

        GUIBuilder builder = GUIBuilder.create(title, menu.rows);

        if (menu.filler != null) {
            ItemStack filler = createItemFromConfig(menu.filler);
            for (int i = 0; i < menu.rows * 9; i++) {
                builder.setItem(i, filler);
            }
        }

        if (menu.border != null && menu.border.enabled) {
            ItemStack borderItem = createItemFromConfig(menu.border.item);
            for (int slot : menu.border.slots) {
                if (slot < menu.rows * 9) {
                    builder.setItem(slot, borderItem);
                }
            }
        }

        ButtonConfig backBtn = menu.buttons.get("back");
        if (backBtn != null && backBtn.defaultItem != null) {
            builder.setItem(backBtn.slot, createItemFromConfig(backBtn.defaultItem), event -> {
                openMainMenu(player);
            });
        }

        ButtonConfig closeBtn = menu.buttons.get("close");
        if (closeBtn != null && closeBtn.defaultItem != null) {
            builder.setItem(closeBtn.slot, createItemFromConfig(closeBtn.defaultItem), event -> {
                player.closeInventory();
            });
        }

        if (page > 0) {
            ButtonConfig prevBtn = menu.buttons.get("prev-page");
            if (prevBtn != null && prevBtn.defaultItem != null) {
                builder.setItem(prevBtn.slot, createItemFromConfig(prevBtn.defaultItem), event -> {
                    openRewardPreview(player, page - 1);
                });
            }
        }

        if (page < 2) {
            ButtonConfig nextBtn = menu.buttons.get("next-page");
            if (nextBtn != null && nextBtn.defaultItem != null) {
                builder.setItem(nextBtn.slot, createItemFromConfig(nextBtn.defaultItem), event -> {
                    openRewardPreview(player, page + 1);
                });
            }
        }

        MonthlyCardData data = plugin.getCardManager().getPlayerData(player.getUniqueId());
        int currentDay = data != null && data.isActive() ? data.getCurrentDay() : 0;

        int startDay = page * REWARDS_PER_PAGE + 1;
        int endDay = Math.min(startDay + REWARDS_PER_PAGE - 1, 30);

        int slot = menu.rewardStartSlot;
        for (int day = startDay; day <= endDay; day++) {
            if (slot >= menu.rows * 9) break;

            String state;
            if (data != null && data.isActive()) {
                if (day < currentDay) {
                    state = data.hasClaimedDay(day) ? "claimed" : "past";
                } else if (day == currentDay) {
                    state = "current";
                } else {
                    state = "future";
                }
            } else {
                state = "future";
            }

            ItemConfig itemConfig = menu.rewardStates.get(state);
            if (itemConfig != null) {
                DailyReward reward = plugin.getCardManager().getRewardForDay(day);
                ItemStack item = createRewardItem(itemConfig, day, reward);
                builder.setItem(slot, item);
            }

            slot++;
            if (slot % 9 == 8) slot += 2;
        }

        GUI gui = builder.build();
        gui.open(player);
    }

    private void handleDailyRewardClick(Player player) {
        if (plugin.getCardManager().claimDailyReward(player.getUniqueId())) {
            String msg = plugin.getConfigManager().getSuccessMessage("claim")
                .replace("%points%", "50");
            player.sendMessage(miniMessage.colorize(msg));
            openMainMenu(player);
        } else {
            String msg = plugin.getConfigManager().getErrorMessage("already-claimed");
            if (msg.isEmpty()) {
                msg = "<red>✘ 无法领取奖励，请检查月卡状态或今日是否已领取";
            }
            player.sendMessage(miniMessage.colorize(msg));
        }
    }

    private void handleMakeupClick(Player player) {
        List<String> missedDays = plugin.getCardManager().getMissedDays(player.getUniqueId());
        if (!missedDays.isEmpty()) {
            String yesterday = missedDays.get(missedDays.size() - 1);
            if (plugin.getCardManager().makeupClaim(player.getUniqueId(), yesterday)) {
                String msg = plugin.getConfigManager().getSuccessMessage("makeup")
                    .replace("%day%", yesterday);
                player.sendMessage(miniMessage.colorize(msg));
                openMainMenu(player);
            } else {
                String msg = plugin.getConfigManager().getErrorMessage("makeup-limit");
                if (msg.isEmpty()) {
                    msg = "<red>补签失败，请检查补签次数或余额";
                }
                player.sendMessage(miniMessage.colorize(msg));
            }
        }
    }

    private void handleMilestoneClick(Player player) {
        player.sendMessage(miniMessage.colorize("<gold>🏆 累计签到奖励"));
        player.sendMessage(miniMessage.colorize("<yellow>7天: <green>200点券 + 500游戏币"));
        player.sendMessage(miniMessage.colorize("<yellow>14天: <green>500点券 + 1500游戏币"));
        player.sendMessage(miniMessage.colorize("<yellow>21天: <green>1000点券 + 3000游戏币"));
        player.sendMessage(miniMessage.colorize("<yellow>30天: <green>2000点券 + 8000游戏币"));
    }

    private void handleCardPurchase(Player player, MonthlyCardType cardType) {
        if (plugin.getCardManager().purchaseCard(player.getUniqueId(), cardType.getId())) {
            player.sendMessage(miniMessage.green("✔ 成功购买 " + cardType.getDisplayName() + "！"));
            openMainMenu(player);
        } else {
            player.sendMessage(miniMessage.red("✘ 购买失败，请检查余额是否充足"));
        }
    }

    private ItemStack createItemFromConfig(ItemConfig config) {
        return createItemFromConfig(config, null);
    }

    private ItemStack createItemFromConfig(ItemConfig config, MonthlyCardData data) {
        if (config == null) {
            return new ItemStack(Material.STONE);
        }

        ItemStack item = new ItemStack(config.material);
        ItemMeta meta = item.getItemMeta();
        if (meta == null) return item;

        String name = config.name;
        List<String> lore = new ArrayList<>(config.lore);

        if (data != null && data.isActive()) {
            name = name.replace("%card_type%", data.getCardTypeId())
                       .replace("%remaining_days%", String.valueOf(plugin.getCardManager().getRemainingDays(data.getPlayerId())))
                       .replace("%claimed_days%", String.valueOf(data.getClaimedDays().size()))
                       .replace("%current_day%", String.valueOf(data.getCurrentDay()));

            for (int i = 0; i < lore.size(); i++) {
                lore.set(i, lore.get(i)
                    .replace("%card_type%", data.getCardTypeId())
                    .replace("%remaining_days%", String.valueOf(plugin.getCardManager().getRemainingDays(data.getPlayerId())))
                    .replace("%claimed_days%", String.valueOf(data.getClaimedDays().size()))
                    .replace("%current_day%", String.valueOf(data.getCurrentDay()))
                    .replace("%claim_status%", data.hasClaimedToday() ? "<green>今日已领取" : "<yellow>今日可领取"));
            }
        }

        meta.displayName(miniMessage.colorize(name));

        List<Component> loreComponents = new ArrayList<>();
        for (String line : lore) {
            loreComponents.add(miniMessage.colorize(line));
        }
        meta.lore(loreComponents);

        if (config.glow) {
            meta.setEnchantmentGlintOverride(true);
        }

        item.setItemMeta(meta);
        return item;
    }

    private ItemStack createCardItem(ItemConfig config, MonthlyCardType cardType) {
        ItemStack item = new ItemStack(config.material);
        ItemMeta meta = item.getItemMeta();
        if (meta == null) return item;

        String name = config.name.replace("%display_name%", cardType.getDisplayName());

        List<String> lore = new ArrayList<>();
        for (String line : config.lore) {
            lore.add(line
                .replace("%display_name%", cardType.getDisplayName())
                .replace("%price%", String.valueOf(cardType.getPrice()))
                .replace("%currency%", cardType.getCurrency())
                .replace("%duration%", String.valueOf(cardType.getDuration()))
                .replace("%description%", cardType.getDescription()));
        }

        meta.displayName(miniMessage.colorize(name));

        List<Component> loreComponents = new ArrayList<>();
        for (String line : lore) {
            loreComponents.add(miniMessage.colorize(line));
        }
        meta.lore(loreComponents);

        if (config.glow) {
            meta.setEnchantmentGlintOverride(true);
        }

        item.setItemMeta(meta);
        return item;
    }

    private ItemStack createRewardItem(ItemConfig config, int day, DailyReward reward) {
        ItemStack item = new ItemStack(config.material);
        ItemMeta meta = item.getItemMeta();
        if (meta == null) return item;

        String name = config.name.replace("%day%", String.valueOf(day));

        List<String> lore = new ArrayList<>();
        for (String line : config.lore) {
            String newLine = line.replace("%day%", String.valueOf(day));
            if (newLine.contains("%reward_info%")) {
                if (reward != null) {
                    newLine = newLine.replace("%reward_info%", reward.getDescription());
                } else {
                    newLine = newLine.replace("%reward_info%", "无奖励");
                }
            }
            lore.add(newLine);
        }

        meta.displayName(miniMessage.colorize(name));

        List<Component> loreComponents = new ArrayList<>();
        for (String line : lore) {
            loreComponents.add(miniMessage.colorize(line));
        }
        meta.lore(loreComponents);

        if (config.glow) {
            meta.setEnchantmentGlintOverride(true);
        }

        item.setItemMeta(meta);
        return item;
    }
}
