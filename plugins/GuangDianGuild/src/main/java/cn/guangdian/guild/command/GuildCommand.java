package cn.guangdian.guild.command;

import cn.guangdian.guild.GuangDianGuild;
import cn.guangdian.rpgcore.command.BaseCommand;
import cn.guangdian.rpgcore.command.CommandContext;
import cn.guangdian.rpgcore.command.CommandInfo;
import cn.guangdian.rpgcore.command.Description;
import cn.guangdian.rpgcore.command.SubCommand;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 公会命令 - 使用 RPGCore CommandFramework
 *
 * <p>基于注解驱动的命令系统，替代传统的 onCommand 方式。</p>
 *
 * @author Astraea RPG Team
 * @since 1.2.0
 */
@CommandInfo(name = "guild", description = "公会系统", permission = "guangdian.guild.use")
public class GuildCommand extends BaseCommand {

    private final GuangDianGuild plugin;

    public GuildCommand(GuangDianGuild plugin) {
        this.plugin = plugin;
    }

    /**
     * 创建公会
     */
    @SubCommand(name = "create", permission = "guangdian.guild.create", playerOnly = true, minArgs = 1)
    @Description("创建公会")
    public void create(CommandContext ctx) {
        Player player = ctx.requirePlayer();
        String name = ctx.getJoinedArgs();

        int min = plugin.getConfig().getInt("settings.min-name-length", 2);
        int max = plugin.getConfig().getInt("settings.max-name-length", 10);

        if (name.length() < min || name.length() > max) {
            ctx.sendError("公会名称长度必须在 " + min + " 到 " + max + " 之间!");
            return;
        }

        if (plugin.isInGuild(player.getName())) {
            ctx.sendError("你已经在一个公会中了!");
            return;
        }

        if (plugin.getGuild(name) != null) {
            ctx.sendError("该公会名称已被使用!");
            return;
        }

        if (plugin.createGuild(name, player)) {
            ctx.sendSuccess("成功创建公会: " + name);
            plugin.getLogger().info(player.getName() + " 创建了公会: " + name);
        } else {
            ctx.sendError("创建公会失败!");
        }
    }

    /**
     * 解散公会
     */
    @SubCommand(name = "disband", playerOnly = true)
    @Description("解散公会")
    public void disband(CommandContext ctx) {
        Player player = ctx.requirePlayer();
        GuangDianGuild.Guild guild = plugin.getPlayerGuild(player.getName());

        if (guild == null) {
            ctx.sendError("你不在任何公会中!");
            return;
        }

        if (!guild.leader.equalsIgnoreCase(player.getName())) {
            ctx.sendError("只有会长才能解散公会!");
            return;
        }

        String name = guild.name;
        plugin.disbandGuild(name);
        ctx.sendSuccess("公会 " + name + " 已解散!");
        plugin.getLogger().info(player.getName() + " 解散了公会: " + name);
    }

    /**
     * 加入公会
     */
    @SubCommand(name = "join", permission = "guangdian.guild.create", playerOnly = true, minArgs = 1)
    @Description("加入公会")
    public void join(CommandContext ctx) {
        Player player = ctx.requirePlayer();
        String name = ctx.getJoinedArgs();

        if (plugin.isInGuild(player.getName())) {
            ctx.sendError("你已经在一个公会中了!");
            return;
        }

        GuangDianGuild.Guild guild = plugin.getGuild(name);
        if (guild == null) {
            ctx.sendError("找不到该公会!");
            return;
        }

        if (!guild.invites.contains(player.getName())) {
            ctx.sendError("你没有被邀请加入这个公会!");
            return;
        }

        if (plugin.joinGuild(name, player)) {
            ctx.sendSuccess("成功加入公会: " + name);
        } else {
            ctx.sendError("加入公会失败!公会可能已满员。");
        }
    }

    /**
     * 离开公会
     */
    @SubCommand(name = "leave", playerOnly = true)
    @Description("离开公会")
    public void leave(CommandContext ctx) {
        Player player = ctx.requirePlayer();
        GuangDianGuild.Guild guild = plugin.getPlayerGuild(player.getName());

        if (guild == null) {
            ctx.sendError("你不在任何公会中!");
            return;
        }

        String name = guild.name;
        plugin.leaveGuild(player.getName());
        ctx.sendSuccess("你已离开公会: " + name);
    }

    /**
     * 邀请玩家
     */
    @SubCommand(name = "invite", playerOnly = true, minArgs = 1)
    @Description("邀请玩家加入公会")
    public void invite(CommandContext ctx) {
        Player player = ctx.requirePlayer();
        String targetName = ctx.getStringArg(0);
        Player target = Bukkit.getPlayer(targetName);

        GuangDianGuild.Guild guild = plugin.getPlayerGuild(player.getName());
        if (guild == null) {
            ctx.sendError("你不在任何公会中!");
            return;
        }

        if (target == null) {
            ctx.sendError("玩家不在线!");
            return;
        }

        if (plugin.isInGuild(target.getName())) {
            ctx.sendError("该玩家已在公会中!");
            return;
        }

        plugin.invitePlayer(player.getName(), target.getName());
        ctx.sendSuccess("已邀请 " + target.getName() + " 加入公会!");
        target.sendMessage(msg.colorize("<gold>[公会] <yellow>" + player.getName() + " 邀请你加入 " + guild.name + " 公会!"));
        target.sendMessage(msg.colorize("<yellow>输入 /guild accept 接受 或 /guild deny 拒绝"));
    }

