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
        String rpgId = plugin.getRPGItemsHook().getRPGItemId(item);
        if (rpgId == null) return false;
        return plugin.getRuleManager().hasRule(rpgId);
    }

    public DecomposeRule getDecomposeRule(ItemStack item) {
        if (item == null) return null;
        String rpgId = plugin.getRPGItemsHook().getRPGItemId(item);
        if (rpgId == null) return null;
        return plugin.getRuleManager().getRule(rpgId);
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
