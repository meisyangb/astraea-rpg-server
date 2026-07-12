package cn.guangdian.chunktrim.command;

import cn.guangdian.chunktrim.GuangDianChunkTrim;
import cn.guangdian.chunktrim.manager.TrimManager;
import cn.guangdian.chunktrim.task.TrimTask;
import com.sk89q.worldedit.LocalSession;
import com.sk89q.worldedit.WorldEdit;
import com.sk89q.worldedit.bukkit.BukkitAdapter;
import com.sk89q.worldedit.bukkit.BukkitPlayer;
import com.sk89q.worldedit.math.BlockVector3;
import com.sk89q.worldedit.regions.Region;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

/**
 * 区块裁剪命令
 */
public class ChunkTrimCommand implements CommandExecutor {

    private final GuangDianChunkTrim plugin;

    public ChunkTrimCommand(GuangDianChunkTrim plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length == 0) {
            sendHelp(sender);
            return true;
        }

        String subCommand = args[0].toLowerCase();

        switch (subCommand) {
            case "help":
                sendHelp(sender);
                break;
                
            case "set":
                handleSet(sender, args);
                break;
                
            case "we":
            case "selection":
                handleWorldEditSelection(sender);
                break;
                
            case "weconfirm":
                handleWorldEditConfirm(sender);
                break;
                
            case "start":
                handleStart(sender, args);
                break;
                
            case "confirm":
                handleConfirm(sender);
                break;
                
            case "pause":
                handlePause(sender, args);
                break;
                
            case "continue":
                handleContinue(sender, args);
                break;
                
            case "cancel":
                handleCancel(sender, args);
                break;
                
            case "status":
                handleStatus(sender, args);
                break;
                
            case "list":
                handleList(sender);
                break;
                
            default:
                sender.sendMessage(Component.text("未知命令: " + subCommand, NamedTextColor.RED));
                sendHelp(sender);
        }

