package cn.guangdian.fakeonline;

import com.comphenix.protocol.PacketType;
import com.comphenix.protocol.ProtocolLibrary;
import com.comphenix.protocol.ProtocolManager;
import com.comphenix.protocol.events.PacketContainer;
import com.comphenix.protocol.wrappers.PlayerInfoData;
import com.comphenix.protocol.wrappers.WrappedGameProfile;
import com.comphenix.protocol.wrappers.EnumWrappers;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * 虚拟玩家管理器
 * 使用ProtocolLib发送真实的数据包，模拟真实玩家
 */
public class VirtualPlayerManager {

    private final GuangDianFakeOnline plugin;
    private final FakeOnlineConfig config;
    private final ProtocolManager protocolManager;
    
    // 虚拟玩家数据存储
    private final Map<UUID, VirtualPlayer> virtualPlayers = new ConcurrentHashMap<>();
    private final List<String> playerNames = new ArrayList<>();
    
    // 当前显示的虚拟玩家数量
    private int currentVirtualCount = 0;

    public VirtualPlayerManager(GuangDianFakeOnline plugin) {
        this.plugin = plugin;
        this.config = plugin.getFakeOnlineConfig();
        this.protocolManager = ProtocolLibrary.getProtocolManager();
        
        initializePlayerNames();
    }

    /**
     * 初始化虚拟玩家名列表
     */
    private void initializePlayerNames() {
        // 添加预设玩家名
        playerNames.addAll(Arrays.asList(
            "小明", "玩家A", "新手玩家", "老玩家", "RPG玩家",
            "冒险者", "战士", "法师", "弓箭手", "刺客",
            "新手", "高手", "大神", "萌新", "老手",
            "玩家1", "玩家2", "玩家3", "玩家4", "玩家5",
            "玩家6", "玩家7", "玩家8", "玩家9", "玩家10",
            "玩家11", "玩家12", "玩家13", "玩家14", "玩家15",
            "玩家16", "玩家17", "玩家18", "玩家19", "玩家20",
            "玩家21", "玩家22", "玩家23", "玩家24", "玩家25",
            "玩家26", "玩家27", "玩家28", "玩家29", "玩家30",
            "玩家31", "玩家32", "玩家33", "玩家34", "玩家35",
            "玩家36", "玩家37", "玩家38", "玩家39", "玩家40",
            "玩家41", "玩家42", "玩家43", "玩家44", "玩家45",
            "玩家46", "玩家47", "玩家48", "玩家49", "玩家50"
        ));
    }

    /**
     * 初始化虚拟玩家
     */
    public void initializeVirtualPlayers() {
        int targetCount = config.getVirtualPlayerCount();
        
        // 创建虚拟玩家
        for (int i = 0; i < targetCount; i++) {
            createVirtualPlayer(i);
        }
        
        currentVirtualCount = targetCount;
        
        // 发送给所有在线玩家
        for (Player player : Bukkit.getOnlinePlayers()) {
            sendVirtualPlayersToPlayer(player);
        }
        
        plugin.getLogger().info("已创建 " + currentVirtualCount + " 个虚拟玩家");
    }

    /**
     * 创建单个虚拟玩家
     */
    private void createVirtualPlayer(int index) {
        String name = playerNames.get(index % playerNames.size());
        UUID uuid = generateUUID(name);
        
        // 获取真实皮肤（从Mojang API）
        WrappedGameProfile profile = getRealSkinProfile(name, uuid);
        
        VirtualPlayer virtualPlayer = new VirtualPlayer(
            uuid,
            name,
            profile,
            EnumWrappers.PlayerInfoAction.ADD_PLAYER,
            EnumWrappers.NativeGameMode.SURVIVAL,
            generateRandomPing()
        );
        
        virtualPlayers.put(uuid, virtualPlayer);
    }

    /**
     * 生成UUID
     */
    private UUID generateUUID(String name) {
        // 使用名字生成固定的UUID
        return UUID.nameUUIDFromBytes(name.getBytes());
    }