    /**
     * 接受邀请
     */
    @SubCommand(name = "accept", playerOnly = true)
    @Description("接受公会邀请")
    public void accept(CommandContext ctx) {
        Player player = ctx.requirePlayer();
        GuangDianGuild.GuildInvite invite = plugin.getPendingInvite(player.getName());

        if (invite == null || invite.isExpired()) {
            plugin.removePendingInvite(player.getName());
            ctx.sendError("没有待处理的邀请!");
            return;
        }

        if (plugin.acceptInvite(player.getName())) {
            ctx.sendSuccess("已接受邀请，加入公会!");
        } else {
            ctx.sendError("加入公会失败!");
        }
    }

    /**
     * 拒绝邀请
     */
    @SubCommand(name = "deny", playerOnly = true)
    @Description("拒绝公会邀请")
    public void deny(CommandContext ctx) {
        Player player = ctx.requirePlayer();
        plugin.removePendingInvite(player.getName());
        ctx.sendSuccess("已拒绝邀请!");
    }

    /**
     * 踢出成员
     */
    @SubCommand(name = "kick", playerOnly = true, minArgs = 1)
    @Description("踢出公会成员")
    public void kick(CommandContext ctx) {
        Player player = ctx.requirePlayer();
        String targetName = ctx.getStringArg(0);

        if (plugin.kickMember(player.getName(), targetName)) {
            ctx.sendSuccess("已将 " + targetName + " 踢出公会!");
            Player target = Bukkit.getPlayer(targetName);
            if (target != null) {
                target.sendMessage(msg.colorize("<red>你已被踢出公会!"));
            }
        } else {
            ctx.sendError("无法踢出该成员!");
        }
    }

    /**
     * 晋升成员
     */
    @SubCommand(name = "promote", playerOnly = true, minArgs = 1)
    @Description("晋升公会成员")
    public void promote(CommandContext ctx) {
        Player player = ctx.requirePlayer();
        String targetName = ctx.getStringArg(0);

        GuangDianGuild.Guild guild = plugin.getPlayerGuild(player.getName());
        if (guild == null) {
            ctx.sendError("你不在任何公会中!");
            return;
        }

        if (plugin.promoteMember(player.getName(), targetName)) {
            GuangDianGuild.GuildMember targetM = guild.members.get(targetName);
            ctx.sendSuccess("已晋升 " + targetName + " 为 " + targetM.rank.getDisplayName());
            Player target = Bukkit.getPlayer(targetName);
            if (target != null) {
                target.sendMessage(msg.colorize("<green>你被晋升为 " + targetM.rank.getDisplayName() + "!"));
            }
        } else {
            ctx.sendError("无法晋升该成员!");
        }
    }

    /**
     * 降职成员
     */
    @SubCommand(name = "demote", playerOnly = true, minArgs = 1)
    @Description("降职公会成员")
    public void demote(CommandContext ctx) {
        Player player = ctx.requirePlayer();
        String targetName = ctx.getStringArg(0);

        GuangDianGuild.Guild guild = plugin.getPlayerGuild(player.getName());
        if (guild == null) {
            ctx.sendError("你不在任何公会中!");
            return;
        }

        if (plugin.demoteMember(player.getName(), targetName)) {
            GuangDianGuild.GuildMember targetM = guild.members.get(targetName);
            ctx.sendSuccess("已降职 " + targetName + " 为 " + targetM.rank.getDisplayName());
            Player target = Bukkit.getPlayer(targetName);
            if (target != null) {
                target.sendMessage(msg.colorize("<red>你被降职为 " + targetM.rank.getDisplayName() + "!"));
            }
        } else {
            ctx.sendError("无法降职该成员!");
        }
    }

    /**
     * 查看公会信息
     */
    @SubCommand(name = "info")
    @Description("查看公会信息")
    public void info(CommandContext ctx) {
        GuangDianGuild.Guild guild;

        if (ctx.getArgCount() >= 1) {
            String name = ctx.getJoinedArgs();
            guild = plugin.getGuild(name);
        } else if (ctx.getPlayer() != null) {
            guild = plugin.getPlayerGuild(ctx.getPlayer().getName());
        } else {
            ctx.sendError("用法: /guild info <公会名>");
            return;
        }

        if (guild == null) {
            ctx.sendError("找不到该公会!");
            return;
        }

        ctx.sendMessage("<gold>========== 公会信息 ==========");
        ctx.sendMessage("<yellow>公会名称: <white>" + guild.name);
        ctx.sendMessage("<yellow>公会前缀: <white>" + (guild.prefix.isEmpty() ? "无" : guild.prefix));
        ctx.sendMessage("<yellow>公会描述: <white>" + (guild.description.isEmpty() ? "无" : guild.description));
        ctx.sendMessage("<yellow>会长: <white>" + guild.leader);
        ctx.sendMessage("<yellow>成员数量: <white>" + guild.members.size());
        ctx.sendMessage("<gold>==============================");
    }

