package cn.guangdian.gift;

import cn.guangdian.gift.adapter.GiftServiceAdapter;
import cn.guangdian.gift.model.GiftConfig;
import cn.guangdian.gift.model.GiftConditions;
import cn.guangdian.rpgcore.RPGCore;
import cn.guangdian.rpgcore.api.GameLogger;
import cn.guangdian.rpgcore.api.ServiceRegistry;
import cn.guangdian.rpgcore.message.MiniMessageService;
import cn.guangdian.rpgcore.plugin.AbstractRPGPlugin;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 光点礼包插件 - GuangDianGift
 *
 * <p>支持 RPGItems 物品发放，包含条件限制功能：
 * <ul>
 *   <li>领取次数限制</li>
 *   <li>消耗物品领取</li>
 *   <li>权限检查</li>
 *   <li>等级限制</li>
 *   <li>冷却时间</li>
 * </ul>
 *
 * @author Gumin
 * @version 2.0.0
 */
public class GuangDianGift extends AbstractRPGPlugin {

    // 礼包配置缓存
    private Map<String, GiftConfig> giftConfigs = new ConcurrentHashMap<>();

    // 玩家领取记录: Map<玩家UUID, Map<礼包名, 领取次数>>
    private Map<UUID, Map<String, Integer>> claimRecords = new ConcurrentHashMap<>();

    // 玩家冷却记录: Map<玩家UUID, Map<礼包名, 上次领取时间戳>>
    private Map<UUID, Map<String, Long>> cooldownRecords = new ConcurrentHashMap<>();

    private GiftServiceAdapter giftServiceAdapter;

    // RPGCore 服务
    private GameLogger gameLogger;
    private MiniMessageService miniMessage;

    @Override
    protected void onPluginEnable() {
        initRPGCoreServices();
        saveDefaultConfig();
        loadGifts();
        loadPlayerData();
        registerRPGCoreService();
        logInfo("GuangDianGift 礼包插件已启用！(RPGItems模式)");
    }

    private void initRPGCoreServices() {
        RPGCore rpgCore = RPGCore.getInstance();
        if (rpgCore != null) {
            gameLogger = rpgCore.getGameLogger();
            miniMessage = rpgCore.getMiniMessageService();
        }
        if (miniMessage == null) {
            miniMessage = MiniMessageService.getInstance();
        }
    }

    public void logInfo(String message) {
        if (gameLogger != null) {
            gameLogger.info(message);
        } else {
            getLogger().info(message);
        }
    }

    public void logWarning(String message) {
        if (gameLogger != null) {
            gameLogger.warning(message);
        } else {
            getLogger().warning(message);
        }
    }

    public void logSevere(String message) {
        if (gameLogger != null) {
            gameLogger.severe(message);
        } else {
            getLogger().severe(message);
        }
    }

    @Override
    protected void onPluginDisable() {
        savePlayerData();
        unregisterRPGCoreService();
        if (scheduler != null) {
            scheduler.cancelAllTasks();
        }
    }

    @Override
    protected String getPluginName() {
        return "GuangDianGift";
    }

    private void registerRPGCoreService() {
        RPGCore rpgCore = RPGCore.getInstance();
        if (rpgCore != null) {
            ServiceRegistry serviceRegistry = rpgCore.getServiceRegistry();
            if (serviceRegistry != null) {
                giftServiceAdapter = new GiftServiceAdapter(this, serviceRegistry);
                giftServiceAdapter.register();
            }
        }
    }

    private void unregisterRPGCoreService() {
        if (giftServiceAdapter != null) {
            giftServiceAdapter.unregister();
        }
    }

