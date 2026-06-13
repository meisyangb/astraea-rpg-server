package cn.guangdian.devour.data;

/**
 * 属性值模型
 * 支持单值、范围值和百分比值
 * 
 * @author Astraea RPG Team
 * @since 1.0.0
 */
public class AttributeValue {
    
    /** 属性值类型 */
    public enum Type {
        SINGLE,     // 单值 (如: 100)
        RANGE,      // 范围值 (如: 100-200)
        PERCENT     // 百分比值 (如: 50%)
    }
    
    private final Type type;
    private final double min;
    private final double max;
    
    // 单值构造
    private AttributeValue(double value) {
        this.type = Type.SINGLE;
        this.min = value;
        this.max = value;
    }
    
    // 范围值构造
    private AttributeValue(double min, double max) {
        this.type = Type.RANGE;
        this.min = min;
        this.max = max;
    }
    
    // 百分比构造
    private AttributeValue(double percent, boolean isPercent) {
        this.type = Type.PERCENT;
        this.min = percent;
        this.max = percent;
    }
    
    /**
     * 创建单值
     */
    public static AttributeValue of(double value) {
        return new AttributeValue(value);
    }
    
    /**
     * 创建范围值
     */
    public static AttributeValue ofRange(double min, double max) {
        return new AttributeValue(min, max);
    }
    
    /**
     * 创建百分比值
     */
    public static AttributeValue ofPercent(double percent) {
        return new AttributeValue(percent, true);
    }
    
    /**
     * 获取类型
     */
    public Type getType() {
        return type;
    }
    
    /**
     * 获取最小值
     */
    public double getMin() {
        return min;
    }
    
    /**
     * 获取最大值
     */
    public double getMax() {
        return max;
    }
    
    /**
     * 获取值 (单值或百分比)
     */
    public double getValue() {
        return min;
    }
    
    /**
     * 是否是范围值
     */
    public boolean isRange() {
        return type == Type.RANGE;
    }
    
    /**
     * 是否是百分比值
     */
    public boolean isPercent() {
        return type == Type.PERCENT;
    }
    
    /**
     * 合并两个属性值 (累加)
     */
    public static AttributeValue merge(AttributeValue a, AttributeValue b) {
        if (a == null) return b;
        if (b == null) return a;

        // 类型不同时，以第一个值的类型为准进行累加
        if (a.type != b.type) {
            switch (a.type) {
                case SINGLE:
                    return new AttributeValue(a.min + b.min);
                case RANGE:
                    return new AttributeValue(a.min + b.min, a.max + b.max);
                case PERCENT:
                    return new AttributeValue(a.min + b.min, true);
                default:
                    return a;
            }
        }

        switch (a.type) {
            case SINGLE:
                return new AttributeValue(a.min + b.min);

            case RANGE:
                return new AttributeValue(a.min + b.min, a.max + b.max);

            case PERCENT:
                return new AttributeValue(a.min + b.min, true);

            default:
                return a;
        }
    }
    
    /**
     * 格式化为字符串
     */
    public String format() {
        switch (type) {
            case SINGLE:
                return String.format("%.0f", min);
                
            case RANGE:
                return String.format("%.0f-%.0f", min, max);
                
            case PERCENT:
                return String.format("%.1f%%", min);
                
            default:
                return String.valueOf(min);
        }
    }
    
    /**
     * 序列化为JSON字符串
     */
    public String toJson() {
        switch (type) {
            case SINGLE:
                return String.format("{\"type\":\"SINGLE\",\"value\":%.2f}", min);
                
            case RANGE:
                return String.format("{\"type\":\"RANGE\",\"min\":%.2f,\"max\":%.2f}", min, max);
                
            case PERCENT:
                return String.format("{\"type\":\"PERCENT\",\"value\":%.2f}", min);
                
            default:
                return "{}";
        }
    }
    
    @Override
    public String toString() {
        return format();
    }
}
