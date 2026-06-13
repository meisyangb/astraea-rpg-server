package cn.guangdian.mcp.command;

import cn.guangdian.mcp.GuangDianMCP;
import cn.guangdian.mcp.config.MCPConfig;
import cn.guangdian.rpgcore.message.MiniMessageService;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;

import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;

public class MCPCommand implements CommandExecutor, TabCompleter {
    
    private final GuangDianMCP plugin;
    private final SecureRandom random = new SecureRandom();
    private final MiniMessage miniMessage = MiniMessage.miniMessage();
    
    public MCPCommand(GuangDianMCP plugin) {
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
            case "reload":
                return handleReload(sender);
            case "token":
                return handleToken(sender, args);
            case "status":
                return handleStatus(sender);
            case "start":
                return handleStart(sender);
            case "stop":
                return handleStop(sender);
            case "restart":
                return handleRestart(sender);
            case "help":
                sendHelp(sender);
                return true;
            default:
                sender.sendMessage(Component.text("未知命令: " + subCommand).color(NamedTextColor.RED));
                sendHelp(sender);
                return true;
        }
    }
    
    private boolean handleReload(CommandSender sender) {
        plugin.reloadConfiguration();
        sender.sendMessage(Component.text("GuangDianMCP 配置已重新加载").color(NamedTextColor.GREEN));
        return true;
    }
    
    private boolean handleToken(CommandSender sender, String[] args) {
        if (args.length < 2) {
            sender.sendMessage(Component.text("用法: /gmcp token <list|generate|add|remove> [token]").color(NamedTextColor.RED));
            return true;
        }
        
        MCPConfig config = plugin.getMCPConfig();
        String action = args[1].toLowerCase();
        
        switch (action) {
            case "list":
                sender.sendMessage(Component.text("当前Token列表:").color(NamedTextColor.YELLOW));
                for (int i = 0; i < config.getTokens().size(); i++) {
                    String token = config.getTokens().get(i);
                    String masked = token.substring(0, Math.min(8, token.length())) + "...";
                    sender.sendMessage(Component.text("  " + (i + 1) + ". " + masked).color(NamedTextColor.GRAY));
                }
                break;
                
            case "generate":
                String newToken = generateToken();
                config.addToken(newToken);
                sender.sendMessage(Component.text("已生成新Token:").color(NamedTextColor.GREEN));
                sender.sendMessage(Component.text("  " + newToken).color(NamedTextColor.YELLOW));
                sender.sendMessage(Component.text("请妥善保存此Token，它不会再次显示！").color(NamedTextColor.RED));
                break;
                
            case "add":
                if (args.length < 3) {
                    sender.sendMessage(Component.text("用法: /gmcp token add <token>").color(NamedTextColor.RED));
                    return true;
                }
                config.addToken(args[2]);
                sender.sendMessage(Component.text("已添加Token").color(NamedTextColor.GREEN));
                break;
                
            case "remove":
                if (args.length < 3) {
                    sender.sendMessage(Component.text("用法: /gmcp token remove <token>").color(NamedTextColor.RED));
                    return true;
                }
                config.removeToken(args[2]);
                sender.sendMessage(Component.text("已移除Token").color(NamedTextColor.GREEN));
                break;
                
            default:
                sender.sendMessage(Component.text("未知操作: " + action).color(NamedTextColor.RED));
        }
        
        return true;
    }
    
    private boolean handleStatus(CommandSender sender) {
        sender.sendMessage(Component.text("=== GuangDianMCP 状态 ===").color(NamedTextColor.YELLOW));
        Component status = plugin.getMCPServer().isRunning() 
            ? Component.text("运行中").color(NamedTextColor.GREEN)
            : Component.text("已停止").color(NamedTextColor.RED);
        sender.sendMessage(Component.text("服务器状态: ").color(NamedTextColor.GRAY).append(status));
        
        if (plugin.getMCPServer().isRunning()) {
            MCPConfig config = plugin.getMCPConfig();
            sender.sendMessage(Component.text("监听地址: ").color(NamedTextColor.GRAY)
                .append(Component.text(config.getHost() + ":" + config.getPort()).color(NamedTextColor.WHITE)));
            sender.sendMessage(Component.text("已注册工具: ").color(NamedTextColor.GRAY)
                .append(Component.text(String.valueOf(plugin.getToolRegistry().getTools().size())).color(NamedTextColor.WHITE)));
        }
        
        sender.sendMessage(Component.text("Token数量: ").color(NamedTextColor.GRAY)
            .append(Component.text(String.valueOf(plugin.getMCPConfig().getTokens().size())).color(NamedTextColor.WHITE)));
        sender.sendMessage(Component.text("IP白名单: ").color(NamedTextColor.GRAY)
            .append(Component.text(plugin.getMCPConfig().isIpWhitelistEnabled() ? "已启用" : "已禁用").color(NamedTextColor.WHITE)));
        
        return true;
    }
    
    private boolean handleStart(CommandSender sender) {
        if (plugin.getMCPServer().isRunning()) {
            sender.sendMessage(Component.text("MCP服务器已在运行中").color(NamedTextColor.YELLOW));
            return true;
        }
        
        plugin.startMCPServer();
        sender.sendMessage(Component.text("正在启动MCP服务器...").color(NamedTextColor.GREEN));
        return true;
    }
    
    private boolean handleStop(CommandSender sender) {
        if (!plugin.getMCPServer().isRunning()) {
            sender.sendMessage(Component.text("MCP服务器未运行").color(NamedTextColor.YELLOW));
            return true;
        }
        
        plugin.stopMCPServer();
        sender.sendMessage(Component.text("MCP服务器已停止").color(NamedTextColor.GREEN));
        return true;
    }
    
    private boolean handleRestart(CommandSender sender) {
        plugin.stopMCPServer();
        plugin.startMCPServer();
        sender.sendMessage(Component.text("MCP服务器已重启").color(NamedTextColor.GREEN));
        return true;
    }
    
    private void sendHelp(CommandSender sender) {
        sender.sendMessage(Component.text("=== GuangDianMCP 帮助 ===").color(NamedTextColor.YELLOW));
        sender.sendMessage(Component.text("/gmcp status ").color(NamedTextColor.GRAY)
            .append(Component.text("- 查看MCP服务器状态").color(NamedTextColor.WHITE)));
        sender.sendMessage(Component.text("/gmcp start ").color(NamedTextColor.GRAY)
            .append(Component.text("- 启动MCP服务器").color(NamedTextColor.WHITE)));
        sender.sendMessage(Component.text("/gmcp stop ").color(NamedTextColor.GRAY)
            .append(Component.text("- 停止MCP服务器").color(NamedTextColor.WHITE)));
        sender.sendMessage(Component.text("/gmcp restart ").color(NamedTextColor.GRAY)
            .append(Component.text("- 重启MCP服务器").color(NamedTextColor.WHITE)));
        sender.sendMessage(Component.text("/gmcp reload ").color(NamedTextColor.GRAY)
            .append(Component.text("- 重载配置").color(NamedTextColor.WHITE)));
        sender.sendMessage(Component.text("/gmcp token list ").color(NamedTextColor.GRAY)
            .append(Component.text("- 列出所有Token").color(NamedTextColor.WHITE)));
        sender.sendMessage(Component.text("/gmcp token generate ").color(NamedTextColor.GRAY)
            .append(Component.text("- 生成新Token").color(NamedTextColor.WHITE)));
        sender.sendMessage(Component.text("/gmcp token add <token> ").color(NamedTextColor.GRAY)
            .append(Component.text("- 添加Token").color(NamedTextColor.WHITE)));
        sender.sendMessage(Component.text("/gmcp token remove <token> ").color(NamedTextColor.GRAY)
            .append(Component.text("- 移除Token").color(NamedTextColor.WHITE)));
    }
    
    private String generateToken() {
        byte[] bytes = new byte[32];
        random.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }
    
    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        List<String> completions = new ArrayList<>();
        
        if (args.length == 1) {
            completions.add("status");
            completions.add("start");
            completions.add("stop");
            completions.add("restart");
            completions.add("reload");
            completions.add("token");
            completions.add("help");
        } else if (args.length == 2 && args[0].equalsIgnoreCase("token")) {
            completions.add("list");
            completions.add("generate");
            completions.add("add");
            completions.add("remove");
        }
        
        String prefix = args[args.length - 1].toLowerCase();
        completions.removeIf(s -> !s.toLowerCase().startsWith(prefix));
        
        return completions;
    }
}
