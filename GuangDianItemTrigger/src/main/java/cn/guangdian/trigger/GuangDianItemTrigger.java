package cn.guangdian.trigger;

import cn.guangdian.trigger.adapter.ItemTriggerServiceAdapter;
import cn.guangdian.rpgcore.RPGCore;
import cn.guangdian.rpgcore.api.GameLogger;
import cn.guangdian.rpgcore.message.MiniMessageService;
import cn.guangdian.rpgcore.plugin.AbstractRPGPlugin;
import cn.guangdian.rpgcore.sound.SoundService;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.Registry;
import org.bukkit.Sound;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabExecutor;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * GuangDianItemTrigger - 光点物品触发器插件
 *
 * <p>本插件已优化集成 RPGCore 服务：</p>
 * <ul>
 *   <li>日志系统 - 使用 RPGCore GameLogger（带降级兼容）</li>
 *   <li>消息发送 - 使用 RPGCore MiniMessageService（带降级兼容）</li>
 *   <li>音效播放 - 使用 RPGCore SoundService（带降级兼容）</li>
 *   <li>颜色转换 - 自动将 & 代码转换为 MiniMessage 格式</li>
 * </ul>
 *
 * <p>支持多种触发类型：右键、左键、潜行点击、攻击命中等</p>
 * <p>支持多种动作类型：命令、消息、菜单、音效、药水效果、RPG技能等</p>
 *
 * @author GuangDian
 * @version 1.1.0
 * @since 1.0.0
 * @see OPTIMIZATION.md 优化详情
 */
public class GuangDianItemTrigger extends AbstractRPGPlugin implements Listener, CommandExecutor, TabExecutor {

    private static GuangDianItemTrigger instance;
    private FileConfiguration config;

    // RPGCore 服务适配器
    private ItemTriggerServiceAdapter serviceAdapter;

    // RPGCore 服务引用
    private SoundService soundService;
    private MiniMessageService miniMessage;
    private MiniMessage miniMessageParser;

    // RPGCore 日志服务
    private GameLogger gameLogger;

    private final Map<String, TriggerConfig> triggers = new ConcurrentHashMap<>();
    private final Map<String, List<TriggerConfig>> triggersByType = new ConcurrentHashMap<>();
    private final Map<UUID, Map<String, Long>> cooldowns = new ConcurrentHashMap<>();
    private final Map<UUID, Map<String, Long>> cooldownMessages = new ConcurrentHashMap<>();
    private final Map<UUID, Set<String>> oneTimeUsed = new ConcurrentHashMap<>();

    private final Map<String, Sound> soundCache = new ConcurrentHashMap<>();
    private final Map<String, PotionEffectType> effectCache = new ConcurrentHashMap<>();

    @Override
    protected void onPluginEnable() {
        instance = this;
        saveDefaultConfig();
        config = getConfig();

        // 初始化 RPGCore 服务
        initRPGCoreServices();

        preCacheEnums();
        loadTriggers();

        serviceAdapter = new ItemTriggerServiceAdapter(this);

        getCommand("gdtrigger").setExecutor(this);
        getServer().getPluginManager().registerEvents(this, this);

        logInfo("光点物品触发插件已启用! 版本: " + getDescription().getVersion());
        logInfo("作者: Gumin | QQ: 2271257344");
        logInfo("已加载 " + triggers.size() + " 个触发器");

        if (Bukkit.getPluginManager().isPluginEnabled("RPGCore")) {
            logInfo("RPGCore 集成模式已启用");
        }
    }

