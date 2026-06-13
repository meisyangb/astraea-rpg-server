package cn.guangdian.rpgcore.message;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextColor;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.minimessage.tag.resolver.TagResolver;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Map;

public final class MiniMessageService {

    private static MiniMessageService instance;

    private final MiniMessage miniMessage;
    private final LegacyComponentSerializer legacySerializer;
    private final LegacyComponentSerializer legacySectionSerializer;

    private MiniMessageService() {
        this.miniMessage = MiniMessage.miniMessage();
        this.legacySerializer = LegacyComponentSerializer.legacyAmpersand();
        this.legacySectionSerializer = LegacyComponentSerializer.legacySection();
    }

    public static MiniMessageService getInstance() {
        if (instance == null) {
            synchronized (MiniMessageService.class) {
                if (instance == null) {
                    instance = new MiniMessageService();
                }
            }
        }
        return instance;
    }

    public @NotNull Component parse(@NotNull String input) {
        if (input == null || input.isEmpty()) {
            return Component.empty();
        }
        return miniMessage.deserialize(input);
    }

    public @NotNull Component parse(@NotNull String input, @NotNull TagResolver resolver) {
        if (input == null || input.isEmpty()) {
            return Component.empty();
        }
        return miniMessage.deserialize(input, resolver);
    }

    public @NotNull String serialize(@NotNull Component component) {
        if (component == null) {
            return "";
        }
        return miniMessage.serialize(component);
    }

    public @NotNull Component legacyParse(@NotNull String input) {
        if (input == null || input.isEmpty()) {
            return Component.empty();
        }
        return legacySerializer.deserialize(input);
    }

    public @NotNull String legacySerialize(@NotNull Component component) {
        if (component == null) {
            return "";
        }
        return legacySerializer.serialize(component);
    }

    public @NotNull Component colorize(@NotNull String text) {
        if (text == null || text.isEmpty()) {
            return Component.empty();
        }
        // 只使用官方 MiniMessage 方式解析，不再兼容 & 格式
        return miniMessage.deserialize(text);
    }

    public @NotNull Component colorizeWithDefaults(@NotNull String text, @NotNull Component defaultColor) {
        if (text == null || text.isEmpty()) {
            return Component.empty().color(defaultColor.color());
        }
        return colorize(text);
    }

    public @NotNull Component parseWithPlaceholders(@NotNull String input, @NotNull String key, @NotNull String value) {
        if (input == null || input.isEmpty()) {
            return Component.empty();
        }
        return miniMessage.deserialize(input, Placeholder.parsed(key, value));
    }

    public @NotNull Component parseWithPlaceholders(@NotNull String input, @NotNull String key, @NotNull Component value) {
        if (input == null || input.isEmpty()) {
            return Component.empty();
        }
        return miniMessage.deserialize(input, Placeholder.component(key, value));
    }

    public @NotNull Component parseWithPlaceholders(@NotNull String input, @NotNull String key, @NotNull String value, boolean parse) {
        if (input == null || input.isEmpty()) {
            return Component.empty();
        }
        TagResolver resolver = parse ? Placeholder.parsed(key, value) : Placeholder.unparsed(key, value);
        return miniMessage.deserialize(input, resolver);
    }

    public @NotNull Component parseWithMultiplePlaceholders(@NotNull String input, @NotNull String... keyValues) {
        if (input == null || input.isEmpty() || keyValues == null || keyValues.length == 0) {
            return Component.empty();
        }
        TagResolver.Builder builder = TagResolver.builder();
        for (int i = 0; i < keyValues.length - 1; i += 2) {
            String key = keyValues[i];
            String value = keyValues[i + 1];
            if (key != null && value != null) {
                builder.resolver(Placeholder.parsed(key, value));
            }
        }
        return miniMessage.deserialize(input, builder.build());
    }

    public @NotNull Component parseWithUnparsedPlaceholders(@NotNull String input, @NotNull String... keyValues) {
        if (input == null || input.isEmpty() || keyValues == null || keyValues.length == 0) {
            return Component.empty();
        }
        TagResolver.Builder builder = TagResolver.builder();
        for (int i = 0; i < keyValues.length - 1; i += 2) {
            String key = keyValues[i];
            String value = keyValues[i + 1];
            if (key != null && value != null) {
                builder.resolver(Placeholder.unparsed(key, value));
            }
        }
        return miniMessage.deserialize(input, builder.build());
    }

    public MiniMessage getMiniMessage() {
        return miniMessage;
    }

    public static LegacyComponentSerializer getLegacySerializer() {
        return LegacyComponentSerializer.legacyAmpersand();
    }

    public @NotNull String legacyColorize(@NotNull String text) {
        if (text == null || text.isEmpty()) {
            return "";
        }
        Component component = colorize(text);
        return legacySectionSerializer.serialize(component);
    }

