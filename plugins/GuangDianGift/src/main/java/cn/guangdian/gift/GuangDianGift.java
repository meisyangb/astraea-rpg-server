package cn.guangdian.gift;

import cn.guangdian.gift.adapter.GiftServiceAdapter;
import cn.guangdian.rpgcore.RPGCore;
import cn.guangdian.rpgcore.api.ServiceRegistry;
import cn.guangdian.rpgcore.plugin.AbstractRPGPlugin;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.*;

public class GuangDianGift extends AbstractRPGPlugin {

    private Map<String, List<String>> giftItems = new HashMap<>();
    private GiftServiceAdapter giftServiceAdapter;

    @Override
    protected void onPluginEnable() {
        saveDefaultConfig();
        loadGifts();
        registerRPGCoreService();
        getLogger().info("GuangDianGift 礼包插件已启用！");
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
            getLogger().info("已加载 " + giftItems.size() + " 个礼包");
        } catch (IOException e) {
            getLogger().severe("加载礼包失败: " + e.getMessage());
        }
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        if (args.length < 1) {
            sender.sendMessage("§e用法: /gift <礼包名称> [玩家]");
            sender.sendMessage("§7可用礼包: " + String.join(", ", giftItems.keySet()));
            return true;
        }

        String giftName = args[0];
        Player target;

        if (args.length > 1) {
            target = Bukkit.getPlayer(args[1]);
            if (target == null) {
                sender.sendMessage("§c玩家不存在: " + args[1]);
                return true;
            }
        } else if (sender instanceof Player) {
            target = (Player) sender;
        } else {
            sender.sendMessage("§c控制台需要指定玩家");
            return true;
        }

        List<String> items = giftItems.get(giftName);
        if (items == null) {
            sender.sendMessage("§c礼包不存在: " + giftName);
            sender.sendMessage("§7可用礼包: " + String.join(", ", giftItems.keySet()));
            return true;
        }

        // 使用调度延迟执行，确保命令正确执行
        final Player finalTarget = target;
        final String finalGiftName = giftName;
        cn.guangdian.rpgcore.RPGCore rpgCore = cn.guangdian.rpgcore.RPGCore.getInstance();
        if (rpgCore != null) {
            rpgCore.getScheduler().runSyncLater(() -> {
                for (String item : items) {
                    String cmd = "mm items give " + finalTarget.getName() + " " + item;
                    Bukkit.dispatchCommand(Bukkit.getConsoleSender(), cmd);
                }
                finalTarget.sendMessage("§a你获得了礼包: §e" + finalGiftName);
                if (sender != finalTarget) {
                    sender.sendMessage("§a已给予 " + finalTarget.getName() + " 礼包: " + finalGiftName);
                }
            }, 1L);
        } else {
            for (String item : items) {
                String cmd = "mm items give " + finalTarget.getName() + " " + item;
                Bukkit.dispatchCommand(Bukkit.getConsoleSender(), cmd);
            }
            finalTarget.sendMessage("§a你获得了礼包: §e" + finalGiftName);
            if (sender != finalTarget) {
                sender.sendMessage("§a已给予 " + finalTarget.getName() + " 礼包: " + finalGiftName);
            }
        }

        return true;
    }
}
