package cn.guangdian.rpgcore.util;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextColor;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 颜色工具类 - 提供颜色文本处理功能
 * 
 * <p><b>推荐使用 MiniMessageService 替代此类</b></p>
 * 
 * <p>MiniMessageService 提供更强大的功能：
 * <ul>
 *   <li>支持 MiniMessage 标签格式：{@code <green>}, {@code <red>} 等</li>
 *   <li>支持渐变效果：{@code <gradient:#ff0000:#00ff00>}</li>
 *   <li>支持彩虹效果：{@code <rainbow>}</li>
 *   <li>支持 Hex 颜色：{@code <#FF5555>}</li>
 *   <li>支持占位符：{@code Placeholder.parsed("player", name)}</li>
 * </ul>
 * 
 * @deprecated 使用 {@link cn.guangdian.rpgcore.message.MiniMessageService} 替代
 * @see cn.guangdian.rpgcore.message.MiniMessageService
 * @since 1.0.0
 */
@Deprecated(since = "1.0.0", forRemoval = false)
public final class ColorUtil {

    private static final Pattern TAG_PATTERN = Pattern.compile("<([^>]+)>(.*?)</\\1>", Pattern.DOTALL);
    private static final Pattern HEX_PATTERN = Pattern.compile("<#([A-Fa-f0-9]{6})>");
    private static final Pattern LEGACY_HEX_PATTERN = Pattern.compile("§x(§[A-Fa-f0-9]){6}");

    private static final Map<String, NamedTextColor> COLOR_MAP = new HashMap<>();
    private static final MiniMessage MINIMESSAGE = MiniMessage.miniMessage();
    private static final LegacyComponentSerializer LEGACY_SERIALIZER = LegacyComponentSerializer.legacyAmpersand();

    static {
        COLOR_MAP.put("black", NamedTextColor.BLACK);
        COLOR_MAP.put("dark_blue", NamedTextColor.DARK_BLUE);
        COLOR_MAP.put("dark_green", NamedTextColor.DARK_GREEN);
        COLOR_MAP.put("dark_aqua", NamedTextColor.DARK_AQUA);
        COLOR_MAP.put("dark_red", NamedTextColor.DARK_RED);
        COLOR_MAP.put("dark_purple", NamedTextColor.DARK_PURPLE);
        COLOR_MAP.put("gold", NamedTextColor.GOLD);
        COLOR_MAP.put("gray", NamedTextColor.GRAY);
        COLOR_MAP.put("dark_gray", NamedTextColor.DARK_GRAY);
        COLOR_MAP.put("blue", NamedTextColor.BLUE);
        COLOR_MAP.put("green", NamedTextColor.GREEN);
        COLOR_MAP.put("aqua", NamedTextColor.AQUA);
        COLOR_MAP.put("red", NamedTextColor.RED);
        COLOR_MAP.put("light_purple", NamedTextColor.LIGHT_PURPLE);
        COLOR_MAP.put("yellow", NamedTextColor.YELLOW);
        COLOR_MAP.put("white", NamedTextColor.WHITE);
    }

    private ColorUtil() {
        throw new UnsupportedOperationException("工具类不允许实例化");
    }

    /**
     * 将文本转换为带颜色的 Component
     * 
     * <p>支持格式：
     * <ul>
     *   <li>MiniMessage 标签：{@code <green>}, {@code <red>}</li>
     *   <li>Legacy 颜色码：{@code &a}, {@code &c}, {@code §a}</li>
     * </ul>
     * 
     * @param text 要转换的文本
     * @return 带颜色的 Component
     * @see MiniMessageService#colorize(String)
     */
    public static Component colorize(String text) {
        if (text == null || text.isEmpty()) {
            return Component.empty();
        }

        if (containsMiniMessageTags(text)) {
            return parseMiniMessage(text);
        }

        if (text.contains("&") || text.contains("§")) {
            return parseLegacy(text);
        }

        return Component.text(text);
    }

    private static boolean containsMiniMessageTags(String text) {
        return text.contains("<red>") || text.contains("<green>") ||
               text.contains("<blue>") || text.contains("<yellow>") ||
               text.contains("<gradient:") || text.contains("<rainbow>") ||
               text.contains("<hover:") || text.contains("<click:") ||
               text.contains("<#");
    }

    private static Component parseMiniMessage(String text) {
        try {
            return MINIMESSAGE.deserialize(text);
        } catch (Exception e) {
            return LEGACY_SERIALIZER.deserialize(text);
        }
    }

    private static Component parseLegacy(String text) {
        String processed = processLegacyColors(text);
        return LEGACY_SERIALIZER.deserialize(processed);
    }

    /**
     * @deprecated 使用 {@link #colorize(String)} 替代
     */
    @Deprecated(since = "1.0.0", forRemoval = false)
    public static Component colorize(String text, NamedTextColor defaultColor) {
        if (text == null || text.isEmpty()) {
            return Component.empty().color(defaultColor);
        }
        Component result = colorize(text);
        if (result == Component.empty()) {
            return Component.text(text).color(defaultColor);
        }
        return result;
    }

    private static String processLegacyColors(String text) {
        if (text == null || text.isEmpty()) {
            return text;
        }
        text = processCustomColorTags(text);
        return text;
    }

    private static String processCustomColorTags(String text) {
        StringBuffer result = new StringBuffer();
        Matcher matcher = TAG_PATTERN.matcher(text);

        while (matcher.find()) {
            String colorName = matcher.group(1).toLowerCase();
            String content = matcher.group(2);
            NamedTextColor color = COLOR_MAP.get(colorName);

            if (color != null) {
                String colorCode = getColorCode(color);
                matcher.appendReplacement(result, Matcher.quoteReplacement(colorCode + content + "§r"));
            }
        }
        matcher.appendTail(result);

        return result.toString();
    }

    private static String getColorCode(NamedTextColor color) {
        if (color == NamedTextColor.BLACK) return "§0";
        if (color == NamedTextColor.DARK_BLUE) return "§1";
        if (color == NamedTextColor.DARK_GREEN) return "§2";
        if (color == NamedTextColor.DARK_AQUA) return "§3";
        if (color == NamedTextColor.DARK_RED) return "§4";
        if (color == NamedTextColor.DARK_PURPLE) return "§5";
        if (color == NamedTextColor.GOLD) return "§6";
        if (color == NamedTextColor.GRAY) return "§7";
        if (color == NamedTextColor.DARK_GRAY) return "§8";
        if (color == NamedTextColor.BLUE) return "§9";
        if (color == NamedTextColor.GREEN) return "§a";
        if (color == NamedTextColor.AQUA) return "§b";
        if (color == NamedTextColor.RED) return "§c";
        if (color == NamedTextColor.LIGHT_PURPLE) return "§d";
        if (color == NamedTextColor.YELLOW) return "§e";
        if (color == NamedTextColor.WHITE) return "§f";
        return "§f";
    }

    /**
     * 解析包含 Hex 颜色的文本
     * 
     * @see MiniMessageService#colorize(String)
     */
    public static Component colorizeHex(String text) {
        if (text == null || text.isEmpty()) {
            return Component.empty();
        }

        if (text.contains("<")) {
            try {
                return MINIMESSAGE.deserialize(text);
            } catch (Exception e) {
                return LEGACY_SERIALIZER.deserialize(text);
            }
        }

        return Component.text(text);
    }

    /**
     * 解析渐变文本
     * 
     * @see MiniMessageService#parse(String)
     */
    public static Component parseGradient(String text) {
        if (text == null || text.isEmpty()) {
            return Component.empty();
        }
        try {
            return MINIMESSAGE.deserialize(text);
        } catch (Exception e) {
            return LEGACY_SERIALIZER.deserialize(text);
        }
    }

    /**
     * 发送消息给玩家
     * @see AudienceService#sendMessage(Player, Component)
     */
    public static void send(Player player, Component message) {
        if (player == null || message == null) return;
        player.sendMessage(message);
    }

    /**
     * 发送消息给命令发送者
     * @see AudienceService#sendMessage(CommandSender, Component)
     */
    public static void send(CommandSender sender, Component message) {
        if (sender == null || message == null) return;
        sender.sendMessage(message);
    }

    /**
     * 发送消息给玩家
     * @see AudienceService#sendMessage(Player, Component)
     */
    public static void send(Player player, String text) {
        if (player == null || text == null) {
            return;
        }
        player.sendMessage(colorize(text));
    }

    /**
     * 发送消息给命令发送者
     * @see AudienceService#sendMessage(CommandSender, Component)
     */
    public static void send(CommandSender sender, String text) {
        if (sender == null || text == null) {
            return;
        }
        sender.sendMessage(colorize(text));
    }

    /**
     * 发送成功消息（绿色）
     * @see MiniMessageService#colorize(String)
     */
    public static void sendSuccess(CommandSender sender, String text) {
        if (sender == null || text == null) {
            return;
        }
        sender.sendMessage(green(text));
    }

    /**
     * 发送错误消息（红色）
     * @see MiniMessageService#colorize(String)
     */
    public static void sendError(CommandSender sender, String text) {
        if (sender == null || text == null) {
            return;
        }
        sender.sendMessage(red(text));
    }

    /**
     * 发送警告消息（黄色）
     * @see MiniMessageService#colorize(String)
     */
    public static void sendWarning(CommandSender sender, String text) {
        if (sender == null || text == null) {
            return;
        }
        sender.sendMessage(yellow(text));
    }

    /**
     * 发送信息消息（青色）
     * @see MiniMessageService#colorize(String)
     */
    public static void sendInfo(CommandSender sender, String text) {
        if (sender == null || text == null) {
            return;
        }
        sender.sendMessage(aqua(text));
    }

    /**
     * 创建绿色文本组件
     * @see MiniMessageService#parse(String) 使用 {@code MiniMessageService.getInstance().parse("<green>" + text)}
     */
    public static Component green(String text) {
        if (text == null || text.isEmpty()) {
            return Component.empty();
        }
        return Component.text(text).color(NamedTextColor.GREEN);
    }

    /**
     * 创建红色文本组件
     * @see MiniMessageService#parse(String) 使用 {@code MiniMessageService.getInstance().parse("<red>" + text)}
     */
    public static Component red(String text) {
        if (text == null || text.isEmpty()) {
            return Component.empty();
        }
        return Component.text(text).color(NamedTextColor.RED);
    }

    /**
     * 创建黄色文本组件
     * @see MiniMessageService#parse(String) 使用 {@code MiniMessageService.getInstance().parse("<yellow>" + text)}
     */
    public static Component yellow(String text) {
        if (text == null || text.isEmpty()) {
            return Component.empty();
        }
        return Component.text(text).color(NamedTextColor.YELLOW);
    }

    /**
     * 创建青色文本组件
     * @see MiniMessageService#parse(String) 使用 {@code MiniMessageService.getInstance().parse("<aqua>" + text)}
     */
    public static Component aqua(String text) {
        if (text == null || text.isEmpty()) {
            return Component.empty();
        }
        return Component.text(text).color(NamedTextColor.AQUA);
    }

    /**
     * 创建白色文本组件
     */
    public static Component white(String text) {
        if (text == null || text.isEmpty()) {
            return Component.empty();
        }
        return Component.text(text).color(NamedTextColor.WHITE);
    }

    /**
     * 创建灰色文本组件
     */
    public static Component gray(String text) {
        if (text == null || text.isEmpty()) {
            return Component.empty();
        }
        return Component.text(text).color(NamedTextColor.GRAY);
    }

    /**
     * 创建金色文本组件
     */
    public static Component gold(String text) {
        if (text == null || text.isEmpty()) {
            return Component.empty();
        }
        return Component.text(text).color(NamedTextColor.GOLD);
    }

    /**
     * 创建深绿色文本组件
     */
    public static Component darkGreen(String text) {
        if (text == null || text.isEmpty()) {
            return Component.empty();
        }
        return Component.text(text).color(NamedTextColor.DARK_GREEN);
    }

    /**
     * 创建深红色文本组件
     */
    public static Component darkRed(String text) {
        if (text == null || text.isEmpty()) {
            return Component.empty();
        }
        return Component.text(text).color(NamedTextColor.DARK_RED);
    }

    /**
     * 创建蓝色文本组件
     */
    public static Component blue(String text) {
        if (text == null || text.isEmpty()) {
            return Component.empty();
        }
        return Component.text(text).color(NamedTextColor.BLUE);
    }

    /**
     * 创建深蓝色文本组件
     */
    public static Component darkBlue(String text) {
        if (text == null || text.isEmpty()) {
            return Component.empty();
        }
        return Component.text(text).color(NamedTextColor.DARK_BLUE);
    }

    /**
     * 创建亮紫色文本组件
     */
    public static Component lightPurple(String text) {
        if (text == null || text.isEmpty()) {
            return Component.empty();
        }
        return Component.text(text).color(NamedTextColor.LIGHT_PURPLE);
    }

    /**
     * 创建深紫色文本组件
     */
    public static Component darkPurple(String text) {
        if (text == null || text.isEmpty()) {
            return Component.empty();
        }
        return Component.text(text).color(NamedTextColor.DARK_PURPLE);
    }

    /**
     * 创建黑色文本组件
     */
    public static Component black(String text) {
        if (text == null || text.isEmpty()) {
            return Component.empty();
        }
        return Component.text(text).color(NamedTextColor.BLACK);
    }

    /**
     * 创建深灰色文本组件
     */
    public static Component darkGray(String text) {
        if (text == null || text.isEmpty()) {
            return Component.empty();
        }
        return Component.text(text).color(NamedTextColor.DARK_GRAY);
    }

    /**
     * 创建深青色文本组件
     */
    public static Component darkAqua(String text) {
        if (text == null || text.isEmpty()) {
            return Component.empty();
        }
        return Component.text(text).color(NamedTextColor.DARK_AQUA);
    }

    /**
     * 创建 Hex 颜色文本组件
     * 
     * @see MiniMessageService#parse(String) 使用 {@code MiniMessageService.getInstance().parse("<#RRGGBB>" + text)}
     */
    public static Component hex(String text, int rgb) {
        if (text == null || text.isEmpty()) {
            return Component.empty();
        }
        return Component.text(text).color(TextColor.color(rgb));
    }

    /**
     * 创建 Hex 颜色文本组件
     * 
     * @see MiniMessageService#parse(String) 使用 {@code MiniMessageService.getInstance().parse("<#RRGGBB>" + text)}
     */
    public static Component hex(String text, String hexColor) {
        if (text == null || text.isEmpty()) {
            return Component.empty();
        }
        try {
            int rgb = Integer.parseInt(hexColor.replace("#", ""), 16);
            return Component.text(text).color(TextColor.color(rgb));
        } catch (NumberFormatException e) {
            return Component.text(text);
        }
    }

    /**
     * 将文本转换为带颜色的字符串（返回 § 颜色码格式）
     * 用于需要字符串的场景，如配置文件、物品Lore等
     */
    public static String legacyColorize(String text) {
        if (text == null || text.isEmpty()) {
            return "";
        }
        Component component = colorize(text);
        return LegacyComponentSerializer.legacyAmpersand().serialize(component);
    }
}
