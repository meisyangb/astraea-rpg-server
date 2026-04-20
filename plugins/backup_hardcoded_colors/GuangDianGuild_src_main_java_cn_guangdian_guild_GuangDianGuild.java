package cn.guangdian.guild;

import cn.guangdian.guild.adapter.GuildServiceAdapter;
import cn.guangdian.rpgcore.message.UnifiedMessageService;
import cn.guangdian.rpgcore.plugin.AbstractRPGPlugin;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.TabCompleter;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * 光点公会插件主类
 *
 * <p>基于 RPGCore 微服务架构的公会管理系统，支持：</p>
 * <ul>
 *   <li>公会创建与管理</li>
 *   <li>成员邀请与权限控制</li>
 *   <li>公会聊天频道</li>
 *   <li>公会数据持久化</li>
 *   <li>PlaceholderAPI 支持</li>
 * </ul>
 *
 * <h3>版本历史：</h3>
 * <ul>
 *   <li><b>2026-04-14</b> - v1.1.0: 迁移到 MiniMessage，使用 RPGCore 消息服务</li>
 *   <li><b>2025-04</b> - v1.0.0: 初始版本发布</li>
 * </ul>
 *
 * <h3>技术栈：</h3>
 * <ul>
 *   <li>Paper 1.21.6</li>
 *   <li>RPGCore 微服务架构</li>
 *   <li>Adventure MiniMessage API</li>
 *   <li>ConcurrentHashMap 线程安全</li>
 * </ul>
 *
 * @author GuangDian
 * @version 1.1.0
 * @since 2025-04
 * @see AbstractRPGPlugin
 * @see MiniMessageService
 */
public class GuangDianGuild extends AbstractRPGPlugin implements Listener, CommandExecutor, TabCompleter {
    private static GuangDianGuild instance;
    private FileConfiguration config;
    private Map<String, Guild> guilds;
    private Map<String, Guild> playerGuilds;
    private Map<String, GuildInvite> pendingInvites;
    
    private GuildServiceAdapter serviceAdapter;
    private UnifiedMessageService msg;

    /**
     * 插件启用时调用
     *
     * <p>初始化流程：</p>
     * <ol>
     *   <li>初始化 MiniMessage 服务（用于消息颜色处理）</li>
     *   <li>初始化公会数据存储</li>
     *   <li>加载公会数据</li>
     *   <li>注册命令处理器</li>
     *   <li>注册事件监听器</li>
     *   <li>注册 PlaceholderAPI 扩展</li>
     *   <li>注册 RPGCore 服务</li>
     * </ol>
     *
     * @since 1.0.0
     * @see MiniMessageService#getInstance()
     * @see #loadGuilds()
     */
    @Override
    protected void onPluginEnable() {
        instance = this;

        // 初始化 UnifiedMessageService 用于消息颜色处理
        msg = UnifiedMessageService.getInstance();

        guilds = new ConcurrentHashMap<>();
        playerGuilds = new ConcurrentHashMap<>();
        pendingInvites = new ConcurrentHashMap<>();
        saveDefaultConfig();
        config = getConfig();
        loadGuilds();
        getCommand("guild").setExecutor(this);
        getCommand("guild").setTabCompleter(this);
        getCommand("guildadmin").setExecutor(this);
        getServer().getPluginManager().registerEvents(this, this);
        if (getServer().getPluginManager().getPlugin("PlaceholderAPI") != null) {
            new GuildPlaceholder(this).register();
            getLogger().info("已注册PlaceholderAPI扩展!");
        }
        
        serviceAdapter = new GuildServiceAdapter(this);
        if (serviceAdapter.isUsingRPGCore()) {
            getLogger().info("已集成 RPGCore 服务系统!");
        }
        
        getLogger().info("GuangDianGuild 工会插件已启用!");
    }

    @Override
    protected void onPluginDisable() {
        // 取消所有调度任务
        cancelAllTasks();
        
        if (serviceAdapter != null) {
            serviceAdapter.unregister();
        }
        
        saveGuilds();
        getLogger().info("GuangDianGuild 工会插件已禁用!");
    }

    @Override
    protected String getPluginName() {
        return "GuangDianGuild";
    }

    public static GuangDianGuild getInstance() {
        return instance;
    }

    private void loadGuilds() {
        guilds.clear();
        playerGuilds.clear();
        for (String key : config.getKeys(false)) {
            if (key.equals("settings") || key.equals("messages")) continue;
            Map<String, Object> data = config.getConfigurationSection(key).getValues(false);
            if (data != null) {
                Guild guild = Guild.deserialize(data);
                guilds.put(key.toLowerCase(), guild);
                for (String memberName : guild.members.keySet()) {
                    playerGuilds.put(memberName.toLowerCase(), guild);
                }
            }
        }
        getLogger().info("已加载 " + guilds.size() + " 个工会!");
    }

