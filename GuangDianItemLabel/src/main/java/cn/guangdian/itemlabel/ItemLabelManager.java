package cn.guangdian.itemlabel;

import cn.guangdian.rpgcore.message.MiniMessageService;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextColor;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.NamespacedKey;
import org.bukkit.World;
import org.bukkit.entity.Display;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Item;
import org.bukkit.entity.TextDisplay;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.util.Transformation;
import org.joml.AxisAngle4f;
import org.joml.Vector3f;

import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class ItemLabelManager {

    private final GuangDianItemLabel plugin;
    private final MiniMessageService miniMessage;
    private final Map<UUID, UUID> itemToLabelMap = new ConcurrentHashMap<>();
    private final Set<UUID> pendingItems = ConcurrentHashMap.newKeySet();
    private final NamespacedKey labelKey;

    private final float labelOffsetY;
    private final float labelScale;
    private final int labelViewRange;
    private final boolean showAmount;
    private final int settleTicks;
    private final Map<UUID, Integer> itemSettleTicks = new ConcurrentHashMap<>();

    // 孤儿清理计数器，每 N 次 updateLabels 执行一次全量孤儿扫描
    private int orphanScanCounter = 0;
    private final int orphanScanInterval;

    public ItemLabelManager(GuangDianItemLabel plugin) {
        this.plugin = plugin;
        this.miniMessage = plugin.getMiniMessage();
        this.labelKey = new NamespacedKey(plugin, "itemlabel_id");

        this.labelOffsetY = (float) plugin.getConfig().getDouble("label.offset-y", 0.8);
        this.labelScale = (float) plugin.getConfig().getDouble("label.scale", 0.8);
        this.labelViewRange = plugin.getConfig().getInt("label.view-range", 20);
        this.showAmount = plugin.getConfig().getBoolean("label.show-amount", true);
        this.settleTicks = plugin.getConfig().getInt("label.settle-ticks", 5);
        // 每 60 次 updateLabels（约 60 秒）执行一次孤儿扫描
        this.orphanScanInterval = plugin.getConfig().getInt("orphan-scan-interval", 60);
    }

    public void queueLabel(Item item) {
        if (item == null || item.isDead()) return;
        if (itemToLabelMap.containsKey(item.getUniqueId())) return;

        pendingItems.add(item.getUniqueId());
        itemSettleTicks.put(item.getUniqueId(), 0);
    }

    public void createLabel(Item item) {
        if (item == null || item.isDead()) return;

        removeLabel(item);

        Location location = item.getLocation().clone().add(0, labelOffsetY, 0);
        TextDisplay textDisplay = location.getWorld().spawn(location, TextDisplay.class, display -> {
            display.text(buildItemLabel(item.getItemStack()));
            display.setBillboard(Display.Billboard.CENTER);
            display.setViewRange(labelViewRange);
            display.setShadowed(true);
            display.setSeeThrough(false);

            Transformation transformation = new Transformation(
                new Vector3f(0, 0, 0),
                new AxisAngle4f(0, 0, 0, 1),
                new Vector3f(labelScale, labelScale, labelScale),
                new AxisAngle4f(0, 0, 0, 1)
            );
            display.setTransformation(transformation);

            PersistentDataContainer pdc = display.getPersistentDataContainer();
            pdc.set(labelKey, PersistentDataType.STRING, item.getUniqueId().toString());
        });

        itemToLabelMap.put(item.getUniqueId(), textDisplay.getUniqueId());
        pendingItems.remove(item.getUniqueId());
        itemSettleTicks.remove(item.getUniqueId());
    }

    public void updateLabels() {
        // 第一层：快速清理 map 中已知失效的映射
        itemToLabelMap.entrySet().removeIf(entry -> {
            UUID itemId = entry.getKey();
            UUID labelId = entry.getValue();

            Entity itemEntity = plugin.getServer().getEntity(itemId);
            Entity labelEntity = plugin.getServer().getEntity(labelId);

            if (itemEntity == null || itemEntity.isDead() || !(itemEntity instanceof Item)) {
                if (labelEntity != null && !labelEntity.isDead()) {
                    labelEntity.remove();
                }
                return true;
            }

            if (labelEntity == null || labelEntity.isDead()) {
                return true;
            }

            Item item = (Item) itemEntity;
            TextDisplay textDisplay = (TextDisplay) labelEntity;

            Location itemLoc = item.getLocation();
            Location labelLoc = labelEntity.getLocation();

            if (itemLoc.getX() != labelLoc.getX() ||
                itemLoc.getY() + labelOffsetY != labelLoc.getY() ||
                itemLoc.getZ() != labelLoc.getZ()) {
                labelEntity.teleport(itemLoc.clone().add(0, labelOffsetY, 0));
            }

            textDisplay.text(buildItemLabel(item.getItemStack()));

            return false;
        });

        processPendingItems();

        // 第二层：定期全量孤儿扫描（兜底机制）
        orphanScanCounter++;
        if (orphanScanCounter >= orphanScanInterval) {
            orphanScanCounter = 0;
            scanAndRemoveOrphanedLabels();
        }
    }

    /**
     * 全量扫描所有世界，移除没有对应 Item 的孤儿 TextDisplay 标签
     * 这是兜底机制，防止事件未触发导致标签残留
     */
    private void scanAndRemoveOrphanedLabels() {
        int removed = 0;
        for (World world : Bukkit.getWorlds()) {
            for (Entity entity : world.getEntities()) {
                if (!(entity instanceof TextDisplay)) continue;

                TextDisplay display = (TextDisplay) entity;
                PersistentDataContainer pdc = display.getPersistentDataContainer();
                String itemUuidStr = pdc.get(labelKey, PersistentDataType.STRING);
                if (itemUuidStr == null) continue;

                // 检查对应的 Item 是否还存在
                UUID itemUuid;
                try {
                    itemUuid = UUID.fromString(itemUuidStr);
                } catch (IllegalArgumentException e) {
                    display.remove();
                    removed++;
                    continue;
                }

                Entity itemEntity = plugin.getServer().getEntity(itemUuid);
                if (itemEntity == null || itemEntity.isDead() || !(itemEntity instanceof Item)) {
                    display.remove();
                    itemToLabelMap.remove(itemUuid);
                    pendingItems.remove(itemUuid);
                    itemSettleTicks.remove(itemUuid);
                    removed++;
                }
            }
        }

        if (removed > 0) {
            plugin.getLogger().info("孤儿标签扫描: 清理了 " + removed + " 个残留标签");
        }
    }

    private void processPendingItems() {
        pendingItems.removeIf(itemId -> {
            Entity itemEntity = plugin.getServer().getEntity(itemId);
            if (itemEntity == null || itemEntity.isDead() || !(itemEntity instanceof Item)) {
                itemSettleTicks.remove(itemId);
                return true;
            }

            if (itemToLabelMap.containsKey(itemId)) {
                itemSettleTicks.remove(itemId);
                return true;
            }

            int ticks = itemSettleTicks.getOrDefault(itemId, 0) + 1;
            if (ticks >= settleTicks) {
                createLabel((Item) itemEntity);
                return true;
            }
            itemSettleTicks.put(itemId, ticks);

            return false;
        });
    }

    public void removeLabel(Item item) {
        if (item == null) return;

        UUID itemId = item.getUniqueId();
        pendingItems.remove(itemId);
        itemSettleTicks.remove(itemId);

        UUID labelId = itemToLabelMap.remove(itemId);
        if (labelId != null) {
            Entity labelEntity = plugin.getServer().getEntity(labelId);
            if (labelEntity != null && !labelEntity.isDead()) {
                labelEntity.remove();
            }
        }
    }

    public void clearAllLabels() {
        itemToLabelMap.forEach((itemId, labelId) -> {
            Entity labelEntity = plugin.getServer().getEntity(labelId);
            if (labelEntity != null && !labelEntity.isDead()) {
                labelEntity.remove();
            }
        });
        itemToLabelMap.clear();
        pendingItems.clear();
        itemSettleTicks.clear();
    }

    public int getLabelCount() {
        return itemToLabelMap.size();
    }

    private Component buildItemLabel(ItemStack itemStack) {
        if (itemStack == null || itemStack.getType().isAir()) {
            return Component.empty();
        }

        int amount = itemStack.getAmount();
        Component label = getItemNameComponent(itemStack);

        if (showAmount && amount > 1) {
            label = label.append(Component.text(" x" + amount).color(TextColor.color(0xAAAAAA)));
        }

        return label;
    }

    private Component getItemNameComponent(ItemStack itemStack) {
        ItemMeta meta = itemStack.getItemMeta();

        if (meta != null && meta.hasDisplayName()) {
            return meta.displayName();
        }

        String translationKey = itemStack.getType().translationKey();
        if (translationKey != null) {
            return Component.translatable(translationKey);
        }

        TextColor color = getRarityColor(itemStack);
        String itemName = getDefaultItemName(itemStack);
        return Component.text(itemName).color(color);
    }

    private String getDefaultItemName(ItemStack itemStack) {
        String typeName = itemStack.getType().name().toLowerCase().replace("_", " ");
        String[] words = typeName.split(" ");
        StringBuilder result = new StringBuilder();
        for (String word : words) {
            if (!word.isEmpty()) {
                result.append(Character.toUpperCase(word.charAt(0)))
                      .append(word.substring(1))
                      .append(" ");
            }
        }
        return result.toString().trim();
    }

    private TextColor getRarityColor(ItemStack itemStack) {
        ItemMeta meta = itemStack.getItemMeta();

        if (meta != null && meta.hasEnchants()) {
            return TextColor.color(0xAA00AA);
        }

        String typeName = itemStack.getType().name();

        if (typeName.contains("DIAMOND") || typeName.contains("NETHERITE")) {
            return TextColor.color(0x00AAAA);
        }
        if (typeName.contains("GOLD") || typeName.contains("GOLDEN")) {
            return TextColor.color(0xFFAA00);
        }
        if (typeName.contains("IRON")) {
            return TextColor.color(0xFFFFFF);
        }
        if (typeName.contains("EMERALD")) {
            return TextColor.color(0x00AA00);
        }

        return TextColor.color(0xFFFFFF);
    }

    public boolean hasLabel(Item item) {
        return itemToLabelMap.containsKey(item.getUniqueId());
    }

    public boolean isPending(Item item) {
        return pendingItems.contains(item.getUniqueId());
    }

    public void cleanOrphanedLabels() {
        scanAndRemoveOrphanedLabels();
    }
}
