package cn.guangdian.mcp.config;

import cn.guangdian.mcp.GuangDianMCP;
import org.bukkit.configuration.file.FileConfiguration;

import java.util.ArrayList;
import java.util.List;

public class MCPConfig {
    
    private final GuangDianMCP plugin;
    
    private int port;
    private String host;
    private boolean serverEnabled;
    
    private List<String> tokens;
    private boolean ipWhitelistEnabled;
    private List<String> ipWhitelist;
    private int requestTimeout;
    
    private boolean allowCommands;
    private boolean allowConfigRead;
    private boolean allowConfigWrite;
    private boolean allowPlayerManagement;
    private boolean allowWorldManagement;
    private boolean allowPluginManagement;
    private boolean allowLogRead;
    
    private boolean logRequests;
    private boolean logSensitiveOperations;
    private int retentionDays;
    
    public MCPConfig(GuangDianMCP plugin) {
        this.plugin = plugin;
    }
    
    public void load() {
        plugin.reloadConfig();
        FileConfiguration config = plugin.getConfig();
        
        port = config.getInt("server.port", 8080);
        host = config.getString("server.host", "127.0.0.1");
        serverEnabled = config.getBoolean("server.enabled", true);
        
        tokens = config.getStringList("security.tokens");
        if (tokens.isEmpty()) {
            tokens.add("default-token-please-change");
        }
        ipWhitelistEnabled = config.getBoolean("security.ip-whitelist-enabled", false);
        ipWhitelist = config.getStringList("security.ip-whitelist");
        requestTimeout = config.getInt("security.request-timeout", 30000);
        
        allowCommands = config.getBoolean("features.allow-commands", true);
        allowConfigRead = config.getBoolean("features.allow-config-read", true);
        allowConfigWrite = config.getBoolean("features.allow-config-write", false);
        allowPlayerManagement = config.getBoolean("features.allow-player-management", true);
        allowWorldManagement = config.getBoolean("features.allow-world-management", true);
        allowPluginManagement = config.getBoolean("features.allow-plugin-management", true);
        allowLogRead = config.getBoolean("features.allow-log-read", true);
        
        logRequests = config.getBoolean("logging.log-requests", true);
        logSensitiveOperations = config.getBoolean("logging.log-sensitive-operations", true);
        retentionDays = config.getInt("logging.retention-days", 7);
    }
    
    public boolean validateToken(String token) {
        return tokens.contains(token);
    }
    
    public boolean isIpAllowed(String ip) {
        if (!ipWhitelistEnabled) {
            return true;
        }
        return ipWhitelist.contains(ip);
    }
    
    public int getPort() { return port; }
    public String getHost() { return host; }
    public boolean isServerEnabled() { return serverEnabled; }
    public List<String> getTokens() { return tokens; }
    public boolean isIpWhitelistEnabled() { return ipWhitelistEnabled; }
    public List<String> getIpWhitelist() { return ipWhitelist; }
    public int getRequestTimeout() { return requestTimeout; }
    public boolean isAllowCommands() { return allowCommands; }
    public boolean isAllowConfigRead() { return allowConfigRead; }
    public boolean isAllowConfigWrite() { return allowConfigWrite; }
    public boolean isAllowPlayerManagement() { return allowPlayerManagement; }
    public boolean isAllowWorldManagement() { return allowWorldManagement; }
    public boolean isAllowPluginManagement() { return allowPluginManagement; }
    public boolean isAllowLogRead() { return allowLogRead; }
    public boolean isLogRequests() { return logRequests; }
    public boolean isLogSensitiveOperations() { return logSensitiveOperations; }
    public int getRetentionDays() { return retentionDays; }
    
    public void addToken(String token) {
        tokens.add(token);
        plugin.getConfig().set("security.tokens", tokens);
        plugin.saveConfig();
    }
    
    public void removeToken(String token) {
        tokens.remove(token);
        plugin.getConfig().set("security.tokens", tokens);
        plugin.saveConfig();
    }
}