    /**
     * 获取真实皮肤Profile
     */
    private WrappedGameProfile getRealSkinProfile(String name, UUID uuid) {
        // 尝试从Mojang API获取真实皮肤
        try {
            // 使用预设的皮肤玩家名（Steve, Alex等）
            String skinName = getSkinName(name);
            
            // 创建一个带有皮肤的Profile
            // 注意：ProtocolLib的WrappedGameProfile.fromPlayer()需要Player对象
            // 这里我们使用UUID和名字创建一个基础Profile
            WrappedGameProfile profile = new WrappedGameProfile(uuid, name);
            
            // 如果需要真实皮肤，可以尝试从缓存或其他方式获取
            // 这里简化处理，使用默认Profile
            
            return profile;
        } catch (Exception e) {
            plugin.getLogger().warning("无法获取玩家 " + name + " 的皮肤: " + e.getMessage());
        }
        
        // 如果无法获取，使用默认Profile
        return new WrappedGameProfile(uuid, name);
    }

    /**
     * 获取皮肤名（根据玩家名映射）
     */
    private String getSkinName(String playerName) {
        // 使用一些真实的玩家名作为皮肤来源
        String[] skinNames = {"Steve", "Alex", "Notch", "jeb_", " Dinnerbone"};
        int index = Math.abs(playerName.hashCode()) % skinNames.length;
        return skinNames[index];
    }

    /**
     * 生成随机Ping值
     */
    private int generateRandomPing() {
        return 20 + new Random().nextInt(80); // 20-100ms
    }

    /**
     * 发送虚拟玩家列表给指定玩家
     */
    public void sendVirtualPlayersToPlayer(Player player) {
        if (virtualPlayers.isEmpty()) {
            return;
        }

        try {
            // ProtocolLib 5.x 使用新的数据包结构
            // 我们需要逐个发送虚拟玩家，而不是批量发送
            
            for (VirtualPlayer virtualPlayer : virtualPlayers.values()) {
                sendSingleVirtualPlayer(player, virtualPlayer);
            }
            
        } catch (Exception e) {
            plugin.getLogger().warning("发送虚拟玩家数据包失败: " + e.getMessage());
            if (plugin.getFakeOnlineConfig().isDebug()) {
                e.printStackTrace();
            }
        }
    }

    /**
     * 添加虚拟玩家（模拟加入）
     */
    public void addVirtualPlayer(String name) {
        UUID uuid = generateUUID(name);
        
        if (virtualPlayers.containsKey(uuid)) {
            return; // 已存在
        }
        
        // 创建虚拟玩家
        createVirtualPlayer(virtualPlayers.size());
        
        // 发送加入消息
        sendJoinMessage(name);
        
        // 发送给所有在线玩家
        for (Player player : Bukkit.getOnlinePlayers()) {
            sendSingleVirtualPlayer(player, virtualPlayers.get(uuid));
        }
        
        currentVirtualCount++;
        plugin.getLogger().info("虚拟玩家 " + name + " 已加入");
    }

    /**
     * 移除虚拟玩家（模拟退出）
     */
    public void removeVirtualPlayer(String name) {
        UUID uuid = generateUUID(name);
        
        VirtualPlayer virtualPlayer = virtualPlayers.get(uuid);
        if (virtualPlayer == null) {
            return; // 不存在
        }
        
        // 发送退出消息
        sendQuitMessage(name);
        
        // 发送移除数据包给所有在线玩家
        for (Player player : Bukkit.getOnlinePlayers()) {
            sendRemoveVirtualPlayer(player, virtualPlayer);
        }
        
        virtualPlayers.remove(uuid);
        currentVirtualCount--;
        plugin.getLogger().info("虚拟玩家 " + name + " 已退出");
    }

