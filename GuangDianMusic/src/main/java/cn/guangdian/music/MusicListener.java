package cn.guangdian.music;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerQuitEvent;

public class MusicListener implements Listener {

    private final GuangDianMusic plugin;

    public MusicListener(GuangDianMusic plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        
        plugin.getLogger().info("玩家 " + player.getName() + " 加入了服务器");
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        Player player = event.getPlayer();
        
        plugin.getPlayerManager().stopTrack(player);
        
        plugin.getLogger().info("玩家 " + player.getName() + " 离开了服务器");
    }

    @EventHandler
    public void onPlayerMove(PlayerMoveEvent event) {
        if (event.getFrom().getBlockX() == event.getTo().getBlockX() &&
            event.getFrom().getBlockY() == event.getTo().getBlockY() &&
            event.getFrom().getBlockZ() == event.getTo().getBlockZ()) {
            return;
        }

        Player player = event.getPlayer();
        MusicConfig config = plugin.getMusicConfig();

        if (!config.isFadeEnabled()) {
            return;
        }

        checkSceneTransition(player, event.getTo().getWorld().getName());
    }

    private void checkSceneTransition(Player player, String worldName) {
        MusicConfig config = plugin.getMusicConfig();
        MusicConfig.SceneMusic scene = null;

        if (worldName.startsWith("F1") || worldName.startsWith("F2")) {
            scene = config.getScene("overworld");
        } else if (worldName.contains("combat") || worldName.contains("boss")) {
            scene = config.getScene("combat");
        } else if (worldName.startsWith("F3") || worldName.startsWith("F4")) {
            scene = config.getScene("exploration");
        }

        if (scene != null && scene.isEnabled()) {
            String currentMusic = plugin.getPlayerMusicId(player);
            if (currentMusic == null || !currentMusic.equals(scene.getMusicId())) {
                MusicPlayerManager manager = plugin.getPlayerManager();
                MusicPlayerManager.MusicTrack track = manager.getTrack(scene.getMusicId());
                if (track != null) {
                    manager.crossFade(player, track, config.getFadeDuration());
                }
            }
        }
    }
}