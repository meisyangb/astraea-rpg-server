package cn.guangdian.trade;

import cn.guangdian.trade.adapter.TradeServiceAdapter;
import cn.guangdian.trade.placeholder.TradePlaceholder;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
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
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitTask;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class GuangDianTrade extends JavaPlugin implements Listener {

    private static GuangDianTrade instance;
    
    private final Map<UUID, TradeSession> activeTrades = new ConcurrentHashMap<>();
    private final Map<UUID, TradeRequest> pendingRequests = new ConcurrentHashMap<>();
    private final Map<UUID, Long> requestCooldowns = new ConcurrentHashMap<>();
    private final Object tradeLock = new Object();
    
    private int requestTimeout = 30;
    private int confirmCountdown = 3;
    private int requestCooldownTime = 5;
    private String prefix = "§6[§e交易§6] ";
    private TradeServiceAdapter serviceAdapter;
    
    @Override
    public void onEnable() {
        instance = this;
        saveDefaultConfig();
        loadConfig();
        getServer().getPluginManager().registerEvents(this, this);
        getCommand("trade").setExecutor(this);
        // 注册RPGCore服务适配器
        serviceAdapter = new TradeServiceAdapter(this);

        // 注册PlaceholderAPI扩展
        if (Bukkit.getPluginManager().isPluginEnabled("PlaceholderAPI")) {
            new TradePlaceholder(this).register();
            getLogger().info("已注册PlaceholderAPI扩展!");
        }

        getLogger().info("光点交易插件已启用! 版本: " + getDescription().getVersion());
        getLogger().info("作者: Gumin | QQ: 2271257344");
    }

    @Override
    public void onDisable() {
        for (TradeSession session : new ArrayList<>(activeTrades.values())) {
            cancelTrade(session);
        }
        for (TradeRequest request : new ArrayList<>(pendingRequests.values())) {
            request.getTask().cancel();
        }
        activeTrades.clear();
        pendingRequests.clear();
        requestCooldowns.clear();
        // 注销RPGCore服务适配器
        if (serviceAdapter != null) {
            serviceAdapter.unregister();
            serviceAdapter = null;
        }
        getLogger().info("光点交易插件已禁用!");
    }

    private void loadConfig() {
        requestTimeout = Math.max(10, Math.min(300, getConfig().getInt("settings.request-timeout", 30)));
        confirmCountdown = Math.max(1, Math.min(60, getConfig().getInt("settings.confirm-countdown", 3)));
        requestCooldownTime = Math.max(0, Math.min(60, getConfig().getInt("settings.request-cooldown", 5)));
        prefix = ChatColor.translateAlternateColorCodes('&', getConfig().getString("settings.prefix", "§6[§e交易§6] "));
    }

    private String getMessage(String key, String... replacements) {
        String msg = getConfig().getString("messages." + key, "");
        if (msg.isEmpty()) {
            switch (key) {
                case "no-permission": msg = "&c你没有权限使用交易功能!"; break;
                case "request-sent": msg = "&a已向 &e%player% &a发送交易请求!"; break;
                case "request-received": msg = "&e%player% &a想与你交易!"; break;
                case "request-timeout": msg = "&c交易请求已超时!"; break;
                case "request-denied": msg = "&c已拒绝交易请求!"; break;
                case "trade-started": msg = "&a交易开始!"; break;
                case "trade-completed": msg = "&a交易完成!"; break;
                case "trade-cancelled": msg = "&c交易已取消!"; break;
                case "already-trading": msg = "&c你已经在交易中!"; break;
                case "target-trading": msg = "&c该玩家正在交易中!"; break;
                case "player-confirmed": msg = "&e%player% &a已确认交易!"; break;
                case "you-confirmed": msg = "&a你已确认交易!"; break;
                case "countdown": msg = "&e交易将在 &c%time% &e秒后完成!"; break;
                case "cooldown": msg = "&c请等待 %time% 秒后再发起交易请求!"; break;
                default: msg = ""; break;
            }
        }
        for (int i = 0; i < replacements.length - 1; i += 2) {
            msg = msg.replace("%" + replacements[i] + "%", replacements[i + 1]);
        }
        return ChatColor.translateAlternateColorCodes('&', msg);
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
            player.sendMessage(prefix + getMessage("no-permission"));
            return;
        }
        
        if (!target.hasPermission("guangdian.trade.use")) {
            player.sendMessage(prefix + "§c该玩家无法进行交易!");
            return;
        }
        
        if (isInTrade(player)) {
            player.sendMessage(prefix + getMessage("already-trading"));
            return;
        }
        
        if (isInTrade(target)) {
            player.sendMessage(prefix + getMessage("target-trading"));
            return;
        }
        
        long now = System.currentTimeMillis() / 1000;
        Long lastRequest = requestCooldowns.get(player.getUniqueId());
        if (lastRequest != null && (now - lastRequest) < requestCooldownTime) {
            int remaining = (int) (requestCooldownTime - (now - lastRequest));
            player.sendMessage(prefix + getMessage("cooldown", "time", String.valueOf(remaining)));
            return;
        }
        
        TradeRequest existing = pendingRequests.get(player.getUniqueId());
        if (existing != null && existing.getTarget().equals(target.getUniqueId())) {
            player.sendMessage(prefix + "§c你已经向该玩家发送了交易请求!");
            return;
        }
        
        TradeRequest reverseRequest = findRequestFrom(target, player);
        if (reverseRequest != null) {
            pendingRequests.remove(reverseRequest.getRequester());
            reverseRequest.getTask().cancel();
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
        BukkitTask task = Bukkit.getScheduler().runTaskLater(this, () -> {
            TradeRequest req = pendingRequests.remove(sender.getUniqueId());
            if (req != null) {
                sender.sendMessage(prefix + getMessage("request-timeout"));
                if (target.isOnline()) {
                    target.sendMessage(prefix + getMessage("request-timeout"));
                }
            }
        }, requestTimeout * 20L);
        
        TradeRequest request = new TradeRequest(sender.getUniqueId(), target.getUniqueId(), task);
        pendingRequests.put(sender.getUniqueId(), request);
        requestCooldowns.put(sender.getUniqueId(), System.currentTimeMillis() / 1000);
        
        sender.sendMessage(prefix + getMessage("request-sent", "player", target.getName()));
        sender.sendMessage(prefix + "§7蹲下右键对方可接受交易");
        
        target.sendMessage(prefix + getMessage("request-received", "player", sender.getName()));
        target.sendMessage(prefix + "§7蹲下右键对方接受交易");
        target.sendMessage(prefix + "§7或等待 §c" + requestTimeout + " §7秒自动拒绝");
        
        target.playSound(target.getLocation(), Sound.BLOCK_NOTE_BLOCK_PLING, 1.0f, 1.0f);
    }

    private void startTrade(Player player1, Player player2) {
        synchronized (tradeLock) {
            if (isInTrade(player1) || isInTrade(player2)) {
                player1.sendMessage(prefix + "§c交易失败，对方正在交易中!");
                player2.sendMessage(prefix + "§c交易失败，对方正在交易中!");
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
        
        player1.sendMessage(prefix + getMessage("trade-started"));
        player2.sendMessage(prefix + getMessage("trade-started"));
        
        player1.playSound(player1.getLocation(), Sound.BLOCK_CHEST_OPEN, 1.0f, 1.0f);
        player2.playSound(player2.getLocation(), Sound.BLOCK_CHEST_OPEN, 1.0f, 1.0f);
        
        // 启动呼吸灯动画
        startBreathingAnimation(session);
    }
    
    /**
     * 启动呼吸灯动画
     */
    private void startBreathingAnimation(TradeSession session) {
        BukkitTask task = Bukkit.getScheduler().runTaskTimer(this, () -> {
            if (session.isClosing()) {
                stopBreathingAnimation(session);
                return;
            }
            
            // 检查玩家是否在线
            Player p1 = session.getPlayer1();
            Player p2 = session.getPlayer2();
            if (p1 == null || p2 == null || !p1.isOnline() || !p2.isOnline()) {
                stopBreathingAnimation(session);
                return;
            }
            
            Inventory inv = session.getInventory();
            
            // 如果双方都已确认，停止呼吸灯，显示绿色静态
            if (session.isBothConfirmed()) {
                ItemStack divider = createGlass(Material.GREEN_STAINED_GLASS_PANE, "§a交易即将完成");
                for (int i = 4; i < 54; i += 9) {
                    inv.setItem(i, divider);
                }
                return;
            }
            
            // 获取当前颜色
            Material currentColor = session.getCurrentBreathingColor();
            String displayName;
            
            // 根据确认状态选择显示名称
            boolean p1Confirmed = session.isConfirmed(session.getPlayer1());
            boolean p2Confirmed = session.isConfirmed(session.getPlayer2());
            
            if (p1Confirmed || p2Confirmed) {
                displayName = "§e等待对方确认...";
            } else {
                displayName = "§7交易进行中...";
            }
            
            ItemStack divider = createGlass(currentColor, displayName);
            for (int i = 4; i < 54; i += 9) {
                inv.setItem(i, divider);
            }
            
            // 切换到下一帧
            session.nextBreathingFrame();
        }, 0L, 10L); // 每0.5秒更新
        
        session.setBreathingAnimationTask(task);
    }
    
    /**
     * 停止呼吸灯动画
     */
    private void stopBreathingAnimation(TradeSession session) {
        BukkitTask task = session.getBreathingAnimationTask();
        if (task != null) {
            task.cancel();
            session.setBreathingAnimationTask(null);
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
        
        ItemStack divider = createGlass(Material.BLACK_STAINED_GLASS_PANE, "§8分隔线");
        for (int i = 4; i < 54; i += 9) {
            inv.setItem(i, divider);
        }
        
        ItemStack confirmBtn = createItem(Material.LIME_WOOL, "§a§l确认交易", 
            Arrays.asList("§7点击确认交易", "§7双方确认后 " + confirmCountdown + " 秒完成"));
        inv.setItem(48, confirmBtn);
        
        ItemStack cancelBtn = createItem(Material.RED_WOOL, "§c§l取消交易", 
            Arrays.asList("§7点击取消交易"));
        inv.setItem(50, cancelBtn);
        
        ItemStack statusBtn = createItem(Material.ORANGE_WOOL, "§e交易状态", 
            Arrays.asList("§7等待双方放入物品", "§7并点击确认按钮"));
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
            player.sendMessage(prefix + "§c交易正在进行中!");
            return;
        }
        
        session.setConfirmed(player, true);
        updateStatus(session);
        
        Player other = session.getPlayer1().equals(player) ? session.getPlayer2() : session.getPlayer1();
        other.sendMessage(prefix + getMessage("player-confirmed", "player", player.getName()));
        other.playSound(other.getLocation(), Sound.BLOCK_NOTE_BLOCK_PLING, 1.0f, 1.2f);
        
        player.sendMessage(prefix + getMessage("you-confirmed"));
        player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_PLING, 1.0f, 1.0f);
        
        if (session.isBothConfirmed()) {
            startCountdown(session);
        }
    }

    private void startCountdown(TradeSession session) {
        session.setCountingDown(true);
        session.setCountdown(confirmCountdown);
        
        updateStatus(session);
        
        BukkitTask task = Bukkit.getScheduler().runTaskTimer(this, () -> {
            int countdown = session.getCountdown();
            
            if (countdown <= 0) {
                session.getCountdownTask().cancel();
                completeTrade(session);
            } else {
                String msg = getMessage("countdown", "time", String.valueOf(countdown));
                session.getPlayer1().sendMessage(prefix + msg);
                session.getPlayer2().sendMessage(prefix + msg);
                
                session.getPlayer1().playSound(session.getPlayer1().getLocation(), Sound.BLOCK_NOTE_BLOCK_HAT, 1.0f, 1.0f);
                session.getPlayer2().playSound(session.getPlayer2().getLocation(), Sound.BLOCK_NOTE_BLOCK_HAT, 1.0f, 1.0f);
                
                session.setCountdown(countdown - 1);
                updateStatus(session);
            }
        }, 20L, 20L);
        
        session.setCountdownTask(task);
    }

    private void updateStatus(TradeSession session) {
        Inventory inv = session.getInventory();
        
        boolean p1Confirmed = session.isConfirmed(session.getPlayer1());
        boolean p2Confirmed = session.isConfirmed(session.getPlayer2());
        
        Material statusColor = session.isBothConfirmed() ? Material.GREEN_WOOL : 
            (p1Confirmed || p2Confirmed ? Material.YELLOW_WOOL : Material.ORANGE_WOOL);
        
        List<String> lore = new ArrayList<>();
        lore.add("§7" + session.getPlayer1().getName() + ": " + (p1Confirmed ? "§a已确认" : "§c未确认"));
        lore.add("§7" + session.getPlayer2().getName() + ": " + (p2Confirmed ? "§a已确认" : "§c未确认"));
        
        if (session.isCountingDown()) {
            lore.add("");
            lore.add("§e交易倒计时: §c" + session.getCountdown() + " §e秒");
        } else if (session.isBothConfirmed()) {
            lore.add("");
            lore.add("§a双方已确认!");
        }
        
        ItemStack statusBtn = createItem(statusColor, "§e交易状态", lore);
        inv.setItem(49, statusBtn);
        
        Material dividerColor = session.isBothConfirmed() ? Material.GREEN_STAINED_GLASS_PANE : Material.BLACK_STAINED_GLASS_PANE;
        ItemStack divider = createGlass(dividerColor, session.isBothConfirmed() ? "§a交易即将完成" : "§8分隔线");
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
            Bukkit.getScheduler().runTaskLater(this, () -> {
                if (activeTrades.containsKey(player.getUniqueId()) && !session.isClosing()) {
                    cancelTrade(session);
                }
            }, 1L);
        }
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onPlayerQuit(PlayerQuitEvent event) {
        Player player = event.getPlayer();
        UUID playerId = player.getUniqueId();
        
        TradeRequest request = pendingRequests.remove(playerId);
        if (request != null) {
            request.getTask().cancel();
        }
        
        pendingRequests.entrySet().removeIf(entry -> {
            if (entry.getValue().getTarget().equals(playerId)) {
                entry.getValue().getTask().cancel();
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
            getLogger().warning("交易完成时玩家不在线，取消交易");
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
            getLogger().severe("交易完成时发生错误: " + e.getMessage());
            getLogger().log(java.util.logging.Level.SEVERE, "详细异常信息", e);
        }
        
        player1.sendMessage(prefix + getMessage("trade-completed"));
        player2.sendMessage(prefix + getMessage("trade-completed"));
        
        player1.playSound(player1.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 1.0f, 1.0f);
        player2.playSound(player2.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 1.0f, 1.0f);
    }

    private void cancelTrade(TradeSession session) {
        if (session.isClosing()) {
            return;
        }
        session.setClosing(true);

        // 停止呼吸灯动画
        stopBreathingAnimation(session);
        
        if (session.getCountdownTask() != null) {
            session.getCountdownTask().cancel();
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
            getLogger().severe("取消交易时发生错误: " + e.getMessage());
            getLogger().log(java.util.logging.Level.SEVERE, "详细异常信息", e);
        }
        
        activeTrades.remove(player1Id);
        activeTrades.remove(player2Id);

        if (player1 != null && player1.isOnline()) player1.closeInventory();
        if (player2 != null && player2.isOnline()) player2.closeInventory();
        
        if (player1 != null && player1.isOnline()) player1.sendMessage(prefix + getMessage("trade-cancelled"));
        if (player2 != null && player2.isOnline()) player2.sendMessage(prefix + getMessage("trade-cancelled"));
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
            meta.setDisplayName(name);
            meta.setLore(lore);
            item.setItemMeta(meta);
        }
        return item;
    }

    @Override
    public boolean onCommand(org.bukkit.command.CommandSender sender, org.bukkit.command.Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage("§c只有玩家可以使用此命令!");
            return true;
        }
        
        Player player = (Player) sender;
        
        if (args.length == 1 && args[0].equalsIgnoreCase("cancel")) {
            TradeSession session = getTradeSession(player);
            if (session != null) {
                cancelTrade(session);
            } else {
                player.sendMessage(prefix + "§c你没有正在进行的交易!");
            }
            return true;
        }
        
        player.sendMessage("§6===== 光点交易系统 =====");
        player.sendMessage("§e蹲下 + 右键玩家 §7- 发送交易请求");
        player.sendMessage("§e对方蹲下 + 右键你 §7- 接受交易请求");
        player.sendMessage("§e点击确认按钮 §7- 确认交易");
        player.sendMessage("§e/trade cancel §7- 取消交易");
        player.sendMessage("§7双方确认后等待 " + confirmCountdown + " 秒完成交易");
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
        private final BukkitTask task;

        public TradeRequest(UUID requester, UUID target, BukkitTask task) {
            this.requester = requester;
            this.target = target;
            this.task = task;
        }

        public UUID getRequester() { return requester; }
        public UUID getTarget() { return target; }
        public BukkitTask getTask() { return task; }
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
        private BukkitTask countdownTask = null;
        
        // 呼吸灯动画相关
        private BukkitTask breathingAnimationTask = null;
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
            this.inventory = Bukkit.createInventory(null, 54, "§6§l交易界面");
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
        
        public BukkitTask getCountdownTask() { return countdownTask; }
        public void setCountdownTask(BukkitTask task) { this.countdownTask = task; }
        
        // 呼吸灯动画相关方法
        public BukkitTask getBreathingAnimationTask() { return breathingAnimationTask; }
        public void setBreathingAnimationTask(BukkitTask task) { this.breathingAnimationTask = task; }
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
