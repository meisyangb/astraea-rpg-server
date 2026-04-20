package cn.guangdian.rpgitems.listener;

import cn.guangdian.rpgitems.RPGItems;
import cn.guangdian.rpgitems.integration.RPGSkillIntegration;
import cn.guangdian.rpgitems.template.ItemTemplate;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;

import java.util.Optional;

/**
 * 物品技能监听器
 * <p>负责监听玩家交互事件，触发物品绑定的技能</p>
 */
public class ItemSkillListener implements Listener {

    private final RPGItems plugin;
    private final RPGSkillIntegration skillIntegration;

    public ItemSkillListener(RPGItems plugin, RPGSkillIntegration skillIntegration) {
        this.plugin = plugin;
        this.skillIntegration = skillIntegration;
    }

    /**
     * 处理玩家交互事件
     * <p>根据点击类型触发对应的物品技能</p>
     */
    @EventHandler
    public void onPlayerInteract(PlayerInteractEvent event) {
        if (!skillIntegration.isEnabled()) return;

        Player player = event.getPlayer();
        ItemStack item = event.getItem();
        if (item == null) return;

        // 检查是否是 RPG 物品
        Optional<String> itemIdOpt = plugin.getItemService().getItemId(item);
        if (itemIdOpt.isEmpty()) return;

        // 获取物品模板
        Optional<ItemTemplate> templateOpt =
                plugin.getItemRegistry().getItem(itemIdOpt.get());
        if (templateOpt.isEmpty()) return;

        ItemTemplate template = templateOpt.get();

        // 处理右键点击
        if (event.getAction() == Action.RIGHT_CLICK_AIR ||
            event.getAction() == Action.RIGHT_CLICK_BLOCK) {

            for (ItemTemplate.SkillBinding binding : template.getSkillBindings()) {
                if ("RIGHT_CLICK".equalsIgnoreCase(binding.getTrigger())) {
                    // 取消事件，防止触发其他行为
                    event.setCancelled(true);
                    skillIntegration.executeSkill(player, binding.getSkillId());
                }
            }
        }

        // 处理左键点击
        if (event.getAction() == Action.LEFT_CLICK_AIR ||
            event.getAction() == Action.LEFT_CLICK_BLOCK) {

            for (ItemTemplate.SkillBinding binding : template.getSkillBindings()) {
                if ("LEFT_CLICK".equalsIgnoreCase(binding.getTrigger())) {
                    // 取消事件，防止触发攻击动画
                    event.setCancelled(true);
                    skillIntegration.executeSkill(player, binding.getSkillId());
                }
            }
        }
    }
}