    /**
     * 加载礼包配置
     */
    private void loadGifts() {
        giftConfigs.clear();
        File giftsFile = new File(getDataFolder(), "gifts.yml");
        if (!giftsFile.exists()) {
            saveResource("gifts.yml", false);
        }

        try {
            String content = Files.readString(giftsFile.toPath());
            org.yaml.snakeyaml.Yaml yaml = new org.yaml.snakeyaml.Yaml();
            Map<String, Object> data = yaml.load(content);

            if (data != null && data.containsKey("gifts")) {
                Map<String, Object> giftsData = (Map<String, Object>) data.get("gifts");
                for (Map.Entry<String, Object> entry : giftsData.entrySet()) {
                    String giftName = entry.getKey();
                    if (entry.getValue() instanceof Map) {
                        GiftConfig config = parseGiftConfig(giftName, (Map<String, Object>) entry.getValue());
                        if (config != null) {
                            giftConfigs.put(giftName, config);
                        }
                    }
                }
            }
            logInfo("已加载 " + giftConfigs.size() + " 个礼包配置");
        } catch (IOException e) {
            logSevere("加载礼包配置失败: " + e.getMessage());
        }
    }

    /**
     * 解析礼包配置
     */
    private GiftConfig parseGiftConfig(String name, Map<String, Object> data) {
        GiftConfig config = new GiftConfig();
        config.setName(name);

        // 解析显示名称
        if (data.containsKey("display")) {
            config.setDisplay((String) data.get("display"));
        }

        // 解析描述
        if (data.containsKey("description")) {
            List<String> desc = new ArrayList<>();
            Object descObj = data.get("description");
            if (descObj instanceof List) {
                for (Object d : (List<?>) descObj) {
                    desc.add(String.valueOf(d));
                }
            }
            config.setDescription(desc);
        }

        // 解析条件
        if (data.containsKey("conditions")) {
            Map<String, Object> condData = (Map<String, Object>) data.get("conditions");
            GiftConditions conditions = new GiftConditions();

            if (condData.containsKey("max-claims")) {
                conditions.setMaxClaims(((Number) condData.get("max-claims")).intValue());
            }
            if (condData.containsKey("permission")) {
                conditions.setPermission((String) condData.get("permission"));
            }
            if (condData.containsKey("min-level")) {
                conditions.setMinLevel(((Number) condData.get("min-level")).intValue());
            }
            if (condData.containsKey("cost-money")) {
                conditions.setCostMoney(((Number) condData.get("cost-money")).intValue());
            }
            if (condData.containsKey("cooldown")) {
                conditions.setCooldown(((Number) condData.get("cooldown")).longValue());
            }
            if (condData.containsKey("cost-items")) {
                List<String> costItems = new ArrayList<>();
                Object costObj = condData.get("cost-items");
                if (costObj instanceof List) {
                    for (Object item : (List<?>) costObj) {
                        costItems.add(String.valueOf(item));
                    }
                }
                conditions.setCostItems(costItems);
            }

            config.setConditions(conditions);
        }

        // 解析物品列表
        if (data.containsKey("items")) {
            List<String> items = new ArrayList<>();
            Object itemsObj = data.get("items");
            if (itemsObj instanceof List) {
                for (Object item : (List<?>) itemsObj) {
                    items.add(String.valueOf(item));
                }
            }
            config.setItems(items);
        }

        return config;
    }

