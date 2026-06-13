package cn.guangdian.music;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitTask;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class MusicPlayerManager {

    private final GuangDianMusic plugin;
    private final Map<UUID, MusicSession> activeSessions;
    private final Map<String, MusicTrack> trackRegistry;

    public MusicPlayerManager(GuangDianMusic plugin) {
        this.plugin = plugin;
        this.activeSessions = new ConcurrentHashMap<>();
        this.trackRegistry = new ConcurrentHashMap<>();
        
        initializeTracks();
    }

    private void initializeTracks() {
        registerTrack(new MusicTrack("re_code_skyline", 
            "yimeng.re_code_skyline", "Re:Code Skyline", true));
        registerTrack(new MusicTrack("azure_core",
            "yimeng.azure_core", "Azure Core", true));
        registerTrack(new MusicTrack("solitary_battlefield",
            "yimeng.solitary_battlefield", "孤独战场", false));
    }

    public void registerTrack(MusicTrack track) {
        trackRegistry.put(track.getId(), track);
    }

    public MusicTrack getTrack(String trackId) {
        return trackRegistry.get(trackId);
    }

    public Map<String, MusicTrack> getAllTracks() {
        return new ConcurrentHashMap<>(trackRegistry);
    }

    public void playTrack(Player player, MusicTrack track) {
        playTrack(player, track, track.getVolume());
    }

    public void playTrack(Player player, MusicTrack track, float volume) {
        if (player == null || track == null) {
            return;
        }

        stopTrack(player);

        MusicSession session = new MusicSession(player.getUniqueId(), track, volume);
        activeSessions.put(player.getUniqueId(), session);

        plugin.playMusic(player, track.getId(), volume);
    }

    public void stopTrack(Player player) {
        if (player == null) {
            return;
        }

        MusicSession session = activeSessions.remove(player.getUniqueId());
        if (session != null && session.getTask() != null) {
            session.getTask().cancel();
        }

        plugin.stopMusic(player);
    }

    public void fadeOutTrack(Player player, int durationMs) {
        MusicSession session = activeSessions.get(player.getUniqueId());
        if (session == null) {
            return;
        }

        float initialVolume = session.getVolume();
        int steps = 20;
        int delayPerStep = durationMs / steps;
        float volumeStep = initialVolume / steps;

        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            for (int i = 0; i < steps; i++) {
                try {
                    Thread.sleep(delayPerStep);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    return;
                }

                float newVolume = initialVolume - (volumeStep * (i + 1));
                final float volume = Math.max(0, newVolume);

                Bukkit.getScheduler().runTask(plugin, () -> {
                    plugin.playMusic(player, session.getTrack().getId(), volume);
                });
            }

            Bukkit.getScheduler().runTask(plugin, () -> {
                stopTrack(player);
            });
        });
    }

    public void crossFade(Player player, MusicTrack newTrack, int durationMs) {
        MusicSession oldSession = activeSessions.get(player.getUniqueId());
        float oldVolume = oldSession != null ? oldSession.getVolume() : 0.5f;

        fadeOutTrack(player, durationMs);

        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            playTrack(player, newTrack, oldVolume);
        }, (durationMs / 50) + 5);
    }

    public MusicSession getSession(Player player) {
        return activeSessions.get(player.getUniqueId());
    }

    public boolean hasActiveSession(Player player) {
        return activeSessions.containsKey(player.getUniqueId());
    }

    public void stopAllMusic() {
        activeSessions.values().forEach(session -> {
            if (session.getTask() != null) {
                session.getTask().cancel();
            }
        });
        activeSessions.clear();
    }

    public static class MusicTrack {
        private final String id;
        private final String soundEvent;
        private final String displayName;
        private final boolean loopable;
        private float volume = 0.5f;
        private int duration = 0;

        public MusicTrack(String id, String soundEvent, String displayName, boolean loopable) {
            this.id = id;
            this.soundEvent = soundEvent;
            this.displayName = displayName;
            this.loopable = loopable;
        }

        public String getId() { return id; }
        public String getSoundEvent() { return soundEvent; }
        public String getDisplayName() { return displayName; }
        public boolean isLoopable() { return loopable; }
        public float getVolume() { return volume; }
        public void setVolume(float volume) { this.volume = volume; }
        public int getDuration() { return duration; }
        public void setDuration(int duration) { this.duration = duration; }
    }

    public static class MusicSession {
        private final UUID playerId;
        private final MusicTrack track;
        private float volume;
        private BukkitTask task;
        private boolean playing;
        private long startTime;

        public MusicSession(UUID playerId, MusicTrack track, float volume) {
            this.playerId = playerId;
            this.track = track;
            this.volume = volume;
            this.playing = true;
            this.startTime = System.currentTimeMillis();
        }

        public UUID getPlayerId() { return playerId; }
        public MusicTrack getTrack() { return track; }
        public float getVolume() { return volume; }
        public void setVolume(float volume) { this.volume = volume; }
        public BukkitTask getTask() { return task; }
        public void setTask(BukkitTask task) { this.task = task; }
        public boolean isPlaying() { return playing; }
        public void setPlaying(boolean playing) { this.playing = playing; }
        public long getStartTime() { return startTime; }
    }
}