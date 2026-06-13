package cn.guangdian.enhance.stone;

import cn.guangdian.enhance.GuangDianEnhance;
import cn.guangdian.rpgcore.util.TextStripper;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.Material;

import java.util.*;

public class EnhanceStoneManager {

    private static final NamespacedKey STONE_ID_KEY = new NamespacedKey("guangdianenhance", "stone_id");
    private static final NamespacedKey STONE_VALUE_KEY = new NamespacedKey("guangdianenhance", "stone_value");
    
    private final GuangDianEnhance plugin;
    private final Map<String, EnhanceStone> registeredStones = new HashMap<>();
    private final Map<StoneType, List<EnhanceStone>> stonesByType = new EnumMap<>(StoneType.class);
    
    public EnhanceStoneManager(GuangDianEnhance plugin) {
        this.plugin = plugin;
        
        for (StoneType type : StoneType.values()) {
            stonesByType.put(type, new ArrayList<>());
        }
        
        registerDefaultStones();
    }
    
    private void registerDefaultStones() {
        registerStone(new EnhanceStone(
            "material_quenched",
            StoneType.MATERIAL,
            1.0,
            1,
            true,
            createDisplayItem(Material.REDSTONE, "淬炼石", "强化装备的必需品")
        ));
        
        registerStone(new EnhanceStone(
            "lucky_stone",
            StoneType.SUCCESS_RATE,
            0.25,
            1,
            true,
            createDisplayItem(Material.LAPIS_LAZULI, "幸运石", "增加25%成功几率")
        ));
        
        registerStone(new EnhanceStone(
            "safety_stone",
            StoneType.SAFETY,
            1.0,
            1,
            true,
            createDisplayItem(Material.AMETHYST_SHARD, "安全石", "失败时不破碎")
        ));
        
        registerStone(new EnhanceStone(
            "protection_t1",
            StoneType.PROTECTION,
            1.0,
            1,
            true,
            createDisplayItem(Material.ECHO_SHARD, "保护石I", "防止降级1次")
        ));
        
        registerStone(new EnhanceStone(
            "protection_t2",
            StoneType.PROTECTION,
            2.0,
            2,
            true,
            createDisplayItem(Material.ECHO_SHARD, "保护石II", "防止降级2次")
        ));
        
        registerStone(new EnhanceStone(
            "guarantee_t1",
            StoneType.GUARANTEE,
            1.0,
            1,
            true,
            createDisplayItem(Material.NETHER_STAR, "必成石", "100%强化成功")
        ));
        
        registerStone(new EnhanceStone(
            "luck_t1",
            StoneType.LUCK,
            0.10,
            1,
            true,
            createDisplayItem(Material.COPPER_INGOT, "暴击石I", "+10%暴击伤害")
        ));
        
        plugin.getLogger().info("已注册 " + registeredStones.size() + " 种强化石");
    }
    
