package dev.thirteenblade.chat;

import com.google.gson.*;
import java.io.*;
import java.net.*;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicReference;

/** Java 8 transport. API credentials and messages never pass through the Minecraft server. */
public final class ChatTransport {
    private static final Gson GSON = new Gson();
    private static final ExecutorService WORKERS = Executors.newFixedThreadPool(2, r -> { Thread t = new Thread(r, "ThirteenBlade chat"); t.setDaemon(true); return t; });
    private static final ScheduledExecutorService TIMER = Executors.newSingleThreadScheduledExecutor(r -> { Thread t = new Thread(r, "ThirteenBlade timeout"); t.setDaemon(true); return t; });
    private ChatTransport() {}
    public static CompletableFuture<String> request(ChatSettings settings, String system, List<ChatMessage> history) {
        URI endpoint = settings.validatedEndpoint(); String key = settings.resolvedKey();
        JsonObject body = new JsonObject(); body.addProperty("model", settings.model); body.addProperty("stream", false);
        body.addProperty("max_tokens", Math.max(32, Math.min(1024, settings.maxTokens)));
        body.addProperty("temperature", Double.isFinite(settings.temperature) ? Math.max(0, Math.min(2, settings.temperature)) : .8);
        JsonArray messages = new JsonArray(); messages.add(GSON.toJsonTree(new ChatMessage("system", system)));
        history.stream().skip(Math.max(0, history.size() - 20)).forEach(m -> messages.add(GSON.toJsonTree(m))); body.add("messages", messages);
        byte[] payload = GSON.toJson(body).getBytes(StandardCharsets.UTF_8);
        int timeout = Math.max(5, Math.min(90, settings.timeoutSeconds)) * 1000;
        CompletableFuture<String> result = new CompletableFuture<>(); AtomicReference<HttpURLConnection> active = new AtomicReference<>();
        ScheduledFuture<?> timer = TIMER.schedule(() -> {
            result.completeExceptionally(new ChatFailure("API timeout")); HttpURLConnection c = active.get(); if (c != null) c.disconnect();
        }, timeout, TimeUnit.MILLISECONDS);
        result.whenComplete((value, error) -> { timer.cancel(false); if (error != null) { HttpURLConnection c = active.get(); if (c != null) c.disconnect(); } });
        WORKERS.execute(() -> {
            HttpURLConnection connection = null;
            try {
                if (result.isDone()) return;
                connection = (HttpURLConnection)endpoint.toURL().openConnection(); active.set(connection);
                connection.setInstanceFollowRedirects(false); connection.setConnectTimeout(Math.min(timeout, 8000)); connection.setReadTimeout(timeout);
                connection.setRequestMethod("POST"); connection.setRequestProperty("Content-Type", "application/json; charset=utf-8"); connection.setRequestProperty("Accept", "application/json");
                if (!key.isEmpty()) connection.setRequestProperty("Authorization", "Bearer " + key);
                connection.setDoOutput(true); connection.setFixedLengthStreamingMode(payload.length);
                try (OutputStream output = connection.getOutputStream()) { output.write(payload); }
                int status = connection.getResponseCode(); if (status < 200 || status >= 300) throw new ChatFailure("HTTP " + status);
                try (InputStream input = connection.getInputStream(); ByteArrayOutputStream bytes = new ByteArrayOutputStream()) {
                    byte[] buffer = new byte[4096]; int count;
                    while ((count = input.read(buffer)) >= 0) {
                        if (result.isDone()) return;
                        if (bytes.size() + count > 128 * 1024) throw new ChatFailure("API response too large"); bytes.write(buffer, 0, count);
                    }
                    result.complete(parseReply(new String(bytes.toByteArray(), StandardCharsets.UTF_8)));
                }
            } catch (Exception e) { result.completeExceptionally(e instanceof ChatFailure ? e : new ChatFailure("API request failed")); }
            finally { if (connection != null) connection.disconnect(); }
        });
        return result;
    }
    static String parseReply(String response) {
        try {
            String content = new JsonParser().parse(response).getAsJsonObject().getAsJsonArray("choices").get(0).getAsJsonObject().getAsJsonObject("message").get("content").getAsString().trim();
            if (content.isEmpty()) throw new IllegalArgumentException();
            content = content.replaceAll("[\\p{Cntrl}&&[^\\n\\t]]", "").replace("\u00a7", "");
            return content.length() > 2000 ? content.substring(0, 2000) + "…" : content;
        } catch (RuntimeException invalid) { throw new ChatFailure("Invalid API response"); }
    }
    public static final class ChatFailure extends RuntimeException { public ChatFailure(String message) { super(message); } }
}
