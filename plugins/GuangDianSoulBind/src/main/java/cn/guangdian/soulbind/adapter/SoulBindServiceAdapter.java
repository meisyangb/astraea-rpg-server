package cn.guangdian.soulbind.adapter;

import cn.guangdian.rpgcore.RPGCore;
import cn.guangdian.soulbind.GuangDianSoulBind;
import cn.guangdian.soulbind.api.SoulBindService;
import cn.guangdian.soulbind.manager.ConfigManager;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.Bukkit;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class SoulBindServiceAdapter implements SoulBindService {

    private static final NamespacedKey BIND_KEY = new NamespacedKey("guangdian", "soulbind");
    private static final NamespacedKey BIND_NAME_KEY = new NamespacedKey("guangdian", "soulbind_name");

    private final GuangDianSoulBind plugin;

    public SoulBindServiceAdapter(GuangDianSoulBind plugin) {
        this.plugin = plugin;
        RPGCore rpgCore = RPGCore.getInstance();
        if (rpgCore != null) {
            rpgCore.getServiceRegistry().registerService(SoulBindService.class, this);
        }
    }

    public void unregister() {
        RPGCore rpgCore = RPGCore.getInstance();
        if (rpgCore != null) {
            rpgCore.getServiceRegistry().unregisterService(SoulBindService.class);
        }
    }

    @Override
    public boolean isBound(ItemStack item) {
        if (item == null || !item.hasItemMeta()) return false;
        return item.getItemMeta().getPersistentDataContainer().has(BIND_KEY, PersistentDataType.STRING);
    }

    @Override
    public boolean isBoundTo(ItemStack item, UUID playerId) {
        if (!isBound(item)) return false;
        String boundId = item.getItemMeta().getPersistentDataContainer().get(BIND_KEY, PersistentDataType.STRING);
        return playerId.toString().equals(boundId);
    }

    @Override
    public UUID getBoundPlayer(ItemStack item) {
        if (!isBound(item)) return null;
        String boundId = item.getItemMeta().getPersistentDataContainer().get(BIND_KEY, PersistentDataType.STRING);
        try {
            return UUID.fromString(boundId);
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    @Override
    public String getBoundPlayerName(ItemStack item) {
        if (!isBound(item)) return null;
        ItemMeta meta = item.getItemMeta();
        String name = meta.getPersistentDataContainer().get(BIND_NAME_KEY, PersistentDataType.STRING);
        if (name != null) return name;
        UUID uuid = getBoundPlayer(item);
        if (uuid != null) {
            Player player = Bukkit.getPlayer(uuid);
            if (player != null) return player.getName();
        }
        return "未知";
    }

    @Override
    public boolean bindItem(ItemStack item, Player player) {
        if (item == null || player == null) return false;
        if (isBound(item)) return false;

        ItemMeta meta = item.getItemMeta();
        meta.getPersistentDataContainer().set(BIND_KEY, PersistentDataType.STRING, player.getUniqueId().toString());
        meta.getPersistentDataContainer().set(BIND_NAME_KEY, PersistentDataType.STRING, player.getName());

        ConfigManager config = plugin.getConfigManager();
        String format = config.getLoreFormat();
        String loreText = format.replace("%player%", player.getName());
        Component loreComponent = LegacyComponentSerializer.legacyAmpersand().deserialize(loreText);

        List<Component> lore = meta.lore();
        if (lore == null) lore = new ArrayList<>();
        lore.add(loreComponent);
        meta.lore(lore);

        item.setItemMeta(meta);
        return true;
    }

    @Override
    public boolean unbindItem(ItemStack item) {
        if (item == null || !isBound(item)) return false;

        ItemMeta meta = item.getItemMeta();
        meta.getPersistentDataContainer().remove(BIND_KEY);
        meta.getPersistentDataContainer().remove(BIND_NAME_KEY);

        List<Component> lore = meta.lore();
        if (lore != null && !lore.isEmpty()) {
            ConfigManager config = plugin.getConfigManager();
            String format = config.getLoreFormat();
            String prefix = format.split("%player%")[0];
            Component prefixComponent = LegacyComponentSerializer.legacyAmpersand().deserialize(prefix);
            
            lore.removeIf(component -> {
                String text = LegacyComponentSerializer.legacySection().serialize(component);
                return text.contains("灵魂绑定") || text.contains("soulbind");
            });
            meta.lore(lore);
        }

        item.setItemMeta(meta);
        return true;
    }

    @Override
    public boolean canInteract(ItemStack item, Player player) {
        if (!isBound(item)) return true;
        return isBoundTo(item, player.getUniqueId());
    }

    @Override
    public boolean isMythicMobsItem(ItemStack item) {
        return plugin.getMythicMobsHook().isMythicItem(item);
    }
}
