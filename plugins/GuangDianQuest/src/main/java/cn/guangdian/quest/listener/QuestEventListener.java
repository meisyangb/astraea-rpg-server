package cn.guangdian.quest.listener;

import cn.guangdian.quest.GuangDianQuest;
import cn.guangdian.quest.model.PlayerQuestData;
import cn.guangdian.quest.model.Quest;
import cn.guangdian.quest.model.QuestObjective;
import org.bukkit.Location;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.player.PlayerFishEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.ItemStack;

import java.lang.reflect.Method;
import java.util.UUID;

public class QuestEventListener implements Listener {

    private final GuangDianQuest plugin;

    private Object mythicMobManager;
    private Method isMythicMobMethod;
    private Method getMythicMobInstanceMethod;
    private boolean mythicMobsAvailable = false;

    public QuestEventListener(GuangDianQuest plugin) {
        this.plugin = plugin;
        initMythicMobs();
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

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onEntityDeath(EntityDeathEvent event) {
        LivingEntity entity = event.getEntity();
        Player killer = entity.getKiller();
        if (killer == null) return;

        UUID playerId = killer.getUniqueId();
        PlayerQuestData data = plugin.getProgressManager().getPlayerData(playerId);

        for (String questId : data.getActiveQuestIds()) {
            Quest quest = plugin.getQuestManager().getQuest(questId);
            if (quest == null) continue;

            for (int i = 0; i < quest.getObjectiveCount(); i++) {
                QuestObjective obj = quest.getObjective(i);
                if (obj.getType() == QuestObjective.ObjectiveType.KILL) {
                    if (matchesKillTarget(entity, obj.getTarget())) {
                        plugin.getProgressManager().incrementProgress(playerId, questId, i, 1);
                    }
                }
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

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onPlayerPickupItem(org.bukkit.event.player.PlayerPickupItemEvent event) {
        Player player = event.getPlayer();
        UUID playerId = player.getUniqueId();
        PlayerQuestData data = plugin.getProgressManager().getPlayerData(playerId);

        ItemStack item = event.getItem().getItemStack();

        for (String questId : data.getActiveQuestIds()) {
            Quest quest = plugin.getQuestManager().getQuest(questId);
            if (quest == null) continue;

            for (int i = 0; i < quest.getObjectiveCount(); i++) {
                QuestObjective obj = quest.getObjective(i);
                if (obj.getType() == QuestObjective.ObjectiveType.COLLECT) {
                    if (item.getType().name().equalsIgnoreCase(obj.getTarget())) {
                        plugin.getProgressManager().incrementProgress(playerId, questId, i, item.getAmount());
                    }
                }
            }
        }
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onBlockBreak(BlockBreakEvent event) {
        Player player = event.getPlayer();
        UUID playerId = player.getUniqueId();
        PlayerQuestData data = plugin.getProgressManager().getPlayerData(playerId);

        String blockType = event.getBlock().getType().name();

        for (String questId : data.getActiveQuestIds()) {
            Quest quest = plugin.getQuestManager().getQuest(questId);
            if (quest == null) continue;

            for (int i = 0; i < quest.getObjectiveCount(); i++) {
                QuestObjective obj = quest.getObjective(i);
                if (obj.getType() == QuestObjective.ObjectiveType.BREAK) {
                    if (blockType.equalsIgnoreCase(obj.getTarget())) {
                        plugin.getProgressManager().incrementProgress(playerId, questId, i, 1);
                    }
                }
            }
        }
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onPlayerFish(PlayerFishEvent event) {
        if (event.getState() != PlayerFishEvent.State.CAUGHT_FISH) return;

        Player player = event.getPlayer();
        UUID playerId = player.getUniqueId();
        PlayerQuestData data = plugin.getProgressManager().getPlayerData(playerId);

        for (String questId : data.getActiveQuestIds()) {
            Quest quest = plugin.getQuestManager().getQuest(questId);
            if (quest == null) continue;

            for (int i = 0; i < quest.getObjectiveCount(); i++) {
                QuestObjective obj = quest.getObjective(i);
                if (obj.getType() == QuestObjective.ObjectiveType.FISH) {
                    plugin.getProgressManager().incrementProgress(playerId, questId, i, 1);
                }
            }
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

        for (String questId : data.getActiveQuestIds()) {
            Quest quest = plugin.getQuestManager().getQuest(questId);
            if (quest == null) continue;

            for (int i = 0; i < quest.getObjectiveCount(); i++) {
                QuestObjective obj = quest.getObjective(i);
                if (obj.getType() == QuestObjective.ObjectiveType.REACH) {
                    if (obj.getWorld() != null && loc.getWorld() != null
                        && loc.getWorld().getName().equals(obj.getWorld())) {
                        double distSq = loc.distanceSquared(new Location(loc.getWorld(), obj.getX(), obj.getY(), obj.getZ()));
                        double radius = obj.getRadius();
                        if (distSq <= radius * radius) {
                            plugin.getProgressManager().incrementProgress(playerId, questId, i, 1);
                        }
                    }
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

        for (String questId : data.getActiveQuestIds()) {
            Quest quest = plugin.getQuestManager().getQuest(questId);
            if (quest == null) continue;

            for (int i = 0; i < quest.getObjectiveCount(); i++) {
                QuestObjective obj = quest.getObjective(i);
                if (obj.getType() == QuestObjective.ObjectiveType.TALK) {
                    String target = obj.getTarget();
                    if (target.equalsIgnoreCase(entity.getType().name())) {
                        plugin.getProgressManager().incrementProgress(playerId, questId, i, 1);
                    }
                    if (target.startsWith("npc:") && entity.hasMetadata("NPC")) {
                        String npcId = target.substring(4);
                        if (entity.hasMetadata("npc-id")) {
                            String entityId = entity.getMetadata("npc-id").get(0).asString();
                            if (npcId.equalsIgnoreCase(entityId)) {
                                plugin.getProgressManager().incrementProgress(playerId, questId, i, 1);
                            }
                        }
                    }
                }
            }
        }
    }

    public void onNPCInteract(String npcId, Player player) {
        UUID playerId = player.getUniqueId();
        PlayerQuestData data = plugin.getProgressManager().getPlayerData(playerId);

        for (String questId : data.getActiveQuestIds()) {
            Quest quest = plugin.getQuestManager().getQuest(questId);
            if (quest == null) continue;

            for (int i = 0; i < quest.getObjectiveCount(); i++) {
                QuestObjective obj = quest.getObjective(i);
                if (obj.getType() == QuestObjective.ObjectiveType.TALK) {
                    if (obj.getTarget().equalsIgnoreCase(npcId)) {
                        plugin.getProgressManager().incrementProgress(playerId, questId, i, 1);
                    }
                }
            }
        }
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
}
