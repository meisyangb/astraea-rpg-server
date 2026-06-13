package cn.guangdian.dungeon.integration;

import cn.guangdian.dungeon.GuangDianDungeon;
import cn.guangdian.dungeon.model.DungeonTemplate;
import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.WorldCreator;
import org.bukkit.WorldType;

import java.io.File;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.CompletableFuture;

public class SlimeWorldBridge {

    private final GuangDianDungeon plugin;
    private final String instancePrefix;
    private final Set<String> loadedInstances;
    private boolean slimeEnabled;

    public SlimeWorldBridge(GuangDianDungeon plugin) {
        this.plugin = plugin;
        this.instancePrefix = plugin.getConfig().getString("world.instance-prefix", "dungeon_");
        this.loadedInstances = new HashSet<>();
        this.slimeEnabled = false;

        checkSlimeWorldManager();
    }

    private void checkSlimeWorldManager() {
        slimeEnabled = Bukkit.getPluginManager().isPluginEnabled("SlimeWorldManager") ||
                       Bukkit.getPluginManager().isPluginEnabled("SWM");

        if (slimeEnabled) {
            plugin.getLogger().info("Slime World Manager 集成已启用");
        } else {
            plugin.getLogger().warning("Slime World Manager 未安装，将使用标准世界加载");
        }
    }

    public boolean isSlimeEnabled() {
        return slimeEnabled;
    }

    /**
     * 异步加载世界
     */
    public CompletableFuture<World> loadWorld(DungeonTemplate template, String instanceId) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                if (slimeEnabled) {
                    return loadSlimeWorld(template, instanceId);
                } else {
                    return loadStandardWorld(template, instanceId);
                }
            } catch (Exception e) {
                plugin.getLogger().severe("加载世界失败: " + e.getMessage());
                return null;
            }
        });
    }

    private World loadSlimeWorld(DungeonTemplate template, String instanceId) {
        try {
            String templateName = template.getWorldTemplate();
            String worldName = instancePrefix + instanceId;

            Object slimePlugin = getSlimePlugin();
            if (slimePlugin == null) {
                return loadStandardWorld(template, instanceId);
            }

            Object loader = getSlimeLoader(slimePlugin);
            if (loader == null) {
                return loadStandardWorld(template, instanceId);
            }

            Object templateWorld = loadSlimeTemplate(slimePlugin, loader, templateName);
            if (templateWorld == null) {
                plugin.getLogger().warning("无法加载Slime模板: " + templateName);
                return loadStandardWorld(template, instanceId);
            }

            Object instanceWorld = cloneSlimeWorld(templateWorld, worldName);
            if (instanceWorld == null) {
                return loadStandardWorld(template, instanceId);
            }

            importSlimeWorld(slimePlugin, instanceWorld);

            World world = Bukkit.getWorld(worldName);
            if (world != null) {
                loadedInstances.add(worldName);
                plugin.getLogger().info("Slime世界加载成功: " + worldName);
                return world;
            }
        } catch (Exception e) {
            plugin.getLogger().warning("Slime加载失败，回退到标准加载: " + e.getMessage());
        }

        return loadStandardWorld(template, instanceId);
    }

    private Object getSlimePlugin() {
        try {
            return Bukkit.getPluginManager().getPlugin("SlimeWorldManager");
        } catch (Exception e) {
            return null;
        }
    }

    private Object getSlimeLoader(Object slimePlugin) {
        try {
            Class<?> pluginClass = slimePlugin.getClass();
            return pluginClass.getMethod("getLoader", String.class)
                .invoke(slimePlugin, "file");
        } catch (Exception e) {
            return null;
        }
    }

    private Object loadSlimeTemplate(Object slimePlugin, Object loader, String templateName) {
        try {
            Class<?> pluginClass = slimePlugin.getClass();
            return pluginClass.getMethod("loadWorld", loader.getClass(), String.class, boolean.class)
                .invoke(slimePlugin, loader, templateName, false);
        } catch (Exception e) {
            return null;
        }
    }

    private Object cloneSlimeWorld(Object templateWorld, String worldName) {
        try {
            Class<?> worldClass = templateWorld.getClass();
            return worldClass.getMethod("clone", String.class)
                .invoke(templateWorld, worldName);
        } catch (Exception e) {
            return null;
        }
    }

    private void importSlimeWorld(Object slimePlugin, Object slimeWorld) {
        try {
            Class<?> pluginClass = slimePlugin.getClass();
            pluginClass.getMethod("importWorld", slimeWorld.getClass())
                .invoke(slimePlugin, slimeWorld);
        } catch (Exception e) {
            plugin.getLogger().warning("导入Slime世界失败: " + e.getMessage());
        }
    }

    private World loadStandardWorld(DungeonTemplate template, String instanceId) {
        String worldName = instancePrefix + instanceId;

        World existingWorld = Bukkit.getWorld(worldName);
        if (existingWorld != null) {
            return existingWorld;
        }

        World templateWorld = Bukkit.getWorld(template.getWorldTemplate());
        if (templateWorld != null) {
            return templateWorld;
        }

        File worldDir = new File(Bukkit.getWorldContainer(), template.getWorldTemplate());
        if (!worldDir.exists()) {
            plugin.getLogger().warning("世界模板目录不存在: " + template.getWorldTemplate());
            return null;
        }

        WorldCreator creator = new WorldCreator(worldName);
        creator.type(WorldType.NORMAL);
        creator.environment(World.Environment.NORMAL);

        World world = Bukkit.createWorld(creator);
        if (world != null) {
            loadedInstances.add(worldName);
            plugin.getLogger().info("标准世界加载成功: " + worldName);
        }

        return world;
    }

    public void unloadWorld(String worldName) {
        World world = Bukkit.getWorld(worldName);
        if (world == null) return;

        world.getPlayers().forEach(player -> {
            player.teleport(Bukkit.getWorlds().get(0).getSpawnLocation());
        });

        Bukkit.unloadWorld(world, false);
        loadedInstances.remove(worldName);

        if (slimeEnabled) {
            deleteSlimeWorld(worldName);
        }

        plugin.getLogger().info("卸载世界: " + worldName);
    }

    private void deleteSlimeWorld(String worldName) {
        try {
            Object slimePlugin = getSlimePlugin();
            if (slimePlugin == null) return;

            Class<?> pluginClass = slimePlugin.getClass();
            pluginClass.getMethod("deleteWorld", String.class)
                .invoke(slimePlugin, worldName);
        } catch (Exception e) {
            plugin.getLogger().warning("删除Slime世界失败: " + e.getMessage());
        }
    }

    public void cleanupAll() {
        for (String worldName : new HashSet<>(loadedInstances)) {
            try {
                unloadWorld(worldName);
            } catch (Exception e) {
                plugin.getLogger().warning("清理世界失败: " + worldName + " - " + e.getMessage());
            }
        }
    }

    public boolean isInstanceWorld(String worldName) {
        return worldName != null && worldName.startsWith(instancePrefix);
    }

    public String getInstanceIdFromWorld(String worldName) {
        if (!isInstanceWorld(worldName)) return null;
        return worldName.substring(instancePrefix.length());
    }
}
