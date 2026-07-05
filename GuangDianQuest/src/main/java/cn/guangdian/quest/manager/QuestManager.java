package cn.guangdian.quest.manager;

import cn.guangdian.quest.GuangDianQuest;
import cn.guangdian.quest.model.PlayerQuestData;
import cn.guangdian.quest.model.Quest;
import cn.guangdian.quest.model.QuestReward;
import cn.guangdian.quest.model.QuestType;
import cn.guangdian.quest.repository.PlayerQuestRepository;
import cn.guangdian.quest.repository.QuestRepository;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.HashSet;
import java.util.UUID;

public class QuestManager {

    private final GuangDianQuest plugin;
    private final QuestRepository questRepository;
    private final PlayerQuestRepository playerRepository;

    public QuestManager(GuangDianQuest plugin, QuestRepository questRepository, PlayerQuestRepository playerRepository) {
        this.plugin = plugin;
        this.questRepository = questRepository;
        this.playerRepository = playerRepository;
    }

    public Quest getQuest(String questId) {
        return questRepository.getQuest(questId);
    }

    public boolean exists(String questId) {
        return questRepository.exists(questId);
    }

    public int getQuestCount() {
        return questRepository.getQuestCount();
    }

    public List<String> getAvailableQuests(UUID playerId) {
        List<String> available = new ArrayList<>();
        PlayerQuestData data = playerRepository.getPlayerData(playerId);

        for (Quest quest : questRepository.getAllQuests()) {
            if (!data.isQuestActive(quest.getId()) && !data.isQuestCompleted(quest.getId())) {
                if (checkPrerequisites(data, quest)) {
                    available.add(quest.getId());
                }
            }
        }

        return available;
    }

    private boolean checkPrerequisites(PlayerQuestData data, Quest quest) {
        for (String prereq : quest.getPrerequisites()) {
            if (!data.isQuestCompleted(prereq)) {
                return false;
            }
        }
        return true;
    }

    public boolean acceptQuest(UUID playerId, String questId) {
        Quest quest = getQuest(questId);
        if (quest == null) return false;

        PlayerQuestData data = playerRepository.getPlayerData(playerId);
        if (data.isQuestActive(questId) || data.isQuestCompleted(questId)) {
            return false;
        }

        if (!checkPrerequisites(data, quest)) {
            return false;
        }

        if (data.getActiveQuestCount() >= plugin.getMaxActiveQuests()) {
            return false;
        }

        Player player = Bukkit.getPlayer(playerId);
        if (player != null && quest.getRequiredLevel() > 0) {
            int level = getPlayerLevel(playerId);
            if (level < quest.getRequiredLevel()) {
                return false;
            }
        }

        data.acceptQuest(questId, quest.getObjectiveCount());

        publishQuestEvent(playerId, questId, "ACCEPT");

        // 立即保存玩家数据（同步）
        savePlayerData(playerId);

        return true;
    }

    private void savePlayerData(UUID playerId) {
        playerRepository.savePlayerData(playerId);
    }

    public boolean completeQuest(UUID playerId, String questId) {
        Quest quest = getQuest(questId);
        if (quest == null) return false;

        PlayerQuestData data = playerRepository.getPlayerData(playerId);
        if (!data.isQuestActive(questId)) return false;

        int[] progress = data.getProgress(questId);
        for (int i = 0; i < quest.getObjectiveCount(); i++) {
            if (progress[i] < quest.getObjective(i).getAmount()) {
                return false;
            }
        }

        data.completeQuest(questId, quest.getType());

        if (quest.getQuestLine() != null && !quest.getQuestLine().isEmpty()) {
            int currentProgress = data.getQuestLineProgress(quest.getQuestLine());
            if (quest.getOrder() > currentProgress) {
                data.updateQuestLineProgress(quest.getQuestLine(), quest.getOrder());
            }
        }

        grantRewards(playerId, quest);

        publishQuestEvent(playerId, questId, "COMPLETE");

        // 立即保存玩家数据（同步）
        savePlayerData(playerId);

        // 触发解锁通知
        Player completer = Bukkit.getPlayer(playerId);
        if (completer != null && plugin.getQuestUnlockManager() != null) {
            plugin.getQuestUnlockManager().onQuestComplete(completer, questId);
        }

        boolean broadcast = plugin.getConfig().getBoolean(
            "quest-complete-broadcast." + quest.getType().name().toLowerCase(), false);
        if (broadcast) {
            Player player = Bukkit.getPlayer(playerId);
            if (player != null) {
                Component msg = GuangDianQuest.color("<yellow><bold>[公告] <green>玩家 <white>" + player.getName()
                    + " <green>完成了 " + quest.getType().getPrefix() + " " + quest.getName() + "<green>！");
                for (Player p : Bukkit.getOnlinePlayers()) {
                    p.sendMessage(msg);
                }
            }
        }

        return true;
    }

