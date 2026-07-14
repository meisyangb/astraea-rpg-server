package cn.guangdian.realm;

import cn.guangdian.rpgcore.message.UnifiedMessageService;
import cn.guangdian.rpgcore.plugin.AbstractRPGPlugin;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.TabCompleter;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 光点境界插件主类
 * 
 * 修炼境界系统（鬼王宗专属）：
 * - 6大境界（练气、筑基、假丹、金丹、元婴、化神）
 * - 修为积累系统（击杀怪物获得修为）
 * - 境界突破系统
 * - 与GuangDianMobs集成，击杀怪物获得修为
 * - 与GuangDianArmorStats集成，境界属性加成
 * 
 * 注意：此为鬼王宗的变强方式，其他宗门有不同的进阶办法
 */
public class GuangDianRealm extends AbstractRPGPlugin implements Listener, CommandExecutor, TabCompleter {
    private static GuangDianRealm instance;
    private FileConfiguration config;
    private UnifiedMessageService msg;
    
    // 境界数据（按顺序存储）
    private final List<Realm> realmList = new ArrayList<>();
    private final Map<String, Realm> realmMap = new ConcurrentHashMap<>();
    
    // 怪物修为配置
    private final Map<String, MobCultivation> mobCultivationMap = new ConcurrentHashMap<>();
    
    // 玩家数据
    private final Map<String, CultivationPlayer> playerData = new ConcurrentHashMap<>();
    
    // 数据管理
    private CultivationDataManager dataManager;
    
    // 监听器
    private CultivationListener cultivationListener;
    private NeidanListener neidanListener;
    
    // 属性集成
    private RealmAttributeIntegration attributeIntegration;
    
    @Override
    protected void onPluginEnable() {
        instance = this;
        
        msg = UnifiedMessageService.getInstance();
        
        saveDefaultConfig();
        config = getConfig();
        
        loadRealms();
        loadMobCultivation();
        
        dataManager = new CultivationDataManager(this);
        dataManager.loadAll();
        
        // 初始化属性集成
        attributeIntegration = new RealmAttributeIntegration(this);
        attributeIntegration.init();
        
        // 注册监听器
        cultivationListener = new CultivationListener(this);
        getServer().getPluginManager().registerEvents(cultivationListener, this);
        
        // 注册内丹监听器
        neidanListener = new NeidanListener(this);
        getServer().getPluginManager().registerEvents(neidanListener, this);
        
        getServer().getPluginManager().registerEvents(this, this);
        
        // 注册命令
        getCommand("realm").setExecutor(this);
        getCommand("realm").setTabCompleter(this);
        getCommand("realmadmin").setExecutor(this);
        
        // 注册PlaceholderAPI
        if (getServer().getPluginManager().getPlugin("PlaceholderAPI") != null) {
            new RealmPlaceholder(this).register();
            getLogger().info("已注册PlaceholderAPI扩展!");
        }
        
        getLogger().info("GuangDianRealm 境界插件已启用!");
        getLogger().info("已加载 " + realmList.size() + " 个境界");
        getLogger().info("这是鬼王宗的变强方式 - 击杀怪物获得修为");
    }
    
    @Override
    protected void onPluginDisable() {
        cancelAllTasks();
        dataManager.saveAll();
        getLogger().info("GuangDianRealm 境界插件已禁用!");
    }
    
    @Override
    protected String getPluginName() {
        return "GuangDianRealm";
    }
    
    public static GuangDianRealm getInstance() {
        return instance;
    }
    
    // ==================== 数据加载 ====================
    
    private void loadRealms() {
        realmList.clear();
        realmMap.clear();
        
        ConfigurationSection realmSection = config.getConfigurationSection("realms");
        if (realmSection == null) return;
        
        // 按顺序加载
        List<String> sortedKeys = new ArrayList<>(realmSection.getKeys(false));
        sortedKeys.sort((a, b) -> {
            ConfigurationSection aSection = realmSection.getConfigurationSection(a);
            ConfigurationSection bSection = realmSection.getConfigurationSection(b);
            if (aSection == null || bSection == null) return 0;
            long aReq = aSection.getLong("required_cultivation", 0);
            long bReq = bSection.getLong("required_cultivation", 0);
            return Long.compare(aReq, bReq);
        });
        
        for (String key : sortedKeys) {
            ConfigurationSection realmData = realmSection.getConfigurationSection(key);
            if (realmData != null) {
                Realm realm = new Realm(key, realmData);
                realmList.add(realm);
                realmMap.put(key.toLowerCase(), realm);
            }
        }
        
        getLogger().info("已加载 " + realmList.size() + " 个境界");
    }
    
