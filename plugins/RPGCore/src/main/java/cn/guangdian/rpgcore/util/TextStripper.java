package cn.guangdian.rpgcore.util;

import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;

import java.util.regex.Pattern;

/**
 * 文本剥离工具类 - 用于解析器剥离颜色代码和 MiniMessage 标签
 *
 * <p>提供高性能的文本剥离功能，支持：</p>
 * <ul>
 *   <li>传统 & 颜色代码: {@code &a}, {@code &c}</li>
 *   <li>传统 § 颜色代码: {@code §a}, {@code §c}</li>
 *   <li>MiniMessage 标签: {@code <green>}, {@code <red>}, {@code <#FF5555>}</li>
 * </ul>
 *
 * <p><b>性能优化：</b>字符级处理，避免正则开销</p>
 *
 * @since 1.1.0
 */
public final class TextStripper {

    private static final MiniMessage MINI_MESSAGE = MiniMessage.miniMessage();
    private static final PlainTextComponentSerializer PLAIN_SERIALIZER = PlainTextComponentSerializer.plainText();

    // MiniMessage 标签模式 - 用于快速检测
    private static final Pattern MINI_MESSAGE_PATTERN = Pattern.compile("<[^>]+>");

    private TextStripper() {
        throw new UnsupportedOperationException("工具类不允许实例化");
    }

    /**
     * 剥离所有格式代码（包括传统颜色码和 MiniMessage 标签）
     *
     * <p>处理顺序：</p>
     * <ol>
     *   <li>先解析 MiniMessage 标签为纯文本</li>
     *   <li>再剥离传统 & 和 § 颜色代码</li>
     * </ol>
     *
     * @param input 输入文本
     * @return 剥离格式后的纯文本
     */
    public static String stripAll(String input) {
        if (input == null || input.isEmpty()) {
            return "";
        }

        // 如果包含 MiniMessage 标签，先解析为纯文本
        if (containsMiniMessageTags(input)) {
            input = stripMiniMessageTags(input);
        }

        // 剥离传统颜色代码
        return stripLegacyColors(input);
    }

    /**
     * 快速检测是否包含 MiniMessage 标签
     *
     * @param input 输入文本
     * @return 是否包含 MiniMessage 标签
     */
    public static boolean containsMiniMessageTags(String input) {
        if (input == null || input.isEmpty()) {
            return false;
        }
        // 快速检查：包含 < 字符且后面有 >
        int ltIndex = input.indexOf('<');
        if (ltIndex == -1) {
            return false;
        }
        int gtIndex = input.indexOf('>', ltIndex);
        return gtIndex != -1;
    }

    /**
     * 剥离 MiniMessage 标签，返回纯文本
     *
     * <p>使用 Adventure API 解析 MiniMessage，然后序列化为纯文本</p>
     *
     * @param input 包含 MiniMessage 标签的文本
     * @return 剥离标签后的纯文本
     */
    public static String stripMiniMessageTags(String input) {
        if (input == null || input.isEmpty()) {
            return "";
        }

        try {
            // 解析 MiniMessage 为 Component
            var component = MINI_MESSAGE.deserialize(input);
            // 序列化为纯文本
            return PLAIN_SERIALIZER.serialize(component);
        } catch (Exception e) {
            // 如果解析失败，使用正则移除标签
            return MINI_MESSAGE_PATTERN.matcher(input).replaceAll("");
        }
    }

    /**
     * 高性能剥离传统颜色代码（& 和 §）
     *
     * <p>字符级处理，避免正则开销</p>
     *
     * @param input 输入文本
     * @return 剥离颜色代码后的文本
     */
    public static String stripLegacyColors(String input) {
        if (input == null || input.isEmpty()) {
            return "";
        }

        int len = input.length();
        StringBuilder sb = new StringBuilder(len);

        for (int i = 0; i < len; i++) {
            char c = input.charAt(i);
            if ((c == '&' || c == '§') && i + 1 < len) {
                char next = input.charAt(i + 1);
                // 检查是否是有效的颜色代码字符
                if (isColorCodeChar(next)) {
                    i++; // 跳过颜色代码
                    continue;
                }
            }
            sb.append(c);
        }

        return sb.toString();
    }

    /**
     * 剥离颜色代码（兼容旧方法名）
     *
     * @deprecated 使用 {@link #stripLegacyColors(String)} 替代
     */
    @Deprecated(since = "1.1.0", forRemoval = false)
    public static String stripColor(String input) {
        return stripLegacyColors(input);
    }

    /**
     * 检查字符是否是有效的颜色代码字符
     *
     * @param c 字符
     * @return 是否是颜色代码字符
     */
    private static boolean isColorCodeChar(char c) {
        return (c >= '0' && c <= '9') ||
               (c >= 'a' && c <= 'f') ||
               (c >= 'k' && c <= 'o') ||
               c == 'r' ||
               c == 'R';
    }

    /**
     * 将传统 & 颜色代码转换为 MiniMessage 格式
     *
     * <p>用于将旧格式文本转换为新格式</p>
     *
     * @param input 包含 & 颜色代码的文本
     * @return 转换为 MiniMessage 格式的文本
     */
    public static String legacyToMiniMessage(String input) {
        if (input == null || input.isEmpty()) {
            return "";
        }

        StringBuilder output = new StringBuilder(input.length() * 2);

        for (int i = 0; i < input.length(); i++) {
            char c = input.charAt(i);
            if (c == '&' && i + 1 < input.length()) {
                char code = Character.toLowerCase(input.charAt(i + 1));
                String miniTag = getMiniMessageTag(code);
                if (miniTag != null) {
                    output.append(miniTag);
                    i++;
                    continue;
                }
            }
            output.append(c);
        }

        return output.toString();
    }

    /**
     * 将传统颜色代码转换为 MiniMessage 标签
     *
     * @param code 颜色代码字符
     * @return MiniMessage 标签，如果不是有效代码则返回 null
     */
    private static String getMiniMessageTag(char code) {
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
}
