package cn.guangdian.market.gui;

import cn.guangdian.market.GuangDianMarket;
import cn.guangdian.market.GuangDianMarket.CurrencyType;
import cn.guangdian.market.GuangDianMarket.MarketItem;
import cn.guangdian.rpgcore.message.MiniMessageService;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.*;
import java.util.stream.Collectors;

/**
 * 工业级市场GUI
 * 支持分类筛选、搜索、排序功能
 */
public class MarketGUI implements InventoryHolder {

    // GUI常量
    private static final int SIZE = 54;
    private static final int[] ITEM_SLOTS = {10, 11, 12, 13, 14, 15, 16, 19, 20, 21, 22, 23, 24, 25, 28, 29, 30, 31, 32, 33, 34, 37, 38, 39, 40, 41, 42, 43};
    
    // 导航槽位
    private static final int PREV_PAGE_SLOT = 47;
    private static final int NEXT_PAGE_SLOT = 51;
    private static final int PAGE_INFO_SLOT = 49;
    private static final int SEARCH_SLOT = 45;
    private static final int SORT_SLOT = 46;
    private static final int FILTER_CURRENCY_SLOT = 48;
    private static final int MY_LISTINGS_SLOT = 50;
    private static final int BALANCE_SLOT = 52;
    private static final int REFRESH_SLOT = 53;
    
    // 分类槽位
    private static final int[] CATEGORY_SLOTS = {0, 1, 2, 3, 4, 5, 6, 7, 8};
    
