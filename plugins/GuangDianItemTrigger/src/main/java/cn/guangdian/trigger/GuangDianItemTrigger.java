package cn.guangdian.trigger;

import cn.guangdian.trigger.adapter.ItemTriggerServiceAdapter;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
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
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class GuangDianItemTrigger extends JavaPlugin implements Listener, CommandExecutor, TabExecutor {

    private static GuangDianItemTrigger instance;
    private FileConfiguration config;
    
    // RPGCore 服务适配器
    private ItemTriggerServiceAdapter serviceAdapter;

    private final Map<String, TriggerConfig> triggers = new HashMap<>();
    private final Map<String, List<TriggerConfig>> triggersByType = new HashMap<>();
    private final Map<UUID, Map<String, Long>> cooldowns = new ConcurrentHashMap<>();
    private final Map<UUID, Map<String, Long>> cooldownMessages = new ConcurrentHashMap<>();
    private final Map<UUID, Set<String>> oneTimeUsed = new ConcurrentHashMap<>();

    private final Map<String, Sound> soundCache = new ConcurrentHashMap<>();
    private final Map<String, PotionEffectType> effectCache = new ConcurrentHashMap<>();

    @Override
    public void onEnable() {
        instance = this;
        saveDefaultConfig();
        config = getConfig();

        preCacheEnums();
        loadTriggers();
        
        // 初始化 RPGCore 服务适配器
        serviceAdapter = new ItemTriggerServiceAdapter(this);

        getCommand("gdtrigger").setExecutor(this);
        getServer().getPluginManager().registerEvents(this, this);

        getLogger().info("光点物品触发插件已启用! 版本: " + getDescription().getVersion());
        getLogger().info("作者: Gumin | QQ: 2271257344");
        getLogger().info("已加载 " + triggers.size() + " 个触发器");
        
        if (Bukkit.getPluginManager().isPluginEnabled("RPGCore")) {
            getLogger().info("RPGCore 集成模式已启用");
        }
    }

    @Override
    public void onDisable() {
        // 注销 RPGCore 服务
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
        getLogger().info("光点物品触发插件已禁用!");
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
            getLogger().warning("未找到触发器配置!");
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

            TriggerConfig triggerConfig = new TriggerConfig(triggerName, triggerType, loreKeyword, actions, cooldown, oneTime);
            triggers.put(triggerName, triggerConfig);

            triggersByType.computeIfAbsent(triggerType.toLowerCase(), k -> new ArrayList<>()).add(triggerConfig);

            if (config.getBoolean("debug", false)) {
                getLogger().info("已加载触发器: " + triggerName + " (关键词: " + loreKeyword + ")");
            }
        }
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onPlayerInteract(PlayerInteractEvent event) {
        if (!config.getBoolean("enabled", true)) return;
        if (event.getHand() != EquipmentSlot.HAND) return;

        Player player = event.getPlayer();
        ItemStack item = player.getInventory().getItemInMainHand();

        if (item == null || item.getType() == Material.AIR) return;

        ItemMeta meta = item.getItemMeta();
        if (meta == null) return;

        // 获取物品名称和Lore
        String displayName = meta.hasDisplayName() ? ChatColor.stripColor(meta.getDisplayName()) : "";
        List<String> lore = meta.hasLore() ? meta.getLore() : new ArrayList<>();

        String triggerType = getTriggerTypeFromAction(event.getAction(), player.isSneaking());
        List<TriggerConfig> relevantTriggers = triggersByType.get(triggerType.toLowerCase());

        if (relevantTriggers == null || relevantTriggers.isEmpty()) return;

        for (TriggerConfig trigger : relevantTriggers) {
            // 同时检查物品名称和Lore
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

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onPlayerJoin(PlayerJoinEvent event) {
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

        String displayName = meta.hasDisplayName() ? ChatColor.stripColor(meta.getDisplayName()) : "";
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

        String lowerKeyword = keyword.toLowerCase();
        for (String line : lore) {
            String strippedLine = ChatColor.stripColor(line);
            if (strippedLine != null && strippedLine.toLowerCase().contains(lowerKeyword)) {
                return true;
            }
        }
        return false;
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
                String message = config.getString("messages.cooldown", "&c该物品还在冷却中，剩余时间: &e%time%秒");
                message = message.replace("%time%", String.format(config.getString("advanced.cooldown-format", "#.##"), remaining / 1000.0));
                player.sendMessage(translateColors(message));
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
                player.sendMessage(translateColors("&c你已经使用过这个物品了!"));
                return;
            }
        }

        for (String action : trigger.getActions()) {
            executeAction(player, action, item);
        }

        if (trigger.getCooldown() > 0) {
            Map<String, Long> playerCooldowns = cooldowns.computeIfAbsent(player.getUniqueId(), k -> new HashMap<>());
            playerCooldowns.put(trigger.getName(), System.currentTimeMillis());
        }

        if (trigger.isOneTime()) {
            Set<String> usedTriggers = oneTimeUsed.computeIfAbsent(player.getUniqueId(), k -> ConcurrentHashMap.newKeySet());
            usedTriggers.add(trigger.getName());
        }
    }

    private void executeAction(Player player, String action, ItemStack item) {
        if (action == null || action.isEmpty()) return;

        action = processPlaceholders(player, action);

        if (action.startsWith("command:")) {
            String command = action.substring(8);
            player.performCommand(command);
        } else if (action.startsWith("console:")) {
            String command = action.substring(8);
            Bukkit.dispatchCommand(Bukkit.getConsoleSender(), command);
        } else if (action.startsWith("message:")) {
            String message = action.substring(8);
            player.sendMessage(translateColors(message));
        } else if (action.startsWith("menu:")) {
            String menuName = action.substring(5);
            if (Bukkit.getPluginManager().getPlugin("GuangDianMenu") != null) {
                player.performCommand("menu " + menuName);
            } else {
                player.sendMessage(translateColors("&c菜单插件未安装!"));
            }
        } else if (action.startsWith("sound:")) {
            String[] parts = action.substring(6).split(":");
            Sound sound = soundCache.get(parts[0].toLowerCase());
            if (sound == null) {
                sound = soundCache.get("block_note_block_bell");
            }
            if (sound != null) {
                try {
                    float volume = parts.length > 1 ? Float.parseFloat(parts[1]) : 1.0f;
                    float pitch = parts.length > 2 ? Float.parseFloat(parts[2]) : 1.0f;
                    player.playSound(player.getLocation(), sound, volume, pitch);
                } catch (Exception e) {
                    getLogger().warning("无效的声音参数: " + action);
                }
            }
        } else if (action.startsWith("effect:")) {
            String[] parts = action.substring(7).split(":");
            PotionEffectType effectType = effectCache.get(parts[0].toLowerCase());
            if (effectType != null) {
                try {
                    int amplifier = parts.length > 1 ? Integer.parseInt(parts[1]) - 1 : 0;
                    int duration = parts.length > 2 ? Integer.parseInt(parts[2]) * 20 : 100;
                    player.addPotionEffect(new PotionEffect(effectType, duration, amplifier));
                } catch (Exception e) {
                    getLogger().warning("无效的药水效果参数: " + action);
                }
            } else {
                getLogger().warning("未知的药水效果: " + parts[0]);
            }
        } else if (action.startsWith("take:")) {
            int amount = Integer.parseInt(action.substring(5));
            if (item.getAmount() > amount) {
                item.setAmount(item.getAmount() - amount);
            } else {
                player.getInventory().setItemInMainHand(null);
            }
        } else if (action.startsWith("damage:")) {
            int damage = Integer.parseInt(action.substring(7));
            item.setDurability((short) (item.getDurability() + damage));
        } else if (action.startsWith("give:")) {
            String[] parts = action.substring(5).split(":");
            try {
                Material material = Material.valueOf(parts[0].toUpperCase());
                int amount = parts.length > 1 ? Integer.parseInt(parts[1]) : 1;
                player.getInventory().addItem(new ItemStack(material, amount));
            } catch (Exception e) {
                getLogger().warning("无效的物品: " + parts[0]);
            }
        } else if (action.startsWith("rpg_skill:")) {
            // RPG技能联动
            String skillName = action.substring(10).trim();
            triggerRPGSkill(player, skillName);
        } else if (action.startsWith("vault_money:")) {
            // Vault金币出售
            try {
                double amount = Double.parseDouble(action.substring(12).trim());
                giveVaultMoney(player, amount, item);
            } catch (NumberFormatException e) {
                getLogger().warning("无效的金币数量: " + action);
            }
        }
    }

    /**
     * 给玩家Vault金币并消耗物品
     */
    private void giveVaultMoney(Player player, double amount, ItemStack item) {
        // 检查Vault是否存在
        var vaultPlugin = Bukkit.getPluginManager().getPlugin("Vault");
        if (vaultPlugin == null || !vaultPlugin.isEnabled()) {
            player.sendMessage(translateColors("&c经济系统未安装!"));
            getLogger().warning("Vault 插件未找到!");
            return;
        }

        try {
            // 获取Vault Economy
            var registeredService = Bukkit.getServicesManager().getRegistration(net.milkbowl.vault.economy.Economy.class);
            if (registeredService == null) {
                player.sendMessage(translateColors("&c经济系统未初始化!"));
                return;
            }
            
            net.milkbowl.vault.economy.Economy economy = registeredService.getProvider();
            
            // 给玩家金币
            economy.depositPlayer(player, amount);
            
            // 消耗物品
            if (item.getAmount() > 1) {
                item.setAmount(item.getAmount() - 1);
            } else {
                player.getInventory().setItemInMainHand(null);
            }
            
            // 发送消息
            String message = config.getString("messages.money-received", "&a你获得了 &e%amount% &a金币!");
            message = message.replace("%amount%", String.format("%.0f", amount));
            player.sendMessage(translateColors(message));
            
            // 播放音效
            player.playSound(player.getLocation(), Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 1.0f, 1.0f);
            
            getLogger().info("玩家 " + player.getName() + " 出售物品获得 " + amount + " 金币");
            
        } catch (Exception e) {
            player.sendMessage(translateColors("&c金币获取失败!"));
            getLogger().severe("Vault金币操作失败: " + e.getMessage());
            getLogger().log(java.util.logging.Level.SEVERE, "详细异常信息", e);
        }
    }

    private void triggerRPGSkill(Player player, String skillName) {
        if (skillName == null || skillName.isEmpty()) {
            getLogger().warning("技能名称为空!");
            return;
        }

        var rpgPlugin = Bukkit.getPluginManager().getPlugin("GuangDianArmorStats");
        if (rpgPlugin == null || !rpgPlugin.isEnabled()) {
            player.sendMessage(translateColors("&cRPG插件未安装或未启用!"));
            getLogger().warning("GuangDianArmorStats 插件未找到!");
            return;
        }

        try {
            var skillManagerMethod = rpgPlugin.getClass().getMethod("getSkillManager");
            var skillManager = skillManagerMethod.invoke(rpgPlugin);
            
            if (skillManager == null) {
                player.sendMessage(translateColors("&c技能系统未初始化!"));
                return;
            }

            var triggerMethod = skillManager.getClass().getMethod("triggerActiveSkill", Player.class, String.class);
            var result = (Boolean) triggerMethod.invoke(skillManager, player, skillName);

            if (result) {
                getLogger().fine("玩家 " + player.getName() + " 触发技能: " + skillName);
                player.playSound(player.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 0.5f, 1.2f);
            } else {
                player.sendMessage(translateColors("&c技能 " + skillName + " 不存在或冷却中!"));
            }
        } catch (Exception e) {
            player.sendMessage(translateColors("&c技能触发失败!"));
            getLogger().severe("触发RPG技能失败: " + e.getMessage());
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

    private String translateColors(String text) {
        if (text == null) return "";
        return ChatColor.translateAlternateColorCodes('&', text);
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
                sender.sendMessage(translateColors("&c未知的命令! 使用 /gdtrigger help 查看帮助"));
                return true;
        }
    }

    private void sendHelp(CommandSender sender) {
        sender.sendMessage(translateColors("&6===== 光点物品触发插件 ====="));
        sender.sendMessage(translateColors("&e/gdtrigger reload &7- 重新加载配置"));
        sender.sendMessage(translateColors("&e/gdtrigger info &7- 显示插件信息"));
        sender.sendMessage(translateColors("&e/gdtrigger list &7- 列出所有触发器"));
        sender.sendMessage(translateColors("&e/gdtrigger help &7- 显示帮助信息"));
        sender.sendMessage(translateColors("&7作者: Gumin | QQ: 2271257344"));
    }

    private boolean handleReload(CommandSender sender) {
        if (!sender.hasPermission("guangdian.trigger.reload")) {
            sender.sendMessage(translateColors(config.getString("messages.no-permission", "&c你没有权限执行此操作!")));
            return true;
        }

        reloadConfig();
        config = getConfig();
        loadTriggers();

        sender.sendMessage(translateColors(config.getString("messages.config-reloaded", "&a配置已重新加载!")));
        return true;
    }

    private boolean handleInfo(CommandSender sender) {
        sender.sendMessage(translateColors("&6===== 光点物品触发插件信息 ====="));
        sender.sendMessage(translateColors("&e版本: &f" + getDescription().getVersion()));
        sender.sendMessage(translateColors("&e作者: &fGumin"));
        sender.sendMessage(translateColors("&eQQ: &f2271257344"));
        sender.sendMessage(translateColors("&e状态: &a已启用"));
        sender.sendMessage(translateColors("&e已加载触发器: &f" + triggers.size()));
        sender.sendMessage(translateColors("&e音效缓存: &f" + soundCache.size()));
        sender.sendMessage(translateColors("&e效果缓存: &f" + effectCache.size()));
        return true;
    }

    private boolean handleList(CommandSender sender) {
        sender.sendMessage(translateColors("&6===== 触发器列表 ====="));
        for (TriggerConfig trigger : triggers.values()) {
            sender.sendMessage(translateColors("&e" + trigger.getName() + " &7- 关键词: &f" + trigger.getLoreKeyword()));
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

    private static class TriggerConfig {
        private final String name;
        private final String triggerType;
        private final String loreKeyword;
        private final List<String> actions;
        private final int cooldown;
        private final boolean oneTime;

        public TriggerConfig(String name, String triggerType, String loreKeyword, List<String> actions, int cooldown, boolean oneTime) {
            this.name = name;
            this.triggerType = triggerType;
            this.loreKeyword = loreKeyword;
            this.actions = actions;
            this.cooldown = cooldown;
            this.oneTime = oneTime;
        }

        public String getName() { return name; }
        public String getTriggerType() { return triggerType; }
        public String getLoreKeyword() { return loreKeyword; }
        public List<String> getActions() { return actions; }
        public int getCooldown() { return cooldown; }
        public boolean isOneTime() { return oneTime; }
    }
}
