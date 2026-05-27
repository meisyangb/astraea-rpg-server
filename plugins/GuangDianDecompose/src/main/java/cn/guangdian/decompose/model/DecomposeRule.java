package cn.guangdian.decompose.model;

import java.util.ArrayList;
import java.util.List;

public class DecomposeRule {

    private final String itemId;
    private final List<MaterialReward> materials;
    private double chance;
    private String message;

    public DecomposeRule(String itemId) {
        this.itemId = itemId;
        this.materials = new ArrayList<>();
        this.chance = 1.0;
        this.message = "";
    }

    public String getItemId() {
        return itemId;
    }

    public List<MaterialReward> getMaterials() {
        return materials;
    }

    public void addMaterial(MaterialReward material) {
        this.materials.add(material);
    }

    public double getChance() {
        return chance;
    }

    public void setChance(double chance) {
        this.chance = chance;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public boolean hasCustomMessage() {
        return message != null && !message.isEmpty();
    }

    public static class MaterialReward {
        private final String type;
        private final String itemId;
        private final int amount;

        public MaterialReward(String type, String itemId, int amount) {
            this.type = type;
            this.itemId = itemId;
            this.amount = amount;
        }

        public String getType() {
            return type;
        }

        public String getItemId() {
            return itemId;
        }

        public int getAmount() {
            return amount;
        }

        public boolean isMythicItem() {
            return "mm".equalsIgnoreCase(type) || "mythicmobs".equalsIgnoreCase(type);
        }

        public boolean isVanillaItem() {
            return "vanilla".equalsIgnoreCase(type);
        }

        @Override
        public String toString() {
            return type + ":" + itemId + ":" + amount;
        }
    }
}
