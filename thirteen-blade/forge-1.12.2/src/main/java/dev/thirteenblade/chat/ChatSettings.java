package dev.thirteenblade.chat;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import java.io.IOException;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

/** Client-local settings. These are never synchronized to a game server. */
public final class ChatSettings {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    public boolean enabled = false;
    public String endpoint = "https://api.example.com/v1/chat/completions";
    public String model = "your-model-name";
    public String apiKeyEnvironmentVariable = "THIRTEEN_BLADE_API_KEY";
    public String apiKey = "";
    public int timeoutSeconds = 30;
    public int maxTokens = 240;
    public double temperature = 0.8;

    public static ChatSettings load(Path path) throws IOException {
        if (!Files.exists(path)) {
            Files.createDirectories(path.getParent());
            Files.write(path, GSON.toJson(new ChatSettings()).getBytes(StandardCharsets.UTF_8));
        }
        try {
            ChatSettings settings = GSON.fromJson(new String(Files.readAllBytes(path), StandardCharsets.UTF_8), ChatSettings.class);
            if (settings == null) throw new IOException("Empty chat configuration");
            return settings;
        } catch (RuntimeException e) {
            throw new IOException("Invalid chat configuration");
        }
    }

    public URI validatedEndpoint() {
        URI uri = URI.create(endpoint == null ? "" : endpoint.trim());
        String host = uri.getHost();
        boolean local = "localhost".equalsIgnoreCase(host) || "127.0.0.1".equals(host)
                || "[::1]".equals(host) || "::1".equals(host);
        if (host == null || uri.getUserInfo() != null || uri.getFragment() != null || uri.getQuery() != null
                || !("https".equalsIgnoreCase(uri.getScheme()) || (local && "http".equalsIgnoreCase(uri.getScheme())))) {
            throw new IllegalArgumentException("Use HTTPS, or HTTP with a loopback host");
        }
        if (model == null || model.trim().isEmpty() || model.length() > 200) {
            throw new IllegalArgumentException("A model name is required");
        }
        return uri;
    }

    public String resolvedKey() {
        String environment = apiKeyEnvironmentVariable == null || apiKeyEnvironmentVariable.trim().isEmpty()
                ? null : System.getenv(apiKeyEnvironmentVariable);
        return environment != null && !environment.trim().isEmpty() ? environment.trim() : apiKey == null ? "" : apiKey.trim();
    }
}
