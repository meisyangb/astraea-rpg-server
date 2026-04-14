package cn.guangdian.rpgcore.http;

import cn.guangdian.rpgcore.RPGCore;
import cn.guangdian.rpgcore.api.HttpClient;
import okhttp3.*;

import java.io.IOException;
import java.time.Duration;
import java.util.Map;
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

    public HttpClientImpl(RPGCore plugin) {
        this.plugin = plugin;
        this.client = new OkHttpClient.Builder()
                .connectTimeout(TIMEOUT)
                .readTimeout(TIMEOUT)
                .writeTimeout(TIMEOUT)
                .connectionPool(new ConnectionPool(5, 5, TimeUnit.MINUTES))
                .retryOnConnectionFailure(true)
                .build();
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