    private void saveGuilds() {
        for (String key : config.getKeys(false)) {
            if (!key.equals("settings") && !key.equals("messages")) {
                config.set(key, null);
            }
        }
        for (Map.Entry<String, Guild> entry : guilds.entrySet()) {
            config.set(entry.getKey(), entry.getValue().serialize());
        }
        saveConfig();
    }

    public boolean createGuild(String name, Player leader) {
        if (guilds.containsKey(name.toLowerCase())) return false;
        if (playerGuilds.containsKey(leader.getName().toLowerCase())) return false;
        Guild guild = new Guild(name, leader.getName());
        guilds.put(name.toLowerCase(), guild);
        playerGuilds.put(leader.getName().toLowerCase(), guild);
        saveGuilds();
        return true;
    }

    public boolean disbandGuild(String guildName) {
        Guild guild = guilds.remove(guildName.toLowerCase());
        if (guild == null) return false;
        for (String memberName : guild.members.keySet()) {
            playerGuilds.remove(memberName.toLowerCase());
        }
        config.set(guildName.toLowerCase(), null);
        saveConfig();
        return true;
    }

    public Guild getGuild(String name) {
        return guilds.get(name.toLowerCase());
    }

    public Guild getPlayerGuild(String playerName) {
        return playerGuilds.get(playerName.toLowerCase());
    }

    public boolean isInGuild(String playerName) {
        return playerGuilds.containsKey(playerName.toLowerCase());
    }

    public boolean joinGuild(String guildName, Player player) {
        Guild guild = guilds.get(guildName.toLowerCase());
        if (guild == null || playerGuilds.containsKey(player.getName().toLowerCase())) return false;
        int maxMembers = config.getInt("settings.max-members", 50);
        if (guild.members.size() >= maxMembers) return false;
        guild.members.put(player.getName(), new GuildMember(player.getName(), GuildRank.RECRUIT));
        guild.invites.remove(player.getName());
        playerGuilds.put(player.getName().toLowerCase(), guild);
        saveGuilds();
        return true;
    }

    public boolean leaveGuild(String playerName) {
        Guild guild = playerGuilds.remove(playerName.toLowerCase());
        if (guild == null) return false;
        guild.members.remove(playerName);
        if (guild.members.isEmpty() || guild.leader.equalsIgnoreCase(playerName)) {
            disbandGuild(guild.name);
        } else {
            saveGuilds();
        }
        return true;
    }

    public boolean kickMember(String kickerName, String targetName) {
        Guild guild = playerGuilds.get(kickerName.toLowerCase());
        if (guild == null) return false;
        GuildMember kicker = guild.members.get(kickerName);
        GuildMember target = guild.members.get(targetName);
        if (target == null || !kicker.rank.isHigherThan(target.rank)) return false;
        guild.members.remove(targetName);
        playerGuilds.remove(targetName.toLowerCase());
        saveGuilds();
        return true;
    }

    public boolean promoteMember(String promoterName, String targetName) {
        Guild guild = playerGuilds.get(promoterName.toLowerCase());
        if (guild == null) return false;
        GuildMember promoter = guild.members.get(promoterName);
        GuildMember target = guild.members.get(targetName);
        if (target == null || !promoter.rank.isAtLeast(GuildRank.VICE_LEADER)) return false;
        GuildRank newRank = target.rank.promote();
        if (newRank == target.rank) return false;
        target.rank = newRank;
        saveGuilds();
        return true;
    }

    public boolean demoteMember(String demoterName, String targetName) {
        Guild guild = playerGuilds.get(demoterName.toLowerCase());
        if (guild == null) return false;
        GuildMember demoter = guild.members.get(demoterName);
        GuildMember target = guild.members.get(targetName);
        if (target == null || !demoter.rank.isHigherThan(target.rank)) return false;
        GuildRank newRank = target.rank.demote();
        if (newRank == target.rank) return false;
        target.rank = newRank;
        saveGuilds();
        return true;
    }

    public void invitePlayer(String inviterName, String targetName) {
        Guild guild = playerGuilds.get(inviterName.toLowerCase());
        if (guild == null) return;
        guild.invites.add(targetName);
        pendingInvites.put(targetName.toLowerCase(), new GuildInvite(guild.name, inviterName, System.currentTimeMillis()));
        saveGuilds();
    }

    public GuildInvite getPendingInvite(String playerName) {
        return pendingInvites.get(playerName.toLowerCase());
    }

    public void removePendingInvite(String playerName) {
        GuildInvite invite = pendingInvites.remove(playerName.toLowerCase());
        if (invite != null) {
            Guild guild = guilds.get(invite.guildName.toLowerCase());
            if (guild != null) guild.invites.remove(playerName);
        }
    }

    public boolean acceptInvite(String playerName) {
        GuildInvite invite = pendingInvites.remove(playerName.toLowerCase());
        if (invite == null) return false;
        Guild guild = guilds.get(invite.guildName.toLowerCase());
        if (guild == null) return false;
        guild.invites.remove(playerName);
        return joinGuild(guild.name, getServer().getPlayer(playerName));
    }

