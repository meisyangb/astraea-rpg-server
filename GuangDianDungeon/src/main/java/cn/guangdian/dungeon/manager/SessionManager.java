package cn.guangdian.dungeon.manager;

import cn.guangdian.dungeon.GuangDianDungeon;
import cn.guangdian.dungeon.integration.MobBridge;
import cn.guangdian.dungeon.model.DungeonParty;
import cn.guangdian.dungeon.model.DungeonTemplate;
import cn.guangdian.dungeon.model.PartyMember;
import cn.guangdian.dungeon.model.PartyState;
import cn.guangdian.dungeon.model.session.DungeonSession;
import cn.guangdian.dungeon.model.stage.*;
import cn.guangdian.rpgcore.RPGCore;
import cn.guangdian.rpgcore.api.SyncScheduler;
import net.kyori.adventure.title.Title;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.NamespacedKey;
import org.bukkit.Registry;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;

import java.time.Duration;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

public class SessionManager {
    private final GuangDianDungeon plugin;
    private final SyncScheduler scheduler;
    private final Map<String, DungeonSession> sessions;
    private final Map<UUID, String> playerSessions;
    private final Map<String, Long> stageTimers;
    private final Map<String, Long> dungeonTimeTimers;
    private MobBridge mobBridge;

    // Boss HP 阈值追踪: entity UUID -> BossTrackingInfo
    private final Map<UUID, BossTrackingInfo> bossTracking = new ConcurrentHashMap<>();
    private long bossCheckTaskId = -1;

    public SessionManager(GuangDianDungeon plugin) {
        this.plugin = plugin;
        RPGCore rpgCore = RPGCore.getInstance();
        this.scheduler = rpgCore != null ? rpgCore.getScheduler() : null;
        this.sessions = new ConcurrentHashMap<>();
        this.playerSessions = new ConcurrentHashMap<>();
        this.stageTimers = new ConcurrentHashMap<>();
        this.dungeonTimeTimers = new ConcurrentHashMap<>();
        startBossCheckTask();
    }

    public void setMobBridge(MobBridge bridge) {
        this.mobBridge = bridge;
    }

    private boolean isDebug() {
        return plugin.getConfig().getBoolean("debug", false);
    }

    private void runSyncLater(Runnable task, long delayTicks) {
        if (scheduler != null) {
            scheduler.runSyncLater(task, delayTicks);
        } else {
            Bukkit.getScheduler().runTaskLater(plugin, task, delayTicks);
        }
    }

    private long runSyncRepeating(Runnable task, long delayTicks, long periodTicks) {
        if (scheduler != null) {
            return scheduler.runSyncRepeating(task, delayTicks, periodTicks);
        } else {
            return Bukkit.getScheduler().runTaskTimer(plugin, task, delayTicks, periodTicks).getTaskId();
        }
    }

    private void cancelTask(long taskId) {
        if (scheduler != null) {
            scheduler.cancelTask(taskId);
        } else {
            Bukkit.getScheduler().cancelTask((int) taskId);
        }
    }

    public DungeonSession createSession(String dungeonId, DungeonParty party, World world) {
        String sessionId = UUID.randomUUID().toString().substring(0, 8);
        DungeonSession session = new DungeonSession();
        session.setSessionId(sessionId);
        session.setDungeonId(dungeonId);
        session.setParty(party);
        session.setInstanceWorld(world);
        session.setState(DungeonSession.SessionState.WAITING);

        sessions.put(sessionId, session);
        for (PartyMember member : party.getMembers()) {
            playerSessions.put(member.getPlayerId(), sessionId);
        }
        party.setActiveSessionId(sessionId);
        party.setState(PartyState.IN_DUNGEON);

        return session;
    }

    public void startSession(DungeonSession session) {
        session.setState(DungeonSession.SessionState.RUNNING);
        session.setStartTime(System.currentTimeMillis());

        int timeLimit = session.getTimeLimit();
        if (timeLimit > 0) {
            broadcastMessage(session, "<yellow><bold>[副本]</bold> <white>时间限制: <gold>" + formatTime(timeLimit));
            startDungeonTimer(session, timeLimit);
        }

        if (!session.getStages().isEmpty()) {
            Stage firstStage = session.getStages().get(0);
            startStage(session, firstStage);
        }
    }

