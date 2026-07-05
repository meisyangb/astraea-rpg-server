package cn.guangdian.marriage;

import cn.guangdian.marriage.adapter.MarriageServiceAdapter;
import cn.guangdian.marriage.storage.MarriageStorage;
import cn.guangdian.rpgcore.plugin.AbstractRPGPlugin;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.TabCompleter;
import me.clip.placeholderapi.PlaceholderAPI;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

public class GuangDianMarriage extends AbstractRPGPlugin implements Listener, CommandExecutor, TabCompleter {
    private static GuangDianMarriage instance;
    private FileConfiguration config;
    private Map<String, Marriage> marriages;
    private Map<String, ProposeRequest> proposeRequests;
    private Map<String, TpRequest> tpRequests;
    private Map<String, Long> tpCooldowns;
    private MarriageServiceAdapter serviceAdapter;
    private MarriageStorage mStorage;
    private int mSaveId = -1;

    @Override
    protected void onPluginEnable() {
        instance = this;
        marriages = new ConcurrentHashMap<>();
        proposeRequests = new ConcurrentHashMap<>();
        tpRequests = new ConcurrentHashMap<>();
        tpCooldowns = new ConcurrentHashMap<>();
        saveDefaultConfig();
        config = getConfig();
        mStorage = new MarriageStorage(this);
        if (mStorage.init()) { mStorage.load(); for (var m : mStorage.all()) { Map<String,Object> data = new HashMap<>(); data.put("player1",m.p1());data.put("player2",m.p2());data.put("marryDate",m.d());data.put("lovePoints",m.lp());data.put("player1Nickname",m.n1());data.put("player2Nickname",m.n2()); Marriage mar = Marriage.deserialize(data); if (mar != null) { marriages.put(mar.player1.toLowerCase(), mar); marriages.put(mar.player2.toLowerCase(), mar); } } }
        loadMarriages();
        mSaveId = getServer().getScheduler().runTaskTimerAsynchronously(this, () -> { if (mStorage != null) { mStorage.clear(); for (Marriage mar : new HashSet<>(marriages.values())) mStorage.add(mar.player1, mar.player2, mar.marryDate, mar.lovePoints, mar.player1Nickname, mar.player2Nickname); mStorage.saveAsync(); } }, 6000L, 6000L).getTaskId();
        getCommand("marriage").setExecutor(this);
        getCommand("marriage").setTabCompleter(this);
        getCommand("marriageadmin").setExecutor(this);
        getServer().getPluginManager().registerEvents(this, this);
        if (getServer().getPluginManager().getPlugin("PlaceholderAPI") != null) {
            new MarriagePlaceholder(this).register();
            getLogger().info("已注册PlaceholderAPI扩展!");
        }
        // 注册RPGCore服务适配器
        serviceAdapter = new MarriageServiceAdapter(this);
        getLogger().info("GuangDianMarriage 结婚插件已启用!");
    }

    @Override
    protected void onPluginDisable() {
        saveMarriages();
        if (serviceAdapter != null) {
            serviceAdapter.unregister();
            serviceAdapter = null;
        }
        getLogger().info("GuangDianMarriage 结婚插件已禁用!");
    }

    @Override
    protected String getPluginName() {
        return "GuangDianMarriage";
    }

    public static GuangDianMarriage getInstance() {
        return instance;
    }

    // ==================== 公开API方法（供RPGCore服务调用） ====================

    /**
     * 检查玩家是否已婚 - API方法（使用UUID）
     * 
     * @param playerId 玩家UUID
     * @return 如果已婚返回 true
     */
    public boolean isMarriedAPI(UUID playerId) {
        String name = getPlayerNameFromUUID(playerId);
        return name != null && isMarried(name);
    }

    /**
     * 获取伴侣名称 - API方法（使用UUID）
     * 
     * @param playerId 玩家UUID
     * @return 伴侣名称，如果未婚返回 null
     */
    public String getPartnerAPI(UUID playerId) {
        String name = getPlayerNameFromUUID(playerId);
        return name != null ? getPartner(name) : null;
    }

    /**
     * 获取伴侣UUID - API方法
     * 
     * @param playerId 玩家UUID
     * @return 伴侣UUID，如果未婚返回 null
     */
    public UUID getPartnerUUIDAPI(UUID playerId) {
        String partnerName = getPartnerAPI(playerId);
        if (partnerName == null) return null;
        return getPlayerUUIDFromName(partnerName);
    }

    /**
     * 获取结婚天数 - API方法（使用UUID，不使用反射）
     * 
     * @param playerId 玩家UUID
     * @return 结婚天数
     */
    public long getMarriageDaysAPI(UUID playerId) {
        String name = getPlayerNameFromUUID(playerId);
        if (name == null) return 0;
        Marriage marriage = marriages.get(name.toLowerCase());
        return marriage != null ? marriage.getDaysMarried() : 0;
    }

