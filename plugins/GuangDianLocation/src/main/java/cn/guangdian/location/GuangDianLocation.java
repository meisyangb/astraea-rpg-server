package cn.guangdian.location;

import cn.guangdian.location.adapter.LocationServiceAdapter;
import cn.guangdian.location.listener.LocationSelectionListener;
import cn.guangdian.location.service.LocationStorageService;
import cn.guangdian.rpgcore.RPGCore;
import cn.guangdian.rpgcore.api.AsyncExecutor;
import cn.guangdian.rpgcore.database.CoreDatabase;
import cn.guangdian.rpgcore.message.UnifiedMessageService;
import cn.guangdian.rpgcore.plugin.AbstractRPGPlugin;
import cn.guangdian.rpgcore.service.api.LocationService;
import cn.guangdian.rpgcore.sound.SoundService;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.World;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

public class GuangDianLocation extends AbstractRPGPlugin implements CommandExecutor, TabCompleter {

    private static GuangDianLocation instance;
    private LocationStorageService storageService;
    private LocationServiceAdapter serviceAdapter;
    private AsyncExecutor asyncExecutor;

    // RPGCore 服务引用
    private SoundService soundService;
    private UnifiedMessageService msg;

    private int maxLocationsPerPlayer;
    private boolean selectionParticleEnabled;
    private boolean selectionSoundEnabled;
    private String selectionParticleType;
    private String selectionSoundType;

    @Override
    protected void onPluginEnable() {
        instance = this;

        // 加载配置
        saveDefaultConfig();
        loadSettings();

        // 初始化数据库
        initDatabase();

        // 初始化服务
        initServices();

        // 注册监听器
        registerListeners();

        // 注册命令
        registerCommands();

        // 注册 RPGCore 服务
        registerRPGCoreService();

        getLogger().info("光点坐标插件已启用! 版本: " + getDescription().getVersion());
        getLogger().info("作者: GuangDian");
    }

    @Override
    protected void onPluginDisable() {
        if (serviceAdapter != null) {
            serviceAdapter.unregister();
        }

        getLogger().info("光点坐标插件已禁用!");
    }

    @Override
    protected String getPluginName() {
        return "GuangDianLocation";
    }

    private void loadSettings() {
        maxLocationsPerPlayer = getConfig().getInt("settings.max-locations-per-player", 50);
        selectionParticleEnabled = getConfig().getBoolean("settings.selection.particle.enabled", true);
        selectionSoundEnabled = getConfig().getBoolean("settings.selection.sound.enabled", true);
        selectionParticleType = getConfig().getString("settings.selection.particle.type", "END_ROD");
        selectionSoundType = getConfig().getString("settings.selection.sound.type", "BLOCK_NOTE_BLOCK_PLING");
    }

    private void initDatabase() {
        boolean useCoreDatabase = getConfig().getBoolean("database.use-rpgcore-pool", true);

        if (useCoreDatabase) {
            // 使用 RPGCore 共享连接池
            if (CoreDatabase.isEnabled()) {
                getLogger().info("使用 RPGCore 共享数据库连接池");
            } else {
                getLogger().warning("RPGCore 数据库连接池未启用，请检查 RPGCore 配置");
            }
        } else {
            // 使用独立连接池
            getLogger().warning("独立数据库连接池暂不支持，请使用 RPGCore 共享连接池");
        }
    }

    private void initServices() {
        // 获取 RPGCore 服务
        if (getServer().getPluginManager().isPluginEnabled("RPGCore")) {
            try {
                RPGCore rpgCore = RPGCore.getInstance();
                asyncExecutor = rpgCore.getAsyncExecutor();
                soundService = rpgCore.getSoundService();
                msg = UnifiedMessageService.getInstance();
                getLogger().info("使用 RPGCore 服务 (AsyncExecutor, SoundService, UnifiedMessageService)");
            } catch (Exception e) {
                getLogger().warning("无法获取 RPGCore 服务: " + e.getMessage());
            }
        }

        // 如果 RPGCore 服务不可用，使用本地降级
        if (soundService == null) {
            soundService = SoundService.getInstance();
        }
        if (msg == null) {
            msg = UnifiedMessageService.getInstance();
        }

        // 创建存储服务
        storageService = new LocationStorageService(this);
    }

    private void registerListeners() {
        getServer().getPluginManager().registerEvents(new LocationSelectionListener(this), this);
    }

