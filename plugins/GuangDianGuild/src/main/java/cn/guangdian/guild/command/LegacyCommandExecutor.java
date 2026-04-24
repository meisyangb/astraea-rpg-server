package cn.guangdian.guild.command;

import cn.guangdian.guild.GuangDianGuild;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.Arrays;

/**
 * 传统命令执行器 - 降级使用
 *
 * <p>当 RPGCore CommandFramework 不可用时，使用此传统命令处理器。</p>
 *
 * @author Astraea RPG Team
 * @since 1.2.0
 */
public class LegacyCommandExecutor implements CommandExecutor {

    private final GuangDianGuild plugin;

    public LegacyCommandExecutor(GuangDianGuild plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length == 0) {
            sendHelp(sender);
            return true;
        }

        String sub = args[0].toLowerCase();
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
        if (!(sender instanceof Player)) { sender.sendMessage(plugin.getMsg("player-only")); return true; }
        Player p = (Player) sender;
        if (!p.hasPermission("guangdian.guild.create")) { p.sendMessage(plugin.getMsg("no-permission")); return true; }
        if (args.length < 2) { p.sendMessage(Component.text("用法: /guild create <名称>").color(NamedTextColor.RED)); return true; }
        String name = String.join(" ", Arrays.copyOfRange(args, 1, args.length));
        int min = plugin.getConfig().getInt("settings.min-name-length", 2);
        int max = plugin.getConfig().getInt("settings.max-name-length", 10);
        if (name.length() < min || name.length() > max) { p.sendMessage(plugin.getMsg("invalid-guild-name")); return true; }
        if (plugin.isInGuild(p.getName())) { p.sendMessage(plugin.getMsg("already-in-guild")); return true; }
        if (plugin.getGuild(name) != null) { p.sendMessage(plugin.getMsg("guild-name-exists")); return true; }
        if (plugin.createGuild(name, p)) {
            p.sendMessage(plugin.getMsg("guild-created", "guild", name));
            plugin.getLogger().info(p.getName() + " 创建了工会: " + name);
        }
        return true;
    }

    private boolean handleDisband(CommandSender sender) {
        if (!(sender instanceof Player)) { sender.sendMessage(plugin.getMsg("player-only")); return true; }
        Player p = (Player) sender;
        GuangDianGuild.Guild guild = plugin.getPlayerGuild(p.getName());
        if (guild == null) { p.sendMessage(plugin.getMsg("not-in-guild")); return true; }
        if (!guild.leader.equalsIgnoreCase(p.getName())) { p.sendMessage(plugin.getMsg("not-leader")); return true; }
        String name = guild.name;
        plugin.disbandGuild(name);
        p.sendMessage(plugin.getMsg("guild-disbanded"));
        plugin.getLogger().info(p.getName() + " 解散了工会: " + name);
        return true;
    }

    private boolean handleJoin(CommandSender sender, String[] args) {
        if (!(sender instanceof Player)) { sender.sendMessage(plugin.getMsg("player-only")); return true; }
        Player p = (Player) sender;
        if (args.length < 2) { p.sendMessage(Component.text("用法: /guild join <名称>").color(NamedTextColor.RED)); return true; }
        String name = String.join(" ", Arrays.copyOfRange(args, 1, args.length));
        if (plugin.isInGuild(p.getName())) { p.sendMessage(plugin.getMsg("already-in-guild")); return true; }
        GuangDianGuild.Guild guild = plugin.getGuild(name);
        if (guild == null) { p.sendMessage(plugin.getMsg("guild-not-found")); return true; }
        if (!guild.invites.contains(p.getName())) { p.sendMessage(Component.text("你没有被邀请加入这个工会!").color(NamedTextColor.RED)); return true; }
        if (plugin.joinGuild(name, p)) p.sendMessage(plugin.getMsg("guild-joined", "guild", name));
        else p.sendMessage(plugin.getMsg("max-members-reached"));
        return true;
    }

    private boolean handleLeave(CommandSender sender) {
        if (!(sender instanceof Player)) { sender.sendMessage(plugin.getMsg("player-only")); return true; }
        Player p = (Player) sender;
        GuangDianGuild.Guild guild = plugin.getPlayerGuild(p.getName());
        if (guild == null) { p.sendMessage(plugin.getMsg("not-in-guild")); return true; }
        String name = guild.name;
        plugin.leaveGuild(p.getName());
        p.sendMessage(plugin.getMsg("guild-left", "guild", name));
        return true;
    }

    private boolean handleInvite(CommandSender sender, String[] args) {
        if (!(sender instanceof Player)) { sender.sendMessage(plugin.getMsg("player-only")); return true; }
        Player p = (Player) sender;
        if (args.length < 2) { p.sendMessage(Component.text("用法: /guild invite <玩家>").color(NamedTextColor.RED)); return true; }
        String target = args[1];
        org.bukkit.entity.Player targetP = plugin.getServer().getPlayer(target);
        GuangDianGuild.Guild guild = plugin.getPlayerGuild(p.getName());
        if (guild == null) { p.sendMessage(plugin.getMsg("not-in-guild")); return true; }
        if (targetP == null) { p.sendMessage(Component.text("玩家不在线!").color(NamedTextColor.RED)); return true; }
        if (plugin.isInGuild(target)) { p.sendMessage(Component.text("该玩家已在工会中!").color(NamedTextColor.RED)); return true; }
        plugin.invitePlayer(p.getName(), target);
        p.sendMessage(plugin.getMsg("invite-sent", "player", target));
        targetP.sendMessage(plugin.getMsg("invite-received", "player", p.getName(), "guild", guild.name));
        targetP.sendMessage(Component.text("输入 /guild accept 接受 或 /guild deny 拒绝").color(NamedTextColor.YELLOW));
        return true;
    }

    private boolean handleAccept(CommandSender sender) {
        if (!(sender instanceof Player)) { sender.sendMessage(plugin.getMsg("player-only")); return true; }
        Player p = (Player) sender;
        GuangDianGuild.GuildInvite invite = plugin.getPendingInvite(p.getName());
        if (invite == null || invite.isExpired()) { plugin.removePendingInvite(p.getName()); p.sendMessage(plugin.getMsg("no-pending-invite")); return true; }
        if (plugin.acceptInvite(p.getName())) p.sendMessage(plugin.getMsg("invite-accepted"));
        else p.sendMessage(Component.text("加入工会失败!").color(NamedTextColor.RED));
        return true;
    }

    private boolean handleDeny(CommandSender sender) {
        if (!(sender instanceof Player)) { sender.sendMessage(plugin.getMsg("player-only")); return true; }
        Player p = (Player) sender;
        plugin.removePendingInvite(p.getName());
        p.sendMessage(plugin.getMsg("invite-denied"));
        return true;
    }

    private boolean handleKick(CommandSender sender, String[] args) {
        if (!(sender instanceof Player)) { sender.sendMessage(plugin.getMsg("player-only")); return true; }
        Player p = (Player) sender;
        if (args.length < 2) { p.sendMessage(Component.text("用法: /guild kick <玩家>").color(NamedTextColor.RED)); return true; }
        String target = args[1];
        if (plugin.kickMember(p.getName(), target)) {
            p.sendMessage(plugin.getMsg("player-kicked", "player", target));
            org.bukkit.entity.Player targetP = plugin.getServer().getPlayer(target);
            if (targetP != null) targetP.sendMessage(plugin.getMsg("kicked"));
        } else p.sendMessage(Component.text("无法踢出该成员!").color(NamedTextColor.RED));
        return true;
    }

    private boolean handlePromote(CommandSender sender, String[] args) {
        if (!(sender instanceof Player)) { sender.sendMessage(plugin.getMsg("player-only")); return true; }
        Player p = (Player) sender;
        if (args.length < 2) { p.sendMessage(Component.text("用法: /guild promote <玩家>").color(NamedTextColor.RED)); return true; }
        String target = args[1];
        if (plugin.promoteMember(p.getName(), target)) {
            GuangDianGuild.Guild guild = plugin.getPlayerGuild(p.getName());
            GuangDianGuild.GuildMember targetM = guild.members.get(target);
            p.sendMessage(plugin.getMsg("player-promoted", "player", target, "rank", targetM.rank.getDisplayName()));
            org.bukkit.entity.Player targetP = plugin.getServer().getPlayer(target);
            if (targetP != null) targetP.sendMessage(plugin.getMsg("promoted", "rank", targetM.rank.getDisplayName()));
        } else p.sendMessage(Component.text("无法晋升该成员!").color(NamedTextColor.RED));
        return true;
    }

    private boolean handleDemote(CommandSender sender, String[] args) {
        if (!(sender instanceof Player)) { sender.sendMessage(plugin.getMsg("player-only")); return true; }
        Player p = (Player) sender;
        if (args.length < 2) { p.sendMessage(Component.text("用法: /guild demote <玩家>").color(NamedTextColor.RED)); return true; }
        String target = args[1];
        if (plugin.demoteMember(p.getName(), target)) {
            GuangDianGuild.Guild guild = plugin.getPlayerGuild(p.getName());
            GuangDianGuild.GuildMember targetM = guild.members.get(target);
            p.sendMessage(plugin.getMsg("player-demoted", "player", target, "rank", targetM.rank.getDisplayName()));
            org.bukkit.entity.Player targetP = plugin.getServer().getPlayer(target);
            if (targetP != null) targetP.sendMessage(plugin.getMsg("demoted", "rank", targetM.rank.getDisplayName()));
        } else p.sendMessage(Component.text("无法降职该成员!").color(NamedTextColor.RED));
        return true;
    }

    private boolean handleInfo(CommandSender sender, String[] args) {
        GuangDianGuild.Guild guild;
        if (args.length >= 2) {
            String name = String.join(" ", Arrays.copyOfRange(args, 1, args.length));
            guild = plugin.getGuild(name);
        } else if (sender instanceof Player) {
            guild = plugin.getPlayerGuild(((Player) sender).getName());
        } else {
            sender.sendMessage(Component.text("用法: /guild info <工会名>").color(NamedTextColor.RED)); return true;
        }
        if (guild == null) { sender.sendMessage(plugin.getMsg("guild-not-found")); return true; }
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
        for (GuangDianGuild.Guild guild : plugin.getAllGuilds()) {
            sender.sendMessage(Component.text(guild.name).color(NamedTextColor.YELLOW)
                .append(Component.text(" - 成员: " + guild.members.size()).color(NamedTextColor.GRAY)));
        }
        sender.sendMessage(Component.text("总计: ").color(NamedTextColor.GOLD)
            .append(Component.text(plugin.getGuildCount() + " 个工会").color(NamedTextColor.WHITE)));
        sender.sendMessage(Component.text("==============================").color(NamedTextColor.GOLD));
        return true;
    }

    private boolean handleChat(CommandSender sender, String[] args) {
        if (!(sender instanceof Player)) { sender.sendMessage(plugin.getMsg("player-only")); return true; }
        Player p = (Player) sender;
        GuangDianGuild.Guild guild = plugin.getPlayerGuild(p.getName());
        if (guild == null) { p.sendMessage(plugin.getMsg("not-in-guild")); return true; }
        if (args.length < 2) { p.sendMessage(Component.text("用法: /guild chat <消息>").color(NamedTextColor.RED)); return true; }
        String message = String.join(" ", Arrays.copyOfRange(args, 1, args.length));
        String chatPrefix = plugin.getConfig().getString("settings.chat-prefix", "<gray>[<gold>工会<gray>] ");
        Component formatted = plugin.getMsg(chatPrefix)
            .append(Component.text(p.getName() + ": ").color(NamedTextColor.YELLOW))
            .append(Component.text(message).color(NamedTextColor.WHITE));
        for (String member : guild.members.keySet()) {
            org.bukkit.entity.Player memberP = plugin.getServer().getPlayer(member);
            if (memberP != null) memberP.sendMessage(formatted);
        }
        return true;
    }

    private boolean handleSetPrefix(CommandSender sender, String[] args) {
        if (!(sender instanceof Player)) { sender.sendMessage(plugin.getMsg("player-only")); return true; }
        Player p = (Player) sender;
        GuangDianGuild.Guild guild = plugin.getPlayerGuild(p.getName());
        if (guild == null) { p.sendMessage(plugin.getMsg("not-in-guild")); return true; }
        GuangDianGuild.GuildMember member = guild.members.get(p.getName());
        if (!member.rank.isAtLeast(GuangDianGuild.GuildRank.VICE_LEADER)) { p.sendMessage(plugin.getMsg("no-permission")); return true; }
        if (args.length < 2) { p.sendMessage(Component.text("用法: /guild setprefix <前缀>").color(NamedTextColor.RED)); return true; }
        String prefix = args[1];
        plugin.setGuildPrefix(guild.name, prefix);
        p.sendMessage(plugin.getMsg("prefix-set", "prefix", prefix));
        return true;
    }

    private boolean handleSetDesc(CommandSender sender, String[] args) {
        if (!(sender instanceof Player)) { sender.sendMessage(plugin.getMsg("player-only")); return true; }
        Player p = (Player) sender;
        GuangDianGuild.Guild guild = plugin.getPlayerGuild(p.getName());
        if (guild == null) { p.sendMessage(plugin.getMsg("not-in-guild")); return true; }
        GuangDianGuild.GuildMember member = guild.members.get(p.getName());
        if (!member.rank.isAtLeast(GuangDianGuild.GuildRank.VICE_LEADER)) { p.sendMessage(plugin.getMsg("no-permission")); return true; }
        if (args.length < 2) { p.sendMessage(Component.text("用法: /guild setdesc <描述>").color(NamedTextColor.RED)); return true; }
        String desc = String.join(" ", Arrays.copyOfRange(args, 1, args.length));
        plugin.setGuildDescription(guild.name, desc);
        p.sendMessage(plugin.getMsg("desc-set"));
        return true;
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
}
