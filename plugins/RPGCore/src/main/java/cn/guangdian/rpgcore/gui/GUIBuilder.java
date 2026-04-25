package cn.guangdian.rpgcore.gui;

import cn.guangdian.rpgcore.gui.action.ActionExecutor;
import cn.guangdian.rpgcore.gui.model.MenuItem;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;
import java.util.function.BiFunction;
import java.util.function.Consumer;
import java.util.function.Function;

public final class GUIBuilder {

    private final String title;
    private final int size;
    private final Map<Integer, ItemStack> items;
    private final Map<Integer, Consumer<InventoryClickEvent>> clickHandlers;
    private final Map<Integer, List<String>> slotActions;
    private ItemStack fillerItem;
    private Consumer<Player> openHandler;
    private Consumer<Player> closeHandler;
    private BiFunction<String, Player, String> placeholderProcessor;
    private String menuId;

    private GUIBuilder(@NotNull String title, int size) {
        this.title = title;
        this.size = validateSize(size);
        this.items = new HashMap<>();
        this.clickHandlers = new HashMap<>();
        this.slotActions = new HashMap<>();
    }

    public static @NotNull GUIBuilder create(@NotNull String title, int rows) {
        return new GUIBuilder(title, rows * 9);
    }

    public static @NotNull GUIBuilder createCustom(@NotNull String title, int size) {
        return new GUIBuilder(title, size);
    }

    public static @NotNull GUIBuilder fromMenuData(String menuId, @NotNull cn.guangdian.rpgcore.gui.model.MenuData menuData) {
        GUIBuilder builder = new GUIBuilder(menuData.getTitle(), menuData.getSize());
        builder.menuId = menuData.getId();
        builder.placeholderProcessor = menuData.getPlaceholderProcessor();

        for (Map.Entry<Integer, MenuItem> entry : menuData.getItemsBySlot().entrySet()) {
            int slot = entry.getKey();
            MenuItem menuItem = entry.getValue();

            ItemStack itemStack = menuItem.build(null);
            List<String> actions = menuItem.getActions();
            if (actions != null && !actions.isEmpty()) {
                builder.items.put(slot, itemStack);
                builder.slotActions.put(slot, new ArrayList<>(actions));
            } else if (menuItem.getClickHandler() != null) {
                builder.items.put(slot, itemStack);
                builder.clickHandlers.put(slot, menuItem.getClickHandler());
            } else {
                builder.items.put(slot, itemStack);
            }
        }

        return builder;
    }

    public @NotNull GUIBuilder setPlaceholderProcessor(@Nullable BiFunction<String, Player, String> processor) {
        this.placeholderProcessor = processor;
        return this;
    }

    public @NotNull GUIBuilder setItem(int slot, @Nullable ItemStack item) {
        if (slot >= 0 && slot < size) {
            items.put(slot, item);
        }
        return this;
    }

    public @NotNull GUIBuilder setItem(int slot, @Nullable ItemStack item, @Nullable Consumer<InventoryClickEvent> handler) {
        setItem(slot, item);
        if (handler != null) {
            clickHandlers.put(slot, handler);
        }
        return this;
    }

    public @NotNull GUIBuilder setItem(int slot, @Nullable ItemStack item, @NotNull String action) {
        return setItem(slot, item, Collections.singletonList(action));
    }

    public @NotNull GUIBuilder setItem(int slot, @Nullable ItemStack item, @NotNull List<String> actions) {
        setItem(slot, item);
        if (actions != null && !actions.isEmpty()) {
            slotActions.put(slot, new ArrayList<>(actions));
        }
        return this;
    }

    public @NotNull GUIBuilder setItems(@NotNull Map<Integer, ItemStack> slotItems) {
        items.putAll(slotItems);
        return this;
    }

    public @NotNull GUIBuilder setItemAction(int slot, @Nullable ItemStack item, @NotNull String action) {
        return setItem(slot, item, action);
    }

    public @NotNull GUIBuilder setItemActions(int slot, @Nullable ItemStack item, @NotNull List<String> actions) {
        return setItem(slot, item, actions);
    }

    public @NotNull GUIBuilder setFiller(@NotNull Material material) {
        this.fillerItem = new ItemStack(material);
        return this;
    }

    public @NotNull GUIBuilder setFillerItem(@NotNull ItemStack filler) {
        this.fillerItem = filler;
        return this;
    }

    public @NotNull GUIBuilder fillRange(int start, int end, @NotNull ItemStack item) {
        for (int i = start; i <= end && i < size; i++) {
            if (!items.containsKey(i)) {
                items.put(i, item);
            }
        }
        return this;
    }

    public @NotNull GUIBuilder fillBorder(@NotNull ItemStack item) {
        for (int i = 0; i < 9; i++) {
            items.putIfAbsent(i, item);
            items.putIfAbsent(size - 9 + i, item);
        }
        for (int row = 1; row < (size / 9) - 1; row++) {
            items.putIfAbsent(row * 9, item);
            items.putIfAbsent(row * 9 + 8, item);
        }
        return this;
    }

    public @NotNull GUIBuilder onUpdateOpen(@NotNull Consumer<Player> handler) {
        this.openHandler = handler;
        return this;
    }

    public @NotNull GUIBuilder onClose(@NotNull Consumer<Player> handler) {
        this.closeHandler = handler;
        return this;
    }

    public @NotNull GUI build() {
        GUI gui = new GUI(title, size);

        items.forEach((slot, item) -> {
            if (placeholderProcessor != null && item != null) {
                ItemStack processedItem = processItemPlaceholders(item, null);
                gui.setItem(slot, processedItem);
            } else {
                gui.setItem(slot, item);
            }
        });

        clickHandlers.forEach((slot, handler) -> {
            gui.setClickHandler(slot, event -> {
                if (placeholderProcessor != null) {
                    ItemStack originalItem = items.get(slot);
                    ItemStack processedItem = processItemPlaceholders(originalItem, event.getWhoClicked() instanceof Player p ? p : null);
                    gui.getInventory().setItem(slot, processedItem);
                }
                handler.accept(event);
            });
        });

        slotActions.forEach((slot, actions) -> {
            gui.setClickHandler(slot, event -> {
                Player player = event.getWhoClicked() instanceof Player p ? p : null;
                if (player == null) return;

                if (placeholderProcessor != null) {
                    ItemStack originalItem = items.get(slot);
                    ItemStack processedItem = processItemPlaceholders(originalItem, player);
                    gui.getInventory().setItem(slot, processedItem);
                }

                ActionExecutor executor = new ActionExecutor(player);
                if (placeholderProcessor != null) {
                    executor = new ActionExecutor(player, placeholderProcessor);
                }
                executor.executeAll(actions);
            });
        });

        if (fillerItem != null) {
            gui.fillEmptySlots(fillerItem);
        }

        if (openHandler != null) {
            gui.onUpdateOpen(openHandler);
        }
        if (closeHandler != null) {
            gui.onClose(closeHandler);
        }

        return gui;
    }

    private ItemStack processItemPlaceholders(ItemStack item, Player player) {
        if (item == null) return null;
        return item;
    }

    private int validateSize(int size) {
        if (size % 9 != 0) {
            throw new IllegalArgumentException("GUI size must be a multiple of 9");
        }
        if (size < 9 || size > 54) {
            throw new IllegalArgumentException("GUI size must be between 9 and 54");
        }
        return size;
    }

    public String getTitle() {
        return title;
    }

    public int getSize() {
        return size;
    }

    public String getMenuId() {
        return menuId;
    }
}