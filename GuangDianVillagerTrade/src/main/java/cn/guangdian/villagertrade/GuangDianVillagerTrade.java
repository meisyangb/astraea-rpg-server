package cn.guangdian.villagertrade;

import cn.guangdian.villagertrade.adapter.VillagerTradeServiceAdapter;
import cn.guangdian.villagertrade.command.VillagerTradeCommand;
import cn.guangdian.villagertrade.config.TradeConfig;
import cn.guangdian.villagertrade.gui.VillagerTradeGUI;
import cn.guangdian.villagertrade.hook.RPGItemsHook;
import cn.guangdian.villagertrade.mythic.MythicItemManager;
import cn.guangdian.villagertrade.recipe.RecipeManager;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.plugin.PluginManager;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class GuangDianVillagerTrade extends JavaPlugin {

    private static GuangDianVillagerTrade instance;

    private RecipeManager recipeManager;
    private VillagerTradeGUI tradeGUI;
    private TradeConfig tradeConfig;
    private VillagerTradeServiceAdapter serviceAdapter;
    private MythicItemManager mythicItemManager;
    private RPGItemsHook rpgItemsHook;
    private MiniMessage miniMessageService;
    private SoundService soundService;

    private final Map<UUID, String> playerOpenRecipes = new HashMap<>();

    @Override
    public void onEnable() {
        instance = this;

        saveDefaultConfig();
        saveDefaultRecipes();

        this.miniMessageService = MiniMessage.miniMessage();
        this.soundService = new SoundService();

        tradeConfig = new TradeConfig(this);

        mythicItemManager = new MythicItemManager(this);
        
        rpgItemsHook = new RPGItemsHook();
        rpgItemsHook.init();

        recipeManager = new RecipeManager(this);
        recipeManager.loadRecipes();

        tradeGUI = new VillagerTradeGUI(this);

        VillagerTradeCommand commandExecutor = new VillagerTradeCommand(this);
        if (getCommand("villagertrade") != null) {
            getCommand("villagertrade").setExecutor(commandExecutor);
            getCommand("villagertrade").setTabCompleter(commandExecutor);
        }
        if (getCommand("villagertradeadmin") != null) {
            getCommand("villagertradeadmin").setExecutor(commandExecutor);
            getCommand("villagertradeadmin").setTabCompleter(commandExecutor);
        }

        PluginManager pm = getServer().getPluginManager();
        pm.registerEvents(tradeGUI, this);

        serviceAdapter = new VillagerTradeServiceAdapter(this);

        getLogger().info("村民兑换系统已启动 已加载 " + recipeManager.getRecipeCount() + " 个配方");
    }

    @Override
    public void onDisable() {
        for (UUID playerUUID : playerOpenRecipes.keySet()) {
            Player player = Bukkit.getPlayer(playerUUID);
            if (player != null && player.isOnline()) {
                player.closeInventory();
            }
        }
        playerOpenRecipes.clear();

        if (serviceAdapter != null) {
            serviceAdapter.unregister();
            serviceAdapter = null;
        }

        getServer().getScheduler().cancelTasks(this);

        getLogger().info("村民兑换系统已关闭");
    }

    private void saveDefaultRecipes() {
        if (!new java.io.File(getDataFolder(), "recipes.yml").exists()) {
            saveResource("recipes.yml", false);
        }
    }

    public boolean openTradeGUI(Player player, String recipeName) {
        if (!recipeManager.hasRecipe(recipeName)) {
            player.sendMessage(miniMessageService.deserialize("<red>找不到兑换配方 " + recipeName + "</red>"));
            return false;
        }

        String permission = recipeManager.getRecipePermission(recipeName);
        if (permission != null && !player.hasPermission(permission)) {
            player.sendMessage(miniMessageService.deserialize("<red>你没有权限使用此兑换</red>"));
            playErrorSound(player);
            return false;
        }

        tradeGUI.openTrade(player, recipeName);
        playerOpenRecipes.put(player.getUniqueId(), recipeName);

        return true;
    }

    /**
     * 打开配方组交易界面
     *
     * @param player 玩家
     * @param groupName 配方组名称
     * @return 是否成功打开
     */
    public boolean openRecipeGroupGUI(Player player, String groupName) {
        if (!recipeManager.hasRecipeGroup(groupName)) {
            player.sendMessage(miniMessageService.deserialize("<red>找不到兑换配方组 " + groupName + "</red>"));
            return false;
        }

        tradeGUI.openRecipeGroup(player, groupName);
        playerOpenRecipes.put(player.getUniqueId(), "group:" + groupName);

        return true;
    }

    public String getPlayerOpenRecipe(UUID playerUUID) {
        return playerOpenRecipes.get(playerUUID);
    }

    public void removePlayerOpenRecipe(UUID playerUUID) {
        playerOpenRecipes.remove(playerUUID);
    }

    public void playSuccessSound(Player player) {
        player.getWorld().playSound(player, "minecraft:block.note_block.pling", 1.0f, 2.0f);
    }

    public void playErrorSound(Player player) {
        player.getWorld().playSound(player, "minecraft:block.note_block.bass", 1.0f, 0.5f);
    }

    public void playClickSound(Player player) {
        player.getWorld().playSound(player, "minecraft:ui.button.click", 0.5f, 1.0f);
    }

    public static GuangDianVillagerTrade getInstance() {
        return instance;
    }

    public RecipeManager getRecipeManager() {
        return recipeManager;
    }

    public VillagerTradeGUI getTradeGUI() {
        return tradeGUI;
    }

    public TradeConfig getTradeConfig() {
        return tradeConfig;
    }

    public MiniMessage getMiniMessage() {
        return miniMessageService;
    }

    public SoundService getSoundService() {
        return soundService;
    }

    public MythicItemManager getMythicItemManager() {
        return mythicItemManager;
    }

    public RPGItemsHook getRPGItemsHook() {
        return rpgItemsHook;
    }

    public Component red(String text) {
        return miniMessageService.deserialize("<red>" + text + "</red>");
    }

    public Component green(String text) {
        return miniMessageService.deserialize("<green>" + text + "</green>");
    }

    public Component yellow(String text) {
        return miniMessageService.deserialize("<yellow>" + text + "</yellow>");
    }

    public Component gray(String text) {
        return miniMessageService.deserialize("<gray>" + text + "</gray>");
    }

    public Component aqua(String text) {
        return miniMessageService.deserialize("<aqua>" + text + "</aqua>");
    }

    public String legacyColorize(String text) {
        return ChatColor.translateAlternateColorCodes('&', text);
    }

    public boolean isDebugEnabled() {
        return getConfig().getBoolean("settings.debug.enabled", false);
    }

    public boolean isGuiDebugEnabled() {
        return getConfig().getBoolean("settings.debug.gui-debug", false);
    }
}