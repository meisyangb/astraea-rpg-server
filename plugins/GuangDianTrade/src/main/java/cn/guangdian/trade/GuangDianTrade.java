package cn.guangdian.trade;

import cn.guangdian.trade.adapter.TradeServiceAdapter;
import cn.guangdian.trade.placeholder.TradePlaceholder;
import cn.guangdian.rpgcore.RPGCore;
import cn.guangdian.rpgcore.api.GameLogger;
import cn.guangdian.rpgcore.message.MiniMessageService;
import cn.guangdian.rpgcore.plugin.AbstractRPGPlugin;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 光点交易插件 - GuangDianTrade
 *
 * <p>RPGCore 服务集成:
 * <ul>
 *   <li>GameLogger: 使用 RPGCore 统一日志服务</li>
 *   <li>SyncScheduler: 使用 RPGCore 同步任务调度器</li>
 * </ul>
 *
 * <p>优先级模式: 优先使用 RPGCore 服务，不可用则降级到本地实现
 *
 * @author Gumin
 * @QQ 2271257344
 * @version 1.0.0
 */
public class GuangDianTrade extends AbstractRPGPlugin implements Listener {

    private static GuangDianTrade instance;

    private final Map<UUID, TradeSession> activeTrades = new ConcurrentHashMap<>();
    private final Map<UUID, TradeRequest> pendingRequests = new ConcurrentHashMap<>();
    private final Map<UUID, Long> requestCooldowns = new ConcurrentHashMap<>();
    private final Object tradeLock = new Object();

    private int requestTimeout = 30;
    private int confirmCountdown = 3;
    private int requestCooldownTime = 5;
    private TradeServiceAdapter serviceAdapter;

    // RPGCore 服务
    private GameLogger gameLogger;
    private MiniMessageService miniMessage;
    
    @Override
    protected void onPluginEnable() {
        instance = this;

        // 初始化 RPGCore 服务
        initRPGCoreServices();

        saveDefaultConfig();
        loadConfig();
        getServer().getPluginManager().registerEvents(this, this);
        getCommand("trade").setExecutor(this);
        // 注册RPGCore服务适配器
        serviceAdapter = new TradeServiceAdapter(this);

        // 注册PlaceholderAPI扩展
        if (Bukkit.getPluginManager().isPluginEnabled("PlaceholderAPI")) {
            new TradePlaceholder(this).register();
            logInfo("已注册PlaceholderAPI扩展!");
        }

        logInfo("光点交易插件已启用! 版本: " + getDescription().getVersion());
        logInfo("作者: Gumin | QQ: 2271257344");
    }

    /**
     * 初始化 RPGCore 核心服务
     * 优先使用 RPGCore 统一服务，本地实现作为降级方案
     */
    private void initRPGCoreServices() {
        RPGCore rpgCore = RPGCore.getInstance();
        if (rpgCore != null) {
            gameLogger = rpgCore.getGameLogger();
            miniMessage = rpgCore.getMiniMessageService();
            if (gameLogger != null) {
                logInfo("已连接到 RPGCore GameLogger");
            }
            if (miniMessage != null) {
                logInfo("已连接到 RPGCore MiniMessageService");
            }
        }
        // 降级方案
        if (gameLogger == null) {
            logInfo("使用 Bukkit Logger（降级）");
        }
        if (miniMessage == null) {
            miniMessage = MiniMessageService.getInstance();
            logInfo("使用本地 MiniMessageService（降级）");
        }
    }

    /**
     * 日志辅助方法 - 优先使用 RPGCore GameLogger
     */
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

    public void logDebug(String message) {
        if (gameLogger != null) {
            gameLogger.debug(message);
        } else {
            getLogger().info("[DEBUG] " + message);
        }
    }

    @Override
    protected void onPluginDisable() {
        for (TradeSession session : new ArrayList<>(activeTrades.values())) {
            cancelTrade(session);
        }
        cn.guangdian.rpgcore.RPGCore rpgCore = cn.guangdian.rpgcore.RPGCore.getInstance();
        for (TradeRequest request : new ArrayList<>(pendingRequests.values())) {
            if (rpgCore != null) {
                rpgCore.getScheduler().cancelTask(request.getTaskId());
            }
        }
        activeTrades.clear();
        pendingRequests.clear();
        requestCooldowns.clear();
        if (serviceAdapter != null) {
            serviceAdapter.unregister();
            serviceAdapter = null;
        }
        if (scheduler != null) {
            scheduler.cancelAllTasks();
        }
        logInfo("光点交易插件已禁用!");
    }
    
    @Override
    protected String getPluginName() {
        return "GuangDianTrade";
    }

