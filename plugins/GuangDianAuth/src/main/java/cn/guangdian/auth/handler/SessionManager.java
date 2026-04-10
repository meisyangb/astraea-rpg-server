package cn.guangdian.auth.handler;

import cn.guangdian.auth.GuangDianAuth;
import org.bukkit.entity.Player;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class SessionManager {

    private final GuangDianAuth plugin;
    private final Map<UUID, Session> sessions = new ConcurrentHashMap<>();
    private final Map<UUID, Integer> loginAttempts = new ConcurrentHashMap<>();

    public SessionManager(GuangDianAuth plugin) {
        this.plugin = plugin;
    }

    public void createSession(Player player) {
        Session session = new Session(
            player.getUniqueId(),
            player.getName(),
            player.getAddress().getAddress().getHostAddress(),
            System.currentTimeMillis()
        );
        sessions.put(player.getUniqueId(), session);
    }

    public void removeSession(UUID playerId) {
        sessions.remove(playerId);
        loginAttempts.remove(playerId);
    }

    public boolean isLoggedIn(UUID playerId) {
        Session session = sessions.get(playerId);
        if (session == null) return false;
        
        if (isSessionExpired(session)) {
            sessions.remove(playerId);
            return false;
        }
        
        return session.isLoggedIn();
    }

    public void setLoggedIn(UUID playerId, boolean loggedIn) {
        Session session = sessions.get(playerId);
        if (session != null) {
            session.setLoggedIn(loggedIn);
            if (loggedIn) {
                session.setLoginTime(System.currentTimeMillis());
                loginAttempts.remove(playerId);
            }
        }
    }

    public Session getSession(UUID playerId) {
        return sessions.get(playerId);
    }

    public void addLoginAttempt(UUID playerId) {
        int attempts = loginAttempts.getOrDefault(playerId, 0) + 1;
        loginAttempts.put(playerId, attempts);
    }

    public int getLoginAttempts(UUID playerId) {
        return loginAttempts.getOrDefault(playerId, 0);
    }

    public void resetLoginAttempts(UUID playerId) {
        loginAttempts.remove(playerId);
    }

    public boolean hasExceededMaxAttempts(UUID playerId) {
        int max = plugin.getAuthConfig().getMaxLoginAttempts();
        return getLoginAttempts(playerId) >= max;
    }

    private boolean isSessionExpired(Session session) {
        if (!session.isLoggedIn()) return false;
        
        long timeout = plugin.getAuthConfig().getSessionTimeout() * 60 * 1000L;
        long elapsed = System.currentTimeMillis() - session.getLoginTime();
        return elapsed > timeout;
    }

    public void saveAll() {
        // Sessions are transient, no persistence needed
    }

    public static class Session {
        private final UUID playerId;
        private final String playerName;
        private final String ip;
        private final long joinTime;
        private long loginTime;
        private boolean loggedIn;

        public Session(UUID playerId, String playerName, String ip, long joinTime) {
            this.playerId = playerId;
            this.playerName = playerName;
            this.ip = ip;
            this.joinTime = joinTime;
            this.loggedIn = false;
        }

        public UUID getPlayerId() { return playerId; }
        public String getPlayerName() { return playerName; }
        public String getIp() { return ip; }
        public long getJoinTime() { return joinTime; }
        public long getLoginTime() { return loginTime; }
        public boolean isLoggedIn() { return loggedIn; }

        public void setLoginTime(long loginTime) { this.loginTime = loginTime; }
        public void setLoggedIn(boolean loggedIn) { this.loggedIn = loggedIn; }
    }
}
