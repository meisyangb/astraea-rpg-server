package cn.guangdian.rpgcore.gui.model;

import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.BiFunction;
import java.util.function.Consumer;

public class MenuData {

    private final String id;
    private final String title;
    private final int size;
    private final Map<String, MenuItem> items;
    private final Map<Integer, MenuItem> slotToItem;
    private final Map<String, Object> metadata;
    private BiFunction<String, Player, String> placeholderProcessor;

    public MenuData(String id, String title, int size) {
        this.id = id.toLowerCase();
        this.title = title;
        this.size = validateSize(size);
        this.items = new ConcurrentHashMap<>();
        this.slotToItem = new ConcurrentHashMap<>();
        this.metadata = new ConcurrentHashMap<>();
    }

    public MenuData(String id, int size) {
        this(id, "", size);
    }

    private int validateSize(int size) {
        if (size < 9) return 9;
        if (size > 54) return 54;
        if (size % 9 != 0) {
            size = (size / 9 + 1) * 9;
            if (size > 54) size = 54;
        }
        return size;
    }

    public void addItem(String itemId, MenuItem item) {
        items.put(itemId.toLowerCase(), item);
        for (int slot : item.getSlots()) {
            if (slot >= 0 && slot < size) {
                slotToItem.put(slot, item);
            }
        }
    }

    public void addItem(MenuItem item, int... slots) {
        String itemId = "item_" + items.size();
        item.getSlots().clear();
        for (int slot : slots) {
            if (slot >= 0 && slot < size) {
                item.getSlots().add(slot);
            }
        }
        addItem(itemId, item);
    }

    @Nullable
    public MenuItem getItem(String itemId) {
        return items.get(itemId.toLowerCase());
    }

    @Nullable
    public MenuItem getItemBySlot(int slot) {
        if (slot < 0 || slot >= size) return null;
        return slotToItem.get(slot);
    }

    @NotNull
    public Collection<MenuItem> getAllItems() {
        return items.values();
    }

    @NotNull
    public Set<Integer> getAllSlots() {
        return new HashSet<>(slotToItem.keySet());
    }

    @NotNull
    public String getId() {
        return id;
    }

    @NotNull
    public String getTitle() {
        return title;
    }

    public int getSize() {
        return size;
    }

    @NotNull
    public Map<String, MenuItem> getItems() {
        return items;
    }

    @NotNull
    public Collection<MenuItem> getItemsWithPermission(Player player) {
        List<MenuItem> result = new ArrayList<>();
        for (MenuItem item : items.values()) {
            if (item.hasPermission(player)) {
                result.add(item);
            }
        }
        return result;
    }

    @Nullable
    public Object getMetadata(String key) {
        return metadata.get(key);
    }

    public void setMetadata(String key, Object value) {
        metadata.put(key, value);
    }

    @Nullable
    public BiFunction<String, Player, String> getPlaceholderProcessor() {
        return placeholderProcessor;
    }

    public void setPlaceholderProcessor(@Nullable BiFunction<String, Player, String> processor) {
        this.placeholderProcessor = processor;
    }

    @NotNull
    public Map<Integer, MenuItem> getItemsBySlot() {
        return slotToItem;
    }

    public static Builder builder(String id, int size) {
        return new Builder(id, size);
    }

    public static Builder builder(String id, String title, int size) {
        return new Builder(id, title, size);
    }

    public static class Builder {
        private final String id;
        private String title = "";
        private int size = 27;
        private final Map<String, MenuItem> items = new HashMap<>();
        private final Map<String, Object> metadata = new HashMap<>();
        private BiFunction<String, Player, String> placeholderProcessor;

        private Builder(String id, int size) {
            this.id = id.toLowerCase();
            this.size = size;
        }

        private Builder(String id, String title, int size) {
            this.id = id.toLowerCase();
            this.title = title;
            this.size = size;
        }

        public Builder title(String title) {
            this.title = title;
            return this;
        }

        public Builder size(int size) {
            this.size = size;
            return this;
        }

        public Builder item(String itemId, MenuItem item) {
            items.put(itemId.toLowerCase(), item);
            return this;
        }

        public Builder item(Material material, int slot, Consumer<MenuItem> configurer) {
            MenuItem.Builder itemBuilder = MenuItem.builder(material).slot(slot);
            if (configurer != null) {
                configurer.accept(itemBuilder.build());
            }
            return this;
        }

        public Builder items(Map<String, MenuItem> items) {
            items.forEach((k, v) -> this.items.put(k.toLowerCase(), v));
            return this;
        }

        public Builder metadata(String key, Object value) {
            metadata.put(key, value);
            return this;
        }

        public Builder placeholderProcessor(BiFunction<String, Player, String> processor) {
            this.placeholderProcessor = processor;
            return this;
        }

        public MenuData build() {
            MenuData menu = new MenuData(id, title, size);
            items.forEach(menu::addItem);
            metadata.forEach(menu::setMetadata);
            menu.setPlaceholderProcessor(placeholderProcessor);
            return menu;
        }
    }
}