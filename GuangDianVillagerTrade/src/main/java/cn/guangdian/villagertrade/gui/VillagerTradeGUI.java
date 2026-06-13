package cn.guangdian.villagertrade.gui;

import cn.guangdian.villagertrade.GuangDianVillagerTrade;
import cn.guangdian.villagertrade.recipe.RecipeGroup;
import cn.guangdian.villagertrade.recipe.TradeIngredient;
import cn.guangdian.villagertrade.recipe.TradeRecipe;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryOpenEvent;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.event.inventory.TradeSelectEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.InventoryView;
import org.bukkit.inventory.Merchant;
import org.bukkit.inventory.MerchantRecipe;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import net.kyori.adventure.text.minimessage.MiniMessage;

/**
 * 村民交易界面管理器
 *
 * <p>使用原版村民交易界面实现兑换功能</p>
 */
public class VillagerTradeGUI implements Listener {

    private final GuangDianVillagerTrade plugin;
    
    // 记录玩家当前的交易界面
    private final Map<UUID, String> playerTradeRecipes = new HashMap<>();
    private final Map<UUID, String> playerRecipeGroups = new HashMap<>();
    private final Map<UUID, Merchant> playerMerchants = new HashMap<>();

    public VillagerTradeGUI(GuangDianVillagerTrade plugin) {
        this.plugin = plugin;
    }

    /**
     * 打开单个配方交易界面
     *
     * @param player 玩家
     * @param recipeName 配方名称
     */
    public void openTrade(Player player, String recipeName) {
        TradeRecipe recipe = plugin.getRecipeManager().getRecipe(recipeName);
        if (recipe == null) {
            player.sendMessage(plugin.red("找不到兑换配方"));
            return;
        }

        // 创建商人
        Merchant merchant = Bukkit.createMerchant(MiniMessage.miniMessage().deserialize(recipe.getDisplayName()));
        
        // 创建交易配方
        List<MerchantRecipe> merchantRecipes = createMerchantRecipes(recipe);
        merchant.setRecipes(merchantRecipes);
        
        // 打开交易界面
        player.openMerchant(merchant, true);
        
        // 记录玩家状态
        playerTradeRecipes.put(player.getUniqueId(), recipeName);
        playerMerchants.put(player.getUniqueId(), merchant);
        
        // 播放音效
        plugin.playClickSound(player);
    }

    /**
     * 打开配方组交易界面（多个兑换表）
     *
     * @param player 玩家
     * @param groupName 配方组名称
     */
    public void openRecipeGroup(Player player, String groupName) {
        RecipeGroup group = plugin.getRecipeManager().getRecipeGroup(groupName);
        if (group == null) {
            player.sendMessage(plugin.red("找不到兑换配方组"));
            return;
        }

        // 检查权限
        if (group.hasPermission() && !player.hasPermission(group.getPermission())) {
            player.sendMessage(plugin.red("你没有权限使用此兑换"));
            return;
        }

        // 创建商人
        Merchant merchant = Bukkit.createMerchant(MiniMessage.miniMessage().deserialize(group.getDisplayName()));
        
        // 创建所有子配方的交易
        List<MerchantRecipe> merchantRecipes = new ArrayList<>();
        for (TradeRecipe recipe : group.getRecipes()) {
            merchantRecipes.addAll(createMerchantRecipes(recipe));
        }
        merchant.setRecipes(merchantRecipes);
        
        // 打开交易界面
        player.openMerchant(merchant, true);
        
        // 记录玩家状态
        playerRecipeGroups.put(player.getUniqueId(), groupName);
        playerMerchants.put(player.getUniqueId(), merchant);
        
        // 播放音效
        plugin.playClickSound(player);
    }

