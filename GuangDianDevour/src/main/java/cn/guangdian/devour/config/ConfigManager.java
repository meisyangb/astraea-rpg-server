package cn.guangdian.devour.config;

import cn.guangdian.devour.GuangDianDevour;
import org.bukkit.Material;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.util.*;

/**
 * 配置管理器
 * 
 * @author Astraea RPG Team
 * @since 1.0.0
 */
public class ConfigManager {
    
    private final GuangDianDevour plugin;
    
    // 配置文件
    private FileConfiguration config;
    
    // GUI 配置
    private GUIConfig guiConfig;
    
    // 打开方式配置
    private OpenMethodConfig openMethodConfig;
    
    // 吞噬设置
    private DevourSettings devourSettings;
    
    // 消息配置
    private MessagesConfig messagesConfig;
    
    // 可吞噬的属性列表
    private Set<String> devourAttributes;
    
    // 吞噬剑定义
    private Map<String, DevourWeaponConfig> devourWeapons;
    
    public ConfigManager(GuangDianDevour plugin) {
        this.plugin = plugin;
        this.devourAttributes = new HashSet<>();
        this.devourWeapons = new HashMap<>();
    }
    
    /**
     * 加载配置
     */
    public void load() {
        plugin.saveDefaultConfig();
        plugin.reloadConfig();
        config = plugin.getConfig();
        
        loadGUIConfig();
        loadOpenMethodConfig();
        loadDevourSettings();
        loadMessagesConfig();
        loadDevourAttributes();
        loadDevourWeapons();
    }
    
    /**
     * 加载GUI配置
     */
    private void loadGUIConfig() {
        guiConfig = new GUIConfig();
        
        guiConfig.setTitle(config.getString("gui.title", "<dark_red>⚔ 吞噬武器 ⚔"));
        guiConfig.setSize(config.getInt("gui.size", 54));
        
        List<Integer> slots = new ArrayList<>();
        for (int slot : config.getIntegerList("gui.devour-slots")) {
            slots.add(slot);
        }
        guiConfig.setDevourSlots(slots);
        
        guiConfig.setAttributeDisplayStart(config.getInt("gui.attribute-display-start", 28));
        guiConfig.setPreviewDisplayStart(config.getInt("gui.preview-display-start", 37));
        guiConfig.setConfirmButton(config.getInt("gui.confirm-button", 48));
        guiConfig.setCancelButton(config.getInt("gui.cancel-button", 50));
    }
    
    /**
     * 加载打开方式配置
     */
    private void loadOpenMethodConfig() {
        openMethodConfig = new OpenMethodConfig();
        openMethodConfig.setCommand(config.getBoolean("open-method.command", true));
        openMethodConfig.setRightClick(config.getBoolean("open-method.right-click", false));
        openMethodConfig.setShiftRightClick(config.getBoolean("open-method.shift-right-click", true));
    }
    
    /**
     * 加载吞噬设置
     */
    private void loadDevourSettings() {
        devourSettings = new DevourSettings();
        
        devourSettings.setConsumeItem(config.getBoolean("settings.consume-item", true));
        devourSettings.setAllowClear(config.getBoolean("settings.allow-clear", true));
        devourSettings.setReturnOnClear(config.getBoolean("settings.return-on-clear", false));
        
        // 音效配置
        devourSettings.setSoundEnable(config.getBoolean("settings.sound.enable", true));
        devourSettings.setSoundOpen(config.getString("settings.sound.open", "BLOCK_CHEST_OPEN"));
        devourSettings.setSoundDevour(config.getString("settings.sound.devour", "ENTITY_PLAYER_LEVELUP"));
        devourSettings.setSoundClear(config.getString("settings.sound.clear", "BLOCK_ANVIL_BREAK"));
        devourSettings.setSoundClose(config.getString("settings.sound.close", "BLOCK_CHEST_CLOSE"));
        devourSettings.setSoundError(config.getString("settings.sound.error", "ENTITY_VILLAGER_NO"));
        devourSettings.setSoundVolume((float) config.getDouble("settings.sound.volume", 1.0));
        devourSettings.setSoundPitch((float) config.getDouble("settings.sound.pitch", 1.0));
        
        // 粒子配置
        devourSettings.setParticleEnable(config.getBoolean("settings.particle.enable", true));
        devourSettings.setParticleType(config.getString("settings.particle.type", "TOTEM"));
        devourSettings.setParticleCount(config.getInt("settings.particle.count", 20));
    }
    
