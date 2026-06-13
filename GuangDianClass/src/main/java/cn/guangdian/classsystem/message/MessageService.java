package cn.guangdian.classsystem.message;

import cn.guangdian.classsystem.GuangDianClass;
import cn.guangdian.rpgcore.RPGCore;
import cn.guangdian.rpgcore.message.MiniMessageService;
import net.kyori.adventure.text.Component;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;

import java.io.File;
import java.util.HashMap;
import java.util.Map;

public class MessageService {
    
    private static MessageService instance;
    
    private final GuangDianClass plugin;
    private final MiniMessageService miniMessage;
    private File messagesFile;
    private FileConfiguration messagesConfig;
    private final Map<String, String> messageCache;
    private String prefix;
    
    public MessageService(GuangDianClass plugin) {
        this.plugin = plugin;
        this.messageCache = new HashMap<>();
        
        RPGCore rpgCore = RPGCore.getInstance();
        this.miniMessage = rpgCore != null ? MiniMessageService.getInstance() : null;
        
        loadMessages();
        instance = this;
    }
    
    public static MessageService getInstance() {
        return instance;
    }
    
    private void loadMessages() {
        messagesFile = new File(plugin.getDataFolder(), "messages.yml");
        if (!messagesFile.exists()) {
            plugin.saveResource("messages.yml", false);
        }
        messagesConfig = YamlConfiguration.loadConfiguration(messagesFile);
        
        prefix = getMessage("messages.prefix", "<gold>[职业系统] <reset>");
        
        messageCache.clear();
        loadMessageCache();
    }
    
    private void loadMessageCache() {
        for (String key : messagesConfig.getKeys(true)) {
            if (messagesConfig.isString(key)) {
                messageCache.put(key, messagesConfig.getString(key));
            }
        }
    }
    
    public String getMessage(String path, String defaultValue) {
        return messageCache.getOrDefault(path, defaultValue);
    }
    
    public String getMessage(String path) {
        return getMessage(path, "");
    }
    
    public Component getComponent(String path, String defaultValue) {
        String message = getMessage(path, defaultValue);
        if (miniMessage != null) {
            return miniMessage.colorize(message);
        }
        return Component.text(message);
    }
    
    public Component getComponent(String path) {
        return getComponent(path, "");
    }
    
    public void sendMessage(Player player, String path, String defaultValue) {
        String message = prefix + getMessage(path, defaultValue);
        if (miniMessage != null) {
            player.sendMessage(miniMessage.colorize(message));
        } else {
            player.sendMessage(Component.text(message));
        }
    }
    
    public void sendMessage(Player player, String path) {
        sendMessage(player, path, "");
    }
    
    public void sendRawMessage(Player player, String path, String defaultValue) {
        String message = getMessage(path, defaultValue);
        if (miniMessage != null) {
            player.sendMessage(miniMessage.colorize(message));
        } else {
            player.sendMessage(Component.text(message));
        }
    }
    
    public void sendRawMessage(Player player, String path) {
        sendRawMessage(player, path, "");
    }
    
    public String format(String path, String... replacements) {
        String message = getMessage(path);
        for (int i = 0; i < replacements.length - 1; i += 2) {
            message = message.replace("%" + replacements[i] + "%", replacements[i + 1]);
        }
        return message;
    }
    
    public Component formatComponent(String path, String... replacements) {
        String message = format(path, replacements);
        if (miniMessage != null) {
            return miniMessage.colorize(message);
        }
        return Component.text(message);
    }
    
    public void sendFormattedMessage(Player player, String path, String... replacements) {
        String message = prefix + format(path, replacements);
        if (miniMessage != null) {
            player.sendMessage(miniMessage.colorize(message));
        } else {
            player.sendMessage(Component.text(message));
        }
    }
    
    public void sendError(Player player, String errorPath, String... replacements) {
        sendFormattedMessage(player, "messages.errors." + errorPath, replacements);
    }
    
    public void sendSuccess(Player player, String successPath, String... replacements) {
        sendFormattedMessage(player, "messages.success." + successPath, replacements);
    }
    
    public void reload() {
        loadMessages();
    }
    
    public String getPrefix() {
        return prefix;
    }
}
