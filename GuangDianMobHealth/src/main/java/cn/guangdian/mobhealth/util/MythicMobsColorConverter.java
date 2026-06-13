package cn.guangdian.mobhealth.util;

import java.io.*;
import java.nio.file.*;
import java.util.*;

public class MythicMobsColorConverter {

    private static final Map<String, String> COLOR_MAP = new LinkedHashMap<>();

    static {
        // 按长度降序排列，避免短标签先被替换
        COLOR_MAP.put("<dark_red>", "&4");
        COLOR_MAP.put("<dark_blue>", "&1");
        COLOR_MAP.put("<dark_gray>", "&8");
        COLOR_MAP.put("<dark_green>", "&2");
        COLOR_MAP.put("<dark_aqua>", "&3");
        COLOR_MAP.put("<dark_purple>", "&5");
        COLOR_MAP.put("<light_purple>", "&d");
        COLOR_MAP.put("<strikethrough>", "&m");
        COLOR_MAP.put("<underlined>", "&n");
        COLOR_MAP.put("<obfuscated>", "&k");
        COLOR_MAP.put("<black>", "&0");
        COLOR_MAP.put("<blue>", "&9");
        COLOR_MAP.put("<green>", "&a");
        COLOR_MAP.put("<aqua>", "&b");
        COLOR_MAP.put("<red>", "&c");
        COLOR_MAP.put("<gray>", "&7");
        COLOR_MAP.put("<gold>", "&6");
        COLOR_MAP.put("<white>", "&f");
        COLOR_MAP.put("<yellow>", "&e");
        COLOR_MAP.put("<bold>", "&l");
        COLOR_MAP.put("<italic>", "&o");
        COLOR_MAP.put("<reset>", "&r");
    }

    public static void main(String[] args) throws IOException {
        String mobsDir = "e:\\RPG\\译梦传说\\plugins\\MythicMobs\\mobs";
        File dir = new File(mobsDir);

        if (!dir.exists() || !dir.isDirectory()) {
            System.err.println("目录不存在: " + mobsDir);
            return;
        }

        File[] files = dir.listFiles((d, name) -> name.endsWith(".yml"));
        if (files == null || files.length == 0) {
            System.out.println("没有找到 .yml 文件");
            return;
        }

        int totalReplacements = 0;

        for (File file : files) {
            String content = Files.readString(file.toPath());
            String originalContent = content;
            int fileReplacements = 0;

            for (Map.Entry<String, String> entry : COLOR_MAP.entrySet()) {
                String tag = entry.getKey();
                String code = entry.getValue();

                int count = countOccurrences(content, tag);
                if (count > 0) {
                    content = content.replace(tag, code);
                    fileReplacements += count;
                    totalReplacements += count;
                }
            }

            if (fileReplacements > 0) {
                // 备份原文件
                File backupFile = new File(file.getAbsolutePath() + ".backup");
                if (!backupFile.exists()) {
                    Files.copy(file.toPath(), backupFile.toPath());
                }

                // 保存新内容
                Files.writeString(file.toPath(), content);
                System.out.println("已转换: " + file.getName() + " - 替换了 " + fileReplacements + " 处");
            }
        }

        System.out.println("\n转换完成！总共替换了 " + totalReplacements + " 处颜色标签");
        System.out.println("原文件已备份为 .backup 文件");
    }

    private static int countOccurrences(String str, String sub) {
        int count = 0;
        int idx = 0;
        while ((idx = str.indexOf(sub, idx)) != -1) {
            count++;
            idx += sub.length();
        }
        return count;
    }
}