    /**
     * 创建村民交易配方列表
     *
     * @param recipe 兑换配方
     * @return 村民交易配方列表
     */
    private List<MerchantRecipe> createMerchantRecipes(TradeRecipe recipe) {
        List<MerchantRecipe> merchantRecipes = new ArrayList<>();
        
        // 为每个输入材料创建一个交易配方
        List<TradeIngredient> inputs = recipe.getInputs();
        org.bukkit.inventory.ItemStack output = recipe.getOutput();
        
        if (inputs.isEmpty()) {
            // 如果没有输入材料，创建一个空配方的交易
            MerchantRecipe merchantRecipe = new MerchantRecipe(
                output.clone(),
                0,  // 已使用次数
                recipe.getMaxUses(),  // 最大使用次数
                recipe.isGiveExperience(),  // 是否给予经验
                recipe.getExperienceAmount(),  // 经验数量
                0.0f  // 价格倍数
            );
            // 添加一个空气作为输入（表示免费）
            merchantRecipe.addIngredient(new org.bukkit.inventory.ItemStack(Material.AIR));
            merchantRecipes.add(merchantRecipe);
        } else {
            // 创建一个包含所有输入材料的交易
            MerchantRecipe merchantRecipe = new MerchantRecipe(
                output.clone(),
                0,
                recipe.getMaxUses(),
                recipe.isGiveExperience(),
                recipe.getExperienceAmount(),
                0.0f
            );
            
            // 添加输入材料（最多2个）
            for (int i = 0; i < Math.min(inputs.size(), 2); i++) {
                TradeIngredient ingredient = inputs.get(i);
                org.bukkit.inventory.ItemStack displayItem = null;

                // 优先检查是否为RPGItems物品
                if (ingredient.hasRpgItem() && plugin.getRPGItemsHook() != null && plugin.getRPGItemsHook().isEnabled()) {
                    if (plugin.isGuiDebugEnabled()) {
                        plugin.getLogger().info("[GUI调试] 尝试获取输入材料RPGItems物品: " + ingredient.getRpgItem());
                    }
                    displayItem = plugin.getRPGItemsHook().getRPGItem(ingredient.getRpgItem(), ingredient.getAmount());
                    if (displayItem != null) {
                        if (plugin.isGuiDebugEnabled()) {
                            plugin.getLogger().info("[GUI调试] 成功获取RPGItems物品: " + ingredient.getRpgItem());
                        }
                    } else {
                        if (plugin.isGuiDebugEnabled()) {
                            plugin.getLogger().warning("[GUI调试] 获取RPGItems物品失败，使用默认显示: " + ingredient.getRpgItem());
                        }
                    }
                }

                // 如果不是RPGItems物品或获取失败，检查是否为MythicMobs物品
                if (displayItem == null && ingredient.hasMythicType() && plugin.getMythicItemManager().isMythicMobsEnabled()) {
                    if (plugin.isGuiDebugEnabled()) {
                        plugin.getLogger().info("[GUI调试] 尝试获取输入材料Mythic物品: " + ingredient.getMythicType());
                    }
                    displayItem = plugin.getMythicItemManager().getMythicItem(ingredient.getMythicType(), ingredient.getAmount());
                    if (displayItem != null) {
                        if (plugin.isGuiDebugEnabled()) {
                            plugin.getLogger().info("[GUI调试] 成功获取Mythic物品: " + ingredient.getMythicType() + ", lore行数: " + (displayItem.hasItemMeta() && displayItem.getItemMeta().hasLore() ? displayItem.getItemMeta().getLore().size() : 0));
                        }
                    } else {
                        if (plugin.isGuiDebugEnabled()) {
                            plugin.getLogger().warning("[GUI调试] 获取Mythic物品失败，使用默认显示: " + ingredient.getMythicType());
                        }
                    }
                }

                // 如果以上都不是或获取失败，使用默认显示物品
                if (displayItem == null) {
                    displayItem = ingredient.createDisplayItem();
                }

                merchantRecipe.addIngredient(displayItem);
            }
            
            merchantRecipes.add(merchantRecipe);
        }
        
        return merchantRecipes;
    }

