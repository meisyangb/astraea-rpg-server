package cn.guangdian.npccommand;

import net.citizensnpcs.api.CitizensAPI;
import net.citizensnpcs.api.npc.NPC;
import org.bukkit.Bukkit;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerInteractEntityEvent;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class NPCClickListener implements Listener {

    private final GuangDianNPCCommand plugin;
    private final Map<UUID, Long> lastClickTime = new HashMap<>();
    private static final long CLICK_COOLDOWN = 500;

    public NPCClickListener(GuangDianNPCCommand plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onPlayerInteractEntity(PlayerInteractEntityEvent event) {
        Player player = event.getPlayer();
        Entity clicked = event.getRightClicked();

        plugin.getLogger().info("[DEBUG] 玩家 " + player.getName() + " 右键点击实体: " + clicked.getType() + " - " + clicked.getName());

        int npcId = getNPCId(clicked);
        if (npcId < 0) {
            plugin.getLogger().info("[DEBUG] 点击的不是 NPC");
            return;
        }

        UUID playerId = player.getUniqueId();
        long now = System.currentTimeMillis();
        Long lastClick = lastClickTime.get(playerId);
        if (lastClick != null && (now - lastClick) < CLICK_COOLDOWN) {
            plugin.getLogger().info("[DEBUG] 冷却中，跳过");
            return;
        }
        lastClickTime.put(playerId, now);

        event.setCancelled(true);
        plugin.getLogger().info("[DEBUG] 检测到 NPC ID: " + npcId);

        handleNPCInteraction(player, npcId);
    }

    private void handleNPCInteraction(Player player, int npcId) {
        NPCCommandData data = plugin.getNPCCommandService().getNPCCommandData(npcId);

        if (data == null) {
            plugin.getLogger().warning("[DEBUG] NPC " + npcId + " 没有配置命令数据");
            player.sendMessage("<red>此 NPC 没有配置命令 (ID: " + npcId + ")");
            return;
        }

        if (data.getCommands().isEmpty()) {
            plugin.getLogger().warning("[DEBUG] NPC " + npcId + " 命令列表为空");
            player.sendMessage("<red>此 NPC 命令列表为空 (ID: " + npcId + ")");
            return;
        }

        plugin.getLogger().info("[DEBUG] NPC " + npcId + " 有 " + data.getCommands().size() + " 个命令");

        if (plugin.getCooldownManager().isOnCooldown(player.getUniqueId(), npcId)) {
            long remaining = plugin.getCooldownManager().getRemainingCooldown(player.getUniqueId(), npcId);
            String formatted = plugin.getCooldownManager().formatCooldown(remaining);
            String message = plugin.getConfig().getString("messages.on-cooldown", "<yellow>请等待 <red>%time% <yellow>后再使用此NPC!");
            player.sendMessage(plugin.getMiniMessage().parseUnified(message, "time", formatted));
            return;
        }

        for (NPCCommandData.CommandEntry entry : data.getCommands()) {
            plugin.getLogger().info("[DEBUG] 执行命令: " + entry.getType() + " - " + entry.getCommand());
            executeCommand(player, entry);
        }

        if (data.getCooldown() > 0) {
            plugin.getCooldownManager().setCooldown(player.getUniqueId(), npcId, data.getCooldown());
        }
    }

    private int getNPCId(Entity entity) {
        if (!CitizensAPI.hasImplementation()) {
            plugin.getLogger().warning("[DEBUG] CitizensAPI 没有实现");
            return -1;
        }

        NPC npc = CitizensAPI.getNPCRegistry().getNPC(entity);
        if (npc == null) {
            plugin.getLogger().info("[DEBUG] 实体不是 Citizens NPC");
            return -1;
        }

        return npc.getId();
    }

    private void executeCommand(Player player, NPCCommandData.CommandEntry entry) {
        String command = entry.getCommand();
        command = command.replace("%player%", player.getName());
        command = command.replace("%p%", player.getName());

        if (plugin.getExternalServices().isPlaceholderAPIEnabled()) {
            command = plugin.getExternalServices().parsePlaceholders(player, command);
        }

        plugin.getLogger().info("[DEBUG] 实际执行命令: " + entry.getType() + " - " + command);

        switch (entry.getType()) {
            case CONSOLE:
                Bukkit.dispatchCommand(Bukkit.getConsoleSender(), command);
                break;
            case PLAYER:
                player.performCommand(command);
                break;
            case OP:
                boolean wasOp = player.isOp();
                try {
                    player.setOp(true);
                    player.performCommand(command);
                } finally {
                    player.setOp(wasOp);
                }
                break;
            case COMMAND:
                player.performCommand(command);
                break;
            case NO_PERMS:
                player.performCommand(command);
                break;
        }
    }
}
