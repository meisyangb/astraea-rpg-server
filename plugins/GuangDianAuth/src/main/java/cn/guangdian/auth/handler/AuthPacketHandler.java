package cn.guangdian.auth.handler;

import cn.guangdian.auth.GuangDianAuth;
import cn.guangdian.auth.data.AuthDataManager;
import com.google.common.io.ByteArrayDataInput;
import com.google.common.io.ByteArrayDataOutput;
import com.google.common.io.ByteStreams;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.entity.Player;
import org.bukkit.plugin.messaging.PluginMessageListener;
import org.jetbrains.annotations.NotNull;

public class AuthPacketHandler implements PluginMessageListener {

    private static final String LOGIN_CHANNEL = "guangdian:auth";
    private static final String RESPONSE_CHANNEL = "guangdian:auth_response";
    private static final String STATE_CHANNEL = "guangdian:auth_state";

    private final GuangDianAuth plugin;

    public AuthPacketHandler(GuangDianAuth plugin) {
        this.plugin = plugin;
    }

    public void register() {
        plugin.getServer().getMessenger().registerIncomingPluginChannel(
            plugin, LOGIN_CHANNEL, this
        );
        plugin.getServer().getMessenger().registerOutgoingPluginChannel(
            plugin, RESPONSE_CHANNEL
        );
        plugin.getServer().getMessenger().registerOutgoingPluginChannel(
            plugin, STATE_CHANNEL
        );
    }

    public void unregister() {
        plugin.getServer().getMessenger().unregisterIncomingPluginChannel(
            plugin, LOGIN_CHANNEL
        );
        plugin.getServer().getMessenger().unregisterOutgoingPluginChannel(
            plugin, RESPONSE_CHANNEL
        );
        plugin.getServer().getMessenger().unregisterOutgoingPluginChannel(
            plugin, STATE_CHANNEL
        );
    }

    @Override
    public void onPluginMessageReceived(@NotNull String channel, @NotNull Player player, byte[] message) {
        if (!LOGIN_CHANNEL.equals(channel)) return;

        ByteArrayDataInput in = ByteStreams.newDataInput(message);
        String password = in.readUTF();

        handleLoginAttempt(player, password);
    }

    private void handleLoginAttempt(Player player, String password) {
        AuthDataManager dataManager = plugin.getDataManager();
        SessionManager sessionManager = plugin.getSessionManager();
        String playerName = player.getName();

        if (sessionManager.isLoggedIn(player.getUniqueId())) {
            sendResponse(player, true, "已登录");
            return;
        }

        if (!dataManager.isRegistered(playerName)) {
            sendResponse(player, false, "请先注册 /register <密码> <确认密码>");
            return;
        }

        if (sessionManager.hasExceededMaxAttempts(player.getUniqueId())) {
            sendResponse(player, false, "登录尝试次数过多，请稍后再试");
            plugin.kickPlayer(player, "登录尝试次数过多");
            return;
        }

        boolean success = dataManager.checkPassword(playerName, password);

        if (success) {
            sessionManager.setLoggedIn(player.getUniqueId(), true);
            dataManager.updateLastLogin(playerName, player.getAddress().getAddress().getHostAddress());
            sendResponse(player, true, "登录成功");
            
            player.sendMessage(Component.text("✓ 登录成功！欢迎来到阿斯特瑞亚").color(NamedTextColor.GREEN));
            plugin.getLogger().info("玩家 " + playerName + " 通过自定义界面登录成功");
        } else {
            sessionManager.addLoginAttempt(player.getUniqueId());
            sendResponse(player, false, "密码错误");
            plugin.getLogger().warning("玩家 " + playerName + " 登录失败: 密码错误");
        }
    }

    public void sendResponse(Player player, boolean success, String message) {
        ByteArrayDataOutput out = ByteStreams.newDataOutput();
        out.writeBoolean(success);
        out.writeUTF(message);
        player.sendPluginMessage(plugin, RESPONSE_CHANNEL, out.toByteArray());
    }

    public void sendLoginState(Player player, String state) {
        ByteArrayDataOutput out = ByteStreams.newDataOutput();
        out.writeUTF(state);
        player.sendPluginMessage(plugin, STATE_CHANNEL, out.toByteArray());
    }

    public void notifyNeedLogin(Player player) {
        sendLoginState(player, "need_login");
    }

    public void notifyLoggedIn(Player player) {
        sendLoginState(player, "logged_in");
    }
}
