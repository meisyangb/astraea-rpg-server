package cn.guangdian.fakeplayer;

import cn.guangdian.auth.GuangDianAuth;
import cn.guangdian.rpgcore.RPGCore;
import cn.guangdian.rpgcore.service.api.AuthService;
import org.bukkit.Bukkit;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 虚拟玩家数据管理器
 * 通过GuangDianAuth注册虚拟玩家，并创建数据文件
 */
public class VirtualPlayerDataManager {

    private final GuangDianFakePlayer plugin;
    private final Map<String, VirtualPlayer> virtualPlayers = new ConcurrentHashMap<>();
    private final List<String> presetNames = new ArrayList<>();
    private final Path playerDataDir;
    private final AuthService authService;
    private int nameIndex = 0;

    public VirtualPlayerDataManager(GuangDianFakePlayer plugin) {
        this.plugin = plugin;
        this.playerDataDir = Paths.get(Bukkit.getWorlds().get(0).getWorldFolder().getAbsolutePath(), "playerdata");
        this.authService = getAuthService();
        loadPresetNames();
        ensurePlayerDataDirExists();
    }

    /**
     * 获取AuthService
     */
    private AuthService getAuthService() {
        // 尝试从RPGCore获取AuthService
        if (Bukkit.getPluginManager().isPluginEnabled("RPGCore")) {
            RPGCore rpgCore = RPGCore.getInstance();
            if (rpgCore != null) {
                try {
                    return rpgCore.getServiceRegistry().getService(AuthService.class);
                } catch (Exception e) {
                    plugin.getLogger().warning("无法从RPGCore获取AuthService: " + e.getMessage());
                }
            }
        }

        // 尝试从GuangDianAuth直接获取（使用反射）
        if (Bukkit.getPluginManager().isPluginEnabled("GuangDianAuth")) {
            GuangDianAuth auth = GuangDianAuth.getInstance();
            if (auth != null) {
                try {
                    // 使用反射获取private字段
                    java.lang.reflect.Field field = GuangDianAuth.class.getDeclaredField("serviceAdapter");
                    field.setAccessible(true);
                    return (AuthService) field.get(auth);
                } catch (Exception e) {
                    plugin.getLogger().warning("无法从GuangDianAuth获取serviceAdapter: " + e.getMessage());
                }
            }
        }

        return null;
    }

    private void loadPresetNames() {
        List<String> names = plugin.getConfig().getStringList("virtual-players.preset-names");
        if (names != null && !names.isEmpty()) {
            presetNames.addAll(names);
        } else {
            // 默认预设名字
            for (int i = 1; i <= 50; i++) {
                presetNames.add("Player" + i);
            }
        }
    }

    private void ensurePlayerDataDirExists() {
        try {
            if (!Files.exists(playerDataDir)) {
                Files.createDirectories(playerDataDir);
                plugin.getLogger().info("创建玩家数据目录: " + playerDataDir);
            }
        } catch (IOException e) {
            plugin.getLogger().warning("无法创建玩家数据目录: " + e.getMessage());
        }
    }

    /**
     * 注册虚拟玩家
     */
    public void registerVirtualPlayer(String name, UUID uuid) {
        if (authService != null) {
            // 通过AuthService注册玩家
            // 使用随机密码（虚拟玩家不需要登录）
            String randomPassword = UUID.randomUUID().toString().substring(0, 8);
            String ip = "127.0.0.1";

            try {
                authService.register(name, uuid, randomPassword, ip);
                if (plugin.getConfig().getBoolean("debug", false)) {
                    plugin.getLogger().info("通过AuthService注册虚拟玩家: " + name + ", UUID: " + uuid);
                }
            } catch (Exception e) {
                plugin.getLogger().warning("通过AuthService注册虚拟玩家失败: " + e.getMessage());
            }
        } else {
            plugin.getLogger().warning("AuthService不可用，无法注册虚拟玩家");
        }
    }

