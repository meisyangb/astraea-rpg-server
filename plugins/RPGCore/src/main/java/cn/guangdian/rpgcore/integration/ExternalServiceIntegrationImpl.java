package cn.guangdian.rpgcore.integration;

import net.luckperms.api.LuckPerms;
import net.luckperms.api.model.user.User;
import net.luckperms.api.node.NodeType;
import net.luckperms.api.node.types.PrefixNode;
import net.luckperms.api.node.types.SuffixNode;
import net.milkbowl.vault.economy.Economy;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.RegisteredServiceProvider;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;

public class ExternalServiceIntegrationImpl implements ExternalServiceIntegration {
    
    private final JavaPlugin plugin;
    private final Logger logger;
    
    private LuckPerms luckPerms;
    private Economy economy;
    private boolean placeholderAPIEnabled;
    
    private final ConcurrentHashMap<java.util.UUID, CacheEntry<User>> userCache = new ConcurrentHashMap<>();
    private final long cacheTtlMs = 5 * 60 * 1000; // 5分钟过期
    private final ScheduledExecutorService cleanupExecutor;

    public ExternalServiceIntegrationImpl(JavaPlugin plugin) {
        this.plugin = plugin;
        this.logger = plugin.getLogger();
        this.cleanupExecutor = Executors.newSingleThreadScheduledExecutor(r -> {
            Thread thread = new Thread(r, "RPGCore-UserCache-Cleanup");
            thread.setDaemon(true);
            return thread;
        });
        startCleanupTask();
        hookAll();
    }

    private void startCleanupTask() {
        cleanupExecutor.scheduleAtFixedRate(() -> {
            long now = System.currentTimeMillis();
            int removed = 0;
            for (var entry : userCache.entrySet()) {
                if (entry.getValue().isExpired(now, cacheTtlMs)) {
                    userCache.remove(entry.getKey());
                    removed++;
                }
            }
            if (removed > 0) {
                logger.fine("[ExternalService] 清理 LuckPerms 用户缓存: " + removed + " 个");
            }
        }, cacheTtlMs, cacheTtlMs, TimeUnit.MILLISECONDS);
    }

    private record CacheEntry<T>(T value, long timestamp) {
        boolean isExpired(long now, long ttlMs) {
            return (now - timestamp) > ttlMs;
        }
    }
    
    private void hookAll() {
        hookLuckPerms();
        hookVault();
        hookPlaceholderAPI();
    }
    
    private void hookLuckPerms() {
        try {
            if (Bukkit.getPluginManager().isPluginEnabled("LuckPerms")) {
                RegisteredServiceProvider<LuckPerms> provider = Bukkit.getServicesManager().getRegistration(LuckPerms.class);
                if (provider != null) {
                    luckPerms = provider.getProvider();
                    logger.info("[ExternalService] LuckPerms hooked successfully");
                } else {
                    logger.warning("[ExternalService] LuckPerms detected but service provider not found");
                }
            }
        } catch (Exception e) {
            logger.warning("[ExternalService] Failed to hook LuckPerms: " + e.getMessage());
        }
    }
    
    private void hookVault() {
        try {
            if (Bukkit.getPluginManager().isPluginEnabled("Vault")) {
                RegisteredServiceProvider<Economy> rsp = Bukkit.getServicesManager().getRegistration(Economy.class);
                if (rsp != null) {
                    economy = rsp.getProvider();
                    logger.info("[ExternalService] Vault Economy hooked successfully");
                }
            }
        } catch (Exception e) {
            logger.warning("[ExternalService] Failed to hook Vault: " + e.getMessage());
        }
    }
    
    private void hookPlaceholderAPI() {
        placeholderAPIEnabled = Bukkit.getPluginManager().isPluginEnabled("PlaceholderAPI");
        if (placeholderAPIEnabled) {
            logger.info("[ExternalService] PlaceholderAPI detected");
        }
    }
    
    public void refreshPlaceholderAPI() {
        if (!placeholderAPIEnabled) {
            hookPlaceholderAPI();
            if (placeholderAPIEnabled) {
                logger.info("[ExternalService] PlaceholderAPI refreshed and enabled");
            }
        }
    }
    
