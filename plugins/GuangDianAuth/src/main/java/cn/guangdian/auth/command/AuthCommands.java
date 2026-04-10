package cn.guangdian.auth.command;

import cn.guangdian.auth.GuangDianAuth;
import cn.guangdian.auth.data.AuthDataManager;
import cn.guangdian.auth.handler.SessionManager;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabExecutor;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class AuthCommands implements CommandExecutor, TabExecutor {

    private final GuangDianAuth plugin;

    public AuthCommands(GuangDianAuth plugin) {
        this.plugin = plugin;
    }

    public void registerAll() {
        plugin.getCommand("login").setExecutor(this);
        plugin.getCommand("register").setExecutor(this);
        plugin.getCommand("changepassword").setExecutor(this);
        plugin.getCommand("unregister").setExecutor(this);
        plugin.getCommand("authadmin").setExecutor(this);
        
        plugin.getCommand("login").setTabCompleter(this);
        plugin.getCommand("register").setTabCompleter(this);
        plugin.getCommand("changepassword").setTabCompleter(this);
        plugin.getCommand("unregister").setTabCompleter(this);
        plugin.getCommand("authadmin").setTabCompleter(this);
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(Component.text("此命令只能由玩家执行").color(NamedTextColor.RED));
            return true;
        }

        String cmdName = command.getName().toLowerCase();

        return switch (cmdName) {
            case "login" -> handleLogin(player, args);
            case "register" -> handleRegister(player, args);
            case "changepassword" -> handleChangePassword(player, args);
            case "unregister" -> handleUnregister(player, args);
            case "authadmin" -> handleAdmin(sender, args);
            default -> false;
        };
    }

    private boolean handleLogin(Player player, String[] args) {
        if (args.length < 1) {
            player.sendMessage(Component.text("用法: /login <密码>").color(NamedTextColor.YELLOW));
            return true;
        }

        SessionManager sessionManager = plugin.getSessionManager();
        AuthDataManager dataManager = plugin.getDataManager();
        String playerName = player.getName();

        if (sessionManager.isLoggedIn(player.getUniqueId())) {
            player.sendMessage(Component.text("你已经登录了").color(NamedTextColor.YELLOW));
            return true;
        }

        if (!dataManager.isRegistered(playerName)) {
            player.sendMessage(Component.text("你还没有注册，请使用 /register <密码> <确认密码>").color(NamedTextColor.RED));
            return true;
        }

        if (sessionManager.hasExceededMaxAttempts(player.getUniqueId())) {
            player.sendMessage(Component.text("登录尝试次数过多，请稍后再试").color(NamedTextColor.RED));
            return true;
        }

        String password = String.join(" ", args);

        if (dataManager.checkPassword(playerName, password)) {
            sessionManager.setLoggedIn(player.getUniqueId(), true);
            dataManager.updateLastLogin(playerName, player.getAddress().getAddress().getHostAddress());
            
            player.sendMessage(Component.text("✓ 登录成功！欢迎来到阿斯特瑞亚").color(NamedTextColor.GREEN));
            plugin.getPacketHandler().notifyLoggedIn(player);
            plugin.getLogger().info("玩家 " + playerName + " 登录成功");
        } else {
            sessionManager.addLoginAttempt(player.getUniqueId());
            player.sendMessage(Component.text("✗ 密码错误").color(NamedTextColor.RED));
            
            if (plugin.getAuthConfig().isKickOnWrongPassword()) {
                plugin.kickPlayer(player, "密码错误");
            }
        }

        return true;
    }

    private boolean handleRegister(Player player, String[] args) {
        if (args.length < 2) {
            player.sendMessage(Component.text("用法: /register <密码> <确认密码>").color(NamedTextColor.YELLOW));
            return true;
        }

        AuthDataManager dataManager = plugin.getDataManager();
        SessionManager sessionManager = plugin.getSessionManager();
        String playerName = player.getName();

        if (dataManager.isRegistered(playerName)) {
            player.sendMessage(Component.text("你已经注册过了，请使用 /login <密码> 登录").color(NamedTextColor.YELLOW));
            return true;
        }

        String password = args[0];
        String confirm = args[1];

        if (!password.equals(confirm)) {
            player.sendMessage(Component.text("✗ 两次输入的密码不一致").color(NamedTextColor.RED));
            return true;
        }

        int minLen = plugin.getAuthConfig().getMinPasswordLength();
        int maxLen = plugin.getAuthConfig().getMaxPasswordLength();

        if (password.length() < minLen) {
            player.sendMessage(Component.text("✗ 密码长度至少需要 " + minLen + " 个字符").color(NamedTextColor.RED));
            return true;
        }

        if (password.length() > maxLen) {
            player.sendMessage(Component.text("✗ 密码长度不能超过 " + maxLen + " 个字符").color(NamedTextColor.RED));
            return true;
        }

        String ip = player.getAddress().getAddress().getHostAddress();
        dataManager.register(playerName, player.getUniqueId(), password, ip);

        player.sendMessage(Component.text("✓ 注册成功！").color(NamedTextColor.GREEN));
        plugin.getLogger().info("玩家 " + playerName + " 注册成功");

        if (plugin.getAuthConfig().isForceLoginAfterRegister()) {
            sessionManager.setLoggedIn(player.getUniqueId(), true);
            player.sendMessage(Component.text("✓ 已自动登录").color(NamedTextColor.GREEN));
            plugin.getPacketHandler().notifyLoggedIn(player);
        }

        return true;
    }

    private boolean handleChangePassword(Player player, String[] args) {
        if (args.length < 2) {
            player.sendMessage(Component.text("用法: /changepassword <旧密码> <新密码>").color(NamedTextColor.YELLOW));
            return true;
        }

        AuthDataManager dataManager = plugin.getDataManager();
        String playerName = player.getName();

        if (!dataManager.isRegistered(playerName)) {
            player.sendMessage(Component.text("你还没有注册").color(NamedTextColor.RED));
            return true;
        }

        String oldPassword = args[0];
        String newPassword = args[1];

        if (!dataManager.checkPassword(playerName, oldPassword)) {
            player.sendMessage(Component.text("✗ 旧密码错误").color(NamedTextColor.RED));
            return true;
        }

        int minLen = plugin.getAuthConfig().getMinPasswordLength();
        if (newPassword.length() < minLen) {
            player.sendMessage(Component.text("✗ 新密码长度至少需要 " + minLen + " 个字符").color(NamedTextColor.RED));
            return true;
        }

        dataManager.changePassword(playerName, newPassword);
        player.sendMessage(Component.text("✓ 密码修改成功").color(NamedTextColor.GREEN));
        plugin.getLogger().info("玩家 " + playerName + " 修改了密码");

        return true;
    }

    private boolean handleUnregister(Player player, String[] args) {
        if (args.length < 1) {
            player.sendMessage(Component.text("用法: /unregister <密码>").color(NamedTextColor.YELLOW));
            return true;
        }

        AuthDataManager dataManager = plugin.getDataManager();
        SessionManager sessionManager = plugin.getSessionManager();
        String playerName = player.getName();

        if (!dataManager.isRegistered(playerName)) {
            player.sendMessage(Component.text("你还没有注册").color(NamedTextColor.RED));
            return true;
        }

        String password = args[0];

        if (!dataManager.checkPassword(playerName, password)) {
            player.sendMessage(Component.text("✗ 密码错误").color(NamedTextColor.RED));
            return true;
        }

        dataManager.unregister(playerName);
        sessionManager.removeSession(player.getUniqueId());
        
        player.sendMessage(Component.text("✓ 账号已注销").color(NamedTextColor.GREEN));
        plugin.getLogger().info("玩家 " + playerName + " 注销了账号");

        return true;
    }

    private boolean handleAdmin(CommandSender sender, String[] args) {
        if (!sender.hasPermission("guangdian.auth.admin")) {
            sender.sendMessage(Component.text("没有权限").color(NamedTextColor.RED));
            return true;
        }

        if (args.length < 1) {
            sender.sendMessage(Component.text("用法: /authadmin <reload|info|unregister|setpassword>").color(NamedTextColor.YELLOW));
            return true;
        }

        String subCmd = args[0].toLowerCase();

        return switch (subCmd) {
            case "reload" -> {
                plugin.getAuthConfig().load();
                sender.sendMessage(Component.text("✓ 配置已重载").color(NamedTextColor.GREEN));
                yield true;
            }
            case "info" -> {
                if (args.length < 2) {
                    sender.sendMessage(Component.text("用法: /authadmin info <玩家名>").color(NamedTextColor.YELLOW));
                    yield true;
                }
                var dataOpt = plugin.getDataManager().getByName(args[1]);
                if (dataOpt.isEmpty()) {
                    sender.sendMessage(Component.text("玩家未注册").color(NamedTextColor.RED));
                } else {
                    var data = dataOpt.get();
                    sender.sendMessage(Component.text("玩家: " + data.getPlayerName()).color(NamedTextColor.GREEN));
                    sender.sendMessage(Component.text("注册时间: " + new java.util.Date(data.getRegisterDate())).color(NamedTextColor.YELLOW));
                    sender.sendMessage(Component.text("最后登录: " + new java.util.Date(data.getLastLogin())).color(NamedTextColor.YELLOW));
                    sender.sendMessage(Component.text("注册IP: " + data.getRegisterIp()).color(NamedTextColor.YELLOW));
                    sender.sendMessage(Component.text("最后IP: " + data.getLastIp()).color(NamedTextColor.YELLOW));
                }
                yield true;
            }
            case "unregister" -> {
                if (args.length < 2) {
                    sender.sendMessage(Component.text("用法: /authadmin unregister <玩家名>").color(NamedTextColor.YELLOW));
                    yield true;
                }
                plugin.getDataManager().unregister(args[1]);
                sender.sendMessage(Component.text("✓ 已注销玩家 " + args[1]).color(NamedTextColor.GREEN));
                yield true;
            }
            case "setpassword" -> {
                if (args.length < 3) {
                    sender.sendMessage(Component.text("用法: /authadmin setpassword <玩家名> <新密码>").color(NamedTextColor.YELLOW));
                    yield true;
                }
                plugin.getDataManager().changePassword(args[1], args[2]);
                sender.sendMessage(Component.text("✓ 已修改玩家 " + args[1] + " 的密码").color(NamedTextColor.GREEN));
                yield true;
            }
            default -> {
                sender.sendMessage(Component.text("未知子命令").color(NamedTextColor.RED));
                yield true;
            }
        };
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        List<String> completions = new ArrayList<>();
        String cmdName = command.getName().toLowerCase();

        switch (cmdName) {
            case "authadmin" -> {
                if (args.length == 1) {
                    completions.addAll(Arrays.asList("reload", "info", "unregister", "setpassword"));
                } else if (args.length == 2 && !args[0].equals("reload")) {
                    Bukkit.getOnlinePlayers().forEach(p -> completions.add(p.getName()));
                }
            }
        }

        return completions;
    }
}
