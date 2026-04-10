package cn.guangdian.npc;

import cn.guangdian.npc.adapter.NPCServiceAdapter;
import cn.guangdian.npc.api.NPCAPI;
import cn.guangdian.npc.api.NPCAPIImpl;
import cn.guangdian.npc.manager.NPCManager;
import cn.guangdian.npc.model.NPCData;
import cn.guangdian.npc.model.NPCType;
import cn.guangdian.npc.papi.NPCPlaceholders;
import cn.guangdian.rpgcore.RPGCore;
import cn.guangdian.rpgcore.api.AsyncExecutor;
import cn.guangdian.rpgcore.plugin.AbstractRPGPlugin;
import me.clip.placeholderapi.PlaceholderAPI;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Villager;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityTargetLivingEntityEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.ServicePriority;

import java.util.*;
import java.util.stream.Collectors;

public final class GuangDianNPC extends AbstractRPGPlugin implements Listener, CommandExecutor, TabCompleter {

    private static final String NPC_TAG = "guangdian_npc";
    private static final String NPC_ID_KEY = "npc_id";

    private static GuangDianNPC instance;

    private NPCManager npcManager;
    private NPCAPI npcAPI;
    private NPCServiceAdapter serviceAdapter;
    private NPCPlaceholders placeholders;

    private org.bukkit.NamespacedKey npcIdKey;

    @Override
    protected void onPluginEnable() {
        instance = this;

        saveDefaultConfig();
        saveResource("npcs.yml", false);

        npcIdKey = new org.bukkit.NamespacedKey(this, NPC_ID_KEY);

        npcManager = new NPCManager(this);
        npcAPI = new NPCAPIImpl(this);

        initRPGCoreIntegration();

        npcManager.load();
        npcManager.spawnAll();

        if (getCommand("npc") != null) {
            getCommand("npc").setExecutor(this);
            getCommand("npc").setTabCompleter(this);
        }

        if (getCommand("npcmenu") != null) {
            getCommand("npcmenu").setExecutor(this);
            getCommand("npcmenu").setTabCompleter(this);
        }

        Bukkit.getPluginManager().registerEvents(this, this);

        hookPlaceholderAPI();

        registerBukkitAPI();

        getLogger().info("GuangDianNPC 已启用，已加载 NPC 数量: " + npcManager.getNPCCount());
        logOptimizationStatus();
    }

    @Override
    protected void onPluginDisable() {
        if (serviceAdapter != null) {
            serviceAdapter.unregister();
        }

        if (placeholders != null) {
            PlaceholderAPI.unregisterExpansion(placeholders);
            placeholders = null;
        }

        if (npcManager != null) {
            npcManager.despawnAll();
            npcManager.save();
        }

        getLogger().info("GuangDianNPC 已禁用");
    }

    @Override
    protected String getPluginName() {
        return "GuangDianNPC";
    }

    private void initRPGCoreIntegration() {
        serviceAdapter = new NPCServiceAdapter(this);
        if (serviceAdapter.isUsingRPGCore()) {
            getLogger().info("已集成 RPGCore 服务系统!");
        }
    }

    private void hookPlaceholderAPI() {
        if (getServer().getPluginManager().isPluginEnabled("PlaceholderAPI")) {
            placeholders = new NPCPlaceholders(this);
            placeholders.register();
            getLogger().info("已注册 PlaceholderAPI 扩展: gdnpc");
        }
    }

    private void registerBukkitAPI() {
        getServer().getServicesManager().register(NPCAPI.class, npcAPI, this, ServicePriority.Normal);
    }

    private void logOptimizationStatus() {
        getLogger().info("========== 优化组件状态 ==========");
        getLogger().info("RPGCore 集成: " + (serviceAdapter != null && serviceAdapter.isUsingRPGCore() ? "已启用" : "未启用"));
        getLogger().info("异步保存: " + (serviceAdapter != null && serviceAdapter.getAsyncExecutor() != null ? "已启用" : "未启用"));
        getLogger().info("PlaceholderAPI: " + (placeholders != null ? "已启用" : "未启用"));
        getLogger().info("==================================");
    }

    public static GuangDianNPC getInstance() {
        return instance;
    }

    public NPCManager getNpcManager() {
        return npcManager;
    }

    public NPCAPI getNpcAPI() {
        return npcAPI;
    }