    /**
     * 初始化 RPGCore 核心服务
     */
    private void initRPGCoreServices() {
        if (Bukkit.getPluginManager().isPluginEnabled("RPGCore")) {
            try {
                RPGCore rpgCore = RPGCore.getInstance();
                if (rpgCore != null) {
                    soundService = rpgCore.getSoundService();
                    miniMessage = rpgCore.getMiniMessageService();
                    gameLogger = rpgCore.getGameLogger();
                    if (miniMessage != null) {
                        miniMessageParser = miniMessage.getMiniMessage();
                    }
                    logInfo("已连接到 RPGCore 服务 (SoundService, MiniMessageService, GameLogger)");
                }
            } catch (Exception e) {
                logWarning("连接 RPGCore 服务失败: " + e.getMessage());
            }
        }

        // 如果 RPGCore 服务不可用，使用本地降级服务
        if (soundService == null) {
            soundService = SoundService.getInstance();
        }
        if (miniMessage == null) {
            miniMessage = MiniMessageService.getInstance();
            miniMessageParser = miniMessage.getMiniMessage();
        }
        if (gameLogger == null) {
            logInfo("使用 Bukkit Logger（降级）");
        }
    }

    @Override
    protected void onPluginDisable() {
        if (serviceAdapter != null) {
            serviceAdapter.unregister();
        }

        triggers.clear();
        triggersByType.clear();
        cooldowns.clear();
        cooldownMessages.clear();
        oneTimeUsed.clear();
        soundCache.clear();
        effectCache.clear();
        logInfo("光点物品触发插件已禁用!");
    }

    @Override
    protected String getPluginName() {
        return "GuangDianItemTrigger";
    }

    // ==================== RPGCore 服务访问 ====================

    public GameLogger getGameLogger() {
        return gameLogger;
    }

    /**
     * 检查是否使用 RPGCore 日志服务
     */
    public boolean isUsingRPGCoreLogger() {
        return gameLogger != null;
    }

    // ==================== 日志快捷方法 ====================

    public void logInfo(String message) {
        if (gameLogger != null) {
            gameLogger.info(message);
        } else {
            getLogger().info(message);
        }
    }

    public void logWarning(String message) {
        if (gameLogger != null) {
            gameLogger.warning(message);
        } else {
            getLogger().warning(message);
        }
    }

    public void logSevere(String message) {
        if (gameLogger != null) {
            gameLogger.severe(message);
        } else {
            getLogger().severe(message);
        }
    }

    public void logSevere(String message, Throwable throwable) {
        if (gameLogger != null) {
            gameLogger.severe(message, throwable);
        } else {
            getLogger().severe(message + " - " + throwable.getMessage());
            throwable.printStackTrace();
        }
    }

    public void logDebug(String message) {
        if (gameLogger != null) {
            gameLogger.debug(message);
        }
    }

    /**
     * 预缓存音效和药水效果枚举
     *
     * Paper 1.21.4: Sound.key() 虽标记过时但可用，是获取音效键名的标准方式
     */
    @SuppressWarnings("deprecation") // Sound.key() 在 Paper 1.21.4 中仍可用
    private void preCacheEnums() {
        for (Sound sound : Registry.SOUNDS) {
            soundCache.put(sound.key().value().toLowerCase(), sound);
        }
        for (PotionEffectType type : PotionEffectType.values()) {
            if (type != null) {
                effectCache.put(type.getKey().getKey().toLowerCase(), type);
            }
        }
    }

