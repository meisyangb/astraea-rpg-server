package cn.guangdian.forge.listener;

import cn.guangdian.forge.GuangDianForge;
import cn.guangdian.forge.gui.ForgeGUI;
import cn.guangdian.forge.gui.RecipeSelectGUI;
import cn.guangdian.forge.model.ForgeRecipe;
import cn.guangdian.forge.model.PlayerForgeData;
import cn.guangdian.forge.util.ForgeUtil;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.*;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * GUI交互监听器 - 处理图纸选择和锻造两个界面
 * 
 * 常见锻造插件的实现要点：
 * 1. 使用 getView().getTopInventory() 获取打开的界面
 * 2. 使用 getClickedInventory() 区分点击的是哪个库存
 * 3. 使用 getRawSlot() 判断点击位置
 * 4. 使用 getAction() 判断操作类型（普通点击、SHIFT点击、拖拽等）
 */
public class ForgeListener implements Listener {
    private final GuangDianForge plugin;

    public ForgeListener(GuangDianForge plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) return;
        
        Inventory topInv = event.getView().getTopInventory();
        Inventory clickedInv = event.getClickedInventory();
        int rawSlot = event.getRawSlot();
        int topSize = topInv.getSize();
        InventoryAction action = event.getAction();
        
        // ===== 图纸选择界面 =====
        if (topInv.getHolder() instanceof RecipeSelectGUI selectGUI) {
            handleRecipeSelect(event, player, selectGUI, rawSlot, topSize);
            return;
        }
        