    private void registerCommands() {
        getCommand("location").setExecutor(this);
        getCommand("location").setTabCompleter(this);
        getCommand("setwarp").setExecutor(this);
        getCommand("setwarp").setTabCompleter(this);
        getCommand("warp").setExecutor(this);
        getCommand("warp").setTabCompleter(this);
    }

    private void registerRPGCoreService() {
        if (getServer().getPluginManager().isPluginEnabled("RPGCore")) {
            serviceAdapter = new LocationServiceAdapter(this);
            if (serviceAdapter.isUsingRPGCore()) {
                getLogger().info("已集成 RPGCore 服务系统: LocationService");
            }
        } else {
            getLogger().warning("RPGCore 未启用，部分功能可能受限");
        }
    }

    /**
     * 处理坐标选择
     * 
     * @param player 玩家
     * @param location 位置
     * @param name 名称
     */
    public void handleLocationSelection(Player player, Location location, String name) {
        UUID playerId = player.getUniqueId();

        // 检查权限
        if (!player.hasPermission("guangdian.location.use")) {
            player.sendMessage(msg.colorize("<red>你没有权限使用坐标选择功能!</red>"));
            return;
        }

        // 检查数量限制
        CompletableFuture<Integer> countFuture = storageService.getLocationCountAsync(playerId);
        countFuture.thenAccept(count -> {
            if (count >= maxLocationsPerPlayer) {
                runSync(() -> player.sendMessage(msg.colorize(
                    "<red>你已经保存了 <yellow>" + count + " <red>个坐标点，达到上限 <yellow>" + maxLocationsPerPlayer + "<red>!")));
                return;
            }

            // 检查是否已存在同名坐标
            CompletableFuture<Boolean> existsFuture = storageService.hasLocationAsync(playerId, name);
            existsFuture.thenAccept(exists -> {
                if (exists) {
                    runSync(() -> player.sendMessage(msg.colorize("<red>坐标点 <yellow>" + name + " <red>已存在!")));
                    return;
                }

                // 保存坐标
                CompletableFuture<Boolean> saveFuture = storageService.saveLocationAsync(playerId, name, location);
                saveFuture.thenAccept(success -> {
                    if (success) {
                        runSync(() -> {
                            // 发送成功提示
                            player.sendMessage(msg.colorize("<green>成功保存坐标点 <yellow>" + name + "<green>!"));
                            player.sendMessage(msg.colorize("<gray>位置: <white>" + formatLocation(location)));

                            // 显示粒子效果
                            playSelectionEffect(player, location);
                        });
                    } else {
                        runSync(() -> player.sendMessage(msg.colorize("<red>保存坐标点失败，请稍后重试!")));
                    }
                });
            });
        });
    }

    /**
     * 播放选点效果
     */
    private void playSelectionEffect(Player player, Location location) {
        World world = location.getWorld();
        if (world == null) return;

        // 粒子效果
        if (selectionParticleEnabled) {
            try {
                Particle particle = Particle.valueOf(selectionParticleType);
                world.spawnParticle(particle, location.add(0, 1, 0), 20, 0.5, 0.5, 0.5, 0.1);
            } catch (Exception e) {
                getLogger().warning("无效的粒子类型: " + selectionParticleType);
            }
        }

        // 音效 - 使用 RPGCore SoundService
        if (selectionSoundEnabled && soundService != null) {
            soundService.playSound(player, selectionSoundType, 1.0f, 1.5f);
        }
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage(msg.colorize("<red>只有玩家可以使用此命令!"));
            return true;
        }

        Player player = (Player) sender;
        UUID playerId = player.getUniqueId();

