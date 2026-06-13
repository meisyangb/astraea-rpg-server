package cn.guangdian.enhance.manager;

import cn.guangdian.enhance.GuangDianEnhance;
import cn.guangdian.enhance.config.EnhanceConfig;
import cn.guangdian.enhance.data.EnhanceData;
import cn.guangdian.enhance.data.EnhanceResult;
import cn.guangdian.enhance.stone.EnhanceStone;
import cn.guangdian.enhance.stone.EnhanceStoneManager;
import cn.guangdian.enhance.stone.StoneType;
import cn.guangdian.enhance.storage.EnhanceStorage;
import cn.guangdian.rpgcore.integration.ExternalServiceIntegration;
import cn.guangdian.rpgcore.message.MiniMessageService;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ThreadLocalRandom;

public class EnhanceManager {

    private final GuangDianEnhance plugin;
    private final EnhanceStorage storage;
    private final EnhanceConfig config;
    private final SuccessRateCalculator rateCalculator;
    private final MiniMessageService miniMessage;
    
    private final Map<UUID, Long> lastEnhanceTime = new ConcurrentHashMap<>();
    private final Map<UUID, EnhanceData> enhanceDataCache = new ConcurrentHashMap<>();
    
    private final Map<UUID, Map<Integer, Integer>> pityCounters = new ConcurrentHashMap<>();

    public EnhanceManager(GuangDianEnhance plugin, EnhanceStorage storage, EnhanceConfig config) {
        this.plugin = plugin;
        this.storage = storage;
        this.config = config;
        this.rateCalculator = new SuccessRateCalculator(config);
        this.miniMessage = plugin.getMiniMessage();
    }

    public EnhanceResult enhance(Player player, ItemStack item) {
        return enhance(player, item, null);
    }
    
    public EnhanceResult enhance(Player player, ItemStack item, EnhanceStone stone) {
        UUID uuid = player.getUniqueId();
        
        if (!config.isEnhanceable(item)) {
            return EnhanceResult.NOT_ENHANCEABLE;
        }
        
        int currentLevel = storage.getLevel(item);
        
        if (currentLevel >= config.getMaxLevel()) {
            return EnhanceResult.MAX_LEVEL_REACHED;
        }
        
        if (isInCooldown(uuid)) {
            return EnhanceResult.IN_COOLDOWN;
        }
        
        List<EnhanceConfig.MaterialCost> costs = config.getMaterialCostForLevel(currentLevel + 1);
        if (!hasMaterials(player, costs)) {
            return EnhanceResult.INSUFFICIENT_MATERIAL;
        }
        
        double moneyCost = config.getMoneyCostForLevel(currentLevel + 1);
        if (moneyCost > 0 && !hasMoney(player, moneyCost)) {
            return EnhanceResult.INSUFFICIENT_MONEY;
        }
        
        if (stone != null && stone.isConsumable()) {
            EnhanceStoneManager stoneManager = plugin.getStoneManager();
            if (!stoneManager.consumeStone(player, stone)) {
                player.sendMessage(miniMessage.colorize("<red>强化石不足!"));
                return EnhanceResult.INSUFFICIENT_MATERIAL;
            }
        }
        
        consumeMaterials(player, costs);
        if (moneyCost > 0) {
            withdrawMoney(player, moneyCost);
        }
        
        lastEnhanceTime.put(uuid, System.currentTimeMillis());
        
        int pityCount = getPityCount(uuid, currentLevel);
        double baseRate = rateCalculator.calculate(currentLevel, item);
        double pityBonus = calculatePityBonus(pityCount);
        double successRate = Math.min(1.0, baseRate + pityBonus);
        
        if (stone != null) {
            successRate = stone.applyEffect(successRate);
        }
        
        boolean success = rateCalculator.rollSuccess(successRate);
        
        if (success) {
            resetPityCount(uuid, currentLevel);
            return handleSuccess(player, item, currentLevel, stone);
        } else {
            incrementPityCount(uuid, currentLevel);
            return handleFailure(player, item, currentLevel, stone);
        }
    }

    private EnhanceResult handleSuccess(Player player, ItemStack item, int currentLevel, EnhanceStone stone) {
        ItemStack enhanced = storage.setLevel(item, currentLevel + 1);
        
        PlayerInventory inv = player.getInventory();
        ItemStack mainHand = inv.getItemInMainHand();
        if (isSameItem(mainHand, item)) {
            inv.setItemInMainHand(enhanced);
        } else {
            for (int i = 0; i < inv.getSize(); i++) {
                if (isSameItem(inv.getItem(i), item)) {
                    inv.setItem(i, enhanced);
                    break;
                }
            }
        }
        
        player.updateInventory();
        
        playSuccessEffect(player);
        
        int newLevel = currentLevel + 1;
        if (stone != null && stone.guaranteesSuccess()) {
            player.sendMessage(miniMessage.colorize(
                "<green>强化成功! <yellow>(必成石) <gold>强化等级: <bold>+" + newLevel + "</bold>"));
        } else {
            player.sendMessage(miniMessage.colorize(
                "<green>强化成功! <yellow>强化等级: <bold>+" + newLevel + "</bold>"));
        }
        
        return EnhanceResult.SUCCESS;
    }