    // 分类定义
    public enum Category {
        ALL("全部", Material.NETHER_STAR, null),
        WEAPONS("武器", Material.DIAMOND_SWORD, Arrays.asList(
            Material.WOODEN_SWORD, Material.STONE_SWORD, Material.IRON_SWORD, Material.GOLDEN_SWORD,
            Material.DIAMOND_SWORD, Material.NETHERITE_SWORD, Material.BOW, Material.CROSSBOW,
            Material.TRIDENT, Material.MACE
        )),
        ARMOR("防具", Material.DIAMOND_CHESTPLATE, Arrays.asList(
            Material.LEATHER_HELMET, Material.LEATHER_CHESTPLATE, Material.LEATHER_LEGGINGS, Material.LEATHER_BOOTS,
            Material.CHAINMAIL_HELMET, Material.CHAINMAIL_CHESTPLATE, Material.CHAINMAIL_LEGGINGS, Material.CHAINMAIL_BOOTS,
            Material.IRON_HELMET, Material.IRON_CHESTPLATE, Material.IRON_LEGGINGS, Material.IRON_BOOTS,
            Material.DIAMOND_HELMET, Material.DIAMOND_CHESTPLATE, Material.DIAMOND_LEGGINGS, Material.DIAMOND_BOOTS,
            Material.NETHERITE_HELMET, Material.NETHERITE_CHESTPLATE, Material.NETHERITE_LEGGINGS, Material.NETHERITE_BOOTS,
            Material.TURTLE_HELMET, Material.ELYTRA
        )),
        TOOLS("工具", Material.DIAMOND_PICKAXE, Arrays.asList(
            Material.WOODEN_PICKAXE, Material.STONE_PICKAXE, Material.IRON_PICKAXE, Material.GOLDEN_PICKAXE,
            Material.DIAMOND_PICKAXE, Material.NETHERITE_PICKAXE,
            Material.WOODEN_AXE, Material.STONE_AXE, Material.IRON_AXE, Material.GOLDEN_AXE,
            Material.DIAMOND_AXE, Material.NETHERITE_AXE,
            Material.WOODEN_SHOVEL, Material.STONE_SHOVEL, Material.IRON_SHOVEL, Material.GOLDEN_SHOVEL,
            Material.DIAMOND_SHOVEL, Material.NETHERITE_SHOVEL,
            Material.WOODEN_HOE, Material.STONE_HOE, Material.IRON_HOE, Material.GOLDEN_HOE,
            Material.DIAMOND_HOE, Material.NETHERITE_HOE,
            Material.FISHING_ROD, Material.FLINT_AND_STEEL, Material.COMPASS, Material.CLOCK, Material.SHEARS
        )),
        MATERIALS("材料", Material.DIAMOND, Arrays.asList(
            Material.DIAMOND, Material.EMERALD, Material.GOLD_INGOT, Material.IRON_INGOT,
            Material.NETHERITE_INGOT, Material.NETHERITE_SCRAP, Material.COPPER_INGOT,
            Material.COAL, Material.CHARCOAL, Material.REDSTONE, Material.LAPIS_LAZULI,
            Material.QUARTZ, Material.GLOWSTONE_DUST, Material.BLAZE_ROD, Material.BLAZE_POWDER,
            Material.ENDER_PEARL, Material.ENDER_EYE, Material.NETHER_STAR, Material.BEACON,
            Material.SLIME_BALL, Material.SLIME_BLOCK, Material.HONEYCOMB, Material.HONEY_BLOCK,
            Material.AMETHYST_SHARD, Material.ECHO_SHARD, Material.DISC_FRAGMENT_5
        )),
        FOOD("食物", Material.GOLDEN_APPLE, Arrays.asList(
            Material.APPLE, Material.GOLDEN_APPLE, Material.ENCHANTED_GOLDEN_APPLE,
            Material.BREAD, Material.COOKED_BEEF, Material.COOKED_PORKCHOP, Material.COOKED_CHICKEN,
            Material.COOKED_MUTTON, Material.COOKED_RABBIT, Material.COOKED_COD, Material.COOKED_SALMON,
            Material.PORKCHOP, Material.RABBIT_STEW, Material.MUSHROOM_STEW,
            Material.GOLDEN_CARROT, Material.BAKED_POTATO, Material.PUMPKIN_PIE, Material.CAKE
        )),
        POTIONS("药水", Material.POTION, Arrays.asList(
            Material.POTION, Material.SPLASH_POTION, Material.LINGERING_POTION,
            Material.TIPPED_ARROW, Material.EXPERIENCE_BOTTLE, Material.HONEY_BOTTLE
        )),
        ENCHANTED("附魔", Material.ENCHANTED_BOOK, Arrays.asList(
            Material.ENCHANTED_BOOK
        )),
        OTHER("其他", Material.CHEST, null);
        
        private final String displayName;
        private final Material icon;
        private final List<Material> materials;
        
        Category(String displayName, Material icon, List<Material> materials) {
            this.displayName = displayName;
            this.icon = icon;
            this.materials = materials;
        }
        
        public String getDisplayName() { return displayName; }
        public Material getIcon() { return icon; }
        public List<Material> getMaterials() { return materials; }
        
        public boolean matches(Material material) {
            if (materials == null) return true; // ALL
            return materials.contains(material);
        }
    }
    
    // 排序类型
    public enum SortType {
        NEWEST("最新上架", Material.CLOCK),
        OLDEST("最早上架", Material.REPEATER),
        PRICE_LOW("价格升序", Material.GOLD_NUGGET),
        PRICE_HIGH("价格降序", Material.GOLD_INGOT);
        
        private final String displayName;
        private final Material icon;
        
        SortType(String displayName, Material icon) {
            this.displayName = displayName;
            this.icon = icon;
        }
        
        public String getDisplayName() { return displayName; }
        public Material getIcon() { return icon; }
        
        public SortType next() {
            SortType[] values = values();
            return values[(ordinal() + 1) % values.length];
        }
    }
    
    // 货币筛选
    public enum CurrencyFilter {
        ALL("全部货币", Material.EMERALD),
        POINTS_ONLY("仅点券", Material.GOLD_NUGGET),
        ECONOMY_ONLY("仅金币", Material.GOLD_INGOT);
        
