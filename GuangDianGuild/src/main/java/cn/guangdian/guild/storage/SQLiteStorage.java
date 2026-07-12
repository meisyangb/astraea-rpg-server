package cn.guangdian.guild.storage;

import cn.guangdian.guild.GuangDianGuild;
import cn.guangdian.guild.GuangDianGuild.Guild;
import cn.guangdian.guild.GuangDianGuild.GuildMember;
import cn.guangdian.guild.GuangDianGuild.GuildRank;
import org.bukkit.plugin.java.JavaPlugin;

import java.sql.*;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * SQLite 存储管理器
 * 独立数据库，不依赖 RPGCore
 */
public class SQLiteStorage {

    private final GuangDianGuild plugin;
    private Connection connection;

    public SQLiteStorage(GuangDianGuild plugin) {
        this.plugin = plugin;
        init();
    }

    private void init() {
        try {
            String dbPath = plugin.getDataFolder().getAbsolutePath() + java.io.File.separator + "guild.db";
            connection = DriverManager.getConnection("jdbc:sqlite:" + dbPath);
            createTables();
            plugin.getLogger().info("SQLite 数据库已初始化: " + dbPath);
        } catch (SQLException e) {
            plugin.getLogger().severe("初始化 SQLite 失败: " + e.getMessage());
        }
    }

    private void createTables() throws SQLException {
        try (Statement stmt = connection.createStatement()) {
            stmt.executeUpdate(
                "CREATE TABLE IF NOT EXISTS guilds (" +
                "  name TEXT PRIMARY KEY," +
                "  prefix TEXT DEFAULT ''," +
                "  description TEXT DEFAULT ''," +
                "  leader TEXT NOT NULL" +
                ")");
            stmt.executeUpdate(
                "CREATE TABLE IF NOT EXISTS members (" +
                "  player_name TEXT PRIMARY KEY," +
                "  guild_name TEXT NOT NULL," +
                "  rank TEXT NOT NULL," +
                "  join_time INTEGER DEFAULT 0," +
                "  contribution INTEGER DEFAULT 0," +
                "  FOREIGN KEY (guild_name) REFERENCES guilds(name) ON DELETE CASCADE" +
                ")");
            stmt.executeUpdate(
                "CREATE TABLE IF NOT EXISTS invites (" +
                "  player_name TEXT NOT NULL," +
                "  guild_name TEXT NOT NULL," +
                "  PRIMARY KEY (player_name, guild_name)" +
                ")");
        }
    }

    public synchronized void saveGuild(Guild guild) {
        try (PreparedStatement ps = connection.prepareStatement(
                "INSERT OR REPLACE INTO guilds (name, prefix, description, leader) VALUES (?, ?, ?, ?)")) {
            ps.setString(1, guild.name);
            ps.setString(2, guild.prefix);
            ps.setString(3, guild.description);
            ps.setString(4, guild.leader);
            ps.executeUpdate();
        } catch (SQLException e) {
            plugin.getLogger().severe("保存公会失败: " + e.getMessage());
        }
    }

    public synchronized void saveMember(String guildName, GuildMember member) {
        try (PreparedStatement ps = connection.prepareStatement(
                "INSERT OR REPLACE INTO members (player_name, guild_name, rank, join_time, contribution) VALUES (?, ?, ?, ?, ?)")) {
            ps.setString(1, member.name);
            ps.setString(2, guildName);
            ps.setString(3, member.rank.name());
            ps.setLong(4, member.joinTime);
            ps.setInt(5, member.contribution);
            ps.executeUpdate();
        } catch (SQLException e) {
            plugin.getLogger().severe("保存成员失败: " + e.getMessage());
        }
    }

    public synchronized void saveAllMembers(Guild guild) {
        try (PreparedStatement ps = connection.prepareStatement(
                "INSERT OR REPLACE INTO members (player_name, guild_name, rank, join_time, contribution) VALUES (?, ?, ?, ?, ?)")) {
            for (Map.Entry<String, GuildMember> e : guild.members.entrySet()) {
                GuildMember m = e.getValue();
                ps.setString(1, m.name);
                ps.setString(2, guild.name);
                ps.setString(3, m.rank.name());
                ps.setLong(4, m.joinTime);
                ps.setInt(5, m.contribution);
                ps.addBatch();
            }
            ps.executeBatch();
        } catch (SQLException e) {
            plugin.getLogger().severe("批量保存成员失败: " + e.getMessage());
        }
    }

    public synchronized void saveAll(Collection<Guild> guildList) {
        try {
            connection.setAutoCommit(false);
            for (Guild guild : guildList) {
                saveGuild(guild);
                saveAllMembers(guild);
                saveInvites(guild);
            }
            connection.commit();
            connection.setAutoCommit(true);
        } catch (SQLException e) {
            try { connection.rollback(); } catch (SQLException ignored) {}
            plugin.getLogger().severe("批量保存失败: " + e.getMessage());
        }
    }