    private EnhanceResult handleFailure(Player player, ItemStack item, int currentLevel, EnhanceStone stone) {
        String failureType = config.getFailureType();
        
        boolean hasProtection = (stone != null && stone.preventsDegrade()) || 
            (config.isProtectionCharmEnabled() && consumeProtectionCharm(player));
        
        if (hasProtection) {
            player.sendMessage(miniMessage.colorize(
                "<yellow>强化失败，但保护效果保护了装备!"));
            playFailEffect(player);
            return EnhanceResult.FAILED_NO_CHANGE;
        }
        
        switch (failureType.toLowerCase()) {
            case "none":
                playFailEffect(player);
                player.sendMessage(miniMessage.colorize(
                    "<red>强化失败，等级不变"));
                return EnhanceResult.FAILED_NO_CHANGE;
                
            case "degrade":
                if (currentLevel > 0 && 
                    ThreadLocalRandom.current().nextDouble() < config.getDegradeChance()) {
                    ItemStack degraded = storage.setLevel(item, currentLevel - 1);
                    replaceItem(player, item, degraded);
                    playDegradeEffect(player);
                    player.sendMessage(miniMessage.colorize(
                        "<red>强化失败，等级下降至 <bold>+" + (currentLevel - 1) + "</bold>"));
                    return EnhanceResult.FAILED_DEGRADE;
                } else {
                    playFailEffect(player);
                    player.sendMessage(miniMessage.colorize(
                        "<red>强化失败，等级不变"));
                    return EnhanceResult.FAILED_NO_CHANGE;
                }
                
            case "destroy":
                if (ThreadLocalRandom.current().nextDouble() < config.getDestroyChance()) {
                    replaceItem(player, item, null);
                    playDestroyEffect(player);
                    player.sendMessage(miniMessage.colorize(
                        "<dark_red>强化失败，装备已破碎!"));
                    return EnhanceResult.FAILED_DESTROY;
                } else if (currentLevel > 0) {
                    ItemStack degraded = storage.setLevel(item, currentLevel - 1);
                    replaceItem(player, item, degraded);
                    playDegradeEffect(player);
                    player.sendMessage(miniMessage.colorize(
                        "<red>强化失败，等级下降至 <bold>+" + (currentLevel - 1) + "</bold>"));
                    return EnhanceResult.FAILED_DEGRADE;
                } else {
                    playFailEffect(player);
                    player.sendMessage(miniMessage.colorize(
                        "<red>强化失败，等级不变"));
                    return EnhanceResult.FAILED_NO_CHANGE;
                }
                
            default:
                playFailEffect(player);
                return EnhanceResult.FAILED_NO_CHANGE;
        }
    }

    private boolean hasMaterials(Player player, List<EnhanceConfig.MaterialCost> costs) {
        if (costs == null || costs.isEmpty()) {
            return true;
        }
        
        Map<Material, Integer> required = new HashMap<>();
        for (EnhanceConfig.MaterialCost cost : costs) {
            required.merge(cost.getMaterial(), cost.getAmount(), Integer::sum);
        }
        
        for (Map.Entry<Material, Integer> entry : required.entrySet()) {
            if (countMaterial(player, entry.getKey()) < entry.getValue()) {
                return false;
            }
        }
        
        return true;
    }

    private int countMaterial(Player player, Material material) {
        int count = 0;
        for (ItemStack item : player.getInventory().getContents()) {
            if (item != null && item.getType() == material) {
                count += item.getAmount();
            }
        }
        return count;
    }

    private void consumeMaterials(Player player, List<EnhanceConfig.MaterialCost> costs) {
        if (costs == null || costs.isEmpty()) {
            return;
        }
        
        Map<Material, Integer> required = new HashMap<>();
        for (EnhanceConfig.MaterialCost cost : costs) {
            required.merge(cost.getMaterial(), cost.getAmount(), Integer::sum);
        }
        
        for (Map.Entry<Material, Integer> entry : required.entrySet()) {
            removeMaterial(player, entry.getKey(), entry.getValue());
        }
    }

    private void removeMaterial(Player player, Material material, int amount) {
        ItemStack[] contents = player.getInventory().getContents();
        
        for (int i = 0; i < contents.length && amount > 0; i++) {
            ItemStack item = contents[i];
            if (item != null && item.getType() == material) {
                int itemAmount = item.getAmount();
                if (itemAmount <= amount) {
                    player.getInventory().setItem(i, null);
                    amount -= itemAmount;
                } else {
                    item.setAmount(itemAmount - amount);
                    amount = 0;
                }
            }
        }
        
        player.updateInventory();
    }

    private boolean hasMoney(Player player, double amount) {
        ExternalServiceIntegration externalServices = plugin.getExternalServices();
        if (externalServices == null || !externalServices.isVaultEnabled()) {
            return true;
        }
        
        return externalServices.getBalance(player) >= amount;
    }