    public boolean abandonQuest(UUID playerId, String questId) {
        PlayerQuestData data = playerRepository.getPlayerData(playerId);
        if (!data.isQuestActive(questId)) return false;

        data.abandonQuest(questId);

        publishQuestEvent(playerId, questId, "ABANDON");

        // 立即保存玩家数据（同步）
        savePlayerData(playerId);

        return true;
    }

    private void grantRewards(UUID playerId, Quest quest) {
        Player player = Bukkit.getPlayer(playerId);
        QuestReward reward = quest.getReward();
        if (reward == null || !reward.hasRewards()) return;

        if (reward.getExperience() > 0) {
            giveExperience(playerId, reward.getExperience());
        }

        if (reward.getPoints() > 0) {
            givePoints(playerId, reward.getPoints());
        }

        if (!reward.getItems().isEmpty()) {
            giveItems(playerId, reward.getItems());
        }

        if (!reward.getCommands().isEmpty()) {
            executeCommands(playerId, reward.getCommands());
        }

        if (!reward.getMessages().isEmpty() && player != null) {
            for (String msg : reward.getMessages()) {
                player.sendMessage(GuangDianQuest.color(msg));
            }
        }
    }

    private void giveExperience(UUID playerId, int amount) {
        Player player = Bukkit.getPlayer(playerId);
        if (player == null) return;

        // 优先给职业阶位经验（通过 ClassService）
        try {
            cn.guangdian.rpgcore.RPGCore rpgCore = cn.guangdian.rpgcore.RPGCore.getInstance();
            if (rpgCore != null) {
                cn.guangdian.rpgcore.api.ServiceRegistry registry = rpgCore.getServiceRegistry();
                if (registry != null) {
                    // 尝试获取 ClassService
                    Object classService = registry.getService(
                        Class.forName("cn.guangdian.classsystem.api.ClassService")
                    );
                    if (classService != null) {
                        java.lang.reflect.Method addExpMethod = classService.getClass().getMethod("addExp", java.util.UUID.class, long.class);
                        addExpMethod.invoke(classService, playerId, (long) amount);
                        plugin.getLogger().info("任务奖励职业经验: " + amount + " -> " + player.getName());
                        return;
                    }
                }
            }
        } catch (Exception e) {
            // ClassService 不可用，回退到原版经验
            plugin.getLogger().warning("ClassService 不可用，回退到原版经验: " + e.getMessage());
        }

        // 回退：给原版经验（但可能被 GuangDianExpControl 拦截）
        player.giveExp(amount);
    }

    private void givePoints(UUID playerId, int amount) {
        try {
            cn.guangdian.rpgcore.RPGCore rpgCore = cn.guangdian.rpgcore.RPGCore.getInstance();
            if (rpgCore != null) {
                cn.guangdian.rpgcore.service.api.PointsService pointsService =
                    rpgCore.getServiceRegistry().getService(cn.guangdian.rpgcore.service.api.PointsService.class);
                if (pointsService != null) {
                    pointsService.addBalance(playerId, amount, "QUEST_REWARD");
                    return;
                }
            }
        } catch (Exception ignored) {}

        if (Bukkit.getPluginManager().isPluginEnabled("GuangDianPoints")) {
            try {
                Object pointsPlugin = Bukkit.getPluginManager().getPlugin("GuangDianPoints");
                if (pointsPlugin != null) {
                    java.lang.reflect.Method getApiMethod = pointsPlugin.getClass().getMethod("getAPI");
                    Object api = getApiMethod.invoke(pointsPlugin);
                    java.lang.reflect.Method addMethod = api.getClass().getMethod("addBalance", UUID.class, long.class);
                    addMethod.invoke(api, playerId, (long) amount);
                }
            } catch (Exception ignored) {}
        }
    }

