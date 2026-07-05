package cn.guangdian.customgui.gui;

import cn.guangdian.customgui.GuangDianCustomGUI;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import net.kyori.adventure.text.minimessage.MiniMessage;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class CustomBackpackGUI implements Listener, InventoryHolder {

    private final GuangDianCustomGUI plugin;
    private final Map<Player, Inventory> openInventories = new HashMap<>();

    // 背包配置
    private String title;
    private int size;
    private int backpackSlots; // 背包可用槽位数
    private int modelData; // 自定义材质ID

    public CustomBackpackGUI(GuangDianCustomGUI plugin) {
        this.plugin = plugin;
        loadConfig();
        plugin.getServer().getPluginManager().registerEvents(this, plugin);
    }

    /**
     * 从配置加载背包设置
     */
    private void loadConfig() {
        var config = plugin.getConfig().getConfigurationSection("custom-backpack");
        if (config == null) {
            // 使用默认配置
            title = "<gold><bold>自定义背包";
            size = 54;
            backpackSlots = 45; // 5行背包区域
            modelData = 10001;
            return;
        }

        title = config.getString("title", "<gold><bold>自定义背包");
        size = config.getInt("size", 54);
        backpackSlots = config.getInt("backpack-slots", 45);
        modelData = config.getInt("model-data", 10001);
    }

    /**
     * 打开自定义背包界面
     */
    public void open(Player player) {
        Inventory inventory = Bukkit.createInventory(this, size, Component.text(title));

        // 填充背包区域
        fillBackpackArea(inventory, player);

        // 填充装饰区域
        fillDecorationArea(inventory);

        // 保存打开的背包
        openInventories.put(player, inventory);

        // 打开界面
        player.openInventory(inventory);
    }

    /**
     * 填充背包区域（玩家可交互）
     */
    private void fillBackpackArea(Inventory inventory, Player player) {
        // 前 backpackSlots 个槽位作为背包区域
        for (int i = 0; i < backpackSlots && i < size; i++) {
            // 这里可以添加自定义背景材质
            // 目前使用透明占位
            ItemStack placeholder = createPlaceholderItem();
            inventory.setItem(i, placeholder);
        }
    }

    /**
     * 填充装饰区域（不可交互）
     */
    private void fillDecorationArea(Inventory inventory) {
        // 最后一行作为装饰/快捷操作区域
        for (int i = backpackSlots; i < size; i++) {
            ItemStack decoration = createDecorationItem(i);
            inventory.setItem(i, decoration);
        }
    }

    /**
     * 创建占位物品（背包槽位）
     */
    private ItemStack createPlaceholderItem() {
        ItemStack item = new ItemStack(Material.GRAY_STAINED_GLASS_PANE);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.displayName(Component.text(" "));
            meta.setCustomModelData(modelData); // 使用自定义材质
            item.setItemMeta(meta);
        }
        return item;
    }

    /**
     * 创建装饰物品
     */
    private ItemStack createDecorationItem(int slot) {
        ItemStack item = new ItemStack(Material.BLACK_STAINED_GLASS_PANE);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.displayName(Component.text(" "));
            meta.setCustomModelData(modelData + 1); // 装饰材质
            item.setItemMeta(meta);
        }
        return item;
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getInventory().getHolder() instanceof CustomBackpackGUI)) {
            return;
        }

        Player player = (Player) event.getWhoClicked();
        int slot = event.getSlot();

        // 背包区域允许交互
        if (slot < backpackSlots) {
            return; // 允许正常背包操作
        }

        // 装饰区域禁止交互
        event.setCancelled(true);

        // 处理快捷操作按钮点击
        handleButtonClick(player, slot);
    }

    @EventHandler
    public void onInventoryClose(InventoryCloseEvent event) {
        if (!(event.getInventory().getHolder() instanceof CustomBackpackGUI)) {
            return;
        }

        Player player = (Player) event.getPlayer();
        openInventories.remove(player);
    }

    /**
     * 处理快捷操作按钮点击
     */
    private void handleButtonClick(Player player, int slot) {
        // 可以根据槽位添加不同的快捷操作
        // 例如：整理背包、关闭背包等
    }

    @Override
    public Inventory getInventory() {
        return null;
    }

    public int getModelData() {
        return modelData;
    }

    public int getBackpackSlots() {
        return backpackSlots;
    }
}
