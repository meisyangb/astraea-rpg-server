package cn.guangdian.rpgcore.service.api;

import java.util.UUID;
import java.util.concurrent.CompletableFuture;

/**
 * 洞府服务接口
 * 
 * @author GuangDian
 * @since 1.0.0
 */
public interface CaveService {

    /**
     * 获取玩家洞府
     */
    Object getPlayerCave(UUID playerId);

    /**
     * 检查玩家是否有洞府
     */
    boolean hasCave(UUID playerId);

    /**
     * 创建洞府
     */
    boolean createCave(UUID playerId);

    /**
     * 删除洞府
     */
    boolean deleteCave(UUID playerId);

    /**
     * 获取洞府等级
     */
    int getCaveLevel(UUID playerId);

    /**
     * 升级洞府
     */
    boolean upgradeCave(UUID playerId);

    /**
     * 传送至洞府
     */
    boolean teleportToCave(UUID playerId);

    /**
     * 邀请玩家访问洞府
     */
    boolean inviteToCave(UUID ownerId, UUID guestId);

    /**
     * 获取洞府总数
     */
    int getCaveCount();

    /**
     * 检查服务是否可用
     */
    boolean isAvailable();
}