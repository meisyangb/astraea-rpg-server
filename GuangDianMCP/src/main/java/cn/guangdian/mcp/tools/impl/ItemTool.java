package cn.guangdian.mcp.tools.impl;

import cn.guangdian.mcp.GuangDianMCP;
import cn.guangdian.rpgcore.RPGCore;
import cn.guangdian.mcp.tools.MCPTool;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.Map;

public class ItemTool implements MCPTool {
    
    private final GuangDianMCP plugin;
    
    public ItemTool(GuangDianMCP plugin) {
        this.plugin = plugin;
    }
    
    @Override
    public String getName() {
        return "item";
    }
    
    @Override
    public String getDescription() {
        return "物品管理: 给予物品、查看背包等";
    }
    
    @Override
    public JsonObject getInputSchema() {
        JsonObject schema = new JsonObject();
        schema.addProperty("type", "object");
        
        JsonObject properties = new JsonObject();
        
        JsonObject action = new JsonObject();
        action.addProperty("type", "string");
        JsonArray actionEnum = new JsonArray();
        actionEnum.add("give");
        actionEnum.add("inventory");
        actionEnum.add("clear");
        action.add("enum", actionEnum);
        action.addProperty("description", "要执行的操作");
        properties.add("action", action);
        
        JsonObject player = new JsonObject();
        player.addProperty("type", "string");
        player.addProperty("description", "玩家名称");
        properties.add("player", player);
        
        JsonObject material = new JsonObject();
        material.addProperty("type", "string");
        material.addProperty("description", "物品材质(如DIAMOND)");
        properties.add("material", material);
        
        JsonObject amount = new JsonObject();
        amount.addProperty("type", "integer");
        amount.addProperty("description", "数量");
        properties.add("amount", amount);
        
        schema.add("properties", properties);
        
        JsonArray required = new JsonArray();
        required.add("action");
        required.add("player");
        schema.add("required", required);
        
        return schema;
    }
    
    @Override
    public ToolResult execute(Map<String, Object> arguments) {
        String action = (String) arguments.get("action");
        String playerName = (String) arguments.get("player");
        
        if (action == null) {
            return ToolResult.error("缺少action参数");
        }
        if (playerName == null) {
            return ToolResult.error("缺少player参数");
        }
        
        Player player = plugin.getServer().getPlayer(playerName);
        if (player == null) {
            return ToolResult.error("玩家不在线: " + playerName);
        }
        
        return switch (action.toLowerCase()) {
            case "give" -> {
                String materialName = (String) arguments.get("material");
                int amount = arguments.containsKey("amount") ? ((Number) arguments.get("amount")).intValue() : 1;
                if (materialName == null) yield ToolResult.error("缺少material参数");
                yield giveItem(player, materialName, amount);
            }
            case "inventory" -> getInventory(player);
            case "clear" -> clearInventory(player);
            default -> ToolResult.error("未知操作: " + action);
        };
    }
    
    private ToolResult giveItem(Player player, String materialName, int amount) {
        Material material;
        try {
            material = Material.valueOf(materialName.toUpperCase());
        } catch (IllegalArgumentException e) {
            return ToolResult.error("无效的物品材质: " + materialName);
        }
        
        ItemStack item = new ItemStack(material, amount);
        
        plugin.getServer().getScheduler().runTask(plugin, () -> {
            player.getInventory().addItem(item);
        });
        
        return ToolResult.success("已给予 " + player.getName() + " " + amount + "x " + materialName);
    }
    
    private ToolResult getInventory(Player player) {
        JsonObject result = new JsonObject();
        JsonArray items = new JsonArray();
        
        ItemStack[] contents = player.getInventory().getContents();
        for (int i = 0; i < contents.length; i++) {
            ItemStack item = contents[i];
            if (item != null && item.getType() != Material.AIR) {
                JsonObject itemObj = new JsonObject();
                itemObj.addProperty("slot", i);
                itemObj.addProperty("material", item.getType().name());
                itemObj.addProperty("amount", item.getAmount());
                if (item.hasItemMeta()) {
                    if (item.getItemMeta().hasDisplayName()) {
                        itemObj.addProperty("displayName", item.getItemMeta().getDisplayName());
                    }
                }
                items.add(itemObj);
            }
        }
        
        result.addProperty("player", player.getName());
        result.add("items", items);
        result.addProperty("itemCount", items.size());
        
        return ToolResult.success(result.toString());
    }
    
    private ToolResult clearInventory(Player player) {
        plugin.getServer().getScheduler().runTask(plugin, () -> {
            player.getInventory().clear();
        });
        
        return ToolResult.success("已清空 " + player.getName() + " 的背包");
    }
}