    /**
     * 结婚 - API方法（使用UUID）
     * 
     * @param player1Id 玩家1 UUID
     * @param player2Id 玩家2 UUID
     * @return 如果成功返回 true
     */
    public boolean marryAPI(UUID player1Id, UUID player2Id) {
        String name1 = getPlayerNameFromUUID(player1Id);
        String name2 = getPlayerNameFromUUID(player2Id);
        if (name1 == null || name2 == null) return false;
        return marry(name1, name2);
    }

    /**
     * 离婚 - API方法（使用UUID）
     * 
     * @param playerId 玩家UUID
     * @return 如果成功返回 true
     */
    public boolean divorceAPI(UUID playerId) {
        String name = getPlayerNameFromUUID(playerId);
        return name != null && divorce(name);
    }

    /**
     * 获取结婚对象 - API方法（使用UUID）
     * 
     * @param playerId 玩家UUID
     * @return 结婚对象
     */
    public Object getMarriageAPI(UUID playerId) {
        String name = getPlayerNameFromUUID(playerId);
        return name != null ? marriages.get(name.toLowerCase()) : null;
    }

    /**
     * 获取亲密度 - API方法
     * 
     * @param playerId 玩家UUID
     * @return 亲密度值
     */
    public int getLovePointsAPI(UUID playerId) {
        String name = getPlayerNameFromUUID(playerId);
        if (name == null) return 0;
        Marriage marriage = marriages.get(name.toLowerCase());
        return marriage != null ? marriage.lovePoints : 0;
    }

    // ==================== 辅助方法 ====================

    /**
     * 从UUID获取玩家名称（优先在线玩家）
     */
    private String getPlayerNameFromUUID(UUID uuid) {
        org.bukkit.entity.Player online = getServer().getPlayer(uuid);
        if (online != null) return online.getName();
        org.bukkit.OfflinePlayer offline = getServer().getOfflinePlayer(uuid);
        return offline.getName();
    }

    /**
     * 从名称获取玩家UUID
     */
    private UUID getPlayerUUIDFromName(String name) {
        org.bukkit.entity.Player online = getServer().getPlayerExact(name);
        if (online != null) return online.getUniqueId();
        org.bukkit.OfflinePlayer offline = getServer().getOfflinePlayer(name);
        if (offline.hasPlayedBefore()) return offline.getUniqueId();
        return null;
    }

    // ==================== 内部方法 ====================

    private void loadMarriages() {
        marriages.clear();
        for (String key : config.getKeys(false)) {
            if (key.equals("settings") || key.equals("messages")) continue;
            
            var section = config.getConfigurationSection(key);
            if (section == null) {
                getLogger().warning("跳过无效的配置节: " + key);
                continue;
            }
            
            Map<String, Object> data = section.getValues(false);
            if (data != null) {
                try {
                    Marriage m = Marriage.deserialize(data);
                    if (m != null) {
                        marriages.put(m.player1.toLowerCase(), m);
                        marriages.put(m.player2.toLowerCase(), m);
                    }
                } catch (Exception e) {
                    getLogger().warning("解析结婚数据失败 [" + key + "]: " + e.getMessage());
                }
            }
        }
        getLogger().info("已加载 " + (marriages.size() / 2) + " 对夫妻!");
    }

    private void saveMarriages() {
        Set<Marriage> unique = new HashSet<>(marriages.values());
        for (String key : config.getKeys(false)) {
            if (!key.equals("settings") && !key.equals("messages")) config.set(key, null);
        }
        for (Marriage m : unique) {
            config.set(m.player1.toLowerCase(), m.serialize());
        }
        saveConfig();
    }

    public boolean marry(String p1, String p2) {
        if (isMarried(p1) || isMarried(p2)) return false;
        Marriage m = new Marriage(p1, p2);
        marriages.put(p1.toLowerCase(), m);
        marriages.put(p2.toLowerCase(), m);
        saveMarriages();
        return true;
    }

    public boolean divorce(String name) {
        Marriage m = marriages.remove(name.toLowerCase());
        if (m == null) return false;
        String partner = m.getPartner(name);
        marriages.remove(partner.toLowerCase());
        saveMarriages();
        return true;
    }

    public Marriage getMarriage(String name) {
        return marriages.get(name.toLowerCase());
    }

    public boolean isMarried(String name) {
        return marriages.containsKey(name.toLowerCase());
    }

    public String getPartner(String name) {
        Marriage m = marriages.get(name.toLowerCase());
        return m != null ? m.getPartner(name) : null;
    }

