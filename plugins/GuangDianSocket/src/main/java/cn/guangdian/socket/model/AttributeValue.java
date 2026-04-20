package cn.guangdian.socket.model;

/**
 * 属性值模型
 */
public abstract class AttributeValue {

    public abstract double getValue();

    public static AttributeValue of(double value) {
        return new FixedValue(value);
    }

    public static AttributeValue range(double min, double max) {
        return new RangeValue(min, max);
    }

    public AttributeValue merge(AttributeValue other) {
        if (this instanceof FixedValue && other instanceof FixedValue) {
            return new FixedValue(((FixedValue) this).value + ((FixedValue) other).value);
        }
        double min1 = this instanceof RangeValue ? ((RangeValue) this).min : this.getValue();
        double max1 = this instanceof RangeValue ? ((RangeValue) this).max : this.getValue();
        double min2 = other instanceof RangeValue ? ((RangeValue) other).min : other.getValue();
        double max2 = other instanceof RangeValue ? ((RangeValue) other).max : other.getValue();
        return new RangeValue(min1 + min2, max1 + max2);
    }

    public static class FixedValue extends AttributeValue {
        private final double value;

        public FixedValue(double value) {
            this.value = value;
        }

        @Override
        public double getValue() {
            return value;
        }
    }

    public static class RangeValue extends AttributeValue {
        private final double min;
        private final double max;

        public RangeValue(double min, double max) {
            this.min = min;
            this.max = max;
        }

        @Override
        public double getValue() {
            return (min + max) / 2;
        }

        public double getMin() { return min; }
        public double getMax() { return max; }
    }
}
