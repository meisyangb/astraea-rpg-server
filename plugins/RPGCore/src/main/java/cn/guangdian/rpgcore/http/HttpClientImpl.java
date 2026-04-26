package cn.guangdian.rpgcore.http;

import cn.guangdian.rpgcore.RPGCore;
import cn.guangdian.rpgcore.api.HttpClient;
import okhttp3.*;

import java.io.IOException;
import java.time.Duration;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

public class HttpClientImpl implements HttpClient {

    private final RPGCore plugin;
    private final OkHttpClient client;
    private final Map<String, String> defaultHeaders = new ConcurrentHashMap<>();
    private final Map<String, RateLimiter.LimitConfig> rateLimitConfigs = new ConcurrentHashMap<>();
    private final Map<String, Map<String, RateLimitEntry>> playerLimits = new ConcurrentHashMap<>();

    private static final String DEFAULT_MEDIA_TYPE = "application/json";
    private static final Duration TIMEOUT = Duration.ofSeconds(10);
    private static final Set<String> ALLOWED_SCHEMES = Set.of("http", "https");
    private static final Set<String> BLOCKED_HOSTS = Set.of(
        "localhost", "127.0.0.1", "0.0.0.0", "::1",
        "10.0.0.0", "172.16.0.0", "192.168.0.0",
        "169.254.0.0", "224.0.0.0", "240.0.0.0"
    );

    public HttpClientImpl(RPGCore plugin) {
        this.plugin = plugin;
        this.client = new OkHttpClient.Builder()
                .connectTimeout(TIMEOUT)
                .readTimeout(TIMEOUT)
                .writeTimeout(TIMEOUT)
                .connectionPool(new ConnectionPool(5, 5, TimeUnit.MINUTES))
                .retryOnConnectionFailure(true)
                .followRedirects(false)
                .followSslRedirects(false)
                .build();
    }

    /**
     * 验证 URL 是否安全（防止 SSRF 攻击）
     */
    private boolean isUrlSafe(String url) {
        try {
            java.net.URI uri = new java.net.URI(url);
            String scheme = uri.getScheme();
            String host = uri.getHost();

            if (scheme == null || host == null) {
                return false;
            }

            if (!ALLOWED_SCHEMES.contains(scheme.toLowerCase())) {
                return false;
            }

            String lowerHost = host.toLowerCase();

            // 检查精确匹配和域名后缀匹配
            for (String blockedHost : BLOCKED_HOSTS) {
                if (lowerHost.equals(blockedHost) || lowerHost.endsWith("." + blockedHost)) {
                    return false;
                }
            }

            // IPv4 私有地址范围检查
            if (lowerHost.startsWith("10.") || 
                lowerHost.startsWith("127.") || 
                lowerHost.startsWith("192.168.") || 
                lowerHost.startsWith("169.254.") ||
                lowerHost.startsWith("0.")) {
                return false;
            }

            // 172.16.0.0/12 范围：172.16.0.0 - 172.31.255.255
            if (lowerHost.startsWith("172.")) {
                try {
                    String[] parts = lowerHost.split("\\.");
                    if (parts.length >= 2) {
                        int secondOctet = Integer.parseInt(parts[1]);
                        if (secondOctet >= 16 && secondOctet <= 31) {
                            return false;
                        }
                    }
                } catch (NumberFormatException e) {
                    return false;
                }
            }

            // IPv6 链路本地地址检查 (fe80::/10)
            if (lowerHost.startsWith("fe80:") || lowerHost.startsWith("fe81:") ||
                lowerHost.startsWith("fe82:") || lowerHost.startsWith("fe83:") ||
                lowerHost.startsWith("fe84:") || lowerHost.startsWith("fe85:") ||
                lowerHost.startsWith("fe86:") || lowerHost.startsWith("fe87:") ||
                lowerHost.startsWith("fe88:") || lowerHost.startsWith("fe89:") ||
                lowerHost.startsWith("fe8a:") || lowerHost.startsWith("fe8b:") ||
                lowerHost.startsWith("fe8c:") || lowerHost.startsWith("fe8d:") ||
                lowerHost.startsWith("fe8e:") || lowerHost.startsWith("fe8f:")) {
                return false;
            }

            // IPv6 本地回环地址 (::1)
            if (lowerHost.equals("::1") || lowerHost.equals("0:0:0:0:0:0:0:1")) {
                return false;
            }

            return true;
        } catch (Exception e) {
            return false;
        }
    }

