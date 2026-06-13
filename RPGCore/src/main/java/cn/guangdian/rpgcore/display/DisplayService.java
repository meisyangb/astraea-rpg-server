package cn.guangdian.rpgcore.display;

import net.kyori.adventure.text.Component;
import org.bukkit.entity.Player;

public interface DisplayService {
    
    void updatePrefix(Player player, String prefix);
    
    void updateSuffix(Player player, String suffix);
    
    void updateDisplayName(Player player, String displayName);
    
    void updateTabName(Player player, String tabName);
    
    void updateTitle(Player player, String title);
    
    void refreshAll(Player player);
    
    void clearAll(Player player);
    
    String getPrefix(Player player);
    
    String getSuffix(Player player);
    
    String getDisplayName(Player player);
    
    String getTitle(Player player);
    
    boolean isEnabled();
    
    void setEnabled(boolean enabled);
}