        private final String displayName;
        private final Material icon;
        
        CurrencyFilter(String displayName, Material icon) {
            this.displayName = displayName;
            this.icon = icon;
        }
        
        public String getDisplayName() { return displayName; }
        public Material getIcon() { return icon; }
        
        public CurrencyFilter next() {
            CurrencyFilter[] values = values();
            return values[(ordinal() + 1) % values.length];
        }
    }
    
    // 实例字段
    private final GuangDianMarket plugin;
    private final MiniMessageService miniMessage;
    private final Player player;
    private final Inventory inventory;
    
    private int currentPage = 1;
    private Category currentCategory = Category.ALL;
    private SortType currentSort = SortType.NEWEST;
    private CurrencyFilter currentCurrencyFilter = CurrencyFilter.ALL;
    private String searchQuery = "";
    
    private List<MarketItem> filteredItems = new ArrayList<>();
    
    public MarketGUI(GuangDianMarket plugin, Player player) {
        this.plugin = plugin;
        this.miniMessage = plugin.getMiniMessageService();
        this.player = player;
        this.inventory = Bukkit.createInventory(this, SIZE, miniMessage.colorize("<gold>全球市场"));
        refreshItems();
    }
    
    public void refreshItems() {
        filteredItems = filterAndSortItems();
        updateInventory();
    }
    
    private List<MarketItem> filterAndSortItems() {
        List<MarketItem> items = new ArrayList<>(plugin.getGlobalMarket());
        
        // 分类筛选
        if (currentCategory != Category.ALL && currentCategory != Category.OTHER) {
            items = items.stream()
                .filter(item -> currentCategory.matches(item.item.getType()))
                .collect(Collectors.toList());
        } else if (currentCategory == Category.OTHER) {
            // 其他分类：不属于任何已定义分类的物品
            items = items.stream()
                .filter(item -> {
                    for (Category cat : Category.values()) {
                        if (cat != Category.ALL && cat != Category.OTHER && cat.matches(item.item.getType())) {
                            return false;
                        }
                    }
                    return true;
                })
                .collect(Collectors.toList());
        }
        
        // 货币筛选
        if (currentCurrencyFilter == CurrencyFilter.POINTS_ONLY) {
            items = items.stream()
                .filter(item -> item.getCurrencyType() == CurrencyType.POINTS)
                .collect(Collectors.toList());
        } else if (currentCurrencyFilter == CurrencyFilter.ECONOMY_ONLY) {
            items = items.stream()
                .filter(item -> item.getCurrencyType() == CurrencyType.ECONOMY)
                .collect(Collectors.toList());
        }
        
        // 搜索筛选
        if (searchQuery != null && !searchQuery.isEmpty()) {
            String query = searchQuery.toLowerCase();
            items = items.stream()
                .filter(item -> {
                    String itemName = item.item.hasItemMeta() && item.item.getItemMeta().hasDisplayName() 
                        ? item.item.getItemMeta().getDisplayName().toLowerCase() 
                        : item.item.getType().name().toLowerCase();
                    return itemName.contains(query) || item.item.getType().name().toLowerCase().contains(query);
                })
                .collect(Collectors.toList());
        }
        
        // 排序
        switch (currentSort) {
            case NEWEST:
                items.sort((a, b) -> Long.compare(b.expireTime, a.expireTime));
                break;
            case OLDEST:
                items.sort((a, b) -> Long.compare(a.expireTime, b.expireTime));
                break;
            case PRICE_LOW:
                items.sort((a, b) -> Long.compare(a.price, b.price));
                break;
            case PRICE_HIGH:
                items.sort((a, b) -> Long.compare(b.price, a.price));
                break;
        }
        
        return items;
    }
    
    public void updateInventory() {
        inventory.clear();
        
        // 填充背景
        fillBackground();
        
        // 设置分类栏
        setCategoryBar();
        
        // 设置物品
        setItems();
        
        // 设置导航栏
        setNavigationBar();
    }
    