    public void sendPropose(String proposer, String target) {
        proposeRequests.put(target.toLowerCase(), new ProposeRequest(proposer, target));
    }

    public ProposeRequest getProposeRequest(String name) {
        ProposeRequest r = proposeRequests.get(name.toLowerCase());
        if (r != null && r.isExpired()) {
            proposeRequests.remove(name.toLowerCase());
            return null;
        }
        return r;
    }

    public void removeProposeRequest(String name) {
        proposeRequests.remove(name.toLowerCase());
    }

    public void sendTpRequest(String requester, String target) {
        tpRequests.put(target.toLowerCase(), new TpRequest(requester, target));
    }

    public TpRequest getTpRequest(String name) {
        TpRequest r = tpRequests.get(name.toLowerCase());
        if (r != null && r.isExpired()) {
            tpRequests.remove(name.toLowerCase());
            return null;
        }
        return r;
    }

    public void removeTpRequest(String name) {
        tpRequests.remove(name.toLowerCase());
    }

    public boolean canTp(String name) {
        Long last = tpCooldowns.get(name.toLowerCase());
        if (last == null) return true;
        int cd = config.getInt("settings.tp-cooldown", 60);
        return System.currentTimeMillis() - last > cd * 1000;
    }

    public int getTpCooldown(String name) {
        Long last = tpCooldowns.get(name.toLowerCase());
        if (last == null) return 0;
        int cd = config.getInt("settings.tp-cooldown", 60);
        long rem = cd * 1000 - (System.currentTimeMillis() - last);
        return rem > 0 ? (int) (rem / 1000) : 0;
    }

    public void setTpCooldown(String name) {
        tpCooldowns.put(name.toLowerCase(), System.currentTimeMillis());
    }

    public void teleportToPartner(Player p) {
        String partner = getPartner(p.getName());
        if (partner == null) return;
        Player targetP = getServer().getPlayer(partner);
        if (targetP != null && targetP.isOnline()) p.teleport(targetP.getLocation());
    }

    public int getMarriageCount() {
        return marriages.size() / 2;
    }

    public Collection<Marriage> getAllMarriages() {
        return new HashSet<>(marriages.values());
    }

    private static final MiniMessage MINI_MESSAGE = MiniMessage.miniMessage();

    public Component getMsg(String key) {
        String prefix = config.getString("messages.prefix", "<light_purple>[结婚] <white>");
        String msg = config.getString("messages." + key, "");
        return MINI_MESSAGE.deserialize(prefix + msg);
    }

    public Component getMsg(String key, String... placeholders) {
        String prefix = config.getString("messages.prefix", "<light_purple>[结婚] <white>");
        String msg = config.getString("messages." + key, "");
        String fullText = prefix + msg;
        for (int i = 0; i < placeholders.length; i += 2) {
            if (i + 1 < placeholders.length) fullText = fullText.replace("{" + placeholders[i] + "}", placeholders[i + 1]);
        }
        return MINI_MESSAGE.deserialize(fullText);
    }

    /**
     * 修复 P1: 权限对齐 - 检查命令权限
     * 根据子命令检查对应的权限声明
     */
    private boolean checkMarriagePermission(CommandSender sender, String subCommand) {
        if (sender.isOp()) return true; // OP 拥有所有权限
        
        switch (subCommand) {
            case "propose":
                return sender.hasPermission("guangdian.marriage.propose");
            case "divorce":
                return sender.hasPermission("guangdian.marriage.divorce");
            case "tp":
            case "tpaccept":
                return sender.hasPermission("guangdian.marriage.tp");
            case "gift":
                return sender.hasPermission("guangdian.marriage.gift");
            case "chat":
                return sender.hasPermission("guangdian.marriage.chat");
            case "accept":
            case "deny":
            case "info":
            case "list":
            case "nickname":
                // 这些命令使用默认权限 (所有玩家可用)
                return true;
            default:
                return true;
        }
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        // 修复 P1: 权限对齐 - 在命令分发层添加权限校验
        if (args.length == 0) { sendHelp(sender); return true; }
        String sub = args[0].toLowerCase();
        if (command.getName().equalsIgnoreCase("marriageadmin")) return handleAdmin(sender, sub, args);
        
        // 根据子命令检查对应权限
        if (!checkMarriagePermission(sender, sub)) {
            sender.sendMessage(getMsg("no-permission"));
            return true;
        }
        
        switch (sub) {
            case "propose": return handlePropose(sender, args);
            case "accept": return handleAccept(sender);
            case "deny": return handleDeny(sender);
            case "divorce": return handleDivorce(sender);
            case "info": return handleInfo(sender, args);
            case "list": return handleList(sender);
            case "tp": return handleTp(sender);
            case "tpaccept": return handleTpAccept(sender);
            case "gift": return handleGift(sender);
            case "chat": return handleChat(sender, args);
            case "nickname": return handleNickname(sender, args);
            default: sendHelp(sender); return true;
        }
    }