    private void loadMobCultivation() {
        mobCultivationMap.clear();
        
        ConfigurationSection mobSection = config.getConfigurationSection("mob_cultivation");
        if (mobSection == null) return;
        
        for (String mobName : mobSection.getKeys(false)) {
            ConfigurationSection mobData = mobSection.getConfigurationSection(mobName);
            if (mobData != null) {
                MobCultivation mc = new MobCultivation(mobName, mobData);
                mobCultivationMap.put(mobName.toLowerCase(), mc);
            }
        }
        
        getLogger().info("已加载 " + mobCultivationMap.size() + " 个怪物修为配置");
    }
    
    // ==================== 玩家数据操作 ====================
    
    public CultivationPlayer getPlayerData(Player player) {
        return playerData.computeIfAbsent(player.getUniqueId().toString(),
            k -> new CultivationPlayer(player.getUniqueId().toString()));
    }
    
    public CultivationPlayer getPlayerData(String playerId) {
        return playerData.get(playerId);
    }
    
    public Realm getCurrentRealm(Player player) {
        CultivationPlayer data = getPlayerData(player);
        if (data == null || data.getCurrentRealmId() == null) return null;
        return realmMap.get(data.getCurrentRealmId().toLowerCase());
    }
    
    public Realm getNextRealm(Player player) {
        CultivationPlayer data = getPlayerData(player);
        if (data == null) {
            return realmList.isEmpty() ? null : realmList.get(0);
        }
        
        if (data.getCurrentRealmId() == null) {
            return realmList.isEmpty() ? null : realmList.get(0);
        }
        
        for (int i = 0; i < realmList.size(); i++) {
            if (realmList.get(i).getId().equalsIgnoreCase(data.getCurrentRealmId())) {
                if (i + 1 < realmList.size()) {
                    return realmList.get(i + 1);
                }
                return null;
            }
        }
        
        return null;
    }
    
    public Realm getRealm(String realmId) {
        return realmMap.get(realmId.toLowerCase());
    }
    
    public List<Realm> getAllRealms() {
        return new ArrayList<>(realmList);
    }
    
    // ==================== 修为操作 ====================
    
    /**
     * 给玩家增加修为
     */
    public void addCultivation(Player player, long amount) {
        if (amount <= 0) return;
        
        CultivationPlayer data = getPlayerData(player);
        data.addCultivation(amount);
        
        notifyCultivation(player, amount, data.getCultivation());
        dataManager.save(data);
        
        // 每获得修为时更新内丹显示（显示最新进度）
        updateNeidan(player);
    }
    
    /**
     * 获取怪物击杀修为
     */
    public long getMobCultivation(String mobName, int mobLevel) {
        MobCultivation mc = mobCultivationMap.get(mobName.toLowerCase());
        if (mc != null) {
            return mc.getCultivation();
        }
        
        if (config.getBoolean("default_cultivation.enabled", true)) {
            double base = config.getDouble("default_cultivation.base", 1);
            double exponent = config.getDouble("default_cultivation.exponent", 1.5);
            long maxCultivation = config.getLong("default_cultivation.max_cultivation", 100000);
            
            long calculated = (long) (base * Math.pow(mobLevel, exponent));
            return Math.min(calculated, maxCultivation);
        }
        
        return 0;
    }
    
