package cn.guangdian.mobhealth;

import cn.guangdian.rpgcore.RPGCore;
import cn.guangdian.rpgcore.message.MiniMessageService;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import net.kyori.adventure.text.minimessage.tag.resolver.TagResolver;
import org.bukkit.attribute.Attribute;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class ActionBarHealthDisplay {

    private final GuangDianMobHealth plugin;
    private final MiniMessageService miniMessage;
    private final MythicMobsHook mythicMobsHook;
    private final Map<UUID, Long> playerLastDisplayTime = new ConcurrentHashMap<>();
    private final Map<UUID, UUID> playerTargetEntity = new ConcurrentHashMap<>();

    private boolean enabled = true;
    private int displayDuration = 60;
    private String format = "<red>❤ <name> <health_bar><reset> <green><health><gray>/<green><max_health>";
    private int barLength = 10;
    private String barSymbol = "█";
    private String emptyColor = "<gray>";
    private String bracketLeft = "<dark_gray>[";
    private String bracketRight = "<dark_gray>]";
    private long cleanupTaskId = -1;

    public ActionBarHealthDisplay(GuangDianMobHealth plugin, MythicMobsHook mythicMobsHook) {
        this.plugin = plugin;
        this.mythicMobsHook = mythicMobsHook;
        this.miniMessage = plugin.getMiniMessageService();
        loadConfig();
    }

    public void loadConfig() {
        enabled = plugin.getConfig().getBoolean("actionbar.enabled", true);
        displayDuration = plugin.getConfig().getInt("actionbar.duration", 60);
        format = plugin.getConfig().getString("actionbar.format", "<red>❤ <name> <health_bar><reset> <green><health><gray>/<green><max_health>");
        format = convertOldPlaceholders(format);
        barLength = plugin.getConfig().getInt("actionbar.bar.length", 10);
        barSymbol = plugin.getConfig().getString("actionbar.bar.symbol", "█");
        emptyColor = plugin.getConfig().getString("actionbar.bar.empty-color", "<gray>");
        bracketLeft = plugin.getConfig().getString("actionbar.bar.bracket-left", "<dark_gray>[");
        bracketRight = plugin.getConfig().getString("actionbar.bar.bracket-right", "<dark_gray>]");
    }

    public void startCleanupTask() {
        if (!enabled) return;

        RPGCore rpgCore = RPGCore.getInstance();
        if (rpgCore == null) return;

        cleanupTaskId = rpgCore.getScheduler().runSyncRepeating(() -> {
            long now = System.currentTimeMillis();
            long timeoutMillis = displayDuration * 50L;

            playerLastDisplayTime.entrySet().removeIf(entry -> {
                if (now - entry.getValue() > timeoutMillis) {
                    playerTargetEntity.remove(entry.getKey());
                    return true;
                }
                return false;
            });
        }, 20L, 20L);
    }

    public void stopCleanupTask() {
        if (cleanupTaskId != -1) {
            RPGCore rpgCore = RPGCore.getInstance();
            if (rpgCore != null) {
                rpgCore.getScheduler().cancelTask(cleanupTaskId);
            }
            cleanupTaskId = -1;
        }
    }

    public void showHealth(Player player, LivingEntity entity) {
        if (!enabled) return;
        if (player == null || entity == null || !entity.isValid() || entity.isDead()) return;

        double health = entity.getHealth();
        double maxHealth = entity.getAttribute(Attribute.MAX_HEALTH).getValue();

        String name = getEntityName(entity);
        String healthBar = buildHealthBar(health, maxHealth);
        int percent = (int) ((health / maxHealth) * 100);

        Component healthBarComponent = miniMessage.parse(healthBar);
        Component nameComponent = miniMessage.parse(name != null ? name : "");

        TagResolver resolver = TagResolver.resolver(
            Placeholder.component("name", nameComponent),
            Placeholder.component("health_bar", healthBarComponent),
            Placeholder.unparsed("health", String.valueOf((int) health)),
            Placeholder.unparsed("max_health", String.valueOf((int) maxHealth)),
            Placeholder.unparsed("percent", String.valueOf(percent))
        );

        Component message = miniMessage.parse(format, resolver);
        player.sendActionBar(message);

        UUID playerId = player.getUniqueId();
        playerLastDisplayTime.put(playerId, System.currentTimeMillis());
        playerTargetEntity.put(playerId, entity.getUniqueId());

        plugin.debug("ActionBar显示: " + player.getName() + " -> " + name + " (" + (int) health + "/" + (int) maxHealth + ")");
    }

    public void clearPlayer(Player player) {
        if (player == null) return;
        UUID playerId = player.getUniqueId();
        playerLastDisplayTime.remove(playerId);
        playerTargetEntity.remove(playerId);
    }

    public void clear() {
        playerLastDisplayTime.clear();
        playerTargetEntity.clear();
    }

    private String getEntityName(LivingEntity entity) {
        String mythicName = mythicMobsHook.getMythicMobName(entity);
        if (mythicName != null && !mythicName.isEmpty()) {
            return convertLegacyColors(mythicName);
        }

        String customName = entity.getCustomName();
        if (customName != null && !customName.isEmpty()) {
            return convertLegacyColors(customName);
        }

        return convertLegacyColors(entity.getName());
    }

    private String convertLegacyColors(String text) {
        if (text == null || text.isEmpty()) {
            return text;
        }

        if (text.contains("<") && text.contains(">")) {
            return text;
        }

        StringBuilder result = new StringBuilder();
        char[] chars = text.toCharArray();

        for (int i = 0; i < chars.length; i++) {
            if (chars[i] == '§' && i + 1 < chars.length) {
                char code = chars[i + 1];
                String miniMessageTag = getMiniMessageTag(code);
                if (miniMessageTag != null) {
                    result.append(miniMessageTag);
                }
                i++;
            } else {
                result.append(chars[i]);
            }
        }

        return result.toString();
    }

    private String getMiniMessageTag(char code) {
        return switch (code) {
            case '0' -> "<black>";
            case '1' -> "<dark_blue>";
            case '2' -> "<dark_green>";
            case '3' -> "<dark_aqua>";
            case '4' -> "<dark_red>";
            case '5' -> "<dark_purple>";
            case '6' -> "<gold>";
            case '7' -> "<gray>";
            case '8' -> "<dark_gray>";
            case '9' -> "<blue>";
            case 'a' -> "<green>";
            case 'b' -> "<aqua>";
            case 'c' -> "<red>";
            case 'd' -> "<light_purple>";
            case 'e' -> "<yellow>";
            case 'f' -> "<white>";
            case 'k' -> "<obfuscated>";
            case 'l' -> "<bold>";
            case 'm' -> "<strikethrough>";
            case 'n' -> "<underlined>";
            case 'o' -> "<italic>";
            case 'r' -> "<reset>";
            default -> null;
        };
    }

    private String buildHealthBar(double health, double maxHealth) {
        if (maxHealth <= 0) return bracketLeft + bracketRight;

        int percent = (int) ((health / maxHealth) * 100);
        int filled = (int) ((health / maxHealth) * barLength);
        filled = Math.max(0, Math.min(barLength, filled));

        String fillColor = getBarColor(percent);

        StringBuilder bar = new StringBuilder();
        bar.append(bracketLeft);

        for (int i = 0; i < barLength; i++) {
            if (i < filled) {
                bar.append(fillColor).append(barSymbol);
            } else {
                bar.append(emptyColor).append(barSymbol);
            }
        }

        bar.append(bracketRight);
        return bar.toString();
    }

    private String getBarColor(int percent) {
        if (percent >= 70) return "<green>";
        if (percent >= 40) return "<yellow>";
        if (percent >= 20) return "<gold>";
        return "<red>";
    }

    private String convertOldPlaceholders(String format) {
        if (format == null || format.isEmpty()) {
            return format;
        }
        return format
            .replace("{name}", "<name>")
            .replace("{health}", "<health>")
            .replace("{max_health}", "<max_health>")
            .replace("{health_bar}", "<health_bar>")
            .replace("{percent}", "<percent>");
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }
}
