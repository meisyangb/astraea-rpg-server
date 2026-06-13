package cn.guangdian.music;

import cn.guangdian.rpgcore.message.MiniMessageService;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class SceneCommand implements CommandExecutor, TabCompleter {

    private final GuangDianMusic plugin;
    private final MusicPlayerManager playerManager;

    public SceneCommand(GuangDianMusic plugin) {
        this.plugin = plugin;
        this.playerManager = plugin.getPlayerManager();
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command,
                           @NotNull String label, @NotNull String[] args) {
        if (!sender.hasPermission("guangdian.music.use")) {
            sendMessage(sender, plugin.getMusicConfig().getMessage("no-permission"));
            return true;
        }

        if (!(sender instanceof Player player)) {
            sender.sendMessage("此命令只能由玩家执行");
            return true;
        }

        if (args.length == 0) {
            sendSceneHelp(player);
            return true;
        }

        String sceneName = args[0].toLowerCase();
        MusicConfig.SceneMusic scene = plugin.getMusicConfig().getScene(sceneName);

        if (scene == null) {
            sendMessage(player, "<red>未知的场景: <white>" + sceneName);
            sendAvailableScenes(player);
            return true;
        }

        if (!scene.isEnabled()) {
            sendMessage(player, "<red>该场景音乐未启用");
            return true;
        }

        MusicPlayerManager.MusicTrack track = playerManager.getTrack(scene.getMusicId());
        if (track == null) {
            sendMessage(player, "<red>场景音乐文件不存在: <white>" + scene.getMusicId());
            return true;
        }

        playerManager.playTrack(player, track, scene.getVolume());
        sendMessage(player, "<green>已切换到场景: <white>" + sceneName + 
            "<gray> - " + track.getDisplayName());

        return true;
    }

    private void sendSceneHelp(Player player) {
        sendMessage(player, "<yellow>========== 场景音乐 ==========");
        sendAvailableScenes(player);
        sendMessage(player, "<gray>用法: /scene <场景名>");
    }

    private void sendAvailableScenes(Player player) {
        Map<String, MusicConfig.SceneMusic> scenes = plugin.getMusicConfig().getScenes();
        
        if (scenes.isEmpty()) {
            sendMessage(player, "<gray>暂无可用的场景音乐");
            return;
        }

        sendMessage(player, "<yellow>可用场景:");
        scenes.forEach((name, scene) -> {
            String status = scene.isEnabled() ? "<green>可用" : "<red>禁用";
            MusicPlayerManager.MusicTrack track = playerManager.getTrack(scene.getMusicId());
            String trackName = track != null ? track.getDisplayName() : "未知";
            
            sendMessage(player, "  <white>" + name + " <gray>- " + trackName + 
                " <gray>[" + status + "]");
        });
    }

    private void sendMessage(CommandSender sender, String message) {
        MiniMessageService mm = MiniMessageService.getInstance();
        if (mm != null) {
            sender.sendMessage(mm.colorize(message));
        } else {
            sender.sendMessage(message);
        }
    }

    @Override
    public @Nullable List<String> onTabComplete(@NotNull CommandSender sender,
                                               @NotNull Command command,
                                               @NotNull String alias,
                                               @NotNull String[] args) {
        if (args.length == 1) {
            Map<String, MusicConfig.SceneMusic> scenes = plugin.getMusicConfig().getScenes();
            List<String> sceneNames = scenes.keySet().stream()
                .filter(name -> scenes.get(name).isEnabled())
                .collect(Collectors.toList());

            String input = args[0].toLowerCase();
            return sceneNames.stream()
                .filter(name -> name.toLowerCase().startsWith(input))
                .collect(Collectors.toList());
        }
        return new ArrayList<>();
    }
}