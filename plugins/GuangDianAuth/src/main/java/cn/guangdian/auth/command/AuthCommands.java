package cn.guangdian.auth.command;

import cn.guangdian.auth.GuangDianAuth;
import cn.guangdian.auth.data.AuthDataManager;
import cn.guangdian.auth.handler.SessionManager;
import cn.guangdian.rpgcore.api.GameLogger;
import cn.guangdian.rpgcore.message.MiniMessageService;
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

/**
 * 认证命令处理器
 *
 * <p>已优化集成 RPGCore 服务：</p>
 * <ul>
 *   <li>消息发送 - 使用 RPGCore MiniMessageService（降级到 Adventure API）</li>
 *   <li>日志记录 - 使用 RPGCore GameLogger</li>
 * </ul>
 *
 * @author GuangDian
 * @since 1.0.0
 */
public class AuthCommands implements CommandExecutor, TabExecutor {

    private final GuangDianAuth plugin;
    private final MiniMessageService miniMessage;
    private final GameLogger logger;

    public AuthCommands(GuangDianAuth plugin) {
        this.plugin = plugin;
        this.miniMessage = plugin.getMiniMessage();
        this.logger = plugin.getGameLogger();
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
            sendError(sender, "此命令只能由玩家执行");
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
            sendWarning(player, "用法: /login <密码>");
            return true;
        }

        SessionManager sessionManager = plugin.getSessionManager();
        AuthDataManager dataManager = plugin.getDataManager();
        String playerName = player.getName();

        if (sessionManager.isLoggedIn(player.getUniqueId())) {
            sendWarning(player, "你已经登录了");
            return true;
        }

        if (sessionManager.hasExceededMaxAttempts(player.getUniqueId())) {
            sendError(player, "登录尝试次数过多，请稍后再试");
            return true;
        }

        String password = String.join(" ", args);

        dataManager.isRegisteredAsync(playerName).thenAccept(registered -> {
            if (!registered) {
                plugin.getScheduler().runSyncLater(() -> {
                    sendError(player, "你还没有注册，请使用 /register <密码> <确认密码>");
                }, 0L);
                return;
            }

            dataManager.checkPasswordAsync(playerName, password).thenAccept(valid -> {
                plugin.getScheduler().runSyncLater(() -> {
                    if (valid) {
                        sessionManager.setLoggedIn(player.getUniqueId(), true);
                        dataManager.updateLastLogin(playerName, player.getAddress().getAddress().getHostAddress());
                        
                        sendSuccess(player, "✓ 登录成功！欢迎来到阿斯特瑞亚");
                        plugin.getPacketHandler().notifyLoggedIn(player);
                        
                        if (logger != null) {
                            logger.info("玩家 " + playerName + " 登录成功");
                        }
                    } else {
                        sessionManager.addLoginAttempt(player.getUniqueId());
                        sendError(player, "✗ 密码错误");
                        
                        if (plugin.getAuthConfig().isKickOnWrongPassword()) {
                            plugin.kickPlayer(player, "密码错误");
                        }
                    }
                }, 0L);
            });
        });

