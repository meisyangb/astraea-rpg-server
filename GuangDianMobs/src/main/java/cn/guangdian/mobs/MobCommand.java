package cn.guangdian.mobs;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Location;
import org.bukkit.command.*;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;

import java.util.*;

/**
 * /gdmm 命令 — spawn/kill/reload/list
 */
public class MobCommand implements CommandExecutor, TabCompleter {

    private final GuangDianMobs plugin;

    public MobCommand(GuangDianMobs plugin) { this.plugin = plugin; }

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        if (args.length == 0) {
            send(sender, "用法: /gdmm spawn|kill|reload|list");
            return true;
        }
        return switch (args[0].toLowerCase()) {
            case "spawn" -> handleSpawn(sender, args);
            case "kill" -> handleKill(sender);
            case "reload" -> handleReload(sender);
            case "list" -> handleList(sender);
            default -> { send(sender, "未知子命令: " + args[0]); yield true; }
        };
    }

    private boolean handleSpawn(CommandSender sender, String[] args) {
        if (!sender.hasPermission("gdmm.spawn")) { send(sender, "无权限"); return true; }
        if (!(sender instanceof Player p)) { send(sender, "仅玩家可用"); return true; }
        if (args.length < 2) { send(sender, "用法: /gdmm spawn <mobId>"); return true; }

        String mobId = args[1];
        MobTemplate t = plugin.getMobTemplates().get(mobId);
        if (t == null) { send(sender, "怪物不存在: " + mobId); return true; }

        Location loc = p.getLocation();
        LivingEntity entity = plugin.getMobSpawner().spawn(t, loc);
        if (entity != null) {
            plugin.getAIController().attach(entity, t);
            send(sender, "已生成 " + mobId + " (生命:" + (long)t.health() + " 伤害:" + (long)t.damage() + ")");
        } else {
            send(sender, "生成失败");
        }
        return true;
    }

    private boolean handleKill(CommandSender sender) {
        if (!sender.hasPermission("gdmm.kill")) { send(sender, "无权限"); return true; }
        int count = 0;
        for (var w : plugin.getServer().getWorlds()) {
            for (var e : w.getLivingEntities()) {
                if (plugin.getMobSpawner().isCustomMob(e)) {
                    e.remove();
                    count++;
                }
            }
        }
        send(sender, "已清除 " + count + " 个自定义怪物");
        return true;
    }

    private boolean handleReload(CommandSender sender) {
        if (!sender.hasPermission("gdmm.reload")) { send(sender, "无权限"); return true; }
        plugin.reload();
        send(sender, "配置已重载 — " + plugin.getMobTemplates().size() + " 怪物");
        return true;
    }

    private boolean handleList(CommandSender sender) {
        if (!sender.hasPermission("gdmm.list")) { send(sender, "无权限"); return true; }
        send(sender, "=== 怪物列表 (" + plugin.getMobTemplates().size() + "个) ===");
        for (var e : plugin.getMobTemplates().entrySet()) {
            MobTemplate t = e.getValue();
            send(sender, "  " + t.id() + " | " + t.entityType() + " | 生命:" + (long)t.health() + " | 伤害:" + (long)t.damage());
        }
        return true;
    }

    private void send(CommandSender sender, String msg) {
        sender.sendMessage(Component.text(msg, NamedTextColor.GRAY));
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command cmd, String label, String[] args) {
        if (args.length == 1) return filter(List.of("spawn", "kill", "reload", "list"), args[0]);
        if (args.length == 2 && "spawn".equalsIgnoreCase(args[0]))
            return filter(new ArrayList<>(plugin.getMobTemplates().keySet()), args[1]);
        return List.of();
    }

    private List<String> filter(List<String> opts, String prefix) {
        List<String> r = new ArrayList<>();
        for (String s : opts) if (s.toLowerCase().startsWith(prefix.toLowerCase())) r.add(s);
        return r.isEmpty() ? opts : r;
    }
}
