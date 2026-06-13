package cn.guangdian.rpgcore.api;

/**
 * 服务优先级枚举
 * 
 * <p>定义服务注册的优先级，当存在多个服务实现时，
 * 优先级高的实现将被优先返回。</p>
 * 
 * @author GuangDian
 * @since 1.0.0
 */
public enum ServicePriority {

    /**
     * 最高优先级
     */
    HIGHEST(5),

    /**
     * 高优先级
     */
    HIGH(4),

    /**
     * 普通优先级（默认）
     */
    NORMAL(3),

    /**
     * 低优先级
     */
    LOW(2),

    /**
     * 最低优先级
     */
    LOWEST(1);

    private final int level;

    ServicePriority(int level) {
        this.level = level;
    }

    /**
     * 获取优先级级别
     * 
     * @return 优先级级别数值
     */
    public int getLevel() {
        return level;
    }

    /**
     * 比较两个优先级
     * 
     * @param other 要比较的优先级
     * @return 如果当前优先级更高返回正数，更低返回负数，相等返回0
     */
    public int compareLevel(ServicePriority other) {
        return Integer.compare(this.level, other.level);
    }

    /**
     * 检查当前优先级是否高于指定优先级
     * 
     * @param other 要比较的优先级
     * @return 如果当前优先级更高返回 true
     */
    public boolean isHigherThan(ServicePriority other) {
        return this.level > other.level;
    }
}