    public void setGuildPrefix(String guildName, String prefix) {
        Guild guild = guilds.get(guildName.toLowerCase());
        if (guild != null) {
            guild.prefix = prefix;
            saveGuilds();
        }
    }

    public void setGuildDescription(String guildName, String description) {
        Guild guild = guilds.get(guildName.toLowerCase());
        if (guild != null) {
            guild.description = description;
            saveGuilds();
        }
    }

    public Collection<Guild> getAllGuilds() {
        return guilds.values();
    }

    public int getGuildCount() {
        return guilds.size();
    }

    /**
     * 获取消息组件（带前缀）
     *
     * <p>从配置中读取消息，并添加前缀，然后使用 MiniMessage 解析颜色代码。</p>
     *
     * <p>示例：</p>
     * <pre>{@code
     * // 配置: messages.prefix: "&6[工会] &f"
     * // 配置: messages.no-guild: "&c你还没有加入公会!"
     * player.sendMessage(getMsg("no-guild"));
     * // 输出: [工会] 你还没有加入公会!（带颜色）
     * }</pre>
     *
     * @param key 消息配置键（不包含 messages. 前缀）
     * @return 解析后的 Adventure Component
     * @since 1.0.0
     * @see #colorize(String)
     * @see MiniMessage#miniMessage()
     */
    public Component getMsg(String key) {
        String prefix = config.getString("messages.prefix", "&6[工会] &f");
        String message = config.getString("messages." + key, "");
        return this.msg.colorize(prefix + message);
    }