    private void loadConfig() {
        requestTimeout = Math.max(10, Math.min(300, getConfig().getInt("settings.request-timeout", 30)));
        confirmCountdown = Math.max(1, Math.min(60, getConfig().getInt("settings.confirm-countdown", 3)));
        requestCooldownTime = Math.max(0, Math.min(60, getConfig().getInt("settings.request-cooldown", 5)));
    }

    /**
     * 获取消息前缀
     */
    private Component getPrefix() {
        return miniMessage.parse("<gold>[<yellow>交易<gold>] ");
    }

    /**
     * 获取消息 Component
     */
    private Component getMessage(String key, String... replacements) {
        String msg = getConfig().getString("messages." + key, "");
        if (msg.isEmpty()) {
            switch (key) {
                case "no-permission": msg = "<red>你没有权限使用交易功能!"; break;
                case "request-sent": msg = "<green>已向 <yellow>%player% <green>发送交易请求!"; break;
                case "request-received": msg = "<yellow>%player% <green>想与你交易!"; break;
                case "request-timeout": msg = "<red>交易请求已超时!"; break;
                case "request-denied": msg = "<red>已拒绝交易请求!"; break;
                case "trade-started": msg = "<green>交易开始!"; break;
                case "trade-completed": msg = "<green>交易完成!"; break;
                case "trade-cancelled": msg = "<red>交易已取消!"; break;
                case "already-trading": msg = "<red>你已经在交易中!"; break;
                case "target-trading": msg = "<red>该玩家正在交易中!"; break;
                case "player-confirmed": msg = "<yellow>%player% <green>已确认交易!"; break;
                case "you-confirmed": msg = "<green>你已确认交易!"; break;
                case "countdown": msg = "<yellow>交易将在 <red>%time% <yellow>秒后完成!"; break;
                case "cooldown": msg = "<red>请等待 %time% 秒后再发起交易请求!"; break;
                default: msg = ""; break;
            }
        }
        for (int i = 0; i < replacements.length - 1; i += 2) {
            msg = msg.replace("%" + replacements[i] + "%", replacements[i + 1]);
        }
        return miniMessage.parse(msg);
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onPlayerInteractEntity(PlayerInteractEntityEvent event) {
        if (event.getHand() != EquipmentSlot.HAND) return;
        if (!(event.getRightClicked() instanceof Player)) return;
        
        Player player = event.getPlayer();
        Player target = (Player) event.getRightClicked();
        
        if (!player.isSneaking()) return;
        
        if (player.getUniqueId().equals(target.getUniqueId())) return;
        
        if (!player.hasPermission("guangdian.trade.use")) {
            player.sendMessage(getPrefix().append(getMessage("no-permission")));
            return;
        }

        if (!target.hasPermission("guangdian.trade.use")) {
            player.sendMessage(getPrefix().append(miniMessage.red("该玩家无法进行交易!")));
            return;
        }

        if (isInTrade(player)) {
            player.sendMessage(getPrefix().append(getMessage("already-trading")));
            return;
        }

        if (isInTrade(target)) {
            player.sendMessage(getPrefix().append(getMessage("target-trading")));
            return;
        }

        long now = System.currentTimeMillis() / 1000;
        Long lastRequest = requestCooldowns.get(player.getUniqueId());
        if (lastRequest != null && (now - lastRequest) < requestCooldownTime) {
            int remaining = (int) (requestCooldownTime - (now - lastRequest));
            player.sendMessage(getPrefix().append(getMessage("cooldown", "time", String.valueOf(remaining))));
            return;
        }

        TradeRequest existing = pendingRequests.get(player.getUniqueId());
        if (existing != null && existing.getTarget().equals(target.getUniqueId())) {
            player.sendMessage(getPrefix().append(miniMessage.red("你已经向该玩家发送了交易请求!")));
            return;
        }
        
        TradeRequest reverseRequest = findRequestFrom(target, player);
        if (reverseRequest != null) {
            pendingRequests.remove(reverseRequest.getRequester());
            cn.guangdian.rpgcore.RPGCore rpgCore = cn.guangdian.rpgcore.RPGCore.getInstance();
            if (rpgCore != null) {
                rpgCore.getScheduler().cancelTask(reverseRequest.getTaskId());
            }
            startTrade(player, target);
        } else {
            sendTradeRequest(player, target);
        }
        
        event.setCancelled(true);
    }

    private TradeRequest findRequestFrom(Player requester, Player target) {
        TradeRequest request = pendingRequests.get(requester.getUniqueId());
        if (request != null && request.getTarget().equals(target.getUniqueId())) {
            return request;
        }
        return null;
    }

    private void sendTradeRequest(Player sender, Player target) {
        cn.guangdian.rpgcore.RPGCore rpgCore = cn.guangdian.rpgcore.RPGCore.getInstance();
        long taskId = -1;
        if (rpgCore != null) {
            taskId = rpgCore.getScheduler().runSyncLater(() -> {
                TradeRequest req = pendingRequests.remove(sender.getUniqueId());
                if (req != null) {
                    sender.sendMessage(getPrefix().append(getMessage("request-timeout")));
                    if (target.isOnline()) {
                        target.sendMessage(getPrefix().append(getMessage("request-timeout")));
                    }
                }
            }, requestTimeout * 20L);
        }

        TradeRequest request = new TradeRequest(sender.getUniqueId(), target.getUniqueId(), taskId);
        pendingRequests.put(sender.getUniqueId(), request);
        requestCooldowns.put(sender.getUniqueId(), System.currentTimeMillis() / 1000);

        sender.sendMessage(getPrefix().append(getMessage("request-sent", "player", target.getName())));
        sender.sendMessage(getPrefix().append(miniMessage.gray("蹲下右键对方可接受交易")));

        target.sendMessage(getPrefix().append(getMessage("request-received", "player", sender.getName())));
        target.sendMessage(getPrefix().append(miniMessage.gray("蹲下右键对方接受交易")));
        target.sendMessage(getPrefix().append(miniMessage.gray("或等待 ")).append(miniMessage.red(String.valueOf(requestTimeout))).append(miniMessage.gray(" 秒自动拒绝")));
        
        target.playSound(target.getLocation(), Sound.BLOCK_NOTE_BLOCK_PLING, 1.0f, 1.0f);
    }

    private void startTrade(Player player1, Player player2) {
        synchronized (tradeLock) {
            if (isInTrade(player1) || isInTrade(player2)) {
                player1.sendMessage(getPrefix().append(miniMessage.red("交易失败，对方正在交易中!")));
                player2.sendMessage(getPrefix().append(miniMessage.red("交易失败，对方正在交易中!")));
                return;
            }

            TradeSession session = new TradeSession(player1.getUniqueId(), player2.getUniqueId());
            activeTrades.put(player1.getUniqueId(), session);
            activeTrades.put(player2.getUniqueId(), session);
        }

        TradeSession session = getTradeSession(player1);
        initializeTradeGUI(session);

        player1.openInventory(session.getInventory());
        player2.openInventory(session.getInventory());

        player1.sendMessage(getPrefix().append(getMessage("trade-started")));
        player2.sendMessage(getPrefix().append(getMessage("trade-started")));
        
        player1.playSound(player1.getLocation(), Sound.BLOCK_CHEST_OPEN, 1.0f, 1.0f);
        player2.playSound(player2.getLocation(), Sound.BLOCK_CHEST_OPEN, 1.0f, 1.0f);
        
        // 启动呼吸灯动画
        startBreathingAnimation(session);
    }
    
    /**
     * 启动呼吸灯动画
     */
    private void startBreathingAnimation(TradeSession session) {
        cn.guangdian.rpgcore.RPGCore rpgCore = cn.guangdian.rpgcore.RPGCore.getInstance();
        if (rpgCore == null) return;
        
        long taskId = rpgCore.getScheduler().runSyncRepeating(() -> {
            if (session.isClosing()) {
                stopBreathingAnimation(session);
                return;
            }
            
            Player p1 = session.getPlayer1();
            Player p2 = session.getPlayer2();
            if (p1 == null || p2 == null || !p1.isOnline() || !p2.isOnline()) {
                stopBreathingAnimation(session);
                return;
            }
            
            Inventory inv = session.getInventory();
            
            if (session.isBothConfirmed()) {
                ItemStack divider = createGlass(Material.GREEN_STAINED_GLASS_PANE, miniMessageToLegacy("<green>交易即将完成"));
                for (int i = 4; i < 54; i += 9) {
                    inv.setItem(i, divider);
                }
                return;
            }

            Material currentColor = session.getCurrentBreathingColor();
            String displayName;

            // 根据确认状态选择显示名称
            boolean p1Confirmed = session.isConfirmed(session.getPlayer1());
            boolean p2Confirmed = session.isConfirmed(session.getPlayer2());

            if (p1Confirmed || p2Confirmed) {
                displayName = miniMessageToLegacy("<yellow>等待对方确认...");
            } else {
                displayName = miniMessageToLegacy("<gray>交易进行中...");
            }

            ItemStack divider = createGlass(currentColor, displayName);
            for (int i = 4; i < 54; i += 9) {
                inv.setItem(i, divider);
            }
            
            session.nextBreathingFrame();
        }, 0L, 10L);
        
        session.setBreathingAnimationTask(taskId);
    }
    
    /**
     * 停止呼吸灯动画
     */
    private void stopBreathingAnimation(TradeSession session) {
        long taskId = session.getBreathingAnimationTaskId();
        if (taskId != -1) {
            cn.guangdian.rpgcore.RPGCore rpgCore = cn.guangdian.rpgcore.RPGCore.getInstance();
            if (rpgCore != null) {
                rpgCore.getScheduler().cancelTask(taskId);
            }
            session.setBreathingAnimationTask(-1);
        }
    }

    private void initializeTradeGUI(TradeSession session) {
        Inventory inv = session.getInventory();
        
        ItemStack filler = createGlass(Material.GRAY_STAINED_GLASS_PANE, " ");
        for (int i = 0; i < 54; i++) {
            inv.setItem(i, filler);
        }
        
        int[] player1Slots = {0, 1, 2, 3, 9, 10, 11, 12, 18, 19, 20, 21, 27, 28, 29, 30};
        int[] player2Slots = {5, 6, 7, 8, 14, 15, 16, 17, 23, 24, 25, 26, 32, 33, 34, 35};
        
        for (int slot : player1Slots) {
            inv.setItem(slot, null);
        }
        for (int slot : player2Slots) {
            inv.setItem(slot, null);
        }
        
        ItemStack divider = createGlass(Material.BLACK_STAINED_GLASS_PANE, " ");
        for (int i = 4; i < 54; i += 9) {
            inv.setItem(i, divider);
        }

        ItemStack confirmBtn = createItem(Material.LIME_WOOL, "<green><bold>确认交易",
            Arrays.asList("<gray>点击确认交易", "<gray>双方确认后 " + confirmCountdown + " 秒完成"));
        inv.setItem(48, confirmBtn);

        ItemStack cancelBtn = createItem(Material.RED_WOOL, "<red><bold>取消交易",
            Arrays.asList("<gray>点击取消交易"));
        inv.setItem(50, cancelBtn);

        ItemStack statusBtn = createItem(Material.ORANGE_WOOL, "<yellow>交易状态",
            Arrays.asList("<gray>等待双方放入物品", "<gray>并点击确认按钮"));
        inv.setItem(49, statusBtn);
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player)) return;
        Player player = (Player) event.getWhoClicked();

