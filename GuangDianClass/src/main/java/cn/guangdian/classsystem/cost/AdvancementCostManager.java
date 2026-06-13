package cn.guangdian.classsystem.cost;

import cn.guangdian.classsystem.GuangDianClass;
import cn.guangdian.rpgcore.RPGCore;
import cn.guangdian.rpgcore.integration.ExternalServiceIntegration;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.*;

public class AdvancementCostManager {
    
    private final GuangDianClass plugin;
    private final Map<Integer, AdvancementCost> advancementCosts;
    private ExternalServiceIntegration externalServices;
    
    public AdvancementCostManager(GuangDianClass plugin) {
        this.plugin = plugin;
        this.advancementCosts = new HashMap<>();
        
        RPGCore rpgCore = RPGCore.getInstance();
        if (rpgCore != null) {
            this.externalServices = rpgCore.getExternalServices();
        }
        
        loadCosts();
    }
    
    private void loadCosts() {
        ConfigurationSection costsSection = plugin.getConfig().getConfigurationSection("advancement-costs");
        if (costsSection == null) {
            setDefaultCosts();
            return;
        }
        
        for (String advancementKey : costsSection.getKeys(false)) {
            try {
                int advancement = Integer.parseInt(advancementKey);
                ConfigurationSection costSection = costsSection.getConfigurationSection(advancementKey);
                if (costSection == null) continue;
                
                AdvancementCost cost = new AdvancementCost();
                cost.setMoney(costSection.getDouble("money", 0));
                cost.setExpCost(costSection.getLong("exp", 0));
                
                List<Map<?, ?>> itemsList = costSection.getMapList("items");
                for (Map<?, ?> itemMap : itemsList) {
                    String materialStr = (String) itemMap.get("material");
                    int amount = itemMap.get("amount") != null ? (Integer) itemMap.get("amount") : 1;
                    String name = (String) itemMap.get("name");
                    
                    if (materialStr != null) {
                        Material material = Material.matchMaterial(materialStr);
                        if (material != null) {
                            cost.addItemRequirement(new ItemRequirement(material, amount, name));
                        }
                    }
                }
                
                List<String> permissions = costSection.getStringList("permissions");
                cost.setRequiredPermissions(permissions);
                
                advancementCosts.put(advancement, cost);
            } catch (NumberFormatException e) {
                plugin.getLogger().warning("无效的转职等级: " + advancementKey);
            }
        }
        
        plugin.getLogger().info("已加载 " + advancementCosts.size() + " 个转职消耗配置");
    }
    
    private void setDefaultCosts() {
        AdvancementCost first = new AdvancementCost();
        first.setMoney(1000);
        advancementCosts.put(1, first);
        
        AdvancementCost second = new AdvancementCost();
        second.setMoney(5000);
        advancementCosts.put(2, second);
        
        AdvancementCost third = new AdvancementCost();
        third.setMoney(20000);
        advancementCosts.put(3, third);
        
        AdvancementCost divine = new AdvancementCost();
        divine.setMoney(100000);
        advancementCosts.put(4, divine);
        
        plugin.getLogger().info("使用默认转职消耗配置");
    }
    
    public boolean canAfford(Player player, int advancementLevel) {
        AdvancementCost cost = advancementCosts.get(advancementLevel);
        if (cost == null) return true;
        
        if (cost.getMoney() > 0) {
            if (externalServices == null || !externalServices.isVaultEnabled()) {
                return true;
            }
            double balance = externalServices.getBalance(player);
            if (balance < cost.getMoney()) {
                return false;
            }
        }
        
        for (ItemRequirement item : cost.getItemRequirements()) {
            if (!hasEnoughItems(player, item)) {
                return false;
            }
        }
        
        return true;
    }
    
    private boolean hasEnoughItems(Player player, ItemRequirement requirement) {
        int count = 0;
        for (ItemStack item : player.getInventory().getContents()) {
            if (item != null && item.getType() == requirement.getMaterial()) {
                count += item.getAmount();
            }
        }
        return count >= requirement.getAmount();
    }
    
