package cn.guangdian.socket.command;

import cn.guangdian.socket.GuangDianSocket;
import cn.guangdian.socket.gui.SocketInlayGUI;
import cn.guangdian.socket.parser.SocketParser;
import cn.guangdian.rpgcore.RPGCore;
import cn.guangdian.rpgcore.message.MiniMessageService;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextComponent;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.List;

public class SocketCommand implements CommandExecutor {

    private final GuangDianSocket plugin;
    private final MiniMessageService miniMessage;

    public SocketCommand(GuangDianSocket plugin) {
        this.plugin = plugin;
        RPGCore rpgCore = RPGCore.getInstance();
        this.miniMessage = rpgCore != null ? rpgCore.getMiniMessageService() : null;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            if (miniMessage != null) {
                sender.sendMessage(miniMessage.red("只有玩家可以使用此命令!"));
            } else {
                sender.sendMessage("只有玩家可以使用此命令!");
            }
            return true;
        }

        Player player = (Player) sender;

        if (args.length == 0 || args[0].equalsIgnoreCase("gui")) {
            SocketInlayGUI gui = new SocketInlayGUI(plugin, player);
            gui.open();
            return true;
        }

        if (args[0].equalsIgnoreCase("debug")) {
            return handleDebug(player);
        }

        if (args[0].equalsIgnoreCase("help")) {
            if (miniMessage != null) {
                player.sendMessage(miniMessage.gold("===== 宝石镶嵌帮助 ====="));
                player.sendMessage(miniMessage.colorize("<yellow>/socket <gray>- 打开镶嵌界面"));
                player.sendMessage(miniMessage.colorize("<yellow>/socket gui <gray>- 打开镶嵌界面"));
                player.sendMessage(miniMessage.colorize("<yellow>/socket reload <gray>- 重载配置"));
                player.sendMessage(miniMessage.colorize("<yellow>/socket debug <gray>- 调试手持物品"));
            } else {
                player.sendMessage("===== 宝石镶嵌帮助 =====");
                player.sendMessage("/socket - 打开镶嵌界面");
                player.sendMessage("/socket gui - 打开镶嵌界面");
                player.sendMessage("/socket reload - 重载配置");
                player.sendMessage("/socket debug - 调试手持物品");
            }
            return true;
        }

        if (args[0].equalsIgnoreCase("reload")) {
            if (!player.hasPermission("guangdiansocket.admin")) {
                if (miniMessage != null) {
                    player.sendMessage(miniMessage.red("你没有权限使用此命令!"));
                } else {
                    player.sendMessage("你没有权限使用此命令!");
                }
                return true;
            }

            plugin.reloadConfig();

            SocketParser.initialize(
                plugin.getConfig().getConfigurationSection("socket_patterns"),
                plugin.getConfig().getConfigurationSection("gem_types")
            );

            if (miniMessage != null) {
                player.sendMessage(miniMessage.green("GuangDianSocket 配置已重载!"));
            } else {
                player.sendMessage("GuangDianSocket 配置已重载!");
            }
            return true;
        }

