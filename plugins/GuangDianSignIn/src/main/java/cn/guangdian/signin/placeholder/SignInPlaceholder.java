package cn.guangdian.signin.placeholder;

import cn.guangdian.signin.GuangDianSignIn;
import cn.guangdian.signin.api.SignInService;
import me.clip.placeholderapi.expansion.PlaceholderExpansion;
import org.bukkit.OfflinePlayer;
import org.jetbrains.annotations.NotNull;

public class SignInPlaceholder extends PlaceholderExpansion {
    
    private final GuangDianSignIn plugin;
    
    public SignInPlaceholder(GuangDianSignIn plugin) {
        this.plugin = plugin;
    }
    
    @Override
    public @NotNull String getIdentifier() {
        return "gdsignin";
    }
    
    @Override
    public @NotNull String getAuthor() {
        return "Astraea RPG Team";
    }
    
    @Override
    public @NotNull String getVersion() {
        return "1.0.0";
    }
    
    @Override
    public boolean persist() {
        return true;
    }
    
    @Override
    public String onRequest(OfflinePlayer player, @NotNull String identifier) {
        if (player == null) {
            return "";
        }
        
        SignInService service = plugin.getSignInService();
        if (service == null) {
            return "";
        }
        
        switch (identifier.toLowerCase()) {
            case "consecutive":
                return String.valueOf(service.getConsecutiveDays(player.getUniqueId()));
            case "total":
                return String.valueOf(service.getTotalDays(player.getUniqueId()));
            case "cansign":
                return service.canSignIn(player.getUniqueId()) ? "true" : "false";
            case "lastsignin":
                return service.getLastSignInDate(player.getUniqueId()) != null 
                    ? service.getLastSignInDate(player.getUniqueId()).toString() 
                    : "从未签到";
            default:
                return null;
        }
    }
}
