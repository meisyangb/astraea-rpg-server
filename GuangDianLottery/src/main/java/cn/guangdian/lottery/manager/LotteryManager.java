package cn.guangdian.lottery.manager;

import cn.guangdian.lottery.GuangDianLottery;
import cn.guangdian.lottery.model.LotteryPool;
import cn.guangdian.lottery.model.Prize;
import org.bukkit.entity.Player;

import java.util.concurrent.ThreadLocalRandom;

public class LotteryManager {

    private final GuangDianLottery plugin;
    
    public LotteryManager(GuangDianLottery plugin) {
        this.plugin = plugin;
    }
    
    public Prize performDraw(String poolId) {
        LotteryPool pool = plugin.getPools().get(poolId);
        if (pool == null) return null;
        
        double random = ThreadLocalRandom.current().nextDouble() * pool.getTotalWeight();
        double currentWeight = 0;
        
        for (Prize prize : pool.getPrizes()) {
            currentWeight += prize.getWeight();
            if (random < currentWeight) {
                return prize;
            }
        }
        
        return pool.getPrizes().isEmpty() ? null : pool.getPrizes().get(0);
    }
    
    public double getChance(Prize prize, LotteryPool pool) {
        return (prize.getWeight() / pool.getTotalWeight()) * 100;
    }
}
