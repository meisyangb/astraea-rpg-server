package cn.guangdian.signin.adapter;

import cn.guangdian.rpgcore.RPGCore;
import cn.guangdian.rpgcore.message.MiniMessageService;
import cn.guangdian.signin.GuangDianSignIn;
import cn.guangdian.signin.api.SignInService;
import cn.guangdian.signin.config.RewardConfig;
import cn.guangdian.signin.data.PlayerSignInData;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class SignInServiceAdapter implements SignInService {
    
    private final GuangDianSignIn plugin;
    private final Map<UUID, PlayerSignInData> dataCache;
    
    public SignInServiceAdapter(GuangDianSignIn plugin) {
        this.plugin = plugin;
        this.dataCache = plugin.getDataHandler().getDataCache();
        
        RPGCore rpgCore = RPGCore.getInstance();
        if (rpgCore != null) {
            rpgCore.getServiceRegistry().registerService(SignInService.class, this);
        }
    }
    
    public void unregister() {
        RPGCore rpgCore = RPGCore.getInstance();
        if (rpgCore != null) {
            rpgCore.getServiceRegistry().unregisterService(SignInService.class);
        }
    }
    
    @Override
    public boolean canSignIn(UUID playerId) {
        PlayerSignInData data = dataCache.get(playerId);
        // 玩家数据不存在时，允许签到（创建新数据）
        if (data == null) {
            return true;
        }
        
        LocalDate lastSignIn = data.getLastSignInDate();
        LocalDate today = LocalDate.now();
        
        return lastSignIn == null || !lastSignIn.equals(today);
    }
    
    @Override
    public boolean signIn(UUID playerId) {
        Player player = Bukkit.getPlayer(playerId);
        if (player == null) {
            return false;
        }
        
        PlayerSignInData data = dataCache.get(playerId);
        // 如果玩家数据不存在，创建一个新数据
        if (data == null) {
            data = new PlayerSignInData(playerId);
            dataCache.put(playerId, data);
        }
        
        LocalDate today = LocalDate.now();
        LocalDate lastSignIn = data.getLastSignInDate();
        
        if (lastSignIn != null && lastSignIn.equals(today)) {
            return false;
        }
        
        int consecutiveDays = data.getConsecutiveDays();
        if (lastSignIn != null && lastSignIn.plusDays(1).equals(today)) {
            consecutiveDays++;
        } else {
            consecutiveDays = 1;
        }
        
        data.setLastSignInDate(today);
        data.setConsecutiveDays(consecutiveDays);
        data.setTotalDays(data.getTotalDays() + 1);
        
        giveReward(player, consecutiveDays);
        
        player.sendMessage(Component.text("签到成功！")
            .color(NamedTextColor.GREEN)
            .append(Component.text(" 连续签到 "))
            .append(Component.text(consecutiveDays)
                .color(NamedTextColor.GOLD))
            .append(Component.text(" 天")));
        
        return true;
    }
    
    @Override
    public int getConsecutiveDays(UUID playerId) {
        PlayerSignInData data = dataCache.get(playerId);
        return data != null ? data.getConsecutiveDays() : 0;
    }
    
    @Override
    public int getTotalDays(UUID playerId) {
        PlayerSignInData data = dataCache.get(playerId);
        return data != null ? data.getTotalDays() : 0;
    }
    
    @Override
    public LocalDate getLastSignInDate(UUID playerId) {
        PlayerSignInData data = dataCache.get(playerId);
        return data != null ? data.getLastSignInDate() : null;
    }
    
    @Override
    public List<SignInRecord> getSignInHistory(UUID playerId, int limit) {
        return new ArrayList<>();
    }
    
    @Override
    public void resetConsecutiveDays(UUID playerId) {
        PlayerSignInData data = dataCache.get(playerId);
        if (data != null) {
            data.setConsecutiveDays(0);
        }
    }
    
    @Override
    public void giveReward(Player player, int consecutiveDays) {
        RewardConfig reward = plugin.getConfigManager().getReward(consecutiveDays);
        if (reward == null) {
            return;
        }
        
        for (String command : reward.getCommands()) {
            String parsedCommand = command.replace("{player}", player.getName())
                .replace("{days}", String.valueOf(consecutiveDays));
            Bukkit.dispatchCommand(Bukkit.getConsoleSender(), parsedCommand);
        }
        
        if (!reward.getMessage().isEmpty()) {
            MiniMessageService mm = MiniMessageService.getInstance();
            player.sendMessage(mm.colorize(reward.getMessage()));
        }
    }
}
