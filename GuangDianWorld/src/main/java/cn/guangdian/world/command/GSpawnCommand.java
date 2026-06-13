package cn.guangdian.world.command;

import cn.guangdian.world.GuangDianWorld;
import cn.guangdian.world.model.GDWorld;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Location;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class GSpawnCommand implements CommandExecutor, TabCompleter {

    private final GuangDianWorld plugin;

    public GSpawnCommand(GuangDianWorld plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(Component.text("此命令只能由玩家执行!", NamedTextColor.RED));
            return true;
        }

        // 权限检查 - 基础传送权限
        if (!player.hasPermission("guangdian.world.spawn")) {
            player.sendMessage(Component.text("你没有权限执行此命令!", NamedTextColor.RED));
            return true;
        }

        if (args.length == 0) {
            return teleportToSpawn(player);
        }

        return switch (args[0].toLowerCase()) {
            case "set" -> setSpawn(player);
            case "world" -> teleportToWorldSpawn(player, args);
            default -> {
                player.sendMessage(Component.text("用法: /gspawn [set|world <世界名>]", NamedTextColor.RED));
                yield true;
            }
        };
    }

    private boolean teleportToSpawn(Player player) {
        GDWorld world = plugin.getWorldManager().getWorld(player.getWorld());
        if (world == null) {
            player.sendMessage(Component.text("当前世界未注册!", NamedTextColor.RED));
            return true;
        }

        Location spawnLoc = world.getSpawnLocation();
        if (spawnLoc != null && world.isLoaded()) {
            spawnLoc = spawnLoc.clone();
            spawnLoc.setWorld(world.getBukkitWorld());
            player.teleport(spawnLoc);
            player.sendMessage(Component.text("已传送到当前世界的出生点!", NamedTextColor.GREEN));
        } else {
            player.teleport(world.getBukkitWorld().getSpawnLocation());
            player.sendMessage(Component.text("已传送到世界出生点!", NamedTextColor.GREEN));
        }
        return true;
    }

    private boolean setSpawn(Player player) {
        // 设置出生点需要管理员权限
        if (!player.hasPermission("guangdian.world.admin")) {
            player.sendMessage(Component.text("你没有权限设置出生点!", NamedTextColor.RED));
            return true;
        }

        String worldName = player.getWorld().getName();
        Location loc = player.getLocation();
        
        plugin.getWorldManager().setSpawnPoint(worldName, loc);
        player.sendMessage(Component.text("已设置世界 " + worldName + " 的出生点!", NamedTextColor.GREEN));
        player.sendMessage(Component.text(String.format("位置: %.1f, %.1f, %.1f", 
            loc.getX(), loc.getY(), loc.getZ()), NamedTextColor.GRAY));
        return true;
    }

    private boolean teleportToWorldSpawn(Player player, String[] args) {
        if (args.length < 2) {
            player.sendMessage(Component.text("用法: /gspawn world <世界名>", NamedTextColor.RED));
            return true;
        }

        String worldName = args[1];
        GDWorld world = plugin.getWorldManager().getWorld(worldName);
        
        if (world == null) {
            player.sendMessage(Component.text("世界不存在: " + worldName, NamedTextColor.RED));
            return true;
        }

        if (!world.isLoaded()) {
            plugin.getWorldManager().loadWorld(worldName);
        }

        if (plugin.getWorldManager().teleportToWorld(player, worldName)) {
            player.sendMessage(Component.text("已传送到世界 " + world.getDisplayName() + " 的出生点!", NamedTextColor.GREEN));
        } else {
            player.sendMessage(Component.text("传送失败!", NamedTextColor.RED));
        }
        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        List<String> completions = new ArrayList<>();
        
        if (args.length == 1) {
            completions.addAll(Arrays.asList("set", "world"));
        } else if (args.length == 2 && args[0].equalsIgnoreCase("world")) {
            completions.addAll(plugin.getWorldManager().getWorldNames());
        }
        
        String lastArg = args[args.length - 1].toLowerCase();
        completions.removeIf(s -> !s.toLowerCase().startsWith(lastArg));
        return completions;
    }
}
