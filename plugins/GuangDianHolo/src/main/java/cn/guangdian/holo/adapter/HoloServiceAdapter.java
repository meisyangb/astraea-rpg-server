package cn.guangdian.holo.adapter;

import cn.guangdian.holo.GuangDianHolo;
import cn.guangdian.holo.api.HologramAPI;
import cn.guangdian.holo.model.Hologram;
import cn.guangdian.holo.event.HologramCreatedEvent;
import cn.guangdian.holo.event.HologramDeletedEvent;
import cn.guangdian.rpgcore.RPGCore;
import cn.guangdian.rpgcore.api.AsyncExecutor;
import cn.guangdian.rpgcore.api.ServiceRegistry;
import org.bukkit.Bukkit;
import org.bukkit.Location;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

/**
 * 全息图服务适配器
 *
 * <p>统一 HologramAPI 与 RPGCore 服务层的集成，
 * 支持 AsyncExecutor 异步操作。</p>
 *
 * @author GuangDian
 * @since 1.0.0
 */
public class HoloServiceAdapter implements HologramAPI {

    private final GuangDianHolo plugin;
    private final HologramAPI delegate;
    private final boolean useRPGCore;
    private AsyncExecutor asyncExecutor;

    public HoloServiceAdapter(GuangDianHolo plugin, HologramAPI delegate) {
        this.plugin = plugin;
        this.delegate = delegate;
        this.useRPGCore = Bukkit.getPluginManager().isPluginEnabled("RPGCore");
        
        if (useRPGCore) {
            try {
                RPGCore rpgCore = RPGCore.getInstance();
                ServiceRegistry registry = rpgCore.getServiceRegistry();
                this.asyncExecutor = rpgCore.getAsyncExecutor();

                // 注册服务
                registry.registerService(HologramAPI.class, this);
                plugin.getLogger().info("已注册到 RPGCore: HologramAPI (通过 ServiceAdapter)");
            } catch (Exception e) {
                plugin.getLogger().warning("注册到 RPGCore 失败: " + e.getMessage());
            }
        }
    }

    @Override
    public Hologram getHologram(String name) {
        return delegate.getHologram(name);
    }

    @Override
    public Collection<Hologram> getAllHolograms() {
        return delegate.getAllHolograms();
    }

    @Override
    public List<String> getHologramNames() {
        return delegate.getHologramNames();
    }

    @Override
    public int getHologramCount() {
        return delegate.getHologramCount();
    }

    @Override
    public boolean hologramExists(String name) {
        return delegate.hologramExists(name);
    }

    @Override
    public Hologram createHologram(String name, Location location) {
        Hologram holo = delegate.createHologram(name, location);

        // 发布全息图创建事件（使用 Bukkit 事件系统）
        if (holo != null) {
            HologramCreatedEvent event = new HologramCreatedEvent(name, name, location, holo.getLines().size());
            Bukkit.getPluginManager().callEvent(event);
        }

        return holo;
    }

    @Override
    public boolean deleteHologram(String name) {
        // 获取全息图信息用于事件
        Hologram holo = delegate.getHologram(name);
        Location location = holo != null ? holo.getLocation() : null;

        // 发布全息图删除事件（删除前）
        if (location != null) {
            HologramDeletedEvent event = new HologramDeletedEvent(name, name, location);
            Bukkit.getPluginManager().callEvent(event);
        }
        
        boolean result = delegate.deleteHologram(name);
        
        if (result) {
            saveAsync();
        }
        
        return result;
    }

    @Override
    public void addLine(String holoName, String text) {
        delegate.addLine(holoName, text);
        saveAsync();
    }

    @Override
    public void setLine(String holoName, int lineIndex, String text) {
        delegate.setLine(holoName, lineIndex, text);
        saveAsync();
    }

    @Override
    public void removeLine(String holoName, int lineIndex) {
        delegate.removeLine(holoName, lineIndex);
        saveAsync();
    }

    @Override
    public void clearLines(String holoName) {
        delegate.clearLines(holoName);
        saveAsync();
    }

    @Override
    public void teleport(String holoName, Location location) {
        delegate.teleport(holoName, location);
        saveAsync();
    }

    @Override
    public void setViewDistance(String holoName, int distance) {
        delegate.setViewDistance(holoName, distance);
        saveAsync();
    }

    @Override
    public void setVisible(String holoName, boolean visible) {
        delegate.setVisible(holoName, visible);
        saveAsync();
    }

    /**
     * 异步保存数据
     */
    private void saveAsync() {
        if (asyncExecutor != null) {
            asyncExecutor.execute(() -> {
                plugin.getHologramManager().saveHolograms();
            });
        } else {
            // 降级：同步保存
            plugin.getHologramManager().saveHolograms();
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
                registry.unregisterService(HologramAPI.class);
                plugin.getLogger().info("已从 RPGCore 注销: HologramAPI");
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