    private void startDungeonTimer(DungeonSession session, int timeLimitSeconds) {
        final int[] timeLeft = {timeLimitSeconds};
        String sessionId = session.getSessionId();

        long timerId = runSyncRepeating(() -> {
            if (session.getState() != DungeonSession.SessionState.RUNNING) {
                cancelDungeonTimer(sessionId);
                return;
            }

            timeLeft[0]--;

            if (timeLeft[0] == 60) {
                broadcastTitle(session, "<red><bold>剩余 1 分钟!", "<yellow>请尽快完成副本", 500, 2000, 500);
                broadcastMessage(session, "<red><bold>[警告]</bold> <white>副本剩余时间: <yellow>1分钟");
                broadcastSound(session, Sound.BLOCK_NOTE_BLOCK_PLING, 1.0f, 1.0f);
            } else if (timeLeft[0] == 30) {
                broadcastTitle(session, "<red><bold>剩余 30 秒!", "<yellow>抓紧时间!", 500, 1500, 500);
                broadcastMessage(session, "<red><bold>[警告]</bold> <white>副本剩余时间: <yellow>30秒");
                broadcastSound(session, Sound.BLOCK_NOTE_BLOCK_PLING, 1.0f, 1.2f);
            } else if (timeLeft[0] == 10) {
                broadcastTitle(session, "<red><bold>剩余 10 秒!", "<white>最后冲刺!", 250, 1000, 250);
                broadcastMessage(session, "<red><bold>[最终警告]</bold> <white>副本剩余时间: <yellow>10秒");
                broadcastSound(session, Sound.BLOCK_NOTE_BLOCK_PLING, 1.0f, 1.5f);
            } else if (timeLeft[0] <= 5 && timeLeft[0] > 0) {
                broadcastTitle(session, "<red><bold>" + timeLeft[0], null, 100, 500, 100);
                broadcastSound(session, Sound.BLOCK_NOTE_BLOCK_PLING, 0.5f, 1.8f);
            }

            if (timeLeft[0] <= 0) {
                cancelDungeonTimer(sessionId);
                onDungeonTimeout(session);
            }
        }, 20L, 20L);

        dungeonTimeTimers.put(sessionId, timerId);
    }

    private void onDungeonTimeout(DungeonSession session) {
        session.setState(DungeonSession.SessionState.FAILED);
        session.setEndTime(System.currentTimeMillis());

        broadcastTitle(session, "<red><bold>挑战失败!", "<white>副本时间耗尽", 500, 3000, 500);
        broadcastMessage(session, "<red><bold>[副本]</bold> <white>时间耗尽，挑战失败！");
        broadcastSound(session, Sound.ENTITY_WITHER_DEATH, 1.0f, 0.5f);

        runSyncLater(() -> {
            cleanupSession(session);
        }, 100L);
    }

    private void cancelDungeonTimer(String sessionId) {
        Long timerId = dungeonTimeTimers.remove(sessionId);
        if (timerId != null) {
            cancelTask(timerId);
        }
    }

    private String formatTime(int seconds) {
        int minutes = seconds / 60;
        int secs = seconds % 60;
        if (minutes > 0) {
            return minutes + "分" + (secs > 0 ? secs + "秒" : "");
        }
        return seconds + "秒";
    }

    public void startStage(DungeonSession session, Stage stage) {
        stage.setActive(true);

        broadcastMessage(session, "<yellow><bold>[副本]</bold> <white>阶段开始: <gold>" + stage.getName());
        broadcastTitle(session, "<gold>" + stage.getName(), "<white>阶段开始!");

        if (stage.getTrigger() != null) {
            TriggerType triggerType = stage.getTrigger().getType();
            if (triggerType == TriggerType.ON_START || triggerType == TriggerType.ON_STAGE_COMPLETE) {
                startFirstWave(session, stage);
            }
        } else {
            startFirstWave(session, stage);
        }

        if (stage.getType() == Stage.StageType.COMBAT || stage.getType() == Stage.StageType.BOSS) {
            startWaveTimer(session, stage);
        }
    }

