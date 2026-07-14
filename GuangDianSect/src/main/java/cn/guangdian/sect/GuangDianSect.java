package cn.guangdian.sect;

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
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.player.PlayerJoinEvent;

import java.io.File;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 光点门派插件主类
 * 
 * 修仙RPG门派系统，支持：
 * - 6大门派选择（青云宗、合欢宗、天音寺、焚香谷、鬼王宗、长生堂）
 * - 门派职位系统（外门弟子 -> 宗主）
 * - 门派变强系统（各门派独特变强方式）
 * - PlaceholderAPI支持
 * - 门派选择GUI
 */
public class GuangDianSect extends AbstractRPGPlugin implements Listener, CommandExecutor, TabCompleter {
    private static GuangDianSect instance;
    private FileConfiguration config;
    private UnifiedMessageService msg;
    
    // 门派数据
    private final Map<String, Sect> sects = new ConcurrentHashMap<>();
    private final Map<String, SectPlayer> playerData = new ConcurrentHashMap<>();
    
    // 职位数据
    private final Map<String, SectRank> ranks = new LinkedHashMap<>();
    
    // 玩家数据存储
    private PlayerDataManager dataManager;
    
    // GUI
    private SectGUI sectGUI;
    
    // 变强监听器
    private SectPowerListener powerListener;
    
    @Override
    protected void onPluginEnable() {
        instance = this;
        
        msg = UnifiedMessageService.getInstance();
        
        saveDefaultConfig();
        config = getConfig();
        
        loadSects();
        loadRanks();
        
        dataManager = new PlayerDataManager(this);
        dataManager.loadAll();
        
        sectGUI = new SectGUI(this);
        
        // 注册变强监听器
        powerListener = new SectPowerListener(this);
        getServer().getPluginManager().registerEvents(powerListener, this);
        
        getCommand("sect").setExecutor(this);
        getCommand("sect").setTabCompleter(this);
        getCommand("sectadmin").setExecutor(this);
        
        getServer().getPluginManager().registerEvents(this, this);
        
        // 注册PlaceholderAPI
        if (getServer().getPluginManager().getPlugin("PlaceholderAPI") != null) {
            new SectPlaceholder(this).register();
            getLogger().info("已注册PlaceholderAPI扩展!");
        }
        
        getLogger().info("GuangDianSect 门派插件已启用!");
        getLogger().info("已加载 " + sects.size() + " 个门派，" + ranks.size() + " 个职位等级");
    }
    
    @Override
    protected void onPluginDisable() {
        cancelAllTasks();
        dataManager.saveAll();
        getLogger().info("GuangDianSect 门派插件已禁用!");
    }
    
    @Override
    protected String getPluginName() {
        return "GuangDianSect";
    }
    
    public static GuangDianSect getInstance() {
        return instance;
    }
    
    private void loadSects() {
        sects.clear();
        
        // 优先从 sects 文件夹读取宗门配置
        File sectsFolder = new File(getDataFolder(), "sects");
        if (sectsFolder.exists() && sectsFolder.isDirectory()) {
            File[] sectFolders = sectsFolder.listFiles(File::isDirectory);
            if (sectFolders != null) {
                for (File sectFolder : sectFolders) {
                    File configFile = new File(sectFolder, "config.yml");
                    if (configFile.exists()) {
                        try {
                            YamlConfiguration sectConfig = YamlConfiguration.loadConfiguration(configFile);
                            String sectId = sectFolder.getName().toLowerCase();
                            
                            // 使用中文文件夹名作为 key 的拼音/英文版本
                            String sectKey = getSectKey(sectConfig.getString("name", sectFolder.getName()));
                            
                            Sect sect = new Sect(sectKey, sectConfig);
                            sects.put(sectKey.toLowerCase(), sect);
                            getLogger().info("从文件夹加载门派: " + sect.getName());
                        } catch (Exception e) {
                            getLogger().warning("加载门派配置失败: " + sectFolder.getName() + " - " + e.getMessage());
                        }
                    }
                }
            }
        }
        
        // 如果文件夹没有配置，从主配置文件读取
        if (sects.isEmpty()) {
            ConfigurationSection sectSection = config.getConfigurationSection("sects");
            if (sectSection != null) {
                for (String key : sectSection.getKeys(false)) {
                    ConfigurationSection sectData = sectSection.getConfigurationSection(key);
                    if (sectData != null) {
                        Sect sect = new Sect(key, sectData);
                        sects.put(key.toLowerCase(), sect);
                    }
                }
            }
        }
        
        getLogger().info("已加载 " + sects.size() + " 个门派");
    }
    