    /**
     * 发送单个虚拟玩家给指定玩家
     */
    private void sendSingleVirtualPlayer(Player player, VirtualPlayer virtualPlayer) {
        try {
            // 使用ProtocolLib的PlayerInfoPacket构建器
            // 这是ProtocolLib 5.x推荐的方式
            
            PacketContainer packet = protocolManager.createPacket(PacketType.Play.Server.PLAYER_INFO);
            
            // 创建单个PlayerInfoData
            List<PlayerInfoData> playerInfoDataList = new ArrayList<>();
            PlayerInfoData data = new PlayerInfoData(
                virtualPlayer.getProfile(),
                virtualPlayer.getPing(),
                virtualPlayer.getGameMode(),
                null
            );
            playerInfoDataList.add(data);
            
            // 设置数据列表
            packet.getPlayerInfoDataLists().write(0, playerInfoDataList);
            
            // 使用EnumSet而不是HashSet
            EnumSet<EnumWrappers.PlayerInfoAction> actions = EnumSet.of(EnumWrappers.PlayerInfoAction.ADD_PLAYER);
            packet.getPlayerInfoActions().write(0, actions);
            
            protocolManager.sendServerPacket(player, packet);
            
        } catch (Exception e) {
            plugin.getLogger().warning("发送单个虚拟玩家失败: " + e.getMessage());
            if (plugin.getFakeOnlineConfig().isDebug()) {
                e.printStackTrace();
            }
        }
    }

    /**
     * 发送移除虚拟玩家数据包
     */
    private void sendRemoveVirtualPlayer(Player player, VirtualPlayer virtualPlayer) {
        try {
            PacketContainer packet = protocolManager.createPacket(PacketType.Play.Server.PLAYER_INFO);
            
            List<PlayerInfoData> playerInfoDataList = new ArrayList<>();
            PlayerInfoData data = new PlayerInfoData(
                virtualPlayer.getProfile(),
                virtualPlayer.getPing(),
                virtualPlayer.getGameMode(),
                null
            );
            playerInfoDataList.add(data);
            
            packet.getPlayerInfoDataLists().write(0, playerInfoDataList);
            
            // 使用EnumSet而不是HashSet
            EnumSet<EnumWrappers.PlayerInfoAction> actions = EnumSet.of(EnumWrappers.PlayerInfoAction.REMOVE_PLAYER);
            packet.getPlayerInfoActions().write(0, actions);
            
            protocolManager.sendServerPacket(player, packet);
            
        } catch (Exception e) {
            plugin.getLogger().warning("发送移除虚拟玩家失败: " + e.getMessage());
            if (plugin.getFakeOnlineConfig().isDebug()) {
                e.printStackTrace();
            }
        }
    }

    /**
     * 发送加入消息
     */
    private void sendJoinMessage(String name) {
        String message = config.getJoinMessage(name);
        
        for (Player player : Bukkit.getOnlinePlayers()) {
            player.sendMessage(message);
        }
    }

    /**
     * 发送退出消息
     */
    private void sendQuitMessage(String name) {
        String message = config.getQuitMessage(name);
        
        for (Player player : Bukkit.getOnlinePlayers()) {
            player.sendMessage(message);
        }
    }

    /**
     * 清理所有虚拟玩家
     */
    public void cleanup() {
        // 移除所有虚拟玩家
        for (Player player : Bukkit.getOnlinePlayers()) {
            for (VirtualPlayer virtualPlayer : virtualPlayers.values()) {
                sendRemoveVirtualPlayer(player, virtualPlayer);
            }
        }
        
        virtualPlayers.clear();
        currentVirtualCount = 0;
        
        plugin.getLogger().info("已清理所有虚拟玩家");
    }

    /**
     * 获取当前虚拟玩家数量
     */
    public int getCurrentVirtualCount() {
        return currentVirtualCount;
    }

    /**
     * 获取虚拟玩家列表
     */
    public Map<UUID, VirtualPlayer> getVirtualPlayers() {
        return virtualPlayers;
    }
}