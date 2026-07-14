package cn.guangdian.quest.dialogue;

import cn.guangdian.quest.GuangDianQuest;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;
import org.bukkit.event.player.PlayerQuitEvent;

import java.util.UUID;

/**
 * 对话监听器
 * 
 * 监听玩家聊天，处理对话选项输入
 */
public class DialogueListener implements Listener {
    
    private final GuangDianQuest plugin;
    private final ChatDialogueManager dialogueManager;
    
    public DialogueListener(GuangDianQuest plugin, ChatDialogueManager dialogueManager) {
        this.plugin = plugin;
        this.dialogueManager = dialogueManager;
    }
    
    /**
     * 监听玩家聊天
     * 如果玩家在对话中，尝试处理输入
     */
    @EventHandler(priority = EventPriority.LOWEST)
    public void onPlayerChat(AsyncPlayerChatEvent event) {
        UUID playerId = event.getPlayer().getUniqueId();
        
        // 检查玩家是否在对话中
        if (dialogueManager.isInDialogue(playerId)) {
            // 取消聊天消息显示（避免干扰对话）
            event.setCancelled(true);
            
            // 在主线程处理输入
            String message = event.getMessage();
            plugin.getServer().getScheduler().runTask(plugin, () -> {
                dialogueManager.handlePlayerInput(event.getPlayer(), message);
            });
        }
    }
    
    /**
     * 玩家退出时结束对话
     */
    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        UUID playerId = event.getPlayer().getUniqueId();
        if (dialogueManager.isInDialogue(playerId)) {
            dialogueManager.endDialogue(event.getPlayer());
        }
    }
    
    /**
     * 阻止对话中的玩家执行命令
     */
    @EventHandler(priority = EventPriority.LOWEST)
    public void onPlayerCommand(PlayerCommandPreprocessEvent event) {
        UUID playerId = event.getPlayer().getUniqueId();
        
        // 对话中的玩家只能使用特定命令
        if (dialogueManager.isInDialogue(playerId)) {
            String cmd = event.getMessage().toLowerCase();
            
            // 允许的命令列表
            if (!cmd.startsWith("/q") && !cmd.startsWith("/quest")) {
                event.setCancelled(true);
                event.getPlayer().sendMessage("§c对话中无法执行该命令，请先完成对话");
            }
        }
    }
}