    /**
     * 获取宗门的英文 key
     */
    private String getSectKey(String name) {
        if (name == null) return "unknown";
        
        // 中文名到英文 key 的映射
        switch (name) {
            case "青云宗": return "qingyun";
            case "合欢宗": return "hehuan";
            case "天音寺": return "tianyin";
            case "焚香谷": return "fenxiang";
            case "鬼王宗": return "guiwang";
            case "长生堂": return "changsheng";
            default: return name.toLowerCase().replace(" ", "_");
        }
    }
    
    private void loadRanks() {
        ranks.clear();
        ConfigurationSection rankSection = config.getConfigurationSection("ranks");
        if (rankSection == null) return;
        
        for (String key : rankSection.getKeys(false)) {
            ConfigurationSection rankData = rankSection.getConfigurationSection(key);
            if (rankData != null) {
                SectRank rank = new SectRank(key, rankData);
                ranks.put(key.toLowerCase(), rank);
            }
        }
    }
    
    // 获取消息
    public Component getMsg(String key) {
        String prefix = config.getString("messages.prefix", "<gold>[宗门] <white>");
        String message = config.getString("messages." + key, "");
        return msg.colorize(prefix + message);
    }
    
    public Component getMsg(String key, String... placeholders) {
        String prefix = config.getString("messages.prefix", "<gold>[宗门] <white>");
        String message = config.getString("messages." + key, "");
        String fullMsg = prefix + message;
        for (int i = 0; i < placeholders.length; i += 2) {
            if (i + 1 < placeholders.length) {
                fullMsg = fullMsg.replace("{" + placeholders[i] + "}", placeholders[i + 1]);
            }
        }
        return msg.colorize(fullMsg);
    }
    
    // 门派操作
    public Sect getSect(String name) {
        return sects.get(name.toLowerCase());
    }
    
    public Collection<Sect> getAllSects() {
        return sects.values();
    }
    
    public SectPlayer getPlayerData(Player player) {
        return playerData.computeIfAbsent(player.getUniqueId().toString(), 
            k -> new SectPlayer(player.getUniqueId().toString()));
    }
    
    public Sect getPlayerSect(Player player) {
        SectPlayer data = getPlayerData(player);
        if (data == null || data.getSectId() == null) return null;
        return sects.get(data.getSectId().toLowerCase());
    }
    
    public boolean isInSect(Player player) {
        SectPlayer data = getPlayerData(player);
        return data != null && data.getSectId() != null;
    }
    
    public boolean joinSect(Player player, String sectId) {
        if (isInSect(player)) return false;
        
        Sect sect = getSect(sectId);
        if (sect == null) return false;
        
        SectPlayer data = getPlayerData(player);
        data.setSectId(sect.getId());
        data.setRankId("waomen_dizi"); // 默认外门弟子
        data.setJoinTime(System.currentTimeMillis());
        data.setContribution(0);
        
        dataManager.save(data);
        return true;
    }
    
    public boolean leaveSect(Player player) {
        if (!isInSect(player)) return false;
        
        if (!config.getBoolean("settings.allow_leave", true)) return false;
        
        SectPlayer data = getPlayerData(player);
        long cooldown = config.getLong("settings.leave_cooldown", 86400) * 1000L;
        
        if (data.getLastLeaveTime() > 0 && 
            System.currentTimeMillis() - data.getLastLeaveTime() < cooldown) {
            return false;
        }
        
        data.setSectId(null);
        data.setRankId(null);
        data.setLastLeaveTime(System.currentTimeMillis());
        
        dataManager.save(data);
        return true;
    }
    