    private void fillBackground() {
        ItemStack glass = createItemComponent(Material.GRAY_STAINED_GLASS_PANE, " ");
        for (int i = 0; i < 9; i++) {
            inventory.setItem(i + 9, glass);
            inventory.setItem(i + 18, glass);
            inventory.setItem(i + 27, glass);
            inventory.setItem(i + 36, glass);
        }
    }
    
    private void setCategoryBar() {
        Category[] categories = Category.values();
        for (int i = 0; i < categories.length && i < CATEGORY_SLOTS.length; i++) {
            Category cat = categories[i];
            ItemStack item = new ItemStack(cat.getIcon());
            ItemMeta meta = item.getItemMeta();

            String prefix = cat == currentCategory ? "<green>✓ " : "<gray>";
            meta.displayName(miniMessage.colorize(prefix + cat.getDisplayName()));

            if (cat == currentCategory) {
                List<Component> lore = new ArrayList<>();
                lore.add(miniMessage.colorize("<yellow>当前选中"));
                meta.lore(lore);
            }

            item.setItemMeta(meta);
            inventory.setItem(CATEGORY_SLOTS[i], item);
        }
    }
    
    private void setItems() {
        int itemsPerPage = ITEM_SLOTS.length;
        int startIndex = (currentPage - 1) * itemsPerPage;
        
        for (int i = 0; i < itemsPerPage; i++) {
            int itemIndex = startIndex + i;
            if (itemIndex < filteredItems.size()) {
                MarketItem marketItem = filteredItems.get(itemIndex);
                inventory.setItem(ITEM_SLOTS[i], plugin.createMarketDisplayItem(marketItem));
            }
        }
    }
    
    private void setNavigationBar() {
        int itemsPerPage = ITEM_SLOTS.length;
        int totalPages = Math.max(1, (int) Math.ceil((double) filteredItems.size() / itemsPerPage));

        // 搜索按钮
        inventory.setItem(SEARCH_SLOT, createItemComponent(Material.OAK_SIGN,
            "<yellow>搜索",
            "<gray>当前: " + (searchQuery.isEmpty() ? "无" : searchQuery),
            "",
            "<aqua>点击输入搜索词"));

        // 排序按钮
        inventory.setItem(SORT_SLOT, createItemComponent(currentSort.getIcon(),
            "<yellow>排序: <white>" + currentSort.getDisplayName(),
            "",
            "<aqua>点击切换排序方式"));

        // 货币筛选按钮
        inventory.setItem(FILTER_CURRENCY_SLOT, createItemComponent(currentCurrencyFilter.getIcon(),
            "<yellow>货币: <white>" + currentCurrencyFilter.getDisplayName(),
            "",
            "<aqua>点击切换货币筛选"));

        // 上一页
        if (currentPage > 1) {
            inventory.setItem(PREV_PAGE_SLOT, createItemComponent(Material.ARROW, "<green>上一页"));
        }

        // 页码信息
        String pageInfo = "<gold>第 <white>" + currentPage +
                         "<gold>/<white>" + totalPages + "<gold> 页";
        inventory.setItem(PAGE_INFO_SLOT, createItemComponent(Material.BOOK, pageInfo,
            "<gray>共 " + filteredItems.size() + " 件商品"));

        // 下一页
        if (currentPage < totalPages) {
            inventory.setItem(NEXT_PAGE_SLOT, createItemComponent(Material.ARROW, "<green>下一页"));
        }

        // 我的上架
        int myCount = plugin.getPlayerListings().getOrDefault(player.getUniqueId(), new ArrayList<>()).size();
        inventory.setItem(MY_LISTINGS_SLOT, createItemComponent(Material.CHEST,
            "<gold>我的上架",
            "<gray>当前上架: " + myCount + " 件"));

        // 余额显示
        String pointsBalance = plugin.formatNumber(plugin.getPointsBalance(player.getUniqueId()));
        String economyBalance = plugin.getEconomy() != null
            ? plugin.getEconomy().format(plugin.getEconomyBalance(player.getUniqueId()))
            : "0";
        inventory.setItem(BALANCE_SLOT, createItemComponent(Material.GOLD_INGOT,
            "<gold>余额",
            "<yellow>点券: <white>" + pointsBalance,
            "<yellow>金币: <white>" + economyBalance));

        // 刷新按钮
        inventory.setItem(REFRESH_SLOT, createItemComponent(Material.CLOCK,
            "<yellow>刷新",
            "<gray>点击刷新列表"));
    }
    
