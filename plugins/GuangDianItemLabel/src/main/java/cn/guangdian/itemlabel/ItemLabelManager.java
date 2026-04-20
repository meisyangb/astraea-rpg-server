package cn.guangdian.itemlabel;

import cn.guangdian.rpgcore.RPGCore;
import cn.guangdian.rpgcore.integration.ExternalServiceIntegration;
import cn.guangdian.rpgcore.message.MiniMessageService;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextComponent;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.Particle;
import org.bukkit.entity.Display;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Item;
import org.bukkit.entity.Player;
import org.bukkit.entity.TextDisplay;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.util.Transformation;
import org.bukkit.util.Vector;
import org.joml.AxisAngle4f;
import org.joml.Vector3f;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class ItemLabelManager {

    private final GuangDianItemLabel plugin;
    private final MiniMessageService miniMessage;
    private final Map<UUID, UUID> itemToLabelMap = new ConcurrentHashMap<>();
    private final Set<UUID> pendingItems = ConcurrentHashMap.newKeySet();
    private final Map<UUID, Integer> itemSettleTicks = new ConcurrentHashMap<>();
    private final NamespacedKey labelKey;
    private final NamespacedKey mythicTypeKey;

    // 显示设置
    private final float labelOffsetY;
    private final float labelScale;
    private final int labelViewRange;
    private final int settleTicks;
    private final boolean shadowed;
    private final boolean seeThrough;
    private final Display.Billboard billboard;
    private final int backgroundOpacity;

    // 显示模式
    private final DisplayMode displayMode;
    private final double proximityDistance;
    private final boolean followItem;

    // 过滤设置
    private final FilterMode filterMode;
    private final Set<Material> whitelistMaterials;
    private final Set<Material> blacklistMaterials;
    private final double minValue;
    private final boolean showEnchanted;
    private final boolean showRenamed;

    // 品质系统
    private final Map<String, RarityLevel> rarityLevels = new HashMap<>();
    private final Map<Material, String> materialRarityMap = new HashMap<>();

    // MythicMobs 支持
    private final boolean mythicMobsEnabled;
    private final Map<String, String> mythicRarityByLevel = new HashMap<>();

    // 标签格式
    private final String singleLineFormat;
    private final String multiLineFormat;
    private final boolean showAmount;
    private final boolean hideAmountWhenSingle;

    // 效果设置
    private final boolean glowEnabled;
    private final boolean glowFollowRarity;
    private final boolean particlesEnabled;
    private final int particleInterval;
    private final int particleCount;

    // 性能优化
    private final boolean batchUpdate;
    private final int batchSize;
    private final boolean cullingEnabled;
    private final double cullingDistance;

    // 粒子计时器
    private int particleTick = 0;

    public ItemLabelManager(GuangDianItemLabel plugin) {
        this.plugin = plugin;
        this.miniMessage = plugin.getMiniMessage();
        this.labelKey = new NamespacedKey(plugin, "itemlabel_id");
        this.mythicTypeKey = new NamespacedKey("mythicmobs", "type");

        // 加载显示设置
        this.labelOffsetY = (float) plugin.getConfig().getDouble("display.offset-y", 0.35);
        this.labelScale = (float) plugin.getConfig().getDouble("display.scale", 0.9);
        this.labelViewRange = plugin.getConfig().getInt("settings.view-range", 24);
        this.settleTicks = plugin.getConfig().getInt("settings.settle-ticks", 2);
        this.shadowed = plugin.getConfig().getBoolean("display.shadowed", true);
        this.seeThrough = plugin.getConfig().getBoolean("display.see-through", false);
        this.billboard = parseBillboard(plugin.getConfig().getString("display.alignment", "CENTER"));
        this.backgroundOpacity = plugin.getConfig().getInt("display.background-opacity", 0);

        // 加载显示模式
        this.displayMode = DisplayMode.valueOf(plugin.getConfig().getString("settings.display-mode", "PROXIMITY").toUpperCase());
        this.proximityDistance = plugin.getConfig().getDouble("settings.proximity-distance", 16.0);
        this.followItem = plugin.getConfig().getBoolean("settings.follow-item", true);

        // 加载过滤设置
        this.filterMode = FilterMode.valueOf(plugin.getConfig().getString("filter.mode", "ALL").toUpperCase());
        this.whitelistMaterials = loadMaterialSet(plugin.getConfig().getStringList("filter.whitelist"));
        this.blacklistMaterials = loadMaterialSet(plugin.getConfig().getStringList("filter.blacklist"));
        this.minValue = plugin.getConfig().getDouble("filter.min-value", 0.0);
        this.showEnchanted = plugin.getConfig().getBoolean("filter.show-enchanted", true);
        this.showRenamed = plugin.getConfig().getBoolean("filter.show-renamed", true);

        // 加载品质系统
        loadRarityLevels();
        loadMaterialRarityMap();

        // 加载 MythicMobs 设置
        this.mythicMobsEnabled = plugin.getConfig().getBoolean("mythicmobs.enabled", true);
        loadMythicMobsRarity();

        // 加载标签格式
        this.singleLineFormat = plugin.getConfig().getString("label-format.single-line", "{rarity_prefix}{name}{rarity_suffix} <gray>x{amount}");
        this.multiLineFormat = plugin.getConfig().getString("label-format.multi-line", null);
        this.showAmount = plugin.getConfig().getBoolean("label-format.amount.enabled", true);
        this.hideAmountWhenSingle = plugin.getConfig().getBoolean("label-format.amount.hide-when-single", true);

        // 加载效果设置
        this.glowEnabled = plugin.getConfig().getBoolean("effects.glow.enabled", true);
        this.glowFollowRarity = plugin.getConfig().getBoolean("effects.glow.follow-rarity", true);
        this.particlesEnabled = plugin.getConfig().getBoolean("effects.particles.enabled", true);
        this.particleInterval = plugin.getConfig().getInt("effects.particles.interval", 20);
        this.particleCount = plugin.getConfig().getInt("effects.particles.count", 3);

        // 加载性能设置
        this.batchUpdate = plugin.getConfig().getBoolean("performance.batch-update", true);
        this.batchSize = plugin.getConfig().getInt("performance.batch-size", 10);
        this.cullingEnabled = plugin.getConfig().getBoolean("performance.culling.enabled", true);
        this.cullingDistance = plugin.getConfig().getDouble("performance.culling.distance", 48.0);
    }

    private Display.Billboard parseBillboard(String alignment) {
        return switch (alignment.toUpperCase()) {
            case "LEFT" -> Display.Billboard.FIXED;
            case "RIGHT" -> Display.Billboard.FIXED;
            default -> Display.Billboard.CENTER;
        };
    }

    private Set<Material> loadMaterialSet(List<String> materialNames) {
        Set<Material> materials = new HashSet<>();
        for (String name : materialNames) {
            try {
                materials.add(Material.valueOf(name.toUpperCase()));
            } catch (IllegalArgumentException ignored) {
                plugin.getLogger().warning("未知的物品类型: " + name);
            }
        }
        return materials;
    }

    private void loadRarityLevels() {
        if (!plugin.getConfig().contains("rarity-levels")) return;

        for (String key : plugin.getConfig().getConfigurationSection("rarity-levels").getKeys(false)) {
            String path = "rarity-levels." + key;
            RarityLevel level = new RarityLevel(
                key,
                plugin.getConfig().getInt(path + ".priority", 1),
                plugin.getConfig().getString(path + ".color", "<white>"),
                plugin.getConfig().getString(path + ".glow-color", "WHITE"),
                plugin.getConfig().getString(path + ".prefix", ""),
                plugin.getConfig().getString(path + ".suffix", ""),
                plugin.getConfig().getBoolean(path + ".show-particles", false),
                plugin.getConfig().getString(path + ".particle-type", "WITCH")
            );
            rarityLevels.put(key.toLowerCase(), level);
        }
    }

    private void loadMaterialRarityMap() {
        if (!plugin.getConfig().contains("item-rarity-mapping")) return;

        for (String rarity : plugin.getConfig().getConfigurationSection("item-rarity-mapping").getKeys(false)) {
            if (rarity.equals("enchanted-default")) continue;

            List<String> materials = plugin.getConfig().getStringList("item-rarity-mapping." + rarity + ".materials");
            for (String matName : materials) {
                try {
                    materialRarityMap.put(Material.valueOf(matName.toUpperCase()), rarity.toLowerCase());
                } catch (IllegalArgumentException ignored) {
                    plugin.getLogger().warning("未知的物品类型: " + matName);
                }
            }
        }
    }

    private void loadMythicMobsRarity() {
        if (!plugin.getConfig().contains("mythicmobs.rarity-by-level")) return;

        for (String range : plugin.getConfig().getConfigurationSection("mythicmobs.rarity-by-level").getKeys(false)) {
            String rarity = plugin.getConfig().getString("mythicmobs.rarity-by-level." + range);
            mythicRarityByLevel.put(range, rarity.toLowerCase());
        }
    }

    public void queueLabel(Item item) {
        if (item == null || item.isDead()) return;
        if (itemToLabelMap.containsKey(item.getUniqueId())) return;
        if (!shouldShowLabel(item.getItemStack())) return;

        // 根据显示模式决定如何处理
        switch (displayMode) {
            case IMMEDIATE:
                // 立即显示模式：直接创建标签
                createLabel(item);
                break;
            case SETTLED:
                // 静止显示模式：加入等待队列
                pendingItems.add(item.getUniqueId());
                itemSettleTicks.put(item.getUniqueId(), 0);
                break;
            case PROXIMITY:
                // 接近显示模式：加入等待队列，等玩家靠近
                pendingItems.add(item.getUniqueId());
                itemSettleTicks.put(item.getUniqueId(), 0);
                break;
        }
    }

    private boolean shouldShowLabel(ItemStack itemStack) {
        if (itemStack == null || itemStack.getType().isAir()) return false;

        Material type = itemStack.getType();
        ItemMeta meta = itemStack.getItemMeta();

        // 检查过滤模式
        switch (filterMode) {
            case WHITELIST -> {
                if (!whitelistMaterials.contains(type)) return false;
            }
            case BLACKLIST -> {
                if (blacklistMaterials.contains(type)) return false;
            }
            case NONE -> {
                return false;
            }
        }

        // 检查附魔物品
        if (meta != null && meta.hasEnchants() && !showEnchanted) {
            return false;
        }

        // 检查重命名物品
        if (meta != null && meta.hasDisplayName() && !showRenamed) {
            return false;
        }

        // 检查最低价值
        if (minValue > 0) {
            RPGCore rpgCore = RPGCore.getInstance();
            if (rpgCore != null) {
                ExternalServiceIntegration external = rpgCore.getExternalServices();
                if (external.isVaultEnabled()) {
                    // 这里可以扩展物品价值计算
                }
            }
        }

        return true;
    }

    public void createLabel(Item item) {
        if (item == null || item.isDead()) return;

        removeLabel(item);

        ItemStack itemStack = item.getItemStack();
        RarityLevel rarity = getItemRarity(itemStack);

        Location location = item.getLocation().clone().add(0, labelOffsetY, 0);
        TextDisplay textDisplay = location.getWorld().spawn(location, TextDisplay.class, display -> {
            display.text(buildItemLabel(itemStack, rarity));
            display.setBillboard(billboard);
            display.setViewRange(labelViewRange);
            display.setShadowed(shadowed);
            display.setSeeThrough(seeThrough);

            if (backgroundOpacity > 0) {
                display.setBackgroundColor(org.bukkit.Color.fromARGB(backgroundOpacity, 0, 0, 0));
            }

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

        // 应用发光效果 (Paper 1.21.6 使用 setGlowing 和 glowingColor)
        if (glowEnabled && glowFollowRarity) {
            try {
                item.setGlowing(true);
                // 注意: 物品实体的发光颜色在 1.21.6 中需要通过团队颜色或其他方式实现
                // 这里简化处理，仅设置发光状态
            } catch (Exception ignored) {
            }
        }
    }

    public void updateLabels() {
        particleTick++;

        // 批量处理更新
        List<Map.Entry<UUID, UUID>> entries = new ArrayList<>(itemToLabelMap.entrySet());
        int batchCount = batchUpdate ? Math.max(1, entries.size() / batchSize) : entries.size();

        for (int i = 0; i < entries.size(); i++) {
            Map.Entry<UUID, UUID> entry = entries.get(i);
            UUID itemId = entry.getKey();
            UUID labelId = entry.getValue();

            Entity itemEntity = plugin.getServer().getEntity(itemId);
            Entity labelEntity = plugin.getServer().getEntity(labelId);

            // 清理无效实体
            if (itemEntity == null || itemEntity.isDead() || !(itemEntity instanceof Item)) {
                if (labelEntity != null && !labelEntity.isDead()) {
                    labelEntity.remove();
                }
                itemToLabelMap.remove(itemId);
                continue;
            }

            if (labelEntity == null || labelEntity.isDead()) {
                itemToLabelMap.remove(itemId);
                continue;
            }

            Item item = (Item) itemEntity;
            TextDisplay textDisplay = (TextDisplay) labelEntity;

            // 视距裁剪 - 检查是否有玩家 nearby
            if (cullingEnabled && !isNearAnyPlayer(item.getLocation())) {
                // 如果配置了接近显示模式，远离玩家时隐藏标签
                if (displayMode == DisplayMode.PROXIMITY) {
                    labelEntity.setVisibleByDefault(false);
                }
                continue;
            } else {
                // 玩家靠近时显示
                labelEntity.setVisibleByDefault(true);
            }

            // 更新位置 (如果配置了跟随物品)
            if (followItem) {
                Location itemLoc = item.getLocation();
                Location labelLoc = labelEntity.getLocation();

                if (itemLoc.getX() != labelLoc.getX() ||
                    itemLoc.getY() + labelOffsetY != labelLoc.getY() ||
                    itemLoc.getZ() != labelLoc.getZ()) {
                    labelEntity.teleport(itemLoc.clone().add(0, labelOffsetY, 0));
                }
            }

            // 更新文本 (只在必要时)
            if (i % batchCount == 0) {
                Component newLabel = buildItemLabel(item.getItemStack(), getItemRarity(item.getItemStack()));
                textDisplay.text(newLabel);
            }

            // 粒子效果
            if (particlesEnabled && particleTick % particleInterval == 0) {
                RarityLevel rarity = getItemRarity(item.getItemStack());
                if (rarity.showParticles) {
                    spawnParticles(item.getLocation(), rarity);
                }
            }
        }

        // 处理待显示的物品
        processPendingItems();

        if (particleTick >= 1000) particleTick = 0;
    }

    private boolean isNearAnyPlayer(Location location) {
        for (Player player : location.getWorld().getPlayers()) {
            if (player.getLocation().distanceSquared(location) <= proximityDistance * proximityDistance) {
                return true;
            }
        }
        return false;
    }

    private void spawnParticles(Location location, RarityLevel rarity) {
        try {
            Particle particle = Particle.valueOf(rarity.particleType);
            location.getWorld().spawnParticle(
                particle,
                location.clone().add(0, 0.5, 0),
                particleCount,
                0.2, 0.2, 0.2,
                0.01
            );
        } catch (IllegalArgumentException ignored) {
        }
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

            switch (displayMode) {
                case SETTLED:
                    // 静止模式：等待物品静止
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
                    break;

                case PROXIMITY:
                    // 接近模式：等待玩家靠近
                    if (isNearAnyPlayer(item.getLocation())) {
                        createLabel(item);
                        return true;
                    }
                    break;

                case IMMEDIATE:
                    // 立即模式：不应该进入这里
                    createLabel(item);
                    return true;
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

    private Component buildItemLabel(ItemStack itemStack, RarityLevel rarity) {
        if (itemStack == null || itemStack.getType().isAir()) {
            return Component.empty();
        }

        int amount = itemStack.getAmount();
        String itemName = getItemDisplayName(itemStack);

        // 构建数量显示
        String amountStr = "";
        if (showAmount && !(hideAmountWhenSingle && amount == 1)) {
            amountStr = " <gray>" + amount;
        }

        // 使用格式模板
        String format = multiLineFormat != null && !multiLineFormat.isEmpty()
            ? multiLineFormat
            : singleLineFormat;

        String labelText = format
            .replace("{name}", itemName)
            .replace("{amount}", String.valueOf(amount))
            .replace("{rarity}", rarity.name)
            .replace("{rarity_prefix}", rarity.prefix)
            .replace("{rarity_suffix}", rarity.suffix)
            .replace("{rarity_color}", rarity.color);

        // 处理多行
        if (labelText.contains("\n")) {
            String[] lines = labelText.split("\n");
            TextComponent.Builder builder = Component.text();
            for (int i = 0; i < lines.length; i++) {
                if (i > 0) builder.append(Component.newline());
                builder.append(miniMessage.colorize(lines[i].trim()));
            }
            return builder.build();
        }

        return miniMessage.colorize(labelText);
    }

    private String getItemDisplayName(ItemStack itemStack) {
        ItemMeta meta = itemStack.getItemMeta();

        // 优先使用自定义显示名称
        if (meta != null && meta.hasDisplayName()) {
            Component displayName = meta.displayName();
            if (displayName instanceof TextComponent textComp) {
                return textComp.content();
            }
            return displayName.toString();
        }

        // 使用默认物品名称
        return formatMaterialName(itemStack.getType());
    }

    private String formatMaterialName(Material material) {
        String typeName = material.name().toLowerCase(Locale.ROOT).replace("_", " ");
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

    private RarityLevel getItemRarity(ItemStack itemStack) {
        if (itemStack == null) {
            return rarityLevels.getOrDefault("common", getDefaultRarity());
        }

        // 检查是否是 MythicMobs 物品
        if (mythicMobsEnabled) {
            ItemMeta meta = itemStack.getItemMeta();
            if (meta != null) {
                PersistentDataContainer pdc = meta.getPersistentDataContainer();
                if (pdc.has(mythicTypeKey, PersistentDataType.STRING)) {
                    return getMythicMobsRarity(pdc.get(mythicTypeKey, PersistentDataType.STRING));
                }
            }
        }

        // 检查附魔物品
        ItemMeta meta = itemStack.getItemMeta();
        if (meta != null && meta.hasEnchants()) {
            String enchantedDefault = plugin.getConfig().getString("item-rarity-mapping.enchanted-default", "rare");
            return rarityLevels.getOrDefault(enchantedDefault, getDefaultRarity());
        }

        // 根据材质获取品质
        String rarityKey = materialRarityMap.get(itemStack.getType());
        if (rarityKey != null) {
            return rarityLevels.getOrDefault(rarityKey, getDefaultRarity());
        }

        return rarityLevels.getOrDefault("common", getDefaultRarity());
    }

    private RarityLevel getMythicMobsRarity(String mythicType) {
        // 简化处理：根据 MythicMobs 物品类型或等级判断
        // 实际实现可能需要查询 MythicMobs API
        return rarityLevels.getOrDefault("rare", getDefaultRarity());
    }

    private RarityLevel getDefaultRarity() {
        return new RarityLevel("common", 1, "<white>", "WHITE", "", "", false, "WITCH");
    }

    private int parseColor(String colorName) {
        return switch (colorName.toUpperCase()) {
            case "WHITE" -> 0xFFFFFF;
            case "BLACK" -> 0x000000;
            case "RED" -> 0xFF0000;
            case "GREEN" -> 0x00FF00;
            case "BLUE" -> 0x0000FF;
            case "YELLOW" -> 0xFFFF00;
            case "PURPLE" -> 0xAA00AA;
            case "ORANGE" -> 0xFFAA00;
            case "GRAY", "GREY" -> 0xAAAAAA;
            case "AQUA" -> 0x55FFFF;
            default -> 0xFFFFFF;
        };
    }

    public boolean hasLabel(Item item) {
        return itemToLabelMap.containsKey(item.getUniqueId());
    }

    public boolean isPending(Item item) {
        return pendingItems.contains(item.getUniqueId());
    }

    public int getRarityLevelCount() {
        return rarityLevels.size();
    }

    // 过滤模式枚举
    private enum FilterMode {
        ALL, WHITELIST, BLACKLIST, NONE
    }

    // 显示模式枚举
    private enum DisplayMode {
        IMMEDIATE,   // 立即显示
        SETTLED,     // 静止后显示
        PROXIMITY    // 接近时显示
    }

    // 品质等级记录
    private record RarityLevel(
        String name,
        int priority,
        String color,
        String glowColor,
        String prefix,
        String suffix,
        boolean showParticles,
        String particleType
    ) {}
}
