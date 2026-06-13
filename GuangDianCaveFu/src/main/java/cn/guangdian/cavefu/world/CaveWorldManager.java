package cn.guangdian.cavefu.world;

import cn.guangdian.cavefu.GuangDianCaveFu;
import cn.guangdian.cavefu.cave.Cave;
import cn.guangdian.cavefu.cave.CaveLevel;
import cn.guangdian.cavefu.config.ConfigManager;
import cn.guangdian.cavefu.hook.LuckPermsHook;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.WorldCreator;
import org.bukkit.WorldType;
import org.bukkit.entity.Player;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

/**
 * 洞府世界管理器
 */
public class CaveWorldManager {
    private final GuangDianCaveFu plugin;
    private final ConfigManager configManager;

    private World caveWorld;
    private int defaultViewDistance;
    private final Set<UUID> playersInCave = new HashSet<>();
    private LuckPermsHook luckPermsHook;

    public CaveWorldManager(GuangDianCaveFu plugin) {
        this.plugin = plugin;
        this.configManager = plugin.getConfigManager();
    }

    public void init() {
        String worldName = configManager.getWorldName();
        caveWorld = Bukkit.getWorld(worldName);

        boolean isNewWorld = false;
        
        if (caveWorld == null) {
            // 检查世界文件夹是否存在
            java.io.File worldFolder = new java.io.File(Bukkit.getWorldContainer(), worldName);
            boolean worldExists = worldFolder.exists() && worldFolder.isDirectory();
            
            if (worldExists) {
                // 世界文件夹存在，加载世界
                plugin.getLogger().info("检测到洞府世界文件夹，正在加载: " + worldName);
                
                WorldCreator creator = new WorldCreator(worldName);
                creator.type(WorldType.FLAT);
                creator.generator(new VoidGenerator());
                creator.generateStructures(false);
                
                caveWorld = creator.createWorld();
                
                if (caveWorld != null) {
                    plugin.getLogger().info("洞府世界加载成功: " + worldName);
                } else {
                    plugin.getLogger().severe("洞府世界加载失败！");
                }
            } else {
                // 世界不存在，创建新世界
                plugin.getLogger().info("正在创建洞府世界: " + worldName);
                
                WorldCreator creator = new WorldCreator(worldName);
                creator.type(WorldType.FLAT);
                creator.generator(new VoidGenerator());
                creator.generateStructures(false);
                creator.seed(System.currentTimeMillis());
                
                caveWorld = creator.createWorld();
                isNewWorld = true;
                
                if (caveWorld != null) {
                    plugin.getLogger().info("洞府世界创建成功！");
                } else {
                    plugin.getLogger().severe("洞府世界创建失败！");
                }
            }
            
            if (caveWorld != null) {
                // 设置世界属性
                caveWorld.setKeepSpawnInMemory(false);
                caveWorld.setAutoSave(true);

                // 设置死亡不掉落
                caveWorld.setGameRule(org.bukkit.GameRule.KEEP_INVENTORY, true);
                // 防止生物生成
                caveWorld.setGameRule(org.bukkit.GameRule.DO_MOB_SPAWNING, false);
                // 关闭火焰蔓延
                caveWorld.setGameRule(org.bukkit.GameRule.DO_FIRE_TICK, false);
            }
        } else {
            plugin.getLogger().info("洞府世界已加载: " + worldName);
        }

        // 新世界需要生成出生点平台
        if (isNewWorld && caveWorld != null) {
            generateSpawnPlatform();
        }

        // 配置 LuckPerms 世界权限继承
        setupLuckPerms(worldName);

        // 获取服务器默认视距
        defaultViewDistance = Bukkit.getViewDistance();
    }

    /**
     * 配置 LuckPerms 世界权限继承
     */
    private void setupLuckPerms(String worldName) {
        luckPermsHook = new LuckPermsHook(worldName);
        if (luckPermsHook.isEnabled()) {
            // 检查是否已配置，避免重复配置
            if (!luckPermsHook.isPermissionsConfigured()) {
                plugin.getLogger().info("正在配置洞府世界权限继承...");
                luckPermsHook.setupWorldPermissions();
            } else {
                plugin.getLogger().info("洞府世界权限已配置");
            }
        }
    }
    
    /**
     * 生成出生点平台（世界中心）
     * 作为管理员入口和备用出生点
     */
    private void generateSpawnPlatform() {
        int baseY = configManager.getBaseY();

        // 生成6x6的基岩平台
        for (int x = -3; x <= 2; x++) {
            for (int z = -3; z <= 2; z++) {
                caveWorld.getBlockAt(x, baseY, z).setType(Material.BEDROCK);
            }
        }

        // 设置出生点在平台中心上方
        caveWorld.setSpawnLocation(0, baseY + 2, 0);

        plugin.getLogger().info("已生成出生点平台(6x6)，位置: (0, " + baseY + ", 0)");
    }