    public boolean consumeCosts(Player player, int advancementLevel) {
        AdvancementCost cost = advancementCosts.get(advancementLevel);
        if (cost == null) return true;
        
        if (!canAfford(player, advancementLevel)) {
            return false;
        }
        
        if (cost.getMoney() > 0 && externalServices != null && externalServices.isVaultEnabled()) {
            externalServices.withdraw(player, cost.getMoney());
        }
        
        for (ItemRequirement item : cost.getItemRequirements()) {
            removeItems(player, item);
        }
        
        return true;
    }
    
    private void removeItems(Player player, ItemRequirement requirement) {
        int remaining = requirement.getAmount();
        ItemStack[] contents = player.getInventory().getContents();
        
        for (int i = 0; i < contents.length && remaining > 0; i++) {
            ItemStack item = contents[i];
            if (item != null && item.getType() == requirement.getMaterial()) {
                int toRemove = Math.min(remaining, item.getAmount());
                item.setAmount(item.getAmount() - toRemove);
                remaining -= toRemove;
            }
        }
    }
    
    public void sendCostInfo(Player player, int advancementLevel) {
        AdvancementCost cost = advancementCosts.get(advancementLevel);
        if (cost == null) {
            player.sendMessage(Component.text("此转职无需消耗").color(NamedTextColor.GREEN));
            return;
        }
        
        player.sendMessage(Component.text("========== 转职消耗 ==========").color(NamedTextColor.GOLD));
        
        if (cost.getMoney() > 0) {
            double balance = externalServices != null && externalServices.isVaultEnabled() 
                ? externalServices.getBalance(player) : 0;
            boolean canAfford = balance >= cost.getMoney();
            player.sendMessage(Component.text("金币: ")
                .color(NamedTextColor.YELLOW)
                .append(Component.text(String.format("%.0f", cost.getMoney()))
                    .color(canAfford ? NamedTextColor.GREEN : NamedTextColor.RED))
                .append(Component.text(" (当前: " + String.format("%.0f", balance) + ")")
                    .color(NamedTextColor.GRAY)));
        }
        
        for (ItemRequirement item : cost.getItemRequirements()) {
            int has = countItems(player, item.getMaterial());
            boolean canAfford = has >= item.getAmount();
            player.sendMessage(Component.text(item.getMaterial().name() + ": ")
                .color(NamedTextColor.YELLOW)
                .append(Component.text(item.getAmount())
                    .color(canAfford ? NamedTextColor.GREEN : NamedTextColor.RED))
                .append(Component.text(" (当前: " + has + ")")
                    .color(NamedTextColor.GRAY)));
        }
        
        player.sendMessage(Component.text("==============================").color(NamedTextColor.GOLD));
    }
    
    private int countItems(Player player, Material material) {
        int count = 0;
        for (ItemStack item : player.getInventory().getContents()) {
            if (item != null && item.getType() == material) {
                count += item.getAmount();
            }
        }
        return count;
    }
    
    public AdvancementCost getCost(int advancementLevel) {
        return advancementCosts.get(advancementLevel);
    }
    
    public void reload() {
        advancementCosts.clear();
        loadCosts();
    }
    
    public static class AdvancementCost {
        private double money = 0;
        private long expCost = 0;
        private List<ItemRequirement> itemRequirements = new ArrayList<>();
        private List<String> requiredPermissions = new ArrayList<>();
        
        public double getMoney() { return money; }
        public void setMoney(double money) { this.money = money; }
        
        public long getExpCost() { return expCost; }
        public void setExpCost(long expCost) { this.expCost = expCost; }
        
        public List<ItemRequirement> getItemRequirements() { return itemRequirements; }
        public void addItemRequirement(ItemRequirement item) { this.itemRequirements.add(item); }
        
        public List<String> getRequiredPermissions() { return requiredPermissions; }
        public void setRequiredPermissions(List<String> requiredPermissions) { this.requiredPermissions = requiredPermissions; }
    }
    
    public static class ItemRequirement {
        private final Material material;
        private final int amount;
        private final String name;
        
        public ItemRequirement(Material material, int amount, String name) {
            this.material = material;
            this.amount = amount;
            this.name = name;
        }
        
        public Material getMaterial() { return material; }
        public int getAmount() { return amount; }
        public String getName() { return name; }
    }
}
