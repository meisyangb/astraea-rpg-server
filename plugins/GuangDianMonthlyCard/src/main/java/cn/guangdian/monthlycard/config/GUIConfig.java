package cn.guangdian.monthlycard.config;

import cn.guangdian.monthlycard.GuangDianMonthlyCard;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * GUI 配置管理器
 */
public class GUIConfig {
    
    private final GuangDianMonthlyCard plugin;
    private YamlConfiguration config;
    
    // 主菜单配置
    private MenuConfig mainMenu;
    private MenuConfig shopMenu;
    private MenuConfig rewardPreviewMenu;
    
    public GUIConfig(GuangDianMonthlyCard plugin) {
        this.plugin = plugin;
        loadConfig();
    }
    
    public void loadConfig() {
        // 从ConfigManager获取gui配置
        config = plugin.getConfigManager().getGuiConfig();
        
        loadMainMenu();
        loadShopMenu();
        loadRewardPreviewMenu();
    }
    
    private void loadMainMenu() {
        mainMenu = new MenuConfig();
        ConfigurationSection section = config.getConfigurationSection("main");
        if (section == null) return;
        
        mainMenu.title = section.getString("title", "<gold>☆ 月卡中心 ☆");
        mainMenu.rows = section.getInt("size", 45) / 9;
        
        // Filler
        mainMenu.filler = loadItemConfig(section.getConfigurationSection("filler"));
        
        // Items
        ConfigurationSection itemsSection = section.getConfigurationSection("items");
        if (itemsSection != null) {
            // 状态按钮
            ButtonConfig statusBtn = new ButtonConfig();
            statusBtn.states.put("active", loadItemConfig(itemsSection.getConfigurationSection("status-active")));
            statusBtn.states.put("inactive", loadItemConfig(itemsSection.getConfigurationSection("status-inactive")));
            statusBtn.slot = section.getInt("slots.status", 13);
            mainMenu.buttons.put("status", statusBtn);
            
            // 领取按钮
            ButtonConfig claimBtn = new ButtonConfig();
            claimBtn.states.put("can-claim", loadItemConfig(itemsSection.getConfigurationSection("claim-available")));
            claimBtn.states.put("claimed", loadItemConfig(itemsSection.getConfigurationSection("claim-claimed")));
            claimBtn.states.put("no-card", loadItemConfig(itemsSection.getConfigurationSection("status-inactive")));
            claimBtn.slot = section.getInt("slots.claim", 20);
            mainMenu.buttons.put("daily-reward", claimBtn);
            
            // 补签按钮
            ButtonConfig makeupBtn = new ButtonConfig();
            makeupBtn.states.put("available", loadItemConfig(itemsSection.getConfigurationSection("makeup-available")));
            makeupBtn.states.put("unavailable", loadItemConfig(itemsSection.getConfigurationSection("makeup-unavailable")));
            makeupBtn.slot = section.getInt("slots.makeup", 21);
            mainMenu.buttons.put("makeup", makeupBtn);
            
            // 预览按钮
            ButtonConfig previewBtn = new ButtonConfig();
            previewBtn.defaultItem = loadItemConfig(itemsSection.getConfigurationSection("preview"));
            previewBtn.slot = section.getInt("slots.preview", 22);
            mainMenu.buttons.put("preview", previewBtn);
            
            // 累计奖励按钮
            ButtonConfig milestoneBtn = new ButtonConfig();
            milestoneBtn.defaultItem = loadItemConfig(itemsSection.getConfigurationSection("milestone"));
            milestoneBtn.slot = section.getInt("slots.milestone", 23);
            mainMenu.buttons.put("milestone", milestoneBtn);
            
            // 关闭按钮
            ButtonConfig closeBtn = new ButtonConfig();
            closeBtn.defaultItem = loadItemConfig(itemsSection.getConfigurationSection("close"));
            closeBtn.slot = section.getInt("slots.close", 40);
            mainMenu.buttons.put("close", closeBtn);
        }
    }
    
    private void loadShopMenu() {
        // 新配置中商店功能集成在主菜单，通过点击月卡类型购买
        // 保留此方法但设置为空实现，保持兼容性
        shopMenu = new MenuConfig();
    }
    