        // Paper 1.21.4: 使用 title() 替代已弃用的 getTitle()
        net.kyori.adventure.text.Component titleComponent = event.getView().title();
        String title = net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer.legacySection().serialize(titleComponent);
        if (!title.contains("交易界面")) return;
        
        TradeSession session = getTradeSession(player);
        if (session == null) {
            event.setCancelled(true);
            return;
        }
        
        int slot = event.getRawSlot();
        Inventory inv = event.getInventory();
        
        if (event.isShiftClick() && slot >= 54) {
            event.setCancelled(true);
            return;
        }
        
        if (slot == 48) {
            event.setCancelled(true);
            handleConfirm(player, session);
            return;
        }
        
        if (slot == 50) {
            event.setCancelled(true);
            cancelTrade(session);
            return;
        }
        
        if (slot >= 0 && slot < 54) {
            int[] player1Slots = {0, 1, 2, 3, 9, 10, 11, 12, 18, 19, 20, 21, 27, 28, 29, 30};
            int[] player2Slots = {5, 6, 7, 8, 14, 15, 16, 17, 23, 24, 25, 26, 32, 33, 34, 35};
            
            boolean isPlayer1 = session.getPlayer1().equals(player);
            boolean isValidSlot = false;
            
            int[] mySlots = isPlayer1 ? player1Slots : player2Slots;
            for (int s : mySlots) {
                if (s == slot) {
                    isValidSlot = true;
                    break;
                }
            }
            
            if (!isValidSlot) {
                event.setCancelled(true);
            } else {
                if (session.isConfirmed(player)) {
                    session.setConfirmed(player, false);
                    updateStatus(session);
                }
            }
        }
    }

    private void handleConfirm(Player player, TradeSession session) {
        if (session.isCountingDown()) {
            player.sendMessage(getPrefix().append(miniMessage.red("交易正在进行中!")));
            return;
        }

        session.setConfirmed(player, true);
        updateStatus(session);

        Player other = session.getPlayer1().equals(player) ? session.getPlayer2() : session.getPlayer1();
        other.sendMessage(getPrefix().append(getMessage("player-confirmed", "player", player.getName())));
        other.playSound(other.getLocation(), Sound.BLOCK_NOTE_BLOCK_PLING, 1.0f, 1.2f);

        player.sendMessage(getPrefix().append(getMessage("you-confirmed")));
        player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_PLING, 1.0f, 1.0f);

        if (session.isBothConfirmed()) {
            startCountdown(session);
        }
    }

    private void startCountdown(TradeSession session) {
        session.setCountingDown(true);
        session.setCountdown(confirmCountdown);
        
        updateStatus(session);
        
        cn.guangdian.rpgcore.RPGCore rpgCore = cn.guangdian.rpgcore.RPGCore.getInstance();
        if (rpgCore == null) return;
        
        long taskId = rpgCore.getScheduler().runSyncRepeating(() -> {
            int countdown = session.getCountdown();
            
            if (countdown <= 0) {
                stopBreathingAnimation(session);
                completeTrade(session);
            } else {
                Component msg = getMessage("countdown", "time", String.valueOf(countdown));
                session.getPlayer1().sendMessage(getPrefix().append(msg));
                session.getPlayer2().sendMessage(getPrefix().append(msg));

                session.getPlayer1().playSound(session.getPlayer1().getLocation(), Sound.BLOCK_NOTE_BLOCK_HAT, 1.0f, 1.0f);
                session.getPlayer2().playSound(session.getPlayer2().getLocation(), Sound.BLOCK_NOTE_BLOCK_HAT, 1.0f, 1.0f);

                session.setCountdown(countdown - 1);
                updateStatus(session);
            }
        }, 20L, 20L);
        
        session.setCountdownTask(taskId);
    }

    private void updateStatus(TradeSession session) {
        Inventory inv = session.getInventory();

        boolean p1Confirmed = session.isConfirmed(session.getPlayer1());
        boolean p2Confirmed = session.isConfirmed(session.getPlayer2());

        Material statusColor = session.isBothConfirmed() ? Material.GREEN_WOOL :
            (p1Confirmed || p2Confirmed ? Material.YELLOW_WOOL : Material.ORANGE_WOOL);

        List<String> lore = new ArrayList<>();
        lore.add("<gray>" + session.getPlayer1().getName() + ": " + (p1Confirmed ? "<green>已确认" : "<red>未确认"));
        lore.add("<gray>" + session.getPlayer2().getName() + ": " + (p2Confirmed ? "<green>已确认" : "<red>未确认"));

        if (session.isCountingDown()) {
            lore.add("");
            lore.add("<yellow>交易倒计时: <red>" + session.getCountdown() + " <yellow>秒");
        } else if (session.isBothConfirmed()) {
            lore.add("");
            lore.add("<green>双方已确认!");
        }

        ItemStack statusBtn = createItem(statusColor, "<yellow>交易状态", lore);
        inv.setItem(49, statusBtn);

        Material dividerColor = session.isBothConfirmed() ? Material.GREEN_STAINED_GLASS_PANE : Material.BLACK_STAINED_GLASS_PANE;
        ItemStack divider = createGlass(dividerColor, session.isBothConfirmed() ? miniMessageToLegacy("<green>交易即将完成") : " ");
        for (int i = 4; i < 54; i += 9) {
            inv.setItem(i, divider);
        }
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onInventoryClose(InventoryCloseEvent event) {
        if (!(event.getPlayer() instanceof Player)) return;
        Player player = (Player) event.getPlayer();

        // Paper 1.21.4: 使用 title() 替代已弃用的 getTitle()
        net.kyori.adventure.text.Component titleComponent = event.getView().title();
        String title = net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer.legacySection().serialize(titleComponent);
        if (!title.contains("交易界面")) return;
        
        TradeSession session = getTradeSession(player);
        if (session != null && activeTrades.containsKey(player.getUniqueId()) && !session.isClosing()) {
            cn.guangdian.rpgcore.RPGCore rpgCore = cn.guangdian.rpgcore.RPGCore.getInstance();
            if (rpgCore != null) {
                rpgCore.getScheduler().runSyncLater(() -> {
                    if (activeTrades.containsKey(player.getUniqueId()) && !session.isClosing()) {
                        cancelTrade(session);
                    }
                }, 1L);
            }
        }
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onPlayerQuit(PlayerQuitEvent event) {
        Player player = event.getPlayer();
        UUID playerId = player.getUniqueId();
        
        TradeRequest request = pendingRequests.remove(playerId);
        if (request != null) {
            cn.guangdian.rpgcore.RPGCore rpgCore = cn.guangdian.rpgcore.RPGCore.getInstance();
            if (rpgCore != null) {
                rpgCore.getScheduler().cancelTask(request.getTaskId());
            }
        }
        
        pendingRequests.entrySet().removeIf(entry -> {
            if (entry.getValue().getTarget().equals(playerId)) {
                cn.guangdian.rpgcore.RPGCore rpgCore = cn.guangdian.rpgcore.RPGCore.getInstance();
                if (rpgCore != null) {
                    rpgCore.getScheduler().cancelTask(entry.getValue().getTaskId());
                }
                return true;
            }
            return false;
        });
        
        requestCooldowns.remove(playerId);
        
        TradeSession session = getTradeSession(player);
        if (session != null) {
            cancelTrade(session);
        }
    }

    private void completeTrade(TradeSession session) {
        if (session.isClosing()) {
            return;
        }
        session.setClosing(true);
        
        // 停止呼吸灯动画
        stopBreathingAnimation(session);

        UUID player1Id = session.getPlayer1Id();
        UUID player2Id = session.getPlayer2Id();
        
        Player player1 = Bukkit.getPlayer(player1Id);
        Player player2 = Bukkit.getPlayer(player2Id);
        
        if (player1 == null || player2 == null) {
            logWarning("交易完成时玩家不在线，取消交易");
            cancelTrade(session);
            return;
        }
        
        int[] player1Slots = {0, 1, 2, 3, 9, 10, 11, 12, 18, 19, 20, 21, 27, 28, 29, 30};
        int[] player2Slots = {5, 6, 7, 8, 14, 15, 16, 17, 23, 24, 25, 26, 32, 33, 34, 35};
        
        Inventory inv = session.getInventory();
        
        List<ItemStack> itemsToPlayer1 = new ArrayList<>();
        List<ItemStack> itemsToPlayer2 = new ArrayList<>();
        
        for (int slot : player1Slots) {
            ItemStack item = inv.getItem(slot);
            if (item != null && item.getType() != Material.AIR) {
                itemsToPlayer2.add(item.clone());
            }
        }
        
        for (int slot : player2Slots) {
            ItemStack item = inv.getItem(slot);
            if (item != null && item.getType() != Material.AIR) {
                itemsToPlayer1.add(item.clone());
            }
        }
        
        activeTrades.remove(player1Id);
        activeTrades.remove(player2Id);
        
        player1.closeInventory();
        player2.closeInventory();
        
        try {
            for (ItemStack item : itemsToPlayer1) {
                HashMap<Integer, ItemStack> leftover = player1.getInventory().addItem(item);
                for (ItemStack drop : leftover.values()) {
                    player1.getWorld().dropItemNaturally(player1.getLocation(), drop);
                }
            }
            
            for (ItemStack item : itemsToPlayer2) {
                HashMap<Integer, ItemStack> leftover = player2.getInventory().addItem(item);
                for (ItemStack drop : leftover.values()) {
                    player2.getWorld().dropItemNaturally(player2.getLocation(), drop);
                }
            }
        } catch (Exception e) {
            logSevere("交易完成时发生错误: " + e.getMessage());
            getLogger().log(java.util.logging.Level.SEVERE, "详细异常信息", e);
        }
        
        player1.sendMessage(getPrefix().append(getMessage("trade-completed")));
        player2.sendMessage(getPrefix().append(getMessage("trade-completed")));

        player1.playSound(player1.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 1.0f, 1.0f);
        player2.playSound(player2.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 1.0f, 1.0f);
    }

    private void cancelTrade(TradeSession session) {
        if (session.isClosing()) {
            return;
        }
        session.setClosing(true);

        stopBreathingAnimation(session);
        
        long countdownTaskId = session.getCountdownTaskId();
        if (countdownTaskId != -1) {
            cn.guangdian.rpgcore.RPGCore rpgCore = cn.guangdian.rpgcore.RPGCore.getInstance();
            if (rpgCore != null) {
                rpgCore.getScheduler().cancelTask(countdownTaskId);
            }
        }
        
        UUID player1Id = session.getPlayer1Id();
        UUID player2Id = session.getPlayer2Id();
        
        Player player1 = Bukkit.getPlayer(player1Id);
        Player player2 = Bukkit.getPlayer(player2Id);
        
        int[] player1Slots = {0, 1, 2, 3, 9, 10, 11, 12, 18, 19, 20, 21, 27, 28, 29, 30};
        int[] player2Slots = {5, 6, 7, 8, 14, 15, 16, 17, 23, 24, 25, 26, 32, 33, 34, 35};
        
        Inventory inv = session.getInventory();
        
        try {
            for (int slot : player1Slots) {
                ItemStack item = inv.getItem(slot);
                if (item != null && item.getType() != Material.AIR && player1 != null && player1.isOnline()) {
                    HashMap<Integer, ItemStack> leftover = player1.getInventory().addItem(item.clone());
                    for (ItemStack drop : leftover.values()) {
                        player1.getWorld().dropItemNaturally(player1.getLocation(), drop);
                    }
                }
            }
            
            for (int slot : player2Slots) {
                ItemStack item = inv.getItem(slot);
                if (item != null && item.getType() != Material.AIR && player2 != null && player2.isOnline()) {
                    HashMap<Integer, ItemStack> leftover = player2.getInventory().addItem(item.clone());
                    for (ItemStack drop : leftover.values()) {
                        player2.getWorld().dropItemNaturally(player2.getLocation(), drop);
                    }
                }
            }
        } catch (Exception e) {
            logSevere("取消交易时发生错误: " + e.getMessage());
            getLogger().log(java.util.logging.Level.SEVERE, "详细异常信息", e);
        }
        
        activeTrades.remove(player1Id);
        activeTrades.remove(player2Id);

        if (player1 != null && player1.isOnline()) player1.closeInventory();
        if (player2 != null && player2.isOnline()) player2.closeInventory();

        if (player1 != null && player1.isOnline()) player1.sendMessage(getPrefix().append(getMessage("trade-cancelled")));
        if (player2 != null && player2.isOnline()) player2.sendMessage(getPrefix().append(getMessage("trade-cancelled")));
    }

    private boolean isInTrade(Player player) {
        return activeTrades.containsKey(player.getUniqueId());
    }

    private TradeSession getTradeSession(Player player) {
        return activeTrades.get(player.getUniqueId());
    }

    private ItemStack createGlass(Material material, String name) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(name);
            item.setItemMeta(meta);
        }
        return item;
    }

    private ItemStack createItem(Material material, String name, List<String> lore) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            // 将 MiniMessage 格式转换为 legacy 格式
            String legacyName = miniMessageToLegacy(name);
            meta.setDisplayName(legacyName);
            if (lore != null) {
                List<String> legacyLore = lore.stream()
                    .map(this::miniMessageToLegacy)
                    .collect(java.util.stream.Collectors.toList());
                meta.setLore(legacyLore);
            }
            item.setItemMeta(meta);
        }
        return item;
    }

    /**
     * 将 MiniMessage 格式转换为 legacy 格式（用于 ItemMeta）
     */
    private String miniMessageToLegacy(String text) {
        if (text == null || text.isEmpty()) return "";
        try {
            Component component = miniMessage.parse(text);
            return net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer.legacySection().serialize(component);
        } catch (Exception e) {
            return text;
        }
    }

    @Override
    public boolean onCommand(org.bukkit.command.CommandSender sender, org.bukkit.command.Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage(miniMessage.red("只有玩家可以使用此命令!"));
            return true;
        }
        
        Player player = (Player) sender;
        
        if (args.length == 1 && args[0].equalsIgnoreCase("cancel")) {
            TradeSession session = getTradeSession(player);
            if (session != null) {
                cancelTrade(session);
            } else {
                player.sendMessage(getPrefix().append(miniMessage.red("你没有正在进行的交易!")));
            }
            return true;
        }

        player.sendMessage(miniMessage.gold("===== 光点交易系统 ====="));
        player.sendMessage(miniMessage.yellow("蹲下 + 右键玩家 ").append(miniMessage.gray("- 发送交易请求")));
        player.sendMessage(miniMessage.yellow("对方蹲下 + 右键你 ").append(miniMessage.gray("- 接受交易请求")));
        player.sendMessage(miniMessage.yellow("点击确认按钮 ").append(miniMessage.gray("- 确认交易")));
        player.sendMessage(miniMessage.yellow("/trade cancel ").append(miniMessage.gray("- 取消交易")));
        player.sendMessage(miniMessage.gray("双方确认后等待 " + confirmCountdown + " 秒完成交易"));
        return true;
    }

    public static GuangDianTrade getInstance() {
        return instance;
    }

    // ==================== 公开API方法（供RPGCore服务调用） ====================

    /**
     * 检查玩家是否在交易中 - API方法
     * 
     * @param playerId 玩家UUID
     * @return 如果在交易中返回 true
     */
    public boolean isInTradeAPI(UUID playerId) {
        return activeTrades.containsKey(playerId);
    }

    /**
     * 获取交易伙伴UUID - API方法
     * 
     * @param playerId 玩家UUID
     * @return 交易伙伴UUID，如果不在交易中返回 null
     */
    public UUID getTradePartnerAPI(UUID playerId) {
        TradeSession session = activeTrades.get(playerId);
        if (session == null) return null;
        return playerId.equals(session.getPlayer1Id()) ? session.getPlayer2Id() : session.getPlayer1Id();
    }

    /**
     * 获取交易会话 - API方法
     * 
     * @param playerId 玩家UUID
     * @return 交易会话对象，如果不在交易中返回 null
     */
    public Object getTradeSessionAPI(UUID playerId) {
        return activeTrades.get(playerId);
    }

    /**
     * 取消交易 - API方法
     * 
     * @param playerId 玩家UUID
     * @return 如果成功取消返回 true
     */
    public boolean cancelTradeAPI(UUID playerId) {
        TradeSession session = activeTrades.get(playerId);
        if (session == null) return false;
        cancelTrade(session);
        return true;
    }

    /**
     * 获取活跃交易数量
     *
     * @return 当前活跃交易数量
     */
    public int getActiveTradeCountAPI() {
        return activeTrades.size() / 2; // 每个交易有两个玩家
    }

    /**
     * 获取玩家待处理的交易请求数量 - API方法
     *
     * @param playerId 玩家UUID
     * @return 待处理的交易请求数量
     */
    public int getPendingRequestCountAPI(UUID playerId) {
        int count = 0;
        for (TradeRequest request : pendingRequests.values()) {
            if (request.getTarget().equals(playerId)) {
                count++;
            }
        }
        return count;
    }

    // ==================== 内部方法 ====================

    private static class TradeRequest {
        private final UUID requester;
        private final UUID target;
        private final long taskId;

        public TradeRequest(UUID requester, UUID target, long taskId) {
            this.requester = requester;
            this.target = target;
            this.taskId = taskId;
        }

        public UUID getRequester() { return requester; }
        public UUID getTarget() { return target; }
        public long getTaskId() { return taskId; }
    }

    private static class TradeSession {
        private final UUID player1Id;
        private final UUID player2Id;
        private final Inventory inventory;
        
        private boolean player1Confirmed = false;
        private boolean player2Confirmed = false;
        private boolean countingDown = false;
        private boolean closing = false;
        private int countdown = 0;
        private long countdownTaskId = -1;
        
        private long breathingAnimationTaskId = -1;
        private int breathingFrame = 0;
        private static final Material[] BREATHING_COLORS = {
            Material.BLACK_STAINED_GLASS_PANE,
            Material.GRAY_STAINED_GLASS_PANE,
            Material.LIGHT_GRAY_STAINED_GLASS_PANE,
            Material.WHITE_STAINED_GLASS_PANE,
            Material.YELLOW_STAINED_GLASS_PANE,
            Material.WHITE_STAINED_GLASS_PANE,
            Material.LIGHT_GRAY_STAINED_GLASS_PANE,
            Material.GRAY_STAINED_GLASS_PANE
        };

        public TradeSession(UUID player1Id, UUID player2Id) {
            this.player1Id = player1Id;
            this.player2Id = player2Id;
            this.inventory = Bukkit.createInventory(null, 54, net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer.legacySection().serialize(
                net.kyori.adventure.text.Component.text("交易界面").color(net.kyori.adventure.text.format.NamedTextColor.GOLD).decorate(net.kyori.adventure.text.format.TextDecoration.BOLD)
            ));
        }

        public UUID getPlayer1Id() { return player1Id; }
        public UUID getPlayer2Id() { return player2Id; }
        public Player getPlayer1() { return Bukkit.getPlayer(player1Id); }
        public Player getPlayer2() { return Bukkit.getPlayer(player2Id); }
        public Inventory getInventory() { return inventory; }

        public void setConfirmed(Player player, boolean confirmed) {
            if (player.getUniqueId().equals(player1Id)) {
                player1Confirmed = confirmed;
            } else if (player.getUniqueId().equals(player2Id)) {
                player2Confirmed = confirmed;
            }
        }

        public boolean isConfirmed(Player player) {
            if (player.getUniqueId().equals(player1Id)) return player1Confirmed;
            if (player.getUniqueId().equals(player2Id)) return player2Confirmed;
            return false;
        }

        public boolean isBothConfirmed() {
            return player1Confirmed && player2Confirmed;
        }
        
        public boolean isCountingDown() { return countingDown; }
        public void setCountingDown(boolean countingDown) { this.countingDown = countingDown; }

        public boolean isClosing() { return closing; }
        public void setClosing(boolean closing) { this.closing = closing; }
        
        public int getCountdown() { return countdown; }
        public void setCountdown(int countdown) { this.countdown = countdown; }
        
        public long getCountdownTaskId() { return countdownTaskId; }
        public void setCountdownTask(long taskId) { this.countdownTaskId = taskId; }
        
        public long getBreathingAnimationTaskId() { return breathingAnimationTaskId; }
        public void setBreathingAnimationTask(long taskId) { this.breathingAnimationTaskId = taskId; }
        public int getBreathingFrame() { return breathingFrame; }
        public void setBreathingFrame(int frame) { this.breathingFrame = frame; }
        public Material getCurrentBreathingColor() {
            return BREATHING_COLORS[breathingFrame % BREATHING_COLORS.length];
        }
        public void nextBreathingFrame() {
            breathingFrame = (breathingFrame + 1) % BREATHING_COLORS.length;
        }
        public static Material[] getBreathingColors() { return BREATHING_COLORS; }
    }
}
