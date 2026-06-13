package cn.guangdian.dungeon.ui;

import cn.guangdian.dungeon.GuangDianDungeon;
import cn.guangdian.dungeon.model.*;
import cn.guangdian.dungeon.model.session.DungeonSession;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class PartyUI extends AbstractDungeonUI {

    private final DungeonParty party;
    private long updateTaskId = -1;

    public PartyUI(GuangDianDungeon plugin, Player player) {
        super(plugin, player, 54, "<dark_gray>队伍信息");
        this.party = plugin.getPartyManager().getPlayerParty(player).orElse(null);
        refresh();
        startAutoRefresh();
    }

    @Override
    protected void refresh() {
        inventory.clear();

        if (party == null) {
            inventory.setItem(13, createItem(Material.BARRIER, "<red>你不在任何队伍中",
                "<gray>使用 /party create <副本> 创建队伍",
                "<gray>或使用 /party accept 接受邀请"));
            inventory.setItem(49, createItem(Material.COMPASS, "<yellow>浏览副本", "<gray>点击浏览可用副本"));
            inventory.setItem(53, createCloseItem());
            fillAllEmpty();
            return;
        }

        // 第一行 - 队伍信息
        inventory.setItem(4, createPartyInfoItem());

        // 第二行 - 分隔
        fillRow(1);

        // 第三行 - 成员列表（动态居中）
        List<PartyMember> members = party.getMembers();
        int totalMembers = members.size();
        int[] allMemberSlots = {10, 11, 12, 13, 14, 15, 16}; // 7格居中排列
        int startOffset = Math.max(0, (7 - totalMembers) / 2);

        for (int i = 0; i < totalMembers && (startOffset + i) < allMemberSlots.length; i++) {
            int slot = allMemberSlots[startOffset + i];
            inventory.setItem(slot, createMemberItem(members.get(i)));
        }

        // 第四行 - 分隔
        fillRow(3);

        // 第五行 - 操作按钮（P0修复：改为真实交互）
        if (party.isLeader(player)) {
            inventory.setItem(38, createItem(Material.PLAYER_HEAD, "<green>邀请玩家",
                "<gray>点击输入玩家名称邀请"));
            inventory.setItem(40, createItem(Material.GOLD_BLOCK, "<gold>转让队长",
                "<gray>选择一名成员转让队长"));
            inventory.setItem(42, createItem(Material.REDSTONE, "<red>踢出成员",
                "<gray>选择一名成员踢出队伍"));
        }

        // 第六行 - 底部操作
        inventory.setItem(45, createItem(Material.COMPASS, "<yellow>浏览副本", "<gray>返回副本列表"));

        if (party.isLeader(player)) {
            inventory.setItem(48, createItem(Material.LIME_DYE, "<green><bold>开始副本",
                "<gray>所有成员准备后选择副本进入"));
            inventory.setItem(50, createItem(Material.BARRIER, "<red>解散队伍", "<gray>点击解散当前队伍"));
        } else {
            // 准备按钮
            PartyMember selfMember = party.getMember(player.getUniqueId());
            boolean isReady = selfMember != null && selfMember.isReady();
            inventory.setItem(49, createItem(
                isReady ? Material.LIME_DYE : Material.GRAY_DYE,
                isReady ? "<green><bold>已准备" : "<yellow><bold>点击准备",
                "<gray>点击切换准备状态"
            ));
            inventory.setItem(51, createItem(Material.OAK_DOOR, "<yellow>离开队伍", "<gray>点击离开当前队伍"));
        }

        inventory.setItem(53, createCloseItem());

        fillAllEmpty();
    }

    private ItemStack createPartyInfoItem() {
        ItemStack item = new ItemStack(Material.GOLDEN_HELMET);
        org.bukkit.inventory.meta.ItemMeta meta = item.getItemMeta();

        meta.displayName(plugin.color("<gold><bold>队伍信息"));

        List<Component> lore = new ArrayList<>();
        lore.add(plugin.color("<gray>队长: <white>" + party.getLeader().getName()));
        lore.add(plugin.color("<gray>人数: <white>" + party.getMemberCount() + "/" + party.getMaxMembers()));
        lore.add(plugin.color("<gray>状态: <white>" + party.getState().getDisplayName()));

        int readyCount = (int) party.getMembers().stream().filter(PartyMember::isReady).count();
        lore.add(plugin.color("<gray>准备: <green>" + readyCount + "<white>/" + party.getMemberCount()));

        // 修复：使用 activeSessionId 而非 getCurrentInstance
        if (party.isInDungeon()) {
            DungeonSession session = plugin.getSessionManager().getSession(party.getActiveSessionId());
            if (session != null) {
                lore.add(Component.empty());
                lore.add(plugin.color("<gray>当前副本: <gold>" + session.getDungeonId()));
                lore.add(plugin.color("<gray>已用时间: <white>" + (session.getElapsedTime() / 1000) + "秒"));
            }
        }

        meta.lore(lore);
        item.setItemMeta(meta);
        return item;
    }

    private ItemStack createMemberItem(PartyMember member) {
        Material material;
        if (member.isLeader()) {
            material = Material.GOLD_BLOCK;
        } else if (member.isReady()) {
            material = Material.EMERALD_BLOCK;
        } else {
            material = Material.REDSTONE_BLOCK;
        }

        Player memberPlayer = member.getPlayer();
        String name = memberPlayer != null ? memberPlayer.getName() : member.getName();
        boolean online = memberPlayer != null && memberPlayer.isOnline();

        List<String> loreLines = new ArrayList<>();
        if (member.isLeader()) {
            loreLines.add("<gold>[队长]");
        }
        if (member.isReady()) {
            loreLines.add("<green>已准备");
        } else if (!member.isLeader()) {
            loreLines.add("<red>未准备");
        }
        if (!online) {
            loreLines.add("<red>(离线)");
        }

        return createItem(material, "<white>" + name, loreLines.toArray(new String[0]));
    }

    private void startAutoRefresh() {
        updateTaskId = plugin.getScheduler().runSyncRepeating(() -> {
            if (player.isOnline() && player.getOpenInventory().getTopInventory().getHolder() == this) {
                refresh();
            } else {
                stopAutoRefresh();
            }
        }, 20L, 20L);
    }

    private void stopAutoRefresh() {
        if (updateTaskId != -1) {
            plugin.getScheduler().cancelTask(updateTaskId);
            updateTaskId = -1;
        }
    }

    @Override
    protected void handleClick(int slot) {
        playClickSound();

        if (party == null) {
            if (slot == 49) {
                close();
                new DungeonListUI(plugin, player).open();
            }
            return;
        }

        // P0修复: 邀请 - 打开铁砧输入框
        if (slot == 38 && party.isLeader(player)) {
            close();
            new AnvilInputUI(plugin, player, "输入玩家名称", name -> {
                Bukkit.getScheduler().runTask(plugin, () -> {
                    Player target = Bukkit.getPlayer(name);
                    if (target == null) {
                        player.sendMessage(plugin.color("<red>玩家不存在或不在线: " + name));
                        new PartyUI(plugin, player).open();
                        return;
                    }
                    plugin.getPartyManager().invitePlayer(player, target);
                    new PartyUI(plugin, player).open();
                });
            }).open();
            return;
        }

        // P0修复: 转让 - 打开成员选择界面
        if (slot == 40 && party.isLeader(player)) {
            close();
            new MemberSelectUI(plugin, player, party, "transfer", targetId -> {
                Bukkit.getScheduler().runTask(plugin, () -> {
                    Player target = Bukkit.getPlayer(targetId);
                    if (target != null) {
                        plugin.getPartyManager().transferLeader(player, target);
                    }
                    new PartyUI(plugin, player).open();
                });
            }).open();
            return;
        }

        // P0修复: 踢出 - 打开成员选择界面
        if (slot == 42 && party.isLeader(player)) {
            close();
            new MemberSelectUI(plugin, player, party, "kick", targetId -> {
                Bukkit.getScheduler().runTask(plugin, () -> {
                    Player target = Bukkit.getPlayer(targetId);
                    if (target != null) {
                        plugin.getPartyManager().kickPlayer(player, target);
                    }
                    new PartyUI(plugin, player).open();
                });
            }).open();
            return;
        }

        // 浏览副本
        if (slot == 45) {
            close();
            new DungeonListUI(plugin, player).open();
            return;
        }

        // 队长操作
        if (party.isLeader(player)) {
            // 开始副本
            if (slot == 48) {
                if (!party.isReady()) {
                    player.sendMessage(plugin.color("<red>还有成员未准备！"));
                    return;
                }
                close();
                new DungeonListUI(plugin, player).open();
                return;
            }

            // 解散队伍
            if (slot == 50) {
                close();
                plugin.getPartyManager().disbandParty(party);
                return;
            }
        } else {
            // 准备/取消准备
            if (slot == 49) {
                PartyMember member = party.getMember(player.getUniqueId());
                if (member != null) {
                    boolean newReady = !member.isReady();
                    party.setReady(player, newReady);
                    refresh();
                    player.sendMessage(plugin.color(newReady ?
                        "<green>你已准备就绪" : "<yellow>你取消了准备"));
                }
                return;
            }

            // 离开队伍
            if (slot == 51) {
                close();
                plugin.getPartyManager().leaveParty(player);
                return;
            }
        }
    }

    @Override
    public void close() {
        stopAutoRefresh();
        super.close();
    }
}