    private boolean handlePropose(CommandSender sender, String[] args) {
        if (!(sender instanceof Player)) { sender.sendMessage(getMsg("player-only")); return true; }
        Player p = (Player) sender;
        if (args.length < 2) { p.sendMessage(Component.text("用法: /marriage propose <玩家>").color(NamedTextColor.RED)); return true; }
        String target = args[1];
        Player targetP = getServer().getPlayer(target);
        if (targetP == null || !targetP.isOnline()) { p.sendMessage(getMsg("target-not-found")); return true; }
        if (targetP.equals(p)) { p.sendMessage(Component.text("你不能和自己结婚!").color(NamedTextColor.RED)); return true; }
        if (isMarried(p.getName())) { p.sendMessage(getMsg("already-married")); return true; }
        if (isMarried(target)) { p.sendMessage(getMsg("target-married")); return true; }
        sendPropose(p.getName(), target);
        p.sendMessage(getMsg("propose-sent", "target", target));
        targetP.sendMessage(getMsg("propose-received", "player", p.getName()));
        return true;
    }

    private boolean handleAccept(CommandSender sender) {
        if (!(sender instanceof Player)) { sender.sendMessage(getMsg("player-only")); return true; }
        Player p = (Player) sender;
        ProposeRequest req = getProposeRequest(p.getName());
        if (req == null) { p.sendMessage(getMsg("no-pending-propose")); return true; }
        Player proposer = getServer().getPlayer(req.proposer);
        if (proposer == null || !proposer.isOnline()) {
            removeProposeRequest(p.getName());
            p.sendMessage(Component.text("求婚者已离线!").color(NamedTextColor.RED)); return true;
        }
        removeProposeRequest(p.getName());
        if (marry(proposer.getName(), p.getName())) {
            p.sendMessage(getMsg("married", "partner", proposer.getName()));
            proposer.sendMessage(getMsg("married", "partner", p.getName()));
            Component broadcast = getMsg("married-broadcast", "player1", proposer.getName(), "player2", p.getName());
            for (Player pl : getServer().getOnlinePlayers()) pl.sendMessage(broadcast);
        }
        return true;
    }

    private boolean handleDeny(CommandSender sender) {
        if (!(sender instanceof Player)) { sender.sendMessage(getMsg("player-only")); return true; }
        Player p = (Player) sender;
        ProposeRequest req = getProposeRequest(p.getName());
        if (req == null) { p.sendMessage(getMsg("no-pending-propose")); return true; }
        Player proposer = getServer().getPlayer(req.proposer);
        removeProposeRequest(p.getName());
        p.sendMessage(getMsg("propose-cancelled"));
        if (proposer != null && proposer.isOnline()) proposer.sendMessage(getMsg("propose-denied"));
        return true;
    }

    private boolean handleDivorce(CommandSender sender) {
        if (!(sender instanceof Player)) { sender.sendMessage(getMsg("player-only")); return true; }
        Player p = (Player) sender;
        if (!isMarried(p.getName())) { p.sendMessage(getMsg("not-married")); return true; }
        String partner = getPartner(p.getName());
        divorce(p.getName());
        p.sendMessage(getMsg("divorced", "partner", partner));
        Player partnerP = getServer().getPlayer(partner);
        if (partnerP != null && partnerP.isOnline()) partnerP.sendMessage(getMsg("divorced", "partner", p.getName()));
        Component broadcast = getMsg("divorced-broadcast", "player1", p.getName(), "player2", partner);
        for (Player pl : getServer().getOnlinePlayers()) pl.sendMessage(broadcast);
        return true;
    }

    private boolean handleInfo(CommandSender sender, String[] args) {
        Marriage m;
        if (args.length >= 2) {
            m = getMarriage(args[1]);
        } else if (sender instanceof Player) {
            m = getMarriage(((Player) sender).getName());
        } else {
            sender.sendMessage(Component.text("用法: /marriage info <玩家>").color(NamedTextColor.RED)); return true;
        }
        if (m == null) { sender.sendMessage(getMsg("not-married")); return true; }
        sender.sendMessage(Component.text("========== 结婚信息 ==========").color(NamedTextColor.LIGHT_PURPLE));
        sender.sendMessage(Component.text("夫妻: ").color(NamedTextColor.YELLOW)
            .append(Component.text(m.player1).color(NamedTextColor.WHITE))
            .append(Component.text(" ❤ ").color(NamedTextColor.LIGHT_PURPLE))
            .append(Component.text(m.player2).color(NamedTextColor.WHITE)));
        sender.sendMessage(Component.text("结婚天数: ").color(NamedTextColor.YELLOW)
            .append(Component.text(m.getDaysMarried() + " 天").color(NamedTextColor.WHITE)));
        sender.sendMessage(Component.text("================================").color(NamedTextColor.LIGHT_PURPLE));
        return true;
    }

