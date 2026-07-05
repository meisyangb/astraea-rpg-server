package cn.guangdian.customgui.resourcepack;

import cn.guangdian.customgui.GuangDianCustomGUI;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerResourcePackStatusEvent;
import org.bukkit.event.player.PlayerResourcePackStatusEvent.Status;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;

public class ResourcePackListener implements Listener {

    private final GuangDianCustomGUI plugin;

    public ResourcePackListener(GuangDianCustomGUI plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onResourcePackStatus(PlayerResourcePackStatusEvent event) {
        Player player = event.getPlayer();
        Status status = event.getStatus();

        switch (status) {
            case SUCCESSFULLY_LOADED:
                plugin.getLogger().info("玩家 " + player.getName() + " 已成功加载资源包");
                player.sendMessage(Component.text("[资源包] 自定义材质加载完成!", NamedTextColor.GREEN));
                break;
            case DECLINED:
                plugin.getLogger().warning("玩家 " + player.getName() + " 拒绝了资源包");
                boolean kickOnDecline = plugin.getConfig().getBoolean("resource-pack.kick-if-declined", false);
                if (kickOnDecline) {
                    player.kick(Component.text("您需要接受服务器资源包才能游玩!", NamedTextColor.RED));
                } else {
                    player.sendMessage(Component.text("[资源包] 警告: 拒绝资源包可能导致部分功能异常!", NamedTextColor.YELLOW));
                }
                break;
            case FAILED_DOWNLOAD:
                plugin.getLogger().warning("玩家 " + player.getName() + " 资源包下载失败");
                player.sendMessage(Component.text("[资源包] 下载失败，请重新加入服务器", NamedTextColor.RED));
                break;
            case ACCEPTED:
                plugin.getLogger().info("玩家 " + player.getName() + " 已接受资源包");
                break;
            case DOWNLOADED:
                plugin.getLogger().info("玩家 " + player.getName() + " 已下载资源包");
                break;
            default:
                break;
        }
    }
}