    /**
     * 尝试突破（带属性加成）
     */
    public boolean attemptBreakthrough(Player player) {
        CultivationPlayer data = getPlayerData(player);
        Realm oldRealm = getCurrentRealm(player);
        Realm nextRealm = getNextRealm(player);
        
        if (nextRealm == null) {
            return false;
        }
        
        if (data.getCultivation() < nextRealm.getRequiredCultivation()) {
            return false;
        }
        
        if (!config.getBoolean("settings.require_breakthrough", true)) {
            // 直接晋升
            data.setCurrentRealmId(nextRealm.getId());
            data.setLastBreakthroughTime(System.currentTimeMillis());
            dataManager.save(data);
            
            // 应用属性加成
            attributeIntegration.updateRealmAttributes(player, oldRealm, nextRealm);
            
            // 更新内丹显示
            updateNeidan(player);
            return true;
        }
        
        // 计算突破成功率
        int baseChance = config.getInt("settings.base_breakthrough_chance", 80);
        int bonus = 0;
        
        long extraCultivation = data.getCultivation() - nextRealm.getRequiredCultivation();
        long tenPercent = nextRealm.getRequiredCultivation() / 10;
        if (tenPercent > 0) {
            bonus += (int) (extraCultivation / tenPercent) * 5;
        }
        
        int totalChance = Math.min(100, baseChance + bonus);
        
        if (new Random().nextInt(100) < totalChance) {
            data.setCurrentRealmId(nextRealm.getId());
            data.setLastBreakthroughTime(System.currentTimeMillis());
            dataManager.save(data);
            
            // 应用属性加成
            attributeIntegration.updateRealmAttributes(player, oldRealm, nextRealm);
            
            // 更新内丹显示
            updateNeidan(player);
            return true;
        } else {
            int lossPercent = config.getInt("settings.fail_loss_percent", 10);
            long loss = nextRealm.getRequiredCultivation() * lossPercent / 100;
            data.subtractCultivation(loss);
            dataManager.save(data);
            
            // 失败也更新内丹显示（修为减少了）
            updateNeidan(player);
            return false;
        }
    }
    
    // ==================== 消息处理 ====================
    
    private void notifyCultivation(Player player, long amount, long total) {
        long notifyInterval = config.getLong("settings.cultivation_notify_interval", 100);
        
        if (total % notifyInterval < amount || total >= 1000) {
            String message = config.getString("messages.cultivation_gain",
                "<yellow>获得 {amount} 点修为! (累计: {total})");
            message = message.replace("{amount}", String.valueOf(amount))
                .replace("{total}", String.valueOf(total));
            player.sendMessage(msg.colorize(message));
        }
    }
    
    public Component getMsg(String key) {
        String prefix = config.getString("messages.prefix", "<gold>[境界] <white>");
        String message = config.getString("messages." + key, "");
        return msg.colorize(prefix + message);
    }
    
    public Component getMsg(String key, String... placeholders) {
        String prefix = config.getString("messages.prefix", "<gold>[境界] <white>");
        String message = config.getString("messages." + key, "");
        String fullMsg = prefix + message;
        for (int i = 0; i < placeholders.length; i += 2) {
            if (i + 1 < placeholders.length) {
                fullMsg = fullMsg.replace("{" + placeholders[i] + "}", placeholders[i + 1]);
            }
        }
        return msg.colorize(fullMsg);
    }
    
    // ==================== 命令处理 ====================
    
    @Override
    public boolean onCommand(org.bukkit.command.CommandSender sender, Command command, String label, String[] args) {
        if (args.length == 0) {
            sendHelp(sender);
            return true;
        }
        
        String sub = args[0].toLowerCase();
        
        if (command.getName().equalsIgnoreCase("realmadmin")) {
            return handleAdmin(sender, sub, args);
        }
        
        switch (sub) {
            case "info": return handleInfo(sender);
            case "breakthrough": return handleBreakthrough(sender);
            case "list": return handleList(sender);
            default: sendHelp(sender); return true;
        }
    }
    
