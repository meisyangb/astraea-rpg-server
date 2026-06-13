package cn.guangdian.armorstats.data;

import java.util.concurrent.ThreadLocalRandom;

public abstract class AttributeValue {

    public abstract double getValue();

    public double getRandom() {
        return getValue();
    }

    public abstract AttributeValue merge(AttributeValue other);

    public static AttributeValue of(double value) {
        return new SingleValue(value);
    }

    public static AttributeValue ofRange(double min, double max) {
        return new RangeValue(min, max);
    }

    public static AttributeValue ofPercent(double value) {
        return new SingleValue(value);
    }

    public boolean isRange() {
        return this instanceof RangeValue;
    }

    public static class SingleValue extends AttributeValue {
        private final double value;

        public SingleValue(double value) {
            this.value = value;
        }

        @Override
        public double getValue() {
            return value;
        }

        @Override
        public AttributeValue merge(AttributeValue other) {
            if (other instanceof SingleValue) {
                return new SingleValue(this.value + ((SingleValue) other).value);
            } else if (other instanceof RangeValue) {
                return new RangeValue(this.value + ((RangeValue) other).getMin(),
                        this.value + ((RangeValue) other).getMax());
            }
            return this;
        }
    }

    public static class RangeValue extends AttributeValue {
        private final double min;
        private final double max;

        public RangeValue(double min, double max) {
            this.min = min;
            this.max = max;
        }

        public double getMin() {
            return min;
        }

        public double getMax() {
            return max;
        }

        @Override
        public double getValue() {
            return (min + max) / 2.0;
        }

        @Override
        public double getRandom() {
            return min + ThreadLocalRandom.current().nextDouble() * (max - min);
        }

        @Override
        public AttributeValue merge(AttributeValue other) {
            if (other instanceof SingleValue) {
                return new RangeValue(this.min + ((SingleValue) other).value,
                        this.max + ((SingleValue) other).value);
            } else if (other instanceof RangeValue) {
                return new RangeValue(this.min + ((RangeValue) other).min,
                        this.max + ((RangeValue) other).max);
            }
            return this;
        }
    }
}
