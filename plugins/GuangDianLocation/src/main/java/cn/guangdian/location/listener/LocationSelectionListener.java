package cn.guangdian.location.listener;

import cn.guangdian.location.GuangDianLocation;
import cn.guangdian.rpgcore.RPGCore;
import cn.guangdian.rpgcore.message.MessageServiceImpl;
import net.kyori.adventure.text.Component;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.ItemStack;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

/**
 * 坐标选择监听器
 * 
 * <p>监听木锄头右键点击事件，处理坐标选择和命名流程。</p>
 * 
 * <h3>工作流程：</h3>
 * <ol>
 *   <li>玩家右键木锄头 -> 记录位置，提示输入名称</li>
 *   <li>玩家输入名称 -> 保存到数据库</li>
 * </ol>
 * 
 * @author GuangDian
 * @since 1.0.0
 */
public class LocationSelectionListener implements Listener {

    private final GuangDianLocation plugin;

    // 等待输入名称的玩家
    private final Map<UUID, PendingSelection> pendingSelections = new ConcurrentHashMap<>();

    // 名称输入超时时间（秒）
    private static final int INPUT_TIMEOUT_SECONDS = 30;

    public LocationSelectionListener(GuangDianLocation plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.NORMAL, ignoreCancelled = true)
    public void onPlayerInteract(PlayerInteractEvent event) {
        // 只处理右键点击方块
        if (event.getAction() != Action.RIGHT_CLICK_BLOCK) {
            return;
        }

        Player player = event.getPlayer();
        ItemStack item = player.getInventory().getItemInMainHand();

        // 检查是否是木锄头
        if (item.getType() != Material.WOODEN_HOE) {
            return;
        }

        // 检查权限
        if (!player.hasPermission("guangdian.location.use")) {
            return;
        }

        // 获取点击位置
        Location clickedLocation = event.getClickedBlock().getLocation();
        // 调整位置到方块上方（更适合玩家站立的点）
        Location targetLocation = clickedLocation.add(0.5, 1, 0.5);

        // 显示选中效果
        showSelectionEffect(player, targetLocation);

        // 提示玩家输入名称
        MessageServiceImpl msg = MessageServiceImpl.getInstance();
        player.sendMessage(msg.colorize("<gold>===== 坐标点选择 ====="));
        player.sendMessage(msg.colorize("<yellow>你选中了一个位置:"));
        player.sendMessage(msg.colorize("<gray>世界: <white>" + targetLocation.getWorld().getName()));
        player.sendMessage(msg.colorize("<gray>坐标: <white>X=" + targetLocation.getBlockX() +
            ", Y=" + targetLocation.getBlockY() +
            ", Z=" + targetLocation.getBlockZ()));
        player.sendMessage(msg.colorize("<green>请在聊天栏输入坐标名称（30秒内有效）"));
        player.sendMessage(msg.colorize("<gray>输入 'cancel' 取消本次操作"));

        // 记录待命名位置
        PendingSelection pending = new PendingSelection(
            player.getUniqueId(),
            targetLocation.clone(),
            System.currentTimeMillis() + TimeUnit.SECONDS.toMillis(INPUT_TIMEOUT_SECONDS)
        );
        pendingSelections.put(player.getUniqueId(), pending);

        // 取消事件（防止锄头耕作）
        event.setCancelled(true);
    }