    private boolean handleInfo(org.bukkit.command.CommandSender sender) {
        if (!(sender instanceof Player)) {
            sender.sendMessage(getMsg("player-only"));
            return true;
        }
        Player player = (Player) sender;
        
        CultivationPlayer data = getPlayerData(player);
        Realm currentRealm = getCurrentRealm(player);
        Realm nextRealm = getNextRealm(player);
        
        sender.sendMessage(Component.text("========== 修炼信息 ==========").color(NamedTextColor.GOLD));
        
        if (currentRealm != null) {
            sender.sendMessage(getMsg("realm_info", "realm", currentRealm.getName()));
            sender.sendMessage(msg.colorize("<yellow>境界类型: <white>" + currentRealm.getRealmTypeName()));
            sender.sendMessage(msg.colorize("<yellow>属性加成: <white>生命+" + currentRealm.getBonuses().getMaxHealth() + 
                " 攻击+" + currentRealm.getBonuses().getAttackDamage() + 
                " 防御+" + currentRealm.getBonuses().getDefense()));
        } else {
            sender.sendMessage(msg.colorize("<yellow>当前境界: <white>凡人"));
        }
        
        if (nextRealm != null) {
            sender.sendMessage(getMsg("cultivation_info", "cultivation", 
                String.valueOf(data.getCultivation()), "required", String.valueOf(nextRealm.getRequiredCultivation())));
            sender.sendMessage(getMsg("next_realm", "next_realm", nextRealm.getName(), 
                "required", String.valueOf(nextRealm.getRequiredCultivation())));
        } else {
            sender.sendMessage(getMsg("realm_max"));
        }
        
        sender.sendMessage(Component.text("==============================").color(NamedTextColor.GOLD));
        return true;
    }
    
    private boolean handleBreakthrough(org.bukkit.command.CommandSender sender) {
        if (!(sender instanceof Player)) {
            sender.sendMessage(getMsg("player-only"));
            return true;
        }
        Player player = (Player) sender;
        
        CultivationPlayer data = getPlayerData(player);
        Realm nextRealm = getNextRealm(player);
        
        if (nextRealm == null) {
            player.sendMessage(getMsg("realm_max"));
            return true;
        }
        
        if (data.getCultivation() < nextRealm.getRequiredCultivation()) {
            player.sendMessage(getMsg("not_ready"));
            return true;
        }
        
        if (attemptBreakthrough(player)) {
            player.sendMessage(getMsg("breakthrough_success", "realm", nextRealm.getName()));
        } else {
            long loss = nextRealm.getRequiredCultivation() * config.getInt("settings.fail_loss_percent", 10) / 100;
            player.sendMessage(getMsg("breakthrough_fail", "loss", String.valueOf(loss)));
        }
        
        return true;
    }
    
    private boolean handleList(org.bukkit.command.CommandSender sender) {
        sender.sendMessage(Component.text("========== 境界列表 ==========").color(NamedTextColor.GOLD));
        for (Realm realm : realmList) {
            sender.sendMessage(msg.colorize("<yellow>" + realm.getName() + 
                " <gray>- <white>需要 " + realm.getRequiredCultivation() + " 修为" +
                " <dark_gray>(生命+" + realm.getBonuses().getMaxHealth() + ")"));
        }
        sender.sendMessage(Component.text("==============================").color(NamedTextColor.GOLD));
        return true;
    }
    
    private boolean handleAdmin(org.bukkit.command.CommandSender sender, String sub, String[] args) {
        if (!sender.hasPermission("guangdian.realm.admin")) {
            sender.sendMessage(getMsg("no-permission"));
            return true;
        }
        
        switch (sub) {
            case "reload":
                reloadConfig();
                config = getConfig();
                loadRealms();
                loadMobCultivation();
                sender.sendMessage(Component.text("配置已重新加载!").color(NamedTextColor.GREEN));
                return true;
            case "setcultivation":
                if (args.length < 3) {
                    sender.sendMessage(Component.text("用法: /realmadmin setcultivation <玩家> <修为>").color(NamedTextColor.RED));
                    return true;
                }
                Player target = Bukkit.getPlayer(args[1]);
                if (target == null) {
                    sender.sendMessage(Component.text("玩家不在线!").color(NamedTextColor.RED));
                    return true;
                }
                long amount = Long.parseLong(args[2]);
                CultivationPlayer data = getPlayerData(target);
                data.setCultivation(amount);
                dataManager.save(data);
                sender.sendMessage(Component.text("已将 " + target.getName() + " 的修为设置为 " + amount).color(NamedTextColor.GREEN));
                return true;
            case "setrealm":
                if (args.length < 3) {
                    sender.sendMessage(Component.text("用法: /realmadmin setrealm <玩家> <境界>").color(NamedTextColor.RED));
                    return true;
                }
                Player target2 = Bukkit.getPlayer(args[1]);
                if (target2 == null) {
                    sender.sendMessage(Component.text("玩家不在线!").color(NamedTextColor.RED));
                    return true;
                }
                Realm realm = getRealm(args[2]);
                if (realm == null) {
                    sender.sendMessage(Component.text("境界不存在!").color(NamedTextColor.RED));
                    return true;
                }
                CultivationPlayer data2 = getPlayerData(target2);
                Realm oldRealm = getCurrentRealm(target2);
                data2.setCurrentRealmId(realm.getId());
                dataManager.save(data2);
                
                // 应用属性加成
                attributeIntegration.updateRealmAttributes(target2, oldRealm, realm);
                
                // 更新内丹显示
                updateNeidan(target2);
                
                sender.sendMessage(Component.text("已将 " + target2.getName() + " 的境界设置为 " + realm.getName()).color(NamedTextColor.GREEN));
                return true;
            default:
                sender.sendMessage(Component.text("用法: /realmadmin <reload|setcultivation|setrealm>").color(NamedTextColor.RED));
                return true;
        }
    }
    