    /**
     * 创建虚拟玩家数据文件
     */
    public void createVirtualPlayerDataFile(String name, UUID uuid) {
        Path dataFile = playerDataDir.resolve(uuid.toString() + ".dat");

        try {
            // 检查文件是否已存在
            if (Files.exists(dataFile)) {
                if (plugin.getConfig().getBoolean("debug", false)) {
                    plugin.getLogger().info("虚拟玩家数据文件已存在: " + name + ", UUID: " + uuid);
                }
                return; // 不覆盖已存在的文件
            }

            // 创建NBT数据
            Map<String, Object> nbtData = createNBTData(name, uuid);

            // 写入数据文件（使用压缩的NBT格式）
            writeCompressedNBTData(dataFile, nbtData);

            if (plugin.getConfig().getBoolean("debug", false)) {
                plugin.getLogger().info("创建虚拟玩家数据文件: " + name + ", UUID: " + uuid);
                plugin.getLogger().info("数据文件路径: " + dataFile);
            }
        } catch (IOException e) {
            plugin.getLogger().warning("无法创建虚拟玩家数据文件: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * 创建NBT数据
     */
    private Map<String, Object> createNBTData(String name, UUID uuid) {
        Map<String, Object> nbtData = new HashMap<>();

        // UUID信息
        nbtData.put("UUID", uuid.toString());
        nbtData.put("UUIDMost", uuid.getMostSignificantBits());
        nbtData.put("UUIDLeast", uuid.getLeastSignificantBits());

        // 基础信息
        Map<String, Object> bukkitData = new HashMap<>();
        bukkitData.put("lastPlayed", System.currentTimeMillis());
        bukkitData.put("firstPlayed", System.currentTimeMillis());
        bukkitData.put("displayName", name);
        bukkitData.put("knownName", name);
        nbtData.put("bukkit", bukkitData);

        // 位置信息
        org.bukkit.World world = Bukkit.getWorlds().get(0);
        double spawnX = world.getSpawnLocation().getX();
        double spawnY = world.getSpawnLocation().getY();
        double spawnZ = world.getSpawnLocation().getZ();

        List<Double> pos = new ArrayList<>();
        pos.add(spawnX);
        pos.add(spawnY);
        pos.add(spawnZ);
        nbtData.put("Pos", pos);

        // 旋转信息
        List<Float> rotation = new ArrayList<>();
        rotation.add(0.0f); // yaw
        rotation.add(0.0f); // pitch
        nbtData.put("Rotation", rotation);

        nbtData.put("World", world.getName());

        // 游戏模式 - 0: 生存, 1: 创造, 2: 冒险, 3: 旁观
        nbtData.put("playerGameType", 0);

        // 生命值
        nbtData.put("Health", 20.0f);
        nbtData.put("HealthPerLife", 20.0f);

        // 食物
        nbtData.put("foodLevel", 20);
        nbtData.put("foodSaturationLevel", 5.0f);
        nbtData.put("foodExhaustionLevel", 0.0f);
        nbtData.put("foodTickTimer", 0);

        // 经验
        nbtData.put("XpLevel", 0);
        nbtData.put("XpP", 0.0f);
        nbtData.put("XpTotal", 0);
        nbtData.put("XpSeed", 0);
        nbtData.put("Score", 0);

        // 难度和游戏时间
        nbtData.put("Difficulty", world.getDifficulty().getValue());
        nbtData.put("Time", world.getTime());
        nbtData.put("DayTime", world.getFullTime());

        // 附加数据（空的）
        nbtData.put("Attributes", createAttributes());
        nbtData.put("Inventory", new ArrayList<>());
        nbtData.put("EnderItems", new ArrayList<>());
        nbtData.put("ActiveEffects", new ArrayList<>());

        // 出生点
        nbtData.put("SpawnX", (int) spawnX);
        nbtData.put("SpawnY", (int) spawnY);
        nbtData.put("SpawnZ", (int) spawnZ);
        nbtData.put("SpawnWorld", world.getName());
        nbtData.put("HasSpawnLocation", 1);

        return nbtData;
    }

    /**
     * 创建属性数据
     */
    private List<Map<String, Object>> createAttributes() {
        List<Map<String, Object>> attributes = new ArrayList<>();

        // 最大生命值属性
        Map<String, Object> maxHealth = new HashMap<>();
        maxHealth.put("Name", "generic.max_health");
        maxHealth.put("Base", 20.0);
        maxHealth.put("Max", 2048.0);
        attributes.add(maxHealth);

        // 跟随范围属性
        Map<String, Object> followRange = new HashMap<>();
        followRange.put("Name", "generic.follow_range");
        followRange.put("Base", 32.0);
        followRange.put("Max", 2048.0);
        attributes.add(followRange);

        // 护甲属性
        Map<String, Object> armor = new HashMap<>();
        armor.put("Name", "generic.armor");
        armor.put("Base", 0.0);
        armor.put("Max", 2048.0);
        attributes.add(armor);

        // 护甲韧性属性
        Map<String, Object> armorToughness = new HashMap<>();
        armorToughness.put("Name", "generic.armor_toughness");
        armorToughness.put("Base", 0.0);
        armorToughness.put("Max", 2048.0);
        attributes.add(armorToughness);

        // 攻击速度属性
        Map<String, Object> attackSpeed = new HashMap<>();
        attackSpeed.put("Name", "generic.attack_speed");
        attackSpeed.put("Base", 4.0);
        attackSpeed.put("Max", 2048.0);
        attributes.add(attackSpeed);

        // 移动速度属性
        Map<String, Object> moveSpeed = new HashMap<>();
        moveSpeed.put("Name", "generic.movement_speed");
        moveSpeed.put("Base", 0.1);
        moveSpeed.put("Max", 2048.0);
        attributes.add(moveSpeed);

        // 击退抗性属性
        Map<String, Object> knockbackRes = new HashMap<>();
        knockbackRes.put("Name", "generic.knockback_resistance");
        knockbackRes.put("Base", 0.0);
        knockbackRes.put("Max", 2048.0);
        attributes.add(knockbackRes);

        // 幸运属性
        Map<String, Object> luck = new HashMap<>();
        luck.put("Name", "generic.luck");
        luck.put("Base", 0.0);
        luck.put("Max", 2048.0);
        attributes.add(luck);

        return attributes;
    }

    /**
     * 写入压缩的NBT数据到文件（Minecraft使用的格式）
     */
    private void writeCompressedNBTData(Path dataFile, Map<String, Object> nbtData) throws IOException {
        // 使用GZIP压缩的NBT格式写入
        try (DataOutputStream dos = new DataOutputStream(
                new java.util.zip.GZIPOutputStream(Files.newOutputStream(dataFile)))) {

            // 写入NBT标签（TAG_Compound = 10）
            dos.writeByte(10);

            // 写入名称（空字符串）
            dos.writeShort(0);

            // 写入所有NBT数据
            writeNBTCompound(dos, nbtData);
        }
    }

    /**
     * 写入NBT复合标签
     */
    private void writeNBTCompound(DataOutputStream dos, Map<String, Object> nbtData) throws IOException {
        for (Map.Entry<String, Object> entry : nbtData.entrySet()) {
            String key = entry.getKey();
            Object value = entry.getValue();

            writeNBTValue(dos, key, value);
        }

        // 写入结束标签（TAG_End = 0）
        dos.writeByte(0);
    }

    /**
     * 写入NBT值
     */
    private void writeNBTValue(DataOutputStream dos, String key, Object value) throws IOException {
        // 写入键名
        dos.writeUTF(key);

        if (value instanceof String) {
            // TAG_String = 8
            dos.writeByte(8);
            dos.writeUTF((String) value);
        } else if (value instanceof Integer) {
            // TAG_Int = 3
            dos.writeByte(3);
            dos.writeInt((Integer) value);
        } else if (value instanceof Long) {
            // TAG_Long = 4
            dos.writeByte(4);
            dos.writeLong((Long) value);
        } else if (value instanceof Float) {
            // TAG_Float = 5
            dos.writeByte(5);
            dos.writeFloat((Float) value);
        } else if (value instanceof Double) {
            // TAG_Double = 6
            dos.writeByte(6);
            dos.writeDouble((Double) value);
        } else if (value instanceof Short) {
            // TAG_Short = 2
            dos.writeByte(2);
            dos.writeShort((Short) value);
        } else if (value instanceof Byte) {
            // TAG_Byte = 1
            dos.writeByte(1);
            dos.writeByte((Byte) value);
        } else if (value instanceof List) {
            // TAG_List = 9
            dos.writeByte(9);
            writeNBTList(dos, (List<?>) value);
        } else if (value instanceof Map) {
            // TAG_Compound = 10
            dos.writeByte(10);
            writeNBTCompound(dos, (Map<String, Object>) value);
        } else if (value instanceof byte[]) {
            // TAG_Byte_Array = 7
            dos.writeByte(7);
            byte[] byteArray = (byte[]) value;
            dos.writeInt(byteArray.length);
            dos.write(byteArray);
        } else if (value instanceof int[]) {
            // TAG_Int_Array = 11
            dos.writeByte(11);
            int[] intArray = (int[]) value;
            dos.writeInt(intArray.length);
            for (int i : intArray) {
                dos.writeInt(i);
            }
        } else if (value instanceof long[]) {
            // TAG_Long_Array = 12
            dos.writeByte(12);
            long[] longArray = (long[]) value;
            dos.writeInt(longArray.length);
            for (long l : longArray) {
                dos.writeLong(l);
            }
        }
    }

    /**
     * 写入NBT列表
     */
    @SuppressWarnings("unchecked")
    private void writeNBTList(DataOutputStream dos, List<?> list) throws IOException {
        if (list.isEmpty()) {
            dos.writeByte(1); // TAG_Byte类型（空列表默认为byte类型）
            dos.writeInt(0);
            return;
        }

        // 确定列表元素类型
        Object firstElement = list.get(0);
        byte tagType;

        if (firstElement instanceof String) {
            tagType = 8;
        } else if (firstElement instanceof Integer) {
            tagType = 3;
        } else if (firstElement instanceof Long) {
            tagType = 4;
        } else if (firstElement instanceof Float) {
            tagType = 5;
        } else if (firstElement instanceof Double) {
            tagType = 6;
        } else if (firstElement instanceof Short) {
            tagType = 2;
        } else if (firstElement instanceof Byte) {
            tagType = 1;
        } else if (firstElement instanceof Map) {
            tagType = 10;
        } else {
            // 默认使用int类型
            tagType = 3;
        }

        dos.writeByte(tagType);
        dos.writeInt(list.size());

        // 写入列表元素
        for (Object element : list) {
            writeNBTValue(dos, "", element);
        }
    }

    /**
     * 批量创建虚拟玩家
     */
    public void createVirtualPlayers(int count) {
        plugin.getLogger().info("开始批量创建 " + count + " 个虚拟玩家");

        int createdCount = 0;
        for (int i = 0; i < count && nameIndex < presetNames.size(); i++) {
            String name = presetNames.get(nameIndex);
            UUID uuid = UUID.randomUUID();
            VirtualPlayer virtualPlayer = new VirtualPlayer(name, uuid);
            virtualPlayers.put(name, virtualPlayer);

            // 通过AuthService注册虚拟玩家（如果可用）
            registerVirtualPlayer(name, uuid);

            // 创建数据文件
            createVirtualPlayerDataFile(name, uuid);

            nameIndex++;
            createdCount++;
        }

        plugin.getLogger().info("批量创建完成，实际创建: " + createdCount + " 个虚拟玩家");
        plugin.getLogger().info("当前虚拟玩家总数: " + virtualPlayers.size());
        if (authService != null) {
            plugin.getLogger().info("已通过AuthService注册虚拟玩家");
        }
    }

    /**
     * 清理所有虚拟玩家
     */
    public void clearAllVirtualPlayers() {
        // 删除数据文件
        for (VirtualPlayer virtualPlayer : virtualPlayers.values()) {
            Path dataFile = playerDataDir.resolve(virtualPlayer.getUuid().toString() + ".dat");
            try {
                Files.deleteIfExists(dataFile);
            } catch (IOException e) {
                plugin.getLogger().warning("无法删除虚拟玩家数据文件: " + e.getMessage());
            }
        }

        virtualPlayers.clear();
        nameIndex = 0;
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
     * 获取所有虚拟玩家
     */
    public Map<String, VirtualPlayer> getVirtualPlayers() {
        return virtualPlayers;
    }

    /**
     * 虚拟玩家数据类
     */
    public static class VirtualPlayer {
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