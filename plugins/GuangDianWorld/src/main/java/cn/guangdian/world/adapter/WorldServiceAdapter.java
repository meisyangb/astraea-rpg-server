package cn.guangdian.world.adapter;

import cn.guangdian.world.GuangDianWorld;
import cn.guangdian.world.api.WorldAPI;
import cn.guangdian.world.model.GDWorld;
import cn.guangdian.world.event.WorldCreatedEvent;
import cn.guangdian.world.event.WorldDeletedEvent;
import cn.guangdian.rpgcore.RPGCore;
import cn.guangdian.rpgcore.api.AsyncExecutor;
import cn.guangdian.rpgcore.api.ServiceRegistry;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Player;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

/**
 * 世界服务适配器
 *
 * <p>统一 WorldAPI 与 RPGCore 服务层的集成，
 * 支持 AsyncExecutor 异步操作。</p>
 *
 * @author GuangDian
 * @since 1.0.0
 */
public class WorldServiceAdapter implements WorldAPI {

    private final GuangDianWorld plugin;
    private final WorldAPI delegate;
    private final boolean useRPGCore;
    private AsyncExecutor asyncExecutor;

    public WorldServiceAdapter(GuangDianWorld plugin, WorldAPI delegate) {
        this.plugin = plugin;
        this.delegate = delegate;
        this.useRPGCore = Bukkit.getPluginManager().isPluginEnabled("RPGCore");
        
        if (useRPGCore) {
            try {
                RPGCore rpgCore = RPGCore.getInstance();
                ServiceRegistry registry = rpgCore.getServiceRegistry();
                this.asyncExecutor = rpgCore.getAsyncExecutor();

                // 注册服务
                registry.registerService(WorldAPI.class, this);
                plugin.getLogger().info("已注册到 RPGCore: WorldAPI (通过 ServiceAdapter)");
            } catch (Exception e) {
                plugin.getLogger().warning("注册到 RPGCore 失败: " + e.getMessage());
            }
        }
    }

    @Override
    public GDWorld getWorld(String name) {
        return delegate.getWorld(name);
    }

    @Override
    public GDWorld getWorld(World world) {
        return delegate.getWorld(world);
    }

    @Override
    public Collection<GDWorld> getAllWorlds() {
        return delegate.getAllWorlds();
    }

    @Override
    public List<String> getWorldNames() {
        return delegate.getWorldNames();
    }

    @Override
    public int getWorldCount() {
        return delegate.getWorldCount();
    }

    @Override
    public boolean worldExists(String name) {
        return delegate.worldExists(name);
    }

    @Override
    public boolean isWorldLoaded(String name) {
        return delegate.isWorldLoaded(name);
    }

    @Override
    public GDWorld createWorld(String name, World.Environment environment) {
        GDWorld world = delegate.createWorld(name, environment);

        // 发布世界创建事件（使用 Bukkit 事件系统）
        if (world != null) {
            WorldCreatedEvent event = new WorldCreatedEvent(name, environment, null);
            Bukkit.getPluginManager().callEvent(event);
        }

        return world;
    }

    @Override
    public boolean loadWorld(String name) {
        boolean result = delegate.loadWorld(name);
        
        if (result) {
            saveAsync();
        }
        
        return result;
    }

    @Override
    public boolean unloadWorld(String name) {
        boolean result = delegate.unloadWorld(name);
        
        if (result) {
            saveAsync();
        }
        
        return result;
    }

    @Override
    public boolean deleteWorld(String name) {
        // 获取世界信息用于事件
        GDWorld world = delegate.getWorld(name);

        // 发布世界删除事件（删除前，使用 Bukkit 事件系统）
        if (world != null) {
            WorldDeletedEvent event = new WorldDeletedEvent(name, null);
            Bukkit.getPluginManager().callEvent(event);
        }

        boolean result = delegate.deleteWorld(name);

        if (result) {
            saveAsync();
        }

        return result;
    }

    @Override
    public boolean teleportToWorld(Player player, String worldName) {
        return delegate.teleportToWorld(player, worldName);
    }

    @Override
    public void setSpawnPoint(String worldName, Location location) {
        delegate.setSpawnPoint(worldName, location);
        saveAsync();
    }

    @Override
    public String getWorldDisplayName(String worldName) {
        return delegate.getWorldDisplayName(worldName);
    }

    @Override
    public String getRespawnWorld(String worldName) {
        return delegate.getRespawnWorld(worldName);
    }

    /**
     * 异步保存数据
     */
    private void saveAsync() {
        if (asyncExecutor != null) {
            asyncExecutor.execute(() -> {
                plugin.getWorldManager().saveAllWorlds();
            });
        } else {
            // 降级：同步保存
            plugin.getWorldManager().saveAllWorlds();
        }
    }

    /**
     * 异步操作（带返回值）
     */
    public <T> CompletableFuture<T> executeAsync(java.util.concurrent.Callable<T> task) {
        CompletableFuture<T> future = new CompletableFuture<>();
        
        if (asyncExecutor != null) {
            asyncExecutor.execute(() -> {
                try {
                    future.complete(task.call());
                } catch (Exception e) {
                    future.completeExceptionally(e);
                }
            });
        } else {
            // 降级：同步执行
            try {
                future.complete(task.call());
            } catch (Exception e) {
                future.completeExceptionally(e);
            }
        }
        
        return future;
    }

    /**
     * 注销服务
     */
    public void unregister() {
        if (useRPGCore) {
            try {
                ServiceRegistry registry = RPGCore.getInstance().getServiceRegistry();
                registry.unregisterService(WorldAPI.class);
                plugin.getLogger().info("已从 RPGCore 注销: WorldAPI");
            } catch (Exception e) {
                plugin.getLogger().warning("从 RPGCore 注销失败: " + e.getMessage());
            }
        }
    }

    public boolean isUsingRPGCore() {
        return useRPGCore;
    }

    public Optional<AsyncExecutor> getAsyncExecutor() {
        return Optional.ofNullable(asyncExecutor);
    }
}