    /**
     * 加载玩家数据
     */
    private void loadPlayerData() {
        File dataFile = new File(getDataFolder(), "playerdata.yml");
        if (!dataFile.exists()) {
            return;
        }

        try {
            String content = Files.readString(dataFile.toPath());
            org.yaml.snakeyaml.Yaml yaml = new org.yaml.snakeyaml.Yaml();
            Map<String, Object> data = yaml.load(content);

            if (data != null) {
                // 加载领取记录
                if (data.containsKey("claims")) {
                    Map<String, Object> claimsData = (Map<String, Object>) data.get("claims");
                    for (Map.Entry<String, Object> entry : claimsData.entrySet()) {
                        UUID uuid = UUID.fromString(entry.getKey());
                        Map<String, Integer> playerClaims = new ConcurrentHashMap<>();
                        if (entry.getValue() instanceof Map) {
                            for (Map.Entry<String, Object> claim : ((Map<String, Object>) entry.getValue()).entrySet()) {
                                playerClaims.put(claim.getKey(), ((Number) claim.getValue()).intValue());
                            }
                        }
                        claimRecords.put(uuid, playerClaims);
                    }
                }

                // 加载冷却记录
                if (data.containsKey("cooldowns")) {
                    Map<String, Object> cooldownData = (Map<String, Object>) data.get("cooldowns");
                    for (Map.Entry<String, Object> entry : cooldownData.entrySet()) {
                        UUID uuid = UUID.fromString(entry.getKey());
                        Map<String, Long> playerCooldowns = new ConcurrentHashMap<>();
                        if (entry.getValue() instanceof Map) {
                            for (Map.Entry<String, Object> cd : ((Map<String, Object>) entry.getValue()).entrySet()) {
                                playerCooldowns.put(cd.getKey(), ((Number) cd.getValue()).longValue());
                            }
                        }
                        cooldownRecords.put(uuid, playerCooldowns);
                    }
                }
            }
            logInfo("已加载玩家数据");
        } catch (Exception e) {
            logWarning("加载玩家数据失败: " + e.getMessage());
        }
    }

    /**
     * 保存玩家数据
     */
    private void savePlayerData() {
        File dataFile = new File(getDataFolder(), "playerdata.yml");
        dataFile.getParentFile().mkdirs();

        try {
            StringBuilder sb = new StringBuilder();
            sb.append("# GuangDianGift 玩家数据\n");
            sb.append("# 自动生成，请勿手动修改\n\n");

            sb.append("claims:\n");
            for (Map.Entry<UUID, Map<String, Integer>> entry : claimRecords.entrySet()) {
                sb.append("  ").append(entry.getKey()).append(":\n");
                for (Map.Entry<String, Integer> claim : entry.getValue().entrySet()) {
                    sb.append("    ").append(claim.getKey()).append(": ").append(claim.getValue()).append("\n");
                }
            }

            sb.append("\ncooldowns:\n");
            for (Map.Entry<UUID, Map<String, Long>> entry : cooldownRecords.entrySet()) {
                sb.append("  ").append(entry.getKey()).append(":\n");
                for (Map.Entry<String, Long> cd : entry.getValue().entrySet()) {
                    sb.append("    ").append(cd.getKey()).append(": ").append(cd.getValue()).append("\n");
                }
            }

            Files.writeString(dataFile.toPath(), sb.toString());
        } catch (IOException e) {
            logSevere("保存玩家数据失败: " + e.getMessage());
        }
    }

    /**
     * 检查玩家是否可以领取礼包
     */
    public ClaimResult canClaim(Player player, String giftName) {
        GiftConfig config = giftConfigs.get(giftName);
        if (config == null) {
            return ClaimResult.fail("礼包不存在: " + giftName);
        }

        GiftConditions conditions = config.getConditions();
        UUID uuid = player.getUniqueId();

        // 检查权限
        String permission = conditions.getPermission();
        if (permission != null && !permission.isEmpty() && !player.hasPermission(permission)) {
            return ClaimResult.fail("你没有权限领取此礼包");
        }

        // 检查领取次数
        int maxClaims = conditions.getMaxClaims();
        if (maxClaims > 0) {
            int claimed = getClaimCount(uuid, giftName);
            if (claimed >= maxClaims) {
                return ClaimResult.fail("你已达到此礼包的最大领取次数 (" + maxClaims + "次)");
            }
        }

        // 检查冷却时间
        long cooldown = conditions.getCooldown();
        if (cooldown > 0) {
            Long lastClaim = getLastClaimTime(uuid, giftName);
            if (lastClaim != null) {
                long elapsed = (System.currentTimeMillis() - lastClaim) / 1000;
                if (elapsed < cooldown) {
                    long remaining = cooldown - elapsed;
                    return ClaimResult.fail("冷却中，还需等待 " + formatTime(remaining));
                }
            }
        }

        // 检查消耗物品
        List<String> costItems = conditions.getCostItems();
        if (costItems != null && !costItems.isEmpty()) {
            for (String costItem : costItems) {
                String[] parts = costItem.split(":");
                String itemName = parts[0];
                int amount = parts.length > 1 ? Integer.parseInt(parts[1]) : 1;

                if (!hasRPGItem(player, itemName, amount)) {
                    return ClaimResult.fail("需要物品: " + itemName + " x" + amount);
                }
            }
        }

        return ClaimResult.success();
    }