    public void setDefaultHeader(String key, String value) {
        defaultHeaders.put(key, value);
    }

    public void removeDefaultHeader(String key) {
        defaultHeaders.remove(key);
    }

    @Override
    public CompletableFuture<String> get(String url) {
        return request(url, "GET", null, null);
    }

    @Override
    public CompletableFuture<String> post(String url, String body) {
        return post(url, body, DEFAULT_MEDIA_TYPE);
    }

    @Override
    public CompletableFuture<String> post(String url, String body, String mediaType) {
        return request(url, "POST", body, mediaType);
    }

    @Override
    public CompletableFuture<String> put(String url, String body) {
        return request(url, "PUT", body, DEFAULT_MEDIA_TYPE);
    }

    @Override
    public CompletableFuture<String> delete(String url) {
        return request(url, "DELETE", null, null);
    }

    @Override
    public CompletableFuture<String> request(String url, String method, String body, String mediaType) {
        if (!isUrlSafe(url)) {
            CompletableFuture<String> failed = new CompletableFuture<>();
            failed.completeExceptionally(new SecurityException("Blocked unsafe URL: " + url));
            return failed;
        }

        CompletableFuture<String> result = new CompletableFuture<>();

        Request.Builder requestBuilder = new Request.Builder()
                .url(url);

        for (Map.Entry<String, String> header : defaultHeaders.entrySet()) {
            requestBuilder.addHeader(header.getKey(), header.getValue());
        }

        if (mediaType != null && !mediaType.isEmpty()) {
            requestBuilder.addHeader("Content-Type", mediaType);
        }

        switch (method.toUpperCase()) {
            case "GET" -> requestBuilder.get();
            case "DELETE" -> requestBuilder.delete();
            case "POST" -> requestBuilder.post(body != null ?
                    RequestBody.create(body, MediaType.parse(mediaType != null ? mediaType : DEFAULT_MEDIA_TYPE)) :
                    RequestBody.create("", MediaType.parse("application/octet-stream")));
            case "PUT" -> requestBuilder.put(body != null ?
                    RequestBody.create(body, MediaType.parse(mediaType != null ? mediaType : DEFAULT_MEDIA_TYPE)) :
                    RequestBody.create("", MediaType.parse("application/octet-stream")));
            case "PATCH" -> requestBuilder.patch(body != null ?
                    RequestBody.create(body, MediaType.parse(mediaType != null ? mediaType : DEFAULT_MEDIA_TYPE)) :
                    RequestBody.create("", MediaType.parse("application/octet-stream")));
            default -> requestBuilder.get();
        }

        Request request = requestBuilder.build();

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                result.completeExceptionally(e);
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                try (ResponseBody responseBody = response.body()) {
                    if (response.isSuccessful()) {
                        result.complete(responseBody != null ? responseBody.string() : "");
                    } else {
                        result.completeExceptionally(new IOException("HTTP " + response.code() + ": " + response.message()));
                    }
                }
            }
        });

        return result;
    }

    public CompletableFuture<String> getWithHeaders(String url, Map<String, String> headers) {
        if (!isUrlSafe(url)) {
            CompletableFuture<String> failed = new CompletableFuture<>();
            failed.completeExceptionally(new SecurityException("Blocked unsafe URL: " + url));
            return failed;
        }

        CompletableFuture<String> result = new CompletableFuture<>();

        Request.Builder requestBuilder = new Request.Builder()
                .url(url)
                .get();

        for (Map.Entry<String, String> header : defaultHeaders.entrySet()) {
            requestBuilder.addHeader(header.getKey(), header.getValue());
        }
        for (Map.Entry<String, String> header : headers.entrySet()) {
            requestBuilder.addHeader(header.getKey(), header.getValue());
        }

        Request request = requestBuilder.build();

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                result.completeExceptionally(e);
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                try (ResponseBody responseBody = response.body()) {
                    if (response.isSuccessful()) {
                        result.complete(responseBody != null ? responseBody.string() : "");
                    } else {
                        result.completeExceptionally(new IOException("HTTP " + response.code() + ": " + response.message()));
                    }
                }
            }
        });

        return result;
    }

    public void shutdown() {
        try {
            client.dispatcher().executorService().shutdown();
            client.connectionPool().evictAll();
        } catch (Exception ignored) {
        }
    }

    private static class RateLimitEntry {
        final int count;
        final long windowStart;

        RateLimitEntry(int count, long windowStart) {
            this.count = count;
            this.windowStart = windowStart;
        }
    }
}
