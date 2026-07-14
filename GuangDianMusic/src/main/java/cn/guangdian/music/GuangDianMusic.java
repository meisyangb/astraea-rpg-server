package cn.guangdian.music;

import cn.guangdian.rpgcore.RPGCore;
import cn.guangdian.rpgcore.api.SyncScheduler;
import cn.guangdian.rpgcore.message.MiniMessageService;
import net.kyori.adventure.sound.Sound;
import net.kyori.adventure.key.Key;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class GuangDianMusic extends JavaPlugin {

    private static GuangDianMusic instance;
    private MusicPlayerManager playerManager;
    private MusicConfig config;
    private SyncScheduler scheduler;

    private final Map<UUID, PlayerMusicState> playerStates = new ConcurrentHashMap<>();

    @Override
    public void onEnable() {
        instance = this;
        
        saveDefaultConfig();
        config = new MusicConfig(this);
        
        if (!config.load()) {
            getLogger().severe("音乐配置文件加载失败!");
            getServer().getPluginManager().disablePlugin(this);
            return;
        }

        RPGCore rpgCore = RPGCore.getInstance();
        if (rpgCore != null) {
            scheduler = rpgCore.getScheduler();
            getLogger().info("已连接 RPGCore 服务框架");
        } else {
            getLogger().warning("未检测到 RPGCore，音乐系统将以独立模式运行");
            scheduler = null;
        }

        playerManager = new MusicPlayerManager(this);

        registerCommands();
        registerListeners();

        getLogger().info(getPluginDescription() + " 已启动!");
    }

    @Override
    public void onDisable() {
        if (playerManager != null) {
            playerManager.stopAllMusic();
        }
        
        if (scheduler != null) {
            scheduler.cancelAllTasks();
        }
        
        playerStates.clear();
        
        getLogger().info(getPluginDescription() + " 已关闭!");
    }

    private void registerCommands() {
        getCommand("music").setExecutor(new MusicCommand(this));
        getCommand("scene").setExecutor(new SceneCommand(this));
    }

    private void registerListeners() {
        getServer().getPluginManager().registerEvents(new MusicListener(this), this);
    }

    public void playMusic(Player player, String musicId) {
        playMusic(player, musicId, config.getDefaultVolume(), false);
    }

    public void playMusic(Player player, String musicId, float volume) {
        playMusic(player, musicId, volume, false);
    }

    public void playMusic(Player player, String musicId, float volume, boolean loop) {
        if (player == null || musicId == null) {
            return;
        }

        // 修复：命名空间从 yimeng 改为 guangdian（与插件名一致）
        String soundEvent = "guangdian." + musicId;
        
        Key soundKey = Key.key(soundEvent);
        Sound sound = Sound.sound(
            soundKey,
            Sound.Source.AMBIENT,
            volume,
            1.0f
        );
        player.playSound(sound);

        PlayerMusicState state = playerStates.computeIfAbsent(
            player.getUniqueId(), 
            k -> new PlayerMusicState()
        );
        state.setCurrentMusic(musicId);
        state.setVolume(volume);
        state.setLooping(loop);

        MiniMessageService mm = MiniMessageService.getInstance();
        if (mm != null) {
            player.sendMessage(mm.colorize(
                config.getMessage("prefix") + config.getMessage("now-playing")
                    .replace("%song%", musicId)
            ));
        }
        
        getLogger().info("播放音乐: " + soundEvent + " 给玩家 " + player.getName());
    }

    public void stopMusic(Player player) {
        if (player == null) {
            return;
        }

        String musicId = getPlayerMusicId(player);
        if (musicId != null) {
            // 修复：命名空间从 yimeng 改为 guangdian
            String soundEvent = "guangdian." + musicId;
            Key soundKey = Key.key(soundEvent);
            Sound sound = Sound.sound(
                soundKey,
                Sound.Source.AMBIENT,
                0f,
                1.0f
            );
            player.stopSound(sound);
            getLogger().info("停止音乐: " + soundEvent + " 给玩家 " + player.getName());
        }

        PlayerMusicState state = playerStates.get(player.getUniqueId());
        if (state != null) {
            state.setCurrentMusic(null);
        }

        MiniMessageService mm = MiniMessageService.getInstance();
        if (mm != null) {
            player.sendMessage(mm.colorize(
                config.getMessage("prefix") + config.getMessage("stopped")
            ));
        }
    }

    public void setVolume(Player player, float volume) {
        if (player == null) {
            return;
        }

        volume = Math.max(0.0f, Math.min(1.0f, volume));

        PlayerMusicState state = playerStates.get(player.getUniqueId());
        if (state != null && state.getCurrentMusic() != null) {
            // 修复：命名空间从 yimeng 改为 guangdian
            String soundEvent = "guangdian." + state.getCurrentMusic();
            Key soundKey = Key.key(soundEvent);
            Sound sound = Sound.sound(
                soundKey,
                Sound.Source.AMBIENT,
                volume,
                1.0f
            );
            player.playSound(sound);
            state.setVolume(volume);
        }

        MiniMessageService mm = MiniMessageService.getInstance();
        if (mm != null) {
            player.sendMessage(mm.colorize(
                config.getMessage("prefix") + config.getMessage("volume-changed")
                    .replace("%volume%", String.valueOf((int)(volume * 100)))
            ));
        }
    }

    public String getPlayerMusicId(Player player) {
        PlayerMusicState state = playerStates.get(player.getUniqueId());
        return state != null ? state.getCurrentMusic() : null;
    }

    public float getPlayerVolume(Player player) {
        PlayerMusicState state = playerStates.get(player.getUniqueId());
        return state != null ? state.getVolume() : config.getDefaultVolume();
    }

    public MusicPlayerManager getPlayerManager() {
        return playerManager;
    }

    public MusicConfig getMusicConfig() {
        return config;
    }

    public static GuangDianMusic getInstance() {
        return instance;
    }

    private String getPluginDescription() {
        return getDescription().getName() + " v" + getDescription().getVersion();
    }

    private static class PlayerMusicState {
        private String currentMusic;
        private float volume = 0.5f;
        private boolean looping = false;

        public String getCurrentMusic() { return currentMusic; }
        public void setCurrentMusic(String music) { this.currentMusic = music; }
        public float getVolume() { return volume; }
        public void setVolume(float volume) { this.volume = volume; }
        public boolean isLooping() { return looping; }
        public void setLooping(boolean looping) { this.looping = looping; }
    }
}