package cn.guangdian.particleblocker;

import com.comphenix.protocol.PacketType;
import com.comphenix.protocol.events.ListenerPriority;
import com.comphenix.protocol.events.PacketAdapter;
import com.comphenix.protocol.events.PacketEvent;
import com.comphenix.protocol.reflect.StructureModifier;
import org.bukkit.entity.Player;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * ProtocolLib 粒子拦截器
 *
 * Paper 1.21.6 粒子提取策略：
 *   遍历数据包所有字段，寻找 ParticleOptions 实例
 *   然后通过 NMS 反射获取注册名
 */
public class ParticlePacketListener {

    private final GuangDianParticleBlocker plugin;
    private final ParticleConfig config;
    private final Map<UUID, Boolean> playerBlocked = new ConcurrentHashMap<>();

    private long totalBlocked = 0;
    private long totalAllowed = 0;
    private PacketAdapter packetAdapter;

    // ---- NMS 反射缓存 ----
    private Class<?> particleOptionsClass;
    private Method particleOptionsGetType;
    private Object builtinParticleRegistry;
    private Method registryGetKeyMethod;
    private boolean reflectionReady = false;

    // 监听模式：记录所有遇到的不同粒子名
    private final Set<String> discoveredParticles = ConcurrentHashMap.newKeySet();
    private long monitorLogged = 0;

    // 详细调试：前 N 个数据包打印完整结构
    private int detailedDebugCount = 0;
    private static final int DETAILED_DEBUG_LIMIT = 10;

    public ParticlePacketListener(GuangDianParticleBlocker plugin, ParticleConfig config) {
        this.plugin = plugin;
        this.config = config;
        initReflection();
        registerPacketListener();
    }

