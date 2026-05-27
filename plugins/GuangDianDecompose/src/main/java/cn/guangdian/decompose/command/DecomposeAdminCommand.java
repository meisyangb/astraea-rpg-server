package cn.guangdian.decompose.command;

import cn.guangdian.decompose.GuangDianDecompose;
import cn.guangdian.decompose.model.DecomposeRule;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class DecomposeAdminCommand implements CommandExecutor, TabCompleter {

    private final GuangDianDecompose plugin;

    public DecomposeAdminCommand(GuangDianDecompose plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.hasPermission("guangdian.decompose.admin")) {
            sender.sendMessage("§c你没有权限执行此命令!");
            return true;
        }

        if (args.length == 0) {
            sendHelp(sender);
            return true;
        }

        switch (args[0].toLowerCase()) {
            case "reload" -> {
                plugin.reloadAllConfig();
                sender.sendMessage("§a配置已重载!");
            }
            case "set" -> handleSetCommand(sender, args);
            case "list" -> handleListCommand(sender);
            case "info" -> handleInfoCommand(sender, args);
            default -> sendHelp(sender);
        }

        return true;
    }

    private void handleSetCommand(CommandSender sender, String[] args) {
        if (args.length < 4) {
            sender.sendMessage("§c用法: /decomposeadmin set <物品ID> <材料> <数量>");
            sender.sendMessage("§7材料格式: mm:物品ID 或 vanilla:材料名");
            return;
        }

        String itemId = args[1];
        String materialStr = args[2];
        int amount;

        try {
            amount = Integer.parseInt(args[3]);
        } catch (NumberFormatException e) {
            sender.sendMessage("§c数量必须是整数!");
            return;
        }

        DecomposeRule rule = plugin.getRuleManager().getRule(itemId);
        if (rule == null) {
            rule = new DecomposeRule(itemId);
        }

        String[] parts = materialStr.split(":");
        if (parts.length < 2) {
            sender.sendMessage("§c材料格式错误! 应为 type:id");
            return;
        }

        String type = parts[0];
        String materialId = parts[1];
        rule.addMaterial(new DecomposeRule.MaterialReward(type, materialId, amount));

        plugin.getRuleManager().saveRule(itemId, rule);
        sender.sendMessage("§a已添加分解规则: " + itemId + " -> " + materialStr + " x" + amount);
    }

    private void handleListCommand(CommandSender sender) {
        sender.sendMessage("§6=== 分解规则列表 ===");
        for (Map.Entry<String, DecomposeRule> entry : plugin.getRuleManager().getAllRules().entrySet()) {
            DecomposeRule rule = entry.getValue();
            StringBuilder materials = new StringBuilder();
            for (int i = 0; i < rule.getMaterials().size(); i++) {
                if (i > 0) materials.append(", ");
                DecomposeRule.MaterialReward mat = rule.getMaterials().get(i);
                materials.append(mat.getItemId()).append("x").append(mat.getAmount());
            }
            sender.sendMessage("§e" + entry.getKey() + " §7-> §f" + materials);
        }
    }

    private void handleInfoCommand(CommandSender sender, String[] args) {
        if (args.length < 2) {
            sender.sendMessage("§c用法: /decomposeadmin info <物品ID>");
            return;
        }

        String itemId = args[1];
        DecomposeRule rule = plugin.getRuleManager().getRule(itemId);

        if (rule == null) {
            sender.sendMessage("§c未找到该物品的分解规则!");
            return;
        }

        sender.sendMessage("§6=== " + itemId + " 分解信息 ===");
        sender.sendMessage("§e成功率: §f" + (int)(rule.getChance() * 100) + "%");
        sender.sendMessage("§e材料:");
        for (DecomposeRule.MaterialReward mat : rule.getMaterials()) {
            sender.sendMessage("  §7- §f" + mat.getType() + ":" + mat.getItemId() + " x" + mat.getAmount());
        }
        if (rule.hasCustomMessage()) {
            sender.sendMessage("§e消息: §f" + rule.getMessage());
        }
    }

    private void sendHelp(CommandSender sender) {
        sender.sendMessage("§6=== 装备分解管理 ===");
        sender.sendMessage("§e/decomposeadmin reload §7- 重载配置");
        sender.sendMessage("§e/decomposeadmin list §7- 列出所有规则");
        sender.sendMessage("§e/decomposeadmin info <物品ID> §7- 查看规则详情");
        sender.sendMessage("§e/decomposeadmin set <物品ID> <材料> <数量> §7- 添加分解材料");
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (!sender.hasPermission("guangdian.decompose.admin")) {
            return new ArrayList<>();
        }

        if (args.length == 1) {
            return Arrays.asList("reload", "list", "info", "set").stream()
                    .filter(s -> s.startsWith(args[0].toLowerCase()))
                    .collect(Collectors.toList());
        }

        if (args.length == 2 && (args[0].equalsIgnoreCase("info") || args[0].equalsIgnoreCase("set"))) {
            return new ArrayList<>(plugin.getRuleManager().getAllRules().keySet()).stream()
                    .filter(s -> s.startsWith(args[1].toLowerCase()))
                    .collect(Collectors.toList());
        }

        if (args.length == 3 && args[0].equalsIgnoreCase("set")) {
            return Arrays.asList("mm:", "vanilla:").stream()
                    .filter(s -> s.startsWith(args[2].toLowerCase()))
                    .collect(Collectors.toList());
        }

        return new ArrayList<>();
    }
}
