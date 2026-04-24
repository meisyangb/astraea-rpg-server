package cn.guangdian.guild;

import cn.guangdian.guild.adapter.GuildServiceAdapter;
import cn.guangdian.guild.command.GuildAdminCommand;
import cn.guangdian.guild.command.GuildCommand;
import cn.guangdian.guild.command.LegacyCommandExecutor;
import cn.guangdian.guild.command.LegacyTabCompleter;
import cn.guangdian.guild.command.LegacyAdminCommandExecutor;
import cn.guangdian.guild.command.LegacyAdminTabCompleter;
import cn.guangdian.rpgcore.RPGCore;
import cn.guangdian.rpgcore.command.CommandFramework;
import cn.guangdian.rpgcore.message.MessageServiceImpl;
import cn.guangdian.rpgcore.plugin.AbstractRPGPlugin;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

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
 *   <li><b>2026-04-24</b> - v1.2.0: 迁移到 RPGCore CommandFramework</li>
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
 * @version 1.2.0
 * @since 2025-04
 * @see AbstractRPGPlugin
 * @see MiniMessageService
 */
public class GuangDianGuild extends AbstractRPGPlugin implements Listener {
    private static GuangDianGuild instance;
    private FileConfiguration config;
    private Map<String, Guild> guilds;
    private Map<String, Guild> playerGuilds;
    private Map<String, GuildInvite> pendingInvites;

    private GuildServiceAdapter serviceAdapter;
    private MessageServiceImpl msg;

    // 命令系统
    private GuildCommand guildCommand;
    private GuildAdminCommand guildAdminCommand;
    private boolean usingCommandFramework = false;

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

        // 初始化 MessageServiceImpl 用于消息颜色处理
        msg = MessageServiceImpl.getInstance();

        guilds = new ConcurrentHashMap<>();
        playerGuilds = new ConcurrentHashMap<>();
        pendingInvites = new ConcurrentHashMap<>();
        saveDefaultConfig();
        config = getConfig();
        loadGuilds();

        // 注册命令系统
        registerCommands();

        getServer().getPluginManager().registerEvents(this, this);
        if (getServer().getPluginManager().getPlugin("PlaceholderAPI") != null) {
            new GuildPlaceholder(this).register();
            getLogger().info("已注册PlaceholderAPI扩展!");
        }

        serviceAdapter = new GuildServiceAdapter(this);
        if (serviceAdapter.isUsingRPGCore()) {
            getLogger().info("已集成 RPGCore 服务系统!");
        }

        getLogger().info("GuangDianGuild 工会插件已启用! 版本: 1.2.0");
        logCommandSystemStatus();
    }

    @Override
    protected void onPluginDisable() {
        // 取消所有调度任务
        cancelAllTasks();

        // 注销 CommandFramework 命令
        if (usingCommandFramework) {
            unregisterCommandFramework();
        }

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

    /**
     * 注册命令系统
     */
    private void registerCommands() {
        // 优先使用 CommandFramework
        if (registerCommandFramework()) {
            getLogger().info("命令系统已注册到 RPGCore CommandFramework");
        } else {
            // 降级到传统命令处理
            getCommand("guild").setExecutor(new LegacyCommandExecutor(this));
            getCommand("guild").setTabCompleter(new LegacyTabCompleter(this));
            getCommand("guildadmin").setExecutor(new LegacyAdminCommandExecutor(this));
            getCommand("guildadmin").setTabCompleter(new LegacyAdminTabCompleter(this));
            getLogger().warning("RPGCore CommandFramework 不可用，使用传统命令处理");
        }
    }

    /**
     * 注册 CommandFramework 命令系统
     * @return 是否成功注册
     */
    private boolean registerCommandFramework() {
        try {
            RPGCore rpgCore = RPGCore.getInstance();
            if (rpgCore == null) {
                getLogger().warning("RPGCore 未初始化，无法使用 CommandFramework");
                return false;
            }

            CommandFramework framework = CommandFramework.getInstance();
            if (framework == null) {
                getLogger().warning("CommandFramework 不可用");
                return false;
            }

            guildCommand = new GuildCommand(this);
            guildAdminCommand = new GuildAdminCommand(this);
            framework.registerCommand(guildCommand);
            framework.registerCommand(guildAdminCommand);
            usingCommandFramework = true;
            return true;
        } catch (Exception e) {
            getLogger().warning("注册 CommandFramework 失败: " + e.getMessage());
            return false;
        }
    }

    /**
     * 注销 CommandFramework 命令
     */
    private void unregisterCommandFramework() {
        try {
            CommandFramework framework = CommandFramework.getInstance();
            if (framework != null) {
                framework.unregisterCommand("guild");
                framework.unregisterCommand("guildadmin");
                getLogger().info("已从 CommandFramework 注销命令");
            }
        } catch (Exception e) {
            getLogger().warning("注销 CommandFramework 命令失败: " + e.getMessage());
        }
    }

    /**
     * 输出命令系统状态
     */
    private void logCommandSystemStatus() {
        getLogger().info("========== 命令系统状态 ==========");
        getLogger().info("命令系统: " + (usingCommandFramework ? "CommandFramework" : "传统Bukkit"));
        getLogger().info("==================================");
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
     * @param key 消息配置键（不包含 messages. 前缀）
     * @return 解析后的 Adventure Component
     */
    public Component getMsg(String key) {
        String prefix = config.getString("messages.prefix", "<gold>[工会] <white>");
        String message = config.getString("messages." + key, "");
        return this.msg.colorize(prefix + message);
    }

    /**
     * 获取消息组件（带前缀和占位符替换）
     *
     * @param key 消息配置键（不包含 messages. 前缀）
     * @param placeholders 占位符键值对（格式: key1, value1, key2, value2...）
     * @return 解析后的 Adventure Component
     */
    public Component getMsg(String key, String... placeholders) {
        String prefix = config.getString("messages.prefix", "<gold>[工会] <white>");
        String message = config.getString("messages." + key, "");
        String fullMsg = prefix + message;
        for (int i = 0; i < placeholders.length; i += 2) {
            if (i + 1 < placeholders.length) {
                fullMsg = fullMsg.replace("{" + placeholders[i] + "}", placeholders[i + 1]);
            }
        }
        return this.msg.colorize(fullMsg);
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
        public List<String> invites = new ArrayList<>();

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

    public static class GuildInvite {
        public String guildName;
        public String inviterName;
        public long timestamp;

        public GuildInvite(String guildName, String inviterName, long timestamp) {
            this.guildName = guildName;
            this.inviterName = inviterName;
            this.timestamp = timestamp;
        }

        public boolean isExpired() {
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
        public String getVersion() { return "1.2.0"; }
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