    private void initReflection() {
        try {
            particleOptionsClass = Class.forName("net.minecraft.core.particles.ParticleOptions");
            particleOptionsGetType = particleOptionsClass.getMethod("getType");

            Class<?> builtInRegistries = Class.forName("net.minecraft.core.registries.BuiltInRegistries");
            Field particleTypeField = builtInRegistries.getField("PARTICLE_TYPE");
            builtinParticleRegistry = particleTypeField.get(null);

            Class<?> registryClass = builtinParticleRegistry.getClass();
            // 尝试多种 getKey 方法签名
            try {
                registryGetKeyMethod = registryClass.getMethod("getKey", Object.class);
            } catch (NoSuchMethodException e) {
                // 某些版本可能是 getOptionalKey 或 getResourceKey
                for (Method m : registryClass.getMethods()) {
                    if (m.getName().equals("getKey") && m.getParameterCount() == 1) {
                        registryGetKeyMethod = m;
                        break;
                    }
                }
                if (registryGetKeyMethod == null) throw e;
            }

            reflectionReady = true;
            plugin.getLogger().info("[粒子拦截] NMS 反射初始化成功");

            // 测试
            Class<?> particleTypes = Class.forName("net.minecraft.core.particles.ParticleTypes");
            Field flameField = particleTypes.getField("FLAME");
            Object flameType = flameField.get(null);
            Object key = registryGetKeyMethod.invoke(builtinParticleRegistry, flameType);
            plugin.getLogger().info("[粒子拦截] 反射测试: FLAME → " + key);
        } catch (Exception e) {
            plugin.getLogger().severe("[粒子拦截] NMS 反射初始化失败: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void registerPacketListener() {
        packetAdapter = new PacketAdapter(
            plugin, ListenerPriority.LOW, PacketType.Play.Server.WORLD_PARTICLES
        ) {
            @Override
            public void onPacketSending(PacketEvent event) {
                Player player = event.getPlayer();

                if (playerBlocked.getOrDefault(player.getUniqueId(), false)) {
                    event.setCancelled(true); totalBlocked++; return;
                }
                if (config.isGlobalBlocked()) {
                    event.setCancelled(true); totalBlocked++; return;
                }

                // 提取粒子名
                String particleName = extractParticleName(event);

                // 监听模式：不屏蔽，只记录
                if (config.isMonitorMode()) {
                    if (particleName != null && discoveredParticles.add(particleName)) {
                        monitorLogged++;
                        plugin.getLogger().info("[粒子监听] 发现新粒子: " + particleName + " (累计: " + discoveredParticles.size() + " 种)");
                    } else if (particleName == null && discoveredParticles.add("<UNKNOWN>")) {
                        plugin.getLogger().info("[粒子监听] 发现无法识别的粒子 (累计: " + discoveredParticles.size() + " 种)");
                    }

                    // 详细调试：前几个数据包打印完整结构
                    if (detailedDebugCount < DETAILED_DEBUG_LIMIT) {
                        dumpPacketStructure(event, particleName);
                        detailedDebugCount++;
                    }

                    totalAllowed++;
                    return;
                }

                // 正常模式：按规则屏蔽
                if (particleName != null) {
                    if (config.shouldBlockByName(particleName)) {
                        event.setCancelled(true);
                        totalBlocked++;
                        return;
                    }
                } else {
                    if (config.isWhitelistMode()) {
                        event.setCancelled(true);
                        totalBlocked++;
                        return;
                    }
                }

                totalAllowed++;
            }
        };

        com.comphenix.protocol.ProtocolLibrary.getProtocolManager().addPacketListener(packetAdapter);
        plugin.getLogger().info("[粒子拦截] 已注册 (监听+反射模式)");
    }

    /**
     * 提取粒子名：遍历数据包所有字段寻找 ParticleOptions 实例
     */
    private String extractParticleName(PacketEvent event) {
        StructureModifier<Object> modifier = event.getPacket().getModifier();

        // 遍历所有字段，找 ParticleOptions 实例
        for (int i = 0; i < modifier.size(); i++) {
            try {
                Object field = modifier.read(i);
                if (field == null) continue;

                // 检查是否是 ParticleOptions 实例
                if (particleOptionsClass != null && particleOptionsClass.isInstance(field)) {
                    String name = resolveParticleName(field);
                    if (name != null) return name;
                }
            } catch (Exception ignored) {}
        }

        // 回退：尝试 ProtocolLib 的 getParticles()
        try {
            StructureModifier<?> particleModifier = event.getPacket().getParticles();
            if (particleModifier != null && particleModifier.size() > 0) {
                Object particle = particleModifier.read(0);
                if (particle != null) {
                    String name = resolveParticleName(particle);
                    if (name != null) return name;

                    // toString 回退
                    String str = particle.toString();
                    String extracted = extractFromToString(str);
                    if (extracted != null) return extracted;
                }
            }
        } catch (Exception ignored) {}

        return null;
    }

    /**
     * 通过 NMS 反射从 ParticleOptions 获取注册名
     */
    private String resolveParticleName(Object particleOptions) {
        if (!reflectionReady) return null;
        try {
            Object particleType = particleOptionsGetType.invoke(particleOptions);
            if (particleType == null) return null;

            Object key = registryGetKeyMethod.invoke(builtinParticleRegistry, particleType);
            if (key == null) return null;

            String fullName = key.toString();
            if (fullName.startsWith("minecraft:")) {
                return fullName.substring(10).toUpperCase();
            }
            return fullName.toUpperCase();
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * 从 toString() 中提取粒子名
     */
    private String extractFromToString(String str) {
        // 尝试 "type=<name>" 模式
        int typeIdx = str.indexOf("type=");
        if (typeIdx >= 0) {
            int start = typeIdx + 5;
            int end = str.indexOf(',', start);
            if (end < 0) end = str.indexOf('}', start);
            if (end > start) {
                String typeName = str.substring(start, end).trim();
                if (typeName.contains(":")) {
                    typeName = typeName.substring(typeName.indexOf(':') + 1);
                }
                return typeName.toUpperCase();
            }
        }
        return null;
    }

    /**
     * 详细打印数据包结构（用于调试）
     */
    private void dumpPacketStructure(PacketEvent event, String extractedName) {
        StringBuilder sb = new StringBuilder();
        sb.append("[粒子调试] 数据包 #").append(detailedDebugCount + 1).append(":\n");
        sb.append("  提取结果: ").append(extractedName != null ? extractedName : "UNKNOWN").append("\n");

        StructureModifier<Object> modifier = event.getPacket().getModifier();
        sb.append("  字段数: ").append(modifier.size()).append("\n");

        for (int i = 0; i < modifier.size(); i++) {
            try {
                Object field = modifier.read(i);
                if (field == null) {
                    sb.append("  [").append(i).append("] null\n");
                } else {
                    String className = field.getClass().getName();
                    String value = field.toString();
                    // 截断过长的值
                    if (value.length() > 100) value = value.substring(0, 100) + "...";
                    sb.append("  [").append(i).append("] ").append(className)
                      .append(" = ").append(value).append("\n");

                    // 如果是 ParticleOptions，尝试反射提取
                    if (particleOptionsClass != null && particleOptionsClass.isInstance(field)) {
                        String resolved = resolveParticleName(field);
                        sb.append("    → 反射解析: ").append(resolved != null ? resolved : "失败").append("\n");
                    }
                }
            } catch (Exception e) {
                sb.append("  [").append(i).append("] 读取失败: ").append(e.getMessage()).append("\n");
            }
        }

        // 尝试 getParticles()
        try {
            StructureModifier<?> pm = event.getPacket().getParticles();
            sb.append("  getParticles() 字段数: ").append(pm.size()).append("\n");
            for (int i = 0; i < pm.size(); i++) {
                try {
                    Object p = pm.read(i);
                    sb.append("  particles[").append(i).append("] = ")
                      .append(p != null ? p.getClass().getName() + " = " + p.toString() : "null").append("\n");
                } catch (Exception e) {
                    sb.append("  particles[").append(i).append("] 读取失败: ").append(e.getMessage()).append("\n");
                }
            }
        } catch (Exception e) {
            sb.append("  getParticles() 失败: ").append(e.getMessage()).append("\n");
        }

        plugin.getLogger().info(sb.toString());
    }

    public void unregister() {
        if (packetAdapter != null) {
            com.comphenix.protocol.ProtocolLibrary.getProtocolManager().removePacketListener(packetAdapter);
        }
    }

    public void setPlayerBlocked(UUID playerId, boolean blocked) {
        if (blocked) playerBlocked.put(playerId, true);
        else playerBlocked.remove(playerId);
    }

    public boolean isPlayerBlocked(UUID playerId) {
        return playerBlocked.getOrDefault(playerId, false);
    }

    public long getTotalBlocked() { return totalBlocked; }
    public long getTotalAllowed() { return totalAllowed; }
    public Set<String> getDiscoveredParticles() { return discoveredParticles; }

    public void resetStats() {
        totalBlocked = 0;
        totalAllowed = 0;
        discoveredParticles.clear();
        monitorLogged = 0;
        detailedDebugCount = 0;
    }
}
