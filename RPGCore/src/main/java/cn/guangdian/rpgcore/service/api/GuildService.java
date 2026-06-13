package cn.guangdian.rpgcore.service.api;

import cn.guangdian.rpgcore.service.api.data.Guild;
import org.jetbrains.annotations.Nullable;

import java.util.UUID;

/**
 * 公会服务接口
 *
 * <p>提供公会/工会管理功能。</p>
 *
 * @author GuangDian
 * @since 1.0.0
 */
public interface GuildService {

    /**
     * 获取公会
     *
     * @param name 公会名称
     * @return 公会对象，如果不存在返回 null
     */
    @Nullable Guild getGuild(String name);

    /**
     * 获取玩家所在公会
     *
     * @param playerId 玩家UUID
     * @return 公会对象，如果不在公会返回 null
     */
    @Nullable Guild getPlayerGuild(UUID playerId);

    /**
     * 检查玩家是否在公会
     * 
     * @param playerId 玩家UUID
     * @return 如果在公会返回 true
     */
    boolean isInGuild(UUID playerId);

    /**
     * 创建公会
     * 
     * @param name 公会名称
     * @param leaderId 会长UUID
     * @return 如果创建成功返回 true
     */
    boolean createGuild(String name, UUID leaderId);

    /**
     * 解散公会
     * 
     * @param name 公会名称
     * @return 如果解散成功返回 true
     */
    boolean disbandGuild(String name);

    /**
     * 邀请玩家加入公会
     * 
     * @param guildName 公会名称
     * @param inviterId 邀请者UUID
     * @param targetId 被邀请者UUID
     * @return 如果邀请成功返回 true
     */
    boolean invitePlayer(String guildName, UUID inviterId, UUID targetId);

    /**
     * 玩家加入公会
     * 
     * @param guildName 公会名称
     * @param playerId 玩家UUID
     * @return 如果加入成功返回 true
     */
    boolean joinGuild(String guildName, UUID playerId);

    /**
     * 玩家离开公会
     * 
     * @param playerId 玩家UUID
     * @return 如果离开成功返回 true
     */
    boolean leaveGuild(UUID playerId);

    /**
     * 踢出成员
     * 
     * @param guildName 公会名称
     * @param kickerId 踢人者UUID
     * @param targetId 被踢者UUID
     * @return 如果踢出成功返回 true
     */
    boolean kickMember(String guildName, UUID kickerId, UUID targetId);

    /**
     * 获取公会成员数量
     * 
     * @param guildName 公会名称
     * @return 成员数量
     */
    int getMemberCount(String guildName);

    /**
     * 获取公会总数
     * 
     * @return 公会总数
     */
    int getGuildCount();
}