    private void sendHelp(org.bukkit.command.CommandSender sender) {
        sender.sendMessage(Component.text("========== 境界帮助 ==========").color(NamedTextColor.GOLD));
        sender.sendMessage(Component.text("/realm info ").color(NamedTextColor.YELLOW)
            .append(Component.text("- 查看修炼信息").color(NamedTextColor.GRAY)));
        sender.sendMessage(Component.text("/realm breakthrough ").color(NamedTextColor.YELLOW)
            .append(Component.text("- 尝试突破境界").color(NamedTextColor.GRAY)));
        sender.sendMessage(Component.text("/realm list ").color(NamedTextColor.YELLOW)
            .append(Component.text("- 境界列表").color(NamedTextColor.GRAY)));
        sender.sendMessage(Component.text("==============================").color(NamedTextColor.GOLD));
    }
    
    @Override
    public List<String> onTabComplete(org.bukkit.command.CommandSender sender, Command command, String alias, String[] args) {
        List<String> list = new ArrayList<>();
        
        if (command.getName().equalsIgnoreCase("realmadmin")) {
            if (args.length == 1) {
                list.addAll(Arrays.asList("reload", "setcultivation", "setrealm"));
            } else if (args.length == 2 && !args[0].equalsIgnoreCase("reload")) {
                for (Player p : Bukkit.getOnlinePlayers()) list.add(p.getName());
            } else if (args.length == 3 && args[0].equalsIgnoreCase("setrealm")) {
                for (Realm realm : realmList) list.add(realm.getId());
            }
            return list;
        }
        
        if (args.length == 1) {
            list.addAll(Arrays.asList("info", "breakthrough", "list"));
        }
        
        return list;
    }
    
    // ==================== 事件监听 ====================
    
    @EventHandler(priority = EventPriority.MONITOR)
    public void onJoin(PlayerJoinEvent e) {
        Player player = e.getPlayer();
        CultivationPlayer data = dataManager.load(player.getUniqueId().toString());
        if (data == null) {
            data = new CultivationPlayer(player.getUniqueId().toString());
        }
        playerData.put(player.getUniqueId().toString(), data);
        
        // 应用境界属性加成
        if (data.getCurrentRealmId() != null) {
            Realm realm = getRealm(data.getCurrentRealmId());
            if (realm != null) {
                attributeIntegration.applyRealmBonus(player, realm);
            }
        }
    }
    
    // ==================== Getter ====================
    
    public CultivationDataManager getDataManager() {
        return dataManager;
    }
    
    public CultivationListener getCultivationListener() {
        return cultivationListener;
    }
    
    public RealmAttributeIntegration getAttributeIntegration() {
        return attributeIntegration;
    }
    
    public NeidanListener getNeidanListener() {
        return neidanListener;
    }
    
    // ==================== 内丹管理 ====================
    
    /**
     * 更新玩家内丹显示
     */
    public void updateNeidan(Player player) {
        if (neidanListener != null) {
            neidanListener.getNeidanManager().updateNeidan(player);
        }
    }
}