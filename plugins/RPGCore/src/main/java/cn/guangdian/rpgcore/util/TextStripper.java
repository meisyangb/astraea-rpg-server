package cn.guangdian.rpgcore.util;

import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;

import java.util.regex.Pattern;

/**
 * 文本剥离工具类 - 职责分离的格式代码处理
 *
 * <p><b>设计原则：单一职责，分离处理</b></p>
 *
 * <p>本类提供两种独立的文本剥离方式：</p>
 * <ol>
 *   <li><b>MiniMessage 处理</b> - 用于处理现代消息格式</li>
 *   <li><b>传统颜色代码处理</b> - 用于处理物品Lore、配置文件等</li>
 * </ol>
 *
 * <p><b>使用场景选择：</b></p>
 * <ul>
 *   <li>聊天消息、GUI文本 → 使用 MiniMessage 处理方法</li>
 *   <li>物品Lore、属性解析 → 使用传统颜色代码处理方法</li>
 *   <li>不确定格式 → 使用组合方法</li>
 * </ul>
 *
 * @author GuangDian
 * @since 1.2.0
 */
public final class TextStripper {

    // MiniMessage 解析器 - 用于处理现代消息格式
    private static final MiniMessage MINI_MESSAGE = MiniMessage.miniMessage();
    
    // 纯文本序列化器 - 用于将 Component 转为纯文本
    private static final PlainTextComponentSerializer PLAIN_SERIALIZER = PlainTextComponentSerializer.plainText();

    // MiniMessage 标签正则模式 - 用于快速匹配
    private static final Pattern MINI_MESSAGE_PATTERN = Pattern.compile("<[^>]+>");

    private TextStripper() {
        throw new UnsupportedOperationException("工具类不允许实例化");
    }

    // ==================== MiniMessage 处理 ====================

    /**
     * 剥离 MiniMessage 标签，返回纯文本
     *
     * <p><b>适用场景：</b></p>
     * <ul>
     *   <li>聊天消息系统使用 MiniMessage 格式的插件</li>
     *   <li>GUI 界面使用 MiniMessage 标签的文本</li>
     *   <li>配置文件使用 MiniMessage 格式</li>
     * </ul>
     *
     * <p><b>处理内容：</b></p>
     * <ul>
     *   <li>颜色标签: {@code <green>}, {@code <red>}, {@code <#FF5555>}</li>
     *   <li>装饰标签: {@code <bold>}, {@code <italic>}, {@code <underlined>}</li>
     *   <li>交互标签: {@code <hover>}, {@code <click>}</li>
     *   <li>渐变标签: {@code <gradient>}, {@code <rainbow>}</li>
     * </ul>
     *
     * <p><b>注意：</b>此方法不处理传统 {@code &} 和 {@code §} 颜色代码</p>
     *
     * @param input 包含 MiniMessage 标签的文本
     * @return 剥离标签后的纯文本
     * @see #stripLegacy(String) 处理传统颜色代码
     * @see #stripAll(String) 同时处理两种格式
     */
    public static String stripMiniMessage(String input) {
        if (input == null || input.isEmpty()) {
            return "";
        }

        try {
            // 解析 MiniMessage 为 Component，然后序列化为纯文本
            var component = MINI_MESSAGE.deserialize(input);
            return PLAIN_SERIALIZER.serialize(component);
        } catch (Exception e) {
            // 如果解析失败（格式错误），使用正则移除标签作为降级方案
            return MINI_MESSAGE_PATTERN.matcher(input).replaceAll("");
        }
    }