    private void startFirstWave(DungeonSession session, Stage stage) {
        if (!stage.getWaves().isEmpty()) {
            Wave firstWave = stage.getWaves().get(0);
            spawnWave(session, firstWave);
        }
    }

    public void spawnWave(DungeonSession session, Wave wave) {
        World world = session.getInstanceWorld();
        if (world == null) return;

        wave.activate();

        if (wave.getStartMessage() != null) {
            broadcastMessage(session, wave.getStartMessage());
        }

        for (MobSpawn spawn : wave.getSpawns()) {
            SpawnPoint spawnPoint = session.getSpawnPoints().get(spawn.getSpawnPointId());
            if (spawnPoint == null) continue;

            Location baseLoc = spawnPoint.getLocation();
            int amount = spawn.getAmount();

            if (spawn.getDelaySeconds() > 0) {
                runSyncLater(() -> {
                    spawnMobs(session, wave, spawn, baseLoc, amount);
                }, spawn.getDelaySeconds() * 20L);
            } else {
                spawnMobs(session, wave, spawn, baseLoc, amount);
            }
        }

        Stage currentStage = session.getCurrentStage();
        if (currentStage != null) {
            startWaveTriggerTimer(session, currentStage, wave);
        }
    }

    private void spawnMobs(DungeonSession session, Wave wave, MobSpawn spawn, Location baseLoc, int amount) {
        if (mobBridge == null) {
            plugin.getLogger().warning("MobBridge is null!");
            return;
        }

        int successCount = 0;
        net.kyori.adventure.text.Component mobDisplayComponent = net.kyori.adventure.text.Component.text(spawn.getMobId());

        for (int i = 0; i < amount; i++) {
            double angle = 2 * Math.PI * i / amount;
            double radius = spawn.getRadius() > 0 ? spawn.getRadius() : 3;
            double x = baseLoc.getX() + radius * Math.cos(angle);
            double z = baseLoc.getZ() + radius * Math.sin(angle);
            Location spawnLoc = new Location(baseLoc.getWorld(), x, baseLoc.getY(), z);

            Entity entity = mobBridge.spawnMob(spawn.getMobId(), spawnLoc);
            if (entity != null) {
                mobBridge.tagEntityForSession(entity, session.getSessionId());
                if (spawn.isBoss()) {
                    mobBridge.tagEntityAsBoss(entity, spawn.getMobId());
                }

                session.addSpawnedMob(entity.getUniqueId());
                wave.addSpawnedMobs(1);
                successCount++;

                if (entity.customName() != null) {
                    mobDisplayComponent = entity.customName();
                }

                if (spawn.isBoss()) {
                    broadcastTitleWithComponent(session, plugin.color("<red><bold>⚠ BOSS出现 ⚠</bold>"), mobDisplayComponent);
                    broadcastSound(session, Sound.ENTITY_WITHER_SPAWN, 1.0f, 0.5f);

                    if (!spawn.getBossHpTriggers().isEmpty()) {
                        BossTrackingInfo trackingInfo = new BossTrackingInfo(
                            session.getSessionId(),
                            new ArrayList<>(spawn.getBossHpTriggers()),
                            spawn.getSpawnPointId()
                        );
                        bossTracking.put(entity.getUniqueId(), trackingInfo);
                    }
                }
            } else {
                plugin.getLogger().warning("Failed to spawn mob: " + spawn.getMobId());
            }
        }

        String mobDisplayName = net.kyori.adventure.text.minimessage.MiniMessage.miniMessage().serialize(mobDisplayComponent);
        broadcastMessage(session, "<gray>[波次] <white>生成 " + successCount + "x " + mobDisplayName);
        broadcastMessage(session, "<yellow><bold>[击杀进度]</bold> <white>0/" + wave.getSpawnedMobCount());
    }