    /**
     * 获取消息组件（带前缀和占位符替换）
     *
     * <p>从配置中读取消息，替换占位符，添加前缀，然后使用 MiniMessage 解析颜色代码。</p>
     *
     * <p>示例：</p>
     * <pre>{@code
     * // 配置: messages.prefix: "&6[工会] &f"
     * // 配置: messages.join-success: "&a你已成功加入 {guild} 公会!"
     * player.sendMessage(getMsg("join-success", "guild", "勇者联盟"));
     * // 输出: [工会] 你已成功加入 勇者联盟 公会!（带颜色）
     * }</pre>
     *
     * @param key 消息配置键（不包含 messages. 前缀）
     * @param placeholders 占位符键值对（格式: key1, value1, key2, value2...）
     * @return 解析后的 Adventure Component
     * @since 1.0.0
     * @see #colorize(String)
     * @see MiniMessage#miniMessage()
     */
    public Component getMsg(String key, String... placeholders) {
        String prefix = config.getString("messages.prefix", "&6[工会] &f");
        String message = config.getString("messages." + key, "");
        String fullMsg = prefix + message;
        for (int i = 0; i < placeholders.length; i += 2) {
            if (i + 1 < placeholders.length) {
                fullMsg = fullMsg.replace("{" + placeholders[i] + "}", placeholders[i + 1]);
            }
        }
        return this.msg.colorize(fullMsg);
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length == 0) {
            sendHelp(sender);
            return true;
        }
        String sub = args[0].toLowerCase();
        if (command.getName().equalsIgnoreCase("guildadmin")) {
            return handleAdmin(sender, sub, args);
        }
        switch (sub) {
            case "create": return handleCreate(sender, args);
            case "disband": return handleDisband(sender);
            case "join": return handleJoin(sender, args);
            case "leave": return handleLeave(sender);
            case "invite": return handleInvite(sender, args);
            case "accept": return handleAccept(sender);
            case "deny": return handleDeny(sender);
            case "kick": return handleKick(sender, args);
            case "promote": return handlePromote(sender, args);
            case "demote": return handleDemote(sender, args);
            case "info": return handleInfo(sender, args);
            case "list": return handleList(sender);
            case "chat": return handleChat(sender, args);
            case "setprefix": return handleSetPrefix(sender, args);
            case "setdesc": return handleSetDesc(sender, args);
            default: sendHelp(sender); return true;
        }
    }

    private boolean handleCreate(CommandSender sender, String[] args) {
        if (!(sender instanceof Player)) { sender.sendMessage(getMsg("player-only")); return true; }
        Player p = (Player) sender;
        if (!p.hasPermission("guangdian.guild.create")) { p.sendMessage(getMsg("no-permission")); return true; }
        if (args.length < 2) { p.sendMessage(Component.text("用法: /guild create <名称>").color(NamedTextColor.RED)); return true; }
        String name = String.join(" ", Arrays.copyOfRange(args, 1, args.length));
        int min = config.getInt("settings.min-name-length", 2);
        int max = config.getInt("settings.max-name-length", 10);
        if (name.length() < min || name.length() > max) { p.sendMessage(getMsg("invalid-guild-name")); return true; }
        if (isInGuild(p.getName())) { p.sendMessage(getMsg("already-in-guild")); return true; }
        if (getGuild(name) != null) { p.sendMessage(getMsg("guild-name-exists")); return true; }
        if (createGuild(name, p)) {
            p.sendMessage(getMsg("guild-created", "guild", name));
            getLogger().info(p.getName() + " 创建了工会: " + name);
        }
        return true;
    }

    private boolean handleDisband(CommandSender sender) {
        if (!(sender instanceof Player)) { sender.sendMessage(getMsg("player-only")); return true; }
        Player p = (Player) sender;
        Guild guild = getPlayerGuild(p.getName());
        if (guild == null) { p.sendMessage(getMsg("not-in-guild")); return true; }
        if (!guild.leader.equalsIgnoreCase(p.getName())) { p.sendMessage(getMsg("not-leader")); return true; }
        String name = guild.name;
        disbandGuild(name);
        p.sendMessage(getMsg("guild-disbanded"));
        getLogger().info(p.getName() + " 解散了工会: " + name);
        return true;
    }

    private boolean handleJoin(CommandSender sender, String[] args) {
        if (!(sender instanceof Player)) { sender.sendMessage(getMsg("player-only")); return true; }
        Player p = (Player) sender;
        if (args.length < 2) { p.sendMessage(Component.text("用法: /guild join <名称>").color(NamedTextColor.RED)); return true; }
        String name = String.join(" ", Arrays.copyOfRange(args, 1, args.length));
        if (isInGuild(p.getName())) { p.sendMessage(getMsg("already-in-guild")); return true; }
        Guild guild = getGuild(name);
        if (guild == null) { p.sendMessage(getMsg("guild-not-found")); return true; }
        if (!guild.invites.contains(p.getName())) { p.sendMessage(Component.text("你没有被邀请加入这个工会!").color(NamedTextColor.RED)); return true; }
        if (joinGuild(name, p)) p.sendMessage(getMsg("guild-joined", "guild", name));
        else p.sendMessage(getMsg("max-members-reached"));
        return true;
    }

    private boolean handleLeave(CommandSender sender) {
        if (!(sender instanceof Player)) { sender.sendMessage(getMsg("player-only")); return true; }
        Player p = (Player) sender;
        Guild guild = getPlayerGuild(p.getName());
        if (guild == null) { p.sendMessage(getMsg("not-in-guild")); return true; }
        String name = guild.name;
        leaveGuild(p.getName());
        p.sendMessage(getMsg("guild-left", "guild", name));
        return true;
    }

    private boolean handleInvite(CommandSender sender, String[] args) {
        if (!(sender instanceof Player)) { sender.sendMessage(getMsg("player-only")); return true; }
        Player p = (Player) sender;
        if (args.length < 2) { p.sendMessage(Component.text("用法: /guild invite <玩家>").color(NamedTextColor.RED)); return true; }
        String target = args[1];
        Player targetP = getServer().getPlayer(target);
        Guild guild = getPlayerGuild(p.getName());
        if (guild == null) { p.sendMessage(getMsg("not-in-guild")); return true; }
        if (targetP == null) { p.sendMessage(Component.text("玩家不在线!").color(NamedTextColor.RED)); return true; }
        if (isInGuild(target)) { p.sendMessage(Component.text("该玩家已在工会中!").color(NamedTextColor.RED)); return true; }
        invitePlayer(p.getName(), target);
        p.sendMessage(getMsg("invite-sent", "player", target));
        targetP.sendMessage(getMsg("invite-received", "player", p.getName(), "guild", guild.name));
        targetP.sendMessage(Component.text("输入 /guild accept 接受 或 /guild deny 拒绝").color(NamedTextColor.YELLOW));
        return true;
    }

    private boolean handleAccept(CommandSender sender) {
        if (!(sender instanceof Player)) { sender.sendMessage(getMsg("player-only")); return true; }
        Player p = (Player) sender;
        GuildInvite invite = getPendingInvite(p.getName());
        if (invite == null || invite.isExpired()) { removePendingInvite(p.getName()); p.sendMessage(getMsg("no-pending-invite")); return true; }
        if (acceptInvite(p.getName())) p.sendMessage(getMsg("invite-accepted"));
        else p.sendMessage(Component.text("加入工会失败!").color(NamedTextColor.RED));
        return true;
    }

    private boolean handleDeny(CommandSender sender) {
        if (!(sender instanceof Player)) { sender.sendMessage(getMsg("player-only")); return true; }
        Player p = (Player) sender;
        removePendingInvite(p.getName());
        p.sendMessage(getMsg("invite-denied"));
        return true;
    }

    private boolean handleKick(CommandSender sender, String[] args) {
        if (!(sender instanceof Player)) { sender.sendMessage(getMsg("player-only")); return true; }
        Player p = (Player) sender;
        if (args.length < 2) { p.sendMessage(Component.text("用法: /guild kick <玩家>").color(NamedTextColor.RED)); return true; }
        String target = args[1];
        if (kickMember(p.getName(), target)) {
            p.sendMessage(getMsg("player-kicked", "player", target));
            Player targetP = getServer().getPlayer(target);
            if (targetP != null) targetP.sendMessage(getMsg("kicked"));
        } else p.sendMessage(Component.text("无法踢出该成员!").color(NamedTextColor.RED));
        return true;
    }

    private boolean handlePromote(CommandSender sender, String[] args) {
        if (!(sender instanceof Player)) { sender.sendMessage(getMsg("player-only")); return true; }
        Player p = (Player) sender;
        if (args.length < 2) { p.sendMessage(Component.text("用法: /guild promote <玩家>").color(NamedTextColor.RED)); return true; }
        String target = args[1];
        if (promoteMember(p.getName(), target)) {
            Guild guild = getPlayerGuild(p.getName());
            GuildMember targetM = guild.members.get(target);
            p.sendMessage(getMsg("player-promoted", "player", target, "rank", targetM.rank.getDisplayName()));
            Player targetP = getServer().getPlayer(target);
            if (targetP != null) targetP.sendMessage(getMsg("promoted", "rank", targetM.rank.getDisplayName()));
        } else p.sendMessage(Component.text("无法晋升该成员!").color(NamedTextColor.RED));
        return true;
    }

    private boolean handleDemote(CommandSender sender, String[] args) {
        if (!(sender instanceof Player)) { sender.sendMessage(getMsg("player-only")); return true; }
        Player p = (Player) sender;
        if (args.length < 2) { p.sendMessage(Component.text("用法: /guild demote <玩家>").color(NamedTextColor.RED)); return true; }
        String target = args[1];
        if (demoteMember(p.getName(), target)) {
            Guild guild = getPlayerGuild(p.getName());
            GuildMember targetM = guild.members.get(target);
            p.sendMessage(getMsg("player-demoted", "player", target, "rank", targetM.rank.getDisplayName()));
            Player targetP = getServer().getPlayer(target);
            if (targetP != null) targetP.sendMessage(getMsg("demoted", "rank", targetM.rank.getDisplayName()));
        } else p.sendMessage(Component.text("无法降职该成员!").color(NamedTextColor.RED));
        return true;
    }

    private boolean handleInfo(CommandSender sender, String[] args) {
        Guild guild;
        if (args.length >= 2) {
            String name = String.join(" ", Arrays.copyOfRange(args, 1, args.length));
            guild = getGuild(name);
        } else if (sender instanceof Player) {
            guild = getPlayerGuild(((Player) sender).getName());
        } else {
            sender.sendMessage(Component.text("用法: /guild info <工会名>").color(NamedTextColor.RED)); return true;
        }
        if (guild == null) { sender.sendMessage(getMsg("guild-not-found")); return true; }
        sender.sendMessage(Component.text("========== 工会信息 ==========").color(NamedTextColor.GOLD));
        sender.sendMessage(Component.text("工会名称: ").color(NamedTextColor.YELLOW).append(Component.text(guild.name).color(NamedTextColor.WHITE)));
        sender.sendMessage(Component.text("工会前缀: ").color(NamedTextColor.YELLOW).append(Component.text(guild.prefix.isEmpty() ? "无" : guild.prefix).color(NamedTextColor.WHITE)));
        sender.sendMessage(Component.text("工会描述: ").color(NamedTextColor.YELLOW).append(Component.text(guild.description.isEmpty() ? "无" : guild.description).color(NamedTextColor.WHITE)));
        sender.sendMessage(Component.text("会长: ").color(NamedTextColor.YELLOW).append(Component.text(guild.leader).color(NamedTextColor.WHITE)));
        sender.sendMessage(Component.text("成员数量: ").color(NamedTextColor.YELLOW).append(Component.text(String.valueOf(guild.members.size())).color(NamedTextColor.WHITE)));
        sender.sendMessage(Component.text("==============================").color(NamedTextColor.GOLD));
        return true;
    }

    private boolean handleList(CommandSender sender) {
        sender.sendMessage(Component.text("========== 工会列表 ==========").color(NamedTextColor.GOLD));
        for (Guild guild : getAllGuilds()) {
            sender.sendMessage(Component.text(guild.name).color(NamedTextColor.YELLOW)
                .append(Component.text(" - 成员: " + guild.members.size()).color(NamedTextColor.GRAY)));
        }
        sender.sendMessage(Component.text("总计: ").color(NamedTextColor.GOLD)
            .append(Component.text(getGuildCount() + " 个工会").color(NamedTextColor.WHITE)));
        sender.sendMessage(Component.text("==============================").color(NamedTextColor.GOLD));
        return true;
    }

    private boolean handleChat(CommandSender sender, String[] args) {
        if (!(sender instanceof Player)) { sender.sendMessage(getMsg("player-only")); return true; }
        Player p = (Player) sender;
        Guild guild = getPlayerGuild(p.getName());
        if (guild == null) { p.sendMessage(getMsg("not-in-guild")); return true; }
        if (args.length < 2) { p.sendMessage(Component.text("用法: /guild chat <消息>").color(NamedTextColor.RED)); return true; }
        String message = String.join(" ", Arrays.copyOfRange(args, 1, args.length));
        String chatPrefix = config.getString("settings.chat-prefix", "&7[&6工会&7] ");
        Component formatted = this.msg.colorize(chatPrefix)
            .append(Component.text(p.getName() + ": ").color(NamedTextColor.YELLOW))
            .append(Component.text(message).color(NamedTextColor.WHITE));
        for (String member : guild.members.keySet()) {
            Player memberP = getServer().getPlayer(member);
            if (memberP != null) memberP.sendMessage(formatted);
        }
        return true;
    }

    private boolean handleSetPrefix(CommandSender sender, String[] args) {
        if (!(sender instanceof Player)) { sender.sendMessage(getMsg("player-only")); return true; }
        Player p = (Player) sender;
        Guild guild = getPlayerGuild(p.getName());
        if (guild == null) { p.sendMessage(getMsg("not-in-guild")); return true; }
        GuildMember member = guild.members.get(p.getName());
        if (!member.rank.isAtLeast(GuildRank.VICE_LEADER)) { p.sendMessage(getMsg("no-permission")); return true; }
        if (args.length < 2) { p.sendMessage(Component.text("用法: /guild setprefix <前缀>").color(NamedTextColor.RED)); return true; }
        String prefix = args[1];
        setGuildPrefix(guild.name, prefix);
        p.sendMessage(getMsg("prefix-set", "prefix", prefix));
        return true;
    }

    private boolean handleSetDesc(CommandSender sender, String[] args) {
        if (!(sender instanceof Player)) { sender.sendMessage(getMsg("player-only")); return true; }
        Player p = (Player) sender;
        Guild guild = getPlayerGuild(p.getName());
        if (guild == null) { p.sendMessage(getMsg("not-in-guild")); return true; }
        GuildMember member = guild.members.get(p.getName());
        if (!member.rank.isAtLeast(GuildRank.VICE_LEADER)) { p.sendMessage(getMsg("no-permission")); return true; }
        if (args.length < 2) { p.sendMessage(Component.text("用法: /guild setdesc <描述>").color(NamedTextColor.RED)); return true; }
        String desc = String.join(" ", Arrays.copyOfRange(args, 1, args.length));
        setGuildDescription(guild.name, desc);
        p.sendMessage(getMsg("desc-set"));
        return true;
    }

    private boolean handleAdmin(CommandSender sender, String sub, String[] args) {
        if (!sender.hasPermission("guangdian.guild.admin")) { sender.sendMessage(getMsg("no-permission")); return true; }
        switch (sub) {
            case "reload":
                reloadConfig();
                config = getConfig();
                loadGuilds();
                sender.sendMessage(Component.text("配置已重新加载!").color(NamedTextColor.GREEN));
                return true;
            case "delete":
                if (args.length < 2) { sender.sendMessage(Component.text("用法: /guildadmin delete <工会名>").color(NamedTextColor.RED)); return true; }
                String name = String.join(" ", Arrays.copyOfRange(args, 1, args.length));
                if (disbandGuild(name)) sender.sendMessage(Component.text("工会已删除: " + name).color(NamedTextColor.GREEN));
                else sender.sendMessage(getMsg("guild-not-found"));
                return true;
            default:
                sender.sendMessage(Component.text("用法: /guildadmin <reload|delete>").color(NamedTextColor.RED));
                return true;
        }
    }

    private void sendHelp(CommandSender sender) {
        sender.sendMessage(Component.text("========== 工会帮助 ==========").color(NamedTextColor.GOLD));
        sender.sendMessage(Component.text("/guild create <名称> ").color(NamedTextColor.YELLOW)
            .append(Component.text("- 创建工会").color(NamedTextColor.GRAY)));
        sender.sendMessage(Component.text("/guild disband ").color(NamedTextColor.YELLOW)
            .append(Component.text("- 解散工会").color(NamedTextColor.GRAY)));
        sender.sendMessage(Component.text("/guild invite <玩家> ").color(NamedTextColor.YELLOW)
            .append(Component.text("- 邀请玩家").color(NamedTextColor.GRAY)));
        sender.sendMessage(Component.text("/guild accept ").color(NamedTextColor.YELLOW)
            .append(Component.text("- 接受邀请").color(NamedTextColor.GRAY)));
        sender.sendMessage(Component.text("/guild deny ").color(NamedTextColor.YELLOW)
            .append(Component.text("- 拒绝邀请").color(NamedTextColor.GRAY)));
        sender.sendMessage(Component.text("/guild join <工会> ").color(NamedTextColor.YELLOW)
            .append(Component.text("- 加入工会").color(NamedTextColor.GRAY)));
        sender.sendMessage(Component.text("/guild leave ").color(NamedTextColor.YELLOW)
            .append(Component.text("- 离开工会").color(NamedTextColor.GRAY)));
        sender.sendMessage(Component.text("/guild kick <玩家> ").color(NamedTextColor.YELLOW)
            .append(Component.text("- 踢出成员").color(NamedTextColor.GRAY)));
        sender.sendMessage(Component.text("/guild promote <玩家> ").color(NamedTextColor.YELLOW)
            .append(Component.text("- 晋升成员").color(NamedTextColor.GRAY)));
        sender.sendMessage(Component.text("/guild demote <玩家> ").color(NamedTextColor.YELLOW)
            .append(Component.text("- 降职成员").color(NamedTextColor.GRAY)));
        sender.sendMessage(Component.text("/guild info [工会] ").color(NamedTextColor.YELLOW)
            .append(Component.text("- 查看信息").color(NamedTextColor.GRAY)));
        sender.sendMessage(Component.text("/guild list ").color(NamedTextColor.YELLOW)
            .append(Component.text("- 工会列表").color(NamedTextColor.GRAY)));
        sender.sendMessage(Component.text("/guild chat <消息> ").color(NamedTextColor.YELLOW)
            .append(Component.text("- 工会聊天").color(NamedTextColor.GRAY)));
        sender.sendMessage(Component.text("/guild setprefix <前缀> ").color(NamedTextColor.YELLOW)
            .append(Component.text("- 设置前缀").color(NamedTextColor.GRAY)));
        sender.sendMessage(Component.text("/guild setdesc <描述> ").color(NamedTextColor.YELLOW)
            .append(Component.text("- 设置描述").color(NamedTextColor.GRAY)));
        sender.sendMessage(Component.text("==============================").color(NamedTextColor.GOLD));
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        List<String> list = new ArrayList<>();
        if (command.getName().equalsIgnoreCase("guildadmin")) {
            if (args.length == 1) list.addAll(Arrays.asList("reload", "delete"));
            return list.stream().filter(s -> s.startsWith(args[0].toLowerCase())).collect(Collectors.toList());
        }
        if (args.length == 1) {
            list.addAll(Arrays.asList("create", "disband", "invite", "accept", "deny", "join", "leave", "kick", "promote", "demote", "info", "list", "chat", "setprefix", "setdesc"));
        } else if (args.length == 2) {
            String sub = args[0].toLowerCase();
            if (sub.equals("invite") || sub.equals("kick") || sub.equals("promote") || sub.equals("demote")) {
                for (Player p : getServer().getOnlinePlayers()) list.add(p.getName());
            } else if (sub.equals("join") || sub.equals("info")) {
                for (Guild g : getAllGuilds()) list.add(g.name);
            }
        }
        return list.stream().filter(s -> s.toLowerCase().startsWith(args[args.length - 1].toLowerCase())).collect(Collectors.toList());
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onJoin(PlayerJoinEvent e) {
        Player p = e.getPlayer();
        Guild guild = getPlayerGuild(p.getName());
        if (guild != null) {
            for (String member : guild.members.keySet()) {
                Player m = getServer().getPlayer(member);
                if (m != null && !m.equals(p)) {
                    m.sendMessage(Component.text("[工会] ").color(NamedTextColor.GREEN)
                        .append(Component.text(p.getName() + " 上线了!").color(NamedTextColor.YELLOW)));
                }
            }
        }
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onQuit(PlayerQuitEvent e) {
        Player p = e.getPlayer();
        Guild guild = getPlayerGuild(p.getName());
        if (guild != null) {
            for (String member : guild.members.keySet()) {
                Player m = getServer().getPlayer(member);
                if (m != null && !m.equals(p)) {
                    m.sendMessage(Component.text("[工会] ").color(NamedTextColor.RED)
                        .append(Component.text(p.getName() + " 下线了...").color(NamedTextColor.YELLOW)));
                }
            }
        }
    }

    public static class Guild {
        public String name;
        public String prefix = "";
        public String description = "";
        public String leader;
        public Map<String, GuildMember> members = new ConcurrentHashMap<>();
        List<String> invites = new ArrayList<>();

        public Guild(String name, String leader) {
            this.name = name;
            this.leader = leader;
            this.members.put(leader, new GuildMember(leader, GuildRank.LEADER));
        }

        Map<String, Object> serialize() {
            Map<String, Object> m = new HashMap<>();
            m.put("name", name);
            m.put("prefix", prefix);
            m.put("description", description);
            m.put("leader", leader);
            Map<String, Map<String, Object>> mems = new HashMap<>();
            for (Map.Entry<String, GuildMember> e : members.entrySet()) {
                mems.put(e.getKey(), e.getValue().serialize());
            }
            m.put("members", mems);
            m.put("invites", invites);
            return m;
        }

        static Guild deserialize(Map<String, Object> data) {
            Guild g = new Guild((String) data.get("name"), (String) data.get("leader"));
            g.prefix = (String) data.getOrDefault("prefix", "");
            g.description = (String) data.getOrDefault("description", "");
            
            Object membersObj = data.get("members");
            if (membersObj instanceof Map) {
                Map<?, ?> membersMap = (Map<?, ?>) membersObj;
                for (Map.Entry<?, ?> e : membersMap.entrySet()) {
                    if (e.getKey() instanceof String && e.getValue() instanceof Map) {
                        String memberName = (String) e.getKey();
                        Map<String, Object> memberData = (Map<String, Object>) e.getValue();
                        g.members.put(memberName, GuildMember.deserialize(memberData));
                    }
                }
            }
            
            Object invitesObj = data.get("invites");
             if (invitesObj instanceof List) {
                 List<?> invitesList = (List<?>) invitesObj;
                 g.invites = new ArrayList<>();
                 for (Object item : invitesList) {
                     if (item instanceof String) {
                         g.invites.add((String) item);
                     }
                 }
             }
             return g;
        }
    }

    public static class GuildMember {
        public String name;
        public GuildRank rank;
        public long joinTime = System.currentTimeMillis();
        public int contribution = 0;

        public GuildMember(String name, GuildRank rank) {
            this.name = name;
            this.rank = rank;
        }

        Map<String, Object> serialize() {
            Map<String, Object> m = new HashMap<>();
            m.put("name", name);
            m.put("rank", rank.name());
            m.put("joinTime", joinTime);
            m.put("contribution", contribution);
            return m;
        }

        static GuildMember deserialize(Map<String, Object> data) {
            GuildMember m = new GuildMember((String) data.get("name"), GuildRank.valueOf((String) data.get("rank")));
            m.joinTime = ((Number) data.getOrDefault("joinTime", System.currentTimeMillis())).longValue();
            m.contribution = ((Number) data.getOrDefault("contribution", 0)).intValue();
            return m;
        }
    }

    public enum GuildRank {
        LEADER("会长", 5),
        VICE_LEADER("副会长", 4),
        ELDER("长老", 3),
        MEMBER("成员", 2),
        RECRUIT("新成员", 1);

        private final String displayName;
        private final int priority;

        GuildRank(String displayName, int priority) {
            this.displayName = displayName;
            this.priority = priority;
        }

        public String getDisplayName() { return displayName; }
        public int getPriority() { return priority; }
        public boolean isHigherThan(GuildRank other) { return this.priority > other.priority; }
        public boolean isAtLeast(GuildRank other) { return this.priority >= other.priority; }

        public GuildRank promote() {
            switch (this) {
                case RECRUIT: return MEMBER;
                case MEMBER: return ELDER;
                case ELDER: return VICE_LEADER;
                default: return this;
            }
        }

        public GuildRank demote() {
            switch (this) {
                case VICE_LEADER: return ELDER;
                case ELDER: return MEMBER;
                case MEMBER: return RECRUIT;
                default: return this;
            }
        }
    }

    static class GuildInvite {
        String guildName;
        String inviterName;
        long timestamp;

        GuildInvite(String guildName, String inviterName, long timestamp) {
            this.guildName = guildName;
            this.inviterName = inviterName;
            this.timestamp = timestamp;
        }

        boolean isExpired() {
            return System.currentTimeMillis() - timestamp > 60000;
        }
    }

    static class GuildPlaceholder extends me.clip.placeholderapi.expansion.PlaceholderExpansion {
        private final GuangDianGuild plugin;

        GuildPlaceholder(GuangDianGuild plugin) {
            this.plugin = plugin;
        }

        @Override
        public String getIdentifier() { return "gdguild"; }
        @Override
        public String getAuthor() { return "GuangDian"; }
        @Override
        public String getVersion() { return "1.0.0"; }
        @Override
        public boolean persist() { return true; }

        @Override
        public String onRequest(org.bukkit.OfflinePlayer p, String params) {
            if (p == null) return "";
            Guild guild = plugin.getPlayerGuild(p.getName());
            String param = params.toLowerCase();
            if (param.equals("name") || param.equals("工会名") || param.equals("名称")) return guild != null ? guild.name : "无";
            if (param.equals("prefix") || param.equals("前缀")) return guild != null ? guild.prefix : "无";
            if (param.equals("leader") || param.equals("会长")) return guild != null ? guild.leader : "无";
            if (param.equals("members") || param.equals("成员数")) return guild != null ? String.valueOf(guild.members.size()) : "0";
            if (param.equals("in_guild") || param.equals("是否有工会")) return guild != null ? "true" : "false";
            if (param.equals("is_leader") || param.equals("是否会长")) return guild != null && guild.leader.equalsIgnoreCase(p.getName()) ? "true" : "false";
            if (param.equals("rank") || param.equals("职位")) {
                if (guild == null) return "无";
                GuildMember m = guild.members.get(p.getName());
                return m != null ? m.rank.getDisplayName() : "无";
            }
            if (param.equals("total_guilds") || param.equals("工会总数")) return String.valueOf(plugin.getGuildCount());
            return null;
        }
    }
}
