package cn.guangdian.npccommand;

import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.util.*;

public class NPCCommandService {

    private final GuangDianNPCCommand plugin;
    private final Map<Integer, NPCCommandData> npcCommands;
    private File configFile;
    private FileConfiguration config;

    public NPCCommandService(GuangDianNPCCommand plugin) {
        this.plugin = plugin;
        this.npcCommands = new HashMap<>();
        loadConfig();
    }

    private void loadConfig() {
        configFile = new File(plugin.getDataFolder(), "npc-commands.yml");
        if (!configFile.exists()) {
            plugin.saveResource("npc-commands.yml", false);
        }
        config = YamlConfiguration.loadConfiguration(configFile);
        loadNPCCommands();
    }

    private void loadNPCCommands() {
        npcCommands.clear();
        ConfigurationSection npcSection = config.getConfigurationSection("npc-commands");
        if (npcSection == null) {
            return;
        }

        for (String npcIdStr : npcSection.getKeys(false)) {
            try {
                int npcId = Integer.parseInt(npcIdStr);
                ConfigurationSection npcData = npcSection.getConfigurationSection(npcIdStr);
                if (npcData == null) continue;

                NPCCommandData data = new NPCCommandData(npcId);
                data.setCooldown(npcData.getLong("cooldown", 0));

                List<Map<?, ?>> commandsList = npcData.getMapList("commands");
                for (Map<?, ?> cmdMap : commandsList) {
                    String typeStr = (String) cmdMap.get("type");
                    String command = (String) cmdMap.get("command");
                    if (typeStr != null && command != null) {
                        try {
                            NPCCommandData.CommandType type = NPCCommandData.CommandType.valueOf(typeStr.toUpperCase());
                            data.addCommand(type, command);
                        } catch (IllegalArgumentException ignored) {
                        }
                    }
                }

                npcCommands.put(npcId, data);
            } catch (NumberFormatException ignored) {
            }
        }
    }

    public void saveConfig() {
        config.set("npc-commands", null);
        ConfigurationSection npcSection = config.createSection("npc-commands");

        for (NPCCommandData data : npcCommands.values()) {
            ConfigurationSection npcData = npcSection.createSection(String.valueOf(data.getNpcId()));
            npcData.set("cooldown", data.getCooldown());

            List<Map<String, String>> commandsList = new ArrayList<>();
            for (NPCCommandData.CommandEntry entry : data.getCommands()) {
                Map<String, String> cmdMap = new LinkedHashMap<>();
                cmdMap.put("type", entry.getType().name().toLowerCase());
                cmdMap.put("command", entry.getCommand());
                commandsList.add(cmdMap);
            }
            npcData.set("commands", commandsList);
        }

        try {
            config.save(configFile);
        } catch (IOException e) {
            plugin.getLogger().severe("无法保存NPC命令配置: " + e.getMessage());
        }
    }

    public NPCCommandData getNPCCommandData(int npcId) {
        return npcCommands.get(npcId);
    }

    public void setNPCCommandData(int npcId, NPCCommandData data) {
        npcCommands.put(npcId, data);
        saveConfig();
    }

    public void removeNPCCommandData(int npcId) {
        npcCommands.remove(npcId);
        saveConfig();
    }

    public boolean hasNPCCommandData(int npcId) {
        return npcCommands.containsKey(npcId);
    }

    public Collection<NPCCommandData> getAllNPCCommandData() {
        return npcCommands.values();
    }

    public void addCommand(int npcId, NPCCommandData.CommandType type, String command) {
        NPCCommandData data = npcCommands.computeIfAbsent(npcId, NPCCommandData::new);
        data.addCommand(type, command);
        saveConfig();
    }

    public void removeCommand(int npcId, int index) {
        NPCCommandData data = npcCommands.get(npcId);
        if (data != null) {
            data.removeCommand(index);
            if (data.getCommands().isEmpty()) {
                npcCommands.remove(npcId);
            }
            saveConfig();
        }
    }

    public void setCooldown(int npcId, long cooldown) {
        NPCCommandData data = npcCommands.get(npcId);
        if (data != null) {
            data.setCooldown(cooldown);
            saveConfig();
        }
    }

    public void reloadConfig() {
        loadConfig();
    }
}
