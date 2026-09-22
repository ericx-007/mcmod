package dev.thirteenblade.chat;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.io.ByteArrayOutputStream;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.Flow;
import java.util.concurrent.TimeUnit;

/** Small, non-streaming Chat Completions protocol client; never logs secrets or response bodies. */
public final class ChatTransport {
    private static final Gson GSON = new Gson();
    private static final HttpClient HTTP = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(8))
            .followRedirects(HttpClient.Redirect.NEVER).build();
    private static final int RESPONSE_LIMIT = 128 * 1024;

    private ChatTransport() {}

    public static CompletableFuture<String> request(ChatSettings settings, String system, List<ChatMessage> history) {
        JsonObject body = new JsonObject();
        body.addProperty("model", settings.model);
        body.addProperty("stream", false);
        body.addProperty("max_tokens", Math.max(32, Math.min(1024, settings.maxTokens)));
        body.addProperty("temperature", Double.isFinite(settings.temperature)
                ? Math.max(0, Math.min(2, settings.temperature)) : 0.8);
        JsonArray messages = new JsonArray();
        messages.add(GSON.toJsonTree(new ChatMessage("system", system)));
        history.stream().skip(Math.max(0, history.size() - 20)).forEach(message -> messages.add(GSON.toJsonTree(message)));
        body.add("messages", messages);
        int timeout = Math.max(5, Math.min(90, settings.timeoutSeconds));
        HttpRequest.Builder request = HttpRequest.newBuilder(settings.validatedEndpoint())
                .timeout(Duration.ofSeconds(timeout))
                .header("Content-Type", "application/json")
                .header("Accept", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(GSON.toJson(body), StandardCharsets.UTF_8));
        String key = settings.resolvedKey();
        if (!key.isEmpty()) request.header("Authorization", "Bearer " + key);
        CompletableFuture<HttpResponse<String>> transport = HTTP.sendAsync(request.build(), ignored -> new BoundedBody());
        CompletableFuture<String> reply = transport.thenApply(response -> {
            if (response.statusCode() < 200 || response.statusCode() >= 300)
                throw new ChatFailure("HTTP " + response.statusCode());
            return parseReply(response.body());
        });
        // A provider can send headers and then stall. Bound the entire body, not just connection/headers.
        reply.orTimeout(timeout, TimeUnit.SECONDS).whenComplete((value, error) -> {
            if (error != null) transport.cancel(true);
        });
        return reply;
    }

    static String parseReply(String response) {
        try {
            String content = JsonParser.parseString(response).getAsJsonObject().getAsJsonArray("choices")
                    .get(0).getAsJsonObject().getAsJsonObject("message").get("content").getAsString().trim();
            if (content.isBlank()) throw new IllegalArgumentException();
            // Plain text only: provider output is never interpreted as game commands or formatting codes.
            content = content.replaceAll("[\\p{Cntrl}&&[^\\n\\t]]", "").replace("\u00a7", "");
            return content.length() > 2000 ? content.substring(0, 2000) + "…" : content;
        } catch (RuntimeException e) {
            throw new ChatFailure("Invalid API response");
        }
    }

    public static final class ChatFailure extends RuntimeException {
        public ChatFailure(String message) { super(message); }
    }

    private static final class BoundedBody implements HttpResponse.BodySubscriber<String> {
        private final CompletableFuture<String> result = new CompletableFuture<>();
        private final ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        private Flow.Subscription subscription;

        public CompletionStage<String> getBody() { return result; }
        public void onSubscribe(Flow.Subscription subscription) {
            this.subscription = subscription;
            subscription.request(1);
        }
        public void onNext(List<ByteBuffer> buffers) {
            for (ByteBuffer buffer : buffers) {
                if (buffer.remaining() > RESPONSE_LIMIT - bytes.size()) {
                    subscription.cancel();
                    result.completeExceptionally(new ChatFailure("API response too large"));
                    return;
                }
                byte[] chunk = new byte[buffer.remaining()];
                buffer.get(chunk);
                bytes.writeBytes(chunk);
            }
            subscription.request(1);
        }
        public void onError(Throwable error) { result.completeExceptionally(error); }
        public void onComplete() { result.complete(bytes.toString(StandardCharsets.UTF_8)); }
    }
}
