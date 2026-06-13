package cn.guangdian.villagertrade.recipe;

import org.bukkit.inventory.ItemStack;

import java.util.List;

/**
 * 配方组
 *
 * <p>包含多个子配方的组合，打开后显示所有子配方</p>
 */
public class RecipeGroup {

    private final String name;
    private final String displayName;
    private final String displayMaterial;
    private final List<String> displayLore;
    private final List<TradeRecipe> recipes;
    private final String permission;

    public RecipeGroup(String name, String displayName, String displayMaterial, List<String> displayLore,
                       List<TradeRecipe> recipes, String permission) {
        this.name = name;
        this.displayName = displayName;
        this.displayMaterial = displayMaterial;
        this.displayLore = displayLore;
        this.recipes = recipes;
        this.permission = permission;
    }

    /**
     * 获取配方组名称
     *
     * @return 配方组名称
     */
    public String getName() {
        return name;
    }

    /**
     * 获取显示名称
     *
     * @return 显示名称
     */
    public String getDisplayName() {
        return displayName;
    }

    /**
     * 获取显示材料
     *
     * @return 显示材料
     */
    public String getDisplayMaterial() {
        return displayMaterial;
    }

    /**
     * 获取显示Lore
     *
     * @return 显示Lore
     */
    public List<String> getDisplayLore() {
        return displayLore;
    }

    /**
     * 获取子配方列表
     *
     * @return 子配方列表
     */
    public List<TradeRecipe> getRecipes() {
        return recipes;
    }

    /**
     * 获取权限
     *
     * @return 权限字符串
     */
    public String getPermission() {
        return permission;
    }

    /**
     * 是否有权限限制
     *
     * @return 是否有权限限制
     */
    public boolean hasPermission() {
        return permission != null && !permission.isEmpty();
    }

    /**
     * 获取子配方数量
     *
     * @return 子配方数量
     */
    public int getRecipeCount() {
        return recipes.size();
    }
}
