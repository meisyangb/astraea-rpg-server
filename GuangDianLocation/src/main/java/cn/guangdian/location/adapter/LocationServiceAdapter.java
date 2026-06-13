package cn.guangdian.location.adapter;

import cn.guangdian.location.GuangDianLocation;
import cn.guangdian.location.service.LocationStorageService;
import cn.guangdian.rpgcore.RPGCore;
import cn.guangdian.rpgcore.api.ServiceRegistry;
import cn.guangdian.rpgcore.service.api.LocationService;
import org.bukkit.Bukkit;
import org.bukkit.Location;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

/**
 * LocationService 适配器
 * 
 * <p>连接 GuangDianLocation 实现与 LocationService 接口，
 * 支持两种运行模式：</p>
 * 
 * <ul>
 *   <li>RPGCore 模式：当 RPGCore 可用时，通过 ServiceRegistry 注册服务</li>
 *   <li>独立模式：当 RPGCore 不可用时，使用独立的实现</li>
 * </ul>
 * 
 * @author GuangDian
 * @since 1.0.0
 */
public class LocationServiceAdapter implements LocationService {

    private final GuangDianLocation plugin;
    private final LocationStorageService storageService;
    private final boolean useRPGCore;

    public LocationServiceAdapter(GuangDianLocation plugin) {
        this.plugin = plugin;
        this.storageService = plugin.getStorageService();
        this.useRPGCore = Bukkit.getPluginManager().isPluginEnabled("RPGCore");

        // 如果 RPGCore 可用，注册服务
        if (useRPGCore) {
            try {
                RPGCore rpgCore = RPGCore.getInstance();
                ServiceRegistry registry = rpgCore.getServiceRegistry();

                registry.registerService(LocationService.class, this);
                plugin.getLogger().info("已注册到 RPGCore 服务注册表: LocationService");
            } catch (Exception e) {
                plugin.getLogger().warning("注册到 RPGCore 失败: " + e.getMessage());
            }
        }
    }

    @Override
    public boolean saveLocation(UUID playerId, String name, Location location) {
        return storageService.saveLocation(playerId, name, location);
    }

    @Override
    public Optional<Location> getLocation(UUID playerId, String name) {
        return storageService.getLocation(playerId, name);
    }

    @Override
    public boolean deleteLocation(UUID playerId, String name) {
        return storageService.deleteLocation(playerId, name);
    }

    @Override
    public List<SavedLocationInfo> listLocations(UUID playerId) {
        return storageService.listLocations(playerId);
    }

    @Override
    public boolean hasLocation(UUID playerId, String name) {
        return storageService.hasLocation(playerId, name);
    }

    @Override
    public int getLocationCount(UUID playerId) {
        return storageService.getLocationCount(playerId);
    }

    @Override
    public CompletableFuture<Boolean> saveLocationAsync(UUID playerId, String name, Location location) {
        return storageService.saveLocationAsync(playerId, name, location);
    }

    @Override
    public CompletableFuture<Optional<Location>> getLocationAsync(UUID playerId, String name) {
        return storageService.getLocationAsync(playerId, name);
    }

    @Override
    public CompletableFuture<Boolean> deleteLocationAsync(UUID playerId, String name) {
        return storageService.deleteLocationAsync(playerId, name);
    }

    @Override
    public CompletableFuture<List<SavedLocationInfo>> listLocationsAsync(UUID playerId) {
        return storageService.listLocationsAsync(playerId);
    }

    @Override
    public int clearLocations(UUID playerId) {
        return storageService.clearLocations(playerId);
    }

    @Override
    public int getTotalLocationCount() {
        return storageService.getTotalLocationCount();
    }

    /**
     * 注销服务
     */
    public void unregister() {
        if (useRPGCore) {
            try {
                ServiceRegistry registry = RPGCore.getInstance().getServiceRegistry();
                registry.unregisterService(LocationService.class);
                plugin.getLogger().info("已从 RPGCore 服务注册表注销: LocationService");
            } catch (Exception e) {
                plugin.getLogger().warning("从 RPGCore 注销失败: " + e.getMessage());
            }
        }
    }

    /**
     * 检查是否使用 RPGCore
     */
    public boolean isUsingRPGCore() {
        return useRPGCore;
    }
}