    private boolean handleList(CommandSender sender) {
        sender.sendMessage(Component.text("========== 夫妻列表 ==========").color(NamedTextColor.LIGHT_PURPLE));
        for (Marriage m : getAllMarriages()) {
            sender.sendMessage(Component.text("❤ ").color(NamedTextColor.LIGHT_PURPLE)
                .append(Component.text(m.player1 + " - " + m.player2 + " (" + m.getDaysMarried() + "天)").color(NamedTextColor.WHITE)));
        }
        sender.sendMessage(Component.text("总计: ").color(NamedTextColor.LIGHT_PURPLE)
            .append(Component.text(getMarriageCount() + " 对夫妻").color(NamedTextColor.WHITE)));
        sender.sendMessage(Component.text("================================").color(NamedTextColor.LIGHT_PURPLE));
        return true;
    }

    private boolean handleTp(CommandSender sender) {
        if (!(sender instanceof Player)) { sender.sendMessage(getMsg("player-only")); return true; }
        Player p = (Player) sender;
        if (!isMarried(p.getName())) { p.sendMessage(getMsg("not-married")); return true; }
        if (!canTp(p.getName())) {
            p.sendMessage(getMsg("tp-cooldown", "time", String.valueOf(getTpCooldown(p.getName())))); return true;
        }
        String partner = getPartner(p.getName());
        Player partnerP = getServer().getPlayer(partner);
        if (partnerP == null || !partnerP.isOnline()) { p.sendMessage(Component.text("配偶不在线!").color(NamedTextColor.RED)); return true; }
        sendTpRequest(p.getName(), partner);
        p.sendMessage(getMsg("tp-sent"));
        partnerP.sendMessage(getMsg("tp-received", "player", p.getName()));
        return true;
    }

    private boolean handleTpAccept(CommandSender sender) {
        if (!(sender instanceof Player)) { sender.sendMessage(getMsg("player-only")); return true; }
        Player p = (Player) sender;
        TpRequest req = getTpRequest(p.getName());
        if (req == null) { p.sendMessage(getMsg("tp-no-request")); return true; }
        Player requester = getServer().getPlayer(req.requester);
        removeTpRequest(p.getName());
        if (requester != null && requester.isOnline()) {
            setTpCooldown(requester.getName());
            requester.teleport(p.getLocation());
            requester.sendMessage(getMsg("tp-accepted"));
            p.sendMessage(Component.text("配偶已传送到你身边!").color(NamedTextColor.GREEN));
        }
        return true;
    }

    private boolean handleGift(CommandSender sender) {
        if (!(sender instanceof Player)) { sender.sendMessage(getMsg("player-only")); return true; }
        Player p = (Player) sender;
        if (!isMarried(p.getName())) { p.sendMessage(getMsg("not-married")); return true; }
        org.bukkit.inventory.ItemStack item = p.getInventory().getItemInMainHand();
        if (item == null || item.getType() == org.bukkit.Material.AIR) { p.sendMessage(Component.text("请手持要送的物品!").color(NamedTextColor.RED)); return true; }
        String partner = getPartner(p.getName());
        Player partnerP = getServer().getPlayer(partner);
        if (partnerP == null || !partnerP.isOnline()) { p.sendMessage(Component.text("配偶不在线!").color(NamedTextColor.RED)); return true; }
        org.bukkit.inventory.ItemStack gift = item.clone();
        item.setAmount(0);
        java.util.HashMap<Integer, org.bukkit.inventory.ItemStack> leftover = partnerP.getInventory().addItem(gift);
        if (!leftover.isEmpty()) {
            p.sendMessage(Component.text("配偶背包已满!").color(NamedTextColor.RED));
            p.getInventory().addItem(gift);
            return true;
        }
        p.sendMessage(getMsg("gift-sent"));
        partnerP.sendMessage(getMsg("gift-received"));
        return true;
    }

