package cn.guangdian.mobs.command;

import cn.guangdian.mobs.GuangDianMobs;
import cn.guangdian.mobs.model.SpawnPoint;
import cn.guangdian.rpgcore.message.MiniMessageService;
import org.bukkit.Location;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 刷新点管理命令
 * /gdmsp - GuangDianMobs SpawnPoint
 */
public class SpawnPointCommand implements CommandExecutor, TabCompleter {

    private final GuangDianMobs plugin;

    public SpawnPointCommand(GuangDianMobs plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length == 0) {
            sendHelp(sender);
            return true;
        }

        switch (args[0].toLowerCase()) {
            case "create" -> handleCreate(sender, args);
            case "delete" -> handleDelete(sender, args);
            case "info" -> handleInfo(sender, args);
            case "list" -> handleList(sender);
            case "tp" -> handleTeleport(sender, args);
            case "spawn" -> handleSpawn(sender, args);
            case "set" -> handleSet(sender, args);
            case "enable" -> handleEnable(sender, args, true);
            case "disable" -> handleEnable(sender, args, false);
            case "near" -> handleNear(sender, args);
            default -> sendHelp(sender);
        }

        return true;
    }

    /**
     * 创建刷新点
     */
    private void handleCreate(CommandSender sender, String[] args) {
        MiniMessageService mm = MiniMessageService.getInstance();

        if (!(sender instanceof Player player)) {
            sender.sendMessage(mm.red("只有玩家可以使用此命令"));
            return;
        }

        if (args.length < 3) {
            sender.sendMessage(mm.colorize("<red>用法: /gdmsp create <ID> <怪物ID> [等级]"));
            return;
        }

        String id = args[1];
        String mobId = args[2];
        int level = -1;

        if (args.length >= 4) {
            try {
                level = Integer.parseInt(args[3]);
            } catch (NumberFormatException e) {
                sender.sendMessage(mm.red("等级必须是数字"));
                return;
            }
        }

        if (plugin.getMobManager().getMobTemplate(mobId) == null) {
            sender.sendMessage(mm.red("怪物不存在: " + mobId));
            return;
        }

        if (plugin.getSpawnPointManager().getSpawnPoint(id) != null) {
            sender.sendMessage(mm.red("刷新点已存在: " + id));
            return;
        }

        SpawnPoint point = plugin.getSpawnPointManager().createSpawnPoint(id, player.getLocation(), mobId);
        if (point == null) {
            sender.sendMessage(mm.red("创建失败"));
            return;
        }

        if (level > 0) {
            point.setLevel(level);
            plugin.getSpawnPointManager().saveSpawnPoint(point);
        }

        sender.sendMessage(mm.green("成功创建刷新点: " + id));
        sender.sendMessage(mm.colorize("<gray>怪物: <white>" + mobId));
        sender.sendMessage(mm.colorize("<gray>位置: <white>" + formatLocation(player.getLocation())));
    }

    /**
     * 删除刷新点
     */
    private void handleDelete(CommandSender sender, String[] args) {
        MiniMessageService mm = MiniMessageService.getInstance();

        if (args.length < 2) {
            sender.sendMessage(mm.colorize("<red>用法: /gdmsp delete <ID>"));
            return;
        }

        String id = args[1];

        if (!plugin.getSpawnPointManager().deleteSpawnPoint(id)) {
            sender.sendMessage(mm.red("刷新点不存在: " + id));
            return;
        }

        sender.sendMessage(mm.green("成功删除刷新点: " + id));
    }

    /**
     * 查看刷新点信息
     */
    private void handleInfo(CommandSender sender, String[] args) {
        MiniMessageService mm = MiniMessageService.getInstance();

        if (args.length < 2) {
            sender.sendMessage(mm.colorize("<red>用法: /gdmsp info <ID>"));
            return;
        }

        String id = args[1];
        SpawnPoint point = plugin.getSpawnPointManager().getSpawnPoint(id);

        if (point == null) {
            sender.sendMessage(mm.red("刷新点不存在: " + id));
            return;
        }

        sender.sendMessage(mm.colorize("<gold><bold>===== 刷新点信息 ====="));
        sender.sendMessage(mm.colorize("<gray>ID: <white>" + point.getId()));
        sender.sendMessage(mm.colorize("<gray>名称: <white>" + point.getDisplayName()));
        sender.sendMessage(mm.colorize("<gray>怪物: <white>" + point.getMobId()));
        sender.sendMessage(mm.colorize("<gray>等级: <white>" + (point.getLevel() > 0 ? point.getLevel() : "默认")));
        sender.sendMessage(mm.colorize("<gray>位置: <white>" + formatLocation(point.getLocation())));
        sender.sendMessage(mm.colorize("<gray>数量: <white>" + point.getAmount() + " / " + point.getMaxMobs()));
        sender.sendMessage(mm.colorize("<gray>冷却: <white>" + (point.getCooldown() / 20) + "秒"));
        sender.sendMessage(mm.colorize("<gray>半径: <white>" + point.getRadius()));
        sender.sendMessage(mm.colorize("<gray>状态: " + (point.isEnabled() ? "<green>启用" : "<red>禁用")));
        sender.sendMessage(mm.colorize("<gray>当前怪物: <white>" + point.getCurrentMobs()));
    }

    /**
     * 列出所有刷新点
     */
    private void handleList(CommandSender sender) {
        MiniMessageService mm = MiniMessageService.getInstance();

        sender.sendMessage(mm.colorize("<gold><bold>===== 刷新点列表 ====="));

        int enabled = 0, disabled = 0;
        for (SpawnPoint point : plugin.getSpawnPointManager().getAllSpawnPoints()) {
            String status = point.isEnabled() ? "<green>✓" : "<red>✗";
            sender.sendMessage(mm.colorize(status + " <gray>" + point.getId() + " <dark_gray>- <white>" + point.getMobId() +
                " <gray>(" + point.getCurrentMobs() + "/" + point.getMaxMobs() + ")"));
            if (point.isEnabled()) enabled++;
            else disabled++;
        }

        sender.sendMessage(mm.colorize("<gold>总计: <green>" + enabled + " <gold>启用, <red>" + disabled + " <gold>禁用"));
    }

    /**
     * 传送到刷新点
     */
    private void handleTeleport(CommandSender sender, String[] args) {
        MiniMessageService mm = MiniMessageService.getInstance();

        if (!(sender instanceof Player player)) {
            sender.sendMessage(mm.red("只有玩家可以使用此命令"));
            return;
        }

        if (args.length < 2) {
            sender.sendMessage(mm.colorize("<red>用法: /gdmsp tp <ID>"));
            return;
        }

        String id = args[1];
        SpawnPoint point = plugin.getSpawnPointManager().getSpawnPoint(id);

        if (point == null) {
            sender.sendMessage(mm.red("刷新点不存在: " + id));
            return;
        }

        player.teleport(point.getLocation());
        sender.sendMessage(mm.green("已传送到刷新点: " + id));
    }

    /**
     * 手动触发刷新
     */
    private void handleSpawn(CommandSender sender, String[] args) {
        MiniMessageService mm = MiniMessageService.getInstance();

        if (args.length < 2) {
            sender.sendMessage(mm.colorize("<red>用法: /gdmsp spawn <ID>"));
            return;
        }

        String id = args[1];

        if (!plugin.getSpawnPointManager().forceSpawn(id)) {
            sender.sendMessage(mm.red("刷新失败，请检查刷新点是否存在或是否已达到最大数量"));
            return;
        }

        sender.sendMessage(mm.green("已触发刷新点: " + id));
    }

    /**
     * 设置刷新点属性
     */
    private void handleSet(CommandSender sender, String[] args) {
        MiniMessageService mm = MiniMessageService.getInstance();

        if (args.length < 4) {
            sender.sendMessage(mm.colorize("<red>用法: /gdmsp set <ID> <属性> <值>"));
            sender.sendMessage(mm.colorize("<gray>可用属性: amount, max-mobs, cooldown, radius, level"));
            return;
        }

        String id = args[1];
        String property = args[2].toLowerCase();
        String value = args[3];

        SpawnPoint point = plugin.getSpawnPointManager().getSpawnPoint(id);
        if (point == null) {
            sender.sendMessage(mm.red("刷新点不存在: " + id));
            return;
        }

        try {
            switch (property) {
                case "amount" -> {
                    point.setAmount(Integer.parseInt(value));
                    sender.sendMessage(mm.green("每次刷新数量已设置为: " + value));
                }
                case "max-mobs" -> {
                    point.setMaxMobs(Integer.parseInt(value));
                    sender.sendMessage(mm.green("最大怪物数量已设置为: " + value));
                }
                case "cooldown" -> {
                    point.setCooldown(Integer.parseInt(value) * 20); // 秒转tick
                    sender.sendMessage(mm.green("刷新冷却已设置为: " + value + "秒"));
                }
                case "radius" -> {
                    point.setRadius(Double.parseDouble(value));
                    sender.sendMessage(mm.green("刷新半径已设置为: " + value));
                }
                case "level" -> {
                    point.setLevel(Integer.parseInt(value));
                    sender.sendMessage(mm.green("怪物等级已设置为: " + value));
                }
                default -> {
                    sender.sendMessage(mm.red("未知属性: " + property));
                    return;
                }
            }
            plugin.getSpawnPointManager().saveSpawnPoint(point);
        } catch (NumberFormatException e) {
            sender.sendMessage(mm.red("值必须是数字"));
        }
    }

    /**
     * 启用/禁用刷新点
     */
    private void handleEnable(CommandSender sender, String[] args, boolean enable) {
        MiniMessageService mm = MiniMessageService.getInstance();

        if (args.length < 2) {
            sender.sendMessage(mm.colorize("<red>用法: /gdmsp " + (enable ? "enable" : "disable") + " <ID>"));
            return;
        }

        String id = args[1];
        SpawnPoint point = plugin.getSpawnPointManager().getSpawnPoint(id);

        if (point == null) {
            sender.sendMessage(mm.red("刷新点不存在: " + id));
            return;
        }

        point.setEnabled(enable);
        plugin.getSpawnPointManager().saveSpawnPoint(point);

        sender.sendMessage(mm.green("刷新点 " + id + " 已" + (enable ? "启用" : "禁用")));
    }

    /**
     * 查找附近的刷新点
     */
    private void handleNear(CommandSender sender, String[] args) {
        MiniMessageService mm = MiniMessageService.getInstance();

        if (!(sender instanceof Player player)) {
            sender.sendMessage(mm.red("只有玩家可以使用此命令"));
            return;
        }

        double range = 50;
        if (args.length >= 2) {
            try {
                range = Double.parseDouble(args[1]);
            } catch (NumberFormatException e) {
                sender.sendMessage(mm.red("范围必须是数字"));
                return;
            }
        }

        sender.sendMessage(mm.colorize("<gold><bold>===== 附近刷新点 (" + range + "格) ====="));

        int count = 0;
        for (SpawnPoint point : plugin.getSpawnPointManager().getAllSpawnPoints()) {
            if (point.getLocation() == null) continue;
            if (!point.getLocation().getWorld().equals(player.getWorld())) continue;

            double distance = point.getLocation().distance(player.getLocation());
            if (distance <= range) {
                String status = point.isEnabled() ? "<green>✓" : "<red>✗";
                sender.sendMessage(mm.colorize(status + " <gray>" + point.getId() + " <dark_gray>- <white>" +
                    String.format("%.1f", distance) + "格"));
                count++;
            }
        }

        if (count == 0) {
            sender.sendMessage(mm.colorize("<gray>附近没有刷新点"));
        } else {
            sender.sendMessage(mm.colorize("<gold>找到 " + count + " 个刷新点"));
        }
    }

    /**
     * 发送帮助信息
     */
    private void sendHelp(CommandSender sender) {
        MiniMessageService mm = MiniMessageService.getInstance();

        sender.sendMessage(mm.colorize("<gold><bold>===== 刷新点管理命令 ====="));
        sender.sendMessage(mm.colorize("<yellow>创建与管理:"));
        sender.sendMessage(mm.colorize("<gray>/gdmsp create <ID> <怪物ID> [等级] <white>- 创建刷新点"));
        sender.sendMessage(mm.colorize("<gray>/gdmsp delete <ID> <white>- 删除刷新点"));
        sender.sendMessage(mm.colorize("<gray>/gdmsp info <ID> <white>- 查看刷新点信息"));
        sender.sendMessage(mm.colorize("<gray>/gdmsp list <white>- 列出所有刷新点"));
        sender.sendMessage(mm.colorize("<yellow>操作:"));
        sender.sendMessage(mm.colorize("<gray>/gdmsp tp <ID> <white>- 传送到刷新点"));
        sender.sendMessage(mm.colorize("<gray>/gdmsp spawn <ID> <white>- 手动触发刷新"));
        sender.sendMessage(mm.colorize("<gray>/gdmsp enable/disable <ID> <white>- 启用/禁用"));
        sender.sendMessage(mm.colorize("<gray>/gdmsp set <ID> <属性> <值> <white>- 设置属性"));
        sender.sendMessage(mm.colorize("<gray>/gdmsp near [范围] <white>- 查找附近刷新点"));
    }

    /**
     * 格式化位置
     */
    private String formatLocation(Location loc) {
        return String.format("%s [%.1f, %.1f, %.1f]",
            loc.getWorld().getName(), loc.getX(), loc.getY(), loc.getZ());
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        List<String> completions = new ArrayList<>();

        if (args.length == 1) {
            completions.addAll(List.of("create", "delete", "info", "list", "tp", "spawn", "set", "enable", "disable", "near"));
        } else if (args.length == 2) {
            switch (args[0].toLowerCase()) {
                case "delete", "info", "tp", "spawn", "enable", "disable" ->
                    completions.addAll(plugin.getSpawnPointManager().getAllSpawnPoints().stream()
                        .map(SpawnPoint::getId)
                        .collect(Collectors.toList()));
                case "create" -> completions.add("<ID>");
                case "set" -> completions.addAll(plugin.getSpawnPointManager().getAllSpawnPoints().stream()
                    .map(SpawnPoint::getId)
                    .collect(Collectors.toList()));
            }
        } else if (args.length == 3) {
            switch (args[0].toLowerCase()) {
                case "create" -> completions.addAll(plugin.getMobManager().getAllMobs().stream()
                    .map(cn.guangdian.mobs.model.CustomMob::getId)
                    .collect(Collectors.toList()));
                case "set" -> completions.addAll(List.of("amount", "max-mobs", "cooldown", "radius", "level"));
            }
        }

        return completions.stream()
            .filter(s -> s.toLowerCase().startsWith(args[args.length - 1].toLowerCase()))
            .collect(Collectors.toList());
    }
}
