package cn.guangdian.rpgcore.gui.model;

import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.BiFunction;

public class MenuItem {

    private final Material material;
    private final String name;
    private final List<String> lore;
    private final List<Integer> slots;
    private final String action;
    private final List<String> actions;
    private final String skullOwner;
    private final boolean glowing;
    private final String permission;
    private final BiFunction<String, org.bukkit.entity.Player, String> placeholderProcessor;

    private ItemStack cachedItemStack;
    private Consumer<org.bukkit.event.inventory.InventoryClickEvent> clickHandler;

    private MenuItem(Builder builder) {
        this.material = builder.material;
        this.name = builder.name;
        this.lore = builder.lore != null ? new ArrayList<>(builder.lore) : new ArrayList<>();
        this.slots = builder.slots != null ? new ArrayList<>(builder.slots) : new ArrayList<>();
        this.action = builder.action;
        this.actions = builder.actions != null ? new ArrayList<>(builder.actions) : new ArrayList<>();
        this.skullOwner = builder.skullOwner;
        this.glowing = builder.glowing;
        this.permission = builder.permission;
        this.placeholderProcessor = builder.placeholderProcessor;
    }

    @NotNull
    public ItemStack build(org.bukkit.entity.Player player) {
        if (cachedItemStack != null && placeholderProcessor == null) {
            return cachedItemStack.clone();
        }

        ItemStack itemStack = new ItemStack(material);
        ItemMeta meta = itemStack.getItemMeta();
        if (meta == null) {
            return itemStack;
        }

        String processedName = processText(name, player);
        String processedSkull = processText(skullOwner != null ? skullOwner : "", player);

        meta.displayName(net.kyori.adventure.text.Component.text(processedName));
        meta.lore(lore.stream()
                .map(line -> net.kyori.adventure.text.Component.text(processText(line, player)))
                .toList());

        if (material == Material.PLAYER_HEAD && !processedSkull.isEmpty() && meta instanceof org.bukkit.inventory.meta.SkullMeta skullMeta) {
            skullMeta.setOwner(processedSkull);
        }

        if (glowing) {
            itemStack.addUnsafeEnchantment(org.bukkit.enchantments.Enchantment.UNBREAKING, 1);
            meta.addItemFlags(org.bukkit.inventory.ItemFlag.HIDE_ENCHANTS);
        }

        meta.addItemFlags(org.bukkit.inventory.ItemFlag.HIDE_ATTRIBUTES);

        itemStack.setItemMeta(meta);
        return itemStack;
    }

    private String processText(String text, org.bukkit.entity.Player player) {
        if (text == null || text.isEmpty()) {
            return text;
        }
        if (placeholderProcessor != null && player != null) {
            return placeholderProcessor.apply(text, player);
        }
        return text;
    }

    public @NotNull Material getMaterial() {
        return material;
    }

    public @NotNull String getName() {
        return name;
    }

    public @NotNull List<String> getLore() {
        return lore;
    }

    public @NotNull List<Integer> getSlots() {
        return slots;
    }

    public @Nullable String getAction() {
        return action;
    }

    public @NotNull List<String> getActions() {
        return actions;
    }

    public @Nullable String getSkullOwner() {
        return skullOwner;
    }

    public boolean isGlowing() {
        return glowing;
    }

    public @Nullable String getPermission() {
        return permission;
    }

    public boolean hasPermission(org.bukkit.entity.Player player) {
        if (permission == null || permission.isEmpty()) {
            return true;
        }
        return player.hasPermission(permission);
    }

    public @Nullable Consumer<org.bukkit.event.inventory.InventoryClickEvent> getClickHandler() {
        return clickHandler;
    }

    public void setClickHandler(@Nullable Consumer<org.bukkit.event.inventory.InventoryClickEvent> handler) {
        this.clickHandler = handler;
    }

    public boolean hasClickHandler() {
        return clickHandler != null;
    }

    public static Builder builder(Material material) {
        return new Builder(material);
    }

    public static class Builder {
        private final Material material;
        private String name = "";
        private List<String> lore;
        private List<Integer> slots;
        private String action;
        private List<String> actions;
        private String skullOwner;
        private boolean glowing = false;
        private String permission;
        private BiFunction<String, org.bukkit.entity.Player, String> placeholderProcessor;

        private Builder(Material material) {
            this.material = material;
        }

        public Builder name(String name) {
            this.name = name;
            return this;
        }

        public Builder lore(List<String> lore) {
            this.lore = lore;
            return this;
        }

        public Builder slots(List<Integer> slots) {
            this.slots = slots;
            return this;
        }

        public Builder slot(int slot) {
            if (this.slots == null) {
                this.slots = new ArrayList<>();
            }
            this.slots.add(slot);
            return this;
        }

        public Builder action(String action) {
            this.action = action;
            return this;
        }

        public Builder actions(List<String> actions) {
            this.actions = actions;
            return this;
        }

        public Builder skullOwner(String skullOwner) {
            this.skullOwner = skullOwner;
            return this;
        }

        public Builder glowing(boolean glowing) {
            this.glowing = glowing;
            return this;
        }

        public Builder permission(String permission) {
            this.permission = permission;
            return this;
        }

        public Builder placeholderProcessor(BiFunction<String, org.bukkit.entity.Player, String> processor) {
            this.placeholderProcessor = processor;
            return this;
        }

        public Builder clickHandler(Consumer<org.bukkit.event.inventory.InventoryClickEvent> handler) {
            return this;
        }

        public MenuItem build() {
            return new MenuItem(this);
        }
    }
}