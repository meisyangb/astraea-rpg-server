package cn.guangdian.forge.adapter;

import cn.guangdian.forge.GuangDianForge;
import cn.guangdian.forge.model.ForgeRecipe;
import cn.guangdian.forge.model.PlayerForgeData;
import cn.guangdian.classsystem.event.PlayerLevelUpEvent;
import cn.guangdian.rpgcore.RPGCore;
import cn.guangdian.rpgcore.api.AsyncExecutor;
import cn.guangdian.rpgcore.api.ServiceRegistry;
import cn.guangdian.rpgcore.service.api.ForgeService;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

/**
 * 锻造服务适配器
 * 
 * <p>连接 GuangDianForge 实现与 RPGCore 服务接口，
 * 支持服务注册、事件发布、异步执行等功能。</p>
 * 
 * @author GuangDian
 * @since 1.0.0
 */
public class ForgeServiceAdapter implements ForgeService {

    private final GuangDianForge plugin;
    private final boolean useRPGCore;
    private AsyncExecutor asyncExecutor;

    public ForgeServiceAdapter(GuangDianForge plugin) {
        this.plugin = plugin;
        this.useRPGCore = Bukkit.getPluginManager().isPluginEnabled("RPGCore");

        if (useRPGCore) {
            try {
                RPGCore rpgCore = RPGCore.getInstance();
                ServiceRegistry registry = rpgCore.getServiceRegistry();
                this.asyncExecutor = rpgCore.getAsyncExecutor();

                registry.registerService(ForgeService.class, this);
                plugin.getLogger().info("已注册到 RPGCore: ForgeService");
            } catch (Exception e) {
                plugin.getLogger().warning("注册到 RPGCore 失败: " + e.getMessage());
            }
        }
    }

    @Override
    public int getForgeLevel(UUID playerId) {
        PlayerForgeData data = plugin.getPlayerDataManager().get(playerId);
        return data != null ? data.getForgeLevel() : 1;
    }

    @Override
    public long getForgeExp(UUID playerId) {
        PlayerForgeData data = plugin.getPlayerDataManager().get(playerId);
        return data != null ? data.getForgeExp() : 0;
    }

    @Override
    public void addForgeExp(UUID playerId, long amount) {
        PlayerForgeData data = plugin.getPlayerDataManager().get(playerId);
        if (data == null) return;
        
        int oldLevel = data.getForgeLevel();
        data.setForgeExp(data.getForgeExp() + amount);
        
        // 检查升级
        plugin.getPlayerDataManager().checkLevelUp(data);
        
        // 发布升级事件
        if (data.getForgeLevel() > oldLevel) {
            Player player = Bukkit.getPlayer(playerId);
            if (player != null) {
                PlayerLevelUpEvent event = new PlayerLevelUpEvent(
                    player,
                    oldLevel,
                    data.getForgeLevel()
                );
                Bukkit.getPluginManager().callEvent(event);
            }
        }
    }

    @Override
    public List<String> getUnlockedRecipes(UUID playerId) {
        PlayerForgeData data = plugin.getPlayerDataManager().get(playerId);
        if (data == null) return new ArrayList<>();
        return new ArrayList<>(data.getLearnedRecipes());
    }

    @Override
    public boolean hasRecipeUnlocked(UUID playerId, String recipeId) {
        PlayerForgeData data = plugin.getPlayerDataManager().get(playerId);
        return data != null && data.hasLearned(recipeId);
    }

    @Override
    public boolean unlockRecipe(UUID playerId, String recipeId) {
        PlayerForgeData data = plugin.getPlayerDataManager().get(playerId);
        if (data == null) return false;
        
        if (data.hasLearned(recipeId)) {
            return false; // 已解锁
        }
        
        data.learnRecipe(recipeId);
        
        // 异步保存
        if (asyncExecutor != null) {
            asyncExecutor.execute(() -> {
                plugin.getPlayerDataManager().save(data);
            });
        } else {
            plugin.getPlayerDataManager().save(data);
        }
        
        return true;
    }

    @Override
    public boolean isForging(UUID playerId) {
        // 检查是否在锻造界面中
        Player player = Bukkit.getPlayer(playerId);
        if (player == null) return false;
        
        return player.getOpenInventory().getTopInventory().getHolder() 
            instanceof cn.guangdian.forge.gui.ForgeGUI;
    }

    @Override
    public boolean startForge(UUID playerId, String recipeId) {
        Player player = Bukkit.getPlayer(playerId);
        if (player == null) return false;
        
        ForgeRecipe recipe = plugin.getRecipeManager().getRecipe(recipeId);
        if (recipe == null) return false;
        
        PlayerForgeData data = plugin.getPlayerDataManager().get(playerId);
        if (data.getForgeLevel() < recipe.getRequiredForgeLevel()) {
            return false;
        }
        
        // 打开锻造界面
        cn.guangdian.forge.gui.ForgeGUI gui = 
            new cn.guangdian.forge.gui.ForgeGUI(plugin, player, recipe);
        gui.open();
        
        return true;
    }

    @Override
    public boolean cancelForge(UUID playerId) {
        Player player = Bukkit.getPlayer(playerId);
        if (player == null) return false;
        
        if (isForging(playerId)) {
            player.closeInventory();
            return true;
        }
        
        return false;
    }

    @Override
    public double getForgeProgress(UUID playerId) {
        // GuangDianForge 不支持锻造进度条，返回 0 或 1
        return isForging(playerId) ? 0.5 : 0.0;
    }

    @Override
    public List<String> getAvailableRecipes() {
        List<String> recipes = new ArrayList<>();
        for (ForgeRecipe recipe : plugin.getRecipeManager().getAllRecipes()) {
            recipes.add(recipe.getId());
        }
        return recipes;
    }

    @Override
    public boolean isAvailable() {
        return true;
    }

    /**
     * 注销服务
     */
    public void unregister() {
        if (useRPGCore) {
            try {
                ServiceRegistry registry = RPGCore.getInstance().getServiceRegistry();
                registry.unregisterService(ForgeService.class);
                plugin.getLogger().info("已从 RPGCore 注销: ForgeService");
            } catch (Exception e) {
                plugin.getLogger().warning("从 RPGCore 注销失败: " + e.getMessage());
            }
        }
    }

    /**
     * 检查是否使用 RPGCore
     */
    public boolean isUsingRPGCore() {
        return useRPGCore;
    }

    /**
     * 获取 AsyncExecutor (供其他组件使用)
     */
    public Optional<AsyncExecutor> getAsyncExecutor() {
        return Optional.ofNullable(asyncExecutor);
    }
}