package cn.guangdian.armorstats.listener;

import cn.guangdian.armorstats.GuangDianArmorStats;
import cn.guangdian.armorstats.manager.HealthManager;
import cn.guangdian.armorstats.manager.IncrementalStatsManager;
import cn.guangdian.armorstats.manager.IncrementalStatsManager.Slot;
import com.destroystokyo.paper.event.player.PlayerArmorChangeEvent;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryAction;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.event.player.PlayerItemHeldEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerSwapHandItemsEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.Material;

/**
 * 增量属性更新监听器
 * 
 * 设计原则：
 * 1. 只监听变化的槽位
 * 2. 增量更新，不刷新全部
 * 3. 主线程同步执行
 */
public class IncrementalStatsListener implements Listener {

    private final GuangDianArmorStats plugin;
    private final IncrementalStatsManager statsManager;
    private final HealthManager healthManager;
    
    // 副手槽位索引
    private static final int OFFHAND_SLOT = 40;
    
    public IncrementalStatsListener(GuangDianArmorStats plugin, IncrementalStatsManager statsManager) {
        this.plugin = plugin;
        this.statsManager = statsManager;
        this.healthManager = plugin.getHealthManager();
    }
    
    // ==================== 玩家生命周期 ====================
    
    @EventHandler(priority = EventPriority.LOWEST)
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        
        // 立即应用生命值缩放（避免显示超多行）
        healthManager.applyHealthScaleImmediately(player);
        
