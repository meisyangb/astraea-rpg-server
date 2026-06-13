package cn.guangdian.rpgcore.api;

import org.bukkit.entity.Player;

import java.util.concurrent.CompletableFuture;

public interface HttpClient {

    CompletableFuture<String> get(String url);

    CompletableFuture<String> post(String url, String body);

    CompletableFuture<String> post(String url, String body, String mediaType);

    CompletableFuture<String> put(String url, String body);

    CompletableFuture<String> delete(String url);

    CompletableFuture<String> request(String url, String method, String body, String mediaType);

    interface RateLimiter {

        boolean tryAcquire(Player player, String action);

        boolean tryAcquire(String playerId, String action);

        void setLimit(String action, int maxPerMinute);

        void setLimit(String action, int maxPerMinute, int windowSeconds);

        int getRemaining(String playerId, String action);

        void clearPlayer(String playerId);

        void clearAll();

        record LimitConfig(int maxRequests, int windowSeconds) {}
    }
}
