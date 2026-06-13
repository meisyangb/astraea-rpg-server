package cn.guangdian.gift.model;

import java.util.ArrayList;
import java.util.List;

/**
 * 礼包配置模型
 */
public class GiftConfig {

    private String name;
    private String display;
    private List<String> description = new ArrayList<>();
    private GiftConditions conditions = new GiftConditions();
    private List<String> items = new ArrayList<>();

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDisplay() {
        return display;
    }

    public void setDisplay(String display) {
        this.display = display;
    }

    public List<String> getDescription() {
        return description;
    }

    public void setDescription(List<String> description) {
        this.description = description;
    }

    public GiftConditions getConditions() {
        return conditions;
    }

    public void setConditions(GiftConditions conditions) {
        this.conditions = conditions;
    }

    public List<String> getItems() {
        return items;
    }

    public void setItems(List<String> items) {
        this.items = items;
    }
}
