package cn.guangdian.villagertrade.recipe;

import org.bukkit.inventory.ItemStack;

import java.util.List;

/**
 * 兑换配方
 *
 * <p>定义一个完整的兑换规则</p>
 */
public class TradeRecipe {

    private final String name;
    private final String displayName;
    private final String displayMaterial;
    private final List<String> displayLore;
    private final List<TradeIngredient> inputs;
    private final ItemStack output;
    private final String permission;
    private final int dailyLimit;
    private final int maxUses;
    private final boolean giveExperience;
    private final int experienceAmount;

    public TradeRecipe(String name, String displayName, String displayMaterial, List<String> displayLore,
                       List<TradeIngredient> inputs, ItemStack output, String permission,
                       int dailyLimit, int maxUses, boolean giveExperience, int experienceAmount) {
        this.name = name;
        this.displayName = displayName;
        this.displayMaterial = displayMaterial;
        this.displayLore = displayLore;
        this.inputs = inputs;
        this.output = output;
        this.permission = permission;
        this.dailyLimit = dailyLimit;
        this.maxUses = maxUses;
        this.giveExperience = giveExperience;
        this.experienceAmount = experienceAmount;
    }

    /**
     * 获取配方名称
     *
     * @return 配方名称
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
     * 获取输入材料列表
     *
     * @return 输入材料列表
     */
    public List<TradeIngredient> getInputs() {
        return inputs;
    }

    /**
     * 获取输出物品
     *
     * @return 输出物品
     */
    public ItemStack getOutput() {
        return output.clone();
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
     * 获取每日限制
     *
     * @return 每日限制，-1表示无限制
     */
    public int getDailyLimit() {
        return dailyLimit;
    }

    /**
     * 获取最大使用次数
     *
     * @return 最大使用次数
     */
    public int getMaxUses() {
        return maxUses;
    }

    /**
     * 是否给予经验
     *
     * @return 是否给予经验
     */
    public boolean isGiveExperience() {
        return giveExperience;
    }

    /**
     * 获取经验数量
     *
     * @return 经验数量
     */
    public int getExperienceAmount() {
        return experienceAmount;
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
     * 是否有每日限制
     *
     * @return 是否有每日限制
     */
    public boolean hasDailyLimit() {
        return dailyLimit > 0;
    }
}
