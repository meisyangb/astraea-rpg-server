package cn.guangdian.rpgskill.skill;

import cn.guangdian.rpgskill.executor.SkillExecutor;
import net.kyori.adventure.text.Component;

import java.util.List;
import java.util.Map;

/**
 * 技能定义
 * 描述技能的基本信息和执行参数
 */
public class SkillDefinition {

    private final String id;
    private final String name;
    private final Component displayName;
    private final List<Component> description;
    private final SkillType type;
    private final TriggerType trigger;
    private final long cooldown;
    private final double manaCost;
    private final SkillExecutor executor;
    private final Map<String, Object> params;

    public SkillDefinition(String id, String name, Component displayName,
                          List<Component> description, SkillType type,
                          TriggerType trigger, long cooldown, double manaCost,
                          SkillExecutor executor, Map<String, Object> params) {
        this.id = id;
        this.name = name;
        this.displayName = displayName;
        this.description = description;
        this.type = type;
        this.trigger = trigger;
        this.cooldown = cooldown;
        this.manaCost = manaCost;
        this.executor = executor;
        this.params = params;
    }

    public String getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public Component getDisplayName() {
        return displayName;
    }

    public List<Component> getDescription() {
        return description;
    }

    public SkillType getType() {
        return type;
    }

    public TriggerType getTrigger() {
        return trigger;
    }

    public long getCooldown() {
        return cooldown;
    }

    public double getManaCost() {
        return manaCost;
    }

    public SkillExecutor getExecutor() {
        return executor;
    }

    public Map<String, Object> getParams() {
        return params;
    }

    @SuppressWarnings("unchecked")
    public <T> T getParam(String key, T defaultValue) {
        Object value = params.get(key);
        if (value == null) {
            return defaultValue;
        }
        try {
            return (T) value;
        } catch (ClassCastException e) {
            return defaultValue;
        }
    }

    public boolean isActive() {
        return type == SkillType.ACTIVE;
    }

    public boolean isPassive() {
        return type == SkillType.PASSIVE;
    }

    public boolean isToggle() {
        return type == SkillType.TOGGLE;
    }

    /**
     * 创建技能定义的 Builder
     */
    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private String id;
        private String name;
        private Component displayName;
        private List<Component> description;
        private SkillType type = SkillType.ACTIVE;
        private TriggerType trigger = TriggerType.RIGHT_CLICK;
        private long cooldown = 0;
        private double manaCost = 0;
        private SkillExecutor executor;
        private Map<String, Object> params = Map.of();

        public Builder id(String id) {
            this.id = id;
            return this;
        }

        public Builder name(String name) {
            this.name = name;
            return this;
        }

        public Builder displayName(Component displayName) {
            this.displayName = displayName;
            return this;
        }

        public Builder description(List<Component> description) {
            this.description = description;
            return this;
        }

        public Builder type(SkillType type) {
            this.type = type;
            return this;
        }

        public Builder trigger(TriggerType trigger) {
            this.trigger = trigger;
            return this;
        }

        public Builder cooldown(long cooldown) {
            this.cooldown = cooldown;
            return this;
        }

        public Builder manaCost(double manaCost) {
            this.manaCost = manaCost;
            return this;
        }

        public Builder executor(SkillExecutor executor) {
            this.executor = executor;
            return this;
        }

        public Builder params(Map<String, Object> params) {
            this.params = params;
            return this;
        }

        public SkillDefinition build() {
            if (id == null || name == null || executor == null) {
                throw new IllegalStateException("id, name, executor 不能为空");
            }
            if (displayName == null) {
                displayName = Component.text(name);
            }
            return new SkillDefinition(id, name, displayName, description, type,
                    trigger, cooldown, manaCost, executor, params);
        }
    }
}
