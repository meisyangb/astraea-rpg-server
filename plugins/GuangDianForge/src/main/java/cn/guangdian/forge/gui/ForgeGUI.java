package cn.guangdian.forge.gui;

import cn.guangdian.forge.GuangDianForge;
import cn.guangdian.forge.hook.MythicMobsHook;
import cn.guangdian.forge.model.ForgeRecipe;
import cn.guangdian.forge.model.PlayerForgeData;
import cn.guangdian.forge.util.ForgeUtil;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import org.bukkit.NamespacedKey;
import org.bukkit.persistence.PersistentDataType;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 锻造界面 - 27格（3行）
 * 布局：
 * [玻璃][材料][材料][材料][玻璃][玻璃][结果][玻璃][玻璃]  第一行
 * [玻璃][材料][材料][材料][玻璃][成功率][锻造按钮][玻璃][返回] 第二行  
 * [玻璃][玻璃][玻璃][玻璃][玻璃][玻璃][玻璃][玻璃][玻璃]  第三行
 * 
 * 只支持 MythicMobs 自定义物品作为材料和结果
 */
public class ForgeGUI implements InventoryHolder {
    private final GuangDianForge plugin;
    private final Player player;
    private final Inventory inventory;
    private final ForgeRecipe recipe;
    private final MiniMessage miniMessage;

    private static final int SIZE = 27;
    private static final int[] MATERIAL_SLOTS = {1, 2, 3, 10, 11, 12};
    private static final int RESULT_SLOT = 6;
    private static final int SUCCESS_RATE_SLOT = 14;
    private static final int FORGE_BUTTON = 15;
    private static final int BACK_BUTTON = 17;

    public ForgeGUI(GuangDianForge plugin, Player player, ForgeRecipe recipe) {
        this.plugin = plugin;
        this.player = player;
        this.recipe = recipe;
        this.miniMessage = plugin.getMiniMessageParser();
        this.inventory = Bukkit.createInventory(this, SIZE,
            Component.text("锻造: " + recipe.getDisplayName(), NamedTextColor.GOLD));
    }
    
    public void open() {
        fillBackground();
        setupSlots();
        player.openInventory(inventory);
    }
    
    private void fillBackground() {
        ItemStack glass = new ItemStack(Material.GRAY_STAINED_GLASS_PANE);
        ItemMeta meta = glass.getItemMeta();
        meta.displayName(Component.empty());
        glass.setItemMeta(meta);
        
        for (int i = 0; i < SIZE; i++) {
            boolean isSlot = false;
            for (int s : MATERIAL_SLOTS) if (s == i) isSlot = true;
            if (i == RESULT_SLOT || i == SUCCESS_RATE_SLOT || i == FORGE_BUTTON || i == BACK_BUTTON) {
                isSlot = true;
            }
            if (!isSlot) {
                inventory.setItem(i, glass);
            }
        }
    }
    
    private void setupSlots() {
        // 材料槽位 - 空气（玩家可以放材料）
        for (int slot : MATERIAL_SLOTS) {
            inventory.setItem(slot, new ItemStack(Material.AIR));
        }
        
        // 结果预览（只显示 MythicMobs 物品）
        updateResultPreview();
        
        // 成功率显示
        updateSuccessRate();
        
        // 锻造按钮
        ItemStack forgeBtn = new ItemStack(Material.ANVIL);
        ItemMeta forgeMeta = forgeBtn.getItemMeta();
        forgeMeta.displayName(Component.text("开始锻造", NamedTextColor.GREEN));
        List<Component> forgeLore = new ArrayList<>();
        forgeLore.add(Component.text("点击开始锻造", NamedTextColor.GRAY));
        forgeMeta.lore(forgeLore);
        forgeBtn.setItemMeta(forgeMeta);
        inventory.setItem(FORGE_BUTTON, forgeBtn);
        
        // 返回按钮
        ItemStack backBtn = new ItemStack(Material.BARRIER);
        ItemMeta backMeta = backBtn.getItemMeta();
        backMeta.displayName(Component.text("返回图纸选择", NamedTextColor.RED));
        backBtn.setItemMeta(backMeta);
        inventory.setItem(BACK_BUTTON, backBtn);
    }
    
