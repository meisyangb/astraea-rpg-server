package cn.guangdian.rpgcore.service;

import cn.guangdian.rpgcore.RPGCore;
import cn.guangdian.rpgcore.service.api.ConfigService;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * 统一配置服务实现
 *
 * <p>管理所有插件的配置，支持命名空间隔离。</p>
 *
 * @author GuangDian
 * @since 2.0.0
 */
public class SimpleConfigService implements ConfigService {

    private final RPGCore plugin;
    private final Logger logger;
    private final Map<String, ConfigHolder> holders = new ConcurrentHashMap<>();

    public SimpleConfigService(RPGCore plugin) {
        this.plugin = plugin;
        this.logger = plugin.getLogger();
    }

    // ==================== 基础操作 ====================

    @Override
    public String getString(String namespace, String path, String def) {
        ConfigHolder holder = getHolder(namespace);
        if (holder == null) {
            return def;
        }

        Object value = holder.get(path);
        return value != null ? String.valueOf(value) : def;
    }

    @Override
    public int getInt(String namespace, String path, int def) {
        ConfigHolder holder = getHolder(namespace);
        if (holder == null) {
            return def;
        }

        Object value = holder.get(path);
        if (value instanceof Number) {
            return ((Number) value).intValue();
        }
        if (value instanceof String) {
            try {
                return Integer.parseInt((String) value);
            } catch (NumberFormatException ignored) {
            }
        }
        return def;
    }

    @Override
    public long getLong(String namespace, String path, long def) {
        ConfigHolder holder = getHolder(namespace);
        if (holder == null) {
            return def;
        }

        Object value = holder.get(path);
        if (value instanceof Number) {
            return ((Number) value).longValue();
        }
        if (value instanceof String) {
            try {
                return Long.parseLong((String) value);
            } catch (NumberFormatException ignored) {
            }
        }
        return def;
    }

    @Override
    public double getDouble(String namespace, String path, double def) {
        ConfigHolder holder = getHolder(namespace);
        if (holder == null) {
            return def;
        }

        Object value = holder.get(path);
        if (value instanceof Number) {
            return ((Number) value).doubleValue();
        }
        if (value instanceof String) {
            try {
                return Double.parseDouble((String) value);
            } catch (NumberFormatException ignored) {
            }
        }
        return def;
    }

    @Override
    public boolean getBoolean(String namespace, String path, boolean def) {
        ConfigHolder holder = getHolder(namespace);
        if (holder == null) {
            return def;
        }

        Object value = holder.get(path);
        if (value instanceof Boolean) {
            return (Boolean) value;
        }
        if (value instanceof String) {
            return Boolean.parseBoolean((String) value);
        }
        return def;
    }

    @Override
    public Optional<String> getStringOptional(String namespace, String path) {
        ConfigHolder holder = getHolder(namespace);
        if (holder == null) {
            return Optional.empty();
        }

        Object value = holder.get(path);
        return value != null ? Optional.of(String.valueOf(value)) : Optional.empty();
    }

    @Override
    @SuppressWarnings("unchecked")
    public List<String> getStringList(String namespace, String path) {
        ConfigHolder holder = getHolder(namespace);
        if (holder == null) {
            return new ArrayList<>();
        }

        Object value = holder.get(path);
        if (value instanceof List) {
            List<String> result = new ArrayList<>();
            for (Object item : (List<?>) value) {
                if (item != null) {
                    result.add(String.valueOf(item));
                }
            }
            return result;
        }
        return new ArrayList<>();
    }

    @Override
    @SuppressWarnings("unchecked")
    public List<Integer> getIntList(String namespace, String path) {
        ConfigHolder holder = getHolder(namespace);
        if (holder == null) {
            return new ArrayList<>();
        }

        Object value = holder.get(path);
        if (value instanceof List) {
            List<Integer> result = new ArrayList<>();
            for (Object item : (List<?>) value) {
                if (item instanceof Number) {
                    result.add(((Number) item).intValue());
                } else if (item instanceof String) {
                    try {
                        result.add(Integer.parseInt((String) item));
                    } catch (NumberFormatException ignored) {
                    }
                }
            }
            return result;
        }
        return new ArrayList<>();
    }

