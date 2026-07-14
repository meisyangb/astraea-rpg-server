package cn.guangdian.vipname.variable;

import cn.guangdian.vipname.VIPname;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 变量管理器
 * 
 * 用于称号的动态配置和显示
 */
public class VariableManager {

    private final VIPname plugin;
    
    // 预定义变量 {variable_name}
    private final Map<String, VariableProvider> variables = new ConcurrentHashMap<>();
    
    // 变量模式匹配
    private static final Pattern VARIABLE_PATTERN = Pattern.compile("\\{([^}]+)\\}");
    
    public VariableManager(VIPname plugin) {
        this.plugin = plugin;
        registerDefaultVariables();
    }
    
    /**
     * 加载配置中的变量
     */
    public void load() {
        FileConfiguration config = plugin.getConfig();
        ConfigurationSection varsSection = config.getConfigurationSection("variables");
        if (varsSection == null) return;
        
        for (String varName : varsSection.getKeys(false)) {
            String value = varsSection.getString(varName, "");
            registerVariable(varName, (player, args) -> value);
        }
    }
    
    /**
     * 注册默认变量
     */
    private void registerDefaultVariables() {
        // 玩家信息
        registerVariable("player_name", (player, args) -> player.getName());
        registerVariable("player_displayname", (player, args) -> player.getDisplayName());
        registerVariable("player_level", (player, args) -> String.valueOf(player.getLevel()));
        registerVariable("player_exp", (player, args) -> String.valueOf(player.getTotalExperience()));
        registerVariable("player_health", (player, args) -> String.valueOf((int) player.getHealth()));
        registerVariable("player_max_health", (player, args) -> String.valueOf((int) player.getMaxHealth()));
        registerVariable("player_food", (player, args) -> String.valueOf(player.getFoodLevel()));
        registerVariable("player_world", (player, args) -> player.getWorld().getName());
        registerVariable("player_gamemode", (player, args) -> player.getGameMode().name());
        
        // 时间相关
        registerVariable("time", (player, args) -> {
            long time = player.getWorld().getTime();
            int hours = (int) ((time / 1000 + 6) % 24);
            int minutes = (int) ((time % 1000) * 60 / 1000);
            return String.format("%02d:%02d", hours, minutes);
        });
        
        // 坐标
        registerVariable("location", (player, args) -> {
            int x = player.getLocation().getBlockX();
            int y = player.getLocation().getBlockY();
            int z = player.getLocation().getBlockZ();
            return x + ", " + y + ", " + z;
        });
        registerVariable("x", (player, args) -> String.valueOf(player.getLocation().getBlockX()));
        registerVariable("y", (player, args) -> String.valueOf(player.getLocation().getBlockY()));
        registerVariable("z", (player, args) -> String.valueOf(player.getLocation().getBlockZ()));
    }
    
    /**
     * 注册变量
     */
    public void registerVariable(String name, VariableProvider provider) {
        variables.put(name.toLowerCase(), provider);
    }
    
    /**
     * 处理文本中的变量
     */
    public String processVariables(Player player, String text) {
        if (text == null || text.isEmpty()) return "";
        
        Matcher matcher = VARIABLE_PATTERN.matcher(text);
        StringBuffer result = new StringBuffer();
        
        while (matcher.find()) {
            String varExpression = matcher.group(1);
            String replacement = resolveVariable(player, varExpression);
            matcher.appendReplacement(result, replacement);
        }
        matcher.appendTail(result);
        
        return result.toString();
    }
    
    /**
     * 解析变量表达式
     */
    private String resolveVariable(Player player, String expression) {
        // 解析变量名和参数 (格式: var_name:arg1:arg2)
        String[] parts = expression.split(":", 2);
        String varName = parts[0].toLowerCase();
        String args = parts.length > 1 ? parts[1] : "";
        
        VariableProvider provider = variables.get(varName);
        if (provider != null) {
            try {
                return provider.provide(player, args);
            } catch (Exception e) {
                plugin.getLogger().warning("变量解析失败: " + varName + " - " + e.getMessage());
            }
        }
        
        // 尝试从 PlaceholderAPI 获取
        if (plugin.getServer().getPluginManager().getPlugin("PlaceholderAPI") != null) {
            try {
                String papiResult = me.clip.placeholderapi.PlaceholderAPI.setPlaceholders(player, "%" + expression + "%");
                if (papiResult != null && !papiResult.equals("%" + expression + "%")) {
                    return papiResult;
                }
            } catch (Exception ignored) {}
        }
        
        return "{" + expression + "}";
    }
    
    /**
     * 变量提供者接口
     */
    @FunctionalInterface
    public interface VariableProvider {
        String provide(Player player, String args);
    }
}