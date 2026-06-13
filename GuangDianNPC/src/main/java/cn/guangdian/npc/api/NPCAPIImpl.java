package cn.guangdian.npc.api;

import cn.guangdian.npc.GuangDianNPC;
import cn.guangdian.npc.manager.NPCManager;
import cn.guangdian.npc.model.NPCData;
import cn.guangdian.rpgcore.message.MiniMessageService;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.*;
import java.util.stream.Collectors;

public class NPCAPIImpl implements NPCAPI {

    private final GuangDianNPC plugin;
    private final NPCManager npcManager;

    public NPCAPIImpl(GuangDianNPC plugin) {
        this.plugin = plugin;
        this.npcManager = plugin.getNpcManager();
    }

    @Override
    public Optional<NPCData> getNPC(String id) {
        return Optional.ofNullable(npcManager.getNPC(id));
    }

    @Override
    public Collection<NPCData> getAllNPCs() {
        return npcManager.getAllNPCs();
    }

    @Override
    public int getNPCCount() {
        return npcManager.getNPCCount();
    }

    @Override
    public NPCData createNPC(String id, Location location, String menuId) {
        if (location == null || location.getWorld() == null) {
            return null;
        }
        String lowerId = id.toLowerCase();
        if (npcManager.getNPC(lowerId) != null) {
            return null;
        }

        NPCData npc = new NPCData(lowerId, "<yellow>" + id, location, menuId != null ? menuId : "main");
        npcManager.getNPCs().put(lowerId, npc);
        npcManager.spawnNPC(npc);
        npcManager.save();

        return npc;
    }

    @Override
    public boolean removeNPC(String id) {
        return npcManager.removeNPC(id);
    }

    @Override
    public boolean exists(String id) {
        return npcManager.getNPC(id) != null;
    }

    @Override
    public void openMenu(Player player, String menuId) {
        // 优先使用 GuangDianMenu 插件
        org.bukkit.plugin.Plugin menuPlugin = Bukkit.getPluginManager().getPlugin("GuangDianMenu");
        if (menuPlugin != null && menuPlugin.isEnabled()) {
            try {
                // 使用命令打开菜单
                player.performCommand("menu " + menuId);
                return;
            } catch (Exception e) {
                plugin.getLogger().warning("调用 GuangDianMenu 失败: " + e.getMessage());
            }
        }
        
        // 回退到 NPC 自带的菜单系统
        NPCManager.MenuDefinition menu = npcManager.getMenu(menuId);
        if (menu == null) {
            player.sendMessage(Component.text("菜单不存在: " + menuId).color(NamedTextColor.RED));
            return;
        }

        NPCMenuHolder holder = new NPCMenuHolder(menu.getId());
        Inventory inventory = Bukkit.createInventory(holder, menu.getSize(), color(menu.getTitle()));

        for (NPCManager.MenuItemDefinition item : menu.getItems()) {
            if (item.getSlot() < 0 || item.getSlot() >= inventory.getSize()) {
                continue;
            }
            ItemStack stack = new ItemStack(item.getMaterial());
            ItemMeta meta = stack.getItemMeta();
            if (meta != null) {
                meta.displayName(color(item.getName()));
                meta.lore(item.getLore().stream().map(this::color).collect(Collectors.toList()));
                stack.setItemMeta(meta);
            }
            inventory.setItem(item.getSlot(), stack);
        }

        player.openInventory(inventory);
    }

    @Override
    public void openNPCMenu(Player player, String npcId) {
        NPCData npc = npcManager.getNPC(npcId);
        if (npc == null) {
            player.sendMessage(net.kyori.adventure.text.Component.text("NPC 不存在: " + npcId, net.kyori.adventure.text.format.NamedTextColor.RED));
            return;
        }
        openMenu(player, npc.getMenuId());
    }

    @Override
    public void teleportToNPC(Player player, String npcId) {
        NPCData npc = npcManager.getNPC(npcId);
        if (npc == null) {
            player.sendMessage(Component.text("NPC 不存在: " + npcId).color(NamedTextColor.RED));
            return;
        }

        World world = Bukkit.getWorld(npc.getWorldName());
        if (world == null) {
            player.sendMessage(net.kyori.adventure.text.Component.text("NPC 所在世界未加载: " + npc.getWorldName(), net.kyori.adventure.text.format.NamedTextColor.RED));
            return;
        }

        Location location = new Location(world, npc.getX(), npc.getY(), npc.getZ(), npc.getYaw(), npc.getPitch());
        player.teleport(location);
    }

    @Override
    public void setNPCEnabled(String id, boolean enabled) {
        NPCData npc = npcManager.getNPC(id);
        if (npc == null) {
            return;
        }
        npc.setEnabled(enabled);
        if (enabled) {
            npcManager.spawnNPC(npc);
        } else {
            npcManager.despawnNPC(npc);
        }
        npcManager.save();
    }

    @Override
    public void setNPCMenu(String id, String menuId) {
        NPCData npc = npcManager.getNPC(id);
        if (npc == null) {
            return;
        }
        npc.setMenuId(menuId);
        npcManager.save();
    }

    @Override
    public void setNPCDisplayName(String id, String displayName) {
        NPCData npc = npcManager.getNPC(id);
        if (npc == null) {
            return;
        }
        npc.setDisplayName(displayName);
        npcManager.respawnNPC(npc);
        npcManager.save();
    }

    @Override
    public void setNPCCommands(String id, List<String> commands) {
        NPCData npc = npcManager.getNPC(id);
        if (npc == null) {
            return;
        }
        npc.setCommands(commands);
        npcManager.save();
    }

    @Override
    public void addNPCCommand(String id, String command) {
        NPCData npc = npcManager.getNPC(id);
        if (npc == null) {
            return;
        }
        npc.addCommand(command);
        npcManager.save();
    }

    /**
     * 使用 MiniMessage 解析颜色代码
     */
    private Component color(String text) {
        if (text == null) return Component.empty();
        // 尝试使用 RPGCore MiniMessageService
        MiniMessageService mm = plugin.getMiniMessageService();
        if (mm != null) {
            return mm.colorize(text);
        }
        // 降级处理：使用本地 MiniMessage
        String converted = text
            .replace("<black>", "<black>").replace("<dark_blue>", "<dark_blue>")
            .replace("<dark_green>", "<dark_green>").replace("<dark_aqua>", "<dark_aqua>")
            .replace("<dark_red>", "<dark_red>").replace("<dark_purple>", "<dark_purple>")
            .replace("<gold>", "<gold>").replace("<gray>", "<gray>")
            .replace("<dark_gray>", "<dark_gray>").replace("<blue>", "<blue>")
            .replace("<green>", "<green>").replace("<aqua>", "<aqua>")
            .replace("<red>", "<red>").replace("<light_purple>", "<light_purple>")
            .replace("<yellow>", "<yellow>").replace("<white>", "<white>")
            .replace("<obfuscated>", "<obfuscated>").replace("<bold>", "<bold>")
            .replace("<strikethrough>", "<strikethrough>").replace("<underlined>", "<underlined>")
            .replace("<italic>", "<italic>").replace("<reset>", "<reset>");
        return MiniMessage.miniMessage().deserialize(converted);
    }

    public static class NPCMenuHolder implements InventoryHolder {
        private final String menuId;

        public NPCMenuHolder(String menuId) {
            this.menuId = menuId;
        }

        public String getMenuId() {
            return menuId;
        }

        @Override
        public Inventory getInventory() {
            return null;
        }
    }
}