    /**
     * 给予玩家礼包
     */
    public boolean giveGift(Player player, String giftName) {
        GiftConfig config = giftConfigs.get(giftName);
        if (config == null) {
            return false;
        }

        // 检查条件
        ClaimResult result = canClaim(player, giftName);
        if (!result.isSuccess()) {
            player.sendMessage(miniMessage.red(result.getMessage()));
            return false;
        }

        GiftConditions conditions = config.getConditions();
        UUID uuid = player.getUniqueId();

        // 扣除消耗物品
        List<String> costItems = conditions.getCostItems();
        if (costItems != null && !costItems.isEmpty()) {
            for (String costItem : costItems) {
                String[] parts = costItem.split(":");
                String itemName = parts[0];
                int amount = parts.length > 1 ? Integer.parseInt(parts[1]) : 1;
                removeRPGItem(player, itemName, amount);
            }
        }

        // 给予物品 (使用 RPGItems 命令)
        List<String> items = config.getItems();
        for (String item : items) {
            String[] parts = item.split(":");
            String itemName = parts[0];
            int amount = parts.length > 1 ? Integer.parseInt(parts[1]) : 1;

            for (int i = 0; i < amount; i++) {
                String cmd = "rpgitem " + itemName + " give " + player.getName();
                Bukkit.dispatchCommand(Bukkit.getConsoleSender(), cmd);
            }
        }

        // 更新领取记录
        incrementClaimCount(uuid, giftName);
        setLastClaimTime(uuid, giftName);

        // 发送消息
        player.sendMessage(miniMessage.green("你获得了礼包: ").append(miniMessage.yellow(giftName)));

        return true;
    }

    /**
     * 检查玩家是否有指定的RPGItem物品
     */
    private boolean hasRPGItem(Player player, String itemName, int amount) {
        int count = 0;
        for (ItemStack item : player.getInventory().getContents()) {
            if (item != null && item.hasItemMeta()) {
                // 检查是否是RPGItem
                if (isRPGItem(item, itemName)) {
                    count += item.getAmount();
                }
            }
        }
        return count >= amount;
    }

    /**
     * 移除玩家背包中的RPGItem物品
     */
    private void removeRPGItem(Player player, String itemName, int amount) {
        int remaining = amount;
        ItemStack[] contents = player.getInventory().getContents();

        for (int i = 0; i < contents.length && remaining > 0; i++) {
            ItemStack item = contents[i];
            if (item != null && item.hasItemMeta() && isRPGItem(item, itemName)) {
                int itemAmount = item.getAmount();
                if (itemAmount <= remaining) {
                    player.getInventory().setItem(i, null);
                    remaining -= itemAmount;
                } else {
                    item.setAmount(itemAmount - remaining);
                    remaining = 0;
                }
            }
        }
        player.updateInventory();
    }

    /**
     * 检查物品是否是指定的RPGItem
     */
    private boolean isRPGItem(ItemStack item, String itemName) {
        if (item == null || !item.hasItemMeta()) {
            return false;
        }
        // 使用PDC检查RPGItem名称
        org.bukkit.NamespacedKey key = new org.bukkit.NamespacedKey("rpgitems", "item");
        String rpgItemName = item.getItemMeta().getPersistentDataContainer().get(key, org.bukkit.persistence.PersistentDataType.STRING);
        return itemName.equals(rpgItemName);
    }

    // ========== 领取记录管理 ==========

