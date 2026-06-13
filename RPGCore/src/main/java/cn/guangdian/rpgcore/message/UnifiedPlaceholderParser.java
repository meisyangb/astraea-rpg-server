package cn.guangdian.rpgcore.message;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.tag.resolver.TagResolver;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class UnifiedPlaceholderParser {

    private static final Pattern PERCENT_PATTERN = Pattern.compile("%([^%]+)%");
    private static final Pattern BRACE_PATTERN = Pattern.compile("\\{([^}]+)\\}");

    private UnifiedPlaceholderParser() {}

    @NotNull
    public static String replaceAll(@NotNull String input, @NotNull Map<String, String> placeholders) {
        if (input == null || input.isEmpty()) {
            return "";
        }
        if (placeholders == null || placeholders.isEmpty()) {
            return input;
        }

        String result = input;

        for (Map.Entry<String, String> entry : placeholders.entrySet()) {
            String key = entry.getKey();
            String value = entry.getValue() != null ? entry.getValue() : "";

            result = result.replace("%" + key + "%", value);
            result = result.replace("{" + key + "}", value);
        }

        return result;
    }

    @NotNull
    public static String replaceAll(@NotNull String input, @NotNull String... keyValues) {
        if (input == null || input.isEmpty()) {
            return "";
        }
        if (keyValues == null || keyValues.length < 2) {
            return input;
        }

        Map<String, String> placeholders = new HashMap<>();
        for (int i = 0; i < keyValues.length - 1; i += 2) {
            String key = keyValues[i];
            String value = keyValues[i + 1];
            if (key != null) {
                placeholders.put(key, value != null ? value : "");
            }
        }

        return replaceAll(input, placeholders);
    }

    @NotNull
    public static String replacePercent(@NotNull String input, @NotNull Map<String, String> placeholders) {
        if (input == null || input.isEmpty()) {
            return "";
        }
        if (placeholders == null || placeholders.isEmpty()) {
            return input;
        }

        String result = input;
        for (Map.Entry<String, String> entry : placeholders.entrySet()) {
            String key = entry.getKey();
            String value = entry.getValue() != null ? entry.getValue() : "";
            result = result.replace("%" + key + "%", value);
        }
        return result;
    }

    @NotNull
    public static String replaceBrace(@NotNull String input, @NotNull Map<String, String> placeholders) {
        if (input == null || input.isEmpty()) {
            return "";
        }
        if (placeholders == null || placeholders.isEmpty()) {
            return input;
        }

        String result = input;
        for (Map.Entry<String, String> entry : placeholders.entrySet()) {
            String key = entry.getKey();
            String value = entry.getValue() != null ? entry.getValue() : "";
            result = result.replace("{" + key + "}", value);
        }
        return result;
    }

    @NotNull
    public static Component parseWithPlaceholders(@NotNull String input, @NotNull Map<String, String> placeholders) {
        if (input == null || input.isEmpty()) {
            return Component.empty();
        }

        String processed = replaceAll(input, placeholders);

        return MiniMessageService.getInstance().parse(processed);
    }

    @NotNull
    public static Component parseWithPlaceholders(@NotNull String input, @NotNull String... keyValues) {
        if (input == null || input.isEmpty()) {
            return Component.empty();
        }

        String processed = replaceAll(input, keyValues);

        return MiniMessageService.getInstance().parse(processed);
    }

    @NotNull
    public static TagResolver buildTagResolver(@NotNull Map<String, String> placeholders) {
        TagResolver.Builder builder = TagResolver.builder();
        if (placeholders != null) {
            for (Map.Entry<String, String> entry : placeholders.entrySet()) {
                String key = entry.getKey();
                String value = entry.getValue();
                if (key != null && value != null) {
                    builder.resolver(Placeholder.parsed(key, value));
                }
            }
        }
        return builder.build();
    }

    @NotNull
    public static TagResolver buildTagResolver(@NotNull String... keyValues) {
        TagResolver.Builder builder = TagResolver.builder();
        if (keyValues != null && keyValues.length >= 2) {
            for (int i = 0; i < keyValues.length - 1; i += 2) {
                String key = keyValues[i];
                String value = keyValues[i + 1];
                if (key != null && value != null) {
                    builder.resolver(Placeholder.parsed(key, value));
                }
            }
        }
        return builder.build();
    }

    @NotNull
    public static Map<String, String> extractPercentPlaceholders(@NotNull String input) {
        Map<String, String> found = new HashMap<>();
        if (input == null || input.isEmpty()) {
            return found;
        }

        Matcher matcher = PERCENT_PATTERN.matcher(input);
        while (matcher.find()) {
            found.put(matcher.group(1), null);
        }
        return found;
    }

    @NotNull
    public static Map<String, String> extractBracePlaceholders(@NotNull String input) {
        Map<String, String> found = new HashMap<>();
        if (input == null || input.isEmpty()) {
            return found;
        }

        Matcher matcher = BRACE_PATTERN.matcher(input);
        while (matcher.find()) {
            found.put(matcher.group(1), null);
        }
        return found;
    }

    @NotNull
    public static Map<String, String> extractAllPlaceholders(@NotNull String input) {
        Map<String, String> found = new HashMap<>();
        if (input == null || input.isEmpty()) {
            return found;
        }

        found.putAll(extractPercentPlaceholders(input));
        found.putAll(extractBracePlaceholders(input));
        return found;
    }

    public static boolean hasPercentPlaceholders(@NotNull String input) {
        if (input == null || input.isEmpty()) {
            return false;
        }
        return PERCENT_PATTERN.matcher(input).find();
    }

    public static boolean hasBracePlaceholders(@NotNull String input) {
        if (input == null || input.isEmpty()) {
            return false;
        }
        return BRACE_PATTERN.matcher(input).find();
    }

    public static boolean hasPlaceholders(@NotNull String input) {
        return hasPercentPlaceholders(input) || hasBracePlaceholders(input);
    }
}