    private boolean handleChat(CommandSender sender, String[] args) {
        if (!(sender instanceof Player)) { sender.sendMessage(getMsg("player-only")); return true; }
        Player p = (Player) sender;
        if (!isMarried(p.getName())) { p.sendMessage(getMsg("not-married")); return true; }
        if (args.length < 2) { p.sendMessage(Component.text("用法: /marriage chat <消息>").color(NamedTextColor.RED)); return true; }
        String msg = String.join(" ", Arrays.copyOfRange(args, 1, args.length));
        String partner = getPartner(p.getName());
        Player partnerP = getServer().getPlayer(partner);
        String chatPrefix = config.getString("messages.chat-prefix", "<light_purple>[夫妻] <white>");
        Component formatted = MINI_MESSAGE.deserialize(chatPrefix)
            .append(Component.text(p.getName()).color(NamedTextColor.YELLOW))
            .append(Component.text(": ").color(NamedTextColor.WHITE))
            .append(Component.text(msg).color(NamedTextColor.WHITE));
        p.sendMessage(formatted);
        if (partnerP != null && partnerP.isOnline()) partnerP.sendMessage(formatted);
        return true;
    }

    private boolean handleNickname(CommandSender sender, String[] args) {
        if (!(sender instanceof Player)) { sender.sendMessage(getMsg("player-only")); return true; }
        Player p = (Player) sender;
        if (!isMarried(p.getName())) { p.sendMessage(getMsg("not-married")); return true; }
        if (args.length < 2) { p.sendMessage(Component.text("用法: /marriage nickname <昵称>").color(NamedTextColor.RED)); return true; }
        Marriage m = getMarriage(p.getName());
        if (m != null) m.setNickname(p.getName(), args[1]);
        p.sendMessage(net.kyori.adventure.text.Component.text("已设置你对配偶的昵称为: " + args[1], net.kyori.adventure.text.format.NamedTextColor.GREEN));
        return true;
    }

    private boolean handleAdmin(CommandSender sender, String sub, String[] args) {
        if (!sender.hasPermission("guangdian.marriage.admin")) { sender.sendMessage(getMsg("no-permission")); return true; }
        switch (sub) {
            case "reload":
                reloadConfig();
                config = getConfig();
                loadMarriages();
                sender.sendMessage(net.kyori.adventure.text.Component.text("配置已重新加载!", net.kyori.adventure.text.format.NamedTextColor.GREEN));
                return true;
            case "force":
                if (args.length < 3) { sender.sendMessage(net.kyori.adventure.text.Component.text("用法: /marriageadmin force <玩家1> <玩家2>", net.kyori.adventure.text.format.NamedTextColor.RED)); return true; }
                if (isMarried(args[1]) || isMarried(args[2])) { sender.sendMessage(net.kyori.adventure.text.Component.text("其中一方已结婚!", net.kyori.adventure.text.format.NamedTextColor.RED)); return true; }
                marry(args[1], args[2]);
                sender.sendMessage(net.kyori.adventure.text.Component.text("已强制让 " + args[1] + " 和 " + args[2] + " 结婚!", net.kyori.adventure.text.format.NamedTextColor.GREEN));
                return true;
            default:
                sender.sendMessage(net.kyori.adventure.text.Component.text("用法: /marriageadmin <reload|force>", net.kyori.adventure.text.format.NamedTextColor.RED));
                return true;
        }
    }

