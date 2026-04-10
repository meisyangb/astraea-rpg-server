package cn.guangdian.rpgcore.service.api;

import org.bukkit.Location;
import org.bukkit.World;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

/**
 * 坐标点服务接口
 * 
 * <p>提供坐标点的保存、查询、删除功能。</p>
 * 
 * <h3>使用示例：</h3>
 * <pre>{@code
 * // 获取服务
 * LocationService locationService = serviceRegistry.getService(LocationService.class);
 * 
 * // 保存坐标
 * locationService.saveLocation(playerUuid, "home", location);
 * 
 * // 获取坐标
 * Optional<Location> loc = locationService.getLocation(playerUuid, "home");
 * 
 * // 列出所有坐标
 * List<SavedLocation> locations = locationService.listLocations(playerUuid);
 * }</pre>
 * 
 * @author GuangDian
 * @since 1.0.0
 */
public interface LocationService {

    // ==================== 同步操作 ====================

    /**
     * 保存坐标点
     * 
     * @param playerId 玩家UUID
     * @param name 坐标名称
     * @param location 坐标位置
     * @return 是否保存成功
     */
    boolean saveLocation(UUID playerId, String name, Location location);

    /**
     * 获取坐标点
     * 
     * @param playerId 玩家UUID
     * @param name 坐标名称
     * @return 坐标位置（如果存在）
     */
    Optional<Location> getLocation(UUID playerId, String name);

    /**
     * 删除坐标点
     * 
     * @param playerId 玩家UUID
     * @param name 坐标名称
     * @return 是否删除成功
     */
    boolean deleteLocation(UUID playerId, String name);

    /**
     * 列出玩家的所有坐标点
     * 
     * @param playerId 玩家UUID
     * @return 坐标点列表
     */
    List<SavedLocationInfo> listLocations(UUID playerId);

    /**
     * 检查坐标点是否存在
     * 
     * @param playerId 玩家UUID
     * @param name 坐标名称
     * @return 是否存在
     */
    boolean hasLocation(UUID playerId, String name);

    /**
     * 获取玩家的坐标点数量
     * 
     * @param playerId 玩家UUID
     * @return 坐标点数量
     */
    int getLocationCount(UUID playerId);

    // ==================== 异步操作 ====================

    /**
     * 异步保存坐标点
     * 
     * @param playerId 玩家UUID
     * @param name 坐标名称
     * @param location 坐标位置
     * @return 包含操作结果的 CompletableFuture
     */
    CompletableFuture<Boolean> saveLocationAsync(UUID playerId, String name, Location location);

    /**
     * 异步获取坐标点
     * 
     * @param playerId 玩家UUID
     * @param name 坐标名称
     * @return 包含坐标位置的 CompletableFuture
     */
    CompletableFuture<Optional<Location>> getLocationAsync(UUID playerId, String name);

    /**
     * 异步删除坐标点
     * 
     * @param playerId 玩家UUID
     * @param name 坐标名称
     * @return 包含操作结果的 CompletableFuture
     */
    CompletableFuture<Boolean> deleteLocationAsync(UUID playerId, String name);

    /**
     * 异步列出坐标点
     * 
     * @param playerId 玩家UUID
     * @return 包含坐标列表的 CompletableFuture
     */
    CompletableFuture<List<SavedLocationInfo>> listLocationsAsync(UUID playerId);

    // ==================== 管理操作 ====================

    /**
     * 清空玩家的所有坐标点
     * 
     * @param playerId 玩家UUID
     * @return 清空的坐标点数量
     */
    int clearLocations(UUID playerId);

    /**
     * 获取所有坐标点的总数（管理统计）
     * 
     * @return 总数
     */
    int getTotalLocationCount();

    /**
     * 坐标点信息（简化结构）
     */
    record SavedLocationInfo(
        String name,
        String worldName,
        double x,
        double y,
        double z,
        float pitch,
        float yaw,
        long createdTime
    ) {
        /**
         * 从 Location 创建信息
         */
        public static SavedLocationInfo fromLocation(String name, Location location, long createdTime) {
            World world = location.getWorld();
            return new SavedLocationInfo(
                name,
                world != null ? world.getName() : "unknown",
                location.getX(),
                location.getY(),
                location.getZ(),
                location.getPitch(),
                location.getYaw(),
                createdTime
            );
        }

        /**
         * 格式化显示
         */
        public String toDisplayString() {
            return String.format("%s: [%s] X=%.1f, Y=%.1f, Z=%.1f", 
                name, worldName, x, y, z);
        }
    }
}