    @SuppressWarnings("deprecation")
    private void giveItems(UUID playerId, java.util.Map<String, Integer> items) {
        Player player = Bukkit.getPlayer(playerId);
        if (player == null) return;

        for (java.util.Map.Entry<String, Integer> entry : items.entrySet()) {
            String itemKey = entry.getKey();
            int amount = entry.getValue();
            String[] parts = itemKey.split(":", 2);
            String type = parts[0];
            String id = parts.length > 1 ? parts[1] : parts[0];

            org.bukkit.inventory.ItemStack itemStack = null;

            if ("vanilla".equalsIgnoreCase(type)) {
                try {
                    org.bukkit.Material material = org.bukkit.Material.valueOf(id.toUpperCase());
                    itemStack = new org.bukkit.inventory.ItemStack(material, amount);
                } catch (IllegalArgumentException ignored) {}
            } else if ("mythicmobs".equalsIgnoreCase(type)) {
                itemStack = getMythicItem(id, amount);
            } else if ("rpgitems".equalsIgnoreCase(type)) {
                itemStack = getRPGItem(id, amount);
            } else if (parts.length == 1) {
                // 无前缀，尝试作为原版物品
                try {
                    org.bukkit.Material material = org.bukkit.Material.valueOf(itemKey.toUpperCase());
                    itemStack = new org.bukkit.inventory.ItemStack(material, amount);
                } catch (IllegalArgumentException ignored) {}
            }

            if (itemStack != null) {
                java.util.Map<Integer, org.bukkit.inventory.ItemStack> leftover = player.getInventory().addItem(itemStack);
                if (!leftover.isEmpty()) {
                    for (org.bukkit.inventory.ItemStack drop : leftover.values()) {
                        player.getWorld().dropItemNaturally(player.getLocation(), drop);
                    }
                }
            }
        }
    }

    private org.bukkit.inventory.ItemStack getRPGItem(String itemId, int amount) {
        try {
            // 直接调用RPGItems API
            Class<?> rpgItemsClass = Class.forName("cn.guangdian.rpgitems.RPGItems");
            Object instance = rpgItemsClass.getMethod("getInstance").invoke(null);
            Object itemService = rpgItemsClass.getMethod("getItemService").invoke(instance);
            
            java.lang.reflect.Method createItemMethod = itemService.getClass().getMethod("createItem", String.class);
            Object result = createItemMethod.invoke(itemService, itemId);
            
            if (result instanceof java.util.Optional<?> opt && opt.isPresent()) {
                org.bukkit.inventory.ItemStack item = (org.bukkit.inventory.ItemStack) opt.get();
                item.setAmount(amount);
                return item;
            }
        } catch (Exception e) {
            plugin.getLogger().warning("获取RPGItems物品失败: " + itemId + " - " + e.getMessage());
        }
        return null;
    }

    private org.bukkit.inventory.ItemStack getMythicItem(String itemId, int amount) {
        try {
            Class<?> mythicBukkitClass = Class.forName("io.lumine.mythic.bukkit.MythicBukkit");
            Object inst = mythicBukkitClass.getMethod("inst").invoke(null);
            Object itemManager = mythicBukkitClass.getMethod("getItemManager").invoke(inst);
            java.lang.reflect.Method getItemStackMethod = itemManager.getClass().getMethod("getItemStack", String.class);
            org.bukkit.inventory.ItemStack item = (org.bukkit.inventory.ItemStack) getItemStackMethod.invoke(itemManager, itemId);
            if (item != null) {
                item.setAmount(amount);
            }
            return item;
        } catch (Exception e) {
            return null;
        }
    }

