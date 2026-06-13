package cn.guangdian.dungeon.command;

import cn.guangdian.dungeon.GuangDianDungeon;
import cn.guangdian.dungeon.model.*;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

public class PartyCommand implements CommandExecutor, TabCompleter {

    private final GuangDianDungeon plugin;

    public PartyCommand(GuangDianDungeon plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage(plugin.color("<red>此命令只能由玩家执行"));
            return true;
        }

        Player player = (Player) sender;

        if (args.length == 0) {
            sendHelp(player);
            return true;
        }

        String subCommand = args[0].toLowerCase();

        switch (subCommand) {
            case "create":
                return handleCreate(player, args);
            case "invite":
                return handleInvite(player, args);
            case "accept":
                return handleAccept(player);
            case "decline":
                return handleDecline(player);
            case "leave":
                return handleLeave(player);
            case "kick":
                return handleKick(player, args);
            case "leader":
                return handleLeader(player, args);
            case "disband":
                return handleDisband(player);
            case "list":
                return handleList(player);
            case "info":
                return handleInfo(player);
            case "ready":
                return handleReady(player);
            default:
                sendHelp(player);
                return true;
        }
    }

    private boolean handleCreate(Player player, String[] args) {
        if (args.length < 2) {
            player.sendMessage(plugin.color("<red>用法: /party create <副本ID>"));
            return true;
        }

        String dungeonId = args[1];
        DungeonTemplate template = plugin.getTemplateLoader().getTemplate(dungeonId);
        
        if (template == null) {
            player.sendMessage(plugin.color("<red>副本不存在: " + dungeonId));
            return true;
        }

        DungeonParty party = plugin.getPartyManager().createParty(player, template);
        if (party == null) {
            player.sendMessage(plugin.color("<red>创建队伍失败，你可能已在队伍中"));
            return true;
        }

        return true;
    }

    private boolean handleInvite(Player player, String[] args) {
        if (args.length < 2) {
            player.sendMessage(plugin.color("<red>用法: /party invite <玩家>"));
            return true;
        }

        Player target = Bukkit.getPlayer(args[1]);
        if (target == null) {
            player.sendMessage(plugin.color("<red>玩家不在线: " + args[1]));
            return true;
        }

        plugin.getPartyManager().invitePlayer(player, target);
        return true;
    }

    private boolean handleAccept(Player player) {
        plugin.getPartyManager().acceptInvite(player);
        return true;
    }

    private boolean handleDecline(Player player) {
        plugin.getPartyManager().declineInvite(player);
        player.sendMessage(plugin.color("<yellow>已拒绝邀请"));
        return true;
    }

    private boolean handleLeave(Player player) {
        plugin.getPartyManager().leaveParty(player);
        return true;
    }

    private boolean handleKick(Player player, String[] args) {
        if (args.length < 2) {
            player.sendMessage(plugin.color("<red>用法: /party kick <玩家>"));
            return true;
        }

        Player target = Bukkit.getPlayer(args[1]);
        if (target == null) {
            player.sendMessage(plugin.color("<red>玩家不在线: " + args[1]));
            return true;
        }

        plugin.getPartyManager().kickPlayer(player, target);
        return true;
    }

    private boolean handleLeader(Player player, String[] args) {
        if (args.length < 2) {
            player.sendMessage(plugin.color("<red>用法: /party leader <玩家>"));
            return true;
        }

        Player target = Bukkit.getPlayer(args[1]);
        if (target == null) {
            player.sendMessage(plugin.color("<red>玩家不在线: " + args[1]));
            return true;
        }

        plugin.getPartyManager().transferLeader(player, target);
        return true;
    }

    private boolean handleDisband(Player player) {
        var partyOpt = plugin.getPartyManager().getPlayerParty(player);
        if (partyOpt.isEmpty()) {
            player.sendMessage(plugin.color("<red>你不在任何队伍中"));
            return true;
        }

        DungeonParty party = partyOpt.get();
        if (!party.isLeader(player)) {
            player.sendMessage(plugin.color("<red>只有队长可以解散队伍"));
            return true;
        }

        plugin.getPartyManager().disbandParty(party);
        return true;
    }

    private boolean handleList(Player player) {
        player.sendMessage(plugin.color("<gold>========== 队伍列表 =========="));
        
        int count = 0;
        for (DungeonParty party : plugin.getPartyManager().getAllParties()) {
            if (party.getState() != PartyState.IN_DUNGEON) {
                player.sendMessage(plugin.color("<yellow>" + party.getLeader().getName() + 
                    " 的队伍 <gray>(" + party.getMemberCount() + "/" + party.getMaxMembers() + ")"));
                count++;
            }
        }
        
        if (count == 0) {
            player.sendMessage(plugin.color("<gray>暂无等待中的队伍"));
        }
        
        player.sendMessage(plugin.color("<gold>================================"));
        return true;
    }

    private boolean handleInfo(Player player) {
        var partyOpt = plugin.getPartyManager().getPlayerParty(player);
        if (partyOpt.isEmpty()) {
            player.sendMessage(plugin.color("<red>你不在任何队伍中"));
            return true;
        }

        DungeonParty party = partyOpt.get();
        
        player.sendMessage(plugin.color("<gold>========== 队伍信息 =========="));
        player.sendMessage(plugin.color("<gray>队长: <white>" + party.getLeader().getName()));
        player.sendMessage(plugin.color("<gray>人数: <white>" + party.getMemberCount() + "/" + party.getMaxMembers()));
        
        StringBuilder members = new StringBuilder();
        for (PartyMember member : party.getMembers()) {
            if (members.length() > 0) members.append(", ");
            members.append(member.getName());
            if (member.isLeader()) members.append("(队长)");
            if (member.isReady()) members.append("(准备)");
        }
        player.sendMessage(plugin.color("<gray>成员: <white>" + members));
        player.sendMessage(plugin.color("<gold>================================"));
        
        return true;
    }

    private boolean handleReady(Player player) {
        var partyOpt = plugin.getPartyManager().getPlayerParty(player);
        if (partyOpt.isEmpty()) {
            player.sendMessage(plugin.color("<red>你不在任何队伍中"));
            return true;
        }

        DungeonParty party = partyOpt.get();
        PartyMember member = party.getMember(player.getUniqueId());
        
        if (member == null) return true;
        
        boolean newReady = !member.isReady();
        party.setReady(player, newReady);
        
        player.sendMessage(plugin.color(newReady ? 
            "<green>你已准备就绪" : "<yellow>你取消了准备"));
        
        return true;
    }

    private void sendHelp(Player player) {
        player.sendMessage(plugin.color("<gold>========== 队伍帮助 =========="));
        player.sendMessage(plugin.color("<yellow>/party create <副本> <gray>- 创建队伍"));
        player.sendMessage(plugin.color("<yellow>/party invite <玩家> <gray>- 邀请玩家"));
        player.sendMessage(plugin.color("<yellow>/party accept <gray>- 接受邀请"));
        player.sendMessage(plugin.color("<yellow>/party decline <gray>- 拒绝邀请"));
        player.sendMessage(plugin.color("<yellow>/party leave <gray>- 离开队伍"));
        player.sendMessage(plugin.color("<yellow>/party kick <玩家> <gray>- 踢出成员"));
        player.sendMessage(plugin.color("<yellow>/party leader <玩家> <gray>- 转让队长"));
        player.sendMessage(plugin.color("<yellow>/party disband <gray>- 解散队伍"));
        player.sendMessage(plugin.color("<yellow>/party info <gray>- 查看队伍信息"));
        player.sendMessage(plugin.color("<yellow>/party ready <gray>- 切换准备状态"));
        player.sendMessage(plugin.color("<gold>================================"));
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        List<String> completions = new ArrayList<>();

        if (args.length == 1) {
            completions.addAll(Arrays.asList("create", "invite", "accept", "decline", 
                "leave", "kick", "leader", "disband", "list", "info", "ready"));
        } else if (args.length == 2) {
            String subCommand = args[0].toLowerCase();
            if (subCommand.equals("create")) {
                completions.addAll(plugin.getTemplateLoader().getTemplateIds());
            } else if (subCommand.equals("invite") || subCommand.equals("kick") || subCommand.equals("leader")) {
                completions.addAll(Bukkit.getOnlinePlayers().stream()
                    .map(Player::getName)
                    .collect(Collectors.toList()));
            }
        }

        String lastArg = args[args.length - 1].toLowerCase();
        return completions.stream()
            .filter(c -> c.toLowerCase().startsWith(lastArg))
            .collect(Collectors.toList());
    }
}