    // 获取职位
    public SectRank getRank(String rankId) {
        return ranks.get(rankId.toLowerCase());
    }
    
    public SectRank getPlayerRank(Player player) {
        SectPlayer data = getPlayerData(player);
        if (data == null || data.getRankId() == null) return null;
        return ranks.get(data.getRankId().toLowerCase());
    }
    
    // 获取下一职位
    public SectRank getNextRank(String currentRankId) {
        boolean foundCurrent = false;
        for (SectRank rank : ranks.values()) {
            if (foundCurrent) return rank;
            if (rank.getId().equalsIgnoreCase(currentRankId)) {
                foundCurrent = true;
            }
        }
        return null;
    }
    
    // 检查并晋升职位
    public void checkAndPromote(Player player) {
        SectPlayer data = getPlayerData(player);
        if (data == null) return;
        
        SectRank currentRank = getRank(data.getRankId());
        if (currentRank == null) return;
        
        SectRank nextRank = getNextRank(data.getRankId());
        if (nextRank == null) return;
        
        if (data.getContribution() >= nextRank.getContributionNeeded()) {
            data.setRankId(nextRank.getId());
            dataManager.save(data);
            player.sendMessage(getMsg("rank-up", "rank", nextRank.getName()));
        }
    }
    
    // 命令处理
    @Override
    public boolean onCommand(org.bukkit.command.CommandSender sender, Command command, String label, String[] args) {
        if (args.length == 0) {
            sendHelp(sender);
            return true;
        }
        
        String sub = args[0].toLowerCase();
        
        if (command.getName().equalsIgnoreCase("sectadmin")) {
            return handleAdmin(sender, sub, args);
        }
        
        switch (sub) {
            case "join": return handleJoin(sender, args);
            case "leave": return handleLeave(sender);
            case "info": return handleInfo(sender, args);
            case "list": return handleList(sender);
            case "gui": return handleGUI(sender);
            case "chat": return handleChat(sender, args);
            case "power": return handlePowerInfo(sender);
            default: sendHelp(sender); return true;
        }
    }
    
    private boolean handleJoin(org.bukkit.command.CommandSender sender, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage(getMsg("player-only"));
            return true;
        }
        Player player = (Player) sender;
        
        if (args.length < 2) {
            player.sendMessage(Component.text("用法: /sect join <门派名>").color(NamedTextColor.RED));
            return true;
        }
        
