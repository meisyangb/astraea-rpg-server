package cn.guangdian.itemlabel;

import cn.guangdian.rpgcore.message.MiniMessageService;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextColor;
import org.bukkit.Location;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Display;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Item;
import org.bukkit.entity.TextDisplay;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.util.Transformation;
import org.bukkit.util.Vector;
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

    public ItemLabelManager(GuangDianItemLabel plugin) {
        this.plugin = plugin;
        this.miniMessage = plugin.getMiniMessage();
        this.labelKey = new NamespacedKey(plugin, "itemlabel_id");

        this.labelOffsetY = (float) plugin.getConfig().getDouble("label.offset-y", 0.3);
        this.labelScale = (float) plugin.getConfig().getDouble("label.scale", 0.8);
        this.labelViewRange = plugin.getConfig().getInt("label.view-range", 20);
        this.showAmount = plugin.getConfig().getBoolean("label.show-amount", true);
        this.settleTicks = plugin.getConfig().getInt("label.settle-ticks", 5);
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
    }

    private void processPendingItems() {
        pendingItems.removeIf(itemId -> {
            Entity itemEntity = plugin.getServer().getEntity(itemId);
            if (itemEntity == null || itemEntity.isDead() || !(itemEntity instanceof Item)) {
                itemSettleTicks.remove(itemId);
                return true;
            }

            Item item = (Item) itemEntity;

            if (itemToLabelMap.containsKey(itemId)) {
                itemSettleTicks.remove(itemId);
                return true;
            }

            if (isItemSettled(item)) {
                int ticks = itemSettleTicks.getOrDefault(itemId, 0) + 1;
                if (ticks >= settleTicks) {
                    createLabel(item);
                    return true;
                }
                itemSettleTicks.put(itemId, ticks);
            } else {
                itemSettleTicks.put(itemId, 0);
            }

            return false;
        });
    }

    private boolean isItemSettled(Item item) {
        if (!item.isOnGround()) {
            return false;
        }

        Vector velocity = item.getVelocity();
        double speed = velocity.length();
        return speed < 0.05;
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

        // 如果物品有自定义显示名称，直接使用（保留原有颜色）
        if (meta != null && meta.hasDisplayName()) {
            return meta.displayName();
        }

        // 根据稀有度或材质类型获取颜色
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
}