    private void startWaveTimer(DungeonSession session, Stage stage) {
        Wave currentWave = stage.getCurrentWave();
        if (currentWave == null || currentWave.getTimeLimit() <= 0) return;

        int[] timeLeft = {currentWave.getTimeLimit()};

        long timerId = runSyncRepeating(() -> {
            if (session.getState() != DungeonSession.SessionState.RUNNING) {
                cancelStageTimerByKey(session.getSessionId() + "_" + stage.getId());
                return;
            }

            timeLeft[0]--;

            if (timeLeft[0] <= 10 && timeLeft[0] > 0) {
                broadcastMessage(session, "<red><bold>[警告]</bold> <white>波次剩余时间: " + timeLeft[0] + "秒");
            }

            if (timeLeft[0] <= 0) {
                onWaveTimeout(session, stage, currentWave);
                cancelStageTimerByKey(session.getSessionId() + "_" + stage.getId());
            }
        }, 20L, 20L);

        stageTimers.put(session.getSessionId() + "_" + stage.getId(), timerId);
    }

    private void onWaveTimeout(DungeonSession session, Stage stage, Wave wave) {
        // 波次超时 - 踢出所有玩家并销毁实例
        broadcastMessage(session, "<red><bold>[副本超时]</bold> <white>未能在规定时间内完成波次！");
        broadcastTitle(session, "<red><bold>副本失败", "<yellow>波次超时，即将退出...", 500, 2000, 500);

        // 延迟3秒后踢出玩家
        runSyncLater(() -> {
            for (PartyMember member : session.getParty().getMembers()) {
                Player p = plugin.getServer().getPlayer(member.getPlayerId());
                if (p != null) {
                    p.sendMessage(plugin.color("<red>副本超时失败！"));
                }
            }
            // 结束会话（失败）
            endSession(session, false);
        }, 60L); // 3秒后执行
    }

    public void onMobKill(DungeonSession session, Entity entity) {
        if (!session.isSessionMob(entity.getUniqueId())) {
            return;
        }

        session.removeSpawnedMob(entity.getUniqueId());
        session.incrementTotalKills();

        String mobId = entity.getName();
        session.incrementMobKill(mobId);

        Stage currentStage = session.getCurrentStage();
        if (currentStage == null) return;

        Wave currentWave = currentStage.getCurrentWave();
        if (currentWave == null) return;

        currentWave.incrementKillCount();

        int killCount = currentWave.getKillCount();
        int totalCount = currentWave.getSpawnedMobCount();
        int remaining = totalCount - killCount;

        String progressBar = generateProgressBar(killCount, totalCount, 20);
        broadcastActionBar(session, "<yellow><bold>[击杀进度]</bold> " + progressBar + " <white>" + killCount + "/" + totalCount);

        if (remaining <= 3 && remaining > 0) {
            broadcastTitle(session, "<red><bold>还剩 " + remaining + " 只!", "<gray>击杀所有敌人进入下一波", 250, 1000, 250);
        } else if (remaining == 0) {
            broadcastTitle(session, "<green><bold>波次完成!", "<yellow>准备进入下一波...", 500, 1500, 500);
        }

        boolean completed = currentWave.checkCompletion();

        if (completed) {
            WaveTrigger trigger = currentWave.getNextWaveTrigger();

            if (trigger == null || trigger.getType() == WaveTriggerType.ON_KILL_COMPLETE) {
                onWaveComplete(session, currentStage, currentWave);
            }
        }
    }

    public void triggerNextWave(DungeonSession session) {
        Stage currentStage = session.getCurrentStage();
        if (currentStage == null) return;

        Wave currentWave = currentStage.getCurrentWave();
        if (currentWave == null || currentWave.isCompleted()) return;

        onWaveComplete(session, currentStage, currentWave);
    }

