package cn.guangdian.regen.model;

import org.bukkit.Material;

import java.util.ArrayList;
import java.util.List;

/**
 * 刷新方块配置
 */
public class RegenBlock {

    private final Material original;
    private final Material replace;
    private final int delay; // 秒
    private final List<DropConfig> drops = new ArrayList<>();

    public RegenBlock(Material original, Material replace, int delay) {
        this.original = original;
        this.replace = replace;
        this.delay = delay;
    }

    /**
     * 添加掉落配置
     */
    public void addDrop(DropConfig drop) {
        drops.add(drop);
    }

    // Getters

    public Material getOriginal() {
        return original;
    }

    public Material getReplace() {
        return replace;
    }

    public int getDelay() {
        return delay;
    }

    public List<DropConfig> getDrops() {
        return drops;
    }

    /**
     * 掉落配置
     */
    public static class DropConfig {
        private final DropType type;
        private final String value;
        private final int minAmount;
        private final int maxAmount;
        private final double chance; // 0.0 ~ 1.0

        public DropConfig(DropType type, String value, int minAmount, int maxAmount) {
            this(type, value, minAmount, maxAmount, 1.0);
        }

        public DropConfig(DropType type, String value, int minAmount, int maxAmount, double chance) {
            this.type = type;
            this.value = value;
            this.minAmount = minAmount;
            this.maxAmount = maxAmount;
            this.chance = Math.min(1.0, Math.max(0.0, chance));
        }

        public DropType getType() {
            return type;
        }

        public String getValue() {
            return value;
        }

        public int getMinAmount() {
            return minAmount;
        }

        public int getMaxAmount() {
            return maxAmount;
        }

        public double getChance() {
            return chance;
        }

        /**
         * 获取随机数量
         */
        public int getRandomAmount() {
            if (minAmount == maxAmount) {
                return minAmount;
            }
            return minAmount + (int) (Math.random() * (maxAmount - minAmount + 1));
        }
    }

    /**
     * 掉落类型
     */
    public enum DropType {
        ITEM,       // 普通物品
        EXPERIENCE, // 经验值
        COMMAND     // 命令
    }
}
