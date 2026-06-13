package cn.guangdian.dungeon.manager;

import cn.guangdian.dungeon.GuangDianDungeon;
import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.WorldCreator;
import org.bukkit.WorldType;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;

import java.io.File;
import java.io.IOException;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class WorldInstanceManager {
    private final GuangDianDungeon plugin;
    private final Map<String, InstanceInfo> activeInstances;
    private final String instancePrefix = "dungeon_inst_";
    
    public WorldInstanceManager(GuangDianDungeon plugin) {
        this.plugin = plugin;
        this.activeInstances = new ConcurrentHashMap<>();
    }
    
    public static class InstanceInfo {
        private final String instanceWorldName;
        private final String templateWorldName;
        private final String dungeonId;
        private final String sessionId;
        private final long createTime;
        private final Set<UUID> players;
        
        public InstanceInfo(String instanceWorldName, String templateWorldName, 
                           String dungeonId, String sessionId) {
            this.instanceWorldName = instanceWorldName;
            this.templateWorldName = templateWorldName;
            this.dungeonId = dungeonId;
            this.sessionId = sessionId;
            this.createTime = System.currentTimeMillis();
            this.players = new HashSet<>();
        }
        
        public String getInstanceWorldName() { return instanceWorldName; }
        public String getTemplateWorldName() { return templateWorldName; }
        public String getDungeonId() { return dungeonId; }
        public String getSessionId() { return sessionId; }
        public long getCreateTime() { return createTime; }
        public Set<UUID> getPlayers() { return players; }
        
        public void addPlayer(UUID playerId) { players.add(playerId); }
        public void removePlayer(UUID playerId) { players.remove(playerId); }
    }
    
    public InstanceInfo createInstance(String templateWorldName, String dungeonId, String sessionId) {
        World templateWorld = Bukkit.getWorld(templateWorldName);
        if (templateWorld == null) {
            plugin.getLogger().warning("Template world not found: " + templateWorldName);
            return null;
        }
        
        String instanceName = generateInstanceName(dungeonId, sessionId);
        
        File templateDir = new File(Bukkit.getWorldContainer(), templateWorldName);
        File instanceDir = new File(Bukkit.getWorldContainer(), instanceName);
        
        if (instanceDir.exists()) {
            plugin.getLogger().warning("Instance directory already exists: " + instanceName);
            deleteWorldFolder(instanceDir);
        }
        
        try {
            copyWorldFolder(templateDir.toPath(), instanceDir.toPath());
            plugin.getLogger().info("Copied world: " + templateWorldName + " -> " + instanceName);
        } catch (IOException e) {
            plugin.getLogger().severe("Failed to copy world: " + e.getMessage());
            return null;
        }
        
        deleteUidFile(instanceDir);
        
        WorldCreator creator = new WorldCreator(instanceName);
        creator.type(WorldType.FLAT);
        creator.generatorSettings("minecraft:air");
        
        World instanceWorld = Bukkit.createWorld(creator);
        if (instanceWorld == null) {
            plugin.getLogger().severe("Failed to create world: " + instanceName);
            return null;
        }
        
        InstanceInfo info = new InstanceInfo(instanceName, templateWorldName, dungeonId, sessionId);
        activeInstances.put(instanceName, info);
        
        plugin.getLogger().info("Created dungeon instance: " + instanceName + " for dungeon: " + dungeonId);
        
        return info;
    }
    
    private String generateInstanceName(String dungeonId, String sessionId) {
        return instancePrefix + dungeonId + "_" + sessionId;
    }
    
    private void copyWorldFolder(Path source, Path target) throws IOException {
        Files.walkFileTree(source, new SimpleFileVisitor<Path>() {
            @Override
            public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) throws IOException {
                Path targetDir = target.resolve(source.relativize(dir));
                Files.createDirectories(targetDir);
                return FileVisitResult.CONTINUE;
            }
            
            @Override
            public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                String fileName = file.getFileName().toString();
                if (fileName.equals("uid.dat") || fileName.equals("session.lock")) {
                    return FileVisitResult.CONTINUE;
                }
                Files.copy(file, target.resolve(source.relativize(file)), 
                          StandardCopyOption.REPLACE_EXISTING);
                return FileVisitResult.CONTINUE;
            }
        });
    }
    
    private void deleteUidFile(File worldDir) {
        File uidFile = new File(worldDir, "uid.dat");
        if (uidFile.exists()) {
            uidFile.delete();
        }
        File sessionLock = new File(worldDir, "session.lock");
        if (sessionLock.exists()) {
            sessionLock.delete();
        }
    }
    
    public void unloadAndDeleteInstance(String instanceWorldName) {
        InstanceInfo info = activeInstances.remove(instanceWorldName);
        if (info == null) {
            plugin.getLogger().warning("No instance info found for: " + instanceWorldName);
            return;
        }
        
        World world = Bukkit.getWorld(instanceWorldName);
        if (world != null) {
            for (UUID playerId : info.getPlayers()) {
                Player player = Bukkit.getPlayer(playerId);
                if (player != null && player.getWorld().equals(world)) {
                    teleportToExitWorld(player);
                }
            }
            
            Bukkit.getScheduler().runTask(plugin, () -> {
                boolean unloaded = Bukkit.unloadWorld(instanceWorldName, false);
                if (unloaded) {
                    plugin.getLogger().info("Unloaded world: " + instanceWorldName);
                    
                    new BukkitRunnable() {
                        @Override
                        public void run() {
                            File worldDir = new File(Bukkit.getWorldContainer(), instanceWorldName);
                            if (worldDir.exists()) {
                                deleteWorldFolder(worldDir);
                                plugin.getLogger().info("Deleted world folder: " + instanceWorldName);
                            }
                        }
                    }.runTaskLater(plugin, 20L);
                } else {
                    plugin.getLogger().warning("Failed to unload world: " + instanceWorldName);
                }
            });
        }
    }
    
    private void deleteWorldFolder(File folder) {
        if (folder.isDirectory()) {
            File[] files = folder.listFiles();
            if (files != null) {
                for (File file : files) {
                    deleteWorldFolder(file);
                }
            }
        }
        folder.delete();
    }
    
    public void teleportToExitWorld(Player player) {
        String exitWorldName = plugin.getConfig().getString("exit-world.world", "world");
        double x = plugin.getConfig().getDouble("exit-world.x", 0);
        double y = plugin.getConfig().getDouble("exit-world.y", 64);
        double z = plugin.getConfig().getDouble("exit-world.z", 0);
        float yaw = (float) plugin.getConfig().getDouble("exit-world.yaw", 0);
        float pitch = (float) plugin.getConfig().getDouble("exit-world.pitch", 0);
        
        World exitWorld = Bukkit.getWorld(exitWorldName);
        if (exitWorld == null) {
            exitWorld = Bukkit.getWorlds().get(0);
            plugin.getLogger().warning("Exit world not found: " + exitWorldName + ", using default world");
        }
        
        org.bukkit.Location exitLoc = new org.bukkit.Location(exitWorld, x, y, z, yaw, pitch);
        player.teleport(exitLoc);
        player.sendMessage(plugin.color("<yellow>你已离开副本"));
    }
    
    public InstanceInfo getInstance(String instanceWorldName) {
        return activeInstances.get(instanceWorldName);
    }
    
    public InstanceInfo getInstanceByPlayer(UUID playerId) {
        for (InstanceInfo info : activeInstances.values()) {
            if (info.getPlayers().contains(playerId)) {
                return info;
            }
        }
        return null;
    }
    
    public boolean isInstanceWorld(String worldName) {
        return activeInstances.containsKey(worldName);
    }
    
    public Collection<InstanceInfo> getAllInstances() {
        return activeInstances.values();
    }
    
    public void cleanupAllInstances() {
        for (String instanceName : new ArrayList<>(activeInstances.keySet())) {
            unloadAndDeleteInstance(instanceName);
        }
    }
    
    public void addPlayerToInstance(String instanceWorldName, UUID playerId) {
        InstanceInfo info = activeInstances.get(instanceWorldName);
        if (info != null) {
            info.addPlayer(playerId);
        }
    }
    
    public void removePlayerFromInstance(String instanceWorldName, UUID playerId) {
        InstanceInfo info = activeInstances.get(instanceWorldName);
        if (info != null) {
            info.removePlayer(playerId);
        }
    }
}
