package cn.guangdian.rpgitems.command;

import cn.guangdian.rpgcore.message.MiniMessageService;
import cn.guangdian.rpgitems.RPGItems;
import cn.guangdian.rpgitems.registry.ItemRegistry;
import cn.guangdian.rpgitems.service.ItemService;
import cn.guangdian.rpgitems.template.ItemTemplate;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 物品命令
 */
public class ItemCommand implements CommandExecutor, TabCompleter {

    private final RPGItems plugin;
    private final ItemService itemService;
    private final ItemRegistry itemRegistry;
    private final MiniMessageService miniMessage;

    public ItemCommand(RPGItems plugin, ItemService itemService, ItemRegistry itemRegistry) {
        this.plugin = plugin;
        this.itemService = itemService;
        this.itemRegistry = itemRegistry;
        this.miniMessage = MiniMessageService.getInstance();
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length < 1) {
            sendHelp(sender);
            return true;
        }

        String subCommand = args[0].toLowerCase();

        switch (subCommand) {
            case "give" -> handleGive(sender, args);
            case "reload" -> handleReload(sender);
            case "list" -> handleList(sender, args);
            case "info" -> handleInfo(sender, args);
            default -> sendHelp(sender);
        }

        return true;
    }

    private void handleGive(CommandSender sender, String[] args) {
        if (!sender.hasPermission("rpgitems.admin")) {
            sender.sendMessage(miniMessage.red("你没有权限使用此命令"));
            return;
        }

        if (args.length < 3) {
            sender.sendMessage(miniMessage.colorize("<red>用法: /rpgitem give <玩家> <物品ID> [数量]"));
            return;
        }

        Player target = Bukkit.getPlayer(args[1]);
        if (target == null) {
            sender.sendMessage(miniMessage.red("玩家不存在: " + args[1]));
            return;
        }

        String itemId = args[2];
        int amount = 1;
        if (args.length >= 4) {
            try {
                amount = Integer.parseInt(args[3]);
            } catch (NumberFormatException e) {
                sender.sendMessage(miniMessage.red("无效的数量"));
                return;
            }
        }

        boolean success = itemService.giveItem(target, itemId, amount);
        if (success) {
            sender.sendMessage(miniMessage.colorize(
                    String.format("<green>已给予 <yellow>%s <green>物品 <yellow>%s <gray>x%d",
                            target.getName(), itemId, amount)));
        } else {
            sender.sendMessage(miniMessage.red("物品不存在: " + itemId));
        }
    }

    private void handleReload(CommandSender sender) {
        if (!sender.hasPermission("rpgitems.admin")) {
            sender.sendMessage(miniMessage.red("你没有权限使用此命令"));
            return;
        }

        // 重载配置
        plugin.getConfigManager().reload();

        // 重新加载物品注册表
        itemRegistry.clear();
        itemRegistry.loadFromConfig(plugin.getConfigManager().getItemConfig());

        sender.sendMessage(miniMessage.green("RPGItems 配置已重载"));
        sender.sendMessage(miniMessage.colorize("<gray>已加载 <gold>" + itemRegistry.getItemCount() + " <gray>个物品"));
    }

    private void handleList(CommandSender sender, String[] args) {
        if (!sender.hasPermission("rpgitems.admin")) {
            sender.sendMessage(miniMessage.red("你没有权限使用此命令"));
            return;
        }

        Map<String, ItemTemplate> items = itemRegistry.getAllItems();

        // 过滤参数
        String materialFilter = args.length >= 2 ? args[1].toUpperCase() : null;
        Material filterMaterial = null;
        if (materialFilter != null) {
            try {
                filterMaterial = Material.valueOf(materialFilter);
            } catch (IllegalArgumentException ignored) {
            }
        }

        sender.sendMessage(miniMessage.colorize("<gold><bold>===== RPG物品列表 ====="));
        sender.sendMessage(miniMessage.colorize("<gray>共 <gold>" + items.size() + " <gray>个物品"));

        if (materialFilter != null && filterMaterial == null) {
            sender.sendMessage(miniMessage.colorize("<yellow>过滤条件: <white>" + materialFilter));
        }

        int index = 0;
        for (Map.Entry<String, ItemTemplate> entry : items.entrySet()) {
            ItemTemplate template = entry.getValue();

            // 应用过滤
            if (filterMaterial != null && template.getMaterial() != filterMaterial) continue;

            index++;
            String itemId = entry.getKey();
            String materialName = template.getMaterial().name();
            int skillCount = template.getSkillBindings().size();

            String skillStr = skillCount > 0 ? "<aqua>" + skillCount + "技能" : "<gray>无技能";

            sender.sendMessage(miniMessage.colorize(
                    String.format("<gray>%d. <gold>%s <gray>- <white>%s <gray>[%s]", 
                        index, itemId, materialName, skillStr)));
        }

        if (index == 0) {
            sender.sendMessage(miniMessage.colorize("<red>没有匹配的物品"));
        }

        sender.sendMessage(miniMessage.colorize(""));
        sender.sendMessage(miniMessage.colorize("<gray>用法: /rpgitem list [材料类型]"));
    }

    private void handleInfo(CommandSender sender, String[] args) {
        if (!sender.hasPermission("rpgitems.admin")) {
            sender.sendMessage(miniMessage.red("你没有权限使用此命令"));
            return;
        }

        if (args.length < 2) {
            sender.sendMessage(miniMessage.colorize("<red>用法: /rpgitem info <物品ID>"));
            return;
        }

        String itemId = args[1];
        ItemTemplate template = itemRegistry.getItem(itemId).orElse(null);

        if (template == null) {
            sender.sendMessage(miniMessage.red("物品不存在: " + itemId));
            return;
        }

        sender.sendMessage(miniMessage.colorize("<gold><bold>===== 物品详情 ====="));
        sender.sendMessage(miniMessage.colorize("<gray>ID: <gold>" + itemId));
        sender.sendMessage(miniMessage.colorize("<gray>材料: <white>" + template.getMaterial().name()));
        sender.sendMessage(miniMessage.colorize("<gray>显示名: <white>" + 
            net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer.legacySection()
                .serialize(template.getDisplayName())));

        // 技能绑定
        if (!template.getSkillBindings().isEmpty()) {
            sender.sendMessage(miniMessage.colorize("<gray>技能绑定:"));
            for (ItemTemplate.SkillBinding binding : template.getSkillBindings()) {
                sender.sendMessage(miniMessage.colorize(
                    String.format("  <dark_gray>- <yellow>%s <gray>触发: <aqua>%s <gray>冷却: <white>%ds",
                        binding.getSkillId(), binding.getTrigger(), binding.getCooldown())));
            }
        } else {
            sender.sendMessage(miniMessage.colorize("<gray>技能绑定: <gray>无"));
        }

        // 附魔
        if (!template.getEnchantments().isEmpty()) {
            sender.sendMessage(miniMessage.colorize("<gray>附魔:"));
            for (String enchant : template.getEnchantments()) {
                sender.sendMessage(miniMessage.colorize("  <dark_gray>- <green>" + enchant));
            }
        }

        // 选项
        ItemTemplate.ItemOptions options = template.getOptions();
        if (options != null) {
            sender.sendMessage(miniMessage.colorize("<gray>选项:"));
            sender.sendMessage(miniMessage.colorize("  <dark_gray>- 不可破坏: <white>" + options.isUnbreakable()));
            sender.sendMessage(miniMessage.colorize("  <dark_gray>- 隐藏属性: <white>" + options.isHideAttributes()));
            if (options.getCustomModelData() != null) {
                sender.sendMessage(miniMessage.colorize("  <dark_gray>- 自定义模型数据: <white>" + options.getCustomModelData()));
            }
        }
    }

    private void sendHelp(CommandSender sender) {
        sender.sendMessage(miniMessage.colorize("<gold><bold>===== RPGItems 命令 ====="));
        sender.sendMessage(miniMessage.colorize("<gray>/rpgitem give <玩家> <物品ID> [数量] <green>- 给予物品"));
        sender.sendMessage(miniMessage.colorize("<gray>/rpgitem reload <green>- 重载配置"));
        sender.sendMessage(miniMessage.colorize("<gray>/rpgitem list [材料] <green>- 列出物品"));
        sender.sendMessage(miniMessage.colorize("<gray>/rpgitem info <物品ID> <green>- 查看物品详情"));
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        List<String> completions = new ArrayList<>();

        if (args.length == 1) {
            completions.addAll(List.of("give", "reload", "list", "info"));
        } else if (args.length == 2) {
            switch (args[0].toLowerCase()) {
                case "give" -> completions.addAll(
                    Bukkit.getOnlinePlayers().stream()
                        .map(Player::getName)
                        .collect(Collectors.toList())
                );
                case "list" -> completions.addAll(
                    List.of("DIAMOND", "IRON", "GOLD", "NETHERITE", "LEATHER", "WOOD", "STONE")
                );
                case "info" -> completions.addAll(itemRegistry.getAllItems().keySet());
            }
        } else if (args.length == 3) {
            if (args[0].equalsIgnoreCase("give")) {
                completions.addAll(itemRegistry.getAllItems().keySet());
            }
        }

        return completions.stream()
            .filter(s -> s.toLowerCase().startsWith(args[args.length - 1].toLowerCase()))
            .collect(Collectors.toList());
    }
}