    public NPCServiceAdapter getServiceAdapter() {
        return serviceAdapter;
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onPlayerInteractEntity(PlayerInteractEntityEvent event) {
        if (!(event.getRightClicked() instanceof Villager villager)) {
            return;
        }
        if (!npcManager.isManagedNPC(villager)) {
            return;
        }

        String npcId = villager.getPersistentDataContainer().get(npcIdKey, PersistentDataType.STRING);
        if (npcId == null) {
            return;
        }

        NPCData npc = npcManager.getNPC(npcId);
        if (npc == null) {
            return;
        }

        event.setCancelled(true);
        npcAPI.openMenu(event.getPlayer(), npc.getMenuId());
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onEntityDamage(EntityDamageEvent event) {
        if (npcManager.isManagedNPC(event.getEntity())) {
            event.setCancelled(true);
            event.setDamage(0.0D);
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onEntityDamageByEntity(EntityDamageByEntityEvent event) {
        if (npcManager.isManagedNPC(event.getEntity())) {
            event.setCancelled(true);
            event.setDamage(0.0D);
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onEntityTarget(EntityTargetLivingEntityEvent event) {
        if (npcManager.isManagedNPC(event.getTarget())) {
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onEntityInteract(org.bukkit.event.entity.EntityInteractEvent event) {
        if (npcManager.isManagedNPC(event.getEntity())) {
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onPlayerInteractAtEntity(org.bukkit.event.player.PlayerInteractAtEntityEvent event) {
        if (npcManager.isManagedNPC(event.getRightClicked())) {
            event.setCancelled(true);
        }
    }
    
    @EventHandler(priority = EventPriority.HIGHEST)
    public void onVehicleEntityCollision(org.bukkit.event.vehicle.VehicleEntityCollisionEvent event) {
        if (npcManager.isManagedNPC(event.getEntity())) {
            event.setCancelled(true);
            event.setCollisionCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onCreatureSpawn(org.bukkit.event.entity.CreatureSpawnEvent event) {
        if (npcManager.isManagedNPC(event.getEntity())) {
            if (event.getSpawnReason() == org.bukkit.event.entity.CreatureSpawnEvent.SpawnReason.DEFAULT) {
                event.setCancelled(true);
            }
        }
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getInventory().getHolder() instanceof NPCAPIImpl.NPCMenuHolder holder)) {
            return;
        }
        event.setCancelled(true);

        if (!(event.getWhoClicked() instanceof Player player)) {
            return;
        }
        if (event.getRawSlot() < 0 || event.getRawSlot() >= event.getInventory().getSize()) {
            return;
        }

        NPCManager.MenuDefinition menu = npcManager.getMenu(holder.getMenuId());
        if (menu == null) {
            return;
        }

        for (NPCManager.MenuItemDefinition item : menu.getItems()) {
            if (item.getSlot() == event.getRawSlot()) {
                executeAction(player, item.getAction());
                return;
            }
        }
    }

    @EventHandler
    public void onInventoryDrag(InventoryDragEvent event) {
        if (event.getInventory().getHolder() instanceof NPCAPIImpl.NPCMenuHolder) {
            event.setCancelled(true);
        }
    }

    private void executeAction(Player player, String action) {
        if (action == null || action.isBlank()) {
            return;
        }

        if (action.startsWith("message:")) {
            player.sendMessage(legacy(action.substring("message:".length())));
            return;
        }
        if (action.startsWith("command:")) {
            player.performCommand(action.substring("command:".length()));
            return;
        }
        if (action.startsWith("console:")) {
            Bukkit.dispatchCommand(Bukkit.getConsoleSender(),
                action.substring("console:".length()).replace("%player%", player.getName()));
            return;
        }
        if (action.startsWith("menu:")) {
            npcAPI.openMenu(player, action.substring("menu:".length()));
            return;
        }
        if (action.startsWith("npc:")) {
            String npcId = action.substring("npc:".length());
            npcAPI.openNPCMenu(player, npcId);
            return;
        }
        // 支持 GuangDianMenu 菜单
        if (action.startsWith("gdmenu:")) {
            String menuId = action.substring("gdmenu:".length());
            player.performCommand("menu " + menuId);
            return;
        }
        // 支持任务命令
        if (action.startsWith("quest:")) {
            String questCmd = action.substring("quest:".length());
            player.performCommand("quest " + questCmd);
            return;
        }
        if (action.equalsIgnoreCase("close")) {
            player.closeInventory();
        }
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        String cmdName = command.getName().toLowerCase();

        if (cmdName.equals("npcmenu")) {
            return handleNPCMenuCommand(sender, args);
        }

        if (args.length == 0) {
            sendHelp(sender);
            return true;
        }

        if (!sender.hasPermission("guangdian.npc.admin")) {
            sender.sendMessage(legacy("&c你没有权限。"));
            return true;
        }

        switch (args[0].toLowerCase()) {
            case "create" -> {
                if (!(sender instanceof Player player) || args.length < 2) {
                    sender.sendMessage(legacy("&c用法: /npc create <id> [menu]"));
                    return true;
                }
                String id = args[1].toLowerCase();
                String menuId = args.length >= 3 ? args[2].toLowerCase() : "main";

                NPCData npc = npcManager.createNPC(id, player, menuId);
                if (npc != null) {
                    sender.sendMessage(legacy("&a已创建 NPC: &e" + id));
                } else {
                    sender.sendMessage(legacy("&cNPC 已存在或创建失败。"));
                }
            }
            case "remove" -> {
                if (args.length < 2) {
                    sender.sendMessage(legacy("&c用法: /npc remove <id>"));
                    return true;
                }
                if (npcManager.removeNPC(args[1])) {
                    sender.sendMessage(legacy("&a已删除 NPC: &e" + args[1]));
                } else {
                    sender.sendMessage(legacy("&cNPC 不存在。"));
                }
            }
            case "movehere" -> {
                if (!(sender instanceof Player player) || args.length < 2) {
                    sender.sendMessage(legacy("&c用法: /npc movehere <id>"));
                    return true;
                }
                NPCData npc = npcManager.getNPC(args[1]);
                if (npc == null) {
                    sender.sendMessage(legacy("&cNPC 不存在。"));
                    return true;
                }
                npc.setLocation(player.getLocation());
                npcManager.respawnNPC(npc);
                npcManager.save();
                sender.sendMessage(legacy("&a已移动 NPC: &e" + npc.getId()));
            }
            case "name" -> {
                if (args.length < 3) {
                    sender.sendMessage(legacy("&c用法: /npc name <id> <名字>"));
                    return true;
                }
                NPCData npc = npcManager.getNPC(args[1]);
                if (npc == null) {
                    sender.sendMessage(legacy("&cNPC 不存在。"));
                    return true;
                }
                String displayName = String.join(" ", Arrays.copyOfRange(args, 2, args.length));
                npc.setDisplayName(displayName);
                npcManager.respawnNPC(npc);
                npcManager.save();
                sender.sendMessage(legacy("&a已更新 NPC 名字。"));
            }
            case "menu" -> {
                if (args.length < 3) {
                    sender.sendMessage(legacy("&c用法: /npc menu <id> <menuId>"));
                    return true;
                }
                NPCData npc = npcManager.getNPC(args[1]);
                if (npc == null) {
                    sender.sendMessage(legacy("&cNPC 不存在。"));
                    return true;
                }
                npc.setMenuId(args[2].toLowerCase());
                npcManager.save();
                sender.sendMessage(legacy("&a已更新 NPC 菜单为: &e" + npc.getMenuId()));
            }
            case "type" -> {
                if (args.length < 3) {
                    sender.sendMessage(legacy("&c用法: /npc type <id> <类型>"));
                    sender.sendMessage(legacy("&e可用类型: &fSHOP, QUEST, TELEPORT, BANK, GUILD, TRAINER, REPAIR, IDENTIFY, GENERAL"));
                    return true;
                }
                NPCData npc = npcManager.getNPC(args[1]);
                if (npc == null) {
                    sender.sendMessage(legacy("&cNPC 不存在。"));
                    return true;
                }
                NPCType type = NPCType.fromString(args[2]);
                npc.setType(type);
                npcManager.respawnNPC(npc);
                npcManager.save();
                sender.sendMessage(legacy("&a已更新 NPC 类型为: &e" + type.getDisplayName()));
            }
            case "tp" -> {
                if (!(sender instanceof Player player) || args.length < 2) {
                    sender.sendMessage(legacy("&c用法: /npc tp <id>"));
                    return true;
                }
                npcAPI.teleportToNPC(player, args[1]);
            }
            case "enable" -> {
                if (args.length < 3) {
                    sender.sendMessage(legacy("&c用法: /npc enable <id> <true/false>"));
                    return true;
                }
                NPCData npc = npcManager.getNPC(args[1]);
                if (npc == null) {
                    sender.sendMessage(legacy("&cNPC 不存在。"));
                    return true;
                }
                boolean enabled = Boolean.parseBoolean(args[2]);
                npc.setEnabled(enabled);
                if (enabled) {
                    npcManager.spawnNPC(npc);
                } else {
                    npcManager.despawnNPC(npc);
                }
                npcManager.save();
                sender.sendMessage(legacy("&a已" + (enabled ? "启用" : "禁用") + " NPC: &e" + npc.getId()));
            }
            case "list" -> {
                sender.sendMessage(legacy("&6NPC 列表 (&e" + npcManager.getNPCCount() + "&6):"));
                for (NPCData npc : npcManager.getAllNPCs()) {
                    String status = npc.isEnabled() ? "&a启用" : "&c禁用";
                    sender.sendMessage(legacy("&e- " + npc.getId() + " &7[" + npc.getWorldName() + " | " + npc.getMenuId() + "] " + status));
                }
            }
            case "reload" -> {
                npcManager.reload();
                sender.sendMessage(legacy("&aNPC 配置已重载。"));
            }
            default -> sender.sendMessage(legacy("&c未知子命令。"));
        }
        return true;
    }

    private boolean handleNPCMenuCommand(CommandSender sender, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(legacy("&c只有玩家可以使用此命令。"));
            return true;
        }

        if (args.length == 0) {
            sender.sendMessage(legacy("&c用法: /npcmenu <menuId>"));
            sender.sendMessage(legacy("&c用法: /npcmenu npc <npcId>"));
            return true;
        }

        if (args[0].equalsIgnoreCase("npc") && args.length >= 2) {
            npcAPI.openNPCMenu(player, args[1]);
        } else {
            npcAPI.openMenu(player, args[0]);
        }
        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        List<String> completions = new ArrayList<>();
        String cmdName = command.getName().toLowerCase();

        if (cmdName.equals("npcmenu")) {
            if (args.length == 1) {
                completions.add("npc");
                completions.addAll(npcManager.getAllMenus().stream()
                    .map(m -> m.getId()).collect(Collectors.toList()));
            }
            if (args.length == 2 && args[0].equalsIgnoreCase("npc")) {
                completions.addAll(npcManager.getAllNPCs().stream()
                    .map(n -> n.getId()).collect(Collectors.toList()));
            }
            return completions.stream()
                .filter(s -> s.toLowerCase().startsWith(args[args.length - 1].toLowerCase()))
                .collect(Collectors.toList());
        }

        if (args.length == 1) {
            completions.addAll(Arrays.asList("create", "remove", "movehere", "name", "menu", "type", "tp", "enable", "list", "reload"));
        } else if (args.length == 2 && !"create".equalsIgnoreCase(args[0])) {
            completions.addAll(npcManager.getAllNPCs().stream()
                .map(n -> n.getId()).collect(Collectors.toList()));
        } else if (args.length == 3 && ("create".equalsIgnoreCase(args[0]) || "menu".equalsIgnoreCase(args[0]))) {
            completions.addAll(npcManager.getAllMenus().stream()
                .map(m -> m.getId()).collect(Collectors.toList()));
        } else if (args.length == 3 && "type".equalsIgnoreCase(args[0])) {
            completions.addAll(Arrays.asList("SHOP", "QUEST", "TELEPORT", "BANK", "GUILD", "TRAINER", "REPAIR", "IDENTIFY", "GENERAL"));
        } else if (args.length == 3 && "enable".equalsIgnoreCase(args[0])) {
            completions.addAll(Arrays.asList("true", "false"));
        }

        return completions.stream()
            .filter(s -> s.toLowerCase().startsWith(args[args.length - 1].toLowerCase()))
            .collect(Collectors.toList());
    }

    private void sendHelp(CommandSender sender) {
        sender.sendMessage(legacy("&6========== NPC 帮助 =========="));
        sender.sendMessage(legacy("&e/npc create <id> [menu] &7- 创建 NPC"));
        sender.sendMessage(legacy("&e/npc remove <id> &7- 删除 NPC"));
        sender.sendMessage(legacy("&e/npc movehere <id> &7- 移动 NPC 到当前位置"));
        sender.sendMessage(legacy("&e/npc name <id> <名字> &7- 设置 NPC 名字"));
        sender.sendMessage(legacy("&e/npc menu <id> <menuId> &7- 设置 NPC 菜单"));
        sender.sendMessage(legacy("&e/npc type <id> <类型> &7- 设置 NPC 类型"));
        sender.sendMessage(legacy("&e/npc tp <id> &7- 传送到 NPC"));
        sender.sendMessage(legacy("&e/npc enable <id> <true/false> &7- 启用/禁用 NPC"));
        sender.sendMessage(legacy("&e/npc list &7- NPC 列表"));
        sender.sendMessage(legacy("&e/npc reload &7- 重载配置"));
        sender.sendMessage(legacy("&e/npcmenu <menuId> &7- 直接打开菜单"));
        sender.sendMessage(legacy("&e/npcmenu npc <npcId> &7- 打开 NPC 的菜单"));
        sender.sendMessage(legacy("&6=============================="));
    }

    private Component color(String text) {
        return LegacyComponentSerializer.legacyAmpersand().deserialize(text == null ? "" : text);
    }

    private String legacy(String text) {
        return ChatColor.translateAlternateColorCodes('&', text == null ? "" : text);
    }
}