        return true;
    }

    private boolean handleRegister(Player player, String[] args) {
        if (args.length < 2) {
            sendWarning(player, "用法: /register <密码> <确认密码>");
            return true;
        }

        AuthDataManager dataManager = plugin.getDataManager();
        SessionManager sessionManager = plugin.getSessionManager();
        String playerName = player.getName();

        String password = args[0];
        String confirm = args[1];

        if (!password.equals(confirm)) {
            sendError(player, "✗ 两次输入的密码不一致");
            return true;
        }

        int minLen = plugin.getAuthConfig().getMinPasswordLength();
        int maxLen = plugin.getAuthConfig().getMaxPasswordLength();

        if (password.length() < minLen) {
            sendError(player, "✗ 密码长度至少需要 " + minLen + " 个字符");
            return true;
        }

        if (password.length() > maxLen) {
            sendError(player, "✗ 密码长度不能超过 " + maxLen + " 个字符");
            return true;
        }

        dataManager.isRegisteredAsync(playerName).thenAccept(registered -> {
            if (registered) {
                plugin.getScheduler().runSyncLater(() -> {
                    sendWarning(player, "你已经注册过了，请使用 /login <密码> 登录");
                }, 0L);
                return;
            }

            String ip = player.getAddress().getAddress().getHostAddress();
            dataManager.registerAsync(playerName, player.getUniqueId(), password, ip).thenRun(() -> {
                plugin.getScheduler().runSyncLater(() -> {
                    sendSuccess(player, "✓ 注册成功！");
                    
                    if (logger != null) {
                        logger.info("玩家 " + playerName + " 注册成功");
                    }

                    if (plugin.getAuthConfig().isForceLoginAfterRegister()) {
                        sessionManager.setLoggedIn(player.getUniqueId(), true);
                        sendSuccess(player, "✓ 已自动登录");
                        plugin.getPacketHandler().notifyLoggedIn(player);
                    }
                }, 0L);
            });
        });

        return true;
    }

    private boolean handleChangePassword(Player player, String[] args) {
        if (args.length < 2) {
            sendWarning(player, "用法: /changepassword <旧密码> <新密码>");
            return true;
        }

        AuthDataManager dataManager = plugin.getDataManager();
        String playerName = player.getName();

        if (!dataManager.isRegistered(playerName)) {
            sendError(player, "你还没有注册");
            return true;
        }

        String oldPassword = args[0];
        String newPassword = args[1];

        if (!dataManager.checkPassword(playerName, oldPassword)) {
            sendError(player, "✗ 旧密码错误");
            return true;
        }

        int minLen = plugin.getAuthConfig().getMinPasswordLength();
        if (newPassword.length() < minLen) {
            sendError(player, "✗ 新密码长度至少需要 " + minLen + " 个字符");
            return true;
        }

        dataManager.changePassword(playerName, newPassword);
        sendSuccess(player, "✓ 密码修改成功");
        
        if (logger != null) {
            logger.info("玩家 " + playerName + " 修改了密码");
        }

        return true;
    }

    private boolean handleUnregister(Player player, String[] args) {
        if (args.length < 1) {
            sendWarning(player, "用法: /unregister <密码>");
            return true;
        }

        AuthDataManager dataManager = plugin.getDataManager();
        SessionManager sessionManager = plugin.getSessionManager();
        String playerName = player.getName();

        if (!dataManager.isRegistered(playerName)) {
            sendError(player, "你还没有注册");
            return true;
        }

        String password = args[0];

        if (!dataManager.checkPassword(playerName, password)) {
            sendError(player, "✗ 密码错误");
            return true;
        }

        dataManager.unregister(playerName);
        sessionManager.removeSession(player.getUniqueId());
        
        sendSuccess(player, "✓ 账号已注销");
        
        if (logger != null) {
            logger.info("玩家 " + playerName + " 注销了账号");
        }

        return true;
    }

    private boolean handleAdmin(CommandSender sender, String[] args) {
        if (!sender.hasPermission("guangdian.auth.admin")) {
            sendError(sender, "没有权限");
            return true;
        }

        if (args.length < 1) {
            sendWarning(sender, "用法: /authadmin <reload|info|unregister|setpassword>");
            return true;
        }

        String subCmd = args[0].toLowerCase();

        return switch (subCmd) {
            case "reload" -> {
                plugin.getAuthConfig().load();
                sendSuccess(sender, "✓ 配置已重载");
                yield true;
            }
            case "info" -> {
                if (args.length < 2) {
                    sendWarning(sender, "用法: /authadmin info <玩家名>");
                    yield true;
                }
                var dataOpt = plugin.getDataManager().getByName(args[1]);
                if (dataOpt.isEmpty()) {
                    sendError(sender, "玩家未注册");
                } else {
                    var data = dataOpt.get();
                    sendSuccess(sender, "玩家: " + data.getPlayerName());
                    sendInfo(sender, "注册时间: " + new java.util.Date(data.getRegisterDate()));
                    sendInfo(sender, "最后登录: " + new java.util.Date(data.getLastLogin()));
                    sendInfo(sender, "注册IP: " + data.getRegisterIp());
                    sendInfo(sender, "最后IP: " + data.getLastIp());
                }
                yield true;
            }
            case "unregister" -> {
                if (args.length < 2) {
                    sendWarning(sender, "用法: /authadmin unregister <玩家名>");
                    yield true;
                }
                plugin.getDataManager().unregister(args[1]);
                sendSuccess(sender, "✓ 已注销玩家 " + args[1]);
                yield true;
            }
            case "setpassword" -> {
                if (args.length < 3) {
                    sendWarning(sender, "用法: /authadmin setpassword <玩家名> <新密码>");
                    yield true;
                }
                plugin.getDataManager().changePassword(args[1], args[2]);
                sendSuccess(sender, "✓ 已修改玩家 " + args[1] + " 的密码");
                yield true;
            }
            default -> {
                sendError(sender, "未知子命令");
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
    
    // ==================== 消息发送辅助方法 ====================
    
    private void sendSuccess(CommandSender sender, String message) {
        if (miniMessage != null) {
            sender.sendMessage(miniMessage.green(message));
        } else {
            sender.sendMessage(Component.text(message).color(NamedTextColor.GREEN));
        }
    }
    
    private void sendError(CommandSender sender, String message) {
        if (miniMessage != null) {
            sender.sendMessage(miniMessage.red(message));
        } else {
            sender.sendMessage(Component.text(message).color(NamedTextColor.RED));
        }
    }
    
    private void sendWarning(CommandSender sender, String message) {
        if (miniMessage != null) {
            sender.sendMessage(miniMessage.yellow(message));
        } else {
            sender.sendMessage(Component.text(message).color(NamedTextColor.YELLOW));
        }
    }
    
    private void sendInfo(CommandSender sender, String message) {
        if (miniMessage != null) {
            sender.sendMessage(miniMessage.aqua(message));
        } else {
            sender.sendMessage(Component.text(message).color(NamedTextColor.AQUA));
        }
    }
}