        // 延迟 2 秒等待其他插件加载完成
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            if (player.isOnline()) {
                statsManager.onPlayerJoin(player);
            }
        }, 40L);
    }
    
    @EventHandler(priority = EventPriority.NORMAL)
    public void onPlayerQuit(PlayerQuitEvent event) {
        statsManager.onPlayerQuit(event.getPlayer());
    }
    
    // ==================== 装备槽位变化 ====================
    
    /**
     * Paper 装备事件
     * 
     * 触发时机：装备槽位实际变化后
     * 操作：只更新该槽位的属性
     */
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onArmorChange(PlayerArmorChangeEvent event) {
        Player player = event.getPlayer();
        
        if (!statsManager.isPlayerLoaded(player.getUniqueId())) {
            return;
        }
        
        // 确定槽位
        Slot slot = mapArmorSlot(event.getSlot());
        if (slot == null) {
            return;
        }
        
        plugin.getLogger().info("[装备变化] " + player.getName() + 
            " 槽位: " + slot.name() + 
            " 旧: " + itemName(event.getOldItem()) + 
            " 新: " + itemName(event.getNewItem()));
        
        // 立即更新该槽位（主线程）
        statsManager.onSlotChange(player, slot, event.getNewItem());
    }
    
    // ==================== 主手武器变化 ====================
    
    /**
     * 快捷栏切换
     * 
     * 触发时机：玩家按 1-9 切换
     * 操作：只更新主手武器属性
     * 注意：手持防具时不解析，防具只在装备栏生效
     */
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onItemHeld(PlayerItemHeldEvent event) {
        Player player = event.getPlayer();
        
        if (!statsManager.isPlayerLoaded(player.getUniqueId())) {
            return;
        }
        
        // 延迟 1 tick 等待物品栏更新
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            if (player.isOnline()) {
                ItemStack newWeapon = player.getInventory().getItemInMainHand();
                
                // 手持防具时不解析属性，防具只在装备栏生效
                if (isArmorType(newWeapon)) {
                    plugin.getLogger().info("[主手变化] " + player.getName() + 
                        " 手持防具，跳过解析: " + itemName(newWeapon));
                    // 传空，清除主手属性
                    statsManager.onWeaponChange(player, null);
                    return;
                }
                
                plugin.getLogger().info("[主手变化] " + player.getName() + 
                    " 新武器: " + itemName(newWeapon));
                statsManager.onWeaponChange(player, newWeapon);
            }
        }, 1L);
    }
    
    /**
     * 主手/副手交换
     * 注意：手持防具时不解析，防具只在装备栏生效
     */
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onSwapHandItems(PlayerSwapHandItemsEvent event) {
        Player player = event.getPlayer();
        
        if (!statsManager.isPlayerLoaded(player.getUniqueId())) {
            return;
        }
        
        // 延迟 1 tick 等待物品栏更新
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            if (player.isOnline()) {
                ItemStack newWeapon = player.getInventory().getItemInMainHand();
                ItemStack newOffHand = player.getInventory().getItemInOffHand();
                
                plugin.getLogger().info("[主手/副手交换] " + player.getName());
                
                // 主手：防具不解析
                if (isArmorType(newWeapon)) {
                    statsManager.onWeaponChange(player, null);
                } else {
                    statsManager.onWeaponChange(player, newWeapon);
                }
                
                // 副手：防具不解析
                if (isArmorType(newOffHand)) {
                    statsManager.onOffHandChange(player, null);
                } else {
                    statsManager.onOffHandChange(player, newOffHand);
                }
            }
        }, 1L);
    }
    
    // ==================== 副手变化 ====================
    
    /**
     * 监听物品栏点击，检测装备槽位变化
     * 
     * 关键：使用 event.getSlot() 而非 getRawSlot()
     * - getRawSlot() 返回视图坐标（生存背包中副手=45，热键栏=36-44）
     * - getSlot()    返回 PlayerInventory 内部索引（副手=40，热键栏=0-8）
     * 
     * 覆盖场景：
     * - 直接点击副手/当前手持槽位 → slot 索引匹配
     * - HOTBAR_SWAP（数字键 1-9）→ 可能影响主手
     * - SWAP_OFFHAND（F 键交换副手）→ 影响副手
     * - COLLECT_TO_CURSOR（双击收集）→ 可能从装备槽吸走
     * - Shift+点击 → 安全兜底
     */
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) return;
        if (!statsManager.isPlayerLoaded(player.getUniqueId())) return;
        
        // 只关注玩家自身物品栏
        if (event.getClickedInventory() == null || 
            !(event.getClickedInventory() instanceof PlayerInventory)) return;
        
        // ★ 使用 getSlot() 而非 getRawSlot()，获取 PlayerInventory 内部索引
        int slot = event.getSlot();
        int heldSlot = player.getInventory().getHeldItemSlot();
        InventoryAction action = event.getAction();
        
        boolean offhandMayChange = false;
        boolean mainHandMayChange = false;
        
        // 1. 直接操作副手槽位（PlayerInventory 索引 40）
        if (slot == OFFHAND_SLOT) {
            offhandMayChange = true;
        }
        
        // 2. 直接操作当前主手快捷栏槽位（PlayerInventory 索引 0-8）
        if (slot == heldSlot) {
            mainHandMayChange = true;
        }
        
        // 3. 数字键交换（HOTBAR_SWAP）
        //    悬停在任意槽位按 1-9，当数字键对应当前手持槽位时主手变化
        if (action == InventoryAction.HOTBAR_SWAP) {
            if (event.getHotbarButton() == heldSlot) {
                mainHandMayChange = true;
            }
        }
        
        // 4. F 键与槽位交换（SWAP_OFFHAND）
        //    悬停在任意背包槽位按 F → 该槽位与副手交换
        //    使用 name() 比较，兼容旧版 API 缺少该枚举常量
        if ("SWAP_OFFHAND".equals(action.name())) {
            offhandMayChange = true;
        }
        
        // 5. 双击收集同类物品（COLLECT_TO_CURSOR）
        //    可能从副手或主手槽位吸走物品到光标
        if (action == InventoryAction.COLLECT_TO_CURSOR) {
            offhandMayChange = true;
            mainHandMayChange = true;
        }
        
        // 6. Shift+点击：安全兜底
        if (event.isShiftClick()) {
            offhandMayChange = true;
            mainHandMayChange = true;
        }
        
        // 统一延迟 1 tick 检测
        if (offhandMayChange || mainHandMayChange) {
            final boolean checkOffhand = offhandMayChange;
            final boolean checkMainHand = mainHandMayChange;
            
            Bukkit.getScheduler().runTaskLater(plugin, () -> {
                if (!player.isOnline()) return;
                
                if (checkOffhand) {
                    ItemStack newOffHand = player.getInventory().getItemInOffHand();
                    if (isArmorType(newOffHand)) {
                        plugin.getLogger().info("[副手变化] " + player.getName() + 
                            " 副手持防具，跳过解析: " + itemName(newOffHand));
                        statsManager.onOffHandChange(player, null);
                    } else {
                        plugin.getLogger().info("[副手变化] " + player.getName() + 
                            " 新副手: " + itemName(newOffHand));
                        statsManager.onOffHandChange(player, newOffHand);
                    }
                }
                
                if (checkMainHand) {
                    ItemStack newWeapon = player.getInventory().getItemInMainHand();
                    if (isArmorType(newWeapon)) {
                        plugin.getLogger().info("[主手变化] " + player.getName() + 
                            " 手持防具，跳过解析: " + itemName(newWeapon));
                        statsManager.onWeaponChange(player, null);
                    } else {
                        plugin.getLogger().info("[主手变化] " + player.getName() + 
                            " 新武器: " + itemName(newWeapon));
                        statsManager.onWeaponChange(player, newWeapon);
                    }
                }
            }, 1L);
        }
    }
    
    // ==================== 物品栏拖拽 ====================
    
    /**
     * 监听物品栏拖拽，检测装备槽位变化
     * 
     * 拖拽事件无法通过 rawSlot 可靠判断装备槽位变化，原因：
     * - getInventorySlots() 返回视图坐标（生存背包中副手=45，热键栏=36-44）
     * - getInventory() 返回顶层背包（生存背包中为 CraftingInventory，非 PlayerInventory）
     * - 不同视图类型下 slot 编号完全不同
     * 
     * 策略：拖拽后统一延迟 1 tick 检测副手和主手（拖拽操作不频繁，开销可接受）
     */
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onInventoryDrag(InventoryDragEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) return;
        if (!statsManager.isPlayerLoaded(player.getUniqueId())) return;
        
        // ★ 修复：检测底层背包（PlayerInventory），而非顶层（CraftingInventory 等）
        if (!(event.getView().getBottomInventory() instanceof PlayerInventory)) return;
        
        // 拖拽后统一延迟检测（无法可靠预测哪些装备槽被涉及）
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            if (!player.isOnline()) return;
            
            // 检查副手
            ItemStack newOffHand = player.getInventory().getItemInOffHand();
            if (isArmorType(newOffHand)) {
                plugin.getLogger().info("[副手变化-拖拽] " + player.getName() + 
                    " 副手持防具，跳过解析: " + itemName(newOffHand));
                statsManager.onOffHandChange(player, null);
            } else {
                plugin.getLogger().info("[副手变化-拖拽] " + player.getName() + 
                    " 新副手: " + itemName(newOffHand));
                statsManager.onOffHandChange(player, newOffHand);
            }
            
            // 检查主手
            ItemStack newWeapon = player.getInventory().getItemInMainHand();
            if (isArmorType(newWeapon)) {
                plugin.getLogger().info("[主手变化-拖拽] " + player.getName() + 
                    " 手持防具，跳过解析: " + itemName(newWeapon));
                statsManager.onWeaponChange(player, null);
            } else {
                plugin.getLogger().info("[主手变化-拖拽] " + player.getName() + 
                    " 新武器: " + itemName(newWeapon));
                statsManager.onWeaponChange(player, newWeapon);
            }
        }, 1L);
    }
    
    // ==================== 辅助方法 ====================
    
    /**
     * 映射 Paper 装备槽位到内部槽位枚举
     */
    private Slot mapArmorSlot(org.bukkit.inventory.EquipmentSlot slot) {
        return switch (slot) {
            case HEAD -> Slot.HELMET;
            case CHEST -> Slot.CHESTPLATE;
            case LEGS -> Slot.LEGGINGS;
            case FEET -> Slot.BOOTS;
            case HAND -> Slot.MAIN_HAND;
            case OFF_HAND -> Slot.OFF_HAND;
            default -> null;
        };
    }
    
    private String itemName(ItemStack item) {
        return item == null || item.getType().isAir() ? "AIR" : item.getType().name();
    }
    
    /**
     * 判断物品是否是防具类型（头盔/胸甲/护腿/靴子）
     * 防具只在装备栏生效，手持时不解析属性
     */
    private boolean isArmorType(ItemStack item) {
        if (item == null || item.getType().isAir()) return false;
        Material type = item.getType();
        return type.name().endsWith("_HELMET") ||
               type.name().endsWith("_CHESTPLATE") ||
               type.name().endsWith("_LEGGINGS") ||
               type.name().endsWith("_BOOTS");
    }
}
