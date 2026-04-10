package cn.guangdian.mcp.command;

import cn.guangdian.mcp.GuangDianMCP;
import cn.guangdian.mcp.config.MCPConfig;
import org.bukkit.ChatColor;
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
                sender.sendMessage(ChatColor.RED + "未知命令: " + subCommand);
                sendHelp(sender);
                return true;
        }
    }
    
    private boolean handleReload(CommandSender sender) {
        plugin.reloadConfiguration();
        sender.sendMessage(ChatColor.GREEN + "GuangDianMCP 配置已重新加载");
        return true;
    }
    
    private boolean handleToken(CommandSender sender, String[] args) {
        if (args.length < 2) {
            sender.sendMessage(ChatColor.RED + "用法: /gmcp token <list|generate|add|remove> [token]");
            return true;
        }
        
        MCPConfig config = plugin.getMCPConfig();
        String action = args[1].toLowerCase();
        
        switch (action) {
            case "list":
                sender.sendMessage(ChatColor.YELLOW + "当前Token列表:");
                for (int i = 0; i < config.getTokens().size(); i++) {
                    String token = config.getTokens().get(i);
                    String masked = token.substring(0, Math.min(8, token.length())) + "...";
                    sender.sendMessage(ChatColor.GRAY + "  " + (i + 1) + ". " + masked);
                }
                break;
                
            case "generate":
                String newToken = generateToken();
                config.addToken(newToken);
                sender.sendMessage(ChatColor.GREEN + "已生成新Token:");
                sender.sendMessage(ChatColor.YELLOW + "  " + newToken);
                sender.sendMessage(ChatColor.RED + "请妥善保存此Token，它不会再次显示！");
                break;
                
            case "add":
                if (args.length < 3) {
                    sender.sendMessage(ChatColor.RED + "用法: /gmcp token add <token>");
                    return true;
                }
                config.addToken(args[2]);
                sender.sendMessage(ChatColor.GREEN + "已添加Token");
                break;
                
            case "remove":
                if (args.length < 3) {
                    sender.sendMessage(ChatColor.RED + "用法: /gmcp token remove <token>");
                    return true;
                }
                config.removeToken(args[2]);
                sender.sendMessage(ChatColor.GREEN + "已移除Token");
                break;
                
            default:
                sender.sendMessage(ChatColor.RED + "未知操作: " + action);
        }
        
        return true;
    }
    
    private boolean handleStatus(CommandSender sender) {
        sender.sendMessage(ChatColor.YELLOW + "=== GuangDianMCP 状态 ===");
        sender.sendMessage(ChatColor.GRAY + "服务器状态: " + 
            (plugin.getMCPServer().isRunning() ? ChatColor.GREEN + "运行中" : ChatColor.RED + "已停止"));
        
        if (plugin.getMCPServer().isRunning()) {
            MCPConfig config = plugin.getMCPConfig();
            sender.sendMessage(ChatColor.GRAY + "监听地址: " + ChatColor.WHITE + 
                config.getHost() + ":" + config.getPort());
            sender.sendMessage(ChatColor.GRAY + "已注册工具: " + ChatColor.WHITE + 
                plugin.getToolRegistry().getTools().size());
        }
        
        sender.sendMessage(ChatColor.GRAY + "Token数量: " + ChatColor.WHITE + 
            plugin.getMCPConfig().getTokens().size());
        sender.sendMessage(ChatColor.GRAY + "IP白名单: " + ChatColor.WHITE + 
            (plugin.getMCPConfig().isIpWhitelistEnabled() ? "已启用" : "已禁用"));
        
        return true;
    }
    
    private boolean handleStart(CommandSender sender) {
        if (plugin.getMCPServer().isRunning()) {
            sender.sendMessage(ChatColor.YELLOW + "MCP服务器已在运行中");
            return true;
        }
        
        plugin.startMCPServer();
        sender.sendMessage(ChatColor.GREEN + "正在启动MCP服务器...");
        return true;
    }
    
    private boolean handleStop(CommandSender sender) {
        if (!plugin.getMCPServer().isRunning()) {
            sender.sendMessage(ChatColor.YELLOW + "MCP服务器未运行");
            return true;
        }
        
        plugin.stopMCPServer();
        sender.sendMessage(ChatColor.GREEN + "MCP服务器已停止");
        return true;
    }
    
    private boolean handleRestart(CommandSender sender) {
        plugin.stopMCPServer();
        plugin.startMCPServer();
        sender.sendMessage(ChatColor.GREEN + "MCP服务器已重启");
        return true;
    }
    
    private void sendHelp(CommandSender sender) {
        sender.sendMessage(ChatColor.YELLOW + "=== GuangDianMCP 帮助 ===");
        sender.sendMessage(ChatColor.GRAY + "/gmcp status " + ChatColor.WHITE + "- 查看MCP服务器状态");
        sender.sendMessage(ChatColor.GRAY + "/gmcp start " + ChatColor.WHITE + "- 启动MCP服务器");
        sender.sendMessage(ChatColor.GRAY + "/gmcp stop " + ChatColor.WHITE + "- 停止MCP服务器");
        sender.sendMessage(ChatColor.GRAY + "/gmcp restart " + ChatColor.WHITE + "- 重启MCP服务器");
        sender.sendMessage(ChatColor.GRAY + "/gmcp reload " + ChatColor.WHITE + "- 重载配置");
        sender.sendMessage(ChatColor.GRAY + "/gmcp token list " + ChatColor.WHITE + "- 列出所有Token");
        sender.sendMessage(ChatColor.GRAY + "/gmcp token generate " + ChatColor.WHITE + "- 生成新Token");
        sender.sendMessage(ChatColor.GRAY + "/gmcp token add <token> " + ChatColor.WHITE + "- 添加Token");
        sender.sendMessage(ChatColor.GRAY + "/gmcp token remove <token> " + ChatColor.WHITE + "- 移除Token");
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
