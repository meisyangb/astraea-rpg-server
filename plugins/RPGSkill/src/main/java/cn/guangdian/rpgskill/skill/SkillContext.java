package cn.guangdian.rpgskill.skill;

import org.bukkit.Location;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;

import java.util.HashMap;
import java.util.Map;

/**
 * 技能执行上下文
 * 包含技能执行所需的所有信息
 */
public class SkillContext {

    private final Player caster;
    private LivingEntity target;
    private Location location;
    private double baseDamage;
    private final Map<String, Object> extraParams;

    private SkillContext(Player caster) {
        this.caster = caster;
        this.extraParams = new HashMap<>();
    }

    public Player getCaster() {
        return caster;
    }

    public LivingEntity getTarget() {
        return target;
    }

    public void setTarget(LivingEntity target) {
        this.target = target;
    }

    public Location getLocation() {
        return location != null ? location : caster.getLocation();
    }

    public void setLocation(Location location) {
        this.location = location;
    }

    public double getBaseDamage() {
        return baseDamage;
    }

    public void setBaseDamage(double baseDamage) {
        this.baseDamage = baseDamage;
    }

    public Map<String, Object> getExtraParams() {
        return extraParams;
    }

    public void setExtraParam(String key, Object value) {
        extraParams.put(key, value);
    }

    @SuppressWarnings("unchecked")
    public <T> T getExtraParam(String key, T defaultValue) {
        Object value = extraParams.get(key);
        if (value == null) {
            return defaultValue;
        }
        try {
            return (T) value;
        } catch (ClassCastException e) {
            return defaultValue;
        }
    }

    /**
     * 创建上下文 Builder
     */
    public static Builder builder(Player caster) {
        return new Builder(caster);
    }

    public static class Builder {
        private final SkillContext context;

        private Builder(Player caster) {
            this.context = new SkillContext(caster);
        }

        public Builder target(LivingEntity target) {
            context.setTarget(target);
            return this;
        }

        public Builder location(Location location) {
            context.setLocation(location);
            return this;
        }

        public Builder baseDamage(double baseDamage) {
            context.setBaseDamage(baseDamage);
            return this;
        }

        public Builder extraParam(String key, Object value) {
            context.setExtraParam(key, value);
            return this;
        }

        public SkillContext build() {
            return context;
        }
    }
}
