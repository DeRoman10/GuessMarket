package com.ui.core;

import com.google.gson.Gson;
import okhttp3.*;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.TimeUnit;

public final class ApiClient {

    public static final String BASE_URL =
            System.getProperty("gm.baseUrl", "http://localhost:8080/GuessMarketServer");

    public static final Gson GSON = new Gson();

    private static final OkHttpClient CLIENT = new OkHttpClient.Builder()
            .cookieJar(new MemoryCookieJar())
            .connectTimeout(5, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .build();

    private ApiClient() {}

    public static OkHttpClient http() { return CLIENT; }

    public static HttpUrl.Builder url(String path) {
        return HttpUrl.parse(BASE_URL + path).newBuilder();
    }

    public static Request get(HttpUrl url) { return new Request.Builder().url(url).get().build(); }

    public static Request post(HttpUrl url) {
        return new Request.Builder().url(url).post(RequestBody.create(MediaType.parse("text/plain"), "")).build();
    }

    public static String message(String body) {
        try {
            String s = GSON.fromJson(body, String.class);
            return s != null ? s : body;
        } catch (Exception e) {
            return body;
        }
    }

    public static void shutdown() {
        CLIENT.dispatcher().executorService().shutdown();
        CLIENT.connectionPool().evictAll();
    }

    private static final class MemoryCookieJar implements CookieJar {
        private final List<Cookie> store = new CopyOnWriteArrayList<>();

        @Override
        public void saveFromResponse(HttpUrl url, List<Cookie> cookies) {
            for (Cookie c : cookies) {
                store.removeIf(o -> o.name().equals(c.name()) && o.domain().equals(c.domain()) && o.path().equals(c.path()));
                store.add(c);
            }
        }

        @Override
        public List<Cookie> loadForRequest(HttpUrl url) {
            long now = System.currentTimeMillis();
            return store.stream().filter(c -> c.expiresAt() > now && c.matches(url)).toList();
        }
    }

    public static void clearSession() {
        if (CLIENT.cookieJar() instanceof MemoryCookieJar jar) {
            jar.store.clear();
        }
    }
}
