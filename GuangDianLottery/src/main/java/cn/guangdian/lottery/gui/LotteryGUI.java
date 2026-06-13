package cn.guangdian.lottery.gui;

import cn.guangdian.lottery.GuangDianLottery;
import cn.guangdian.lottery.model.LotteryPool;
import cn.guangdian.lottery.model.Prize;
import cn.guangdian.rpgcore.message.MiniMessageService;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class LotteryGUI implements Listener {

    private final GuangDianLottery plugin;
    private final MiniMessageService msg;
    private final Map<UUID, String> playerOpenPool = new HashMap<>();
    private final Map<UUID, Inventory> openInventories = new HashMap<>();
    
    public LotteryGUI(GuangDianLottery plugin) {
        this.plugin = plugin;
        this.msg = plugin.getMsg();
    }
    
    public void openMainMenu(Player player) {
        int size = Math.min(54, Math.max(9, ((plugin.getPools().size() / 9) + 1) * 9));
        Inventory inv = Bukkit.createInventory(new LotteryHolder("main"), size, 
            Component.text("抽奖系统"));
        
        int slot = 0;
        for (Map.Entry<String, LotteryPool> entry : plugin.getPools().entrySet()) {
            if (slot >= size) break;
            
            LotteryPool pool = entry.getValue();
            ItemStack item = createPoolIcon(pool, player);
            inv.setItem(slot++, item);
        }
        
        player.openInventory(inv);
        openInventories.put(player.getUniqueId(), inv);
    }
    
    public void openPoolMenu(Player player, String poolId) {
        LotteryPool pool = plugin.getPools().get(poolId);
        if (pool == null) {
            player.sendMessage(msg.colorize("<red>抽奖池不存在!"));
            return;
        }
        
        int size = 54;
        Inventory inv = Bukkit.createInventory(new LotteryHolder("pool:" + poolId), size,
            Component.text(pool.getDisplayName()));
        
        ItemStack drawItem = new ItemStack(Material.NETHER_STAR);
        ItemMeta drawMeta = drawItem.getItemMeta();
        drawMeta.displayName(msg.colorize("<green>点击抽奖"));
        List<Component> drawLore = new ArrayList<>();
        drawLore.add(Component.empty());
        drawLore.add(msg.colorize("<gray>花费: <yellow>" + pool.getCost() + " " + pool.getCurrencyType()));
        
        if (plugin.isOnCooldown(player.getUniqueId(), poolId)) {
            long remaining = plugin.getRemainingCooldown(player.getUniqueId(), poolId);
            int seconds = (int) (remaining / 1000);
            drawLore.add(msg.colorize("<red>冷却中: " + seconds + " 秒"));
        } else {
            drawLore.add(msg.colorize("<green>可以抽奖!"));
        }
        
        drawLore.add(Component.empty());
        drawMeta.lore(drawLore);
        drawItem.setItemMeta(drawMeta);
        inv.setItem(4, drawItem);
        
        int[] displaySlots = {10, 11, 12, 13, 14, 15, 16, 19, 20, 21, 22, 23, 24, 25, 28, 29, 30, 31, 32, 33, 34};
        int slotIndex = 0;
        
        for (Prize prize : pool.getPrizes()) {
            if (slotIndex >= displaySlots.length) break;
            
            ItemStack prizeItem = createPrizeIcon(prize, pool);
            inv.setItem(displaySlots[slotIndex++], prizeItem);
        }
        
        ItemStack backItem = new ItemStack(Material.ARROW);
        ItemMeta backMeta = backItem.getItemMeta();
        backMeta.displayName(msg.colorize("<yellow>返回"));
        backItem.setItemMeta(backMeta);
        inv.setItem(49, backItem);
        
        player.openInventory(inv);
        openInventories.put(player.getUniqueId(), inv);
        playerOpenPool.put(player.getUniqueId(), poolId);
    }
    
    private ItemStack createPoolIcon(LotteryPool pool, Player player) {
        ItemStack item = new ItemStack(pool.getIconMaterial());
        ItemMeta meta = item.getItemMeta();
        
        meta.displayName(msg.colorize(pool.getDisplayName()));
        
        List<Component> lore = new ArrayList<>();
        lore.add(Component.empty());
        lore.add(msg.colorize("<gray>花费: <yellow>" + pool.getCost() + " " + pool.getCurrencyType()));
        lore.add(msg.colorize("<gray>奖品数量: <white>" + pool.getPrizes().size()));
        
        if (!pool.getPermission().isEmpty() && !player.hasPermission(pool.getPermission())) {
            lore.add(msg.colorize("<red>需要权限!"));
        } else if (plugin.isOnCooldown(player.getUniqueId(), pool.getId())) {
            long remaining = plugin.getRemainingCooldown(player.getUniqueId(), pool.getId());
            int seconds = (int) (remaining / 1000);
            lore.add(msg.colorize("<yellow>冷却中: " + seconds + " 秒"));
        } else {
            lore.add(msg.colorize("<green>可以抽奖!"));
        }
        
        lore.add(Component.empty());
        lore.add(msg.colorize("<yellow>点击查看详情"));
        
        meta.lore(lore);
        
        if (pool.getIconCustomModelData() > 0) {
            meta.setCustomModelData(pool.getIconCustomModelData());
        }
        
        item.setItemMeta(meta);
        return item;
    }
    
    private ItemStack createPrizeIcon(Prize prize, LotteryPool pool) {
        ItemStack item = new ItemStack(prize.getMaterial());
        ItemMeta meta = item.getItemMeta();
        
        meta.displayName(msg.colorize(prize.getRarityColor() + prize.getDisplayName()));
        
        double chance = (prize.getWeight() / pool.getTotalWeight()) * 100;
        
        List<Component> lore = new ArrayList<>();
        lore.add(Component.empty());
        lore.add(msg.colorize("<gray>数量: <white>" + prize.getAmount()));
        lore.add(msg.colorize("<gray>概率: <yellow>" + String.format("%.2f", chance) + "%"));
        if (prize.isRare()) {
            lore.add(msg.colorize("<gold>★ 稀有奖品 ★"));
        }
        
        meta.lore(lore);
        
        if (prize.getCustomModelData() > 0) {
            meta.setCustomModelData(prize.getCustomModelData());
        }
        
        item.setItemMeta(meta);
        return item;
    }
    
    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getInventory().getHolder() instanceof LotteryHolder)) return;
        if (!(event.getWhoClicked() instanceof Player)) return;
        
        event.setCancelled(true);
        
        Player player = (Player) event.getWhoClicked();
        LotteryHolder holder = (LotteryHolder) event.getInventory().getHolder();
        int slot = event.getRawSlot();
        Inventory inventory = event.getInventory();
        
        if (holder.getType().equals("main")) {
            handleMainClick(player, slot, inventory);
        } else if (holder.getType().startsWith("pool:")) {
            String poolId = holder.getType().substring(5);
            handlePoolClick(player, poolId, slot);
        }
    }
    
    private void handleMainClick(Player player, int slot, Inventory inventory) {
        if (slot < 0 || slot >= inventory.getSize()) return;
        
        ItemStack item = inventory.getItem(slot);
        if (item == null || item.getType() == Material.AIR) return;
        
        int index = 0;
        for (String poolId : plugin.getPools().keySet()) {
            if (index == slot) {
                openPoolMenu(player, poolId);
                player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 1.0f, 1.0f);
                return;
            }
            index++;
        }
    }
    
    private void handlePoolClick(Player player, String poolId, int slot) {
        if (slot == 4) {
            handleDraw(player, poolId);
        } else if (slot == 49) {
            openMainMenu(player);
            player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 1.0f, 1.0f);
        }
    }
    
    private void handleDraw(Player player, String poolId) {
        LotteryPool pool = plugin.getPools().get(poolId);
        if (pool == null) {
            player.sendMessage(msg.colorize("<red>抽奖池不存在!"));
            return;
        }
        
        if (!plugin.canDraw(player, poolId)) {
            if (!pool.getPermission().isEmpty() && !player.hasPermission(pool.getPermission())) {
                player.sendMessage(msg.colorize("<red>你没有权限使用此抽奖池!"));
            } else {
                long remaining = plugin.getRemainingCooldown(player.getUniqueId(), poolId);
                int seconds = (int) (remaining / 1000);
                player.sendMessage(msg.colorize("<red>冷却中，请等待 " + seconds + " 秒!"));
            }
            return;
        }
        
        if (!plugin.chargeCurrency(player, pool.getCurrencyType(), pool.getCost())) {
            player.sendMessage(msg.colorize("<red>货币不足! 需要 " + pool.getCost() + " " + pool.getCurrencyType()));
            return;
        }
        
        Prize prize = plugin.drawPrize(poolId);
        if (prize == null) {
            player.sendMessage(msg.colorize("<red>抽奖失败，请联系管理员!"));
            return;
        }
        
        plugin.setCooldown(player.getUniqueId(), poolId);
        plugin.givePrize(player, prize);
        plugin.addToHistory(player.getUniqueId(), prize.getDisplayName());
        
        player.sendMessage(msg.colorize("<green>恭喜你获得了 " + prize.getRarityColor() + prize.getDisplayName() + "<green>!"));
        
        openPoolMenu(player, poolId);
    }
    
    @EventHandler
    public void onInventoryClose(InventoryCloseEvent event) {
        if (!(event.getPlayer() instanceof Player)) return;
        
        Player player = (Player) event.getPlayer();
        openInventories.remove(player.getUniqueId());
        playerOpenPool.remove(player.getUniqueId());
    }
    
    public void closeAll() {
        for (Map.Entry<UUID, Inventory> entry : openInventories.entrySet()) {
            Player player = Bukkit.getPlayer(entry.getKey());
            if (player != null) {
                player.closeInventory();
            }
        }
        openInventories.clear();
        playerOpenPool.clear();
    }
    
    public static class LotteryHolder implements InventoryHolder {
        private final String type;
        
        public LotteryHolder(String type) {
            this.type = type;
        }
        
        public String getType() {
            return type;
        }
        
        @Override
        public Inventory getInventory() {
            return null;
        }
    }
}
