package cn.guangdian.guild.storage;

import cn.guangdian.guild.GuangDianGuild;
import cn.guangdian.guild.GuangDianGuild.Guild;
import cn.guangdian.guild.GuangDianGuild.GuildMember;
import cn.guangdian.guild.GuangDianGuild.GuildRank;
import cn.guangdian.rpgcore.database.CoreDatabase;

import java.sql.*;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 公会数据库存储管理器
 * 
 * <p>使用 MySQL 数据库存储公会数据，支持异步操作。</p>
 * 
 * @author GuangDian
 * @since 1.0.0
 */
public class DatabaseStorage {

    private final GuangDianGuild plugin;
    private final Map<String, Guild> guilds = new ConcurrentHashMap<>();
    private final Map<String, Guild> playerGuilds = new ConcurrentHashMap<>();

    public DatabaseStorage(GuangDianGuild plugin) {
        this.plugin = plugin;
    }

    /**
     * 从数据库加载所有数据
     */
    public void load() {
        if (!CoreDatabase.isEnabled()) {
            plugin.getLogger().warning("数据库未启用，无法加载公会数据");
            return;
        }

        guilds.clear();
        playerGuilds.clear();

        try (Connection conn = CoreDatabase.getConnection()) {
            loadGuilds(conn);
            loadMembers(conn);

            plugin.getLogger().info("已从数据库加载 " + guilds.size() + " 个公会");
        } catch (SQLException e) {
            plugin.getLogger().severe("加载公会数据库数据失败: " + e.getMessage());
            plugin.getLogger().log(java.util.logging.Level.SEVERE, "详细异常信息", e);
        }
    }

    private void loadGuilds(Connection conn) throws SQLException {
        String sql = "SELECT guild_id, guild_name, owner_uuid, level, exp FROM guild_data";
        
        try (PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            
            while (rs.next()) {
                String guildId = rs.getString("guild_id");
                String guildName = rs.getString("guild_name");
                String ownerUuid = rs.getString("owner_uuid");
                
                Guild guild = new Guild(guildName, ownerUuid);
                guilds.put(guildId, guild);
                playerGuilds.put(ownerUuid.toLowerCase(), guild);
            }
        }
    }

    private void loadMembers(Connection conn) throws SQLException {
        String sql = "SELECT guild_id, uuid, rank, contribution FROM guild_members";
        
        try (PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            
            while (rs.next()) {
                String guildId = rs.getString("guild_id");
                String memberUuid = rs.getString("uuid");
                String rankStr = rs.getString("rank");
                int contribution = rs.getInt("contribution");

                Guild guild = guilds.get(guildId);
                if (guild != null) {
                    GuildRank rank = GuildRank.valueOf(rankStr);
                    GuildMember member = new GuildMember(memberUuid, rank);
                    member.contribution = contribution;
                    guild.members.put(memberUuid, member);
                    playerGuilds.put(memberUuid.toLowerCase(), guild);
                }
            }
        }
    }

    /**
     * 异步保存所有数据
     */
    public CompletableFuture<Void> saveAsync() {
        return CompletableFuture.runAsync(this::saveSync);
    }

    /**
     * 同步保存所有数据
     */
    public void saveSync() {
        if (!CoreDatabase.isEnabled()) {
            return;
        }

        try (Connection conn = CoreDatabase.getConnection()) {
            conn.setAutoCommit(false);
            
            try {
                saveGuilds(conn);
                saveMembers(conn);
                conn.commit();
            } catch (SQLException e) {
                conn.rollback();
                throw e;
            }
        } catch (SQLException e) {
            plugin.getLogger().severe("保存公会数据到数据库失败: " + e.getMessage());
            plugin.getLogger().log(java.util.logging.Level.SEVERE, "详细异常信息", e);
        }
    }

    private void saveGuilds(Connection conn) throws SQLException {
        String deleteSql = "DELETE FROM guild_data";
        try (PreparedStatement ps = conn.prepareStatement(deleteSql)) {
            ps.executeUpdate();
        }

        String insertSql = "INSERT INTO guild_data (guild_id, guild_name, owner_uuid, level, exp) VALUES (?, ?, ?, ?, ?)";
        try (PreparedStatement ps = conn.prepareStatement(insertSql)) {
            for (Map.Entry<String, Guild> entry : guilds.entrySet()) {
                Guild guild = entry.getValue();
                ps.setString(1, entry.getKey());
                ps.setString(2, guild.name);
                ps.setString(3, guild.leader);
                ps.setInt(4, 1);
                ps.setLong(5, 0);
                ps.addBatch();
            }
            ps.executeBatch();
        }
    }