    public synchronized void saveInvites(Guild guild) {
        try (PreparedStatement del = connection.prepareStatement("DELETE FROM invites WHERE guild_name = ?")) {
            del.setString(1, guild.name);
            del.executeUpdate();
        } catch (SQLException e) {
            plugin.getLogger().severe("清除邀请失败: " + e.getMessage());
        }
        if (guild.invites.isEmpty()) return;
        try (PreparedStatement ps = connection.prepareStatement(
                "INSERT OR REPLACE INTO invites (player_name, guild_name) VALUES (?, ?)")) {
            for (String name : guild.invites) {
                ps.setString(1, name);
                ps.setString(2, guild.name);
                ps.addBatch();
            }
            ps.executeBatch();
        } catch (SQLException e) {
            plugin.getLogger().severe("保存邀请失败: " + e.getMessage());
        }
    }

    public synchronized void deleteGuild(String guildName) {
        try (PreparedStatement ps1 = connection.prepareStatement("DELETE FROM members WHERE guild_name = ?");
             PreparedStatement ps2 = connection.prepareStatement("DELETE FROM invites WHERE guild_name = ?");
             PreparedStatement ps3 = connection.prepareStatement("DELETE FROM guilds WHERE name = ?")) {
            connection.setAutoCommit(false);
            ps1.setString(1, guildName);
            ps1.executeUpdate();
            ps2.setString(1, guildName);
            ps2.executeUpdate();
            ps3.setString(1, guildName);
            ps3.executeUpdate();
            connection.commit();
            connection.setAutoCommit(true);
        } catch (SQLException e) {
            try { connection.rollback(); } catch (SQLException ignored) {}
            plugin.getLogger().severe("删除公会失败: " + e.getMessage());
        }
    }

    public synchronized void deleteMember(String playerName) {
        try (PreparedStatement ps = connection.prepareStatement("DELETE FROM members WHERE player_name = ?")) {
            ps.setString(1, playerName);
            ps.executeUpdate();
        } catch (SQLException e) {
            plugin.getLogger().severe("删除成员失败: " + e.getMessage());
        }
    }

    public synchronized Guild loadGuild(String guildName) {
        try (PreparedStatement ps = connection.prepareStatement("SELECT * FROM guilds WHERE name = ?")) {
            ps.setString(1, guildName);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return loadGuildFromDb(guildName);
            }
        } catch (SQLException e) {
            plugin.getLogger().severe("加载公会失败: " + e.getMessage());
        }
        return null;
    }

    public synchronized Map<String, Guild> loadAllGuilds() {
        Map<String, Guild> result = new ConcurrentHashMap<>();
        try (Statement stmt = connection.createStatement();
             ResultSet rs = stmt.executeQuery("SELECT name FROM guilds")) {
            while (rs.next()) {
                String name = rs.getString("name");
                Guild guild = loadGuildFromDb(name);
                if (guild != null) result.put(name.toLowerCase(), guild);
            }
        } catch (SQLException e) {
            plugin.getLogger().severe("加载所有公会失败: " + e.getMessage());
        }
        return result;
    }

    private Guild loadGuildFromDb(String guildName) throws SQLException {
        Guild guild = null;
        try (PreparedStatement ps = connection.prepareStatement("SELECT * FROM guilds WHERE name = ?")) {
            ps.setString(1, guildName);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    guild = new Guild(guildName, rs.getString("leader"));
                    guild.prefix = rs.getString("prefix");
                    guild.description = rs.getString("description");
                }
            }
        }
        if (guild == null) return null;
        // 加载成员
        try (PreparedStatement ps = connection.prepareStatement("SELECT * FROM members WHERE guild_name = ?")) {
            ps.setString(1, guildName);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    String memberName = rs.getString("player_name");
                    GuildRank rank = GuildRank.valueOf(rs.getString("rank"));
                    GuildMember member = new GuildMember(memberName, rank);
                    member.joinTime = rs.getLong("join_time");
                    member.contribution = rs.getInt("contribution");
                    guild.members.put(memberName, member);
                }
            }
        }
        // 加载邀请
        try (PreparedStatement ps = connection.prepareStatement("SELECT player_name FROM invites WHERE guild_name = ?")) {
            ps.setString(1, guildName);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    guild.invites.add(rs.getString("player_name"));
                }
            }
        }
        return guild;
    }

    public void close() {
        try {
            if (connection != null && !connection.isClosed()) {
                connection.close();
            }
        } catch (SQLException e) {
            plugin.getLogger().warning("关闭数据库失败: " + e.getMessage());
        }
    }
}
