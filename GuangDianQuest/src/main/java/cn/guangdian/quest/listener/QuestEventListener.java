package cn.guangdian.quest.listener;

import cn.guangdian.quest.GuangDianQuest;
import cn.guangdian.quest.model.PlayerQuestData;
import cn.guangdian.quest.model.Quest;
import cn.guangdian.quest.model.QuestObjective;
import cn.guangdian.quest.service.ChatMessageService;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.player.PlayerFishEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.NamespacedKey;
import org.bukkit.persistence.PersistentDataType;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class QuestEventListener implements Listener {

    private final GuangDianQuest plugin;

    private Object mythicMobManager;
    private Method isMythicMobMethod;
    private Method getMythicMobInstanceMethod;
    private boolean mythicMobsAvailable = false;

    // RPGItems 支持
    private boolean rpgItemsAvailable = false;
    private Object rpgItemsItemService;

    public QuestEventListener(GuangDianQuest plugin) {
        this.plugin = plugin;
        initMythicMobs();
        initRPGItems();
    }

    private void initRPGItems() {
        if (!org.bukkit.Bukkit.getPluginManager().isPluginEnabled("RPGItems")) return;
        try {
            Class<?> rpgItemsClass = Class.forName("cn.guangdian.rpgitems.RPGItems");
            Object instance = rpgItemsClass.getMethod("getInstance").invoke(null);
            rpgItemsItemService = rpgItemsClass.getMethod("getItemService").invoke(instance);
            rpgItemsAvailable = true;
            plugin.getLogger().info("RPGItems 支持已启用");
        } catch (Exception e) {
            plugin.getLogger().warning("RPGItems 集成初始化失败: " + e.getMessage());
        }
    }

    private void initMythicMobs() {
        if (!org.bukkit.Bukkit.getPluginManager().isPluginEnabled("MythicMobs")) return;
        try {
            Class<?> mythicBukkitClass = Class.forName("io.lumine.mythic.bukkit.MythicBukkit");
            Object inst = mythicBukkitClass.getMethod("inst").invoke(null);
            mythicMobManager = mythicBukkitClass.getMethod("getMobManager").invoke(inst);

            Class<?> mobManagerClass = mythicMobManager.getClass();
            isMythicMobMethod = mobManagerClass.getMethod("isMythicMob", org.bukkit.entity.Entity.class);
            getMythicMobInstanceMethod = mobManagerClass.getMethod("getMythicMobInstance", org.bukkit.entity.Entity.class);

            mythicMobsAvailable = true;
            plugin.getLogger().info("MythicMobs 支持已启用");
        } catch (Exception e) {
            plugin.getLogger().warning("MythicMobs 集成初始化失败: " + e.getMessage());
        }
    }

    /**
     * 按类型索引的目标包装，包含所属任务ID、任务对象和目标对象
     */
    private record TypedObjective(String questId, Quest quest, QuestObjective objective) {}

    /**
     * 获取玩家活跃任务中指定类型的目标列表
     */
    private List<TypedObjective> getObjectivesByType(PlayerQuestData data, QuestObjective.ObjectiveType type) {
        List<TypedObjective> result = new ArrayList<>();
        for (String questId : data.getActiveQuestIds()) {
            Quest quest = plugin.getQuestManager().getQuest(questId);
            if (quest == null) continue;
            for (int i = 0; i < quest.getObjectiveCount(); i++) {
                QuestObjective obj = quest.getObjective(i);
                if (obj.getType() == type) {
                    result.add(new TypedObjective(questId, quest, obj));
                }
            }
        }
        return result;
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onEntityDeath(EntityDeathEvent event) {
        LivingEntity entity = event.getEntity();
        Player killer = entity.getKiller();
        if (killer == null) return;

        UUID playerId = killer.getUniqueId();
        PlayerQuestData data = plugin.getProgressManager().getPlayerData(playerId);

        for (TypedObjective to : getObjectivesByType(data, QuestObjective.ObjectiveType.KILL)) {
            QuestObjective obj = to.objective();
            if (matchesKillTarget(entity, obj.getTarget())) {
                plugin.getProgressManager().incrementProgress(playerId, to.questId(), obj.getIndex(), 1);
            }
        }
    }

    private boolean matchesKillTarget(LivingEntity entity, String target) {
        String entityType = entity.getType().name();
        if (entityType.equalsIgnoreCase(target)) {
            return true;
        }

        if (mythicMobsAvailable && isMythicMob(entity)) {
            String mythicId = getMythicMobId(entity);
            if (mythicId != null && mythicId.equalsIgnoreCase(target)) {
                return true;
            }
        }

        return false;
    }

    private boolean isMythicMob(LivingEntity entity) {
        if (!mythicMobsAvailable || mythicMobManager == null || isMythicMobMethod == null) {
            return false;
        }
        try {
            return (boolean) isMythicMobMethod.invoke(mythicMobManager, entity);
        } catch (Exception e) {
            return false;
        }
    }

    private String getMythicMobId(LivingEntity entity) {
        if (!mythicMobsAvailable || mythicMobManager == null || getMythicMobInstanceMethod == null) {
            return null;
        }
        try {
            Object mythicMobInstance = getMythicMobInstanceMethod.invoke(mythicMobManager, entity);
            if (mythicMobInstance != null) {
                String[] methodNames = {"getInternalName", "getMobType", "getType", "getEntityTypeName"};
                for (String methodName : methodNames) {
                    try {
                        Method method = mythicMobInstance.getClass().getMethod(methodName);
                        Object result = method.invoke(mythicMobInstance);
                        if (result instanceof String) {
                            return (String) result;
                        }
                    } catch (NoSuchMethodException ignored) {}
                }
                try {
                    Method getMobTypeObjMethod = mythicMobInstance.getClass().getMethod("getMobType");
                    Object mobType = getMobTypeObjMethod.invoke(mythicMobInstance);
                    if (mobType != null) {
                        Method getInternalNameMethod = mobType.getClass().getMethod("getInternalName");
                        return (String) getInternalNameMethod.invoke(mobType);
                    }
                } catch (Exception ignored) {}
                try {
                    Method getNameMethod = mythicMobInstance.getClass().getMethod("getName");
                    return (String) getNameMethod.invoke(mythicMobInstance);
                } catch (Exception ignored) {}
            }
        } catch (Exception ignored) {}
        return null;
    }

    private String getMythicItemType(ItemStack item) {
        if (item == null || !item.hasItemMeta()) return null;
        ItemMeta meta = item.getItemMeta();
        NamespacedKey typeKey = new NamespacedKey("mythicmobs", "type");
        return meta.getPersistentDataContainer().get(typeKey, PersistentDataType.STRING);
    }

    /**
     * 获取物品的RPGItems ID
     * 支持两种方式：
     * 1. 通过ItemService API获取
     * 2. 直接读取PDC (备用方案)
     */
    private String getRPGItemId(ItemStack item) {
        if (item == null || !item.hasItemMeta()) return null;

        // 方式1: 通过ItemService API
        if (rpgItemsAvailable && rpgItemsItemService != null) {
            try {
                java.lang.reflect.Method getItemIdMethod = rpgItemsItemService.getClass().getMethod("getItemId", ItemStack.class);
                Object result = getItemIdMethod.invoke(rpgItemsItemService, item);
                if (result instanceof java.util.Optional<?> opt && opt.isPresent()) {
                    return (String) opt.get();
                }
            } catch (Exception ignored) {}
        }

        // 方式2: 直接读取PDC (备用)
        ItemMeta meta = item.getItemMeta();
        NamespacedKey idKey = new NamespacedKey("rpgitems", "id");
        return meta.getPersistentDataContainer().get(idKey, PersistentDataType.STRING);
    }

    private boolean matchesSubmitTarget(ItemStack item, String target) {
        if (item == null || target == null) return false;

        String itemType = item.getType().name();
        if (itemType.equalsIgnoreCase(target)) {
            return true;
        }

        // 检查 MythicMobs 物品
        String mythicType = getMythicItemType(item);
        if (mythicType != null && mythicType.equalsIgnoreCase(target)) {
            return true;
        }

        // 检查 RPGItems 物品
        String rpgItemId = getRPGItemId(item);
        if (rpgItemId != null && rpgItemId.equalsIgnoreCase(target)) {
            return true;
        }

        // 支持带前缀的格式: type:id
        if (target.contains(":")) {
            String[] parts = target.split(":", 2);
            if (parts.length == 2) {
                String prefix = parts[0].toLowerCase();
                String id = parts[1];

                switch (prefix) {
                    case "mythic":
                        return mythicType != null && mythicType.equalsIgnoreCase(id);
                    case "rpgitems":
                    case "rpg":
                        return rpgItemId != null && rpgItemId.equalsIgnoreCase(id);
                    case "material":
                    case "vanilla":
                        return itemType.equalsIgnoreCase(id);
                }
            }
        }

        return false;
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onPlayerPickupItem(org.bukkit.event.player.PlayerPickupItemEvent event) {
        Player player = event.getPlayer();
        UUID playerId = player.getUniqueId();
        PlayerQuestData data = plugin.getProgressManager().getPlayerData(playerId);

        ItemStack item = event.getItem().getItemStack();

        for (TypedObjective to : getObjectivesByType(data, QuestObjective.ObjectiveType.COLLECT)) {
            QuestObjective obj = to.objective();
            if (matchesSubmitTarget(item, obj.getTarget())) {
                plugin.getProgressManager().incrementProgress(playerId, to.questId(), obj.getIndex(), item.getAmount());
            }
        }
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onBlockBreak(BlockBreakEvent event) {
        Player player = event.getPlayer();
        UUID playerId = player.getUniqueId();
        PlayerQuestData data = plugin.getProgressManager().getPlayerData(playerId);

        String blockType = event.getBlock().getType().name();

        for (TypedObjective to : getObjectivesByType(data, QuestObjective.ObjectiveType.BREAK)) {
            QuestObjective obj = to.objective();
            if (blockType.equalsIgnoreCase(obj.getTarget())) {
                plugin.getProgressManager().incrementProgress(playerId, to.questId(), obj.getIndex(), 1);
            }
        }
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onPlayerFish(PlayerFishEvent event) {
        if (event.getState() != PlayerFishEvent.State.CAUGHT_FISH) return;

        Player player = event.getPlayer();
        UUID playerId = player.getUniqueId();
        PlayerQuestData data = plugin.getProgressManager().getPlayerData(playerId);

        for (TypedObjective to : getObjectivesByType(data, QuestObjective.ObjectiveType.FISH)) {
            plugin.getProgressManager().incrementProgress(playerId, to.questId(), to.objective().getIndex(), 1);
        }
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onPlayerMove(PlayerMoveEvent event) {
        if (event.getFrom().getBlockX() == event.getTo().getBlockX()
            && event.getFrom().getBlockY() == event.getTo().getBlockY()
            && event.getFrom().getBlockZ() == event.getTo().getBlockZ()) {
            return;
        }

        Player player = event.getPlayer();
        UUID playerId = player.getUniqueId();
        PlayerQuestData data = plugin.getProgressManager().getPlayerData(playerId);
        Location loc = player.getLocation();

        for (TypedObjective to : getObjectivesByType(data, QuestObjective.ObjectiveType.REACH)) {
            QuestObjective obj = to.objective();
            if (obj.getWorld() != null && loc.getWorld() != null
                && loc.getWorld().getName().equals(obj.getWorld())) {
                double distSq = loc.distanceSquared(new Location(loc.getWorld(), obj.getX(), obj.getY(), obj.getZ()));
                double radius = obj.getRadius();
                if (distSq <= radius * radius) {
                    plugin.getProgressManager().incrementProgress(playerId, to.questId(), obj.getIndex(), 1);
                }
            }
        }
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onPlayerInteractEntity(org.bukkit.event.player.PlayerInteractEntityEvent event) {
        if (!(event.getRightClicked() instanceof org.bukkit.entity.Entity entity)) return;
        Player player = event.getPlayer();
        UUID playerId = player.getUniqueId();
        PlayerQuestData data = plugin.getProgressManager().getPlayerData(playerId);

        for (TypedObjective to : getObjectivesByType(data, QuestObjective.ObjectiveType.TALK)) {
            QuestObjective obj = to.objective();
            String target = obj.getTarget();
            if (target.equalsIgnoreCase(entity.getType().name())) {
                plugin.getProgressManager().incrementProgress(playerId, to.questId(), obj.getIndex(), 1);
            }
            if (target.startsWith("npc:") && entity.hasMetadata("NPC")) {
                String npcId = target.substring(4);
                if (entity.hasMetadata("npc-id")) {
                    String entityId = entity.getMetadata("npc-id").get(0).asString();
                    if (npcId.equalsIgnoreCase(entityId)) {
                        plugin.getProgressManager().incrementProgress(playerId, to.questId(), obj.getIndex(), 1);
                    }
                }
            }
        }
    }

    public void onNPCInteract(String npcId, Player player) {
        UUID playerId = player.getUniqueId();
        PlayerQuestData data = plugin.getProgressManager().getPlayerData(playerId);

        plugin.getLogger().info("[DEBUG] onNPCInteract called: npcId=" + npcId + ", player=" + player.getName());
        plugin.getLogger().info("[DEBUG] Active quest count: " + data.getActiveQuestCount());

        boolean found = false;
        for (TypedObjective to : getObjectivesByType(data, QuestObjective.ObjectiveType.TALK)) {
            QuestObjective obj = to.objective();
            plugin.getLogger().info("[DEBUG] Found TALK objective, target=" + obj.getTarget() + ", npcId=" + npcId);
            if (obj.getTarget().equalsIgnoreCase(npcId)) {
                int[] progress = data.getProgress(to.questId());
                int current = (progress != null && obj.getIndex() < progress.length) ? progress[obj.getIndex()] : 0;
                plugin.getLogger().info("[DEBUG] Progress: " + current + "/" + obj.getAmount());
                if (current < obj.getAmount()) {
                    plugin.getProgressManager().incrementProgress(playerId, to.questId(), obj.getIndex(), 1);
                    plugin.getLogger().info("[DEBUG] Sending info message to player");
                    player.sendMessage("§b§lℹ §b任务进度更新: " + to.quest().getName());
                    found = true;
                }
            }
        }

        if (!found && data.getActiveQuestCount() == 0) {
            plugin.getLogger().info("[DEBUG] No active quests, sending hint");
            player.sendMessage("§e提示: 你还没有接取任何任务，使用 §a/quest list §e查看可接取的任务");
        }
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onPlayerInteract(PlayerInteractEvent event) {
        if (event.useItemInHand() == org.bukkit.event.Event.Result.DENY) return;

        Player player = event.getPlayer();
        ItemStack itemInHand = player.getInventory().getItemInMainHand();

        if (itemInHand == null || itemInHand.getType() == Material.AIR) return;

        UUID playerId = player.getUniqueId();
        PlayerQuestData data = plugin.getProgressManager().getPlayerData(playerId);

        boolean hasSubmitQuest = false;
        for (TypedObjective to : getObjectivesByType(data, QuestObjective.ObjectiveType.SUBMIT)) {
            hasSubmitQuest = true;
            QuestObjective obj = to.objective();
            if (matchesSubmitTarget(itemInHand, obj.getTarget())) {
                int[] progress = data.getProgress(to.questId());
                int currentProgress = (progress != null && obj.getIndex() < progress.length) ? progress[obj.getIndex()] : 0;
                int required = obj.getAmount();

                if (currentProgress < required) {
                    int amountToConsume = Math.min(itemInHand.getAmount(), required - currentProgress);

                    ItemStack consumed = itemInHand.clone();
                    consumed.setAmount(amountToConsume);

                    plugin.getProgressManager().incrementProgress(playerId, to.questId(), obj.getIndex(), amountToConsume);

                    itemInHand.setAmount(itemInHand.getAmount() - amountToConsume);

                    MiniMessage mm = MiniMessage.miniMessage();
                    player.sendMessage(mm.deserialize("<green>已提交 " + amountToConsume + " 个物品，任务进度: " +
                        (currentProgress + amountToConsume) + "/" + required));

                    if (currentProgress + amountToConsume >= required) {
                        player.sendMessage(mm.deserialize("<gold>目标完成！记得提交任务领取奖励！"));
                    }

                    event.setCancelled(true);
                }
            }
        }

        if (!hasSubmitQuest && data.getActiveQuestIds().isEmpty()) return;
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        PlayerQuestData data = plugin.getProgressManager().getPlayerData(player.getUniqueId());

        if (data.needsDailyReset()) {
            java.util.Set<String> dailyIds = plugin.getQuestManager().getDailyQuestIds();
            data.resetDaily(dailyIds);
        }
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onPlayerQuit(PlayerQuitEvent event) {
        plugin.savePlayerOnQuit(event.getPlayer());
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onPlayerCommand(PlayerCommandPreprocessEvent event) {
        String message = event.getMessage();
        if (!message.startsWith("/quest:")) return;

        event.setCancelled(true);

        Player player = event.getPlayer();
        String[] parts = message.substring(7).split(" ");
        if (parts.length == 0) return;

        String action = parts[0].toLowerCase();
        String questId = parts.length > 1 ? parts[1].toLowerCase() : null;

        ChatMessageService msgService = plugin.getChatMessageService();

        switch (action) {
            case "detail" -> {
                if (questId != null) {
                    msgService.sendQuestDetail(player, questId);
                }
            }
            case "abandon" -> {
                if (questId != null) {
                    plugin.getQuestManager().abandonQuest(player.getUniqueId(), questId);
                    Quest quest = plugin.getQuestManager().getQuest(questId);
                    String name = quest != null ? quest.getName() : questId;
                    msgService.sendSuccess(player, "已放弃任务: " + name);
                    msgService.sendQuestList(player);
                }
            }
            case "accept" -> {
                if (questId != null) {
                    plugin.getQuestManager().acceptQuest(player.getUniqueId(), questId);
                    Quest quest = plugin.getQuestManager().getQuest(questId);
                    String name = quest != null ? quest.getName() : questId;
                    msgService.sendSuccess(player, "已接取任务: " + name);
                    msgService.sendQuestList(player);
                }
            }
            case "complete" -> {
                if (questId != null) {
                    plugin.getQuestManager().completeQuest(player.getUniqueId(), questId);
                    Quest quest = plugin.getQuestManager().getQuest(questId);
                    String name = quest != null ? quest.getName() : questId;
                    msgService.sendSuccess(player, "已完成任务: " + name);
                    msgService.sendQuestList(player);
                }
            }
            case "daily" -> msgService.sendDailyQuests(player);
            case "available" -> msgService.sendQuestList(player);
            case "main" -> msgService.sendQuestList(player);
        }
    }
}
