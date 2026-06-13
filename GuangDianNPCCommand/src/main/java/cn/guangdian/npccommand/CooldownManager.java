package cn.guangdian.npccommand;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class CooldownManager {

    private final GuangDianNPCCommand plugin;
    private final Map<String, Long> cooldowns;

    public CooldownManager(GuangDianNPCCommand plugin) {
        this.plugin = plugin;
        this.cooldowns = new ConcurrentHashMap<>();
    }

    private String getKey(UUID playerUUID, int npcId) {
        return playerUUID.toString() + ":" + npcId;
    }

    public boolean isOnCooldown(UUID playerUUID, int npcId) {
        String key = getKey(playerUUID, npcId);
        Long cooldownEnd = cooldowns.get(key);
        if (cooldownEnd == null) {
            return false;
        }
        if (System.currentTimeMillis() >= cooldownEnd) {
            cooldowns.remove(key);
            return false;
        }
        return true;
    }

    public long getRemainingCooldown(UUID playerUUID, int npcId) {
        String key = getKey(playerUUID, npcId);
        Long cooldownEnd = cooldowns.get(key);
        if (cooldownEnd == null) {
            return 0;
        }
        long remaining = cooldownEnd - System.currentTimeMillis();
        if (remaining <= 0) {
            cooldowns.remove(key);
            return 0;
        }
        return remaining;
    }

    public void setCooldown(UUID playerUUID, int npcId, long cooldownSeconds) {
        if (cooldownSeconds <= 0) {
            return;
        }
        String key = getKey(playerUUID, npcId);
        cooldowns.put(key, System.currentTimeMillis() + (cooldownSeconds * 1000));
    }

    public void clearCooldown(UUID playerUUID, int npcId) {
        cooldowns.remove(getKey(playerUUID, npcId));
    }

    public void clearAllCooldowns() {
        cooldowns.clear();
    }

    public String formatCooldown(long milliseconds) {
        long seconds = milliseconds / 1000;
        long minutes = seconds / 60;
        long hours = minutes / 60;
        long days = hours / 24;

        String displayType = plugin.getConfig().getString("cooldown-display", "MEDIUM");

        switch (displayType.toUpperCase()) {
            case "SHORT":
                return formatShort(days, hours % 24, minutes % 60, seconds % 60);
            case "FULL":
                return formatFull(days, hours % 24, minutes % 60, seconds % 60);
            case "MEDIUM":
            default:
                return formatMedium(days, hours % 24, minutes % 60, seconds % 60);
        }
    }

    private String formatShort(long days, long hours, long minutes, long seconds) {
        StringBuilder sb = new StringBuilder();
        if (days > 0) sb.append(days).append("d ");
        if (hours > 0) sb.append(hours).append("h ");
        if (minutes > 0) sb.append(minutes).append("m ");
        if (seconds > 0 || sb.length() == 0) sb.append(seconds).append("s");
        return sb.toString().trim();
    }

    private String formatMedium(long days, long hours, long minutes, long seconds) {
        StringBuilder sb = new StringBuilder();
        if (days > 0) sb.append(days).append("天 ");
        if (hours > 0) sb.append(hours).append("小时 ");
        if (minutes > 0) sb.append(minutes).append("分钟 ");
        if (seconds > 0 || sb.length() == 0) sb.append(seconds).append("秒");
        return sb.toString().trim();
    }

    private String formatFull(long days, long hours, long minutes, long seconds) {
        StringBuilder sb = new StringBuilder();
        if (days > 0) sb.append(days).append(days == 1 ? " day " : " days ");
        if (hours > 0) sb.append(hours).append(hours == 1 ? " hour " : " hours ");
        if (minutes > 0) sb.append(minutes).append(minutes == 1 ? " minute " : " minutes ");
        if (seconds > 0 || sb.length() == 0) sb.append(seconds).append(seconds == 1 ? " second" : " seconds");
        return sb.toString().trim();
    }
}
