package cn.guangdian.realm;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.NamespacedKey;

import java.util.ArrayList;
import java.util.List;

/**
 * 内丹管理器
 * 
 * 内丹是所有门派弟子都有的修炼结晶，固定在物品栏第9格
 * - 显示当前境界信息
 * - 显示修为进度
 * - 显示属性加成
 * - 不可移动、不可丢弃
 */
public class NeidanManager {
    private final GuangDianRealm plugin;
    private final NamespacedKey neidanKey;
    private final NamespacedKey ownerKey;
    
    public NeidanManager(GuangDianRealm plugin) {
        this.plugin = plugin;
        this.neidanKey = new NamespacedKey(plugin, "neidan_item");
        this.ownerKey = new NamespacedKey(plugin, "neidan_owner");
    }
    
    /**
     * 给玩家赋予内丹
     */
    public void giveNeidan(Player player) {
        ItemStack neidan = createNeidan(player);
        
        // 放入第9格（索引8）
        player.getInventory().setItem(8, neidan);
    }
    
    /**
     * 检查玩家是否有内丹
     */
    public boolean hasNeidan(Player player) {
        ItemStack item = player.getInventory().getItem(8);
        return isNeidan(item, player);
    }
    
    /**
     * 更新内丹显示（境界变化时调用）
     */
    public void updateNeidan(Player player) {
        if (!hasNeidan(player)) {
            giveNeidan(player);
            return;
        }
        
        ItemStack neidan = createNeidan(player);
        player.getInventory().setItem(8, neidan);
    }
    
    /**
     * 检查物品是否是内丹
     */
    public boolean isNeidan(ItemStack item, Player player) {
        if (item == null || item.getType() != Material.NETHER_STAR) {
            return false;
        }
        
        ItemMeta meta = item.getItemMeta();
        if (meta == null) return false;
        
        PersistentDataContainer pdc = meta.getPersistentDataContainer();
        if (!pdc.has(neidanKey, PersistentDataType.BYTE)) {
            return false;
        }
        
        // 检查是否属于该玩家
        String owner = pdc.get(ownerKey, PersistentDataType.STRING);
        return owner != null && owner.equals(player.getUniqueId().toString());
    }
    
    /**
     * 检查物品是否是内丹（不验证玩家）
     */
    public boolean isNeidanItem(ItemStack item) {
        if (item == null || item.getType() != Material.NETHER_STAR) {
            return false;
        }
        
        ItemMeta meta = item.getItemMeta();
        if (meta == null) return false;
        
        PersistentDataContainer pdc = meta.getPersistentDataContainer();
        return pdc.has(neidanKey, PersistentDataType.BYTE);
    }
    
    /**
     * 获取内丹所有者
     */
    public String getNeidanOwner(ItemStack item) {
        if (item == null || item.getType() != Material.NETHER_STAR) {
            return null;
        }
        
        ItemMeta meta = item.getItemMeta();
        if (meta == null) return null;
        
        PersistentDataContainer pdc = meta.getPersistentDataContainer();
        return pdc.get(ownerKey, PersistentDataType.STRING);
    }
    
    /**
     * 创建内丹物品
     */
    private ItemStack createNeidan(Player player) {
        ItemStack neidan = new ItemStack(Material.NETHER_STAR);
        ItemMeta meta = neidan.getItemMeta();
        
        // 设置显示名
        CultivationPlayer data = plugin.getPlayerData(player);
        Realm realm = plugin.getCurrentRealm(player);
        Realm nextRealm = plugin.getNextRealm(player);
        
        String realmName = realm != null ? realm.getName() : "凡人";
        String realmType = realm != null ? realm.getRealmTypeName() : "无";
        
        meta.displayName(Component.text("§6§l内丹 §f- " + realmName)
            .decoration(TextDecoration.ITALIC, false));
        
        // 设置描述
        List<Component> lore = new ArrayList<>();
        
        // 分隔线
        lore.add(Component.text("§7§m─────────────────────"));
        
        // 境界信息
        lore.add(Component.text("§e【境界信息】"));
        lore.add(Component.text("§f  当前境界: §b" + realmName));
        lore.add(Component.text("§f  境界类型: §7" + realmType));
        
        if (realm != null && realm.getStage() > 0) {
            lore.add(Component.text("§f  境界阶段: §7第" + realm.getStage() + "阶"));
        }
        
        // 分隔线
        lore.add(Component.text(""));
        lore.add(Component.text("§e【修炼进度】"));
        
        long currentCultivation = data.getCultivation();
        long requiredCultivation = nextRealm != null ? nextRealm.getRequiredCultivation() : 0;
        
        if (nextRealm != null) {
            String progress = String.format("%.1f", 
                (currentCultivation * 100.0 / requiredCultivation));
            lore.add(Component.text("§f  当前修为: §a" + formatNumber(currentCultivation)));
            lore.add(Component.empty()
                .append(Component.text("§f  下境需求: §c" + formatNumber(requiredCultivation)))
                .append(Component.text(" §8(" + progress + "%)")));
        } else {
            lore.add(Component.text("§f  当前修为: §a" + formatNumber(currentCultivation)));
            lore.add(Component.text("§f  下境需求: §d已至巅峰"));
        }
        
        // 分隔线
        lore.add(Component.text(""));
        lore.add(Component.text("§e【属性加成】"));
        
        if (realm != null) {
            Realm.RealmBonuses bonuses = realm.getBonuses();
            lore.add(Component.text("§f  生命加成: §c+" + bonuses.getMaxHealth()));
            lore.add(Component.text("§f  攻击加成: §4+" + bonuses.getAttackDamage()));
            lore.add(Component.text("§f  防御加成: §9+" + bonuses.getDefense()));
        } else {
            lore.add(Component.text("§7  暂无属性加成"));
        }
        
        // 分隔线
        lore.add(Component.text(""));
        lore.add(Component.text("§7§m─────────────────────"));
        
        // 提示
        lore.add(Component.text("§8  击杀怪物获得修为"));
        lore.add(Component.text("§8  使用 /realm 查看详情"));
        
        meta.lore(lore);
        
        // 设置不可破坏
        meta.setUnbreakable(true);
        
        // 存储NBT标记
        PersistentDataContainer pdc = meta.getPersistentDataContainer();
        pdc.set(neidanKey, PersistentDataType.BYTE, (byte) 1);
        pdc.set(ownerKey, PersistentDataType.STRING, player.getUniqueId().toString());
        
        neidan.setItemMeta(meta);
        return neidan;
    }
    
    /**
     * 格式化数字
     */
    private String formatNumber(long num) {
        if (num >= 1000000000) {
            return String.format("%.1f亿", num / 1000000000.0);
        } else if (num >= 10000) {
            return String.format("%.1f万", num / 10000.0);
        } else {
            return String.valueOf(num);
        }
    }
}