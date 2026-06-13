package cn.guangdian.dungeon.ui;

import cn.guangdian.dungeon.GuangDianDungeon;
import cn.guangdian.dungeon.model.DungeonParty;
import cn.guangdian.dungeon.model.PartyMember;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;

import java.util.*;
import java.util.function.Consumer;

/**
 * 成员选择界面 - 用于踢出/转让队长
 * 点击成员头像执行回调
 */
public class MemberSelectUI extends AbstractDungeonUI {

    private final DungeonParty party;
    private final String actionType; // "kick" 或 "transfer"
    private final Map<Integer, UUID> slotToPlayer;
    private final Consumer<UUID> callback;

    public MemberSelectUI(GuangDianDungeon plugin, Player player, DungeonParty party,
                          String actionType, Consumer<UUID> callback) {
        super(plugin, player, 27, "<dark_gray>" + (actionType.equals("kick") ? "踢出成员" : "转让队长"));
        this.party = party;
        this.actionType = actionType;
        this.slotToPlayer = new HashMap<>();
        this.callback = callback;
        refresh();
    }

    @Override
    protected void refresh() {
        inventory.clear();
        slotToPlayer.clear();

        // 成员列表 (slot 10-16, 居中7格)
        int[] slots = {10, 11, 12, 13, 14, 15, 16};
        int i = 0;

        for (PartyMember member : party.getMembers()) {
            if (i >= slots.length) break;

            // 不能踢出/转让给自己
            if (member.getPlayerId().equals(player.getUniqueId())) {
                i++;
                continue;
            }

            Player memberPlayer = Bukkit.getPlayer(member.getPlayerId());
            String name = memberPlayer != null ? memberPlayer.getName() : member.getName();
            Material icon = memberPlayer != null && memberPlayer.isOnline() ?
                Material.PLAYER_HEAD : Material.SKELETON_SKULL;

            List<String> loreLines = new ArrayList<>();
            loreLines.add(member.isLeader() ? "<gold>队长" : "<white>成员");
            loreLines.add(memberPlayer != null && memberPlayer.isOnline() ?
                "<green>在线" : "<red>离线");
            loreLines.add("");
            loreLines.add(actionType.equals("kick") ?
                "<red>点击踢出" : "<yellow>点击转让队长");

            int slot = slots[i];
            inventory.setItem(slot, createItem(icon, "<white>" + name, loreLines.toArray(new String[0])));
            slotToPlayer.put(slot, member.getPlayerId());
            i++;
        }

        // 底部导航
        inventory.setItem(18, createBackItem());
        inventory.setItem(26, createCloseItem());

        fillAllEmpty();
    }

    @Override
    protected void handleClick(int slot) {
        playClickSound();

        if (slot == 18) {
            close();
            return;
        }

        if (slot == 26) {
            close();
            return;
        }

        UUID targetId = slotToPlayer.get(slot);
        if (targetId != null) {
            close();
            callback.accept(targetId);
        }
    }
}
