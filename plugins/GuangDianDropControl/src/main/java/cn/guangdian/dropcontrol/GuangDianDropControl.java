package cn.guangdian.dropcontrol;

import cn.guangdian.dropcontrol.adapter.DropControlServiceAdapter;
import cn.guangdian.dropcontrol.command.DropControlCommand;
import cn.guangdian.rpgcore.RPGCore;
import cn.guangdian.rpgcore.command.CommandFramework;
import cn.guangdian.rpgcore.message.MiniMessageService;
import cn.guangdian.rpgcore.plugin.AbstractRPGPlugin;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.Bukkit;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.event.player.PlayerQuitEvent;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class GuangDianDropControl extends AbstractRPGPlugin implements Listener {

    private static GuangDianDropControl instance;
    private FileConfiguration config;
    private DropControlServiceAdapter serviceAdapter;

    // RPGCore 服务引用
    private MiniMessageService miniMessage;
    private final MiniMessage miniMessageParser = MiniMessage.miniMessage();

    private boolean dropEnabled = false;
    private final ConcurrentHashMap<UUID, Boolean> playerDropStatus = new ConcurrentHashMap<>();

    // 消息配置（保留原始字符串，发送时解析）
    private String rawMessageNoPermission;
    private String rawMessageDropDisabled;
    private String rawMessageDropEnabled;
    private String rawMessagePlayerEnabled;
    private String rawMessagePlayerDisabled;
    private String rawMessageReload;
    private String rawMessageUsage;
    private String rawMessageStatus;
    private String rawMessageToggleSuccess;
    private String rawMessagePlayerStatus;
    private boolean playerDefaultEnabled = false;

    @Override
    protected void onPluginEnable() {
        instance = this;
        saveDefaultConfig();
        config = getConfig();

        // 初始化 RPGCore 服务
        initRPGCoreServices();

        loadConfig();

        // 注册 RPGCore 服务适配器
        serviceAdapter = new DropControlServiceAdapter(this);
        if (serviceAdapter.isUsingRPGCore()) {
            getLogger().info("已集成 RPGCore 服务系统!");
        }

        // 注册命令 - 使用 RPGCore CommandFramework
        registerCommands();
        getServer().getPluginManager().registerEvents(this, this);

        getLogger().info("光点丢弃控制插件已启用! 版本: " + getDescription().getVersion());
        getLogger().info("作者: Gumin | QQ: 2271257344");
        getLogger().info("全局丢弃状态: " + (dropEnabled ? "启用" : "禁用"));
    }

    @Override
    protected void onPluginDisable() {
        if (serviceAdapter != null) {
            serviceAdapter.unregister();
        }
        playerDropStatus.clear();
        getLogger().info("光点丢弃控制插件已禁用!");
    }

    @Override
    protected String getPluginName() {
        return "GuangDianDropControl";
    }

    private void initRPGCoreServices() {
        // 获取 RPGCore 服务
        if (getServer().getPluginManager().isPluginEnabled("RPGCore")) {
            try {
                RPGCore rpgCore = RPGCore.getInstance();
                miniMessage = rpgCore.getMiniMessageService();
                getLogger().info("使用 RPGCore MiniMessageService");
            } catch (Exception e) {
                getLogger().warning("无法获取 RPGCore MiniMessageService: " + e.getMessage());
            }
        }

        // 如果 RPGCore 服务不可用，使用本地降级
        if (miniMessage == null) {
            miniMessage = MiniMessageService.getInstance();
        }
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        // 清理玩家状态
        playerDropStatus.remove(event.getPlayer().getUniqueId());
    }

    private void loadConfig() {
        dropEnabled = config.getBoolean("settings.enabled", false);
        playerDefaultEnabled = config.getBoolean("settings.player-default-enabled", false);

        // 加载消息配置（保留原始字符串，发送时解析颜色和变量）
        rawMessageNoPermission = config.getString("messages.no-permission", "<red>你没有权限执行此操作!");
        rawMessageDropDisabled = config.getString("messages.drop-disabled", "<red>物品丢弃已禁用!");
        rawMessageDropEnabled = config.getString("messages.drop-enabled", "<green>物品丢弃已启用!");
        rawMessagePlayerEnabled = config.getString("messages.player-enabled", "<green>已允许 %player% 丢弃物品");
        rawMessagePlayerDisabled = config.getString("messages.player-disabled", "<red>已禁止 %player% 丢弃物品");
        rawMessageReload = config.getString("messages.reload", "<green>配置已重新加载!");
        rawMessageUsage = config.getString("messages.usage", "<yellow>用法: /gddrop <enable|disable|player|reload|status> [玩家]");
        rawMessageStatus = config.getString("messages.status", "<yellow>当前全局丢弃状态: %status%");
        rawMessageToggleSuccess = config.getString("messages.toggle-success", "<yellow>你已将丢弃功能切换为: %status%");
        rawMessagePlayerStatus = config.getString("messages.player-status", "<yellow>当前丢弃状态: %status%");
    }

    @EventHandler(priority = EventPriority.LOW, ignoreCancelled = true)
    public void onPlayerDropItem(PlayerDropItemEvent event) {
        Player player = event.getPlayer();
        UUID uuid = player.getUniqueId();

        if (player.hasPermission("gddrop.bypass")) {
            return;
        }

        // 检查是否有使用权限
        if (!player.hasPermission("gddrop.use")) {
            event.setCancelled(true);
            player.sendMessage(color(rawMessageDropDisabled));
            return;
        }

        Boolean playerStatus = playerDropStatus.get(uuid);

        if (playerStatus != null) {
            if (!playerStatus) {
                event.setCancelled(true);
                player.sendMessage(color(rawMessageDropDisabled));
            }
            return;
        }

        // 使用默认配置，(playerDefaultEnabled || dropEnabled) 只要有其一为true就允许
        if (!playerDefaultEnabled && !dropEnabled) {
            event.setCancelled(true);
            player.sendMessage(color(rawMessageDropDisabled));
        }
    }

    /**
     * 注册命令 - 使用 RPGCore CommandFramework
     */
    private void registerCommands() {
        CommandFramework framework = CommandFramework.getInstance();
        if (framework != null) {
            framework.registerCommand(new DropControlCommand(this));
            getLogger().info("已使用 CommandFramework 注册命令");
        } else {
            getLogger().severe("CommandFramework 不可用，命令注册失败");
        }
    }

    // 保留旧方法用于兼容
    private boolean onCommandLegacy(org.bukkit.command.CommandSender sender, org.bukkit.command.Command command, String label, String[] args) {
        if (args.length == 0) {
            // 无参数时，给玩家显示帮助或自己的状态
            if (sender.hasPermission("gddrop.admin")) {
                sender.sendMessage(color(rawMessageUsage));
            } else if (sender instanceof Player && sender.hasPermission("gddrop.use")) {
                // 普通玩家显示自己的状态
                Player player = (Player) sender;
                UUID uuid = player.getUniqueId();
                Boolean status = playerDropStatus.get(uuid);
                String statusText;
                if (status != null) {
                    statusText = status ? "<green>启用" : "<red>禁用";
                } else {
                    statusText = playerDefaultEnabled ? "<green>启用 (默认)" : "<red>禁用 (默认)";
                }
                sender.sendMessage(color(rawMessagePlayerStatus.replace("%status%", statusText)));
            } else {
                sender.sendMessage(color(rawMessageNoPermission));
            }
            return true;
        }

        String subCommand = args[0].toLowerCase();

        // toggle 命令 - 玩家自己切换丢弃功能
        if (subCommand.equals("toggle")) {
            if (!(sender instanceof Player)) {
                sender.sendMessage(color("<red>此命令只能由玩家执行!"));
                return true;
            }

            Player player = (Player) sender;
            if (!player.hasPermission("gddrop.use")) {
                player.sendMessage(color(rawMessageNoPermission));
                return true;
            }

            UUID playerUuid = player.getUniqueId();
            Boolean currentStatus = playerDropStatus.get(playerUuid);

            if (currentStatus == null) {
                currentStatus = playerDefaultEnabled;
            }

            boolean newStatus = !currentStatus;
            playerDropStatus.put(playerUuid, newStatus);

            String statusText = newStatus ? "<green>启用" : "<red>禁用";
            player.sendMessage(color(rawMessageToggleSuccess.replace("%status%", statusText)));
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
                    statusText = status ? "<green>启用" : "<red>禁用";
                } else {
                    statusText = playerDefaultEnabled ? "<green>启用 (默认)" : "<red>禁用 (默认)";
                }
                sender.sendMessage(color(rawMessagePlayerStatus.replace("%status%", statusText)));
                return true;
            }
            // 管理员查看全局状态（继续执行下面的管理员检查）
        }

        // 以下命令需要管理员权限
        if (!sender.hasPermission("gddrop.admin")) {
            sender.sendMessage(color(rawMessageNoPermission));
            return true;
        }

        switch (subCommand) {
            case "enable":
                dropEnabled = true;
                config.set("settings.enabled", true);
                saveConfig();
                Bukkit.getServer().broadcast(color(rawMessageDropEnabled));
                break;

            case "disable":
                dropEnabled = false;
                config.set("settings.enabled", false);
                saveConfig();
                Bukkit.getServer().broadcast(color(rawMessageDropDisabled));
                break;

            case "player":
                if (args.length < 2) {
                    sender.sendMessage(color(rawMessageUsage));
                    return true;
                }

                Player target = Bukkit.getPlayer(args[1]);
                if (target == null) {
                    sender.sendMessage(color("<red>玩家不存在或不在线!"));
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
                    sender.sendMessage(color(rawMessagePlayerEnabled.replace("%player%", target.getName())));
                } else {
                    sender.sendMessage(color(rawMessagePlayerDisabled.replace("%player%", target.getName())));
                }
                break;

            case "reload":
                reloadConfig();
                loadConfig();
                sender.sendMessage(color(rawMessageReload));
                break;

            case "status":
                String status = dropEnabled ? "<green>启用" : "<red>禁用";
                sender.sendMessage(color(rawMessageStatus.replace("%status%", status)));
                break;

            default:
                sender.sendMessage(color(rawMessageUsage));
                break;
        }

        return true;
    }



    /**
     * 使用 MiniMessage 解析颜色代码
     */
    private Component color(String message) {
        if (message == null) return Component.empty();
        // 将 & 颜色代码转换为 MiniMessage 格式
        String miniMessageText = message
            .replace("<black>", "<black>").replace("<dark_blue>", "<dark_blue>")
            .replace("<dark_green>", "<dark_green>").replace("<dark_aqua>", "<dark_aqua>")
            .replace("<dark_red>", "<dark_red>").replace("<dark_purple>", "<dark_purple>")
            .replace("<gold>", "<gold>").replace("<gray>", "<gray>")
            .replace("<dark_gray>", "<dark_gray>").replace("<blue>", "<blue>")
            .replace("<green>", "<green>").replace("<aqua>", "<aqua>")
            .replace("<red>", "<red>").replace("<light_purple>", "<light_purple>")
            .replace("<yellow>", "<yellow>").replace("<white>", "<white>")
            .replace("<obfuscated>", "<obfuscated>").replace("<bold>", "<bold>")
            .replace("<strikethrough>", "<strikethrough>").replace("<underlined>", "<underlined>")
            .replace("<italic>", "<italic>").replace("<reset>", "<reset>");
        return miniMessageParser.deserialize(miniMessageText);
    }

    /**
     * 获取 MiniMessageService
     * @return MiniMessageService 实例（可能为本地降级实现）
     */
    public MiniMessageService getMiniMessageService() {
        return miniMessage;
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
