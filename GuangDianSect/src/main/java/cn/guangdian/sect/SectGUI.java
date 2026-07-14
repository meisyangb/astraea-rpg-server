package cn.guangdian.sect;

import cn.guangdian.rpgcore.message.UnifiedMessageService;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import net.kyori.adventure.text.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * 门派选择GUI
 */
public class SectGUI {
    private final GuangDianSect plugin;
    private final UnifiedMessageService msg;
    
    public SectGUI(GuangDianSect plugin) {
        this.plugin = plugin;
        this.msg = UnifiedMessageService.getInstance();
    }
    
    public void open(Player player) {
        FileConfiguration config = plugin.getConfig();
        String title = config.getString("settings.gui_title", "选择你的门派");
        Inventory inv = Bukkit.createInventory(null, 9, msg.colorize(title));
        
        int slot = 0;
        for (Sect sect : plugin.getAllSects()) {
            if (slot >= 9) break;
            
            ItemStack item = createSectItem(sect);
            inv.setItem(slot, item);
            slot++;
        }
        
        player.openInventory(inv);
    }
    
    private ItemStack createSectItem(Sect sect) {
        ItemStack item = new ItemStack(sect.getIcon());
        ItemMeta meta = item.getItemMeta();
        
        // 设置名称
        meta.displayName(msg.colorize(sect.getColor() + "§l" + sect.getName()));
        
        // 设置描述
        List<Component> lore = new ArrayList<>();
        lore.add(msg.colorize("<gray>类型: <white>" + sect.getType()));
        lore.add(msg.colorize(""));
        lore.add(msg.colorize("<gold>✦ 变强方式"));
        lore.add(msg.colorize("<yellow>  " + sect.getPowerDescription()));
        lore.add(msg.colorize(""));
        lore.add(msg.colorize("<gray>" + sect.getDescription()));
        lore.add(msg.colorize(""));
        lore.add(msg.colorize("<green>▶ 点击加入门派"));
        meta.lore(lore);
        
        item.setItemMeta(meta);
        return item;
    }
}