    private ItemStack createItem(Material material, String name, String... lore) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName(name);
        if (lore.length > 0) {
            meta.setLore(Arrays.asList(lore));
        }
        item.setItemMeta(meta);
        return item;
    }

    private ItemStack createItemComponent(Material material, String name, String... lore) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        meta.displayName(miniMessage.colorize(name));
        if (lore.length > 0) {
            List<Component> loreComponents = new ArrayList<>();
            for (String line : lore) {
                if (!line.isEmpty()) {
                    loreComponents.add(miniMessage.colorize(line));
                } else {
                    loreComponents.add(Component.empty());
                }
            }
            meta.lore(loreComponents);
        }
        item.setItemMeta(meta);
        return item;
    }
    
    // 事件处理方法
    public void handleClick(int slot) {
        // 分类点击
        if (slot >= 0 && slot < 9) {
            Category[] categories = Category.values();
            if (slot < categories.length) {
                currentCategory = categories[slot];
                currentPage = 1;
                refreshItems();
            }
            return;
        }
        
        // 物品点击 - 检查是否是物品槽位
        for (int i = 0; i < ITEM_SLOTS.length; i++) {
            if (slot == ITEM_SLOTS[i]) {
                int itemIndex = (currentPage - 1) * ITEM_SLOTS.length + i;
                if (itemIndex < filteredItems.size()) {
                    MarketItem item = filteredItems.get(itemIndex);
                    plugin.purchaseItem(player, item);
                    refreshItems();
                }
                return;
            }
        }
        
        // 导航按钮点击
        switch (slot) {
            case SEARCH_SLOT:
                // 搜索功能需要通过聊天输入
                player.closeInventory();
                player.sendMessage(miniMessage.colorize("<yellow>请在聊天中输入搜索关键词，输入 'cancel' 取消"));
                plugin.setSearchMode(player, true);
                return;
                
            case SORT_SLOT:
                currentSort = currentSort.next();
                refreshItems();
                return;
                
            case FILTER_CURRENCY_SLOT:
                currentCurrencyFilter = currentCurrencyFilter.next();
                refreshItems();
                return;
                
            case PREV_PAGE_SLOT:
                if (currentPage > 1) {
                    currentPage--;
                    updateInventory();
                }
                return;
                
            case NEXT_PAGE_SLOT:
                int totalPages = Math.max(1, (int) Math.ceil((double) filteredItems.size() / ITEM_SLOTS.length));
                if (currentPage < totalPages) {
                    currentPage++;
                    updateInventory();
                }
                return;
                
            case MY_LISTINGS_SLOT:
                player.closeInventory();
                plugin.openMyListingsGUI(player);
                return;
                
            case REFRESH_SLOT:
                refreshItems();
                player.sendMessage(miniMessage.colorize("<green>已刷新市场列表"));
                return;
        }
    }
    
    public void setSearchQuery(String query) {
        this.searchQuery = query;
        this.currentPage = 1;
    }
    
    public String getSearchQuery() {
        return searchQuery;
    }
    
    @Override
    public Inventory getInventory() {
        return inventory;
    }
    
    public Player getPlayer() {
        return player;
    }
    
    public List<MarketItem> getFilteredItems() {
        return filteredItems;
    }
}