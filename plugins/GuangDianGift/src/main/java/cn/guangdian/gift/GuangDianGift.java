package cn.guangdian.gift;

import cn.guangdian.gift.adapter.GiftServiceAdapter;
import cn.guangdian.rpgcore.RPGCore;
import cn.guangdian.rpgcore.api.GameLogger;
import cn.guangdian.rpgcore.api.ServiceRegistry;
import cn.guangdian.rpgcore.message.MiniMessageService;
import cn.guangdian.rpgcore.plugin.AbstractRPGPlugin;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.*;

/**
 * 光点礼包插件 - GuangDianGift
 *
 * <p>RPGCore 服务集成:
 * <ul>
 *   <li>GameLogger: 使用 RPGCore 统一日志服务</li>
 *   <li>ServiceRegistry: 使用 RPGCore 服务注册表</li>
 *   <li>SyncScheduler: 使用 RPGCore 同步任务调度器</li>
 * </ul>
 *
 * <p>优先级模式: 优先使用 RPGCore 服务，不可用则降级到本地实现
 *
 * @author Gumin
 * @QQ 2271257344
 * @version 1.0.0
 */
public class GuangDianGift extends AbstractRPGPlugin {

    private Map<String, List<String>> giftItems = new HashMap<>();
    private GiftServiceAdapter giftServiceAdapter;

    // RPGCore 服务
    private GameLogger gameLogger;
    private MiniMessageService miniMessage;

    @Override
    protected void onPluginEnable() {
        // 初始化 RPGCore 服务
        initRPGCoreServices();

        saveDefaultConfig();
        loadGifts();
        registerRPGCoreService();
        logInfo("GuangDianGift 礼包插件已启用！");
    }

    /**
     * 初始化 RPGCore 核心服务
     * 优先使用 RPGCore 统一服务，本地实现作为降级方案
     */
    private void initRPGCoreServices() {
        RPGCore rpgCore = RPGCore.getInstance();
        if (rpgCore != null) {
            gameLogger = rpgCore.getGameLogger();
            miniMessage = rpgCore.getMiniMessageService();
            if (gameLogger != null) {
                logInfo("已连接到 RPGCore GameLogger");
            }
            if (miniMessage != null) {
                logInfo("已连接到 RPGCore MiniMessageService");
            }
        }
        // 降级方案
        if (gameLogger == null) {
            logInfo("使用 Bukkit Logger（降级）");
        }
        if (miniMessage == null) {
            miniMessage = MiniMessageService.getInstance();
            logInfo("使用本地 MiniMessageService（降级）");
        }
    }

    /**
     * 日志辅助方法 - 优先使用 RPGCore GameLogger
     */
    public void logInfo(String message) {
        if (gameLogger != null) {
            gameLogger.info(message);
        } else {
            getLogger().info(message);
        }
    }

    public void logWarning(String message) {
        if (gameLogger != null) {
            gameLogger.warning(message);
        } else {
            getLogger().warning(message);
        }
    }

    public void logSevere(String message) {
        if (gameLogger != null) {
            gameLogger.severe(message);
        } else {
            getLogger().severe(message);
        }
    }

    public void logDebug(String message) {
        if (gameLogger != null) {
            gameLogger.debug(message);
        } else {
            getLogger().info("[DEBUG] " + message);
        }
    }

    @Override
    protected void onPluginDisable() {
        unregisterRPGCoreService();
        // 取消所有任务
        if (scheduler != null) {
            scheduler.cancelAllTasks();
        }
    }
    
    @Override
    protected String getPluginName() {
        return "GuangDianGift";
    }
    
    private void registerRPGCoreService() {
        RPGCore rpgCore = RPGCore.getInstance();
        if (rpgCore != null) {
            ServiceRegistry serviceRegistry = rpgCore.getServiceRegistry();
            if (serviceRegistry != null) {
                giftServiceAdapter = new GiftServiceAdapter(this, serviceRegistry);
                giftServiceAdapter.register();
            }
        }
    }
    
