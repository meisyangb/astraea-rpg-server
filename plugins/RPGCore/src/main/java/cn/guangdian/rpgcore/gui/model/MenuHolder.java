package cn.guangdian.rpgcore.gui.model;

import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.jetbrains.annotations.NotNull;

public class MenuHolder implements InventoryHolder {

    private final String menuName;
    private final String menuId;

    public MenuHolder(String menuName) {
        this.menuName = menuName;
        this.menuId = menuName.toLowerCase();
    }

    public MenuHolder(String menuName, String menuId) {
        this.menuName = menuName;
        this.menuId = menuId != null ? menuId.toLowerCase() : menuName.toLowerCase();
    }

    @Override
    public @NotNull Inventory getInventory() {
        return null;
    }

    public String getMenuName() {
        return menuName;
    }

    public String getMenuId() {
        return menuId;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;
        MenuHolder that = (MenuHolder) obj;
        return menuId.equals(that.menuId);
    }

    @Override
    public int hashCode() {
        return menuId.hashCode();
    }
}