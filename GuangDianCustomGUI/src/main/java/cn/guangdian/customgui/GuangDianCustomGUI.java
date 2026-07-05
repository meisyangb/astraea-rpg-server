package cn.guangdian.customgui;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.plugin.java.JavaPlugin;

import cn.guangdian.customgui.gui.CustomBackpackGUI;
import cn.guangdian.customgui.resourcepack.ResourcePackManager;
import cn.guangdian.customgui.resourcepack.ResourcePackListener;

import net.kyori.adventure.text.minimessage.MiniMessage;

public class GuangDianCustomGUI extends JavaPlugin implements Listener {

    private static GuangDianCustomGUI instance;
    private ResourcePackManager resourcePackManager;
    private CustomBackpackGUI backpackGUI;

    @Override
    public void onEnable() {
        instance = this;
        saveDefaultConfig();

        getLogger().info("=================================");
        getLogger().info("  GuangDianCustomGUI 正在加载...");
        getLogger().info("=================================");

        // 初始化资源包管理器
        resourcePackManager = new ResourcePackManager(this);
        resourcePackManager.load();

        // 初始化自定义背包GUI
        backpackGUI = new CustomBackpackGUI(this);

        // 注册事件
        getServer().getPluginManager().registerEvents(this, this);
        getServer().getPluginManager().registerEvents(new ResourcePackListener(this), this);

        // 注册命令
        getCommand("customgui").setExecutor(new CustomGUICommand(this));

        getLogger().info("=================================");
        getLogger().info("  GuangDianCustomGUI 加载完成!");
        getLogger().info("  版本: " + getDescription().getVersion());
        getLogger().info("=================================");
    }

    @Override
    public void onDisable() {
        getLogger().info("GuangDianCustomGUI 已禁用");
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        // 延迟发送资源包，避免与登录流程冲突
        Bukkit.getScheduler().runTaskLater(this, () -> {
            resourcePackManager.sendToPlayer(player);
        }, 50L); // 2.5秒后发送
    }

    public static GuangDianCustomGUI getInstance() {
        return instance;
    }

    public ResourcePackManager getResourcePackManager() {
        return resourcePackManager;
    }

    public CustomBackpackGUI getBackpackGUI() {
        return backpackGUI;
    }

    /**
     * 打开自定义背包界面
     */
    public void openBackpack(Player player) {
        backpackGUI.open(player);
    }

    /**
     * 使用MiniMessage处理文本
     */
    public static String formatMessage(String text) {
        return MiniMessage.miniMessage().serialize(
            MiniMessage.miniMessage().deserialize(text)
        );
    }
}
