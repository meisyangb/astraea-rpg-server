package cn.guangdian.npccommand.migration;

import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.util.*;

public class CitizensCMDMigrator {

    public static void migrate(File citizensCmdFile, File outputFile) {
        System.out.println("[迁移工具] 开始迁移 CitizensCMD 配置...");
        System.out.println("[迁移工具] 源文件: " + citizensCmdFile.getAbsolutePath());
        System.out.println("[迁移工具] 目标文件: " + outputFile.getAbsolutePath());

        if (!citizensCmdFile.exists()) {
            System.err.println("[迁移工具] 错误: 源文件不存在!");
            return;
        }

        FileConfiguration sourceConfig = YamlConfiguration.loadConfiguration(citizensCmdFile);
        FileConfiguration targetConfig = new YamlConfiguration();

        // 添加头部注释
        targetConfig.options().header("""
            ============================================
            GuangDianNPCCommand - NPC命令配置
            从 CitizensCMD 迁移的数据
            ============================================
            
            命令类型说明:
              console  - 以控制台身份执行
              player   - 以玩家身份执行(带权限检查)
              op       - 以OP权限执行
              command  - 直接执行命令(不添加/)
              no_perms - 以玩家身份执行(无权限检查)
            
            可用变量:
              %player% - 玩家名称
              %p%      - 玩家名称(简写)
            """);

        // 获取 SavedNPCs 部分
        if (!sourceConfig.contains("SavedNPCs")) {
            System.err.println("[迁移工具] 错误: 源文件中没有 SavedNPCs 部分!");
            return;
        }

        Set<String> npcIds = sourceConfig.getConfigurationSection("SavedNPCs").getKeys(false);
        int migratedCount = 0;

        for (String npcId : npcIds) {
            String path = "SavedNPCs." + npcId;
            
            int cooldown = sourceConfig.getInt(path + ".cooldown", 0);
            String customPerms = sourceConfig.getString(path + ".customPerms", "");
            String commands = sourceConfig.getString(path + ".commands", "");

            // 跳过空命令
            if (commands == null || commands.trim().isEmpty() || commands.trim().equals("''")) {
                continue;
            }

            // 解析权限和命令
            List<String> permList = parseList(customPerms);
            List<String> cmdList = parseList(commands);

            // 创建命令列表
            List<Map<String, String>> commandList = new ArrayList<>();
            
            for (int i = 0; i < cmdList.size(); i++) {
                String cmd = cmdList.get(i).trim();
                if (cmd.isEmpty() || cmd.equals("''")) {
                    continue;
                }

                // 获取对应的权限类型
                String perm = "console"; // 默认
                if (i < permList.size()) {
                    String p = permList.get(i).trim();
                    if (!p.isEmpty()) {
                        perm = p.toLowerCase();
                    }
                }

                // 转换权限类型
                String type = convertPermType(perm);

                Map<String, String> cmdMap = new LinkedHashMap<>();
                cmdMap.put("type", type);
                cmdMap.put("command", cmd);
                commandList.add(cmdMap);
            }

            // 只有当有命令时才添加
            if (!commandList.isEmpty()) {
                String targetPath = "npc-commands." + npcId;
                targetConfig.set(targetPath + ".cooldown", cooldown);
                targetConfig.set(targetPath + ".commands", commandList);
                migratedCount++;
                
                System.out.println("[迁移工具] 已迁移 NPC " + npcId + " (" + commandList.size() + " 个命令)");
            }
        }

        // 保存文件
        try {
            targetConfig.save(outputFile);
            System.out.println("[迁移工具] 迁移完成! 共迁移 " + migratedCount + " 个NPC");
        } catch (IOException e) {
            System.err.println("[迁移工具] 保存文件失败: " + e.getMessage());
        }
    }

    private static List<String> parseList(String input) {
        List<String> result = new ArrayList<>();
        if (input == null || input.trim().isEmpty()) {
            return result;
        }
        
        String[] parts = input.split(";");
        for (String part : parts) {
            result.add(part.trim());
        }
        return result;
    }

    private static String convertPermType(String perm) {
        switch (perm) {
            case "player":
                return "player";
            case "console":
                return "console";
            case "op":
                return "op";
            case "command":
                return "command";
            case "noperms":
            case "noperm":
            case "no_perms":
                return "no_perms";
            case "conseole": // 处理拼写错误
                return "console";
            default:
                return "console";
        }
    }

    public static void main(String[] args) {
        File source = new File("e:/RPG/阿辰艾尔2.0/plugins/CitizensCMD/Saves.yml");
        File target = new File("e:/RPG/译梦传说/plugins/GuangDianNPCCommand/npc-commands.yml");
        
        // 确保目标目录存在
        target.getParentFile().mkdirs();
        
        migrate(source, target);
    }
}
