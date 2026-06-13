package cn.guangdian.auth.data;

import java.util.UUID;

public class PlayerAuthData {

    private final String playerName;
    private final UUID uuid;
    private String passwordHash;
    private String salt;
    private long registerDate;
    private String registerIp;
    private long lastLogin;
    private String lastIp;

    public PlayerAuthData(String playerName, UUID uuid) {
        this.playerName = playerName;
        this.uuid = uuid;
        this.registerDate = System.currentTimeMillis();
    }

    public String getPlayerName() { return playerName; }
    public UUID getUuid() { return uuid; }
    public String getPasswordHash() { return passwordHash; }
    public String getSalt() { return salt; }
    public long getRegisterDate() { return registerDate; }
    public String getRegisterIp() { return registerIp; }
    public long getLastLogin() { return lastLogin; }
    public String getLastIp() { return lastIp; }

    public void setPasswordHash(String passwordHash) { this.passwordHash = passwordHash; }
    public void setSalt(String salt) { this.salt = salt; }
    public void setRegisterDate(long registerDate) { this.registerDate = registerDate; }
    public void setRegisterIp(String registerIp) { this.registerIp = registerIp; }
    public void setLastLogin(long lastLogin) { this.lastLogin = lastLogin; }
    public void setLastIp(String lastIp) { this.lastIp = lastIp; }
}
