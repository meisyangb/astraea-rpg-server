package cn.guangdian.rpgcore.util;

import org.bukkit.NamespacedKey;

public final class PDCKeys {

    private PDCKeys() {
        throw new UnsupportedOperationException("工具类不允许实例化");
    }

    public static final String PLUGIN_NAMESPACE = "rpgcore";

    public static NamespacedKey key(String key) {
        return new NamespacedKey(PLUGIN_NAMESPACE, key);
    }

    public static NamespacedKey playerData(String key) {
        return key("player_" + key);
    }

    public static NamespacedKey itemData(String key) {
        return key("item_" + key);
    }

    public static NamespacedKey entityData(String key) {
        return key("entity_" + key);
    }

    public static NamespacedKey custom(String namespace, String key) {
        return new NamespacedKey(namespace, key);
    }

    public static NamespacedKeyMythicMobs withMythicMobsType() {
        return new NamespacedKeyMythicMobs();
    }

    public static class NamespacedKeyMythicMobs {
        private static final String MYTHIC_NAMESPACE = "mythicmobs";

        public NamespacedKey type() {
            return new NamespacedKey(MYTHIC_NAMESPACE, "type");
        }

        public NamespacedKey mobId() {
            return new NamespacedKey(MYTHIC_NAMESPACE, "mobid");
        }

        public NamespacedKey identifier() {
            return new NamespacedKey(MYTHIC_NAMESPACE, "identifier");
        }
    }
}
