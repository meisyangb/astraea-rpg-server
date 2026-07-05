package cn.guangdian.custommodels.command;

import cn.guangdian.custommodels.factory.CustomItemFactory;
import cn.guangdian.custommodels.model.ModelGenerator;
import cn.guangdian.custommodels.registry.CustomItemRegistry;
import cn.guangdian.custommodels.resourcepack.ResourcePackGenerator;
import cn.guangdian.custommodels.texture.TextureManager;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * 模型命令处理器
 * 处理所有插件命令
 *
 * 修复：
 * 1. 添加权限检查
 * 2. scan/generate 使用异步任务避免阻塞主线程
 * 3. reload 命令完整重载所有模块
 */
public class ModelCommand implements CommandExecutor, TabCompleter {

    private final JavaPlugin plugin;
    private final TextureManager textureManager;
    private final ModelGenerator modelGenerator;
    private final ResourcePackGenerator resourcePackGenerator;
    private final CustomItemRegistry itemRegistry;
    private final CustomItemFactory itemFactory;

    public ModelCommand(JavaPlugin plugin, TextureManager textureManager, ModelGenerator modelGenerator,
                        ResourcePackGenerator resourcePackGenerator, CustomItemRegistry itemRegistry,
                        CustomItemFactory itemFactory) {
        this.plugin = plugin;
        this.textureManager = textureManager;
        this.modelGenerator = modelGenerator;
        this.resourcePackGenerator = resourcePackGenerator;
        this.itemRegistry = itemRegistry;
        this.itemFactory = itemFactory;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length == 0) {
            sendHelp(sender);
            return true;
        }

        String subCommand = args[0].toLowerCase();

