package cn.guangdian.npc;

import cn.guangdian.npc.adapter.NPCServiceAdapter;
import cn.guangdian.npc.api.NPCAPI;
import cn.guangdian.npc.api.NPCAPIImpl;
import cn.guangdian.npc.dialogue.DialogueListener;
import cn.guangdian.npc.dialogue.DialogueManager;
import cn.guangdian.npc.manager.NPCManager;
import cn.guangdian.npc.model.NPCData;
import cn.guangdian.npc.model.NPCType;
import cn.guangdian.npc.papi.NPCPlaceholders;
import cn.guangdian.rpgcore.RPGCore;
import cn.guangdian.rpgcore.api.AsyncExecutor;
import cn.guangdian.rpgcore.entity.EntityService;
import cn.guangdian.rpgcore.message.MiniMessageService;
import cn.guangdian.rpgcore.plugin.AbstractRPGPlugin;
import me.clip.placeholderapi.PlaceholderAPI;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.Bukkit;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
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
    private DialogueManager dialogueManager;
    private DialogueListener dialogueListener;

    // RPGCore 服务引用
    private EntityService entityService;
    private MiniMessageService miniMessage;

    private org.bukkit.NamespacedKey npcIdKey;

    @Override
    protected void onPluginEnable() {
        instance = this;

        saveDefaultConfig();
        saveResource("npcs.yml", false);
        saveResource("dialogues.yml", false);

        npcIdKey = new org.bukkit.NamespacedKey(this, NPC_ID_KEY);

        npcManager = new NPCManager(this);
        npcAPI = new NPCAPIImpl(this);
        dialogueManager = new DialogueManager(this);

        initRPGCoreIntegration();

        npcManager.load();
        dialogueManager.load();
        npcManager.spawnAll();

        dialogueListener = new DialogueListener(this, dialogueManager);
        Bukkit.getPluginManager().registerEvents(dialogueListener, this);

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

        getLogger().info("GuangDianNPC 已启用");
        getLogger().info("  - NPC 数量: " + npcManager.getNPCCount());
        getLogger().info("  - 对话数量: " + dialogueManager.getDialogueCount());
        logOptimizationStatus();
    }

    @Override
    protected void onPluginDisable() {
        // 取消所有调度任务
        cancelAllTasks();

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

        if (dialogueManager != null) {
            dialogueManager.save();
        }

        getLogger().info("GuangDianNPC 已禁用");
    }

    @Override
    protected String getPluginName() {
        return "GuangDianNPC";
    }

    private void initRPGCoreIntegration() {
        serviceAdapter = new NPCServiceAdapter(this);

        // 获取 RPGCore 服务
        if (getServer().getPluginManager().isPluginEnabled("RPGCore")) {
            try {
                RPGCore rpgCore = RPGCore.getInstance();
                entityService = rpgCore.getEntityService();
                miniMessage = rpgCore.getMiniMessageService();
                getLogger().info("使用 RPGCore 服务 (EntityService, MiniMessageService)");
            } catch (Exception e) {
                getLogger().warning("无法获取 RPGCore 服务: " + e.getMessage());
            }
        }

        // 如果 RPGCore 服务不可用，使用本地降级
        if (entityService == null) {
            entityService = EntityService.getInstance();
        }
        if (miniMessage == null) {
            miniMessage = MiniMessageService.getInstance();
        }

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

    /**
     * 获取 EntityService
     * @return EntityService 实例（可能为本地降级实现）
     */
    public EntityService getEntityService() {
        return entityService;
    }

    /**
     * 获取 MiniMessageService
     * @return MiniMessageService 实例（可能为本地降级实现）
     */
    public MiniMessageService getMiniMessageService() {
        return miniMessage;
    }

    public DialogueManager getDialogueManager() {
        return dialogueManager;
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

        // 优先检查是否有对话
        if (dialogueManager.hasDialogue(npcId)) {
            boolean started = dialogueManager.startDialogue(event.getPlayer(), npcId);
            if (started) {
                return;
            }
        }

        // 没有对话则打开菜单
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
            // 使用 RPGCore EntityService 处理碰撞取消
            if (entityService != null) {
                entityService.setCollisionCancelled(event.getEntity(), true);
            } else {
                // 降级处理
                event.setCollisionCancelled(true);
            }
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
            sender.sendMessage(legacy("<red>你没有权限。"));
            return true;
        }

        switch (args[0].toLowerCase()) {
            case "create" -> {
                if (!(sender instanceof Player player) || args.length < 2) {
                    sender.sendMessage(legacy("<red>用法: /npc create <id> [menu]"));
                    return true;
                }
                String id = args[1].toLowerCase();
                String menuId = args.length >= 3 ? args[2].toLowerCase() : "main";

                NPCData npc = npcManager.createNPC(id, player, menuId);
                if (npc != null) {
                    sender.sendMessage(legacy("<green>已创建 NPC: <yellow>" + id));
                } else {
                    sender.sendMessage(legacy("<red>NPC 已存在或创建失败。"));
                }
            }
            case "remove" -> {
                if (args.length < 2) {
                    sender.sendMessage(legacy("<red>用法: /npc remove <id>"));
                    return true;
                }
                if (npcManager.removeNPC(args[1])) {
                    sender.sendMessage(legacy("<green>已删除 NPC: <yellow>" + args[1]));
                } else {
                    sender.sendMessage(legacy("<red>NPC 不存在。"));
                }
            }
            case "movehere" -> {
                if (!(sender instanceof Player player) || args.length < 2) {
                    sender.sendMessage(legacy("<red>用法: /npc movehere <id>"));
                    return true;
                }
                NPCData npc = npcManager.getNPC(args[1]);
                if (npc == null) {
                    sender.sendMessage(legacy("<red>NPC 不存在。"));
                    return true;
                }
                npc.setLocation(player.getLocation());
                npcManager.respawnNPC(npc);
                npcManager.save();
                sender.sendMessage(legacy("<green>已移动 NPC: <yellow>" + npc.getId()));
            }
            case "name" -> {
                if (args.length < 3) {
                    sender.sendMessage(legacy("<red>用法: /npc name <id> <名字>"));
                    return true;
                }
                NPCData npc = npcManager.getNPC(args[1]);
                if (npc == null) {
                    sender.sendMessage(legacy("<red>NPC 不存在。"));
                    return true;
                }
                String displayName = String.join(" ", Arrays.copyOfRange(args, 2, args.length));
                npc.setDisplayName(displayName);
                npcManager.respawnNPC(npc);
                npcManager.save();
                sender.sendMessage(legacy("<green>已更新 NPC 名字。"));
            }
            case "menu" -> {
                if (args.length < 3) {
                    sender.sendMessage(legacy("<red>用法: /npc menu <id> <menuId>"));
                    return true;
                }
                NPCData npc = npcManager.getNPC(args[1]);
                if (npc == null) {
                    sender.sendMessage(legacy("<red>NPC 不存在。"));
                    return true;
                }
                npc.setMenuId(args[2].toLowerCase());
                npcManager.save();
                sender.sendMessage(legacy("<green>已更新 NPC 菜单为: <yellow>" + npc.getMenuId()));
            }
            case "type" -> {
                if (args.length < 3) {
                    sender.sendMessage(legacy("<red>用法: /npc type <id> <类型>"));
                    sender.sendMessage(legacy("<yellow>可用类型: <white>SHOP, QUEST, TELEPORT, BANK, GUILD, TRAINER, REPAIR, IDENTIFY, GENERAL"));
                    return true;
                }
                NPCData npc = npcManager.getNPC(args[1]);
                if (npc == null) {
                    sender.sendMessage(legacy("<red>NPC 不存在。"));
                    return true;
                }
                NPCType type = NPCType.fromString(args[2]);
                npc.setType(type);
                npcManager.respawnNPC(npc);
                npcManager.save();
                sender.sendMessage(legacy("<green>已更新 NPC 类型为: <yellow>" + type.getDisplayName()));
            }
            case "tp" -> {
                if (!(sender instanceof Player player) || args.length < 2) {
                    sender.sendMessage(legacy("<red>用法: /npc tp <id>"));
                    return true;
                }
                npcAPI.teleportToNPC(player, args[1]);
            }
            case "enable" -> {
                if (args.length < 3) {
                    sender.sendMessage(legacy("<red>用法: /npc enable <id> <true/false>"));
                    return true;
                }
                NPCData npc = npcManager.getNPC(args[1]);
                if (npc == null) {
                    sender.sendMessage(legacy("<red>NPC 不存在。"));
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
                sender.sendMessage(legacy("<green>已" + (enabled ? "启用" : "禁用") + " NPC: <yellow>" + npc.getId()));
            }
            case "list" -> {
                sender.sendMessage(legacy("<gold>NPC 列表 (<yellow>" + npcManager.getNPCCount() + "<gold>):"));
                for (NPCData npc : npcManager.getAllNPCs()) {
                    String status = npc.isEnabled() ? "<green>启用" : "<red>禁用";
                    sender.sendMessage(legacy("<yellow>- " + npc.getId() + " <gray>[" + npc.getWorldName() + " | " + npc.getMenuId() + "] " + status));
                }
            }
            case "reload" -> {
                npcManager.reload();
                dialogueManager.reload();
                sender.sendMessage(legacy("<green>NPC 配置已重载。"));
            }
            case "dialogue" -> {
                return handleDialogueCommand(sender, args);
            }
            default -> sender.sendMessage(legacy("<red>未知子命令。"));
        }
        return true;
    }

    private boolean handleDialogueCommand(CommandSender sender, String[] args) {
        if (args.length < 2) {
            sender.sendMessage(legacy("<red>用法: /npc dialogue <bind|unbind|list|test>"));
            return true;
        }

        String subCmd = args[1].toLowerCase();
        switch (subCmd) {
            case "bind" -> {
                if (args.length < 4) {
                    sender.sendMessage(legacy("<red>用法: /npc dialogue bind <npcId> <dialogueId>"));
                    return true;
                }
                String npcId = args[2].toLowerCase();
                String dialogueId = args[3].toLowerCase();

                NPCData npc = npcManager.getNPC(npcId);
                if (npc == null) {
                    sender.sendMessage(legacy("<red>NPC 不存在。"));
                    return true;
                }

                dialogueManager.registerDialogueToNPC(npcId, dialogueId);
                sender.sendMessage(legacy("<green>已将对话 <yellow>" + dialogueId + " <green>绑定到 NPC <yellow>" + npcId));
            }
            case "unbind" -> {
                if (args.length < 3) {
                    sender.sendMessage(legacy("<red>用法: /npc dialogue unbind <npcId>"));
                    return true;
                }
                String npcId = args[2].toLowerCase();
                dialogueManager.unregisterDialogueFromNPC(npcId);
                sender.sendMessage(legacy("<green>已解除 NPC <yellow>" + npcId + " <green>的对话绑定"));
            }
            case "list" -> {
                sender.sendMessage(legacy("<gold>对话列表 (<yellow>" + dialogueManager.getDialogueCount() + "<gold>):"));
                for (var dialogue : dialogueManager.getAllDialogues()) {
                    String npcName = dialogue.getNpcId().isEmpty() ? "<gray>未绑定" : "<yellow>" + dialogue.getNpcId();
                    sender.sendMessage(legacy("<yellow>- " + dialogue.getId() + " <gray>[NPC: " + npcName + "<gray>]"));
                }
            }
            case "test" -> {
                if (!(sender instanceof Player player)) {
                    sender.sendMessage(legacy("<red>只有玩家可以使用此命令。"));
                    return true;
                }
                if (args.length < 3) {
                    sender.sendMessage(legacy("<red>用法: /npc dialogue test <dialogueId>"));
                    return true;
                }
                String dialogueId = args[2].toLowerCase();
                var dialogue = dialogueManager.getDialogue(dialogueId);
                if (dialogue == null) {
                    sender.sendMessage(legacy("<red>对话不存在。"));
                    return true;
                }
                dialogueManager.startDialogue(player, dialogue.getNpcId().isEmpty() ? "test" : dialogue.getNpcId());
                sender.sendMessage(legacy("<green>开始测试对话: <yellow>" + dialogueId));
            }
            default -> sender.sendMessage(legacy("<red>未知子命令。可用: bind, unbind, list, test"));
        }
        return true;
    }

    private boolean handleNPCMenuCommand(CommandSender sender, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(legacy("<red>只有玩家可以使用此命令。"));
            return true;
        }

        if (args.length == 0) {
            sender.sendMessage(legacy("<red>用法: /npcmenu <menuId>"));
            sender.sendMessage(legacy("<red>用法: /npcmenu npc <npcId>"));
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
            completions.addAll(Arrays.asList("create", "remove", "movehere", "name", "menu", "type", "tp", "enable", "list", "reload", "dialogue"));
        } else if (args.length == 2 && "dialogue".equalsIgnoreCase(args[0])) {
            completions.addAll(Arrays.asList("bind", "unbind", "list", "test"));
        } else if (args.length == 2 && !"create".equalsIgnoreCase(args[0]) && !"dialogue".equalsIgnoreCase(args[0])) {
            completions.addAll(npcManager.getAllNPCs().stream()
                .map(n -> n.getId()).collect(Collectors.toList()));
        } else if (args.length == 3 && ("create".equalsIgnoreCase(args[0]) || "menu".equalsIgnoreCase(args[0]))) {
            completions.addAll(npcManager.getAllMenus().stream()
                .map(m -> m.getId()).collect(Collectors.toList()));
        } else if (args.length == 3 && "dialogue".equalsIgnoreCase(args[0]) && "bind".equalsIgnoreCase(args[1])) {
            completions.addAll(npcManager.getAllNPCs().stream()
                .map(n -> n.getId()).collect(Collectors.toList()));
        } else if (args.length == 4 && "dialogue".equalsIgnoreCase(args[0]) && "bind".equalsIgnoreCase(args[1])) {
            completions.addAll(dialogueManager.getAllDialogues().stream()
                .map(d -> d.getId()).collect(Collectors.toList()));
        } else if (args.length == 3 && "dialogue".equalsIgnoreCase(args[0]) && ("unbind".equalsIgnoreCase(args[1]) || "test".equalsIgnoreCase(args[1]))) {
            completions.addAll(npcManager.getAllNPCs().stream()
                .map(n -> n.getId()).collect(Collectors.toList()));
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
        sender.sendMessage(Component.text("========== NPC 帮助 ==========").color(NamedTextColor.GOLD));
        sender.sendMessage(Component.text("/npc create <id> [menu]").color(NamedTextColor.YELLOW)
            .append(Component.text(" - 创建 NPC").color(NamedTextColor.GRAY)));
        sender.sendMessage(Component.text("/npc remove <id>").color(NamedTextColor.YELLOW)
            .append(Component.text(" - 删除 NPC").color(NamedTextColor.GRAY)));
        sender.sendMessage(Component.text("/npc movehere <id>").color(NamedTextColor.YELLOW)
            .append(Component.text(" - 移动 NPC 到当前位置").color(NamedTextColor.GRAY)));
        sender.sendMessage(Component.text("/npc name <id> <名字>").color(NamedTextColor.YELLOW)
            .append(Component.text(" - 设置 NPC 名字").color(NamedTextColor.GRAY)));
        sender.sendMessage(Component.text("/npc menu <id> <menuId>").color(NamedTextColor.YELLOW)
            .append(Component.text(" - 设置 NPC 菜单").color(NamedTextColor.GRAY)));
        sender.sendMessage(Component.text("/npc type <id> <类型>").color(NamedTextColor.YELLOW)
            .append(Component.text(" - 设置 NPC 类型").color(NamedTextColor.GRAY)));
        sender.sendMessage(Component.text("/npc tp <id>").color(NamedTextColor.YELLOW)
            .append(Component.text(" - 传送到 NPC").color(NamedTextColor.GRAY)));
        sender.sendMessage(Component.text("/npc enable <id> <true/false>").color(NamedTextColor.YELLOW)
            .append(Component.text(" - 启用/禁用 NPC").color(NamedTextColor.GRAY)));
        sender.sendMessage(Component.text("/npc list").color(NamedTextColor.YELLOW)
            .append(Component.text(" - NPC 列表").color(NamedTextColor.GRAY)));
        sender.sendMessage(Component.text("/npc reload").color(NamedTextColor.YELLOW)
            .append(Component.text(" - 重载配置").color(NamedTextColor.GRAY)));
        sender.sendMessage(Component.text("/npc dialogue bind <npcId> <dialogueId>").color(NamedTextColor.YELLOW)
            .append(Component.text(" - 绑定对话到NPC").color(NamedTextColor.GRAY)));
        sender.sendMessage(Component.text("/npc dialogue unbind <npcId>").color(NamedTextColor.YELLOW)
            .append(Component.text(" - 解除NPC对话绑定").color(NamedTextColor.GRAY)));
        sender.sendMessage(Component.text("/npc dialogue list").color(NamedTextColor.YELLOW)
            .append(Component.text(" - 列出所有对话").color(NamedTextColor.GRAY)));
        sender.sendMessage(Component.text("/npc dialogue test <dialogueId>").color(NamedTextColor.YELLOW)
            .append(Component.text(" - 测试对话").color(NamedTextColor.GRAY)));
        sender.sendMessage(Component.text("/npcmenu <menuId>").color(NamedTextColor.YELLOW)
            .append(Component.text(" - 直接打开菜单").color(NamedTextColor.GRAY)));
        sender.sendMessage(Component.text("/npcmenu npc <npcId>").color(NamedTextColor.YELLOW)
            .append(Component.text(" - 打开 NPC 的菜单").color(NamedTextColor.GRAY)));
        sender.sendMessage(Component.text("==============================").color(NamedTextColor.GOLD));
    }

    private Component color(String text) {
        return LegacyComponentSerializer.legacyAmpersand().deserialize(text == null ? "" : text);
    }

    private String legacy(String text) {
        return text == null ? "" : net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer.legacyAmpersand().serialize(net.kyori.adventure.text.Component.text(text));
    }
}