    @Override
    public Set<String> getKeys(String namespace, String path) {
        ConfigHolder holder = getHolder(namespace);
        if (holder == null) {
            return new HashSet<>();
        }
        return holder.getKeys(path);
    }

    @Override
    public boolean contains(String namespace, String path) {
        ConfigHolder holder = getHolder(namespace);
        return holder != null && holder.contains(path);
    }

    // ==================== 写入操作 ====================

    @Override
    public void set(String namespace, String path, Object value) {
        ConfigHolder holder = getHolder(namespace);
        if (holder == null) {
            logger.warning("Cannot set config: namespace '" + namespace + "' not found");
            return;
        }
        holder.set(path, value);
    }

    @Override
    public void remove(String namespace, String path) {
        ConfigHolder holder = getHolder(namespace);
        if (holder != null) {
            holder.set(path, null);
        }
    }

    // ==================== 文件操作 ====================

    @Override
    public void save(String namespace) {
        ConfigHolder holder = getHolder(namespace);
        if (holder != null) {
            holder.save();
            logger.fine("Saved config: " + namespace);
        }
    }

    @Override
    public void saveAll() {
        for (String namespace : holders.keySet()) {
            try {
                save(namespace);
            } catch (Exception e) {
                logger.log(Level.WARNING, "Failed to save config: " + namespace, e);
            }
        }
    }

    @Override
    public void reload(String namespace) {
        ConfigHolder holder = getHolder(namespace);
        if (holder != null) {
            holder.reload();
            logger.fine("Reloaded config: " + namespace);
        }
    }

    @Override
    public void reloadAll() {
        for (String namespace : holders.keySet()) {
            try {
                reload(namespace);
            } catch (Exception e) {
                logger.log(Level.WARNING, "Failed to reload config: " + namespace, e);
            }
        }
    }

    // ==================== 命名空间管理 ====================

    @Override
    public void registerNamespace(String namespace, ConfigHolder configHolder) {
        if (namespace == null || configHolder == null) {
            throw new IllegalArgumentException("Namespace and holder cannot be null");
        }

        holders.put(namespace, configHolder);
        logger.info("Registered config namespace: " + namespace);
    }

    @Override
    public void unregisterNamespace(String namespace) {
        ConfigHolder removed = holders.remove(namespace);
        if (removed != null) {
            logger.info("Unregistered config namespace: " + namespace);
        }
    }

    @Override
    public boolean hasNamespace(String namespace) {
        return holders.containsKey(namespace);
    }

    @Override
    public Set<String> getNamespaces() {
        return new HashSet<>(holders.keySet());
    }

    // ==================== 扩展功能 ====================

    @Override
    @SuppressWarnings("unchecked")
    public Map<String, Object> getSection(String namespace, String path) {
        ConfigHolder holder = getHolder(namespace);
        if (holder == null) {
            return new HashMap<>();
        }

        Object value = holder.get(path);
        if (value instanceof Map) {
            return new HashMap<>((Map<String, Object>) value);
        }
        return new HashMap<>();
    }

    @Override
    public int getConfigVersion(String namespace) {
        return getInt(namespace, "config-version", 1);
    }

    @Override
    public boolean needsMigration(String namespace, int targetVersion) {
        int current = getConfigVersion(namespace);
        return current < targetVersion;
    }

    /**
     * 获取配置持有者
     */
    private ConfigHolder getHolder(String namespace) {
        if (namespace == null) {
            return null;
        }
        return holders.get(namespace);
    }

    /**
     * 获取注册的命名空间数量
     */
    public int getNamespaceCount() {
        return holders.size();
    }

    /**
     * 清空所有命名空间
     */
    public void clear() {
        holders.clear();
    }
}