    /**
     * 更新结果预览 - 只显示 MythicMobs 物品
     */
    public void updateResultPreview() {
        ItemStack result;
        
        MythicMobsHook hook = plugin.getMythicMobsHook();
        if (hook != null && hook.isEnabled()) {
            result = hook.getMythicItem(recipe.getResultMythicMobsItem());
            if (result == null) {
                // MythicMobs物品获取失败，显示错误提示
                result = new ItemStack(Material.BEDROCK);
                ItemMeta meta = result.getItemMeta();
                meta.displayName(Component.text("MythicMobs物品不存在", NamedTextColor.RED));
                List<Component> lore = new ArrayList<>();
                lore.add(Component.text("物品ID: " + recipe.getResultMythicMobsItem(), NamedTextColor.GRAY));
                lore.add(Component.text("请检查 MythicMobs 配置", NamedTextColor.YELLOW));
                meta.lore(lore);
                result.setItemMeta(meta);
            }
        } else {
            // MythicMobs未启用，显示错误提示
            result = new ItemStack(Material.BEDROCK);
            ItemMeta meta = result.getItemMeta();
            meta.displayName(Component.text("需要 MythicMobs 支持", NamedTextColor.RED));
            List<Component> lore = new ArrayList<>();
            lore.add(Component.text("请确保 MythicMobs 已安装", NamedTextColor.YELLOW));
            meta.lore(lore);
            result.setItemMeta(meta);
        }
        
        inventory.setItem(RESULT_SLOT, result);
    }
    
    public void updateSuccessRate() {
        PlayerForgeData data = plugin.getPlayerDataManager().get(player.getUniqueId());
        double rate = ForgeUtil.calcSuccessRate(recipe, data, plugin.getConfig());
        
        ItemStack rateItem = new ItemStack(Material.PAPER);
        ItemMeta meta = rateItem.getItemMeta();
        meta.displayName(Component.text("成功率: " + (int)(rate * 100) + "%", 
            rate >= 0.7 ? NamedTextColor.GREEN : rate >= 0.4 ? NamedTextColor.YELLOW : NamedTextColor.RED));
        
        List<Component> lore = new ArrayList<>();
        lore.add(Component.text("基础: " + (int)(recipe.getBaseSuccessRate() * 100) + "%", NamedTextColor.GRAY));
        int bonus = Math.max(0, data.getForgeLevel() - recipe.getRequiredForgeLevel());
        double perLevel = plugin.getConfig().getDouble("success-rate-per-forge-level", 0.02);
        lore.add(Component.text("等级加成: +" + (int)(bonus * perLevel * 100) + "%", NamedTextColor.AQUA));
        lore.add(Component.text("最高: 95%", NamedTextColor.DARK_GRAY));
        
        // 显示所需材料
        lore.add(Component.empty());
        lore.add(Component.text("所需材料:", NamedTextColor.GOLD));
        for (Map.Entry<String, Integer> entry : recipe.getIngredients().entrySet()) {
            String displayName = recipe.getIngredientDisplayName(entry.getKey());
            int required = entry.getValue();
            int have = getMaterialCount(entry.getKey());
            // 使用 MiniMessage 处理颜色代码
            String prefix = have >= required ? "  <green>✔ " : "  <red>✘ ";
            lore.add(miniMessage.deserialize(prefix + displayName + " x" + required + " (" + have + ")"));
        }
        
        // 显示结果物品
        lore.add(Component.empty());
        lore.add(Component.text("锻造结果:", NamedTextColor.GOLD));
        lore.add(Component.text(recipe.getResultMythicMobsItem(), NamedTextColor.LIGHT_PURPLE));
        
        meta.lore(lore);
        rateItem.setItemMeta(meta);
        inventory.setItem(SUCCESS_RATE_SLOT, rateItem);
    }
    
