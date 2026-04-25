package cn.guangdian.classsystem.util;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.jetbrains.annotations.NotNull;

import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 旧版颜色代码转换器
 * 将 § 颜色代码转换为 MiniMessage 格式
 */
public class LegacyColorConverter {

    private static final Map<Character, String> COLOR_MAP = new HashMap<>();
    private static final Map<Character, String> FORMAT_MAP = new HashMap<>();
    private static final Pattern LEGACY_PATTERN = Pattern.compile("§([0-9a-fk-or])");

    static {
        // 颜色代码映射
        COLOR_MAP.put('0', "<black>");
        COLOR_MAP.put('1', "<dark_blue>");
        COLOR_MAP.put('2', "<dark_green>");
        COLOR_MAP.put('3', "<dark_aqua>");
        COLOR_MAP.put('4', "<dark_red>");
        COLOR_MAP.put('5', "<dark_purple>");
        COLOR_MAP.put('6', "<gold>");
        COLOR_MAP.put('7', "<gray>");
        COLOR_MAP.put('8', "<dark_gray>");
        COLOR_MAP.put('9', "<blue>");
        COLOR_MAP.put('a', "<green>");
        COLOR_MAP.put('b', "<aqua>");
        COLOR_MAP.put('c', "<red>");
        COLOR_MAP.put('d', "<light_purple>");
        COLOR_MAP.put('e', "<yellow>");
        COLOR_MAP.put('f', "<white>");

        // 格式代码映射
        FORMAT_MAP.put('k', "<obfuscated>");
        FORMAT_MAP.put('l', "<bold>");
        FORMAT_MAP.put('m', "<strikethrough>");
        FORMAT_MAP.put('n', "<underlined>");
        FORMAT_MAP.put('o', "<italic>");
        FORMAT_MAP.put('r', "<reset>");
    }

    /**
     * 将旧版颜色代码字符串转换为 MiniMessage 格式
     */
    public static String toMiniMessage(@NotNull String text) {
        if (!text.contains("§")) {
            return text;
        }

        StringBuilder result = new StringBuilder();
        Matcher matcher = LEGACY_PATTERN.matcher(text);

        while (matcher.find()) {
            char code = matcher.group(1).charAt(0);
            String replacement = COLOR_MAP.getOrDefault(code, FORMAT_MAP.getOrDefault(code, ""));
            matcher.appendReplacement(result, replacement);
        }
        matcher.appendTail(result);

        return result.toString();
    }

    /**
     * 将旧版颜色代码字符串转换为 Component
     */
    public static Component toComponent(@NotNull String text) {
        String miniMessage = toMiniMessage(text);
        return MiniMessage.miniMessage().deserialize(miniMessage);
    }

    /**
     * 创建带颜色的 Component（支持旧版颜色代码）
     */
    public static Component text(@NotNull String text) {
        return toComponent(text);
    }

    /**
     * 创建带颜色的 Component（支持旧版颜色代码）并添加装饰
     */
    public static Component text(@NotNull String text, TextDecoration... decorations) {
        Component component = toComponent(text);
        for (TextDecoration decoration : decorations) {
            component = component.decoration(decoration, true);
        }
        return component;
    }
}
