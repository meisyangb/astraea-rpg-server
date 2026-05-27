package cn.guangdian.points.placeholder;

import cn.guangdian.points.GuangDianPoints;
import me.clip.placeholderapi.expansion.PlaceholderExpansion;
import org.bukkit.OfflinePlayer;
import org.jetbrains.annotations.NotNull;

import java.util.*;
import java.util.stream.Collectors;

public class PointsPlaceholder extends PlaceholderExpansion {

    private final GuangDianPoints plugin;

    public PointsPlaceholder(GuangDianPoints plugin) {
        this.plugin = plugin;
    }

    @Override
    public @NotNull String getIdentifier() {
        return "gdpoints";
    }

    @Override
    public @NotNull String getAuthor() {
        return "Gumin";
    }

    @Override
    public @NotNull String getVersion() {
        return plugin.getDescription().getVersion();
    }

    @Override
    public boolean persist() {
        return true;
    }

    @Override
    public String onRequest(OfflinePlayer player, @NotNull String params) {
        if (player == null) return "";
        
        UUID uuid = player.getUniqueId();
        long balance = plugin.getBalance(uuid);
        
        switch (params.toLowerCase()) {
            case "balance":
                return String.valueOf(balance);
            case "balance_formatted":
                return formatNumber(balance);
            case "balance_万":
                return String.format("%.1f", balance / 10000.0);
            case "balance_亿":
                return String.format("%.2f", balance / 100000000.0);
            case "rank":
                return String.valueOf(getPlayerRank(uuid));
            case "rank_formatted":
                return formatRank(getPlayerRank(uuid));
            case "top_1":
                return getTopPlayerName(0);
            case "top_2":
                return getTopPlayerName(1);
            case "top_3":
                return getTopPlayerName(2);
            case "top_1_balance":
                return formatNumber(getTopPlayerBalance(0));
            case "top_2_balance":
                return formatNumber(getTopPlayerBalance(1));
            case "top_3_balance":
                return formatNumber(getTopPlayerBalance(2));
            default:
                if (params.startsWith("top_")) {
                    try {
                        int index = Integer.parseInt(params.substring(4)) - 1;
                        if (index >= 0 && index < 10) {
                            return getTopPlayerName(index);
                        }
                    } catch (NumberFormatException e) {
                        // ignore
                    }
                }
                return String.valueOf(balance);
        }
    }

    private int getPlayerRank(UUID uuid) {
        long balance = plugin.getBalance(uuid);
        List<Long> allBalances = new ArrayList<>(plugin.getInstance().getBalances().values());
        allBalances.sort(Comparator.reverseOrder());
        int rank = 1;
        for (Long b : allBalances) {
            if (b > balance) rank++;
            else break;
        }
        return rank;
    }

    private String formatRank(int rank) {
        if (rank == 1) return "§6🥇第1名";
        if (rank == 2) return "§7🥈第2名";
        if (rank == 3) return "§c🥉第3名";
        return "§e第" + rank + "名";
    }

    private String getTopPlayerName(int index) {
        List<Map.Entry<UUID, Long>> sorted = plugin.getInstance().getBalances().entrySet()
            .stream()
            .sorted(Map.Entry.<UUID, Long>comparingByValue().reversed())
            .collect(Collectors.toList());
        
        if (index < sorted.size()) {
            UUID topUuid = sorted.get(index).getKey();
            OfflinePlayer topPlayer = plugin.getServer().getOfflinePlayer(topUuid);
            return topPlayer.getName() != null ? topPlayer.getName() : "§c未知";
        }
        return "§7-";
    }

    private long getTopPlayerBalance(int index) {
        List<Map.Entry<UUID, Long>> sorted = plugin.getInstance().getBalances().entrySet()
            .stream()
            .sorted(Map.Entry.<UUID, Long>comparingByValue().reversed())
            .collect(Collectors.toList());
        
        if (index < sorted.size()) {
            return sorted.get(index).getValue();
        }
        return 0;
    }

    private String formatNumber(long num) {
        if (num >= 100000000) {
            return String.format("%.2f亿", num / 100000000.0);
        } else if (num >= 10000) {
            return String.format("%.2f万", num / 10000.0);
        }
        return String.format("%,d", num);
    }
}