    public void onPlayerReachLocation(DungeonSession session, Player player, Location location) {
        Stage currentStage = session.getCurrentStage();
        if (currentStage == null) return;

        Wave currentWave = currentStage.getCurrentWave();
        if (currentWave == null || currentWave.isCompleted()) return;

        WaveTrigger trigger = currentWave.getNextWaveTrigger();
        if (trigger != null && trigger.getType() == WaveTriggerType.ON_LOCATION) {
            Location target = trigger.getTargetLocation();
            double radius = trigger.getLocationRadius();

            if (target != null && location.distance(target) <= radius) {
                broadcastMessage(session, "<green><bold>[触发]</bold> <white>玩家 " + player.getName() + " 到达目标位置！");
                onWaveComplete(session, currentStage, currentWave);
            }
        }
    }

    public void startWaveTriggerTimer(DungeonSession session, Stage stage, Wave wave) {
        WaveTrigger trigger = wave.getNextWaveTrigger();
        if (trigger == null || trigger.getType() != WaveTriggerType.ON_TIME) return;

        int delaySeconds = trigger.getDelaySeconds();

        long timerId = runSyncRepeating(() -> {
            if (session.getState() != DungeonSession.SessionState.RUNNING) {
                cancelStageTimerByKey(session.getSessionId() + "_" + stage.getId() + "_trigger");
                return;
            }

            Wave currentWave = stage.getCurrentWave();
            if (currentWave != null && !currentWave.isCompleted() && currentWave.getId().equals(wave.getId())) {
                broadcastMessage(session, "<yellow><bold>[时间到]</bold> <white>波次时间触发！");
                onWaveComplete(session, stage, currentWave);
                cancelStageTimerByKey(session.getSessionId() + "_" + stage.getId() + "_trigger");
            }
        }, delaySeconds * 20L, delaySeconds * 20L);

        stageTimers.put(session.getSessionId() + "_" + stage.getId() + "_trigger", timerId);
    }

    private void onWaveComplete(DungeonSession session, Stage stage, Wave wave) {
        wave.setCompleted(true);

        cancelStageTimer(session, stage);

        broadcastMessage(session, "<green><bold>[波次完成]</bold> " + (wave.getCompletionMessage() != null ? wave.getCompletionMessage() : ""));

        if (wave.getExpReward() > 0) {
            giveExpReward(session, wave.getExpReward());
        }

        checkWaveCompletion(session, stage, wave);
    }

    private void checkWaveCompletion(DungeonSession session, Stage stage, Wave completedWave) {
        if (stage.hasNextWave()) {
            stage.advanceWave();
            Wave nextWave = stage.getCurrentWave();

            broadcastMessage(session, "<yellow><bold>[副本]</bold> <white>下一波即将开始...");

            runSyncLater(() -> {
                spawnWave(session, nextWave);
                startWaveTimer(session, stage);
            }, 60L);
        } else {
            onStageComplete(session, stage);
        }
    }

    private void onStageComplete(DungeonSession session, Stage stage) {
        stage.setCompleted(true);
        stage.setActive(false);

        cancelStageTimer(session, stage);

        broadcastMessage(session, "<green><bold>[阶段完成]</bold> <white>" + stage.getName());
        broadcastSound(session, Sound.ENTITY_PLAYER_LEVELUP, 1.0f, 1.0f);

        executeActions(session, stage.getOnComplete());

        if (session.hasNextStage()) {
            session.advanceStage();
            Stage nextStage = session.getCurrentStage();

            runSyncLater(() -> {
                startStage(session, nextStage);
            }, 100L);
        } else {
            onDungeonComplete(session);
        }
    }

    private void onDungeonComplete(DungeonSession session) {
        session.setState(DungeonSession.SessionState.COMPLETED);
        session.setEndTime(System.currentTimeMillis());

        cancelDungeonTimer(session.getSessionId());

        int elapsedSeconds = (int) (session.getElapsedTime() / 1000);
        broadcastTitle(session, "<green><bold>副本通关！", "<yellow>用时: " + elapsedSeconds + "秒");
        broadcastSound(session, Sound.UI_TOAST_CHALLENGE_COMPLETE, 1.0f, 1.0f);

        // 计算评分并发放奖励
        int score = calculateSessionScore(session);
        plugin.getRewardManager().distributeRewards(session, score);

        runSyncLater(() -> {
            cleanupSession(session);
        }, 100L);
    }

