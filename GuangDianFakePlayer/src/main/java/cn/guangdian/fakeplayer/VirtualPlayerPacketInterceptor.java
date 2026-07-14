package cn.guangdian.fakeplayer;

import com.comphenix.protocol.PacketType;
import com.comphenix.protocol.ProtocolLibrary;
import com.comphenix.protocol.ProtocolManager;
import com.comphenix.protocol.events.ListenerPriority;
import com.comphenix.protocol.events.PacketAdapter;
import com.comphenix.protocol.events.PacketContainer;
import com.comphenix.protocol.events.PacketEvent;
import com.comphenix.protocol.wrappers.EnumWrappers;
import com.comphenix.protocol.wrappers.PlayerInfoData;
import com.comphenix.protocol.wrappers.WrappedChatComponent;
import com.comphenix.protocol.wrappers.WrappedGameProfile;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 虚拟玩家数据包拦截器
 * 拦截服务器发送的PlayerInfo数据包，添加虚拟玩家信息
 */
public class VirtualPlayerPacketInterceptor {

    private final GuangDianFakePlayer plugin;
    private final ProtocolManager protocolManager;
    private final VirtualPlayerDataManager dataManager;
    private com.comphenix.protocol.events.PacketListener packetListener;

    public VirtualPlayerPacketInterceptor(GuangDianFakePlayer plugin, VirtualPlayerDataManager dataManager) {
        this.plugin = plugin;
        this.protocolManager = ProtocolLibrary.getProtocolManager();
        this.dataManager = dataManager;
        registerPacketListener();
    }

    /**
     * 注册数据包监听器
     */
    private void registerPacketListener() {
        // 保存 plugin 引用，用于在匿名内部类中访问
        final GuangDianFakePlayer fakePlayerPlugin = this.plugin;
        final FakePlayerConfig config = fakePlayerPlugin.getFakePlayerConfig();
        
        packetListener = new PacketAdapter(
            plugin,
            ListenerPriority.NORMAL,
            PacketType.Play.Server.PLAYER_INFO
        ) {
            @Override
            public void onPacketSending(PacketEvent event) {
                // 拦截服务器发送的PlayerInfo数据包
                PacketContainer packet = event.getPacket();

                try {
                    // 获取动作
                    Object actionsObj = packet.getPlayerInfoActions().read(0);
                    EnumSet<EnumWrappers.PlayerInfoAction> actions;

                    if (actionsObj instanceof EnumSet) {
                        actions = (EnumSet<EnumWrappers.PlayerInfoAction>) actionsObj;
                    } else if (actionsObj instanceof Set) {
                        actions = EnumSet.copyOf((Set<EnumWrappers.PlayerInfoAction>) actionsObj);
                    } else {
                        // 如果无法获取动作，跳过处理
                        return;
                    }

                    // 只处理ADD_PLAYER动作
                    if (actions.contains(EnumWrappers.PlayerInfoAction.ADD_PLAYER)) {
                        // 获取原始玩家数据列表
                        List<PlayerInfoData> originalData = packet.getPlayerInfoDataLists().read(1);

                        // 创建新的玩家数据列表（包含真实玩家 + 虚拟玩家）
                        List<PlayerInfoData> newData = new ArrayList<>(originalData);

                        // 添加虚拟玩家
                        for (VirtualPlayerDataManager.VirtualPlayer virtualPlayer : dataManager.getVirtualPlayers().values()) {
                            WrappedGameProfile gameProfile = new WrappedGameProfile(
                                virtualPlayer.getUuid(),
                                virtualPlayer.getName()
                            );

                            // 设置皮肤（可选）
                            // gameProfile = gameProfile.withProperty("textures", skinValue, skinSignature);

                            // 获取虚拟玩家的显示格式
                            FakePlayerConfig.VirtualPlayerFormat format = config.getFormatForName(virtualPlayer.getName());
                            
                            // 构建显示名称
                            String displayName = config.buildDisplayName(virtualPlayer.getName(), format);
                            
                            // 将 MiniMessage 格式转换为 legacy § 格式
                            displayName = config.miniMessageToLegacy(displayName);
                            
                            // 限制显示名称长度（Minecraft TAB 列表限制）
                            if (displayName.length() > 80) {
                                displayName = displayName.substring(0, 80);
                            }

                            // 随机 Ping 值 (20-80ms)
                            int ping = 20 + new Random().nextInt(60);

                            PlayerInfoData playerInfoData = new PlayerInfoData(
                                gameProfile,
                                ping,
                                EnumWrappers.NativeGameMode.SURVIVAL,
                                WrappedChatComponent.fromText(displayName)
                            );

                            newData.add(playerInfoData);
                        }

                        // 写入新的玩家数据列表
                        packet.getPlayerInfoDataLists().write(1, newData);

                        if (fakePlayerPlugin.getConfig().getBoolean("debug", false)) {
                            fakePlayerPlugin.getLogger().info("拦截PlayerInfo数据包，添加 " + dataManager.getVirtualPlayers().size() + " 个虚拟玩家");
                            fakePlayerPlugin.getLogger().info("原始玩家数: " + originalData.size() + ", 新玩家数: " + newData.size());
                        }
                    }
                } catch (Exception e) {
                    fakePlayerPlugin.getLogger().warning("处理PlayerInfo数据包失败: " + e.getMessage());
                    if (fakePlayerPlugin.getConfig().getBoolean("debug", false)) {
                        e.printStackTrace();
                    }
                }
            }
        };

        protocolManager.addPacketListener(packetListener);
        plugin.getLogger().info("已注册PlayerInfo数据包拦截器");
    }

    /**
     * 停止数据包拦截器，注销监听器
     */
    public void stop() {
        if (packetListener != null) {
            protocolManager.removePacketListener(packetListener);
            packetListener = null;
            plugin.getLogger().info("已注销PlayerInfo数据包拦截器");
        }
    }

    /**
     * 获取虚拟玩家数量
     */
    public int getVirtualPlayerCount() {
        return dataManager.getVirtualPlayerCount();
    }

    /**
     * 获取所有虚拟玩家名
     */
    public List<String> getVirtualPlayerNames() {
        return dataManager.getVirtualPlayerNames();
    }
}