    private void executeCommands(UUID playerId, List<String> commands) {
        Player player = Bukkit.getPlayer(playerId);
        String playerName = player != null ? player.getName() : "Unknown";

        for (String cmd : commands) {
            String parsed = cmd.replace("{player}", playerName).replace("{uuid}", playerId.toString());
            if (parsed.startsWith("[console]")) {
                String consoleCmd = parsed.substring("[console]".length()).trim();
                Bukkit.dispatchCommand(Bukkit.getConsoleSender(), consoleCmd);
            } else if (parsed.startsWith("[message]")) {
                String message = parsed.substring("[message]".length()).trim();
                if (player != null) {
                    player.sendMessage(GuangDianQuest.color(message));
                }
            } else {
                Bukkit.dispatchCommand(Bukkit.getConsoleSender(), parsed);
            }
        }
    }

    private int getPlayerLevel(UUID playerId) {
        Player player = Bukkit.getPlayer(playerId);
        if (player == null) return 0;
        
        cn.guangdian.rpgcore.integration.ExternalServiceIntegration externalServices = plugin.getExternalServices();
        if (externalServices != null) {
            try {
                String levelStr = externalServices.parsePlaceholders(player, "%rpgcore_level%");
                if (levelStr != null && !levelStr.isEmpty() && !levelStr.equals("%rpgcore_level%")) {
                    try {
                        return Integer.parseInt(levelStr);
                    } catch (NumberFormatException ignored) {}
                }
                levelStr = externalServices.parsePlaceholders(player, "%player_level%");
                if (levelStr != null && !levelStr.isEmpty() && !levelStr.equals("%player_level%")) {
                    try {
                        return Integer.parseInt(levelStr);
                    } catch (NumberFormatException ignored) {}
                }
            } catch (Exception ignored) {}
        }
        return player.getLevel();
    }

    private void publishQuestEvent(UUID playerId, String questId, String action) {
        try {
            cn.guangdian.rpgcore.RPGCore rpgCore = cn.guangdian.rpgcore.RPGCore.getInstance();
            if (rpgCore == null) return;
            cn.guangdian.rpgcore.api.EventBus eventBus = rpgCore.getEventBus();
            if (eventBus == null) return;

            Quest quest = getQuest(questId);
            String questName = quest != null ? quest.getName() : questId;
            String questType = quest != null ? quest.getType().name() : "UNKNOWN";

            cn.guangdian.rpgcore.event.events.RpgQuestEvent event = null;
            switch (action) {
                case "ACCEPT" -> event = new cn.guangdian.rpgcore.event.events.RpgQuestEvent.Accept(playerId, questId, questName, questType);
                case "COMPLETE" -> event = new cn.guangdian.rpgcore.event.events.RpgQuestEvent.Complete(playerId, questId, questName, questType);
                case "ABANDON" -> event = new cn.guangdian.rpgcore.event.events.RpgQuestEvent.Abandon(playerId, questId, questName, questType);
            }

            if (event != null) {
                eventBus.publish(event);
            }
        } catch (Exception ignored) {}
    }

    public boolean canComplete(UUID playerId, String questId) {
        Quest quest = getQuest(questId);
        if (quest == null) return false;

        PlayerQuestData data = playerRepository.getPlayerData(playerId);
        if (!data.isQuestActive(questId)) return false;

        int[] progress = data.getProgress(questId);
        for (int i = 0; i < quest.getObjectiveCount(); i++) {
            if (progress[i] < quest.getObjective(i).getAmount()) {
                return false;
            }
        }

        return true;
    }

    public boolean canAccept(UUID playerId, String questId) {
        Quest quest = getQuest(questId);
        if (quest == null) return false;

        PlayerQuestData data = playerRepository.getPlayerData(playerId);
        if (data.isQuestActive(questId) || data.isQuestCompleted(questId)) {
            return false;
        }

        if (data.getActiveQuestCount() >= plugin.getMaxActiveQuests()) {
            return false;
        }

        if (quest.getRequiredLevel() > 0 && getPlayerLevel(playerId) < quest.getRequiredLevel()) {
            return false;
        }

        return checkPrerequisites(data, quest);
    }

    public Set<String> getDailyQuestIds() {
        Set<String> ids = new HashSet<>();
        for (Quest quest : questRepository.getAllQuests()) {
            if (quest.getType() == QuestType.DAILY) {
                ids.add(quest.getId());
            }
        }
        return ids;
    }

    public PlayerQuestRepository getPlayerRepository() {
        return playerRepository;
    }

    public QuestRepository getQuestRepository() {
        return questRepository;
    }
}