        if (miniMessage != null) {
            player.sendMessage(miniMessage.red("用法: /socket [gui|help|reload|debug]"));
        } else {
            player.sendMessage("用法: /socket [gui|help|reload|debug]");
        }
        return true;
    }

    private boolean handleDebug(Player player) {
        ItemStack item = player.getInventory().getItemInMainHand();
        
        String playerName = player.getName();
        plugin.getLogger().info("========== 宝石孔调试 [" + playerName + "] ==========");
        player.sendMessage("§e========== 宝石孔调试 ==========");
        
        if (item == null || item.getType().isAir()) {
            plugin.getLogger().info("[" + playerName + "] 手中没有物品!");
            player.sendMessage("§c手中没有物品!");
            return true;
        }
        
        plugin.getLogger().info("[" + playerName + "] 物品类型: " + item.getType().name());
        player.sendMessage("§a物品类型: §f" + item.getType().name());
        
        if (!item.hasItemMeta()) {
            plugin.getLogger().info("[" + playerName + "] 物品没有 ItemMeta!");
            player.sendMessage("§c物品没有 ItemMeta!");
            return true;
        }
        
        ItemMeta meta = item.getItemMeta();
        
        if (meta.hasDisplayName()) {
            String displayName = PlainTextComponentSerializer.plainText().serialize(meta.displayName());
            plugin.getLogger().info("[" + playerName + "] 显示名称: " + displayName);
            player.sendMessage("§a显示名称: §f" + displayName);
        }
        
        if (!meta.hasLore()) {
            plugin.getLogger().info("[" + playerName + "] 物品没有 Lore!");
            player.sendMessage("§c物品没有 Lore!");
            return true;
        }
        
        List<Component> loreComponents = meta.lore();
        plugin.getLogger().info("[" + playerName + "] Lore 行数: " + (loreComponents != null ? loreComponents.size() : 0));
        player.sendMessage("§aLore 行数: §f" + (loreComponents != null ? loreComponents.size() : 0));
        plugin.getLogger().info("[" + playerName + "] ---------- Lore 原始内容 (Adventure Component) ----------");
        player.sendMessage("§e---------- Lore 原始内容 ----------");
        
        MiniMessage mm = MiniMessage.miniMessage();
        int lineNum = 0;
        
        if (loreComponents != null) {
            for (Component line : loreComponents) {
                lineNum++;
                String plainText = PlainTextComponentSerializer.plainText().serialize(line);
                String miniMsg = mm.serialize(line);
                
                if (plainText.contains("可镶嵌") || plainText.contains("宝石")) {
                    plugin.getLogger().info("[" + playerName + "] [行" + lineNum + "] (匹配)");
                    plugin.getLogger().info("[" + playerName + "]     PlainText: " + plainText);
                    plugin.getLogger().info("[" + playerName + "]     MiniMessage: " + miniMsg);
                    
                    player.sendMessage("§6[行" + lineNum + "] §b(匹配)");
                    player.sendMessage("§7    PlainText: §f" + plainText);
                    player.sendMessage("§7    MiniMessage: §f" + miniMsg);
                    
                    dumpComponentStructure(player, line, "    ");

                    // 不再从 Lore 解析宝石类型，改为从 PDC 读取
                    // Lore 只用于展示，不参与属性计算
                    player.sendMessage("§7    宝石类型: §e从 PDC 读取 (Lore 仅展示)");
                } else {
                    plugin.getLogger().info("[" + playerName + "] [行" + lineNum + "] PlainText: " + plainText);
                    player.sendMessage("§7[行" + lineNum + "] §f" + plainText);
                }
            }
        }
        
        plugin.getLogger().info("[" + playerName + "] ---------- 解析结果 ----------");
        player.sendMessage("§e---------- 解析结果 ----------");
        
        List<String> sockets = SocketParser.parseSocketGems(item);
        plugin.getLogger().info("[" + playerName + "] 识别到的宝石孔数量: " + sockets.size());
        player.sendMessage("§a识别到的宝石孔数量: §f" + sockets.size());
        
        if (!sockets.isEmpty()) {
            for (int i = 0; i < sockets.size(); i++) {
                plugin.getLogger().info("[" + playerName + "]   孔" + (i + 1) + ": " + sockets.get(i));
                player.sendMessage("§7  孔" + (i + 1) + ": §b" + sockets.get(i));
            }
        } else {
            plugin.getLogger().info("[" + playerName + "] 未识别到任何宝石孔!");
            plugin.getLogger().info("[" + playerName + "] 提示: 检查 Lore 格式是否为 [可镶嵌红宝石]");
            player.sendMessage("§c未识别到任何宝石孔!");
            player.sendMessage("§7提示: 检查 Lore 格式是否为 [可镶嵌红宝石]");
        }
        
        boolean isGem = SocketParser.isGem(item);
        plugin.getLogger().info("[" + playerName + "] 是否为宝石: " + (isGem ? "是" : "否"));
        player.sendMessage("§a是否为宝石: §f" + (isGem ? "§a是" : "§c否"));
        
        if (isGem) {
            String gemType = SocketParser.getGemType(item);
            plugin.getLogger().info("[" + playerName + "] 宝石类型: " + gemType);
            player.sendMessage("§a宝石类型: §f" + gemType);
        }
        
        plugin.getLogger().info("[" + playerName + "] =================================");
        player.sendMessage("§e=================================");
        return true;
    }
    
    private void dumpComponentStructure(Player player, Component component, String indent) {
        if (component == null) return;
        
        net.kyori.adventure.text.format.TextColor color = component.color();
        String colorStr = color != null ? color.toString() : "null";
        
        if (component instanceof TextComponent) {
            TextComponent textComp = (TextComponent) component;
            String content = textComp.content();
            
            if (!content.isEmpty()) {
                String namedColor = getNamedColorName(color);
                String logMsg = indent + "TextComponent: \"" + content + "\" color=" + colorStr + " (" + namedColor + ")";
                plugin.getLogger().info("[" + player.getName() + "] " + logMsg);
                player.sendMessage("§7" + indent + "§f\"" + content + "\" §8color=" + colorStr);
            }
        }
        
        List<Component> children = component.children();
        if (!children.isEmpty()) {
            plugin.getLogger().info("[" + player.getName() + "] " + indent + "children: " + children.size());
            for (Component child : children) {
                dumpComponentStructure(player, child, indent + "  ");
            }
        }
    }
    
    private String getNamedColorName(net.kyori.adventure.text.format.TextColor color) {
        if (color == null) return "none";
        if (color.equals(net.kyori.adventure.text.format.NamedTextColor.RED)) return "RED";
        if (color.equals(net.kyori.adventure.text.format.NamedTextColor.DARK_RED)) return "DARK_RED";
        if (color.equals(net.kyori.adventure.text.format.NamedTextColor.BLUE)) return "BLUE";
        if (color.equals(net.kyori.adventure.text.format.NamedTextColor.DARK_BLUE)) return "DARK_BLUE";
        if (color.equals(net.kyori.adventure.text.format.NamedTextColor.GREEN)) return "GREEN";
        if (color.equals(net.kyori.adventure.text.format.NamedTextColor.DARK_GREEN)) return "DARK_GREEN";
        if (color.equals(net.kyori.adventure.text.format.NamedTextColor.YELLOW)) return "YELLOW";
        if (color.equals(net.kyori.adventure.text.format.NamedTextColor.GOLD)) return "GOLD";
        if (color.equals(net.kyori.adventure.text.format.NamedTextColor.LIGHT_PURPLE)) return "LIGHT_PURPLE";
        if (color.equals(net.kyori.adventure.text.format.NamedTextColor.DARK_PURPLE)) return "DARK_PURPLE";
        if (color.equals(net.kyori.adventure.text.format.NamedTextColor.AQUA)) return "AQUA";
        if (color.equals(net.kyori.adventure.text.format.NamedTextColor.DARK_AQUA)) return "DARK_AQUA";
        if (color.equals(net.kyori.adventure.text.format.NamedTextColor.GRAY)) return "GRAY";
        if (color.equals(net.kyori.adventure.text.format.NamedTextColor.DARK_GRAY)) return "DARK_GRAY";
        if (color.equals(net.kyori.adventure.text.format.NamedTextColor.WHITE)) return "WHITE";
        if (color.equals(net.kyori.adventure.text.format.NamedTextColor.BLACK)) return "BLACK";
        return "custom";
    }
}
