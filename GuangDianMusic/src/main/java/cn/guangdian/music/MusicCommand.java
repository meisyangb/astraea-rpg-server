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
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class MusicCommand implements CommandExecutor, TabCompleter {

    private final GuangDianMusic plugin;
    private final MusicPlayerManager playerManager;

    public MusicCommand(GuangDianMusic plugin) {
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

        if (args.length == 0) {
            sendHelp(sender);
            return true;
        }

        String subCommand = args[0].toLowerCase();

        switch (subCommand) {
            case "play" -> handlePlay(sender, args);
            case "stop" -> handleStop(sender);
            case "volume" -> handleVolume(sender, args);
            case "list" -> handleList(sender);
            case "now" -> handleNowPlaying(sender);
            default -> sendHelp(sender);
        }

        return true;
    }

    private void handlePlay(CommandSender sender, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("此命令只能由玩家执行");
            return;
        }

        if (args.length < 2) {
            sendMessage(player, "<yellow>用法: /music play <音乐ID> [音量]");
            return;
        }

        String trackId = args[1].toLowerCase();
        MusicPlayerManager.MusicTrack track = playerManager.getTrack(trackId);

        if (track == null) {
            sendMessage(player, plugin.getMusicConfig().getMessage("invalid-song")
                .replace("%song%", trackId));
            return;
        }

        float volume = plugin.getMusicConfig().getDefaultVolume();
        if (args.length >= 3) {
            try {
                int volumePercent = Integer.parseInt(args[2]);
                volume = Math.max(0, Math.min(100, volumePercent)) / 100f;
            } catch (NumberFormatException ignored) {
            }
        }

        playerManager.playTrack(player, track, volume);
    }

    private void handleStop(CommandSender sender) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("此命令只能由玩家执行");
            return;
        }

        playerManager.stopTrack(player);
    }

    private void handleVolume(CommandSender sender, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("此命令只能由玩家执行");
            return;
        }

        if (args.length < 2) {
            float currentVolume = plugin.getPlayerVolume(player);
            sendMessage(player, "<yellow>当前音量: <white>" + 
                (int)(currentVolume * 100) + "%");
            sendMessage(player, "<yellow>用法: /music volume <0-100>");
            return;
        }

        try {
            int volumePercent = Integer.parseInt(args[1]);
            float volume = Math.max(0, Math.min(100, volumePercent)) / 100f;
            plugin.setVolume(player, volume);
        } catch (NumberFormatException e) {
            sendMessage(player, "<red>无效的音量值，请输入 0-100 之间的数字");
        }
    }

    private void handleList(CommandSender sender) {
        Map<String, MusicPlayerManager.MusicTrack> tracks = playerManager.getAllTracks();

        sendMessage(sender, "<yellow>========== 可用音乐列表 ==========");
        
        tracks.values().forEach(track -> {
            String status = playerManager.hasActiveSession((Player) sender) &&
                plugin.getPlayerMusicId((Player) sender).equals(track.getId())
                ? " <green>[正在播放]" : "";
            sendMessage(sender, "<white>" + track.getId() + " <gray>- <yellow>" + 
                track.getDisplayName() + status);
        });

        sendMessage(sender, "<gray>输入 /music play <音乐ID> 播放音乐");
    }

    private void handleNowPlaying(CommandSender sender) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("此命令只能由玩家执行");
            return;
        }

        String currentMusic = plugin.getPlayerMusicId(player);
        if (currentMusic != null) {
            MusicPlayerManager.MusicTrack track = playerManager.getTrack(currentMusic);
            if (track != null) {
                sendMessage(player, "<green>正在播放: <white>" + track.getDisplayName() +
                    " <gray>(" + currentMusic + ")");
                sendMessage(player, "<yellow>音量: <white>" + 
                    (int)(plugin.getPlayerVolume(player) * 100) + "%");
            }
        } else {
            sendMessage(player, "<yellow>当前没有播放任何音乐");
        }
    }

    private void sendHelp(CommandSender sender) {
        sendMessage(sender, "<yellow>========== 音乐系统帮助 ==========");
        sendMessage(sender, "<white>/music play <音乐ID> [音量] <gray>- 播放指定音乐");
        sendMessage(sender, "<white>/music stop <gray>- 停止当前音乐");
        sendMessage(sender, "<white>/music volume <0-100> <gray>- 调整音量");
        sendMessage(sender, "<white>/music list <gray>- 查看可用音乐列表");
        sendMessage(sender, "<white>/music now <gray>- 查看当前播放的音乐");
        sendMessage(sender, "<white>/scene <场景名> <gray>- 切换场景音乐");
        sendMessage(sender, "<yellow>可用音乐: re_code_skyline, azure_core, solitary_battlefield");
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
        List<String> completions = new ArrayList<>();

        if (args.length == 1) {
            completions.addAll(Arrays.asList("play", "stop", "volume", "list", "now"));
        } else if (args.length == 2) {
            if (args[0].equalsIgnoreCase("play")) {
                completions.addAll(playerManager.getAllTracks().keySet());
            } else if (args[0].equalsIgnoreCase("volume")) {
                completions.addAll(Arrays.asList("25", "50", "75", "100"));
            }
        }

        String input = args[args.length - 1].toLowerCase();
        return completions.stream()
            .filter(s -> s.toLowerCase().startsWith(input))
            .collect(Collectors.toList());
    }
}