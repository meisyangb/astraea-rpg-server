package cn.guangdian.mobs.command;

import cn.guangdian.mobs.GuangDianMobs;
import cn.guangdian.mobs.model.CustomMob;
import cn.guangdian.rpgcore.message.MiniMessageService;
import org.bukkit.Location;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 怪物管理命令
 */
public class MobCommand implements CommandExecutor, TabCompleter {

    private final GuangDianMobs plugin;

    public MobCommand(GuangDianMobs plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length == 0) {
            sendHelp(sender);
            return true;
        }

        switch (args[0].toLowerCase()) {
            case "spawn" -> handleSpawn(sender, args);
            case "kill" -> handleKill(sender, args);
            case "reload" -> handleReload(sender);
            case "list" -> handleList(sender);
            default -> sendHelp(sender);
        }

        return true;
    }

    /**
     * 处理生成命令
     */
    private void handleSpawn(CommandSender sender, String[] args) {
        MiniMessageService mm = MiniMessageService.getInstance();

        if (!(sender instanceof Player player)) {
            sender.sendMessage(mm.red("只有玩家可以使用此命令"));
            return;
        }

        if (args.length < 2) {
            sender.sendMessage(mm.colorize("<red>用法: /gdmm spawn <怪物ID> [数量]"));
            return;
        }

        String mobId = args[1];
        int amount = 1;
        if (args.length >= 3) {
            try {
                amount = Integer.parseInt(args[2]);
            } catch (NumberFormatException e) {
                sender.sendMessage(mm.red("数量必须是数字"));
                return;
            }
        }

        CustomMob mobTemplate = plugin.getMobManager().getMobTemplate(mobId);
        if (mobTemplate == null) {
            sender.sendMessage(mm.red("怪物不存在: " + mobId));
            return;
        }

        Location loc = player.getLocation();
        for (int i = 0; i < amount; i++) {
            LivingEntity entity = plugin.getMobManager().spawnMob(mobId, loc);
            if (entity == null) {
                sender.sendMessage(mm.red("生成失败"));
                return;
            }
        }

        sender.sendMessage(mm.green("成功生成 " + amount + " 个 " + mobTemplate.getDisplayName()));
    }

    /**
     * 处理清除命令
     */
    private void handleKill(CommandSender sender, String[] args) {
        MiniMessageService mm = MiniMessageService.getInstance();

        if (!(sender instanceof Player player)) {
            sender.sendMessage(mm.red("只有玩家可以使用此命令"));
            return;
        }

        String mobId = args.length >= 2 ? args[1] : null;
        int radius = 50;
        if (args.length >= 3) {
            try {
                radius = Integer.parseInt(args[2]);
            } catch (NumberFormatException e) {
                sender.sendMessage(mm.red("半径必须是数字"));
                return;
            }
        }

        Location loc = player.getLocation();
        int killed = 0;

        for (org.bukkit.entity.Entity entity : loc.getWorld().getEntities()) {
            if (entity instanceof LivingEntity living) {
                String entityMobId = plugin.getMobManager().getMobIdFromEntity(living);
                if (entityMobId != null) {
                    if (mobId == null || mobId.equals(entityMobId)) {
                        if (entity.getLocation().distance(loc) <= radius) {
                            entity.remove();
                            killed++;
                        }
                    }
                }
            }
        }

        sender.sendMessage(mm.green("已清除 " + killed + " 个怪物"));
    }

    /**
     * 处理重载命令
     */
    private void handleReload(CommandSender sender) {
        MiniMessageService mm = MiniMessageService.getInstance();

        plugin.getMobManager().loadMobs();
        plugin.getSkillManager().loadSkills();
        plugin.getDropManager().loadDrops();
        plugin.getSpawnManager().loadSpawns();

        sender.sendMessage(mm.green("配置已重载"));
    }

    /**
     * 处理列表命令
     */
    private void handleList(CommandSender sender) {
        MiniMessageService mm = MiniMessageService.getInstance();

        sender.sendMessage(mm.colorize("<gold><bold>===== 怪物列表 ====="));
        for (CustomMob mob : plugin.getMobManager().getAllMobs()) {
            sender.sendMessage(mm.colorize("<gray>- <white>" + mob.getId() + " <gray>(" + mob.getDisplayName() + "<gray>)"));
        }
        sender.sendMessage(mm.colorize("<gold>共 " + plugin.getMobManager().getMobCount() + " 个怪物"));
    }

    /**
     * 发送帮助信息
     */
    private void sendHelp(CommandSender sender) {
        MiniMessageService mm = MiniMessageService.getInstance();

        sender.sendMessage(mm.colorize("<gold><bold>===== GuangDianMobs 命令 ====="));
        sender.sendMessage(mm.colorize("<gray>/gdmm spawn <怪物ID> [数量] <white>- 生成怪物"));
        sender.sendMessage(mm.colorize("<gray>/gdmm kill [怪物ID] [半径] <white>- 清除怪物"));
        sender.sendMessage(mm.colorize("<gray>/gdmm reload <white>- 重载配置"));
        sender.sendMessage(mm.colorize("<gray>/gdmm list <white>- 列出所有怪物"));
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        List<String> completions = new ArrayList<>();

        if (args.length == 1) {
            completions.add("spawn");
            completions.add("kill");
            completions.add("reload");
            completions.add("list");
        } else if (args.length == 2) {
            if (args[0].equalsIgnoreCase("spawn") || args[0].equalsIgnoreCase("kill")) {
                completions.addAll(plugin.getMobManager().getAllMobs().stream()
                    .map(CustomMob::getId)
                    .collect(Collectors.toList()));
            }
        }

        return completions.stream()
            .filter(s -> s.toLowerCase().startsWith(args[args.length - 1].toLowerCase()))
            .collect(Collectors.toList());
    }
}