    private void withdrawMoney(Player player, double amount) {
        ExternalServiceIntegration externalServices = plugin.getExternalServices();
        if (externalServices == null || !externalServices.isVaultEnabled()) {
            return;
        }
        
        externalServices.withdraw(player, amount);
    }

    private boolean consumeProtectionCharm(Player player) {
        Material charmMaterial = config.getProtectionCharmItem();
        
        for (int i = 0; i < player.getInventory().getSize(); i++) {
            ItemStack item = player.getInventory().getItem(i);
            if (item != null && item.getType() == charmMaterial) {
                if (config.isProtectionCharmConsume()) {
                    if (item.getAmount() > 1) {
                        item.setAmount(item.getAmount() - 1);
                    } else {
                        player.getInventory().setItem(i, null);
                    }
                    player.updateInventory();
                }
                return true;
            }
        }
        
        return false;
    }

    private boolean isInCooldown(UUID uuid) {
        Long lastTime = lastEnhanceTime.get(uuid);
        if (lastTime == null) {
            return false;
        }
        return System.currentTimeMillis() - lastTime < config.getCooldownMs();
    }

    private boolean isSameItem(ItemStack item1, ItemStack item2) {
        if (item1 == null || item2 == null) {
            return false;
        }
        return item1.getType() == item2.getType() && 
               item1.getAmount() == item2.getAmount() &&
               Objects.equals(item1.getItemMeta(), item2.getItemMeta());
    }

    private void replaceItem(Player player, ItemStack oldItem, ItemStack newItem) {
        PlayerInventory inv = player.getInventory();
        
        ItemStack mainHand = inv.getItemInMainHand();
        if (isSameItem(mainHand, oldItem)) {
            inv.setItemInMainHand(newItem);
            player.updateInventory();
            return;
        }
        
        for (int i = 0; i < inv.getSize(); i++) {
            if (isSameItem(inv.getItem(i), oldItem)) {
                inv.setItem(i, newItem);
                player.updateInventory();
                return;
            }
        }
    }

    private void playSuccessEffect(Player player) {
        player.playSound(player.getLocation(), Sound.BLOCK_ANVIL_USE, 1.0f, 1.5f);
        player.playSound(player.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 0.5f, 1.2f);
    }

    private void playFailEffect(Player player) {
        player.playSound(player.getLocation(), Sound.BLOCK_ANVIL_BREAK, 1.0f, 0.8f);
    }

    private void playDegradeEffect(Player player) {
        player.playSound(player.getLocation(), Sound.BLOCK_ANVIL_BREAK, 1.0f, 0.6f);
        player.playSound(player.getLocation(), Sound.ENTITY_ITEM_BREAK, 0.5f, 1.0f);
    }

    private void playDestroyEffect(Player player) {
        player.playSound(player.getLocation(), Sound.ENTITY_ITEM_BREAK, 1.0f, 0.5f);
        player.playSound(player.getLocation(), Sound.BLOCK_GLASS_BREAK, 1.0f, 0.8f);
    }

    public int getLevel(ItemStack item) {
        return storage.getLevel(item);
    }

    public double getSuccessRate(int level, ItemStack item) {
        return rateCalculator.calculate(level, item);
    }

    public double getAttributeMultiplier(int level) {
        return 1.0 + level * config.getAttributeBonusPerLevel();
    }

    public SuccessRateCalculator getRateCalculator() {
        return rateCalculator;
    }
    
    private int getPityCount(UUID uuid, int level) {
        if (!config.isPityEnabled()) {
            return 0;
        }
        
        Map<Integer, Integer> playerPity = pityCounters.computeIfAbsent(uuid, k -> new ConcurrentHashMap<>());
        return playerPity.getOrDefault(level, 0);
    }
    
    private void incrementPityCount(UUID uuid, int level) {
        if (!config.isPityEnabled()) {
            return;
        }
        
        Map<Integer, Integer> playerPity = pityCounters.computeIfAbsent(uuid, k -> new ConcurrentHashMap<>());
        int current = playerPity.getOrDefault(level, 0);
        playerPity.put(level, current + 1);
    }
    
    private void resetPityCount(UUID uuid, int level) {
        Map<Integer, Integer> playerPity = pityCounters.get(uuid);
        if (playerPity != null) {
            playerPity.remove(level);
        }
    }
    
    private double calculatePityBonus(int pityCount) {
        if (!config.isPityEnabled() || pityCount <= 0) {
            return 0.0;
        }
        
        double bonus = pityCount * config.getPityBonusPerFail();
        return Math.min(bonus, config.getPityMaxBonus());
    }
    
    public int getPityCountForPlayer(UUID uuid, int level) {
        return getPityCount(uuid, level);
    }
    
    public double getPityBonusForPlayer(UUID uuid, int level) {
        return calculatePityBonus(getPityCount(uuid, level));
    }

    public void removePlayer(UUID uuid) {
        lastEnhanceTime.remove(uuid);
        enhanceDataCache.remove(uuid);
        pityCounters.remove(uuid);
    }
}