    private void saveMembers(Connection conn) throws SQLException {
        String deleteSql = "DELETE FROM guild_members";
        try (PreparedStatement ps = conn.prepareStatement(deleteSql)) {
            ps.executeUpdate();
        }

        String insertSql = "INSERT INTO guild_members (guild_id, uuid, rank, contribution) VALUES (?, ?, ?, ?)";
        try (PreparedStatement ps = conn.prepareStatement(insertSql)) {
            for (Map.Entry<String, Guild> entry : guilds.entrySet()) {
                String guildId = entry.getKey();
                Guild guild = entry.getValue();
                
                for (Map.Entry<String, GuildMember> memberEntry : guild.members.entrySet()) {
                    GuildMember member = memberEntry.getValue();
                    ps.setString(1, guildId);
                    ps.setString(2, memberEntry.getKey());
                    ps.setString(3, member.rank.name());
                    ps.setInt(4, member.contribution);
                    ps.addBatch();
                }
            }
            ps.executeBatch();
        }
    }

    /**
     * 创建公会
     */
    public boolean createGuild(String guildId, String name, String leaderUuid) {
        if (!CoreDatabase.isEnabled()) {
            return false;
        }

        try (Connection conn = CoreDatabase.getConnection()) {
            String sql = "INSERT INTO guild_data (guild_id, guild_name, owner_uuid, level, exp) VALUES (?, ?, ?, ?, ?)";
            
            try (PreparedStatement ps = conn.prepareStatement(sql)) {
                ps.setString(1, guildId);
                ps.setString(2, name);
                ps.setString(3, leaderUuid);
                ps.setInt(4, 1);
                ps.setLong(5, 0);
                ps.executeUpdate();
            }

            Guild guild = new Guild(name, leaderUuid);
            guilds.put(guildId, guild);
            playerGuilds.put(leaderUuid.toLowerCase(), guild);
            return true;
        } catch (SQLException e) {
            plugin.getLogger().severe("创建公会失败: " + e.getMessage());
            return false;
        }
    }

    /**
     * 删除公会
     */
    public boolean deleteGuild(String guildId) {
        if (!CoreDatabase.isEnabled()) {
            return false;
        }

        Guild guild = guilds.get(guildId);
        if (guild == null) return false;

        try (Connection conn = CoreDatabase.getConnection()) {
            String sql = "DELETE FROM guild_data WHERE guild_id = ?";
            try (PreparedStatement ps = conn.prepareStatement(sql)) {
                ps.setString(1, guildId);
                ps.executeUpdate();
            }

            guilds.remove(guildId);
            for (String memberUuid : guild.members.keySet()) {
                playerGuilds.remove(memberUuid.toLowerCase());
            }
            return true;
        } catch (SQLException e) {
            plugin.getLogger().severe("删除公会失败: " + e.getMessage());
            return false;
        }
    }

    /**
     * 添加成员
     */
    public void addMember(String guildId, String memberUuid, GuildRank rank) {
        if (!CoreDatabase.isEnabled()) {
            return;
        }

        Guild guild = guilds.get(guildId);
        if (guild == null) return;

        try (Connection conn = CoreDatabase.getConnection()) {
            String sql = "INSERT INTO guild_members (guild_id, uuid, rank, contribution) VALUES (?, ?, ?, ?)";
            try (PreparedStatement ps = conn.prepareStatement(sql)) {
                ps.setString(1, guildId);
                ps.setString(2, memberUuid);
                ps.setString(3, rank.name());
                ps.setInt(4, 0);
                ps.executeUpdate();
            }

            guild.members.put(memberUuid, new GuildMember(memberUuid, rank));
            playerGuilds.put(memberUuid.toLowerCase(), guild);
        } catch (SQLException e) {
            plugin.getLogger().severe("添加公会成员失败: " + e.getMessage());
        }
    }

    /**
     * 移除成员
     */
    public void removeMember(String memberUuid) {
        if (!CoreDatabase.isEnabled()) {
            return;
        }

        try (Connection conn = CoreDatabase.getConnection()) {
            String sql = "DELETE FROM guild_members WHERE uuid = ?";
            try (PreparedStatement ps = conn.prepareStatement(sql)) {
                ps.setString(1, memberUuid);
                ps.executeUpdate();
            }

            Guild guild = playerGuilds.remove(memberUuid.toLowerCase());
            if (guild != null) {
                guild.members.remove(memberUuid);
            }
        } catch (SQLException e) {
            plugin.getLogger().severe("移除公会成员失败: " + e.getMessage());
        }
    }

    public Map<String, Guild> getGuilds() {
        return guilds;
    }

    public Map<String, Guild> getPlayerGuilds() {
        return playerGuilds;
    }

    public Guild getGuild(String guildId) {
        return guilds.get(guildId);
    }

    public Guild getPlayerGuild(String playerUuid) {
        return playerGuilds.get(playerUuid.toLowerCase());
    }

    public int getGuildCount() {
        return guilds.size();
    }
}
