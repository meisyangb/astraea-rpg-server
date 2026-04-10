package cn.guangdian.decompose.adapter;

import cn.guangdian.decompose.GuangDianDecompose;
import cn.guangdian.decompose.manager.DecomposeManager;
import cn.guangdian.decompose.model.DecomposeRule;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

public class DecomposeServiceAdapter {

    private final GuangDianDecompose plugin;

    public DecomposeServiceAdapter(GuangDianDecompose plugin) {
        this.plugin = plugin;
    }

    public boolean canDecompose(ItemStack item) {
        if (item == null) return false;
        String mythicId = plugin.getMythicMobsHook().getMythicItemId(item);
        if (mythicId == null) return false;
        return plugin.getRuleManager().hasRule(mythicId);
    }

    public DecomposeRule getDecomposeRule(ItemStack item) {
        if (item == null) return null;
        String mythicId = plugin.getMythicMobsHook().getMythicItemId(item);
        if (mythicId == null) return null;
        return plugin.getRuleManager().getRule(mythicId);
    }

    public DecomposeManager.DecomposeResult decompose(Player player, ItemStack item) {
        return plugin.getDecomposeManager().decompose(player, item);
    }

    public void openDecomposeGUI(Player player) {
        plugin.getDecomposeGUI().open(player);
    }

    public void reloadConfig() {
        plugin.reloadAllConfig();
    }
}
