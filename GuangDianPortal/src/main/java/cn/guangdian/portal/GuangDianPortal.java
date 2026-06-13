package cn.guangdian.portal;

import cn.guangdian.portal.command.PortalCommand;
import cn.guangdian.portal.listener.PortalListener;
import cn.guangdian.portal.manager.PortalManager;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.plugin.java.JavaPlugin;

public class GuangDianPortal extends JavaPlugin {

    private static GuangDianPortal instance;
    private PortalManager portalManager;
    private MiniMessage miniMessage;

    private int teleportDelay;
    private boolean teleportEffectEnabled;
    private boolean teleportSoundEnabled;

    @Override
    public void onEnable() {
        instance = this;
        miniMessage = MiniMessage.miniMessage();

        saveDefaultConfig();
        loadSettings();

        portalManager = new PortalManager(this);
        portalManager.loadPortals();

        registerListeners();
        registerCommands();

        getLogger().info("光点传送门插件已启用! 版本: " + getDescription().getVersion());
    }

    @Override
    public void onDisable() {
        if (portalManager != null) {
            portalManager.savePortals();
        }

        getLogger().info("光点传送门插件已禁用!");
    }

    private void loadSettings() {
        teleportDelay = getConfig().getInt("settings.teleport-delay", 0);
        teleportEffectEnabled = getConfig().getBoolean("settings.effects.enabled", true);
        teleportSoundEnabled = getConfig().getBoolean("settings.sound.enabled", true);
    }

    private void registerListeners() {
        getServer().getPluginManager().registerEvents(new PortalListener(this), this);
    }

    private void registerCommands() {
        var portalCmd = getCommand("portal");
        if (portalCmd != null) {
            var command = new PortalCommand(this);
            portalCmd.setExecutor(command);
            portalCmd.setTabCompleter(command);
        }
    }

    public static GuangDianPortal getInstance() {
        return instance;
    }

    public PortalManager getPortalManager() {
        return portalManager;
    }

    public int getTeleportDelay() {
        return teleportDelay;
    }

    public boolean isTeleportEffectEnabled() {
        return teleportEffectEnabled;
    }

    public boolean isTeleportSoundEnabled() {
        return teleportSoundEnabled;
    }

    public String colorize(String text) {
        return miniMessage.deserialize(text).toString();
    }

    public Component colorizeToComponent(String text) {
        return miniMessage.deserialize(text);
    }

    public void sendMessage(org.bukkit.command.CommandSender sender, String text) {
        sender.sendMessage(miniMessage.deserialize(text));
    }
}