    @EventHandler(priority = EventPriority.NORMAL, ignoreCancelled = true)
    public void onPlayerChat(AsyncPlayerChatEvent event) {
        Player player = event.getPlayer();
        UUID playerId = player.getUniqueId();

        // 检查是否有待命名的坐标
        PendingSelection pending = pendingSelections.get(playerId);
        if (pending == null) {
            return;
        }

        // 检查是否超时
        if (System.currentTimeMillis() > pending.getExpireTime()) {
            pendingSelections.remove(playerId);
            RPGCore rpgCore = RPGCore.getInstance();
            if (rpgCore != null) {
                rpgCore.getScheduler().runSync(() ->
                    player.sendMessage(plugin.getMessageService().colorize("<red>输入超时，坐标选择已取消。")));
            }
            return;
        }

        String input = event.getMessage().trim();

        // 检查是否取消
        if (input.equalsIgnoreCase("cancel") || input.equalsIgnoreCase("取消")) {
            pendingSelections.remove(playerId);
            event.setCancelled(true);
            RPGCore rpgCore = RPGCore.getInstance();
            if (rpgCore != null) {
                rpgCore.getScheduler().runSync(() ->
                    player.sendMessage(plugin.getMessageService().colorize("<yellow>已取消本次坐标选择。")));
            }
            return;
        }

        // 验证名称
        if (!isValidName(input)) {
            event.setCancelled(true);
            RPGCore rpgCore = RPGCore.getInstance();
            if (rpgCore != null) {
                rpgCore.getScheduler().runSync(() ->
                    player.sendMessage(plugin.getMessageService().colorize("<red>坐标名称无效！名称只能包含字母、数字、下划线、中文，长度2-32个字符。")));
            }
            return;
        }

        // 取消聊天消息（防止显示给其他玩家）
        event.setCancelled(true);

        // 移除待处理状态
        pendingSelections.remove(playerId);

        // 保存坐标
        Location location = pending.getLocation();
        String name = input;

        RPGCore rpgCore = RPGCore.getInstance();
        if (rpgCore != null) {
            rpgCore.getScheduler().runSync(() -> 
                plugin.handleLocationSelection(player, location, name));
        }
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onPlayerQuit(PlayerQuitEvent event) {
        // 清理玩家的待处理状态
        pendingSelections.remove(event.getPlayer().getUniqueId());
    }

    /**
     * 显示选中效果
     */
    private void showSelectionEffect(Player player, Location location) {
        World world = location.getWorld();
        if (world == null) return;

        // 粒子效果 - 使用 END_ROD 粒子突出显示选中位置
        try {
            world.spawnParticle(Particle.END_ROD, location, 30, 0.3, 0.3, 0.3, 0.05);
            // 添加一圈粒子
            for (int i = 0; i < 8; i++) {
                double angle = i * Math.PI / 4;
                double x = location.getX() + Math.cos(angle) * 0.5;
                double z = location.getZ() + Math.sin(angle) * 0.5;
                world.spawnParticle(Particle.HAPPY_VILLAGER, 
                    new Location(world, x, location.getY(), z), 3, 0, 0, 0, 0);
            }
        } catch (Exception e) {
            // 粒子效果失败不影响主流程
        }

        // 音效
        try {
            player.playSound(location, Sound.BLOCK_NOTE_BLOCK_PLING, 1.0f, 2.0f);
        } catch (Exception e) {
            // 音效失败不影响主流程
        }
    }

    /**
     * 验证名称是否有效
     */
    private boolean isValidName(String name) {
        if (name == null) return false;
        if (name.length() < 2 || name.length() > 32) return false;
        // 只允许字母、数字、下划线、中文
        return name.matches("[a-zA-Z0-9_\\u4e00-\\u9fa5]+");
    }

    /**
     * 待命名位置
     */
    private static class PendingSelection {
        private final UUID playerId;
        private final Location location;
        private final long expireTime;

        public PendingSelection(UUID playerId, Location location, long expireTime) {
            this.playerId = playerId;
            this.location = location;
            this.expireTime = expireTime;
        }

        public UUID getPlayerId() {
            return playerId;
        }

        public Location getLocation() {
            return location;
        }

        public long getExpireTime() {
            return expireTime;
        }
    }

    /**
     * 获取等待输入的玩家数量（用于调试）
     */
    public int getPendingCount() {
        return pendingSelections.size();
    }

    /**
     * 检查玩家是否有待处理的选择
     */
    public boolean hasPendingSelection(UUID playerId) {
        PendingSelection pending = pendingSelections.get(playerId);
        if (pending == null) return false;
        // 检查是否超时
        if (System.currentTimeMillis() > pending.getExpireTime()) {
            pendingSelections.remove(playerId);
            return false;
        }
        return true;
    }
}