    private void unregisterRPGCoreService() {
        if (giftServiceAdapter != null) {
            giftServiceAdapter.unregister();
        }
    }
    
    public Map<String, List<String>> getGiftItems() {
        return giftItems;
    }

    private void loadGifts() {
        giftItems.clear();
        File giftsFile = new File(getDataFolder(), "gifts.yml");
        if (!giftsFile.exists()) {
            saveResource("gifts.yml", false);
        }

        try {
            String content = Files.readString(giftsFile.toPath());
            org.yaml.snakeyaml.Yaml yaml = new org.yaml.snakeyaml.Yaml();
            Map<String, Object> data = yaml.load(content);

            if (data != null && data.containsKey("gifts")) {
                Map<String, Object> giftsData = (Map<String, Object>) data.get("gifts");
                for (Map.Entry<String, Object> entry : giftsData.entrySet()) {
                    String giftName = entry.getKey();
                    List<String> items = new ArrayList<>();

                    if (entry.getValue() instanceof List) {
                        List<?> itemsList = (List<?>) entry.getValue();
                        for (Object item : itemsList) {
                            if (item instanceof String itemStr) {
                                items.add(itemStr);
                            }
                        }
                    }
                    giftItems.put(giftName, items);
                }
            }
            logInfo("已加载 " + giftItems.size() + " 个礼包");
        } catch (IOException e) {
            logSevere("加载礼包失败: " + e.getMessage());
        }
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        if (args.length < 1) {
            sender.sendMessage(miniMessage.yellow("用法: /gift <礼包名称> [玩家]"));
            sender.sendMessage(miniMessage.gray("可用礼包: " + String.join(", ", giftItems.keySet())));
            return true;
        }

        String giftName = args[0];
        Player target;

        if (args.length > 1) {
            target = Bukkit.getPlayer(args[1]);
            if (target == null) {
                sender.sendMessage(miniMessage.red("玩家不存在: " + args[1]));
                return true;
            }
        } else if (sender instanceof Player) {
            target = (Player) sender;
        } else {
            sender.sendMessage(miniMessage.red("控制台需要指定玩家"));
            return true;
        }

        List<String> items = giftItems.get(giftName);
        if (items == null) {
            sender.sendMessage(miniMessage.red("礼包不存在: " + giftName));
            sender.sendMessage(miniMessage.gray("可用礼包: " + String.join(", ", giftItems.keySet())));
            return true;
        }

        // 使用调度延迟执行，确保命令正确执行
        final Player finalTarget = target;
        final String finalGiftName = giftName;
        final CommandSender finalSender = sender;
        
        if (scheduler != null) {
            scheduler.runSyncLater(() -> {
                for (String item : items) {
                    String cmd = "mm items give " + finalTarget.getName() + " " + item;
                    Bukkit.dispatchCommand(Bukkit.getConsoleSender(), cmd);
                }
                finalTarget.sendMessage(miniMessage.green("你获得了礼包: ").append(miniMessage.yellow(finalGiftName)));
                if (finalSender != finalTarget) {
                    finalSender.sendMessage(miniMessage.green("已给予 ").append(miniMessage.yellow(finalTarget.getName()))
                        .append(miniMessage.green(" 礼包: ")).append(miniMessage.yellow(finalGiftName)));
                }
            }, 1L);
        } else {
            for (String item : items) {
                String cmd = "mm items give " + finalTarget.getName() + " " + item;
                Bukkit.dispatchCommand(Bukkit.getConsoleSender(), cmd);
            }
            finalTarget.sendMessage(miniMessage.green("你获得了礼包: ").append(miniMessage.yellow(finalGiftName)));
            if (sender != finalTarget) {
                sender.sendMessage(miniMessage.green("已给予 ").append(miniMessage.yellow(finalTarget.getName()))
                    .append(miniMessage.green(" 礼包: ")).append(miniMessage.yellow(finalGiftName)));
            }
        }

        return true;
    }
}