    /**
     * 快速检测是否包含 MiniMessage 标签
     *
     * <p>用于在剥离前快速判断文本格式类型</p>
     *
     * @param input 输入文本
     * @return 是否包含 MiniMessage 标签
     */
    public static boolean containsMiniMessage(String input) {
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

    // ==================== 传统颜色代码处理 ====================

    /**
     * 剥离传统颜色代码（& 和 §）
     *
     * <p><b>适用场景：</b></p>
     * <ul>
     *   <li>物品 Lore 解析（Minecraft 物品Lore通常使用 & 格式）</li>
     *   <li>旧版配置文件（Bukkit YamlConfiguration 传统格式）</li>
     *   <li>属性解析器（装备属性、技能描述等）</li>
     *   <li>从其他插件读取的文本数据</li>
     * </ul>
     *
     * <p><b>处理内容：</b></p>
     * <ul>
     *   <li>颜色代码: {@code &a}, {@code &c}, {@code §a}, {@code §c}</li>
     *   <li>格式代码: {@code &l} 粗体, {@code &o} 斜体, {@code &n} 下划线</li>
     *   <li>重置代码: {@code &r}, {@code §r}</li>
     * </ul>
     *
     * <p><b>性能优化：</b>使用字符级处理，避免正则表达式开销</p>
     *
     * <p><b>注意：</b>此方法不处理 MiniMessage 标签</p>
     *
     * @param input 包含传统颜色代码的文本
     * @return 剥离颜色代码后的纯文本
     * @see #stripMiniMessage(String) 处理 MiniMessage 标签
     * @see #stripAll(String) 同时处理两种格式
     */
    public static String stripLegacy(String input) {
        if (input == null || input.isEmpty()) {
            return "";
        }

        int len = input.length();
        StringBuilder sb = new StringBuilder(len);

        for (int i = 0; i < len; i++) {
            char c = input.charAt(i);
            // 检查是否是颜色代码前缀
            if ((c == '&' || c == '§') && i + 1 < len) {
                char next = input.charAt(i + 1);
                // 检查下一个字符是否是有效的颜色代码
                if (isLegacyColorCode(next)) {
                    i++; // 跳过颜色代码字符
                    continue;
                }
            }
            sb.append(c);
        }

        return sb.toString();
    }

    /**
     * 快速检测是否包含传统颜色代码
     *
     * <p>用于在剥离前快速判断文本格式类型</p>
     *
     * @param input 输入文本
     * @return 是否包含传统颜色代码
     */
    public static boolean containsLegacy(String input) {
        if (input == null || input.isEmpty()) {
            return false;
        }
        return input.indexOf('&') != -1 || input.indexOf('§') != -1;
    }

    // ==================== 组合方法 ====================

    /**
     * 剥离所有格式代码（MiniMessage + 传统颜色）
     *
     * <p><b>适用场景：</b></p>
     * <ul>
     *   <li>不确定文本格式类型的通用处理</li>
     *   <li>需要兼容多种格式的场景</li>
     *   <li>用户输入的清理处理</li>
     * </ul>
     *
     * <p><b>处理顺序：</b></p>
     * <ol>
     *   <li>先解析 MiniMessage 标签（现代格式优先）</li>
     *   <li>再剥离传统 & 和 § 颜色代码</li>
     * </ol>
     *
     * <p><b>性能注意：</b>此方法会经过两次处理，如果已知格式类型，
     * 建议直接使用 {@link #stripMiniMessage(String)} 或 {@link #stripLegacy(String)}</p>
     *
     * @param input 输入文本（可能包含任何格式）
     * @return 剥离所有格式后的纯文本
     * @see #stripMiniMessage(String) 仅处理 MiniMessage
     * @see #stripLegacy(String) 仅处理传统颜色
     */
    public static String stripAll(String input) {
        if (input == null || input.isEmpty()) {
            return "";
        }

        // 第一步：处理 MiniMessage 标签
        String result = stripMiniMessage(input);
        
        // 第二步：处理传统颜色代码
        return stripLegacy(result);
    }

    /**
     * 根据文本内容自动选择处理方式
     *
     * <p>智能检测文本格式类型，选择最合适的剥离方法：</p>
     * <ul>
     *   <li>包含 MiniMessage 标签 → 使用 {@link #stripMiniMessage(String)}</li>
     *   <li>包含传统颜色代码 → 使用 {@link #stripLegacy(String)}</li>
     *   <li>两者都有 → 使用 {@link #stripAll(String)}</li>
     *   <li>都没有 → 直接返回原文</li>
     * </ul>
     *
     * @param input 输入文本
     * @return 剥离格式后的纯文本
     */
    public static String stripSmart(String input) {
        if (input == null || input.isEmpty()) {
            return "";
        }

        boolean hasMiniMessage = containsMiniMessage(input);
        boolean hasLegacy = containsLegacy(input);

        if (hasMiniMessage && hasLegacy) {
            // 两种格式都有，使用组合方法
            return stripAll(input);
        } else if (hasMiniMessage) {
            // 只有 MiniMessage
            return stripMiniMessage(input);
        } else if (hasLegacy) {
            // 只有传统颜色
            return stripLegacy(input);
        } else {
            // 没有格式代码
            return input;
        }
    }

    // ==================== 工具方法 ====================

    /**
     * 检查字符是否是有效的传统颜色代码字符
     *
     * <p>有效颜色代码：0-9, a-f, k-o, r</p>
     *
     * @param c 字符
     * @return 是否是颜色代码字符
     */
    private static boolean isLegacyColorCode(char c) {
        return (c >= '0' && c <= '9') ||
               (c >= 'a' && c <= 'f') ||
               (c >= 'k' && c <= 'o') ||
               c == 'r' ||
               c == 'R';
    }

    // ==================== 格式转换方法 ====================

    /**
     * 将传统 & 颜色代码转换为 MiniMessage 格式
     *
     * <p><b>适用场景：</b></p>
     * <ul>
     *   <li>迁移旧版配置到新版 MiniMessage 格式</li>
     *   <li>兼容旧插件的文本输出</li>
     *   <li>统一消息格式</li>
     * </ul>
     *
     * <p><b>转换示例：</b></p>
     * <pre>
     * &aHello → &lt;green&gt;Hello
     * &cError → &lt;red&gt;Error
     * &lBold → &lt;bold&gt;Bold
     * </pre>
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
     * 将传统颜色代码字符转换为 MiniMessage 标签
     *
     * @param code 颜色代码字符（如 'a', 'c', 'l'）
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

    // ==================== 废弃方法（兼容旧代码） ====================

    /**
     * 剥离 MiniMessage 标签（兼容旧方法名）
     *
     * @deprecated 使用 {@link #stripMiniMessage(String)} 替代
     */
    @Deprecated(since = "1.2.0", forRemoval = false)
    public static String stripMiniMessageTags(String input) {
        return stripMiniMessage(input);
    }

    /**
     * 剥离传统颜色代码（兼容旧方法名）
     *
     * @deprecated 使用 {@link #stripLegacy(String)} 替代
     */
    @Deprecated(since = "1.2.0", forRemoval = false)
    public static String stripLegacyColors(String input) {
        return stripLegacy(input);
    }

    /**
     * 剥离颜色代码（兼容旧方法名）
     *
     * @deprecated 使用 {@link #stripLegacy(String)} 替代
     */
    @Deprecated(since = "1.1.0", forRemoval = false)
    public static String stripColor(String input) {
        return stripLegacy(input);
    }
}
