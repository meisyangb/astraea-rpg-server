package cn.guangdian.rpgcore.lifecycle;

import cn.guangdian.rpgcore.RPGCore;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

public abstract class AbstractPlayerDataHandler implements PlayerDataHandler {
    
    protected final JavaPlugin plugin;
    protected final RPGCore rpgCore;
    
    public AbstractPlayerDataHandler(JavaPlugin plugin) {
        this.plugin = plugin;
        this.rpgCore = RPGCore.getInstance();
    }
    
    public void register() {
        if (rpgCore == null) {
            plugin.getLogger().warning("[PlayerLifecycle] RPGCore 实例未初始化，无法注册数据处理器: " + getHandlerName());
            return;
        }
        if (rpgCore.getPlayerLifecycle() == null) {
            plugin.getLogger().warning("[PlayerLifecycle] PlayerLifecycle 未初始化，无法注册数据处理器: " + getHandlerName());
            return;
        }
        rpgCore.getPlayerLifecycle().registerHandler(this);
        plugin.getLogger().info("[PlayerLifecycle] 已注册数据处理器: " + getHandlerName());
    }
    
    public void unregister() {
        if (rpgCore == null) {
            return;
        }
        if (rpgCore.getPlayerLifecycle() == null) {
            return;
        }
        rpgCore.getPlayerLifecycle().unregisterHandler(this);
    }
    
    @Override
    public void onLoad(PlayerDataLoadEvent event) {
        onPlayerLoad(event.getPlayer());
    }
    
    @Override
    public void onSave(PlayerDataSaveEvent event) {
        onPlayerSave(event.getPlayer());
    }
    
    protected abstract void onPlayerLoad(Player player);
    
    protected abstract void onPlayerSave(Player player);
    
    @Override
    public boolean shouldLoad(Player player) {
        return true;
    }
    
    @Override
    public boolean shouldSave(Player player) {
        return true;
    }
}
