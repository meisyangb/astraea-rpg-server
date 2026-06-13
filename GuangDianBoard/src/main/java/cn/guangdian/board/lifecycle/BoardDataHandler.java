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
            plugin.getBoardToggleState().putIfAbsent(player.getUniqueId(), 
                plugin.getConfig().getBoolean("advanced.show-by-default", true));
        }
        
        if (plugin.shouldShowBoard(player)) {
            plugin.createBoard(player);
        }
    }
    
    @Override
    protected void onPlayerSave(Player player) {
        // 完全清理玩家面板数据，确保重新上线时能正常显示
        plugin.removeBoard(player);
        
        // boardToggleState 的保留由主配置决定，不在此处强制清除
        // 这样可以确保玩家重新上线时能恢复之前的显示状态
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