        String sectName = args[1];
        if (joinSect(player, sectName)) {
            Sect sect = getSect(sectName);
            player.sendMessage(getMsg("join-success", "sect", sect.getName()));
            // 显示变强提示
            sendPowerTip(player, sect);
        } else {
            player.sendMessage(getMsg("already-in-sect"));
        }
        return true;
    }
    
    private void sendPowerTip(Player player, Sect sect) {
        String powerType = sect.getPowerMode().getType();
        String tipKey = switch (powerType) {
            case "enhance_weapon" -> "power-tip-qingyun";
            case "marriage" -> "power-tip-hehuan";
            case "heal" -> "power-tip-tianyin";
            case "damage" -> "power-tip-fenxiang";
            case "kill" -> "power-tip-guiwang";
            case "alchemy" -> "power-tip-changsheng";
            default -> null;
        };
        if (tipKey != null) {
            player.sendMessage(getMsg(tipKey));
        }
    }
    
    private boolean handleLeave(org.bukkit.command.CommandSender sender) {
        if (!(sender instanceof Player)) {
            sender.sendMessage(getMsg("player-only"));
            return true;
        }
        Player player = (Player) sender;
        
        if (!isInSect(player)) {
            player.sendMessage(getMsg("not-in-sect"));
            return true;
        }
        
        Sect sect = getPlayerSect(player);
        if (leaveSect(player)) {
            player.sendMessage(getMsg("leave-success", "sect", sect.getName()));
        } else {
            SectPlayer data = getPlayerData(player);
            long remaining = config.getLong("settings.leave_cooldown", 86400) * 1000L - 
                (System.currentTimeMillis() - data.getLastLeaveTime());
            player.sendMessage(getMsg("leave-cooldown", "time", String.valueOf(remaining / 1000)));
        }
        return true;
    }
    
    private boolean handleInfo(org.bukkit.command.CommandSender sender, String[] args) {
        Sect sect;
        if (args.length >= 2) {
            sect = getSect(args[1]);
        } else if (sender instanceof Player) {
            sect = getPlayerSect((Player) sender);
        } else {
            sender.sendMessage(Component.text("用法: /sect info <门派名>").color(NamedTextColor.RED));
            return true;
        }
        
        if (sect == null) {
            sender.sendMessage(getMsg("sect-not-found"));
            return true;
        }
        
        sender.sendMessage(getMsg("sect-info-title", "sect", sect.getName()));
        sender.sendMessage(getMsg("sect-info-type", "type", sect.getType()));
        sender.sendMessage(getMsg("sect-info-power", "power", sect.getPowerDescription()));
        sender.sendMessage(getMsg("sect-info-desc", "description", sect.getDescription()));
        
        if (sender instanceof Player) {
            Player player = (Player) sender;
            SectPlayer data = getPlayerData(player);
            if (data.getSectId() != null && data.getSectId().equalsIgnoreCase(sect.getId())) {
                sender.sendMessage(getMsg("sect-info-contribution", "contribution", 
                    String.valueOf(data.getContribution())));
            }
        }
        
        int memberCount = 0;
        for (SectPlayer data : playerData.values()) {
            if (data.getSectId() != null && data.getSectId().equalsIgnoreCase(sect.getId())) {
                memberCount++;
            }
        }
        sender.sendMessage(getMsg("sect-info-members", "count", String.valueOf(memberCount)));
        return true;
    }
    
    private boolean handleList(org.bukkit.command.CommandSender sender) {
        sender.sendMessage(Component.text("========== 门派列表 ==========").color(NamedTextColor.GOLD));
        for (Sect sect : getAllSects()) {
            sender.sendMessage(msg.colorize(sect.getColor() + sect.getName() + 
                " <gray>- <white>" + sect.getType() + " · 变强方式: " + sect.getPowerDescription()));
        }
        sender.sendMessage(Component.text("==============================").color(NamedTextColor.GOLD));
        return true;
    }
    
    private boolean handleGUI(org.bukkit.command.CommandSender sender) {
        if (!(sender instanceof Player)) {
            sender.sendMessage(getMsg("player-only"));
            return true;
        }
        Player player = (Player) sender;
        
        if (isInSect(player)) {
            player.sendMessage(getMsg("already-in-sect"));
            return true;
        }
        
        sectGUI.open(player);
        return true;
    }
    
    private boolean handlePowerInfo(org.bukkit.command.CommandSender sender) {
        if (!(sender instanceof Player)) {
            sender.sendMessage(getMsg("player-only"));
            return true;
        }
        Player player = (Player) sender;
        
        if (!isInSect(player)) {
            player.sendMessage(getMsg("not-in-sect"));
            return true;
        }
        
        Sect sect = getPlayerSect(player);
        SectPlayer data = getPlayerData(player);
        SectRank rank = getPlayerRank(player);
        
        player.sendMessage(Component.text("========== 门派实力 ==========").color(NamedTextColor.GOLD));
        player.sendMessage(msg.colorize("<yellow>门派: " + sect.getColor() + sect.getName()));
        player.sendMessage(msg.colorize("<yellow>职位: <white>" + (rank != null ? rank.getName() : "无")));
        player.sendMessage(msg.colorize("<yellow>变强方式: <white>" + sect.getPowerDescription()));
        player.sendMessage(msg.colorize("<yellow>贡献值: <white>" + data.getContribution()));
        
        // 显示下一职位
        SectRank nextRank = getNextRank(data.getRankId());
        if (nextRank != null) {
            int needed = nextRank.getContributionNeeded() - data.getContribution();
            player.sendMessage(msg.colorize("<yellow>下一职位: <white>" + nextRank.getName() + 
                " <gray>(还需 " + needed + " 贡献)"));
        }
        player.sendMessage(Component.text("==============================").color(NamedTextColor.GOLD));
        return true;
    }
    
    private boolean handleChat(org.bukkit.command.CommandSender sender, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage(getMsg("player-only"));
            return true;
        }
        Player player = (Player) sender;
        
        if (!isInSect(player)) {
            player.sendMessage(getMsg("not-in-sect"));
            return true;
        }
        
        if (args.length < 2) {
            player.sendMessage(Component.text("用法: /sect chat <消息>").color(NamedTextColor.RED));
            return true;
        }
        
        String message = String.join(" ", Arrays.copyOfRange(args, 1, args.length));
        Sect sect = getPlayerSect(player);
        SectRank rank = getPlayerRank(player);
        
        String chatPrefix = config.getString("settings.chat_prefix", "<gray>[<gold>宗门<gray>] ");
        Component formatted = msg.colorize(chatPrefix)
            .append(msg.colorize(sect.getColor() + "[" + sect.getName() + "] "))
            .append(Component.text(rank != null ? rank.getName() + " " : ""))
            .append(Component.text(player.getName() + ": ").color(NamedTextColor.YELLOW))
            .append(Component.text(message).color(NamedTextColor.WHITE));
        
        // 发送给同门派玩家
        for (SectPlayer data : playerData.values()) {
            if (data.getSectId() != null && data.getSectId().equalsIgnoreCase(sect.getId())) {
                Player member = Bukkit.getPlayer(UUID.fromString(data.getPlayerId()));
                if (member != null) {
                    member.sendMessage(formatted);
                }
            }
        }
        return true;
    }
    
    private boolean handleAdmin(org.bukkit.command.CommandSender sender, String sub, String[] args) {
        if (!sender.hasPermission("guangdian.sect.admin")) {
            sender.sendMessage(getMsg("no-permission"));
            return true;
        }
        
        switch (sub) {
            case "reload":
                reloadConfig();
                config = getConfig();
                loadSects();
                loadRanks();
                sender.sendMessage(Component.text("配置已重新加载!").color(NamedTextColor.GREEN));
                return true;
            case "setrank":
                if (args.length < 3) {
                    sender.sendMessage(Component.text("用法: /sectadmin setrank <玩家> <职位>").color(NamedTextColor.RED));
                    return true;
                }
                Player target = Bukkit.getPlayer(args[1]);
                if (target == null) {
                    sender.sendMessage(Component.text("玩家不在线!").color(NamedTextColor.RED));
                    return true;
                }
                SectRank rank = getRank(args[2]);
                if (rank == null) {
                    sender.sendMessage(Component.text("职位不存在!").color(NamedTextColor.RED));
                    return true;
                }
                SectPlayer data = getPlayerData(target);
                data.setRankId(rank.getId());
                dataManager.save(data);
                sender.sendMessage(getMsg("rank-set", "player", target.getName(), "rank", rank.getName()));
                return true;
            case "setcontribution":
                if (args.length < 3) {
                    sender.sendMessage(Component.text("用法: /sectadmin setcontribution <玩家> <贡献值>").color(NamedTextColor.RED));
                    return true;
                }
                Player target2 = Bukkit.getPlayer(args[1]);
                if (target2 == null) {
                    sender.sendMessage(Component.text("玩家不在线!").color(NamedTextColor.RED));
                    return true;
                }
                int amount = Integer.parseInt(args[2]);
                SectPlayer data2 = getPlayerData(target2);
                data2.setContribution(amount);
                dataManager.save(data2);
                sender.sendMessage(getMsg("contribution-set", "player", target2.getName(), "amount", String.valueOf(amount)));
                return true;
            default:
                sender.sendMessage(Component.text("用法: /sectadmin <reload|setrank|setcontribution>").color(NamedTextColor.RED));
                return true;
        }
    }
    
    private void sendHelp(org.bukkit.command.CommandSender sender) {
        sender.sendMessage(Component.text("========== 门派帮助 ==========").color(NamedTextColor.GOLD));
        sender.sendMessage(Component.text("/sect join <门派> ").color(NamedTextColor.YELLOW)
            .append(Component.text("- 加入门派").color(NamedTextColor.GRAY)));
        sender.sendMessage(Component.text("/sect leave ").color(NamedTextColor.YELLOW)
            .append(Component.text("- 退出门派").color(NamedTextColor.GRAY)));
        sender.sendMessage(Component.text("/sect info [门派] ").color(NamedTextColor.YELLOW)
            .append(Component.text("- 查看门派信息").color(NamedTextColor.GRAY)));
        sender.sendMessage(Component.text("/sect list ").color(NamedTextColor.YELLOW)
            .append(Component.text("- 门派列表").color(NamedTextColor.GRAY)));
        sender.sendMessage(Component.text("/sect gui ").color(NamedTextColor.YELLOW)
            .append(Component.text("- 打开门派选择界面").color(NamedTextColor.GRAY)));
        sender.sendMessage(Component.text("/sect power ").color(NamedTextColor.YELLOW)
            .append(Component.text("- 查看我的门派实力").color(NamedTextColor.GRAY)));
        sender.sendMessage(Component.text("/sect chat <消息> ").color(NamedTextColor.YELLOW)
            .append(Component.text("- 门派聊天").color(NamedTextColor.GRAY)));
        sender.sendMessage(Component.text("==============================").color(NamedTextColor.GOLD));
    }
    
    @Override
    public List<String> onTabComplete(org.bukkit.command.CommandSender sender, Command command, String alias, String[] args) {
        List<String> list = new ArrayList<>();
        
        if (command.getName().equalsIgnoreCase("sectadmin")) {
            if (args.length == 1) {
                list.addAll(Arrays.asList("reload", "setrank", "setcontribution"));
            } else if (args.length == 2 && (args[0].equalsIgnoreCase("setrank") || args[0].equalsIgnoreCase("setcontribution"))) {
                for (Player p : Bukkit.getOnlinePlayers()) list.add(p.getName());
            } else if (args.length == 3 && args[0].equalsIgnoreCase("setrank")) {
                list.addAll(ranks.keySet());
            }
            return list;
        }
        
        if (args.length == 1) {
            list.addAll(Arrays.asList("join", "leave", "info", "list", "gui", "chat", "power"));
        } else if (args.length == 2) {
            if (args[0].equalsIgnoreCase("join") || args[0].equalsIgnoreCase("info")) {
                for (Sect sect : getAllSects()) list.add(sect.getId());
            }
        }
        
        return list;
    }
    
    // 事件监听
    @EventHandler(priority = EventPriority.MONITOR)
    public void onJoin(PlayerJoinEvent e) {
        Player player = e.getPlayer();
        SectPlayer data = dataManager.load(player.getUniqueId().toString());
        if (data == null) {
            data = new SectPlayer(player.getUniqueId().toString());
        }
        playerData.put(player.getUniqueId().toString(), data);
        
        // 已移除自动打开GUI逻辑，玩家需要手动使用 /sect gui
    }
    
    // GUI点击事件
    @EventHandler(priority = EventPriority.HIGH)
    public void onInventoryClick(InventoryClickEvent e) {
        if (!e.getView().title().equals(msg.colorize(config.getString("settings.gui_title", "选择你的门派")))) {
            return;
        }
        
        e.setCancelled(true);
        
        if (!(e.getWhoClicked() instanceof Player)) return;
        Player player = (Player) e.getWhoClicked();
        
        int slot = e.getRawSlot();
        if (slot < 0 || slot >= 9) return;
        
        int index = 0;
        for (Sect sect : getAllSects()) {
            if (index == slot) {
                if (joinSect(player, sect.getId())) {
                    player.sendMessage(getMsg("join-success", "sect", sect.getName()));
                    sendPowerTip(player, sect);
                    player.closeInventory();
                } else {
                    player.sendMessage(getMsg("already-in-sect"));
                }
                break;
            }
            index++;
        }
    }
    
    // Getter
    public Map<String, SectRank> getRanks() {
        return ranks;
    }
    
    public PlayerDataManager getDataManager() {
        return dataManager;
    }
    
    public SectPowerListener getPowerListener() {
        return powerListener;
    }
}