        // ===== 锻造界面 =====
        if (topInv.getHolder() instanceof ForgeGUI forgeGUI) {
            handleForge(event, player, forgeGUI, rawSlot, topSize, clickedInv, action);
            return;
        }
    }

    private void handleRecipeSelect(InventoryClickEvent event, Player player, 
                                    RecipeSelectGUI gui, int rawSlot, int topSize) {
        // 图纸选择界面禁止所有物品操作
        event.setCancelled(true);
        
        // 忽略底部背包的点击
        if (rawSlot >= topSize) return;
        
        // 点击图纸槽位
        if (gui.isRecipeSlot(rawSlot)) {
            ForgeRecipe recipe = gui.getRecipeAtSlot(rawSlot);
            if (recipe != null) {
                PlayerForgeData data = plugin.getPlayerDataManager().get(player.getUniqueId());
                if (data.getForgeLevel() < recipe.getRequiredForgeLevel()) {
                    player.sendMessage(Component.text("锻造等级不足! 需要: " + recipe.getRequiredForgeLevel(), NamedTextColor.RED));
                    return;
                }
                
                player.closeInventory();
                new ForgeGUI(plugin, player, recipe).open();
            }
            return;
        }
        
        // 导航按钮
        if (gui.isPrevButton(rawSlot)) {
            gui.prevPage();
        } else if (gui.isNextButton(rawSlot)) {
            gui.nextPage();
        }
    }

    private void handleForge(InventoryClickEvent event, Player player, ForgeGUI forgeGUI,
                             int rawSlot, int topSize, Inventory clickedInv, InventoryAction action) {
        
        // ===== 情况1：点击锻造界面（顶部） =====
        if (rawSlot < topSize) {
            // 材料槽 - 允许操作
            if (forgeGUI.isMaterialSlot(rawSlot)) {
                event.setCancelled(false);
                // 延迟刷新
                plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
                    if (player.isOnline()) {
                        forgeGUI.updateSuccessRate();
                    }
                }, 1L);
                return;
            }
            
            // 其他槽位禁止操作
            event.setCancelled(true);
            
            // 锻造按钮
            if (forgeGUI.isForgeButton(rawSlot)) {
                doForge(player, forgeGUI);
                return;
            }
            
            // 返回按钮
            if (forgeGUI.isBackButton(rawSlot)) {
                player.closeInventory();
                new RecipeSelectGUI(plugin, player).open();
            }
            return;
        }
        
        // ===== 情况2：点击玩家背包（底部） =====
        if (clickedInv == player.getInventory()) {
            // SHIFT+点击：尝试移动物品到锻造界面
            if (action == InventoryAction.MOVE_TO_OTHER_INVENTORY) {
                ItemStack clickedItem = event.getCurrentItem();
                if (clickedItem != null && clickedItem.getType() != Material.AIR) {
                    // 检查是否有空的材料槽
                    int[] materialSlots = forgeGUI.getMaterialSlots();
                    boolean canMove = false;
                    
                    for (int slot : materialSlots) {
                        ItemStack slotItem = forgeGUI.getInventory().getItem(slot);
                        if (slotItem == null || slotItem.getType() == Material.AIR) {
                            canMove = true;
                            break;
                        }
                        // 可以堆叠
                        if (slotItem.isSimilar(clickedItem) && slotItem.getAmount() < slotItem.getMaxStackSize()) {
                            canMove = true;
                            break;
                        }
                    }
                    
                    if (!canMove) {
                        event.setCancelled(true);
                        return;
                    }
                    
                    event.setCancelled(false);
                    // 延迟刷新
                    plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
                        if (player.isOnline()) {
                            forgeGUI.updateSuccessRate();
                        }
                    }, 1L);
                    return;
                }
            }
            
            // 普通点击背包 - 允许操作
            event.setCancelled(false);
        }
    }

    private void doForge(Player player, ForgeGUI gui) {
        ForgeRecipe recipe = gui.getRecipe();
        PlayerForgeData data = plugin.getPlayerDataManager().get(player.getUniqueId());
        
        // 检查锻造等级
        if (data.getForgeLevel() < recipe.getRequiredForgeLevel()) {
            player.sendMessage(Component.text("锻造等级不足!", NamedTextColor.RED));
            return;
        }
        
        // 检查经验消耗
        int expCost = plugin.getConfig().getInt("forge-exp-cost", 3);
        if (player.getLevel() < expCost) {
            player.sendMessage(Component.text("需要 " + expCost + " 级经验!", NamedTextColor.RED));
            return;
        }
        
        // 检查材料
        if (!gui.hasEnoughMaterials()) {
            player.sendMessage(Component.text("材料不足!", NamedTextColor.RED));
            for (Map.Entry<String, Integer> req : recipe.getIngredients().entrySet()) {
                int have = gui.getMaterialCount(req.getKey());
                if (have < req.getValue()) {
                    String displayName = recipe.getIngredientDisplayName(req.getKey());
                    String cleanName = displayName.replace("§d", "").replace("§e", "");
                    player.sendMessage(Component.text("缺少: " + cleanName + " x" + (req.getValue() - have), NamedTextColor.YELLOW));
                }
            }
            return;
        }
        
        // 消耗材料和经验
        gui.consumeMaterials();
        player.setLevel(player.getLevel() - expCost);
        
        // 计算成功率并执行
        double rate = ForgeUtil.calcSuccessRate(recipe, data, plugin.getConfig());
        boolean success = Math.random() < rate;
        
        if (success) {
            ItemStack result = ForgeUtil.buildResult(recipe, plugin);
            if (result == null) {
                // MythicMobs 物品获取失败，退还材料（理论上不应该发生）
                player.sendMessage(Component.text("锻造出错: 无法获取结果物品，请联系管理员!", NamedTextColor.RED));
                plugin.getLogger().warning("锻造失败: 无法获取 MythicMobs 物品 " + recipe.getResultMythicMobsItem() + "，玩家: " + player.getName());
                return;
            }
            player.getInventory().addItem(result);
            player.sendMessage(Component.text("锻造成功!", NamedTextColor.GREEN));
            player.getWorld().spawnParticle(Particle.HAPPY_VILLAGER, player.getLocation(), 20, 0.5, 1, 0.5);
            
            data.setSuccessForges(data.getSuccessForges() + 1);
            long expGain = plugin.getConfig().getLong("exp-on-success", 100);
            ForgeUtil.addExp(data, expGain, player, plugin);
        } else {
            player.sendMessage(Component.text("锻造失败，材料已消耗!", NamedTextColor.RED));
            player.getWorld().spawnParticle(Particle.SMOKE, player.getLocation(), 15, 0.3, 0.5, 0.3);
            
            long expGain = plugin.getConfig().getLong("exp-on-failure", 20);
            ForgeUtil.addExp(data, expGain, player, plugin);
        }
        
        data.setTotalForges(data.getTotalForges() + 1);
        plugin.getPlayerDataManager().save(data);
        
        // 刷新界面
        gui.updateSuccessRate();
        player.sendMessage(Component.text("成功率: " + (int)(rate * 100) + "%", NamedTextColor.AQUA));
    }

    @EventHandler
    public void onInventoryDrag(InventoryDragEvent event) {
        Inventory topInv = event.getView().getTopInventory();
        
        if (topInv.getHolder() instanceof ForgeGUI forgeGUI) {
            // 检查是否拖拽到非材料槽
            for (int slot : event.getRawSlots()) {
                if (slot < topInv.getSize() && !forgeGUI.isMaterialSlot(slot)) {
                    event.setCancelled(true);
                    return;
                }
            }
            // 只拖拽到材料槽，允许
        }
        
        if (topInv.getHolder() instanceof RecipeSelectGUI) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onInventoryClose(InventoryCloseEvent event) {
        if (event.getInventory().getHolder() instanceof ForgeGUI gui) {
            // 返还材料
            for (int slot : gui.getMaterialSlots()) {
                ItemStack item = event.getInventory().getItem(slot);
                if (item != null && item.getType() != Material.AIR) {
                    event.getPlayer().getInventory().addItem(item);
                }
            }
        }
    }
}