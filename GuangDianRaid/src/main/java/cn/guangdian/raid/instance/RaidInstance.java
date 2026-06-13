package cn.guangdian.raid.instance;

import cn.guangdian.raid.GuangDianRaid;
import cn.guangdian.raid.api.RaidEvent;
import cn.guangdian.raid.model.*;
import cn.guangdian.rpgcore.RPGCore;
import cn.guangdian.rpgcore.api.SyncScheduler;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.title.Title;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Item;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class RaidInstance {

    private final String instanceId;
    private final Raid raid;
    private final RaidTeam team;
    private final GuangDianRaid plugin;
    private final SyncScheduler scheduler;

    private RaidPhaseType currentPhase;
    private long startTime;
    private long phaseStartTime;
    private int phaseTimeRemaining;

    private int collectedIntel;
    private int killCount;
    private Set<String> unlockedAreas;
    private Map<String, Integer> objectiveProgress;
    private Set<UUID> spawnedEnemies;
    private Set<UUID> droppedItems;

    private DifficultyScaling currentDifficulty;
    private boolean extractionActive;
    private String activeExtractionPoint;

    private final Set<Long> scheduledTasks;
    private boolean active;

    public RaidInstance(String instanceId, Raid raid, List<Player> players, GuangDianRaid plugin) {
        this.instanceId = instanceId;
        this.raid = raid;
        this.plugin = plugin;
        this.scheduler = RPGCore.getInstance().getScheduler();

        Player leader = players.isEmpty() ? null : players.get(0);
        this.team = new RaidTeam(leader);
        for (int i = 1; i < players.size(); i++) {
            team.addMember(players.get(i));
        }

        this.currentPhase = RaidPhaseType.WAITING;
        this.collectedIntel = 0;
        this.killCount = 0;
        this.unlockedAreas = ConcurrentHashMap.newKeySet();
        this.objectiveProgress = new ConcurrentHashMap<>();
        this.spawnedEnemies = ConcurrentHashMap.newKeySet();
        this.droppedItems = ConcurrentHashMap.newKeySet();
        this.scheduledTasks = ConcurrentHashMap.newKeySet();
        this.active = true;

        this.currentDifficulty = raid.getDifficulty().scaleForPlayers(team.size());
    }

    public void start() {
        startTime = System.currentTimeMillis();
        
        Bukkit.getPluginManager().callEvent(new RaidEvent.RaidStartEvent(this));

        teleportPlayersToRaid();
        
        setPhase(RaidPhaseType.INFILTRATION);
        
        team.broadcastMessage(Component.text("═══════════════════════════════").color(NamedTextColor.GOLD));
        team.broadcastMessage(Component.text("  副本开始: ").color(NamedTextColor.YELLOW)
            .append(Component.text(raid.getName()).color(NamedTextColor.GOLD)));
        team.broadcastMessage(Component.text("  队伍人数: ").color(NamedTextColor.GRAY)
            .append(Component.text(String.valueOf(team.size())).color(NamedTextColor.GREEN)));
        team.broadcastMessage(Component.text("═══════════════════════════════").color(NamedTextColor.GOLD));

        startPhaseTimer();
        startMainTimer();
    }

    private void teleportPlayersToRaid() {
        World world = Bukkit.getWorld(raid.getWorldName());
        if (world == null) {
            world = createOrLoadWorld();
        }

        Location spawnLoc = raid.getSpawnLocation();
        if (spawnLoc == null) {
            spawnLoc = world.getSpawnLocation();
        } else {
            spawnLoc = spawnLoc.clone();
            spawnLoc.setWorld(world);
        }

        Location finalSpawn = spawnLoc;
        for (Player player : team.getOnlinePlayers()) {
            player.teleport(finalSpawn);
            player.setGameMode(org.bukkit.GameMode.SURVIVAL);
        }
    }

    private World createOrLoadWorld() {
        String worldName = raid.getWorldName();
        World world = Bukkit.getWorld(worldName);
        if (world == null) {
            world = Bukkit.getWorlds().get(0);
        }
        return world;
    }

    private void startMainTimer() {
        long taskId = scheduler.runSyncRepeating(() -> {
            if (!active) return;

            long elapsed = (System.currentTimeMillis() - startTime) / 1000;
            int remaining = raid.getTotalTimeLimit() - (int) elapsed;

            if (remaining <= 0) {
                fail("时间耗尽");
            } else if (remaining <= 60 && remaining % 10 == 0) {
                team.broadcastMessage(Component.text("剩余时间: " + remaining + "秒").color(NamedTextColor.RED));
            }
        }, 20L, 20L);
        scheduledTasks.add(taskId);
    }

    private void startPhaseTimer() {
        long taskId = scheduler.runSyncRepeating(() -> {
            if (!active) return;

            phaseTimeRemaining--;

            if (phaseTimeRemaining <= 0) {
                onPhaseTimeout();
            } else if (phaseTimeRemaining <= 30 && phaseTimeRemaining % 10 == 0) {
                team.broadcastMessage(Component.text("阶段剩余: " + phaseTimeRemaining + "秒").color(NamedTextColor.YELLOW));
            }
        }, 20L, 20L);
        scheduledTasks.add(taskId);
    }

    public void setPhase(RaidPhaseType newPhase) {
        RaidPhaseType oldPhase = this.currentPhase;
        this.currentPhase = newPhase;
        this.phaseStartTime = System.currentTimeMillis();

        Bukkit.getPluginManager().callEvent(new RaidEvent.RaidPhaseChangeEvent(this, oldPhase, newPhase));

        switch (newPhase) {
            case INFILTRATION -> {
                phaseTimeRemaining = 30;
                team.broadcastTitle("", "§e潜入副本...", 10, 40, 10);
            }
            case SEARCH -> {
                phaseTimeRemaining = raid.getSearchPhase() != null ? raid.getSearchPhase().getDuration() : 180;
                team.broadcastTitle("§6搜索阶段", "§e收集情报，解锁区域", 10, 60, 10);
                spawnIntelItems();
                plugin.getSpawnManager().spawnWavesForPhase(this, RaidPhaseType.SEARCH);
            }
            case COMBAT -> {
                phaseTimeRemaining = raid.getCombatPhase() != null ? raid.getCombatPhase().getDuration() : 300;
                team.broadcastTitle("§c战斗阶段", "§e消灭所有敌人", 10, 60, 10);
                plugin.getSpawnManager().spawnWavesForPhase(this, RaidPhaseType.COMBAT);
            }
            case EXTRACTION -> {
                phaseTimeRemaining = raid.getExtractPhase() != null ? raid.getExtractPhase().getDuration() : 120;
                extractionActive = true;
                team.broadcastTitle("§a撤离阶段", "§e前往撤离点", 10, 60, 10);
                team.broadcastSound(Sound.BLOCK_BELL_USE, 1.0f, 1.0f);
                plugin.getExtractionManager().activateExtractionPoints(this);
            }
            case COMPLETED -> complete();
            case FAILED -> {}
            default -> {}
        }
    }

    private void onPhaseTimeout() {
        switch (currentPhase) {
            case INFILTRATION -> setPhase(RaidPhaseType.SEARCH);
            case SEARCH -> {
                if (checkSearchObjectives()) {
                    setPhase(RaidPhaseType.COMBAT);
                } else {
                    fail("搜索阶段目标未完成");
                }
            }
            case COMBAT -> {
                if (checkCombatObjectives()) {
                    setPhase(RaidPhaseType.EXTRACTION);
                } else {
                    fail("战斗阶段目标未完成");
                }
            }
            case EXTRACTION -> fail("撤离失败");
            default -> {}
        }
    }

    private boolean checkSearchObjectives() {
        return collectedIntel >= getRequiredIntel();
    }

    private boolean checkCombatObjectives() {
        return spawnedEnemies.isEmpty() || killCount >= getRequiredKills();
    }

    private int getRequiredIntel() {
        if (raid.getSearchPhase() == null) return 1;
        return raid.getSearchPhase().getObjective().stream()
            .filter(o -> o.getType() == RaidObjective.ObjectiveType.COLLECT_INTEL)
            .mapToInt(RaidObjective::getAmount)
            .sum();
    }

    private int getRequiredKills() {
        if (raid.getCombatPhase() == null) return 0;
        return raid.getCombatPhase().getObjective().stream()
            .filter(o -> o.getType() == RaidObjective.ObjectiveType.KILL_MOBS)
            .mapToInt(RaidObjective::getAmount)
            .sum();
    }

    private void spawnIntelItems() {
        World world = getWorld();
        if (world == null) return;

        Random random = new Random();
        for (Intel intel : raid.getIntelItems().values()) {
            Location loc = intel.getRandomSpawnLocation(random);
            if (loc != null) {
                loc.setWorld(world);
                ItemStack item = intel.createItemStack();
                Item dropped = world.dropItem(loc, item);
                dropped.setPersistent(true);
                droppedItems.add(dropped.getUniqueId());
            }
        }
    }

    public void collectIntel(Player player, Intel intel) {
        RaidPlayer rp = team.getMember(player.getUniqueId());
        if (rp == null) return;

        collectedIntel += intel.getValue();
        rp.addIntel(intel.getValue());

        for (String areaId : intel.getUnlockAreas()) {
            unlockArea(areaId);
        }

        Bukkit.getPluginManager().callEvent(new RaidEvent.IntelCollectEvent(player, this, intel.getId(), intel.getValue()));

        team.broadcastMessage(Component.text(player.getName() + " 收集了情报 [" + intel.getName() + "]")
            .color(NamedTextColor.GREEN));

        if (checkSearchObjectives() && currentPhase == RaidPhaseType.SEARCH) {
            team.broadcastMessage(Component.text("搜索目标已完成！准备进入战斗阶段").color(NamedTextColor.GOLD));
        }
    }

    public void unlockArea(String areaId) {
        if (unlockedAreas.add(areaId)) {
            team.broadcastMessage(Component.text("区域已解锁: " + areaId).color(NamedTextColor.AQUA));
        }
    }

    public void onMobKill(Player killer, LivingEntity mob) {
        if (!spawnedEnemies.remove(mob.getUniqueId())) return;

        killCount++;
        RaidPlayer rp = team.getMember(killer.getUniqueId());
        if (rp != null) {
            rp.addKill();
        }

        if (checkCombatObjectives() && currentPhase == RaidPhaseType.COMBAT) {
            team.broadcastMessage(Component.text("战斗目标已完成！撤离点已激活").color(NamedTextColor.GOLD));
            setPhase(RaidPhaseType.EXTRACTION);
        }
    }

    public void onPlayerDeath(Player player) {
        RaidPlayer rp = team.getMember(player.getUniqueId());
        if (rp == null) return;

        rp.setState(RaidPlayerState.DEAD);

        long aliveCount = team.getMembers().stream()
            .filter(RaidPlayer::isAlive)
            .count();

        if (aliveCount == 0) {
            fail("全员阵亡");
        } else {
            team.broadcastMessage(Component.text(player.getName() + " 已阵亡！剩余存活: " + aliveCount)
                .color(NamedTextColor.RED));
        }
    }

    public void onPlayerExtraction(Player player) {
        RaidPlayer rp = team.getMember(player.getUniqueId());
        if (rp == null) return;

        rp.setState(RaidPlayerState.EXTRACTED);

        Bukkit.getPluginManager().callEvent(new RaidEvent.PlayerExtractionEvent(player, this));

        team.broadcastMessage(Component.text(player.getName() + " 已成功撤离！").color(NamedTextColor.GREEN));

        long extractedCount = team.getMembers().stream()
            .filter(p -> p.getState() == RaidPlayerState.EXTRACTED)
            .count();

        if (extractedCount == team.size()) {
            setPhase(RaidPhaseType.COMPLETED);
        }
    }

    private void complete() {
        active = false;
        cancelAllTasks();
        cleanupEntities();

        RaidReward reward = plugin.getLootManager().calculateReward(this);
        distributeRewards(reward);

        team.broadcastTitle("§a任务完成", "§e成功撤离！", 10, 100, 20);
        team.broadcastSound(Sound.UI_TOAST_CHALLENGE_COMPLETE, 1.0f, 1.0f);

        Bukkit.getPluginManager().callEvent(new RaidEvent.RaidCompleteEvent(this, true));

        scheduler.runSyncLater(() -> {
            for (RaidPlayer rp : team.getMembers()) {
                Player player = rp.getPlayer();
                if (player != null && player.isOnline()) {
                    Location returnLoc = rp.getJoinLocation();
                    if (returnLoc != null) {
                        player.teleport(returnLoc);
                    }
                }
            }
        }, 100L);
    }

    public void fail(String reason) {
        if (currentPhase == RaidPhaseType.FAILED) return;

        setPhase(RaidPhaseType.FAILED);
        active = false;
        cancelAllTasks();
        cleanupEntities();

        team.broadcastTitle("§c任务失败", "§7" + reason, 10, 100, 20);
        team.broadcastSound(Sound.ENTITY_WITHER_DEATH, 1.0f, 0.5f);

        Bukkit.getPluginManager().callEvent(new RaidEvent.RaidCompleteEvent(this, false));

        scheduler.runSyncLater(() -> {
            for (RaidPlayer rp : team.getMembers()) {
                Player player = rp.getPlayer();
                if (player != null && player.isOnline()) {
                    Location returnLoc = rp.getJoinLocation();
                    if (returnLoc != null) {
                        player.teleport(returnLoc);
                    }
                }
            }
        }, 100L);
    }

    private void distributeRewards(RaidReward reward) {
        for (RaidPlayer rp : team.getMembers()) {
            Player player = rp.getPlayer();
            if (player == null || !player.isOnline()) continue;

            player.sendMessage(Component.text("═══════════════════════════════").color(NamedTextColor.GOLD));
            player.sendMessage(Component.text("  副本结算: ").color(NamedTextColor.YELLOW)
                .append(Component.text(raid.getName()).color(NamedTextColor.GOLD)));
            player.sendMessage(Component.text("  点数奖励: ").color(NamedTextColor.GRAY)
                .append(Component.text(String.valueOf(reward.getBasePoints())).color(NamedTextColor.GREEN)));
            player.sendMessage(Component.text("  经验奖励: ").color(NamedTextColor.GRAY)
                .append(Component.text(String.valueOf(reward.getBaseExp())).color(NamedTextColor.GREEN)));
            player.sendMessage(Component.text("  击杀数: ").color(NamedTextColor.GRAY)
                .append(Component.text(String.valueOf(rp.getKills())).color(NamedTextColor.RED)));
            player.sendMessage(Component.text("  情报收集: ").color(NamedTextColor.GRAY)
                .append(Component.text(String.valueOf(rp.getIntelCollected())).color(NamedTextColor.AQUA)));
            player.sendMessage(Component.text("═══════════════════════════════").color(NamedTextColor.GOLD));

            for (RaidReward.ItemReward itemReward : reward.getItemRewards()) {
                if (Math.random() < itemReward.getChance()) {
                    plugin.getLootManager().giveRewardItem(player, itemReward);
                }
            }
        }
    }

    private void cleanupEntities() {
        World world = getWorld();
        if (world == null) return;

        for (UUID entityId : spawnedEnemies) {
            Entity entity = Bukkit.getEntity(entityId);
            if (entity != null) {
                entity.remove();
            }
        }

        for (UUID itemId : droppedItems) {
            Entity entity = Bukkit.getEntity(itemId);
            if (entity != null) {
                entity.remove();
            }
        }

        spawnedEnemies.clear();
        droppedItems.clear();
    }

    private void cancelAllTasks() {
        for (Long taskId : scheduledTasks) {
            scheduler.cancelTask(taskId);
        }
        scheduledTasks.clear();
    }

    public void shutdown() {
        active = false;
        cancelAllTasks();
        cleanupEntities();
    }

    public World getWorld() {
        return Bukkit.getWorld(raid.getWorldName());
    }

    public String getInstanceId() { return instanceId; }
    public Raid getRaid() { return raid; }
    public RaidTeam getTeam() { return team; }
    public RaidPhaseType getCurrentPhase() { return currentPhase; }
    public int getCollectedIntel() { return collectedIntel; }
    public int getKillCount() { return killCount; }
    public Set<String> getUnlockedAreas() { return unlockedAreas; }
    public DifficultyScaling getCurrentDifficulty() { return currentDifficulty; }
    public boolean isExtractionActive() { return extractionActive; }
    public boolean isActive() { return active; }
    public Set<UUID> getSpawnedEnemies() { return spawnedEnemies; }
    public Set<UUID> getDroppedItems() { return droppedItems; }
    public int getPhaseTimeRemaining() { return phaseTimeRemaining; }
    public long getStartTime() { return startTime; }

    public void addSpawnedEnemy(UUID entityId) { spawnedEnemies.add(entityId); }
    public void addDroppedItem(UUID itemId) { droppedItems.add(itemId); }
    public void setActiveExtractionPoint(String pointId) { this.activeExtractionPoint = pointId; }
    public String getActiveExtractionPoint() { return activeExtractionPoint; }
}