    /**
     * 公会列表
     */
    @SubCommand(name = "list")
    @Description("查看所有公会列表")
    public void list(CommandContext ctx) {
        ctx.sendMessage("<gold>========== 公会列表 ==========");
        for (GuangDianGuild.Guild guild : plugin.getAllGuilds()) {
            ctx.sendMessage("<yellow>" + guild.name + " <gray>- 成员: " + guild.members.size());
        }
        ctx.sendMessage("<gold>总计: <white>" + plugin.getGuildCount() + " 个公会");
        ctx.sendMessage("<gold>==============================");
    }

    /**
     * 公会聊天
     */
    @SubCommand(name = "chat", playerOnly = true, minArgs = 1)
    @Description("发送公会聊天消息")
    public void chat(CommandContext ctx) {
        Player player = ctx.requirePlayer();
        String message = ctx.getJoinedArgs();

        GuangDianGuild.Guild guild = plugin.getPlayerGuild(player.getName());
        if (guild == null) {
            ctx.sendError("你不在任何公会中!");
            return;
        }

        String chatPrefix = plugin.getConfig().getString("settings.chat-prefix", "<gray>[<gold>公会<gray>] ");
        String formatted = chatPrefix + "<yellow>" + player.getName() + ": <white>" + message;

        for (String member : guild.members.keySet()) {
            Player memberP = Bukkit.getPlayer(member);
            if (memberP != null) {
                memberP.sendMessage(msg.colorize(formatted));
            }
        }
    }

    /**
     * 设置公会前缀
     */
    @SubCommand(name = "setprefix", playerOnly = true, minArgs = 1)
    @Description("设置公会前缀")
    public void setPrefix(CommandContext ctx) {
        Player player = ctx.requirePlayer();
        String prefix = ctx.getStringArg(0);

        GuangDianGuild.Guild guild = plugin.getPlayerGuild(player.getName());
        if (guild == null) {
            ctx.sendError("你不在任何公会中!");
            return;
        }

        GuangDianGuild.GuildMember member = guild.members.get(player.getName());
        if (!member.rank.isAtLeast(GuangDianGuild.GuildRank.VICE_LEADER)) {
            ctx.sendError("只有副会长及以上才能设置前缀!");
            return;
        }

        plugin.setGuildPrefix(guild.name, prefix);
        ctx.sendSuccess("公会前缀已设置为: " + prefix);
    }

    /**
     * 设置公会描述
     */
    @SubCommand(name = "setdesc", playerOnly = true, minArgs = 1)
    @Description("设置公会描述")
    public void setDesc(CommandContext ctx) {
        Player player = ctx.requirePlayer();
        String desc = ctx.getJoinedArgs();

        GuangDianGuild.Guild guild = plugin.getPlayerGuild(player.getName());
        if (guild == null) {
            ctx.sendError("你不在任何公会中!");
            return;
        }

        GuangDianGuild.GuildMember member = guild.members.get(player.getName());
        if (!member.rank.isAtLeast(GuangDianGuild.GuildRank.VICE_LEADER)) {
            ctx.sendError("只有副会长及以上才能设置描述!");
            return;
        }

        plugin.setGuildDescription(guild.name, desc);
        ctx.sendSuccess("公会描述已设置!");
    }

    /**
     * 帮助
     */
    @SubCommand(name = "help")
    @Description("显示帮助信息")
    public void help(CommandContext ctx) {
        showHelp(ctx.getSender());
    }

    @Override
    public List<String> onTabComplete(java.lang.reflect.Method subCommandMethod, CommandContext context) {
        List<String> completions = new ArrayList<>();
        String subCommandName = subCommandMethod.getAnnotation(SubCommand.class).name();

        if (context.getArgCount() == 0 || context.getArgCount() == 1) {
            String partial = context.getArgCount() == 0 ? "" : context.getStringArg(0).toLowerCase();

            switch (subCommandName.toLowerCase()) {
                case "invite":
                case "kick":
                case "promote":
                case "demote":
                    for (Player p : Bukkit.getOnlinePlayers()) {
                        if (p.getName().toLowerCase().startsWith(partial)) {
                            completions.add(p.getName());
                        }
                    }
                    break;
                case "join":
                case "info":
                    for (GuangDianGuild.Guild g : plugin.getAllGuilds()) {
                        if (g.name.toLowerCase().startsWith(partial)) {
                            completions.add(g.name);
                        }
                    }
                    break;
            }
        }

        return completions;
    }
}
