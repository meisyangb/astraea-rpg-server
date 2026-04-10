package cn.guangdian.rpgcore.display;

import cn.guangdian.rpgcore.RPGCore;
import cn.guangdian.rpgcore.integration.ExternalServiceIntegration;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.scoreboard.Scoreboard;
import org.bukkit.scoreboard.Team;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Logger;

public class DisplayServiceImpl implements DisplayService {
    
    private final RPGCore plugin;
    private final Logger logger;
    private final ExternalServiceIntegration externalServices;
    private final Map<UUID, PlayerDisplay> displays = new ConcurrentHashMap<>();
    private final Scoreboard mainScoreboard;
    private boolean enabled = true;
    
    public DisplayServiceImpl(RPGCore plugin) {
        this.plugin = plugin;
        this.logger = plugin.getLogger();
        this.externalServices = plugin.getExternalServices();
        this.mainScoreboard = Bukkit.getScoreboardManager().getMainScoreboard();
    }
    
    @Override
    public void updatePrefix(Player player, String prefix) {
        PlayerDisplay display = getOrCreateDisplay(player);
        display.prefix = prefix;
        applyDisplay(player, display);
    }
    
    @Override
    public void updateSuffix(Player player, String suffix) {
        PlayerDisplay display = getOrCreateDisplay(player);
        display.suffix = suffix;
        applyDisplay(player, display);
    }
    
    @Override
    public void updateDisplayName(Player player, String displayName) {
        PlayerDisplay display = getOrCreateDisplay(player);
        display.displayName = displayName;
        applyDisplay(player, display);
    }
    
    @Override
    public void updateTabName(Player player, String tabName) {
        PlayerDisplay display = getOrCreateDisplay(player);
        display.tabName = tabName;
        player.playerListName(LegacyComponentSerializer.legacySection().deserialize(tabName));
    }
    
    @Override
    public void updateTitle(Player player, String title) {
        PlayerDisplay display = getOrCreateDisplay(player);
        display.title = title;
    }
    
    @Override
    public void refreshAll(Player player) {
        PlayerDisplay display = displays.get(player.getUniqueId());
        if (display != null) {
            applyDisplay(player, display);
            if (display.tabName != null) {
                player.playerListName(LegacyComponentSerializer.legacySection().deserialize(display.tabName));
            }
        }
    }
    
    @Override
    public void clearAll(Player player) {
        displays.remove(player.getUniqueId());
        Team team = mainScoreboard.getTeam(getTeamName(player));
        if (team != null) {
            team.unregister();
        }
        player.playerListName(Component.text(player.getName()));
    }
    
    @Override
    public String getPrefix(Player player) {
        PlayerDisplay display = displays.get(player.getUniqueId());
        if (display != null && display.prefix != null) {
            return display.prefix;
        }
        if (externalServices != null) {
            return externalServices.getPlayerPrefix(player);
        }
        return "";
    }
    
    @Override
    public String getSuffix(Player player) {
        PlayerDisplay display = displays.get(player.getUniqueId());
        if (display != null && display.suffix != null) {
            return display.suffix;
        }
        if (externalServices != null) {
            return externalServices.getPlayerSuffix(player);
        }
        return "";
    }
    
    @Override
    public String getDisplayName(Player player) {
        PlayerDisplay display = displays.get(player.getUniqueId());
        if (display != null && display.displayName != null) {
            return display.displayName;
        }
        return player.getName();
    }
    
    @Override
    public String getTitle(Player player) {
        PlayerDisplay display = displays.get(player.getUniqueId());
        return display != null ? display.title : "";
    }
    
    @Override
    public boolean isEnabled() {
        return enabled;
    }
    
    @Override
    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }
    
    private PlayerDisplay getOrCreateDisplay(Player player) {
        return displays.computeIfAbsent(player.getUniqueId(), k -> new PlayerDisplay());
    }
    
    private void applyDisplay(Player player, PlayerDisplay display) {
        if (!enabled) return;
        
        String teamName = getTeamName(player);
        Team team = mainScoreboard.getTeam(teamName);
        if (team == null) {
            team = mainScoreboard.registerNewTeam(teamName);
        }
        
        team.addEntry(player.getName());
        
        if (display.prefix != null) {
            team.prefix(LegacyComponentSerializer.legacySection().deserialize(display.prefix));
        }
        if (display.suffix != null) {
            team.suffix(LegacyComponentSerializer.legacySection().deserialize(display.suffix));
        }
    }
    
    private String getTeamName(Player player) {
        String name = "rpg_" + player.getName();
        if (name.length() > 16) {
            name = name.substring(0, 16);
        }
        return name;
    }
    
    private static class PlayerDisplay {
        String prefix;
        String suffix;
        String displayName;
        String tabName;
        String title;
    }
}
