package cn.guangdian.rpgcore.integration;

import net.luckperms.api.LuckPerms;
import net.luckperms.api.model.user.User;
import org.bukkit.entity.Player;

import java.util.Optional;

public interface ExternalServiceIntegration {
    
    Optional<LuckPerms> getLuckPerms();
    
    Optional<User> getLuckPermsUser(Player player);
    
    String getPlayerPrefix(Player player);
    
    String getPlayerSuffix(Player player);
    
    String getPlayerPrimaryGroup(Player player);
    
    Optional<Object> getVaultEconomy();
    
    boolean hasVaultEconomy();
    
    double getBalance(Player player);
    
    boolean deposit(Player player, double amount);
    
    boolean withdraw(Player player, double amount);
    
    String parsePlaceholders(Player player, String text);
    
    boolean isPlaceholderAPIEnabled();
    
    boolean isLuckPermsEnabled();
    
    boolean isVaultEnabled();
    
    String getExternalServiceStatus();
}