    private int getClaimCount(UUID uuid, String giftName) {
        Map<String, Integer> playerClaims = claimRecords.get(uuid);
        if (playerClaims == null) {
            return 0;
        }
        return playerClaims.getOrDefault(giftName, 0);
    }

    private void incrementClaimCount(UUID uuid, String giftName) {
        Map<String, Integer> playerClaims = claimRecords.computeIfAbsent(uuid, k -> new ConcurrentHashMap<>());
        playerClaims.merge(giftName, 1, Integer::sum);
    }

    private Long getLastClaimTime(UUID uuid, String giftName) {
        Map<String, Long> playerCooldowns = cooldownRecords.get(uuid);
        if (playerCooldowns == null) {
            return null;
        }
        return playerCooldowns.get(giftName);
    }

    private void setLastClaimTime(UUID uuid, String giftName) {
        Map<String, Long> playerCooldowns = cooldownRecords.computeIfAbsent(uuid, k -> new ConcurrentHashMap<>());
        playerCooldowns.put(giftName, System.currentTimeMillis());
    }

    // ========== 工具方法 ==========

    private String formatTime(long seconds) {
        if (seconds < 60) {
            return seconds + "秒";
        } else if (seconds < 3600) {
            return (seconds / 60) + "分钟";
        } else if (seconds < 86400) {
            return (seconds / 3600) + "小时";
        } else {
            return (seconds / 86400) + "天";
        }
    }

    // ========== Getter ==========

    public Map<String, GiftConfig> getGiftConfigs() {
        return giftConfigs;
    }

    public GiftConfig getGiftConfig(String name) {
        return giftConfigs.get(name);
    }

    public MiniMessageService getMiniMessage() {
        return miniMessage;
    }

    // ========== 命令处理 ==========

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        if (args.length < 1) {
            sendHelp(sender);
            return true;
        }

        String subCommand = args[0].toLowerCase();

        switch (subCommand) {
            case "list":
                listGifts(sender);
                break;
            case "info":
                if (args.length < 2) {
                    sender.sendMessage(miniMessage.yellow("用法: /gift info <礼包名>"));
                } else {
                    showGiftInfo(sender, args[1]);
                }
                break;
            case "reload":
                loadGifts();
                sender.sendMessage(miniMessage.green("礼包配置已重新加载"));
                break;
            case "claim":
            case "领取":
                if (!(sender instanceof Player)) {
                    sender.sendMessage(miniMessage.red("只有玩家可以领取礼包"));
                    return true;
                }
                if (args.length < 2) {
                    sender.sendMessage(miniMessage.yellow("用法: /gift claim <礼包名>"));
                } else {
                    giveGift((Player) sender, args[1]);
                }
                break;
            default:
                // 兼容旧命令格式: /gift <礼包名> [玩家]
                handleLegacyCommand(sender, args);
                break;
        }