        switch (command.getName().toLowerCase()) {
            case "location":
                return handleLocationCommand(player, args);
            case "setwarp":
                return handleSetWarpCommand(player, args);
            case "warp":
                return handleWarpCommand(player, args);
            default:
                return false;
        }
    }

    private boolean handleLocationCommand(Player player, String[] args) {
        if (args.length == 0) {
            sendLocationHelp(player);
            return true;
        }

        UUID playerId = player.getUniqueId();
        String subCommand = args[0].toLowerCase();

        switch (subCommand) {
            case "list":
                // 列出所有坐标
                CompletableFuture<List<LocationService.SavedLocationInfo>> listFuture = 
                    storageService.listLocationsAsync(playerId);
                listFuture.thenAccept(locations -> runSync(() -> {
                    if (locations.isEmpty()) {
                        player.sendMessage(msg.colorize("<yellow>你还没有保存任何坐标点。"));
                        player.sendMessage(msg.colorize("<gray>使用木锄头右键选点并输入名称来保存坐标。"));
                    } else {
                        player.sendMessage(msg.colorize("<gold>===== 你的坐标点列表 ====="));
                        for (LocationService.SavedLocationInfo info : locations) {
                            player.sendMessage(msg.colorize("<yellow>" + info.toDisplayString()));
                        }
                        player.sendMessage(msg.colorize("<gray>共 <yellow>" + locations.size() + " <gray>个坐标点"));
                    }
                }));
                return true;

            case "delete":
                if (args.length < 2) {
                    player.sendMessage(msg.colorize("<red>用法: /location delete <名称>"));
                    return true;
                }
                String deleteName = args[1];
                CompletableFuture<Boolean> deleteFuture = storageService.deleteLocationAsync(playerId, deleteName);
                deleteFuture.thenAccept(success -> runSync(() -> {
                    if (success) {
                        player.sendMessage(msg.colorize("<green>已删除坐标点 <yellow>" + deleteName + "<green>!"));
                    } else {
                        player.sendMessage(msg.colorize("<red>坐标点 <yellow>" + deleteName + " <red>不存在!"));
                    }
                }));
                return true;

            case "clear":
                if (!player.hasPermission("guangdian.location.admin")) {
                    player.sendMessage(msg.colorize("<red>没有权限!"));
                    return true;
                }
                CompletableFuture<Integer> clearFuture = CompletableFuture.supplyAsync(
                    () -> storageService.clearLocations(playerId));
                clearFuture.thenAccept(count -> runSync(() -> {
                    player.sendMessage(msg.colorize("<green>已清空所有坐标点，共 <yellow>" + count + " <green>个。"));
                }));
                return true;

            case "info":
                if (args.length < 2) {
                    player.sendMessage(msg.colorize("<red>用法: /location info <名称>"));
                    return true;
                }
                String infoName = args[1];
                CompletableFuture<Optional<Location>> infoFuture = storageService.getLocationAsync(playerId, infoName);
                infoFuture.thenAccept(locOpt -> runSync(() -> {
                    if (locOpt.isPresent()) {
                        Location loc = locOpt.get();
                        player.sendMessage(msg.colorize("<gold>===== 坐标点信息: <yellow>" + infoName + "<gold> ====="));
                        player.sendMessage(msg.colorize("<gray>世界: <white>" + (loc.getWorld() != null ? loc.getWorld().getName() : "未知")));
                        player.sendMessage(msg.colorize("<gray>坐标: <white>X=" + loc.getBlockX() + ", Y=" + loc.getBlockY() + ", Z=" + loc.getBlockZ()));
                        player.sendMessage(msg.colorize("<gray>精确: <white>" + formatLocation(loc)));
                    } else {
                        player.sendMessage(msg.colorize("<red>坐标点 <yellow>" + infoName + " <red>不存在!"));
                    }
                }));
                return true;

            case "help":
                sendLocationHelp(player);
                return true;

            default:
                sendLocationHelp(player);
                return true;
        }
    }

    private boolean handleSetWarpCommand(Player player, String[] args) {
        if (args.length < 1) {
            player.sendMessage(msg.colorize("<red>用法: /setwarp <名称>"));
            return true;
        }

        String name = args[0];
        Location location = player.getLocation();

        handleLocationSelection(player, location, name);
        return true;
    }

    private boolean handleWarpCommand(Player player, String[] args) {
        if (!player.hasPermission("guangdian.location.teleport")) {
            player.sendMessage(msg.colorize("<red>你没有权限传送!"));
            return true;
        }

        if (args.length < 1) {
            player.sendMessage(msg.colorize("<red>用法: /warp <名称>"));
            return true;
        }

        String name = args[0];
        UUID playerId = player.getUniqueId();

        CompletableFuture<Optional<Location>> locFuture = storageService.getLocationAsync(playerId, name);
        locFuture.thenAccept(locOpt -> runSync(() -> {
            if (locOpt.isPresent()) {
                Location loc = locOpt.get();
                World world = loc.getWorld();
                if (world == null) {
                    player.sendMessage(msg.colorize("<red>坐标点所在世界不存在或未加载!"));
                    return;
                }
                player.teleport(loc);
                player.sendMessage(msg.colorize("<green>已传送到坐标点 <yellow>" + name + "<green>!"));
                player.sendMessage(msg.colorize("<gray>位置: <white>" + formatLocation(loc)));
            } else {
                player.sendMessage(msg.colorize("<red>坐标点 <yellow>" + name + " <red>不存在!"));
                player.sendMessage(msg.colorize("<gray>使用 <yellow>/location list <gray>查看所有坐标点"));
            }
        }));
        return true;
    }

    private void sendLocationHelp(Player player) {
        player.sendMessage(msg.colorize("<gold>===== 坐标点系统帮助 ====="));
        player.sendMessage(msg.colorize("<yellow>木锄头右键 + 输入名称 <gray>- 选择并保存坐标"));
        player.sendMessage(msg.colorize("<yellow>/setwarp <名称> <gray>- 保存当前位置"));
        player.sendMessage(msg.colorize("<yellow>/warp <名称> <gray>- 传送到坐标点"));
        player.sendMessage(msg.colorize("<yellow>/location list <gray>- 列出所有坐标"));
        player.sendMessage(msg.colorize("<yellow>/location info <名称> <gray>- 查看坐标详情"));
        player.sendMessage(msg.colorize("<yellow>/location delete <名称> <gray>- 删除坐标"));
        if (player.hasPermission("guangdian.location.admin")) {
            player.sendMessage(msg.colorize("<yellow>/location clear <gray>- 清空所有坐标"));
        }
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        List<String> completions = new ArrayList<>();

        if (!(sender instanceof Player)) {
            return completions;
        }

        Player player = (Player) sender;
        String cmdName = command.getName().toLowerCase();

        switch (cmdName) {
            case "location":
                if (args.length == 1) {
                    completions.add("list");
                    completions.add("info");
                    completions.add("delete");
                    completions.add("help");
                    if (player.hasPermission("guangdian.location.admin")) {
                        completions.add("clear");
                    }
                } else if (args.length == 2) {
                    String subCmd = args[0].toLowerCase();
                    if (subCmd.equals("info") || subCmd.equals("delete")) {
                        // 补充坐标名称
                        List<LocationService.SavedLocationInfo> locations = 
                            storageService.listLocations(player.getUniqueId());
                        for (LocationService.SavedLocationInfo info : locations) {
                            completions.add(info.name());
                        }
                    }
                }
                break;

            case "warp":
                if (args.length == 1) {
                    // 补充坐标名称
                    List<LocationService.SavedLocationInfo> locations = 
                        storageService.listLocations(player.getUniqueId());
                    for (LocationService.SavedLocationInfo info : locations) {
                        completions.add(info.name());
                    }
                }
                break;

            case "setwarp":
                if (args.length == 1) {
                    completions.add("<名称>");
                }
                break;
        }

        return completions;
    }

    /**
     * 在主线程执行任务
     */
    private void runSync(Runnable task) {
        cn.guangdian.rpgcore.RPGCore rpgCore = cn.guangdian.rpgcore.RPGCore.getInstance();
        if (rpgCore != null) {
            rpgCore.getScheduler().runSync(task);
        }
    }

    private String formatLocation(Location loc) {
        World world = loc.getWorld();
        return String.format("[%s] X=%.2f, Y=%.2f, Z=%.2f (P=%.1f, Y=%.1f)",
            world != null ? world.getName() : "未知",
            loc.getX(), loc.getY(), loc.getZ(),
            loc.getPitch(), loc.getYaw());
    }

    /**
     * 发送 MiniMessage 格式的消息给玩家
     */
    private void sendMessage(Player player, String text) {
        player.sendMessage(msg.colorize(text));
    }

    /**
     * 发送 MiniMessage 格式的消息给 CommandSender
     */
    private void sendMessage(CommandSender sender, String text) {
        sender.sendMessage(msg.colorize(text));
    }

    public static GuangDianLocation getInstance() {
        return instance;
    }

    public LocationStorageService getStorageService() {
        return storageService;
    }

    public AsyncExecutor getAsyncExecutor() {
        return asyncExecutor;
    }

    public int getMaxLocationsPerPlayer() {
        return maxLocationsPerPlayer;
    }

    /**
     * 获取 SoundService
     * @return SoundService 实例（可能为本地降级实现）
     */
    public SoundService getSoundService() {
        return soundService;
    }

    /**
     * 获取 UnifiedMessageService (兼容旧 API)
     * @return UnifiedMessageService 实例
     */
    public UnifiedMessageService getUnifiedMessageService() {
        return msg;
    }
}