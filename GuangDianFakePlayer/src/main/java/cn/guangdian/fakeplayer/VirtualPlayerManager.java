package cn.guangdian.fakeplayer;

import com.comphenix.protocol.PacketType;
import com.comphenix.protocol.ProtocolLibrary;
import com.comphenix.protocol.ProtocolManager;
import com.comphenix.protocol.events.PacketContainer;
import com.comphenix.protocol.wrappers.EnumWrappers;
import com.comphenix.protocol.wrappers.PlayerInfoData;
import com.comphenix.protocol.wrappers.WrappedChatComponent;
import com.comphenix.protocol.wrappers.WrappedGameProfile;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 虚拟玩家管理器
 * 使用ProtocolLib数据包在TAB列表显示虚拟玩家
 */
public class VirtualPlayerManager {

    private final GuangDianFakePlayer plugin;
    private final ProtocolManager protocolManager;
    private final Map<String, VirtualPlayer> virtualPlayers = new ConcurrentHashMap<>();
    private final List<String> presetNames = new ArrayList<>();
    private int nameIndex = 0;

    public VirtualPlayerManager(GuangDianFakePlayer plugin) {
        this.plugin = plugin;
        this.protocolManager = ProtocolLibrary.getProtocolManager();
        loadPresetNames();
    }

    private void loadPresetNames() {
        List<String> names = plugin.getConfig().getStringList("virtual-players.preset-names");
        if (names != null && !names.isEmpty()) {
            presetNames.addAll(names);
        } else {
            // 默认预设名字
            for (int i = 1; i <= 50; i++) {
                presetNames.add("玩家" + i);
            }
        }
    }

    /**
     * 创建虚拟玩家
     */
    public void createVirtualPlayer(String name) {
        if (virtualPlayers.containsKey(name)) {
            plugin.getLogger().info("虚拟玩家已存在，跳过创建: " + name);
            return; // 已存在
        }

        UUID uuid = UUID.randomUUID();
        VirtualPlayer virtualPlayer = new VirtualPlayer(name, uuid);
        virtualPlayers.put(name, virtualPlayer);

        plugin.getLogger().info("开始创建虚拟玩家: " + name + ", UUID: " + uuid);

        // 发送数据包到所有在线玩家
        sendAddPlayerPacket(virtualPlayer);

        if (plugin.getFakePlayerConfig().isDebug()) {
            plugin.getLogger().info("虚拟玩家创建完成: " + name);
        }
    }

    /**
     * 移除虚拟玩家
     */
    public void removeVirtualPlayer(String name) {
        VirtualPlayer virtualPlayer = virtualPlayers.remove(name);
        if (virtualPlayer == null) {
            return; // 不存在
        }

        // 发送移除数据包到所有在线玩家
        sendRemovePlayerPacket(virtualPlayer);

        if (plugin.getFakePlayerConfig().isDebug()) {
            plugin.getLogger().info("移除虚拟玩家: " + name);
        }
    }

    /**
     * 批量创建虚拟玩家
     */
    public void createVirtualPlayers(int count) {
        plugin.getLogger().info("开始批量创建 " + count + " 个虚拟玩家");
        plugin.getLogger().info("预设名字列表数量: " + presetNames.size());
        plugin.getLogger().info("当前名字索引: " + nameIndex);

        int createdCount = 0;
        for (int i = 0; i < count && nameIndex < presetNames.size(); i++) {
            String name = presetNames.get(nameIndex);
            plugin.getLogger().info("尝试创建虚拟玩家 #" + (i+1) + ": " + name);
            createVirtualPlayer(name);
            nameIndex++;
            createdCount++;
        }

        plugin.getLogger().info("批量创建完成，实际创建: " + createdCount + " 个虚拟玩家");
        plugin.getLogger().info("当前虚拟玩家总数: " + virtualPlayers.size());
    }

    /**
     * 清理所有虚拟玩家
     */
    public void clearAllVirtualPlayers() {
        for (String name : virtualPlayers.keySet()) {
            removeVirtualPlayer(name);
        }
        virtualPlayers.clear();
        nameIndex = 0;
    }

