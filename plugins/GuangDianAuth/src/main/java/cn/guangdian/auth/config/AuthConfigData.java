package cn.guangdian.auth.config;

import org.spongepowered.configurate.objectmapping.ConfigSerializable;
import org.spongepowered.configurate.objectmapping.meta.Setting;

/**
 * Auth 配置数据类
 * 使用 Configurate 进行配置管理
 */
@ConfigSerializable
public class AuthConfigData {

    @Setting("password")
    private PasswordConfig password = new PasswordConfig();

    @Setting("session")
    private SessionConfig session = new SessionConfig();

    @Setting("login")
    private LoginConfig login = new LoginConfig();

    @Setting("register")
    private RegisterConfig register = new RegisterConfig();

    @Setting("restrictions")
    private RestrictionsConfig restrictions = new RestrictionsConfig();

    // Getters
    public PasswordConfig getPassword() { return password; }
    public SessionConfig getSession() { return session; }
    public LoginConfig getLogin() { return login; }
    public RegisterConfig getRegister() { return register; }
    public RestrictionsConfig getRestrictions() { return restrictions; }

    @ConfigSerializable
    public static class PasswordConfig {
        @Setting("min-length")
        private int minLength = 6;

        @Setting("max-length")
        private int maxLength = 32;

        public int getMinLength() { return minLength; }
        public int getMaxLength() { return maxLength; }
    }

    @ConfigSerializable
    public static class SessionConfig {
        @Setting("timeout-minutes")
        private int timeoutMinutes = 43200; // 30 days

        public int getTimeoutMinutes() { return timeoutMinutes; }
    }

    @ConfigSerializable
    public static class LoginConfig {
        @Setting("timeout-seconds")
        private int timeoutSeconds = 120;

        @Setting("max-attempts")
        private int maxAttempts = 5;

        @Setting("kick-on-wrong-password")
        private boolean kickOnWrongPassword = false;

        public int getTimeoutSeconds() { return timeoutSeconds; }
        public int getMaxAttempts() { return maxAttempts; }
        public boolean isKickOnWrongPassword() { return kickOnWrongPassword; }
    }

    @ConfigSerializable
    public static class RegisterConfig {
        @Setting("force-login-after")
        private boolean forceLoginAfter = true;

        public boolean isForceLoginAfter() { return forceLoginAfter; }
    }

    @ConfigSerializable
    public static class RestrictionsConfig {
        @Setting("allow-movement")
        private boolean allowMovement = false;

        @Setting("allow-chat")
        private boolean allowChat = false;

        public boolean isAllowMovement() { return allowMovement; }
        public boolean isAllowChat() { return allowChat; }
    }
}
