package cn.guangdian.chain;

import cn.guangdian.chain.adapter.ChainServiceAdapter;
import cn.guangdian.chain.api.ChainService;
import cn.guangdian.chain.config.ChainConfig;
import cn.guangdian.chain.listener.ChainListener;
import cn.guangdian.rpgcore.RPGCore;
import cn.guangdian.rpgcore.plugin.AbstractRPGPlugin;

public class GuangDianChain extends AbstractRPGPlugin {
    
    private ChainServiceAdapter serviceAdapter;
    private ChainConfig chainConfig;
    private ChainListener chainListener;
    
    @Override
    protected void onPluginEnable() {
        this.chainConfig = new ChainConfig(this);
        this.chainConfig.load();
        
        this.serviceAdapter = new ChainServiceAdapter(this);
        
        this.chainListener = new ChainListener(this);
        getServer().getPluginManager().registerEvents(chainListener, this);
        
        getLogger().info(getPluginName() + " 已启动 - 连锁挖矿/砍伐系统已加载");
    }
    
    @Override
    protected void onPluginDisable() {
        if (scheduler != null) {
            scheduler.cancelAllTasks();
        }
        if (serviceAdapter != null) {
            serviceAdapter.unregister();
        }
        getLogger().info(getPluginName() + " 已关闭");
    }
    
    @Override
    protected String getPluginName() {
        return "GuangDianChain";
    }
    
    public ChainService getChainService() {
        return serviceAdapter;
    }
    
    public ChainConfig getChainConfig() {
        return chainConfig;
    }
}