    private void loadTriggers() {
        triggers.clear();
        triggersByType.clear();

        ConfigurationSection triggersSection = config.getConfigurationSection("triggers");
        if (triggersSection == null) {
            logWarning("未找到触发器配置!");
            return;
        }

        for (String triggerName : triggersSection.getKeys(false)) {
            ConfigurationSection triggerSection = triggersSection.getConfigurationSection(triggerName);
            if (triggerSection == null) continue;

            boolean enabled = triggerSection.getBoolean("enabled", true);
            if (!enabled) continue;

            String triggerType = triggerSection.getString("trigger-type", "right_click");
            String loreKeyword = triggerSection.getString("lore-keyword", "");
            List<String> actions = triggerSection.getStringList("actions");
            int cooldown = triggerSection.getInt("cooldown", 0);
            boolean oneTime = triggerSection.getBoolean("one-time", false);
            boolean allowAir = triggerSection.getBoolean("allow-air", false);

            TriggerConfig triggerConfig = new TriggerConfig(triggerName, triggerType, loreKeyword, actions, cooldown, oneTime, allowAir);
            triggers.put(triggerName, triggerConfig);

            triggersByType.computeIfAbsent(triggerType.toLowerCase(), k -> new ArrayList<>()).add(triggerConfig);

            if (config.getBoolean("debug", false)) {
                logDebug("已加载触发器: " + triggerName + " (关键词: " + loreKeyword + ", 允许空气: " + allowAir + ")");
            }
        }
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onPlayerInteract(PlayerInteractEvent event) {
        if (!config.getBoolean("enabled", true)) return;
        if (event.getHand() != EquipmentSlot.HAND) return;

        Player player = event.getPlayer();
        ItemStack item = player.getInventory().getItemInMainHand();

        boolean isAir = item == null || item.getType() == Material.AIR;

        String displayName = "";
        List<String> lore = new ArrayList<>();

        if (!isAir) {
            ItemMeta meta = item.getItemMeta();
            if (meta == null) return;
            displayName = meta.hasDisplayName() ? stripColor(meta.getDisplayName()) : "";
            lore = meta.hasLore() ? meta.getLore() : new ArrayList<>();
        }

        String triggerType = getTriggerTypeFromAction(event.getAction(), player.isSneaking());
        List<TriggerConfig> relevantTriggers = triggersByType.get(triggerType.toLowerCase());

        if (relevantTriggers == null || relevantTriggers.isEmpty()) return;

        for (TriggerConfig trigger : relevantTriggers) {
            if (isAir) {
                if (!trigger.isAllowAir()) continue;
                if (checkCooldownWithMessage(player, trigger)) {
                    executeTrigger(player, trigger, item);
                    event.setCancelled(true);
                    event.setUseInteractedBlock(org.bukkit.event.Event.Result.DENY);
                    event.setUseItemInHand(org.bukkit.event.Event.Result.DENY);
                }
                break;
            } else {
                if (containsKeyword(lore, trigger.getLoreKeyword()) ||
                    displayName.toLowerCase().contains(trigger.getLoreKeyword().toLowerCase())) {
                    if (checkCooldownWithMessage(player, trigger)) {
                        executeTrigger(player, trigger, item);
                        event.setCancelled(true);
                        event.setUseInteractedBlock(org.bukkit.event.Event.Result.DENY);
                        event.setUseItemInHand(org.bukkit.event.Event.Result.DENY);
                    }
                    break;
                }
            }
        }
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onPlayerJoin(PlayerJoinEvent event) {
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onPlayerQuit(org.bukkit.event.player.PlayerQuitEvent event) {
        UUID playerId = event.getPlayer().getUniqueId();
        cooldowns.remove(playerId);
        cooldownMessages.remove(playerId);
        oneTimeUsed.remove(playerId);
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onEntityDamageByEntity(EntityDamageByEntityEvent event) {
        if (!config.getBoolean("enabled", true)) return;
        if (!(event.getDamager() instanceof Player)) return;

        Player player = (Player) event.getDamager();
        ItemStack item = player.getInventory().getItemInMainHand();

        if (item == null || item.getType() == Material.AIR) return;

        ItemMeta meta = item.getItemMeta();
        if (meta == null) return;

        String displayName = meta.hasDisplayName() ? stripColor(meta.getDisplayName()) : "";
        List<String> lore = meta.hasLore() ? meta.getLore() : new ArrayList<>();

        List<TriggerConfig> relevantTriggers = triggersByType.get("on_hit");

        if (relevantTriggers == null || relevantTriggers.isEmpty()) return;

        for (TriggerConfig trigger : relevantTriggers) {
            if (containsKeyword(lore, trigger.getLoreKeyword()) ||
                displayName.toLowerCase().contains(trigger.getLoreKeyword().toLowerCase())) {
                if (checkCooldownWithMessage(player, trigger)) {
                    executeTrigger(player, trigger, item);
                }
                break;
            }
        }
    }

    /**
     * 处理玩家对实体右键事件（生物、NPC等）
     * 支持 right_click 和 right_click_entity 触发类型
     */
    @EventHandler(priority = EventPriority.HIGH)
    public void onPlayerInteractEntity(PlayerInteractEntityEvent event) {
        if (!config.getBoolean("enabled", true)) return;
        if (event.getHand() != EquipmentSlot.HAND) return;

        Player player = event.getPlayer();
        ItemStack item = player.getInventory().getItemInMainHand();

        if (item == null || item.getType() == Material.AIR) return;

        ItemMeta meta = item.getItemMeta();
        if (meta == null) return;

        String displayName = meta.hasDisplayName() ? stripColor(meta.getDisplayName()) : "";
        List<String> lore = meta.hasLore() ? meta.getLore() : new ArrayList<>();

        String triggerType = player.isSneaking() ? "shift_right_click" : "right_click";
        
        List<TriggerConfig> relevantTriggers = triggersByType.get(triggerType.toLowerCase());
        List<TriggerConfig> entityTriggers = triggersByType.get("right_click_entity");

        boolean triggered = false;

        if (relevantTriggers != null) {
            for (TriggerConfig trigger : relevantTriggers) {
                if (containsKeyword(lore, trigger.getLoreKeyword()) ||
                    displayName.toLowerCase().contains(trigger.getLoreKeyword().toLowerCase())) {
                    if (checkCooldownWithMessage(player, trigger)) {
                        executeTrigger(player, trigger, item);
                        event.setCancelled(true);
                        triggered = true;
                    }
                    break;
                }
            }
        }

        if (!triggered && entityTriggers != null) {
            for (TriggerConfig trigger : entityTriggers) {
                if (containsKeyword(lore, trigger.getLoreKeyword()) ||
                    displayName.toLowerCase().contains(trigger.getLoreKeyword().toLowerCase())) {
                    if (checkCooldownWithMessage(player, trigger)) {
                        executeTrigger(player, trigger, item);
                        event.setCancelled(true);
                    }
                    break;
                }
            }
        }
    }

    private String getTriggerTypeFromAction(Action action, boolean sneaking) {
        if (sneaking) {
            if (action == Action.RIGHT_CLICK_AIR || action == Action.RIGHT_CLICK_BLOCK) {
                return "shift_right_click";
            } else if (action == Action.LEFT_CLICK_AIR || action == Action.LEFT_CLICK_BLOCK) {
                return "shift_left_click";
            }
        } else {
            if (action == Action.RIGHT_CLICK_AIR || action == Action.RIGHT_CLICK_BLOCK) {
                return "right_click";
            } else if (action == Action.LEFT_CLICK_AIR || action == Action.LEFT_CLICK_BLOCK) {
                return "left_click";
            }
        }
        return "";
    }

    private boolean containsKeyword(List<String> lore, String keyword) {
        if (keyword == null || keyword.isEmpty()) return false;

        String strippedKeyword = stripColor(keyword).toLowerCase();
        if (strippedKeyword.isEmpty()) return false;

        for (String line : lore) {
            String strippedLine = stripColor(line);
            if (strippedLine != null && strippedLine.toLowerCase().contains(strippedKeyword)) {
                return true;
            }
        }
        return false;
    }

    /**
     * 移除文本中的颜色代码
     * 支持传统 & 颜色代码和 MiniMessage 格式
     */
    private String stripColor(String text) {
        if (text == null) return "";
        // 先尝试解析为 Component，然后获取纯文本
        try {
            Component component = miniMessageParser.deserialize(text);
            return net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer.plainText().serialize(component);
        } catch (Exception e) {
            // 如果解析失败，使用传统方式移除 & 颜色代码
            return text.replaceAll("&[0-9a-fk-or]", "");
        }
    }

    private boolean checkCooldownWithMessage(Player player, TriggerConfig trigger) {
        if (trigger.getCooldown() <= 0) return true;

        UUID playerId = player.getUniqueId();
        Map<String, Long> playerCooldowns = cooldowns.computeIfAbsent(playerId, k -> new HashMap<>());
        Long lastUse = playerCooldowns.get(trigger.getName());

        if (lastUse == null) return true;

        long remaining = (lastUse + trigger.getCooldown() * 1000L) - System.currentTimeMillis();
        if (remaining > 0) {
            long messageCooldown = config.getInt("advanced.cooldown-message-cooldown", 5) * 1000L;
            Map<String, Long> lastMessages = cooldownMessages.computeIfAbsent(playerId, k -> new HashMap<>());
            Long lastMessageTime = lastMessages.get(trigger.getName());

            if (lastMessageTime == null || System.currentTimeMillis() - lastMessageTime > messageCooldown) {
                String message = config.getString("messages.cooldown", "<red>该物品还在冷却中，剩余时间: <yellow>%time%秒</yellow></red>");
                String format = config.getString("advanced.cooldown-format", "%.1f");
                message = message.replace("%time%", String.format(format, remaining / 1000.0));
                sendMessage(player, message);
                lastMessages.put(trigger.getName(), System.currentTimeMillis());
            }
            return false;
        }

        return true;
    }

    private void executeTrigger(Player player, TriggerConfig trigger, ItemStack item) {
        if (trigger.isOneTime()) {
            Set<String> usedTriggers = oneTimeUsed.computeIfAbsent(player.getUniqueId(), k -> ConcurrentHashMap.newKeySet());
            if (usedTriggers.contains(trigger.getName())) {
                sendMessage(player, "<red>你已经使用过这个物品了!</red>");
                return;
            }
        }

        boolean allActionsSuccess = true;
        for (String action : trigger.getActions()) {
            if (!executeAction(player, action, item)) {
                allActionsSuccess = false;
                break;
            }
        }

        if (allActionsSuccess && trigger.getCooldown() > 0) {
            Map<String, Long> playerCooldowns = cooldowns.computeIfAbsent(player.getUniqueId(), k -> new HashMap<>());
            playerCooldowns.put(trigger.getName(), System.currentTimeMillis());
        }

        if (allActionsSuccess && trigger.isOneTime()) {
            Set<String> usedTriggers = oneTimeUsed.computeIfAbsent(player.getUniqueId(), k -> ConcurrentHashMap.newKeySet());
            usedTriggers.add(trigger.getName());
        }
    }

    private boolean executeAction(Player player, String action, ItemStack item) {
        if (action == null || action.isEmpty()) return true;

        action = processPlaceholders(player, action);

        if (action.startsWith("command:")) {
            String command = action.substring(8);
            player.performCommand(command);
            return true;
        } else if (action.startsWith("console:")) {
            String command = action.substring(8);
            Bukkit.dispatchCommand(Bukkit.getConsoleSender(), command);
            return true;
        } else if (action.startsWith("message:")) {
            String message = action.substring(8);
            sendMessage(player, message);
            return true;
        } else if (action.startsWith("menu:")) {
            String menuName = action.substring(5);
            if (Bukkit.getPluginManager().getPlugin("GuangDianMenu") != null) {
                player.performCommand("menu " + menuName);
            } else {
                sendMessage(player, "<red>菜单插件未安装!</red>");
            }
            return true;
        } else if (action.startsWith("sound:")) {
            String soundParams = action.substring(6);
            int lastColon = soundParams.lastIndexOf(":");
            int secondLastColon = soundParams.substring(0, lastColon).lastIndexOf(":");
            
            String soundName;
            float volume = 1.0f;
            float pitch = 1.0f;
            
            if (secondLastColon > 0) {
                soundName = soundParams.substring(0, secondLastColon);
                try {
                    volume = Float.parseFloat(soundParams.substring(secondLastColon + 1, lastColon));
                    pitch = Float.parseFloat(soundParams.substring(lastColon + 1));
                } catch (Exception e) {
                    soundName = soundParams;
                }
            } else {
                soundName = soundParams;
            }
            
            if (soundService != null) {
                try {
                    soundService.playSound(player, soundName, volume, pitch);
                } catch (Exception e) {
                    logWarning("无效的声音参数: " + action + " - " + e.getMessage());
                }
            }
            return true;
        } else if (action.startsWith("effect:")) {
            String[] parts = action.substring(7).split(":");
            PotionEffectType effectType = effectCache.get(parts[0].toLowerCase());
            if (effectType != null) {
                try {
                    int amplifier = parts.length > 1 ? Integer.parseInt(parts[1]) - 1 : 0;
                    int duration = parts.length > 2 ? Integer.parseInt(parts[2]) * 20 : 100;
                    player.addPotionEffect(new PotionEffect(effectType, duration, amplifier));
                } catch (Exception e) {
                    logWarning("无效的药水效果参数: " + action);
                }
            } else {
                logWarning("未知的药水效果: " + parts[0]);
            }
            return true;
        } else if (action.startsWith("take:")) {
            int amount = Integer.parseInt(action.substring(5));
            if (item.getAmount() > amount) {
                item.setAmount(item.getAmount() - amount);
            } else {
                player.getInventory().setItemInMainHand(null);
            }
            return true;
        } else if (action.startsWith("damage:")) {
            int damage = Integer.parseInt(action.substring(7));
            item.setDurability((short) (item.getDurability() + damage));
            return true;
        } else if (action.startsWith("give:")) {
            String[] parts = action.substring(5).split(":");
            try {
                Material material = Material.valueOf(parts[0].toUpperCase());
                int amount = parts.length > 1 ? Integer.parseInt(parts[1]) : 1;
                player.getInventory().addItem(new ItemStack(material, amount));
            } catch (Exception e) {
                logWarning("无效的物品: " + parts[0]);
            }
            return true;
        } else if (action.startsWith("rpg_skill:")) {
            String skillName = action.substring(10).trim();
            return triggerRPGSkill(player, skillName);
        } else if (action.startsWith("vault_money:")) {
            try {
                double amount = Double.parseDouble(action.substring(12).trim());
                giveVaultMoney(player, amount, item);
                return true;
            } catch (NumberFormatException e) {
                logWarning("无效的金币数量: " + action);
                return false;
            }
        }
        return true;
    }

    /**
     * 给玩家Vault金币并消耗物品
     */
    private void giveVaultMoney(Player player, double amount, ItemStack item) {
        // 检查Vault是否存在
        var vaultPlugin = Bukkit.getPluginManager().getPlugin("Vault");
        if (vaultPlugin == null || !vaultPlugin.isEnabled()) {
            sendMessage(player, "<red>经济系统未安装!</red>");
            logWarning("Vault 插件未找到!");
            return;
        }

        try {
            // 获取Vault Economy
            var registeredService = Bukkit.getServicesManager().getRegistration(net.milkbowl.vault.economy.Economy.class);
            if (registeredService == null) {
                sendMessage(player, "<red>经济系统未初始化!</red>");
                return;
            }

            cn.guangdian.rpgcore.RPGCore rpgCore = cn.guangdian.rpgcore.RPGCore.getInstance();
            if (rpgCore != null && rpgCore.getExternalServices() != null && rpgCore.getExternalServices().isVaultEnabled()) {
                rpgCore.getExternalServices().deposit(player, amount);
            }

            // 消耗物品
            if (item.getAmount() > 1) {
                item.setAmount(item.getAmount() - 1);
            } else {
                player.getInventory().setItemInMainHand(null);
            }

            // 发送消息
            String message = config.getString("messages.money-received", "<green>你获得了 <gold>%amount%</gold> 金币!</green>");
            message = message.replace("%amount%", String.format("%.0f", amount));
            sendMessage(player, message);

            // 播放音效 - 使用 RPGCore SoundService
            if (soundService != null) {
                soundService.playSound(player, "minecraft:entity.experience_orb.pickup", 1.0f, 1.0f);
            }

            logInfo("玩家 " + player.getName() + " 出售物品获得 " + amount + " 金币");

        } catch (Exception e) {
            sendMessage(player, "<red>金币获取失败!</red>");
            logSevere("Vault金币操作失败: " + e.getMessage(), e);
        }
    }

    private boolean triggerRPGSkill(Player player, String skillName) {
        if (skillName == null || skillName.isEmpty()) {
            logWarning("技能名称为空!");
            return false;
        }

        var rpgPlugin = Bukkit.getPluginManager().getPlugin("GuangDianArmorStats");
        if (rpgPlugin == null || !rpgPlugin.isEnabled()) {
            sendMessage(player, "<red>RPG插件未安装或未启用!</red>");
            logWarning("GuangDianArmorStats 插件未找到!");
            return false;
        }

        try {
            var skillManagerMethod = rpgPlugin.getClass().getMethod("getSkillManager");
            var skillManager = skillManagerMethod.invoke(rpgPlugin);

            if (skillManager == null) {
                sendMessage(player, "<red>技能系统未初始化!</red>");
                return false;
            }

            var triggerMethod = skillManager.getClass().getMethod("triggerActiveSkill", Player.class, String.class);
            var result = (Boolean) triggerMethod.invoke(skillManager, player, skillName);

            if (result) {
                logDebug("玩家 " + player.getName() + " 触发技能: " + skillName);
                if (soundService != null) {
                    soundService.playSound(player, "minecraft:entity.player.levelup", 0.5f, 1.2f);
                }
                return true;
            } else {
                sendMessage(player, "<red>技能 " + skillName + " 不存在或冷却中!</red>");
                return false;
            }
        } catch (Exception e) {
            sendMessage(player, "<red>技能触发失败!</red>");
            logSevere("触发RPG技能失败: " + e.getMessage(), e);
            return false;
        }
    }

    private String processPlaceholders(Player player, String text) {
        if (text == null) return "";
        StringBuilder sb = new StringBuilder(text);
        replaceAll(sb, "%player%", player.getName());
        replaceAll(sb, "%player_name%", player.getName());
        replaceAll(sb, "%player_displayname%", player.getDisplayName());
        replaceAll(sb, "%player_level%", String.valueOf(player.getLevel()));
        replaceAll(sb, "%player_health%", String.valueOf((int) player.getHealth()));
        replaceAll(sb, "%player_world%", player.getWorld().getName());
        replaceAll(sb, "%online%", String.valueOf(Bukkit.getOnlinePlayers().size()));
        replaceAll(sb, "%max_players%", String.valueOf(Bukkit.getMaxPlayers()));
        return sb.toString();
    }

    private void replaceAll(StringBuilder sb, String target, String replacement) {
        int index;
        while ((index = sb.indexOf(target)) != -1) {
            sb.replace(index, index + target.length(), replacement);
        }
    }

    /**
     * 使用 MiniMessage 解析颜色代码
     * 将 & 颜色代码转换为 MiniMessage 格式
     */
    private void sendMessage(Player player, String text) {
        if (text == null || text.isEmpty()) return;
        player.sendMessage(miniMessage.colorize(text));
    }

    private void sendMessage(CommandSender sender, String text) {
        if (text == null || text.isEmpty()) return;
        sender.sendMessage(miniMessage.colorize(text));
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length == 0) {
            sendHelp(sender);
            return true;
        }

        switch (args[0].toLowerCase()) {
            case "reload":
                return handleReload(sender);
            case "info":
                return handleInfo(sender);
            case "list":
                return handleList(sender);
            case "help":
                sendHelp(sender);
                return true;
            default:
                sendMessage(sender, "<red>未知的命令! 使用 /gdtrigger help 查看帮助</red>");
                return true;
        }
    }

    private void sendHelp(CommandSender sender) {
        sendMessage(sender, "<gold><bold>===== 光点物品触发插件 =====</bold></gold>");
        sendMessage(sender, "<yellow>/gdtrigger reload</yellow> <gray>- 重新加载配置</gray>");
        sendMessage(sender, "<yellow>/gdtrigger info</yellow> <gray>- 显示插件信息</gray>");
        sendMessage(sender, "<yellow>/gdtrigger list</yellow> <gray>- 列出所有触发器</gray>");
        sendMessage(sender, "<yellow>/gdtrigger help</yellow> <gray>- 显示帮助信息</gray>");
        sendMessage(sender, "<gray>作者: Gumin | QQ: 2271257344</gray>");
    }

    private boolean handleReload(CommandSender sender) {
        if (!sender.hasPermission("guangdian.trigger.reload")) {
            sendMessage(sender, config.getString("messages.no-permission", "<red>你没有权限执行此操作!</red>"));
            return true;
        }

        reloadConfig();
        config = getConfig();
        loadTriggers();

        sendMessage(sender, config.getString("messages.config-reloaded", "<green>配置已重新加载!</green>"));
        return true;
    }

    private boolean handleInfo(CommandSender sender) {
        sendMessage(sender, "<gold><bold>===== 光点物品触发插件信息 =====</bold></gold>");
        sendMessage(sender, "<yellow>版本:</yellow> <white>" + getDescription().getVersion() + "</white>");
        sendMessage(sender, "<yellow>作者:</yellow> <white>Gumin</white>");
        sendMessage(sender, "<yellow>QQ:</yellow> <white>2271257344</white>");
        sendMessage(sender, "<yellow>状态:</yellow> <green>已启用</green>");
        sendMessage(sender, "<yellow>已加载触发器:</yellow> <white>" + triggers.size() + "</white>");
        sendMessage(sender, "<yellow>音效缓存:</yellow> <white>" + soundCache.size() + "</white>");
        sendMessage(sender, "<yellow>效果缓存:</yellow> <white>" + effectCache.size() + "</white>");
        return true;
    }

    private boolean handleList(CommandSender sender) {
        sendMessage(sender, "<gold><bold>===== 触发器列表 =====</bold></gold>");
        for (TriggerConfig trigger : triggers.values()) {
            sendMessage(sender, "<yellow>" + trigger.getName() + "</yellow> <gray>- 关键词: </gray><white>" + trigger.getLoreKeyword() + "</white>");
        }
        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        List<String> completions = new ArrayList<>();

        if (args.length == 1) {
            completions.add("reload");
            completions.add("info");
            completions.add("list");
            completions.add("help");
        }

        return completions;
    }

    public static GuangDianItemTrigger getInstance() {
        return instance;
    }

    /**
     * 获取 MiniMessageService
     * @return MiniMessageService 实例
     */
    public MiniMessageService getMiniMessage() {
        return miniMessage;
    }

    /**
     * 获取 SoundService
     * @return SoundService 实例
     */
    public SoundService getSoundService() {
        return soundService;
    }

    private static class TriggerConfig {
        private final String name;
        private final String triggerType;
        private final String loreKeyword;
        private final List<String> actions;
        private final int cooldown;
        private final boolean oneTime;
        private final boolean allowAir;

        public TriggerConfig(String name, String triggerType, String loreKeyword, List<String> actions, int cooldown, boolean oneTime, boolean allowAir) {
            this.name = name;
            this.triggerType = triggerType;
            this.loreKeyword = loreKeyword;
            this.actions = actions;
            this.cooldown = cooldown;
            this.oneTime = oneTime;
            this.allowAir = allowAir;
        }

        public String getName() { return name; }
        public String getTriggerType() { return triggerType; }
        public String getLoreKeyword() { return loreKeyword; }
        public List<String> getActions() { return actions; }
        public int getCooldown() { return cooldown; }
        public boolean isOneTime() { return oneTime; }
        public boolean isAllowAir() { return allowAir; }
    }
}