        switch (subCommand) {
            case "scan":
                if (!hasAdminPermission(sender)) return true;
                return handleScan(sender);
            case "generate":
                if (!hasAdminPermission(sender)) return true;
                return handleGenerate(sender);
            case "pack":
                if (!hasAdminPermission(sender)) return true;
                return handlePack(sender);
            case "list":
                return handleList(sender, args);
            case "give":
                if (!hasGivePermission(sender)) return true;
                return handleGive(sender, args);
            case "reload":
                if (!hasAdminPermission(sender)) return true;
                return handleReload(sender);
            case "info":
                return handleItemInfo(sender, args);
            case "help":
                sendHelp(sender);
                return true;
            default:
                sender.sendMessage(ChatColor.RED + "未知命令，使用 /custommodels help 查看帮助");
                return true;
        }
    }

    /**
     * 权限检查 — 管理员权限
     */
    private boolean hasAdminPermission(CommandSender sender) {
        if (!sender.hasPermission("custommodels.admin")) {
            sender.sendMessage(ChatColor.RED + "你没有权限使用此命令 (需要 custommodels.admin)");
            return false;
        }
        return true;
    }

    /**
     * 权限检查 — give 权限
     */
    private boolean hasGivePermission(CommandSender sender) {
        if (!sender.hasPermission("custommodels.give") && !sender.hasPermission("custommodels.admin")) {
            sender.sendMessage(ChatColor.RED + "你没有权限使用此命令 (需要 custommodels.give 或 custommodels.admin)");
            return false;
        }
        return true;
    }

    /**
     * 扫描贴图 — 异步执行，避免阻塞主线程
     */
    private boolean handleScan(CommandSender sender) {
        sender.sendMessage(ChatColor.GREEN + "开始异步扫描贴图...");

        plugin.getServer().getScheduler().runTaskAsynchronously(plugin, () -> {
            textureManager.scanTextures();

            int count = textureManager.getTextureCount();

            // 统计信息需要在主线程发送给玩家
            plugin.getServer().getScheduler().runTask(plugin, () -> {
                sender.sendMessage(ChatColor.GREEN + "扫描完成，发现 " + count + " 个贴图");

                // 打印分类统计
                sender.sendMessage(ChatColor.GOLD + "========== 分类统计 ==========");
                for (Map.Entry<String, List<TextureManager.TextureInfo>> entry : textureManager.getCategoryMap().entrySet()) {
                    sender.sendMessage(ChatColor.YELLOW + entry.getKey() + ": " + ChatColor.WHITE + entry.getValue().size() + " 个");
                }
                sender.sendMessage(ChatColor.GOLD + "================================");

                // 扫描完成后自动注册物品
                itemRegistry.generateItemDefinitionsFromTextures(textureManager);
                sender.sendMessage(ChatColor.GREEN + "物品注册完成，共 " + itemRegistry.getItemCount() + " 个");
            });
        });

        return true;
    }

    /**
     * 生成模型 — 异步执行
     */
    private boolean handleGenerate(CommandSender sender) {
        sender.sendMessage(ChatColor.GREEN + "开始异步生成模型...");

        plugin.getServer().getScheduler().runTaskAsynchronously(plugin, () -> {
            modelGenerator.generateModels(textureManager);
            int count = modelGenerator.getModelCount();

            plugin.getServer().getScheduler().runTask(plugin, () -> {
                sender.sendMessage(ChatColor.GREEN + "模型生成完成，共 " + count + " 个");
            });
        });

        return true;
    }

    /**
     * 打包资源包 — 异步执行
     */
    private boolean handlePack(CommandSender sender) {
        sender.sendMessage(ChatColor.GREEN + "开始异步生成资源包...");

        // 使用异步任务生成资源包
        plugin.getServer().getScheduler().runTaskAsynchronously(plugin, () -> {
            try {
                // 生成模型JSON文件
                modelGenerator.generateModels(textureManager);

                // 打包资源包
                File resourcePack = resourcePackGenerator.generatePack(textureManager, modelGenerator);

                if (resourcePack != null && resourcePack.exists()) {
                    plugin.getServer().getScheduler().runTask(plugin, () -> {
                        sender.sendMessage(ChatColor.GREEN + "资源包生成成功！");
                        sender.sendMessage(ChatColor.YELLOW + "路径: " + ChatColor.WHITE + resourcePack.getAbsolutePath());
                        sender.sendMessage(ChatColor.YELLOW + "大小: " + ChatColor.WHITE + (resourcePack.length() / 1024 / 1024) + " MB");
                        sender.sendMessage(ChatColor.AQUA + "资源包将自动发送给在线玩家");

                        // 设置资源包文件给发送监听器
                        cn.guangdian.custommodels.GuangDianCustomModels mainPlugin =
                                cn.guangdian.custommodels.GuangDianCustomModels.getInstance();
                        if (mainPlugin != null && mainPlugin.getResourcePackSender() != null) {
                            mainPlugin.getResourcePackSender().setResourcePackFile(resourcePack);
                        }
                    });
                } else {
                    plugin.getServer().getScheduler().runTask(plugin, () -> {
                        sender.sendMessage(ChatColor.RED + "资源包生成失败");
                    });
                }
            } catch (Exception e) {
                plugin.getServer().getScheduler().runTask(plugin, () -> {
                    sender.sendMessage(ChatColor.RED + "资源包生成异常: " + e.getMessage());
                });
            }
        });
        return true;
    }

    /**
     * 列出物品
     */
    private boolean handleList(CommandSender sender, String[] args) {
        if (args.length == 1) {
            // 列出所有物品
            sender.sendMessage(ChatColor.GOLD + "========== 自定义物品列表 ==========");
            int index = 1;
            for (Map.Entry<String, CustomItemRegistry.CustomItemDefinition> entry : itemRegistry.getAllDefinitions().entrySet()) {
                sender.sendMessage(ChatColor.YELLOW + String.valueOf(index) + ". " + ChatColor.WHITE + entry.getKey());
                index++;
            }
            sender.sendMessage(ChatColor.GOLD + "共 " + ChatColor.WHITE + itemRegistry.getItemCount() + ChatColor.GOLD + " 个物品");
            sender.sendMessage(ChatColor.GOLD + "===================================");
        } else if (args.length == 2) {
            // 列出指定物品详情
            String itemId = args[1];
            CustomItemRegistry.CustomItemDefinition definition = itemRegistry.getDefinition(itemId);

            if (definition == null) {
                sender.sendMessage(ChatColor.RED + "物品不存在: " + itemId);
                return true;
            }

            sender.sendMessage(ChatColor.GOLD + "========== 物品详情 ==========");
            sender.sendMessage(ChatColor.YELLOW + "ID: " + ChatColor.WHITE + definition.getId());
            sender.sendMessage(ChatColor.YELLOW + "名称: " + ChatColor.WHITE + definition.getDisplayName());
            sender.sendMessage(ChatColor.YELLOW + "材质: " + ChatColor.WHITE + definition.getMaterial());
            sender.sendMessage(ChatColor.YELLOW + "模型数据: " + ChatColor.WHITE + definition.getCustomModelData());
            sender.sendMessage(ChatColor.YELLOW + "贴图: " + ChatColor.WHITE + definition.getTexture());
            sender.sendMessage(ChatColor.YELLOW + "模板: " + ChatColor.WHITE + definition.getModelTemplate());
            sender.sendMessage(ChatColor.GOLD + "==============================");
        }

        return true;
    }

    /**
     * 给予物品
     */
    private boolean handleGive(CommandSender sender, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage(ChatColor.RED + "只有玩家可以使用此命令");
            return true;
        }

        if (args.length < 2) {
            sender.sendMessage(ChatColor.RED + "用法: /custommodels give <物品ID> [数量]");
            return true;
        }

        Player player = (Player) sender;
        String itemId = args[1];
        int amount = 1;

        if (args.length >= 3) {
            try {
                amount = Integer.parseInt(args[2]);
            } catch (NumberFormatException e) {
                sender.sendMessage(ChatColor.RED + "数量必须是数字");
                return true;
            }
        }

        ItemStack item = itemFactory.createItem(itemId, amount);

        if (item == null) {
            sender.sendMessage(ChatColor.RED + "物品创建失败: " + itemId);
            return true;
        }

        player.getInventory().addItem(item);
        sender.sendMessage(ChatColor.GREEN + "已获得 " + ChatColor.WHITE + amount + ChatColor.GREEN + " 个 " + ChatColor.WHITE + itemId);

        return true;
    }

    /**
     * 重载配置 — 完整重载所有模块
     *
     * 修复：重新加载配置 → 重新扫描贴图 → 重新注册物品 → 重新生成模型 → 重新打包资源包
     */
    private boolean handleReload(CommandSender sender) {
        sender.sendMessage(ChatColor.GREEN + "开始完整重载所有模块...");

        // 1. 重新加载配置
        plugin.reloadConfig();
        cn.guangdian.custommodels.GuangDianCustomModels mainPlugin =
                cn.guangdian.custommodels.GuangDianCustomModels.getInstance();
        if (mainPlugin != null) {
            mainPlugin.getConfigManager().loadConfig();
        }
        sender.sendMessage(ChatColor.YELLOW + "配置已重载");

        // 2. 重新扫描贴图（异步）
        plugin.getServer().getScheduler().runTaskAsynchronously(plugin, () -> {
            textureManager.scanTextures();
            plugin.getLogger().info("贴图重新扫描完成: " + textureManager.getTextureCount() + " 个");

            // 3. 重新注册物品
            itemRegistry.generateItemDefinitionsFromTextures(textureManager);
            plugin.getLogger().info("物品重新注册完成: " + itemRegistry.getItemCount() + " 个");

            // 4. 重新生成模型
            modelGenerator.generateModels(textureManager);
            plugin.getLogger().info("模型重新生成完成: " + modelGenerator.getModelCount() + " 个");

            // 5. 重新打包资源包
            File resourcePack = resourcePackGenerator.generatePack(textureManager, modelGenerator);
            if (resourcePack != null && resourcePack.exists()) {
                plugin.getLogger().info("资源包重新打包完成: " + resourcePack.getAbsolutePath());

                // 设置资源包文件（主线程）
                plugin.getServer().getScheduler().runTask(plugin, () -> {
                    if (mainPlugin != null && mainPlugin.getResourcePackSender() != null) {
                        mainPlugin.getResourcePackSender().setResourcePackFile(resourcePack);
                    }
                    sender.sendMessage(ChatColor.GREEN + "所有模块已完整重载！");
                    sender.sendMessage(ChatColor.YELLOW + "贴图数量: " + ChatColor.WHITE + textureManager.getTextureCount());
                    sender.sendMessage(ChatColor.YELLOW + "物品数量: " + ChatColor.WHITE + itemRegistry.getItemCount());
                    sender.sendMessage(ChatColor.YELLOW + "模型数量: " + ChatColor.WHITE + modelGenerator.getModelCount());
                    sender.sendMessage(ChatColor.YELLOW + "资源包: " + ChatColor.WHITE + resourcePack.getName());
                });
            } else {
                plugin.getServer().getScheduler().runTask(plugin, () -> {
                    sender.sendMessage(ChatColor.GREEN + "重载完成（资源包生成失败）");
                    sender.sendMessage(ChatColor.YELLOW + "贴图数量: " + ChatColor.WHITE + textureManager.getTextureCount());
                    sender.sendMessage(ChatColor.YELLOW + "物品数量: " + ChatColor.WHITE + itemRegistry.getItemCount());
                });
            }
        });

        return true;
    }

    /**
     * 查看物品详细信息
     */
    private boolean handleItemInfo(CommandSender sender, String[] args) {
        if (args.length < 2) {
            // 显示插件整体信息
            sender.sendMessage(ChatColor.GOLD + "========== GuangDianCustomModels 信息 ==========");
            sender.sendMessage(ChatColor.YELLOW + "版本: " + ChatColor.WHITE + plugin.getDescription().getVersion());
            sender.sendMessage(ChatColor.YELLOW + "贴图数量: " + ChatColor.WHITE + textureManager.getTextureCount());
            sender.sendMessage(ChatColor.YELLOW + "模型数量: " + ChatColor.WHITE + modelGenerator.getModelCount());
            sender.sendMessage(ChatColor.YELLOW + "物品数量: " + ChatColor.WHITE + itemRegistry.getItemCount());
            sender.sendMessage(ChatColor.GOLD + "==============================================");
            sender.sendMessage(ChatColor.AQUA + "使用 /custommodels info <物品ID> 查看物品详细信息");
            return true;
        }

        String itemId = args[1];
        CustomItemRegistry.CustomItemDefinition definition = itemRegistry.getDefinition(itemId);

        if (definition == null) {
            sender.sendMessage(ChatColor.RED + "物品不存在: " + args[1]);
            sender.sendMessage(ChatColor.YELLOW + "提示: 物品ID格式为贴图名去除扩展名并小写化");
            sender.sendMessage(ChatColor.YELLOW + "示例: 贴图 '1SD100003_ICON1_1.png' → 物品ID '1sd100003_icon1_1'");
            return true;
        }

        sender.sendMessage(ChatColor.GOLD + "========== 物品详情 ==========");
        sender.sendMessage(ChatColor.YELLOW + "ID: " + ChatColor.WHITE + definition.getId());
        sender.sendMessage(ChatColor.YELLOW + "名称: " + ChatColor.WHITE + definition.getDisplayName());
        sender.sendMessage(ChatColor.YELLOW + "材质: " + ChatColor.WHITE + definition.getMaterial());
        sender.sendMessage(ChatColor.YELLOW + "模型数据: " + ChatColor.WHITE + definition.getCustomModelData());
        sender.sendMessage(ChatColor.YELLOW + "贴图: " + ChatColor.WHITE + definition.getTexture());
        sender.sendMessage(ChatColor.YELLOW + "模板: " + ChatColor.WHITE + definition.getModelTemplate());
        sender.sendMessage(ChatColor.GOLD + "==============================");

        return true;
    }

    /**
     * 发送帮助信息
     */
    private void sendHelp(CommandSender sender) {
        sender.sendMessage(ChatColor.GOLD + "========== GuangDianCustomModels 帮助 ==========");
        sender.sendMessage(ChatColor.YELLOW + "/custommodels scan " + ChatColor.GRAY + "- 扫描贴图目录 [admin]");
        sender.sendMessage(ChatColor.YELLOW + "/custommodels generate " + ChatColor.GRAY + "- 生成模型JSON [admin]");
        sender.sendMessage(ChatColor.YELLOW + "/custommodels pack " + ChatColor.GRAY + "- 打包资源包 [admin]");
        sender.sendMessage(ChatColor.YELLOW + "/custommodels list " + ChatColor.GRAY + "- 列出所有物品");
        sender.sendMessage(ChatColor.YELLOW + "/custommodels list <ID> " + ChatColor.GRAY + "- 查看物品详情");
        sender.sendMessage(ChatColor.YELLOW + "/custommodels give <ID> [数量] " + ChatColor.GRAY + "- 给予物品 [give]");
        sender.sendMessage(ChatColor.YELLOW + "/custommodels reload " + ChatColor.GRAY + "- 完整重载所有模块 [admin]");
        sender.sendMessage(ChatColor.YELLOW + "/custommodels info " + ChatColor.GRAY + "- 查看插件信息");
        sender.sendMessage(ChatColor.YELLOW + "/custommodels info <物品ID> " + ChatColor.GRAY + "- 查看物品详情");
        sender.sendMessage(ChatColor.YELLOW + "/custommodels help " + ChatColor.GRAY + "- 显示帮助");
        sender.sendMessage(ChatColor.GOLD + "==============================================");
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        List<String> completions = new ArrayList<>();

        if (args.length == 1) {
            completions.add("scan");
            completions.add("generate");
            completions.add("pack");
            completions.add("list");
            completions.add("give");
            completions.add("reload");
            completions.add("info");
            completions.add("help");
        } else if (args.length == 2) {
            if (args[0].equalsIgnoreCase("list") || args[0].equalsIgnoreCase("give") || args[0].equalsIgnoreCase("info")) {
                // 补全物品ID
                for (String itemId : itemRegistry.getAllDefinitions().keySet()) {
                    completions.add(itemId);
                }
            }
        }

        return completions;
    }
}
