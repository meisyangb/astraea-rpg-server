package cn.guangdian.mobs.manager;

import cn.guangdian.mobs.GuangDianMobs;
import cn.guangdian.mobs.model.CustomMob;
import cn.guangdian.rpgcore.RPGCore;
import net.kyori.adventure.bossbar.BossBar;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Boss血条管理器
 */
public class BossBarManager {

    private final GuangDianMobs plugin;
    private final Map<UUID, BossBar> bossBars = new ConcurrentHashMap<>();
    private long updateTaskId = -1;

    public BossBarManager(GuangDianMobs plugin) {
        this.plugin = plugin;
        startUpdateTask();
    }

    /**
     * 创建Boss血条
     */
    public void createBossBar(LivingEntity entity, CustomMob template) {
        if (!template.getOptions().isShowBossBar()) return;

        String name = template.getDisplayName() != null ? template.getDisplayName() : entity.getName();
        BossBar.Color color = parseColor(template.getOptions().getBossBarColor());
        BossBar.Overlay overlay = parseOverlay(template.getOptions().getBossBarStyle());

        BossBar bossBar = BossBar.bossBar(
            MiniMessage.miniMessage().deserialize(name),
            1.0f,
            color,
            overlay
        );

        bossBars.put(entity.getUniqueId(), bossBar);
    }

    /**
     * 更新血条进度
     */
    public void updateBossBar(LivingEntity entity) {
        BossBar bossBar = bossBars.get(entity.getUniqueId());
        if (bossBar == null) return;

        float progress = (float) (entity.getHealth() / entity.getMaxHealth());
        bossBar.progress(Math.max(0, Math.min(1, progress)));
    }

    /**
     * 显示给玩家
     */
    public void showToPlayer(LivingEntity entity, Player player) {
        BossBar bossBar = bossBars.get(entity.getUniqueId());
        if (bossBar != null) {
            player.showBossBar(bossBar);
        }
    }

    /**
     * 隐藏给玩家
     */
    public void hideFromPlayer(LivingEntity entity, Player player) {
        BossBar bossBar = bossBars.get(entity.getUniqueId());
        if (bossBar != null) {
            player.hideBossBar(bossBar);
        }
    }

    /**
     * 移除血条
     */
    public void removeBossBar(LivingEntity entity) {
        BossBar bossBar = bossBars.remove(entity.getUniqueId());
        if (bossBar != null) {
            // 隐藏给所有玩家
            bossBar.viewers().forEach(viewer -> {
                if (viewer instanceof Player player) {
                    player.hideBossBar(bossBar);
                }
            });
        }
    }

    /**
     * 启动更新任务
     */
    private void startUpdateTask() {
        RPGCore rpgCore = RPGCore.getInstance();
        if (rpgCore == null) return;

        updateTaskId = rpgCore.getScheduler().runSyncRepeating(() -> {
            bossBars.forEach((uuid, bossBar) -> {
                org.bukkit.entity.Entity entity = plugin.getServer().getEntity(uuid);
                if (entity instanceof LivingEntity living) {
                    updateBossBar(living);
                }
            });
        }, 20L, 20L); // 每秒更新一次
    }

    /**
     * 清理所有血条
     */
    public void cleanup() {
        if (updateTaskId != -1) {
            RPGCore rpgCore = RPGCore.getInstance();
            if (rpgCore != null) {
                rpgCore.getScheduler().cancelTask(updateTaskId);
            }
        }

        bossBars.values().forEach(bossBar -> {
            bossBar.viewers().forEach(viewer -> {
                if (viewer instanceof Player player) {
                    player.hideBossBar(bossBar);
                }
            });
        });
        bossBars.clear();
    }

    /**
     * 解析颜色
     */
    private BossBar.Color parseColor(String color) {
        try {
            return BossBar.Color.valueOf(color.toUpperCase());
        } catch (IllegalArgumentException e) {
            return BossBar.Color.RED;
        }
    }

    /**
     * 解析样式
     */
    private BossBar.Overlay parseOverlay(String overlay) {
        try {
            return BossBar.Overlay.valueOf(overlay.toUpperCase());
        } catch (IllegalArgumentException e) {
            return BossBar.Overlay.PROGRESS;
        }
    }
}
