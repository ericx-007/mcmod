package dev.thirteenblade.chat;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.sun.net.httpserver.HttpServer;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletionException;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import static org.junit.jupiter.api.Assertions.*;

class ChatTransportTest {
    private HttpServer server;
    private final AtomicReference<String> requestBody = new AtomicReference<>();
    private final AtomicReference<String> authorization = new AtomicReference<>();

    @AfterEach void stopServer() { if (server != null) server.stop(0); }

    private ChatSettings mock(int status, String response) throws Exception {
        server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/v1/chat/completions", exchange -> {
            requestBody.set(new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8));
            authorization.set(exchange.getRequestHeaders().getFirst("Authorization"));
            byte[] bytes = response.getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().set("Content-Type", "application/json");
            exchange.sendResponseHeaders(status, bytes.length);
            try (var stream = exchange.getResponseBody()) { stream.write(bytes); }
        });
        server.start();
        ChatSettings config = new ChatSettings();
        config.endpoint = "http://127.0.0.1:" + server.getAddress().getPort() + "/v1/chat/completions";
        config.model = "local-test-model";
        config.apiKeyEnvironmentVariable = "";
        return config;
    }

    @Test void postsPrivateConversationAndParsesUnicodeReply() throws Exception {
        ChatSettings config = mock(200, "{\"choices\":[{\"message\":{\"content\":\"我记得你。\"}}]}");
        config.apiKey = "fake-test-key";
        String reply = ChatTransport.request(config, "Sword state only", List.of(new ChatMessage("user", "你好"))).join();
        assertEquals("我记得你。", reply);
        assertEquals("Bearer fake-test-key", authorization.get());
        JsonObject body = JsonParser.parseString(requestBody.get()).getAsJsonObject();
        assertEquals("local-test-model", body.get("model").getAsString());
        assertFalse(body.get("stream").getAsBoolean());
        assertEquals("system", body.getAsJsonArray("messages").get(0).getAsJsonObject().get("role").getAsString());
        assertEquals("你好", body.getAsJsonArray("messages").get(1).getAsJsonObject().get("content").getAsString());
        assertFalse(requestBody.get().contains("fake-test-key"));
    }

    @Test void sendsOnlyRecentHistoryAndSupportsKeylessLocalModels() throws Exception {
        ChatSettings config = mock(200, "{\"choices\":[{\"message\":{\"content\":\"ok\"}}]}");
        List<ChatMessage> history = new ArrayList<>();
        for (int i = 0; i < 40; i++) history.add(new ChatMessage(i % 2 == 0 ? "user" : "assistant", "turn-" + i));
        ChatTransport.request(config, "system", history).join();
        var messages = JsonParser.parseString(requestBody.get()).getAsJsonObject().getAsJsonArray("messages");
        assertEquals(21, messages.size());
        assertEquals("turn-20", messages.get(1).getAsJsonObject().get("content").getAsString());
        assertNull(authorization.get());
    }

    @Test void remotePlainHttpAndCredentialBearingUrlsAreRejected() {
        ChatSettings config = new ChatSettings();
        for (String endpoint : List.of("http://example.com/v1/chat/completions", "https://key@example.com/path",
                "https://example.com/path?api_key=secret", "file:///tmp/key", "https://example.com/path#secret")) {
            config.endpoint = endpoint;
            assertThrows(IllegalArgumentException.class, config::validatedEndpoint, endpoint);
        }
        config.endpoint = "https://example.com/v1/chat/completions";
        assertDoesNotThrow(config::validatedEndpoint);
        config.endpoint = "http://localhost:11434/v1/chat/completions";
        assertDoesNotThrow(config::validatedEndpoint);
        config.endpoint = "http://[::1]:11434/v1/chat/completions";
        assertDoesNotThrow(config::validatedEndpoint);
    }

    @Test void httpErrorsDoNotExposeProviderResponseBodies() throws Exception {
        ChatSettings config = mock(401, "secret provider diagnostics that must stay private");
        CompletionException exception = assertThrows(CompletionException.class,
                () -> ChatTransport.request(config, "system", List.of()).join());
        assertEquals("HTTP 401", exception.getCause().getMessage());
    }

    @Test void oversizedResponsesAreCancelled() throws Exception {
        ChatSettings config = mock(200, "x".repeat(140000));
        CompletionException exception = assertThrows(CompletionException.class,
                () -> ChatTransport.request(config, "system", List.of()).join());
        assertTrue(exception.getCause().getMessage().contains("too large"));
    }

    @Test void providerThatStallsAfterHeadersCannotKeepChatBusyForever() throws Exception {
        CountDownLatch release = new CountDownLatch(1);
        server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/stalled", exchange -> {
            exchange.sendResponseHeaders(200, 100);
            exchange.getResponseBody().write(' ');
            exchange.getResponseBody().flush();
            try { release.await(10, TimeUnit.SECONDS); }
            catch (InterruptedException e) { Thread.currentThread().interrupt(); }
            finally { exchange.close(); }
        });
        server.start();
        ChatSettings config = new ChatSettings();
        config.endpoint = "http://127.0.0.1:" + server.getAddress().getPort() + "/stalled";
        config.apiKeyEnvironmentVariable = "";
        config.timeoutSeconds = 5;
        try {
            var future = ChatTransport.request(config, "system", List.of());
            assertThrows(java.util.concurrent.ExecutionException.class, () -> future.get(7, TimeUnit.SECONDS));
            assertTrue(future.isCompletedExceptionally());
        } finally { release.countDown(); }
    }

    @Test void malformedOrEmptyProviderResponsesFailCleanly() {
        for (String invalid : List.of("not json", "{}", "{\"choices\":[]}",
                "{\"choices\":[{\"message\":{\"content\":null}}]}",
                "{\"choices\":[{\"message\":{\"content\":\"  \"}}]}"))
            assertThrows(ChatTransport.ChatFailure.class, () -> ChatTransport.parseReply(invalid));
    }

    @Test void providerFormattingCannotInjectMinecraftColorCodes() {
        assertEquals("a4b\nc", ChatTransport.parseReply("{\"choices\":[{\"message\":{\"content\":\"a§4b\\nc\"}}]}"));
    }

    @Test void firstLaunchIsOfflineAndCreatesEditableConfiguration(@TempDir Path temporary) throws Exception {
        Path path = temporary.resolve("config/chat.json");
        ChatSettings config = ChatSettings.load(path);
        assertFalse(config.enabled);
        assertEquals("", config.apiKey);
        assertTrue(Files.readString(path).contains("apiKeyEnvironmentVariable"));
    }
}