    private int calculateSessionScore(DungeonSession session) {
        int timeBonus = 0;
        int timeLimit = session.getTimeLimit();
        if (timeLimit > 0) {
            long elapsed = session.getElapsedTime() / 1000;
            if (elapsed < timeLimit) {
                timeBonus = (int) ((timeLimit - elapsed) * 2);
            }
        }
        int deathDeduction = session.getTotalDeaths() * 50;
        return Math.max(0, session.getTotalKills() * 10 + timeBonus - deathDeduction);
    }

    public void forceCompleteSession(String sessionId) {
        DungeonSession session = sessions.get(sessionId);
        if (session != null) {
            onDungeonComplete(session);
        }
    }

    public void forceFailSession(String sessionId, String reason) {
        DungeonSession session = sessions.get(sessionId);
        if (session != null) {
            session.setState(DungeonSession.SessionState.FAILED);
            session.setEndTime(System.currentTimeMillis());
            broadcastMessage(session, "<red><bold>[副本]</bold> <white>挑战失败: " + reason);
            runSyncLater(() -> cleanupSession(session), 100L);
        }
    }

    public int cleanupExpired(long timeoutMs) {
        int cleaned = 0;
        long now = System.currentTimeMillis();

        for (DungeonSession session : List.copyOf(sessions.values())) {
            if (session.getState() == DungeonSession.SessionState.RUNNING
                && session.getStartTime() > 0
                && (now - session.getStartTime()) > timeoutMs) {
                session.setState(DungeonSession.SessionState.FAILED);
                broadcastMessage(session, "<red><bold>[副本]</bold> <white>副本超时自动关闭");
                cleanupSession(session);
                cleaned++;
            }
        }

        for (DungeonSession session : List.copyOf(sessions.values())) {
            if (session.getState() == DungeonSession.SessionState.COMPLETED
                || session.getState() == DungeonSession.SessionState.FAILED
                || session.getState() == DungeonSession.SessionState.CLEANUP) {
                if (now - session.getEndTime() > 60000) {
                    forceCleanupSession(session);
                    cleaned++;
                }
            }
        }

        return cleaned;
    }

    private void forceCleanupSession(DungeonSession session) {
        String sessionId = session.getSessionId();
        cancelDungeonTimer(sessionId);
        cancelAllStageTimers(sessionId);

        for (PartyMember member : session.getParty().getMembers()) {
            playerSessions.remove(member.getPlayerId());
        }
        session.getParty().setActiveSessionId(null);
        session.getParty().setState(PartyState.DISBANDED);

        sessions.remove(sessionId);
    }

    private void cleanupSession(DungeonSession session) {
        String sessionId = session.getSessionId();
        String instanceWorldName = session.getInstanceWorldName();

        cancelDungeonTimer(sessionId);
        cancelAllStageTimers(sessionId);

        for (PartyMember member : session.getParty().getMembers()) {
            Player player = Bukkit.getPlayer(member.getPlayerId());
            if (player != null) {
                // 使用玩家进入副本前的原始位置
                org.bukkit.Location originalLoc = session.getOriginalLocation(member.getPlayerId());
                plugin.getMapInstanceManager().teleportToOriginalLocation(player, originalLoc);
            }
            playerSessions.remove(member.getPlayerId());
        }

        session.getParty().setActiveSessionId(null);
        session.getParty().setState(PartyState.DISBANDED);

        sessions.remove(sessionId);

        if (instanceWorldName != null && !instanceWorldName.isEmpty()) {
            runSyncLater(() -> {
                plugin.getMapInstanceManager().destroyInstance(instanceWorldName);
            }, 40L);
        }
    }

