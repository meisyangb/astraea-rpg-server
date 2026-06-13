package cn.guangdian.npc.dialogue;

import cn.guangdian.npc.GuangDianNPC;
import io.papermc.paper.event.player.AsyncChatEvent;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;
import org.bukkit.event.player.PlayerQuitEvent;

/**
 * 对话监听器 - 处理玩家在对话中的输入
 */
public class DialogueListener implements Listener {

    private final GuangDianNPC plugin;
    private final DialogueManager dialogueManager;

    public DialogueListener(GuangDianNPC plugin, DialogueManager dialogueManager) {
        this.plugin = plugin;
        this.dialogueManager = dialogueManager;
    }

    /**
     * 监听玩家聊天 - 用于对话选择
     */
    @EventHandler(priority = EventPriority.HIGHEST)
    public void onPlayerChat(AsyncChatEvent event) {
        Player player = event.getPlayer();

        if (!dialogueManager.isInDialogue(player)) {
            return;
        }

        event.setCancelled(true);

        // 获取玩家输入的消息
        String message = event.message().toString();
        // 去除 Component 的包装，获取纯文本
        message = message.replaceAll("[^0-9]", "");

        if (message.isEmpty()) {
            player.sendMessage(Component.text("请输入数字选项").color(NamedTextColor.RED));
            return;
        }

        int option;
        try {
            option = Integer.parseInt(message);
        } catch (NumberFormatException e) {
            player.sendMessage(Component.text("请输入有效的数字").color(NamedTextColor.RED));
            return;
        }

        // 选项 0 表示结束对话
        if (option == 0) {
            dialogueManager.endDialogue(player);
            player.sendMessage(Component.text("对话已结束").color(NamedTextColor.GRAY));
            return;
        }

        // 处理选项 (转换为 0-based 索引)
        plugin.getServer().getScheduler().runTask(plugin, () -> {
            dialogueManager.handlePlayerInput(player, option - 1);
        });
    }

    /**
     * 监听玩家命令 - 对话中禁止执行命令
     */
    @EventHandler(priority = EventPriority.HIGHEST)
    public void onPlayerCommand(PlayerCommandPreprocessEvent event) {
        Player player = event.getPlayer();
        String message = event.getMessage();

        if (!dialogueManager.isInDialogue(player)) {
            return;
        }

        // 允许退出对话的命令
        if (message.equalsIgnoreCase("/dialogue end") || 
            message.equalsIgnoreCase("/npc dialogue end")) {
            return;
        }

        // 检查是否是数字输入（玩家可能直接输入数字而不是聊天）
        String cmd = message.substring(1).trim();
        try {
            int option = Integer.parseInt(cmd);
            event.setCancelled(true);
            
            if (option == 0) {
                dialogueManager.endDialogue(player);
                player.sendMessage(Component.text("对话已结束").color(NamedTextColor.GRAY));
                return;
            }
            
            dialogueManager.handlePlayerInput(player, option - 1);
            return;
        } catch (NumberFormatException e) {
            // 不是数字，继续处理
        }

        event.setCancelled(true);
        player.sendMessage(Component.text("对话中无法执行命令，请先输入 0 结束对话").color(NamedTextColor.RED));
    }

    /**
     * 玩家退出 - 清理对话状态
     */
    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        Player player = event.getPlayer();
        dialogueManager.endDialogue(player);
    }
}
