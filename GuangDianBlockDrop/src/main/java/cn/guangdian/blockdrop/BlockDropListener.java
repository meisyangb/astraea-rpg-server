package cn.guangdian.blockdrop;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.inventory.ItemStack;

import java.util.concurrent.ThreadLocalRandom;

public class BlockDropListener implements Listener {

    private final GuangDianBlockDrop plugin;

    public BlockDropListener(GuangDianBlockDrop plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.NORMAL, ignoreCancelled = true)
    public void onBlockBreak(BlockBreakEvent event) {
        Player player = event.getPlayer();
        Material blockType = event.getBlock().getType();
        Location dropLocation = event.getBlock().getLocation().add(0.5, 0.5, 0.5);

        DropConfigManager configManager = plugin.getDropConfigManager();
        if (configManager == null || !configManager.isEnabled()) return;

        DropConfigManager.BlockDropEntry blockEntry = configManager.getBlockDrop(blockType);
        if (blockEntry == null) return;

        for (DropConfigManager.DropEntry drop : blockEntry.getDrops()) {
            if (ThreadLocalRandom.current().nextDouble() > drop.getChance()) {
                debug(player.getName() + " 破坏 " + blockType.name() + " -> " + drop.getName() + " 未通过概率");
                continue;
            }

            int amount = drop.resolveAmount();

            switch (drop.getType()) {
                case EXPERIENCE -> processExperience(player, amount);
                case MYTHIC -> processMythicItem(dropLocation, drop.getName(), amount);
                case VANILLA -> processVanillaItem(dropLocation, drop.getName(), amount);
                case COMMAND -> processCommand(player, drop.getName());
            }

            debug(player.getName() + " 破坏 " + blockType.name() + " -> 掉落 " + drop.getName() + " x" + amount);
        }
    }

    private void processExperience(Player player, int amount) {
        player.giveExp(amount);
    }

    private void processMythicItem(Location location, String itemName, int amount) {
        ItemStack item = MythicMobsIntegration.getMythicItem(itemName, amount);
        if (item != null) {
            location.getWorld().dropItemNaturally(location, item);
        } else {
            plugin.getLogger().warning("无法获取 MythicMobs 物品: " + itemName);
        }
    }

    private void processVanillaItem(Location location, String materialName, int amount) {
        try {
            Material material = Material.valueOf(materialName.toUpperCase());
            ItemStack item = new ItemStack(material, amount);
            location.getWorld().dropItemNaturally(location, item);
        } catch (IllegalArgumentException e) {
            plugin.getLogger().warning("未知的原版物品: " + materialName);
        }
    }

    private void processCommand(Player player, String command) {
        String parsed = command.replace("%player%", player.getName());
        org.bukkit.Bukkit.dispatchCommand(org.bukkit.Bukkit.getConsoleSender(), parsed);
    }

    private void debug(String message) {
        if (plugin.getDropConfigManager().isDebug()) {
            plugin.getLogger().info("[DEBUG] " + message);
        }
    }
}