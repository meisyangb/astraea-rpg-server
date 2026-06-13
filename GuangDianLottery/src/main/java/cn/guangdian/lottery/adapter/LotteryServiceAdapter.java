package cn.guangdian.lottery.adapter;

import cn.guangdian.lottery.GuangDianLottery;
import cn.guangdian.rpgcore.RPGCore;

public class LotteryServiceAdapter {

    private final GuangDianLottery plugin;
    private boolean usingRPGCore = false;
    
    public LotteryServiceAdapter(GuangDianLottery plugin) {
        this.plugin = plugin;
        
        RPGCore rpgCore = RPGCore.getInstance();
        if (rpgCore != null) {
            usingRPGCore = true;
            plugin.getLogger().info("LotteryServiceAdapter 已连接到 RPGCore");
        }
    }
    
    public boolean isUsingRPGCore() {
        return usingRPGCore;
    }
    
    public void unregister() {
        RPGCore rpgCore = RPGCore.getInstance();
        if (rpgCore != null) {
            plugin.getLogger().info("LotteryServiceAdapter 已从 RPGCore 注销");
        }
    }
}