    /**
     * 获取洞府世界出生点位置
     */
    public Location getWorldSpawnLocation() {
        if (caveWorld == null) {
            return null;
        }
        int baseY = configManager.getBaseY();
        return new Location(caveWorld, 0.5, baseY + 2, 0.5, 0, 0);
    }

    public World getCaveWorld() {
        return caveWorld;
    }

    public boolean isCaveWorld(World world) {
        return world != null && world.getName().equals(configManager.getWorldName());
    }

    /**
     * 计算洞府中心坐标
     */
    public Location calculateCaveCenter(int caveId) {
        int gridSize = configManager.getGridSize();
        int rowWidth = 100; // 每行100个洞府

        int row = caveId / rowWidth;
        int col = caveId % rowWidth;

        int x = col * gridSize;
        int z = row * gridSize;

        return new Location(caveWorld, x, configManager.getBaseY(), z);
    }

    /**
     * 生成洞府平台
     */
    public void generatePlatform(Cave cave) {
        if (caveWorld == null) {
            plugin.getLogger().severe("洞府世界未加载！无法生成平台！");
            return;
        }
        
        CaveLevel level = configManager.getLevel(cave.getLevel());
        if (level == null) {
            plugin.getLogger().warning("无法获取洞府等级配置: " + cave.getLevel());
            return;
        }

        int size = level.getSize();
        int halfSize = size / 2;
        int baseY = configManager.getBaseY();

        Material platformBlock = Material.matchMaterial(configManager.getPlatformBlock());
        if (platformBlock == null) {
            platformBlock = Material.BEDROCK;
        }

        Material surfaceBlock = Material.matchMaterial(configManager.getSurfaceBlock());
        if (surfaceBlock == null) {
            surfaceBlock = Material.GRASS_BLOCK;
        }

        int centerX = cave.getCenterX();
        int centerZ = cave.getCenterZ();

        plugin.getLogger().info("正在生成洞府平台: 中心(" + centerX + "," + centerZ + ") Y=" + baseY + " 大小=" + size);

        // 生成底层平台（基岩）
        for (int x = centerX - halfSize; x < centerX + halfSize; x++) {
            for (int z = centerZ - halfSize; z < centerZ + halfSize; z++) {
                caveWorld.getBlockAt(x, baseY, z).setType(platformBlock);
            }
        }

        // 生成表面层（草坪）
        for (int x = centerX - halfSize; x < centerX + halfSize; x++) {
            for (int z = centerZ - halfSize; z < centerZ + halfSize; z++) {
                caveWorld.getBlockAt(x, baseY + 1, z).setType(surfaceBlock);
            }
        }

        plugin.getLogger().info("已生成洞府平台: ID=" + cave.getId() + " 大小=" + size + "x" + size);
    }

    /**
     * 扩展洞府平台（升级时）
     */
    public void expandPlatform(Cave cave, int oldSize, int newSize) {
        int halfOldSize = oldSize / 2;
        int halfNewSize = newSize / 2;
        int baseY = configManager.getBaseY();

        Material platformBlock = Material.matchMaterial(configManager.getPlatformBlock());
        if (platformBlock == null) {
            platformBlock = Material.BEDROCK;
        }

        Material surfaceBlock = Material.matchMaterial(configManager.getSurfaceBlock());
        if (surfaceBlock == null) {
            surfaceBlock = Material.GRASS_BLOCK;
        }

        int centerX = cave.getCenterX();
        int centerZ = cave.getCenterZ();

        // 只生成扩展部分
        for (int x = centerX - halfNewSize; x < centerX + halfNewSize; x++) {
            for (int z = centerZ - halfNewSize; z < centerZ + halfNewSize; z++) {
                // 跳过原有区域
                if (x >= centerX - halfOldSize && x < centerX + halfOldSize
                    && z >= centerZ - halfOldSize && z < centerZ + halfOldSize) {
                    continue;
                }
                // 底层
                caveWorld.getBlockAt(x, baseY, z).setType(platformBlock);
                // 表面层
                caveWorld.getBlockAt(x, baseY + 1, z).setType(surfaceBlock);
            }
        }

        plugin.getLogger().info("已扩展洞府平台: ID=" + cave.getId() + " 新大小=" + newSize + "x" + newSize);
    }

    /**
     * 玩家进入洞府世界时限制视距
     */
    public void onEnterCave(Player player) {
        if (!playersInCave.contains(player.getUniqueId())) {
            playersInCave.add(player.getUniqueId());
            // 视距由世界配置控制，不需要单独设置玩家视距
        }
    }

    /**
     * 玩家离开洞府世界时恢复视距
     */
    public void onLeaveCave(Player player) {
        if (playersInCave.remove(player.getUniqueId())) {
            // 视距由世界配置控制
        }
    }

    public boolean isInCaveWorld(Player player) {
        return isCaveWorld(player.getWorld());
    }

    public int getDefaultViewDistance() {
        return defaultViewDistance;
    }
}