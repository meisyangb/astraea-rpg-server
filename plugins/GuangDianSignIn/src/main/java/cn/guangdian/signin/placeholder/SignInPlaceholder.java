package cn.guangdian.signin.placeholder;

import cn.guangdian.signin.GuangDianSignIn;
import cn.guangdian.signin.api.SignInService;
import cn.guangdian.rpgcore.integration.PlaceholderService;
import org.bukkit.OfflinePlayer;

public class SignInPlaceholder {
    
    private final GuangDianSignIn plugin;
    
    public SignInPlaceholder(GuangDianSignIn plugin) {
        this.plugin = plugin;
    }
    
    public void register() {
        PlaceholderService service = PlaceholderService.getInstance();
        if (service == null) return;
        
        service.register("gdsignin", (player, params) -> {
            if (player == null) return "";
            
            SignInService signInService = plugin.getSignInService();
            if (signInService == null) return "";
            
            switch (params.toLowerCase()) {
                case "consecutive":
                    return String.valueOf(signInService.getConsecutiveDays(player.getUniqueId()));
                case "total":
                    return String.valueOf(signInService.getTotalDays(player.getUniqueId()));
                case "cansign":
                    return signInService.canSignIn(player.getUniqueId()) ? "true" : "false";
                case "lastsignin":
                    return signInService.getLastSignInDate(player.getUniqueId()) != null 
                        ? signInService.getLastSignInDate(player.getUniqueId()).toString() 
                        : "从未签到";
                default:
                    return null;
            }
        });
    }
    
    public void unregister() {
        // PlaceholderService handles cleanup automatically
    }
}