    private void sendHelp(CommandSender sender) {
        sender.sendMessage(net.kyori.adventure.text.Component.text("========== 结婚帮助 ==========", net.kyori.adventure.text.format.NamedTextColor.LIGHT_PURPLE));
        sender.sendMessage(net.kyori.adventure.text.Component.text("/marriage propose <玩家> ", net.kyori.adventure.text.format.NamedTextColor.YELLOW).append(net.kyori.adventure.text.Component.text("- 求婚", net.kyori.adventure.text.format.NamedTextColor.GRAY)));
        sender.sendMessage(net.kyori.adventure.text.Component.text("/marriage accept ", net.kyori.adventure.text.format.NamedTextColor.YELLOW).append(net.kyori.adventure.text.Component.text("- 接受求婚", net.kyori.adventure.text.format.NamedTextColor.GRAY)));
        sender.sendMessage(net.kyori.adventure.text.Component.text("/marriage deny ", net.kyori.adventure.text.format.NamedTextColor.YELLOW).append(net.kyori.adventure.text.Component.text("- 拒绝求婚", net.kyori.adventure.text.format.NamedTextColor.GRAY)));
        sender.sendMessage(net.kyori.adventure.text.Component.text("/marriage divorce ", net.kyori.adventure.text.format.NamedTextColor.YELLOW).append(net.kyori.adventure.text.Component.text("- 离婚", net.kyori.adventure.text.format.NamedTextColor.GRAY)));
        sender.sendMessage(net.kyori.adventure.text.Component.text("/marriage info [玩家] ", net.kyori.adventure.text.format.NamedTextColor.YELLOW).append(net.kyori.adventure.text.Component.text("- 查看信息", net.kyori.adventure.text.format.NamedTextColor.GRAY)));
        sender.sendMessage(net.kyori.adventure.text.Component.text("/marriage list ", net.kyori.adventure.text.format.NamedTextColor.YELLOW).append(net.kyori.adventure.text.Component.text("- 夫妻列表", net.kyori.adventure.text.format.NamedTextColor.GRAY)));
        sender.sendMessage(net.kyori.adventure.text.Component.text("/marriage tp ", net.kyori.adventure.text.format.NamedTextColor.YELLOW).append(net.kyori.adventure.text.Component.text("- 传送到配偶", net.kyori.adventure.text.format.NamedTextColor.GRAY)));
        sender.sendMessage(net.kyori.adventure.text.Component.text("/marriage tpaccept ", net.kyori.adventure.text.format.NamedTextColor.YELLOW).append(net.kyori.adventure.text.Component.text("- 接受传送", net.kyori.adventure.text.format.NamedTextColor.GRAY)));
        sender.sendMessage(net.kyori.adventure.text.Component.text("/marriage gift ", net.kyori.adventure.text.format.NamedTextColor.YELLOW).append(net.kyori.adventure.text.Component.text("- 送礼物", net.kyori.adventure.text.format.NamedTextColor.GRAY)));
        sender.sendMessage(net.kyori.adventure.text.Component.text("/marriage chat <消息> ", net.kyori.adventure.text.format.NamedTextColor.YELLOW).append(net.kyori.adventure.text.Component.text("- 夫妻私聊", net.kyori.adventure.text.format.NamedTextColor.GRAY)));
        sender.sendMessage(net.kyori.adventure.text.Component.text("/marriage nickname <昵称> ", net.kyori.adventure.text.format.NamedTextColor.YELLOW).append(net.kyori.adventure.text.Component.text("- 设置昵称", net.kyori.adventure.text.format.NamedTextColor.GRAY)));
        sender.sendMessage(net.kyori.adventure.text.Component.text("================================", net.kyori.adventure.text.format.NamedTextColor.LIGHT_PURPLE));
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        List<String> list = new ArrayList<>();
        if (command.getName().equalsIgnoreCase("marriageadmin")) {
            if (args.length == 1) list.addAll(Arrays.asList("reload", "force"));
            return list.stream().filter(s -> s.startsWith(args[0].toLowerCase())).collect(Collectors.toList());
        }
        if (args.length == 1) {
            list.addAll(Arrays.asList("propose", "accept", "deny", "divorce", "info", "list", "tp", "tpaccept", "gift", "chat", "nickname"));
        } else if (args.length == 2) {
            String sub = args[0].toLowerCase();
            if (sub.equals("propose") || sub.equals("info")) {
                for (Player p : getServer().getOnlinePlayers()) list.add(p.getName());
            }
        }
        return list.stream().filter(s -> s.toLowerCase().startsWith(args[args.length - 1].toLowerCase())).collect(Collectors.toList());
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onJoin(PlayerJoinEvent e) {
        Player p = e.getPlayer();
        String partner = getPartner(p.getName());
        if (partner != null) {
            Player partnerP = getServer().getPlayer(partner);
            if (partnerP != null && partnerP.isOnline()) {
                partnerP.sendMessage(net.kyori.adventure.text.Component.text("[结婚] " + p.getName() + " ❤ 上线了!", net.kyori.adventure.text.format.NamedTextColor.LIGHT_PURPLE));
            }
        }
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onQuit(PlayerQuitEvent e) {
        Player p = e.getPlayer();
        String partner = getPartner(p.getName());
        if (partner != null) {
            Player partnerP = getServer().getPlayer(partner);
            if (partnerP != null && partnerP.isOnline()) {
                partnerP.sendMessage(net.kyori.adventure.text.Component.text("[结婚] " + p.getName() + " 下线了...", net.kyori.adventure.text.format.NamedTextColor.LIGHT_PURPLE));
            }
        }
    }

    static class Marriage {
        String player1;
        String player2;
        long marryDate;
        int lovePoints;
        String player1Nickname;
        String player2Nickname;

        Marriage(String player1, String player2) {
            this.player1 = player1;
            this.player2 = player2;
            this.marryDate = System.currentTimeMillis();
            this.lovePoints = 0;
            this.player1Nickname = "";
            this.player2Nickname = "";
        }

        String getPartner(String name) {
            if (player1.equalsIgnoreCase(name)) return player2;
            if (player2.equalsIgnoreCase(name)) return player1;
            return null;
        }

        long getDaysMarried() {
            return (System.currentTimeMillis() - marryDate) / (1000 * 60 * 60 * 24);
        }

        void setNickname(String name, String nickname) {
            if (player1.equalsIgnoreCase(name)) player1Nickname = nickname;
            else if (player2.equalsIgnoreCase(name)) player2Nickname = nickname;
        }

        String getNickname(String name) {
            if (player1.equalsIgnoreCase(name)) return player1Nickname.isEmpty() ? player1 : player1Nickname;
            if (player2.equalsIgnoreCase(name)) return player2Nickname.isEmpty() ? player2 : player2Nickname;
            return name;
        }

        Map<String, Object> serialize() {
            Map<String, Object> m = new HashMap<>();
            m.put("player1", player1);
            m.put("player2", player2);
            m.put("marryDate", marryDate);
            m.put("lovePoints", lovePoints);
            m.put("player1Nickname", player1Nickname);
            m.put("player2Nickname", player2Nickname);
            return m;
        }

        static Marriage deserialize(Map<String, Object> data) {
            Object p1 = data.get("player1");
            Object p2 = data.get("player2");
            
            if (p1 == null || p2 == null) {
                throw new IllegalArgumentException("缺少必要的字段: player1 或 player2");
            }
            
            String player1 = String.valueOf(p1);
            String player2 = String.valueOf(p2);
            
            Marriage m = new Marriage(player1, player2);
            
            Object marryDateObj = data.get("marryDate");
            if (marryDateObj instanceof Number) {
                m.marryDate = ((Number) marryDateObj).longValue();
            } else if (marryDateObj != null) {
                try {
                    m.marryDate = Long.parseLong(String.valueOf(marryDateObj));
                } catch (NumberFormatException e) {
                    m.marryDate = System.currentTimeMillis();
                }
            }
            
            Object lovePointsObj = data.get("lovePoints");
            if (lovePointsObj instanceof Number) {
                m.lovePoints = ((Number) lovePointsObj).intValue();
            } else if (lovePointsObj != null) {
                try {
                    m.lovePoints = Integer.parseInt(String.valueOf(lovePointsObj));
                } catch (NumberFormatException e) {
                    m.lovePoints = 0;
                }
            }
            
            m.player1Nickname = data.containsKey("player1Nickname") ? String.valueOf(data.get("player1Nickname")) : "";
            m.player2Nickname = data.containsKey("player2Nickname") ? String.valueOf(data.get("player2Nickname")) : "";
            
            return m;
        }
    }

    static class ProposeRequest {
        String proposer;
        String target;
        long timestamp;

        ProposeRequest(String proposer, String target) {
            this.proposer = proposer;
            this.target = target;
            this.timestamp = System.currentTimeMillis();
        }

        boolean isExpired() {
            return System.currentTimeMillis() - timestamp > 60000;
        }
    }

    static class TpRequest {
        String requester;
        String target;
        long timestamp;

        TpRequest(String requester, String target) {
            this.requester = requester;
            this.target = target;
            this.timestamp = System.currentTimeMillis();
        }

        boolean isExpired() {
            return System.currentTimeMillis() - timestamp > 30000;
        }
    }

    static class MarriagePlaceholder extends me.clip.placeholderapi.expansion.PlaceholderExpansion {
        private final GuangDianMarriage plugin;

        MarriagePlaceholder(GuangDianMarriage plugin) {
            this.plugin = plugin;
        }

        @Override
        public String getIdentifier() { return "gdmarriage"; }
        @Override
        public String getAuthor() { return "GuangDian"; }
        @Override
        public String getVersion() { return "1.0.0"; }
        @Override
        public boolean persist() { return true; }

        @Override
        public String onRequest(org.bukkit.OfflinePlayer p, String params) {
            if (p == null) return "";
            String param = params.toLowerCase();
            if (param.equals("partner") || param.equals("配偶")) {
                String partner = plugin.getPartner(p.getName());
                return partner != null ? partner : "无";
            }
            if (param.equals("is_married") || param.equals("是否结婚")) {
                return plugin.isMarried(p.getName()) ? "true" : "false";
            }
            if (param.equals("status") || param.equals("状态")) {
                return plugin.isMarried(p.getName()) ? "\u2764 已婚" : "单身";
            }
            if (param.equals("days") || param.equals("天数")) {
                Marriage m = plugin.getMarriage(p.getName());
                return m != null ? String.valueOf(m.getDaysMarried()) : "0";
            }
            if (param.equals("total") || param.equals("总数")) {
                return String.valueOf(plugin.getMarriageCount());
            }
            if (param.equals("partner_status") || param.equals("配偶状态")) {
                String partner = plugin.getPartner(p.getName());
                if (partner == null) return "无";
                return plugin.getServer().getPlayer(partner) != null ? "\u00a7a在线" : "\u00a7c离线";
            }
            return null;
        }
    }
}