    /**
     * 发送添加虚拟玩家数据包
     */
    private void sendAddPlayerPacket(VirtualPlayer virtualPlayer) {
        try {
            PacketContainer packet = protocolManager.createPacket(PacketType.Play.Server.PLAYER_INFO);

            // 设置动作：ADD_PLAYER
            EnumSet<EnumWrappers.PlayerInfoAction> actions = EnumSet.of(EnumWrappers.PlayerInfoAction.ADD_PLAYER);
            packet.getPlayerInfoActions().write(0, actions);

            // 创建玩家信息数据
            WrappedGameProfile gameProfile = new WrappedGameProfile(virtualPlayer.getUuid(), virtualPlayer.getName());
            PlayerInfoData playerInfoData = new PlayerInfoData(
                gameProfile,
                50, // Ping值
                EnumWrappers.NativeGameMode.SURVIVAL,
                WrappedChatComponent.fromText(virtualPlayer.getName())
            );

            // 写入玩家数据列表
            List<PlayerInfoData> playerInfoDataList = new ArrayList<>();
            playerInfoDataList.add(playerInfoData);
            packet.getPlayerInfoDataLists().write(1, playerInfoDataList);

            // 发送到所有在线玩家
            for (Player player : Bukkit.getOnlinePlayers()) {
                try {
                    protocolManager.sendServerPacket(player, packet);
                    if (plugin.getFakePlayerConfig().isDebug()) {
                        plugin.getLogger().info("发送添加虚拟玩家数据包给 " + player.getName() + ": " + virtualPlayer.getName());
                    }
                } catch (Exception e) {
                    plugin.getLogger().warning("发送数据包给玩家 " + player.getName() + " 失败: " + e.getMessage());
                    if (plugin.getFakePlayerConfig().isDebug()) {
                        e.printStackTrace();
                    }
                }
            }

            if (plugin.getFakePlayerConfig().isDebug()) {
                plugin.getLogger().info("发送添加虚拟玩家数据包完成: " + virtualPlayer.getName());
            }
        } catch (Exception e) {
            plugin.getLogger().warning("发送添加虚拟玩家数据包失败: " + e.getMessage());
            if (plugin.getFakePlayerConfig().isDebug()) {
                e.printStackTrace();
            }
        }
    }

    /**
     * 发送移除虚拟玩家数据包
     */
    private void sendRemovePlayerPacket(VirtualPlayer virtualPlayer) {
        try {
            // Paper 1.21+ 使用新的 PLAYER_INFO_REMOVE 数据包类型
            PacketContainer packet = protocolManager.createPacket(PacketType.Play.Server.PLAYER_INFO_REMOVE);

            // 写入要移除的玩家UUID列表
            // 使用 BukkitConverters.getListConverter() 来转换UUID列表
            packet.getModifier().write(0, new ArrayList<>(java.util.Collections.singletonList(virtualPlayer.getUuid())));

            // 发送到所有在线玩家
            for (Player player : Bukkit.getOnlinePlayers()) {
                protocolManager.sendServerPacket(player, packet);
            }

            if (plugin.getFakePlayerConfig().isDebug()) {
                plugin.getLogger().info("发送移除虚拟玩家数据包: " + virtualPlayer.getName());
            }
        } catch (Exception e) {
            plugin.getLogger().warning("发送移除虚拟玩家数据包失败: " + e.getMessage());
            if (plugin.getFakePlayerConfig().isDebug()) {
                e.printStackTrace();
            }
        }
    }

    /**
     * 获取虚拟玩家数量
     */
    public int getVirtualPlayerCount() {
        return virtualPlayers.size();
    }

    /**
     * 获取所有虚拟玩家名
     */
    public List<String> getVirtualPlayerNames() {
        return new ArrayList<>(virtualPlayers.keySet());
    }

    /**
     * 虚拟玩家数据类
     */
    private static class VirtualPlayer {
        private final String name;
        private final UUID uuid;

        public VirtualPlayer(String name, UUID uuid) {
            this.name = name;
            this.uuid = uuid;
        }

        public String getName() {
            return name;
        }

        public UUID getUuid() {
            return uuid;
        }
    }
}