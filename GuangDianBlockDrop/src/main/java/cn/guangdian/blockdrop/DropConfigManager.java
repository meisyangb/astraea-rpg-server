package cn.guangdian.blockdrop;

import cn.guangdian.rpgcore.message.MiniMessageService;
import org.bukkit.Material;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ThreadLocalRandom;

public class DropConfigManager {

    private final GuangDianBlockDrop plugin;
    private final Map<String, BlockDropEntry> blockDrops = new HashMap<>();
    private boolean enabled;
    private boolean debug;

    public DropConfigManager(GuangDianBlockDrop plugin) {
        this.plugin = plugin;
    }

    public void load() {
        blockDrops.clear();

        plugin.saveDefaultConfig();
        plugin.reloadConfig();
        FileConfiguration config = plugin.getConfig();

        enabled = config.getBoolean("enabled", true);
        debug = config.getBoolean("debug", false);

        ConfigurationSection blocksSection = config.getConfigurationSection("blocks");
        if (blocksSection == null) {
            plugin.getLogger().warning("未找到 blocks 配置节!");
            return;
        }

        Set<String> blockKeys = blocksSection.getKeys(false);
        for (String blockKey : blockKeys) {
            Material material;
            try {
                material = Material.valueOf(blockKey);
            } catch (IllegalArgumentException e) {
                plugin.getLogger().warning("未知的方块类型: " + blockKey);
                continue;
            }

            ConfigurationSection blockSection = blocksSection.getConfigurationSection(blockKey);
            if (blockSection == null) continue;

            ConfigurationSection dropsSection = blockSection.getConfigurationSection("drops");
            if (dropsSection == null) continue;

            BlockDropEntry entry = new BlockDropEntry(material);

            for (String dropKey : dropsSection.getKeys(false)) {
                ConfigurationSection dropSection = dropsSection.getConfigurationSection(dropKey);
                if (dropSection == null) continue;

                String type = dropSection.getString("type", "mythic");
                String amountStr = dropSection.getString("amount", "1");
                double chance = dropSection.getDouble("chance", 1.0);

                if ("experience".equalsIgnoreCase(type)) {
                    entry.addDrop(new DropEntry(DropType.EXPERIENCE, "experience", amountStr, chance));
                } else if ("mythic".equalsIgnoreCase(type)) {
                    entry.addDrop(new DropEntry(DropType.MYTHIC, dropKey, amountStr, chance));
                } else if ("vanilla".equalsIgnoreCase(type)) {
                    entry.addDrop(new DropEntry(DropType.VANILLA, dropKey, amountStr, chance));
                } else if ("command".equalsIgnoreCase(type)) {
                    String command = dropSection.getString("command", "");
                    entry.addDrop(new DropEntry(DropType.COMMAND, command, amountStr, chance));
                }
            }

            blockDrops.put(blockKey, entry);
        }

        plugin.getLogger().info("已加载 " + blockDrops.size() + " 个方块掉落配置");
    }

    public BlockDropEntry getBlockDrop(Material material) {
        if (!enabled) return null;
        return blockDrops.get(material.name());
    }

    public int getBlockCount() {
        return blockDrops.size();
    }

    public boolean isEnabled() {
        return enabled;
    }

    public boolean isDebug() {
        return debug;
    }

    public void setDebug(boolean debug) {
        this.debug = debug;
    }

    public void sendBlockList(CommandSender sender) {
        MiniMessageService mm = plugin.getMiniMessageService();
        sender.sendMessage(mm.gold("========== 方块掉落列表 =========="));
        if (blockDrops.isEmpty()) {
            sender.sendMessage(mm.colorize("<gray>暂无配置"));
        } else {
            for (Map.Entry<String, BlockDropEntry> entry : blockDrops.entrySet()) {
                BlockDropEntry blockEntry = entry.getValue();
                StringBuilder dropInfo = new StringBuilder();
                for (DropEntry drop : blockEntry.getDrops()) {
                    if (dropInfo.length() > 0) dropInfo.append("<gray>, ");
                    String chanceStr = String.format("%.0f%%", drop.chance * 100);
                    dropInfo.append("<white>").append(drop.name)
                        .append("<gray>(<yellow>").append(chanceStr).append("<gray>)");
                }
                sender.sendMessage(mm.colorize(
                    "<gold>" + entry.getKey() + " <gray>-> " + dropInfo));
            }
        }
        sender.sendMessage(mm.gold("=================================="));
    }

    public static class BlockDropEntry {
        private final Material material;
        private final java.util.List<DropEntry> drops = new java.util.ArrayList<>();

        public BlockDropEntry(Material material) {
            this.material = material;
        }

        public void addDrop(DropEntry drop) {
            drops.add(drop);
        }

        public java.util.List<DropEntry> getDrops() {
            return drops;
        }

        public Material getMaterial() {
            return material;
        }
    }

    public static class DropEntry {
        private final DropType type;
        private final String name;
        private final String amountStr;
        private final double chance;

        public DropEntry(DropType type, String name, String amountStr, double chance) {
            this.type = type;
            this.name = name;
            this.amountStr = amountStr;
            this.chance = chance;
        }

        public DropType getType() { return type; }
        public String getName() { return name; }
        public double getChance() { return chance; }

        public int resolveAmount() {
            if (amountStr.contains("-")) {
                String[] parts = amountStr.split("-");
                if (parts.length == 2) {
                    try {
                        int min = Integer.parseInt(parts[0].trim());
                        int max = Integer.parseInt(parts[1].trim());
                        return ThreadLocalRandom.current().nextInt(max - min + 1) + min;
                    } catch (NumberFormatException e) {
                        return 1;
                    }
                }
            }
            try {
                return Integer.parseInt(amountStr);
            } catch (NumberFormatException e) {
                return 1;
            }
        }
    }

    public enum DropType {
        MYTHIC,
        VANILLA,
        EXPERIENCE,
        COMMAND
    }
}