package cn.guangdian.villagertrade.adapter;

import cn.guangdian.villagertrade.GuangDianVillagerTrade;

public class VillagerTradeServiceAdapter implements VillagerTradeService {

    private final GuangDianVillagerTrade plugin;

    public VillagerTradeServiceAdapter(GuangDianVillagerTrade plugin) {
        this.plugin = plugin;
    }

    private void registerService() {
    }

    public void unregister() {
    }

    @Override
    public boolean openTrade(String playerName, String recipeName) {
        org.bukkit.entity.Player player = org.bukkit.Bukkit.getPlayerExact(playerName);
        if (player == null) {
            return false;
        }
        return plugin.openTradeGUI(player, recipeName);
    }

    @Override
    public boolean hasRecipe(String recipeName) {
        return plugin.getRecipeManager().hasRecipe(recipeName);
    }

    @Override
    public java.util.List<String> getRecipeNames() {
        return new java.util.ArrayList<>(plugin.getRecipeManager().getRecipeNames());
    }

    @Override
    public void reloadRecipes() {
        plugin.getRecipeManager().reload();
    }
}