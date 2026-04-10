package cn.guangdian.board.lifecycle;

import cn.guangdian.board.GuangDianBoard;
import cn.guangdian.rpgcore.lifecycle.AbstractPlayerDataHandler;
import org.bukkit.entity.Player;

public class BoardDataHandler extends AbstractPlayerDataHandler {
    
    private final GuangDianBoard plugin;
    
    public BoardDataHandler(GuangDianBoard plugin) {
        super(plugin);
        this.plugin = plugin;
    }
    
    @Override
    protected void onPlayerLoad(Player player) {
        if (plugin.getConfig().getBoolean("advanced.remember-toggle-state", true)) {
            if (!plugin.getBoardToggleState().containsKey(player.getUniqueId())) {
                plugin.getBoardToggleState().put(player.getUniqueId(), 
                    plugin.getConfig().getBoolean("advanced.show-by-default", true));
            }
        }
        
        if (plugin.shouldShowBoard(player)) {
            plugin.createBoard(player);
        }
    }
    
    @Override
    protected void onPlayerSave(Player player) {
        plugin.getPlayerBoards().remove(player.getUniqueId());
        plugin.getBoardToggleState().remove(player.getUniqueId());
        plugin.getBoardCache().remove(player.getUniqueId());
    }
    
    @Override
    public int getPriority() {
        return 400;
    }
    
    @Override
    public String getHandlerName() {
        return "Board";
    }
}
