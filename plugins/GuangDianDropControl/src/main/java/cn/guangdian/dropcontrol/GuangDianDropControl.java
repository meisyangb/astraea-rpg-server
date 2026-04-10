package cn.guangdian.dropcontrol;

import cn.guangdian.dropcontrol.adapter.DropControlServiceAdapter;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabExecutor;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class GuangDianDropControl extends JavaPlugin implements Listener, CommandExecutor, TabExecutor {

    private static GuangDianDropControl instance;
    private FileConfiguration config;
    private DropControlServiceAdapter serviceAdapter;

    private boolean dropEnabled = false;
    private final ConcurrentHashMap<UUID, Boolean> playerDropStatus = new ConcurrentHashMap<>();

    private String messageNoPermission;
    private String messageDropDisabled;
    private String messageDropEnabled;
    private String messagePlayerEnabled;
    private String messagePlayerDisabled;
    private String messageReload;
    private String messageUsage;
    private String messageStatus;
    private String messageToggleSuccess;
    private String messagePlayerStatus;
    private boolean playerDefaultEnabled = false;

    @Override
    public void onEnable() {
        instance = this;
        saveDefaultConfig();
        config = getConfig();

        loadConfig();

        // 注册 RPGCore 服务适配器
        serviceAdapter = new DropControlServiceAdapter(this);
        if (serviceAdapter.isUsingRPGCore()) {
            getLogger().info("已集成 RPGCore 服务系统!");
        }

        getCommand("gddrop").setExecutor(this);
        getCommand("gddrop").setTabCompleter(this);
        getServer().getPluginManager().registerEvents(this, this);

        getLogger().info("光点丢弃控制插件已启用! 版本: " + getDescription().getVersion());
        getLogger().info("作者: Gumin | QQ: 2271257344");
        getLogger().info("全局丢弃状态: " + (dropEnabled ? "启用" : "禁用"));
    }

    @Override
    public void onDisable() {
        // 注销 RPGCore 服务
        if (serviceAdapter != null) {
            serviceAdapter.unregister();
        }
        playerDropStatus.clear();
        getLogger().info("光点丢弃控制插件已禁用!");
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        // 清理玩家状态
        playerDropStatus.remove(event.getPlayer().getUniqueId());
    }

    private void loadConfig() {
        dropEnabled = config.getBoolean("settings.enabled", false);
        playerDefaultEnabled = config.getBoolean("settings.player-default-enabled", false);

        messageNoPermission = color(config.getString("messages.no-permission", "&c你没有权限执行此操作!"));
        messageDropDisabled = color(config.getString("messages.drop-disabled", "&c物品丢弃已禁用!"));
        messageDropEnabled = color(config.getString("messages.drop-enabled", "&a物品丢弃已启用!"));
        messagePlayerEnabled = color(config.getString("messages.player-enabled", "&a已允许 %player% 丢弃物品"));
        messagePlayerDisabled = color(config.getString("messages.player-disabled", "&c已禁止 %player% 丢弃物品"));
        messageReload = color(config.getString("messages.reload", "&a配置已重新加载!"));
        messageUsage = color(config.getString("messages.usage", "&e用法: /gddrop <enable|disable|player|reload|status> [玩家]"));
        messageStatus = color(config.getString("messages.status", "&e当前全局丢弃状态: %status%"));
        messageToggleSuccess = color(config.getString("messages.toggle-success", "&e你已将丢弃功能切换为: %status%"));
        messagePlayerStatus = color(config.getString("messages.player-status", "&e当前丢弃状态: %status%"));
    }

    @EventHandler
    public void onPlayerDropItem(PlayerDropItemEvent event) {
        Player player = event.getPlayer();
        UUID uuid = player.getUniqueId();

        if (player.hasPermission("gddrop.bypass")) {
            return;
        }

        // 检查是否有使用权限
        if (!player.hasPermission("gddrop.use")) {
            event.setCancelled(true);
            player.sendMessage(messageDropDisabled);
            return;
        }

        Boolean playerStatus = playerDropStatus.get(uuid);

        if (playerStatus != null) {
            if (!playerStatus) {
                event.setCancelled(true);
                player.sendMessage(messageDropDisabled);
            }
            return;
        }

        // 使用默认配置，(playerDefaultEnabled || dropEnabled) 只要有其一为true就允许
        if (!playerDefaultEnabled && !dropEnabled) {
            event.setCancelled(true);
            player.sendMessage(messageDropDisabled);
        }
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length == 0) {
            // 无参数时，给玩家显示帮助或自己的状态
            if (sender.hasPermission("gddrop.admin")) {
                sender.sendMessage(messageUsage);
            } else if (sender instanceof Player && sender.hasPermission("gddrop.use")) {
                // 普通玩家显示自己的状态
                Player player = (Player) sender;
                UUID uuid = player.getUniqueId();
                Boolean status = playerDropStatus.get(uuid);
                String statusText;
                if (status != null) {
                    statusText = status ? "&a启用" : "&c禁用";
                } else {
                    statusText = playerDefaultEnabled ? "&a启用 (默认)" : "&c禁用 (默认)";
                }
                sender.sendMessage(messagePlayerStatus.replace("%status%", statusText));
            } else {
                sender.sendMessage(messageNoPermission);
            }
            return true;
        }

        String subCommand = args[0].toLowerCase();

        // toggle 命令 - 玩家自己切换丢弃功能
        if (subCommand.equals("toggle")) {
            if (!(sender instanceof Player)) {
                sender.sendMessage(color("&c此命令只能由玩家执行!"));
                return true;
            }

            Player player = (Player) sender;
            if (!player.hasPermission("gddrop.use")) {
                player.sendMessage(messageNoPermission);
                return true;
            }

            UUID playerUuid = player.getUniqueId();
            Boolean currentStatus = playerDropStatus.get(playerUuid);

            if (currentStatus == null) {
                currentStatus = playerDefaultEnabled;
            }

            boolean newStatus = !currentStatus;
            playerDropStatus.put(playerUuid, newStatus);

            String statusText = newStatus ? "&a启用" : "&c禁用";
            player.sendMessage(messageToggleSuccess.replace("%status%", statusText));
            return true;
        }

        // status 命令 - 玩家查看自己的状态
        if (subCommand.equals("status")) {
            if (sender instanceof Player && sender.hasPermission("gddrop.use") && !sender.hasPermission("gddrop.admin")) {
                // 普通玩家查看自己的状态
                Player player = (Player) sender;
                UUID uuid = player.getUniqueId();
                Boolean status = playerDropStatus.get(uuid);
                String statusText;
                if (status != null) {
                    statusText = status ? "&a启用" : "&c禁用";
                } else {
                    statusText = playerDefaultEnabled ? "&a启用 (默认)" : "&c禁用 (默认)";
                }
                sender.sendMessage(messagePlayerStatus.replace("%status%", statusText));
                return true;
            }
            // 管理员查看全局状态（继续执行下面的管理员检查）
        }

        // 以下命令需要管理员权限
        if (!sender.hasPermission("gddrop.admin")) {
            sender.sendMessage(messageNoPermission);
            return true;
        }

        switch (subCommand) {
            case "enable":
                dropEnabled = true;
                config.set("settings.enabled", true);
                saveConfig();
                Bukkit.broadcastMessage(messageDropEnabled);
                break;

            case "disable":
                dropEnabled = false;
                config.set("settings.enabled", false);
                saveConfig();
                Bukkit.broadcastMessage(messageDropDisabled);
                break;

            case "player":
                if (args.length < 2) {
                    sender.sendMessage(messageUsage);
                    return true;
                }

                Player target = Bukkit.getPlayer(args[1]);
                if (target == null) {
                    sender.sendMessage(color("&c玩家不存在或不在线!"));
                    return true;
                }

                UUID targetUuid = target.getUniqueId();
                Boolean targetStatus = playerDropStatus.get(targetUuid);

                if (targetStatus == null) {
                    targetStatus = dropEnabled;
                }

                boolean newTargetStatus = !targetStatus;
                playerDropStatus.put(targetUuid, newTargetStatus);

                if (newTargetStatus) {
                    sender.sendMessage(messagePlayerEnabled.replace("%player%", target.getName()));
                } else {
                    sender.sendMessage(messagePlayerDisabled.replace("%player%", target.getName()));
                }
                break;

            case "reload":
                reloadConfig();
                loadConfig();
                sender.sendMessage(messageReload);
                break;

            case "status":
                String status = dropEnabled ? "&a启用" : "&c禁用";
                sender.sendMessage(messageStatus.replace("%status%", status));
                break;

            default:
                sender.sendMessage(messageUsage);
                break;
        }

        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        List<String> completions = new ArrayList<>();

        if (args.length == 1) {
            // 普通玩家可以看到 toggle 和 status
            if (sender.hasPermission("gddrop.use")) {
                completions.add("toggle");
                completions.add("status");
            }
            // 管理员可以看到所有命令
            if (sender.hasPermission("gddrop.admin")) {
                completions.add("enable");
                completions.add("disable");
                completions.add("player");
                completions.add("reload");
            }
        } else if (args.length == 2 && args[0].equalsIgnoreCase("player") && sender.hasPermission("gddrop.admin")) {
            for (Player online : Bukkit.getOnlinePlayers()) {
                completions.add(online.getName());
            }
        }

        return completions;
    }

    private String color(String message) {
        return ChatColor.translateAlternateColorCodes('&', message);
    }

    public static GuangDianDropControl getInstance() {
        return instance;
    }

    /**
     * 获取丢弃开关状态
     */
    public boolean isDropEnabled() {
        return dropEnabled;
    }

    /**
     * 设置丢弃开关状态
     */
    public void setDropEnabled(boolean enabled) {
        this.dropEnabled = enabled;
        config.set("settings.enabled", enabled);
        saveConfig();
    }

    /**
     * 获取玩家默认开关状态
     */
    public boolean isPlayerDefaultEnabled() {
        return playerDefaultEnabled;
    }

    /**
     * 获取玩家丢弃状态映射
     */
    public ConcurrentHashMap<UUID, Boolean> getPlayerDropStatus() {
        return playerDropStatus;
    }

    /**
     * 获取服务适配器
     */
    public DropControlServiceAdapter getServiceAdapter() {
        return serviceAdapter;
    }
}
