package cn.guangdian.rpgcore.service.api.data;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.UUID;

/**
 * 公会数据接口
 *
 * <p>定义公会的基本属性和操作，由具体插件实现。</p>
 *
 * @author Astraea RPG Team
 * @since 1.2.0
 */
public interface Guild {

    /**
     * 获取公会唯一标识符
     *
     * @return 公会ID
     */
    @NotNull String getId();

    /**
     * 获取公会名称
     *
     * @return 公会名称
     */
    @NotNull String getName();

    /**
     * 获取公会显示名称（带格式）
     *
     * @return 显示名称
     */
    @NotNull String getDisplayName();

    /**
     * 获取公会会长UUID
     *
     * @return 会长UUID
     */
    @NotNull UUID getLeaderId();

    /**
     * 获取公会成员列表
     *
     * @return 成员UUID列表
     */
    @NotNull List<UUID> getMembers();

    /**
     * 获取公会成员数量
     *
     * @return 成员数量
     */
    int getMemberCount();

    /**
     * 获取公会最大成员数
     *
     * @return 最大成员数
     */
    int getMaxMembers();

    /**
     * 获取公会等级
     *
     * @return 公会等级
     */
    int getLevel();

    /**
     * 获取公会经验值
     *
     * @return 经验值
     */
    long getExperience();

    /**
     * 获取公会资金
     *
     * @return 资金
     */
    double getBalance();

    /**
     * 获取公会公告
     *
     * @return 公告内容，如果没有返回 null
     */
    @Nullable String getNotice();

    /**
     * 获取公会创建时间
     *
     * @return 创建时间戳（毫秒）
     */
    long getCreateTime();

    /**
     * 检查玩家是否是成员
     *
     * @param playerId 玩家UUID
     * @return 如果是成员返回 true
     */
    boolean isMember(@NotNull UUID playerId);

    /**
     * 检查玩家是否是管理员
     *
     * @param playerId 玩家UUID
     * @return 如果是管理员返回 true
     */
    boolean isAdmin(@NotNull UUID playerId);

    /**
     * 检查玩家是否是会长
     *
     * @param playerId 玩家UUID
     * @return 如果是会长返回 true
     */
    boolean isLeader(@NotNull UUID playerId);

    /**
     * 获取玩家在公会中的职位
     *
     * @param playerId 玩家UUID
     * @return 职位名称
     */
    @NotNull String getMemberRole(@NotNull UUID playerId);
}
