package dev.thirteenblade.chat;
public final class ChatMessage {
    public final String role, content;
    public ChatMessage(String role, String content) { this.role = role; this.content = content; }
    public String role() { return role; }
    public String content() { return content; }
}
