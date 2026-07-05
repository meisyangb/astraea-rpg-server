package cn.guangdian.socket.constant;

import org.bukkit.NamespacedKey;
import org.bukkit.persistence.PersistentDataType;

import java.util.Arrays;
import java.util.List;

/**
 * 镶嵌槽位枚举 - 固定槽位，无遍历
 */
public enum SocketSlot {
    SLOT_0(0),
    SLOT_1(1),
    SLOT_2(2),
    SLOT_3(3),
    SLOT_4(4),
    SLOT_5(5),
    SLOT_6(6);

    private static final String NAMESPACE = "rpgitems";
    private static final String GEM_PREFIX = "gem_";
    private static final String SOCKET_PREFIX = "socket_";
    private static final String LORE_INDEX_PREFIX = "lore_idx_";

    private final int index;

    SocketSlot(int index) {
        this.index = index;
    }

    public int getIndex() {
        return index;
    }

    // PDC Key - 宝石ID
    public NamespacedKey getGemKey() {
        return new NamespacedKey(NAMESPACE, GEM_PREFIX + index);
    }

    // PDC Key - 槽位类型
    public NamespacedKey getSocketKey() {
        return new NamespacedKey(NAMESPACE, SOCKET_PREFIX + index);
    }

    // PDC Key - Lore 行索引（用于快速定位）
    public NamespacedKey getLoreIndexKey() {
        return new NamespacedKey(NAMESPACE, LORE_INDEX_PREFIX + index);
    }

    // 静态常量列表 - 避免每次创建
    public static final List<SocketSlot> VALUES = Arrays.asList(values());

    // 根据索引获取枚举 - O(1)
    public static SocketSlot fromIndex(int index) {
        if (index < 0 || index >= VALUES.size()) {
            return null;
        }
        return VALUES.get(index);
    }

    // 获取槽位数量
    public static int size() {
        return VALUES.size();
    }
}