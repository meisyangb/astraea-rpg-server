package cn.guangdian.gearscore.manager;

import cn.guangdian.gearscore.GuangDianGearScore;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class LeaderboardManager {

    private final GuangDianGearScore plugin;
    private final Map<UUID, Long> sortedScores = new ConcurrentHashMap<>();
    private volatile List<Map.Entry<UUID, Long>> leaderboard = new ArrayList<>();
    private volatile Map<UUID, Integer> rankCache = new ConcurrentHashMap<>();

    public LeaderboardManager(GuangDianGearScore plugin) {
        this.plugin = plugin;
    }

    public void updateLeaderboard() {
        Map<UUID, Long> scores = plugin.getPlayerScores();
        
        List<Map.Entry<UUID, Long>> sorted = new ArrayList<>(scores.entrySet());
        sorted.sort((a, b) -> Long.compare(b.getValue(), a.getValue()));
        
        leaderboard = sorted;
        
        Map<UUID, Integer> newRankCache = new ConcurrentHashMap<>();
        for (int i = 0; i < sorted.size(); i++) {
            newRankCache.put(sorted.get(i).getKey(), i + 1);
        }
        rankCache = newRankCache;
    }

    public int getRank(UUID uuid) {
        Integer rank = rankCache.get(uuid);
        return rank != null ? rank : -1;
    }

    public List<Map.Entry<UUID, Long>> getTopPlayers(int count) {
        List<Map.Entry<UUID, Long>> result = new ArrayList<>();
        List<Map.Entry<UUID, Long>> current = leaderboard;
        
        int limit = Math.min(count, current.size());
        for (int i = 0; i < limit; i++) {
            result.add(current.get(i));
        }
        
        return result;
    }

    public long getScoreAtRank(int rank) {
        if (rank < 1 || rank > leaderboard.size()) {
            return 0;
        }
        return leaderboard.get(rank - 1).getValue();
    }

    public int getTotalPlayers() {
        return leaderboard.size();
    }
}