    private void executeActions(DungeonSession session, List<StageAction> actions) {
        for (StageAction action : actions) {
            switch (action.getType()) {
                case MESSAGE:
                    broadcastMessage(session, action.getContent());
                    break;
                case TITLE:
                    broadcastTitle(session, action.getContent(), action.getSubtitle());
                    break;
                case SOUND:
                    try {
                        Sound sound = Registry.SOUNDS.get(
                            NamespacedKey.minecraft(action.getSound().toLowerCase()));
                        if (sound != null) {
                            broadcastSound(session, sound, action.getVolume(), 1.0f);
                        }
                    } catch (Exception e) {
                        plugin.getLogger().warning("播放副本声音失败: " + action.getSound() + " - " + e.getMessage());
                    }
                    break;
                case DELAY:
                    break;
                case GIVE_EXP:
                    try {
                        giveExpReward(session, Integer.parseInt(action.getContent()));
                    } catch (NumberFormatException ignored) {}
                    break;
                default:
                    break;
            }
        }
    }

    private void giveExpReward(DungeonSession session, int exp) {
        for (PartyMember member : session.getParty().getMembers()) {
            Player player = Bukkit.getPlayer(member.getPlayerId());
            if (player != null) {
                player.giveExp(exp);
                plugin.sendMessage(player, "<green><bold>[奖励]</bold> <white>获得 " + exp + " 经验值");
            }
        }
    }

    private void broadcastMessage(DungeonSession session, String message) {
        for (PartyMember member : session.getParty().getMembers()) {
            Player player = Bukkit.getPlayer(member.getPlayerId());
            if (player != null) {
                plugin.sendMessageDirect(player, message);
            }
        }
    }

    private void broadcastTitle(DungeonSession session, String title, String subtitle) {
        broadcastTitle(session, title, subtitle, 500, 3000, 500);
    }

    private void broadcastTitleWithComponent(DungeonSession session, net.kyori.adventure.text.Component title, net.kyori.adventure.text.Component subtitle) {
        for (PartyMember member : session.getParty().getMembers()) {
            Player player = Bukkit.getPlayer(member.getPlayerId());
            if (player != null) {
                Title.Times times = Title.Times.times(
                    Duration.ofMillis(500),
                    Duration.ofMillis(3000),
                    Duration.ofMillis(500)
                );
                Title adventureTitle = Title.title(title, subtitle, times);
                player.showTitle(adventureTitle);
            }
        }
    }

    private void broadcastTitle(DungeonSession session, String title, String subtitle, long fadeInMs, long stayMs, long fadeOutMs) {
        for (PartyMember member : session.getParty().getMembers()) {
            Player player = Bukkit.getPlayer(member.getPlayerId());
            if (player != null) {
                Title.Times times = Title.Times.times(
                    Duration.ofMillis(fadeInMs),
                    Duration.ofMillis(stayMs),
                    Duration.ofMillis(fadeOutMs)
                );
                Title adventureTitle = Title.title(
                    plugin.color(title),
                    subtitle != null ? plugin.color(subtitle) : net.kyori.adventure.text.Component.empty(),
                    times
                );
                player.showTitle(adventureTitle);
            }
        }
    }

    private void broadcastSound(DungeonSession session, Sound sound, float volume, float pitch) {
        for (PartyMember member : session.getParty().getMembers()) {
            Player player = Bukkit.getPlayer(member.getPlayerId());
            if (player != null) {
                player.playSound(player.getLocation(), sound, volume, pitch);
            }
        }
    }

    private void broadcastActionBar(DungeonSession session, String message) {
        for (PartyMember member : session.getParty().getMembers()) {
            Player player = Bukkit.getPlayer(member.getPlayerId());
            if (player != null) {
                player.sendActionBar(plugin.color(message));
            }
        }
    }

    private String generateProgressBar(int current, int max, int length) {
        if (max <= 0) return "";
        double percent = (double) current / max;
        int filled = (int) (percent * length);
        int empty = length - filled;

        StringBuilder bar = new StringBuilder();
        bar.append("<green>");
        for (int i = 0; i < filled; i++) {
            bar.append("█");
        }
        bar.append("<gray>");
        for (int i = 0; i < empty; i++) {
            bar.append("█");
        }
        return bar.toString();
    }

    private void cancelStageTimer(DungeonSession session, Stage stage) {
        String key = session.getSessionId() + "_" + stage.getId();
        cancelStageTimerByKey(key);
    }

    private void cancelStageTimerByKey(String key) {
        Long timerId = stageTimers.remove(key);
        if (timerId != null) {
            cancelTask(timerId);
        }
    }