    private ItemStack createDisplayItem(Material material, String name, String desc) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        meta.displayName(cn.guangdian.rpgcore.message.MiniMessageService.getInstance()
            .colorize("<gold>" + name));
        List<net.kyori.adventure.text.Component> lore = new ArrayList<>();
        lore.add(cn.guangdian.rpgcore.message.MiniMessageService.getInstance()
            .colorize("<gray>" + desc));
        meta.lore(lore);
        item.setItemMeta(meta);
        return item;
    }
    
    public void registerStone(EnhanceStone stone) {
        registeredStones.put(stone.getId(), stone);
        stonesByType.get(stone.getType()).add(stone);
    }
    
    public EnhanceStone getStone(String id) {
        return registeredStones.get(id);
    }
    
    public Collection<EnhanceStone> getAllStones() {
        return Collections.unmodifiableCollection(registeredStones.values());
    }
    
    public List<EnhanceStone> getStonesByType(StoneType type) {
        return Collections.unmodifiableList(stonesByType.getOrDefault(type, Collections.emptyList()));
    }
    
    public EnhanceStone detectStone(ItemStack item) {
        if (item == null || !item.hasItemMeta()) {
            return null;
        }
        
        ItemMeta meta = item.getItemMeta();
        
        String stoneId = meta.getPersistentDataContainer()
            .get(STONE_ID_KEY, PersistentDataType.STRING);
        
        if (stoneId != null) {
            return registeredStones.get(stoneId);
        }
        
        return detectStoneByLore(item);
    }
    
    private EnhanceStone detectStoneByLore(ItemStack item) {
        if (!item.hasItemMeta()) return null;
        
        ItemMeta meta = item.getItemMeta();
        String displayName = meta.hasDisplayName() ? 
            TextStripper.stripAll(meta.displayName().toString()).toLowerCase() : "";
        
        if (displayName.contains("淬炼石")) {
            return getStone("material_quenched");
        }
        if (displayName.contains("幸运石")) {
            return getStone("lucky_stone");
        }
        if (displayName.contains("安全石")) {
            return getStone("safety_stone");
        }
        
        if (!meta.hasLore()) return null;
        
        List<String> lore = meta.getLore();
        if (lore == null) return null;
        
        for (String line : lore) {
            String stripped = TextStripper.stripAll(line).toLowerCase();
            
            if (stripped.contains("淬炼石")) {
                return getStone("material_quenched");
            }
            if (stripped.contains("幸运石")) {
                return getStone("lucky_stone");
            }
            if (stripped.contains("安全石")) {
                return getStone("safety_stone");
            }
            if (stripped.contains("必成石")) {
                return getStone("guarantee_t1");
            }
            if (stripped.contains("保护石")) {
                if (stripped.contains("ii") || stripped.contains("2")) {
                    return getStone("protection_t2");
                }
                return getStone("protection_t1");
            }
            if (stripped.contains("暴击石")) {
                return getStone("luck_t1");
            }
        }
        
        return null;
    }
    
    public ItemStack createStoneItem(String stoneId) {
        EnhanceStone stone = registeredStones.get(stoneId);
        if (stone == null) {
            return null;
        }
        
        ItemStack item = stone.getDisplayItem();
        if (item == null) {
            return null;
        }
        
        ItemMeta meta = item.getItemMeta();
        meta.getPersistentDataContainer()
            .set(STONE_ID_KEY, PersistentDataType.STRING, stoneId);
        meta.getPersistentDataContainer()
            .set(STONE_VALUE_KEY, PersistentDataType.DOUBLE, stone.getValue());
        
        List<net.kyori.adventure.text.Component> lore = new ArrayList<>();
        if (meta.hasLore() && meta.lore() != null) {
            lore.addAll(meta.lore());
        }
        lore.add(cn.guangdian.rpgcore.message.MiniMessageService.getInstance()
            .colorize("<gray>强化石ID: " + stoneId));
        meta.lore(lore);
        
        item.setItemMeta(meta);
        return item;
    }
    
    public boolean isStone(ItemStack item) {
        return detectStone(item) != null;
    }
    
    public boolean consumeStone(Player player, EnhanceStone stone) {
        return consumeStone(player, stone, 1);
    }
    
    public boolean consumeStone(org.bukkit.entity.Player player, EnhanceStone stone, int amount) {
        org.bukkit.inventory.PlayerInventory inv = player.getInventory();
        int remaining = amount;
        
        for (int i = 0; i < inv.getSize() && remaining > 0; i++) {
            ItemStack item = inv.getItem(i);
            if (item == null) continue;
            
            EnhanceStone detected = detectStone(item);
            if (detected != null && detected.getId().equals(stone.getId())) {
                int itemAmount = item.getAmount();
                if (itemAmount <= remaining) {
                    inv.setItem(i, null);
                    remaining -= itemAmount;
                } else {
                    item.setAmount(itemAmount - remaining);
                    remaining = 0;
                }
            }
        }
        
        player.updateInventory();
        return remaining == 0;
    }
    
    public int countStone(org.bukkit.entity.Player player, EnhanceStone stone) {
        org.bukkit.inventory.PlayerInventory inv = player.getInventory();
        int count = 0;
        
        for (ItemStack item : inv.getContents()) {
            if (item == null) continue;
            
            EnhanceStone detected = detectStone(item);
            if (detected != null && detected.getId().equals(stone.getId())) {
                count += item.getAmount();
            }
        }
        
        return count;
    }
    
    public Map<StoneType, EnhanceStone> detectStonesInInventory(org.bukkit.entity.Player player) {
        Map<StoneType, EnhanceStone> result = new EnumMap<>(StoneType.class);
        org.bukkit.inventory.PlayerInventory inv = player.getInventory();
        
        for (ItemStack item : inv.getContents()) {
            if (item == null) continue;
            
            EnhanceStone stone = detectStone(item);
            if (stone != null && !result.containsKey(stone.getType())) {
                result.put(stone.getType(), stone);
            }
        }
        
        return result;
    }
}
