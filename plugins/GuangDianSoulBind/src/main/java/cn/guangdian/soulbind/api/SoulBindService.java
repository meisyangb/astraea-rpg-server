package cn.guangdian.soulbind.api;

import org.bukkit.inventory.ItemStack;
import org.bukkit.entity.Player;

import java.util.UUID;

public interface SoulBindService {

    boolean isBound(ItemStack item);

    boolean isBoundTo(ItemStack item, UUID playerId);

    UUID getBoundPlayer(ItemStack item);

    String getBoundPlayerName(ItemStack item);

    boolean bindItem(ItemStack item, Player player);

    boolean unbindItem(ItemStack item);

    boolean canInteract(ItemStack item, Player player);

    boolean isMythicMobsItem(ItemStack item);
}