    /**
     * 加载消息配置
     */
    private void loadMessagesConfig() {
        messagesConfig = new MessagesConfig();
        
        messagesConfig.setPrefix(config.getString("messages.prefix", "<dark_red>[吞噬系统]<reset> "));
        messagesConfig.setSuccess(config.getString("messages.success", "<green>成功吞噬 <yellow>{item}<green>！属性已累加"));
        messagesConfig.setNoSlot(config.getString("messages.no-slot", "<red>没有可用的吞噬槽位"));
        messagesConfig.setNotWeapon(config.getString("messages.not-weapon", "<red>只能吞噬武器类物品"));
        messagesConfig.setAlreadyDevoured(config.getString("messages.already-devoured", "<red>该槽位已被占用"));
        messagesConfig.setSlotCleared(config.getString("messages.slot-cleared", "<green>槽位已清空"));
        messagesConfig.setNotDevourWeapon(config.getString("messages.not-devour-weapon", "<red>这不是吞噬剑"));
        messagesConfig.setGuiOpened(config.getString("messages.gui-opened", "<gray>吞噬界面已打开"));
        messagesConfig.setReload(config.getString("messages.reload", "<green>配置已重载"));
    }
    
    /**
     * 加载可吞噬的属性列表
     */
    private void loadDevourAttributes() {
        devourAttributes.clear();
        List<String> attrs = config.getStringList("devour-attributes");
        devourAttributes.addAll(attrs);
        
        plugin.getLogger().info("已加载 " + devourAttributes.size() + " 个可吞噬属性");
    }
    
    /**
     * 加载吞噬剑定义
     */
    private void loadDevourWeapons() {
        devourWeapons.clear();
        
        if (config.contains("devour-weapons")) {
            for (String key : config.getConfigurationSection("devour-weapons").getKeys(false)) {
                String path = "devour-weapons." + key;
                
                int maxSlots = config.getInt(path + ".max-slots", 3);
                String mythicType = config.getString(path + ".mythic-type", key);
                String materialStr = config.getString(path + ".material", "DIAMOND_SWORD");
                
                Material material;
                try {
                    material = Material.valueOf(materialStr);
                } catch (IllegalArgumentException e) {
                    material = Material.DIAMOND_SWORD;
                }
                
                DevourWeaponConfig weaponConfig = new DevourWeaponConfig(key, maxSlots, mythicType, material);
                devourWeapons.put(key, weaponConfig);
            }
        }
        
        plugin.getLogger().info("已加载 " + devourWeapons.size() + " 种吞噬剑");
    }
    
    // Getters
    
    public GUIConfig getGuiConfig() {
        return guiConfig;
    }
    
    public OpenMethodConfig getOpenMethodConfig() {
        return openMethodConfig;
    }
    
    public DevourSettings getDevourSettings() {
        return devourSettings;
    }
    
    public MessagesConfig getMessagesConfig() {
        return messagesConfig;
    }
    
    public Set<String> getDevourAttributes() {
        return devourAttributes;
    }
    
    public Map<String, DevourWeaponConfig> getDevourWeapons() {
        return devourWeapons;
    }
    
    /**
     * 检查属性是否可吞噬
     */
    public boolean isDevourableAttribute(String attrName) {
        return devourAttributes.contains(attrName);
    }
    
    /**
     * 检查是否为吞噬剑
     */
    public boolean isDevourWeapon(String mythicType) {
        return devourWeapons.containsKey(mythicType);
    }
    
    /**
     * 获取吞噬剑配置
     */
    public DevourWeaponConfig getDevourWeaponConfig(String mythicType) {
        return devourWeapons.get(mythicType);
    }
}
