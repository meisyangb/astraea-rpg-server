package cn.guangdian.auth.handler;

import cn.guangdian.auth.GuangDianAuth;
import cn.guangdian.auth.data.AuthDataManager;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.entity.Player;
import org.bukkit.plugin.messaging.PluginMessageListener;
import org.jetbrains.annotations.NotNull;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;

public class AuthPacketHandler implements PluginMessageListener {

    private static final String LOGIN_CHANNEL = "guangdian:auth";
    private static final String REGISTER_CHANNEL = "guangdian:auth_register";
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
        plugin.getServer().getMessenger().registerIncomingPluginChannel(
            plugin, REGISTER_CHANNEL, this
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
        plugin.getServer().getMessenger().unregisterIncomingPluginChannel(
            plugin, REGISTER_CHANNEL
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
        if (LOGIN_CHANNEL.equals(channel)) {
            String password = readString(message, 0);
            handleLoginAttempt(player, password);
        } else if (REGISTER_CHANNEL.equals(channel)) {
            int[] offset = {0};
            String password = readString(message, offset);
            String confirmPassword = readString(message, offset);
            handleRegisterAttempt(player, password, confirmPassword);
        }
    }

    private String readString(byte[] data, int offset) {
        int[] off = {offset};
        return readString(data, off);
    }

    private String readString(byte[] data, int[] offset) {
        int length = readVarInt(data, offset);
        String str = new String(data, offset[0], length, StandardCharsets.UTF_8);
        offset[0] += length;
        return str;
    }

    private int readVarInt(byte[] data, int[] offset) {
        int value = 0;
        int length = 0;
        byte current;
        do {
            current = data[offset[0]];
            value |= (current & 0x7F) << (length * 7);
            length++;
            offset[0]++;
            if (length > 5) {
                throw new RuntimeException("VarInt too big");
            }
        } while ((current & 0x80) == 0x80);
        return value;
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
            sendResponse(player, false, "账号未注册，请先注册");
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

    private void handleRegisterAttempt(Player player, String password, String confirmPassword) {
        AuthDataManager dataManager = plugin.getDataManager();
        SessionManager sessionManager = plugin.getSessionManager();
        String playerName = player.getName();

        if (dataManager.isRegistered(playerName)) {
            sendResponse(player, false, "账号已存在，请直接登录");
            return;
        }

        if (password.length() < 4) {
            sendResponse(player, false, "密码长度至少4位");
            return;
        }

        if (password.length() > 32) {
            sendResponse(player, false, "密码长度不能超过32位");
            return;
        }

        if (!password.equals(confirmPassword)) {
            sendResponse(player, false, "两次输入的密码不一致");
            return;
        }

        String ip = player.getAddress().getAddress().getHostAddress();
        dataManager.register(playerName, player.getUniqueId(), password, ip);
        
        sessionManager.setLoggedIn(player.getUniqueId(), true);
        sendResponse(player, true, "注册成功");
        
        player.sendMessage(Component.text("✓ 注册成功！欢迎来到阿斯特瑞亚").color(NamedTextColor.GREEN));
        plugin.getLogger().info("玩家 " + playerName + " 通过自定义界面注册成功");
    }

    public void sendResponse(Player player, boolean success, String message) {
        try {
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            DataOutputStream out = new DataOutputStream(baos);
            out.writeBoolean(success);
            writeMinecraftString(out, message);
            player.sendPluginMessage(plugin, RESPONSE_CHANNEL, baos.toByteArray());
        } catch (IOException e) {
            plugin.getLogger().severe("发送响应失败: " + e.getMessage());
        }
    }

    public void sendLoginState(Player player, String state) {
        try {
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            DataOutputStream out = new DataOutputStream(baos);
            writeMinecraftString(out, state);
            byte[] data = baos.toByteArray();
            player.sendPluginMessage(plugin, STATE_CHANNEL, data);
            plugin.getLogger().info("发送登录状态给 " + player.getName() + ": " + state + " (数据长度: " + data.length + " 字节)");
        } catch (IOException e) {
            plugin.getLogger().severe("发送状态失败: " + e.getMessage());
        }
    }

    private void writeMinecraftString(DataOutputStream out, String str) throws IOException {
        byte[] bytes = str.getBytes(StandardCharsets.UTF_8);
        writeVarInt(out, bytes.length);
        out.write(bytes);
    }

    private void writeVarInt(DataOutputStream out, int value) throws IOException {
        while ((value & ~0x7F) != 0) {
            out.writeByte((value & 0x7F) | 0x80);
            value >>>= 7;
        }
        out.writeByte(value);
    }

    public void notifyNeedRegister(Player player) {
        sendLoginState(player, "need_register");
    }

    public void notifyNeedLogin(Player player) {
        sendLoginState(player, "need_login");
    }

    public void notifyLoggedIn(Player player) {
        sendLoginState(player, "logged_in");
    }
}