        return true;
    }

    private void sendHelp(CommandSender sender) {
        sender.sendMessage(Component.text("=== GuangDianChunkTrim 帮助 ===", NamedTextColor.GOLD));
        sender.sendMessage(Component.text("/chunktrim we", NamedTextColor.YELLOW)
            .append(Component.text(" - 使用木斧选区裁剪（保留选区内区块）", NamedTextColor.GRAY)));
        sender.sendMessage(Component.text("/chunktrim weconfirm", NamedTextColor.YELLOW)
            .append(Component.text(" - 确认执行选区裁剪", NamedTextColor.GRAY)));
        sender.sendMessage(Component.text("/chunktrim set <半径> [世界]", NamedTextColor.YELLOW)
            .append(Component.text(" - 设置保留区域（以出生点为中心）", NamedTextColor.GRAY)));
        sender.sendMessage(Component.text("/chunktrim start <世界> <x> <z> <半径>", NamedTextColor.YELLOW)
            .append(Component.text(" - 开始裁剪", NamedTextColor.GRAY)));
        sender.sendMessage(Component.text("/chunktrim confirm", NamedTextColor.YELLOW)
            .append(Component.text(" - 确认执行裁剪", NamedTextColor.GRAY)));
        sender.sendMessage(Component.text("/chunktrim pause <世界>", NamedTextColor.YELLOW)
            .append(Component.text(" - 暂停裁剪", NamedTextColor.GRAY)));
        sender.sendMessage(Component.text("/chunktrim continue <世界>", NamedTextColor.YELLOW)
            .append(Component.text(" - 继续裁剪", NamedTextColor.GRAY)));
        sender.sendMessage(Component.text("/chunktrim cancel <世界>", NamedTextColor.YELLOW)
            .append(Component.text(" - 取消裁剪", NamedTextColor.GRAY)));
        sender.sendMessage(Component.text("/chunktrim status <世界>", NamedTextColor.YELLOW)
            .append(Component.text(" - 查看进度", NamedTextColor.GRAY)));
        sender.sendMessage(Component.text("/chunktrim list", NamedTextColor.YELLOW)
            .append(Component.text(" - 列出所有任务", NamedTextColor.GRAY)));
    }

    private void handleSet(CommandSender sender, String[] args) {
        if (!checkPermission(sender, "guangdian.chunktrim.set")) return;
        
        if (args.length < 2) {
            sender.sendMessage(Component.text("用法: /chunktrim set <半径> [世界]", NamedTextColor.RED));
            return;
        }
        
        try {
            int radius = Integer.parseInt(args[1]);
            World world = args.length >= 3 ? Bukkit.getWorld(args[2]) : 
                (sender instanceof Player ? ((Player) sender).getWorld() : null);
            
            if (world == null) {
                sender.sendMessage(Component.text("世界不存在!", NamedTextColor.RED));
                return;
            }
            
            Location spawn = world.getSpawnLocation();
            sender.sendMessage(Component.text("=== 保留区域设置 ===", NamedTextColor.GOLD));
            sender.sendMessage(Component.text("世界: " + world.getName(), NamedTextColor.YELLOW));
            sender.sendMessage(Component.text("中心: (" + spawn.getBlockX() + ", " + spawn.getBlockZ() + ")", NamedTextColor.YELLOW));
            sender.sendMessage(Component.text("半径: " + radius + " 格", NamedTextColor.YELLOW));
            sender.sendMessage(Component.text("请使用 /chunktrim start 确认执行", NamedTextColor.AQUA));
            
        } catch (NumberFormatException e) {
            sender.sendMessage(Component.text("半径必须是数字!", NamedTextColor.RED));
        }
    }

    private void handleStart(CommandSender sender, String[] args) {
        if (!checkPermission(sender, "guangdian.chunktrim.start")) return;
        
        World world;
        int centerX, centerZ, radius;
        
        if (args.length >= 5) {
            // /chunktrim start <世界> <x> <z> <半径>
            world = Bukkit.getWorld(args[1]);
            if (world == null) {
                sender.sendMessage(Component.text("世界不存在: " + args[1], NamedTextColor.RED));
                return;
            }
            try {
                centerX = Integer.parseInt(args[2]);
                centerZ = Integer.parseInt(args[3]);
                radius = Integer.parseInt(args[4]);
            } catch (NumberFormatException e) {
                sender.sendMessage(Component.text("坐标和半径必须是数字!", NamedTextColor.RED));
                return;
            }
        } else if (sender instanceof Player player) {
            // 以玩家当前位置为中心
            world = player.getWorld();
            centerX = player.getLocation().getBlockX();
            centerZ = player.getLocation().getBlockZ();
            
            if (args.length >= 2) {
                try {
                    radius = Integer.parseInt(args[1]);
                } catch (NumberFormatException e) {
                    sender.sendMessage(Component.text("半径必须是数字!", NamedTextColor.RED));
                    return;
                }
            } else {
                radius = plugin.getTrimConfig().getDefaultRadius();
            }
        } else {
            sender.sendMessage(Component.text("控制台必须指定完整参数: /chunktrim start <世界> <x> <z> <半径>", NamedTextColor.RED));
            return;
        }
        
        // 检查是否已有任务
        if (plugin.getTrimManager().hasActiveTask(world.getName())) {
            sender.sendMessage(Component.text("这个世界已有裁剪任务运行!", NamedTextColor.RED));
            return;
        }
        
        // 警告信息
        sender.sendMessage(Component.text("=== ⚠️ 警告 ===", NamedTextColor.RED));
        sender.sendMessage(Component.text("即将删除 " + world.getName() + " 世界边界外的所有区块!", NamedTextColor.YELLOW));
        sender.sendMessage(Component.text("中心: (" + centerX + ", " + centerZ + ") | 半径: " + radius + " 格", NamedTextColor.YELLOW));
        sender.sendMessage(Component.text("此操作不可逆! 请确认已备份!", NamedTextColor.RED));
        sender.sendMessage(Component.text("使用 /chunktrim confirm 确认执行", NamedTextColor.AQUA));
    }

    private void handleConfirm(CommandSender sender) {
        if (!checkPermission(sender, "guangdian.chunktrim.confirm")) return;
        
        sender.sendMessage(Component.text("请先使用 /chunktrim start 设置参数", NamedTextColor.YELLOW));
    }

    private void handlePause(CommandSender sender, String[] args) {
        if (!checkPermission(sender, "guangdian.chunktrim.pause")) return;
        
        String worldName = args.length >= 2 ? args[1] : 
            (sender instanceof Player ? ((Player) sender).getWorld().getName() : null);
        
        if (worldName == null) {
            sender.sendMessage(Component.text("请指定世界名称", NamedTextColor.RED));
            return;
        }
        
        if (plugin.getTrimManager().pauseTrim(worldName)) {
            sender.sendMessage(Component.text("已暂停 " + worldName + " 的裁剪任务", NamedTextColor.GREEN));
        } else {
            sender.sendMessage(Component.text("没有找到运行中的任务: " + worldName, NamedTextColor.RED));
        }
    }

    private void handleContinue(CommandSender sender, String[] args) {
        if (!checkPermission(sender, "guangdian.chunktrim.continue")) return;
        
        String worldName = args.length >= 2 ? args[1] : 
            (sender instanceof Player ? ((Player) sender).getWorld().getName() : null);
        
        if (worldName == null) {
            sender.sendMessage(Component.text("请指定世界名称", NamedTextColor.RED));
            return;
        }
        
        if (plugin.getTrimManager().continueTrim(worldName)) {
            sender.sendMessage(Component.text("已继续 " + worldName + " 的裁剪任务", NamedTextColor.GREEN));
        } else {
            sender.sendMessage(Component.text("没有找到暂停的任务: " + worldName, NamedTextColor.RED));
        }
    }

    private void handleCancel(CommandSender sender, String[] args) {
        if (!checkPermission(sender, "guangdian.chunktrim.cancel")) return;
        
        String worldName = args.length >= 2 ? args[1] : 
            (sender instanceof Player ? ((Player) sender).getWorld().getName() : null);
        
        if (worldName == null) {
            sender.sendMessage(Component.text("请指定世界名称", NamedTextColor.RED));
            return;
        }
        
        if (plugin.getTrimManager().cancelTrim(worldName)) {
            sender.sendMessage(Component.text("已取消 " + worldName + " 的裁剪任务", NamedTextColor.GREEN));
        } else {
            sender.sendMessage(Component.text("没有找到运行中的任务: " + worldName, NamedTextColor.RED));
        }
    }

    private void handleStatus(CommandSender sender, String[] args) {
        if (!checkPermission(sender, "guangdian.chunktrim.status")) return;
        
        String worldName = args.length >= 2 ? args[1] : 
            (sender instanceof Player ? ((Player) sender).getWorld().getName() : null);
        
        if (worldName == null) {
            sender.sendMessage(Component.text("请指定世界名称", NamedTextColor.RED));
            return;
        }
        
        TrimTask task = plugin.getTrimManager().getTask(worldName);
        if (task == null) {
            sender.sendMessage(Component.text("没有运行中的任务: " + worldName, NamedTextColor.YELLOW));
            return;
        }
        
        sender.sendMessage(Component.text("=== 任务状态 ===", NamedTextColor.GOLD));
        sender.sendMessage(Component.text("世界: " + worldName, NamedTextColor.YELLOW));
        sender.sendMessage(Component.text("状态: " + (task.isPaused() ? "暂停" : "运行中"), NamedTextColor.YELLOW));
        sender.sendMessage(Component.text("进度: " + task.getProgress() + "%", NamedTextColor.GREEN));
        sender.sendMessage(Component.text("已删除: " + task.getDeletedChunks() + " 个区域文件", NamedTextColor.AQUA));
    }

    private void handleList(CommandSender sender) {
        if (!checkPermission(sender, "guangdian.chunktrim.list")) return;
        
        var tasks = plugin.getTrimManager().getActiveTasks();
        
        if (tasks.isEmpty()) {
            sender.sendMessage(Component.text("当前没有运行中的裁剪任务", NamedTextColor.YELLOW));
            return;
        }
        
        sender.sendMessage(Component.text("=== 活动任务 ===", NamedTextColor.GOLD));
        for (var entry : tasks.entrySet()) {
            TrimTask task = entry.getValue();
            sender.sendMessage(Component.text(entry.getKey() + ": " + 
                task.getProgress() + "% | 已删除: " + task.getDeletedChunks(), NamedTextColor.YELLOW));
        }
    }

    private boolean checkPermission(CommandSender sender, String permission) {
        if (!sender.hasPermission(permission)) {
            sender.sendMessage(Component.text("没有权限!", NamedTextColor.RED));
            return false;
        }
        return true;
    }
    
    /**
     * 使用 WorldEdit 选区裁剪
     */
    private void handleWorldEditSelection(CommandSender sender) {
        if (!checkPermission(sender, "guangdian.chunktrim.we")) return;
        
        if (!(sender instanceof Player player)) {
            sender.sendMessage(Component.text("此命令只能由玩家执行!", NamedTextColor.RED));
            return;
        }
        
        World world = player.getWorld();
        
        // 检查是否已有任务
        if (plugin.getTrimManager().hasActiveTask(world.getName())) {
            sender.sendMessage(Component.text("这个世界已有裁剪任务运行!", NamedTextColor.RED));
            return;
        }
        
        try {
            // 获取 WorldEdit 选区
            BukkitPlayer wePlayer = BukkitAdapter.adapt(player);
            LocalSession session = WorldEdit.getInstance().getSessionManager().get(wePlayer);
            
            if (!session.isSelectionDefined(session.getSelectionWorld())) {
                sender.sendMessage(Component.text("请先用木斧选择一个区域!", NamedTextColor.RED));
                sender.sendMessage(Component.text("左键点击设置第一个点，右键点击设置第二个点", NamedTextColor.YELLOW));
                return;
            }
            
            Region region = session.getSelection(session.getSelectionWorld());
            BlockVector3 min = region.getMinimumPoint();
            BlockVector3 max = region.getMaximumPoint();
            
            // 计算选区的中心和半径
            int minX = min.x();
            int minZ = min.z();
            int maxX = max.x();
            int maxZ = max.z();
            
            int centerX = (minX + maxX) / 2;
            int centerZ = (minZ + maxZ) / 2;
            
            // 计算半径（取最大距离）
            int radiusX = (maxX - minX) / 2;
            int radiusZ = (maxZ - minZ) / 2;
            int radius = Math.max(radiusX, radiusZ);
            
            // 显示选区信息
            sender.sendMessage(Component.text("=== WorldEdit 选区裁剪 ===", NamedTextColor.GOLD));
            sender.sendMessage(Component.text("世界: " + world.getName(), NamedTextColor.YELLOW));
            sender.sendMessage(Component.text("选区范围: (" + minX + ", " + minZ + ") ~ (" + maxX + ", " + maxZ + ")", NamedTextColor.YELLOW));
            sender.sendMessage(Component.text("计算中心: (" + centerX + ", " + centerZ + ")", NamedTextColor.YELLOW));
            sender.sendMessage(Component.text("计算半径: " + radius + " 格", NamedTextColor.YELLOW));
            sender.sendMessage(Component.text("", NamedTextColor.GRAY));
            sender.sendMessage(Component.text("=== ⚠️ 警告 ===", NamedTextColor.RED));
            sender.sendMessage(Component.text("即将删除选区外的所有区块!", NamedTextColor.YELLOW));
            sender.sendMessage(Component.text("此操作不可逆! 请确认已备份!", NamedTextColor.RED));
            sender.sendMessage(Component.text("使用 /chunktrim weconfirm 确认执行", NamedTextColor.AQUA));
            
            // 保存待执行的参数
            plugin.getTrimManager().savePendingTrim(world, centerX, centerZ, radius);
            
        } catch (Exception e) {
            sender.sendMessage(Component.text("获取 WorldEdit 选区失败: " + e.getMessage(), NamedTextColor.RED));
            plugin.getLogger().warning("WorldEdit 选区获取失败: " + e.getMessage());
        }
    }
    
    /**
     * 确认 WorldEdit 选区裁剪
     */
    private void handleWorldEditConfirm(CommandSender sender) {
        if (!checkPermission(sender, "guangdian.chunktrim.we")) return;
        
        if (!(sender instanceof Player player)) {
            sender.sendMessage(Component.text("此命令只能由玩家执行!", NamedTextColor.RED));
            return;
        }
        
        World world = player.getWorld();
        TrimManager.PendingTrim pending = plugin.getTrimManager().getPendingTrim(world.getName());
        
        if (pending == null) {
            sender.sendMessage(Component.text("没有待执行的裁剪任务!", NamedTextColor.RED));
            sender.sendMessage(Component.text("请先使用 /chunktrim we 选择区域", NamedTextColor.YELLOW));
            return;
        }
        
        // 启动裁剪
        if (plugin.getTrimManager().startTrim(pending.world, pending.centerX, pending.centerZ, pending.radius)) {
            sender.sendMessage(Component.text("裁剪任务已启动!", NamedTextColor.GREEN));
            sender.sendMessage(Component.text("使用 /chunktrim status " + world.getName() + " 查看进度", NamedTextColor.AQUA));
            plugin.getTrimManager().removePendingTrim(world.getName());
        } else {
            sender.sendMessage(Component.text("启动裁剪任务失败!", NamedTextColor.RED));
        }
    }
}