    @Override
    public Optional<LuckPerms> getLuckPerms() {
        return Optional.ofNullable(luckPerms);
    }
    
    @Override
    public Optional<User> getLuckPermsUser(Player player) {
        if (luckPerms == null) return Optional.empty();

        java.util.UUID uuid = player.getUniqueId();
        CacheEntry<User> entry = userCache.get(uuid);
        long now = System.currentTimeMillis();

        if (entry != null && !entry.isExpired(now, cacheTtlMs)) {
            return Optional.ofNullable(entry.value());
        }

        User user = luckPerms.getUserManager().getUser(uuid);
        if (user != null) {
            userCache.put(uuid, new CacheEntry<>(user, now));
        }
        return Optional.ofNullable(user);
    }
    
    @Override
    public String getPlayerPrefix(Player player) {
        if (luckPerms == null) return "";
        
        User user = getLuckPermsUser(player).orElse(null);
        if (user == null) return "";
        
        String prefix = user.getCachedData().getMetaData().getPrefix();
        return prefix != null ? prefix : "";
    }
    
    @Override
    public String getPlayerSuffix(Player player) {
        if (luckPerms == null) return "";
        
        User user = getLuckPermsUser(player).orElse(null);
        if (user == null) return "";
        
        String suffix = user.getCachedData().getMetaData().getSuffix();
        return suffix != null ? suffix : "";
    }
    
    @Override
    public String getPlayerPrimaryGroup(Player player) {
        if (luckPerms == null) return "default";
        
        User user = getLuckPermsUser(player).orElse(null);
        if (user == null) return "default";
        
        return user.getPrimaryGroup();
    }
    
    @Override
    public Optional<Object> getVaultEconomy() {
        return Optional.ofNullable(economy);
    }
    
    @Override
    public boolean hasVaultEconomy() {
        return economy != null;
    }
    
    @Override
    public double getBalance(Player player) {
        if (economy == null) return 0.0;
        return economy.getBalance(player);
    }
    
    @Override
    public boolean deposit(Player player, double amount) {
        if (economy == null) return false;
        return economy.depositPlayer(player, amount).transactionSuccess();
    }
    
    @Override
    public boolean withdraw(Player player, double amount) {
        if (economy == null) return false;
        return economy.withdrawPlayer(player, amount).transactionSuccess();
    }
    
    @Override
    public String parsePlaceholders(Player player, String text) {
        if (text == null) return "";
        
        if (!placeholderAPIEnabled) {
            refreshPlaceholderAPI();
        }
        
        if (!placeholderAPIEnabled) return text;
        
        try {
            return me.clip.placeholderapi.PlaceholderAPI.setPlaceholders(player, text);
        } catch (Exception e) {
            return text;
        }
    }
    
    @Override
    public boolean isPlaceholderAPIEnabled() {
        return placeholderAPIEnabled;
    }
    
    @Override
    public boolean isLuckPermsEnabled() {
        return luckPerms != null;
    }
    
    @Override
    public boolean isVaultEnabled() {
        return economy != null;
    }
    
    @Override
    public String getExternalServiceStatus() {
        StringBuilder sb = new StringBuilder();
        sb.append("LuckPerms: ").append(luckPerms != null ? "✓" : "✗").append(", ");
        sb.append("Vault: ").append(economy != null ? "✓" : "✗").append(", ");
        sb.append("PlaceholderAPI: ").append(placeholderAPIEnabled ? "✓" : "✗");
        return sb.toString();
    }
    
    public void clearUserCache(java.util.UUID playerId) {
        userCache.remove(playerId);
    }
    
    public void clearAllUserCache() {
        userCache.clear();
    }
    
    public void shutdown() {
        clearAllUserCache();
        cleanupExecutor.shutdown();
        try {
            if (!cleanupExecutor.awaitTermination(5, TimeUnit.SECONDS)) {
                cleanupExecutor.shutdownNow();
            }
        } catch (InterruptedException e) {
            cleanupExecutor.shutdownNow();
            Thread.currentThread().interrupt();
        }
        luckPerms = null;
        economy = null;
    }
}