    private void loadRewardPreviewMenu() {
        rewardPreviewMenu = new MenuConfig();
        ConfigurationSection section = config.getConfigurationSection("preview");
        if (section == null) return;
        
        rewardPreviewMenu.title = section.getString("title", "<gold>☆ 奖励预览 - 第%page%/%total_pages%页 ☆");
        rewardPreviewMenu.rows = section.getInt("size", 54) / 9;
        rewardPreviewMenu.filler = loadItemConfig(section.getConfigurationSection("filler"));
        rewardPreviewMenu.rewardStartSlot = section.getInt("slots.reward-start", 10);
        
        // Items
        ConfigurationSection itemsSection = section.getConfigurationSection("items");
        if (itemsSection != null) {
            // 奖励状态
            rewardPreviewMenu.rewardStates.put("claimed", loadItemConfig(itemsSection.getConfigurationSection("claimed")));
            rewardPreviewMenu.rewardStates.put("current", loadItemConfig(itemsSection.getConfigurationSection("current")));
            rewardPreviewMenu.rewardStates.put("past", loadItemConfig(itemsSection.getConfigurationSection("missed")));
            rewardPreviewMenu.rewardStates.put("future", loadItemConfig(itemsSection.getConfigurationSection("future")));
            
            // 导航按钮
            ButtonConfig backBtn = new ButtonConfig();
            backBtn.defaultItem = loadItemConfig(itemsSection.getConfigurationSection("back"));
            backBtn.slot = section.getInt("slots.back", 45);
            rewardPreviewMenu.buttons.put("back", backBtn);
            
            ButtonConfig prevBtn = new ButtonConfig();
            prevBtn.defaultItem = loadItemConfig(itemsSection.getConfigurationSection("prev-page"));
            prevBtn.slot = section.getInt("slots.prev-page", 48);
            rewardPreviewMenu.buttons.put("prev-page", prevBtn);
            
            ButtonConfig nextBtn = new ButtonConfig();
            nextBtn.defaultItem = loadItemConfig(itemsSection.getConfigurationSection("next-page"));
            nextBtn.slot = section.getInt("slots.next-page", 50);
            rewardPreviewMenu.buttons.put("next-page", nextBtn);
            
            ButtonConfig closeBtn = new ButtonConfig();
            closeBtn.defaultItem = loadItemConfig(itemsSection.getConfigurationSection("close"));
            closeBtn.slot = section.getInt("slots.close", 49);
            rewardPreviewMenu.buttons.put("close", closeBtn);
        }
    }
    
    private ItemConfig loadItemConfig(ConfigurationSection section) {
        if (section == null) return null;
        
        ItemConfig item = new ItemConfig();
        String matName = section.getString("material", "STONE");
        try {
            item.material = Material.valueOf(matName.toUpperCase());
        } catch (IllegalArgumentException e) {
            item.material = Material.STONE;
            plugin.getLogger().warning("Invalid material: " + matName);
        }
        item.name = section.getString("name", "");
        item.lore = section.getStringList("lore");
        item.glow = section.getBoolean("glow", false);
        return item;
    }
    
    private BorderConfig loadBorderConfig(ConfigurationSection section) {
        if (section == null) return null;
        
        BorderConfig border = new BorderConfig();
        border.enabled = section.getBoolean("enabled", false);
        border.item = loadItemConfig(section);
        border.slots = section.getIntegerList("slots");
        return border;
    }
    
    private ButtonConfig loadButtonConfig(ConfigurationSection section) {
        if (section == null) return null;
        
        ButtonConfig button = new ButtonConfig();
        button.slot = section.getInt("slot", 0);
        
        // Check for state-based config (like status.active/inactive)
        if (section.contains("active")) {
            button.states.put("active", loadItemConfig(section.getConfigurationSection("active")));
            button.states.put("inactive", loadItemConfig(section.getConfigurationSection("inactive")));
        }
        if (section.contains("can-claim")) {
            button.states.put("can-claim", loadItemConfig(section.getConfigurationSection("can-claim")));
            button.states.put("claimed", loadItemConfig(section.getConfigurationSection("claimed")));
            button.states.put("no-card", loadItemConfig(section.getConfigurationSection("no-card")));
        }
        
        // Simple button
        if (button.states.isEmpty()) {
            button.defaultItem = loadItemConfig(section);
        }
        
        return button;
    }
    
    // Getters
    public MenuConfig getMainMenu() { return mainMenu; }
    public MenuConfig getShopMenu() { return shopMenu; }
    public MenuConfig getRewardPreviewMenu() { return rewardPreviewMenu; }
    
    /**
     * 菜单配置
     */
    public static class MenuConfig {
        public String title = "";
        public int rows = 6;
        public ItemConfig filler;
        public BorderConfig border;
        public Map<String, ButtonConfig> buttons = new HashMap<>();
        public List<Integer> cardSlots = new ArrayList<>();
        public Map<String, ItemConfig> cardTemplates = new HashMap<>();
        public Map<String, ItemConfig> rewardStates = new HashMap<>();
        public int rewardStartSlot = 10;
    }
    
    /**
     * 物品配置
     */
    public static class ItemConfig {
        public Material material = Material.STONE;
        public String name = "";
        public List<String> lore = new ArrayList<>();
        public boolean glow = false;
    }
    
    /**
     * 边框配置
     */
    public static class BorderConfig {
        public boolean enabled = false;
        public ItemConfig item;
        public List<Integer> slots = new ArrayList<>();
    }
    
    /**
     * 按钮配置
     */
    public static class ButtonConfig {
        public int slot = 0;
        public ItemConfig defaultItem;
        public Map<String, ItemConfig> states = new HashMap<>();
    }
}