    private void cancelAllStageTimers(String sessionId) {
        stageTimers.entrySet().removeIf(entry -> {
            if (entry.getKey().startsWith(sessionId)) {
                cancelTask(entry.getValue());
                return true;
            }
            return false;
        });
    }

    public void endSession(DungeonSession session, boolean success) {
        if (success) {
            onDungeonComplete(session);
        } else {
            session.setState(DungeonSession.SessionState.FAILED);
            broadcastMessage(session, "<red><bold>[副本]</bold> <white>挑战失败！");
            runSyncLater(() -> cleanupSession(session), 100L);
        }
    }

    public DungeonSession getSession(String sessionId) {
        return sessions.get(sessionId);
    }

    public Map<String, DungeonSession> getSessions() {
        return sessions;
    }

    public DungeonSession getPlayerSession(UUID playerId) {
        String sessionId = playerSessions.get(playerId);
        return sessionId != null ? sessions.get(sessionId) : null;
    }

    public boolean isInDungeon(UUID playerId) {
        return playerSessions.containsKey(playerId);
    }

    public Collection<DungeonSession> getAllSessions() {
        return sessions.values();
    }

    public Map<UUID, String> getPlayerSessions() {
        return playerSessions;
    }

    public int getActiveSessionCount() {
        return sessions.size();
    }

    public List<DungeonSession> getActiveSessions() {
        return sessions.values().stream()
            .filter(s -> s.getState() == DungeonSession.SessionState.RUNNING
                      || s.getState() == DungeonSession.SessionState.WAITING
                      || s.getState() == DungeonSession.SessionState.STARTING)
            .collect(Collectors.toList());
    }

    private void startBossCheckTask() {
        bossCheckTaskId = runSyncRepeating(() -> {
            if (bossTracking.isEmpty()) return;

            for (Map.Entry<UUID, BossTrackingInfo> entry : new ArrayList<>(bossTracking.entrySet())) {
                UUID bossUuid = entry.getKey();
                BossTrackingInfo info = entry.getValue();

                org.bukkit.entity.Entity entity = Bukkit.getEntity(bossUuid);
                if (!(entity instanceof LivingEntity living) || living.isDead() || !living.isValid()) {
                    bossTracking.remove(bossUuid);
                    continue;
                }

                double hpPercent = (living.getHealth() / living.getMaxHealth()) * 100;

                for (BossHpTrigger trigger : new ArrayList<>(info.triggers)) {
                    if (trigger.shouldTrigger(hpPercent)) {
                        trigger.setTriggered(true);
                        executeBossHpTrigger(info.sessionId, trigger, living.getLocation());
                    }
                }

                if (info.triggers.stream().allMatch(BossHpTrigger::isTriggered)) {
                    bossTracking.remove(bossUuid);
                }
            }
        }, 20L, 20L);
    }

    private void executeBossHpTrigger(String sessionId, BossHpTrigger trigger, Location bossLoc) {
        DungeonSession session = sessions.get(sessionId);
        if (session == null) return;

        if (trigger.getMessage() != null) {
            broadcastMessage(session, "<dark_red><bold>[BOSS]</bold> <white>" + trigger.getMessage());
            broadcastSound(session, Sound.ENTITY_WITHER_HURT, 1.0f, 0.8f);
        }

        for (MobSpawn spawn : trigger.getSpawns()) {
            SpawnPoint spawnPoint = session.getSpawnPoints().get(spawn.getSpawnPointId());
            Location baseLoc = spawnPoint != null ? spawnPoint.getLocation() : bossLoc;
            spawnMobs(session, null, spawn, baseLoc, spawn.getAmount());
        }
    }

    private static class BossTrackingInfo {
        final String sessionId;
        final List<BossHpTrigger> triggers;
        final String spawnPointId;

        BossTrackingInfo(String sessionId, List<BossHpTrigger> triggers, String spawnPointId) {
            this.sessionId = sessionId;
            this.triggers = triggers;
            this.spawnPointId = spawnPointId;
        }
    }
}