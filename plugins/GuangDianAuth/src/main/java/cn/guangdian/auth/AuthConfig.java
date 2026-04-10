package cn.guangdian.auth;

import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;

public class AuthConfig {

    private final File file;
    private FileConfiguration config;

    private int minPasswordLength = 6;
    private int maxPasswordLength = 32;
    private int sessionTimeout = 43200;
    private int loginTimeout = 120;
    private int maxLoginAttempts = 5;
    private boolean kickOnWrongPassword = false;
    private boolean forceLoginAfterRegister = true;
    private boolean allowMovementBeforeLogin = false;
    private boolean allowChatBeforeLogin = false;

    public AuthConfig(File file) {
        this.file = file;
    }

    public void load() {
        if (!file.exists()) {
            createDefault();
        }
        config = YamlConfiguration.loadConfiguration(file);
        readValues();
    }

    private void createDefault() {
        file.getParentFile().mkdirs();
        config = new YamlConfiguration();
        
        config.set("password.min-length", 6);
        config.set("password.max-length", 32);
        config.set("session.timeout-minutes", 43200);
        config.set("login.timeout-seconds", 120);
        config.set("login.max-attempts", 5);
        config.set("login.kick-on-wrong-password", false);
        config.set("register.force-login-after", true);
        config.set("restrictions.allow-movement", false);
        config.set("restrictions.allow-chat", false);
        
        try {
            config.save(file);
        } catch (IOException e) {
            // Ignore
        }
    }

    private void readValues() {
        minPasswordLength = config.getInt("password.min-length", 6);
        maxPasswordLength = config.getInt("password.max-length", 32);
        sessionTimeout = config.getInt("session.timeout-minutes", 43200);
        loginTimeout = config.getInt("login.timeout-seconds", 120);
        maxLoginAttempts = config.getInt("login.max-attempts", 5);
        kickOnWrongPassword = config.getBoolean("login.kick-on-wrong-password", false);
        forceLoginAfterRegister = config.getBoolean("register.force-login-after", true);
        allowMovementBeforeLogin = config.getBoolean("restrictions.allow-movement", false);
        allowChatBeforeLogin = config.getBoolean("restrictions.allow-chat", false);
    }

    public int getMinPasswordLength() { return minPasswordLength; }
    public int getMaxPasswordLength() { return maxPasswordLength; }
    public int getSessionTimeout() { return sessionTimeout; }
    public int getLoginTimeout() { return loginTimeout; }
    public int getMaxLoginAttempts() { return maxLoginAttempts; }
    public boolean isKickOnWrongPassword() { return kickOnWrongPassword; }
    public boolean isForceLoginAfterRegister() { return forceLoginAfterRegister; }
    public boolean isAllowMovementBeforeLogin() { return allowMovementBeforeLogin; }
    public boolean isAllowChatBeforeLogin() { return allowChatBeforeLogin; }
}