    public @NotNull Component green(@NotNull String text) {
        if (text == null || text.isEmpty()) {
            return Component.empty();
        }
        return Component.text(text).color(NamedTextColor.GREEN);
    }

    public @NotNull Component red(@NotNull String text) {
        if (text == null || text.isEmpty()) {
            return Component.empty();
        }
        return Component.text(text).color(NamedTextColor.RED);
    }

    public @NotNull Component yellow(@NotNull String text) {
        if (text == null || text.isEmpty()) {
            return Component.empty();
        }
        return Component.text(text).color(NamedTextColor.YELLOW);
    }

    public @NotNull Component gold(@NotNull String text) {
        if (text == null || text.isEmpty()) {
            return Component.empty();
        }
        return Component.text(text).color(NamedTextColor.GOLD);
    }

    public @NotNull Component gray(@NotNull String text) {
        if (text == null || text.isEmpty()) {
            return Component.empty();
        }
        return Component.text(text).color(NamedTextColor.GRAY);
    }

    public @NotNull Component aqua(@NotNull String text) {
        if (text == null || text.isEmpty()) {
            return Component.empty();
        }
        return Component.text(text).color(NamedTextColor.AQUA);
    }

    public @NotNull Component white(@NotNull String text) {
        if (text == null || text.isEmpty()) {
            return Component.empty();
        }
        return Component.text(text).color(NamedTextColor.WHITE);
    }

    public @NotNull Component blue(@NotNull String text) {
        if (text == null || text.isEmpty()) {
            return Component.empty();
        }
        return Component.text(text).color(NamedTextColor.BLUE);
    }

    public @NotNull Component darkBlue(@NotNull String text) {
        if (text == null || text.isEmpty()) {
            return Component.empty();
        }
        return Component.text(text).color(NamedTextColor.DARK_BLUE);
    }

    public @NotNull Component darkGreen(@NotNull String text) {
        if (text == null || text.isEmpty()) {
            return Component.empty();
        }
        return Component.text(text).color(NamedTextColor.DARK_GREEN);
    }

    public @NotNull Component darkRed(@NotNull String text) {
        if (text == null || text.isEmpty()) {
            return Component.empty();
        }
        return Component.text(text).color(NamedTextColor.DARK_RED);
    }

    public @NotNull Component darkPurple(@NotNull String text) {
        if (text == null || text.isEmpty()) {
            return Component.empty();
        }
        return Component.text(text).color(NamedTextColor.DARK_PURPLE);
    }

    public @NotNull Component lightPurple(@NotNull String text) {
        if (text == null || text.isEmpty()) {
            return Component.empty();
        }
        return Component.text(text).color(NamedTextColor.LIGHT_PURPLE);
    }

    public @NotNull Component darkGray(@NotNull String text) {
        if (text == null || text.isEmpty()) {
            return Component.empty();
        }
        return Component.text(text).color(NamedTextColor.DARK_GRAY);
    }

    public @NotNull Component darkAqua(@NotNull String text) {
        if (text == null || text.isEmpty()) {
            return Component.empty();
        }
        return Component.text(text).color(NamedTextColor.DARK_AQUA);
    }

    public @NotNull Component black(@NotNull String text) {
        if (text == null || text.isEmpty()) {
            return Component.empty();
        }
        return Component.text(text).color(NamedTextColor.BLACK);
    }

    public @NotNull Component hex(@NotNull String text, int rgb) {
        if (text == null || text.isEmpty()) {
            return Component.empty();
        }
        return Component.text(text).color(TextColor.color(rgb));
    }

    public @NotNull Component hex(@NotNull String text, @NotNull String hexColor) {
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

    @NotNull
    public Component parseUnified(@NotNull String input, @NotNull Map<String, String> placeholders) {
        if (input == null || input.isEmpty()) {
            return Component.empty();
        }
        return UnifiedPlaceholderParser.parseWithPlaceholders(input, placeholders);
    }

    @NotNull
    public Component parseUnified(@NotNull String input, @NotNull String... keyValues) {
        if (input == null || input.isEmpty()) {
            return Component.empty();
        }
        return UnifiedPlaceholderParser.parseWithPlaceholders(input, keyValues);
    }

    @NotNull
    public String replaceUnified(@NotNull String input, @NotNull Map<String, String> placeholders) {
        if (input == null || input.isEmpty()) {
            return "";
        }
        return UnifiedPlaceholderParser.replaceAll(input, placeholders);
    }

    @NotNull
    public String replaceUnified(@NotNull String input, @NotNull String... keyValues) {
        if (input == null || input.isEmpty()) {
            return "";
        }
        return UnifiedPlaceholderParser.replaceAll(input, keyValues);
    }
}