    /**
     * 检查是否有足够的材料
     */
    public boolean hasEnoughMaterials() {
        for (Map.Entry<String, Integer> req : recipe.getIngredients().entrySet()) {
            int have = getMaterialCount(req.getKey());
            if (have < req.getValue()) return false;
        }
        return true;
    }
    
    /**
     * 获取指定材料数量（通过 MythicMobs ID 匹配）
     */
    public int getMaterialCount(String ingredientKey) {
        int count = 0;
        MythicMobsHook hook = plugin.getMythicMobsHook();
        boolean debug = plugin.getConfig().getBoolean("debug", false);
        
        if (debug) {
            plugin.getLogger().info("[调试] 检查材料: " + ingredientKey);
        }
        
        for (int slot : MATERIAL_SLOTS) {
            ItemStack item = inventory.getItem(slot);
            if (item == null || item.getType() == Material.AIR) continue;
            
            // 调试模式：打印物品的 PDC 内容
            if (debug && item.hasItemMeta()) {
                var pdc = item.getItemMeta().getPersistentDataContainer();
                plugin.getLogger().info("[调试] 槽位 " + slot + " 物品类型: " + item.getType());
                plugin.getLogger().info("[调试] 槽位 " + slot + " PDC Keys: " + pdc.getKeys());
                
                // 检查 mythicmobs:type key
                NamespacedKey mmKey = new NamespacedKey("mythicmobs", "type");
                if (pdc.has(mmKey, PersistentDataType.STRING)) {
                    String id = pdc.get(mmKey, PersistentDataType.STRING);
                    plugin.getLogger().info("[调试] 槽位 " + slot + " MythicMobs ID: " + id);
                }
            }
            
            // 使用 MythicMobs ID 匹配
            boolean matches = false;
            if (hook != null && hook.isEnabled()) {
                matches = hook.matchesConfig(item, ingredientKey);
            }
            
            if (debug) {
                plugin.getLogger().info("[调试] 槽位 " + slot + " 匹配结果: " + matches);
            }
            
            if (matches) {
                count += item.getAmount();
            }
        }
        
        if (debug) {
            plugin.getLogger().info("[调试] 材料 " + ingredientKey + " 总数: " + count);
        }
        return count;
    }
    
    /**
     * 消耗材料
     */
    public void consumeMaterials() {
        Map<String, Integer> required = new HashMap<>(recipe.getIngredients());
        MythicMobsHook hook = plugin.getMythicMobsHook();
        
        for (int slot : MATERIAL_SLOTS) {
            ItemStack item = inventory.getItem(slot);
            if (item == null || item.getType() == Material.AIR) continue;
            
            // 找到匹配的材料需求
            String matchedKey = null;
            for (String key : required.keySet()) {
                boolean matches = false;
                
                if (hook != null && hook.isEnabled()) {
                    matches = hook.matchesConfig(item, key);
                }
                
                if (matches && required.get(key) > 0) {
                    matchedKey = key;
                    break;
                }
            }
            
            if (matchedKey != null) {
                int need = required.get(matchedKey);
                int take = Math.min(need, item.getAmount());
                item.setAmount(item.getAmount() - take);
                required.put(matchedKey, need - take);
                
                if (item.getAmount() <= 0) {
                    inventory.setItem(slot, new ItemStack(Material.AIR));
                }
            }
        }
    }
    
    public boolean isMaterialSlot(int slot) {
        for (int s : MATERIAL_SLOTS) if (s == slot) return true;
        return false;
    }
    
    public boolean isForgeButton(int slot) { return slot == FORGE_BUTTON; }
    public boolean isBackButton(int slot) { return slot == BACK_BUTTON; }
    public ForgeRecipe getRecipe() { return recipe; }
    public Player getPlayer() { return player; }
    public GuangDianForge getPlugin() { return plugin; }
    public int[] getMaterialSlots() { return MATERIAL_SLOTS; }
    
    @Override
    public Inventory getInventory() { return inventory; }
}