        return true;
    }

    private void handleLegacyCommand(CommandSender sender, String[] args) {
        String giftName = args[0];
        Player target;

        if (args.length > 1) {
            target = Bukkit.getPlayer(args[1]);
            if (target == null) {
                sender.sendMessage(miniMessage.red("玩家不存在: " + args[1]));
                return;
            }
        } else if (sender instanceof Player) {
            target = (Player) sender;
        } else {
            sender.sendMessage(miniMessage.red("控制台需要指定玩家"));
            return;
        }

        giveGift(target, giftName);
    }

    private void sendHelp(CommandSender sender) {
        sender.sendMessage(miniMessage.aqua("========== GuangDianGift 帮助 =========="));
        sender.sendMessage(miniMessage.colorize("<yellow>/gift list <gray>- 查看所有礼包"));
        sender.sendMessage(miniMessage.colorize("<yellow>/gift info <礼包名> <gray>- 查看礼包详情"));
        sender.sendMessage(miniMessage.colorize("<yellow>/gift claim <礼包名> <gray>- 领取礼包"));
        sender.sendMessage(miniMessage.colorize("<yellow>/gift reload <gray>- 重载配置"));
        sender.sendMessage(miniMessage.aqua("========================================"));
    }

    private void listGifts(CommandSender sender) {
        sender.sendMessage(miniMessage.aqua("========== 礼包列表 =========="));
        for (Map.Entry<String, GiftConfig> entry : giftConfigs.entrySet()) {
            GiftConfig config = entry.getValue();
            sender.sendMessage(miniMessage.colorize("<yellow> - " + entry.getKey() +
                    " <gray>(" + config.getItems().size() + "件物品)"));
        }
        sender.sendMessage(miniMessage.aqua("=============================="));
    }

    private void showGiftInfo(CommandSender sender, String giftName) {
        GiftConfig config = giftConfigs.get(giftName);
        if (config == null) {
            sender.sendMessage(miniMessage.red("礼包不存在: " + giftName));
            return;
        }

        sender.sendMessage(miniMessage.aqua("========== " + giftName + " =========="));

        // 显示描述
        for (String desc : config.getDescription()) {
            sender.sendMessage(miniMessage.colorize(desc));
        }

        // 显示条件
        GiftConditions conditions = config.getConditions();
        sender.sendMessage(miniMessage.gray("--- 领取条件 ---"));
        sender.sendMessage(miniMessage.gray("最大领取次数: " +
                (conditions.getMaxClaims() < 0 ? "无限" : conditions.getMaxClaims())));
        if (conditions.getPermission() != null && !conditions.getPermission().isEmpty()) {
            sender.sendMessage(miniMessage.gray("需要权限: " + conditions.getPermission()));
        }
        if (conditions.getMinLevel() > 0) {
            sender.sendMessage(miniMessage.gray("需要等级: " + conditions.getMinLevel()));
        }
        if (conditions.getCostMoney() > 0) {
            sender.sendMessage(miniMessage.gray("消耗金币: " + conditions.getCostMoney()));
        }
        if (conditions.getCooldown() > 0) {
            sender.sendMessage(miniMessage.gray("冷却时间: " + formatTime(conditions.getCooldown())));
        }
        if (conditions.getCostItems() != null && !conditions.getCostItems().isEmpty()) {
            sender.sendMessage(miniMessage.gray("消耗物品:"));
            for (String item : conditions.getCostItems()) {
                sender.sendMessage(miniMessage.gray("  - " + item));
            }
        }

        // 显示物品
        sender.sendMessage(miniMessage.gray("--- 礼包内容 ---"));
        for (String item : config.getItems()) {
            sender.sendMessage(miniMessage.gray("  - " + item));
        }

        // 显示玩家领取状态
        if (sender instanceof Player) {
            Player player = (Player) sender;
            int claimed = getClaimCount(player.getUniqueId(), giftName);
            int max = conditions.getMaxClaims();
            if (max > 0) {
                sender.sendMessage(miniMessage.aqua("已领取: " + claimed + "/" + max));
            }

            Long lastClaim = getLastClaimTime(player.getUniqueId(), giftName);
            if (lastClaim != null && conditions.getCooldown() > 0) {
                long elapsed = (System.currentTimeMillis() - lastClaim) / 1000;
                if (elapsed < conditions.getCooldown()) {
                    sender.sendMessage(miniMessage.red("冷却中，还需: " + formatTime(conditions.getCooldown() - elapsed)));
                } else {
                    sender.sendMessage(miniMessage.green("可以领取"));
                }
            }
        }

        sender.sendMessage(miniMessage.aqua("=============================="));
    }

    /**
     * 领取结果类
     */
    public static class ClaimResult {
        private final boolean success;
        private final String message;

        private ClaimResult(boolean success, String message) {
            this.success = success;
            this.message = message;
        }

        public static ClaimResult success() {
            return new ClaimResult(true, "");
        }

        public static ClaimResult fail(String message) {
            return new ClaimResult(false, message);
        }

        public boolean isSuccess() {
            return success;
        }

        public String getMessage() {
            return message;
        }
    }
}