    /**
     * 处理交易选择事件
     */
    @EventHandler(priority = EventPriority.HIGH)
    public void onTradeSelect(TradeSelectEvent event) {
        Player player = (Player) event.getWhoClicked();
        UUID playerUUID = player.getUniqueId();
        
        String recipeName = playerTradeRecipes.get(playerUUID);
        String groupName = playerRecipeGroups.get(playerUUID);
        if (recipeName == null && groupName == null) {
            return;
        }
        
        // 检查是否是插件打开的交易界面
        InventoryView view = event.getView();
        if (view.getType() != InventoryType.MERCHANT) {
            return;
        }
        
        plugin.playClickSound(player);
    }

    /**
     * 处理玩家退出事件 - 清理内存缓存
     */
    @EventHandler(priority = EventPriority.MONITOR)
    public void onPlayerQuit(PlayerQuitEvent event) {
        UUID playerUUID = event.getPlayer().getUniqueId();
        playerTradeRecipes.remove(playerUUID);
        playerRecipeGroups.remove(playerUUID);
        playerMerchants.remove(playerUUID);
        plugin.removePlayerOpenRecipe(playerUUID);
    }

    /**
     * 处理界面关闭事件
     */
    @EventHandler(priority = EventPriority.MONITOR)
    public void onInventoryClose(InventoryCloseEvent event) {
        if (!(event.getPlayer() instanceof Player player)) {
            return;
        }
        
        UUID playerUUID = player.getUniqueId();
        
        // 检查是否是交易界面
        if (event.getInventory().getType() == InventoryType.MERCHANT) {
            // 清理玩家状态
            playerTradeRecipes.remove(playerUUID);
            playerRecipeGroups.remove(playerUUID);
            playerMerchants.remove(playerUUID);
            plugin.removePlayerOpenRecipe(playerUUID);
        }
    }

    /**
     * 处理界面打开事件
     */
    @EventHandler(priority = EventPriority.MONITOR)
    public void onInventoryOpen(InventoryOpenEvent event) {
        if (!(event.getPlayer() instanceof Player player)) {
            return;
        }
        
        // 检查是否是交易界面
        if (event.getInventory().getType() == InventoryType.MERCHANT) {
            UUID playerUUID = player.getUniqueId();
            String recipeName = playerTradeRecipes.get(playerUUID);
            String groupName = playerRecipeGroups.get(playerUUID);
            
            if (recipeName != null) {
                plugin.getLogger().fine("玩家 " + player.getName() + " 打开了兑换界面: " + recipeName);
            } else if (groupName != null) {
                plugin.getLogger().fine("玩家 " + player.getName() + " 打开了配方组界面: " + groupName);
            }
        }
    }

    /**
     * 获取玩家当前的交易配方
     *
     * @param playerUUID 玩家UUID
     * @return 配方名称
     */
    public String getPlayerRecipe(UUID playerUUID) {
        return playerTradeRecipes.get(playerUUID);
    }

    /**
     * 获取玩家当前的配方组
     *
     * @param playerUUID 玩家UUID
     * @return 配方组名称
     */
    public String getPlayerRecipeGroup(UUID playerUUID) {
        return playerRecipeGroups.get(playerUUID);
    }

    /**
     * 检查玩家是否在使用兑换系统
     *
     * @param playerUUID 玩家UUID
     * @return 是否在使用
     */
    public boolean isPlayerTrading(UUID playerUUID) {
        return playerTradeRecipes.containsKey(playerUUID) || playerRecipeGroups.containsKey(playerUUID);
    }

    /**
     * 强制关闭玩家的交易界面
     *
     * @param player 玩家
     */
    public void forceCloseTrade(Player player) {
        UUID playerUUID = player.getUniqueId();
        
        if (playerTradeRecipes.containsKey(playerUUID) || playerRecipeGroups.containsKey(playerUUID)) {
            player.closeInventory();
            playerTradeRecipes.remove(playerUUID);
            playerRecipeGroups.remove(playerUUID);
            playerMerchants.remove(playerUUID);
        }
    }
}
